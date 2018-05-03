/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.services.data;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ldap.LdapUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.JDKMessageDigest;
import org.kitodo.data.database.beans.LdapServer;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.helper.enums.PasswordEncryption;
import org.kitodo.data.database.persistence.LdapServerDAO;
import org.kitodo.security.SecurityPasswordEncoder;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.base.SearchDatabaseService;

public class LdapServerService extends SearchDatabaseService<LdapServer, LdapServerDAO> {

    private final ServiceManager serviceManager = new ServiceManager();
    private static final Logger logger = LogManager.getLogger(LdapServerService.class);
    private static LdapServerService instance = null;
    private SecurityPasswordEncoder passwordEncoder = new SecurityPasswordEncoder();

    /**
     * Return singleton variable of type LdapServerService.
     *
     * @return unique instance of LdapServerService
     */
    public static LdapServerService getInstance() {
        if (Objects.equals(instance, null)) {
            synchronized (LdapServerService.class) {
                if (Objects.equals(instance, null)) {
                    instance = new LdapServerService();
                }
            }
        }
        return instance;
    }

    private LdapServerService() {
        super(new LdapServerDAO());
    }

    @Override
    public Long countDatabaseRows() throws DAOException {
        return countDatabaseRows("FROM LdapServer");
    }

    private String buildUserDN(User inUser) {
        String userDN = inUser.getLdapGroup().getUserDN();
        userDN = userDN.replaceAll("\\{login\\}", inUser.getLogin());
        if (inUser.getLdapLogin() != null) {
            userDN = userDN.replaceAll("\\{ldaplogin\\}", inUser.getLdapLogin());
        }
        userDN = userDN.replaceAll("\\{firstname\\}", inUser.getName());
        userDN = userDN.replaceAll("\\{lastname\\}", inUser.getSurname());
        return userDN;
    }

    private Hashtable<String, String> initializeWithLdapConnectionSettings(LdapServer ldapServer) {
        Hashtable<String, String> env = new Hashtable<>(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapServer.getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapServer.getManagerLogin());

        String encryptedManagerPassword = ldapServer.getManagerPassword();
        String decryptedManagerPassword = passwordEncoder.decrypt(encryptedManagerPassword);

        env.put(Context.SECURITY_CREDENTIALS, decryptedManagerPassword);

        if (ldapServer.isUseSsl()) {
            String keystorepath = ldapServer.getKeystore();
            String keystorepasswd = ldapServer.getKeystorePassword();

            // add all necessary certificates first
            loadCertificates(keystorepath, keystorepasswd, ldapServer);

            // set properties, so that the current keystore is used for SSL
            System.setProperty("javax.net.ssl.keyStore", keystorepath);
            System.setProperty("javax.net.ssl.trustStore", keystorepath);
            System.setProperty("javax.net.ssl.keyStorePassword", keystorepasswd);
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        return env;
    }

    /**
     * create new user in LDAP-directory.
     *
     * @param user
     *            User object
     * @param password
     *            String
     */
    public void createNewUser(User user, String password)
            throws NamingException, NoSuchAlgorithmException, IOException {

        if (!user.getLdapGroup().getLdapServer().isReadOnly()) {
            Hashtable<String, String> ldapEnvironment = initializeWithLdapConnectionSettings(
                user.getLdapGroup().getLdapServer());

            LdapUser ldapUser = new LdapUser();
            ldapUser.configure(user, password, getNextUidNumber(user.getLdapGroup().getLdapServer()));
            DirContext ctx = new InitialDirContext(ldapEnvironment);
            ctx.bind(buildUserDN(user), ldapUser);
            ctx.close();
            setNextUidNumber(user.getLdapGroup().getLdapServer());
            Helper.setMeldung(null,
                Helper.getTranslation("ldapWritten") + " " + serviceManager.getUserService().getFullName(user), "");
            /*
             * check if HomeDir exists, else create it
             */
            logger.debug("HomeVerzeichnis pruefen");

            URI homePath = getUserHomeDirectory(user);

            if (!new File(homePath).exists()) {
                logger.debug("HomeVerzeichnis existiert noch nicht");
                serviceManager.getFileService().createDirectoryForUser(homePath, user.getLogin());
                logger.debug("HomeVerzeichnis angelegt");
            } else {
                logger.debug("HomeVerzeichnis existiert schon");
            }
        } else {
            Helper.setMeldung(Helper.getTranslation("ldapIsReadOnly"));
        }
    }

    /**
     * Check if connection with login and password possible.
     *
     * @param user
     *            User object
     * @param password
     *            String
     * @return Login correct or not
     */
    public boolean isUserPasswordCorrect(User user, String password) {
        logger.debug("start login session with ldap");
        Hashtable<String, String> env = initializeWithLdapConnectionSettings(user.getLdapGroup().getLdapServer());

        // Start TLS
        if (ConfigCore.getBooleanParameter("ldap_useTLS", false)) {
            logger.debug("use TLS for auth");
            env.put("java.naming.ldap.version", "3");
            LdapContext ctx = null;
            StartTlsResponse tls = null;
            try {
                ctx = new InitialLdapContext(env, null);

                // Authentication must be performed over a secure channel
                tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
                tls.negotiate();

                // Authenticate via SASL EXTERNAL mechanism using client X.509
                // certificate contained in JVM keystore
                ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, buildUserDN(user));
                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
                ctx.reconnect(null);
                return true;
                // Perform search for privileged attributes under authenticated
                // context

            } catch (IOException e) {
                logger.error("TLS negotiation error:", e);
                return false;
            } catch (NamingException e) {
                logger.error("JNDI error:", e);
                return false;
            } finally {
                if (tls != null) {
                    try {
                        // Tear down TLS connection
                        tls.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                if (ctx != null) {
                    try {
                        // Close LDAP connection
                        ctx.close();
                    } catch (NamingException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            logger.debug("don't use TLS for auth");
            if (ConfigCore.getBooleanParameter("useSimpleAuthentification", false)) {
                env.put(Context.SECURITY_AUTHENTICATION, "none");
                // TODO auf passwort testen
            } else {
                env.put(Context.SECURITY_PRINCIPAL, buildUserDN(user));
                env.put(Context.SECURITY_CREDENTIALS, password);
            }
            logger.debug("ldap environment set");

            try {
                logger.debug("start classic ldap authentication");
                logger.debug("user DN is {}", buildUserDN(user));

                if (ConfigCore.getParameter("ldap_AttributeToTest") == null) {
                    logger.debug("ldap attribute to test is null");
                    DirContext ctx = new InitialDirContext(env);
                    ctx.close();
                    return true;
                } else {
                    logger.debug("ldap attribute to test is not null");
                    DirContext ctx = new InitialDirContext(env);

                    Attributes attrs = ctx.getAttributes(buildUserDN(user));
                    Attribute la = attrs.get(ConfigCore.getParameter("ldap_AttributeToTest"));
                    logger.debug("ldap attributes set");
                    String test = (String) la.get(0);
                    if (test.equals(ConfigCore.getParameter("ldap_ValueOfAttribute"))) {
                        logger.debug("ldap ok");
                        ctx.close();
                        return true;
                    } else {
                        logger.debug("ldap not ok");
                        ctx.close();
                        return false;
                    }
                }
            } catch (NamingException e) {
                logger.debug("login not allowed for {}. Exception: {}", user.getLogin(), e);
                return false;
            }
        }
    }

    /**
     * Retrieve home directory of given user.
     *
     * @param user
     *            User object
     * @return path as URI
     */
    public URI getUserHomeDirectory(User user) {

        URI userFolderBasePath = URI.create("file:///" + ConfigCore.getParameter("dir_Users"));

        if (ConfigCore.getBooleanParameter("useLocalDirectory", false)) {
            return userFolderBasePath.resolve(user.getLogin());
        }
        Hashtable<String, String> env = initializeWithLdapConnectionSettings(user.getLdapGroup().getLdapServer());
        if (ConfigCore.getBooleanParameter("ldap_useTLS", false)) {

            env.put("java.naming.ldap.version", "3");
            LdapContext ctx = null;
            StartTlsResponse tls = null;
            try {
                ctx = new InitialLdapContext(env, null);

                // Authentication must be performed over a secure channel
                tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
                tls.negotiate();

                ctx.reconnect(null);

                Attributes attrs = ctx.getAttributes(buildUserDN(user));
                Attribute la = attrs.get("homeDirectory");
                return URI.create((String) la.get(0));

                // Perform search for privileged attributes under authenticated
                // context

            } catch (IOException e) {
                logger.error("TLS negotiation error:", e);

                return userFolderBasePath.resolve(user.getLogin());
            } catch (NamingException e) {

                logger.error("JNDI error:", e);

                return userFolderBasePath.resolve(user.getLogin());
            } finally {
                if (tls != null) {
                    try {
                        // Tear down TLS connection
                        tls.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                if (ctx != null) {
                    try {
                        // Close LDAP connection
                        ctx.close();
                    } catch (NamingException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        if (ConfigCore.getBooleanParameter("useSimpleAuthentification", false)) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        DirContext ctx;
        URI userFolderPath = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(buildUserDN(user));
            Attribute ldapAttribute = attrs.get("homeDirectory");
            userFolderPath = URI.create((String) ldapAttribute.get(0));
            ctx.close();
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }

        if (userFolderPath != null && !userFolderPath.isAbsolute()) {
            if (userFolderPath.getPath().startsWith("/")) {
                userFolderPath = serviceManager.getFileService().deleteFirstSlashFromPath(userFolderPath);
            }
            return userFolderBasePath.resolve(userFolderPath);
        } else {
            return userFolderPath;
        }
    }

    /**
     * Check if User already exists on system.
     *
     * @param user
     *            The User.
     * @return result as boolean
     */
    public boolean isUserAlreadyExists(User user) {
        Hashtable<String, String> ldapEnvironment = initializeWithLdapConnectionSettings(
            user.getLdapGroup().getLdapServer());
        DirContext ctx;
        boolean result = false;
        try {
            ctx = new InitialDirContext(ldapEnvironment);
            Attributes matchAttrs = new BasicAttributes(true);
            NamingEnumeration<SearchResult> answer = ctx.search(buildUserDN(user), matchAttrs);
            result = answer.hasMoreElements();

            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                logger.debug(">>>{}", sr.getName());
                Attributes attrs = sr.getAttributes();
                String givenName = getStringForAttribute(attrs, "givenName");
                String surName = getStringForAttribute(attrs, "sn");
                String mail = getStringForAttribute(attrs, "mail");
                String cn = getStringForAttribute(attrs, "cn");
                String homeDirectory = getStringForAttribute(attrs, "homeDirectory");

                logger.debug(givenName);
                logger.debug(surName);
                logger.debug(mail);
                logger.debug(cn);
                logger.debug(homeDirectory);
            }

            ctx.close();
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    private String getStringForAttribute(Attributes attrs, String identifier) {
        try {
            return attrs.get(identifier).toString();
        } catch (RuntimeException e) {
            return " ";
        }
    }

    /**
     * Get next free uidNumber.
     *
     * @return next free uidNumber
     */
    private String getNextUidNumber(LdapServer ldapServer) {
        Hashtable<String, String> ldapEnvironment = initializeWithLdapConnectionSettings(ldapServer);
        DirContext ctx;
        String rueckgabe = "";
        try {
            ctx = new InitialDirContext(ldapEnvironment);
            Attributes attrs = ctx.getAttributes(ldapServer.getNextFreeUnixIdPattern());
            Attribute la = attrs.get("uidNumber");
            rueckgabe = (String) la.get(0);
            ctx.close();
        } catch (NamingException e) {
            Helper.setErrorMessage(e.getMessage(), logger, e);
        }
        return rueckgabe;
    }

    /**
     * Set next free uidNumber.
     */
    private void setNextUidNumber(LdapServer ldapServer) {
        Hashtable<String, String> ldapEnvironment = initializeWithLdapConnectionSettings(ldapServer);
        DirContext ctx;

        try {
            ctx = new InitialDirContext(ldapEnvironment);
            Attributes attrs = ctx.getAttributes(ldapServer.getNextFreeUnixIdPattern());
            Attribute la = attrs.get("uidNumber");
            String oldValue = (String) la.get(0);
            int bla = Integer.parseInt(oldValue) + 1;

            BasicAttribute attrNeu = new BasicAttribute("uidNumber", String.valueOf(bla));
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrNeu);
            ctx.modifyAttributes(ldapServer.getNextFreeUnixIdPattern(), mods);

            ctx.close();
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * change password of given user, needs old password for authentication.
     *
     * @param user
     *            User object
     * @param inNewPassword
     *            String
     * @return boolean about result of change
     */
    public boolean changeUserPassword(User user, String inNewPassword) throws NoSuchAlgorithmException {
        JDKMessageDigest.MD4 digester = new JDKMessageDigest.MD4();
        PasswordEncryption passwordEncryption = user.getLdapGroup().getLdapServer().getPasswordEncryptionEnum();
        Hashtable<String, String> env = initializeWithLdapConnectionSettings(user.getLdapGroup().getLdapServer());
        if (!user.getLdapGroup().getLdapServer().isReadOnly()) {
            try {
                // encryption of password and Base64-Encoding
                MessageDigest md = MessageDigest.getInstance(passwordEncryption.getTitle());
                md.update(inNewPassword.getBytes(StandardCharsets.UTF_8));
                String encryptedPassword = new String(Base64.encodeBase64(md.digest()), StandardCharsets.UTF_8);

                // change attribute userPassword
                BasicAttribute userPassword = new BasicAttribute("userPassword",
                        "{" + passwordEncryption + "}" + encryptedPassword);

                // change attribute lanmgrPassword
                BasicAttribute lanmgrPassword = proceedPassword("sambaLMPassword", inNewPassword, null);

                // change attribute ntlmPassword
                BasicAttribute ntlmPassword = proceedPassword("sambaNTPassword", inNewPassword, digester);

                BasicAttribute sambaPwdLastSet = new BasicAttribute("sambaPwdLastSet",
                        String.valueOf(System.currentTimeMillis() / 1000L));

                ModificationItem[] mods = new ModificationItem[4];
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, userPassword);
                mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, lanmgrPassword);
                mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ntlmPassword);
                mods[3] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, sambaPwdLastSet);

                DirContext ctx = new InitialDirContext(env);
                ctx.modifyAttributes(buildUserDN(user), mods);

                // Close the context when we're done
                ctx.close();
                return true;
            } catch (NamingException e) {
                logger.debug("Benutzeranmeldung nicht korrekt oder Passwortänderung nicht möglich", e);
                return false;
            }
        }
        return false;
    }

    private BasicAttribute proceedPassword(String identifier, String newPassword, JDKMessageDigest.MD4 digester) {
        try {
            byte[] hash;
            if (Objects.isNull(digester)) {
                hash = LdapUser.lmHash(newPassword);
            } else {
                hash = digester.digest(newPassword.getBytes("UnicodeLittleUnmarked"));
            }
            return new BasicAttribute(identifier, LdapUser.toHexString(hash));
            // TODO: Don't catch super class exception, make sure that
            // the password isn't logged here
        } catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | RuntimeException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    // TODO test if this methods works
    private void loadCertificates(String path, String passwd, LdapServer ldapServer) {
        /* wenn die Zertifikate noch nicht im Keystore sind, jetzt einlesen */
        File myPfad = new File(path);
        if (!myPfad.exists()) {
            try (FileOutputStream ksos = (FileOutputStream) serviceManager.getFileService().write(myPfad.toURI());
                    // TODO: Rename parameters to something more meaningful,
                    // this is quite specific for the GDZ
                    FileInputStream cacertFile = new FileInputStream(ldapServer.getRootCertificate());
                    FileInputStream certFile2 = new FileInputStream(ldapServer.getPdcCertificate())) {

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cacert = (X509Certificate) cf.generateCertificate(cacertFile);
                X509Certificate servercert = (X509Certificate) cf.generateCertificate(certFile2);

                KeyStore ks = KeyStore.getInstance("jks");
                char[] password = passwd.toCharArray();

                // TODO: Let this method really load a keystore if configured
                // initialize the keystore, if file is available, load the
                // keystore
                ks.load(null);

                ks.setCertificateEntry("ROOTCERT", cacert);
                ks.setCertificateEntry("PDC", servercert);
                ks.store(ksos, password);
            } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException
                    | RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

        }
    }
}

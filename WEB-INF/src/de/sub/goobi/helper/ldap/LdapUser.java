package de.sub.goobi.helper.ldap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import de.sub.goobi.Beans.Benutzer;
import de.sub.goobi.Beans.LdapGruppe;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.helper.encryption.MD4;

/**
 * This class is used by the DirObj example.
 * It is a DirContext class that can be stored by service
 * providers like the LDAP system providers.
 */
public class LdapUser implements DirContext {
   private static final Logger myLogger = Logger.getLogger(LdapUser.class);
   String type;
   Attributes myAttrs;

   

   /**
    * Constructor of LdapUser
    */
   public LdapUser() {
      myAttrs = new BasicAttributes(true);
   }

   

   /**
    * configure LdapUser with Userdetails 
    * @param inUser
    * @param inPassword
    * @throws NamingException 
    * @throws NoSuchAlgorithmException 
    * @throws InterruptedException 
    * @throws IOException 
    */
   public void configure(Benutzer inUser, String inPassword, String inUidNumber) throws NamingException,
         NoSuchAlgorithmException, IOException, InterruptedException {
      type = inUser.getLogin();
      LdapGruppe lp = inUser.getLdapGruppe();
      if (lp.getObjectClasses() == null)
         throw new NamingException("no objectclass defined");

      /* ObjectClasses */
      Attribute oc = new BasicAttribute("objectclass");
      StringTokenizer tokenizer = new StringTokenizer(lp.getObjectClasses(), ",");
      while (tokenizer.hasMoreTokens())
         oc.add(tokenizer.nextToken());
      myAttrs.put(oc);

      myAttrs.put("uid", ReplaceVariables(lp.getUid(), inUser, inUidNumber));
      myAttrs.put("cn", ReplaceVariables(lp.getUid(), inUser, inUidNumber));
      myAttrs.put("displayName", ReplaceVariables(lp.getDisplayName(), inUser, inUidNumber));
      myAttrs.put("description", ReplaceVariables(lp.getDescription(), inUser, inUidNumber));
      myAttrs.put("gecos", ReplaceVariables(lp.getGecos(), inUser, inUidNumber));
      myAttrs.put("loginShell", ReplaceVariables(lp.getLoginShell(), inUser, inUidNumber));
      myAttrs.put("sn", ReplaceVariables(lp.getSn(), inUser, inUidNumber));
      myAttrs.put("homeDirectory", ReplaceVariables(lp.getHomeDirectory(), inUser, inUidNumber));

      myAttrs.put("sambaAcctFlags", ReplaceVariables(lp.getSambaAcctFlags(), inUser, inUidNumber));
      myAttrs.put("sambaLogonScript", ReplaceVariables(lp.getSambaLogonScript(), inUser, inUidNumber));
      myAttrs
            .put("sambaPrimaryGroupSID", ReplaceVariables(lp.getSambaPrimaryGroupSID(), inUser, inUidNumber));
      myAttrs.put("sambaSID", ReplaceVariables(lp.getSambaSID(), inUser, inUidNumber));

      myAttrs.put("sambaPwdMustChange", ReplaceVariables(lp.getSambaPwdMustChange(), inUser, inUidNumber));
      myAttrs.put("sambaPasswordHistory", ReplaceVariables(lp.getSambaPasswordHistory(), inUser, inUidNumber));
      myAttrs.put("sambaLogonHours", ReplaceVariables(lp.getSambaLogonHours(), inUser, inUidNumber));
      myAttrs.put("sambaKickoffTime", ReplaceVariables(lp.getSambaKickoffTime(), inUser, inUidNumber));

      myAttrs.put("uidNumber", inUidNumber);
      myAttrs.put("gidNumber", ReplaceVariables(lp.getGidNumber(), inUser, inUidNumber));

      /* --------------------------------
       * Samba passwords
       * --------------------------------*/
      /* LanMgr */
      try {
         //         System.out.println(toHexString(lmHash(inPassword)));
         myAttrs.put("sambaLMPassword", toHexString(lmHash(inPassword)));
      } catch (Exception e) {
         e.printStackTrace();
      }
      /* NTLM */
      try {
         byte hmm[] = MD4.mdfour(inPassword.getBytes("UnicodeLittleUnmarked"));
         //         System.out.println(toHexString(hmm));
         myAttrs.put("sambaNTPassword", toHexString(hmm));
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }

      /* --------------------------------
       * Encryption of password und Base64-Enconding
       * --------------------------------*/
   
      MessageDigest md = MessageDigest.getInstance(ConfigMain.getParameter("ldap_encryption", "SHA"));
      md.update(inPassword.getBytes());
      String digestBase64 = new String(Base64.encodeBase64(md.digest()));
      myAttrs.put("userPassword", "{" + ConfigMain.getParameter("ldap_encryption", "SHA") + "}" + digestBase64);
   }

   

   /**
    * Replace Variables with current user details
    * @param inString
    * @param inUser
    * @return String with replaced variables
    */
   private String ReplaceVariables(String inString, Benutzer inUser, String inUidNumber) {
      if (inString==null) return "";
   	String rueckgabe = inString.replaceAll("\\{login\\}", inUser.getLogin());
      rueckgabe = rueckgabe.replaceAll("\\{user full name\\}", inUser.getVorname() + " "
            + inUser.getNachname());
      rueckgabe = rueckgabe.replaceAll("\\{uidnumber\\*2\\+1000\\}", String.valueOf(Integer
            .parseInt(inUidNumber) * 2 + 1000));
      rueckgabe = rueckgabe.replaceAll("\\{uidnumber\\*2\\+1001\\}", String.valueOf(Integer
            .parseInt(inUidNumber) * 2 + 1001));
      myLogger.debug("Replace instring: " + inString + " - " + inUser + " - " + inUidNumber);
      myLogger.debug("Replace outstring: " + rueckgabe);
      return rueckgabe;
   }

   

   /**
    * Creates the LM Hash of the user's password.
    *
    * @param password The password.
    *
    * @return The LM Hash of the given password, used in the calculation
    * of the LM Response.
    */
   public static byte[] lmHash(String password) throws Exception {
      byte[] oemPassword = password.toUpperCase().getBytes("US-ASCII");
      int length = Math.min(oemPassword.length, 14);
      byte[] keyBytes = new byte[14];
      System.arraycopy(oemPassword, 0, keyBytes, 0, length);
      Key lowKey = createDESKey(keyBytes, 0);
      Key highKey = createDESKey(keyBytes, 7);
      byte[] magicConstant = "KGS!@#$%".getBytes("US-ASCII");
      Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
      des.init(Cipher.ENCRYPT_MODE, lowKey);
      byte[] lowHash = des.doFinal(magicConstant);
      des.init(Cipher.ENCRYPT_MODE, highKey);
      byte[] highHash = des.doFinal(magicConstant);
      byte[] lmHash = new byte[16];
      System.arraycopy(lowHash, 0, lmHash, 0, 8);
      System.arraycopy(highHash, 0, lmHash, 8, 8);
      return lmHash;
   }

   

   /**
    * Creates a DES encryption key from the given key material.
    *
    * @param bytes A byte array containing the DES key material.
    * @param offset The offset in the given byte array at which
    * the 7-byte key material starts.
    *
    * @return A DES encryption key created from the key material
    * starting at the specified offset in the given byte array.
    */
   private static Key createDESKey(byte[] bytes, int offset) {
      byte[] keyBytes = new byte[7];
      System.arraycopy(bytes, offset, keyBytes, 0, 7);
      byte[] material = new byte[8];
      material[0] = keyBytes[0];
      material[1] = (byte) (keyBytes[0] << 7 | (keyBytes[1] & 0xff) >>> 1);
      material[2] = (byte) (keyBytes[1] << 6 | (keyBytes[2] & 0xff) >>> 2);
      material[3] = (byte) (keyBytes[2] << 5 | (keyBytes[3] & 0xff) >>> 3);
      material[4] = (byte) (keyBytes[3] << 4 | (keyBytes[4] & 0xff) >>> 4);
      material[5] = (byte) (keyBytes[4] << 3 | (keyBytes[5] & 0xff) >>> 5);
      material[6] = (byte) (keyBytes[5] << 2 | (keyBytes[6] & 0xff) >>> 6);
      material[7] = (byte) (keyBytes[6] << 1);
      oddParity(material);
      return new SecretKeySpec(material, "DES");
   }

   

   /**
    * Applies odd parity to the given byte array.
    *
    * @param bytes The data whose parity bits are to be adjusted for
    * odd parity.
    */
   private static void oddParity(byte[] bytes) {
      for (int i = 0; i < bytes.length; i++) {
         byte b = bytes[i];
         boolean needsParity = (((b >>> 7) ^ (b >>> 6) ^ (b >>> 5) ^ (b >>> 4) ^ (b >>> 3) ^ (b >>> 2) ^ (b >>> 1)) & 0x01) == 0;
         if (needsParity) {
            bytes[i] |= (byte) 0x01;
         } else {
            bytes[i] &= (byte) 0xfe;
         }
      }
   }

   

   public static String toHexString(byte bytes[]) {
      StringBuffer retString = new StringBuffer();
      for (int i = 0; i < bytes.length; ++i) {
         retString.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1));
      }
      return retString.toString().toUpperCase();
   }

   

   public Attributes getAttributes(String name) throws NamingException {
      if (!name.equals("")) {
         throw new NameNotFoundException();
      }
      return (Attributes) myAttrs.clone();
   }

   public Attributes getAttributes(Name name) throws NamingException {
      return getAttributes(name.toString());
   }

   public Attributes getAttributes(String name, String[] ids) throws NamingException {
      if (!name.equals("")) {
         throw new NameNotFoundException();
      }

      Attributes answer = new BasicAttributes(true);
      Attribute target;
      for (int i = 0; i < ids.length; i++) {
         target = myAttrs.get(ids[i]);
         if (target != null) {
            answer.put(target);
         }
      }
      return answer;
   }

   public Attributes getAttributes(Name name, String[] ids) throws NamingException {
      return getAttributes(name.toString(), ids);
   }

   public String toString() {
      return type;
   }

   // not used for this example

   public Object lookup(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Object lookup(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void bind(Name name, Object obj) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void bind(String name, Object obj) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rebind(Name name, Object obj) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rebind(String name, Object obj) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void unbind(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void unbind(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rename(Name oldName, Name newName) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rename(String oldName, String newName) throws NamingException {
      throw new OperationNotSupportedException();
   }

   
   public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void destroySubcontext(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void destroySubcontext(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Context createSubcontext(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Context createSubcontext(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Object lookupLink(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Object lookupLink(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NameParser getNameParser(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NameParser getNameParser(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public String composeName(String name, String prefix) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Name composeName(Name name, Name prefix) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Object addToEnvironment(String propName, Object propVal) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Object removeFromEnvironment(String propName) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public Hashtable<?,?> getEnvironment() throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void close() throws NamingException {
      throw new OperationNotSupportedException();
   }

   // -- DirContext
   public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void bind(String name, Object obj, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext getSchema(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext getSchema(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext getSchemaClassDefinition(Name name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public DirContext getSchemaClassDefinition(String name) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
         throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn)
         throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
         throws NamingException {
      throw new OperationNotSupportedException();
   }

   public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
         throws NamingException {
      throw new OperationNotSupportedException();
   }

   public String getNameInNamespace() throws NamingException {
      throw new OperationNotSupportedException();
   }
}

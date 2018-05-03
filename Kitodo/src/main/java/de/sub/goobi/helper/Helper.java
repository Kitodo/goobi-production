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

package de.sub.goobi.helper;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.forms.LoginForm;
import de.sub.goobi.forms.SpracheForm;
import de.sub.goobi.helper.enums.ReportLevel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.mq.WebServiceResult;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.helper.HibernateHelper;
import org.kitodo.data.database.helper.Util;

/**
 * Extends Helper from Kitodo Data Management module.
 */
public class Helper extends HibernateHelper implements Observer {

    /**
     * Always treat de-serialization as a full-blown constructor, by validating the
     * final state of the de-serialized object.
     */
    private void readObject(ObjectInputStream aInputStream) {

    }

    /**
     * This is the default implementation of writeObject. Customise if necessary.
     */
    private void writeObject(ObjectOutputStream aOutputStream) {

    }

    public static Map<String, String> activeMQReporting = null;
    private static final Logger logger = LogManager.getLogger(Helper.class);
    private static final long serialVersionUID = -7449236652821237059L;
    private static Map<Locale, ResourceBundle> commonMessages = null;
    private static Map<Locale, ResourceBundle> localMessages = null;
    private static String compoundMessage;
    private static User currentUser = null;

    /**
     * Determine a specific parameter of the request.
     *
     * @return parameter als String
     */
    @SuppressWarnings("rawtypes")
    public static String getRequestParameter(String parameter) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map requestParams = context.getExternalContext().getRequestParameterMap();
        return (String) requestParams.get(parameter);
    }

    public static void setFehlerMeldung(String message) {
        setMeldung(null, message, "", false);
    }

    public static void setFehlerMeldung(String message, String description) {
        setMeldung(null, message, description != null ? description : "", false);
    }

    public static void setFehlerMeldung(String control, String message, String description) {
        setMeldung(control, message, description != null ? description : "", false);
    }

    public static void setFehlerMeldung(Exception e) {
        setFehlerMeldung("Error (" + e.getClass().getName() + "): ", getExceptionMessage(e));
    }

    public static void setFehlerMeldung(String control, String message, Exception e) {
        setFehlerMeldung(control, message + " (" + e.getClass().getSimpleName() + "): ", getExceptionMessage(e));
    }

    /**
     * Set error message to message tag with given name 'title'. Substitute all
     * placeholders in message tag with elements of given array 'parameters'.
     *
     * @param title
     *            name of the message tag set as error message
     * @param parameters
     *            list of parameters used for string substitution in message tag
     */
    public static void setErrorMessage(String title, final Object[] parameters) {
        if (Objects.nonNull(parameters) && parameters.length > 0) {
            setFehlerMeldung(MessageFormat.format(getTranslation(title), parameters));
        } else {
            setFehlerMeldung(getTranslation(title));
        }
    }

    /**
     * Set error message to message tag with given name 'title'.
     *
     * <p>
     * This method also accepts logger and exception instances to automatically log
     * the exceptions message or stackTrace values to the given logger.
     * </p>
     *
     * @param title
     *            name of the message tag set as error message
     * @param logger
     *            Logger instance for error logging
     * @param exception
     *            Exception instance for error logging
     */
    public static void setErrorMessage(String title, Logger logger, Exception exception) {
        logger.error(title, exception);
        if (Objects.isNull(exception.getMessage()) || exception.getMessage().equals(title)) {
            setFehlerMeldung(title);
        } else {
            setFehlerMeldung(title, exception.getMessage());
        }
    }

    /**
     * Set error message to message tag with given name 'title'. Substitute all
     * placeholders in message tag with elements of given array 'parameters'.
     *
     * <p>
     * This method also accepts logger and exception instances to automatically log
     * the exceptions message or stackTrace values to the given logger.
     * </p>
     *
     * @param title
     *            name of the message tag set as error message
     * @param parameters
     *            list of parameters used for string substitution in message tag
     * @param logger
     *            Logger instance for error logging
     * @param exception
     *            Exception instance for error logging
     */
    public static void setErrorMessage(String title, final Object[] parameters, Logger logger, Exception exception) {
        logger.error(title, exception);
        setErrorMessage(title, parameters);
    }

    /**
     * Set error message to message tag with given name 'title'.
     *
     * <p>
     * This method also accepts a description text and logger and exception
     * instances to automatically log the exceptions message or stackTrace values to
     * the given logger.
     * </p>
     *
     * @param title
     *            name of the message tag set as error message
     * @param description
     *            description text that will be displayed in the faces message
     * @param logger
     *            Logger instance for error logging
     * @param exception
     *            Exception instance for error logging
     */
    public static void setErrorMessage(String title, String description, Logger logger, Exception exception) {
        logger.error(title, exception);
        setFehlerMeldung(title, description);
    }

    private static String getExceptionMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            message = sw.toString();
        }
        return message;
    }

    public static void setMeldung(String message) {
        setMeldung(null, message, "", true);
    }

    public static void setMeldung(String message, String description) {
        setMeldung(null, message, description, true);
    }

    public static void setMeldung(String control, String message, String description) {
        setMeldung(control, message, description, true);
    }

    /**
     * Dem aktuellen Formular eine Fehlermeldung für ein bestimmtes Control
     * übergeben.
     */
    private static void setMeldung(String control, String message, String description, boolean onlyInfo) {
        FacesContext context = FacesContext.getCurrentInstance();

        // Never forget: Strings are immutable
        message = message.replaceAll("<", "&lt;");
        message = message.replaceAll(">", "&gt;");
        description = description.replaceAll("<", "&lt;");
        description = description.replaceAll(">", "&gt;");

        Locale language = Locale.ENGLISH;
        SpracheForm sf = (SpracheForm) Helper.getManagedBeanValue("#{SpracheForm}");
        if (sf != null) {
            language = sf.getLocale();
        }

        String msg;
        try {
            msg = getString(language, message);
        } catch (RuntimeException e) {
            msg = message;
        }
        String descript;
        try {
            descript = getString(language, description);
        } catch (RuntimeException e) {
            descript = description;
        }

        compoundMessage = msg.replaceFirst(":\\s*$", "") + ": " + descript;
        if (activeMQReporting != null) {
            new WebServiceResult(activeMQReporting.get("queueName"), activeMQReporting.get("id"),
                    onlyInfo ? ReportLevel.INFO : ReportLevel.ERROR, compoundMessage).send();
        }
        if (context != null) {
            context.addMessage(control,
                new FacesMessage(onlyInfo ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_ERROR, msg, descript));
        } else {
            // wenn kein Kontext da ist, dann die Meldungen in Log
            logger.log(onlyInfo ? Level.INFO : Level.ERROR, compoundMessage);
        }
    }

    /**
     * Returns a Map holding all translations that are configured in the front end
     * of a given resource key.
     *
     * @param key
     *            resource key to get translations for
     * @return a map with all language id strings and the corresponding resource
     */
    public static HashMap<String, String> getAllStrings(String key) {
        HashMap<String, String> result = new HashMap<>(Util.hashCapacityFor(commonMessages.entrySet()));
        @SuppressWarnings("unchecked")
        Iterator<Locale> languages = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        while (languages.hasNext()) {
            Locale language = languages.next();
            result.put(language.getLanguage(), getString(language, key));
        }
        return result;
    }

    /**
     * Get String.
     *
     * @param language
     *            Locale object
     * @param key
     *            String
     * @return String
     */
    public static String getString(Locale language, String key) {
        if (commonMessages == null || commonMessages.size() <= 1) {
            loadMsgs();
        }

        if (localMessages.containsKey(language)) {
            ResourceBundle languageLocal = localMessages.get(language);
            if (languageLocal.containsKey(key)) {
                return languageLocal.getString(key);
            }
            String lowKey = key.toLowerCase();
            if (languageLocal.containsKey(lowKey)) {
                return languageLocal.getString(lowKey);
            }
        }
        try {
            return commonMessages.get(language).getString(key);
        } catch (RuntimeException irrelevant) {
            return key;
        }
    }

    /**
     * Get date as formatted String.
     *
     * @param date
     *            Date object
     * @return String
     */
    public static String getDateAsFormattedString(Date date) {
        if (date == null) {
            return "-";
        } else {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(date);
        }
    }

    /**
     * The procedure removeManagedBean() removes a managed bean from the faces
     * context by name. If nothing such is available, nothing happens.
     *
     * @param name
     *            managed bean to remove
     */
    public static void removeManagedBean(String name) {
        try {
            @SuppressWarnings("rawtypes")
            Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
            if (sessionMap.containsKey(name)) {
                sessionMap.remove(name);
            }
        } catch (RuntimeException nothingToDo) {
            logger.error(nothingToDo);
        }
    }

    private static void loadMsgs() {
        commonMessages = new HashMap<>();
        localMessages = new HashMap<>();
        if (FacesContext.getCurrentInstance() != null) {
            @SuppressWarnings("unchecked")
            Iterator<Locale> polyglot = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
            while (polyglot.hasNext()) {
                Locale language = polyglot.next();
                commonMessages.put(language, ResourceBundle.getBundle("messages.messages", language));
                File file = new File(ConfigCore.getParameter("localMessages", "/usr/local/kitodo/messages/"));
                if (file.exists()) {
                    // Load local message bundle from file system only if file
                    // exists;
                    // if value not exists in bundle, use default bundle from
                    // classpath

                    try {
                        final URL resourceURL = file.toURI().toURL();
                        URLClassLoader urlLoader = AccessController
                                .doPrivileged(new PrivilegedAction<URLClassLoader>() {
                                    @Override
                                    public URLClassLoader run() {
                                        return new URLClassLoader(new URL[] {resourceURL });
                                    }
                                });
                        ResourceBundle localBundle = ResourceBundle.getBundle("messages", language, urlLoader);
                        if (localBundle != null) {
                            localMessages.put(language, localBundle);
                        }

                    } catch (RuntimeException | MalformedURLException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            Locale defaultLocale = new Locale("EN");
            commonMessages.put(defaultLocale, ResourceBundle.getBundle("messages.messages", defaultLocale));
        }
    }

    /**
     * Get translation.
     *
     * @param title
     *            String
     * @return translated String
     */
    public static String getTranslation(String title) {
        // running instance of ResourceBundle doesn't respond on user language
        // changes, workaround by instantiating it every time

        if (FacesContext.getCurrentInstance() != null) {
            Locale desiredLanguage = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            if (desiredLanguage != null) {
                return getString(desiredLanguage, title);
            }
        }
        return getString(Locale.ENGLISH, title);
    }

    public static String getTranslation(String inParameter, String inDefaultIfNull) {
        String result = getTranslation(inParameter);
        return result != null && !result.equals(inParameter) ? result : inDefaultIfNull;
    }

    /**
     * Get translation.
     *
     * @param title
     *            String
     * @param parameterList
     *            list of Strings
     * @return translated String
     */
    public static String getTranslation(String title, List<String> parameterList) {
        String value = getTranslation(title);

        if (value != null && parameterList != null && parameterList.size() > 0) {
            int parameterCount = 0;
            for (String parameter : parameterList) {
                value = value.replace("{" + parameterCount + "}", parameter);
                parameterCount++;
            }
        }
        return value;
    }

    /**
     * for easy access of the implemented Interface Observer.
     *
     * @return Observer -> can be added to an Observable
     */
    public Observer createObserver() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof String)) {
            Helper.setFehlerMeldung("User notification failed by object: '" + arg.toString()
                    + "' which isn't an expected String Object. This error is caused by an implementation of "
                    + "the Observer Interface in Helper");
        } else {
            Helper.setFehlerMeldung((String) arg);
        }
    }

    /**
     * Get current logged in user.
     *
     * @return current logged in user
     */
    public static User getCurrentUser() {
        if (Objects.isNull(currentUser)) {
            LoginForm login = (LoginForm) Helper.getManagedBeanValue("#{LoginForm}");
            currentUser = login != null ? login.getMyBenutzer() : null;
        }
        return currentUser;
    }

    // TODO: find way to test without this method - faces
    /**
     * Set current logged in user. Used for test purpose.
     *
     * @param user
     *            current logged in user as User
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Get metadata language for currently logged user.
     *
     * @return metadata language as String
     */
    public static String getMetadataLanguageForCurrentUser() {
        return (String) Helper.getManagedBeanValue("#{LoginForm.myBenutzer.metadataLanguage}");
    }

    public static final FilenameFilter imageNameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            boolean fileOk = false;
            String prefix = ConfigCore.getParameter("ImagePrefix", "\\d{8}");

            if (name.matches(prefix + "\\.[Tt][Ii][Ff][Ff]?")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[jJ][pP][eE]?[gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[jJ][pP][2]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[pP][nN][gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[gG][iI][fF]")) {
                fileOk = true;
            }
            return fileOk;
        }
    };

    public static final FilenameFilter dataFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            boolean fileOk = false;
            String prefix = ConfigCore.getParameter("ImagePrefix", "\\d{8}");
            if (name.matches(prefix + "\\.[Tt][Ii][Ff][Ff]?")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[jJ][pP][eE]?[gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[jJ][pP][2]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[pP][nN][gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[gG][iI][fF]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[pP][dD][fF]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[aA][vV][iI]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[mM][pP][gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[mM][pP]4")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[mM][pP]3")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[wW][aA][vV]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[wW][mM][vV]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[fF][lL][vV]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.[oO][gG][gG]")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.docx")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.doc")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.xls")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.xlsx")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.pptx")) {
                fileOk = true;
            } else if (name.matches(prefix + "\\.ppt")) {
                fileOk = true;
            }
            return fileOk;
        }
    };

    /**
     * The function getLastMessage() returns the last message processed to be shown
     * to the user. This is a last resort only to show the user why perhaps
     * something didn’t work if no error message is available otherwise.
     *
     * @return the most recent message created to be shown to the user
     */
    public static String getLastMessage() {
        return compoundMessage;
    }

}

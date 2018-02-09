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

package de.sub.goobi.forms;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.metadaten.copier.CopierData;
import de.sub.goobi.metadaten.copier.DataCopier;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.production.cli.helper.CopyProcess;
import org.goobi.production.cli.helper.WikiFieldHelper;
import org.goobi.production.constants.Parameters;
import org.goobi.production.plugin.CataloguePlugin.CataloguePlugin;
import org.goobi.production.plugin.CataloguePlugin.Hit;
import org.goobi.production.plugin.CataloguePlugin.QueryBuilder;
import org.goobi.production.plugin.PluginLoader;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.services.ServiceManager;

import ugh.dl.Fileformat;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

@Named("ProzesskopieForm")
@SessionScoped
public class ProzesskopieForm implements Serializable {
    private static final Logger logger = LogManager.getLogger(ProzesskopieForm.class);
    private static final long serialVersionUID = -4512865679353743L;
    private transient ServiceManager serviceManager = new ServiceManager();

    /**
     * The class SelectableHit represents a hit on the hit list that shows up if
     * a catalogue search yielded more than one result. We need an inner class
     * for this because Faces is striclty object oriented and the always
     * argument-less actions can only be executed relatively to the list entry
     * in question this way if they are concerning elements that are rendered by
     * iterating along a list.
     *
     * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
     */
    public class SelectableHit {
        /**
         * The field hit holds the hit to be rendered as a list entry.
         */
        private final Hit hit;

        /**
         * The field error holds an error message to be rendered as a list entry
         * in case that retrieving the hit failed within the plug-in used for
         * catalogue access.
         */
        private final String error;

        /**
         * Selectable hit constructor. Creates a new SelectableHit object with a
         * hit to show.
         *
         * @param hit
         *            Hit to show
         */
        public SelectableHit(Hit hit) {
            this.hit = hit;
            error = null;
        }

        /**
         * Selectable hit constructor. Creates a new SelectableHit object with
         * an error message to show.
         *
         * @param error
         *            error message
         */
        public SelectableHit(String error) {
            hit = null;
            this.error = error;
        }

        /**
         * The function getBibliographicCitation() returns a summary of this hit
         * in bibliographic citation style as HTML as read-only property
         * “bibliographicCitation”.
         *
         * @return a summary of this hit in bibliographic citation style as HTML
         */
        public String getBibliographicCitation() {
            return hit.getBibliographicCitation();
        }

        /**
         * The function getErrorMessage() returns an error if that had occurred
         * when trying to retrieve that hit from the catalogue as read-only
         * property “errorMessage”.
         *
         * @return an error message to be rendered as a list entry
         */
        public String getErrorMessage() {
            return error;
        }

        /**
         * The function isError() returns whether an error occurred when trying
         * to retrieve that hit from the catalogue as read-only property
         * “error”.
         *
         * @return whether an error occurred when retrieving that hit
         */
        public boolean isError() {
            return hit == null;
        }

        /**
         * The function selectClick() is called if the user clicks on a
         * catalogue hit summary in order to import it into Production.
         *
         * @return always "", indicating to Faces to stay on that page
         */
        public String selectClick() {
            try {
                importHit(hit);
            } catch (Exception e) {
                Helper.setFehlerMeldung("Error on reading opac ", e);
            } finally {
                hitlistPage = -1;
            }
            return null;
        }
    }

    /**
     * The constant DEFAULT_HITLIST_PAGE_SIZE holds the fallback number of hits
     * to show per page on the hit list if the user conducted a catalogue search
     * that yielded more than one result, if none is configured in the
     * Production configuration file.
     */
    private static final int DEFAULT_HITLIST_PAGE_SIZE = 10;

    static final String NAVI_FIRST_PAGE = "/pages/NewProcess/Page1";

    private String addToWikiField = "";
    private List<AdditionalField> additionalFields;
    private String atstsl = "";
    private List<String> digitalCollections;
    private String docType;
    private Integer guessedImages = 0;

    /**
     * The field hitlist holds some reference to the hitlist retrieved from a
     * library catalogue. The internals of this object are subject to the plugin
     * implementation and are not to be accessed directly.
     */
    private Object hitlist;

    /**
     * The field hitlistPage holds the zero-based index of the page of the
     * hitlist currently showing. A negative value means that the hitlist is
     * hidden, otherwise it is showing the respective page.
     */
    private long hitlistPage = -1;
    /**
     * The field hits holds the number of hits in the hitlist last retrieved
     * from a library catalogue.
     */
    private long hits;

    /**
     * The field importCatalogue holds the catalogue plugin used to access the
     * library catalogue.
     */
    private CataloguePlugin importCatalogue;

    private Fileformat rdf;
    private String opacSuchfeld = "12";
    private String opacSuchbegriff;
    private String opacKatalog;
    private List<String> possibleDigitalCollection;
    private Process prozessVorlage = new Process();
    private Process prozessKopie = new Process();
    private boolean useOpac;
    private boolean useTemplates;
    private Integer auswahl;
    private HashMap<String, Boolean> standardFields;
    private String tifHeaderImageDescription = "";
    private String tifHeaderDocumentName = "";
    private CopyProcess copyProcess = new CopyProcess();

    /**
     * Prepare.
     *
     * @return empty String
     */
    public String prepare(int id) {
        atstsl = "";
        try {
            this.prozessVorlage = serviceManager.getProcessService().getById(id);
        } catch (DAOException e) {
            logger.error(e.getMessage());
            Helper.setFehlerMeldung("Process " + id + " not found.");
            return null;
        }

        copyProcess.setProzessVorlage(this.prozessVorlage);
        boolean result = copyProcess.prepare(null);
        setProzessKopie(copyProcess.getProzessKopie());

        if (result) {
            return NAVI_FIRST_PAGE;
        } else {
            return null;
        }
    }

    /**
     * Get Process templates.
     *
     * @return list of SelectItem objects
     */
    public List<SelectItem> getProzessTemplates() {
        List<SelectItem> processTemplates = new ArrayList<>();

        // Einschränkung auf bestimmte Projekte, wenn kein Admin
        // TODO: remove it after method getMaximaleBerechtigung() is gone
        LoginForm loginForm = (LoginForm) Helper.getManagedBeanValue("#{LoginForm}");
        List<Process> processes = serviceManager.getProcessService().getProcessTemplates();
        if (loginForm != null) {
            User currentUser = loginForm.getMyBenutzer();
            try {
                currentUser = serviceManager.getUserService().getById(loginForm.getMyBenutzer().getId());
            } catch (DAOException e) {
                logger.error(e);
            }
            if (currentUser != null) {
                /*
                 * wenn die maximale Berechtigung nicht Admin ist, dann nur
                 * bestimmte
                 */
                if (loginForm.getMaximaleBerechtigung() > 1) {
                    ArrayList<Integer> projectIds = new ArrayList<>();
                    for (Project project : currentUser.getProjects()) {
                        projectIds.add(project.getId());
                    }
                    if (projectIds.size() > 0) {
                        processes = serviceManager.getProcessService().getProcessTemplatesForUser(projectIds);
                    }
                }
            }
        }

        for (Process process : processes) {
            processTemplates.add(new SelectItem(process.getId(), process.getTitle(), null));
        }
        return processTemplates;
    }

    /**
     * The function evaluateOpac() is executed if a user clicks the command link
     * to start a catalogue search. It performs the search and loads the hit if
     * it is unique. Otherwise, it will cause a hit list to show up for the user
     * to select a hit.
     *
     * @return always "", telling JSF to stay on that page
     */
    public String evaluateOpac() {
        long timeout = CataloguePlugin.getTimeout();
        try {
            clearValues();
            //readProjectConfigs();
            if (!pluginAvailableFor(opacKatalog)) {
                return null;
            }

            String query = QueryBuilder.restrictToField(opacSuchfeld, opacSuchbegriff);
            query = QueryBuilder.appendAll(query, ConfigOpac.getRestrictionsForCatalogue(opacKatalog));

            hitlist = importCatalogue.find(query, timeout);
            hits = importCatalogue.getNumberOfHits(hitlist, timeout);

            switch ((int) Math.min(hits, Integer.MAX_VALUE)) {
                case 0:
                    Helper.setFehlerMeldung("No hit found", "");
                    break;
                case 1:
                    importHit(importCatalogue.getHit(hitlist, 0, timeout));
                    break;
                default:
                    hitlistPage = 0; // show first page of hitlist
                    break;
            }
            return null;
        } catch (Exception e) {
            Helper.setFehlerMeldung("Error on reading opac ", e);
            return null;
        }
    }

    /**
     * The function pluginAvailableFor(catalogue) verifies that a plugin
     * suitable for accessing the library catalogue identified by the given
     * String is available in the global variable importCatalogue. If
     * importCatalogue is empty or the current plugin doesn’t support the given
     * catalogue, the function will try to load a suitable plugin. Upon success
     * the preferences and the catalogue to use will be configured in the
     * plugin, otherwise an error message will be set to be shown.
     *
     * @param catalogue
     *            identifier string for the catalogue that the plugin shall
     *            support
     * @return whether a plugin is available in the global varibale
     *         importCatalogue
     */
    private boolean pluginAvailableFor(String catalogue) {
        if (importCatalogue == null || !importCatalogue.supportsCatalogue(catalogue)) {
            importCatalogue = PluginLoader.getCataloguePluginForCatalogue(catalogue);
        }
        if (importCatalogue == null) {
            Helper.setFehlerMeldung("NoCataloguePluginForCatalogue", catalogue);
            return false;
        } else {
            importCatalogue
                    .setPreferences(serviceManager.getRulesetService().getPreferences(prozessKopie.getRuleset()));
            importCatalogue.useCatalogue(catalogue);
            return true;
        }
    }

    /**
     * alle Konfigurationseigenschaften und Felder zurücksetzen.
     */
    private void clearValues() {
        if (this.opacKatalog == null) {
            this.opacKatalog = "";
        }
        this.standardFields = new HashMap<>();
        this.standardFields.put("collections", true);
        this.standardFields.put("doctype", true);
        this.standardFields.put("regelsatz", true);
        this.standardFields.put("images", true);
        this.additionalFields = new ArrayList<>();
        this.tifHeaderDocumentName = "";
        this.tifHeaderImageDescription = "";
    }

    /**
     * The method importHit() loads a hit into the display.
     *
     * @param hit
     *            Hit to load
     */
    protected void importHit(Hit hit) throws PreferencesException {
        rdf = hit.getFileformat();
        docType = hit.getDocType();
        copyProcess.fillFieldsFromMetadataFile();
        applyCopyingRules(new CopierData(rdf, prozessVorlage));
        atstsl = createAtstsl(hit.getTitle(), hit.getAuthors());
    }

    /**
     * Creates a DataCopier with the given configuration, lets it process the
     * given data and wraps any errors to display in the front end.
     *
     * @param data
     *            data to process
     */
    private void applyCopyingRules(CopierData data) {
        String rules = ConfigCore.getParameter("copyData.onCatalogueQuery");
        if (rules != null && !rules.equals("- keine Konfiguration gefunden -")) {
            try {
                new DataCopier(rules).process(data);
            } catch (ConfigurationException e) {
                Helper.setFehlerMeldung("dataCopier.syntaxError", e.getMessage());
            } catch (RuntimeException exception) {
                if (RuntimeException.class.equals(exception.getClass())) {
                    Helper.setFehlerMeldung("dataCopier.runtimeException", exception.getMessage());
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * Auswahl des Prozesses auswerten.
     */
    public String templateAuswahlAuswerten() throws DAOException {
        return copyProcess.evaluateSelectedTemplate();
    }

    /**
     * Validierung der Eingaben.
     *
     * @return sind Fehler bei den Eingaben vorhanden?
     */
    boolean isContentValid() {
        copyProcess.setProzessKopie(this.prozessKopie);
        return copyProcess.isContentValid(true);
    }

    boolean isContentValid(boolean criticiseEmptyTitle) {
        copyProcess.setProzessKopie(this.prozessKopie);
        return copyProcess.isContentValid(criticiseEmptyTitle);
    }

    public String goToPageOne() {
        return NAVI_FIRST_PAGE;
    }

    //TODO: why do we need page two?
    /**
     * Go to page 2.
     *
     * @return page
     */
    public String goToPageTwo() {
        if (!isContentValid()) {
            return NAVI_FIRST_PAGE;
        } else {
            return "/pages/NewProcess/Page2";
        }
    }

    /**
     * Anlegen des Prozesses und save der Metadaten.
     */
    public String createNewProcess()
            throws ReadException, IOException, PreferencesException, WriteException {

        copyProcess.setProzessKopie(this.prozessKopie);
        boolean result = copyProcess.createNewProcess();
        setProzessKopie(copyProcess.getProzessKopie());
        if (result) {
            return "/pages/NewProcess/Page3";
        } else {
            return NAVI_FIRST_PAGE;
        }
    }

    /**
     * Create new file format.
     */
    public void createNewFileformat() {
        copyProcess.createNewFileformat();
    }

    public String getDocType() {
        return this.docType;
    }

    /**
     * Set document type.
     *
     * @param docType
     *            String
     */
    public void setDocType(String docType) {
        copyProcess.setDocType(docType);
        this.docType = copyProcess.getDocType();
    }

    public Process getProzessVorlage() {
        return this.prozessVorlage;
    }

    /**
     * The function getProzessVorlageTitel() returns some kind of identifier for
     * this ProzesskopieForm. The title of the process template that a process
     * will be created from can be considered with some reason to be some good
     * identifier for the ProzesskopieForm, too.
     *
     * @return a human-readable identifier for this object
     */
    public String getProzessVorlageTitel() {
        return prozessVorlage != null ? prozessVorlage.getTitle() : null;
    }

    public void setProzessVorlage(Process prozessVorlage) {
        this.prozessVorlage = prozessVorlage;
    }

    public Integer getAuswahl() {
        return this.auswahl;
    }

    public void setAuswahl(Integer auswahl) {
        this.auswahl = auswahl;
    }

    public List<AdditionalField> getAdditionalFields() {
        return this.additionalFields;
    }

    /**
     * The method setAdditionalField() sets the value of an AdditionalField held
     * by a ProzesskopieForm object.
     *
     * @param key
     *            the title of the AdditionalField whose value shall be modified
     * @param value
     *            the new value for the AdditionalField
     * @param strict
     *            throw a RuntimeException if the field is unknown
     * @throws RuntimeException
     *             in case that no field with a matching title was found in the
     *             ProzesskopieForm object
     */
    public void setAdditionalField(String key, String value, boolean strict) throws RuntimeException {
        boolean unknownField = true;
        for (AdditionalField field : additionalFields) {
            if (key.equals(field.getTitle())) {
                field.setValue(value);
                unknownField = false;
            }
        }
        if (unknownField && strict) {
            throw new RuntimeException("Couldn’t set “" + key + "” to “" + value + "”: No such field in record.");
        }
    }

    public void setAdditionalFields(List<AdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
    }

    /*
     * this is needed for GUI, render multiple select only if this is false if
     * this is true use the only choice
     *
     * @author Wulf
     */
    public boolean isSingleChoiceCollection() {
        return (getPossibleDigitalCollections() != null && getPossibleDigitalCollections().size() == 1);

    }

    /**
     * This is needed for GUI, render multiple select only if this is false if
     * isSingleChoiceCollection is true use this choice.
     *
     * @author Wulf
     */
    public String getDigitalCollectionIfSingleChoice() {
        List<String> pdc = getPossibleDigitalCollections();
        if (pdc.size() == 1) {
            return pdc.get(0);
        } else {
            return null;
        }
    }

    public List<String> getPossibleDigitalCollections() {
        return this.possibleDigitalCollection;
    }

    /**
     * Get all OPAC catalogues.
     *
     * @return list of catalogues
     */
    public List<String> getAllOpacCatalogues() {
        try {
            return ConfigOpac.getAllCatalogueTitles();
        } catch (Throwable t) {
            logger.error("Error while reading von opac-config", t);
            Helper.setFehlerMeldung("Error while reading von opac-config", t.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all document types.
     *
     * @return list of ConfigOpacDoctype objects
     */
    public List<ConfigOpacDoctype> getAllDoctypes() {
        try {
            return ConfigOpac.getAllDoctypes();
        } catch (Throwable t) {
            logger.error("Error while reading von opac-config", t);
            Helper.setFehlerMeldung("Error while reading von opac-config", t.getMessage());
            return new ArrayList<>();
        }
    }

    /*
     * changed, so that on first request list gets set if there is only one
     * choice
     */
    public List<String> getDigitalCollections() {
        return this.digitalCollections;
    }

    public void setDigitalCollections(List<String> digitalCollections) {
        this.digitalCollections = digitalCollections;
    }

    public HashMap<String, Boolean> getStandardFields() {
        return this.standardFields;
    }

    public boolean isUseOpac() {
        return this.useOpac;
    }

    public boolean isUseTemplates() {
        return this.useTemplates;
    }

    public String getTifHeaderDocumentName() {
        return this.tifHeaderDocumentName;
    }

    public void setTifHeaderDocumentName(String tifHeaderDocumentName) {
        this.tifHeaderDocumentName = tifHeaderDocumentName;
    }

    public String getTifHeaderImageDescription() {
        return this.tifHeaderImageDescription;
    }

    public void setTifHeaderImageDescription(String tifHeaderImageDescription) {
        this.tifHeaderImageDescription = tifHeaderImageDescription;
    }

    public Process getProzessKopie() {
        return this.prozessKopie;
    }

    public void setProzessKopie(Process prozessKopie) {
        this.prozessKopie = prozessKopie;
    }

    public String getOpacSuchfeld() {
        return this.opacSuchfeld;
    }

    public void setOpacSuchfeld(String opacSuchfeld) {
        this.opacSuchfeld = opacSuchfeld;
    }

    public String getOpacKatalog() {
        return this.opacKatalog;
    }

    public void setOpacKatalog(String opacKatalog) {
        this.opacKatalog = opacKatalog;
    }

    public String getOpacSuchbegriff() {
        return this.opacSuchbegriff;
    }

    public void setOpacSuchbegriff(String opacSuchbegriff) {
        this.opacSuchbegriff = opacSuchbegriff;
    }


    /**
     * Prozesstitel und andere Details generieren.
     */
    public void calculateProcessTitle() {
        try {
            generateTitle(null);
        } catch (IOException e) {
            Helper.setFehlerMeldung("IOException", e.getMessage());
        }
    }

    /**
     * Generate title.
     *
     * @param genericFields
     *            Map of Strings
     * @return String
     */
    public String generateTitle(Map<String, String> genericFields) throws IOException {
        String currentAuthors = "";
        String currentTitle = "";
        int counter = 0;
        for (AdditionalField field : this.additionalFields) {
            if (field.getAutogenerated() && field.getValue().isEmpty()) {
                field.setValue(String.valueOf(System.currentTimeMillis() + counter));
                counter++;
            }
            if (field.getMetadata() != null && field.getMetadata().equals("TitleDocMain")
                    && currentTitle.length() == 0) {
                currentTitle = field.getValue();
            } else if (field.getMetadata() != null && field.getMetadata().equals("ListOfCreators")
                    && currentAuthors.length() == 0) {
                currentAuthors = field.getValue();
            }

        }
        StringBuilder newTitle = new StringBuilder();
        String titeldefinition = "";
        ConfigProjects cp = new ConfigProjects(this.prozessVorlage.getProject().getTitle());

        int count = cp.getParamList("createNewProcess.itemlist.processtitle").size();
        for (int i = 0; i < count; i++) {
            String titel = cp.getParamString("createNewProcess.itemlist.processtitle(" + i + ")");
            String isdoctype = cp.getParamString("createNewProcess.itemlist.processtitle(" + i + ")[@isdoctype]");
            String isnotdoctype = cp.getParamString("createNewProcess.itemlist.processtitle(" + i + ")[@isnotdoctype]");

            if (titel == null) {
                titel = "";
            }
            if (isdoctype == null) {
                isdoctype = "";
            }
            if (isnotdoctype == null) {
                isnotdoctype = "";
            }

            /* wenn nix angegeben wurde, dann anzeigen */
            if (isdoctype.equals("") && isnotdoctype.equals("")) {
                titeldefinition = titel;
                break;
            }

            /* wenn beides angegeben wurde */
            if (!isdoctype.equals("") && !isnotdoctype.equals("")
                    && StringUtils.containsIgnoreCase(isdoctype, this.docType)
                    && !StringUtils.containsIgnoreCase(isnotdoctype, this.docType)) {
                titeldefinition = titel;
                break;
            }

            /* wenn nur pflicht angegeben wurde */
            if (isnotdoctype.equals("") && StringUtils.containsIgnoreCase(isdoctype, this.docType)) {
                titeldefinition = titel;
                break;
            }
            /* wenn nur "darf nicht" angegeben wurde */
            if (isdoctype.equals("") && !StringUtils.containsIgnoreCase(isnotdoctype, this.docType)) {
                titeldefinition = titel;
                break;
            }
        }

        StringTokenizer tokenizer = new StringTokenizer(titeldefinition, "+");
        /* jetzt den Bandtitel parsen */
        while (tokenizer.hasMoreTokens()) {
            String myString = tokenizer.nextToken();
            /*
             * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so
             * übernehmen
             */
            if (myString.startsWith("'") && myString.endsWith("'")) {
                newTitle.append(myString.substring(1, myString.length() - 1));
            } else if (myString.startsWith("#")) {
                /*
                 * resolve strings beginning with # from generic fields
                 */
                if (genericFields != null) {
                    String genericValue = genericFields.get(myString);
                    if (genericValue != null) {
                        newTitle.append(genericValue);
                    }
                }
            } else {
                /* andernfalls den string als Feldnamen auswerten */
                for (AdditionalField additionalField : this.additionalFields) {
                    /*
                     * wenn es das ATS oder TSL-Feld ist, dann den berechneten
                     * atstsl einsetzen, sofern noch nicht vorhanden
                     */
                    if ((additionalField.getTitle().equals("ATS") || additionalField.getTitle().equals("TSL"))
                            && additionalField.getShowDependingOnDoctype()
                            && (additionalField.getValue() == null || additionalField.getValue().equals(""))) {
                        if (atstsl == null || atstsl.length() == 0) {
                            atstsl = createAtstsl(currentTitle, currentAuthors);
                        }
                        additionalField.setValue(this.atstsl);
                    }

                    /* den Inhalt zum Titel hinzufügen */
                    if (additionalField.getTitle().equals(myString) && additionalField.getShowDependingOnDoctype()
                            && additionalField.getValue() != null) {
                        newTitle.append(
                                calculateProcessTitleCheck(additionalField.getTitle(), additionalField.getValue()));
                    }
                }
            }
        }

        if (newTitle.toString().endsWith("_")) {
            newTitle.setLength(newTitle.length() - 1);
        }
        // remove non-ascii characters for the sake of TIFF header limits
        String filteredTitle = newTitle.toString().replaceAll("[^\\p{ASCII}]", "");
        prozessKopie.setTitle(filteredTitle);
        calculateTiffHeader();
        return filteredTitle;
    }

    private String calculateProcessTitleCheck(String inFeldName, String inFeldWert) {
        String rueckgabe = inFeldWert;

        /*
         * Bandnummer
         */
        if (inFeldName.equals("Bandnummer") || inFeldName.equals("Volume number")) {
            try {
                int bandint = Integer.parseInt(inFeldWert);
                java.text.DecimalFormat df = new java.text.DecimalFormat("#0000");
                rueckgabe = df.format(bandint);
            } catch (NumberFormatException e) {
                if (inFeldName.equals("Bandnummer")) {
                    Helper.setFehlerMeldung(
                            Helper.getTranslation("UngueltigeDaten: ") + "Bandnummer ist keine gültige Zahl");
                } else {
                    Helper.setFehlerMeldung(
                            Helper.getTranslation("UngueltigeDaten: ") + "Volume number is not a valid number");
                }
            }
            if (rueckgabe != null && rueckgabe.length() < 4) {
                rueckgabe = "0000".substring(rueckgabe.length()) + rueckgabe;
            }
        }

        return rueckgabe;
    }

    /**
     * Calculate tiff header.
     */
    public void calculateTiffHeader() {
        ConfigProjects cp;
        try {
            cp = new ConfigProjects(this.prozessVorlage.getProject().getTitle());
        } catch (IOException e) {
            Helper.setFehlerMeldung("IOException", e.getMessage());
            return;
        }
        String tifDefinition = cp.getParamString("tifheader." + this.docType, "intranda");

        // possible replacements
        tifDefinition = tifDefinition.replaceAll("\\[\\[", "<");
        tifDefinition = tifDefinition.replaceAll("\\]\\]", ">");

        // Documentname ist im allgemeinen = Prozesstitel
        this.tifHeaderDocumentName = this.prozessKopie.getTitle();
        this.tifHeaderImageDescription = "";
        // image description
        StringTokenizer tokenizer = new StringTokenizer(tifDefinition, "+");
        // jetzt den Tiffheader parsen
        String title = "";
        while (tokenizer.hasMoreTokens()) {
            String myString = tokenizer.nextToken();
            /*
             * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so
             * übernehmen
             */
            if (myString.startsWith("'") && myString.endsWith("'") && myString.length() > 2) {
                this.tifHeaderImageDescription += myString.substring(1, myString.length() - 1);
            } else if (myString.equals("$Doctype")) {
                /* wenn der Doctype angegeben werden soll */
                try {
                    this.tifHeaderImageDescription += ConfigOpac.getDoctypeByName(this.docType).getTifHeaderType();
                } catch (Throwable t) {
                    logger.error("Error while reading von opac-config", t);
                    Helper.setFehlerMeldung("Error while reading von opac-config", t.getMessage());
                }
            } else {
                /* andernfalls den string als Feldnamen auswerten */
                for (AdditionalField additionalField : this.additionalFields) {
                    if (additionalField.getTitle().equals("Titel") || additionalField.getTitle().equals("Title")
                            && additionalField.getValue() != null && !additionalField.getValue().equals("")) {
                        title = additionalField.getValue();
                    }
                    /*
                     * wenn es das ATS oder TSL-Feld ist, dann den berechneten
                     * atstsl einsetzen, sofern noch nicht vorhanden
                     */
                    if ((additionalField.getTitle().equals("ATS") || additionalField.getTitle().equals("TSL"))
                            && additionalField.getShowDependingOnDoctype()
                            && (additionalField.getValue() == null || additionalField.getValue().equals(""))) {
                        additionalField.setValue(this.atstsl);
                    }

                    /* den Inhalt zum Titel hinzufügen */
                    if (additionalField.getTitle().equals(myString) && additionalField.getShowDependingOnDoctype()
                            && additionalField.getValue() != null) {
                        this.tifHeaderImageDescription += calculateProcessTitleCheck(additionalField.getTitle(),
                                additionalField.getValue());
                    }

                }
            }
            // reduce to 255 character
        }
        int length = this.tifHeaderImageDescription.length();
        if (length > 255) {
            try {
                int toCut = length - 255;
                String newTitle = title.substring(0, title.length() - toCut);
                this.tifHeaderImageDescription = this.tifHeaderImageDescription.replace(title, newTitle);
            } catch (IndexOutOfBoundsException e) {
                logger.error(e);
            }
        }
    }

    /**
     * Downloads a docket for the process.
     * 
     * @return the navigation-strign
     */
    public String downloadDocket() {
        try {
            serviceManager.getProcessService().downloadDocket(this.prozessKopie);
        } catch (IOException e) {
            logger.error("Excetion thrown, when creating the docket", e.getMessage());
            // TODO: Handle exceptions in Frontend
        }
        return "";
    }

    /**
     * Set images guessed.
     *
     * @param imagesGuessed
     *            the imagesGuessed to set
     */
    public void setImagesGuessed(Integer imagesGuessed) {
        if (imagesGuessed == null) {
            imagesGuessed = 0;
        }
        this.guessedImages = imagesGuessed;
    }

    /**
     * Get images guessed.
     *
     * @return the imagesGuessed
     */
    public Integer getImagesGuessed() {
        return this.guessedImages;
    }

    public String getAddToWikiField() {
        return this.addToWikiField;
    }

    /**
     * Set add to wiki field.
     *
     * @param addToWikiField
     *            String
     */
    public void setAddToWikiField(String addToWikiField) {
        this.prozessKopie.setWikiField(prozessVorlage.getWikiField());
        this.addToWikiField = addToWikiField;
        if (addToWikiField != null && !addToWikiField.equals("")) {
            User user = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
            String message = this.addToWikiField + " (" + serviceManager.getUserService().getFullName(user) + ")";
            this.prozessKopie
                    .setWikiField(WikiFieldHelper.getWikiMessage(prozessKopie.getWikiField(), "info", message));
        }
    }

    /**
     * Create Atstsl.
     *
     * @param title
     *            String
     * @param author
     *            String
     * @return String
     */
    public static String createAtstsl(String title, String author) {
        StringBuilder result = new StringBuilder(8);
        if (author != null && author.trim().length() > 0) {
            result.append(author.length() > 4 ? author.substring(0, 4) : author);
            result.append(title.length() > 4 ? title.substring(0, 4) : title);
        } else {
            StringTokenizer titleWords = new StringTokenizer(title);
            int wordNo = 1;
            while (titleWords.hasMoreTokens() && wordNo < 5) {
                String word = titleWords.nextToken();
                switch (wordNo) {
                    case 1:
                        result.append(word.length() > 4 ? word.substring(0, 4) : word);
                        break;
                    case 2:
                    case 3:
                        result.append(word.length() > 2 ? word.substring(0, 2) : word);
                        break;
                    case 4:
                        result.append(word.length() > 1 ? word.substring(0, 1) : word);
                        break;
                    default:
                        assert false : wordNo;
                }
                wordNo++;
            }
        }
        return result.toString().replaceAll("[\\W]", ""); // delete umlauts etc.
    }

    /**
     * The function getHitlist returns the hits for the currently showing page
     * of the hitlist as read-only property "hitlist".
     *
     * @return a list of hits to render in the hitlist
     */
    public List<SelectableHit> getHitlist() {
        if (hitlistPage < 0) {
            return Collections.emptyList();
        }
        int pageSize = getPageSize();
        List<SelectableHit> result = new ArrayList<>(pageSize);
        long firstHit = hitlistPage * pageSize;
        long lastHit = Math.min(firstHit + pageSize - 1, hits - 1);
        for (long index = firstHit; index <= lastHit; index++) {
            try {
                Hit hit = importCatalogue.getHit(hitlist, index, CataloguePlugin.getTimeout());
                result.add(new SelectableHit(hit));
            } catch (RuntimeException e) {
                result.add(new SelectableHit(e.getMessage()));
            }
        }
        return result;
    }

    /**
     * The function getNumberOfHits() returns the number of hits on the hit list
     * as read-only property "numberOfHits".
     *
     * @return the number of hits on the hit list
     */
    public long getNumberOfHits() {
        return hits;
    }

    /**
     * The function getPageSize() retrieves the desired number of hits on one
     * page of the hit list from the configuration.
     *
     * @return desired number of hits on one page of the hit list from the
     *         configuration
     */
    private int getPageSize() {
        return ConfigCore.getIntParameter(Parameters.HITLIST_PAGE_SIZE, DEFAULT_HITLIST_PAGE_SIZE);
    }

    /**
     * The function isFirstPage() returns whether the currently showing page of
     * the hitlist is the first page of it as read-only property "firstPage".
     *
     * @return whether the currently showing page of the hitlist is the first
     *         one
     */
    public boolean isFirstPage() {
        return hitlistPage == 0;
    }

    /**
     * The function getHitlistShowing returns whether the hitlist shall be
     * rendered or not as read-only property "hitlistShowing".
     *
     * @return whether the hitlist is to be shown or not
     */
    public boolean isHitlistShowing() {
        return hitlistPage >= 0;
    }

    /**
     * The function isLastPage() returns whether the currently showing page of
     * the hitlist is the last page of it as read-only property "lastPage".
     *
     * @return whether the currently showing page of the hitlist is the last one
     */
    public boolean isLastPage() {
        return (hitlistPage + 1) * getPageSize() > hits - 1;
    }

    /**
     * The function nextPageClick() is executed if the user clicks the action
     * link to flip one page forward in the hit list.
     */
    public void nextPageClick() {
        hitlistPage++;
    }

    /**
     * The function previousPageClick() is executed if the user clicks the
     * action link to flip one page backwards in the hit list.
     */
    public void previousPageClick() {
        hitlistPage--;
    }

    /**
     * The function isCalendarButtonShowing tells whether the calendar button
     * shall show up or not as read-only property "calendarButtonShowing".
     *
     * @return whether the calendar button shall show
     */
    public boolean isCalendarButtonShowing() {
        try {
            return ConfigOpac.getDoctypeByName(docType).isNewspaper();
        } catch (NullPointerException e) {
            // may occur if user continues to interact with the page across a
            // restart of the servlet container
            return false;
        } catch (FileNotFoundException e) {
            logger.error("Error while reading von opac-config", e);
            Helper.setFehlerMeldung("Error while reading von opac-config", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the representation of the file holding the document metadata in
     * memory.
     *
     * @return the metadata file in memory
     */
    public Fileformat getFileformat() {
        return rdf;
    }
}

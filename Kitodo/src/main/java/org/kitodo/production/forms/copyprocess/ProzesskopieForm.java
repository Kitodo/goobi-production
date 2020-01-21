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

package org.kitodo.production.forms.copyprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.JDOMException;
import org.kitodo.api.dataeditor.rulesetmanagement.RulesetManagementInterface;
import org.kitodo.api.dataformat.IncludedStructuralElement;
import org.kitodo.api.dataformat.Workpiece;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.ConfigProject;
import org.kitodo.config.DigitalCollection;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.Template;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.enums.TaskEditType;
import org.kitodo.data.database.enums.TaskStatus;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.exceptions.ProcessCreationException;
import org.kitodo.exceptions.ProcessGenerationException;
import org.kitodo.production.cli.helper.CopyProcess;
import org.kitodo.production.enums.ObjectType;
import org.kitodo.production.forms.BaseForm;
import org.kitodo.production.forms.createprocess.ProcessMetadataTab;
import org.kitodo.production.forms.createprocess.TitleRecordLinkTab;
import org.kitodo.production.forms.dataeditor.DataEditorForm;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.SelectItemList;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyDocStructHelperInterface;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyLogicalDocStructHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataTypeHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetsModsDigitalDocumentHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyPrefsHelper;
import org.kitodo.production.interfaces.RulesetSetupInterface;
import org.kitodo.production.metadata.MetadataEditor;
import org.kitodo.production.metadata.copier.CopierData;
import org.kitodo.production.metadata.copier.DataCopier;
import org.kitodo.production.plugin.PluginLoader;
import org.kitodo.production.plugin.catalogue.CataloguePlugin;
import org.kitodo.production.plugin.catalogue.Hit;
import org.kitodo.production.plugin.catalogue.QueryBuilder;
import org.kitodo.production.process.ProcessGenerator;
import org.kitodo.production.process.TiffHeaderGenerator;
import org.kitodo.production.process.TitleGenerator;
import org.kitodo.production.process.field.AdditionalField;
import org.kitodo.production.search.opac.ConfigOpac;
import org.kitodo.production.search.opac.ConfigOpacDoctype;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.data.ProcessService;
import org.kitodo.production.thread.TaskScriptThread;
import org.omnifaces.util.Ajax;
import org.primefaces.PrimeFaces;

@Named("ProzesskopieForm")
@SessionScoped
public class ProzesskopieForm extends BaseForm implements RulesetSetupInterface, Serializable {
    private static final Logger logger = LogManager.getLogger(ProzesskopieForm.class);
    private static final String OPAC_CONFIG = "configurationOPAC";
    private static final String PHYS_SEQUENCE = "physSequence";
    private static final String FIRST_CHILD = "firstchild";
    private static final String LIST_OF_CREATORS = "ListOfCreators";


    private static final String DIRECTORY_SUFFIX = ConfigCore
            .getParameterOrDefaultValue(ParameterCore.DIRECTORY_SUFFIX);

    /**
     * A filter on the rule set depending on the workflow step. So far this is
     * not configurable anywhere and is therefore on “create”.
     */
    private String acquisitionStage = "create";

    /**
     * Backing bean for the metadata panel.
     */
    private ProcessMetadataTab processMetadataTab;
    /**
     * The ruleset that the file is based on.
     */
    private RulesetManagementInterface ruleset;
    /**
     * The language preference list of the editing user for displaying the
     * metadata labels. We cache this because it’s used thousands of times and
     * otherwise the access would always go through the search engine, which
     * would delay page creation.
     */
    private List<Locale.LanguageRange> priorityList;

    private Workpiece workpiece;

    private String atstsl = "";
    private Integer guessedImages = 0;
    private Process processForChoice;

    /**
     * The field hitlist holds some reference to the hitlist retrieved from a
     * library catalog. The internals of this object are subject to the plug-in
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
     * from a library catalog.
     */
    private long hits;

    /**
     * The field importCatalogue holds the catalog plug-in used to access the
     * library catalog.
     */
    private transient CataloguePlugin importCatalogue;

    private LegacyMetsModsDigitalDocumentHelper rdf;
    private String opacSuchfeld = "12";
    private String opacSuchbegriff;
    private String opacKatalog;
    private final String processListPath = MessageFormat.format(REDIRECT_PATH, "processes");
    private final String processFromTemplatePath = MessageFormat.format(REDIRECT_PATH, "processFromTemplate");

    private final TitleRecordLinkTab titleRecordLinkTab = new TitleRecordLinkTab(null);

    protected String docType;
    protected Template template = new Template();
    protected Process prozessKopie = new Process();
    protected Project project;
    protected boolean useOpac;
    protected boolean useTemplates;
    protected transient List<AdditionalField> additionalFields;
    protected transient Map<String, Boolean> standardFields;
    protected String tifDefinition;
    protected String titleDefinition;
    protected String tifHeaderImageDescription = "";
    protected String tifHeaderDocumentName = "";
    protected transient List<String> digitalCollections;
    protected transient List<String> possibleDigitalCollection;

    protected static final String INCOMPLETE_DATA = "errorDataIncomplete";

    /**
     * Get atstsl.
     *
     * @return value of atstsl
     */
    public String getAtstsl() {
        return atstsl;
    }

    /**
     * Get title definition.
     *
     * @return value of titleDefinition
     */
    public String getTitleDefinition() {
        return titleDefinition;
    }

    /**
     * Prepare template and project for which new process will be created.
     *
     * @param templateId
     *            id of template to query from database
     * @param projectId
     *            id of project to query from database
     *
     * @return path to page with form
     */
    public String prepare(int templateId, int projectId) {
        if (prepareProcess(templateId, projectId)) {
            return processFromTemplatePath;
        }
        return this.stayOnCurrentPage;
    }

    /**
     * Prepare new process which will be created.
     *
     * @param templateId
     *            id of template to query from database
     * @param projectId
     *            id of project to query from database
     *
     * @return true if process was prepared, otherwise false
     */
    public boolean prepareProcess(int templateId, int projectId) {
        atstsl = "";

        ProcessGenerator processGenerator = new ProcessGenerator();
        try {
            boolean generated = processGenerator.generateProcess(templateId, projectId);

            if (generated) {
                this.prozessKopie = processGenerator.getGeneratedProcess();
                this.project = processGenerator.getProject();
                this.template = processGenerator.getTemplate();

                clearValues();
                readProjectConfigs();
                this.ruleset = openRulesetFile(this.prozessKopie.getRuleset().getFile());
                processMetadataTab = new ProcessMetadataTab(null);
                this.workpiece = new Workpiece();
                processMetadataTab.initializeProcessDetails(workpiece.getRootElement());
                this.rdf = null;
                this.digitalCollections = new ArrayList<>();
                initializePossibleDigitalCollections();

                return true;
            }
        } catch (ProcessGenerationException | IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }

        return false;
    }

    /**
     * Get Process templates.
     *
     * @return list of SelectItem objects
     */
    public List<SelectItem> getProcessesForChoiceList() {
        return SelectItemList.getProcessesForChoiceList();
    }

    /**
     * The function is executed if a user clicks the command link
     * to start a catalog search. It performs the search and loads the hit if
     * it is unique. Otherwise, it will cause a hit list to show up for the user
     * to select a hit.
     */
    public void evaluateOpac() {
        long timeout = CataloguePlugin.getTimeout();
        clearValues();
        PrimeFaces.current().ajax().update("hitlistForm");
        try {
            readProjectConfigs();
            if (pluginAvailableFor(opacKatalog)) {
                String query = QueryBuilder.restrictToField(opacSuchfeld, opacSuchbegriff);
                query = QueryBuilder.appendAll(query, ConfigOpac.getRestrictionsForCatalogue(opacKatalog));

                hitlist = importCatalogue.find(query, timeout);
                hits = importCatalogue.getNumberOfHits(hitlist, timeout);

                String message = MessageFormat.format(Helper.getTranslation("newProcess.catalogueSearch.results"),
                    hits);

                switch ((int) Math.min(hits, Integer.MAX_VALUE)) {
                    case 0:
                        Helper.setErrorMessage(message);
                        break;
                    case 1:
                        importHit(importCatalogue.getHit(hitlist, 0, timeout));
                        Helper.setMessage(message);
                        break;
                    default:
                        hitlistPage = 0; // show first page of hitlist
                        Helper.setMessage(message);
                        PrimeFaces.current().executeScript("PF('hitlistDialog').show()");
                        break;
                }
            } else {
                Helper.setErrorMessage("ERROR: No suitable plugin available for OPAC '" + opacKatalog + "'");
            }
        } catch (FileNotFoundException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {"OPAC " + opacKatalog }, logger, e);
        }
    }

    /**
     * Read project configs for display in GUI.
     */
    protected void readProjectConfigs() {
        ConfigProject cp;
        try {
            cp = new ConfigProject(this.project.getTitle());
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return;
        }

        this.docType = cp.getDocType();
        this.useOpac = cp.isUseOpac();
        this.useTemplates = cp.isUseTemplates();
        if (this.opacKatalog.equals("")) {
            this.opacKatalog = cp.getOpacCatalog();
        }

        this.tifDefinition = cp.getTifDefinition();
        this.titleDefinition = cp.getTitleDefinition();

        this.standardFields.putAll(cp.getHiddenFields());
        this.additionalFields = cp.getAdditionalFields();
    }

    /**
     * The function pluginAvailableFor(catalog) verifies that a plugin suitable
     * for accessing the library catalog identified by the given String is
     * available in the global variable importCatalogue. If importCatalogue is
     * empty or the current plug-in doesn’t support the given catalog, the
     * function will try to load a suitable plug-in. Upon success the
     * preferences and the catalog to use will be configured in the plug-in,
     * otherwise an error message will be set to be shown.
     *
     * @param catalogue
     *            identifier string for the catalog that the plug-in shall
     *            support
     * @return whether a plug-in is available in the global variable
     *         importCatalogue
     */
    private boolean pluginAvailableFor(String catalogue) {
        if (Objects.isNull(importCatalogue) || !importCatalogue.supportsCatalogue(catalogue)) {
            importCatalogue = PluginLoader.getCataloguePluginForCatalogue(catalogue);
        }
        if (Objects.isNull(importCatalogue)) {
            Helper.setErrorMessage("NoCataloguePluginForCatalogue", catalogue);
            return false;
        } else {
            importCatalogue
                    .setPreferences(ServiceManager.getRulesetService().getPreferences(prozessKopie.getRuleset()));
            importCatalogue.useCatalogue(catalogue);
            return true;
        }
    }

    /**
     * Reset all configuration properties and fields.
     */
    protected void clearValues() {
        if (Objects.isNull(this.opacKatalog)) {
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
     * Loads a hit into the display.
     *
     * @param hit
     *            Hit to load
     */
    protected void importHit(Hit hit) {
        rdf = hit.getFileformat();
        docType = hit.getDocType();
        fillFieldsFromMetadataFile();
        applyCopyingRules(new CopierData(rdf, this.template));
        atstsl = TitleGenerator.createAtstsl(hit.getTitle(), hit.getAuthors());
        setEditActiveTabIndex(0);
    }

    /**
     * Creates a DataCopier with the given configuration, lets it process the
     * given data and wraps any errors to display in the front end.
     *
     * @param data
     *            data to process
     */
    private void applyCopyingRules(CopierData data) {
        String rules = ConfigCore.getParameter(ParameterCore.COPY_DATA_ON_CATALOGUE_QUERY);
        if (Objects.nonNull(rules)) {
            try {
                new DataCopier(rules).process(data);
            } catch (ConfigurationException e) {
                Helper.setErrorMessage("dataCopier.syntaxError", logger, e);
            }
        }
    }

    /**
     * die Eingabefelder für die Eigenschaften mit Inhalten aus der RDF-Datei
     * füllen.
     */
    private void fillFieldsFromMetadataFile() {
        if (Objects.nonNull(this.rdf)) {
            for (AdditionalField field : this.additionalFields) {
                if (field.isUghBinding() && field.showDependingOnDoctype()) {
                    proceedField(field);
                    String value = field.getValue();
                    if (Objects.nonNull(value) && !value.isEmpty()) {
                        field.setValue(value.replace("&amp;", "&"));
                    }
                }
            }
        }
    }

    private void proceedField(AdditionalField field) {
        LegacyDocStructHelperInterface docStruct = getDocStruct(field);
        try {
            if (field.getMetadata().equals(LIST_OF_CREATORS)) {
                throw new UnsupportedOperationException("Dead code pending removal");
            } else {
                // evaluate the content in normal fields
                LegacyMetadataTypeHelper mdt = LegacyPrefsHelper.getMetadataType(
                    ServiceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset()),
                    field.getMetadata());
                LegacyMetadataHelper md = LegacyLogicalDocStructHelper.getMetadata(docStruct, mdt);
                if (Objects.nonNull(md)) {
                    field.setValue(md.getValue());
                    md.setStringValue(field.getValue().replace("&amp;", "&"));
                }
            }
        } catch (IllegalArgumentException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    private LegacyDocStructHelperInterface getDocStruct(AdditionalField field) {
        LegacyMetsModsDigitalDocumentHelper digitalDocument = this.rdf.getDigitalDocument();
        LegacyDocStructHelperInterface docStruct = digitalDocument.getLogicalDocStruct();
        if (field.getDocStruct().equals(FIRST_CHILD)) {
            docStruct = digitalDocument.getLogicalDocStruct().getAllChildren().get(0);
        }
        if (field.getDocStruct().equals(PHYS_SEQUENCE)) {
            docStruct = digitalDocument.getPhysicalDocStruct();
        }
        return docStruct;
    }

    /**
     * Auswahl des Prozesses auswerten.
     */
    public String evaluateTemplateSelection() {
        readTemplateSelection();

        try {
            this.rdf = ServiceManager.getProcessService().readMetadataAsTemplateFile(this.processForChoice);
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {"template-metadata" }, logger, e);
        }

        removeCollectionsForChildren(this.rdf, this.prozessKopie);
        return null;
    }

    protected void readTemplateSelection() {
        readTemplateWorkpieces(this.additionalFields, this.processForChoice);
        readTemplateTemplates(this.additionalFields, this.processForChoice);
        readTemplateProperties(this.digitalCollections, this.processForChoice);
    }

    protected void readTemplateWorkpieces(List<AdditionalField> additionalFields, Process processForChoice) {
        for (Property workpieceProperty : processForChoice.getWorkpieces()) {
            for (AdditionalField field : additionalFields) {
                if (field.getTitle().equals(workpieceProperty.getTitle())) {
                    field.setValue(workpieceProperty.getValue());
                }
                if (workpieceProperty.getTitle().equals("DocType") && !(this instanceof CopyProcess)) {
                    this.docType = workpieceProperty.getValue();
                }
            }
        }
    }

    protected void readTemplateTemplates(List<AdditionalField> additionalFields, Process processForChoice) {
        for (Property templateProperty : processForChoice.getTemplates()) {
            for (AdditionalField field : additionalFields) {
                if (field.getTitle().equals(templateProperty.getTitle())) {
                    field.setValue(templateProperty.getValue());
                }
            }
        }
    }

    private void readTemplateProperties(List<String> digitalCollections, Process processForChoice) {
        for (Property processProperty : processForChoice.getProperties()) {
            if (processProperty.getTitle().equals("digitalCollection")) {
                digitalCollections.add(processProperty.getValue());
            }
        }
    }

    /**
     * If there is a first child, the collections are for it.
     */
    protected void removeCollectionsForChildren(LegacyMetsModsDigitalDocumentHelper rdf, Process processCopy) {
        try {
            LegacyDocStructHelperInterface colStruct = rdf.getDigitalDocument().getLogicalDocStruct();
            removeCollections(colStruct, processCopy);
            colStruct = colStruct.getAllChildren().get(0);
            removeCollections(colStruct, processCopy);
        } catch (RuntimeException e) {
            logger.debug("das Firstchild unterhalb des Topstructs konnte nicht ermittelt werden", e);
        }
    }

    /**
     * Create the process and save the metadata.
     */
    public String createNewProcess() {
        if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
            if (Objects.isNull(titleRecordLinkTab.getSelectedInsertionPosition())) {
                Helper.setErrorMessage("prozesskopieForm.createNewProcess.noInsertionPositionSelected");
                return stayOnCurrentPage;
            } else {
                User titleRecordOpenUser = DataEditorForm
                        .getUserOpened(titleRecordLinkTab.getTitleRecordProcess().getId());
                if (Objects.nonNull(titleRecordOpenUser)) {
                    Helper.setErrorMessage("prozesskopieForm.createNewProcess.titleRecordOpen",
                        titleRecordOpenUser.getFullName());
                    return stayOnCurrentPage;
                }
            }
        }
        if (createProcess()) {
            if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
                ServiceManager.getProcessService().refresh(prozessKopie);
                try {
                    MetadataEditor.addLink(titleRecordLinkTab.getTitleRecordProcess(),
                        titleRecordLinkTab.getSelectedInsertionPosition(), prozessKopie.getId());

                } catch (IOException exception) {
                    Helper.setErrorMessage("errorSaving", titleRecordLinkTab.getTitleRecordProcess().getTitle(), logger,
                        exception);
                }
            }
            return processListPath;
        }

        return this.stayOnCurrentPage;
    }

    /**
     * Create process.
     *
     * @return true if process was created, otherwise false
     */
    public boolean createProcess() {
        /*if (!ProcessValidator.isContentValid(this.prozessKopie.getTitle(), this.additionalFields,
            this.getDigitalCollections(), this.standardFields, true)) {
            return false;
        }*/
        addProperties();
        updateTasks(this.prozessKopie);

        try {
            this.prozessKopie.setSortHelperImages(this.guessedImages);
            ServiceManager.getProcessService().save(this.prozessKopie);
        } catch (DataException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {ObjectType.PROCESS.getTranslationSingular() }, logger,
                e);
            return false;
        }

        if (!createProcessLocation()) {
            return false;
        }

        processRdfConfiguration();

        if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
            prozessKopie.setParent(titleRecordLinkTab.getTitleRecordProcess());
            titleRecordLinkTab.getTitleRecordProcess().getChildren().add(prozessKopie);
        }

        try {
            ServiceManager.getProcessService().save(this.prozessKopie);
        } catch (DataException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {ObjectType.PROCESS.getTranslationSingular() }, logger,
                e);
            return false;
        }
        return true;
    }

    private boolean createProcessLocation() {
        try {
            URI processBaseUri = ServiceManager.getFileService().createProcessLocation(this.prozessKopie);
            this.prozessKopie.setProcessBaseUri(processBaseUri);
            return true;
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            try {
                ServiceManager.getProcessService().remove(this.prozessKopie);
            } catch (DataException ex) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
            return false;
        }
    }

    /**
     * If there is an RDF configuration (for example, from the catalog import,
     * or freshly created), then supplement these.
     */
    private void processRdfConfiguration() {
        // create RDF config if there is none
        if (Objects.isNull(this.rdf)) {
            createNewFileformat();
        }

        if (Objects.nonNull(workpiece)) {
            processMetadataTab.preserve();
            try (OutputStream out = ServiceManager.getFileService().write(
                    ServiceManager.getProcessService().getMetadataFileUri(prozessKopie))) {
                ServiceManager.getMetsService().save(workpiece, out);
            } catch (IOException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
        try {
            if (Objects.nonNull(this.rdf)) {

                for (AdditionalField field : this.additionalFields) {
                    if (field.isUghBinding() && field.showDependingOnDoctype()) {
                        processAdditionalField(field);
                    }
                }

                updateMetadata();
                insertCollections();
                insertImagePath();
            }

            ServiceManager.getProcessService().readMetadataFile(this.prozessKopie);

            startTaskScriptThreads();
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    private void processAdditionalField(AdditionalField field) {
        // which DocStruct
        LegacyDocStructHelperInterface tempStruct = this.rdf.getDigitalDocument().getLogicalDocStruct();
        LegacyDocStructHelperInterface tempChild = null;
        String fieldDocStruct = field.getDocStruct();
        if (fieldDocStruct.equals(FIRST_CHILD)) {
            try {
                tempStruct = this.rdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
            } catch (RuntimeException e) {
                Helper.setErrorMessage(
                    e.getMessage() + " The first child below the top structure could not be determined!", logger, e);
            }
        }
        // if topstruct and first child should get the metadata
        if (!fieldDocStruct.equals(FIRST_CHILD) && fieldDocStruct.contains(FIRST_CHILD)) {
            try {
                tempChild = this.rdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
            } catch (RuntimeException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
        if (fieldDocStruct.equals(PHYS_SEQUENCE)) {
            tempStruct = this.rdf.getDigitalDocument().getPhysicalDocStruct();
        }
        // which Metadata
        try {
            processAdditionalFieldWhichMetadata(field, tempStruct, tempChild);
        } catch (RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Except for the authors, take all additional into the metadata.
     */
    private void processAdditionalFieldWhichMetadata(AdditionalField field, LegacyDocStructHelperInterface tempStruct,
            LegacyDocStructHelperInterface tempChild) {

        if (!field.getMetadata().equals(LIST_OF_CREATORS)) {
            LegacyPrefsHelper prefs = ServiceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset());
            LegacyMetadataTypeHelper mdt = LegacyPrefsHelper.getMetadataType(prefs, field.getMetadata());
            LegacyMetadataHelper metadata = LegacyLogicalDocStructHelper.getMetadata(tempStruct, mdt);
            if (Objects.nonNull(metadata)) {
                metadata.setStringValue(field.getValue());
            }
            // if the topstruct and the first child should be given the
            // value
            if (Objects.nonNull(tempChild)) {
                metadata = LegacyLogicalDocStructHelper.getMetadata(tempChild, mdt);
                if (Objects.nonNull(metadata)) {
                    metadata.setStringValue(field.getValue());
                }
            }
        }
    }

    private void insertCollections() {
        LegacyDocStructHelperInterface colStruct = this.rdf.getDigitalDocument().getLogicalDocStruct();
        if (Objects.nonNull(colStruct) && !colStruct.getAllChildren().isEmpty()) {
            try {
                addCollections(colStruct);
                // falls ein erstes Kind vorhanden ist, sind die Collectionen
                // dafür
                colStruct = colStruct.getAllChildren().get(0);
                addCollections(colStruct);
            } catch (RuntimeException e) {
                Helper.setErrorMessage("The first child below the top structure could not be determined!", logger, e);
            }
        }
    }

    /**
     * Insert image path and delete any existing ones first.
     */
    private void insertImagePath() throws IOException {
        LegacyMetsModsDigitalDocumentHelper digitalDocument = this.rdf.getDigitalDocument();
        try {
            LegacyMetadataTypeHelper mdt = ProcessService.getMetadataType(this.prozessKopie, "pathimagefiles");
            List<? extends LegacyMetadataHelper> allImagePaths = digitalDocument.getPhysicalDocStruct()
                    .getAllMetadataByType(mdt);
            for (LegacyMetadataHelper metadata : allImagePaths) {
                digitalDocument.getPhysicalDocStruct().getAllMetadata().remove(metadata);
            }
            LegacyMetadataHelper newMetadata = new LegacyMetadataHelper(mdt);
            String path = ServiceManager.getFileService().getImagesDirectory(this.prozessKopie)
                    + this.prozessKopie.getTitle().trim() + "_" + DIRECTORY_SUFFIX;
            if (SystemUtils.IS_OS_WINDOWS) {
                newMetadata.setStringValue("file:/" + path);
            } else {
                newMetadata.setStringValue("file://" + path);
            }
            digitalDocument.getPhysicalDocStruct().addMetadata(newMetadata);

            // write Rdf file
            ServiceManager.getFileService().writeMetadataFile(this.rdf, this.prozessKopie);
        } catch (IllegalArgumentException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    protected void updateTasks(Process process) {
        for (Task task : process.getTasks()) {
            // always save date and user for each step
            task.setProcessingTime(process.getCreationDate());
            task.setEditType(TaskEditType.AUTOMATIC);
            // only if its done, set edit start and end date
            if (task.getProcessingStatus() == TaskStatus.DONE) {
                task.setProcessingBegin(process.getCreationDate());
                // this concerns steps, which are set as done right on creation
                // bearbeitungsbeginn is set to creation timestamp of process
                // because the creation of it is basically begin of work
                Date date = new Date();
                task.setProcessingTime(date);
                task.setProcessingEnd(date);
            }
        }
    }

    /**
     * Metadata inheritance and enrichment.
     */
    private void updateMetadata() {
        if (ConfigCore.getBooleanParameter(ParameterCore.USE_METADATA_ENRICHMENT)) {
            LegacyDocStructHelperInterface enricher = rdf.getDigitalDocument().getLogicalDocStruct();
            Map<String, Map<String, LegacyMetadataHelper>> higherLevelMetadata = new HashMap<>();

            // save higher level metadata for lower enrichment
            List<LegacyMetadataHelper> allMetadata = enricher.getAllMetadata();

            iterateOverAllMetadata(higherLevelMetadata, allMetadata);

            // enrich children with inherited metadata
            for (LegacyDocStructHelperInterface nextChild : enricher.getAllChildren()) {
                enricher = nextChild;
                iterateOverHigherLevelMetadata(enricher, higherLevelMetadata);
            }
        }
    }

    private void iterateOverAllMetadata(Map<String, Map<String, LegacyMetadataHelper>> higherLevelMetadata,
            List<LegacyMetadataHelper> allMetadata) {
        for (LegacyMetadataHelper available : allMetadata) {
            String availableKey = available.getMetadataType().getName();
            String availableValue = available.getValue();
            Map<String, LegacyMetadataHelper> availableMetadata = higherLevelMetadata.containsKey(availableKey)
                    ? higherLevelMetadata.get(availableKey)
                    : new HashMap<>();
            if (!availableMetadata.containsKey(availableValue)) {
                availableMetadata.put(availableValue, available);
            }
            higherLevelMetadata.put(availableKey, availableMetadata);
        }
    }

    private void iterateOverHigherLevelMetadata(LegacyDocStructHelperInterface enricher,
            Map<String, Map<String, LegacyMetadataHelper>> higherLevelMetadata) {
        for (Entry<String, Map<String, LegacyMetadataHelper>> availableHigherMetadata : higherLevelMetadata
                .entrySet()) {
            String enrichable = availableHigherMetadata.getKey();
            if (!isAddable(enricher, enrichable)) {
                continue;
            }

            for (Entry<String, LegacyMetadataHelper> higherElement : availableHigherMetadata.getValue().entrySet()) {
                List<LegacyMetadataHelper> amNotNull = enricher.getAllMetadata();

                boolean breakMiddle = false;
                for (LegacyMetadataHelper existentMetadata : amNotNull) {
                    if (existentMetadata.getMetadataType().getName().equals(enrichable)
                            && existentMetadata.getValue().equals(higherElement.getKey())) {
                        breakMiddle = true;
                        break;
                    }
                }
                if (breakMiddle) {
                    break;
                } else {
                    enricher.addMetadata(higherElement.getValue());
                }
            }
        }
    }

    private boolean isAddable(LegacyDocStructHelperInterface enricher, String enrichable) {
        boolean addable = false;
        List<LegacyMetadataTypeHelper> addableTypesNotNull = enricher.getAddableMetadataTypes();

        for (LegacyMetadataTypeHelper addableMetadata : addableTypesNotNull) {
            if (addableMetadata.getName().equals(enrichable)) {
                addable = true;
                break;
            }
        }
        return addable;
    }

    private void startTaskScriptThreads() {
        /* damit die Sortierung stimmt nochmal einlesen */
        ServiceManager.getProcessService().refresh(this.prozessKopie);

        List<Task> tasks = this.prozessKopie.getTasks();
        for (Task task : tasks) {
            if (task.getProcessingStatus() == TaskStatus.OPEN && task.isTypeAutomatic()) {
                TaskScriptThread thread = new TaskScriptThread(task);
                thread.start();
            }
        }
    }

    private void addCollections(LegacyDocStructHelperInterface colStruct) {
        for (String s : this.digitalCollections) {
            try {
                LegacyMetadataHelper md = new LegacyMetadataHelper(LegacyPrefsHelper.getMetadataType(
                    ServiceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset()),
                    "singleDigCollection"));
                md.setStringValue(s);
                md.setDocStruct(colStruct);
                colStruct.addMetadata(md);
            } catch (IllegalArgumentException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
    }

    /**
     * alle Kollektionen eines übergebenen DocStructs entfernen.
     */
    protected void removeCollections(LegacyDocStructHelperInterface colStruct, Process process) {
        try {
            LegacyMetadataTypeHelper mdt = LegacyPrefsHelper.getMetadataType(
                ServiceManager.getRulesetService().getPreferences(process.getRuleset()), "singleDigCollection");
            ArrayList<LegacyMetadataHelper> myCollections = new ArrayList<>(colStruct.getAllMetadataByType(mdt));
            for (LegacyMetadataHelper md : myCollections) {
                colStruct.removeMetadata(md);
            }
        } catch (IllegalArgumentException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Creates a new file format. When a new process is created, an empty METS
     * file  must be created for it.
     */
    public void createNewFileformat() {
        RulesetManagementInterface ruleset = ServiceManager.getRulesetService()
                .getPreferences(this.prozessKopie.getRuleset()).getRuleset();
        try {
            IncludedStructuralElement includedStructuralElement = workpiece.getRootElement();
            ConfigOpacDoctype configOpacDoctype = ConfigOpac.getDoctypeByName(this.docType);
            if (Objects.nonNull(configOpacDoctype)) {
                // monograph
                if (!configOpacDoctype.isPeriodical() && !configOpacDoctype.isMultiVolume()) {
                    workpiece.getRootElement().setType(configOpacDoctype.getRulesetType());
                    this.rdf = new LegacyMetsModsDigitalDocumentHelper(ruleset, workpiece);
                } else if (configOpacDoctype.isPeriodical()) {
                    // journal
                    includedStructuralElement.setType("Periodical");
                    addChild(includedStructuralElement, "PeriodicalVolume");
                    this.rdf = new LegacyMetsModsDigitalDocumentHelper(ruleset, workpiece);
                } else if (configOpacDoctype.isMultiVolume()) {
                    // volume of a multi-volume publication
                    includedStructuralElement.setType("MultiVolumeWork");
                    addChild(includedStructuralElement, "Volume");
                    this.rdf = new LegacyMetsModsDigitalDocumentHelper(ruleset, workpiece);
                }
            }
            if (this.docType.equals("volumerun")) {
                includedStructuralElement.setType("VolumeRun");
                addChild(includedStructuralElement, "Record");
                this.rdf = new LegacyMetsModsDigitalDocumentHelper(ruleset, workpiece);
            }
        } catch (FileNotFoundException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
        }
    }

    /**
     * Adds a child node to a part of the logical structure tree.
     *
     * @param includedStructuralElement
     *            tree to add to
     * @param type
     *            type of child to create
     */
    private void addChild(IncludedStructuralElement includedStructuralElement, String type) {
        IncludedStructuralElement volume = new IncludedStructuralElement();
        volume.setType(type);
        includedStructuralElement.getChildren().add(volume);
    }

    private void addProperties() {
        addAdditionalFields(this.additionalFields, this.prozessKopie);

        for (String col : digitalCollections) {
            ProcessGenerator.addPropertyForProcess(this.prozessKopie, "digitalCollection", col);
        }

        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "DocType", this.docType);
        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderImagedescription",
            this.tifHeaderImageDescription);
        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderDocumentname", this.tifHeaderDocumentName);
        ProcessGenerator.addPropertyForProcess(this.prozessKopie, "Template", this.template.getTitle());
        ProcessGenerator.addPropertyForProcess(this.prozessKopie, "TemplateID", String.valueOf(this.template.getId()));
    }

    protected void addAdditionalFields(List<AdditionalField> additionalFields, Process process) {
        for (AdditionalField field : additionalFields) {
            if (field.showDependingOnDoctype()) {
                if (field.getFrom().equals("werk")) {
                    ProcessGenerator.addPropertyForWorkpiece(process, field.getTitle(), field.getValue());
                }
                if (field.getFrom().equals("vorlage")) {
                    ProcessGenerator.addPropertyForTemplate(process, field.getTitle(), field.getValue());
                }
                if (field.getFrom().equals("prozess")) {
                    ProcessGenerator.addPropertyForProcess(process, field.getTitle(), field.getValue());
                }
            }
        }
    }

    @Override
    public RulesetManagementInterface getRuleset() {
        return ruleset;
    }

    public String getAcquisitionStage() {
        return acquisitionStage;
    }

    @Override
    public List<Locale.LanguageRange> getPriorityList() {
        return priorityList;
    }

    public String getDocType() {
        return this.docType;
    }

    private RulesetManagementInterface openRulesetFile(String fileName) throws IOException {
        final long begin = System.nanoTime();
        String metadataLanguage = ServiceManager.getUserService().getCurrentUser().getMetadataLanguage();
        priorityList = Locale.LanguageRange.parse(metadataLanguage.isEmpty() ? "en" : metadataLanguage);
        RulesetManagementInterface ruleset = ServiceManager.getRulesetManagementService().getRulesetManagement();
        ruleset.load(new File(Paths.get(ConfigCore.getParameter(ParameterCore.DIR_RULESETS), fileName).toString()));
        if (logger.isTraceEnabled()) {
            logger.trace("Reading ruleset took {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
        }
        return ruleset;
    }

    /**
     * Get processMetadataTab.
     *
     * @return value of processMetadataTab
     */
    public ProcessMetadataTab getProcessMetadataTab() {
        return processMetadataTab;
    }

    /**
     * Set processMetadataTab.
     *
     * @param processMetadataTab as org.kitodo.production.forms.createprocess.ProcessMetadataTab
     */
    public void setProcessMetadataTab(ProcessMetadataTab processMetadataTab) {
        this.processMetadataTab = processMetadataTab;
    }

    /**
     * Set document type.
     *
     * @param docType
     *            String
     */
    public void setDocType(String docType) {
        if (!this.docType.equals(docType)) {
            this.docType = docType;
            // This is now done in "ProcessDataTab"!
            //processMetadataTab.setDocType(docType);
            if (Objects.nonNull(rdf)) {
                LegacyMetsModsDigitalDocumentHelper tmp = rdf;

                createNewFileformat();
                if (rdf.getDigitalDocument().getLogicalDocStruct()
                        .equals(tmp.getDigitalDocument().getLogicalDocStruct())) {
                    rdf = tmp;
                } else {
                    LegacyDocStructHelperInterface oldLogicalDocstruct = tmp.getDigitalDocument().getLogicalDocStruct();
                    LegacyDocStructHelperInterface newLogicalDocstruct = rdf.getDigitalDocument().getLogicalDocStruct();
                    // both have no children
                    if (oldLogicalDocstruct.getAllChildren().isEmpty() && newLogicalDocstruct.getAllChildren().isEmpty()) {
                        copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                    } else if (!oldLogicalDocstruct.getAllChildren().isEmpty()
                            && newLogicalDocstruct.getAllChildren().isEmpty()) {
                        // old has a child, new has no child
                        copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                        copyMetadata(oldLogicalDocstruct.getAllChildren().get(0), newLogicalDocstruct);
                    } else if (oldLogicalDocstruct.getAllChildren().isEmpty()
                            && !newLogicalDocstruct.getAllChildren().isEmpty()) {
                        // new has a child, but old not
                        copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                        throw new UnsupportedOperationException("Dead code pending removal");
                    } else if (!oldLogicalDocstruct.getAllChildren().isEmpty()
                            && !newLogicalDocstruct.getAllChildren().isEmpty()) {
                        // both have children
                        copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                        copyMetadata(oldLogicalDocstruct.getAllChildren().get(0),
                            newLogicalDocstruct.getAllChildren().get(0));
                    }
                }
                fillFieldsFromMetadataFile();
            }
        }
    }

    private void copyMetadata(LegacyDocStructHelperInterface oldDocStruct,
            LegacyDocStructHelperInterface newDocStruct) {
        for (LegacyMetadataHelper md : oldDocStruct.getAllMetadata()) {
            newDocStruct.addMetadata(md);
        }
    }

    /**
     * Get template.
     *
     * @return value of template
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Set template.
     *
     * @param template
     *            as Template object
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
     * Get process for choice list.
     *
     * @return process for choice list
     */
    public Process getProcessForChoice() {
        return this.processForChoice;
    }

    /**
     * Set process for choice list.
     *
     * @param processForChoice
     *            as Process object
     */
    public void setProcessForChoice(Process processForChoice) {
        this.processForChoice = processForChoice;
    }

    public List<AdditionalField> getAdditionalFields() {
        return this.additionalFields;
    }

    /**
     * The method getVisibleAdditionalFields returns a list of visible
     * additional fields.
     *
     * @return list of AdditionalField
     */
    public List<AdditionalField> getVisibleAdditionalFields() {
        return this.getAdditionalFields().stream().filter(AdditionalField::showDependingOnDoctype)
                .collect(Collectors.toList());
    }

    /**
     * Sets the value of an AdditionalField held
     * by a ProzesskopieForm object.
     *
     * @param key
     *            the title of the AdditionalField whose value shall be modified
     * @param value
     *            the new value for the AdditionalField
     * @param strict
     *            throw a RuntimeException if the field is unknown
     * @throws ProcessCreationException
     *             in case that no field with a matching title was found in the
     *             ProzesskopieForm object
     */
    public void setAdditionalField(String key, String value, boolean strict) {
        boolean unknownField = true;
        for (AdditionalField field : additionalFields) {
            if (key.equals(field.getTitle())) {
                field.setValue(value);
                unknownField = false;
            }
        }
        if (unknownField && strict) {
            throw new ProcessCreationException(
                    "Couldn’t set “" + key + "” to “" + value + "”: No such field in record.");
        }
    }

    public void setAdditionalFields(List<AdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
    }

    /**
     * This is needed for GUI, render multiple select only if this is false if
     * this is true use the only choice.
     *
     * @return true or false
     */
    public boolean isSingleChoiceCollection() {
        return getPossibleDigitalCollections().size() == 1;
    }

    /**
     * Get possible digital collections if single choice.
     *
     * @return possible digital collections if single choice
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

    protected void initializePossibleDigitalCollections() {
        try {
            DigitalCollection.possibleDigitalCollectionsForProcess(this.prozessKopie.getProject());
        } catch (JDOMException | IOException e) {
            Helper.setErrorMessage("Error while parsing digital collections", logger, e);
        }

        this.possibleDigitalCollection = DigitalCollection.getPossibleDigitalCollection();
        this.digitalCollections = DigitalCollection.getDigitalCollections();

        // if only one collection is possible take it directly
        if (isSingleChoiceCollection()) {
            this.digitalCollections.add(getDigitalCollectionIfSingleChoice());
        }
    }

    /**
     * Get all library catalogs.
     *
     * @return list of catalogs
     */
    public List<String> getAllOpacCatalogues() {
        try {
            return ConfigOpac.getAllCatalogueTitles();
        } catch (RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
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
        } catch (RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
            return new ArrayList<>();
        }
    }

    /**
     * Changed, so that on first request list gets set if there is only one
     * choice.
     *
     * @return list of digital collections
     */
    public List<String> getDigitalCollections() {
        return this.digitalCollections;
    }

    public void setDigitalCollections(List<String> digitalCollections) {
        this.digitalCollections = digitalCollections;
    }

    public Map<String, Boolean> getStandardFields() {
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

    public Project getProject() {
        return this.project;
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
     * Returns the data object underlying the title record link tab.
     *
     * @return the data object underlying the title record link tab
     */
    public TitleRecordLinkTab getTitleRecordLinkTab() {
        return titleRecordLinkTab;
    }

    /**
     * Generate process titles and other details.
     */
    public void calculateProcessTitle() {
        TitleGenerator titleGenerator = new TitleGenerator(this.atstsl, null);
        try {
            String newTitle = titleGenerator.generateTitle(this.titleDefinition, null);
            this.prozessKopie.setTitle(newTitle);
            // atstsl is created in title generator and next used in tiff header generator
            this.atstsl = titleGenerator.getAtstsl();
        } catch (ProcessGenerationException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return;
        }

        calculateTiffHeader();

        Ajax.update("editForm:processFromTemplateTabView:processDataEditGrid");
    }

    /**
     * Calculate tiff header.
     */
    public void calculateTiffHeader() {
        // document name is generally equal to process title
        this.tifHeaderDocumentName = this.prozessKopie.getTitle();

        TiffHeaderGenerator tiffHeaderGenerator = new TiffHeaderGenerator(this.atstsl, null);
        try {
            this.tifHeaderImageDescription = tiffHeaderGenerator.generateTiffHeader(this.tifDefinition, this.docType);
        } catch (ProcessGenerationException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Set images guessed.
     *
     * @param imagesGuessed
     *            the imagesGuessed to set
     */
    public void setImagesGuessed(Integer imagesGuessed) {
        if (Objects.isNull(imagesGuessed)) {
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
            Helper.setErrorMessage(ERROR_READING, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
            return false;
        }
    }

    /**
     * Returns the representation of the file holding the document metadata in
     * memory.
     *
     * @return the metadata file in memory
     */
    public LegacyMetsModsDigitalDocumentHelper getFileformat() {
        return rdf;
    }
}

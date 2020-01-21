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

package org.kitodo.production.forms;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.JDOMException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.DigitalCollection;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Batch;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.Template;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.exceptions.ProcessGenerationException;
import org.kitodo.production.enums.ImportFormat;
import org.kitodo.production.enums.ImportReturnValue;
import org.kitodo.production.enums.ImportType;
import org.kitodo.production.enums.PluginType;
import org.kitodo.production.flow.helper.JobCreation;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyPrefsHelper;
import org.kitodo.production.importer.DocstructElement;
import org.kitodo.production.importer.ImportObject;
import org.kitodo.production.importer.Record;
import org.kitodo.production.plugin.PluginLoader;
import org.kitodo.production.plugin.interfaces.IImportPlugin;
import org.kitodo.production.properties.ImportProperty;
import org.kitodo.production.search.opac.ConfigOpac;
import org.kitodo.production.services.ServiceManager;
import org.primefaces.model.UploadedFile;

@Named("MassImportForm")
@SessionScoped
public class MassImportForm extends BaseForm {
    private static final Logger logger = LogManager.getLogger(MassImportForm.class);
    private Template template;
    private Project project;
    private List<Process> processes;
    private List<String> digitalCollections;
    private List<String> possibleDigitalCollections;
    private String opacCatalogue;
    private List<String> ids = new ArrayList<>();
    private ImportFormat format = null;
    private String idList = "";
    private String records = "";
    private List<String> usablePluginsForRecords;
    private List<String> usablePluginsForIDs;
    private List<String> usablePluginsForFiles;
    private List<String> usablePluginsForFolder;
    private String currentPlugin = "";
    private transient IImportPlugin plugin;
    private File importFile = null;
    private UploadedFile uploadedFile;
    private List<Process> processList;
    private List<String> allFilenames = new ArrayList<>();
    private List<String> selectedFilenames = new ArrayList<>();
    private static final String GET_CURRENT_DOC_STRUCTS = "getCurrentDocStructs";
    private static final String OPAC_CONFIG = "configurationOPAC";
    private final String massImportPath = MessageFormat.format(REDIRECT_PATH, "massImport");
    private final String massImportTwoPath = MessageFormat.format(REDIRECT_PATH, "massImport2");
    private final String massImportThreePath = MessageFormat.format(REDIRECT_PATH, "massImport3");

    /**
     * Constructor.
     */
    public MassImportForm() {
        usablePluginsForRecords = PluginLoader.getImportPluginsForType(ImportType.RECORD);
        usablePluginsForIDs = PluginLoader.getImportPluginsForType(ImportType.ID);
        usablePluginsForFiles = PluginLoader.getImportPluginsForType(ImportType.FILE);
        usablePluginsForFolder = PluginLoader.getImportPluginsForType(ImportType.FOLDER);
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
        try {
            this.template = ServiceManager.getTemplateService().getById(templateId);
            this.project = ServiceManager.getProjectService().getById(projectId);
        } catch (DAOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return this.stayOnCurrentPage;
        }

        try {
            ServiceManager.getTemplateService().checkForUnreachableTasks(this.template.getTasks());
        } catch (ProcessGenerationException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return this.stayOnCurrentPage;
        }

        try {
            DigitalCollection.possibleDigitalCollectionsForProcess(this.project);
        } catch (IOException | JDOMException e) {
            Helper.setErrorMessage("Error while parsing digital collections", logger, e);
        }

        this.digitalCollections = DigitalCollection.getDigitalCollections();
        this.possibleDigitalCollections = DigitalCollection.getPossibleDigitalCollection();

        return massImportPath;
    }

    public List<String> getAllFilenames() {
        return this.allFilenames;
    }

    public void setAllFilenames(List<String> allFilenames) {
        this.allFilenames = allFilenames;
    }

    public List<String> getSelectedFilenames() {
        return this.selectedFilenames;
    }

    public void setSelectedFilenames(List<String> selectedFilenames) {
        this.selectedFilenames = selectedFilenames;
    }

    /**
     * Convert data.
     *
     * @return String
     */
    public String convertData() throws IOException {
        this.processList = new ArrayList<>();
        if (StringUtils.isEmpty(currentPlugin)) {
            Helper.setErrorMessage("missingPlugin");
            return this.stayOnCurrentPage;
        }
        if (testForData()) {
            List<ImportObject> answer = new ArrayList<>();

            // found list with ids
            LegacyPrefsHelper prefs = ServiceManager.getRulesetService().getPreferences(this.template.getRuleset());
            String tempFolder = ConfigCore.getParameter(ParameterCore.DIR_TEMP);
            this.plugin.setImportFolder(tempFolder);
            this.plugin.setPrefs(prefs);
            this.plugin.setOpacCatalogue(this.getOpacCatalogue());
            this.plugin.setKitodoConfigDirectory(ConfigCore.getKitodoConfigDirectory());

            if (StringUtils.isNotEmpty(this.idList)) {
                answer = this.plugin.generateFiles(generateRecordList());
            } else if (Objects.nonNull(this.importFile)) {
                this.plugin.setFile(this.importFile);
                answer = getAnswer(this.plugin.generateRecordsFromFile());
            } else if (StringUtils.isNotEmpty(this.records)) {
                answer = getAnswer(this.plugin.splitRecords(this.records));
            } else if (!this.selectedFilenames.isEmpty()) {
                answer = getAnswer(this.plugin.generateRecordsFromFilenames(this.selectedFilenames));
            }

            iterateOverAnswer(answer);

            if (answer.size() != this.processList.size()) {
                // some error on process generation, don't go to next page
                return this.stayOnCurrentPage;
            }
        } else {
            Helper.setErrorMessage("missingData");
            return this.stayOnCurrentPage;
        }

        removeFiles();

        this.idList = null;
        this.records = "";
        return massImportThreePath;
    }

    private void iterateOverAnswer(List<ImportObject> answer) throws IOException {
        Batch batch = null;
        if (answer.size() > 1) {
            batch = getBatch();
        }

        for (ImportObject io : answer) {
            if (Objects.nonNull(batch)) {
                io.getBatches().add(batch);
            }

            if (io.getImportReturnValue().equals(ImportReturnValue.EXPORT_FINISHED)) {
                addProcessToList(io);
            } else {
                removeImportFileNameFromSelectedFileNames(io);
            }
        }
    }

    private void removeFiles() throws IOException {
        if (Objects.nonNull(this.importFile)) {
            Files.delete(this.importFile.toPath());
            this.importFile = null;
        }
        if (Objects.nonNull(selectedFilenames) && !selectedFilenames.isEmpty()) {
            this.plugin.deleteFiles(this.selectedFilenames);
        }
    }

    /**
     * File upload with binary copying.
     */
    public void uploadFile() {
        if (Objects.isNull(this.uploadedFile)) {
            Helper.setErrorMessage("noFileSelected");
            return;
        }

        String fileName = this.uploadedFile.getFileName();

        File temporalFile = new File(
                FilenameUtils.concat(ConfigCore.getParameterOrDefaultValue(ParameterCore.DIR_TEMP), fileName));

        try {
            FileUtils.copyToFile(this.uploadedFile.getInputstream(), temporalFile);
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    public UploadedFile getUploadedFile() {
        return this.uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    /**
     * tests input fields for correct data.
     *
     * @return true if data is valid or false otherwise
     */

    private boolean testForData() {
        return !(StringUtils.isEmpty(this.idList) && StringUtils.isEmpty(this.records)
                && Objects.isNull(this.importFile) && this.selectedFilenames.isEmpty());
    }

    private List<Record> generateRecordList() {
        List<String> pluginIds = this.plugin.splitIds(this.idList);
        List<Record> recordList = new ArrayList<>();
        for (String id : pluginIds) {
            Record r = new Record();
            r.setData(id);
            r.setId(id);
            r.setCollections(this.digitalCollections);
            recordList.add(r);
        }
        return recordList;
    }

    private List<ImportObject> getAnswer(List<Record> recordList) {
        for (Record record : recordList) {
            record.setCollections(this.digitalCollections);
        }
        return this.plugin.generateFiles(recordList);
    }

    private Batch getBatch() {
        if (Objects.nonNull(importFile)) {
            List<String> arguments = new ArrayList<>();
            arguments.add(FilenameUtils.getBaseName(importFile.getAbsolutePath()));
            arguments.add(DateTimeFormat.shortDateTime().print(new DateTime()));
            return new Batch(Helper.getTranslation("importedBatchLabel", arguments));
        } else {
            return new Batch();
        }
    }

    private void addProcessToList(ImportObject importObject) throws IOException {
        URI importFileName = importObject.getImportFileName();
        Process process = JobCreation.generateProcess(importObject, this.template);
        if (Objects.isNull(process)) {
            if (Objects.nonNull(importFileName)
                    && !ServiceManager.getFileService().getFileName(importFileName).isEmpty()
                    && Objects.nonNull(selectedFilenames) && !selectedFilenames.isEmpty()) {
                selectedFilenames.remove(importFileName.getRawPath());
            }
            Helper.setErrorMessage("import failed for " + importObject.getProcessTitle() + ", process generation failed");
        } else {
            Helper.setMessage(ImportReturnValue.EXPORT_FINISHED.getValue() + " for " + importObject.getProcessTitle());
            this.processList.add(process);
        }
    }

    private void removeImportFileNameFromSelectedFileNames(ImportObject io) {
        URI importFileName = io.getImportFileName();
        Helper.setErrorMessage("importFailedError", new Object[] {io.getProcessTitle(), io.getErrorMessage() });
        if (Objects.nonNull(importFileName) && !ServiceManager.getFileService().getFileName(importFileName).isEmpty()
                && Objects.nonNull(selectedFilenames) && !selectedFilenames.isEmpty()) {
            selectedFilenames.remove(importFileName.getRawPath());
        }
    }

    /**
     * Set id list.
     *
     * @param idList
     *            the idList to set
     */
    public void setIdList(String idList) {
        this.idList = idList;
    }

    /**
     * Get id list.
     *
     * @return the idList
     */
    public String getIdList() {
        return this.idList;
    }

    /**
     * Set records.
     *
     * @param records
     *            the records to set
     */
    public void setRecords(String records) {
        this.records = records;
    }

    /**
     * Get records.
     *
     * @return the records
     */
    public String getRecords() {
        return this.records;
    }

    /**
     * Set processes.
     *
     * @param processes
     *            the process list to set
     */
    public void setProcess(List<Process> processes) {
        this.processes = processes;
    }

    /**
     * Get processes.
     *
     * @return the process
     */
    public List<Process> getProcess() {
        return this.processes;
    }

    /**
     * Set template.
     *
     * @param template
     *            the template to set
     */
    public void setTemplate(Template template) {
        this.template = template;

    }

    /**
     * Get template.
     *
     * @return the template
     */
    public Template getTemplate() {
        return this.template;
    }

    /**
     * Get all library catalogs.
     *
     * @return the library catalogs
     */
    public List<String> getAllOpacCatalogues() {
        List<String> allOpacCatalogues = new ArrayList<>();
        try {
            allOpacCatalogues = ConfigOpac.getAllCatalogueTitles();
        } catch (RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
        }
        return allOpacCatalogues;
    }

    /**
     * Set library catalog.
     *
     * @param opacCatalogue
     *            the opacCatalogue to set
     */

    public void setOpacCatalogue(String opacCatalogue) {
        this.opacCatalogue = opacCatalogue;
    }

    /**
     * Get library catalog.
     *
     * @return the opac catalogs
     */

    public String getOpacCatalogue() {
        return this.opacCatalogue;
    }

    /**
     * Set digital collections.
     *
     * @param digitalCollections
     *            the digitalCollections to set
     */
    public void setDigitalCollections(List<String> digitalCollections) {
        this.digitalCollections = digitalCollections;
    }

    /**
     * Get digital collections.
     *
     * @return the digitalCollections
     */
    public List<String> getDigitalCollections() {
        return this.digitalCollections;
    }

    /**
     * Set possible digital collection.
     *
     * @param possibleDigitalCollection
     *            the possibleDigitalCollection to set
     */
    public void setPossibleDigitalCollection(List<String> possibleDigitalCollection) {
        this.possibleDigitalCollections = possibleDigitalCollection;
    }

    /**
     * Get possible digital collection.
     *
     * @return the possibleDigitalCollection
     */
    public List<String> getPossibleDigitalCollection() {
        return this.possibleDigitalCollections;
    }

    public void setPossibleDigitalCollections(List<String> possibleDigitalCollections) {
        this.possibleDigitalCollections = possibleDigitalCollections;
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }

    /**
     * Set ids.
     *
     * @param ids
     *            the ids to set
     */
    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    /**
     * Get ids.
     *
     * @return the ids
     */
    public List<String> getIds() {
        return this.ids;
    }

    /**
     * Set format.
     *
     * @param format
     *            the format to set
     */
    public void setFormat(String format) {
        this.format = ImportFormat.getTypeFromTitle(format);
    }

    /**
     * Get format.
     *
     * @return the format
     */
    public String getFormat() {
        if (Objects.isNull(this.format)) {
            return null;
        }
        return this.format.getTitle();
    }

    /**
     * Set current plug-in.
     *
     * @param currentPlugin
     *            the currentPlugin to set
     */
    public void setCurrentPlugin(String currentPlugin) {
        this.currentPlugin = currentPlugin;
        if (Objects.nonNull(this.currentPlugin) && !this.currentPlugin.isEmpty()) {
            this.plugin = (IImportPlugin) PluginLoader.getPluginByTitle(PluginType.IMPORT, this.currentPlugin);

            if (Objects.nonNull(this.plugin)) {
                if (this.plugin.getImportTypes().contains(ImportType.FOLDER)) {
                    this.allFilenames = this.plugin.getAllFilenames();
                }
                this.plugin.setPrefs(ServiceManager.getRulesetService().getPreferences(template.getRuleset()));
            }
        }
    }

    /**
     * Get current plug-in.
     *
     * @return the currentPlugin
     */
    public String getCurrentPlugin() {
        return this.currentPlugin;
    }

    public IImportPlugin getPlugin() {
        return plugin;
    }

    /**
     * Set usable plug-ins for records.
     *
     * @param usablePluginsForRecords
     *            the usablePluginsForRecords to set
     */
    public void setUsablePluginsForRecords(List<String> usablePluginsForRecords) {
        this.usablePluginsForRecords = usablePluginsForRecords;
    }

    /**
     * Get usable plug-ins for records.
     *
     * @return the usablePluginsForRecords
     */
    public List<String> getUsablePluginsForRecords() {
        return this.usablePluginsForRecords;
    }

    /**
     * Set usable plug-ins for ids.
     *
     * @param usablePluginsForIDs
     *            the usablePluginsForIDs to set
     */
    public void setUsablePluginsForIDs(List<String> usablePluginsForIDs) {
        this.usablePluginsForIDs = usablePluginsForIDs;
    }

    /**
     * Get usable plug-ins for ids.
     *
     * @return the usablePluginsForIDs
     */
    public List<String> getUsablePluginsForIDs() {
        return this.usablePluginsForIDs;
    }

    /**
     * Set usable plug-ins for files.
     *
     * @param usablePluginsForFiles
     *            the usablePluginsForFiles to set
     */
    public void setUsablePluginsForFiles(List<String> usablePluginsForFiles) {
        this.usablePluginsForFiles = usablePluginsForFiles;
    }

    /**
     * get usable plug-ins for files.
     *
     * @return the usablePluginsForFiles
     */
    public List<String> getUsablePluginsForFiles() {
        return this.usablePluginsForFiles;
    }

    /**
     * Get has next page.
     *
     * @return boolean
     */
    public boolean getHasNextPage() {
        //TODO: find out why not error is thrown here
        /*Method method;
        try {
            method = this.plugin.getClass().getMethod(GET_CURRENT_DOC_STRUCTS);
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (list != null) {
                return true;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        try {
            method = this.plugin.getClass().getMethod("getProperties");
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<ImportProperty> list = (List<ImportProperty>) o;
            return !list.isEmpty();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }*/
        return false;
    }

    /**
     * Get next page.
     *
     * @return next page
     */
    public String nextPage() {
        if (!testForData()) {
            Helper.setErrorMessage("missingData");
            return this.stayOnCurrentPage;
        }
        java.lang.reflect.Method method;
        try {
            method = this.plugin.getClass().getMethod(GET_CURRENT_DOC_STRUCTS);
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (Objects.nonNull(list)) {
                return "/pages/MultiMassImportPage2";
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return massImportTwoPath;
    }

    /**
     * Get properties.
     *
     * @return list of ImportProperty objects
     */
    public List<ImportProperty> getProperties() {

        if (Objects.nonNull(this.plugin)) {
            return this.plugin.getProperties();
        }
        return new ArrayList<>();
    }

    public List<Process> getProcessList() {
        return this.processList;
    }

    public void setProcessList(List<Process> processList) {
        this.processList = processList;
    }

    public List<String> getUsablePluginsForFolder() {
        return this.usablePluginsForFolder;
    }

    public void setUsablePluginsForFolder(List<String> usablePluginsForFolder) {
        this.usablePluginsForFolder = usablePluginsForFolder;
    }

    /**
     * Download docket.
     */
    public void downloadDocket() {
        try {
            ServiceManager.getProcessService().downloadDocket(this.processList);
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Get document structure.
     *
     * @return list of DocstructElement objects
     */
    public List<? extends DocstructElement> getDocstructs() {
        java.lang.reflect.Method method;
        try {
            method = this.plugin.getClass().getMethod(GET_CURRENT_DOC_STRUCTS);
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (Objects.nonNull(list)) {
                return list;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return new ArrayList<>();
    }

    public int getDocstructssize() {
        return getDocstructs().size();
    }

    public String getInclude() {
        return "plugins/" + plugin.getTitle() + ".jsp";
    }

    /**
     * Get project.
     *
     * @return value of project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Set project.
     *
     * @param project as Project
     */
    public void setProject(Project project) {
        this.project = project;
    }
}

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

import com.lowagie.text.DocumentException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Batch;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Role;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.Workflow;
import org.kitodo.data.database.enums.PropertyType;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.export.ExportDms;
import org.kitodo.export.ExportMets;
import org.kitodo.production.dto.ProcessDTO;
import org.kitodo.production.enums.ObjectType;
import org.kitodo.production.exporter.ExportXmlLog;
import org.kitodo.production.helper.CustomListColumnInitializer;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.SelectItemList;
import org.kitodo.production.helper.WebDav;
import org.kitodo.production.model.LazyDTOModel;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.command.KitodoScriptService;
import org.kitodo.production.services.data.ProcessService;
import org.kitodo.production.services.file.FileService;
import org.kitodo.production.services.workflow.WorkflowControllerService;
import org.primefaces.model.SortOrder;

@Named("ProcessForm")
@SessionScoped
public class ProcessForm extends TemplateBaseForm {
    private static final Logger logger = LogManager.getLogger(ProcessForm.class);
    private Process process = new Process();
    private Task task = new Task();
    private Property templateProperty;
    private Property workpieceProperty;
    private String kitodoScriptSelection;
    private String kitodoScriptPage;
    private String kitodoScriptAll;
    private String newProcessTitle;
    private boolean showClosedProcesses = false;
    private boolean showInactiveProjects = false;
    private List<Property> properties;
    private List<Property> templates;
    private List<Property> workpieces;
    private Property property;
    private transient FileService fileService = ServiceManager.getFileService();
    private transient WorkflowControllerService workflowControllerService = ServiceManager.getWorkflowControllerService();
    private String doneDirectoryName;
    private static final String ERROR_CREATING = "errorCreating";
    private static final String EXPORT_FINISHED = "exportFinished";
    private List selectedProcesses = new ArrayList<>();
    final String processListPath = MessageFormat.format(REDIRECT_PATH, "processes");
    private final String processEditPath = MessageFormat.format(REDIRECT_PATH, "processEdit");

    private String processEditReferer = DEFAULT_LINK;
    private String taskEditReferer = DEFAULT_LINK;

    private List<SelectItem> customColumns;

    @Inject
    private CustomListColumnInitializer initializer;

    /**
     * Constructor.
     */
    public ProcessForm() {
        super();
        super.setLazyDTOModel(new LazyDTOModel(ServiceManager.getProcessService()));
        doneDirectoryName = ConfigCore.getParameterOrDefaultValue(ParameterCore.DONE_DIRECTORY_NAME);
    }

    /**
     * Initialize SelectItems used for configuring displayed columns in process
     * list.
     */
    @PostConstruct
    public void init() {
        columns = new ArrayList<>();

        SelectItemGroup processColumnGroup;
        try {
            processColumnGroup = ServiceManager.getListColumnService()
                    .getListColumnsForListAsSelectItemGroup("process");
            columns.add(processColumnGroup);
        } catch (DAOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage());
        }

        // Read process properties to display from configuration
        customColumns = new ArrayList<>();
        SelectItemGroup customColumnGroup = new SelectItemGroup(Helper.getTranslation("process"));
        customColumnGroup.setSelectItems(ServiceManager.getListColumnService().getAllCustomListColumns().stream()
                .map(listColumn -> new SelectItem(listColumn, listColumn.getTitle())).toArray(SelectItem[]::new));
        customColumns.add(customColumnGroup);

        selectedColumns =
                ServiceManager.getListColumnService().getSelectedListColumnsForListAndClient("process");
    }

    /**
     * Return list of process properties configured as custom list columns in kitodo
     * configuration.
     * 
     * @return list of process property names
     */
    public String[] getProcessPropertyNames() {
        return initializer.getProcessProperties();
    }

    /**
     * Retrieve and return process property value of property with given name
     * 'propertyName' from given ProcessDTO 'process'.
     * 
     * @param process
     *            the ProcessDTO object from which the property value is retrieved
     * @param propertyName
     *            name of the property for the property value is retrieved
     * @return property value if process has property with name 'propertyName',
     *         empty String otherwise
     */
    public static String getPropertyValue(ProcessDTO process, String propertyName) {
        return ProcessService.getPropertyValue(process, propertyName);
    }

    /**
     * Calculate and return age of given process as a String.
     *
     * @param processDTO
     *            ProcessDTO object whose duration/age is calculated
     * @return process age of given process
     */
    public static String getProcessDuration(ProcessDTO processDTO) {
        return ProcessService.getProcessDuration(processDTO);
    }

    /**
     * Save process and redirect to list view.
     *
     * @return url to list view
     */
    public String save() {
        /*
         * wenn der Vorgangstitel geändert wurde, wird dieser geprüft und bei
         * erfolgreicher Prüfung an allen relevanten Stellen mitgeändert
         */
        if (Objects.nonNull(this.process) && Objects.nonNull(this.process.getTitle())) {
            if (!this.process.getTitle().equals(this.newProcessTitle) && Objects.nonNull(this.newProcessTitle)
                    && !renameAfterProcessTitleChanged()) {
                return this.stayOnCurrentPage;
            }

            try {
                ServiceManager.getProcessService().save(this.process);
                return processListPath;
            } catch (DataException e) {
                Helper.setErrorMessage(ERROR_SAVING, new Object[] {ObjectType.PROCESS.getTranslationSingular() },
                    logger, e);
            }
        } else {
            Helper.setErrorMessage("titleEmpty");
        }
        reload();
        return this.stayOnCurrentPage;
    }

    /**
     * Delete process.
     */
    public void delete() {
        deleteMetadataDirectory();
        try {
            this.process.getProject().getProcesses().remove(this.process);
            this.process.setProject(null);
            this.process.getTemplate().getProcesses().remove(this.process);
            this.process.setTemplate(null);
            List<Batch> batches = new CopyOnWriteArrayList<>(process.getBatches());
            for (Batch batch : batches) {
                batch.getProcesses().remove(this.process);
                this.process.getBatches().remove(batch);
                ServiceManager.getBatchService().save(batch);
            }
            ServiceManager.getProcessService().remove(this.process);
        } catch (DataException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_DELETING, new Object[] {ObjectType.PROCESS.getTranslationSingular() }, logger,
                e);
        }
    }

    /**
     * Get diagram image for current template.
     *
     * @return diagram image file
     */
    public InputStream getTasksDiagram() {
        Workflow workflow = this.process.getTemplate().getWorkflow();
        if (Objects.nonNull(workflow)) {
            return ServiceManager.getTemplateService().getTasksDiagram(workflow.getTitle());
        }
        return ServiceManager.getTemplateService().getTasksDiagram("");
    }

    /**
     * Get translation of task status title.
     *
     * @param taskStatusTitle
     *            'statusDone', 'statusLocked' and so on
     * @return translated message for given task status title
     */
    public String getTaskStatusTitle(String taskStatusTitle) {
        return Helper.getTranslation(taskStatusTitle);
    }

    /**
     * Remove content.
     *
     * @return String
     */
    public String deleteContent() {
        try {
            URI ocr = fileService.getOcrDirectory(this.process);
            if (fileService.fileExist(ocr)) {
                fileService.delete(ocr);
            }
            URI images = fileService.getImagesDirectory(this.process);
            if (fileService.fileExist(images)) {
                fileService.delete(images);
            }
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage("errorDirectoryDeleting", new Object[] {Helper.getTranslation("metadata") }, logger,
                e);
        }

        Helper.setMessage("Content deleted");
        return this.stayOnCurrentPage;
    }

    private boolean renameAfterProcessTitleChanged() {
        String validateRegEx = ConfigCore.getParameterOrDefaultValue(ParameterCore.VALIDATE_PROCESS_TITLE_REGEX);
        if (!this.newProcessTitle.matches(validateRegEx)) {
            Helper.setErrorMessage("processTitleInvalid", new Object[] {validateRegEx });
            return false;
        } else {
            renamePropertiesValuesForProcessTitle(this.process.getProperties());
            renamePropertiesValuesForProcessTitle(this.process.getTemplates());
            removePropertiesWithEmptyTitle(this.process.getWorkpieces());

            try {
                renameImageDirectories();
                renameOcrDirectories();
                renameDefinedDirectories();
            } catch (IOException | RuntimeException e) {
                Helper.setErrorMessage("errorRenaming", new Object[] {Helper.getTranslation("directory") }, logger, e);
            }

            this.process.setTitle(this.newProcessTitle);

            // remove Tiffwriter file
            KitodoScriptService gs = new KitodoScriptService();
            List<Process> pro = new ArrayList<>();
            pro.add(this.process);
            gs.deleteTiffHeaderFile(pro);
        }
        return true;
    }

    private void renamePropertiesValuesForProcessTitle(List<Property> properties) {
        for (Property property : properties) {
            if (Objects.nonNull(property.getValue()) && property.getValue().contains(this.process.getTitle())) {
                property.setValue(property.getValue().replaceAll(this.process.getTitle(), this.newProcessTitle));
            }
        }
    }

    private void renameImageDirectories() throws IOException {
        URI imageDirectory = fileService.getImagesDirectory(process);
        renameDirectories(imageDirectory);
    }

    private void renameOcrDirectories() throws IOException {
        URI ocrDirectory = fileService.getOcrDirectory(process);
        renameDirectories(ocrDirectory);
    }

    private void renameDirectories(URI directory) throws IOException {
        if (fileService.isDirectory(directory)) {
            List<URI> subDirs = fileService.getSubUris(directory);
            for (URI imageDir : subDirs) {
                if (fileService.isDirectory(imageDir)) {
                    fileService.renameFile(imageDir,
                        fileService.getFileName(imageDir).replace(process.getTitle(), newProcessTitle));
                }
            }
        }
    }

    private void renameDefinedDirectories() {
        String[] processDirs = ConfigCore.getStringArrayParameter(ParameterCore.PROCESS_DIRS);
        for (String processDir : processDirs) {
            // TODO: check it out
            URI processDirAbsolute = ServiceManager.getProcessService().getProcessDataDirectory(process)
                    .resolve(processDir.replace("(processtitle)", process.getTitle()));

            File dir = new File(processDirAbsolute);
            boolean renamed;
            if (dir.isDirectory()) {
                renamed = dir.renameTo(new File(dir.getAbsolutePath().replace(process.getTitle(), newProcessTitle)));
                if (!renamed) {
                    Helper.setErrorMessage("errorRenaming", new Object[] {dir.getName() });
                }
            }
        }
    }

    private void deleteMetadataDirectory() {
        for (Task task : this.process.getTasks()) {
            this.task = task;
            deleteSymlinksFromUserHomes();
        }
        try {
            fileService.delete(ServiceManager.getProcessService().getProcessDataDirectory(this.process));
            URI ocrDirectory = fileService.getOcrDirectory(this.process);
            if (fileService.fileExist(ocrDirectory)) {
                fileService.delete(ocrDirectory);
            }
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage("errorDirectoryDeleting", new Object[] {Helper.getTranslation("metadata") }, logger,
                e);
        }
    }

    /**
     * Remove template properties.
     */
    public void deleteTemplateProperty() {
        this.templateProperty.getProcesses().clear();
        this.process.getTemplates().remove(this.templateProperty);
        loadTemplateProperties();
    }

    /**
     * Remove workpiece properties.
     */
    public void deleteWorkpieceProperty() {
        this.workpieceProperty.getProcesses().clear();
        this.process.getWorkpieces().remove(this.workpieceProperty);
        loadWorkpieceProperties();
    }

    /**
     * Create new template property.
     */
    public void createTemplateProperty() {
        if (Objects.isNull(this.templates)) {
            this.templates = new ArrayList<>();
        }
        Property newProperty = new Property();
        newProperty.setDataType(PropertyType.STRING);
        this.templates.add(newProperty);
        this.templateProperty = newProperty;
    }

    /**
     * Create new workpiece property.
     */
    public void createWorkpieceProperty() {
        if (Objects.isNull(this.workpieces)) {
            this.workpieces = new ArrayList<>();
        }
        Property newProperty = new Property();
        newProperty.setDataType(PropertyType.STRING);
        this.workpieces.add(newProperty);
        this.workpieceProperty = newProperty;
    }

    /**
     * Save template property.
     */
    public void saveTemplateProperty() {
        if (!this.process.getTemplates().contains(this.templateProperty)) {
            this.process.getTemplates().add(this.templateProperty);
        }
        loadTemplateProperties();
    }

    /**
     * Save workpiece property.
     */
    public void saveWorkpieceProperty() {
        if (!this.process.getWorkpieces().contains(this.workpieceProperty)) {
            this.process.getWorkpieces().add(this.workpieceProperty);
        }
        loadWorkpieceProperties();
    }

    /**
     * Save task and redirect to processEdit view.
     *
     * @return url to processEdit view
     */
    public String saveTaskAndRedirect() {
        saveTask(this.task, this.process, ObjectType.PROCESS.getTranslationSingular(), ServiceManager.getTaskService());
        return processEditPath + "&id=" + (Objects.isNull(this.process.getId()) ? 0 : this.process.getId());
    }

    /**
     * Remove task.
     */
    public void removeTask() {
        this.process.getTasks().remove(this.task);

        List<Role> roles = this.task.getRoles();
        for (Role role : roles) {
            role.getTasks().remove(this.task);
        }
        deleteSymlinksFromUserHomes();
    }

    private void deleteSymlinksFromUserHomes() {
        WebDav webDav = new WebDav();

        for (Role role : this.task.getRoles()) {
            for (User user : role.getUsers()) {
                try {
                    webDav.uploadFromHome(user, this.task.getProcess());
                } catch (RuntimeException e) {
                    Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                }
            }
        }
    }

    /**
     * Remove role from the task.
     *
     * @return stay on the same page
     */
    public String deleteRole() {
        try {
            int roleId = Integer.parseInt(Helper.getRequestParameter("ID"));
            for (Role role : this.task.getRoles()) {
                if (role.getId().equals(roleId)) {
                    this.task.getRoles().remove(role);
                }
            }
        } catch (NumberFormatException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return this.stayOnCurrentPage;
    }

    /**
     * Add role to the task.
     *
     * @return stay on the same page
     */
    public String addRole() {
        int roleId = 0;
        try {
            roleId = Integer.parseInt(Helper.getRequestParameter("ID"));
            Role role = ServiceManager.getRoleService().getById(roleId);

            if (!this.task.getRoles().contains(role)) {
                this.task.getRoles().add(role);
            }
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                new Object[] {ObjectType.ROLE.getTranslationSingular(), roleId }, logger, e);
        } catch (NumberFormatException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return this.stayOnCurrentPage;
    }

    /**
     * Export METS.
     */
    public void exportMets() {
        ExportMets export = new ExportMets();
        try {
            export.startExport(this.process);
            Helper.setMessage(EXPORT_FINISHED);
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage("An error occurred while trying to export METS file for: " + this.process.getTitle(),
                logger, e);
        }
    }

    /**
     * Export PDF.
     */
    public void exportPdf() {
        Helper.setErrorMessage("Not implemented");
    }

    /**
     * Export DMS.
     */
    public void exportDMS() {
        ExportDms export = new ExportDms();
        try {
            export.startExport(this.process);
            Helper.setMessage(EXPORT_FINISHED);
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_EXPORTING,
                new Object[] {ObjectType.PROCESS.getTranslationSingular(), this.process.getId() }, logger, e);
        }
    }

    /**
     * Export DMS for selected processes.
     */
    public void exportDMSForSelection() {
        exportDMSForProcesses(this.selectedProcesses);
    }

    /**
     * Export DMS processes on the page.
     */
    @SuppressWarnings("unchecked")
    public void exportDMSForPage() {
        exportDMSForProcesses(lazyDTOModel.getEntities());
    }

    /**
     * Export DMS for all found processes.
     */
    @SuppressWarnings("unchecked")
    public void exportDMSForAll() {
        //TODO: find a way to pass filters
        exportDMSForProcesses(lazyDTOModel.load(0, 100000, "", SortOrder.ASCENDING, null));
    }

    private void exportDMSForProcesses(List<ProcessDTO> processes) {
        ExportDms export = new ExportDms();
        for (ProcessDTO processToExport : processes) {
            try {
                Process processBean = ServiceManager.getProcessService().getById(processToExport.getId());
                export.startExport(processBean);
                Helper.setMessage(EXPORT_FINISHED);
            } catch (DAOException e) {
                Helper.setErrorMessage(ERROR_LOADING_ONE,
                    new Object[] {ObjectType.PROCESS.getTranslationSingular(), processToExport.getId() }, logger, e);
            } catch (IOException | RuntimeException e) {
                Helper.setErrorMessage(ERROR_EXPORTING,
                    new Object[] {ObjectType.PROCESS.getTranslationSingular(), processToExport.getId() }, logger, e);
            }
        }
    }

    /**
     * Upload all processes from home.
     */
    public void uploadFromHomeForAll() {
        WebDav myDav = new WebDav();
        List<URI> folder = myDav.uploadAllFromHome(doneDirectoryName);
        myDav.removeAllFromHome(folder, URI.create(doneDirectoryName));
        Helper.setMessage("directoryRemovedAll", doneDirectoryName);
    }

    /**
     * Upload from home for single process.
     */
    public void uploadFromHome() {
        WebDav myDav = new WebDav();
        myDav.uploadFromHome(this.process);
        Helper.setMessage("directoryRemoved", this.process.getTitle());
    }

    /**
     * Download to home for single process.
     */
    public void downloadToHome() {
        /*
         * zunächst prüfen, ob dieser Band gerade von einem anderen Nutzer in
         * Bearbeitung ist und in dessen Homeverzeichnis abgelegt wurde,
         * ansonsten Download
         */
        if (!ServiceManager.getProcessService().isImageFolderInUse(this.process)) {
            WebDav myDav = new WebDav();
            myDav.downloadToHome(this.process, false);
        } else {
            Helper.setMessage(
                Helper.getTranslation("directory ") + " " + this.process.getTitle() + " "
                        + Helper.getTranslation("isInUse"),
                ServiceManager.getUserService()
                        .getFullName(ServiceManager.getProcessService().getImageFolderInUseUser(this.process)));
            WebDav myDav = new WebDav();
            myDav.downloadToHome(this.process, true);
        }
    }

    /**
     * Download to home for selected processes.
     */
    @SuppressWarnings("unchecked")
    public void downloadToHomeForSelection() {
        WebDav myDav = new WebDav();
        for (ProcessDTO processDTO : (List<ProcessDTO>) getSelectedProcesses()) {
            download(myDav, processDTO);
        }
        // TODO: fix message
        Helper.setMessage("createdInUserHomeAll");
    }

    /**
     * Download to home for all process on the page.
     */
    @SuppressWarnings("unchecked")
    public void downloadToHomeForPage() {
        WebDav webDav = new WebDav();
        for (ProcessDTO processForWebDav : (List<ProcessDTO>) lazyDTOModel.getEntities()) {
            download(webDav, processForWebDav);
        }
        Helper.setMessage("createdInUserHome");
    }

    /**
     * Download to home for all found processes.
     */
    @SuppressWarnings("unchecked")
    public void downloadToHomeForAll() {
        WebDav webDav = new WebDav();
        for (ProcessDTO processDTO : (List<ProcessDTO>) lazyDTOModel.load(0, 100000, "", SortOrder.ASCENDING, null)) {
            download(webDav, processDTO);
        }
        Helper.setMessage("createdInUserHomeAll");
    }

    private void download(WebDav webDav, ProcessDTO processDTO) {
        try {
            Process processForDownload = ServiceManager.getProcessService().getById(processDTO.getId());
            if (!ServiceManager.getProcessService().isImageFolderInUse(processDTO)) {
                webDav.downloadToHome(processForDownload, false);
            } else {
                Helper.setMessage(
                    Helper.getTranslation("directory ") + " " + processDTO.getTitle() + " "
                            + Helper.getTranslation("isInUse"),
                    ServiceManager.getUserService().getFullName(
                        ServiceManager.getProcessService().getImageFolderInUseUser(processForDownload)));
                webDav.downloadToHome(processForDownload, true);
            }
        } catch (DAOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Set up processing status page.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusUpForPage() {
        setTaskStatusUpForProcesses(lazyDTOModel.getEntities());
    }

    /**
     * Set up processing status selection.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusUpForSelection() {
        setTaskStatusUpForProcesses(getSelectedProcesses());
    }

    /**
     * Set up processing status for all found processes.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusUpForAll() {
        setTaskStatusUpForProcesses(lazyDTOModel.load(0, 100000, "", SortOrder.ASCENDING, null));
    }

    private void setTaskStatusUpForProcesses(List<ProcessDTO> processes) {
        for (ProcessDTO processForStatus : processes) {
            try {
                Process processBean = ServiceManager.getProcessService().getById(processForStatus.getId());
                workflowControllerService.setTasksStatusUp(processBean);
                ServiceManager.getProcessService().save(processBean);
            } catch (DAOException | DataException | IOException e) {
                Helper.setErrorMessage("errorChangeTaskStatus",
                    new Object[] {Helper.getTranslation("up"), processForStatus.getId() }, logger, e);
            }
        }
    }

    /**
     * Set down processing status page.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusDownForPage() {
        setTaskStatusDownForProcesses(lazyDTOModel.getEntities());
    }

    /**
     * Set down processing status selection.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusDownForSelection() {
        setTaskStatusDownForProcesses(getSelectedProcesses());
    }

    /**
     * Set down processing status hits.
     */
    @SuppressWarnings("unchecked")
    public void setTaskStatusDownForAll() {
        setTaskStatusDownForProcesses(lazyDTOModel.load(0, 100000, "", SortOrder.ASCENDING, null));
    }

    private void setTaskStatusDownForProcesses(List<ProcessDTO> processes) {
        for (ProcessDTO processForStatus : processes) {
            try {
                Process processBean = ServiceManager.getProcessService().getById(processForStatus.getId());
                workflowControllerService.setTasksStatusDown(processBean);
                ServiceManager.getProcessService().save(processBean);
            } catch (DAOException | DataException e) {
                Helper.setErrorMessage("errorChangeTaskStatus",
                    new Object[] {Helper.getTranslation("down"), processForStatus.getId() }, logger, e);
            }
        }
    }

    /**
     * Task status up.
     */
    public void setTaskStatusUp() throws DataException, IOException {
        workflowControllerService.setTaskStatusUp(this.task);
        save();
        deleteSymlinksFromUserHomes();
    }

    /**
     * Task status down.
     */
    public void setTaskStatusDown() {
        workflowControllerService.setTaskStatusDown(this.task);
        save();
        deleteSymlinksFromUserHomes();
    }

    /**
     * Get process object.
     *
     * @return process object
     */
    public Process getProcess() {
        return this.process;
    }

    /**
     * Set process by ID.
     *
     * @param processID
     *            ID of process to set.
     */
    public void setProcessByID(int processID) {
        try {
            setProcess(ServiceManager.getProcessService().getById(processID));
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_LOADING_ONE,
                new Object[] {ObjectType.PROCESS.getTranslationSingular(), processID }, logger, e);
        }
    }

    /**
     * Set process.
     *
     * @param process
     *            Process object
     */
    public void setProcess(Process process) {
        this.process = process;
        this.newProcessTitle = process.getTitle();
        loadProcessProperties();
        loadTemplateProperties();
        loadWorkpieceProperties();
    }

    /**
     * Get task object.
     *
     * @return Task object
     */
    public Task getTask() {
        return this.task;
    }

    /**
     * Set task.
     *
     * @param task
     *            Task object
     */
    public void setTask(Task task) {
        this.task = task;
        this.task.setLocalizedTitle(ServiceManager.getTaskService().getLocalizedTitle(task.getTitle()));
    }

    public Property getTemplateProperty() {
        return this.templateProperty;
    }

    public void setTemplateProperty(Property templateProperty) {
        this.templateProperty = templateProperty;
    }

    public Property getWorkpieceProperty() {
        return this.workpieceProperty;
    }

    public void setWorkpieceProperty(Property workpieceProperty) {
        this.workpieceProperty = workpieceProperty;
    }

    /**
     * Reload task and process.
     */
    private void reload() {
        reload(this.task, ObjectType.TASK.getTranslationSingular(), ServiceManager.getTaskService());
        reload(this.process, ObjectType.PROCESS.getTranslationSingular(), ServiceManager.getProcessService());
    }

    /**
     * Execute Kitodo script for hits list.
     */
    @SuppressWarnings("unchecked")
    public void executeKitodoScriptAll() {
        executeKitodoScriptForProcesses(lazyDTOModel.load(0, 100000, "", SortOrder.ASCENDING, null),
            this.kitodoScriptAll);
    }

    /**
     * Execute Kitodo script for processes displayed on the page.
     */
    @SuppressWarnings("unchecked")
    public void executeKitodoScriptPage() {
        executeKitodoScriptForProcesses(lazyDTOModel.getEntities(), this.kitodoScriptPage);
    }

    /**
     * Execute Kitodo script for selected processes.
     */
    @SuppressWarnings("unchecked")
    public void executeKitodoScriptSelection() {
        executeKitodoScriptForProcesses(getSelectedProcesses(), this.kitodoScriptSelection);
    }

    private void executeKitodoScriptForProcesses(List<ProcessDTO> processes, String kitodoScript) {
        KitodoScriptService service = new KitodoScriptService();
        try {
            service.execute(ServiceManager.getProcessService().convertDtosToBeans(processes), kitodoScript);
        } catch (DAOException | DataException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Get kitodo script for selected results.
     * 
     * @return kitodo script for selected results
     */
    public String getKitodoScriptSelection() {
        return this.kitodoScriptSelection;
    }

    /**
     * Set kitodo script for selected results.
     *
     * @param kitodoScriptSelection
     *            the kitodoScript
     */
    public void setKitodoScriptSelection(String kitodoScriptSelection) {
        this.kitodoScriptSelection = kitodoScriptSelection;
    }

    /**
     * Get kitodo script for all results.
     * 
     * @return kitodo script for all results
     */
    public String getKitodoScriptPage() {
        return this.kitodoScriptPage;
    }

    /**
     * Set kitodo script for page results.
     *
     * @param kitodoScriptPage
     *            the kitodoScript
     */
    public void setKitodoScriptPage(String kitodoScriptPage) {
        this.kitodoScriptPage = kitodoScriptPage;
    }

    /**
     * Get kitodo script for all results.
     * 
     * @return kitodo script for all results
     */
    public String getKitodoScriptAll() {
        return this.kitodoScriptAll;
    }

    /**
     * Set kitodo script for all results.
     *
     * @param kitodoScriptAll
     *            the kitodoScript
     */
    public void setKitodoScriptAll(String kitodoScriptAll) {
        this.kitodoScriptAll = kitodoScriptAll;
    }

    public String getNewProcessTitle() {
        return this.newProcessTitle;
    }

    public void setNewProcessTitle(String newProcessTitle) {
        this.newProcessTitle = newProcessTitle;
    }

    /**
     * Starts generation of xml logfile for current process.
     */
    public void createXML() {
        try {
            ExportXmlLog xmlExport = new ExportXmlLog();
            String directory = new File(ServiceManager.getUserService().getHomeDirectory(getUser())).getPath();
            String destination = directory + "/" + Helper.getNormalizedTitle(this.process.getTitle()) + "_log.xml";
            xmlExport.startExport(this.process, destination);
        } catch (IOException e) {
            Helper.setErrorMessage("Error creating log file in home directory", logger, e);
        }
    }

    /**
     * Downloads a docket for process.
     */
    public void downloadDocket() {
        try {
            ServiceManager.getProcessService().downloadDocket(this.process);
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Generate result as PDF.
     */
    public void generateResultAsPdf() {
        try {
            ServiceManager.getProcessService().generateResultAsPdf(this.filter);
        } catch (IOException | DocumentException e) {
            Helper.setErrorMessage(ERROR_CREATING, new Object[] {Helper.getTranslation("resultPDF") }, logger, e);
        }
    }

    /**
     * Generate result set.
     */
    public void generateResult() {
        try {
            ServiceManager.getProcessService().generateResult(this.filter);
        } catch (IOException e) {
            Helper.setErrorMessage(ERROR_CREATING, new Object[] {Helper.getTranslation("resultSet") }, logger, e);
        }
    }

    /**
     * Return whether closed processes should be displayed or not.
     *
     * @return parameter controlling whether closed processes should be displayed or
     *         not
     */
    public boolean isShowClosedProcesses() {
        return this.showClosedProcesses;
    }

    /**
     * Set whether closed processes should be displayed or not.
     *
     * @param showClosedProcesses
     *            boolean flag signaling whether closed processes should be
     *            displayed or not
     */
    public void setShowClosedProcesses(boolean showClosedProcesses) {
        this.showClosedProcesses = showClosedProcesses;
        ServiceManager.getProcessService().setShowClosedProcesses(showClosedProcesses);
    }

    /**
     * Set whether inactive projects should be displayed or not.
     *
     * @param showInactiveProjects
     *            boolean flag signaling whether inactive projects should be
     *            displayed or not
     */
    public void setShowInactiveProjects(boolean showInactiveProjects) {
        this.showInactiveProjects = showInactiveProjects;
        ServiceManager.getProcessService().setShowInactiveProjects(showInactiveProjects);
    }

    /**
     * Return whether inactive projects should be displayed or not.
     *
     * @return parameter controlling whether inactive projects should be displayed
     *         or not
     */
    public boolean isShowInactiveProjects() {
        return this.showInactiveProjects;
    }

    /**
     * Get property for process.
     *
     * @return property for process
     */
    public Property getProperty() {
        return this.property;
    }

    /**
     * Set property for process.
     *
     * @param property
     *            for process as Property object
     */
    public void setProperty(Property property) {
        this.property = property;
    }

    /**
     * Get list of properties for process.
     *
     * @return list of process properties
     */
    public List<Property> getProperties() {
        return this.properties;
    }

    /**
     * Set list of properties for process.
     *
     * @param properties
     *            for process as Property objects
     */
    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    /**
     * Get list of templates for process.
     *
     * @return list of templates for process
     */
    public List<Property> getTemplates() {
        return this.templates;
    }

    /**
     * Set list of templates for process.
     *
     * @param templates
     *            for process as Property objects
     */
    public void setTemplates(List<Property> templates) {
        this.templates = templates;
    }

    /**
     * Get list of workpieces for process.
     *
     * @return list of workpieces for process
     */
    public List<Property> getWorkpieces() {
        return this.workpieces;
    }

    /**
     * Set list of workpieces for process.
     *
     * @param workpieces
     *            for process as Property objects
     */
    public void setWorkpieces(List<Property> workpieces) {
        this.workpieces = workpieces;
    }

    private void loadProcessProperties() {
        this.properties = this.process.getProperties();
    }

    private void loadTemplateProperties() {
        this.templates = this.process.getTemplates();
    }

    private void loadWorkpieceProperties() {
        this.workpieces = this.process.getWorkpieces();
    }

    /**
     * Create new property.
     */
    public void createNewProperty() {
        if (Objects.isNull(this.properties)) {
            this.properties = new ArrayList<>();
        }
        Property newProperty = new Property();
        newProperty.setDataType(PropertyType.STRING);
        this.properties.add(newProperty);
        this.property = newProperty;
    }

    /**
     * Save current property.
     */
    public void saveCurrentProperty() {
        if (!this.process.getProperties().contains(this.property)) {
            this.process.getProperties().add(this.property);
        }
        loadProcessProperties();
    }

    /**
     * Delete property.
     */
    public void deleteProperty() {
        this.property.getProcesses().clear();
        this.process.getProperties().remove(this.property);

        List<Property> propertiesToFilterTitle = this.process.getProperties();
        removePropertiesWithEmptyTitle(propertiesToFilterTitle);
        loadProcessProperties();
    }

    /**
     * Duplicate property.
     */
    public void duplicateProperty() {
        Property newProperty = ServiceManager.getPropertyService().transfer(this.property);
        newProperty.getProcesses().add(this.process);
        this.process.getProperties().add(newProperty);
        loadProcessProperties();
    }

    // TODO: is it really a case that title is empty?
    private void removePropertiesWithEmptyTitle(List<Property> properties) {
        for (Property processProperty : properties) {
            if (Objects.isNull(processProperty.getTitle()) || processProperty.getTitle().isEmpty()) {
                processProperty.getProcesses().clear();
                this.process.getProperties().remove(processProperty);
            }
        }
    }

    /**
     * Get dockets for select list.
     *
     * @return list of dockets as SelectItem objects
     */
    public List<SelectItem> getDockets() {
        return SelectItemList.getDockets(ServiceManager.getDocketService().getAllForSelectedClient());
    }

    /**
     * Get list of projects.
     *
     * @return list of projects as SelectItem objects
     */
    public List<SelectItem> getProjects() {
        return SelectItemList.getProjects(ServiceManager.getProjectService().getAllForSelectedClient());
    }

    /**
     * Get rulesets for select list.
     *
     * @return list of rulesets as SelectItem objects
     */
    public List<SelectItem> getRulesets() {
        return SelectItemList.getRulesets(ServiceManager.getRulesetService().getAllForSelectedClient());
    }

    /**
     * Get task statuses for select list.
     *
     * @return list of task statuses as SelectItem objects
     */
    public List<SelectItem> getTaskStatuses() {
        return SelectItemList.getTaskStatuses();
    }

    /**
     * Method being used as viewAction for process edit form. If the given parameter
     * 'id' is '0', the form for creating a new process will be displayed.
     *
     * @param id
     *            ID of the process to load
     */
    public void load(int id) {
        try {
            if (id != 0) {
                setProcess(ServiceManager.getProcessService().getById(id));
            }
            setSaveDisabled(true);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_LOADING_ONE, new Object[] {ObjectType.PROCESS.getTranslationSingular(), id },
                logger, e);
        }
    }

    /**
     * Method being used as viewAction for task form.
     */
    public void loadTask(int id) {
        try {
            if (id != 0) {
                setTask(ServiceManager.getTaskService().getById(id));
            }
            setSaveDisabled(true);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_LOADING_ONE, new Object[] {ObjectType.TASK.getTranslationSingular(), id },
                logger, e);
        }
    }

    /**
     * Returns selected processDTO. This method should return ProcessDTO objects,
     * but for some unkown reason it returns Process beans out of nowhere.
     *
     * @return The list of processDTO.
     */
    @SuppressWarnings("unchecked")
    public List getSelectedProcesses() {
        if (!this.selectedProcesses.isEmpty()) {
            if (this.selectedProcesses.get(0) instanceof ProcessDTO) {
                return this.selectedProcesses;
            } else if (this.selectedProcesses.get(0) instanceof Process) {
                List<Process> magicSelectedProcesses = new ArrayList<>(this.selectedProcesses);
                this.selectedProcesses.clear();
                for (Process magicSelectedProcess : magicSelectedProcesses) {
                    try {
                        this.selectedProcesses
                                .add(ServiceManager.getProcessService().findById(magicSelectedProcess.getId()));
                    } catch (DataException e) {
                        Helper.setErrorMessage(ERROR_LOADING_ONE,
                            new Object[] {ObjectType.PROCESS.getTranslationSingular(), magicSelectedProcess.getId() },
                            logger, e);
                    }
                }

            }
        }
        return selectedProcesses;
    }

    /**
     * Sets selected processDTOs.
     *
     * @param selectedProcesses
     *            The list of ProcessDTOs.
     */
    public void setSelectedProcesses(List selectedProcesses) {
        this.selectedProcesses = selectedProcesses;
    }

    /**
     * Set referring view which will be returned when the user clicks "save" or
     * "cancel" on the task edit page.
     *
     * @param referer
     *            the referring view
     */
    public void setTaskEditReferer(String referer) {
        if (referer.equals("processEdit?id=" + this.task.getProcess().getId())) {
            this.taskEditReferer = referer;
        } else {
            this.taskEditReferer = DEFAULT_LINK;
        }
    }

    /**
     * Get task edit page referring view.
     *
     * @return task eit page referring view
     */
    public String getTaskEditReferer() {
        return this.taskEditReferer;
    }

    /**
     * Set referring view which will be returned when the user clicks "save" or
     * "cancel" on the process edit page.
     *
     * @param referer
     *            the referring view
     */
    public void setProcessEditReferer(String referer) {
        if (!referer.isEmpty()) {
            if (referer.equals("processes")) {
                this.processEditReferer = referer;
            } else if (!referer.contains("taskEdit") || this.processEditReferer.isEmpty()) {
                this.processEditReferer = DEFAULT_LINK;
            }
        }
    }

    /**
     * Get process edit page referring view.
     *
     * @return process edit page referring view
     */
    public String getProcessEditReferer() {
        return this.processEditReferer;
    }
}

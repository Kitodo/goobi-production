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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.json.JsonException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.exceptions.ProjectDeletionException;
import org.kitodo.production.dto.ProcessDTO;
import org.kitodo.production.dto.ProjectDTO;
import org.kitodo.production.dto.TaskDTO;
import org.kitodo.production.enums.ObjectType;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.WebDav;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.data.ProcessService;
import org.kitodo.production.services.data.ProjectService;
import org.primefaces.model.SortOrder;

@Named("DesktopForm")
@ViewScoped
public class DesktopForm extends BaseForm {
    private static final Logger logger = LogManager.getLogger(DesktopForm.class);

    /**
     * Used to determine if a string is a URL.
     */
    private static final Pattern SCHEME_TEST = Pattern.compile("[A-Za-z][+\\-\\.0-9A-Za-z]*:[^ ]+");

    private static final String SORT_TITLE = "title";
    private static final String SORT_ID = "properties.id";
    private List<TaskDTO> taskList = new ArrayList<>();
    private List<ProcessDTO> processList = new ArrayList<>();
    private List<ProjectDTO> projectList = new ArrayList<>();

    /**
     * Default constructor.
     */
    public DesktopForm() {
        super();
    }

    /**
     * Returns the link location of the help file. If the link is local
     * absolute, i.e. begins with a slash, or is a URL, i.e. begins with a
     * scheme (e.g. "http"), it is returned as-is. Otherwise, a link relative to
     * the context path of the web application is returned.
     *
     * @return link location of the help
     */
    public String getHelpLocation() {
        String helpLocation = ConfigCore.getParameter(ParameterCore.HELP_LOCATION,
            (String) ParameterCore.HELP_LOCATION.getParameter().getDefaultValue());
        if (helpLocation.startsWith("/") || SCHEME_TEST.matcher(helpLocation).matches()) {
            return helpLocation;
        } else {
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            return contextPath + '/' + helpLocation;
        }
    }

    /**
     * Get values of ObjectType enum.
     *
     * @return array containing values of ObjectType enum
     */
    public List<ObjectType> getObjectTypes() {
        ArrayList<ObjectType> objectTypes = new ArrayList<>();
        objectTypes.add(ObjectType.TASK);
        objectTypes.add(ObjectType.USER);
        objectTypes.add(ObjectType.PROCESS);
        objectTypes.add(ObjectType.DOCKET);
        objectTypes.add(ObjectType.PROJECT);
        objectTypes.add(ObjectType.RULESET);
        objectTypes.add(ObjectType.TEMPLATE);
        objectTypes.add(ObjectType.ROLE);
        objectTypes.add(ObjectType.WORKFLOW);
        return objectTypes;
    }

    /**
     * Get tasks.
     *
     * @return task list
     */
    public List<TaskDTO> getTasks() {
        try {
            if (ServiceManager.getSecurityAccessService().hasAuthorityToViewTaskList() && taskList.isEmpty()) {
                taskList = ServiceManager.getTaskService().loadData(0, 10, SORT_TITLE, SortOrder.ASCENDING, new HashMap<>());
            }
        } catch (DataException | JsonException e) {
            Helper.setErrorMessage(ERROR_LOADING_MANY, new Object[] {ObjectType.TASK.getTranslationPlural() }, logger,
                e);
        }
        return taskList;
    }

    /**
     * Get processes.
     *
     * @return process list
     */
    public List<ProcessDTO> getProcesses() {
        try {
            if (ServiceManager.getSecurityAccessService().hasAuthorityToViewProcessList() && processList.isEmpty()) {
                processList = ServiceManager.getProcessService().loadData(0, 10, SORT_ID, SortOrder.DESCENDING, null);
            }
        } catch (DataException | JsonException e) {
            Helper.setErrorMessage(ERROR_LOADING_MANY, new Object[] {ObjectType.PROCESS.getTranslationPlural() },
                logger, e);
        }
        return processList;
    }

    /**
     * Get projects.
     *
     * @return project list
     */
    public List<ProjectDTO> getProjects() {
        try {
            if (ServiceManager.getSecurityAccessService().hasAuthorityToViewProjectList() && projectList.isEmpty()) {
                projectList = ServiceManager.getProjectService().loadData(0, 10, SORT_TITLE, SortOrder.ASCENDING, null);
            }
        } catch (DataException | JsonException e) {
            Helper.setErrorMessage(ERROR_LOADING_MANY, new Object[] {ObjectType.PROJECT.getTranslationPlural() },
                logger, e);
        }
        return projectList;
    }

    /**
     * Delete given Process 'process'.
     *
     * @param processID ID of Process to delete
     */
    public void deleteProcess(int processID) {
        try {
            ProcessService.deleteProcess(processID);
            emptyCache();
        } catch (DataException | DAOException | IOException e) {
            Helper.setErrorMessage(ERROR_DELETING, new Object[] {ObjectType.PROCESS.getTranslationSingular() },
                    logger, e);
        }
    }

    /**
     * Delete project by ID.
     *
     * @param projectID ID of project to be deleted
     */
    public void deleteProject(int projectID) {
        try {
            ProjectService.delete(projectID);
            emptyCache();
        } catch (DataException | DAOException e) {
            Helper.setErrorMessage(ERROR_DELETING, new Object[] {ObjectType.PROJECT.getTranslationSingular() }, logger,
                    e);
        } catch (ProjectDeletionException e) {
            Helper.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Export METS.
     */
    public void exportMets(int processId) {
        try {
            ProcessService.exportMets(processId);
            Helper.setMessage(EXPORT_FINISHED);
        } catch (IOException | DAOException e) {
            Helper.setErrorMessage("An error occurred while trying to export METS file for process "
                    + processId, logger, e);
        }
    }

    /**
     * Download to home for single process. First check if this volume is currently
     * being edited by another user and placed in his home directory, otherwise
     * download.
     */
    public void downloadToHome(int processId) {
        try {
            ProcessService.downloadToHome(new WebDav(), processId);
        } catch (DAOException e) {
            Helper.setErrorMessage("Error downloading process " + processId + " to home directory!");
        }
    }

    /**
     * Get number of elements of given type 'objectType' in index.
     *
     * @param objectType
     *            type of elements
     * @return number of elements
     */
    public long getNumberOfElements(ObjectType objectType) {
        try {
            switch (objectType) {
                case TASK:
                    return ServiceManager.getTaskService().countResults(null);
                case USER:
                    return ServiceManager.getUserService().countResults(null);
                case DOCKET:
                    return ServiceManager.getDocketService().countResults(null);
                case PROCESS:
                    return ServiceManager.getProcessService().countResults(null);
                case PROJECT:
                    return ServiceManager.getProjectService().countResults(null);
                case RULESET:
                    return ServiceManager.getRulesetService().countResults(null);
                case TEMPLATE:
                    return ServiceManager.getTemplateService().countResults(null);
                case ROLE:
                    return ServiceManager.getRoleService().countResults(null);
                case WORKFLOW:
                    return ServiceManager.getWorkflowService().countResults(null);
                default:
                    return 0L;
            }

        } catch (DAOException | DataException | JsonException | ElasticsearchStatusException e) {
            Helper.setErrorMessage("Unable to load number of elements", logger, e);
        }
        return 0L;
    }

    /**
     * Empties the lists for caching.
     */
    public void emptyCache() {
        taskList.clear();
        processList.clear();
        projectList.clear();
    }
}

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

package org.kitodo.forms;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.xml.fileformats.FileFormat;
import org.kitodo.config.xml.fileformats.FileFormatsConfig;
import org.kitodo.data.database.beans.Folder;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.Template;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.dto.ProjectDTO;
import org.kitodo.dto.TemplateDTO;
import org.kitodo.enums.ObjectType;
import org.kitodo.helper.Helper;
import org.kitodo.model.LazyDTOModel;

@Named("ProjectForm")
@SessionScoped
public class ProjectForm extends BaseForm {
    private static final long serialVersionUID = 6735912903249358786L;
    private static final Logger logger = LogManager.getLogger(ProjectForm.class);
    private Project project;

    /**
     * The folder currently under edit in the pop-up dialog.
     */
    /*
     * This is a hack. The clean solution would be to have an inner class bean for
     * the data table row an dialog, but this approach was introduced decades ago
     * and has been maintained until today.
     */
    private Folder myFolder;
    private Project baseProject;

    // lists accepting the preliminary actions of adding and delting folders
    // it needs the execution of commit folders to make these changes
    // permanent
    private List<Integer> newFolders = new ArrayList<>();
    private List<Integer> deletedFolders = new ArrayList<>();

    private boolean copyTemplates;

    private String projectListPath = MessageFormat.format(REDIRECT_PATH, "projects");
    private String projectEditPath = MessageFormat.format(REDIRECT_PATH, "projectEdit");

    private String projectEditReferer = DEFAULT_LINK;

    /**
     * Cash for the list of possible MIME types. So that the list does not have to
     * be read from file several times for one page load.
     */
    private Map<String, String> mimeTypes = Collections.emptyMap();

    /**
     * Empty default constructor that also sets the LazyDTOModel instance of this
     * bean.
     */
    public ProjectForm() {
        super();
        super.setLazyDTOModel(new LazyDTOModel(serviceManager.getProjectService()));
    }

    /**
     * This method deletes folders by their IDs in the list.
     *
     * @param folderIds
     *            IDs of folders to delete
     */
    private void deleteFolders(List<Integer> folderIds) {
        if (Objects.nonNull(this.project)) {
            for (Integer id : folderIds) {
                for (Folder f : this.project.getFolders()) {
                    if (f.getId() == null ? id == null : f.getId().equals(id)) {
                        this.project.getFolders().remove(f);
                        break;
                    }
                }
            }
        }
    }

    /**
     * this method flushes the newFolders list, thus makes them permanent and
     * deletes those marked for deleting, making the removal permanent.
     */
    private void commitFolders() {
        // resetting the list of new folders
        this.newFolders = new ArrayList<>();
        // deleting the folders marked for deletion
        deleteFolders(this.deletedFolders);
        // resetting the list of folders marked for deletion
        this.deletedFolders = new ArrayList<>();
    }

    /**
     * This needs to be executed in order to rollback adding of folders.
     *
     * @return page address
     */
    public String cancel() {
        // flushing new folders
        deleteFolders(this.newFolders);
        // resetting the list of new folders
        this.newFolders = new ArrayList<>();
        // resetting the List of folders marked for deletion
        this.deletedFolders = new ArrayList<>();
        return projectListPath;
    }

    /**
     * Create new project.
     *
     * @return page address
     */
    public String newProject() {
        this.project = new Project();
        this.project.setClient(serviceManager.getUserService().getSessionClientOfAuthenticatedUser());
        return projectEditPath;
    }

    /**
     * Duplicate the selected project.
     *
     * @param itemId
     *            ID of the project to duplicate
     * @return page address; either redirect to the edit project page or return
     *         'null' if the project could not be retrieved, which will prompt JSF
     *         to remain on the same page and reuse the bean.
     */
    public String duplicate(Integer itemId) {
        setCopyTemplates(true);
        try {
            this.baseProject = serviceManager.getProjectService().getById(itemId);
            this.project = serviceManager.getProjectService().duplicateProject(baseProject);
            return projectEditPath;
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DUPLICATE, new Object[] {ObjectType.PROJECT.getTranslationSingular() }, logger,
                e);
            return this.stayOnCurrentPage;
        }
    }

    /**
     * Saves current project if title is not empty and redirects to projects page.
     *
     * @return page or null
     */
    public String save() {
        serviceManager.getProjectService().evict(this.project);
        // call this to make saving and deleting permanent
        this.commitFolders();
        if (this.project.getTitle().equals("") || this.project.getTitle() == null) {
            Helper.setErrorMessage("errorProjectNoTitleGiven");
            return this.stayOnCurrentPage;
        } else {
            try {
                addFirstUserToNewProject();

                serviceManager.getProjectService().save(this.project);
                if (this.copyTemplates) {
                    for (Template template : this.baseProject.getTemplates()) {
                        template.getProjects().add(this.project);
                        this.project.getTemplates().add(template);
                        serviceManager.getTemplateService().save(template);
                    }
                    setCopyTemplates(false);
                }

                return projectListPath;
            } catch (DataException e) {
                Helper.setErrorMessage(ERROR_SAVING, new Object[] {ObjectType.PROJECT.getTranslationSingular() },
                    logger, e);
                return this.stayOnCurrentPage;
            } catch (DAOException e) {
                Helper.setErrorMessage(ERROR_LOADING_ONE, new Object[] {ObjectType.USER.getTranslationSingular() },
                    logger, e);
                return this.stayOnCurrentPage;
            }
        }
    }

    private void addFirstUserToNewProject() throws DAOException, DataException {
        if (this.project.getUsers().isEmpty()) {
            User user = serviceManager.getUserService().getCurrentUser();
            user.getProjects().add(this.project);
            this.project.getUsers().add(user);
            serviceManager.getProjectService().save(this.project);
            serviceManager.getUserService().save(user);
        }
    }

    /**
     * Remove.
     */
    public void delete() {
        if (!this.project.getUsers().isEmpty()) {
            Helper.setErrorMessage("userAssignedError");
        } else {
            try {
                serviceManager.getProjectService().remove(this.project);
            } catch (DataException e) {
                Helper.setErrorMessage(ERROR_DELETING, new Object[] {ObjectType.PROJECT.getTranslationSingular() },
                    logger, e);
            }
        }
    }

    /**
     * Add folder.
     *
     * @return String
     */
    public String addFolder() {
        this.myFolder = new Folder();
        this.myFolder.setProject(this.project);
        this.newFolders.add(this.myFolder.getId());
        return this.stayOnCurrentPage;
    }

    /**
     * Save folder.
     */
    public void saveFolder() {
        if (this.project.getFolders() == null) {
            this.project.setFolders(new ArrayList<>());
        }
        if (!this.project.getFolders().contains(this.myFolder)) {
            this.project.getFolders().add(this.myFolder);
        }
    }

    /**
     * Delete folder.
     *
     * @return page
     */
    public String deleteFolder() {
        // to be deleted folder IDs are listed
        // and deleted after a commit
        this.deletedFolders.add(this.myFolder.getId());
        return this.stayOnCurrentPage;
    }

    /**
     * Return list of templates assignable to this project. Templates are assignable
     * when they are not assigned already to this project and they belong to the
     * same client as the project and user which edits this project.
     *
     * @return list of assignable templates
     */
    public List<TemplateDTO> getTemplates() {
        try {
            return serviceManager.getTemplateService().findAllAvailableForAssignToProject(this.project.getId());
        } catch (DataException e) {
            Helper.setErrorMessage(ERROR_LOADING_MANY, new Object[] {ObjectType.TEMPLATE.getTranslationPlural() },
                logger, e);
            return new LinkedList<>();
        }
    }

    /**
     * Add template to project.
     *
     * @return stay on the same page
     */
    public String addTemplate() {
        int templateId = 0;
        try {
            templateId = Integer.parseInt(Helper.getRequestParameter("ID"));
            Template template = serviceManager.getTemplateService().getById(templateId);

            if (!this.project.getTemplates().contains(template)) {
                this.project.getTemplates().add(template);
            }
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                new Object[] {ObjectType.TEMPLATE.getTranslationSingular(), templateId }, logger, e);
        } catch (NumberFormatException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return this.stayOnCurrentPage;
    }

    /**
     * Remove template from project.
     *
     * @return stay on the same page
     */
    public String deleteTemplate() {
        try {
            int templateId = Integer.parseInt(Helper.getRequestParameter("ID"));
            for (Template template : this.project.getTemplates()) {
                if (template.getId().equals(templateId)) {
                    this.project.getTemplates().remove(template);
                }
            }
        } catch (NumberFormatException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return this.stayOnCurrentPage;
    }

    /**
     * Get project.
     *
     * @return Project object
     */
    public Project getProject() {
        return this.project;
    }

    /**
     * Set my project.
     *
     * @param inProjekt
     *            Project object
     */
    public void setProject(Project inProjekt) {
        // has to be called if a page back move was done
        this.cancel();
        this.project = inProjekt;
    }

    /**
     * Set project by ID.
     *
     * @param projectID
     *            ID of project to set.
     */
    public void setProjectById(int projectID) {
        try {
            setProject(serviceManager.getProjectService().getById(projectID));
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_LOADING_ONE,
                new Object[] {ObjectType.PROJECT.getTranslationSingular(), projectID }, logger, e);
        }
    }

    /**
     * Set copy templates.
     *
     * @param copyTemplates
     *            as boolean
     */
    public void setCopyTemplates(boolean copyTemplates) {
        this.copyTemplates = copyTemplates;
    }

    /**
     * Get copy templates.
     *
     * @return value of copy templates
     */
    public boolean isCopyTemplates() {
        return copyTemplates;
    }

    /**
     * The need to commit deleted folders only after the save action requires a
     * filter, so that those folders marked for delete are not shown anymore.
     *
     * @return modified ArrayList
     */
    public List<Folder> getFolderList() {
        List<Folder> filteredFolderList = new ArrayList<>(this.project.getFolders());

        for (Integer id : this.deletedFolders) {
            for (Folder f : this.project.getFolders()) {
                if (f.getId() == null ? id == null : f.getId().equals(id)) {
                    filteredFolderList.remove(f);
                    break;
                }
            }
        }
        return filteredFolderList;
    }

    private Map<String, Folder> getFolderMap() {
        return getFolderList().parallelStream().collect(Collectors.toMap(Folder::toString, Function.identity()));
    }

    /**
     * Returns the folder currently under edit in the pop-up dialog.
     *
     * @return the folder currently under edit
     */
    public Folder getMyFolder() {
        return this.myFolder;
    }

    /**
     * Sets the folder currently under edit in the pop-up dialog.
     *
     * @param myFolder
     *            folder to set to be under edit now
     */
    public void setMyFolder(Folder myFolder) {
        this.myFolder = myFolder;
    }

    /**
     * Returns the list of possible MIME types to display them in the drop-down
     * select.
     *
     * @return possible MIME types
     */
    public Map<String, String> getMimeTypes() {
        if (mimeTypes.isEmpty()) {
            try {
                Locale language = FacesContext.getCurrentInstance().getViewRoot().getLocale();
                List<LanguageRange> languages = Arrays.asList(new LanguageRange(language.toLanguageTag()));
                mimeTypes = FileFormatsConfig.getFileFormats().parallelStream()
                        .collect(Collectors.toMap(locale -> locale.getLabel(languages), FileFormat::getMimeType,
                            (prior, recent) -> recent, TreeMap::new));
            } catch (JAXBException | RuntimeException e) {
                Helper.setErrorMessage(ERROR_READING, new Object[] {e.getMessage() }, logger, e);
            }
        }
        return mimeTypes;
    }

    /**
     * Returns the folder to use as source for generation of derived resources of
     * this project.
     *
     * @return the source folder for generation
     */
    public String getGeneratorSource() {
        Folder source = project.getGeneratorSource();
        return source == null ? null : source.toString();
    }

    /**
     * Sets the folder to use as source for generation of derived resources of this
     * project.
     *
     * @param generatorSource
     *            source folder for generation to set
     */
    public void setGeneratorSource(String generatorSource) {
        project.setGeneratorSource(getFolderMap().get(generatorSource));
    }

    /**
     * Returns the folder to use for the media view.
     *
     * @return media view folder
     */
    public String getMediaView() {
        Folder mediaView = project.getMediaView();
        return mediaView == null ? null : mediaView.toString();
    }

    /**
     * Sets the folder to use for the media view.
     *
     * @param mediaView
     *            media view folder
     */
    public void setMediaView(String mediaView) {
        project.setMediaView(getFolderMap().get(mediaView));
    }

    /**
     * Returns the folder to use for preview.
     *
     * @return preview folder
     */
    public String getPreview() {
        Folder preview = project.getPreview();
        return preview == null ? null : preview.toString();
    }

    /**
     * Sets the folder to use for preview.
     *
     * @param preview
     *            preview folder
     */
    public void setPreview(String preview) {
        project.setPreview(getFolderMap().get(preview));
    }

    /**
     * Method being used as viewAction for project edit form.
     *
     * @param id
     *            ID of the ruleset to load
     */
    public void loadProject(int id) {
        try {
            if (!Objects.equals(id, 0)) {
                setProject(this.serviceManager.getProjectService().getById(id));
            }
            setSaveDisabled(true);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_LOADING_ONE, new Object[] {ObjectType.PROJECT.getTranslationSingular(), id },
                logger, e);
        }

    }

    /**
     * Return list of projects.
     *
     * @return list of projects
     */
    public List<ProjectDTO> getProjects() {
        try {
            return serviceManager.getProjectService().findAll();
        } catch (DataException e) {
            Helper.setErrorMessage(ERROR_LOADING_MANY, new Object[] {ObjectType.PROJECT.getTranslationPlural() },
                logger, e);
            return new LinkedList<>();
        }
    }

    /**
     * Set referring view which will be returned when the user clicks "save" or
     * "cancel" on the project edit page.
     *
     * @param referer
     *            the referring view
     */
    public void setProjectEditReferer(String referer) {
        if (!referer.isEmpty()) {
            if (referer.equals("projects")) {
                this.projectEditReferer = referer;
            } else {
                this.projectEditReferer = DEFAULT_LINK;
            }
        }
    }

    /**
     * Get project edit page referring view.
     * 
     * @return project edit page referring view
     */
    public String getProjectEditReferer() {
        return this.projectEditReferer;
    }
}

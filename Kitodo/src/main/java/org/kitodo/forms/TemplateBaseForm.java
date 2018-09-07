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

import de.sub.goobi.forms.BaseForm;
import de.sub.goobi.helper.Helper;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.BaseBean;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.UserGroup;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.enums.ObjectType;
import org.kitodo.services.data.base.SearchDatabaseService;

public class TemplateBaseForm extends BaseForm {

    private static final long serialVersionUID = 6566567843176821176L;
    private static final Logger logger = LogManager.getLogger(TemplateBaseForm.class);
    private boolean showInactiveProjects = false;

    /**
     * Check if inactive projects should be shown.
     *
     * @return true or false
     */
    public boolean isShowInactiveProjects() {
        return this.showInactiveProjects;
    }

    /**
     * Set if inactive projects should be shown.
     *
     * @param showInactiveProjects
     *            true or false
     */
    public void setShowInactiveProjects(boolean showInactiveProjects) {
        this.showInactiveProjects = showInactiveProjects;
    }

    /**
     * Add user group to task.
     *
     * @param task
     *            to add user group
     */
    public void addUserGroup(Task task) {
        Integer userGroupId = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            UserGroup userGroup = serviceManager.getUserGroupService().getById(userGroupId);
            for (UserGroup taskUserGroup : task.getUserGroups()) {
                if (taskUserGroup.equals(userGroup)) {
                    return;
                }
            }
            task.getUserGroups().add(userGroup);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                    new Object[]{ObjectType.USER_GROUP.getTranslationSingular(), userGroupId}, logger, e);
        }
    }

    /**
     * Add user to task.
     *
     * @param task
     *            to add user
     */
    public void addUser(Task task) {
        Integer userId = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            User user = serviceManager.getUserService().getById(userId);
            for (User taskUser : task.getUsers()) {
                if (taskUser.equals(user)) {
                    return;
                }
            }
            task.getUsers().add(user);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                    new Object[]{ObjectType.USER.getTranslationSingular(), userId}, logger, e);
        }
    }

    /**
     * Remove user from task.
     *
     * @param task
     *            for delete user
     */
    public void deleteUser(Task task) {
        Integer userId = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            User user = serviceManager.getUserService().getById(userId);
            task.getUsers().remove(user);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                    new Object[]{ObjectType.USER.getTranslationSingular(), userId}, logger, e);
        }
    }

    /**
     * Remove user group from task.
     *
     * @param task
     *            for delete user group
     */
    public void deleteUserGroup(Task task) {
        Integer userGroupId = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            UserGroup userGroup = serviceManager.getUserGroupService().getById(userGroupId);
            task.getUserGroups().remove(userGroup);
        } catch (DAOException e) {
            Helper.setErrorMessage(ERROR_DATABASE_READING,
                    new Object[]{ObjectType.USER_GROUP.getTranslationSingular(), userGroupId}, logger, e);
        }
    }

    protected void saveTask(Task task, BaseBean baseBean, String message, SearchDatabaseService searchDatabaseService) {
        try {
            serviceManager.getTaskService().save(task);
            serviceManager.getTaskService().evict(task);
            reload(baseBean, message, searchDatabaseService);
        } catch (DataException e) {
            Helper.setErrorMessage(ERROR_SAVING, new Object[] {ObjectType.TASK.getTranslationSingular() }, logger, e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void reload(BaseBean baseBean, String message, SearchDatabaseService searchDatabaseService) {
        if (Objects.nonNull(baseBean) && Objects.nonNull(baseBean.getId())) {
            try {
                searchDatabaseService.refresh(baseBean);
            } catch (RuntimeException e) {
                Helper.setErrorMessage(ERROR_RELOADING, new Object[] {message }, logger, e);
            }
        }
    }
}

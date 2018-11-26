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

package org.kitodo.workflow.model.beans;

import java.util.Objects;

import org.camunda.bpm.model.bpmn.instance.Task;

public class KitodoTask {

    private String workflowId;
    private String title;
    private Integer priority;
    private Integer editType;
    private Integer processingStatus;
    private Boolean typeMetadata;
    private Boolean typeAutomatic;
    private Boolean typeExportDms;
    private Boolean typeImagesRead;
    private Boolean typeImagesWrite;
    private Boolean typeAcceptClose;
    private Boolean typeCloseVerify;
    private Boolean batchStep;
    private Integer userRole;

    static final String NAMESPACE = "http://www.kitodo.org/template";

    /**
     * Constructor.
     * 
     * @param task
     *            BPMN model task
     */
    public KitodoTask(Task task) {
        this.workflowId = task.getId();
        this.title = task.getName();
        this.priority = getIntegerValue(task.getAttributeValueNs(NAMESPACE, "priority"));
        this.editType = getIntegerValue(task.getAttributeValueNs(NAMESPACE, "editType"));
        this.processingStatus = getIntegerValue(task.getAttributeValueNs(NAMESPACE, "processingStatus"));
        this.typeMetadata = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeMetadata"));
        this.typeAutomatic = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeAutomatic"));
        this.typeExportDms = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeExportDMS"));
        this.typeImagesRead = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeImagesRead"));
        this.typeImagesWrite = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeImagesWrite"));
        this.typeAcceptClose = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeAcceptClose"));
        this.typeCloseVerify = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "typeCloseVerify"));
        this.batchStep = getBooleanValue(task.getAttributeValueNs(NAMESPACE, "batchStep"));
        this.userRole = getIntegerValue(task.getAttributeValueNs(NAMESPACE, "permittedUserRole"));
    }

    private Boolean getBooleanValue(String value) {
        if (Objects.nonNull(value)) {
            return Boolean.valueOf(value);
        }
        return false;
    }

    private Integer getIntegerValue(String value) {
        if (Objects.nonNull(value)) {
            return Integer.valueOf(value);
        }
        return -1;
    }

    /**
     * Get workflow id.
     *
     * @return workflow id as String
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Set workflow id.
     *
     * @param workflowId
     *            as String
     */
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Get title.
     *
     * @return value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title.
     *
     * @param title
     *            as String
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get priority.
     *
     * @return value of priority
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set priority.
     *
     * @param priority
     *            as Integer
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Get editType.
     *
     * @return value of editType
     */
    public Integer getEditType() {
        return editType;
    }

    /**
     * Set editType.
     *
     * @param editType
     *            as Integer
     */
    public void setEditType(Integer editType) {
        this.editType = editType;
    }

    /**
     * Get processingStatus.
     *
     * @return value of processingStatus
     */
    public Integer getProcessingStatus() {
        return processingStatus;
    }

    /**
     * Set processingStatus.
     *
     * @param processingStatus
     *            as java.lang.Integer
     */
    public void setProcessingStatus(Integer processingStatus) {
        this.processingStatus = processingStatus;
    }

    /**
     * Get typeMetadata.
     *
     * @return value of typeMetadata
     */
    public Boolean getTypeMetadata() {
        return typeMetadata;
    }

    /**
     * Set typeMetadata.
     *
     * @param typeMetadata
     *            as Boolean
     */
    public void setTypeMetadata(Boolean typeMetadata) {
        this.typeMetadata = typeMetadata;
    }

    /**
     * Get typeAutomatic.
     *
     * @return value of typeAutomatic
     */
    public Boolean getTypeAutomatic() {
        return typeAutomatic;
    }

    /**
     * Set typeAutomatic.
     *
     * @param typeAutomatic
     *            as Boolean
     */
    public void setTypeAutomatic(Boolean typeAutomatic) {
        this.typeAutomatic = typeAutomatic;
    }

    /**
     * Get typeExportDms.
     *
     * @return value of typeExportDms
     */
    public Boolean getTypeExportDms() {
        return typeExportDms;
    }

    /**
     * Set typeExportDms.
     *
     * @param typeExportDms
     *            as Boolean
     */
    public void setTypeExportDms(Boolean typeExportDms) {
        this.typeExportDms = typeExportDms;
    }

    /**
     * Get typeImagesRead.
     *
     * @return value of typeImagesRead
     */
    public Boolean getTypeImagesRead() {
        return typeImagesRead;
    }

    /**
     * Set typeImagesRead.
     *
     * @param typeImagesRead
     *            as java.lang.Boolean
     */
    public void setTypeImagesRead(Boolean typeImagesRead) {
        this.typeImagesRead = typeImagesRead;
    }

    /**
     * Get typeImagesWrite.
     *
     * @return value of typeImagesWrite
     */
    public Boolean getTypeImagesWrite() {
        return typeImagesWrite;
    }

    /**
     * Set typeImagesWrite.
     *
     * @param typeImagesWrite
     *            as java.lang.Boolean
     */
    public void setTypeImagesWrite(Boolean typeImagesWrite) {
        this.typeImagesWrite = typeImagesWrite;
    }

    /**
     * Get typeAcceptClose.
     *
     * @return value of typeAcceptClose
     */
    public Boolean getTypeAcceptClose() {
        return typeAcceptClose;
    }

    /**
     * Set typeAcceptClose.
     *
     * @param typeAcceptClose
     *            as java.lang.Boolean
     */
    public void setTypeAcceptClose(Boolean typeAcceptClose) {
        this.typeAcceptClose = typeAcceptClose;
    }

    /**
     * Get typeCloseVerify.
     *
     * @return value of typeCloseVerify
     */
    public Boolean getTypeCloseVerify() {
        return typeCloseVerify;
    }

    /**
     * Set typeCloseVerify.
     *
     * @param typeCloseVerify
     *            as java.lang.Boolean
     */
    public void setTypeCloseVerify(Boolean typeCloseVerify) {
        this.typeCloseVerify = typeCloseVerify;
    }

    /**
     * Get batchStep.
     *
     * @return value of batchStep
     */
    public Boolean getBatchStep() {
        return batchStep;
    }

    /**
     * Set batchStep.
     *
     * @param batchStep
     *            as java.lang.Boolean
     */
    public void setBatchStep(Boolean batchStep) {
        this.batchStep = batchStep;
    }

    /**
     * Get user role.
     *
     * @return value of userRole
     */
    public Integer getUserRole() {
        return userRole;
    }

    /**
     * Set user role.
     *
     * @param userRole
     *            as java.lang.Integer
     */
    public void setUserRole(Integer userRole) {
        this.userRole = userRole;
    }
}

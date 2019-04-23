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

package org.kitodo.data.elasticsearch.index.type.enums;

public enum TaskTypeField implements TypeInterface {

    ID("id"),
    TITLE("title"),
    PRIORITY("priority"),
    ORDERING("ordering"),
    EDIT_TYPE("editType"),
    PROCESSING_STATUS("processingStatus"),
    PROCESSING_TIME("processingTime"),
    PROCESSING_BEGIN("processingBegin"),
    PROCESSING_END("processingEnd"),
    PROCESSING_USER_ID("processingUser.id"),
    PROCESSING_USER_LOGIN("processingUser.login"),
    PROCESSING_USER_NAME("processingUser.name"),
    PROCESSING_USER_SURNAME("processingUser.surname"),
    HOME_DIRECTORY("homeDirectory"),
    TYPE_METADATA("typeMetadata"),
    TYPE_AUTOMATIC("typeAutomatic"),
    TYPE_IMAGES_READ("typeImagesRead"),
    TYPE_IMAGES_WRITE("typeImagesWrite"),
    BATCH_STEP("batchStep"),
    PROCESS_ID("processForTask.id"),
    PROCESS_TITLE("processForTask.title"),
    CLIENT_ID("clientForTask"),
    TEMPLATE_ID("templateForTask.id"),
    TEMPLATE_TITLE("templateForTask.title"),
    ROLES("roles");

    private String name;

    TaskTypeField(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

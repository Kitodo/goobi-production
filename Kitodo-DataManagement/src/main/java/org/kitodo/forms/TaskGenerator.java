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

import java.util.List;

import org.kitodo.data.database.beans.Folder;
import org.kitodo.data.database.beans.Task;

/**
 * An encapsulation to access the generator properties of the task.
 */
public class TaskGenerator {
    /**
     * Folder represented by this generator switch.
     */
    private Folder folder;

    /**
     * Modifiable list containing enabled generators. This list is member of the
     * {@link Task} and saves the generator state when the task is saved.
     */
    private List<Folder> typeGenerate;

    /**
     * Creates a new generator for this task.
     *
     * @param folder
     *            folder represented by this toggle switch
     * @param typeGenerate
     *            modifyable list of enabled toggle switches
     */
    public TaskGenerator(Folder folder, List<Folder> typeGenerate) {
        this.folder = folder;
        this.typeGenerate = typeGenerate;
    }

    /**
     * Returns a label for the folder.
     *
     * @return a label for the folder
     */
    public String getLabel() {
        return folder.toString();
    }

    /**
     * Returns the toggle switch value by looking into the list of folders to
     * generate.
     *
     * @returns the value for the toggle switch
     */
    public boolean isValue() {
        return typeGenerate.contains(folder);
    }

    /**
     * Sets the boolean value by updating the list of folders to generate.
     *
     * @param value
     *            value to set
     */
    public void setValue(boolean value) {
        if (!value) {
            typeGenerate.remove(folder);
        } else if (!typeGenerate.contains(folder)) {
            typeGenerate.add(folder);
        }
    }
}

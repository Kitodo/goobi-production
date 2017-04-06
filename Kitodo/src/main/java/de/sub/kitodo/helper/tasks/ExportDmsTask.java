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

package de.sub.kitodo.helper.tasks;

import de.sub.kitodo.export.dms.ExportDms;
import de.sub.kitodo.helper.Helper;

import org.kitodo.data.database.beans.Process;

/**
 * The class ExportDmsTask accepts an {@link de.sub.kitodo.export.dms.ExportDms}
 * for a process and provides the ability to run the export in the background
 * this way. This is especially valuable if the export has a big load of images
 * to copy.
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class ExportDmsTask extends EmptyTask {

    private final ExportDms exportDms;
    private final Process process;
    private final String userHome;

    /**
     * ExportDmsTask constructor. Creates a ExportDmsTask.
     *
     * @param exportDms
     *            ExportDMS configuration
     * @param process
     *            the process to export
     * @param userHome
     *            home directory of the user who started the export
     */
    public ExportDmsTask(ExportDms exportDms, Process process, String userHome) {
        super(process.getTitle());
        this.exportDms = exportDms;
        this.process = process;
        this.userHome = userHome;
    }

    /**
     * Clone constructor. Provides the ability to restart an export that was
     * previously interrupted by the user.
     *
     * @param source
     *            terminated thread
     */
    private ExportDmsTask(ExportDmsTask source) {
        super(source);
        this.exportDms = source.exportDms;
        this.process = source.process;
        this.userHome = source.userHome;
    }

    /**
     * Returns the display name of the task to show to the user.
     *
     * @see de.sub.kitodo.helper.tasks.INameableTask#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Helper.getTranslation("ExportDmsTask");
    }

    /**
     * If the task is started, it will execute this run() method which will
     * start the export on the ExportDms. This task instance is passed in
     * addition so that the ExportDms can update the task’s state.
     *
     * @see de.sub.kitodo.helper.tasks.EmptyTask#run()
     */
    @Override
    public void run() {
        try {
            exportDms.startExport(process, userHome, this);
        } catch (Exception e) {
            setException(e);
        }
    }

    /**
     * Calls the clone constructor to create a not yet executed instance of this
     * thread object. This is necessary for threads that have terminated in
     * order to render possible to restart them.
     *
     * @return a not-yet-executed replacement of this thread
     * @see de.sub.kitodo.helper.tasks.EmptyTask#replace()
     */
    @Override
    public ExportDmsTask replace() {
        return new ExportDmsTask(this);
    }
}

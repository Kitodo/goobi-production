/**
 * This file is part of the Goobi Application - a Workflow tool for the support
 * of mass digitization.
 * 
 * (c) 2014 Goobi. Digialisieren im Verein e.V. &lt;contact@goobi.org&gt;
 * 
 * Visit the websites for more information.
 *     		- http://www.goobi.org/en/
 *     		- https://github.com/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination. As a special
 * exception, the copyright holders of this library give you permission to link
 * this library with independent modules to produce an executable, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions of the
 * license of that module. An independent module is a module which is not
 * derived from or based on this library. If you modify this library, you may
 * extend this exception to your version of the library, but you are not obliged
 * to do so. If you do not wish to do so, delete this exception statement from
 * your version.
 */
package de.sub.goobi.helper.tasks;

import de.sub.goobi.beans.Prozess;
import de.sub.goobi.export.dms.ExportDms;

/**
 * The class ExportDmsTask accepts an {@link de.sub.goobi.export.dms.ExportDms}
 * for a process and provides the ability to run the export in the background
 * this way. This is especially valuable if the export has a big load of images
 * to copy.
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class ExportDmsTask extends EmptyTask {

	private final ExportDms exportDms;
	private final Prozess process;
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
	public ExportDmsTask(ExportDms exportDms, Prozess process, String userHome) {
		this.exportDms = exportDms;
		this.process = process;
		this.userHome = userHome;
		setNameDetail(process.getTitel());
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
	 * If the task is started, it will execute this run() method which will
	 * start the export on the ExportDms. This task instance is passed in
	 * addition so that the ExportDms can update the task’s state.
	 * 
	 * @see de.sub.goobi.helper.tasks.EmptyTask#run()
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
	 * The function clone() provides the ability to copy the task object to
	 * restart an export that was previously interrupted by the user.
	 * 
	 * @see de.sub.goobi.helper.tasks.EmptyTask#clone()
	 */
	@Override
	public ExportDmsTask clone() {
		return new ExportDmsTask(this);
	}
}

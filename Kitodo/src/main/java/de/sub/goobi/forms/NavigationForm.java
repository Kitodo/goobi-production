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

package de.sub.goobi.forms;

import de.sub.goobi.config.ConfigMain;

public class NavigationForm {
	private String aktuell = "0";

	public String getAktuell() {
		return this.aktuell;
	}

	public void setAktuell(String aktuell) {
		this.aktuell = aktuell;
	}

	public String Reload() {
		return "";
	}

	public String JeniaPopupCloseAction() {
		return "jeniaClosePopupFrameWithAction";
	}

	public String BenutzerBearbeiten() {
		return "BenutzerBearbeiten";
	}

	/**
	* @return true if show_taskmanager in file goobi_config.properties is =true
	*/
	public Boolean getShowTaskManager() {
		return ConfigMain.getBooleanParameter("taskManager.showInSidebar", true);
	}

	public Boolean getShowModuleManager() {
		return ConfigMain.getBooleanParameter("show_modulemanager", false);
	}
}

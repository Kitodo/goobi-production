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

package de.sub.goobi.persistence.apache;

import de.sub.goobi.beans.ProjectFileGroup;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

public class ProjectManager {
	private static final Logger logger = Logger.getLogger(MySQLHelper.class);

	/**
	 * @param projectId add description
	 * @return add description
	 */
	public static ProjectObject getProjectById(int projectId) {
		try {
			return MySQLHelper.getProjectObjectById(projectId);
		} catch (SQLException e) {
			logger.error("Cannot not load project with id " + projectId, e);
		}
		return null;
	}

	/**
	 * @param projectId add description
	 * @return add description
	 */
	public static List<ProjectFileGroup> getFilegroupsForProjectId(int projectId) {
		try {
			return MySQLHelper.getFilegroupsForProjectId(projectId);
		} catch (SQLException e) {
			logger.error("Cannot not load project filegroups with id " + projectId, e);
		}
		return null;
	}

}

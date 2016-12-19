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

package org.goobi.production.cli;

public class CommandResponse {
	private int status = 200;
	private String title;
	private String message;

	/**
	 * @param status add description
	 * @param inTitle add description
	 * @param inMessage add description
	 */
	public CommandResponse(int status, String inTitle, String inMessage) {
		this.status = status;
		title = inTitle;
		message = inMessage;
	}

	/**
	 * @param inTitle add description
	 * @param inMessage add description
	 */
	public CommandResponse(String inTitle, String inMessage) {
		status = 200;
		title = inTitle;
		message = inMessage;
	}

	public int getStatus() {
		return status;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}
}

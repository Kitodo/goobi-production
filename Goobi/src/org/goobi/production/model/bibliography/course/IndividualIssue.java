/**
 * This file is part of the Goobi Application - a Workflow tool for the support
 * of mass digitization.
 * 
 * (c) 2013 Goobi. Digialisieren im Verein e.V. &lt;contact@goobi.org&gt;
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

package org.goobi.production.model.bibliography.course;

import org.joda.time.LocalDate;
import org.w3c.dom.Element;

import com.sharkysoft.util.UnreachableCodeException;

/**
 * The class IndividualIssue represents a stamping of an Issue, that is one
 * distinguishable physically appeared issue (meanwhile the class Issue
 * represents the <em>type</em> of issue).
 * 
 * <p>
 * The class IndividualIssue is final.
 * </p>
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class IndividualIssue {
	/**
	 * Date of this issue
	 */
	protected final LocalDate date;

	/**
	 * Labelling of the kind of issue this is
	 */
	protected final String issue;

	/**
	 * Labelling of the newspaper that this is an issue from
	 */
	protected final String title;

	/**
	 * Constructor to create an IndividualIssue
	 * 
	 * @param title
	 *            Name of the newspaper
	 * @param date
	 *            Date of appearance
	 * @param issue
	 *            Name of the issue
	 */
	IndividualIssue(String title, LocalDate date, String issue) {
		this.title = title;
		this.issue = issue;
		this.date = date;
	}

	/**
	 * Returns an integer which, for a given BreakMode, shall indicate for two
	 * neighbouring individual issues whether they form the same process (break
	 * mark is equal) or to different processes (break mark differs).
	 * 
	 * @param mode
	 *            how the course shall be broken into processes
	 * @return an int which differs if two neighbouring individual issues belong
	 *         to different processes
	 */
	int getBreakMark(BreakMode mode) {
		final int prime = 31;
		switch (mode) {
		case ISSUES:
			return this.hashCode();
		case DAYS:
			return date.hashCode();
		case WEEKS:
			return prime * date.getYear() + date.getWeekOfWeekyear();
		case MONTHS:
			return prime * date.getYear() + date.getMonthOfYear();
		case QUARTERS:
			return prime * date.getYear() + (date.getMonthOfYear() - 1) / 3;
		case YEARS:
			return date.getYear();
		default:
			throw new UnreachableCodeException("default case in complete switch statement");
		}
	}

	/**
	 * The function getId() returns an identifier for the issue. Currently, the
	 * identifier is the hexadecimal representation of the hashCode() of this
	 * bean class.
	 * 
	 * @return an identifier for the issue
	 */
	String getId() {
		return String.format("%08x", hashCode());
	}

	/**
	 * The function populate() populates an DOM tree element with three
	 * attributes holding the ID, title name and issue name of this individual
	 * issue.
	 * 
	 * @param result
	 *            the DOM tree element to populate
	 * @return the DOM tree element
	 */
	Element populate(Element result) {
		final String ID_ATTRIBUTE_NAME = "id";
		final String ISSUE_ATTRIBUTE_NAME = "issue";
		final String TITLE_ATTRIBUTE_NAME = "title";

		result.setAttribute(ID_ATTRIBUTE_NAME, getId());
		result.setIdAttribute(ID_ATTRIBUTE_NAME, true);
		if (title != null)
			result.setAttribute(TITLE_ATTRIBUTE_NAME, title);
		if (issue != null)
			result.setAttribute(ISSUE_ATTRIBUTE_NAME, issue);
		result.setAttribute("date", date.toString());
		return result;
	}

	/**
	 * Returns a hash code for the object which depends on the content of its
	 * variables. Whenever IndividualIssue objects are held in HashSet objects,
	 * a hashCode() is essentially necessary.
	 * 
	 * <p>
	 * The method was generated by Eclipse using right-click → Source → Generate
	 * hashCode() and equals()…. If you will ever change the classes’ fields,
	 * just re-generate it.
	 * </p>
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((issue == null) ? 0 : issue.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	/**
	 * Returns whether two individual issues are equal; the decision depends on
	 * the content of its variables.
	 * 
	 * <p>
	 * The method was generated by Eclipse using right-click → Source → Generate
	 * hashCode() and equals()…. If you will ever change the classes’ fields,
	 * just re-generate it.
	 * </p>
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IndividualIssue))
			return false;
		IndividualIssue other = (IndividualIssue) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (issue == null) {
			if (other.issue != null)
				return false;
		} else if (!issue.equals(other.issue))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
}

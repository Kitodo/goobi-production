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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

/**
 * The class Title is a bean class that represents an interval of time in the
 * course of appearance of a newspaper within which it wasn’t suspended and
 * didn’t change its name either. A Title instance handles one or more Issue
 * objects.
 * 
 * TODO: Rename class. The name “Title” was chosen because the class was
 * originally intended to reflect changes of the title of a newspaper. Now, it
 * does only represent temporary blocks.
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class Title {
	/**
	 * The field course holds a reference to the course this Title block is in.
	 */
	private final Course course;

	/**
	 * The field variant may hold a variant identifer that can be used to
	 * distinguish different title blocks with equal heading during the buildup
	 * of a course of appearance from individual issues.
	 * 
	 * Given a newspaper appeared three times a week for a period of time, and
	 * then changed to being published six times a week without changing its
	 * heading, and this change shall be represented by different title blocks,
	 * the variant identifier can be used to distinguish the blocks. Otherwise,
	 * both time ranges would be represented in one combined block, what would
	 * be factual correct but would result in a multitude of exceptions, which
	 * could be undesired.
	 */
	private final String variant;

	/**
	 * The field firstAppearance holds the date representing the first day of
	 * the period of time represented by this title block. The date is treated
	 * as inclusive.
	 */
	private LocalDate firstAppearance;

	/**
	 * The field lastAppearance holds the date representing the last day of the
	 * period of time represented by this title block. The date is treated as
	 * inclusive.
	 */
	private LocalDate lastAppearance;

	/**
	 * The field issues holds the issues that have appeared during the period of
	 * time represented by this title block.
	 */
	private List<Issue> issues;

	/**
	 * Default constructor. Creates a Title object without any data.
	 * 
	 * @param course
	 */
	public Title(Course course) {
		this.course = course;
		this.variant = null;
		this.firstAppearance = null;
		this.lastAppearance = null;
		this.issues = new ArrayList<Issue>();
	}

	/**
	 * Constructor for a title with a given heading and variant identifier.
	 * 
	 * @param heading
	 *            the name of the title
	 * @param variant
	 *            a variant identifier (may be null)
	 */
	public Title(Course course, String variant) {
		this.course = course;
		this.variant = variant;
		this.firstAppearance = null;
		this.lastAppearance = null;
		this.issues = new ArrayList<Issue>();
	}

	/**
	 * The function addIssue() adds an Issue to this title if it is not already
	 * present.
	 * 
	 * @param issue
	 *            Issue to add
	 * @return true if the set was changed
	 */
	public boolean addIssue(Issue issue) {
		if (issue.countIndividualIssues(firstAppearance, lastAppearance) > 0)
			course.clearProcesses();
		return issues.add(issue);
	}

	/**
	 * The function clone() creates and returns a copy of this Title.
	 * 
	 * @param course
	 *            Course this title belongs to
	 */
	public Title clone(Course course) {
		Title copy = new Title(course);
		copy.firstAppearance = firstAppearance;
		copy.lastAppearance = lastAppearance;
		ArrayList<Issue> copiedIssues = new ArrayList<Issue>(issues.size() > 10 ? issues.size() : 10);
		for (Issue issue : issues)
			copiedIssues.add(issue.clone(course));
		copy.issues = copiedIssues;
		return copy;
	}

	/**
	 * The function countIndividualIssues() determines how many stampings of
	 * issues physically appeared without generating a list of IndividualIssue
	 * objects.
	 * 
	 * @return the count of issues
	 */
	public long countIndividualIssues() {
		if (firstAppearance == null || lastAppearance == null)
			return 0;
		long result = 0;
		for (LocalDate day = firstAppearance; !day.isAfter(lastAppearance); day = day.plusDays(1)) {
			for (Issue issue : getIssues()) {
				if (issue.isMatch(day))
					result += 1;
			}
		}
		return result;
	}

	/**
	 * The function getIssues() returns the list of issues contained in this
	 * Title.
	 * 
	 * @return the list of issues from this Title
	 */
	public List<Issue> getIssues() {
		return new ArrayList<Issue>(issues);
	}

	/**
	 * The function getIndividualIssues() generates a list of IndividualIssue
	 * objects for a given day, each of them representing a stamping of one
	 * physically appeared issue.
	 * 
	 * @return a List of IndividualIssue objects, each of them representing one
	 *         physically appeared issue
	 */
	public List<IndividualIssue> getIndividualIssues(LocalDate date) {
		if (!isMatch(date))
			return Collections.emptyList();
		ArrayList<IndividualIssue> result = new ArrayList<IndividualIssue>(issues.size());
		for (Issue issue : getIssues()) {
			if (issue.isMatch(date)) {
				result.add(new IndividualIssue(this, issue, date));
			}
		}
		return result;
	}

	/**
	 * The function getIssue() returns an issue from the Title by the issue’s
	 * heading, or null if the title doesn’t contain an issue with that heading.
	 * 
	 * @param heading
	 *            Heading of the issue to look for
	 * @return Issue with that heading
	 */
	public Issue getIssue(String heading) {
		for (Issue issue : issues)
			if (heading.equals(issue.getHeading()))
				return issue;
		return null;
	}

	/**
	 * The function getFirstAppearance() returns the date the regularity of this
	 * title begins with.
	 * 
	 * @return the date of first appearance
	 */
	public LocalDate getFirstAppearance() {
		return firstAppearance;
	}

	/**
	 * The function getLastAppearance() returns the date the regularity of this
	 * title ends with.
	 * 
	 * @return the date of last appearance
	 */
	public LocalDate getLastAppearance() {
		return lastAppearance;
	}

	/**
	 * The function isEmpty() returns whether the title is in an empty state or
	 * not.
	 * 
	 * @return whether the title is dataless
	 */
	public boolean isEmpty() {
		return firstAppearance == null && lastAppearance == null && (issues == null || issues.isEmpty());
	}

	public boolean isIdentifiedBy(String variant) {
		return variant == null && this.variant == null || this.variant.equals(variant);
	}

	/**
	 * The function isMatch() returns whether a given LocalDate comes within the
	 * limits of this title. Defaults to false if either the argument or one of
	 * the fields to compare against is null.
	 * 
	 * @param date
	 *            a LocalDate to examine
	 * @return whether the date is within the limits of this title block
	 */
	public boolean isMatch(LocalDate date) {
		try {
			return !date.isBefore(firstAppearance) && !date.isAfter(lastAppearance);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * The method recalculateRegularityOfIssues() recalculates for each Issue
	 * the daysOfWeek of its regular appearance within the interval of time of
	 * the Title. This is especially sensible to detect the underlying
	 * regularity after lots of issues whose existence is known have been added
	 * one by one as additions to the underlying issue(s).
	 */
	public void recalculateRegularityOfIssues() {
		for (Issue issue : issues)
			issue.recalculateRegularity(firstAppearance, lastAppearance);
	}

	/**
	 * The function removeIssue() removes the specified Issue from this Title if
	 * it is present.
	 * 
	 * @param issue
	 *            Issue to be removed from the set
	 * @return true if the set was changed
	 */
	public boolean removeIssue(Issue issue) {
		if (issue.countIndividualIssues(firstAppearance, lastAppearance) > 0)
			course.clearProcesses();
		return issues.remove(issue);
	}

	/**
	 * The method setFirstAppearance() sets a LocalDate as day of first
	 * appearance for this Title.
	 * 
	 * @param firstAppearance
	 *            date of first appearance
	 * @throws IllegalArgumentException
	 *             if the date would overlap with another block
	 */
	public void setFirstAppearance(LocalDate firstAppearance) throws IllegalArgumentException {
		prohibitOverlaps(firstAppearance, lastAppearance != null ? lastAppearance : firstAppearance);
		try {
			if (!this.firstAppearance.equals(firstAppearance))
				course.clearProcesses();
		} catch (NullPointerException e) {
			if (this.firstAppearance == null ^ firstAppearance == null)
				course.clearProcesses();
		}
		this.firstAppearance = firstAppearance;
	}

	/**
	 * The method setLastAppearance() sets a LocalDate as day of last appeanance
	 * for this Title.
	 * 
	 * @param lastAppearance
	 *            date of last appearance
	 * @throws IllegalArgumentException
	 *             if the date would overlap with another block
	 */
	public void setLastAppearance(LocalDate lastAppearance) {
		prohibitOverlaps(firstAppearance != null ? firstAppearance : lastAppearance, lastAppearance);
		try {
			if (!this.lastAppearance.equals(lastAppearance))
				course.clearProcesses();
		} catch (NullPointerException e) {
			if (this.lastAppearance == null ^ lastAppearance == null)
				course.clearProcesses();
		}
		this.lastAppearance = lastAppearance;
	}

	/**
	 * The method checkForOverlaps() tests an not yet set time range for this
	 * title whether it doesn’t overlap with other titles in this course and can
	 * be set. (Because this method is called prior to setting a new value as a
	 * field value, it doesn’t take the values from the classes’ fields even
	 * though it isn’t static.) If the given dates would cause an overlapping,
	 * an IllegalArgumentException will be thrown.
	 * 
	 * @param from
	 *            date of first appearance to check
	 * @param until
	 *            date of last appearance to check
	 * @throws IllegalArgumentException
	 *             if the check fails
	 */
	private void prohibitOverlaps(LocalDate from, LocalDate until) throws IllegalArgumentException {
		for (Title title : course)
			if (title != this
					&& (title.getFirstAppearance().isBefore(until) && !title.getLastAppearance().isBefore(from) || (title
							.getLastAppearance().isAfter(from) && !title.getFirstAppearance().isAfter(until))))
				throw new IllegalArgumentException();
	}

	/**
	 * The function toString() provides returns a string that contains a concise
	 * but informative representation of this title that is easy for a person to
	 * read.
	 * 
	 * @return a string representation of the title
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (firstAppearance != null)
			result.append(firstAppearance.toString());
		result.append(" - ");
		if (lastAppearance != null)
			result.append(lastAppearance.toString());
		result.append(" [");
		boolean first = true;
		for (Issue issue : issues) {
			if (!first)
				result.append(", ");
			result.append(issue.toString());
			first = false;
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * The function toString() provides returns a string that contains a textual
	 * representation of this title that is easy for a person to read.
	 * 
	 * @param dateConverter
	 *            a DateTimeFormatter for formatting the local dates
	 * @return a string to identify the title
	 */
	public String toString(DateTimeFormatter dateConverter) {
		StringBuilder result = new StringBuilder();
		if (firstAppearance != null)
			result.append(dateConverter.print(firstAppearance));
		result.append(" − ");
		if (lastAppearance != null)
			result.append(dateConverter.print(lastAppearance));
		return result.toString();
	}

	/**
	 * Returns a hash code for the object which depends on the content of its
	 * variables. Whenever Title objects are held in HashSet objects, a
	 * hashCode() is essentially necessary.
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
		result = prime * result + ((firstAppearance == null) ? 0 : firstAppearance.hashCode());
		result = prime * result + ((issues == null) ? 0 : issues.hashCode());
		result = prime * result + ((lastAppearance == null) ? 0 : lastAppearance.hashCode());
		result = prime * result + ((variant == null) ? 0 : variant.hashCode());
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
		if (!(obj instanceof Title))
			return false;
		Title other = (Title) obj;
		if (firstAppearance == null) {
			if (other.firstAppearance != null)
				return false;
		} else if (!firstAppearance.equals(other.firstAppearance))
			return false;
		if (issues == null) {
			if (other.issues != null)
				return false;
		} else if (!issues.equals(other.issues))
			return false;
		if (lastAppearance == null) {
			if (other.lastAppearance != null)
				return false;
		} else if (!lastAppearance.equals(other.lastAppearance))
			return false;
		if (variant == null) {
			if (other.variant != null)
				return false;
		} else if (!variant.equals(other.variant))
			return false;
		return true;
	}
}

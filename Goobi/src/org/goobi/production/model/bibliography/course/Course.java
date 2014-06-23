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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.sub.goobi.helper.XMLUtils;

/**
 * The class Course represents the course of appearance of a newspaper.
 * 
 * <p>
 * A course of appearance consists of one or more Title elements. Interruptions
 * in the course of appearance can be modeled by subsequent Titles with the same
 * heading. In case that the newspaper changed its name, a new Title is
 * required, too.
 * </p>
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class Course extends ArrayList<Title> {
	private static final long serialVersionUID = 1L;

	/**
	 * Attribute <code>date="…"</code> used in the XML representation of a
	 * course of appearance.
	 */
	private static final String ATTRIBUTE_DATE = "date";

	/**
	 * Attribute <code>index="…"</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * The attribute <code>index="…"</code> is optional. It may be used to
	 * distinguish different title block with the same title (value from
	 * <code>heading="…"</code> attribute).
	 * </p>
	 */
	private static final String ATTRIBUTE_VARIANT = "index";

	/**
	 * Attribute <code>issue="…"</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * The attribute <code>issue="…"</code> holds the name of the issue.
	 * Newspapers, especially bigger ones, can have several issues that, e.g.,
	 * may differ in time of publication (morning issue, evening issue, …) or
	 * geographic distribution (Edinburgh issue, London issue, …).
	 * </p>
	 */
	private static final String ATTRIBUTE_ISSUE_HEADING = "issue";

	/**
	 * Element <code>&lt;appeared&gt;</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * Each <code>&lt;appeared&gt;</code> element represents one issue that
	 * physically appeared. It has the attributes <code>issue="…"</code>
	 * (required, may be empty) and <code>date="…"</code> (required) and cannot
	 * hold child elements.
	 * </p>
	 */
	private static final String ELEMENT_APPEARED = "appeared";

	/**
	 * Element <code>&lt;course&gt;</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * <code>&lt;course&gt;</code> is the root element of the XML
	 * representation. It can hold two children,
	 * <code>&lt;description&gt;</code> (output only, optional) and
	 * <code>&lt;processes&gt;</code> (required).
	 * </p>
	 */
	private static final String ELEMENT_COURSE = "course";

	/**
	 * Element <code>&lt;description&gt;</code> used in the XML representation
	 * of a course of appearance.
	 * 
	 * <p>
	 * <code>&lt;description&gt;</code> holds a verbal, human-readable
	 * description of the course of appearance, which is generated only and
	 * doesn’t have an effect on input.
	 * </p>
	 */
	private static final String ELEMENT_DESCRIPTION = "description";

	/**
	 * Element <code>&lt;process&gt;</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * Each <code>&lt;process&gt;</code> element represents one process to be
	 * generated in Goobi Production. It can hold <code>&lt;title&gt;</code>
	 * elements (of any quantity).
	 * </p>
	 */
	private static final String ELEMENT_PROCESS = "process";

	/**
	 * Element <code>&lt;processes&gt;</code> used in the XML representation of
	 * a course of appearance.
	 * 
	 * <p>
	 * Each <code>&lt;processes&gt;</code> element represents the processes to
	 * be generated in Goobi Production. It can hold
	 * <code>&lt;process&gt;</code> elements (of any quantity).
	 * </p>
	 */
	private static final String ELEMENT_PROCESSES = "processes";

	/**
	 * Element <code>&lt;title&gt;</code> used in the XML representation of a
	 * course of appearance.
	 * 
	 * <p>
	 * Each <code>&lt;title&gt;</code> element represents the title block the
	 * appeared issues belong to. It has the attributes <code>heading="…"</code>
	 * (required, must not be empty) and <code>index="…"</code> (optional) and
	 * can hold <code>&lt;appeared&gt;</code> elements (of any quantity).
	 * </p>
	 */
	private static final String ELEMENT_TITLE = "title";

	/**
	 * List of Lists of Issues, each representing a process.
	 */
	private final List<List<IndividualIssue>> processes = new ArrayList<List<IndividualIssue>>();
	private final Map<String, Title> resolveByTitleVariantCache = new HashMap<String, Title>();

	private boolean processesAreVolatile = true;

	/**
	 * Default constructor, creates an empty course. Must be made explicit since
	 * we offer other constructors, too.
	 */
	public Course() {
		super();
	}

	/**
	 * Constructor to create a course from an xml source
	 * 
	 * @param xml
	 *            XML document data structure
	 * @throws NoSuchElementException
	 *             if ELEMENT_COURSE or ELEMENT_PROCESSES cannot be found
	 */
	public Course(Document xml) throws NoSuchElementException {
		super();
		processesAreVolatile = false;
		Element rootNode = XMLUtils.getFirstChildWithTagName(xml, ELEMENT_COURSE);
		Element processesNode = XMLUtils.getFirstChildWithTagName(rootNode, ELEMENT_PROCESSES);
		int initialCapacity = 10;
		for (Node processNode = processesNode.getFirstChild(); processNode != null; processNode = processNode
				.getNextSibling()) {
			if (!(processNode instanceof Element) || !processNode.getNodeName().equals(ELEMENT_PROCESS))
				continue;
			List<IndividualIssue> process = new ArrayList<IndividualIssue>(initialCapacity);
			for (Node titleNode = processNode.getFirstChild(); titleNode != null; titleNode = titleNode
					.getNextSibling()) {
				if (!(titleNode instanceof Element) || !titleNode.getNodeName().equals(ELEMENT_TITLE))
					continue;
				String variant = ((Element) titleNode).getAttribute(ATTRIBUTE_VARIANT);
				for (Node issueNode = titleNode.getFirstChild(); issueNode != null; issueNode = issueNode
						.getNextSibling()) {
					if (!(issueNode instanceof Element) || !issueNode.getNodeName().equals(ELEMENT_APPEARED))
						continue;
					String issue = ((Element) issueNode).getAttribute(ATTRIBUTE_ISSUE_HEADING);
					if (issue == null)
						issue = "";
					String date = ((Element) issueNode).getAttribute(ATTRIBUTE_DATE);
					IndividualIssue individualIssue = addAddition(variant, issue, LocalDate.parse(date));
					process.add(individualIssue);
				}
			}
			processes.add(process);
			initialCapacity = (int) Math.round(1.1 * process.size());
		}
		recalculateRegularityOfIssues();
		processesAreVolatile = true;
	}

	/**
	 * Appends the specified Title to the end of this course.
	 * 
	 * @param title
	 *            title to be appended to this course
	 * @return true (as specified by Collection.add(E))
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(Title title) {
		super.add(title);
		if (title.countIndividualIssues() > 0)
			processes.clear();
		return true;
	}

	/**
	 * Adds a LocalDate to the set of additions of the issue identified by
	 * issueHeading in the title block identified by titleHeading
	 * and—optionally—variant. Note that in case that the date is outside the
	 * time range of the described title block, the time range will be expanded.
	 * Do not use this function in contexts where there is one or more issues in
	 * the block that have a regular appearance set, because in this case the
	 * regularly appeared issues in the expanded block will show up later, too,
	 * which is probably not what you want.
	 * 
	 * @param titleHeading
	 *            heading of the title this issue is in
	 * @param variant
	 *            variant of the title heading (may be null)
	 * @param issueHeading
	 *            heading of the issue this issue is of
	 * @param date
	 *            date to add
	 * @return an IndividualIssue representing the added issue
	 */
	private IndividualIssue addAddition(String variant, String issueHeading, LocalDate date) {
		Title title = get(variant);
		if (title == null) {
			title = new Title(this, variant);
			title.setFirstAppearance(date);
			title.setLastAppearance(date);
			add(title);
		} else {
			if (title.getFirstAppearance().isAfter(date))
				title.setFirstAppearance(date);
			if (title.getLastAppearance().isBefore(date))
				title.setLastAppearance(date);
		}
		Issue issue = title.getIssue(issueHeading);
		if (issue == null) {
			issue = new Issue(this, issueHeading);
			title.addIssue(issue);
		}
		issue.addAddition(date);
		return new IndividualIssue(title, issue, date);
	}

	void clearProcesses() {
		if (processesAreVolatile)
			processes.clear();
	}

	/**
	 * The method countIndividualIssues() determines how many stampings of
	 * issues physically appeared without generating a list of IndividualIssue
	 * objects.
	 * 
	 * @return the count of issues
	 */
	public long countIndividualIssues() {
		long result = 0;
		for (Title title : this)
			result += title.countIndividualIssues();
		return result;
	}

	/**
	 * Returns the title identified by the given title and—optionally—variant,
	 * or null if no title with the given combination can be found.
	 * 
	 * @param title
	 *            the heading of the title to be returned
	 * @param variant
	 *            the variant of the title (may be null)
	 * @return the title identified by the given title and—optionally—variant,
	 *         or null if no title with the given combination can be found
	 */
	private Title get(String variant) {
		if (resolveByTitleVariantCache.containsKey(variant)) {
			Title potentialResult = resolveByTitleVariantCache.get(variant);
			if (potentialResult.isIdentifiedBy(variant))
				return potentialResult;
			else
				resolveByTitleVariantCache.remove(variant);
		}
		for (Title candidate : this) {
			if (candidate.isIdentifiedBy(variant)) {
				resolveByTitleVariantCache.put(variant, candidate);
				return candidate;
			}
		}
		return null;
	}

	/**
	 * The function getIndividualIssues() generates a list of IndividualIssue
	 * objects, each of them representing a stamping of one physically appeared
	 * issue.
	 * 
	 * @return a LinkedHashSet of IndividualIssue objects, each of them
	 *         representing one physically appeared issue
	 */
	public LinkedHashSet<IndividualIssue> getIndividualIssues() {
		LinkedHashSet<IndividualIssue> result = new LinkedHashSet<IndividualIssue>();
		LocalDate lastAppearance = getLastAppearance();
		for (LocalDate day = getFirstAppearance(); !day.isAfter(lastAppearance); day = day.plusDays(1)) {
			for (Title title : this) {
				result.addAll(title.getIndividualIssues(day));
			}
		}
		return result;
	}

	/**
	 * The function getFirstAppearance() returns the date the regularity of this
	 * course of appearance starts with.
	 * 
	 * @return the date of first appearance
	 */
	public LocalDate getFirstAppearance() {
		if (super.isEmpty())
			return null;
		LocalDate result = super.get(0).getFirstAppearance();
		for (int index = 1; index < super.size(); index++) {
			LocalDate firstAppearance = super.get(index).getFirstAppearance();
			if (firstAppearance.isBefore(result))
				result = firstAppearance;
		}
		return result;
	}

	/**
	 * The function getLastAppearance() returns the date the regularity of this
	 * course of appearance ends with.
	 * 
	 * @return the date of last appearance
	 */
	public LocalDate getLastAppearance() {
		if (super.isEmpty())
			return null;
		LocalDate result = super.get(0).getLastAppearance();
		for (int index = 1; index < super.size(); index++) {
			LocalDate lastAppearance = super.get(index).getLastAppearance();
			if (lastAppearance.isAfter(result))
				result = lastAppearance;
		}
		return result;
	}

	/**
	 * The function getNumberOfProcesses() returns the number of processes into
	 * which the course of appearance will be split.
	 * 
	 * @return the number of processes
	 */
	public int getNumberOfProcesses() {
		return processes.size();
	}

	/**
	 * The function guessTotalNumberOfPages() calculates a guessed number of
	 * pages for a course of appearance of a newspaper, presuming each issue
	 * having 40 pages and Sunday issues having six times that size because most
	 * people buy the Sunday issue most often and therefore advertisers buy the
	 * most space on that day.
	 * 
	 * @return a guessed total number of pages for the full course of appearance
	 */
	public long guessTotalNumberOfPages() {
		final int WEEKDAY_PAGES = 40;
		final int SUNDAY_PAGES = 240;

		long result = 0;
		for (Title title : this) {
			LocalDate lastAppearance = title.getLastAppearance();
			for (LocalDate day = title.getFirstAppearance(); !day.isAfter(lastAppearance); day = day.plusDays(1)) {
				for (Issue issue : title.getIssues()) {
					if (issue.isMatch(day))
						result += day.getDayOfWeek() != DateTimeConstants.SUNDAY ? WEEKDAY_PAGES : SUNDAY_PAGES;
				}
			}
		}
		return result;
	}

	/**
	 * The function getProcesses() returns the processes to create from the
	 * course of appearance.
	 * 
	 * @return the processes
	 */
	public List<List<IndividualIssue>> getProcesses() {
		return processes;
	}

	/**
	 * The function isMatch() iterates over the array of title blocks and
	 * returns the first one that matches a given date. Since there shouldn’t be
	 * overlapping blocks, there should be at most one block for which this is
	 * true. If no matching block is found, it will return null.
	 * 
	 * @param date
	 *            a LocalDate to examine
	 * @return the title block on which this date is represented, if any
	 */
	public Title isMatch(LocalDate date) {
		for (Title title : this)
			if (title.isMatch(date))
				return title;
		return null;
	}

	/**
	 * The method recalculateRegularityOfIssues() recalculates for all Title
	 * objects of this Course for each Issue the daysOfWeek of its regular
	 * appearance within the interval of time of the Title. This is especially
	 * sensible to detect the underlying regularity after lots of issues whose
	 * existence is known have been added one by one as additions to the
	 * underlying issue(s).
	 */
	public void recalculateRegularityOfIssues() {
		for (Title title : this)
			title.recalculateRegularityOfIssues();
	}

	/**
	 * The function remove() removes the element at the specified position in
	 * this list. Shifts any subsequent elements to the left (subtracts one from
	 * their indices). Additionally, any references to the object held in the
	 * map used for resolving are being removed so that the object can be
	 * garbage-collected.
	 * 
	 * @param index
	 *            the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range (index < 0 || index >= size())
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public Title remove(int index) {
		Title title = super.remove(index);
		for (Map.Entry<String, Title> entry : resolveByTitleVariantCache.entrySet())
			if (entry.getValue() == title) // pointer equality
				resolveByTitleVariantCache.remove(entry.getKey());
		if (title.countIndividualIssues() > 0)
			processes.clear();
		return title;
	}

	/**
	 * The method splitInto() calculates the processes depending on the given
	 * BreakMode.
	 * 
	 * @param mode
	 *            how the course shall be broken into issues
	 */

	public void splitInto(Granularity mode) {
		int initialCapacity = 10;
		Integer lastMark = null;
		List<IndividualIssue> process = null;

		processes.clear();
		for (IndividualIssue issue : getIndividualIssues()) {
			Integer mark = issue.getBreakMark(mode);
			if (!mark.equals(lastMark) && process != null) {
				initialCapacity = (int) Math.round(1.1 * process.size());
				processes.add(process);
				process = null;
			}
			if (process == null)
				process = new ArrayList<IndividualIssue>(initialCapacity);
			process.add(issue);
			lastMark = mark;
		}
		if (process != null)
			processes.add(process);
	}

	/**
	 * The function toXML() transforms a course of appearance to XML.
	 * 
	 * @param lang
	 *            language to use for the “description”
	 * @return XML as String
	 */
	public Document toXML() {
		Document result = XMLUtils.newDocument();
		Element courseNode = result.createElement(ELEMENT_COURSE);

		Element description = result.createElement(ELEMENT_DESCRIPTION);
		description.appendChild(result.createTextNode(StringUtils.join(CourseToGerman.asReadableText(this), "\n\n")));
		courseNode.appendChild(description);

		Element processesNode = result.createElement(ELEMENT_PROCESSES);
		for (List<IndividualIssue> process : processes) {
			Element processNode = result.createElement(ELEMENT_PROCESS);
			Element titleNode = null;
			int previous = -1;
			for (IndividualIssue issue : process) {
				int index = issue.indexIn(this);
				if (index != previous && titleNode != null) {
					processNode.appendChild(titleNode);
					titleNode = null;
				}
				if (titleNode == null) {
					titleNode = result.createElement(ELEMENT_TITLE);
					titleNode.setAttribute(ATTRIBUTE_VARIANT, Integer.toString(index + 1));
				}
				Element issueNode = result.createElement(ELEMENT_APPEARED);
				if (issue != null)
					issueNode.setAttribute(ATTRIBUTE_ISSUE_HEADING, issue.getHeading());
				issueNode.setAttribute(ATTRIBUTE_DATE, issue.getDate().toString());
				titleNode.appendChild(issueNode);
				previous = index;
			}
			if (titleNode != null)
				processNode.appendChild(titleNode);
			processesNode.appendChild(processNode);
		}
		courseNode.appendChild(processesNode);

		result.appendChild(courseNode);
		return result;
	}
}

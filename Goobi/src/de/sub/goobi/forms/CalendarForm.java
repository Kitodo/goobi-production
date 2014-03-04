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
package de.sub.goobi.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.goobi.production.model.bibliography.course.Course;
import org.goobi.production.model.bibliography.course.Issue;
import org.goobi.production.model.bibliography.course.Title;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.sub.goobi.config.ConfigMain;

/**
 * The class CalendarForm provides the screen logic for a JSF calendar editor to
 * enter the course of appearance of a newspaper.
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class CalendarForm {

	/**
	 * The constant field DATE_FORMATTER provides a DateTimeFormatter that is
	 * used to convert between LocalDate objects and String in common German
	 * notation.
	 */
	protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy");

	/**
	 * The constant field EMPTY_LIST_OF_ISSUE_OPTIONS holds empty list of
	 * IssueOption. It is used to prevent null pointer exceptions, however one
	 * can use the same object everywhere.
	 */
	protected static final List<IssueOption> EMPTY_LIST_OF_ISSUE_OPTIONS = new ArrayList<IssueOption>(0);

	/**
	 * The constant field ISSUE_COLOURS holds the colours used to represent the
	 * issues in the calendar editor. It is populated on form bean creation, so
	 * changing the configuration should take effect without need to restart the
	 * servlet container.
	 */
	protected static String[] ISSUE_COLOURS;

	/**
	 * The field course holds the course of appearance currently under edit by
	 * this calendar form instance.
	 */
	protected Course course;

	/**
	 * The Map titlePickerResolver is populated with the IDs used in the title
	 * picker list box and the references to the Title objects referenced by the
	 * IDs for easily looking them up upon change.
	 */
	protected Map<String, Title> titlePickerResolver;

	/**
	 * The field titleShowing holds the Title block currently showing in this
	 * calendar instance. The Title held in titleShowing must be part of the
	 * course object, too.
	 */
	protected Title titleShowing;

	/**
	 * The field titlePickerUnchanged is of importance during the update model
	 * values phase of the JSF life-cycle. During that phase several setter
	 * methods are sequentially called. The first method called is
	 * setTitlePickerSelected(). If the user chose a different title block to be
	 * displayed, titleShowing will be altered. This would cause the subsequent
	 * calls to other setter methods to overwrite the values in the newly
	 * selected title block with the values of the previously displayed block
	 * which come back in in the form that is submitted by the browser if this
	 * is not blocked. Therefore setTitlePickerSelected() sets
	 * titlePickerUnchanged to control whether the other setter methods shall or
	 * shall not write the incoming data into the respective fields.
	 */
	protected boolean titlePickerUnchanged = true;

	/**
	 * The field yearShowing tells the year currently showing in this calendar
	 * instance.
	 */
	protected int yearShowing = 1979; // Cf. 42

	/**
	 * The class IssueController backs the control elements that are necessary
	 * to manage the properties of an issue using Faces, including the option of
	 * its deletion.
	 * 
	 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
	 */
	protected class IssueController {
		/**
		 * The field issue holds the issue that is managed by this controller.
		 */
		protected final Issue issue;

		/**
		 * The field index holds a consecutive index representing its position
		 * in the list of issues held by the title block.
		 */
		protected final int index;

		/**
		 * Constructor. Creates a new IssueController for the given issue and
		 * sets its index value.
		 * 
		 * @param issue
		 *            Issue that shall be managed by this controller
		 * @param index
		 *            consecutive index of the issue in the title block
		 */
		protected IssueController(Issue issue, int index) {
			this.issue = issue;
			this.index = index;
		}

		/**
		 * The method deleteClick() deletes the issue wrapped by this controller
		 * from the set of issues held by the title currently showing.
		 */
		public void deleteClick() {
			titleShowing.removeIssue(issue);
		}

		/**
		 * The method getColour() returns a colour representative for optically
		 * distinguishing the given issue as read-only property "colour".
		 * 
		 * @return the HTML colour code of the issue
		 */
		public String getColour() {
			return ISSUE_COLOURS[index % ISSUE_COLOURS.length];
		}

		/**
		 * The function getFriday() returns whether the issue held by this
		 * controller regularly appears on Fridays as read-write property
		 * "friday".
		 * 
		 * @return whether the issue appears on Fridays
		 */
		public boolean getFriday() {
			return issue.isFriday();
		}

		/**
		 * The function getHeading() returns the issue’s name as read-write
		 * property "heading".
		 * 
		 * @return the issue’s name
		 */
		public String getHeading() {
			return issue.getHeading();
		}

		/**
		 * The function getIssue() returns the issue held by this controller.
		 * 
		 * @return the issue managed by this adapter
		 */
		protected Issue getIssue() {
			return issue;
		}

		/**
		 * The function getMonday() returns whether the issue held by this
		 * controller regularly appears on Mondays as read-write property
		 * "monday".
		 * 
		 * @return whether the issue appears on Mondays
		 */
		public boolean getMonday() {
			return issue.isMonday();
		}

		/**
		 * The function getSaturday() returns whether the issue held by this
		 * controller regularly appears on Saturdays as read-write property
		 * "saturday".
		 * 
		 * @return whether the issue appears on Saturdays
		 */
		public boolean getSaturday() {
			return issue.isSaturday();
		}

		/**
		 * The function getSunday() returns whether the issue held by this
		 * controller regularly appears on Sundays as read-write property
		 * "sunday".
		 * 
		 * @return whether the issue appears on Sundays
		 */
		public boolean getSunday() {
			return issue.isSunday();
		}

		/**
		 * The function getThursday() returns whether the issue held by this
		 * controller regularly appears on Thursdays as read-write property
		 * "thursday".
		 * 
		 * @return whether the issue appears on Thursdays
		 */
		public boolean getThursday() {
			return issue.isThursday();
		}

		/**
		 * The function getTuesday() returns whether the issue held by this
		 * controller regularly appears on Tuesdays as read-write property
		 * "tuesday".
		 * 
		 * @return whether the issue appears on Tuesdays
		 */
		public boolean getTuesday() {
			return issue.isTuesday();
		}

		/**
		 * The function getWednesday() returns whether the issue held by this
		 * controller regularly appears on Wednesdays as read-write property
		 * "wednesday".
		 * 
		 * @return whether the issue appears on Wednesdays
		 */
		public boolean getWednesday() {
			return issue.isWednesday();
		}

		/**
		 * The method setFriday() will be called by Faces to store a new value
		 * of the read-write property "friday" which represents whether the
		 * issue held by this controller regularly appears on Fridays.
		 * 
		 * @param appears
		 *            whether the issue appears on Fridays
		 */
		public void setFriday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addFriday();
				else
					issue.removeFriday();
		}

		/**
		 * The method setMonday() will be called by Faces to store a new value
		 * of the read-write property "heading" which represents the issue’s
		 * name.
		 * 
		 * @param heading
		 *            heading to be used
		 */
		public void setHeading(String heading) {
			if (titlePickerUnchanged)
				issue.setHeading(heading);
		}

		/**
		 * The method setMonday() will be called by Faces to store a new value
		 * of the read-write property "monday" which represents whether the
		 * issue held by this controller regularly appears on Mondays.
		 * 
		 * @param appears
		 *            whether the issue appears on Mondays
		 */
		public void setMonday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addMonday();
				else
					issue.removeMonday();
		}

		/**
		 * The method setSaturday() will be called by Faces to store a new value
		 * of the read-write property "saturday" which represents whether the
		 * issue held by this controller regularly appears on Saturdays.
		 * 
		 * @param appears
		 *            whether the issue appears on Saturdays
		 */
		public void setSaturday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addSaturday();
				else
					issue.removeSaturday();
		}

		/**
		 * The method setSunday() will be called by Faces to store a new value
		 * of the read-write property "sunday" which represents whether the
		 * issue held by this controller regularly appears on Sundays.
		 * 
		 * @param appears
		 *            whether the issue appears on Sundays
		 */
		public void setSunday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addSunday();
				else
					issue.removeSunday();
		}

		/**
		 * The method setThursday() will be called by Faces to store a new value
		 * of the read-write property "thursday" which represents whether the
		 * issue held by this controller regularly appears on Thursdays.
		 * 
		 * @param appears
		 *            whether the issue appears on Thursdays
		 */
		public void setThursday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addThursday();
				else
					issue.removeThursday();
		}

		/**
		 * The method setTuesday() will be called by Faces to store a new value
		 * of the read-write property "tuesday" which represents whether the
		 * issue held by this controller regularly appears on Tuesdays.
		 * 
		 * @param appears
		 *            whether the issue appears on Tuesdays
		 */
		public void setTuesday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addTuesday();
				else
					issue.removeTuesday();
		}

		/**
		 * The method setWednesday() will be called by Faces to store a new
		 * value of the read-write property "wednesday" which represents whether
		 * the issue held by this controller regularly appears on Wednesdays.
		 * 
		 * @param appears
		 *            whether the issue appears on Wednesdays
		 */
		public void setWednesday(boolean appears) {
			if (titlePickerUnchanged)
				if (appears)
					issue.addWednesday();
				else
					issue.removeWednesday();
		}
	}

	/**
	 * The class Cell represents a single table cell on the calendar sheet.
	 * 
	 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
	 */
	public class Cell {
		/**
		 * The field date holds the date that this cell represents in the course
		 * of time.
		 */
		protected LocalDate date = null;

		/**
		 * The field issues holds the possible issues for that day.
		 */
		protected List<IssueOption> issues = EMPTY_LIST_OF_ISSUE_OPTIONS;

		/**
		 * The field onTitle contains the statement, whether the day is covered
		 * by the currently showing title block (or othewise needs to be
		 * greyed-out in the front end).
		 */
		protected boolean onTitle = true; // do not grey out dates which aren’t defined by the calendar system

		/**
		 * The function getDay() returns the day of month (that is a number in
		 * 1−31) of the date the cell represents, followed by a full stop, as
		 * read-only property "day". For cells which are undefined by the
		 * calendar system, it returns the empty String.
		 * 
		 * @return the day of month in enumerative form
		 */
		public String getDay() {
			if (date == null)
				return "";
			return Integer.toString(date.getDayOfMonth()).concat(".");
		}

		/**
		 * The function getIssues() returns the issues that may have appeared on
		 * that day as read-only field “issues”.
		 * 
		 * @return the issues optionally appeared that day
		 */
		public List<IssueOption> getIssues() {
			return issues;
		}

		/**
		 * The function getStyleClass returns the CSS class names to be printed
		 * into the HTML to display the table cell state as read-only property
		 * “styleClass”.
		 * 
		 * @return the cell’s CSS style class name
		 */
		public String getStyleClass() {
			if (date == null)
				return "";
			if (onTitle) {
				switch (date.getDayOfWeek()) {
				case DateTimeConstants.SATURDAY:
					return "saturday";
				case DateTimeConstants.SUNDAY:
					return "sunday";
				default:
					return "weekday";
				}
			} else {
				switch (date.getDayOfWeek()) {
				case DateTimeConstants.SATURDAY:
					return "saturdayNoTitle";
				case DateTimeConstants.SUNDAY:
					return "sundayNoTitle";
				default:
					return "weekdayNoTitle";
				}
			}
		}

		/**
		 * The method setDate() sets the date represented by this calendar sheet
		 * cell.
		 * 
		 * @param date
		 *            the date represented by this calendar sheet cell
		 */
		protected void setDate(LocalDate date) {
			this.date = date;
		}

		/**
		 * The method setIssues() sets the list of possible issues for the date
		 * represented by this calendar sheet cell.
		 * 
		 * @param issues
		 *            the list of issues possible in this cell
		 */
		protected void setIssues(List<IssueOption> issues) {
			this.issues = issues;
		}

		/**
		 * The method setOnTitle() can be used to change the piece of
		 * information whether the day is covered by the currently showing title
		 * block or not.
		 * 
		 * @param onTitle
		 *            whether the day is covered by the currently showing title
		 *            block
		 */
		protected void setOnTitle(boolean onTitle) {
			this.onTitle = onTitle;
		}
	}

	/**
	 * The class IssuesOption represents the option that an Issue may have been
	 * issued on a certain day in history.
	 * 
	 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
	 */
	protected class IssueOption {
		/**
		 * The field date holds the date of this possible issue in the course of
		 * time.
		 */
		protected final LocalDate date;

		/**
		 * The field colour holds the colour representative for optically
		 * distinguishing the given issue
		 */
		protected final String colour;

		/**
		 * The field issue holds the issue this that this possible issue would
		 * be of.
		 */
		protected final Issue issue;

		/**
		 * Constructor for an IssueOption.
		 * 
		 * @param controller
		 *            IssueController class for that issue
		 * @param date
		 *            date of the issue option
		 */
		public IssueOption(IssueController controller, LocalDate date) {
			this.colour = controller.getColour();
			this.issue = controller.getIssue();
			this.date = date;
		}

		/**
		 * The function getColour() returns a colour representative for
		 * optically distinguishing the given issue as read-only property
		 * “colour”.
		 * 
		 * @return the HTML colour code of the issue
		 */
		public String getColour() {
			return colour;
		}

		/**
		 * The function getIssue() returns the issue’s name as read-only
		 * property “issue”.
		 * 
		 * @return the issue’s name
		 */
		public String getIssue() {
			return issue.getHeading();
		}

		/**
		 * The function getSelected() returns whether the issue appeared on the
		 * given date as read-only property “selected”, taking into
		 * consideration the daysOfWeek of regular appearance, the Set of
		 * exclusions and the Set of additions.
		 * 
		 * @return whether the issue appeared that day
		 */
		public boolean getSelected() {
			return issue.isMatch(date);
		}

		/**
		 * The method selectClick() is executed if the user clicks an issue
		 * option in unselected state. If this is an exception, the exception
		 * will be removed. Otherwise, an additional issue will be added.
		 */
		public void selectClick() {
			if (issue.isDayOfWeek(date.getDayOfWeek()))
				issue.removeExclusion(date);
			else
				issue.addAddition(date);
		}

		/**
		 * The method unselectClick() is executed if the user clicks an issue
		 * option in selected state. If this is regular appearance of that
		 * issue, an exception will be added. Otherwise, the additional issue
		 * will be removed.
		 */
		public void unselectClick() {
			if (issue.isDayOfWeek(date.getDayOfWeek()))
				issue.addExclusion(date);
			else
				issue.removeAddition(date);
		}
	}

	/**
	 * Empty constructor. Creates a new form without yet any data.
	 * 
	 * <p>
	 * The issue colour presets are samples which have been chosen to provide
	 * distinguishability also for users with red-green color vision deficiency.
	 * Arbitrary colours can be defined in goobi_config.properties by setting
	 * the property “issue.colours”.
	 * </p>
	 */
	public CalendarForm() {
		ISSUE_COLOURS = ConfigMain.getParameter("issue.colours",
				"#CC0000;#0000AA;#33FF00;#FF9900;#5555FF;#006600;#AAAAFF;#000055;#0000FF;#FFFF00;#000000").split(";");
		course = new Course();
		titlePickerResolver = new HashMap<String, Title>();
		titleShowing = null;
	}

	/**
	 * The method addTitleClick() creates and adds a copy of the currently
	 * showing title block.
	 */
	public void addTitleClick() {
		Title copy = titleShowing.clone();
		LocalDate firstAppearance = course.getLastAppearance().plusDays(1);
		copy.setFirstAppearance(firstAppearance);
		copy.setLastAppearance(firstAppearance);
		course.add(copy);
		titleShowing = copy;
		navigate();
	}

	/**
	 * The method addIssueClick() adds a new issue to the set of issues held by
	 * the title currently showing.
	 */
	public void addIssueClick() {
		titleShowing.addIssue(new Issue());
	}

	/**
	 * The method backwardClick() flips the calendar sheet back one year in
	 * time.
	 */
	public void backwardClick() {
		yearShowing -= 1;
	}

	/**
	 * The method forwardClick() flips the calendar sheet forward one year in
	 * time.
	 */
	public void forwardClick() {
		yearShowing += 1;
	}

	/**
	 * The function getBlank() returns whether the calendar editor is in mint
	 * condition, i.e. there is no title block defined yet, as read-only
	 * property “blank”.
	 * 
	 * <p>
	 * Side note: “empty” is a reserved word in JSP and cannot be used as
	 * property name.
	 * </p>
	 * 
	 * @return whether there is no title block yet
	 */
	public boolean getBlank() {
		return course.size() == 0;
	}

	/**
	 * The function getCalendarSheet() returns the data required to build the
	 * calendar sheet as read-only property "calendarSheet". The outer list
	 * contains 31 entries, each representing a row of the calendar (the days
	 * 1−31), each line then contains 12 cells representing the months. This is
	 * due to HTML table being produced line by line.
	 * 
	 * @return
	 */
	public List<List<Cell>> getCalendarSheet() {
		List<List<Cell>> result = getEmptySheet();
		populateByCalendar(result);
		return result;
	}

	/**
	 * The function getEmptySheet() builds the empty calendar sheet with 31 rows
	 * of twelve cells with empty objects of type Cell().
	 * 
	 * @return an empty calendar sheet
	 */
	protected List<List<Cell>> getEmptySheet() {
		List<List<Cell>> result = new ArrayList<List<Cell>>(31);
		for (int day = 1; day <= 31; day++) {
			ArrayList<Cell> row = new ArrayList<Cell>(DateTimeConstants.DECEMBER);
			for (int month = 1; month <= 12; month++)
				row.add(new Cell());
			result.add(row);
		}
		return result;
	}

	/**
	 * The method populateByCalendar() populates an empty calendar sheet by
	 * iterating on LocalDate.
	 * 
	 * @param sheet
	 *            calendar sheet to populate
	 */
	protected void populateByCalendar(List<List<Cell>> sheet) {
		List<IssueController> issueControllersCreatedOnce = getIssues();
		ReadablePartial nextYear = new LocalDate(yearShowing + 1, DateTimeConstants.JANUARY, 1);
		for (LocalDate date = new LocalDate(yearShowing, DateTimeConstants.JANUARY, 1); date.isBefore(nextYear); date = date
				.plusDays(1)) {
			Cell cell = sheet.get(date.getDayOfMonth() - 1).get(date.getMonthOfYear() - 1);
			cell.setDate(date);
			if (titleShowing == null || !titleShowing.isMatch(date)) {
				cell.setOnTitle(false);
			} else {
				cell.setIssues(buildIssueOptions(issueControllersCreatedOnce, date));
			}
		}
	}

	/**
	 * The function buildIssueOptions() creates a list of issueOptions for a
	 * given date.
	 * 
	 * @param issueControllers
	 *            the list of issue controllers in question
	 * @param date
	 *            the date in question
	 * @return a list of issue options for the date
	 */
	protected List<IssueOption> buildIssueOptions(List<IssueController> issueControllers, LocalDate date) {
		List<IssueOption> result = new ArrayList<IssueOption>();
		for (IssueController controller : issueControllers)
			result.add(new IssueOption(controller, date));
		return result;
	}

	/**
	 * The function getFirstAppearance() returns the date of first appearance of
	 * the Title block currently showing as read-write property
	 * "firstAppearance".
	 * 
	 * @return date of first appearance of currently showing title
	 */
	public String getFirstAppearance() {
		if (titleShowing != null && titleShowing.getFirstAppearance() != null)
			return DATE_FORMATTER.print(titleShowing.getFirstAppearance());
		else
			return "";
	}

	/**
	 * The function getIssues() returns the list of issues held by the title
	 * block currently showing read-only property "issues".
	 * 
	 * @return the list of issues
	 */
	public List<IssueController> getIssues() {
		List<IssueController> result = new ArrayList<IssueController>();
		if (titleShowing != null)
			for (Issue issue : titleShowing.getIssues())
				result.add(new IssueController(issue, result.size()));
		return result;
	}

	/**
	 * The function getLastAppearance() returns the date of last appearance of
	 * the Title block currently showing as read-write property
	 * "lastAppearance".
	 * 
	 * @return date of last appearance of currently showing title
	 */
	public String getLastAppearance() {
		if (titleShowing != null && titleShowing.getLastAppearance() != null)
			return DATE_FORMATTER.print(titleShowing.getLastAppearance());
		else
			return "";
	}

	/**
	 * The function getTitleHeading() returns the heading of the Title block
	 * currently showing as read-write property "titleHeading".
	 * 
	 * @return heading of currently showing title
	 */
	public String getTitleHeading() {
		if (titleShowing == null)
			return "";
		return titleShowing.getHeading();
	}

	/**
	 * The function getTitlePickerOptions() returns the elements for the title
	 * picker list box as read only property "titlePickerOptions". It returns a
	 * List of Map with each two entries: "value" and "label". "value" is the
	 * hashCode() in hex of the Title which will later be used if the field
	 * "titlePickerSelected" is altered to choose the currently selected title
	 * block, "label" is the readable description to be shown to the user.
	 * 
	 * @return the elements for the title picker list box
	 */
	public List<Map<String, String>> getTitlePickerOptions() {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		for (Title title : course) {
			String value = Integer.toHexString(title.hashCode());
			titlePickerResolver.put(value, title);
			Map<String, String> item = new HashMap<String, String>();
			item.put("value", value);
			item.put("label", title.toString(DATE_FORMATTER));
			result.add(item);
		}
		return result;
	}

	/**
	 * The function getTitlePickerSelected() returns the hashCode() value of the
	 * title block currently selected as read-write property
	 * "titlePickerSelected". If a new block is under edit, returns the empty
	 * String.
	 * 
	 * @return identifier of the selected title
	 */
	public String getTitlePickerSelected() {
		return titleShowing == null ? "" : Integer.toHexString(titleShowing.hashCode());
	}

	/**
	 * The function getYear() returns the year to be shown in the calendar sheet
	 * as read-only property "year".
	 * 
	 * @return
	 */
	public String getYear() {
		return Integer.toString(yearShowing);
	}

	/**
	 * The method navigate() alters the year the calendar sheet is shown for so
	 * that something of the current title block is visible to prevent the user
	 * from needing to click through centuries manually to get there.
	 */
	protected void navigate() {
		try {
			if (yearShowing > titleShowing.getLastAppearance().getYear())
				yearShowing = titleShowing.getLastAppearance().getYear();
			if (yearShowing < titleShowing.getFirstAppearance().getYear())
				yearShowing = titleShowing.getFirstAppearance().getYear();
		} catch (NullPointerException e) {
		}
	}

	/**
	 * The method removeTitleClick() deletes the currently selected Title block
	 * from the course of appearance.The method is not intended to be used if
	 * there is only one block left.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the title referenced by “titleShowing” isn’t contained in
	 *             the course of appearance
	 */
	public void removeTitleClick() {
		assert course.size() > 1;
		int index = course.indexOf(titleShowing);
		course.remove(index);
		if (index > 0)
			index--;
		titleShowing = course.get(index);
		navigate();
	}

	/**
	 * The method setTitleHeading() will be called by Faces to store a new value
	 * of the read-write property "firstAppearance", which represents the the
	 * date of first appearance of the Title block currently showing. The event
	 * will be used to either alter the date of first appearance of the Title
	 * block defined by the “titleShowing” field or, in case that a new title
	 * block is under edit, to initially set its the date of first appearance.
	 * 
	 * @param firstAppearance
	 *            new date of first appearance
	 */
	public void setFirstAppearance(String firstAppearance) {
		LocalDate newFirstAppearance = DATE_FORMATTER.parseLocalDate(firstAppearance);
		if (titleShowing != null) {
			if (titlePickerUnchanged) {
				if (titleShowing.getFirstAppearance() == null
						|| !titleShowing.getFirstAppearance().isEqual(newFirstAppearance)) {
					titleShowing.setFirstAppearance(newFirstAppearance);
					navigate();
				}
			}
		} else {
			titleShowing = new Title();
			titleShowing.setFirstAppearance(newFirstAppearance);
			course.add(titleShowing);
		}
	}

	/**
	 * The method setTitleHeading() will be called by Faces to store a new value
	 * of the read-write property "lastAppearance", which represents the the
	 * date of last appearance of the Title block currently showing. The event
	 * will be used to either alter the date of last appearance of the Title
	 * block defined by the “titleShowing” field or, in case that a new title
	 * block is under edit, to initially set its the date of last appearance.
	 * 
	 * @param lastAppearance
	 *            new date of last appearance
	 */
	public void setLastAppearance(String lastAppearance) {
		LocalDate newLastAppearance = DATE_FORMATTER.parseLocalDate(lastAppearance);
		if (titleShowing != null) {
			if (titlePickerUnchanged) {
				if (titleShowing.getLastAppearance() == null
						|| !titleShowing.getLastAppearance().isEqual(newLastAppearance)) {
					titleShowing.setLastAppearance(newLastAppearance);
					navigate();
				}
			}
		} else {
			titleShowing = new Title();
			titleShowing.setLastAppearance(newLastAppearance);
			course.add(titleShowing);
		}
	}

	/**
	 * The method setTitleHeading() will be called by Faces to store a new value
	 * of the read-write property "titleHeading", which represents the heading
	 * of the Title block currently showing. The event will be used to either
	 * alter the heading of the Title block defined by the “titleShowing” field
	 * or, in case that a new title block is under edit, to initially set its
	 * title.
	 * 
	 * @param heading
	 *            new heading for the title block
	 */
	public void setTitleHeading(String heading) {
		if (titleShowing != null) {
			if (titlePickerUnchanged)
				titleShowing.setHeading(heading);
		} else {
			titleShowing = new Title(heading);
			course.add(titleShowing);
		}
	}

	/**
	 * The method setTitlePickerSelected() will be called by Faces to store a
	 * new value of the read-write property "titlePickerSelected". If it is
	 * different from the current one, this means that the user selected a
	 * different Title block in the title picker list box. The event will be
	 * used to alter the “titleShowing” field which keeps the Title block
	 * currently showing. “updateAllowed” will be set accordingly to update the
	 * contents of the current title block or to prevent fields containing data
	 * from the previously displaying title block to overwrite the data inside
	 * the newly selected one.
	 * 
	 * @param value
	 *            hashCode() in hex of the Title to be selected
	 */
	public void setTitlePickerSelected(String value) {
		if (value == null)
			return;
		titlePickerUnchanged = value.equals(Integer.toHexString(titleShowing.hashCode()));
		if (!titlePickerUnchanged) {
			titleShowing = titlePickerResolver.get(value);
			navigate();
		}
	}
}

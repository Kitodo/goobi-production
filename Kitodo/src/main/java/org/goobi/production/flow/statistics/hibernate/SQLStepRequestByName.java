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

package org.goobi.production.flow.statistics.hibernate;

import de.sub.goobi.helper.enums.HistoryEventType;

import java.util.Date;
import java.util.List;

import org.goobi.production.flow.statistics.enums.TimeUnit;

/**
 * Class provides SQL for Step Requests statistics on the history table it offers a little more functionality compared
 * to the other SQL Source classes. There are a little more parameters which can be set
 *
 * @author Wulf Riebensahm
 */
public class SQLStepRequestByName extends SQLGenerator {
	/**
	 * @param timeFrom add description
	 * @param timeTo add description
	 * @param timeUnit add description
	 * @param ids add description
	 */
	public SQLStepRequestByName(Date timeFrom, Date timeTo, TimeUnit timeUnit, List<Integer> ids) {
		// "history.processid - overrides the default value of prozesse.prozesseID
		// which is set in super class SQLGenerator
		super(timeFrom, timeTo, timeUnit, ids, "processid");
	}

	/**
	 * This is an extended SQL generator for an SQL extracting data from the historyEvent log.
	 * Depending on the parameters the query returns up to four fields
	 * (non-Javadoc)
	 * @see org.goobi.production.flow.statistics.hibernate.SQLGenerator#getSQL()
	 *
	 * @param typeSelection - operates as additional filter
	 * @param stepName - operates as additional filter
	 * @param stepOrderGrouping - 'stepName' fields in select and in group by clause
	 * @param includeLoops - adding additional stepOpen from Correction and other loops
	 *
	 * @return SQLExpression for MySQL DBMS - default fields stepCount and intervall
	 */
	public String getSQL(HistoryEventType typeSelection, String stepName, Boolean stepOrderGrouping,
			Boolean includeLoops) {

		String timeLimiter = "h.date";
		String groupInnerSelect = "";

		// evaluate if groupingFunction comes along with HistoryEventType
		// and if so implement this function in sql
		if (typeSelection.getGroupingFunction() != null && !includeLoops) {
			timeLimiter = typeSelection.getGroupingFunction() + "(h.date)";
			groupInnerSelect = " GROUP BY processid, numericvalue, stringvalue ";
		}

		String subQuery = "";
		String outerWhereClauseTimeFrame = getWhereClauseForTimeFrame(myTimeFrom, myTimeTo, "timeLimiter");
		String outerWhereClause = "";

		if (outerWhereClauseTimeFrame.length() > 0) {
			outerWhereClause = "WHERE " + outerWhereClauseTimeFrame;
		}

		// inner table -> alias "table_1"
		String innerWhereClause;

		if (myIdsCondition != null) {
			// adding ids to the where clause
			innerWhereClause = "(h.type=" + typeSelection.getValue().toString() + ")  AND (" + myIdsCondition + ") ";
		} else {
			innerWhereClause = "(h.type=" + typeSelection.getValue().toString() + ") ";
		}

		// adding a stepOrder filter to numericvalue if parameter is set
		if (stepName != null) {
			innerWhereClause = innerWhereClause + " AND h.stringvalue='" + stepName + "' ";
		}

		subQuery = "(SELECT numericvalue AS 'stepOrder', " + getIntervallExpression(myTimeUnit, "history.date") + " "
				+ "AS 'intervall', history.date AS 'timeLimiter', history.stringvalue AS 'stepName' " + "FROM "

				+ "(SELECT DISTINCT h.numericvalue, h.stringvalue, " + timeLimiter + " as date, h.processid, h.type "
				+ "FROM history h " + "WHERE " + innerWhereClause + groupInnerSelect + ") AS history " + ") AS table_1";

		mySql = "SELECT count(table_1.stepName) AS 'stepCount', table_1.intervall AS 'intervall' "
				+ addedListing(stepOrderGrouping) + "FROM " + subQuery + " " + outerWhereClause
				+ " GROUP BY table_1.intervall" + addedGrouping(stepOrderGrouping) + " ORDER BY  table_1.intervall"
				+ addedSorting(stepOrderGrouping);

		return mySql;
	}

	/** Method is purposfully not implemented. Method getSQL is overloaded
	 *  with parametered method.
	 *
	 * @see org.goobi.production.flow.statistics.hibernate.SQLGenerator#getSQL()
	 */
	@Override
	public String getSQL() {
		throw new UnsupportedOperationException(
				"The class "
						+ this.getClass().getName()
						+ " does not support the parameterless getSQL() method. Instead you need to use getSQL() "
						+ "with parameters.");
	}

	/**
	 * @param include add description
	 * @return SQL snippet for Order by clause
	 */

	private String addedSorting(Boolean include) {
		if (include) {
			return ", table_1.stepOrder";
		} else {
			return "";
		}
	}

	/**
	 * @param include add description
	 * @return SQL snippet for Select clause
	 */
	private String addedListing(Boolean include) {
		if (include) {
			return ",table_1.stepName ";
		} else {
			return "";
		}
	}

	/**
	 * @param include add description
	 * @return SQL snippet for Group by clause
	 */
	private String addedGrouping(Boolean include) {
		if (include) {
			return ", table_1.stepName ";
		} else {
			return "";
		}
	}

	/**
	 * @param eventSelection add description
	 * @return SQL String to retrieve the highest numericvalue (stepOrder) for the event defined in eventSelection
	 */
	public String SQLMaxStepOrder(HistoryEventType eventSelection) {

		String timeRestriction;
		String innerWhereClause = null;
		if (myIdsCondition != null) {
			// adding ids to the where clause
			innerWhereClause = "(history.type=" + eventSelection.getValue().toString() + ")  AND (" + myIdsCondition
					+ ") ";
		} else {
			innerWhereClause = "(history.type=" + eventSelection.getValue().toString() + ") ";
		}

		timeRestriction = getWhereClauseForTimeFrame(myTimeFrom, myTimeTo, "history.date");

		if (timeRestriction.length() > 0) {
			innerWhereClause = innerWhereClause.concat(" AND " + timeRestriction);
		}

		return "SELECT max(history.numericvalue) AS maxStep FROM history WHERE " + innerWhereClause;
	}

	/**
	 * @param eventSelection add description
	 * @return SQL String to retrieve the lowest numericvalue (stepOrder) for the event defined in eventSelection
	 */
	public String SQLMinStepOrder(HistoryEventType eventSelection) {

		String timeRestriction;
		String innerWhereClause = null;
		if (myIdsCondition != null) {
			// adding ids to the where clause
			innerWhereClause = "(history.type=" + eventSelection.getValue().toString() + ")  AND (" + myIdsCondition
					+ ") ";
		} else {
			innerWhereClause = "(history.type=" + eventSelection.getValue().toString() + ") ";
		}

		timeRestriction = getWhereClauseForTimeFrame(myTimeFrom, myTimeTo, "history.date");

		if (timeRestriction.length() > 0) {
			innerWhereClause = innerWhereClause.concat(" AND " + timeRestriction);
		}

		return "SELECT min(history.numericvalue) AS minStep FROM history WHERE " + innerWhereClause;
	}

}

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

package org.goobi.production.chart;

import de.sub.goobi.beans.Projekt;
import de.sub.goobi.beans.Schritt;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.StepStatus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * This class implements the IProvideProjectTaskList and approaches the problem by using a projection on the hibernate
 * criteria, which accelerates data  retrieval
 *
 * @author Wulf Riebensahm
 */

public class HibernateProjectionProjectTaskList implements IProvideProjectTaskList {
	private static final Logger logger = Logger.getLogger(HibernateProjectionProjectTaskList.class);

	@Override
	public List<IProjectTask> calculateProjectTasks(Projekt inProject, Boolean countImages, Integer inMax) {
		List<IProjectTask> myTaskList = new ArrayList<IProjectTask>();
		calculate(inProject, myTaskList, countImages, inMax);
		return myTaskList;
	}

	@SuppressWarnings("rawtypes")
	private synchronized void calculate(Projekt inProject, List<IProjectTask> myTaskList, Boolean countImages,
			Integer inMax) {

		Session session = Helper.getHibernateSession();
		Criteria crit = session.createCriteria(Schritt.class);

		crit.createCriteria("prozess", "proc");

		crit.addOrder(Order.asc("reihenfolge"));

		crit.add(Restrictions.eq("proc.istTemplate", Boolean.FALSE));
		crit.add(Restrictions.eq("proc.projekt", inProject));

		ProjectionList proList = Projections.projectionList();

		proList.add(Projections.property("titel"));
		proList.add(Projections.property("bearbeitungsstatus"));
		proList.add(Projections.sum("proc.sortHelperImages"));
		proList.add(Projections.count("id"));
		// proList.add(Projections.groupProperty(("reihenfolge")));

		proList.add(Projections.groupProperty(("titel")));
		proList.add(Projections.groupProperty(("bearbeitungsstatus")));

		crit.setProjection(proList);

		List list = crit.list();

		Iterator it = list.iterator();
		if (!it.hasNext()) {
			logger.debug("No any data!");
		} else {
			Integer rowCount = 0;
			while (it.hasNext()) {
				Object[] row = (Object[]) it.next();
				rowCount++;
				String message = "";
				int i;

				String shorttitle;
				if (((String) row[FieldList.stepName.getFieldLocation()]).length() > 60) {
					shorttitle = ((String) row[FieldList.stepName.getFieldLocation()]).substring(0, 60) + "...";
				} else {
					shorttitle = (String) row[FieldList.stepName.getFieldLocation()];
				}

				IProjectTask pt = null;
				for (IProjectTask task : myTaskList) {
					if (task.getTitle().equals(shorttitle)) {
						pt = task;
						break;
					}
				}

				if (pt == null) {
					pt = new ProjectTask(shorttitle, 0, 0);
					myTaskList.add(pt);
				}

				if (StepStatus.DONE.getValue().equals(row[FieldList.stepStatus.getFieldLocation()])) {
					if (countImages) {
						pt.setStepsCompleted((Integer) row[FieldList.pageCount.getFieldLocation()]);
					} else {
						pt.setStepsCompleted((Integer) row[FieldList.processCount.getFieldLocation()]);
					}
				}

				if (countImages) {
					pt.setStepsMax(pt.getStepsMax() + (Integer) row[FieldList.pageCount.getFieldLocation()]);
				} else {
					pt.setStepsMax(pt.getStepsMax() + (Integer) row[FieldList.processCount.getFieldLocation()]);
				}

				// TODO remove following lines all the way to system.out
				for (i = 0; i < row.length; i++) {
					message = message + "|" + row[i];
				}
				logger.debug(Integer.toString(rowCount) + message);

			}
		}

	}

	private enum FieldList {
		stepName(0), stepStatus(1), pageCount(2), processCount(3);

		Integer fieldLocation;

		FieldList(Integer fieldLocation) {
			this.fieldLocation = fieldLocation;
		}

		Integer getFieldLocation() {
			return this.fieldLocation;
		}
	}

}

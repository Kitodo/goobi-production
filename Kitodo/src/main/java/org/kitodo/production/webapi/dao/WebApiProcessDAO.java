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

package org.kitodo.production.webapi.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.persistence.HibernateUtil;
import org.kitodo.production.webapi.beans.IdentifierPPN;
import org.kitodo.production.webapi.beans.WebApiProcess;
import org.kitodo.production.webapi.beans.WebApiStep;

public class WebApiProcessDAO {

    private static final Logger logger = LogManager.getLogger(WebApiProcessDAO.class);

    /**
     * Private constructor to hide the implicit public one.
     */
    private WebApiProcessDAO() {

    }

    /**
     * Get process by PPN.
     *
     * @param ppn
     *            as IdentifierPPN
     * @return WebApiProcess
     */
    public static WebApiProcess getProcessByPPN(IdentifierPPN ppn) {
        Session session;
        WebApiProcess process = null;

        session = HibernateUtil.getSession();

        try {

            Criteria criteria = session.createCriteria(Process.class).createAlias("templates", "v")
                    .createAlias("templates.properties", "ve").createAlias("workpieces", "w")
                    .createAlias("workpieces.properties", "we")
                    .add(Restrictions.or(Restrictions.eq("we.title", "PPN digital a-Satz"),
                            Restrictions.eq("we.title", "PPN digital f-Satz")))
                    .add(Restrictions.eq("ve.title", "Titel")).add(Restrictions.eq("we.value", ppn.toString()))
                    .addOrder(Order.asc("we.value"))
                    .setProjection(Projections.projectionList().add(Projections.property("we.value"), "identifier")
                            .add(Projections.property("ve.value"), "title"))
                    .setResultTransformer(Transformers.aliasToBean(WebApiProcess.class));

            process = (WebApiProcess) criteria.uniqueResult();

        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }

        return process;
    }

    /**
     * Get all processes.
     *
     * @return List of WebApiProcess objects
     */
    public static List<WebApiProcess> getAllProcesses() {
        Session session = HibernateUtil.getSession();
        List<WebApiProcess> allProcesses = new ArrayList<>();

        try {
            Criteria criteria = session.createCriteria(Process.class).createAlias("templates", "v")
                    .createAlias("templates.properties", "ve").createAlias("workpieces", "w")
                    .createAlias("workpieces.properties", "we")
                    .add(Restrictions.or(Restrictions.eq("we.title", "PPN digital a-Satz"),
                            Restrictions.eq("we.title", "PPN digital f-Satz")))
                    .add(Restrictions.eq("ve.title", "Titel")).addOrder(Order.asc("we.value"))
                    .setProjection(Projections.projectionList().add(Projections.property("we.value"), "identifier")
                            .add(Projections.property("ve.value"), "title"))
                    .setResultTransformer(Transformers.aliasToBean(WebApiProcess.class));

            @SuppressWarnings(value = "unchecked")
            List<WebApiProcess> list = criteria.list();
            if (Objects.nonNull(list) && !list.isEmpty()) {
                allProcesses.addAll(list);
            }
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }

        return allProcesses;
    }

    /**
     * Get all process tasks.
     *
     * @param ppn
     *            as IdentifierPPN
     * @return List of WebApiStep objects
     */
    public static List<WebApiStep> getAllProcessSteps(IdentifierPPN ppn) {
        List<WebApiStep> allProcessSteps = new ArrayList<>();
        Session session = HibernateUtil.getSession();

        try {
            Criteria criteria = session.createCriteria(Task.class).createAlias("process", "p")
                    .createAlias("process.workpieces", "w").createAlias("process.workpieces.properties", "we")
                    .add(Restrictions.or(Restrictions.eq("we.title", "PPN digital a-Satz"),
                            Restrictions.eq("we.title", "PPN digital f-Satz")))
                    .add(Restrictions.eq("we.wert", ppn.toString())).addOrder(Order.asc("reihenfolge"))
                    .setProjection(Projections.projectionList().add(Projections.property("ordering"), "sequence")
                            .add(Projections.property("processingStatus"), "state").add(Projections.property("title"),
                                    "title"))
                    .setResultTransformer(Transformers.aliasToBean(WebApiStep.class));

            @SuppressWarnings(value = "unchecked")
            List<WebApiStep> list = criteria.list();

            if (Objects.nonNull(list) && !list.isEmpty()) {
                allProcessSteps.addAll(list);
            }
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }

        return allProcessSteps;
    }

}

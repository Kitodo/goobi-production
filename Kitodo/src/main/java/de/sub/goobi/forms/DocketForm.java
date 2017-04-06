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

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.Page;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.kitodo.data.database.beans.Docket;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.persistence.apache.ProcessManager;
import org.kitodo.services.ServiceManager;

public class DocketForm extends BasisForm {
    private static final long serialVersionUID = -445707928042517243L;
    private Docket myDocket = new Docket();
    private final ServiceManager serviceManager = new ServiceManager();
    private static final Logger logger = Logger.getLogger(DocketForm.class);

    public String Neu() {
        this.myDocket = new Docket();
        return "DocketEdit";
    }

    /**
     * Save docket.
     *
     * @return page or empty String
     */
    public String Speichern() {
        try {
            if (hasValidRulesetFilePath(myDocket, ConfigCore.getParameter("xsltFolder"))) {
                this.serviceManager.getDocketService().save(myDocket);
                return "DocketList";
            } else {
                Helper.setFehlerMeldung("DocketNotFound");
                return "";
            }
        } catch (DAOException e) {
            Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e.getMessage());
            logger.error(e);
            return "";
        } catch (IOException e) {
            Helper.setFehlerMeldung("errorElasticSearch", e.getMessage());
            logger.error(e);
            return "";
        }
    }

    private boolean hasValidRulesetFilePath(Docket d, String pathToRulesets) {
        File rulesetFile = new File(pathToRulesets + d.getFile());
        return rulesetFile.exists();
    }

    /**
     * Delete docket.
     *
     * @return page or empty String
     */
    public String Loeschen() {
        try {
            if (hasAssignedProcesses(myDocket)) {
                Helper.setFehlerMeldung("DocketInUse");
                return "";
            } else {
                this.serviceManager.getDocketService().remove(this.myDocket);
            }
        } catch (DAOException e) {
            Helper.setFehlerMeldung("fehlerNichtLoeschbar", e.getMessage());
            return "";
        } catch (IOException e) {
            Helper.setFehlerMeldung("errorElasticSearch", e.getMessage());
            return "";
        }
        return "DocketList";
    }

    private boolean hasAssignedProcesses(Docket d) {
        Integer number = ProcessManager.getNumberOfProcessesWithDocket(d.getId());
        if (number != null && number > 0) {
            return true;
        }
        return false;
    }

    /**
     * No filter.
     *
     * @return page or empty String
     */
    public String FilterKein() {
        try {
            // HibernateUtil.clearSession();
            Session session = Helper.getHibernateSession();
            // session.flush();
            session.clear();
            Criteria crit = session.createCriteria(Docket.class);
            crit.addOrder(Order.asc("name"));
            this.page = new Page(crit, 0);
        } catch (HibernateException he) {
            Helper.setFehlerMeldung("fehlerBeimEinlesen", he.getMessage());
            return "";
        }
        return "DocketList";
    }

    public String FilterKeinMitZurueck() {
        FilterKein();
        return this.zurueck;
    }

    /*
     * Getter und Setter
     */

    public Docket getMyDocket() {
        return this.myDocket;
    }

    public void setMyDocket(Docket docket) {
        Helper.getHibernateSession().clear();
        this.myDocket = docket;
    }
}

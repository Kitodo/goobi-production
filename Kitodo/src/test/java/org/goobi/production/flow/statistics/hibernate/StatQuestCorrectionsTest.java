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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.intranda.commons.chart.renderer.ChartRenderer;
import de.intranda.commons.chart.renderer.IRenderer;
import de.intranda.commons.chart.results.DataRow;
import de.intranda.commons.chart.results.DataTable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.goobi.production.flow.statistics.enums.CalculationUnit;
import org.goobi.production.flow.statistics.enums.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class StatQuestCorrectionsTest {
    static StatQuestCorrections test;

    /**
     * Performs computationally expensive setup shared several tests. This
     * compromises the independence of the tests, bit is a necessary
     * optimization here.
     */
    @BeforeClass
    public static void setUp() {
        // TODO: HIBERNATE fix
        // Configuration cfg = HibernateUtil.getConfiguration();
        // cfg.setProperty("hibernate.connection.url",
        // "jdbc:mysql://localhost/testgoobi");
        // HibernateUtil.rebuildSessionFactory();
        test = new StatQuestCorrections();
    }

    @Test
    public final void testSetTimeUnit() {
        test.setTimeUnit(TimeUnit.days);
    }

    @Ignore("Crashing")
    @Test
    public final void testGetDataTables() {
        List testFilter = new ArrayList();
        test.setTimeUnit(TimeUnit.days);
        List<DataTable> tables = test.getDataTables(testFilter);
        java.util.Iterator<DataTable> tablesIterator = tables.iterator();
        int counter = 0;
        DataTable table = null;
        DataRow row = null;
        while (tablesIterator.hasNext()) {
            table = tablesIterator.next();
            List<DataRow> rows = table.getDataRows();
            java.util.Iterator<DataRow> rowsIterator = rows.iterator();
            while (rowsIterator.hasNext()) {
                row = rowsIterator.next();
                counter += row.getMaxValue();
            }
        }
        // count on max value of each row on test database should be 13
        assertEquals(counter, 13);
    }

    @Test
    public final void testSetCalculationUnit() {
        test.setCalculationUnit(CalculationUnit.pages);
    }

    @Test
    public final void testSetTimeFrame() {
        Calendar one = Calendar.getInstance();
        Calendar other = Calendar.getInstance();
        one.set(2009, 01, 01);
        other.set(2009, 03, 31);
        test.setTimeFrame(one.getTime(), other.getTime());
    }

    @Test
    public final void testIsRendererInverted() {
        IRenderer inRenderer = new ChartRenderer();
        assertTrue(test.isRendererInverted(inRenderer));
    }

    @Test
    public final void testGetNumberFormatPattern() {
        String answer = null;
        answer = test.getNumberFormatPattern();
        assertNotNull(answer);
    }

}

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

package org.kitodo.production.services.data;

import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.elasticsearch.index.query.Operator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kitodo.data.database.beans.Batch;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.production.MockDatabase;
import org.kitodo.production.services.ServiceManager;

/**
 * Tests for BatchService class.
 */
public class BatchServiceIT {

    private static final BatchService batchService = ServiceManager.getBatchService();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertProcessesFull();
        MockDatabase.setUpAwaitility();
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCountAllBatches() {
        await().untilAsserted(
            () -> assertEquals("Batches were not counted correctly!", Long.valueOf(4), batchService.count()));
    }

    @Test
    public void shouldCountAllBatchesAccordingToQuery() {
        String query = matchQuery("title", "First batch").operator(Operator.AND).toString();
        await().untilAsserted(
            () -> assertEquals("Batches were not counted correctly!", Long.valueOf(1), batchService.count(query)));
    }

    @Test
    public void shouldCountAllDatabaseRowsForBatches() throws Exception {
        Long amount = batchService.countDatabaseRows();
        assertEquals("Batches were not counted correctly!", Long.valueOf(4), amount);
    }

    @Test
    public void shouldGetBatch() throws Exception {
        Batch batch = batchService.getById(1);
        boolean condition = batch.getTitle().equals("First batch") && batch.getType().equals(Batch.Type.LOGISTIC);
        assertTrue("Batch was not found in database!", condition);

        assertEquals("Batch was found but processes were not inserted!", 1, batch.getProcesses().size());
    }

    @Test
    public void shouldFindAllBatches() throws Exception {
        List<Batch> batches = batchService.getAll();
        assertEquals("Not all batches were found in database!", 4, batches.size());
    }

    @Test
    public void shouldGetAllBatchesInGivenRange() throws Exception {
        List<Batch> batches = batchService.getAll(2, 10);
        assertEquals("Not all batches were found in database!", 2, batches.size());
    }

    @Test
    public void shouldRemoveBatch() throws Exception {
        Batch batch = new Batch();
        batch.setTitle("To Remove");
        batch.setType(Batch.Type.SERIAL);
        batchService.save(batch);
        Batch foundBatch = batchService.getById(5);
        assertEquals("Additional batch was not inserted in database!", "To Remove", foundBatch.getTitle());

        batchService.remove(foundBatch);
        exception.expect(DAOException.class);
        batchService.getById(5);

        batch = new Batch();
        batch.setTitle("To remove");
        batch.setType(Batch.Type.SERIAL);
        batchService.save(batch);
        foundBatch = batchService.getById(6);
        assertEquals("Additional batch was not inserted in database!", "To remove", foundBatch.getTitle());

        batchService.remove(6);
        exception.expect(DAOException.class);
        batchService.getById(6);
    }

    @Test
    public void shouldFindById() {
        String expected = "First batch";
        await().untilAsserted(
            () -> assertEquals("Batch was not found in index!", expected, batchService.findById(1).getTitle()));
    }

    @Test
    public void shouldFindManyByTitle() {
        await().untilAsserted(
            () -> assertEquals("Batches were not found in index!", 3, batchService.findByTitle("batch", true).size()));
    }

    @Test
    public void shouldFindOneByTitle() {
        await().untilAsserted(() -> assertEquals("Batch was not found in index!", 1,
            batchService.findByTitle("First batch", true).size()));
    }

    @Test
    public void shouldNotFindByType() {
        await().untilAsserted(
            () -> assertEquals("Batch was found in index!", 0, batchService.findByTitle("noBatch", true).size()));
    }

    @Test
    public void shouldFindByTitleAndType() {
        await().untilAsserted(() -> assertEquals("Batch was not found in index!", 1,
            batchService.findByTitleAndType("First batch", Batch.Type.LOGISTIC).size()));
    }

    @Test
    public void shouldNotFindByTitleAndType() {
        await().untilAsserted(() -> assertEquals("Batch was found in index!", 0,
            batchService.findByTitleAndType("Second batch", Batch.Type.SERIAL).size()));
    }

    @Test
    public void shouldFindManyByTitleOrType() {
        await().untilAsserted(() -> assertEquals("Batches were not found in index!", 2,
            batchService.findByTitleOrType("First batch", Batch.Type.SERIAL).size()));
    }

    @Test
    public void shouldFindByTitleOrType() {
        await().untilAsserted(() -> assertEquals("More batches were found in index!", 1,
            batchService.findByTitleOrType("None", Batch.Type.SERIAL).size()));
    }

    @Test
    public void shouldFindManyByProcessId() {
        await().untilAsserted(
            () -> assertEquals("Batches were not found in index!", 2, batchService.findByProcessId(1).size()));
    }

    @Test
    public void shouldFindOneByProcessId() {
        await().untilAsserted(
            () -> assertEquals("Batch was not found in index!", 1, batchService.findByProcessId(2).size()));
    }

    @Test
    public void shouldNotFindByProcessId() {
        await().untilAsserted(
            () -> assertEquals("Some batches were found in index!", 0, batchService.findByProcessId(3).size()));
    }

    @Test
    public void shouldFindManyByProcessTitle() {
        await().untilAsserted(() -> assertEquals("Batches were not found in index!", 2,
            batchService.findByProcessTitle("First process").size()));
    }

    @Test
    public void shouldFindOneByProcessTitle() {
        await().untilAsserted(() -> assertEquals("Batch was not found in index!", 1,
            batchService.findByProcessTitle("Second process").size()));
    }

    @Test
    public void shouldNotFindByProcessTitle() {
        await().untilAsserted(() -> assertEquals("Some batches were found in index!", 0,
            batchService.findByProcessTitle("DBConnectionTest").size()));
    }

    @Test
    public void shouldContainCharSequence() throws Exception {
        Batch batch = batchService.getById(1);
        boolean condition = batch.getTitle().contains("bat") == batchService.contains(batch, "bat");
        assertTrue("It doesn't contain given char sequence!", condition);
    }

    @Test
    public void shouldGetIdString() throws Exception {
        Batch batch = batchService.getById(1);
        boolean condition = batchService.getIdString(batch).equals("1");
        assertTrue("Id's String doesn't match the given plain text!", condition);
    }

    @Test
    public void shouldGetLabel() throws Exception {
        Batch firstBatch = batchService.getById(1);
        boolean firstCondition = batchService.getLabel(firstBatch).equals("First batch");
        assertTrue("It doesn't get given label!", firstCondition);

        Batch secondBatch = batchService.getById(4);
        boolean secondCondition = batchService.getLabel(secondBatch).equals("Batch 4");
        assertTrue("It doesn't get given label!", secondCondition);
    }

    @Test
    public void shouldGetSizeOfProcesses() throws Exception {
        Batch batch = batchService.getById(1);
        int size = batchService.size(batch);
        assertEquals("Size of processes is not equal 1!", 1, size);
    }

    @Test
    public void shouldOverrideToString() throws Exception {
        Batch batch = batchService.getById(1);
        String toString = batchService.toString(batch);
        assertEquals("Override toString method is incorrect!", "First batch (1 processes) [logistics]", toString);
    }
}

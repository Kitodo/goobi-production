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

package org.goobi.io;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kitodo.data.database.beans.Process;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.ProcessService;
import org.kitodo.services.file.FileService;

public class BackupFileRotationTest {

    private static final String BACKUP_FILE_NAME = "testMeta.xml";
    private static ProcessService processService = new ServiceManager().getProcessService();
    private static FileService fileService = new FileService();

    @Before
    public void setUp() throws Exception {
        URI directory = fileService.createDirectory(URI.create(""), "2");
        fileService.createResource(directory, BACKUP_FILE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        fileService.delete(URI.create("2"));
    }

    @Test
    public void shouldCreateSingleBackupFile() throws Exception {
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        int numberOfBackups = 1;
        runBackup(numberOfBackups, process);
        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".1");
    }

    @Test
    public void backupFileShouldContainSameContentAsOriginalFile() throws IOException {
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        int numberOfBackups = 1;
        String content = "Test One.";
        URI correctURI = URI
                .create(processService.getProcessDataDirectory(process).toString() + "/" + BACKUP_FILE_NAME);
        writeFile(correctURI, content);
        runBackup(numberOfBackups, process);
        assertFileHasContent(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".1", content);
    }

    @Test
    public void modifiedDateShouldNotChangedOnBackup() throws IOException {
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        int numberOfBackups = 1;
        long originalModifiedDate = getLastModifiedFileDate(
            processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);
        assertLastModifiedDate(processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME + ".1",
            originalModifiedDate);
    }

    @Test
    public void shouldWriteTwoBackupFiles() throws IOException {
        int numberOfBackups = 2;
        FileService fileService = new FileService();

        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));

        runBackup(numberOfBackups, process);

        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);
        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".1");
        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".2");
    }

    @Test
    public void initialContentShouldEndUpInSecondBackupFileAfterTwoBackupRuns() throws IOException {
        String contentOne = "Test One.";
        int numberOfBackups = 2;
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        URI resolve = fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        writeFile(resolve, contentOne);
        runBackup(numberOfBackups, process);

        assertFileHasContent(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".1",
            contentOne);

        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);

        assertFileHasContent(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".2",
            contentOne);
    }

    @Test
    public void secondBackupFileCorrectModifiedDate() throws IOException {
        long expectedLastModifiedDate;
        int numberOfBackups = 2;
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        expectedLastModifiedDate = getLastModifiedFileDate(
            processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME);

        runBackup(numberOfBackups, process);
        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);

        assertLastModifiedDate(processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME + ".2",
            expectedLastModifiedDate);
    }

    @Test
    public void threeBackupRunsCreateThreeBackupFiles() throws IOException {
        int numberOfBackups = 3;

        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        runBackup(numberOfBackups, process);
        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);
        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);

        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".1");
        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".2");
        assertFileExists(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".3");
    }

    @Test
    public void initialContentShouldEndUpInThirdBackupFileAfterThreeBackupRuns() throws IOException {
        int numberOfBackups = 3;
        String contentOne = "Test One.";

        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        URI correctURI = URI
                .create(processService.getProcessDataDirectory(process).toString() + "/" + BACKUP_FILE_NAME);
        writeFile(correctURI, contentOne);
        runBackup(numberOfBackups, process);
        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);
        fileService.createResource(processService.getProcessDataDirectory(process), BACKUP_FILE_NAME);
        runBackup(numberOfBackups, process);

        assertFileHasContent(processService.getProcessDataDirectory(process) + "/" + BACKUP_FILE_NAME + ".3",
            contentOne);
    }

    @Test
    public void noBackupIsPerformedWithNumberOfBackupsSetToZero() throws Exception {
        int numberOfBackups = 0;
        Process process = new Process();
        process.setId(3);
        process.setProcessBaseUri(URI.create("3"));
        runBackup(numberOfBackups, process);
        assertFileNotExists(processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME + ".1");
    }

    @Test
    public void nothingHappensIfFilePatternDontMatch() throws Exception {
        int numberOfBackups = 1;
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));
        runBackup(numberOfBackups, "veryLongMatchingToNothingName", process);

        assertFileNotExists(processService.getProcessDataDirectory(process) + BACKUP_FILE_NAME + ".1");
    }

    private void assertLastModifiedDate(String fileName, long expectedLastModifiedDate) {
        long currentLastModifiedDate = getLastModifiedFileDate(fileName);
        assertEquals("Last modified date of file " + fileName + " differ:", expectedLastModifiedDate,
            currentLastModifiedDate);
    }

    private long getLastModifiedFileDate(String fileName) {
        File testFile = new File(fileName);
        return testFile.lastModified();
    }

    private void runBackup(int numberOfBackups, Process process) throws IOException {
        runBackup(numberOfBackups, BACKUP_FILE_NAME, process);
    }

    private void runBackup(int numberOfBackups, String format, Process process) throws IOException {

        BackupFileRotation bfr = new BackupFileRotation();
        bfr.setNumberOfBackups(numberOfBackups);
        bfr.setProcess(process);
        bfr.setFormat(format);
        bfr.performBackup();
    }

    private void assertFileHasContent(String fileName, String expectedContent) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(fileService.read(URI.create(fileName)));
        BufferedReader br = new BufferedReader(inputStreamReader);
        String line;
        String content = "";
        while ((line = br.readLine()) != null) {
            content += line;
        }
        br.close();
        assertEquals("File " + fileName + " does not contain expected content:", expectedContent, content);
    }

    private void assertFileExists(String fileName) {
        boolean fileExist = fileService.fileExist(URI.create(fileName));
        if (!fileExist) {
            fail("File " + fileName + " does not exist.");
        }
    }

    private void assertFileNotExists(String fileName) {
        File newFile = new File(fileName);
        if (newFile.exists()) {
            fail("File " + fileName + " should not exist.");
        }
    }

    private void writeFile(URI uri, String content) throws IOException {
        OutputStream outputStream = fileService.write(uri);
        final PrintStream printStream = new PrintStream(outputStream);
        printStream.print(content);
        printStream.flush();
        printStream.close();
    }

}

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

package org.kitodo.services.file;

import de.sub.goobi.config.ConfigCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kitodo.ExecutionPermission;
import org.kitodo.data.database.beans.Process;

public class FileServiceTest {

    private static FileService fileService = new FileService();
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(FileServiceTest.class);

    @BeforeClass
    public static void setUp() throws IOException {
        fileService.createDirectory(URI.create(""), "fileServiceTest");
        URI directory = fileService.createDirectory(URI.create(""), "2");
        fileService.createResource(directory, "meta.xml");
    }

    @AfterClass
    public static void tearDown() throws IOException {
        fileService.delete(URI.create("fileServiceTest"));
        fileService.delete(URI.create("2"));
    }

    @Test
    public void testCreateMetaDirectory() {
        try {
            File script = new File(ConfigCore.getParameter("script_createDirMeta"));
            if (!SystemUtils.IS_OS_WINDOWS) {
                ExecutionPermission.setExecutePermission(script);
            }

            boolean result = fileService.createMetaDirectory(URI.create("fileServiceTest"), "testMetaScript");
            File file = fileService.getFile((URI.create("fileServiceTest/testMetaScript")));

            if (!SystemUtils.IS_OS_WINDOWS) {
                ExecutionPermission.setNoExecutePermission(script);
            }

            Assert.assertTrue(result);
            Assert.assertTrue(file.isDirectory());
            Assert.assertFalse(file.isFile());
            Assert.assertTrue(file.exists());
        } catch (IOException e) {
            logger.error("Probably you run this test on Windows: " + e.getMessage());
        }
    }

    @Test
    public void testCreateDirectory() throws IOException {
        URI testMetaUri = fileService.createDirectory(URI.create("fileServiceTest"), "testMeta");
        File file = fileService.getFile(URI.create("fileServiceTest/testMeta"));

        Assert.assertTrue(file.isDirectory());
        Assert.assertFalse(file.isFile());
        Assert.assertTrue(file.exists());
        Assert.assertTrue("Incorrect path!", Paths.get(file.getPath()).toUri().getPath().contains(testMetaUri.getPath()));
    }

    @Test
    public void testCreateDirectoryWithMissingRoot() {
        try {
            fileService.createDirectory(URI.create("fileServiceTestMissing"), "testMeta");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        File file = fileService.getFile(URI.create("fileServiceTestMissing/testMeta"));

        Assert.assertFalse(file.exists());
    }

    @Test
    public void testCreateDirectoryWithAlreadyExistingDirectory() throws IOException {
        fileService.createDirectory(URI.create("fileServiceTest"), "testMetaExisting");

        File file = fileService.getFile(URI.create("fileServiceTest/testMetaExisting"));
        Assert.assertTrue(file.exists());

        URI testMetaUri = fileService.createDirectory(URI.create("fileServiceTest"), "testMetaExisting");
        file = fileService.getFile(URI.create("fileServiceTest/testMetaExisting"));

        Assert.assertTrue(file.exists());
        Assert.assertTrue("Incorrect path!", Paths.get(file.getPath()).toUri().getPath().contains(testMetaUri.getPath()));
    }

    @Test
    public void testCreateDirectoryWithNameOnly() throws IOException {
        URI testMetaNameOnly = fileService.createDirectory(URI.create("fileServiceTest"), "testMetaNameOnly");
        Assert.assertTrue(fileService.fileExist(testMetaNameOnly));

        URI uri = URI.create("fileServiceTest/testMetaNameOnly/");
        Assert.assertEquals(testMetaNameOnly, uri);

    }

    @Test
    public void testRenameFile() throws IOException {
        URI resource = fileService.createResource(URI.create("fileServiceTest"), "oldName.xml");
        URI oldUri = URI.create("fileServiceTest/oldName.xml");
        Assert.assertTrue(fileService.fileExist(oldUri));
        Assert.assertEquals(resource, oldUri);

        fileService.renameFile(resource, "newName.xml");
        URI newUri = URI.create("fileServiceTest/newName.xml");
        Assert.assertFalse(fileService.fileExist(oldUri));
        Assert.assertTrue(fileService.fileExist(newUri));
    }

    @Test(expected = IOException.class)
    public void testRenameFileWithExistingTarget() throws IOException {
        FileService fileService = new FileService();

        URI oldUri = fileService.createResource(URI.create("fileServiceTest"), "oldName.xml");
        URI newUri = fileService.createResource(URI.create("fileServiceTest"), "newName.xml");
        Assert.assertTrue(fileService.fileExist(oldUri));
        Assert.assertTrue(fileService.fileExist(newUri));

        fileService.renameFile(oldUri, "newName.xml");
    }

    @Test(expected = FileNotFoundException.class)
    public void testRenameFileWithMissingSource() throws IOException {
        URI oldUri = URI.create("fileServiceTest/oldNameMissing.xml");
        Assert.assertFalse(fileService.fileExist(oldUri));

        fileService.renameFile(oldUri, "newName.xml");
    }

    @Test
    public void testGetNumberOfFiles() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "countFiles0");
        fileService.createResource(directory, "test.xml");
        fileService.createResource(directory, "test2.xml");

        int numberOfFiles = fileService.getNumberOfFiles(directory);

        Assert.assertEquals(2, numberOfFiles);

    }

    @Test
    public void testGetNumberOfFilesWithSubDirectory() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "countFiles1");
        fileService.createResource(directory, "test.pdf");
        URI subDirectory = fileService.createDirectory(directory, "subdirectory");
        fileService.createResource(subDirectory, "subTest.xml");
        fileService.createResource(subDirectory, "subTest2.jpg");

        int numberOfFiles = fileService.getNumberOfFiles(directory);

        Assert.assertEquals(3, numberOfFiles);

    }

    @Test
    public void testGetNumberOfImageFiles() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "countFiles2");
        fileService.createResource(directory, "test.pdf");
        fileService.createResource(directory, "subTest.xml");
        fileService.createResource(directory, "subTest2.jpg");

        int numberOfFiles = fileService.getNumberOfImageFiles(directory);

        Assert.assertEquals(1, numberOfFiles);

    }

    @Test
    public void testGetNumberOfImageFilesWithSubDirectory() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "countFiles3");
        fileService.createResource(directory, "test.pdf");
        URI subDirectory = fileService.createDirectory(directory, "subdirectory");
        fileService.createResource(subDirectory, "subTest.xml");
        fileService.createResource(subDirectory, "subTest2.jpg");

        int numberOfFiles = fileService.getNumberOfImageFiles(directory);

        Assert.assertEquals(1, numberOfFiles);

    }

    @Test
    public void testCopyDirectory() throws IOException {
        URI fromDirectory = fileService.createDirectory(URI.create("fileServiceTest"), "copyDirectory");
        fileService.createResource(fromDirectory, "test.pdf");
        URI toDirectory = URI.create("fileServiceTest/copyDirectoryTo/");

        Assert.assertFalse(fileService.fileExist(toDirectory));

        fileService.copyDirectory(fromDirectory, toDirectory);

        Assert.assertTrue(fileService.fileExist(toDirectory));
        Assert.assertTrue(fileService.fileExist(toDirectory.resolve("test.pdf")));
        Assert.assertTrue(fileService.fileExist(fromDirectory));

    }

    @Test(expected = FileNotFoundException.class)
    public void testCopyDirectoryWithMissingSource() throws IOException {
        URI fromDirectory = URI.create("fileServiceTest/copyDirectoryNotExisting/");
        URI toDirectory = URI.create("fileServiceTest/copyDirectoryNotExistingTo/");

        Assert.assertFalse(fileService.fileExist(fromDirectory));
        Assert.assertFalse(fileService.fileExist(toDirectory));

        fileService.copyDirectory(fromDirectory, toDirectory);

    }

    @Test
    public void testCopyDirectoryWithExistingTarget() throws IOException {
        URI fromDirectory = fileService.createDirectory(URI.create("fileServiceTest"), "copyDirectoryExistingTarget");
        fileService.createResource(fromDirectory, "testToCopy.pdf");

        URI toDirectory = fileService.createDirectory(URI.create("fileServiceTest"), "copyDirectoryNotExistingTarget");
        fileService.createResource(toDirectory, "testExisting.pdf");

        Assert.assertTrue(fileService.fileExist(fromDirectory));
        Assert.assertTrue(fileService.fileExist(toDirectory));

        fileService.copyDirectory(fromDirectory, toDirectory);

        Assert.assertTrue(fileService.fileExist(toDirectory));
        Assert.assertTrue(fileService.fileExist(toDirectory.resolve("testToCopy.pdf")));
        Assert.assertTrue(fileService.fileExist(toDirectory.resolve("testExisting.pdf")));
        Assert.assertTrue(fileService.fileExist(fromDirectory));
        Assert.assertTrue(fileService.fileExist(fromDirectory.resolve("testToCopy.pdf")));
        Assert.assertFalse(fileService.fileExist(fromDirectory.resolve("testExisting.pdf")));

    }

    @Test
    public void testCopyFile() throws IOException {
        URI originFile = fileService.createResource(URI.create("fileServiceTest"), "copyFile");
        URI targetFile = URI.create("fileServiceTest/copyFileTarget");

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertFalse(fileService.fileExist(targetFile));

        fileService.copyFile(originFile, targetFile);

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetFile));

    }

    @Test(expected = FileNotFoundException.class)
    public void testCopyFileWithMissingSource() throws IOException {
        URI originFile = URI.create("fileServiceTest/copyFileMissing");
        URI targetFile = URI.create("fileServiceTest/copyFileTargetMissing");

        Assert.assertFalse(fileService.fileExist(originFile));
        Assert.assertFalse(fileService.fileExist(targetFile));

        fileService.copyFile(originFile, targetFile);

    }

    @Test
    public void testCopyFileWithExistingTarget() throws IOException {
        URI originFile = fileService.createResource(URI.create("fileServiceTest"), "copyFileExisting.txt");
        URI targetFile = fileService.createResource(URI.create("fileServiceTest"), "copyFileExistingTarget.txt");

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetFile));

        fileService.copyFile(originFile, targetFile);

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetFile));

    }

    @Test
    public void testCopyFileToDirectory() throws IOException {
        URI originFile = fileService.createResource(URI.create("fileServiceTest"), "copyFileToDirectory.txt");
        URI targetDirectory = fileService.createDirectory(URI.create("fileServiceTest"), "copyFileToDirectoryTarget");

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetDirectory));
        Assert.assertFalse(fileService.fileExist(targetDirectory.resolve("copyFileToDirectory.txt")));

        fileService.copyFileToDirectory(originFile, targetDirectory);

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetDirectory.resolve("copyFileToDirectory.txt")));

    }

    @Test
    public void testCopyFileToDirectoryWithMissingDirectory() throws IOException {
        URI originFile = fileService.createResource(URI.create("fileServiceTest"), "copyFileToDirectoryMissing");
        URI targetDirectory = URI.create("fileServiceTest/copyFileToDirectoryMissingTarget/");

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertFalse(fileService.fileExist(targetDirectory));
        Assert.assertFalse(fileService.fileExist(targetDirectory.resolve("copyFileToDirectoryMissing")));

        fileService.copyFileToDirectory(originFile, targetDirectory);

        Assert.assertTrue(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetDirectory.resolve("copyFileToDirectoryMissing")));

    }

    @Test(expected = FileNotFoundException.class)
    public void testCopyFileToDirectoryWithMissingSource() throws IOException {
        URI originFile = URI.create("fileServiceTest/copyFileToDirectoryMissingSource");
        URI targetDirectory = fileService.createDirectory(URI.create("fileServiceTest"),
                "copyFileToDirectoryMissingSourceTarget");

        Assert.assertFalse(fileService.fileExist(originFile));
        Assert.assertTrue(fileService.fileExist(targetDirectory));
        Assert.assertFalse(fileService.fileExist(targetDirectory.resolve("copyFileToDirectoryMissingSource")));

        fileService.copyFileToDirectory(originFile, targetDirectory);

    }

    @Test
    public void testDeleteFile() throws IOException {
        URI originFile = fileService.createResource(URI.create("fileServiceTest"), "deleteFile");
        Assert.assertTrue(fileService.fileExist(originFile));

        fileService.delete(originFile);
        Assert.assertFalse(fileService.fileExist(originFile));
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        URI originFile = fileService.createDirectory(URI.create("fileServiceTest"), "deleteDirectory");
        Assert.assertTrue(fileService.fileExist(originFile));

        fileService.delete(originFile);
        Assert.assertFalse(fileService.fileExist(originFile));
    }

    @Test
    public void testDeleteWithNotExisting() throws IOException {
        URI originFile = URI.create("fileServiceTest/deleteNotExisting");
        Assert.assertFalse(fileService.fileExist(originFile));

        boolean delete = fileService.delete(originFile);
        Assert.assertFalse(fileService.fileExist(originFile));
        Assert.assertTrue(delete);
    }

    @Test
    public void testFileExist() throws IOException {
        URI notExisting = URI.create("fileServiceTest/fileExists");
        Assert.assertFalse(fileService.fileExist(notExisting));

        URI existing = fileService.createResource(URI.create("fileServiceTest"), "fileExists");
        Assert.assertTrue(fileService.fileExist(existing));

    }

    @Test
    public void testGetFileName() throws IOException {
        URI existing = fileService.createResource(URI.create("fileServiceTest"), "fileName.xml");

        String fileName = fileService.getFileName(existing);

        Assert.assertEquals("fileName", fileName);
    }

    @Test
    public void testGetFileNameFromDirectory() throws IOException {
        URI existing = fileService.createDirectory(URI.create("fileServiceTest"), "directoryName");

        String fileName = fileService.getFileName(existing);

        Assert.assertEquals("", fileName);
    }

    @Test
    public void testGetFileNameFromNotExisting() {
        URI notExisting = URI.create("fileServiceTest/fileName.xml");

        String fileName = fileService.getFileName(notExisting);

        Assert.assertEquals("fileName", fileName);
    }

    @Test
    public void testMoveDirectory() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "movingDirectory");
        fileService.createResource(directory, "test.xml");
        URI target = URI.create("fileServiceTest/movingDirectoryTarget/");

        Assert.assertTrue(fileService.fileExist(directory));
        Assert.assertTrue(fileService.fileExist(directory.resolve("test.xml")));
        Assert.assertFalse(fileService.fileExist(target));

        fileService.moveDirectory(directory, target);

        Assert.assertFalse(fileService.fileExist(directory));
        Assert.assertFalse(fileService.fileExist(directory.resolve("test.xml")));
        Assert.assertTrue(fileService.fileExist(target));
        Assert.assertTrue(fileService.fileExist(target.resolve("test.xml")));

    }

    @Test(expected = FileNotFoundException.class)
    public void testMoveDirectoryWithMissingSource() throws IOException {
        URI directory = URI.create("fileServiceTest/movingDirectoryMissing/");
        URI target = URI.create("fileServiceTest/movingDirectoryMissingTarget/");

        Assert.assertFalse(fileService.fileExist(directory));
        Assert.assertFalse(fileService.fileExist(target));

        fileService.moveDirectory(directory, target);

    }

    @Test
    public void testMoveDirectoryWithExistingTarget() throws IOException {
        URI directory = fileService.createDirectory(URI.create("fileServiceTest"), "movingDirectoryTargetMissing");
        fileService.createResource(directory, "test.xml");
        URI target = fileService.createDirectory(URI.create("fileServiceTest"), "movingTargetMissing");
        fileService.createResource(target, "testTarget.xml");

        Assert.assertTrue(fileService.fileExist(directory));
        Assert.assertTrue(fileService.fileExist(directory.resolve("test.xml")));
        Assert.assertTrue(fileService.fileExist(target));
        Assert.assertTrue(fileService.fileExist(target.resolve("testTarget.xml")));

        fileService.moveDirectory(directory, target);

        Assert.assertFalse(fileService.fileExist(directory));
        Assert.assertFalse(fileService.fileExist(directory.resolve("test.xml")));
        Assert.assertTrue(fileService.fileExist(target));
        Assert.assertTrue(fileService.fileExist(target.resolve("test.xml")));
        Assert.assertTrue(fileService.fileExist(target.resolve("testTarget.xml")));

    }

    @Test
    public void testMoveFile() throws IOException {
        URI file = fileService.createResource(URI.create("fileServiceTest"), "movingFile");
        URI target = URI.create("fileServiceTest/movingFileTarget");

        Assert.assertTrue(fileService.fileExist(file));
        Assert.assertFalse(fileService.fileExist(target));

        fileService.moveFile(file, target);

        Assert.assertFalse(fileService.fileExist(file));
        Assert.assertTrue(fileService.fileExist(target));

    }

    @Test(expected = FileNotFoundException.class)
    public void testMoveFileWithMissingSource() throws IOException {
        URI file = URI.create("fileServiceTest/movingFileMissing");
        URI target = URI.create("fileServiceTest/movingFileMissingTarget");

        Assert.assertFalse(fileService.fileExist(file));
        Assert.assertFalse(fileService.fileExist(target));

        fileService.moveDirectory(file, target);

    }

    @Test
    public void testMoveFileWithExistingTarget() throws IOException {
        URI file = fileService.createDirectory(URI.create("fileServiceTest"), "movingFileTargetMissing");
        URI target = fileService.createDirectory(URI.create("fileServiceTest"), "movingFileTargetMissingTarget");

        Assert.assertTrue(fileService.fileExist(file));
        Assert.assertTrue(fileService.fileExist(target));

        fileService.moveDirectory(file, target);

        Assert.assertFalse(fileService.fileExist(file));
        Assert.assertTrue(fileService.fileExist(target));

    }

    @Test
    public void testCreateBackupFile() throws IOException {
        Process process = new Process();
        process.setId(2);
        process.setProcessBaseUri(URI.create("2"));

        Assert.assertFalse(fileService.fileExist(URI.create("2/meta.xml.1")));
        Assert.assertFalse(fileService.fileExist(URI.create("2/meta.xml.2")));

        fileService.createBackupFile(process);

        Assert.assertTrue(fileService.fileExist(URI.create("2/meta.xml.1")));
        Assert.assertFalse(fileService.fileExist(URI.create("2/meta.xml.2")));

        fileService.createResource(URI.create("2"), "meta.xml");
        fileService.createBackupFile(process);

        Assert.assertTrue(fileService.fileExist(URI.create("2/meta.xml.1")));
        Assert.assertTrue(fileService.fileExist(URI.create("2/meta.xml.2")));

        // No third backup file is created, when numberOfBackups is set to two
        fileService.createResource(URI.create("2"), "meta.xml");
        fileService.createBackupFile(process);

        Assert.assertTrue(fileService.fileExist(URI.create("2/meta.xml.1")));
        Assert.assertTrue(fileService.fileExist(URI.create("2/meta.xml.2")));
        Assert.assertFalse(fileService.fileExist(URI.create("2/meta.xml.3")));
    }

    @Test
    public void testDeleteFirstSlashFromPath() {
        URI uri = URI.create("/test/test");
        URI actualUri = fileService.deleteFirstSlashFromPath(uri);
        Assert.assertEquals("Paths of Uri did not match", "test/test", actualUri.getPath());
    }

}

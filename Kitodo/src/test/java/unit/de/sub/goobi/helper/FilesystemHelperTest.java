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

package de.sub.goobi.helper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Test;

import static junit.framework.Assert.fail;

import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FilesystemHelperTest {

	@BeforeClass
	public static void oneTimeSetUp() {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		deleteFile("old.xml");
		deleteFile("new.xml");
	}

	@Test(expected = java.io.FileNotFoundException.class)
	public void RenamingOfNonExistingFileShouldThrowFileNotFoundException () throws IOException {
		String oldFileName = "old.xml";
		String newFileName = "new.xml";

		FilesystemHelper.renameFile(oldFileName, newFileName);
	}

	@Test
	public void shouldRenameAFile()
		throws IOException {
		createFile("old.xml");
		FilesystemHelper.renameFile("old.xml", "new.xml");
		assertFileExists("new.xml");
		assertFileNotExists("old.xml");
	}

	@Test
	public void nothingHappensIfSourceFilenameIsNotSet()
		throws IOException {
		FilesystemHelper.renameFile(null, "new.xml");
		assertFileNotExists("new.xml");
	}
	
	@Test
	public void nothingHappensIfTargetFilenameIsNotSet()
		throws IOException {
		createFile("old.xml");
		FilesystemHelper.renameFile("old.xml", null);
		assertFileNotExists("new.xml");
	}

	private void assertFileExists(String fileName) {
		File newFile = new File(fileName);
		if (!newFile.exists()) {
			fail("File " + fileName + " does not exist.");
		}
	}

	private void assertFileNotExists(String fileName) {
		File newFile = new File(fileName);
		if (newFile.exists()) {
			fail("File " + fileName + " should not exist.");
		}
	}

	private void createFile(String fileName) throws IOException {
		File testFile = new File(fileName);
		FileWriter writer = new FileWriter(testFile);
		writer.close();
	}

	private void deleteFile(String fileName) {
		File testFile = new File(fileName);
		testFile.delete();
	}
}

/*
 * This file is part of the Goobi Application - a Workflow tool for the support of
 * mass digitization.
 *
 * Visit the websites for more information.
 *     - http://gdz.sub.uni-goettingen.de
 *     - http://www.goobi.org
 *     - http://launchpad.net/goobi-production
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */

package de.sub.goobi.export.dms;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.goobi.io.FileListFilter;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import ugh.fileformats.excel.RDFFile;
import ugh.fileformats.mets.MetsModsImportExport;
import de.sub.goobi.beans.Benutzer;
import de.sub.goobi.beans.Prozess;
import de.sub.goobi.export.download.ExportMets;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.metadaten.MetadatenVerifizierung;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.Messages;
import de.sub.goobi.helper.enums.MetadataFormat;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;

public class ExportDms extends ExportMets {
	private static final Logger myLogger = Logger.getLogger(ExportDms.class);
	ConfigProjects cp;
	private boolean exportWithImages = true;

	public final static String DIRECTORY_SUFFIX = "_tif";

	public ExportDms() {
	}

	public ExportDms(boolean exportImages) {
		exportWithImages = exportImages;
	}

	/**
	 * DMS-Export an eine gewünschte Stelle
	 * 
	 * @param myProzess
	 * @param zielVerzeichnis
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws WriteException
	 * @throws PreferencesException
	 * @throws UghHelperException
	 * @throws ExportFileException
	 * @throws MetadataTypeNotAllowedException
	 * @throws DocStructHasNoTypeException
	 * @throws DAOException
	 * @throws SwapException
	 * @throws TypeNotAllowedForParentException
	 */
	public void startExport(Prozess myProzess, String inZielVerzeichnis) throws IOException, InterruptedException, WriteException,
			PreferencesException, DocStructHasNoTypeException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException,
			SwapException, DAOException, TypeNotAllowedForParentException {
		myPrefs = myProzess.getRegelsatz().getPreferences();
		cp = new ConfigProjects(myProzess.getProjekt());
		String atsPpnBand = myProzess.getTitel();

		/*
		 * -------------------------------- Dokument einlesen --------------------------------
		 */
		Fileformat gdzfile;
		Fileformat newfile;
		try {
			gdzfile = myProzess.readMetadataFile();
			switch (MetadataFormat.findFileFormatsHelperByName(myProzess.getProjekt().getFileFormatDmsExport())) {
			case METS:
				newfile = new MetsModsImportExport(myPrefs);
				break;

			case METS_AND_RDF:
				newfile = new RDFFile(myPrefs);
				break;

			default:
				newfile = new RDFFile(myPrefs);
				break;
			}

				
			
			
			newfile.setDigitalDocument(gdzfile.getDigitalDocument());
			gdzfile = newfile;
		
				
		} catch (Exception e) {
			Helper.setFehlerMeldung(Messages.getString("exportError") + myProzess.getTitel(), e);
			myLogger.error("Export abgebrochen, xml-LeseFehler", e);
			return;
		}

		/* nur beim Rusdml-Projekt die Metadaten aufbereiten */
		ConfigProjects cp = new ConfigProjects(myProzess.getProjekt());
		// TODO: Remove this
		if (cp.getParamList("dmsImport.check").contains("rusdml")) {
			ExportDms_CorrectRusdml expcorr = new ExportDms_CorrectRusdml(myProzess, myPrefs, gdzfile);
			atsPpnBand = expcorr.correctionStart();
		}

		trimAllMetadata(gdzfile.getDigitalDocument().getLogicalDocStruct());

		/*
		 * -------------------------------- Metadaten validieren --------------------------------
		 */

		if (ConfigMain.getBooleanParameter("useMetadatenvalidierung")) {
			MetadatenVerifizierung mv = new MetadatenVerifizierung();
			if (!mv.validate(gdzfile, myPrefs, myProzess))
				return;
		}

		/*
		 * -------------------------------- Speicherort vorbereiten und downloaden --------------------------------
		 */
		String zielVerzeichnis;
		File benutzerHome;
		if (myProzess.getProjekt().isUseDmsImport()) {
			zielVerzeichnis = myProzess.getProjekt().getDmsImportImagesPath();
			benutzerHome = new File(zielVerzeichnis);

			/* ggf. noch einen Vorgangsordner anlegen */
			if (myProzess.getProjekt().isDmsImportCreateProcessFolder()) {
				benutzerHome = new File(benutzerHome + File.separator + myProzess.getTitel());
				zielVerzeichnis = benutzerHome.getAbsolutePath();
				/* alte Import-Ordner löschen */
				if (!Helper.deleteDir(benutzerHome)) {
					Helper.setFehlerMeldung("Export canceled, Process: " + myProzess.getTitel(), "Import folder could not be cleared");
					return;
				}
				/* alte Success-Ordner löschen */
				File successFile = new File(myProzess.getProjekt().getDmsImportSuccessPath() + File.separator + myProzess.getTitel());
				if (!Helper.deleteDir(successFile)) {
					Helper.setFehlerMeldung("Export canceled, Process: " + myProzess.getTitel(), "Success folder could not be cleared");
					return;
				}
				/* alte Error-Ordner löschen */
				File errorfile = new File(myProzess.getProjekt().getDmsImportErrorPath() + File.separator + myProzess.getTitel());
				if (!Helper.deleteDir(errorfile)) {
					Helper.setFehlerMeldung("Export canceled, Process: " + myProzess.getTitel(), "Error folder could not be cleared");
					return;
				}

				if (!benutzerHome.exists())
					benutzerHome.mkdir();
			}

		} else {
			zielVerzeichnis = inZielVerzeichnis + atsPpnBand + File.separator;
			// wenn das Home existiert, erst löschen und dann neu anlegen
			benutzerHome = new File(zielVerzeichnis);
			if (!Helper.deleteDir(benutzerHome)) {
				Helper.setFehlerMeldung("Export canceled: " + myProzess.getTitel(), "could not delete home directory");
				return;
			}
			prepareUserDirectory(zielVerzeichnis);
		}

		/*
		 * -------------------------------- der eigentliche Download der Images --------------------------------
		 */
		try {
			if (exportWithImages) {
				imageDownload(myProzess, benutzerHome, atsPpnBand, DIRECTORY_SUFFIX);
				File ocrDirectory = new File(myProzess.getOcrDirectory());
				exportContentOfOcrDirectory(ocrDirectory, benutzerHome, atsPpnBand);
			}
		} catch (Exception e) {
			Helper.setFehlerMeldung("Export canceled, Process: " + myProzess.getTitel(), e);
			return;
		}

		/*
		 * -------------------------------- zum Schluss Datei an gewünschten Ort exportieren entweder direkt in den Import-Ordner oder ins
		 * Benutzerhome anschliessend den Import-Thread starten --------------------------------
		 */
		if (myProzess.getProjekt().isUseDmsImport()) {
			if (MetadataFormat.findFileFormatsHelperByName(myProzess.getProjekt().getFileFormatDmsExport()) == MetadataFormat.METS) {
				/* Wenn METS, dann per writeMetsFile schreiben... */
				writeMetsFile(myProzess, benutzerHome + File.separator + atsPpnBand + ".xml", gdzfile);
			} else {
				/* ...wenn nicht, nur ein Fileformat schreiben. */
				gdzfile.write(benutzerHome + File.separator + atsPpnBand + ".xml");
			}

			// TODO generischer lösen
			/* ggf. sollen im Export mets und rdf geschrieben werden */
			if (MetadataFormat.findFileFormatsHelperByName(myProzess.getProjekt().getFileFormatDmsExport()) == MetadataFormat.METS_AND_RDF) {
				writeMetsFile(myProzess, benutzerHome + File.separator + atsPpnBand + ".mets.xml", gdzfile);
			}

			Helper.setMeldung(null, myProzess.getTitel() + ": ", "DMS-Export started");
			DmsImportThread agoraThread = new DmsImportThread(myProzess, atsPpnBand);
			agoraThread.start();
			if (!ConfigMain.getBooleanParameter("exportWithoutTimeLimit")) {
				try {
					/* 30 Sekunden auf den Thread warten, evtl. killen */
					agoraThread.join(myProzess.getProjekt().getDmsImportTimeOut().longValue());
					if (agoraThread.isAlive()) {
						agoraThread.stopThread();
					}
				} catch (InterruptedException e) {
					Helper.setFehlerMeldung(myProzess.getTitel() + ": error on export - ", e.getMessage());
					myLogger.error(myProzess.getTitel() + ": error on export", e);
				}
				if (agoraThread.rueckgabe.length() > 0)
					Helper.setFehlerMeldung(myProzess.getTitel() + ": ", agoraThread.rueckgabe);
				else {
					Helper.setMeldung(null, myProzess.getTitel() + ": ", "ExportFinished");
					/* Success-Ordner wieder löschen */
					if (myProzess.getProjekt().isDmsImportCreateProcessFolder()) {
						File successFile = new File(myProzess.getProjekt().getDmsImportSuccessPath() + File.separator + myProzess.getTitel());
						Helper.deleteDir(successFile);
					}
				}
			}
		} else {
			/* ohne Agora-Import die xml-Datei direkt ins Home schreiben */
			if (MetadataFormat.findFileFormatsHelperByName(myProzess.getProjekt().getFileFormatDmsExport()) == MetadataFormat.METS) {
				writeMetsFile(myProzess, zielVerzeichnis + atsPpnBand + ".xml", gdzfile);
			} else {
				gdzfile.write(zielVerzeichnis + atsPpnBand + ".xml");
			}

			Helper.setMeldung(null, myProzess.getTitel() + ": ", "ExportFinished");
		}

		copyAdditionalFilesOnExport(myProzess.getProcessDataDirectory(), zielVerzeichnis);
	}

	/**
	 * run through all metadata and children of given docstruct to trim the strings calls itself recursively
	 */
	private void trimAllMetadata(DocStruct inStruct) {
		/* trimm all metadata values */
		if (inStruct.getAllMetadata() != null) {
			for (Metadata md : inStruct.getAllMetadata()) {
				if (md.getValue() != null) {
					md.setValue(md.getValue().trim());
				}
			}
		}

		/* run through all children of docstruct */
		if (inStruct.getAllChildren() != null) {
			for (DocStruct child : inStruct.getAllChildren()) {
				trimAllMetadata(child);
			}
		}
	}

	public void imageDownload(Prozess myProzess, File benutzerHome, String atsPpnBand, final String ordnerEndung) throws IOException,
			InterruptedException, SwapException, DAOException {

		/*
		 * -------------------------------- dann den Ausgangspfad ermitteln --------------------------------
		 */
		File tifOrdner = new File(myProzess.getImagesTifDirectory());

		/*
		 * -------------------------------- jetzt die Ausgangsordner in die Zielordner kopieren --------------------------------
		 */
		if (tifOrdner.exists()) {
			File zielTif = new File(benutzerHome + File.separator + atsPpnBand + ordnerEndung);

			/* bei Agora-Import einfach den Ordner anlegen */
			if (myProzess.getProjekt().isUseDmsImport()) {
				if (!zielTif.exists())
					zielTif.mkdir();
			} else {
				/* wenn kein Agora-Import, dann den Ordner mit Benutzerberechtigung neu anlegen */
				Benutzer myBenutzer = (Benutzer) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
				try {
					FilesystemHelper.createDirectoryForUser(zielTif.getAbsolutePath(), myBenutzer.getLogin());
				} catch (Exception e) {
					Helper.setFehlerMeldung("Export canceled, error", "could not create destination directory");
					myLogger.error("could not create destination directory", e);
				}
			}

			/* jetzt den eigentlichen Kopiervorgang */

			File[] dateien = tifOrdner.listFiles(MetadatenImagesHelper.filter);
			for (int i = 0; i < dateien.length; i++) {
				File meinZiel = new File(zielTif + File.separator + dateien[i].getName());
				Helper.copyFile(dateien[i], meinZiel);
			}
		}
	}

	protected void exportContentOfOcrDirectory(File ocrDirectory, File userHome, String atsPpnBand)
			throws IOException, SwapException, DAOException, InterruptedException {

		if (ocrDirectory.exists()) {
			File[] folder = ocrDirectory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					int liof = name.lastIndexOf("_");
					int leng = name.length()-1;
					return (liof > -1) && (liof < leng);
				}
			});
			if (folder != null) {
				for (File ocrSubDirectory : folder) {
					if (ocrSubDirectory.isDirectory() && ocrSubDirectory.list().length > 0) {
						String suffix = ocrSubDirectory.getName().substring(ocrSubDirectory.getName().lastIndexOf("_"));
						File destination = new File(userHome + File.separator + atsPpnBand + suffix);
						copyDirectory(ocrSubDirectory, destination);
					}
				}
			}
		} else {
			myLogger.warn("OCR directory " + ocrDirectory.getAbsolutePath() + " does not exists.");
		}
	}

	private void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {

		copyFilesOfDirectories(sourceDirectory, destinationDirectory, null);
	}

	private void copyAdditionalFilesOnExport(String sourceDirectory, String destinationDirectory) throws IOException {
		File source = new File(sourceDirectory);
		File destination = new File(destinationDirectory);
		String fileMatcher = ConfigMain.getParameter("copyAdditionalFilesOnExport", null);

		if (fileMatcher == null || fileMatcher.isEmpty()) {
			myLogger.trace("No additional files to copy on export.");
			return;
		}

		FileListFilter filter = new FileListFilter(fileMatcher);
		copyFilesOfDirectories(source, destination, filter);
	}

	private void copyFilesOfDirectories(File source, File destination, FileListFilter filter) throws IOException {

		if (! source.isDirectory()) {
			myLogger.error("Given source " + source.getPath() + " is not a directory!");
			return;
		}

		if (! destination.exists()) {
			myLogger.trace("Destination directory " + destination.getPath() + " does not exists. Creating it!");
			boolean result;
			result = destination.mkdir();
			if (! result) {
				myLogger.error("Could not create directory " + destination.getPath() + "!");
				return;
			}
		}

		File[] sourceFiles = source.listFiles(filter);
		if (sourceFiles != null) {
			for (File sourceFile : sourceFiles) {
				File destinationFile = new File(destination + File.separator + sourceFile.getName());
				Helper.copyFile(sourceFile, destinationFile);
			}
		}

	}

}

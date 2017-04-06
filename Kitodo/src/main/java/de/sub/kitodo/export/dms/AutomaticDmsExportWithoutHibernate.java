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

package de.sub.kitodo.export.dms;

import de.sub.kitodo.config.ConfigCore;
import de.sub.kitodo.config.ConfigProjects;
import de.sub.kitodo.export.download.ExportMetsWithoutHibernate;
import de.sub.kitodo.helper.FilesystemHelper;
import de.sub.kitodo.helper.Helper;
import de.sub.kitodo.helper.tasks.EmptyTask;
import de.sub.kitodo.metadaten.MetadatenVerifizierungWithoutHibernate;
import de.sub.kitodo.metadaten.copier.CopierData;
import de.sub.kitodo.metadaten.copier.DataCopier;
import de.sub.kitodo.persistence.apache.FolderInformation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.exceptions.SwapException;
import org.kitodo.data.database.helper.enums.MetadataFormat;
import org.kitodo.data.database.persistence.apache.ProcessManager;
import org.kitodo.data.database.persistence.apache.ProcessObject;
import org.kitodo.data.database.persistence.apache.ProjectManager;
import org.kitodo.data.database.persistence.apache.ProjectObject;
import org.kitodo.io.SafeFile;
import org.kitodo.services.ServiceManager;

import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import ugh.fileformats.excel.RDFFile;
import ugh.fileformats.mets.MetsModsImportExport;

public class AutomaticDmsExportWithoutHibernate extends ExportMetsWithoutHibernate {
    private static final Logger myLogger = Logger.getLogger(AutomaticDmsExportWithoutHibernate.class);
    ConfigProjects cp;
    private boolean exportWithImages = true;
    private boolean exportFullText = true;
    private FolderInformation fi;
    private ProjectObject project;
    private final ServiceManager serviceManager = new ServiceManager();

    /**
     * The field task holds an optional task instance. Its progress and its
     * errors will be passed to the task manager screen (if available) for
     * visualisation.
     */
    private EmptyTask task;

    public static final String DIRECTORY_SUFFIX = "_tif";

    public AutomaticDmsExportWithoutHibernate() {
    }

    public AutomaticDmsExportWithoutHibernate(boolean exportImages) {
        this.exportWithImages = exportImages;
    }

    public void setExportFulltext(boolean exportFullText) {
        this.exportFullText = exportFullText;
    }

    /**
     * DMS-Export an eine gewünschte Stelle.
     *
     * @param process
     *            object
     */

    @Override
    public boolean startExport(ProcessObject process) throws DAOException, IOException, PreferencesException,
            WriteException, SwapException, TypeNotAllowedForParentException, InterruptedException {
        this.myPrefs = serviceManager.getRulesetService()
                .getPreferences(ProcessManager.getRuleset(process.getRulesetId()));
        ;

        this.project = ProjectManager.getProjectById(process.getProjectId());

        this.cp = new ConfigProjects(this.project.getTitle());
        String atsPpnBand = process.getTitle();

        /*
         * Dokument einlesen
         */
        Fileformat gdzfile;
        Fileformat newfile;
        try {
            this.fi = new FolderInformation(process.getId(), process.getTitle());
            String metadataPath = this.fi.getMetadataFilePath();
            gdzfile = process.readMetadataFile(metadataPath, this.myPrefs);
            switch (MetadataFormat.findFileFormatsHelperByName(this.project.getFileFormatDmsExport())) {
                case METS:
                    newfile = new MetsModsImportExport(this.myPrefs);
                    break;
                case METS_AND_RDF:
                default:
                    newfile = new RDFFile(this.myPrefs);
                    break;
            }

            newfile.setDigitalDocument(gdzfile.getDigitalDocument());
            gdzfile = newfile;

        } catch (Exception e) {
            if (task != null) {
                task.setException(e);
            }
            Helper.setFehlerMeldung(Helper.getTranslation("exportError") + process.getTitle(), e);
            myLogger.error("Export abgebrochen, xml-LeseFehler", e);
            return false;
        }

        String rules = ConfigCore.getParameter("copyData.onExport");
        if (rules != null && !rules.equals("- keine Konfiguration gefunden -")) {
            try {
                new DataCopier(rules).process(new CopierData(newfile, process));
            } catch (ConfigurationException e) {
                if (task != null) {
                    task.setException(e);
                }
                Helper.setFehlerMeldung("dataCopier.syntaxError", e.getMessage());
                return false;
            } catch (RuntimeException e) {
                if (task != null) {
                    task.setException(e);
                }
                Helper.setFehlerMeldung("dataCopier.runtimeException", e.getMessage());
                return false;
            }
        }

        trimAllMetadata(gdzfile.getDigitalDocument().getLogicalDocStruct());

        /*
         * Metadaten validieren
         */

        if (ConfigCore.getBooleanParameter("useMetadatenvalidierung")) {
            MetadatenVerifizierungWithoutHibernate mv = new MetadatenVerifizierungWithoutHibernate();
            if (!mv.validate(gdzfile, this.myPrefs, process.getId(), process.getTitle())) {
                return false;

            }
        }

        /*
         * Speicherort vorbereiten und downloaden
         */
        String zielVerzeichnis;
        SafeFile benutzerHome;

        zielVerzeichnis = this.project.getDmsImportImagesPath();
        benutzerHome = new SafeFile(zielVerzeichnis);

        /* ggf. noch einen Vorgangsordner anlegen */
        if (this.project.isDmsImportCreateProcessFolder()) {
            benutzerHome = new SafeFile(benutzerHome + File.separator + process.getTitle());
            zielVerzeichnis = benutzerHome.getAbsolutePath();
            /* alte Import-Ordner löschen */
            if (!benutzerHome.deleteDir()) {
                Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                        "Import folder could not be cleared");
                return false;
            }
            /* alte Success-Ordner löschen */
            SafeFile successFile = new SafeFile(
                    this.project.getDmsImportSuccessPath() + File.separator + process.getTitle());
            if (!successFile.deleteDir()) {
                Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                        "Success folder could not be cleared");
                return false;
            }
            /* alte Error-Ordner löschen */
            SafeFile errorfile = new SafeFile(
                    this.project.getDmsImportErrorPath() + File.separator + process.getTitle());
            if (!errorfile.deleteDir()) {
                Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                        "Error folder could not be cleared");
                return false;
            }

            if (!benutzerHome.exists()) {
                benutzerHome.mkdir();
            }
            if (task != null) {
                task.setProgress(1);
            }
        }

        /*
         * der eigentliche Download der Images
         */
        try {
            if (this.exportWithImages) {
                imageDownload(process, benutzerHome, atsPpnBand, DIRECTORY_SUFFIX);
                fulltextDownload(process, benutzerHome, atsPpnBand, DIRECTORY_SUFFIX);
            } else if (this.exportFullText) {
                fulltextDownload(process, benutzerHome, atsPpnBand, DIRECTORY_SUFFIX);
            }

            directoryDownload(process, zielVerzeichnis);
        } catch (Exception e) {
            if (task != null) {
                task.setException(e);
            }
            Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(), e);
            return false;
        }

        /*
         * zum Schluss Datei an gewünschten Ort exportieren entweder direkt in
         * den Import-Ordner oder ins Benutzerhome anschliessend den
         * Import-Thread starten
         */
        if (this.project.isUseDmsImport()) {
            if (task != null) {
                task.setWorkDetail(atsPpnBand + ".xml");
            }
            if (MetadataFormat
                    .findFileFormatsHelperByName(this.project.getFileFormatDmsExport()) == MetadataFormat.METS) {
                /* Wenn METS, dann per writeMetsFile schreiben... */
                writeMetsFile(process, benutzerHome + File.separator + atsPpnBand + ".xml", gdzfile, false);
            } else {
                /* ...wenn nicht, nur ein Fileformat schreiben. */
                gdzfile.write(benutzerHome + File.separator + atsPpnBand + ".xml");
            }

            /* ggf. sollen im Export mets und rdf geschrieben werden */
            if (MetadataFormat.findFileFormatsHelperByName(
                    this.project.getFileFormatDmsExport()) == MetadataFormat.METS_AND_RDF) {
                writeMetsFile(process, benutzerHome + File.separator + atsPpnBand + ".mets.xml", gdzfile, false);
            }

            Helper.setMeldung(null, process.getTitle() + ": ", "DMS-Export started");

            if (!ConfigCore.getBooleanParameter("exportWithoutTimeLimit")) {
                /* Success-Ordner wieder löschen */
                if (this.project.isDmsImportCreateProcessFolder()) {
                    SafeFile successFile = new SafeFile(
                            this.project.getDmsImportSuccessPath() + File.separator + process.getTitle());
                    successFile.deleteDir();
                }
            }
        }
        if (task != null) {
            task.setProgress(100);
        }
        return true;
    }

    /**
     * Run through all metadata and children of given docstruct to trim the
     * strings calls itself recursively.
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

    /**
     * Download full text.
     *
     * @param myProcess
     *            process object
     * @param userHome
     *            safe file
     * @param atsPpnBand
     *            String
     * @param ordnerEndung
     *            String
     */
    public void fulltextDownload(ProcessObject myProcess, SafeFile userHome, String atsPpnBand,
            final String ordnerEndung) throws IOException, InterruptedException, SwapException, DAOException {

        // download sources
        SafeFile sources = new SafeFile(fi.getSourceDirectory());
        if (sources.exists() && sources.list().length > 0) {
            SafeFile destination = new SafeFile(userHome + File.separator + atsPpnBand + "_src");
            if (!destination.exists()) {
                destination.mkdir();
            }
            SafeFile[] dateien = sources.listFiles();
            for (int i = 0; i < dateien.length; i++) {
                if (dateien[i].isFile()) {
                    SafeFile meinZiel = new SafeFile(destination + File.separator + dateien[i].getName());
                    dateien[i].copyFile(meinZiel, false);
                }
            }
        }

        SafeFile ocr = new SafeFile(fi.getOcrDirectory());
        if (ocr.exists()) {
            SafeFile[] folder = ocr.listFiles();
            for (SafeFile dir : folder) {
                if (dir.isDirectory() && dir.list().length > 0 && dir.getName().contains("_")) {
                    String suffix = dir.getName().substring(dir.getName().lastIndexOf("_"));
                    SafeFile destination = new SafeFile(userHome + File.separator + atsPpnBand + suffix);
                    if (!destination.exists()) {
                        destination.mkdir();
                    }
                    SafeFile[] files = dir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile()) {
                            SafeFile target = new SafeFile(destination + File.separator + files[i].getName());
                            files[i].copyFile(target, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Download image.
     *
     * @param myProcess
     *            process object
     * @param userHome
     *            safe file
     * @param atsPpnBand
     *            String
     * @param ordnerEndung
     *            String
     */
    public void imageDownload(ProcessObject myProcess, SafeFile userHome, String atsPpnBand, final String ordnerEndung)
            throws IOException, InterruptedException, SwapException, DAOException {
        /*
         * den Ausgangspfad ermitteln
         */
        SafeFile tifOrdner = new SafeFile(this.fi.getImagesTifDirectory(true));

        /*
         * jetzt die Ausgangsordner in die Zielordner kopieren
         */
        if (tifOrdner.exists() && tifOrdner.list().length > 0) {
            SafeFile zielTif = new SafeFile(userHome + File.separator + atsPpnBand + ordnerEndung);

            /* bei Agora-Import einfach den Ordner anlegen */
            if (this.project.isUseDmsImport()) {
                if (!zielTif.exists()) {
                    zielTif.mkdir();
                }
            } else {
                /*
                 * wenn kein Agora-Import, dann den Ordner mit
                 * Benutzerberechtigung neu anlegen
                 */
                User myUser = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
                try {
                    FilesystemHelper.createDirectoryForUser(zielTif.getAbsolutePath(), myUser.getLogin());
                } catch (Exception e) {
                    if (task != null) {
                        task.setException(e);
                    }
                    Helper.setFehlerMeldung("Export canceled, error", "could not create destination directory");
                    myLogger.error("could not create destination directory", e);
                }
            }

            /* jetzt den eigentlichen Kopiervorgang */

            SafeFile[] dateien = tifOrdner.listFiles(Helper.dataFilter);
            for (int i = 0; i < dateien.length; i++) {
                if (task != null) {
                    task.setWorkDetail(dateien[i].getName());
                }
                if (dateien[i].isFile()) {
                    SafeFile meinZiel = new SafeFile(zielTif + File.separator + dateien[i].getName());
                    dateien[i].copyFile(meinZiel, false);
                }
                if (task != null) {
                    task.setProgress((int) ((i + 1) * 98d / dateien.length + 1));
                    if (task.isInterrupted()) {
                        throw new InterruptedException();
                    }
                }
            }
            if (task != null) {
                task.setWorkDetail(null);
            }
        }
    }

    /**
     * Starts copying all directories configured in kitodo_config.properties
     * parameter "processDirs" to export folder.
     *
     * @param myProcess
     *            the process object
     * @param zielVerzeichnis
     *            the destination directory
     */
    private void directoryDownload(ProcessObject myProcess, String zielVerzeichnis) throws IOException {
        String[] processDirs = ConfigCore.getStringArrayParameter("processDirs");

        for (String processDir : processDirs) {

            SafeFile srcDir = new SafeFile(FilenameUtils.concat(fi.getProcessDataDirectory(),
                    processDir.replace("(processtitle)", myProcess.getTitle())));
            SafeFile dstDir = new SafeFile(
                    FilenameUtils.concat(zielVerzeichnis, processDir.replace("(processtitle)", myProcess.getTitle())));

            if (srcDir.isDirectory()) {
                srcDir.copyDir(dstDir);
            }
        }
    }

    /**
     * The method setTask() can be used to pass in a task instance. If that is
     * passed in, the progress in it will be updated during processing and
     * occurring errors will be passed to it to be visible in the task manager
     * screen.
     *
     * @param task
     *            object to submit progress updates and errors to
     */
    public void setTask(EmptyTask task) {
        this.task = task;
    }
}

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

package org.kitodo.exporter.download;

import java.io.IOException;
import java.net.URI;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.MetsModsImportExportInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.kitodo.config.ConfigProjects;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.User;
import org.kitodo.exceptions.ExportFileException;
import org.kitodo.exporter.dms.ExportDmsCorrectRusdml;
import org.kitodo.helper.Helper;
import org.kitodo.legacy.UghImplementation;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.file.FileService;

public class ExportMets {
    private final ServiceManager serviceManager = new ServiceManager();

    private final FileService fileService = serviceManager.getFileService();
    protected Helper help = new Helper();
    protected PrefsInterface myPrefs;

    private static final Logger logger = LogManager.getLogger(ExportMets.class);

    /**
     * DMS-Export in das Benutzer-Homeverzeichnis.
     *
     * @param process
     *            Process object
     */
    public boolean startExport(Process process) throws IOException, PreferencesException, WriteException,
            MetadataTypeNotAllowedException, ExportFileException, ReadException, JAXBException {
        User user = serviceManager.getUserService().getAuthenticatedUser();
        URI userHome = serviceManager.getUserService().getHomeDirectory(user);
        return startExport(process, userHome);
    }

    /**
     * DMS-Export an eine gewünschte Stelle.
     *
     * @param process
     *            Process object
     * @param userHome
     *            String
     */
    public boolean startExport(Process process, URI userHome) throws IOException, PreferencesException, WriteException,
            MetadataTypeNotAllowedException, ExportFileException, ReadException, JAXBException {

        /*
         * Read Document
         */
        this.myPrefs = serviceManager.getRulesetService().getPreferences(process.getRuleset());
        String atsPpnBand = serviceManager.getProcessService().getNormalizedTitle(process.getTitle());
        FileformatInterface gdzfile = serviceManager.getProcessService().readMetadataFile(process);

        if (serviceManager.getProcessService().handleExceptionsForConfiguration(gdzfile, process)) {
            return false;
        }

        // only for the metadata of the RUSDML project
        ConfigProjects cp = new ConfigProjects(process.getProject().getTitle());
        if (cp.getParamList("dmsImport.check").contains("rusdml")) {
            ExportDmsCorrectRusdml exportCorrect = new ExportDmsCorrectRusdml(process, this.myPrefs, gdzfile);
            atsPpnBand = exportCorrect.correctionStart();
        }

        prepareUserDirectory(userHome);

        String targetFileName = atsPpnBand + "_mets.xml";
        URI metaFile = userHome.resolve(userHome.getRawPath() + "/" + targetFileName);
        return writeMetsFile(process, metaFile, gdzfile, false);
    }

    /**
     * prepare user directory.
     *
     * @param targetFolder
     *            the folder to prove and maybe create it
     */
    protected void prepareUserDirectory(URI targetFolder) {
        User user = serviceManager.getUserService().getAuthenticatedUser();
        try {
            fileService.createDirectoryForUser(targetFolder, user.getLogin());
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage("Export canceled, could not create destination directory: " + targetFolder, logger,
                e);
        }
    }

    /**
     * write MetsFile to given Path.
     *
     * @param process
     *            the Process to use
     * @param metaFile
     *            the meta file which should be written
     * @param gdzfile
     *            the FileFormat-Object to use for Mets-Writing
     * @param writeLocalFilegroup
     *            true or false
     * @return true or false
     */
    protected boolean writeMetsFile(Process process, URI metaFile, FileformatInterface gdzfile,
            boolean writeLocalFilegroup) throws PreferencesException, WriteException, IOException, JAXBException {

        MetsModsImportExportInterface mm = UghImplementation.INSTANCE.createMetsModsImportExport(this.myPrefs);
        mm.setWriteLocal(writeLocalFilegroup);
        mm = serviceManager.getSchemaService().tempConvert(gdzfile, this, mm, this.myPrefs, process);
        if (mm != null) {
            mm.write(metaFile.getRawPath());
            Helper.setMessage(process.getTitle() + ": ", "exportFinished");
            return true;
        }
        Helper.setErrorMessage(process.getTitle() + ": was not finished!");
        return false;
    }
}

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

import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.UghHelperException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.exceptions.DocStructHasNoTypeException;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.TypeNotAllowedForParentException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Ruleset;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.UserGroup;
import org.kitodo.data.database.helper.enums.TaskStatus;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.legacy.UghImplementation;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.file.FileService;

public class GoobiScript {
    private HashMap<String, String> parameters;
    private static final Logger logger = LogManager.getLogger(GoobiScript.class);
    private final ServiceManager serviceManager = new ServiceManager();
    private final FileService fileService = serviceManager.getFileService();
    private static final String DIRECTORY_SUFFIX = "_tif";
    private static final String KITODO_SCRIPT_FIELD = "kitodoScriptfield";

    /**
     * Start the script execution.
     *
     * @param processes
     *            list of Process objects
     * @param script
     *            from frontend passed as String
     */
    public void execute(List<Process> processes, String script) throws DataException {
        this.parameters = new HashMap<>();
        // decompose and capture all script parameters
        StrTokenizer tokenizer = new StrTokenizer(script, ' ', '\"');
        while (tokenizer.hasNext()) {
            String tok = tokenizer.nextToken();
            if (Objects.isNull(tok) || !tok.contains(":")) {
                Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "missing delimiter / unknown parameter: ", tok);
            } else {
                String key = tok.substring(0, tok.indexOf(':'));
                String value = tok.substring(tok.indexOf(':') + 1);
                this.parameters.put(key, value);
            }
        }

        // pass the appropriate method with the correct parameters
        if (this.parameters.get("action") == null) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "missing action",
                " - possible: 'action:swapsteps, action:adduser, action:addusergroup, "
                        + "action:swapprozessesout, action:swapprozessesin, action:deleteTiffHeaderFile, "
                        + "action:importFromFileSystem'");
            return;
        }

        // call the correct method via the parameter
        switch (this.parameters.get("action")) {
            case "swapSteps":
                swapTasks(processes);
                break;
            case "importFromFileSystem":
                importFromFileSystem(processes);
                break;
            case "addUser":
                addUser(processes);
                break;
            case "addUserGroup":
                addUserGroup(processes);
                break;
            case "setTaskProperty":
                setTaskProperty(processes);
                break;
            case "deleteStep":
                deleteTask(processes);
                break;
            case "addStep":
                addTask(processes);
                break;
            case "setStepNumber":
                setTaskNumber(processes);
                break;
            case "setStepStatus":
                setTaskStatus(processes);
                break;
            case "addShellScriptToStep":
                addShellScriptToStep(processes);
                break;
            case "updateImagePath":
                updateImagePath(processes);
                break;
            case "updateContentFiles":
                updateContentFiles(processes);
                break;
            case "deleteTiffHeaderFile":
                deleteTiffHeaderFile(processes);
                break;
            case "setRuleset":
                setRuleset(processes);
                break;
            case "exportDms":
                exportDms(processes, this.parameters.get("exportImages"), true);
                break;
            case "export":
                exportDms(processes, this.parameters.get("exportImages"),
                    Boolean.valueOf(this.parameters.get("exportOcr")));
                break;
            case "doit":
                exportDms(processes, "false", false);
                break;
            case "doit2":
                exportDms(processes, "false", true);
                break;
            case "runscript":
                String taskName = this.parameters.get("stepname");
                String scriptName = this.parameters.get("script");
                if (scriptName == null) {
                    Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "", "Missing parameter");
                } else {
                    runScript(processes, taskName, scriptName);
                }
                break;
            case "deleteProcess":
                String value = parameters.get("contentOnly");
                boolean contentOnly = true;
                if (value != null && value.equalsIgnoreCase("false")) {
                    contentOnly = false;
                }
                deleteProcess(processes, contentOnly);
                break;
            default:
                Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Unknown action",
                    " - use: 'action:swapsteps, action:adduser, action:addusergroup, "
                            + "action:swapprozessesout, action:swapprozessesin, action:deleteTiffHeaderFile, "
                            + "action:importFromFileSystem'");
                return;
        }

        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "kitodoScript finished");
    }

    private void updateContentFiles(List<Process> processes) {
        for (Process process : processes) {
            try {
                FileformatInterface rdf = serviceManager.getProcessService().readMetadataFile(process);
                rdf.getDigitalDocument().addAllContentFiles();
                serviceManager.getFileService().writeMetadataFile(rdf, process);
                Helper.setMeldung(KITODO_SCRIPT_FIELD, "ContentFiles updated: ", process.getTitle());
            } catch (PreferencesException | IOException | ReadException | WriteException | RuntimeException e) {
                Helper.setErrorMessage(KITODO_SCRIPT_FIELD, "Error while updating content files", logger, e);
            }
        }
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "updateContentFiles finished");
    }

    private void deleteProcess(List<Process> processes, boolean contentOnly) {
        for (Process process : processes) {
            String title = process.getTitle();
            if (contentOnly) {
                try {
                    URI ocr = fileService.getOcrDirectory(process);
                    if (fileService.fileExist(ocr)) {
                        fileService.delete(ocr);
                    }
                    URI images = fileService.getImagesDirectory(process);
                    if (fileService.fileExist(images)) {
                        fileService.delete(images);
                    }
                    Helper.setMeldung("Content deleted for " + title);
                } catch (IOException | RuntimeException e) {
                    Helper.setErrorMessage("Can not delete content for " + title, logger, e);
                }
            }
            if (!contentOnly) {
                try {
                    deleteMetadataDirectory(process);
                    serviceManager.getProcessService().remove(process);
                    Helper.setMeldung("Process " + title + " deleted.");
                } catch (DataException | IOException e) {
                    Helper.setErrorMessage("could not delete process " + title, logger, e);
                }
            }
        }
    }

    private void deleteMetadataDirectory(Process process) throws IOException {
        serviceManager.getFileService().deleteProcessContent(process);
    }

    private void runScript(List<Process> processes, String taskName, String scriptName) throws DataException {
        for (Process process : processes) {
            for (Task task : process.getTasks()) {
                if (task.getTitle().equalsIgnoreCase(taskName)) {
                    if (scriptName != null) {
                        if (task.getScriptName().equals(scriptName)) {
                            String path = task.getScriptPath();
                            serviceManager.getTaskService().executeScript(task, path, false);
                        }
                    } else {
                        serviceManager.getTaskService().executeScript(task, false);
                    }
                }
            }
        }
    }

    /**
     * Import the data from a directories of the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void importFromFileSystem(List<Process> processes) {
        if (isActionParameterInvalid("sourcefolder")) {
            return;
        }

        URI sourceFolder = new File(this.parameters.get("sourcefolder")).toURI();
        try {
            if (!fileService.isDirectory(sourceFolder)) {
                Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD,
                    "Directory " + this.parameters.get("sourcefolder") + " does not exisist");
                return;
            }
            for (Process process : processes) {
                Integer processId = process.getId();
                String processTitle = process.getTitle();
                URI imagesFolder = serviceManager.getProcessService().getImagesOrigDirectory(false, process);
                if (fileService.getSubUris(imagesFolder).size() > 0) {
                    Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "",
                        "The process " + processTitle + " [" + processId + "] has already data in image folder");
                } else {
                    URI sourceFolderProcess = fileService.createResource(sourceFolder, processTitle);
                    if (!fileService.isDirectory(sourceFolder)) {
                        Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "",
                            "The directory for process " + processTitle + " [" + processId + "] is not existing");
                    } else {
                        fileService.copyDirectory(sourceFolderProcess, imagesFolder);
                        Helper.setMeldung(KITODO_SCRIPT_FIELD, "",
                            "The directory for process " + processTitle + " [" + processId + "] is copied");
                    }
                    Helper.setMeldung(KITODO_SCRIPT_FIELD, "",
                        "The process " + processTitle + " [" + processId + "] is copied");
                }
            }
        } catch (IOException e) {
            Helper.setErrorMessage(e.toString(), logger, e);
        }
    }

    /**
     * Set ruleset.
     *
     * @param processes
     *            list of Process objects
     */
    private void setRuleset(List<Process> processes) {
        if (isActionParameterInvalid("ruleset")) {
            return;
        }

        try {
            List<Ruleset> rulesets = serviceManager.getRulesetService()
                    .getByQuery("from Ruleset where title='" + this.parameters.get("ruleset") + "'");
            if (rulesets == null || rulesets.size() == 0) {
                Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Could not find ruleset: ", "ruleset");
                return;
            }
            Ruleset ruleset = rulesets.get(0);

            for (Process process : processes) {
                process.setRuleset(ruleset);
                serviceManager.getProcessService().save(process);
            }
        } catch (DataException | RuntimeException e) {
            Helper.setFehlerMeldung(e);
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Swap two tasks against each other.
     *
     * @param processes
     *            list of Process objects
     */
    private void swapTasks(List<Process> processes) {
        if (isActionParameterInvalid("swap1nr") || isActionParameterInvalid("swap2nr")
                || isActionParameterInvalid("swap1title") || isActionParameterInvalid("swap2title")) {
            return;
        }

        int firstOrder;
        int secondOrder;
        try {
            firstOrder = Integer.parseInt(this.parameters.get("swap1nr"));
            secondOrder = Integer.parseInt(this.parameters.get("swap2nr"));
        } catch (NumberFormatException e1) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Invalid order number used: ",
                this.parameters.get("swap1nr") + " - " + this.parameters.get("swap2nr"));
            return;
        }

        executeActionForSwapTasks(processes, firstOrder, secondOrder);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "swapsteps finished: ");
    }

    private void executeActionForSwapTasks(List<Process> processes, int firstOrder, int secondOrder) {
        for (Process process : processes) {
            Task firstTask = null;
            Task secondTask = null;
            for (Task task : process.getTasks()) {
                if (task.getTitle().equals(this.parameters.get("swap1title")) && task.getOrdering() == firstOrder) {
                    firstTask = task;
                }
                if (task.getTitle().equals(this.parameters.get("swap2title")) && task.getOrdering() == secondOrder) {
                    secondTask = task;
                }
            }
            if (firstTask != null && secondTask != null) {
                TaskStatus statusTemp = firstTask.getProcessingStatusEnum();
                firstTask.setProcessingStatusEnum(secondTask.getProcessingStatusEnum());
                secondTask.setProcessingStatusEnum(statusTemp);
                firstTask.setOrdering(secondOrder);
                secondTask.setOrdering(firstOrder);
                try {
                    serviceManager.getTaskService().save(firstTask);
                    serviceManager.getTaskService().save(secondTask);
                } catch (DataException e) {
                    Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Error on save while swapping tasks in process: ",
                        process.getTitle() + " - " + firstTask.getTitle() + " : " + secondTask.getTitle());
                    logger.error("Error on save while swapping process: " + process.getTitle() + " - "
                            + firstTask.getTitle() + " : " + secondTask.getTitle(),
                        e);
                }
                Helper.setMeldung(KITODO_SCRIPT_FIELD, "Swapped tasks in: ", process.getTitle());
            }

        }
    }

    /**
     * Delete task for the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void deleteTask(List<Process> processes) {
        if (isActionParameterInvalid("steptitle")) {
            return;
        }

        executeActionForDeleteTask(processes);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "deleteStep finished: ");
    }

    private void executeActionForDeleteTask(List<Process> processes) {
        for (Process process : processes) {
            if (process.getTasks() != null) {
                for (Task task : process.getTasks()) {
                    if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                        process.getTasks().remove(task);
                        saveProcess(process);
                        Helper.setMeldung(KITODO_SCRIPT_FIELD, "Removed step from process: ", process.getTitle());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Add tasks to the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void addTask(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("number")
                || isActionParameterInvalidNumber()) {
            return;
        }

        executeActionForAddTask(processes);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "addStep finished: ");
    }

    private void executeActionForAddTask(List<Process> processes) {
        for (Process process : processes) {
            Task task = new Task();
            task.setTitle(this.parameters.get("steptitle"));
            task.setOrdering(Integer.parseInt(this.parameters.get("number")));
            task.setProcess(process);
            process.getTasks().add(task);
            saveProcess(process);
            Helper.setMeldung(KITODO_SCRIPT_FIELD, "Added task to process: ", process.getTitle());
        }
    }

    /**
     * Add ShellScript to task of the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void addShellScriptToStep(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("label")
                || isActionParameterInvalid("script")) {
            return;
        }

        executeActionForAddShellToScript(processes);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "addShellScriptToStep finished: ");
    }

    private void executeActionForAddShellToScript(List<Process> processes) {
        for (Process process : processes) {
            if (process.getTasks() != null) {
                for (Task task : process.getTasks()) {
                    if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                        task.setScriptPath(this.parameters.get("script"));
                        task.setScriptName(this.parameters.get("label"));
                        saveProcess(process);
                        Helper.setMeldung(KITODO_SCRIPT_FIELD, "Added script to step: ", process.getTitle());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Flag von Schritten setzen.
     *
     * @param processes
     *            list of Process objects
     */
    private void setTaskProperty(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("property")
                || isActionParameterInvalid("value")) {
            return;
        }

        String property = this.parameters.get("property");
        String value = this.parameters.get("value");

        if (!property.equals("metadata") && !property.equals("readimages") && !property.equals("writeimages")
                && !property.equals("validate") && !property.equals("exportdms") && !property.equals("batch")
                && !property.equals("automatic")) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "",
                "wrong parameter 'property'; possible values: metadata, readimages, writeimages, "
                        + "validate, exportdms");
            return;
        }

        if (!value.equals("true") && !value.equals("false")) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "wrong parameter 'value'; possible " + "values: true, false");
            return;
        }

        executeActionForSetTaskProperty(processes, property, value);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "setTaskProperty abgeschlossen: ");
    }

    private void executeActionForSetTaskProperty(List<Process> processes, String property, String value) {
        for (Process process : processes) {
            if (process.getTasks() != null) {
                for (Task task : process.getTasks()) {
                    if (task.getTitle().equals(this.parameters.get("steptitle"))) {

                        if (property.equals("metadata")) {
                            task.setTypeMetadata(Boolean.parseBoolean(value));
                        }
                        if (property.equals("automatic")) {
                            task.setTypeAutomatic(Boolean.parseBoolean(value));
                        }
                        if (property.equals("batch")) {
                            task.setBatchStep(Boolean.parseBoolean(value));
                        }
                        if (property.equals("readimages")) {
                            task.setTypeImagesRead(Boolean.parseBoolean(value));
                        }
                        if (property.equals("writeimages")) {
                            task.setTypeImagesWrite(Boolean.parseBoolean(value));
                        }
                        if (property.equals("validate")) {
                            task.setTypeCloseVerify(Boolean.parseBoolean(value));
                        }
                        if (property.equals("exportdms")) {
                            task.setTypeExportDMS(Boolean.parseBoolean(value));
                        }

                        saveProcess(process);
                        Helper.setMeldung(KITODO_SCRIPT_FIELD, "Error while saving process: ", process.getTitle());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Set task status for the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void setTaskStatus(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("status")) {
            return;
        }

        if (!this.parameters.get("status").equals("0") && !this.parameters.get("status").equals("1")
                && !this.parameters.get("status").equals("2") && !this.parameters.get("status").equals("3")) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Wrong status parameter: status ",
                "(possible: 0=closed, 1=open, 2=in work, 3=finished");
            return;
        }

        executeActionForSetTaskStatus(processes);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "setStepStatus finished: ");
    }

    private void executeActionForSetTaskStatus(List<Process> processes) {
        for (Process process : processes) {
            for (Task task : process.getTasks()) {
                if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                    task.setProcessingStatus(
                        serviceManager.getTaskService().setProcessingStatusAsString(this.parameters.get("status")));
                    saveTask(process.getTitle(), task);
                    Helper.setMeldung(KITODO_SCRIPT_FIELD, "stepstatus set in process: ", process.getTitle());
                    break;
                }
            }
        }
    }

    /**
     * Schritte auf bestimmten Reihenfolge setzen.
     *
     * @param processes
     *            list of Process objects
     */
    private void setTaskNumber(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("number")
                || isActionParameterInvalidNumber()) {
            return;
        }

        executeActionForSetTaskNumber(processes);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "setStepNumber finished ");
    }

    private void executeActionForSetTaskNumber(List<Process> processes) {
        for (Process process : processes) {
            for (Task task : process.getTasks()) {
                if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                    task.setOrdering(Integer.parseInt(this.parameters.get("number")));
                    saveTask(process.getTitle(), task);
                    Helper.setMeldung(KITODO_SCRIPT_FIELD, "step order changed in process: ", process.getTitle());
                    break;
                }
            }
        }
    }

    /**
     * Add user to task of the given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void addUser(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("username")) {
            return;
        }

        // checks if user exists
        User user;
        List<User> foundUsers = serviceManager.getUserService()
                .getByQuery("from User where login='" + this.parameters.get("username") + "'");
        if (foundUsers != null && foundUsers.size() > 0) {
            user = foundUsers.get(0);
        } else {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Unknown user: ", this.parameters.get("username"));
            return;
        }

        executeActionForAddUser(processes, user);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "adduser finished.");
    }

    private void executeActionForAddUser(List<Process> processes, User user) {
        for (Process process : processes) {
            for (Task task : process.getTasks()) {
                if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                    List<User> users = task.getUsers();
                    if (!users.contains(user)) {
                        users.add(user);
                        saveTask(process.getTitle(), task);
                    }
                }
            }
            Helper.setMeldung(KITODO_SCRIPT_FIELD, "Added user to step: ", process.getTitle());
        }
    }

    /**
     * Add user group to the task of given processes.
     *
     * @param processes
     *            list of Process objects
     */
    private void addUserGroup(List<Process> processes) {
        if (isActionParameterInvalid("steptitle") || isActionParameterInvalid("group")) {
            return;
        }

        // check if user group exists
        UserGroup userGroup;
        List<UserGroup> foundUserGroups = serviceManager.getUserGroupService()
                .getByQuery("from UserGroup where title='" + this.parameters.get("group") + "'");
        if (foundUserGroups != null && foundUserGroups.size() > 0) {
            userGroup = foundUserGroups.get(0);
        } else {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Unknown group: ", this.parameters.get("group"));
            return;
        }

        executeActionForAddUserGroup(processes, userGroup);
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "addusergroup finished");
    }

    private void executeActionForAddUserGroup(List<Process> processes, UserGroup userGroup) {
        for (Process process : processes) {
            for (Task task : process.getTasks()) {
                if (task.getTitle().equals(this.parameters.get("steptitle"))) {
                    List<UserGroup> userGroups = task.getUserGroups();
                    if (!userGroups.contains(userGroup)) {
                        userGroups.add(userGroup);
                        saveTask(process.getTitle(), task);
                    }
                }
            }
            Helper.setMeldung(KITODO_SCRIPT_FIELD, "added usergroup to step: ", process.getTitle());
        }
    }

    /**
     * Delete TiffHeader file from given processes.
     *
     * @param processes
     *            list of Process objects
     */
    public void deleteTiffHeaderFile(List<Process> processes) {
        for (Process process : processes) {
            try {
                File tiffHeaderFile = new File(
                        serviceManager.getFileService().getImagesDirectory(process) + "tiffwriter.conf");
                if (tiffHeaderFile.exists()) {
                    tiffHeaderFile.delete();
                }
                Helper.setMeldung(KITODO_SCRIPT_FIELD, "TiffHeaderFile deleted: ", process.getTitle());
            } catch (RuntimeException e) {
                Helper.setErrorMessage(KITODO_SCRIPT_FIELD, "Error while deleting TiffHeader", logger, e);
            }
        }
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "deleteTiffHeaderFile finished");
    }

    /**
     * Reset image path in the metadata (possibly delete existing ones first).
     *
     * @param processes
     *            list of Process objects
     */
    public void updateImagePath(List<Process> processes) {
        for (Process process : processes) {
            try {
                FileformatInterface rdf = serviceManager.getProcessService().readMetadataFile(process);
                MetadataTypeInterface mdt = UghHelper.getMetadataType(process, "pathimagefiles");
                List<? extends MetadataInterface> allImagePaths = rdf.getDigitalDocument().getPhysicalDocStruct()
                        .getAllMetadataByType(mdt);
                if (allImagePaths.size() > 0) {
                    for (MetadataInterface md : allImagePaths) {
                        rdf.getDigitalDocument().getPhysicalDocStruct().getAllMetadata().remove(md);
                    }
                }
                MetadataInterface newMetadata = UghImplementation.INSTANCE.createMetadata(mdt);
                if (SystemUtils.IS_OS_WINDOWS) {
                    newMetadata.setStringValue("file:/" + serviceManager.getFileService().getImagesDirectory(process)
                            + process.getTitle() + DIRECTORY_SUFFIX);
                } else {
                    newMetadata.setStringValue("file://" + serviceManager.getFileService().getImagesDirectory(process)
                            + process.getTitle() + DIRECTORY_SUFFIX);
                }
                rdf.getDigitalDocument().getPhysicalDocStruct().addMetadata(newMetadata);
                serviceManager.getFileService().writeMetadataFile(rdf, process);
                Helper.setMeldung(KITODO_SCRIPT_FIELD, "ImagePath updated: ", process.getTitle());
            } catch (UghHelperException | MetadataTypeNotAllowedException | PreferencesException | IOException
                    | ReadException | WriteException | RuntimeException e) {
                Helper.setErrorMessage(KITODO_SCRIPT_FIELD, "Error while updating imagepath", logger, e);
            }
        }
        Helper.setMeldung(KITODO_SCRIPT_FIELD, "", "updateImagePath finished");
    }

    private void exportDms(List<Process> processes, String exportImages, boolean exportFulltext) {
        boolean withoutImages = exportImages != null && exportImages.equals("false");
        for (Process process : processes) {
            try {
                ExportDms dms = new ExportDms(!withoutImages);
                if (withoutImages) {
                    dms.setExportFullText(exportFulltext);
                }
                dms.startExport(process);
            } catch (DocStructHasNoTypeException | PreferencesException | WriteException
                    | MetadataTypeNotAllowedException | ReadException | TypeNotAllowedForParentException | IOException
                    | ExportFileException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private boolean isActionParameterInvalid(String parameter) {
        if (Objects.isNull(this.parameters.get(parameter)) || Objects.equals(this.parameters.get(parameter), "")) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "missing parameter: ", parameter);
            return true;
        }
        return false;
    }

    private boolean isActionParameterInvalidNumber() {
        if (!StringUtils.isNumeric(this.parameters.get("number"))) {
            Helper.setFehlerMeldung(KITODO_SCRIPT_FIELD, "Wrong number parameter", "(only numbers allowed)");
            return true;
        }
        return false;
    }

    private void saveProcess(Process process) {
        try {
            serviceManager.getProcessService().save(process);
        } catch (DataException e) {
            Helper.setErrorMessage(KITODO_SCRIPT_FIELD, "Error while saving process: " + process.getTitle(), logger, e);
        }
    }

    private void saveTask(String processTitle, Task task) {
        try {
            serviceManager.getTaskService().save(task);
        } catch (DataException e) {
            Helper.setErrorMessage(KITODO_SCRIPT_FIELD, "Error while saving - " + processTitle, logger, e);
        }
    }
}

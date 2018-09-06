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

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.metadaten.MetadatenImagesHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.production.cli.helper.WikiFieldHelper;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.kitodo.config.Parameters;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.helper.enums.TaskEditType;
import org.kitodo.data.database.helper.enums.TaskStatus;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.workflow.Problem;
import org.kitodo.workflow.Solution;

public class BatchStepHelper extends BatchHelper {
    private List<Task> steps;
    private static final Logger logger = LogManager.getLogger(BatchStepHelper.class);
    private Task currentStep;
    private Problem problem = new Problem();
    private Solution solution = new Solution();
    private String processName = "";
    private String addToWikiField = "";
    private String script;
    private final WebDav myDav = new WebDav();
    private List<String> processNameList = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param steps
     *            list of tasks
     */
    public BatchStepHelper(List<Task> steps) {
        this.steps = steps;
        for (Task s : steps) {
            this.processNameList.add(s.getProcess().getTitle());
        }
        if (!steps.isEmpty()) {
            this.currentStep = steps.get(0);
            this.processName = this.currentStep.getProcess().getTitle();
            loadProcessProperties(this.currentStep);
        }
    }

    public BatchStepHelper() {
    }

    public List<Task> getSteps() {
        return this.steps;
    }

    public void setSteps(List<Task> steps) {
        this.steps = steps;
    }

    public Task getCurrentStep() {
        return this.currentStep;
    }

    public void setCurrentStep(Task currentStep) {
        this.currentStep = currentStep;
    }

    public List<String> getProcessNameList() {
        return this.processNameList;
    }

    public void setProcessNameList(List<String> processNameList) {
        this.processNameList = processNameList;
    }

    public String getProcessName() {
        return this.processName;
    }

    /**
     * Set process' name.
     *
     * @param processName
     *            String
     */
    public void setProcessName(String processName) {
        this.processName = processName;
        for (Task s : this.steps) {
            if (s.getProcess().getTitle().equals(processName)) {
                this.currentStep = s;
                loadProcessProperties(this.currentStep);
                break;
            }
        }
    }

    private void loadProcessProperties(Task task) {
        Process process = task.getProcess();
        serviceManager.getProcessService().refresh(process);
        this.properties = process.getProperties();
    }

    private void saveStep() throws DataException, DAOException {
        Process p = serviceManager.getProcessService().getById(this.currentStep.getProcess().getId());
        List<Property> props = p.getProperties();
        for (Property processProperty : props) {
            if (processProperty.getTitle() == null) {
                p.getProperties().remove(processProperty);
            }
        }
        this.serviceManager.getProcessService().save(p);
    }

    /**
     * Error management for single.
     */
    public void reportProblemForSingle() {
        this.myDav.uploadFromHome(this.currentStep.getProcess());
        serviceManager.getWorkflowControllerService().setProblem(getProblem());
        try {
            this.currentStep = serviceManager.getWorkflowControllerService().reportProblem(this.currentStep);
            saveStep();
        } catch (DAOException | DataException e) {
            Helper.setErrorMessage("correctionReportProblem", logger, e);
        }
        setProblem(serviceManager.getWorkflowControllerService().getProblem());
    }

    /**
     * Error management for all.
     */
    // TODO: when method will be used should only execute, return value must be given in Form class
    public void reportProblemForAll() {
        for (Task task : this.steps) {
            this.currentStep = task;
            this.myDav.uploadFromHome(this.currentStep.getProcess());
            serviceManager.getWorkflowControllerService().setProblem(getProblem());
            try {
                setCurrentStep(serviceManager.getWorkflowControllerService().reportProblem(this.currentStep));
                saveStep();
            } catch (DAOException | DataException e) {
                Helper.setErrorMessage("correctionReportProblem", logger, e);
            }
        }
        setProblem(serviceManager.getWorkflowControllerService().getProblem());
    }

    /**
     * Get previous tasks for problem reporting.
     *
     * @return list of selected items
     */
    public List<SelectItem> getPreviousStepsForProblemReporting() {
        List<SelectItem> answer = new ArrayList<>();
        List<Task> previousTasksForProblemReporting = serviceManager.getTaskService()
                .getPreviousTasksForProblemReporting(this.currentStep.getOrdering(),
                    this.currentStep.getProcess().getId());
        for (Task task : previousTasksForProblemReporting) {
            answer.add(new SelectItem(task.getTitle(), serviceManager.getTaskService().getTitleWithUserName(task)));
        }
        return answer;
    }

    /**
     * Get next tasks for problem solution.
     *
     * @return list of selected items
     */
    public List<SelectItem> getNextStepsForProblemSolution() {
        List<SelectItem> answer = new ArrayList<>();
        List<Task> nextTasksForProblemSolution = serviceManager.getTaskService()
                .getNextTasksForProblemSolution(this.currentStep.getOrdering(), this.currentStep.getProcess().getId());
        for (Task task : nextTasksForProblemSolution) {
            answer.add(new SelectItem(task.getTitle(), serviceManager.getTaskService().getTitleWithUserName(task)));
        }
        return answer;
    }

    /**
     * Solve problem for single.
     */
    public void solveProblemForSingle(Task currentStep) {
        this.currentStep = currentStep;
        try {
            setCurrentStep(serviceManager.getWorkflowControllerService().solveProblem(this.currentStep));
            saveStep();
        } catch (DAOException | DataException e) {
            Helper.setErrorMessage("correctionSolveProblem", logger, e);
        }
    }

    /**
     * Solve problem for all.
     *
     * @return String
     */
    public String solveProblemForAll() {
        for (Task task : this.steps) {
            this.currentStep = task;
            serviceManager.getWorkflowControllerService().setSolution(getSolution());
            try {
                setCurrentStep(serviceManager.getWorkflowControllerService().solveProblem(this.currentStep));
                saveStep();
            } catch (DAOException | DataException e) {
                Helper.setErrorMessage("correctionSolveProblem", logger, e);
            }
        }
        setSolution(serviceManager.getWorkflowControllerService().getSolution());

        return "";
    }

    /**
     * Temporal method to unify reportProblem and solveProblem methods in
     * WorkflowService.
     *
     * @return id of task to set for problem/solution
     */
    private Integer getIdForCorrection(String taskTitle) {
        for (Task task : this.currentStep.getProcess().getTasks()) {
            if (task.getTitle().equals(taskTitle)) {
                return task.getId();
            }
        }
        return 0;
    }

    /**
     * Get problem.
     *
     * @return Problem object
     */
    public Problem getProblem() {
        return problem;
    }

    /**
     * Set problem.
     *
     * @param problem
     *            object
     */
    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    /**
     * Get solution.
     *
     * @return Solution object
     */
    public Solution getSolution() {
        return solution;
    }

    /**
     * Set solution.
     *
     * @param solution
     *            object
     */
    public void setSolution(Solution solution) {
        this.solution = solution;
    }

    /**
     * sets new value for wiki field.
     *
     * @param inString
     *            input String
     */
    public void setWikiField(String inString) {
        this.currentStep.getProcess().setWikiField(inString);
    }

    public String getWikiField() {
        return this.currentStep.getProcess().getWikiField();
    }

    public String getAddToWikiField() {
        return this.addToWikiField;
    }

    public void setAddToWikiField(String addToWikiField) {
        this.addToWikiField = addToWikiField;
    }

    /**
     * Add to wiki field.
     */
    public void addToWikiField() {
        if (addToWikiField != null && addToWikiField.length() > 0) {
            User user = serviceManager.getUserService().getAuthenticatedUser();
            String message = this.addToWikiField + " (" + serviceManager.getUserService().getFullName(user) + ")";
            this.currentStep.getProcess().setWikiField(WikiFieldHelper.getWikiMessage(this.currentStep.getProcess(),
                this.currentStep.getProcess().getWikiField(), "user", message));
            this.addToWikiField = "";
            try {
                this.serviceManager.getProcessService().save(this.currentStep.getProcess());
            } catch (DataException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Add to wiki field for all.
     */
    public void addToWikiFieldForAll() {
        if (addToWikiField != null && addToWikiField.length() > 0) {
            User user = serviceManager.getUserService().getAuthenticatedUser();
            String message = this.addToWikiField + " (" + serviceManager.getUserService().getFullName(user) + ")";
            for (Task task : this.steps) {
                task.getProcess().setWikiField(WikiFieldHelper.getWikiMessage(task.getProcess(),
                    task.getProcess().getWikiField(), "user", message));
                try {
                    this.serviceManager.getProcessService().save(task.getProcess());
                } catch (DataException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            this.addToWikiField = "";
        }
    }

    public String getScript() {
        return this.script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Execute script.
     */
    public void executeScript() throws DataException {
        for (Task task : this.steps) {
            if (task.getScriptName().equals(this.script)) {
                String scriptPath = task.getScriptPath();
                serviceManager.getTaskService().executeScript(task, scriptPath, false);
            }
        }
    }

    /**
     * Export DMS.
     */
    public void exportDMS() {
        for (Task step : this.steps) {
            ExportDms export = new ExportDms();
            try {
                export.startExport(step.getProcess());
            } catch (PreferencesException | WriteException | MetadataTypeNotAllowedException | ReadException
                    | IOException | ExportFileException | RuntimeException | JAXBException e) {
                Helper.setErrorMessage("errorExporting",
                    new Object[] {Helper.getTranslation("arbeitschritt"), step.getId() }, logger, e);
            }
        }
    }

    /**
     * Not sure.
     *
     * @return String
     */
    public String setBatchTasksToOpen() {

        for (Task task : this.steps) {
            this.myDav.uploadFromHome(task.getProcess());
            task.setProcessingStatusEnum(TaskStatus.OPEN);
            if (serviceManager.getWorkflowControllerService().isCorrectionTask(task)) {
                task.setProcessingBegin(null);
            }
            task.setEditTypeEnum(TaskEditType.MANUAL_MULTI);
            task.setProcessingTime(new Date());
            User user = serviceManager.getUserService().getAuthenticatedUser();
            serviceManager.getTaskService().replaceProcessingUser(task, user);

            try {
                this.serviceManager.getTaskService().save(task);
            } catch (DataException e) {
                Helper.setErrorMessage(e.getMessage(), logger, e);
            }
        }
        return "";
    }

    /**
     * Not sure.
     *
     * @return String
     */
    public String closeBatchTasks() {
        for (Task task : this.steps) {

            try {
                boolean valid = isTaskValid(task);

                if (valid) {
                    this.myDav.uploadFromHome(task.getProcess());
                    task.setEditTypeEnum(TaskEditType.MANUAL_MULTI);
                    serviceManager.getWorkflowControllerService().close(task);
                }
            } catch (DataException | IOException e) {
                Helper.setErrorMessage(e.getMessage(), logger, e);
            }
        }

        return "";
    }

    private boolean isTaskValid(Task task) throws IOException {
        boolean valid = true;

        if (task.isTypeCloseVerify()) {
            if (invalidMetadataExists(task)) {
                valid = false;
            }
            if (invalidImageExists(task)) {
                valid = false;
            }

            loadProcessProperties(task);
            if (invalidPropertyExists(task)) {
                valid = false;
            }
        }

        return valid;
    }

    private boolean invalidMetadataExists(Task task) {
        if (task.isTypeMetadata() && ConfigCore.getBooleanParameter(Parameters.USE_META_DATA_VALIDATION)) {
            serviceManager.getMetadataValidationService().setAutoSave(true);
            return !serviceManager.getMetadataValidationService().validate(task.getProcess());
        }
        return false;
    }

    private boolean invalidImageExists(Task task) throws IOException {
        if (task.isTypeImagesWrite()) {
            MetadatenImagesHelper mih = new MetadatenImagesHelper(null, null);
            if (!mih.checkIfImagesValid(task.getProcess().getTitle(),
                serviceManager.getProcessService().getImagesOrigDirectory(false, task.getProcess()))) {
                Helper.setErrorMessage("Error on image validation!");
                return true;
            }
        }
        return false;
    }

    private boolean invalidPropertyExists(Task task) {
        for (Property prop : this.properties) {
            if (isPropertyInvalid(prop, task)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPropertyInvalid(Property property, Task task) {
        if (property.getValue() == null || property.getValue().equals("")) {
            Helper.setErrorMessage("BatchPropertyEmpty",
                new Object[] {property.getTitle(), task.getProcess().getTitle() });
            return true;
        }
        return false;
    }
}

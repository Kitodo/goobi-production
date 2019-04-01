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

package org.kitodo.production.services.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.SystemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kitodo.ExecutionPermission;
import org.kitodo.MockDatabase;
import org.kitodo.SecurityTestUtils;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.enums.TaskStatus;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.data.TaskService;
import org.kitodo.production.services.file.FileService;

public class WorkflowControllerServiceIT {

    private static final File scriptCreateDirUserHome = new File(
            ConfigCore.getParameter(ParameterCore.SCRIPT_CREATE_DIR_USER_HOME));
    private static final File scriptCreateSymLink = new File(
            ConfigCore.getParameter(ParameterCore.SCRIPT_CREATE_SYMLINK));
    private static final File scriptNotWorking = new File("src/test/resources/scripts/not_working_script.sh");
    private static final File scriptWorking = new File("src/test/resources/scripts/working_script.sh");
    private static final File usersDirectory = new File("src/test/resources/users");
    private static final FileService fileService = ServiceManager.getFileService();
    private static final TaskService taskService = ServiceManager.getTaskService();
    private static final WorkflowControllerService workflowService = ServiceManager.getWorkflowControllerService();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertProcessesForWorkflowFull();
        SecurityTestUtils.addUserDataToSecurityContext(ServiceManager.getUserService().getById(1), 1);

        usersDirectory.mkdir();

        if (!SystemUtils.IS_OS_WINDOWS) {
            ExecutionPermission.setExecutePermission(scriptCreateDirUserHome);
            ExecutionPermission.setExecutePermission(scriptCreateSymLink);
            ExecutionPermission.setExecutePermission(scriptNotWorking);
            ExecutionPermission.setExecutePermission(scriptWorking);
        }
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();
        SecurityTestUtils.cleanSecurityContext();

        if (!SystemUtils.IS_OS_WINDOWS) {
            ExecutionPermission.setNoExecutePermission(scriptCreateDirUserHome);
            ExecutionPermission.setNoExecutePermission(scriptCreateSymLink);
            ExecutionPermission.setNoExecutePermission(scriptNotWorking);
            ExecutionPermission.setNoExecutePermission(scriptWorking);
        }

        usersDirectory.delete();
    }

    @Test
    public void shouldSetTaskStatusUp() throws Exception {
        Task task = taskService.getById(10);

        workflowService.setTaskStatusUp(task);
        assertEquals("Task '" + task.getTitle() + "' status was not set up!", TaskStatus.OPEN,
            task.getProcessingStatus());

        workflowService.setTaskStatusDown(task);
        taskService.save(task);
    }

    @Test
    public void shouldSetTasksStatusUp() throws Exception {
        Process process = ServiceManager.getProcessService().getById(1);

        workflowService.setTasksStatusUp(process);
        for (Task task : process.getTasks()) {
            if (Objects.equals(task.getId(), 9)) {
                assertEquals("Task '" + task.getTitle() + "' status was not set up!", TaskStatus.INWORK,
                    task.getProcessingStatus());
            } else if (Objects.equals(task.getId(), 10)) {
                assertEquals("Task '" + task.getTitle() + "' status was not set up!", TaskStatus.OPEN,
                    task.getProcessingStatus());
            } else {
                assertEquals("Task '" + task.getTitle() + "' status was not set up!", TaskStatus.DONE,
                    task.getProcessingStatus());
            }
        }

        // set up task to previous state
        Task task = taskService.getById(6);
        workflowService.setTaskStatusDown(task);
        taskService.save(task);
    }

    @Test
    public void shouldSetTasksStatusDown() throws Exception {
        Process process = ServiceManager.getProcessService().getById(1);

        workflowService.setTasksStatusDown(process);
        List<Task> tasks = process.getTasks();
        // TODO: shouldn't be changed this status from done to in work?
        // assertEquals("Task status was not set down for first task!",
        // TaskStatus.INWORK, tasks.get(0).getProcessingStatusEnum());
        assertEquals("Task '" + tasks.get(3).getTitle() + "' status was not set down!", TaskStatus.OPEN,
            tasks.get(3).getProcessingStatus());

        // set up task to previous state
        Task task = taskService.getById(8);
        workflowService.setTaskStatusUp(task);
        taskService.save(task);
    }

    @Test
    public void shouldClose() throws Exception {
        Task task = taskService.getById(9);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        Task nextTask = taskService.getById(10);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        // set up tasks to previous states
        workflowService.setTaskStatusDown(task);
        workflowService.setTaskStatusDown(nextTask);

        taskService.save(task);
        taskService.save(nextTask);
    }

    @Test
    public void shouldCloseForProcessWithParallelTasks() throws Exception {
        Task task = taskService.getById(19);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        // Task 2 and 4 are set up to open because they are concurrent and conditions
        // were evaluated to true
        Task nextTask = taskService.getById(20);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        // Task 3 has XPath which evaluates to false - it gets immediately closed
        nextTask = taskService.getById(21);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to done!", TaskStatus.DONE,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(22);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(23);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseForInWorkProcessWithParallelTasks() throws Exception {
        Task task = taskService.getById(25);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        // Task 3 has XPath which evaluates to false - it gets immediately closed
        Task nextTask = taskService.getById(26);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to done!", TaskStatus.DONE,
            nextTask.getProcessingStatus());

        // Task 3 and 4 are concurrent - 3 got immediately finished, 4 is set to open
        nextTask = taskService.getById(27);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(28);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseForInWorkProcessWithBlockingParallelTasks() throws Exception {
        Task task = taskService.getById(30);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        Task nextTask = taskService.getById(31);
        assertEquals("Task '" + nextTask.getTitle() + "' is not in work!", TaskStatus.INWORK,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(32);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set to locked!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(33);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseForInWorkProcessWithNonBlockingParallelTasks() throws Exception {
        Task task = taskService.getById(35);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        Task nextTask = taskService.getById(36);
        assertEquals("Task '" + nextTask.getTitle() + "' is not in work!", TaskStatus.INWORK,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(37);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(38);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseForAlmostFinishedProcessWithParallelTasks() throws Exception {
        Task task = taskService.getById(42);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        Task nextTask = taskService.getById(43);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseAndAssignNextForProcessWithParallelTasks() throws Exception {
        Task task = taskService.getById(44);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        // Task 2 and 4 are set up to open because they are concurrent and conditions
        // were evaluated to true
        Task nextTask = taskService.getById(45);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
                nextTask.getProcessingStatus());

        // Task 3 has XPath which evaluates to false - it gets immediately closed
        nextTask = taskService.getById(46);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to done!", TaskStatus.DONE,
                nextTask.getProcessingStatus());

        nextTask = taskService.getById(47);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
                nextTask.getProcessingStatus());

        nextTask = taskService.getById(48);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
                nextTask.getProcessingStatus());

        fileService.createDirectory(URI.create("9"), "images");

        workflowService.assignTaskToUser(taskService.getById(45));

        fileService.delete(URI.create("9/images"));

        // Task 4 should be kept open
        Task nextConcurrentTask = taskService.getById(47);
        assertEquals("Task '" + nextConcurrentTask.getTitle() + "' was not kept to open!", TaskStatus.OPEN,
                nextConcurrentTask.getProcessingStatus());
    }

    @Test
    public void shouldCloseForProcessWithScriptParallelTasks() throws Exception {
        assumeTrue(!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC);
        // if you want to execute test on windows change sh to bat in
        // gateway-test5.bpmn20.xml

        Task task = taskService.getById(54);

        workflowService.close(task);
        assertEquals("Task '" + task.getTitle() + "' was not closed!", TaskStatus.DONE, task.getProcessingStatus());

        // Task 2 and 4 are set up to open because they are concurrent and conditions
        // were evaluated to true
        Task nextTask = taskService.getById(55);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        // Task 3 has Script which evaluates to false - it gets immediately closed
        nextTask = taskService.getById(56);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to done!", TaskStatus.DONE,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(57);
        assertEquals("Task '" + nextTask.getTitle() + "' was not set up to open!", TaskStatus.OPEN,
            nextTask.getProcessingStatus());

        nextTask = taskService.getById(58);
        assertEquals("Task '" + nextTask.getTitle() + "' was set up to open!", TaskStatus.LOCKED,
            nextTask.getProcessingStatus());
    }

    @Test
    public void shouldAssignTaskToUser() throws Exception {
        fileService.createDirectory(URI.create(""), "1");
        fileService.createDirectory(URI.create("1"), "images");

        Task task = taskService.getById(6);

        workflowService.assignTaskToUser(task);
        assertEquals("Incorrect user was assigned to the task!", Integer.valueOf(1), task.getProcessingUser().getId());

        fileService.delete(URI.create("1/images"));
        fileService.delete(URI.create("1"));
    }

    @Test
    public void shouldUnassignTaskFromUser() throws Exception {
        Task task = taskService.getById(6);

        workflowService.unassignTaskFromUser(task);
        assertNull("User was not unassigned from the task!", task.getProcessingUser());
        assertEquals("Task was not set up to open after unassing of the user!", TaskStatus.OPEN,
            task.getProcessingStatus());
    }

    @Test
    public void shouldReportProblem() throws Exception {
        /*Problem problem = new Problem();
        problem.setId(6);
        problem.setMessage("Fix it!");
        workflowService.setProblem(problem);

        Task currentTask = taskService.getById(8);
        workflowService.reportProblem(currentTask);

        Task correctionTask = taskService.getById(6);
        assertEquals(
            "Report of problem was incorrect - task '" + correctionTask.getTitle() + "' is not set up to open!",
            TaskStatus.OPEN, correctionTask.getProcessingStatus());

        assertTrue(
            "Report of problem was incorrect - task '" + correctionTask.getTitle() + "' is not a correction task!",
            workflowService.isCorrectionTask(correctionTask));

        Process process = currentTask.getProcess();
        for (Task task : process.getTasks()) {
            if (correctionTask.getOrdering() < task.getOrdering() && task.getOrdering() < currentTask.getOrdering()) {
                assertEquals("Report of problem was incorrect - tasks between were not set up to locked!",
                    TaskStatus.LOCKED, task.getProcessingStatus());
            }
        }

        // set up tasks to previous states
        MockDatabase.cleanDatabase();
        MockDatabase.insertProcessesForWorkflowFull();*/
    }

    @Test
    public void shouldSolveProblem() throws Exception {
        /*Problem problem = new Problem();
        problem.setId(6);
        problem.setMessage("Fix it!");

        Solution solution = new Solution();
        solution.setId(8);
        solution.setMessage("Fixed");

        workflowService.setProblem(problem);
        workflowService.setSolution(solution);

        Task currentTask = taskService.getById(8);
        workflowService.reportProblem(currentTask);
        currentTask = taskService.getById(6);
        workflowService.solveProblem(currentTask);

        Task correctionTask = taskService.getById(8);

        Process process = currentTask.getProcess();
        for (Task task : process.getTasks()) {
            if (currentTask.getOrdering() < task.getOrdering() && task.getOrdering() < correctionTask.getOrdering()) {
                assertEquals("Solve of problem was incorrect - tasks between were not set up to done!", TaskStatus.DONE,
                    task.getProcessingStatus());
            }
        }

        assertEquals("Solve of problem was incorrect - tasks from which correction was send was not set up to open!",
            TaskStatus.OPEN, correctionTask.getProcessingStatus());

        // set up tasks to previous states
        MockDatabase.cleanDatabase();
        MockDatabase.insertProcessesForWorkflowFull();*/
    }
}

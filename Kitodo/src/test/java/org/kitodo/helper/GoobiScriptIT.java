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

package org.kitodo.helper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kitodo.MockDatabase;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.helper.enums.TaskStatus;
import org.kitodo.services.ServiceManager;

public class GoobiScriptIT {

    private final ServiceManager serviceManager = new ServiceManager();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertProcessesFull();
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();
    }

    @Test
    public void shouldExecuteAddRoleScript() throws Exception {
        GoobiScript goobiScript = new GoobiScript();

        Task task = serviceManager.getTaskService().getById(8);
        int amountOfRoles = task.getRoles().size();

        String script = "action:addRole \"steptitle:Progress\" role:General";
        List<Process> processes = new ArrayList<>();
        processes.add(serviceManager.getProcessService().getById(1));
        goobiScript.execute(processes, script);

        task = serviceManager.getTaskService().getById(8);
        assertEquals("Role was not correctly added to task!", amountOfRoles + 1, task.getRoles().size());
    }

    @Test
    public void shouldExecuteSetTaskStatusScript() throws Exception {
        MockDatabase.cleanDatabase();
        MockDatabase.insertProcessesFull();

        GoobiScript goobiScript = new GoobiScript();

        String script = "action:setStepStatus \"steptitle:Progress\" status:3";
        List<Process> processes = new ArrayList<>();
        processes.add(serviceManager.getProcessService().getById(1));
        goobiScript.execute(processes, script);

        Task task = serviceManager.getTaskService().getById(8);
        assertEquals("Processing status was not correctly changed!", TaskStatus.DONE, task.getProcessingStatusEnum());
    }

    @Test
    public void shouldExecuteAddShellScriptToTaskScript() throws Exception {
        GoobiScript goobiScript = new GoobiScript();

        String script = "action:addShellScriptToStep \"steptitle:Progress\" \"label:script\" \"script:/some/new/path\"";
        List<Process> processes = new ArrayList<>();
        processes.add(serviceManager.getProcessService().getById(1));
        goobiScript.execute(processes, script);

        Task task = serviceManager.getTaskService().getById(8);
        assertEquals("Script was not added to task - incorrect name!", "script", task.getScriptName());
        assertEquals("Script was not added to task - incorrect path!", "/some/new/path", task.getScriptPath());
    }

    @Test
    public void shouldExecuteSetPropertyTaskScript() throws Exception {
        GoobiScript goobiScript = new GoobiScript();

        String script = "action:setTaskProperty \"steptitle:Closed\" property:validate value:true";
        List<Process> processes = new ArrayList<>();
        processes.add(serviceManager.getProcessService().getById(1));
        goobiScript.execute(processes, script);

        Task task = serviceManager.getTaskService().getById(7);
        assertTrue("Task property was not set!", task.isTypeCloseVerify());
    }

    @Test
    public void shouldNotExecuteSetPropertyTaskScript() throws Exception {
        GoobiScript goobiScript = new GoobiScript();

        String script = "action:setTaskProperty \"steptitle:Closed\" property:validate value:invalid";
        List<Process> processes = new ArrayList<>();
        processes.add(serviceManager.getProcessService().getById(1));
        goobiScript.execute(processes, script);

        Task task = serviceManager.getTaskService().getById(7);
        assertFalse("Task property was set - default value is false!", task.isTypeCloseVerify());
    }
}

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

package org.kitodo.production.services.data;

import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.URI;
import java.util.List;

import javax.json.JsonObject;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kitodo.data.database.beans.Authority;
import org.kitodo.data.database.beans.Role;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.production.ExecutionPermission;
import org.kitodo.production.MockDatabase;
import org.kitodo.production.SecurityTestUtils;
import org.kitodo.production.config.ConfigCore;
import org.kitodo.production.config.enums.ParameterCore;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.file.FileService;

/**
 * Tests for UserService class.
 */
public class UserServiceIT {

    private static final FileService fileService = ServiceManager.getFileService();
    private static final UserService userService = ServiceManager.getUserService();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertProcessesFull();
        MockDatabase.setUpAwaitility();

        fileService.createDirectory(URI.create(""), "users");
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();

        fileService.delete(URI.create("users"));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCountAllUsers() {
        await().untilAsserted(
            () -> assertEquals("Users were not counted correctly!", Long.valueOf(6), userService.count()));
    }

    @Test
    public void shouldCountUsersAccordingToQuery() {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.mustNot(matchQuery("_id", "1"));
        boolQuery.must(matchQuery("login", "kowal"));
        await().untilAsserted(
            () -> assertEquals("User was found!", Long.valueOf(0), userService.count(boolQuery.toString())));

        await().untilAsserted(() -> assertEquals("User was found!", Long.valueOf(0),
            userService.getAmountOfUsersWithExactlyTheSameLogin("1", "kowal")));

        BoolQueryBuilder boolQuerySecond = new BoolQueryBuilder();
        boolQuerySecond.must(matchQuery("_id", "1"));
        boolQuerySecond.must(matchQuery("login", "kowal"));
        await().untilAsserted(
            () -> assertEquals("User was not found!", Long.valueOf(1), userService.count(boolQuerySecond.toString())));

        await().untilAsserted(() -> assertEquals("User was not found!", Long.valueOf(1),
            userService.getAmountOfUsersWithExactlyTheSameLogin(null, "kowal")));

        await().untilAsserted(() -> assertEquals("User was not found!", Long.valueOf(1),
            userService.getAmountOfUsersWithExactlyTheSameLogin("2", "kowal")));
    }

    @Test
    public void shouldCountAllDatabaseRowsForUsers() throws Exception {
        Long amount = userService.countDatabaseRows();
        assertEquals("Users were not counted correctly!", Long.valueOf(6), amount);
    }

    @Test
    public void shouldGetUser() throws Exception {
        User user = userService.getById(1);
        boolean condition = user.getName().equals("Jan") && user.getSurname().equals("Kowalski");
        assertTrue("User was not found in database!", condition);

        assertEquals("User was found but tasks were not inserted!", 3, user.getProcessingTasks().size());
    }

    @Test
    public void shouldGetAllUsers() throws Exception {
        List<User> users = userService.getAll();
        assertEquals("Not all users were found in database!", 6, users.size());
    }

    @Test
    public void shouldGetAllUsersInGivenRange() throws Exception {
        List<User> users = userService.getAll(2, 10);
        assertEquals("Not all users were found in database!", 4, users.size());
    }

    @Test
    public void shouldRemoveUser() throws Exception {
        User user = new User();
        user.setLogin("Remove");
        userService.save(user);
        User foundUser = userService.convertJSONObjectToBean(userService.findByLogin("Remove"));
        assertEquals("Additional user was not inserted in database!", "Remove", foundUser.getLogin());

        userService.remove(foundUser);
        foundUser = userService.getById(foundUser.getId());
        assertNull("Additional user was not removed from database!", foundUser.getLogin());

        user = new User();
        user.setLogin("remove");
        userService.save(user);
        foundUser = userService.convertJSONObjectToBean(userService.findByLogin("remove"));
        assertEquals("Additional user was not inserted in database!", "remove", foundUser.getLogin());

        userService.remove(foundUser.getId());
        foundUser = userService.getById(foundUser.getId());
        assertNull("Additional user was not removed from database!", foundUser.getLogin());
    }

    @Test
    public void shouldRemoveUserButNotRole() throws Exception {
        RoleService roleService = ServiceManager.getRoleService();

        Role role = new Role();
        role.setTitle("Cascade Group");
        roleService.saveToDatabase(role);

        User user = new User();
        user.setLogin("Cascade");
        user.getRoles().add(
            roleService.getByQuery("FROM Role WHERE title = 'Cascade Group' ORDER BY id DESC").get(0));
        userService.saveToDatabase(user);
        User foundUser = userService.getByQuery("FROM User WHERE login = 'Cascade'").get(0);
        assertEquals("Additional user was not inserted in database!", "Cascade", foundUser.getLogin());

        userService.removeFromDatabase(foundUser);
        int size = userService.getByQuery("FROM User WHERE login = 'Cascade'").size();
        assertEquals("Additional user was not removed from database!", 0, size);

        size = roleService.getByQuery("FROM Role WHERE title = 'Cascade Group'").size();
        assertEquals("Role was removed from database!", 1, size);

        roleService
                .removeFromDatabase(roleService.getByQuery("FROM Role WHERE title = 'Cascade Group'").get(0));
    }

    @Test
    public void shouldFindById() {
        String expected = "kowal";
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", expected, userService.findById(1).getLogin()));
    }

    @Test
    public void shouldFindByName() {
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", 1, userService.findByName("Jan").size()));
    }

    @Test
    public void shouldNotFindByName() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 0, userService.findByName("Jannik").size()));
    }

    @Test
    public void shouldFindBySurname() {
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", 1, userService.findBySurname("Kowalski").size()));
    }

    @Test
    public void shouldNotFindBySurname() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 0, userService.findBySurname("Müller").size()));
    }

    @Test
    public void shouldFindByFullName() {
        await().untilAsserted(() -> assertEquals("User was not found in index!", 1,
            userService.findByFullName("Jan", "Kowalski").size()));
    }

    @Test
    public void shouldNotFindByFullName() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 0, userService.findByFullName("Jannik", "Müller").size()));
    }

    @Test
    public void shouldFindByLogin() {
        Integer expected = 1;
        await().untilAsserted(() -> assertEquals("User was not found in index!", expected,
            userService.getIdFromJSONObject(userService.findByLogin("kowal"))));
    }

    @Test
    public void shouldNotFindByLogin() {
        Integer expected = 0;
        await().untilAsserted(() -> assertEquals("User was found in index!", expected,
            userService.getIdFromJSONObject(userService.findByLogin("random"))));
    }

    @Test
    public void shouldFindByLdapLogin() {
        Integer expected = 1;
        await().untilAsserted(() -> assertEquals("User was not found in index!", expected,
            userService.getIdFromJSONObject(userService.findByLdapLogin("kowalLDP"))));
    }

    @Test
    public void shouldNotFindByLdapLogin() {
        Integer expected = 0;
        await().untilAsserted(() -> assertEquals("User was found in index!", expected,
            userService.getIdFromJSONObject(userService.findByLdapLogin("random"))));
    }

    @Test
    public void shouldFindManyByLocation() {
        await().untilAsserted(
            () -> assertEquals("Users were not found in index!", 4, userService.findByLocation("Dresden").size()));
    }

    @Test
    public void shouldFindOneByLocation() {
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", 1, userService.findByLocation("Leipzig").size()));
    }

    @Test
    public void shouldNotFindByLocation() {
        await().untilAsserted(
            () -> assertEquals("Users were found in index!", 0, userService.findByLocation("Wroclaw").size()));
    }

    @Test
    public void shouldFindByActive() throws Exception {
        List<JsonObject> users = userService.findByActive(true);
        boolean result = users.size() == 2 || users.size() == 3 || users.size() == 4 || users.size() == 5;
        assertTrue("Users were not found in index!", result);

        users = userService.findByActive(false);
        result = users.size() == 1 || users.size() == 2 || users.size() == 3 || users.size() == 4;
        assertTrue("Users were found in index!", result);
    }

    @Test
    public void shouldFindByRoleId() {
        await().untilAsserted(
            () -> assertEquals("Users were not found in index!", 2, userService.findByRoleId(1).size()));
    }

    @Test
    public void shouldNotFindByRoleId() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 1, userService.findByRoleId(3).size()));
    }

    @Test
    public void shouldFindByRoleTitle() {
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", 2, userService.findByRoleTitle("Admin").size()));
    }

    @Test
    public void shouldNotFindByRoleTitle() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 0, userService.findByRoleTitle("None").size()));
    }

    @Test
    public void shouldFindByFilter() {
        await().untilAsserted(
            () -> assertEquals("User was not found in index!", 1, userService.findByFilter("\"id:1\"").size()));
    }

    @Test
    public void shouldNotFindByFilter() {
        await().untilAsserted(
            () -> assertEquals("User was found in index!", 0, userService.findByFilter("\"id:5\"").size()));
    }

    @Test
    public void shouldGetTableSize() throws Exception {
        User user = userService.getById(1);
        int actual = user.getTableSize();
        assertEquals("Table size is incorrect!", 20, actual);

        user = userService.getById(2);
        actual = user.getTableSize();
        assertEquals("Table size is incorrect!", 10, actual);
    }

    @Test
    public void shouldGetRolesSize() {
        await().untilAsserted(
            () -> assertEquals("User groups' size is incorrect!", 2, userService.findById(1).getRolesSize()));

        await().untilAsserted(
            () -> assertEquals("User groups' size is incorrect!", 2, userService.findById(1, true).getRolesSize()));

        await().untilAsserted(() -> assertEquals("User group's title is incorrect!", "Admin",
            userService.findById(1, true).getRoles().get(0).getTitle()));
    }

    @Test
    public void shouldGetProcessingTasksSize() {
        await().untilAsserted(() -> assertEquals("Processing tasks' size is incorrect!", 3,
            userService.findById(1).getProcessingTasks().size()));
    }

    @Test
    public void shouldFindClientSize() {
        await().untilAsserted(
            () -> assertEquals("Projects' size is incorrect!", 1, userService.findById(1).getClientsSize()));
    }

    @Test
    public void shouldGetProjectsSize() {
        await().untilAsserted(
            () -> assertEquals("Projects' size is incorrect!", 2, userService.findById(1).getProjectsSize()));

        await().untilAsserted(
            () -> assertEquals("Projects' size is incorrect!", 2, userService.findById(2).getProjectsSize()));

        await().untilAsserted(
            () -> assertEquals("Projects' size is incorrect!", 2, userService.findById(2, true).getProjectsSize()));

        await().untilAsserted(() -> assertEquals("Project's title is incorrect!", "First project",
            userService.findById(2, true).getProjects().get(0).getTitle()));
    }

    @Test
    public void shouldGetFiltersSize() {
        Integer expected = 2;
        await().untilAsserted(
            () -> assertEquals("Properties' size is incorrect!", expected, userService.findById(1).getFiltersSize()));
    }

    @Test
    public void shouldGetFullName() throws Exception {
        User user = userService.getById(1);
        boolean condition = userService.getFullName(user).equals("Kowalski, Jan");
        assertTrue("Full name of user is incorrect!", condition);
    }

    @Test
    public void shouldGetHomeDirectory() throws Exception {
        assumeTrue(!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC);

        User user = userService.getById(1);
        String homeDirectory = ConfigCore.getParameter(ParameterCore.DIR_USERS);

        File script = new File(ConfigCore.getParameter(ParameterCore.SCRIPT_CREATE_DIR_USER_HOME));
        ExecutionPermission.setExecutePermission(script);

        URI homeDirectoryForUser = userService.getHomeDirectory(user);
        boolean condition = homeDirectoryForUser.getRawPath().contains(homeDirectory + user.getLogin());
        assertTrue("Home directory of user is incorrect!", condition);

        user = userService.getById(2);
        homeDirectoryForUser = userService.getHomeDirectory(user);
        condition = homeDirectoryForUser.getRawPath().contains(user.getLogin());
        assertTrue("Home directory of user is incorrect!", condition);

        ExecutionPermission.setNoExecutePermission(script);
    }

    @Test
    public void shouldFindAllVisibleUsers() {
        await().untilAsserted(
            () -> assertEquals("Size of users is incorrect!", 6, userService.findAllVisibleUsers().size()));

        await().untilAsserted(() -> assertEquals("Size of users is incorrect!", 6,
            userService.findAllVisibleUsersWithRelations().size()));
    }

    @Test
    public void shouldFindAllActiveUsers() {
        await().untilAsserted(
            () -> assertEquals("Size of users is incorrect!", 5, userService.findAllActiveUsers().size()));

        await().untilAsserted(
            () -> assertEquals("Size of users is incorrect!", 5, userService.findAllActiveUsersWithRelations().size()));
    }

    @Test
    public void shouldGetAuthorityOfUser() throws Exception {
        Authority authority = userService.getByLogin("kowal").getRoles().get(0).getAuthorities().get(1);
        assertEquals("Authority title is incorrect!", "viewClient_globalAssignable", authority.getTitle());
    }

    @Test
    public void shouldNotSaveUserWithSameLogin() throws DataException {
        User newUser = new User();
        newUser.setLogin("kowal");
        exception.expect(DataException.class);
        userService.save(newUser);
    }

    @Test
    public void shouldGetLdapServerOfUser() throws DAOException {
        User user = userService.getById(2);
        assertEquals("LdapServer title is incorrect!", "FirstLdapServer",
            user.getLdapGroup().getLdapServer().getTitle());
    }

    @Test
    public void shouldGetUserByLdapLogin() throws DAOException {
        User user = userService.getByLdapLogin("kowalLDP");
        assertEquals("User surname is incorrect!", "Kowalski", user.getSurname());
    }

    @Test
    public void shouldGetUserTasksInProgress() throws DAOException {
        User user = userService.getByLdapLogin("nowakLDP");
        List<Task> tasks = userService.getTasksInProgress(user);
        assertEquals("Number of tasks in process is incorrect!", 1, tasks.size());
        assertEquals("Title of task is incorrect!", "Progress", tasks.get(0).getTitle());
    }

    @Test
    public void shouldGetAuthenticatedUser() throws DAOException {
        SecurityTestUtils.addUserDataToSecurityContext(userService.getById(1), 1);
        User authenticatedUser = userService.getAuthenticatedUser();
        assertEquals("Returned authenticated user was wrong", "kowal", authenticatedUser.getLogin());
        SecurityTestUtils.cleanSecurityContext();
    }
}

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

package org.kitodo.services.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kitodo.MockDatabase;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.UserGroup;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.dto.UserGroupDTO;
import org.kitodo.services.ServiceManager;

/**
 * Tests for UserGroupService class.
 */
public class UserGroupServiceIT {

    private static final UserGroupService userGroupService = new ServiceManager().getUserGroupService();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertUserGroupsFull();
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();
    }

    @Before
    public void multipleInit() throws InterruptedException {
        Thread.sleep(500);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCountAllUserGroups() throws Exception {
        Long amount = userGroupService.count();
        assertEquals("User groups were not counted correctly!", Long.valueOf(3), amount);
    }

    @Test
    public void shouldCountAllDatabaseRowsForUserGroups() throws Exception {
        Long amount = userGroupService.countDatabaseRows();
        assertEquals("User groups were not counted correctly!", Long.valueOf(3), amount);
    }

    @Test
    public void shouldFindAllUserGroups() throws Exception {
        List<UserGroupDTO> userGroups = userGroupService.findAll();
        assertEquals("Not all user's groups were found in database!", 3, userGroups.size());
    }

    @Test
    public void shouldGetUserGroup() throws Exception {
        UserGroup userGroup = userGroupService.getById(1);
        boolean condition = userGroup.getTitle().equals("Admin") && userGroup.getAuthorizations().get(0).getTitle().equals("admin");
        assertTrue("User group was not found in database!", condition);
    }

    @Test
    public void shouldGetAllUserGroupsInGivenRange() throws Exception {
        List<UserGroup> userGroups = userGroupService.getAll(1,10);
        assertEquals("Not all user's groups were found in database!", 2, userGroups.size());
    }

    @Test
    public void shouldRemoveUserGroup() throws Exception {
        UserGroup userGroup = new UserGroup();
        userGroup.setTitle("To Remove");
        userGroupService.save(userGroup);
        Thread.sleep(1000);
        UserGroup foundUserGroup = userGroupService
                .convertJSONObjectToBean(userGroupService.findByTitle("To Remove", true).get(0));
        assertEquals("Additional user group was not inserted in database!", "To Remove", foundUserGroup.getTitle());

        userGroupService.remove(foundUserGroup);
        exception.expect(DAOException.class);
        userGroupService.getById(foundUserGroup.getId());

        userGroup = new UserGroup();
        userGroup.setTitle("To remove");
        userGroupService.save(userGroup);
        Thread.sleep(1000);
        foundUserGroup = userGroupService
                .convertJSONObjectToBean(userGroupService.findByTitle("To remove", true).get(0));
        assertEquals("Additional user group was not inserted in database!", "To remove", foundUserGroup.getTitle());

        userGroupService.remove(foundUserGroup.getId());
        exception.expect(DAOException.class);
        userGroupService.convertJSONObjectToBean(userGroupService.findByTitle("To remove", true).get(0));
    }

    @Test
    public void shouldRemoveUserGroupButNotUser() throws Exception {
        UserService userService = new ServiceManager().getUserService();

        User user = new User();
        user.setLogin("Cascados");
        userService.saveToDatabase(user);

        UserGroup userGroup = new UserGroup();
        userGroup.setTitle("Cascados Group");
        userGroup.getUsers().add(userService.getByQuery("FROM User WHERE login = 'Cascados' ORDER BY id DESC").get(0));
        userGroupService.saveToDatabase(userGroup);

        UserGroup foundUserGroup = userGroupService.getByQuery("FROM UserGroup WHERE title = 'Cascados Group'").get(0);
        assertEquals("Additional user was not inserted in database!", "Cascados Group", foundUserGroup.getTitle());

        userGroupService.removeFromDatabase(foundUserGroup);
        int size = userGroupService.getByQuery("FROM UserGroup WHERE title = 'Cascados Group'").size();
        assertEquals("Additional user was not removed from database!", 0, size);

        size = userService.getByQuery("FROM User WHERE login = 'Cascados'").size();
        assertEquals("User was removed from database!", 1, size);

        userService.removeFromDatabase(userService.getByQuery("FROM User WHERE login = 'Cascados'").get(0));
    }

    @Test
    public void shouldFindById() throws Exception {
        UserGroupDTO userGroup = userGroupService.findById(1);
        String actual = userGroup.getTitle();
        String expected = "Admin";
        assertEquals("User group was not found in index!", expected, actual);

        int usersSize = userGroup.getUsersSize();
        assertEquals("User group was not found in index!", 2, usersSize);
    }

    @Test
    public void shouldFindByTitle() throws Exception {
        List<JSONObject> userGroups = userGroupService.findByTitle("Admin", true);
        Integer actual = userGroups.size();
        Integer expected = 1;
        assertEquals("User group was not found in index!", expected, actual);

        userGroups = userGroupService.findByTitle("none", true);
        actual = userGroups.size();
        expected = 0;
        assertEquals("User group was found in index!", expected, actual);
    }

    @Test
    public void shouldFindByAuthorization() throws Exception {

        List<JSONObject> userGroups = userGroupService.findByAuthorizationTitle("admin");
        Integer actual = userGroups.size();
        Integer expected = 1;
        assertEquals("User group was not found in index!", expected, actual);

        userGroups = userGroupService.findByAuthorizationTitle("user");
        actual = userGroups.size();
        expected = 2;
        assertEquals("User group was not found in index!", expected, actual);

        userGroups = userGroupService.findByAuthorizationTitle("notExisting");
        actual = userGroups.size();
        expected = 0;
        assertEquals("User group was found in index!", expected, actual);
    }

    @Test
    public void shouldFindByUserId() throws Exception {
        List<JSONObject> userGroups = userGroupService.findByUserId(1);
        Integer actual = userGroups.size();
        Integer expected = 1;
        assertEquals("User group was not found in index!", expected, actual);

        userGroups = userGroupService.findByUserId(3);
        actual = userGroups.size();
        expected = 0;
        assertEquals("User groups were found in index!", expected, actual);
    }

    @Test
    public void shouldFindByUserLogin() throws Exception {
        List<JSONObject> userGroups = userGroupService.findByUserLogin("kowal");
        Integer actual = userGroups.size();
        Integer expected = 1;
        assertEquals("User group was not found in index!", expected, actual);

        userGroups = userGroupService.findByUserLogin("dora");
        actual = userGroups.size();
        expected = 0;
        assertEquals("User groups were found in index!", expected, actual);
    }

    @Test
    public void shouldGetAuthorizationsAsString() throws Exception {
        UserGroup userGroup = userGroupService.getById(1);
        List<String> actual = userGroupService.getAuthorizationsAsString(userGroup);
        List<String> expected = Arrays.asList("admin","manager","user");
        assertEquals("Permission strings doesn't match to given plain text!", expected, actual);
    }
}

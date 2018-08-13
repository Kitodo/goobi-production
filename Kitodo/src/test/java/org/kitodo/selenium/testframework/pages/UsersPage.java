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

package org.kitodo.selenium.testframework.pages;

import static org.awaitility.Awaitility.await;
import static org.kitodo.selenium.testframework.Browser.getRowsOfTable;
import static org.kitodo.selenium.testframework.Browser.getTableDataByColumn;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kitodo.MockDatabase;
import org.kitodo.data.database.beans.UserGroup;
import org.kitodo.enums.ObjectType;
import org.kitodo.selenium.testframework.Browser;
import org.kitodo.selenium.testframework.Pages;
import org.kitodo.selenium.testframework.enums.TabIndex;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UsersPage extends Page<UsersPage> {

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView")
    private WebElement usersTabView;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:usersTable_data")
    private WebElement usersTable;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:userGroupsTable_data")
    private WebElement userGroupsTable;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:clientsTable_data")
    private WebElement clientsTable;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:ldapGroupsTable_data")
    private WebElement ldapGroupsTable;

    @SuppressWarnings("unused")
    @FindBy(id = "newElementForm:newElementButton_button")
    private WebElement newElementButton;

    @SuppressWarnings("unused")
    @FindBy(id = "newElementForm:newUserButton")
    private WebElement newUserButton;

    @SuppressWarnings("unused")
    @FindBy(id = "newElementForm:newUserGroupButton")
    private WebElement newUserGroupButton;

    @SuppressWarnings("unused")
    @FindBy(id = "newElementForm:newClientButton")
    private WebElement newClientButton;

    @SuppressWarnings("unused")
    @FindBy(id = "newElementForm:newLdapGroupButton")
    private WebElement newLdapGroupButton;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:usersTable:0:actionForm:deleteUser")
    private WebElement deleteFirstUserButton;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:userGroupsTable:0:actionForm:deleteUsergroup")
    private WebElement deleteFirstUserGroupButton;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:clientsTable:0:actionForm:deleteClient")
    private WebElement deleteFirstClientButton;

    @SuppressWarnings("unused")
    @FindBy(id = "usersTabView:ldapGroupsTable:0:actionForm:deleteLdapgroup")
    private WebElement deleteFirstLDAPGroupButton;

    public UsersPage() {
        super("pages/users.jsf");
    }

    /**
     * Goes to users page.
     * 
     * @return The users page.
     */
    public UsersPage goTo() throws Exception {
        Pages.getTopNavigation().gotoUsers();
        await("Wait for execution of link click").pollDelay(Browser.getDelayMinAfterLinkClick(), TimeUnit.MILLISECONDS)
                .atMost(Browser.getDelayMaxAfterLinkClick(), TimeUnit.MILLISECONDS).ignoreExceptions()
                .until(this::isAt);
        return this;
    }

    /**
     * Counts rows of users table.
     * 
     * @return The number of rows of users table.
     */
    public int countListedUsers() throws Exception {
        if (isNotAt()) {
            goTo();
        }
        return getRowsOfTable(usersTable).size();
    }

    /**
     * Counts rows of user groups table.
     *
     * @return The number of rows of user groups table.
     */
    public int countListedUserGroups() throws Exception {
        switchToTabByIndex(TabIndex.USER_GROUPS.getIndex());
        return getRowsOfTable(userGroupsTable).size();
    }

    /**
     * Count rows of clients table.
     *
     * @return The number of rows of the clients table.
     */
    public int countListedClients() throws Exception {
        switchToTabByIndex(TabIndex.CLIENTS.getIndex());
        return getRowsOfTable(clientsTable).size();
    }


    /**
     * Counts rows of ldap groups table.
     *
     * @return The number of rows of ldap groups table.
     */
    public int countListedLdapGroups() throws Exception {
        switchToTabByIndex(TabIndex.LDAP_GROUPS.getIndex());
        return getRowsOfTable(ldapGroupsTable).size();
    }

    /**
     * Goes to edit page for creating a new user.
     * 
     * @return The user edit page.
     */
    public UserEditPage createNewUser() throws Exception {
        if (isNotAt()) {
            goTo();
        }
        newElementButton.click();
        clickButtonAndWaitForRedirect(newUserButton, Pages.getUserEditPage().getUrl());
        return Pages.getUserEditPage();
    }

    /**
     * Goes to edit page for creating a new ldap group.
     *
     * @return The user edit page.
     */
    public LdapGroupEditPage createNewLdapGroup() throws Exception {
        if (isNotAt()) {
            goTo();
        }
        newElementButton.click();
        clickButtonAndWaitForRedirect(newLdapGroupButton, Pages.getLdapGroupEditPage().getUrl());
        return Pages.getLdapGroupEditPage();
    }

    /**
     * Returns a list of all ldap groups titles which were displayed on ldap group
     * page.
     *
     * @return The list of ldap group titles
     */
    public List<String> getLdapGroupNames() throws Exception {
        switchToTabByIndex(TabIndex.LDAP_GROUPS.getIndex());
        return getTableDataByColumn(ldapGroupsTable, 0);
    }

    /**
     * Clicks on the tab indicated by given index (starting with 0 for the first
     * tab).
     * 
     * @return The users page.
     */
    public UsersPage switchToTabByIndex(int index) throws Exception {
        return switchToTabByIndex(index, usersTabView);
    }

    /**
     * Goes to edit page for creating a new user group.
     *
     * @return The user group edit page.
     */
    public UserGroupEditPage createNewUserGroup() throws Exception {
        if (isNotAt()) {
            goTo();
        }
        newElementButton.click();
        clickButtonAndWaitForRedirect(newUserGroupButton, Pages.getUserGroupEditPage().getUrl());
        return Pages.getUserGroupEditPage();
    }

    /**
     * Goes to edit page for editing a given user group.
     * 
     * @param userGroup
     *            The user group.
     * @return The user group edit page.
     */
    public UserGroupEditPage editUserGroup(UserGroup userGroup) throws Exception {
        switchToTabByIndex(TabIndex.USER_GROUPS.getIndex());

        WebElement userGroupEditLink = Browser.getDriver()
                .findElementByXPath("//a[@href='/kitodo/pages/usergroupEdit.jsf?id=" + userGroup.getId() + "']");
        userGroupEditLink.click();
        return Pages.getUserGroupEditPage();
    }

    /**
     * Goes to edit page for editing a given user group, specified by title.
     *
     * @param userGroupTitle
     *            The user group title.
     * @return The user group edit page.
     */
    public UserGroupEditPage editUserGroup(String userGroupTitle) throws Exception {
        switchToTabByIndex(TabIndex.USER_GROUPS.getIndex());

        List<WebElement> tableRows = getRowsOfTable(userGroupsTable);

        for (WebElement tableRow : tableRows) {
            if (Browser.getCellDataByRow(tableRow, 0).equals(userGroupTitle)) {
                clickEditLinkOfTableRow(tableRow);
                return Pages.getUserGroupEditPage();
            }
        }
        throw new NoSuchElementException("No user group with given title was not found: " + userGroupTitle);
    }

    /**
     * Goes to edit page for editing a given ldap group, specified by title.
     *
     * @param ldapGroupTitle
     *            The ldap group title.
     * @return The ldap group edit page.
     */
    public LdapGroupEditPage editLdapGroup(String ldapGroupTitle) throws Exception {
        switchToTabByIndex(TabIndex.LDAP_GROUPS.getIndex());

        List<WebElement> tableRows = getRowsOfTable(ldapGroupsTable);

        for (WebElement tableRow : tableRows) {
            if (Browser.getCellDataByRow(tableRow, 0).equals(ldapGroupTitle)) {
                clickEditLinkOfTableRow(tableRow);
                return Pages.getLdapGroupEditPage();
            }
        }
        throw new NoSuchElementException("No ldap group with given title was found: " + ldapGroupTitle);
    }

    private void clickEditLinkOfTableRow(WebElement tableRow) throws Exception {
        WebElement ldapGroupEditLink = tableRow.findElement(By.tagName("a"));
        ldapGroupEditLink.click();
        Thread.sleep(Browser.getDelayMinAfterLinkClick());
    }

    /**
     * Returns a list of all user group titles which were displayed on user groups
     * page.
     * 
     * @return The list of user group titles
     */
    public List<String> getUserGroupTitles() throws Exception {
        switchToTabByIndex(TabIndex.USER_GROUPS.getIndex());
        return getTableDataByColumn(userGroupsTable, 0);
    }

    /**
     * Returns a list of all client titles which were displayed on clients tab.
     *
     * @return The list of client titles
     */
    public List<String> getClientNames() throws Exception {
        switchToTabByIndex(TabIndex.CLIENTS.getIndex());
        return getTableDataByColumn(clientsTable, 0);
    }

    /**
     * Goes to edit page for creating a new client.
     *
     * @return The client edit page.
     */
    public ClientEditPage createNewClient() throws Exception {
        if (isNotAt()) {
            goTo();
        }
        newElementButton.click();
        clickButtonAndWaitForRedirect(newClientButton, Pages.getClientEditPage().getUrl());
        return Pages.getClientEditPage();
    }

    /**
     * Remove user from corresponding list on user page.
     */
    public void deleteRemovableUser() throws Exception {
        deleteElement("User",
                MockDatabase.getRemovableObjectIDs().get(ObjectType.USER.name()),
                TabIndex.USERS.getIndex(),
                usersTabView);
    }

    /**
     * Remove user group from corresponding list on user page.
     */
    public void deleteRemovableUserGroup() throws Exception {
        deleteElement("Usergroup",
                MockDatabase.getRemovableObjectIDs().get(ObjectType.USERGROUP.name()),
                TabIndex.USER_GROUPS.getIndex(),
                usersTabView);
    }

    /**
     * Remove client from corresponding list on user page.
     */
    public void deleteRemovableClient() throws Exception {
        deleteElement("Client",
                MockDatabase.getRemovableObjectIDs().get(ObjectType.CLIENT.name()),
                TabIndex.CLIENTS.getIndex(),
                usersTabView);
    }
}

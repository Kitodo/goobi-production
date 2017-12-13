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

package org.kitodo.data.database.beans;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "authorization")
public class Authorization extends BaseIndexedBean {

    private static final long serialVersionUID = -5187947220333987498L;

    @Column(name = "title")
    private String title;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "userGroup_x_authorization",
        joinColumns = {
            @JoinColumn(name = "userGroup_id", foreignKey = @ForeignKey(name = "FK_userGroup_x_authorization_userGroup_id")) },
        inverseJoinColumns = {
            @JoinColumn(name = "authorization_id", foreignKey = @ForeignKey(name = "FK_userGroup_x_authorization_authorization_id")) })
    private List<UserGroup> userGroups;

    /**
     * Gets the title.
     *
     * @return
     *      The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title
     *      The titel.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets all user groups in which this authorization is used.
     *
     * @return
     *      The user groups.
     */
    public List<UserGroup> getUserGroups() {
        if (this.userGroups == null) {
            this.userGroups = new ArrayList<>();
        }
        return userGroups;
    }

    /**
     * Sets the user groups.
     *
     * @param userGroups
     *      The user groups.
     */
    public void setUserGroups(List<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }
}

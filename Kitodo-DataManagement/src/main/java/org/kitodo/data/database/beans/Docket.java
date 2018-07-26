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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "docket")
public class Docket extends BaseIndexedBean {

    private static final long serialVersionUID = -5187947220333984868L;

    @Column(name = "title")
    private String title;

    @Column(name = "file")
    private String file;

    @Column(name = "active")
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "client_id", foreignKey = @ForeignKey(name = "FK_docket_client_id"))
    private Client client;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Check if docket is active.
     *
     * @return true or false
     */
    public Boolean isActive() {
        if (Objects.isNull(this.active)) {
            this.active = true;
        }
        return this.active;
    }

    /**
     * Set docket as active.
     *
     * @param active as boolean
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get client.
     *
     * @return Client object
     */
    public Client getClient() {
        return this.client;
    }

    /**
     * Set client.
     *
     * @param client
     *            as Client object
     */
    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Docket docket = (Docket) o;
        return Objects.equals(title, docket.title)
            && Objects.equals(file, docket.file)
            && Objects.equals(active, docket.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, file, active);
    }
}

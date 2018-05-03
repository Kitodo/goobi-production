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
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "workflow")
public class Workflow extends BaseBean {
    private static final long serialVersionUID = 6831844584235763486L;

    @Column(name = "title")
    private String title;

    @Column(name = "fileName")
    private String fileName;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Template> templates;

    /**
     * Public constructor.
     * 
     * @param title
     *            of workflow
     * @param fileName
     *            in which diagram is stored
     */
    public Workflow(String title, String fileName) {
        this.title = title;
        this.fileName = fileName;
    }

    /**
     * Get title.
     *
     * @return value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title.
     *
     * @param title
     *            as String
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get file name of file in which diagram workflow is stored.
     *
     * @return value of file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set file name of file in which diagram workflow is stored.
     *
     * @param fileName
     *            as String
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get list of template assigned to this workflow.
     *
     * @return list of template assigned to this workflow
     */
    public List<Template> getTemplates() {
        if (Objects.isNull(this.templates)) {
            this.templates = new ArrayList<>();
        }
        return templates;
    }

    /**
     * Set list of template assigned to this workflow.
     *
     * @param templates
     *            list of template assigned to this workflow
     */
    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }
}

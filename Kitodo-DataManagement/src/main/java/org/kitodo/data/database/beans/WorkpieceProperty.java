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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.kitodo.data.database.beans.property.KitodoPropertyInterface;
import org.kitodo.data.database.helper.enums.PropertyType;

@Entity
@Table(name = "workpieceProperty")
public class WorkpieceProperty implements Serializable, KitodoPropertyInterface {
    private static final long serialVersionUID = -88407008893258729L;

    @Id
    @Column(name = "id")
    @GeneratedValue
    private Integer id;

    @Column(name = "title")
    private String title;

    @Column(name = "value", columnDefinition = "longtext")
    private String value;

    @Column(name = "obligatory")
    private Boolean obligatory;

    @Column(name = "dataType")
    private Integer dataType;

    @Column(name = "choice")
    private String choice;

    @Column(name = "creationDate")
    private Date creationDate;

    @Column(name = "container")
    private Integer container;

    @ManyToOne
    @JoinColumn(name = "workpiece_id", foreignKey = @ForeignKey(name = "FK_workpieceProperty_workpiece_id"))
    private Workpiece workpiece;

    @Transient
    private List<String> valueList;

    /**
     * Constructor.
     */
    public WorkpieceProperty() {
        this.obligatory = false;
        this.dataType = PropertyType.String.getId();
        this.creationDate = new Date();
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getChoice() {
        return this.choice;
    }

    @Override
    public void setChoice(String choice) {
        this.choice = choice;
    }

    @Override
    public Boolean isObligatory() {
        if (this.obligatory == null) {
            this.obligatory = false;
        }
        return this.obligatory;
    }

    @Override
    public void setObligatory(Boolean obligatory) {
        this.obligatory = obligatory;
    }

    @Override
    public Date getCreationDate() {
        return this.creationDate;
    }

    @Override
    public void setCreationDate(Date creation) {
        this.creationDate = creation;
    }

    /**
     * Getter for data type set to private for hibernate, for use in program use
     * getType instead.
     *
     * @return dataType as integer
     */
    @SuppressWarnings("unused")
    private Integer getDataType() {
        return this.dataType;
    }

    /**
     * Set data type to defined integer. only for internal use through
     * hibernate, for changing data type use setType instead.
     *
     * @param dataType
     *            as Integer
     */
    @SuppressWarnings("unused")
    private void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    /**
     * Set data type to specific value from {@link PropertyType}.
     *
     * @param inputType
     *            as {@link PropertyType}
     */
    @Override
    public void setType(PropertyType inputType) {
        this.dataType = inputType.getId();
    }

    /**
     * Get data type as {@link PropertyType}.
     *
     * @return current data type
     */
    @Override
    public PropertyType getType() {
        if (this.dataType == null) {
            this.dataType = PropertyType.String.getId();
        }
        return PropertyType.getById(this.dataType);
    }

    public List<String> getValueList() {
        if (this.valueList == null) {
            this.valueList = new ArrayList<String>();
        }
        return this.valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    @Override
    public Integer getContainer() {
        if (this.container == null) {
            return 0;
        }
        return this.container;
    }

    @Override
    public void setContainer(Integer order) {
        if (order == null) {
            order = 0;
        }
        this.container = order;
    }

    public Workpiece getWorkpiece() {
        return this.workpiece;
    }

    public void setWorkpiece(Workpiece workpiece) {
        this.workpiece = workpiece;
    }
}

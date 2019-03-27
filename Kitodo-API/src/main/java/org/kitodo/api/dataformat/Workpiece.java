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

package org.kitodo.api.dataformat;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

/**
 * The administrative structure of the product of an element that passes through
 * a Production workflow.
 */
public class Workpiece {
    /**
     * The time this file was first created.
     */
    private GregorianCalendar creationDate = new GregorianCalendar();

    /**
     * The processing history.
     */
    private List<ProcessingNote> editHistory = new ArrayList<>();

    /**
     * The identifier of the workpiece.
     */
    private String id;

    /**
     * The media unit that belongs to this workpiece. The media unit can have
     * children, such as a bound book that can have pages.
     */
    private MediaUnit mediaUnit = new MediaUnit();

    /**
     * The logical structure.
     */
    private Structure structure = new Structure();

    /**
     * The list of parent linked structures. The list is to be read from top to
     * bottom, i.e. the element 0 of the list is the topmost structure.
     */
    private Uplinks uplinks = new Uplinks();

    /**
     * Returns the creation date of the workpiece.
     *
     * @return the creation date
     */
    public GregorianCalendar getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creation date of the workpiece.
     *
     * @param creationDate
     *            creation date to set
     */
    public void setCreationDate(GregorianCalendar creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the edit history.
     *
     * @return the edit history
     */
    public List<ProcessingNote> getEditHistory() {
        return editHistory;
    }

    /**
     * Returns the ID of the workpiece.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the workpiece.
     *
     * @param id
     *            ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the media unit of this workpiece.
     *
     * @return the media units
     */
    public MediaUnit getMediaUnit() {
        return mediaUnit;
    }

    /**
     * Returns the media units of this workpiece.
     *
     * @return the media units
     * @deprecated Use {@code getMediaUnit().getChildren()}.
     */
    @Deprecated
    public List<MediaUnit> getMediaUnits() {
        return mediaUnit.getChildren();
    }

    /**
     * Returns the root element of the structure.
     *
     * @return root element of the structure
     */
    public Structure getStructure() {
        return structure;
    }

    /**
     * Sets the media unit of the workpiece.
     *
     * @param mediaUnit
     *            media unit to set
     */
    public void setMediaUnit(MediaUnit mediaUnit) {
        this.mediaUnit = mediaUnit;
    }

    /**
     * Sets the structure of the workpiece.
     *
     * @param structure
     *            structure to set
     */
    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    /**
     * Returns the uplinks of this workpiece.
     *
     * @return the uplinks
     */
    public Uplinks getUplinks() {
        return uplinks;
    }

    @Override
    public String toString() {
        return id + ", " + structure;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Workpiece) {
            Workpiece other = (Workpiece) obj;

            if (Objects.isNull(id)) {
                return Objects.isNull(other.id);
            } else {
                return id.equals(other.id);
            }
        }
        return false;
    }
}

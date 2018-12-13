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
package org.kitodo.production.lugh.pagination;

/**
 * A static piece of text as part of a pagination sequence. The text may either
 * appear on the front, back, or both sides of a sheet.
 *
 * @author Matthias Ronge
 */
public class StaticText implements Fragment {

    private HalfInteger increment;
    private Boolean page; // true: odd (left) page, false: even (right) page,
                          // null: any page
    private String value;

    /**
     * Creates a static text that is used on odd or even pages only.
     *
     * @param value
     *            text string
     * @param odd
     *            if true, the text is printed for odd pages only, else for even
     *            pages only, null for both pages
     */
    StaticText(String value, Boolean odd) {
        this.value = value;
        this.page = odd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String format(HalfInteger value) {
        if (page == null || page == value.isHalf())
            return this.value;
        else
            return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HalfInteger getIncrement() {
        return increment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer intValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIncrement(HalfInteger increment) {
        this.increment = increment;

    }

    /**
     * Returns a concise string representation of this instance.
     *
     * @return a string representing this instance
     */
    @Override
    public String toString() {
        return '"' + value + "\" (" + (increment != null ? increment : "default") + (page != null ? ", " + page : "") + ")";
    }

}

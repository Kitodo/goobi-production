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

package org.kitodo.forms;

import org.kitodo.data.database.beans.SubfolderType;

/**
 * An encapsulation to access the generator properties of the folder.
 */
public class FolderGenerator {
    /**
     * Generator method that changes the DPI of an image.
     */
    private static final String CHANGE_DPI = "changeDpi";

    /**
     * Generator method that creates a derivative for an image.
     */
    private static final String CREATE_DERIVATIVE = "createDerivative";

    /**
     * Generator method that scales an image.
     */
    private static final String GET_SCALED_WEB_IMAGE = "getScaledWebImage";

    /**
     * Generator method that changes the size (in pixel) of the image.
     */
    private static final String GET_SIZED_WEB_IMAGE = "getSizedWebImage";

    /**
     * Multiplier used to display percentage.
     */
    private static final double PERCENT = 100.0d;

    /**
     * Image resoulution in DPI.
     */
    private int dpi = 300;

    /**
     * Scale factor.
     */
    private double factor = 1.00d;

    /**
     * {@code Folder.this}.
     */
    private final SubfolderType subfolderType;

    /**
     * Image width in pixels.
     */
    private int width = 150;

    /**
     * Creates a new generator for this folder.
     *
     * @param subfolderType
     *            {@code Folder.this}
     */
    public FolderGenerator(SubfolderType subfolderType) {
        this.subfolderType = subfolderType;
    }

    /**
     * Returns the image resolution in DPI.
     *
     * @return the DPI
     */
    public int getDpi() {
        return dpi;
    }

    /**
     * Returns the scale factor to set.
     *
     * @return the scale factor
     */
    public double getFactor() {
        return factor * PERCENT;
    }

    /**
     * Returns the selected generator method.
     *
     * @return the generator methodd
     */
    public String getMethod() {
        if (subfolderType.getDerivative().isPresent()) {
            return CREATE_DERIVATIVE;
        } else if (subfolderType.getDpi().isPresent()) {
            return CHANGE_DPI;
        } else if (subfolderType.getImageScale().isPresent()) {
            return GET_SCALED_WEB_IMAGE;
        } else if (subfolderType.getImageSize().isPresent()) {
            return GET_SIZED_WEB_IMAGE;
        } else {
            return "";
        }
    }

    /**
     * Sets the image width in pixels.
     *
     * @return the image width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the image resolution in DPI.
     *
     * @param dpi
     *            resolution to set
     */
    public void setDpi(int dpi) {
        this.dpi = dpi;
        setMethod(getMethod());
    }

    /**
     * Sets the scale factor.
     *
     * @param factor
     *            scale factor to set
     */
    public void setFactor(double factor) {
        this.factor = factor / PERCENT;
        setMethod(getMethod());
    }

    /**
     * Sets the generator method.
     *
     * @param method
     *            method to set
     */
    public void setMethod(String method) {
        subfolderType.setDerivative(method.equals(CREATE_DERIVATIVE) ? factor : null);
        subfolderType.setDpi(method.equals(CHANGE_DPI) ? dpi : null);
        subfolderType.setImageScale(method.equals(GET_SCALED_WEB_IMAGE) ? factor : null);
        subfolderType.setImageSize(method.equals(GET_SIZED_WEB_IMAGE) ? width : null);
    }

    /**
     * Sets the image width in pixels.
     *
     * @param width
     *            image width
     */
    public void setWidth(int width) {
        this.width = width;
        setMethod(getMethod());
    }
}

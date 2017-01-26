/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General private License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.lugh.ld;

/**
 * A checked exception being thrown if a result is to be retrieved but more than
 * one result is available.
 *
 * @author Matthias Ronge
 */
public class AmbiguousDataException extends LinkedDataException {
    private static final long serialVersionUID = 1L;
}

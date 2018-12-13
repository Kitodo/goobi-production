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

package org.kitodo.legacy;

import org.kitodo.api.ugh.FactoryInterface;
import org.kitodo.legacy.joining.ReplacementFactory;

public class UghImplementation {
    public static final FactoryInterface INSTANCE = new ReplacementFactory();

    /**
     * Private constructor to hide the implicit public one.
     */
    private UghImplementation() {

    }
}

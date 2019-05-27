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

package org.kitodo.production.helper;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.kitodo.production.enums.SortType;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyLogicalDocStructTypeHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataTypeHelper;
import org.kitodo.production.services.ServiceManager;

public class HelperComparator implements Comparator<Object>, Serializable {

    private SortType sortType;

    @Override
    public int compare(Object firstObject, Object secondObject) {
        int result = 0;

        switch (sortType) {
            case DOC_STRUCT_TYPE:
                result = compareDocStructTypes(firstObject, secondObject);
                break;
            case METADATA:
                throw new UnsupportedOperationException("Dead code pending removal");
            case METADATA_TYPE:
                result = compareMetadataTypes(firstObject, secondObject);
                break;
            default:
                break;
        }

        return result;
    }

    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    private int compareMetadataTypes(Object firstObject, Object secondObject) {
        LegacyMetadataTypeHelper firstMetadata = (LegacyMetadataTypeHelper) firstObject;
        LegacyMetadataTypeHelper secondMetadata = (LegacyMetadataTypeHelper) secondObject;

        String language = ServiceManager.getUserService().getAuthenticatedUser().getMetadataLanguage();

        String firstName = firstMetadata.getLanguage(language);
        String secondName = secondMetadata.getLanguage(language);

        return compareString(firstName, secondName);
    }

    private int compareDocStructTypes(Object firstObject, Object secondObject) {
        LegacyLogicalDocStructTypeHelper firstDocStructType = (LegacyLogicalDocStructTypeHelper) firstObject;
        LegacyLogicalDocStructTypeHelper secondDocStructType = (LegacyLogicalDocStructTypeHelper) secondObject;

        String language = ServiceManager.getUserService().getAuthenticatedUser().getMetadataLanguage();

        String firstName = firstDocStructType.getNameByLanguage(language);
        String secondName = secondDocStructType.getNameByLanguage(language);

        return compareString(firstName, secondName);
    }

    private int compareString(String firstName, String secondName) {
        if (Objects.isNull(firstName)) {
            firstName = "";
        }
        if (Objects.isNull(secondName)) {
            secondName = "";
        }
        return firstName.compareToIgnoreCase(secondName);
    }

}

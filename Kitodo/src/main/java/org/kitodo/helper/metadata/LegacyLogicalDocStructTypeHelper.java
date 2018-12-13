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

package org.kitodo.helper.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.dataeditor.rulesetmanagement.StructuralElementViewInterface;
import org.kitodo.api.ugh.DocStructTypeInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;

/**
 * Connects a legacy doc struct type from the logical map to a division view.
 * This is a soldering class to keep legacy code operational which is about to
 * be removed. Do not use this class.
 */
public class LegacyLogicalDocStructTypeHelper implements DocStructTypeInterface {
    private static final Logger logger = LogManager.getLogger(LegacyLogicalDocStructTypeHelper.class);

    private StructuralElementViewInterface divisionView;

    public LegacyLogicalDocStructTypeHelper(StructuralElementViewInterface divisionView) {
        this.divisionView = divisionView;
    }

    @Override
    public List<String> getAllAllowedDocStructTypes() {
        return new ArrayList<>(divisionView.getAllowedSubstructuralElements().keySet());
    }

    @Override
    public List<MetadataTypeInterface> getAllMetadataTypes() {
        throw andLog(new UnsupportedOperationException("Not yet implemented"));
    }

    @Override
    public String getAnchorClass() {
        return null; // muss null sein = keine
    }

    @Override
    public String getName() {
        return divisionView.getId();
    }

    @Override
    public String getNameByLanguage(String language) {
        return divisionView.getLabel();
    }

    @Override
    public String getNumberOfMetadataType(MetadataTypeInterface metadataType) {
        throw andLog(new UnsupportedOperationException("Not yet implemented"));
    }

    /**
     * This method generates a comprehensible log message in case something was
     * overlooked and one of the unimplemented methods should ever be called in
     * operation. The name was chosen deliberately short in order to keep the
     * calling code clear. This method must be implemented in every class
     * because it uses the logger tailored to the class.
     * 
     * @param exception
     *            created {@code UnsupportedOperationException}
     * @return the exception
     */
    private static RuntimeException andLog(UnsupportedOperationException exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StringBuilder buffer = new StringBuilder(255);
        buffer.append(stackTrace[1].getClassName());
        buffer.append('.');
        buffer.append(stackTrace[1].getMethodName());
        if (stackTrace[1].getLineNumber() > -1) {
            buffer.append(" line ");
            buffer.append(stackTrace[1].getLineNumber());
        }
        buffer.append(" unexpectedly called unimplemented ");
        buffer.append(stackTrace[0].getMethodName());
        if (exception.getMessage() != null) {
            buffer.append(": ");
            buffer.append(exception.getMessage());
        }
        logger.error(buffer.toString());
        return exception;
    }
}

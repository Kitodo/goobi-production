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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyDocStructHelperInterface;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetsModsDigitalDocumentHelper;
import org.kitodo.production.services.ServiceManager;

public class XmlArticleCounter {

    private static final Logger logger = LogManager.getLogger(XmlArticleCounter.class);

    public enum CountType {
        METADATA,
        DOCSTRUCT
    }

    /**
     * Anzahl der Strukturelemente ermitteln.
     *
     * @param myProcess
     *            process object
     */
    public int getNumberOfUghElements(Process myProcess, CountType inType) {
        int result = 0;

        // read document
        LegacyMetsModsDigitalDocumentHelper gdzfile;
        try {
            gdzfile = ServiceManager.getProcessService().readMetadataFile(myProcess, false);
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage("xml error", logger, e);
            return -1;
        }

        LegacyMetsModsDigitalDocumentHelper document = gdzfile.getDigitalDocument();
        LegacyDocStructHelperInterface logicalTopstruct = document.getLogicalDocStruct();
        result += getNumberOfUghElements(logicalTopstruct, inType);

        // save the determined number in the process
        myProcess.setSortHelperArticles(result);
        try {
            ServiceManager.getProcessService().save(myProcess);
        } catch (DataException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Anzahl der Strukturelemente oder der Metadaten ermitteln, die ein Band
     * hat, rekursiv durchlaufen.
     *
     * @param inStruct
     *            DocStruct object
     * @param inType
     *            CountType object
     */
    public int getNumberOfUghElements(LegacyDocStructHelperInterface inStruct, CountType inType) {
        int result = 0;
        if (Objects.nonNull(inStruct)) {
            /*
             * increment number of docstructs, or add number of metadata
             * elements
             */
            if (inType == CountType.DOCSTRUCT) {
                result++;
            } else {
                result += countNonEmptyMetadata(inStruct);
            }

            // call children recursive
            if (Objects.nonNull(inStruct.getAllChildren())) {
                for (LegacyDocStructHelperInterface struct : inStruct.getAllChildren()) {
                    result += getNumberOfUghElements(struct, inType);
                }
            }
        }
        return result;
    }

    private int countNonEmptyMetadata(LegacyDocStructHelperInterface inStruct) {
        int result = 0;
        List<LegacyMetadataHelper> allMetadata = inStruct.getAllMetadata();
        if (Objects.nonNull(allMetadata)) {
            for (LegacyMetadataHelper md : allMetadata) {
                String value = md.getValue();
                if (Objects.nonNull(value) && !value.trim().isEmpty()) {
                    result++;
                }
            }
        }
        return result;
    }

}

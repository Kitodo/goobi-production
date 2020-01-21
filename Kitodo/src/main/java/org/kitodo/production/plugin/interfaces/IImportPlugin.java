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

package org.kitodo.production.plugin.interfaces;

import java.io.File;
import java.util.List;

import org.kitodo.exceptions.ImportPluginException;
import org.kitodo.production.enums.ImportType;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetsModsDigitalDocumentHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyPrefsHelper;
import org.kitodo.production.importer.DocstructElement;
import org.kitodo.production.importer.ImportObject;
import org.kitodo.production.importer.Record;
import org.kitodo.production.properties.ImportProperty;

public interface IImportPlugin extends IPlugin {

    void setPrefs(LegacyPrefsHelper prefs);

    void setData(Record r);

    LegacyMetsModsDigitalDocumentHelper convertData() throws ImportPluginException;

    String getImportFolder();

    String getProcessTitle();

    List<ImportObject> generateFiles(List<Record> records);

    void setImportFolder(String folder);

    List<Record> splitRecords(String records);

    List<Record> generateRecordsFromFile();

    List<Record> generateRecordsFromFilenames(List<String> filenames);

    void setFile(File importFile);

    List<String> splitIds(String ids);

    List<ImportType> getImportTypes();

    List<ImportProperty> getProperties();

    List<String> getAllFilenames();

    void deleteFiles(List<String> selectedFilenames);

    List<? extends DocstructElement> getCurrentDocStructs();

    String deleteDocstruct();

    String addDocstruct();

    List<String> getPossibleDocstructs();

    DocstructElement getDocstruct();

    void setDocstruct(DocstructElement dse);

    void setOpacCatalogue(String opacCatalogue);

    void setKitodoConfigDirectory(String configDir);
}

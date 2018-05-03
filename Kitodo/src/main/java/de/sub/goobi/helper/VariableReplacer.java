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

package de.sub.goobi.helper;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.exceptions.UghHelperException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Task;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.ProcessService;
import org.kitodo.services.file.FileService;

public class VariableReplacer {

    private enum MetadataLevel {
        ALL,
        FIRSTCHILD,
        TOPSTRUCT
    }

    private static final Logger logger = LogManager.getLogger(VariableReplacer.class);

    private DigitalDocumentInterface dd;
    private PrefsInterface prefs;
    // $(meta.abc)
    private final String namespaceMeta = "\\$\\(meta\\.([\\w.-]*)\\)";

    private Process process;
    private Task task;
    private final ServiceManager serviceManager = new ServiceManager();
    private final FileService fileService = serviceManager.getFileService();
    private final ProcessService processService = serviceManager.getProcessService();

    @SuppressWarnings("unused")
    private VariableReplacer() {
    }

    VariableReplacer(Process process) {
        this.process = process;
    }

    /**
     * Constructor.
     *
     * @param inDigitalDocument
     *            DigitalDocument object
     * @param inPrefs
     *            Prefs object
     * @param p
     *            Process object
     * @param s
     *            Task object
     */
    public VariableReplacer(DigitalDocumentInterface inDigitalDocument, PrefsInterface inPrefs, Process p, Task s) {
        this.dd = inDigitalDocument;
        this.prefs = inPrefs;
        this.process = p;
        this.task = s;
    }

    /**
     * Variablen innerhalb eines Strings ersetzen. Dabei vergleichbar zu Ant die
     * Variablen durchlaufen und aus dem Digital Document holen
     *
     * @param inString
     *            to replacement
     * @return replaced String
     */
    public String replace(String inString) {
        if (inString == null) {
            return "";
        }

        inString = replaceMetadata(inString);

        // replace paths and files
        try {
            // TIFF writer scripts will have a path without an end slash
            String processPath = replaceSlashAndSeparator(processService.getProcessDataDirectory(this.process));
            String tifPath = replaceSlashAndSeparator(processService.getImagesTifDirectory(false, this.process));
            String imagePath = replaceSlashAndSeparator(fileService.getImagesDirectory(this.process));
            String origPath = replaceSlashAndSeparator(processService.getImagesOrigDirectory(false, this.process));
            String metaFile = replaceSlash(fileService.getMetadataFilePath(this.process));
            String ocrBasisPath = replaceSlashAndSeparator(fileService.getOcrDirectory(this.process));
            String ocrPlaintextPath = replaceSlashAndSeparator(fileService.getTxtDirectory(this.process));
            String sourcePath = replaceSlashAndSeparator(fileService.getSourceDirectory(this.process));
            String importPath = replaceSlashAndSeparator(fileService.getImportDirectory(this.process));
            String prefs = ConfigCore.getParameter("RegelsaetzeVerzeichnis") + this.process.getRuleset().getFile();

            inString = replaceStringAccordingToOS(inString, "(tifurl)", tifPath);
            inString = replaceStringAccordingToOS(inString, "(origurl)", origPath);
            inString = replaceStringAccordingToOS(inString, "(imageurl)", imagePath);

            inString = replaceString(inString, "(tifpath)", tifPath);
            inString = replaceString(inString, "(origpath)", origPath);
            inString = replaceString(inString, "(imagepath)", imagePath);
            inString = replaceString(inString, "(processpath)", processPath);
            inString = replaceString(inString, "(importpath)", importPath);
            inString = replaceString(inString, "(sourcepath)", sourcePath);
            inString = replaceString(inString, "(ocrbasispath)", ocrBasisPath);
            inString = replaceString(inString, "(ocrplaintextpath)", ocrPlaintextPath);
            inString = replaceString(inString, "(processtitle)", this.process.getTitle());
            inString = replaceString(inString, "(processid)", String.valueOf(this.process.getId().intValue()));
            inString = replaceString(inString, "(metaFile)", metaFile);
            inString = replaceString(inString, "(prefs)", prefs);

            inString = replaceStringForTask(inString);

            inString = replaceForWorkpieceProperty(inString);
            inString = replaceForTemplateProperty(inString);
            inString = replaceForProcessProperty(inString);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return inString;
    }

    /**
     * Replace metadata, usage: $(meta.firstchild.METADATANAME).
     *
     * @param input
     *            String for replacement
     * @return replaced String
     */
    private String replaceMetadata(String input) {
        for (MatchResult r : findRegexMatches(this.namespaceMeta, input)) {
            if (r.group(1).toLowerCase().startsWith("firstchild.")) {
                input = input.replace(r.group(),
                    getMetadataFromDigitalDocument(MetadataLevel.FIRSTCHILD, r.group(1).substring(11)));
            } else if (r.group(1).toLowerCase().startsWith("topstruct.")) {
                input = input.replace(r.group(),
                    getMetadataFromDigitalDocument(MetadataLevel.TOPSTRUCT, r.group(1).substring(10)));
            } else {
                input = input.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.ALL, r.group(1)));
            }
        }

        return input;
    }

    private String replaceSlash(URI directory) {
        return fileService.getFileName(directory).replace("\\", "/");
    }

    private String replaceSeparator(String input) {
        if (input.endsWith(File.separator)) {
            input = input.substring(0, input.length() - File.separator.length()).replace("\\", "/");
        }
        return input;
    }

    private String replaceSlashAndSeparator(URI directory) {
        return replaceSeparator(replaceSlash(directory));
    }

    private String replaceStringAccordingToOS(String input, String condition, String replacer) {
        if (input.contains(condition)) {
            if (SystemUtils.IS_OS_WINDOWS) {
                input = input.replace(condition, "file:/" + replacer);
            } else {
                input = input.replace(condition, "file://" + replacer);
            }
        }
        return input;
    }

    private String replaceString(String input, String condition, String replacer) {
        if (input.contains(condition)) {
            input = input.replace(condition, replacer);
        }
        return input;
    }

    private String replaceStringForTask(String input) {
        if (this.task != null) {
            String taskId = String.valueOf(this.task.getId());
            String taskName = this.task.getTitle();

            input = input.replace("(stepid)", taskId);
            input = input.replace("(stepname)", taskName);
        }
        return input;
    }

    /**
     * Replace WerkstueckEigenschaft, usage: (product.PROPERTYTITLE).
     *
     * @param input
     *            String for replacement
     * @return replaced String
     */
    private String replaceForWorkpieceProperty(String input) {
        for (MatchResult r : findRegexMatches("\\(product\\.([\\w.-]*)\\)", input)) {
            String propertyTitle = r.group(1);
            for (Property workpieceProperty : this.process.getWorkpieces()) {
                if (workpieceProperty.getTitle().equalsIgnoreCase(propertyTitle)) {
                    input = input.replace(r.group(), workpieceProperty.getValue());
                    break;
                }
            }
        }
        return input;
    }

    /**
     * Replace Vorlageeigenschaft, usage: (template.PROPERTYTITLE).
     *
     * @param input
     *            String for replacement
     * @return replaced String
     */
    private String replaceForTemplateProperty(String input) {
        for (MatchResult r : findRegexMatches("\\(template\\.([\\w.-]*)\\)", input)) {
            String propertyTitle = r.group(1);
            for (Property templateProperty : this.process.getTemplates()) {
                if (templateProperty.getTitle().equalsIgnoreCase(propertyTitle)) {
                    input = input.replace(r.group(), templateProperty.getValue());
                    break;
                }
            }
        }
        return input;
    }

    /**
     * Replace Prozesseigenschaft, usage: (process.PROPERTYTITLE).
     *
     * @param input
     *            String for replacement
     * @return replaced String
     */
    private String replaceForProcessProperty(String input) {
        for (MatchResult r : findRegexMatches("\\(process\\.([\\w.-]*)\\)", input)) {
            String propertyTitle = r.group(1);
            List<Property> ppList = this.process.getProperties();
            for (Property pe : ppList) {
                if (pe.getTitle().equalsIgnoreCase(propertyTitle)) {
                    input = input.replace(r.group(), pe.getValue());
                    break;
                }
            }
        }
        return input;
    }

    /**
     * Metadatum von FirstChild oder TopStruct ermitteln (vorzugsweise vom
     * FirstChild) und zurückgeben.
     */
    private String getMetadataFromDigitalDocument(MetadataLevel inLevel, String metadata) {
        if (this.dd != null) {
            /* TopStruct und FirstChild ermitteln */
            DocStructInterface topstruct = this.dd.getLogicalDocStruct();
            DocStructInterface firstchildstruct = null;
            if (topstruct.getAllChildren() != null && topstruct.getAllChildren().size() > 0) {
                firstchildstruct = topstruct.getAllChildren().get(0);
            }

            /* MetadataType ermitteln und ggf. Fehler melden */
            MetadataTypeInterface mdt;
            try {
                mdt = UghHelper.getMetadataType(this.prefs, metadata);
            } catch (UghHelperException e) {
                Helper.setErrorMessage(e.toString(), logger, e);
                return "";
            }

            String result = "";
            String resultTop = getMetadataValue(topstruct, mdt);
            String resultFirst = null;
            if (firstchildstruct != null) {
                resultFirst = getMetadataValue(firstchildstruct, mdt);
            }

            switch (inLevel) {
                case FIRSTCHILD:
                    // without existing FirstChild, this can not be returned
                    if (resultFirst == null) {
                        logger.info("Can not replace firstChild-variable for METS: {}", metadata);
                        result = "";
                    } else {
                        result = resultFirst;
                    }
                    break;

                case TOPSTRUCT:
                    if (resultTop == null) {
                        result = "";
                        logger.warn("Can not replace topStruct-variable for METS: {}", metadata);
                    } else {
                        result = resultTop;
                    }
                    break;

                case ALL:
                    if (resultFirst != null) {
                        result = resultFirst;
                    } else if (resultTop != null) {
                        result = resultTop;
                    } else {
                        result = "";
                        logger.warn("Can not replace variable for METS: {}", metadata);
                    }
                    break;

                default:
                    break;
            }
            return result;
        } else {
            return "";
        }
    }

    /**
     * Metadatum von übergebenen Docstruct ermitteln, im Fehlerfall wird null
     * zurückgegeben.
     */
    private String getMetadataValue(DocStructInterface inDocstruct, MetadataTypeInterface mdt) {
        List<? extends MetadataInterface> mds = inDocstruct.getAllMetadataByType(mdt);
        if (mds.size() > 0) {
            return mds.get(0).getValue();
        } else {
            return null;
        }
    }

    /**
     * Suche nach regulären Ausdrücken in einem String, liefert alle gefundenen
     * Treffer als Liste zurück.
     */
    private static Iterable<MatchResult> findRegexMatches(String pattern, CharSequence s) {
        List<MatchResult> results = new ArrayList<>();
        for (Matcher m = Pattern.compile(pattern).matcher(s); m.find();) {
            results.add(m.toMatchResult());
        }
        return results;
    }
}

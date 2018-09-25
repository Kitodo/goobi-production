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

package org.kitodo.helper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Task;
import org.kitodo.exceptions.UghHelperException;
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
    private static final String NAMESPACE_META = "\\$\\(meta\\.([\\w.-]*)\\)";

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
            String prefs = ConfigCore.getParameter(ParameterCore.DIR_RULESETS) + this.process.getRuleset().getFile();

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
        for (MatchResult r : findRegexMatches(NAMESPACE_META, input)) {
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
            if (Objects.nonNull(topstruct.getAllChildren()) && !topstruct.getAllChildren().isEmpty()) {
                firstchildstruct = topstruct.getAllChildren().get(0);
            }

            /* MetadataType ermitteln und ggf. Fehler melden */
            MetadataTypeInterface mdt;
            try {
                mdt = UghHelper.getMetadataType(this.prefs, metadata);
            } catch (UghHelperException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                return "";
            }

            String resultTop = getMetadataValue(topstruct, mdt);
            String resultFirst = null;
            if (firstchildstruct != null) {
                resultFirst = getMetadataValue(firstchildstruct, mdt);
            }
            return getResultAccordingToMetadataLevel(inLevel, metadata, resultFirst, resultTop);
        } else {
            return "";
        }
    }

    private String getResultAccordingToMetadataLevel(MetadataLevel metadataLevel, String metadata, String resultFirst,
                                                     String resultTop) {
        String result = "";
        switch (metadataLevel) {
            case FIRSTCHILD:
                // without existing FirstChild, this can not be returned
                if (Objects.isNull(resultFirst)) {
                    logger.info("Can not replace firstChild-variable for METS: {}", metadata);
                } else {
                    result = resultFirst;
                }
                break;
            case TOPSTRUCT:
                if (Objects.isNull(resultTop)) {
                    logger.warn("Can not replace topStruct-variable for METS: {}", metadata);
                } else {
                    result = resultTop;
                }
                break;
            case ALL:
                if (Objects.nonNull(resultFirst)) {
                    result = resultFirst;
                } else if (Objects.nonNull(resultTop)) {
                    result = resultTop;
                } else {
                    logger.warn("Can not replace variable for METS: {}", metadata);
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Metadatum von übergebenen Docstruct ermitteln, im Fehlerfall wird null
     * zurückgegeben.
     */
    private String getMetadataValue(DocStructInterface inDocstruct, MetadataTypeInterface mdt) {
        List<? extends MetadataInterface> mds = inDocstruct.getAllMetadataByType(mdt);
        if (!mds.isEmpty()) {
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

    /**
     * Returns a map of the non-meta variables of this variable replacer.
     *
     * @return map of the non-meta variables
     * @throws IOException
     *             if I/O fails
     */
    public Map<String, String> mapOfVariables() throws IOException {
        Map<String, String> result = new HashMap<>();
        String processPath = replaceSlashAndSeparator(processService.getProcessDataDirectory(this.process));
        String tifPath = replaceSlashAndSeparator(processService.getImagesTifDirectory(false, this.process));
        String imagePath = replaceSlashAndSeparator(fileService.getImagesDirectory(this.process));
        String origPath = replaceSlashAndSeparator(processService.getImagesOrigDirectory(false, this.process));
        String metaFile = replaceSlash(fileService.getMetadataFilePath(this.process));
        String ocrBasisPath = replaceSlashAndSeparator(fileService.getOcrDirectory(this.process));
        String ocrPlaintextPath = replaceSlashAndSeparator(fileService.getTxtDirectory(this.process));
        String sourcePath = replaceSlashAndSeparator(fileService.getSourceDirectory(this.process));
        String importPath = replaceSlashAndSeparator(fileService.getImportDirectory(this.process));
        String prefs = ConfigCore.getParameter(ParameterCore.DIR_RULESETS) + this.process.getRuleset().getFile();

        result.put("tifurl", tifPath);
        result.put("origurl", origPath);
        result.put("imageurl", imagePath);

        result.put("tifpath", tifPath);
        result.put("origpath", origPath);
        result.put("imagepath", imagePath);
        result.put("processpath", processPath);
        result.put("importpath", importPath);
        result.put("sourcepath", sourcePath);
        result.put("ocrbasispath", ocrBasisPath);
        result.put("ocrplaintextpath", ocrPlaintextPath);
        result.put("processtitle", this.process.getTitle());
        result.put("processid", String.valueOf(this.process.getId().intValue()));
        result.put("metaFile", metaFile);
        result.put("prefs", prefs);

        if (this.task != null) {
            String taskId = String.valueOf(this.task.getId());
            String taskName = this.task.getTitle();

            result.put("stepid", taskId);
            result.put("stepname", taskName);
        }

        Pair<List<Property>, String> workpieceProperties = Pair.of(this.process.getWorkpieces(), "workpiece.");
        Pair<List<Property>, String> templateProperties = Pair.of(this.process.getTemplates(), "template.");
        Pair<List<Property>, String> processProperties = Pair.of(this.process.getProperties(), "process.");

        Stream<Pair<List<Property>, String>> propertyLists = Arrays
                .asList(workpieceProperties, templateProperties, processProperties).parallelStream();

        Stream<Pair<Property, String>> properties = propertyLists
                .flatMap(λ -> λ.getLeft().parallelStream().map(μ -> Pair.of(μ, λ.getRight())));

        Stream<Pair<String, String>> keyVariantsWithValues = properties.flatMap((propertyValuePair) -> {
            Stream<String> propertyNameVariants = toAnyCase(propertyValuePair.getLeft().getTitle()).parallelStream();
            Stream<String> keyVariants = propertyNameVariants.map(λ -> propertyValuePair.getRight().concat(λ));
            return keyVariants.map(λ -> Pair.of(λ, propertyValuePair.getValue()));
        });

        keyVariantsWithValues.forEach(λ -> result.put(λ.getKey(), λ.getValue()));

        return result;
    }

    /**
     * Converts a String to all of its upper/lowercase variants.
     *
     * @param string
     *            input string
     * @return a set with all variants of the string
     */
    public static Set<String> toAnyCase(String string) {
        if ((string == null) || (string.length() == 0)) {
            return new HashSet<String>(Arrays.asList(string));
        }
        Set<String> recursion = toAnyCase(string.substring(1));
        Set<String> result = new HashSet<String>((int) Math.ceil((2 * recursion.size()) / 0.75));
        String first = string.substring(0, 1);
        for (String s : recursion) {
            result.add(first.toUpperCase().concat(s));
            result.add(first.toLowerCase().concat(s));
        }
        return result;
    }
}

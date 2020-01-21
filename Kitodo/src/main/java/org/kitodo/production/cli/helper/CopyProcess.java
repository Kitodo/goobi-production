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

package org.kitodo.production.cli.helper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.exceptions.ProcessGenerationException;
import org.kitodo.production.forms.copyprocess.ProzesskopieForm;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyDocStructHelperInterface;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyLogicalDocStructHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetadataTypeHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyMetsModsDigitalDocumentHelper;
import org.kitodo.production.helper.metadata.legacytypeimplementations.LegacyPrefsHelper;
import org.kitodo.production.importer.ImportObject;
import org.kitodo.production.process.ProcessGenerator;
import org.kitodo.production.process.ProcessValidator;
import org.kitodo.production.process.field.AdditionalField;
import org.kitodo.production.search.opac.ConfigOpac;
import org.kitodo.production.search.opac.ConfigOpacDoctype;
import org.kitodo.production.services.ServiceManager;

public class CopyProcess extends ProzesskopieForm {

    private static final Logger logger = LogManager.getLogger(CopyProcess.class);
    private transient LegacyMetsModsDigitalDocumentHelper myRdf;
    private String opacSuchfeld = "12";
    private String opacSuchbegriff;
    private String opacKatalog;
    private URI metadataFile;
    private String naviFirstPage;
    private Process processForChoice;

    /**
     * Prepare import object.
     *
     * @param io
     *            import object
     * @return page or empty String
     */
    // TODO: why this not used ImportObject here?
    public String prepare(ImportObject io) {
        try {
            ServiceManager.getTemplateService().checkForUnreachableTasks(this.template.getTasks());
        } catch (ProcessGenerationException e) {
            logger.error(e.getMessage(), e);
            return "";
        }

        clearValues();
        readPreferences();
        this.prozessKopie = new Process();
        this.prozessKopie.setTitle("");
        this.prozessKopie.setProject(this.project);
        this.prozessKopie.setRuleset(this.template.getRuleset());
        this.prozessKopie.setDocket(this.template.getDocket());
        this.digitalCollections = new ArrayList<>();

        ProcessGenerator.copyTasks(this.template, this.prozessKopie);

        return this.naviFirstPage;
    }

    @Override
    public String prepare(int templateId, int projectId) {
        ProcessGenerator processGenerator = new ProcessGenerator();
        try {
            boolean generated = processGenerator.generateProcess(templateId, projectId);

            if (generated) {
                this.prozessKopie = processGenerator.getGeneratedProcess();
                this.project = processGenerator.getProject();
                this.template = processGenerator.getTemplate();

                clearValues();
                readPreferences();
                this.digitalCollections = new ArrayList<>();
                initializePossibleDigitalCollections();

                return this.naviFirstPage;
            }
        } catch (ProcessGenerationException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return null;
    }

    /**
     * OpacAnfrage.
     */
    @Override
    public void evaluateOpac() {
        clearValues();
        readProjectConfigs();
        try {
            LegacyPrefsHelper myPrefs = ServiceManager.getRulesetService().getPreferences(this.template.getRuleset());
            /* den Opac abfragen und ein RDF draus bauen lassen */
            this.myRdf = new LegacyMetsModsDigitalDocumentHelper(myPrefs.getRuleset());
            this.myRdf.read(this.metadataFile.getPath());
            this.docType = this.myRdf.getDigitalDocument().getLogicalDocStruct().getDocStructType().getName();

            fillFieldsFromMetadataFile(this.myRdf);
            fillFieldsFromConfig();
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {"Opac-Ergebnisses" }, logger, e);
        }
    }

    private void readPreferences() {
        LegacyPrefsHelper prefs = ServiceManager.getRulesetService().getPreferences(this.template.getRuleset());
        try {
            this.myRdf = new LegacyMetsModsDigitalDocumentHelper(prefs.getRuleset());
            this.myRdf.read(this.metadataFile.getPath());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * die Eingabefelder für die Eigenschaften mit Inhalten aus der RDF-Datei
     * füllen.
     */
    private void fillFieldsFromMetadataFile(LegacyMetsModsDigitalDocumentHelper myRdf) {
        if (Objects.nonNull(myRdf)) {
            for (AdditionalField field : this.additionalFields) {
                if (field.isUghBinding() && field.showDependingOnDoctype()) {
                    LegacyDocStructHelperInterface myTempStruct = getDocstructForMetadataFile(myRdf, field);
                    try {
                        setMetadataForMetadataFile(field, myTempStruct);
                    } catch (IllegalArgumentException e) {
                        Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                    }
                }
            }
        }
    }

    private LegacyDocStructHelperInterface getDocstructForMetadataFile(LegacyMetsModsDigitalDocumentHelper myRdf,
            AdditionalField field) {
        LegacyDocStructHelperInterface myTempStruct = myRdf.getDigitalDocument().getLogicalDocStruct();
        if (field.getDocStruct().equals("firstchild")) {
            try {
                myTempStruct = myRdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (field.getDocStruct().equals("physSequence")) {
            myTempStruct = myRdf.getDigitalDocument().getPhysicalDocStruct();
        }
        return myTempStruct;
    }

    private void setMetadataForMetadataFile(AdditionalField field, LegacyDocStructHelperInterface myTempStruct) {
        if (field.getMetadata().equals("ListOfCreators")) {
            throw new UnsupportedOperationException("Dead code pending removal");
        } else {
            /* evaluate the content in normal fields */
            LegacyMetadataTypeHelper mdt = LegacyPrefsHelper.getMetadataType(
                ServiceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset()), field.getMetadata());
            LegacyMetadataHelper md = LegacyLogicalDocStructHelper.getMetadata(myTempStruct, mdt);
            if (Objects.nonNull(md)) {
                field.setValue(md.getValue());
            }
        }
    }

    private void fillFieldsFromConfig() {
        for (AdditionalField field : this.additionalFields) {
            if (!field.isUghBinding() && field.showDependingOnDoctype() && !field.getSelectList().isEmpty()) {
                field.setValue((String) field.getSelectList().get(0).getValue());
            }
        }
        calculateTiffHeader();

    }

    /**
     * Auswahl des Prozesses auswerten.
     */
    @Override
    public String evaluateTemplateSelection() {
        readTemplateSelection();

        try {
            this.myRdf = ServiceManager.getProcessService().readMetadataAsTemplateFile(this.processForChoice);
        } catch (IOException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READING, new Object[] {"Template-Metadaten" }, logger, e);
        }

        removeCollectionsForChildren(this.myRdf, this.prozessKopie);

        return "";
    }

    @Override
    protected void readTemplateSelection() {
        readTemplateWorkpieces(this.additionalFields, this.processForChoice);
        readTemplateTemplates(this.additionalFields, this.processForChoice);
    }

    /**
     * Test title correction.
     *
     * @return true if title is correct, false otherwise
     */
    public boolean testTitle() {
        if (ConfigCore.getBooleanParameterOrDefaultValue(ParameterCore.MASS_IMPORT_UNIQUE_TITLE)) {
            return ProcessValidator.isProcessTitleCorrect(this.prozessKopie.getTitle());
        }
        return true;
    }

    /**
     * Create Process.
     *
     * @param io
     *            import object
     * @return Process object
     */
    public Process createProcess(ImportObject io) throws DataException, IOException {
        addProperties(io);
        updateTasks(this.prozessKopie);

        if (!io.getBatches().isEmpty()) {
            this.prozessKopie.getBatches().addAll(io.getBatches());
        }

        ServiceManager.getProcessService().save(this.prozessKopie);
        ServiceManager.getProcessService().refresh(this.prozessKopie);

        /*
         * wenn noch keine RDF-Datei vorhanden ist (weil keine Opac-Abfrage
         * stattfand, dann jetzt eine anlegen
         */
        if (Objects.isNull(this.myRdf)) {
            createNewFileformat();
        }

        ServiceManager.getFileService().writeMetadataFile(this.myRdf, this.prozessKopie);
        ServiceManager.getProcessService().readMetadataFile(this.prozessKopie);

        /* damit die Sortierung stimmt nochmal einlesen */
        ServiceManager.getProcessService().refresh(this.prozessKopie);
        return this.prozessKopie;
    }

    @Override
    public void createNewFileformat() {

        LegacyPrefsHelper myPrefs = ServiceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset());

        LegacyMetsModsDigitalDocumentHelper ff;
        try {
            ff = new LegacyMetsModsDigitalDocumentHelper(myPrefs.getRuleset());
            ff.read(this.metadataFile.getPath());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void addProperties(ImportObject importObject) {
        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "DocType", this.docType);
        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderImagedescription",
                this.tifHeaderImageDescription);
        ProcessGenerator.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderDocumentname", this.tifHeaderDocumentName);

        if (Objects.isNull(importObject)) {
            addAdditionalFields(this.additionalFields, this.prozessKopie);
        } else {
            for (Property processProperty : importObject.getProcessProperties()) {
                ProcessGenerator.copyPropertyForProcess(this.prozessKopie, processProperty);
            }
            for (Property workpieceProperty : importObject.getWorkProperties()) {
                ProcessGenerator.copyPropertyForWorkpiece(this.prozessKopie, workpieceProperty);
            }

            for (Property templateProperty : importObject.getTemplateProperties()) {
                ProcessGenerator.copyPropertyForTemplate(this.prozessKopie, templateProperty);
            }
            ProcessGenerator.addPropertyForProcess(this.prozessKopie, "Template", this.template.getTitle());
            ProcessGenerator.addPropertyForProcess(this.prozessKopie, "TemplateID", String.valueOf(this.template.getId()));
        }
    }

    @Override
    public Process getProcessForChoice() {
        return this.processForChoice;
    }

    @Override
    public void setProcessForChoice(Process processForChoice) {
        this.processForChoice = processForChoice;
    }

    /**
     * this is needed for GUI, render multiple select only if this is false if
     * this is true use the only choice.
     *
     * @author Wulf
     */
    @Override
    public boolean isSingleChoiceCollection() {
        return getPossibleDigitalCollections().size() == 1;

    }

    @Override
    public List<String> getAllOpacCatalogues() {
        return ConfigOpac.getAllCatalogueTitles();
    }

    @Override
    public List<ConfigOpacDoctype> getAllDoctypes() {
        return ConfigOpac.getAllDoctypes();
    }

    @Override
    public String getOpacSuchfeld() {
        return this.opacSuchfeld;
    }

    @Override
    public void setOpacSuchfeld(String opacSuchfeld) {
        this.opacSuchfeld = opacSuchfeld;
    }

    @Override
    public String getOpacKatalog() {
        return this.opacKatalog;
    }

    @Override
    public void setOpacKatalog(String opacKatalog) {
        this.opacKatalog = opacKatalog;
    }

    @Override
    public String getOpacSuchbegriff() {
        return this.opacSuchbegriff;
    }

    @Override
    public void setOpacSuchbegriff(String opacSuchbegriff) {
        this.opacSuchbegriff = opacSuchbegriff;
    }

    /**
     * Generate process titles and other details.
     */
    @Override
    public void calculateProcessTitle() {
        StringBuilder newTitle = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(titleDefinition, "+");
        /* jetzt den Bandtitel parsen */
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            // if the string begins and ends with ', then take over the content
            if (token.startsWith("'") && token.endsWith("'")) {
                newTitle.append(token, 1, token.length() - 1);
            } else {
                appendDataFromAdditionalFields(token, newTitle);
            }
        }

        if (newTitle.toString().endsWith("_")) {
            newTitle.substring(0, newTitle.length() - 1);
        }
        this.prozessKopie.setTitle(newTitle.toString());
        calculateTiffHeader();
    }

    private String calcProcessTitleCheck(String fieldName, String fieldValue) {
        String processTitleCheck = fieldValue;

        if ("Bandnummer".equals(fieldName)) {
            try {
                int bandInt = Integer.parseInt(fieldValue);
                java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("#0000");
                processTitleCheck = decimalFormat.format(bandInt);
            } catch (NumberFormatException e) {
                Helper.setErrorMessage(INCOMPLETE_DATA, "Bandnummer ist keine gültige Zahl", logger, e);
            }
            if (Objects.nonNull(processTitleCheck) && processTitleCheck.length() < 4) {
                processTitleCheck = "0000".substring(processTitleCheck.length()) + processTitleCheck;
            }
        }
        return processTitleCheck;
    }

    @Override
    public void calculateTiffHeader() {
        // possible replacements
        this.tifDefinition = this.tifDefinition.replaceAll("\\[\\[", "<");
        this.tifDefinition = this.tifDefinition.replaceAll("\\]\\]", ">");

        /*
         * Documentname ist im allgemeinen = Prozesstitel
         */
        this.tifHeaderDocumentName = this.prozessKopie.getTitle();
        StringBuilder tifHeaderImageDescriptionBuilder = new StringBuilder();

        // image description
        StringTokenizer tokenizer = new StringTokenizer(this.tifDefinition, "+");
        /* jetzt den Tiffheader parsen */
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            // if the string begins and ends with ', then take over the content
            if (token.startsWith("'") && token.endsWith("'") && token.length() > 2) {
                tifHeaderImageDescriptionBuilder.append(token, 1, token.length() - 1);
            } else if ("$Doctype".equals(token)) {
                tifHeaderImageDescriptionBuilder.append(this.docType);
            } else {
                appendDataFromAdditionalFields(token, tifHeaderImageDescriptionBuilder);
            }
        }
        this.tifHeaderImageDescription = tifHeaderImageDescriptionBuilder.toString();
    }

    /**
     * Evaluate the token as field name.
     *
     * @param token
     *            as String
     * @param stringBuilder
     *            as StringBuilder
     */
    private void appendDataFromAdditionalFields(String token, StringBuilder stringBuilder) {
        for (AdditionalField additionalField : this.additionalFields) {
            /*
             * if it is the ATS or TSL field, then use the calculated atstsl if it does not
             * already exist
             */
            String title = additionalField.getTitle();
            String value = additionalField.getValue();
            if (("ATS".equals(title) || "TSL".equals(title)) && additionalField.showDependingOnDoctype()
                    && StringUtils.isEmpty(value)) {
                additionalField.setValue("");
                value = additionalField.getValue();
            }

            // add the content to the title
            if (title.equals(token) && additionalField.showDependingOnDoctype() && Objects.nonNull(value)) {
                stringBuilder.append(calcProcessTitleCheck(title, value));
            }
        }
    }

    public void setMetadataFile(URI mdFile) {
        this.metadataFile = mdFile;
    }

    public URI getMetadataFile() {
        return this.metadataFile;
    }
}

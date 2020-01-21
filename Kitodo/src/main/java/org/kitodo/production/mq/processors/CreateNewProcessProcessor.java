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

package org.kitodo.production.mq.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Template;
import org.kitodo.exceptions.ProcessCreationException;
import org.kitodo.production.forms.copyprocess.ProzesskopieForm;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.mq.ActiveMQProcessor;
import org.kitodo.production.mq.MapMessageObjectReader;
import org.kitodo.production.process.field.AdditionalField;
import org.kitodo.production.search.opac.ConfigOpacDoctype;
import org.kitodo.production.services.ServiceManager;

/**
 * CreateNewProcessProcessor is an Apache Active MQ consumer which registers to
 * a queue configured by "activeMQ.createNewProcess.queue" on application
 * startup. It was designed to create new processes from outside Production. There
 * are two ways providing to create new processes. If the MapMessage on that
 * queue contains of all the fields listed, the bibliographic data is retrieved
 * using a catalog configured within Production. If “opac” is missing, it will try
 * to create a process just upon the data passed in the “userFields” − “field”
 * and “value” will be ignored in that case, and the “docType” can be set
 * manually.
 *
 * <p>
 * Field summary:
 *
 * <dl>
 * <dt>String template</dt>
 * <dd>name of the process template to use.</dd>
 * <dt>String opac</dt>
 * <dd>Cataloge to use for lookup.</dd>
 * <dt>String field</dt>
 * <dd>Field to look into, usually 12 (PPN).</dd>
 * <dt>String value</dt>
 * <dd>Value to look for, id of physical medium</dd>
 * <dt>String docType</dt>
 * <dd>DocType value to use if no catalog request is performed.</dd>
 * <dt>Set&lt;String&gt; collections</dt>
 * <dd>Collections to be selected.</dd>
 * <dt>Map&lt;String, String&gt; userFields collections</dt>
 * <dd>Fields to be populated manually.</dd>
 * </dl>
 */
public class CreateNewProcessProcessor extends ActiveMQProcessor {
    private static final Logger logger = LogManager.getLogger(CreateNewProcessProcessor.class);
    private static final String ERROR_CREATE = "errorCreating";

    public CreateNewProcessProcessor() {
        super(ConfigCore.getOptionalString(ParameterCore.ACTIVE_MQ_CREATE_NEW_PROCESSES_QUEUE).orElse(null));
    }

    @Override
    protected void process(MapMessageObjectReader args) throws JMSException {
        Set<String> collections = args.getMandatorySetOfString("collections");
        String id = args.getMandatoryString("id");
        String template = args.getMandatoryString("template");
        Map<String, String> userFields = args.getMapOfStringToString("userFields");
        if (args.hasField("opac")) {
            String opac = args.getMandatoryString("opac");
            String field = args.getMandatoryString("field");
            String value = args.getMandatoryString("value");
            createNewProcessMain(template, opac, field, value, id, null, collections, userFields);
        } else {
            String docType = args.getString("docType");
            createNewProcessMain(template, null, null, null, id, docType, collections, userFields);
        }

    }

    /**
     * This is the main routine used to create new processes.
     *
     * @param template
     *            title of the process template the new process shall be derived
     *            from
     * @param opac
     *            name of the connection to a library catalog to load the
     *            bibliographic data from (may be null)
     * @param field
     *            number of the catalog search field (ignored if “opac” is
     *            null)
     * @param value
     *            search string (ignored if “opac” is null)
     * @param id
     *            identifier to be used for the digitisation
     * @param docType
     *            docType to set (may be null)
     * @param collections
     *            collections to add the digitisation to
     * @param userFields
     *            Values for additional fields can be set here (may be null)
     */
    private static void createNewProcessMain(String template, String opac, String field, String value, String id,
            String docType, Set<String> collections, Map<String, String> userFields) {

        try {
            ProzesskopieForm newProcess = newProcessFromTemplate(template);
            newProcess.setDigitalCollections(validCollectionsForProcess(collections, newProcess));
            if (Objects.nonNull(opac)) {
                getBibliorgaphicData(newProcess, opac, field, value);
            }
            if (Objects.nonNull(docType)) {
                docTypeIsPossible(newProcess, docType);
                newProcess.setDocType(docType);
            }
            if (Objects.nonNull(userFields)) {
                setUserFields(newProcess, userFields);
            }
            newProcess.calculateProcessTitle();
            boolean created = newProcess.createProcess();
            if (!created) {
                throw new ProcessCreationException(
                        Helper.getTranslation(ERROR_CREATE, Collections.singletonList("process: " + id)));
            }
            logger.info("Created new process: {}", id);
        } catch (RuntimeException e) {
            logger.error(Helper.getTranslation(ERROR_CREATE, Collections.singletonList("process: " + id)), e);
            throw new ProcessCreationException(
                    Helper.getTranslation(ERROR_CREATE, Collections.singletonList("process: " + id)));
        }
    }

    /**
     * Derives a ProzesskopieForm object from a given template.
     *
     * @param templateTitle
     *            title value of the template to look for
     * @return a ProzesskopieForm object, prepared from a given template
     * @throws IllegalArgumentException
     *             if no suitable template is found
     */
    public static ProzesskopieForm newProcessFromTemplate(String templateTitle) {
        ProzesskopieForm prozesskopieFormFromTemplate = new ProzesskopieForm();

        Template selectedTemplate = getTemplateByTitle(templateTitle);
        prozesskopieFormFromTemplate.setTemplate(selectedTemplate);
        //TODO: how to get here id of correct project?
        prozesskopieFormFromTemplate.prepareProcess(selectedTemplate.getId(), 0);
        return prozesskopieFormFromTemplate;
    }

    /**
     * Fetches the first process template with the given title from the
     * database.
     *
     * @param templateTitle
     *            the title of the template to be picked up
     * @return the template, if found
     * @throws IllegalArgumentException
     *             is thrown, if there is no template matching the given
     *             templateTitle
     */
    private static Template getTemplateByTitle(String templateTitle) {
        List<Template> response = ServiceManager.getTemplateService().getProcessTemplatesWithTitle(templateTitle);

        if (!response.isEmpty()) {
            return response.get(0);
        } else {
            throw new IllegalArgumentException("Bad argument: No template \"" + templateTitle + "\" available.");
        }
    }

    /**
     * Tests whether a given set of collections can be assigned to new process.
     * If so, the set of collections is returned as a list ready for assignment.
     *
     * @param collections
     *            a set of collection names to be tested
     * @param process
     *            a ProzesskopieForm object whose prozessVorlage has been set
     * @return an ArrayList which can be used to set the digitalCollections of a
     *         ProzesskopieForm
     * @throws IllegalArgumentException
     *             in case that the given collection isn’t a valid subset of the
     *             digitalCollections possible here
     */
    private static List<String> validCollectionsForProcess(Set<String> collections, ProzesskopieForm process) {
        HashSet<String> possibleCollections = new HashSet<>(process.getPossibleDigitalCollections());
        if (!possibleCollections.containsAll(collections)) {
            throw new IllegalArgumentException("Bad argument: One or more elements of \"collections\" is not "
                    + "available for template \"" + process.getTemplate().getTitle() + "\".");
        }
        return new ArrayList<>(collections);
    }

    /**
     * Tests whether a given docType String can be applied to a given process
     * template. If so, it will execute without exception, otherwise, it will
     * throw an informative IllegalArgumentException.
     *
     * @param dialog
     *            the ProzesskopieForm object to test against
     * @param docType
     *            the desired docType ID string
     * @throws IllegalArgumentException
     *             if a docType is not applicable to the template or the docType
     *             isn’t valid
     */
    private static void docTypeIsPossible(ProzesskopieForm dialog, String docType) {
        Boolean fieldIsUsed = dialog.getStandardFields().get("doctype");
        if (Objects.isNull(fieldIsUsed) || fieldIsUsed.equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(
                    "Bad argument “docType”: Selected template doesn’t provide the standard field “doctype”.");
        }

        boolean valueIsValid;
        Iterator<ConfigOpacDoctype> configOpacDoctypeIterator = dialog.getAllDoctypes().iterator();
        do {
            ConfigOpacDoctype option = configOpacDoctypeIterator.next();
            valueIsValid = docType.equals(option.getTitle());
        } while (!valueIsValid && configOpacDoctypeIterator.hasNext());

        if (!valueIsValid) {
            throw new IllegalArgumentException("Bad argument “docType”: Selected template doesn’t provide "
                    + "a docType “{0}”.".replace("{0}", docType));
        }
    }

    /**
     * Allows to set any additional field to a user-specific value.
     *
     * @param form
     *            a ProzesskopieForm object whose AdditionalField objects are
     *            subject to the change
     * @param userFields
     *            the data to pass to the form
     * @throws RuntimeException
     *             in case that no field with a matching title was found in the
     *             ProzesskopieForm object
     */
    private static void setUserFields(ProzesskopieForm form, Map<String, String> userFields) {
        for (Map.Entry<String, String> entry : userFields.entrySet()) {
            form.setAdditionalField(entry.getKey(), entry.getValue(), true);
        }
    }

    /**
     * Sets the bibliographic data for a new process from a library catalog.
     * This is equal to manually choosing a catalog and a getByQuery field,
     * entering the getByQuery string and clicking “apply”.
     *
     * <p>
     * Since the underlying evaluateOpac() method doesn’t raise exceptions, we
     * count the populated “additional details” fields before and after running
     * the request and assume the method to have failed if not even one more
     * field was populated by the method call.
     *
     * @param inputForm
     *            the ProzesskopieForm to be set
     * @param opac
     *            the value for “Search in Opac”
     * @param field
     *            the number of the search field, e.g. “12” for PPN.
     * @param value
     *            the getByQuery string
     * @throws RuntimeException
     *             is thrown if the search didn’t bring any results
     */
    private static void getBibliorgaphicData(ProzesskopieForm inputForm, String opac, String field, String value) {

        inputForm.setOpacKatalog(opac);
        inputForm.setOpacSuchfeld(field);
        inputForm.setOpacSuchbegriff(value);

        int before = countPopulatedAdditionalFields(inputForm);
        inputForm.evaluateOpac();
        int afterwards = countPopulatedAdditionalFields(inputForm);

        if (afterwards <= before) {
            throw new RuntimeException("Searching the OPAC didn’t yield any results.");
        }
    }

    /**
     * Returns the number of AdditionalFields in the given ProzesskopieForm that
     * have meaningful content.
     *
     * @param form
     *            a ProzesskopieForm object to examine
     * @return the number of AdditionalFields populated
     */
    private static int countPopulatedAdditionalFields(ProzesskopieForm form) {
        int populatedAdditionalFields = 0;

        for (AdditionalField field : form.getAdditionalFields()) {
            String value = field.getValue();
            if (Objects.nonNull(value) && !value.isEmpty()) {
                populatedAdditionalFields++;
            }
        }

        return populatedAdditionalFields;
    }

}

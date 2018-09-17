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

package org.kitodo.config.enums;

/**
 * These constants define configuration parameters usable in the configuration
 * file. This file reflects the order of the global
 * {@code kitodo_config.properties} file (as far as possible), and vice versa.
 */
public enum Parameter implements ParameterInterface {

    /*
     * FILE AND DIRECTORY MANAGEMENT
     *
     * Directories
     */

    /*
     * Parameters.java has been sorted to corresponding to
     * kitodo_config.properties, including the section headers as you see.
     * However there is an entry directory.config in
     * kitodo_config.properties, but there is no constant for it here. This
     * comment is to explain where the constant is if someone compares the two
     * files in the future:
     */

    DIR_XML_CONFIG("directory.config"),

    /**
     * Absolute path to the directory that the rule set definition files will be
     * read from. It must be terminated by a directory separator ("/").
     */
    DIR_RULESETS("directory.rulesets"),

    /**
     * Absolute path to the directory that XSLT files are stored in which are
     * used to transform the "XML log" (as visible from the XML button in the
     * processes list) to a downloadable PDF docket which can be enclosed with
     * the physical binding units to digitise. The path must be terminated by a
     * directory separator ("/").
     */
    DIR_XSLT("directory.xslt"),

    /*
     * Parameters.java has been sorted to corresponding to
     * kitodo_config.properties, including the section headers as you see.
     * However there is an entry directory.metadata in
     * kitodo_config.properties, but there is no constant for it here. This
     * comment is to explain where the constant is if someone compares the two
     * files in the future:
     */

    DIR_PROCESSES("directory.metadata"),

    /**
     * Absolute path to the base directory of the users' home directories,
     * terminated by a directory separator ("/"). If a user accepts a task to
     * work on which will require him or her to have access permission to the
     * data of a process, a symbolic link to the process directory in question
     * will be created in his or her home directory that will be removed again
     * after finishing the task. Note: If LDAP is used, the users' home dirs
     * will instead be read from LDAP
     */
    DIR_USERS("directory.users"),

    /**
     * Absolute path to a folder the application can temporarily create files
     * in, terminated by a directory separator ("/").
     */
    DIR_TEMP("directory.temp"),

    /**
     * Path to directory in which BPMN diagrams are stored.
     */
    DIR_DIAGRAMS("diagramsFolder"),

    /*
     * Parameters.java has been sorted to corresponding to
     * kitodo_config.properties, including the section headers as you see.
     * However there is an entry directory.modules in kitodo_config.properties, but
     * there is no constant for it here. This comment is to explain where the
     * constant is if someone compares the two files in the future:
     */

    DIR_MODULES("directory.modules"),

    /**
     * Points to a folder on the file system that contains <b>legacy</b>
     * Production plug-in jars. In the folder, there must be subfolders named as
     * defined in enum PluginType (currently: “import” and “opac”) in which the
     * plug-in jars must be stored.
     *
     * <p>
     * Must be terminated by the file separator.
     */
    DIR_PLUGINS("directory.plugins"),

    /**
     * Points to a folder on the file system that <b>legacy</b> plug-ins may use
     * to write temporary files.
     */
    DIR_PLUGINS_TEMP("directory.debug"),

    /*
     * Directory management
     */

    /**
     * Boolean, defaults to {@code false}.
     */
    CREATE_ORIG_FOLDER_IF_NOT_EXISTS("createOrigFolderIfNotExists"),

    /*
     * Parameters.java has been sorted to corresponding to
     * kitodo_config.properties, including the section headers as you see.
     * However there is an entry createSourceFolder in kitodo_config.properties,
     * but there is no constant for it here. This comment is to explain where
     * the constant is if someone compares the two files in the future:
     */
    CREATE_SOURCE_FOLDER("createSourceFolder"),

    /**
     * Prefix of image directory name created on process creation.
     */
    DIRECTORY_PREFIX("DIRECTORY_PREFIX"),

    /**
     * Directory suffix for created image directory on process creation.
     */
    DIRECTORY_SUFFIX("DIRECTORY_SUFFIX"),

    /**
     * Boolean, defaults to {@code false}.
     */
    IMPORT_USE_OLD_CONFIGURATION("importUseOldConfiguration"),

    /**
     * Creation and export of process sub-directories, e.g.
     * {@code images/(processtitle)_tif&ocr/(processtitle)_pdf}.
     * {@code (processtitle)} is a placeholder for the process title If you
     * comment in the parameter processDirs without a value, the result is that
     * the whole process directory will be exported and no directory well be
     * created. If you leave the parameter commented out, the whole
     * functionality is disabled. Using the {@code processDirs} parameter is
     * always an addition to the existing folder creating and exporting
     * functions of Kitodo.Production.
     */
    PROCESS_DIRS("processDirs"),

    /**
     * Set if master images folder {@code orig_} should be used at all. Boolean,
     * defaults to {@code true}.
     */
    USE_ORIG_FOLDER("useOrigFolder"),

    /*
     * Directory and symbolic link management
     */

    /**
     * Script to create the user's home directory when adding a new user.
     */
    SCRIPT_CREATE_DIR_USER_HOME("script_createDirUserHome"),

    /**
     * Script to create the directory for a new process.
     */
    SCRIPT_CREATE_DIR_META("script_createDirMeta"),

    /**
     * Script to create a symbolic link in the user home directory and set
     * permissions for the user.
     */
    SCRIPT_CREATE_SYMLINK("script_createSymLink"),

    /**
     * Script to remove the symbolic link from the user home directory.
     */
    SCRIPT_DELETE_SYMLINK("script_deleteSymLink"),

    /*
     * Runnotes
     */

    /**
     * Filename of the XSLT file for transforming old metadata files which need
     * to be in the xslt folder above.
     */
    XSLT_FILENAME_METADATA_TRANSFORMATION("xsltFilenameMetadataTransformation"),

    /*
     * Images
     */

    /**
     * Prefix for image names as regex. Default is 8 digits \\d{8} and gets
     * validated.
     */
    IMAGE_PREFIX("ImagePrefix"),

    /**
     * Sorting of images.
     */
    IMAGE_SORTING("ImageSorting"),

    /**
     * Numeric sorting of images. 1 is lesser then 002, compares the number of
     * image names, characters other than digits are not supported
     */
    IMAGE_SORTING_VALUE_NUMBER("number"),

    /**
     * Alphanumeric sorting of images. 1 is greater then 002, compares character
     * by character of image names, all characters are supported.
     */
    IMAGE_SORTING_VALUE_ALPHANUMERIC("alphanumeric"),

    /**
     * Defaults to {@code fertig/}.
     */
    DONE_DIRECTORY_NAME("doneDirectoryName"),

    /*
     * VISUAL APPEARANCE
     *
     * Internationalization
     */

    /**
     * Absolute path to the directory that the resource bundle files are stored
     * in, terminated by a directory separator ("/").
     *
     * <p>
     * Note: If this directory DOESN'T EXIST, the internal resource bundles will
     * be used. If this directory exists BUT DOES NOT CONTAIN suitable
     * resources, the screens will not work as expected.
     */
    DIR_LOCAL_MESSAGES("directory.messages"),

    /**
     * Start-up language: If not set, Kitodo.Production will start up with the
     * language best matching the user's Accept-Languages HTTP Request header.
     * You can override this behaviour by setting a default language here.
     */
    LANGUAGE_FORCE_DEFAULT("language.force-default"),

    /**
     * If no Accept-Language Http Request header is present, use the following
     * language.
     */
    LANGUAGE_DEFAULT("language.default"),

    /*
     * Data protection
     */

    /**
     * The General Data Protection Regulation or local law might require to set
     * this value to true. anonymized statistics, displaying user on steps, etc.
     * Boolean, defaults to {@code false}.
     */
    ANONYMIZE("anonymize"),

    /**
     * Enable / disable search for steps done by user. Boolean, defaults to
     * {@code false}.
     */
    WITH_USER_STEP_DONE_SEARCH("withUserStepDoneSearch"),

    /*
     * Error page
     */

    /**
     * Page the user will be directed to continue.
     */
    ERR_LINK_TO_PAGE("err_linkToPage"),

    /*
     * METADATA PROCESSING
     *
     * Catalogue search
     */

    /**
     * Number of hits to show per page on the hitlist when multiple hits were
     * found on a catalogue search. Integer, defaults to 12.
     */
    HITLIST_PAGE_SIZE("catalogue.hitlist.pageSize"),

    /**
     * Indicates the maximum duration an interaction with a library catalogue
     * may take. Milliseconds, defaults to 30 minutes.
     */
    CATALOGUE_TIMEOUT("catalogue.timeout"),

    /*
     * Metadata editor behavior
     */

    /**
     * Long, value in milliseconds. Defaults to 180000 (30 minutes).
     */
    METS_EDITOR_LOCKING_TIME("MetsEditorLockingTime"),

    /**
     * Use special image folder for METS editor if exists (define suffix here).
     */
    METS_EDITOR_DEFAULT_SUFFIX("MetsEditorDefaultSuffix"),

    /**
     * Enables or disables automatic pagination changes in the meta-data editor.
     * If false, pagination must be updated manually by clicking the link “Read
     * in pagination from images”. Boolean, defaults to {@code true}.
     */
    WITH_AUTOMATIC_PAGINATION("MetsEditorWithAutomaticPagination"),

    /**
     * Use special pagination type for automatic default pagination.
     */
    METS_EDITOR_DEFAULT_PAGINATION("MetsEditorDefaultPagination"),
    // TODO: it doesn't fit here
    METS_EDITOR_DEFAULT_PAGINATION_VALUE_ARABIC("arabic"),
    METS_EDITOR_DEFAULT_PAGINATION_VALUE_ROMAN("roman"),
    METS_EDITOR_DEFAULT_PAGINATION_VALUE_UNCOUNTED("uncounted"),

    /**
     * Use a maximum of characters to display titles in the left part of mets
     * editor. Integer, the default value is 0 (everything is displayed).
     */
    METS_EDITOR_MAX_TITLE_LENGTH("MetsEditorMaxTitleLength"),

    /**
     * Initialize all sub elements in METS editor to assign default values.
     * Boolean, defaults to {@code true}.
     */
    METS_EDITOR_ENABLE_DEFAULT_INITIALISATION("MetsEditorEnableDefaultInitialisation"),

    /**
     * Display the file manipulation dialog within the METS editor.
     */
    METS_EDITOR_DISPLAY_FILE_MANIPULATION("MetsEditorDisplayFileManipulation"),

    /**
     * Comma-separated list of Strings which may be enclosed in double quotes.
     * Separators available for double page pagination modes.
     */
    PAGE_SEPARATORS("pageSeparators"),

    /*
     * backup of metadata configuration
     */

    /**
     * Backup of metadata configuration. Integer.
     */
    NUMBER_OF_META_BACKUPS("numberOfMetaBackups"),

    /*
     * Metadata enrichment
     */

    /**
     * Set to true to enable the feature of automatic meta data inheritance and
     * enrichment. If this is enabled, all meta data elements from a higher
     * level of the logical document structure are automatically inherited and
     * lower levels are enriched with them upon process creation, given they
     * have the same meta data type addable. Boolean, defaults to false.
     */
    USE_METADATA_ENRICHMENT("useMetadataEnrichment"),

    /*
     * Data copy rules
     */

    /**
     * Data copy rules may be used to copy Kitodo internal data and metadata on
     * catalogue query.
     */
    COPY_DATA_ON_CATALOGUE_QUERY("copyData.onCatalogueQuery"),

    /**
     * Data copy rules may be used to copy Kitodo internal data and metadata on
     * DMS export.
     */
    COPY_DATA_ON_EXPORT("copyData.onExport"),

    /*
     * Metadata validation
     */

    /**
     * Perform basic metadata validation or not. Boolean, defaults to
     * {@code false}.
     */
    USE_META_DATA_VALIDATION("useMetadatenvalidierung"),

    /**
     * Validation of process title via regular expression.
     */
    VALIDATE_PROCESS_TITLE_REGEX("validateProzessTitelRegex"),

    /**
     * Validation of the identifier via regular expression.
     */
    VALIDATE_IDENTIFIER_REGEX("validateIdentifierRegex"),

    /*
     * AUTOMATION
     *
     * Mass process generation
     */

    /**
     * Boolean, defaults to {@code false}.
     */
    MASS_IMPORT_ALLOWED("massImportAllowed"),

    /**
     * Boolean, defaults to {@code true}.
     */
    MASS_IMPORT_UNIQUE_TITLE("MassImportUniqueTitle"),

    /**
     * Colours used to represent the issues in the calendar editor.
     */
    ISSUE_COLOURS("issue.colours"),

    /**
     * Number of pages per process below which the features in the granularity
     * dialog shall be locked. Long.
     */
    MINIMAL_NUMBER_OF_PAGES("numberOfPages.minimum"),

    /*
     * Batch processing
     */

    /**
     * Limits the number of batches showing on the page “Batches”. Defaults to
     * -1 which disables this functionality. If set, only the limited number of
     * batches will be shown, the other batches will be present but hidden and
     * thus cannot be modified and not even be deleted. Integer.
     */
    BATCH_DISPLAY_LIMIT("batchMaxSize"),

    /**
     * Turn on or off whether each assignment of processes to or removal from
     * batches shall result in rewriting each processes' wiki field in order to
     * leave a note there. Enabling this function may slow down operations in
     * the batches dialogue. Boolean, defaults to {@code false}.
     */
    BATCHES_LOG_CHANGES("batches.logChangesToWikiField"),

    /*
     * content server for PDF generation
     */

    /**
     * Boolean, defaults to {@code false}.
     */
    PDF_AS_DOWNLOAD("pdfAsDownload"),

    /**
     * If empty, internal content server will be used.
     */
    KITODO_CONTENT_SERVER_URL("kitodoContentServerUrl"),

    /**
     * Timeout for content server requests via HTTP in ms. Integer, defaults to
     * 60000 (60 sec).
     */

    KITODO_CONTENT_SERVER_TIMEOUT("kitodoContentServerTimeOut"),

    /*
     * Task manager
     */

    /**
     * Overrides the limit of tasks run in parallel. Integer, defaults to the
     * number of available cores.
     */
    TASK_MANAGER_AUTORUN_LIMIT("taskManager.autoRunLimit"),

    /**
     * Sets the time interval between two inspections of the task list. Long,
     * defaults to 2000 ms.
     */
    TASK_MANAGER_INSPECTION_INTERVAL_MILLIS("taskManager.inspectionIntervalMillis"),

    /**
     * Sets the maximum number of failed threads to keep around in RAM. Keep in
     * mind that zombie processes still occupy all their ressources and aren't
     * available for garbage collection, so choose these values as restrictive
     * as possible. Integer, defaults to 10.
     */
    TASK_MANAGER_KEEP_FAILED("taskManager.keepThreads.failed.count"),

    /**
     * Sets the maximum time to keep failed threads around in RAM. Keep in mind
     * that zombie processes still occupy all their ressources and aren't
     * available for garbage collection, so choose these values as restrictive
     * as possible. Integer, defaults to 240 minutes.
     */
    TASK_MANAGER_KEEP_FAILED_MINS("taskManager.keepThreads.failed.minutes"),

    /**
     * Sets the maximum number of successfully finished threads to keep around
     * in RAM. Defaults to 3. Keep in mind that zombie processes still occupy
     * all their ressources and aren't available for garbage collection, so
     * choose these values as restrictive as possible.
     */
    TASK_MANAGER_KEEP_SUCCESSFUL("taskManager.keepThreads.successful.count"),

    /**
     * Sets the maximum time to keep successfully finished threads around in
     * RAM. Defaults to 20 minutes. Keep in mind that zombie processes still
     * occupy all their resources and aren't available for garbage collection,
     * so choose these values as restrictive as possible.
     */
    TASK_MANAGER_KEEP_SUCCESSFUL_MINS("taskManager.keepThreads.successful.minutes"),

    /**
     * Sets whether or not to show an option to "add a sample task" in the task
     * manager. This isif for anything at all—useful for debugging or
     * demonstration purposes only. Boolean, defaults to {@code false}.
     */
    TASK_MANAGER_SHOW_SAMPLE_TASK("taskManager.showSampleTask"),

    /*
     * Export to presentation module
     */

    /**
     * If you set this to true the exports will be done asynchronously (in the
     * background). This requires that the automatic export was set up in the
     * project settings. Boolean, defaults to {@code false}.
     */
    ASYNCHRONOUS_AUTOMATIC_EXPORT("asynchronousAutomaticExport"),

    /**
     * Whether during an export to the DMS the images will be copied. Boolean,
     * defaults to {@code true}.
     */
    EXPORT_WITH_IMAGES("automaticExportWithImages"),

    /**
     * Boolean, defaults to {@code true}.
     */
    AUTOMATIC_EXPORT_WITH_OCR("automaticExportWithOcr"),

    /**
     * Boolean, defaults to {@code true}.
     */
    EXPORT_VALIDATE_IMAGES("ExportValidateImages"),

    /**
     * Boolean, defaults to {@code false}.
     */
    EXPORT_WITHOUT_TIME_LIMIT("exportWithoutTimeLimit"),

    /*
     * REMOTE SERVICES
     *
     * LDAP Configuration
     */

    /**
     * Boolean, defaults to {@code true}.
     */
    LDAP_USE("ldap_use"),

    LDAP_ATTRIBUTE_TO_TEST("ldap_AttributeToTest"),

    LDAP_VALUE_OF_ATTRIBUTE("ldap_ValueOfAttribute"),

    /**
     * Boolean, defaults to {@code false}.
     */
    LDAP_USE_LOCAL_DIRECTORY("useLocalDirectory"),

    /**
     * Boolean, defaults to {@code false}.
     */
    LDAP_USE_TLS("ldap_useTLS"),

    LDAP_USE_SIMPLE_AUTH("useSimpleAuthentification"),

    /*
     * Authority control configuration
     */

    /**
     * Which authority identifier to use for a given URI prefix.
     *
     * <p>
     * Example: authority.http\://d-nb.info/gnd/.id=gnd
     * </p>
     */
    AUTHORITY_ID_FROM_URI("authority.{0}.id"),

    /**
     * Content to put in the URI field when adding a new metadata element of
     * type person. This should usually be your preferred norm data file’s URI
     * prefix as to the user doesn’t have to enter it over and over again.
     *
     * <p>
     * Example: authority.default=http\://d-nb.info/gnd/
     * </p>
     */
    AUTHORITY_DEFAULT("authority.default"),

    /*
     * INTERACTIVE ERROR MANAGEMENT
     */

    /**
     * Boolean, defaults to {@code false}.
     */
    ERR_USER_HANDLING("err_userHandling"),

    /**
     * Use this to turn the email feature on or off. Boolean, defaults to
     * {@code false}.
     */
    ERR_EMAIL_ENABLED("err_emailEnabled"),

    /**
     * An indefinite number of e-mail addresses can be entered here. Create one
     * enumerated configuration entry line per address, like:
     * {@code err_emailAddress1=(...)}, {@code err_emailAddress2=(...)},
     * {@code err_emailAddress3=(...)}, ...
     */
    ERR_EMAIL_ADDRESS("err_emailAddress"),

    /*
     * FUNCTIONAL EXTENSIONS
     *
     * OCR service access
     */

    /**
     * Boolean, defaults to {@code false}.
     */
    SHOW_OCR_BUTTON("showOcrButton"),

    /**
     * Base path to OCR, without parameters.
     */
    OCR_URL("ocrUrl"),

    /*
     * ActiveMQ web services
     */

    ACTIVE_MQ_HOST_URL("activeMQ.hostURL"),

    ACTIVE_MQ_CREATE_NEW_PROCESSES_QUEUE("activeMQ.createNewProcess.queue"),

    ACTIVE_MQ_FINALIZE_STEP_QUEUE("activeMQ.finaliseStep.queue"),

    ACTIVE_MQ_RESULTS_TOPIC("activeMQ.results.topic"),

    /**
     * Long, value in milliseconds.
     */
    ACTIVE_MQ_RESULTS_TTL("activeMQ.results.timeToLive"),

    /*
     * Elasticsearch properties
     */

    ELASTICSEARCH_BATCH("elasticsearch.batch");

    private String name;

    /**
     * Private constructor to hide the implicit public one.
     * 
     * @param name
     *            of parameter
     */
    Parameter(String name) {
        this.name = name;
    }

    @Override
    public java.lang.String toString() {
        return this.name;
    }
}

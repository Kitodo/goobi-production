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

package org.kitodo.production.search.opac;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.enums.KitodoConfigFile;
import org.kitodo.production.helper.Helper;

@XmlRootElement(name = "catalogueConfiguration")
public class ConfigOpac {
    private static final Logger logger = LogManager.getLogger(ConfigOpac.class);
    private static final String ERROR_READ = "errorReading";
    private static final String CATALOGUE = "catalogue";
    private static final String DOCTYPES_TYPE = "doctypes.type";
    private static final String OPAC_CONFIG = "configurationOPAC";
    private static XMLConfiguration config;

    private static XMLConfiguration getConfig() throws FileNotFoundException {
        if (Objects.nonNull(config)) {
            return config;
        }

        KitodoConfigFile opacConfiguration = KitodoConfigFile.OPAC_CONFIGURATION;

        if (!opacConfiguration.exists()) {
            throw new FileNotFoundException("File not found: " + opacConfiguration.getAbsolutePath());
        }
        try {
            config = new XMLConfiguration(opacConfiguration.getFile());
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
            config = new XMLConfiguration();
        }
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        return config;
    }

    /**
     * Returns all configured catalog titles from the config file.
     *
     * @return all catalog titles
     */
    public static List<String> getAllCatalogueTitles() {
        return getTitles(CATALOGUE);
    }

    /**
     * Return all configured Doctype-Titles from Config file.
     */
    private static List<String> getAllDoctypeTitles() {
        return getTitles(DOCTYPES_TYPE);
    }

    private static List<String> getTitles(String label) {
        List<String> titles = new ArrayList<>();
        try {
            int countTypes = getConfig().getMaxIndex(label);
            for (int i = 0; i <= countTypes; i++) {
                String title = getConfig().getString(label + "(" + i + ")[@title]");
                titles.add(title);
            }
        } catch (FileNotFoundException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[]{Helper.getTranslation(OPAC_CONFIG)}, logger, e);
        }
        return titles;
    }

    /**
     * Returns all configured media types from the config file.
     *
     * @return all media types
     */
    public static List<ConfigOpacDoctype> getAllDoctypes() {
        List<ConfigOpacDoctype> myList = new ArrayList<>();
        try {
            for (String title : getAllDoctypeTitles()) {
                myList.add(getDoctypeByName(title));
            }
        } catch (FileNotFoundException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[]{Helper.getTranslation(OPAC_CONFIG)}, logger, e);
        }
        return myList;
    }

    /**
     * get doctype from title.
     */
    public static ConfigOpacDoctype getDoctypeByName(String inTitle) throws FileNotFoundException {
        int countCatalogues = getConfig().getMaxIndex(DOCTYPES_TYPE);
        for (int i = 0; i <= countCatalogues; i++) {
            String title = getConfig().getString(DOCTYPES_TYPE + "(" + i + ")[@title]");
            if (title.equals(inTitle)) {
                /* Sprachen erfassen */
                HashMap<String, String> labels = new HashMap<>();
                int countLabels = getConfig().getMaxIndex(DOCTYPES_TYPE + "(" + i + ").label");
                for (int j = 0; j <= countLabels; j++) {
                    String language = getConfig().getString(DOCTYPES_TYPE + "(" + i + ").label(" + j + ")[@language]");
                    String value = getConfig().getString(DOCTYPES_TYPE + "(" + i + ").label(" + j + ")");
                    labels.put(language, value);
                }
                String inRulesetType = getConfig().getString(DOCTYPES_TYPE + "(" + i + ")[@rulesetType]");
                String inTifHeaderType = getConfig().getString(DOCTYPES_TYPE + "(" + i + ")[@tifHeaderType]");
                boolean periodical = getConfig().getBoolean(DOCTYPES_TYPE + "(" + i + ")[@isPeriodical]");
                boolean multiVolume = getConfig().getBoolean(DOCTYPES_TYPE + "(" + i + ")[@isMultiVolume]");
                boolean newspaper;
                try {
                    newspaper = getConfig().getBoolean(DOCTYPES_TYPE + "(" + i + ")[@isNewspaper]");
                } catch (NoSuchElementException noParameterIsNewspaper) {
                    newspaper = false;
                }
                List<Object> configs = getConfig().getList(DOCTYPES_TYPE + "(" + i + ").mapping");
                List<String> mappings = configs.stream().map(object -> Objects.toString(object, null))
                        .collect(Collectors.toList());

                return new ConfigOpacDoctype(inTitle, inRulesetType, inTifHeaderType, periodical, multiVolume,
                        newspaper, labels, mappings);
            }
        }
        return null;
    }

    /**
     * Returns the content of all {@code <restriction>} elements from the
     * {@code <catalogue>} entry with the given {@code title} from
     * {@code <kitodo_opac.xml>}.
     *
     * <p>
     * The function will return an empty list if there are no such entries for
     * the given catalog.
     *
     * @param title
     *            Title parameter of the {@code <catalogue>} entry to examine
     * @return List
     */
    public static List<String> getRestrictionsForCatalogue(String title) throws FileNotFoundException {
        List<String> restrictionsForCatalogue = new LinkedList<>();
        List<HierarchicalConfiguration> catalogues = getConfig().configurationsAt(CATALOGUE);
        for (HierarchicalConfiguration catalogue : catalogues) {
            if (title.equals(catalogue.getString("[@title]"))) {
                restrictionsForCatalogue.addAll(Arrays.asList(catalogue.getStringArray("restriction")));
                break;
            }
        }
        return restrictionsForCatalogue;
    }

    /**
     * Returns all configured catalog titles from the config file. The Jersey
     * API cannot invoke static methods, so we need this wrapper method.
     *
     * @return all catalog titles
     */
    @XmlElement(name = "interface")
    public List<String> getInterface() {
        return getAllCatalogueTitles();
    }

    /**
     * Returns all configured media types from the config file. The Jersey API
     * cannot invoke static methods, so we need this wrapper method.
     *
     * @return all media types
     */
    @XmlElement(name = "mediaType")
    public List<ConfigOpacDoctype> getMediaType() {
        return getAllDoctypes();
    }
}

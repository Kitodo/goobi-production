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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.faces.context.FacesContext;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.kitodo.exceptions.NotImplementedException;
import org.kitodo.production.webapi.beans.Label;
import org.kitodo.production.webapi.beans.Label.KeyAttribute;

public class ConfigOpacDoctype {
    private String title;
    private String rulesetType;
    private String tifHeaderType;
    private boolean periodical;
    private boolean multiVolume;
    private HashMap<String, String> labels;
    private List<String> mappings;
    private boolean newspaper;

    public ConfigOpacDoctype() {
        throw new NotImplementedException("Jersey API requires no-arg constructor which is never used");
    }

    ConfigOpacDoctype(String inTitle, String inRulesetType, String inTifHeaderType, boolean inPeriodical,
            boolean inMultiVolume, boolean newspaper, HashMap<String, String> inLabels, List<String> inMappings) {
        this.title = inTitle;
        this.rulesetType = inRulesetType;
        this.tifHeaderType = inTifHeaderType;
        this.periodical = inPeriodical;
        this.multiVolume = inMultiVolume;
        this.newspaper = newspaper;
        this.labels = inLabels;
        this.mappings = inMappings;
    }

    @XmlAttribute(name = "key")
    public String getTitle() {
        return this.title;
    }

    public String getRulesetType() {
        return this.rulesetType;
    }

    @XmlElement(name = "tiffHeaderTag")
    public String getTifHeaderType() {
        return this.tifHeaderType;
    }

    public boolean isPeriodical() {
        return this.periodical;
    }

    public boolean isMultiVolume() {
        return this.multiVolume;
    }

    public boolean isNewspaper() {
        return this.newspaper;
    }

    @XmlElement(name = "label")
    public List<Label> getLabelsForJerseyApi() {
        return Label.toListOfLabels(labels, KeyAttribute.LANGUAGE);
    }

    @XmlElement(name = "receivingValue")
    public List<String> getMappings() {
        return this.mappings;
    }

    /**
     * Get localized label.
     *
     * @return String
     */
    public String getLocalizedLabel() {
        String currentLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        if (Objects.nonNull(currentLocale) && !currentLocale.isEmpty()) {
            String answer = this.labels.get(currentLocale);
            if (Objects.nonNull(answer) && !answer.isEmpty()) {
                return answer;
            }
        }
        return this.labels.get(this.labels.keySet().iterator().next());
    }

}

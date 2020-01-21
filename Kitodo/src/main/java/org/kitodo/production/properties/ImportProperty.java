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

package org.kitodo.production.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportProperty implements IProperty {

    private String name = "";
    private Integer container = 0;
    private String validation = "";
    private Type type = Type.TEXT;
    private String value = "";
    private List<String> projects;
    private boolean required = false;

    /**
     * Constructor.
     */
    public ImportProperty() {
        this.projects = new ArrayList<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getContainer() {
        return this.container;
    }

    @Override
    public void setContainer(int container) {
        this.container = container;
    }

    @Override
    public String getValidation() {
        return this.validation;
    }

    @Override
    public void setValidation(String validation) {
        this.validation = validation;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public List<String> getProjects() {
        return this.projects;
    }

    @Override
    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    @Override
    public boolean isValid() {
        Pattern pattern = Pattern.compile(this.validation);
        Matcher matcher = pattern.matcher(this.value);
        return matcher.matches();
    }

    /**
     * Get value list.
     *
     * @return list of Strings
     */
    public List<String> getValueList() {
        String[] values = this.value.split("; ");
        return new ArrayList<>(Arrays.asList(values));
    }

    /**
     * Set value list.
     *
     * @param valueList
     *            list of Strings
     */
    public void setValueList(List<String> valueList) {
        StringBuilder valueBuilder = new StringBuilder();
        for (String val : valueList) {
            valueBuilder.append(val);
            valueBuilder.append("; ");
        }
        this.value = valueBuilder.toString();
    }

    /**
     * Get boolean value.
     *
     * @return boolean
     */
    public boolean getBooleanValue() {
        return this.value.equalsIgnoreCase("true");
    }

    /**
     * Set boolean value.
     *
     * @param val
     *            boolean
     */
    public void setBooleanValue(boolean val) {
        if (val) {
            this.value = "true";
        } else {
            this.value = "false";
        }
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}

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

package org.kitodo.production.forms.dataeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.kitodo.api.Metadata;
import org.kitodo.api.dataeditor.rulesetmanagement.StructuralElementViewInterface;
import org.kitodo.api.dataformat.IncludedStructuralElement;
import org.kitodo.api.dataformat.MediaUnit;
import org.kitodo.production.helper.Helper;

/**
 * Backing bean for the meta-data panel of the meta-data editor.
 */
public class MetadataPanel implements Serializable {

    private String addMetadataKeySelectedItem = "";

    private String addMetadataValue = "";

    private Collection<Metadata> clipboard = new ArrayList<>();

    private final RulesetSetupInterface rulesetSetup;

    private FieldedMetadataTableRow logicalMetadataTable = FieldedMetadataTableRow.EMPTY;
    private FieldedMetadataTableRow physicalMetadataTable = FieldedMetadataTableRow.EMPTY;

    MetadataPanel(RulesetSetupInterface rulesetSetup) {
        this.rulesetSetup = rulesetSetup;
    }

    // TODO create similar method for physical meta-data entries
    /**
     * The method is executed when a user clicks the add meta-data button. A new
     * meta-data entry will be created with the entered type and value. Actually
     * this procedure is not in the sense of the inventor. Especially with
     * selection types, the user must enter the coded value, which is
     * inconvenient. Nor can this procedure be transferred to meta-data groups.
     * The better approach would be to first create and display a field and then
     * give the user the option to enter or select the value.
     */
    public void addMetadataEntryClick() {
        try {
            /*
             * First, we get the translated label of the table line into which
             * the value must be entered. It could disappear from the list of
             * addable meta-data if we specify the field as additionally
             * selected and the maximum number of occurrences would be reached.
             * That's why we have to do that first.
             */
            String label = logicalMetadataTable.getAddableMetadata().parallelStream()
                    .filter(selectItem -> addMetadataKeySelectedItem.equals(selectItem.getValue())).findAny()
                    .orElseThrow(IllegalStateException::new).getLabel();

            /*
             * Then we add the meta-data to add. This will rebuild the table and
             * create an empty table line (somewhere) into which we can enter
             * the value.
             */
            logicalMetadataTable.addAdditionallySelectedField(addMetadataKeySelectedItem);

            /*
             * Now we just have to find the line and enter the value. The latter
             * happens differently depending on what kind of input field it is.
             */
            for (MetadataTableRow row : logicalMetadataTable.getRows()) {
                if (label.equals(row.getLabel())) {
                    if (row instanceof TextMetadataTableRow) {
                        TextMetadataTableRow textInput = (TextMetadataTableRow) row;
                        if (textInput.getValue().isEmpty()) {
                            textInput.setValue(addMetadataValue);
                            break;
                        }
                    } else if (row instanceof SelectMetadataTableRow) {
                        SelectMetadataTableRow selectInput = (SelectMetadataTableRow) row;
                        if (selectInput.getSelectedItem().isEmpty()) {
                            selectInput.setSelectedItem(addMetadataValue);
                            break;
                        }
                    } else if (row instanceof BooleanMetadataTableRow) {
                        BooleanMetadataTableRow booleanInput = (BooleanMetadataTableRow) row;
                        if (!booleanInput.isActive()) {
                            booleanInput.setActive(!addMetadataValue.isEmpty());
                        }
                    }
                }
            }
        } catch (NoSuchMetadataFieldException | InvalidMetadataValueException e) {
            Helper.setErrorMessage(e.getLocalizedMessage());
        }
    }

    /**
     * Empties the meta-data panel.
     */
    public void clear() {
        logicalMetadataTable = FieldedMetadataTableRow.EMPTY;
        physicalMetadataTable = FieldedMetadataTableRow.EMPTY;
        clipboard.clear();
        addMetadataKeySelectedItem = "";
        addMetadataValue = "";
    }

    /**
     * Returns the value of the addLogicalMetadata text box.
     *
     * @return the value of the addLogicalMetadata
     */
    public String getAddLogicalMetadataValue() {
        return addMetadataValue;
    }

    /**
     * Sets the value of the addMetadata text box.
     *
     * @param addMetadataValue
     *            value to set
     */
    public void setAddLogicalMetadataValue(String addMetadataValue) {
        this.addMetadataValue = addMetadataValue;
    }

    Collection<Metadata> getClipboard() {
        return clipboard;
    }

    /**
     * Returns the rows of logical meta-data that JSF has to display.
     *
     * @return the rows of logical meta-data
     */
    public List<MetadataTableRow> getLogicalMetadataRows() {
        return logicalMetadataTable.getRows();
    }

    /**
     * Returns the rows of physical meta-data that JSF has to display.
     *
     * @return the rows of physical meta-data
     */
    public List<MetadataTableRow> getPhysicalMetadataRows() {
        return physicalMetadataTable.getRows();
    }

    void showLogical(Optional<IncludedStructuralElement> optionalStructure) {
        if (optionalStructure.isPresent()) {
            StructuralElementViewInterface divisionView = rulesetSetup.getRuleset().getStructuralElementView(
                    optionalStructure.get().getType(), rulesetSetup.getAcquisitionStage(), rulesetSetup.getPriorityList());
            logicalMetadataTable = new FieldedMetadataTableRow(this, optionalStructure.get(), divisionView);
        } else {
            logicalMetadataTable = FieldedMetadataTableRow.EMPTY;
        }

    }

    void showPhysical(Optional<MediaUnit> optionalMediaUnit) {
        if (optionalMediaUnit.isPresent() && Objects.nonNull(optionalMediaUnit.get().getType())) {
            StructuralElementViewInterface divisionView = rulesetSetup.getRuleset().getStructuralElementView(
                    optionalMediaUnit.get().getType(), rulesetSetup.getAcquisitionStage(), rulesetSetup.getPriorityList());
            physicalMetadataTable = new FieldedMetadataTableRow(this, optionalMediaUnit.get().getMetadata(), divisionView);
        } else {
            physicalMetadataTable = FieldedMetadataTableRow.EMPTY;
        }

    }

    /**
     * Callback function 'paste' button in MetadataPanel. (Not yet implemented!)
     */
    public void pasteClick() {
        logicalMetadataTable.pasteClick();
    }

    void preserve() throws InvalidMetadataValueException, NoSuchMetadataFieldException {
        this.preserveLogical();
        this.preservePhysical();
    }

    void preserveLogical() throws InvalidMetadataValueException, NoSuchMetadataFieldException {
        logicalMetadataTable.preserve();
    }

    void preservePhysical() throws InvalidMetadataValueException, NoSuchMetadataFieldException {
        physicalMetadataTable.preserve();
    }
}

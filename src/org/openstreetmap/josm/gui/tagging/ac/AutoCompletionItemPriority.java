// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;

/**
 * Describes the priority of an item in an autocompletion list.
 * The selected flag is currently only used in plugins.
 *
 * Instances of this class are not modifiable.
 * @since 1762
 * @deprecated to be removed end of 2017. Use {@link AutoCompletionPriority} instead
 */
@Deprecated
public class AutoCompletionItemPriority implements Comparable<AutoCompletionItemPriority> {

    /**
     * Indicates, that the value is standard and it is found in the data.
     * This has higher priority than some arbitrary standard value that is
     * usually not used by the user.
     */
    public static final AutoCompletionItemPriority IS_IN_STANDARD_AND_IN_DATASET = new AutoCompletionItemPriority(
            AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET);

    /**
     * Indicates that this is an arbitrary value from the data set, i.e.
     * the value of a tag name=*.
     */
    public static final AutoCompletionItemPriority IS_IN_DATASET = new AutoCompletionItemPriority(AutoCompletionPriority.IS_IN_DATASET);

    /**
     * Indicates that this is a standard value, i.e. a standard tag name
     * or a standard value for a given tag name (from the presets).
     */
    public static final AutoCompletionItemPriority IS_IN_STANDARD = new AutoCompletionItemPriority(AutoCompletionPriority.IS_IN_STANDARD);

    /**
     * Indicates that this is a value from a selected object.
     */
    public static final AutoCompletionItemPriority IS_IN_SELECTION = new AutoCompletionItemPriority(AutoCompletionPriority.IS_IN_SELECTION);

    /** Unknown priority. This is the lowest priority. */
    public static final AutoCompletionItemPriority UNKNOWN = new AutoCompletionItemPriority(AutoCompletionPriority.UNKNOWN);

    private static final int NO_USER_INPUT = Integer.MAX_VALUE;

    private final AutoCompletionPriority priority;

    /**
     * Constructs a new {@code AutoCompletionItemPriority}.
     *
     * @param inDataSet true, if the item is found in the currently active data layer
     * @param inStandard true, if the item is a standard tag, e.g. from the presets
     * @param selected true, if it is found on an object that is currently selected
     * @param userInput null, if the user hasn't entered this tag so far. A number when
     * the tag key / value has been entered by the user before. A lower number means
     * this happened more recently and beats a higher number in priority.
     */
    public AutoCompletionItemPriority(boolean inDataSet, boolean inStandard, boolean selected, Integer userInput) {
        this(new AutoCompletionPriority(inDataSet, inStandard, selected, userInput));
    }

    /**
     * Constructs a new {@code AutoCompletionItemPriority}.
     *
     * @param inDataSet true, if the item is found in the currently active data layer
     * @param inStandard true, if the item is a standard tag, e.g. from the presets
     * @param selected true, if it is found on an object that is currently selected
     */
    public AutoCompletionItemPriority(boolean inDataSet, boolean inStandard, boolean selected) {
        this(inDataSet, inStandard, selected, NO_USER_INPUT);
    }

    /**
     * Constructs a new {@code AutoCompletionItemPriority} from an existing {@link AutoCompletionPriority}.
     * @param other {@code AutoCompletionPriority} to convert
     * @since 12859
     */
    public AutoCompletionItemPriority(AutoCompletionPriority other) {
        this.priority = other;
    }

    /**
     * Determines if the item is found in the currently active data layer.
     * @return {@code true} if the item is found in the currently active data layer
     */
    public boolean isInDataSet() {
        return priority.isInDataSet();
    }

    /**
     * Determines if the item is a standard tag, e.g. from the presets.
     * @return {@code true} if the item is a standard tag, e.g. from the presets
     */
    public boolean isInStandard() {
        return priority.isInStandard();
    }

    /**
     * Determines if it is found on an object that is currently selected.
     * @return {@code true} if it is found on an object that is currently selected
     */
    public boolean isSelected() {
        return priority.isSelected();
    }

    /**
     * Returns a number when the tag key / value has been entered by the user before.
     * A lower number means this happened more recently and beats a higher number in priority.
     * @return a number when the tag key / value has been entered by the user before.
     *         {@code null}, if the user hasn't entered this tag so far.
     */
    public Integer getUserInput() {
        return priority.getUserInput();
    }

    /**
     * Imposes an ordering on the priorities.
     * Currently, being in the current DataSet is worth more than being in the Presets.
     */
    @Override
    public int compareTo(AutoCompletionItemPriority other) {
        return priority.compareTo(other.priority);
    }

    /**
     * Merges two priorities.
     * The resulting priority is always &gt;= the original ones.
     * @param other other priority
     * @return the merged priority
     */
    public AutoCompletionItemPriority mergeWith(AutoCompletionItemPriority other) {
        return new AutoCompletionItemPriority(priority.mergeWith(other.priority));
    }

    @Override
    public String toString() {
        return priority.toString();
    }

    /**
     * Returns the underlying priority.
     * @return the underlying priority
     * @since 12859
     */
    public AutoCompletionPriority getPriority() {
        return priority;
    }
}

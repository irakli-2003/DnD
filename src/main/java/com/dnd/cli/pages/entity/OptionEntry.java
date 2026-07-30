package com.dnd.cli.pages.entity;

/**
 * A single selectable option shown to the user when picking a value from a
 * known set (e.g. an existing class/race/item id, or a concrete subtype).
 */
public final class OptionEntry {
    private final String value;
    private final String label;

    public OptionEntry(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}


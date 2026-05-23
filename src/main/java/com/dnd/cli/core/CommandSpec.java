package com.dnd.cli.core;

import com.dnd.cli.core.Page;

public class CommandSpec {
    private final String key;
    private final String description;
    private final Page target;
    private final CommandAction action;

    public CommandSpec(String key, String description, Page target) {
        this.key = key;
        this.description = description;
        this.target = target;
        this.action = null;
    }

    public CommandSpec(String key, String description, CommandAction action) {
        this.key = key;
        this.description = description;
        this.target = null;
        this.action = action;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Page getTarget() {
        return target;
    }

    public CommandAction getAction() {
        return action;
    }
}

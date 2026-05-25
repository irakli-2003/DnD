package com.dnd.cli.pages;

import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;

import java.util.Collections;
import java.util.List;

public class PlaceholderPage implements Page {
    private final String title;
    private final String body;
    private Page parent;

    public PlaceholderPage(String title, String body, Page parent) {
        this.title = title;
        this.body = body;
        this.parent = parent;
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public List<CommandSpec> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public Page getParent() {
        return parent;
    }
}

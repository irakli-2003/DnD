package com.dnd.cli.pages;

import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;

import java.util.Collections;
import java.util.List;

public class PlaceholderPage implements Page {
    private final String title;
    private final String body;

    public PlaceholderPage(String title, String body) {
        this.title = title;
        this.body = body;
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
}

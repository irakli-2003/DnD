package com.dnd.cli.core;

import java.util.List;

public interface Page {
    String getTitle();

    String getBody();

    List<CommandSpec> getCommands();

    default Page getParent() {
        return null;
    }
}

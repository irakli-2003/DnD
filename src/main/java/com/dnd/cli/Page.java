package com.dnd.cli;

import java.util.List;

public interface Page {
    String getTitle();

    String getBody();

    List<CommandSpec> getCommands();
}


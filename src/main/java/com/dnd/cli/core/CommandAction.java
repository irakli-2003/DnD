package com.dnd.cli.core;

import com.dnd.cli.core.Page;

@FunctionalInterface
public interface CommandAction {
    Page execute(CliSession session);
}

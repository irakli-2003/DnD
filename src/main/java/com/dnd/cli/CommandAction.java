package com.dnd.cli;

@FunctionalInterface
public interface CommandAction {
    Page execute(CliSession session);
}


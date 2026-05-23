package com.dnd.cli.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CommandResolverTest {
    private static final Page DUMMY_PAGE = new Page() {
        @Override
        public String getTitle() {
            return "Dummy";
        }

        @Override
        public String getBody() {
            return "Dummy";
        }

        @Override
        public List<CommandSpec> getCommands() {
            return Arrays.asList();
        }
    };

    @Test
    public void resolvesUniquePrefixCaseInsensitive() {
        List<CommandSpec> commands = Arrays.asList(
            new CommandSpec("create", "", DUMMY_PAGE),
            new CommandSpec("delete", "", DUMMY_PAGE)
        );

        CommandResolver.Result result = CommandResolver.resolve(" C ", commands);
        assertEquals(CommandResolver.ResultType.NAVIGATE, result.getType());
        assertNotNull(result.getCommand());
        assertEquals(DUMMY_PAGE, result.getCommand().getTarget());
    }

    @Test
    public void rejectsAmbiguousPrefix() {
        List<CommandSpec> commands = Arrays.asList(
            new CommandSpec("edit", "", DUMMY_PAGE),
            new CommandSpec("exit", "", DUMMY_PAGE)
        );

        CommandResolver.Result result = CommandResolver.resolve("e", commands);
        assertEquals(CommandResolver.ResultType.INVALID, result.getType());
    }

    @Test
    public void resolvesExit() {
        List<CommandSpec> commands = Arrays.asList(
            new CommandSpec("edit", "", DUMMY_PAGE)
        );

        CommandResolver.Result result = CommandResolver.resolve("ex", commands);
        assertEquals(CommandResolver.ResultType.EXIT, result.getType());
    }

    @Test
    public void resolvesBackAliases() {
        List<CommandSpec> commands = Arrays.asList(
            new CommandSpec("edit", "", DUMMY_PAGE)
        );

        CommandResolver.Result result = CommandResolver.resolve("b", commands);
        assertEquals(CommandResolver.ResultType.BACK, result.getType());
    }
}

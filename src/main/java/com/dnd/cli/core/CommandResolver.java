package com.dnd.cli.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandResolver {
    public enum ResultType {
        NAVIGATE,
        BACK,
        EXIT,
        INVALID
    }

    public static final class Result {
        private final ResultType type;
        private final CommandSpec command;

        private Result(ResultType type, CommandSpec command) {
            this.type = type;
            this.command = command;
        }

        public ResultType getType() {
            return type;
        }

        public CommandSpec getCommand() {
            return command;
        }
    }

    private CommandResolver() {
    }

    public static Result resolve(String input, List<CommandSpec> commands) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return new Result(ResultType.INVALID, null);
        }

        if (normalized.equals("back") || normalized.equals("b")) {
            return new Result(ResultType.BACK, null);
        }

        boolean exitMatches = "exit".startsWith(normalized);
        List<CommandSpec> matches = new ArrayList<>();
        for (CommandSpec command : commands) {
            String key = normalize(command.getKey());
            if (key.startsWith(normalized)) {
                matches.add(command);
            }
        }

        int matchCount = matches.size() + (exitMatches ? 1 : 0);
        if (matchCount == 0) {
            return new Result(ResultType.INVALID, null);
        }
        if (matchCount > 1) {
            return new Result(ResultType.INVALID, null);
        }

        if (exitMatches) {
            return new Result(ResultType.EXIT, null);
        }

        return new Result(ResultType.NAVIGATE, matches.get(0));
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }
}


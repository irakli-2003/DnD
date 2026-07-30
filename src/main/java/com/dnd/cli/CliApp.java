package com.dnd.cli;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandResolver;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.ConsoleIO;
import com.dnd.cli.core.Page;
import com.dnd.data.DataAccessException;

import java.util.List;

public class CliApp {
    public static void main(String[] args) {
        AppContext context;
        try {
            context = AppContext.build();
        } catch (DataAccessException e) {
            System.out.println("Failed to initialize campaign storage: " + e.getMessage());
            return;
        }

        runLoop(context.getLandingPage(), context.getSession());
    }

    private static void runLoop(Page landingPage, CliSession session) {
        Page current = landingPage;
        ConsoleIO console = session.getConsole();

        while (true) {
            boolean canGoBack = current.getParent() != null;
            renderPage(console, current, canGoBack);

            String input = console.readLine();
            CommandResolver.Result result = CommandResolver.resolve(input, current.getCommands());

            if (result.getType() == CommandResolver.ResultType.INVALID) {
                console.println("invalid command");
                continue;
            }
            if (result.getType() == CommandResolver.ResultType.EXIT) {
                console.println("Exiting.");
                break;
            }
            if (result.getType() == CommandResolver.ResultType.BACK) {
                if (canGoBack) {
                    current = current.getParent();
                }
                continue;
            }

            CommandSpec command = result.getCommand();
            if (command == null) {
                console.println("invalid command");
                continue;
            }

            if (command.getAction() != null) {
                Page target;
                try {
                    target = command.getAction().execute(session);
                } catch (DataAccessException | IllegalArgumentException | IllegalStateException e) {
                    // Boundary for otherwise-uncaught data/validation failures bubbling up from the
                    // data or model layers, so a single bad campaign file doesn't crash the whole CLI.
                    console.println("Action failed: " + e.getMessage());
                    continue;
                }
                if (target != null) {
                    current = target;
                }
                continue;
            }

            Page target = command.getTarget();
            if (target != null) {
                current = target;
            }
        }
    }

    private static void renderPage(ConsoleIO console, Page page, boolean canGoBack) {
        console.println();
        console.println("==================================================");
        console.println(page.getTitle());
        console.println(page.getBody());
        console.println();
        console.println("Commands:");

        List<CommandSpec> commands = page.getCommands();
        for (CommandSpec command : commands) {
            console.println("- " + command.getKey() + " : " + command.getDescription());
        }

        console.println("- exit : Exit program");
        if (canGoBack) {
            console.println("- back (b) : Go to previous page");
        } else {
            console.println("- back (b) : No previous page");
        }
        console.println("==================================================");
        console.println();
    }
}

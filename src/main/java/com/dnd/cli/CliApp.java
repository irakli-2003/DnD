package com.dnd.cli;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandResolver;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.cli.pages.CampaignSelectionPage;
import com.dnd.cli.pages.CreateCampaignPage;
import com.dnd.cli.pages.DmMenuPage;
import com.dnd.cli.pages.LandingPage;
import com.dnd.cli.pages.PlaceholderPage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

public class CliApp {
    public static void main(String[] args) {
        CampaignStorage storage = new CampaignStorage();
        try {
            storage.ensureInitialized();
        } catch (IOException e) {
            System.out.println("Failed to initialize campaign storage: " + e.getMessage());
        }

        Scanner scanner = new Scanner(System.in);
        CliSession session = new CliSession(storage, scanner);

        PlaceholderPage playerPage = new PlaceholderPage(
            "Player Mode",
            "Player mode is not implemented yet."
        );
        PlaceholderPage createPage = new PlaceholderPage(
            "Create Content",
            "Create new content is not implemented yet."
        );
        PlaceholderPage editPage = new PlaceholderPage(
            "Edit Content",
            "Edit existing content is not implemented yet."
        );
        PlaceholderPage deletePage = new PlaceholderPage(
            "Delete Content",
            "Delete existing content is not implemented yet."
        );
        PlaceholderPage openPage = new PlaceholderPage(
            "Open Content",
            "Open existing content is not implemented yet."
        );

        DmMenuPage dmMenuPage = new DmMenuPage(createPage, editPage, deletePage, openPage, session);
        CreateCampaignPage createCampaignPage = new CreateCampaignPage(session, storage, dmMenuPage);
        CampaignSelectionPage campaignSelectionPage = new CampaignSelectionPage(session, storage, createCampaignPage, dmMenuPage);
        LandingPage landingPage = new LandingPage(campaignSelectionPage, playerPage);

        runLoop(landingPage, session);
    }

    private static void runLoop(Page landingPage, CliSession session) {
        Page current = landingPage;
        Deque<Page> history = new ArrayDeque<>();

        Scanner scanner = session.getScanner();
        while (true) {
            boolean canGoBack = !history.isEmpty();
            renderPage(current, canGoBack);

            String input = scanner.nextLine();
            CommandResolver.Result result = CommandResolver.resolve(input, current.getCommands());

            if (result.getType() == CommandResolver.ResultType.INVALID) {
                System.out.println("invalid command");
                continue;
            }
            if (result.getType() == CommandResolver.ResultType.EXIT) {
                System.out.println("Exiting.");
                break;
            }
            if (result.getType() == CommandResolver.ResultType.BACK) {
                if (canGoBack) {
                    current = history.pop();
                }
                continue;
            }

            CommandSpec command = result.getCommand();
            if (command == null) {
                System.out.println("invalid command");
                continue;
            }

            if (command.getAction() != null) {
                Page target = command.getAction().execute(session);
                if (target != null) {
                    history.push(current);
                    current = target;
                }
                continue;
            }

            Page target = command.getTarget();
            if (target != null) {
                history.push(current);
                current = target;
            }
        }
    }

    private static void renderPage(Page page, boolean canGoBack) {
        System.out.println();
        System.out.println(page.getTitle());
        System.out.println(page.getBody());
        System.out.println();
        System.out.println("Commands:");

        List<CommandSpec> commands = page.getCommands();
        for (CommandSpec command : commands) {
            System.out.println("- " + command.getKey() + " : " + command.getDescription());
        }

        System.out.println("- exit : Exit program");
        if (canGoBack) {
            System.out.println("- back (b) : Go to previous page");
        } else {
            System.out.println("- back (b) : No previous page");
        }
        System.out.println();
    }
}

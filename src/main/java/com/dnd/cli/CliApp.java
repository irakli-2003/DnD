package com.dnd.cli;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandResolver;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.cli.pages.CampaignSelectionPage;
import com.dnd.cli.pages.CreateCampaignPage;
import com.dnd.cli.pages.DmMenuPage;
import com.dnd.cli.pages.EntitySelectionPage;
import com.dnd.cli.pages.LandingPage;
import com.dnd.cli.pages.PlaceholderPage;

import java.io.IOException;
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
            "Player mode is not implemented yet.",
            null
        );

        EntitySelectionPage createPage = new EntitySelectionPage(EntitySelectionPage.Operation.CREATE, null);
        EntitySelectionPage editPage = new EntitySelectionPage(EntitySelectionPage.Operation.EDIT, null);
        EntitySelectionPage deletePage = new EntitySelectionPage(EntitySelectionPage.Operation.DELETE, null);
        EntitySelectionPage openPage = new EntitySelectionPage(EntitySelectionPage.Operation.OPEN, null);
        DmMenuPage dmMenuPage = new DmMenuPage(createPage, editPage, deletePage, openPage, session, null);
        CreateCampaignPage createCampaignPage = new CreateCampaignPage(session, storage, dmMenuPage, null);
        CampaignSelectionPage campaignSelectionPage = new CampaignSelectionPage(storage, createCampaignPage, dmMenuPage, null);
        LandingPage landingPage = new LandingPage(campaignSelectionPage, playerPage);

        playerPage.setParent(landingPage);
        campaignSelectionPage.setParent(landingPage);
        createCampaignPage.setParent(campaignSelectionPage);
        dmMenuPage.setParent(campaignSelectionPage);
        createPage.setParent(dmMenuPage);
        editPage.setParent(dmMenuPage);
        deletePage.setParent(dmMenuPage);
        openPage.setParent(dmMenuPage);

        runLoop(landingPage, session);
    }

    private static void runLoop(Page landingPage, CliSession session) {
        Page current = landingPage;

        Scanner scanner = session.getScanner();
        while (true) {
            boolean canGoBack = current.getParent() != null;
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
                    current = current.getParent();
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

    private static void renderPage(Page page, boolean canGoBack) {
        System.out.println();
        System.out.println("==================================================");
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
        System.out.println("==================================================");
        System.out.println();
    }
}

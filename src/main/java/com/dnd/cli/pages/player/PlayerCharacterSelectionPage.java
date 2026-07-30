package com.dnd.cli.pages.player;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.ConsoleIO;
import com.dnd.cli.core.Page;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.DataAccessException;
import com.dnd.model.character.PlayerCharacter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user pick which {@link PlayerCharacter} in the active campaign
 * they're playing this session. One command per character - same pattern as
 * {@link com.dnd.cli.pages.CampaignSelectionPage}'s per-campaign commands.
 */
public class PlayerCharacterSelectionPage implements Page {
    private final CliSession session;
    private final Page homePage;
    private Page parent;

    public PlayerCharacterSelectionPage(CliSession session, Page homePage, Page parent) {
        this.session = session;
        this.homePage = homePage;
        this.parent = parent;
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public String getTitle() {
        return "Player Mode - Choose Character";
    }

    @Override
    public String getBody() {
        if (session.getCampaignContext() == null) {
            return "No campaign selected.";
        }
        return "Campaign: " + session.getCampaignContext().getName() + "\nChoose which character you are playing.";
    }

    @Override
    public List<CommandSpec> getCommands() {
        List<CommandSpec> commands = new ArrayList<>();
        CampaignRepositories repos = PlayerModeSupport.repositoriesFor(session);
        if (repos == null) {
            return commands;
        }

        List<PlayerCharacter> players;
        try {
            players = repos.players().list();
        } catch (DataAccessException e) {
            return commands;
        }

        for (PlayerCharacter pc : players) {
            String label = pc.getName() != null && !pc.getName().isEmpty() ? pc.getName() : pc.getId();
            String characterId = pc.getId();
            commands.add(new CommandSpec(label, "Play as " + label, selectedSession -> {
                ConsoleIO console = selectedSession.getConsole();
                if (pc.hasPassword()) {
                    console.print("Enter password for " + label + ": ");
                    String attempt = console.readLine();
                    if (!pc.checkPassword(attempt)) {
                        console.println("Incorrect password.");
                        return this;
                    }
                }
                selectedSession.setActivePlayerCharacterId(characterId);
                return homePage;
            }));
        }
        return commands;
    }

    @Override
    public Page getParent() {
        return parent;
    }
}


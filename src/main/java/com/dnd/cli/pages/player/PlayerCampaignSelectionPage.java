package com.dnd.cli.pages.player;

import com.dnd.cli.core.CampaignContext;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.cli.storage.CampaignStorage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Player-mode campaign picker. Read-only: unlike the DM's
 * {@link com.dnd.cli.pages.CampaignSelectionPage}, players don't
 * create/rename/delete campaigns - they just pick which one their
 * character belongs to.
 */
public class PlayerCampaignSelectionPage implements Page {
    private final CampaignStorage storage;
    private final Page characterSelectionPage;
    private Page parent;

    public PlayerCampaignSelectionPage(CampaignStorage storage, Page characterSelectionPage, Page parent) {
        this.storage = storage;
        this.characterSelectionPage = characterSelectionPage;
        this.parent = parent;
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public String getTitle() {
        return "Player Mode - Choose Campaign";
    }

    @Override
    public String getBody() {
        return "Choose the campaign your character belongs to.";
    }

    @Override
    public List<CommandSpec> getCommands() {
        List<CommandSpec> commands = new ArrayList<>();
        List<String> campaigns = storage.listCustomCampaigns();
        for (String campaign : campaigns) {
            commands.add(new CommandSpec(campaign, "Enter campaign", session -> {
                Path path = storage.resolveCustomCampaignPath(campaign);
                session.setCampaignContext(new CampaignContext(campaign, path));
                return characterSelectionPage;
            }));
        }
        return commands;
    }

    @Override
    public Page getParent() {
        return parent;
    }
}


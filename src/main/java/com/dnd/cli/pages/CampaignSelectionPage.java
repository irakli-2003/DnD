package com.dnd.cli.pages;

import com.dnd.cli.core.CampaignContext;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CampaignSelectionPage implements Page {
    private final CliSession session;
    private final CampaignStorage storage;
    private final Page createCampaignPage;
    private final Page dmMenuPage;

    public CampaignSelectionPage(CliSession session, CampaignStorage storage, Page createCampaignPage, Page dmMenuPage) {
        this.session = session;
        this.storage = storage;
        this.createCampaignPage = createCampaignPage;
        this.dmMenuPage = dmMenuPage;
    }

    @Override
    public String getTitle() {
        return "Campaign Selection";
    }

    @Override
    public String getBody() {
        return "Choose a campaign to run or create a new one.";
    }

    @Override
    public List<CommandSpec> getCommands() {
        List<CommandSpec> commands = new ArrayList<>();
        commands.add(new CommandSpec("create", "Create a new campaign", createCampaignPage));

        List<String> campaigns = storage.listCustomCampaigns();
        for (String campaign : campaigns) {
            commands.add(new CommandSpec(campaign, "Open campaign", selectedSession -> {
                Path path = storage.resolveCustomCampaignPath(campaign);
                selectedSession.setCampaignContext(new CampaignContext(campaign, path));
                return dmMenuPage;
            }));
        }

        return commands;
    }
}

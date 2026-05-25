package com.dnd.cli.pages;

import com.dnd.cli.core.CampaignContext;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CreateCampaignPage implements Page {
    private final CliSession session;
    private final CampaignStorage storage;
    private final Page dmMenuPage;
    private Page parent;

    public CreateCampaignPage(CliSession session, CampaignStorage storage, Page dmMenuPage, Page parent) {
        this.session = session;
        this.storage = storage;
        this.dmMenuPage = dmMenuPage;
        this.parent = parent;
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public String getTitle() {
        return "Create Campaign";
    }

    @Override
    public String getBody() {
        return "Choose how to create the campaign.";
    }

    @Override
    public List<CommandSpec> getCommands() {
        return Arrays.asList(
            new CommandSpec("default", "Copy the default campaign", selectedSession -> createCampaign(selectedSession, false)),
            new CommandSpec("blank", "Create a blank campaign", selectedSession -> createCampaign(selectedSession, true))
        );
    }

    @Override
    public Page getParent() {
        return parent;
    }

    private Page createCampaign(CliSession selectedSession, boolean blank) {
        System.out.print("Enter campaign name: ");
        String name = selectedSession.getScanner().nextLine();

        try {
            CampaignContext context = storage.createCampaignFromDefault(name, blank);
            selectedSession.setCampaignContext(context);
            System.out.println("Created campaign: " + context.getName());
            return dmMenuPage;
        } catch (IOException e) {
            System.out.println("Failed to create campaign: " + e.getMessage());
            return this;
        }
    }
}

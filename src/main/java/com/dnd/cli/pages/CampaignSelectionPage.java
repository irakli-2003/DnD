package com.dnd.cli.pages;

import com.dnd.cli.core.CampaignContext;
import com.dnd.cli.core.ConsoleIO;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.data.DataAccessException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CampaignSelectionPage implements Page {
    private final CampaignStorage storage;
    private final Page createCampaignPage;
    private final Page dmMenuPage;
    private Page parent;

    public CampaignSelectionPage(CampaignStorage storage, Page createCampaignPage, Page dmMenuPage, Page parent) {
        this.storage = storage;
        this.createCampaignPage = createCampaignPage;
        this.dmMenuPage = dmMenuPage;
        this.parent = parent;
    }

    public void setParent(Page parent) {
        this.parent = parent;
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
        commands.add(new CommandSpec("rename", "Rename a campaign", this::renameCampaign));
        commands.add(new CommandSpec("delete", "Delete a campaign", this::deleteCampaign));

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

    @Override
    public Page getParent() {
        return parent;
    }

    private Page renameCampaign(CliSession selectedSession) {
        ConsoleIO console = selectedSession.getConsole();
        List<String> campaigns = storage.listCustomCampaigns();
        if (campaigns.isEmpty()) {
            console.println("No campaigns available to rename.");
            return this;
        }

        console.println("Available campaigns:");
        for (String campaign : campaigns) {
            console.println("- " + campaign);
        }

        console.print("Enter campaign name to rename: ");
        String input = console.readLine();
        String currentName = resolveCampaignName(input, campaigns, true);
        if (currentName == null) {
            console.println("Unknown campaign: " + input.trim());
            return this;
        }

        console.print("Enter new campaign name: ");
        String newInput = console.readLine();
        String requestedName = storage.normalizeCampaignName(newInput);
        if (requestedName.isEmpty()) {
            console.println("Rename cancelled.");
            return this;
        }

        try {
            String updatedName = storage.renameCampaign(currentName, requestedName);
            console.println("Renamed campaign to: " + updatedName);
            CampaignContext context = selectedSession.getCampaignContext();
            if (context != null && context.getName().equals(currentName)) {
                selectedSession.setCampaignContext(new CampaignContext(updatedName, storage.resolveCustomCampaignPath(updatedName)));
            }
        } catch (DataAccessException | IllegalArgumentException e) {
            console.println("Failed to rename campaign: " + e.getMessage());
        }

        return this;
    }

    private Page deleteCampaign(CliSession selectedSession) {
        ConsoleIO console = selectedSession.getConsole();
        List<String> campaigns = storage.listCustomCampaigns();
        if (campaigns.isEmpty()) {
            console.println("No campaigns available to delete.");
            return this;
        }

        console.println("Available campaigns:");
        for (String campaign : campaigns) {
            console.println("- " + campaign);
        }

        console.print("Enter campaign name to delete: ");
        String input = console.readLine();
        String name = resolveCampaignName(input, campaigns, false);
        if (name == null) {
            console.println("Unknown campaign: " + input.trim());
            return this;
        }

        console.print("Type DELETE to finalize: ");
        String secondConfirm = console.readLine().trim();

        if (!"DELETE".equals(secondConfirm)) {
            console.println("Delete cancelled.");
            return this;
        }

        try {
            boolean deleted = storage.deleteCampaign(name);
            if (deleted) {
                console.println("Deleted campaign: " + name);
                CampaignContext context = selectedSession.getCampaignContext();
                if (context != null && context.getName().equals(name)) {
                    selectedSession.setCampaignContext(null);
                }
            } else {
                console.println("Failed to delete campaign: " + name);
            }
        } catch (DataAccessException e) {
            console.println("Failed to delete campaign: " + e.getMessage());
        }

        return this;
    }

    private String resolveCampaignName(String input, List<String> campaigns, boolean allowPrefix) {
        String normalized = storage.normalizeCampaignName(input);
        if (normalized.isEmpty()) {
            return null;
        }
        for (String campaign : campaigns) {
            if (campaign.equals(normalized)) {
                return campaign;
            }
        }
        if (!allowPrefix) {
            return null;
        }
        String match = null;
        for (String campaign : campaigns) {
            if (campaign.startsWith(normalized)) {
                if (match != null) {
                    return null;
                }
                match = campaign;
            }
        }
        return match;
    }
}

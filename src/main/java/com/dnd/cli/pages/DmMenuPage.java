package com.dnd.cli.pages;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;

import java.util.Arrays;
import java.util.List;

public class DmMenuPage implements Page {
    private final Page createPage;
    private final Page editPage;
    private final Page deletePage;
    private final Page openPage;
    private final CliSession session;

    public DmMenuPage(Page createPage, Page editPage, Page deletePage, Page openPage, CliSession session) {
        this.createPage = createPage;
        this.editPage = editPage;
        this.deletePage = deletePage;
        this.openPage = openPage;
        this.session = session;
    }

    @Override
    public String getTitle() {
        return "Dungeon Master";
    }

    @Override
    public String getBody() {
        String campaignName = session.getCampaignContext() == null ? "(no campaign selected)" : session.getCampaignContext().getName();
        return "Campaign: " + campaignName + "\nChoose an option:";
    }

    @Override
    public List<CommandSpec> getCommands() {
        return Arrays.asList(
            new CommandSpec("create", "Create new content", createPage),
            new CommandSpec("edit", "Edit existing content", editPage),
            new CommandSpec("delete", "Delete existing content", deletePage),
            new CommandSpec("open", "Open existing content", openPage)
        );
    }
}

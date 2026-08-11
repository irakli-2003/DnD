package com.dnd.cli;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.Page;
import com.dnd.cli.core.SystemConsoleIO;
import com.dnd.cli.pages.CampaignSelectionPage;
import com.dnd.cli.pages.CreateCampaignPage;
import com.dnd.cli.pages.DmMenuPage;
import com.dnd.cli.pages.EntitySelectionPage;
import com.dnd.cli.pages.LandingPage;
import com.dnd.cli.pages.OnlineSessionPage;
import com.dnd.cli.pages.player.PlayerOnlineSessionPage;
import com.dnd.cli.pages.player.PlayerCampaignSelectionPage;
import com.dnd.cli.pages.player.PlayerCharacterSelectionPage;
import com.dnd.cli.pages.player.PlayerHomePage;
import com.dnd.cli.storage.CampaignStorage;

import java.util.Scanner;

/**
 * Assembles the application's object graph (pages, session, storage) in one place.
 * This keeps {@link CliApp#main} focused on running the program instead of wiring it,
 * acting as a lightweight substitute for a full dependency-injection framework.
 */
public final class AppContext {
    private final CliSession session;
    private final Page landingPage;

    private AppContext(CliSession session, Page landingPage) {
        this.session = session;
        this.landingPage = landingPage;
    }

    public CliSession getSession() {
        return session;
    }

    public Page getLandingPage() {
        return landingPage;
    }

    public static AppContext build() {
        CampaignStorage storage = new CampaignStorage();
        storage.ensureInitialized();

        CliSession session = new CliSession(storage, new SystemConsoleIO(new Scanner(System.in)));

        PlayerOnlineSessionPage playerOnlinePage = new PlayerOnlineSessionPage(session, null);
        PlayerHomePage playerHomePage = new PlayerHomePage(session, playerOnlinePage, null);
        playerOnlinePage.setParent(playerHomePage);
        PlayerCharacterSelectionPage playerCharacterSelectionPage = new PlayerCharacterSelectionPage(session, playerHomePage, null);
        PlayerCampaignSelectionPage playerCampaignSelectionPage = new PlayerCampaignSelectionPage(storage, playerCharacterSelectionPage, null);

        EntitySelectionPage createPage = new EntitySelectionPage(EntitySelectionPage.Operation.CREATE, null);
        EntitySelectionPage editPage = new EntitySelectionPage(EntitySelectionPage.Operation.EDIT, null);
        EntitySelectionPage deletePage = new EntitySelectionPage(EntitySelectionPage.Operation.DELETE, null);
        EntitySelectionPage openPage = new EntitySelectionPage(EntitySelectionPage.Operation.OPEN, null);
        OnlineSessionPage onlineSessionPage = new OnlineSessionPage(session, null);
        DmMenuPage dmMenuPage = new DmMenuPage(createPage, editPage, deletePage, openPage, onlineSessionPage, session, null);
        onlineSessionPage.setParent(dmMenuPage);
        CreateCampaignPage createCampaignPage = new CreateCampaignPage(session, storage, dmMenuPage, null);
        CampaignSelectionPage campaignSelectionPage = new CampaignSelectionPage(storage, createCampaignPage, dmMenuPage, null);
        LandingPage landingPage = new LandingPage(campaignSelectionPage, playerCampaignSelectionPage);

        playerCampaignSelectionPage.setParent(landingPage);
        playerCharacterSelectionPage.setParent(playerCampaignSelectionPage);
        playerHomePage.setParent(playerCharacterSelectionPage);
        campaignSelectionPage.setParent(landingPage);
        createCampaignPage.setParent(campaignSelectionPage);
        dmMenuPage.setParent(campaignSelectionPage);
        createPage.setParent(dmMenuPage);
        editPage.setParent(dmMenuPage);
        deletePage.setParent(dmMenuPage);
        openPage.setParent(dmMenuPage);

        return new AppContext(session, landingPage);
    }
}


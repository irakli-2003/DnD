package com.dnd.cli.core;

import com.dnd.cli.storage.CampaignStorage;

public class CliSession {
    private final CampaignStorage storage;
    private final ConsoleIO console;
    private CampaignContext campaignContext;
    /**
     * Id of the {@link com.dnd.model.character.PlayerCharacter} the current
     * player-mode user is playing, once selected via
     * {@code PlayerCharacterSelectionPage}. Unused in DM mode.
     */
    private String activePlayerCharacterId;

    public CliSession(CampaignStorage storage, ConsoleIO console) {
        this.storage = storage;
        this.console = console;
    }

    public CampaignStorage getStorage() {
        return storage;
    }

    public ConsoleIO getConsole() {
        return console;
    }

    public CampaignContext getCampaignContext() {
        return campaignContext;
    }

    public void setCampaignContext(CampaignContext campaignContext) {
        this.campaignContext = campaignContext;
    }

    public String getActivePlayerCharacterId() {
        return activePlayerCharacterId;
    }

    public void setActivePlayerCharacterId(String activePlayerCharacterId) {
        this.activePlayerCharacterId = activePlayerCharacterId;
    }
}

package com.dnd.cli;

import com.dnd.cli.storage.CampaignStorage;

import java.util.Scanner;

public class CliSession {
    private final CampaignStorage storage;
    private final Scanner scanner;
    private CampaignContext campaignContext;

    public CliSession(CampaignStorage storage, Scanner scanner) {
        this.storage = storage;
        this.scanner = scanner;
    }

    public CampaignStorage getStorage() {
        return storage;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public CampaignContext getCampaignContext() {
        return campaignContext;
    }

    public void setCampaignContext(CampaignContext campaignContext) {
        this.campaignContext = campaignContext;
    }
}


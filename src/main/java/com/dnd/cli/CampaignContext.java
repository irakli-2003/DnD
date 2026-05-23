package com.dnd.cli;

import java.nio.file.Path;

public class CampaignContext {
    private final String name;
    private final Path path;

    public CampaignContext(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }
}


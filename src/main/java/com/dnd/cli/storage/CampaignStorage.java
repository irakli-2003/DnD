package com.dnd.cli.storage;

import com.dnd.cli.core.CampaignContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CampaignStorage {
    private static final Path RESOURCE_DATA_ROOT = Paths.get("src", "main", "resources", "data");

    private final Path dataRoot;
    private final Path defaultCampaignRoot;
    private final Path customCampaignRoot;

    public CampaignStorage() {
        this(resolveDefaultRoot());
    }

    public CampaignStorage(Path dataRoot) {
        this.dataRoot = dataRoot;
        this.defaultCampaignRoot = dataRoot.resolve("default-campaign");
        this.customCampaignRoot = dataRoot.resolve("custom-campaigns");
    }

    private static Path resolveDefaultRoot() {
        if (Files.exists(RESOURCE_DATA_ROOT)) {
            return RESOURCE_DATA_ROOT;
        }
        return Paths.get("data");
    }

    public Path getDefaultCampaignRoot() {
        return defaultCampaignRoot;
    }

    public Path getCustomCampaignRoot() {
        return customCampaignRoot;
    }

    public void ensureInitialized() throws IOException {
        Files.createDirectories(dataRoot);
        Files.createDirectories(customCampaignRoot);
        if (!Files.exists(defaultCampaignRoot)) {
            Files.createDirectories(defaultCampaignRoot);
            CampaignTemplate.writeTemplate(defaultCampaignRoot, false);
        }
    }

    public List<String> listCustomCampaigns() {
        if (!Files.exists(customCampaignRoot)) {
            return Collections.emptyList();
        }
        try {
            return Files.list(customCampaignRoot)
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Path resolveCustomCampaignPath(String name) {
        return customCampaignRoot.resolve(name);
    }

    public CampaignContext createCampaignFromDefault(String requestedName, boolean blank) throws IOException {
        ensureInitialized();

        String name = sanitizeName(requestedName);
        if (name.isEmpty()) {
            name = "campaign";
        }
        String uniqueName = ensureUniqueName(name);

        Path campaignPath = customCampaignRoot.resolve(uniqueName);
        Files.createDirectories(campaignPath);
        CampaignTemplate.writeTemplate(campaignPath, blank);

        return new CampaignContext(uniqueName, campaignPath);
    }

    private String ensureUniqueName(String baseName) {
        List<String> taken = new ArrayList<>(listCustomCampaigns());
        if (!taken.contains(baseName)) {
            return baseName;
        }
        int index = 2;
        while (taken.contains(baseName + "-" + index)) {
            index++;
        }
        return baseName + "-" + index;
    }

    private String sanitizeName(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        String sanitized = trimmed.replaceAll("[^a-z0-9\\-_]+", "-");
        return sanitized.replaceAll("(^-+|-+$)", "");
    }
}

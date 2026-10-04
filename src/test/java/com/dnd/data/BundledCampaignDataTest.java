package com.dnd.data;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/** Every bundled campaign must load cleanly - e.g. no leftover merge-conflict markers in its JSON. */
public class BundledCampaignDataTest {

    private static final Path CAMPAIGNS = Paths.get("src", "main", "resources", "data", "custom-campaigns");

    @Test
    public void jsonFilesHaveNoMergeConflictMarkers() throws IOException {
        try (Stream<Path> files = Files.walk(CAMPAIGNS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    assertFalse("Merge conflict left in " + file,
                        line.startsWith("<<<<<<<") || line.startsWith(">>>>>>>") || line.equals("======="));
                }
            }
        }
    }

    @Test
    public void everyRepositoryLoads() throws IOException {
        List<Path> campaigns;
        try (Stream<Path> dirs = Files.list(CAMPAIGNS)) {
            campaigns = dirs.filter(Files::isDirectory).toList();
        }
        assertFalse(campaigns.isEmpty());
        for (Path campaign : campaigns) {
            CampaignRepositories repos = new CampaignRepositories(campaign);
            String name = campaign.getFileName().toString();
            assertNotNull(name, repos.players().list());
            assertNotNull(name, repos.classes().list());
            assertNotNull(name, repos.races().list());
            assertNotNull(name, repos.items().list());
            assertNotNull(name, repos.spells().list());
            assertNotNull(name, repos.places().list());
            assertNotNull(name, repos.effects().list());
            assertNotNull(name, repos.damageTypes().list());
            assertNotNull(name, repos.npcs().list());
            assertNotNull(name, repos.monsters().list());
            assertNotNull(name, repos.beasts().list());
            assertNotNull(name, repos.languages().list());
            assertNotNull(name, repos.alchemyIngredients().list());
            assertNotNull(name, repos.books().list());
            assertNotNull(name, repos.dice().list());
            assertNotNull(name, repos.maps().list());
        }
    }
}

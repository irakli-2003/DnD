package com.dnd.data;

import com.dnd.model.world.Place;
import com.dnd.ui.EntityCategory;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class EntityInfoFormatterTest {

    private EntityInfoFormatter newFormatter() throws IOException {
        Path campaignRoot = Files.createTempDirectory("dnd-formatter");
        return new EntityInfoFormatter(new CampaignRepositories(campaignRoot));
    }

    @Test
    public void humanizeSplitsCamelCaseIntoTitleWords() {
        assertEquals("Damage Type", EntityInfoFormatter.humanize("damageType"));
        assertEquals("Name", EntityInfoFormatter.humanize("name"));
        assertEquals("", EntityInfoFormatter.humanize(null));
    }

    @Test
    public void nameOfPrefersNameThenFallsBackToId() {
        Place named = new Place();
        named.setId("plc-1");
        named.setName("Old Mill");
        assertEquals("Old Mill", EntityInfoFormatter.nameOf(named));

        Place unnamed = new Place();
        unnamed.setId("plc-2");
        assertEquals("plc-2", EntityInfoFormatter.nameOf(unnamed));

        assertEquals("(unnamed)", EntityInfoFormatter.nameOf(null));
    }

    @Test
    public void formatProducesHeaderAndSkipsInternalAndEmptyFields() throws IOException {
        EntityInfoFormatter formatter = newFormatter();
        Place item = new Place();
        item.setId("plc-1");
        item.setName("Old Mill");
        item.setDescription("A derelict watermill.");

        String text = formatter.format(EntityCategory.PLACE, item);

        assertTrue(text.startsWith("[Place: Old Mill]"));
        assertTrue(text.contains("Description: A derelict watermill."));
        // The internal id is plumbing, and the name is already in the header.
        assertFalse(text.contains("Id: plc-1"));
        assertFalse(text.contains("Name: Old Mill"));
    }

    @Test
    public void formatOfNullEntityIsEmpty() throws IOException {
        assertEquals("", newFormatter().format(EntityCategory.PLACE, null));
    }

    @Test
    public void everyInsertableCategoryHasALabelAndIsListable() throws IOException {
        EntityInfoFormatter formatter = newFormatter();
        for (EntityCategory category : EntityInfoFormatter.insertableCategories()) {
            assertNotNull(EntityInfoFormatter.categoryLabel(category));
            assertFalse(EntityInfoFormatter.categoryLabel(category).isBlank());
            // An empty temp campaign yields empty lists, but the call must not blow up.
            assertNotNull(formatter.list(category));
        }
    }

    @Test
    public void everyCategoryEnumValueHasALabel() {
        for (EntityCategory category : EntityCategory.values()) {
            assertNotNull(EntityInfoFormatter.categoryLabel(category));
        }
    }
}

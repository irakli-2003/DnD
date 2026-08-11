package com.dnd.model.item;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ItemTest {
    @Test
    public void acceptsNonNegativeValueGold() {
        TestItem item = new TestItem();
        item.setValueGold(0);
        assertEquals(0, item.getValueGold());
        item.setValueGold(1000);
        assertEquals(1000, item.getValueGold());
    }

    @Test
    public void rejectsNegativeValueGold() {
        TestItem item = new TestItem();
        assertThrows(IllegalArgumentException.class, () -> item.setValueGold(-1));
    }

    @Test
    public void acceptsNonNegativeWeight() {
        TestItem item = new TestItem();
        item.setWeight(0.0);
        assertEquals(0.0, item.getWeight(), 0.0);
        item.setWeight(15.5);
        assertEquals(15.5, item.getWeight(), 0.0);
    }

    @Test
    public void rejectsNegativeWeight() {
        TestItem item = new TestItem();
        assertThrows(IllegalArgumentException.class, () -> item.setWeight(-0.1));
    }

    @Test
    public void durabilityRejectsNegativeMax() {
        assertThrows(IllegalArgumentException.class, () -> new Item.ItemDurability(-1, 0));
    }

    @Test
    public void durabilityRejectsNegativeCurrent() {
        Item.ItemDurability durability = new Item.ItemDurability(10, 5);
        assertThrows(IllegalArgumentException.class, () -> durability.setCurrent(-1));
    }

    @Test
    public void durabilityRejectsCurrentExceedingMax() {
        Item.ItemDurability durability = new Item.ItemDurability(10, 5);
        assertThrows(IllegalArgumentException.class, () -> durability.setCurrent(11));
    }

    @Test
    public void durabilityAcceptsCurrentEqualToMax() {
        Item.ItemDurability durability = new Item.ItemDurability(10, 5);
        durability.setCurrent(10);
        assertEquals(10, durability.getCurrent());
    }

    @Test
    public void durabilityAcceptsValidValues() {
        Item.ItemDurability durability = new Item.ItemDurability(20, 15);
        assertEquals(20, durability.getMax());
        assertEquals(15, durability.getCurrent());
    }

    // Concrete test implementation
    private static class TestItem extends Item {
        public TestItem() {
            super("test", "Test Item", "test", "A test item", 0, 0.0, null, null);
        }
    }
}

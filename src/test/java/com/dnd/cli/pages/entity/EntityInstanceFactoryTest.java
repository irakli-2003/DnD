package com.dnd.cli.pages.entity;

import com.dnd.cli.core.FakeConsoleIO;
import com.dnd.model.item.Item;
import com.dnd.model.item.books.Book;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EntityInstanceFactoryTest {
    @Test
    public void resolveConcreteClassReturnsSameTypeWhenAlreadyConcrete() {
        FakeConsoleIO console = new FakeConsoleIO();
        Class<?> resolved = EntityInstanceFactory.resolveConcreteClass(Book.class, console, "Book");
        assertSame(Book.class, resolved);
    }

    @Test
    public void resolveConcreteClassPromptsAndResolvesFirstOptionForAbstractType() {
        // "1" selects the first option in the ITEM_TYPES registry (Armor).
        FakeConsoleIO console = new FakeConsoleIO("1");
        Class<?> resolved = EntityInstanceFactory.resolveConcreteClass(Item.class, console, "Item");
        assertNotNull(resolved);
        assertTrue(Item.class.isAssignableFrom(resolved));
    }

    @Test
    public void resolveConcreteClassReturnsNullWhenUserCancels() {
        FakeConsoleIO console = new FakeConsoleIO("");
        Class<?> resolved = EntityInstanceFactory.resolveConcreteClass(Item.class, console, "Item");
        assertNull(resolved);
    }

    @Test
    public void instantiateTypeUsesNoArgConstructor() {
        Object instance = EntityInstanceFactory.instantiateType(Book.class);
        assertNotNull(instance);
        assertTrue(instance instanceof Book);
    }

    @Test
    public void createInstanceForTypeResolvesAndInstantiatesConcreteType() {
        FakeConsoleIO console = new FakeConsoleIO();
        Object instance = EntityInstanceFactory.createInstanceForType(Book.class, console, "Book");
        assertNotNull(instance);
        assertTrue(instance instanceof Book);
    }
}



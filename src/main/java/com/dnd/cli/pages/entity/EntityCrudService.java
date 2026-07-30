package com.dnd.cli.pages.entity;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.ConsoleIO;
import com.dnd.cli.core.Page;
import com.dnd.cli.pages.EntityType;
import com.dnd.cli.pages.MapSessionPage;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.DataAccessException;
import com.dnd.data.IdHandler;
import com.dnd.data.JsonRepository;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.world.map.GameMap;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Handles the create/edit/delete/open flows for campaign entities: listing,
 * selecting, and persisting via {@link JsonRepository}. Delegates interactive
 * field population to {@link EntityPropertyEditor} and stays free of
 * type-resolution/reflection-instantiation concerns (see {@link EntityInstanceFactory}).
 * All I/O goes through {@link ConsoleIO} so this class can be unit tested with
 * a fake console instead of capturing real stdout/stdin.
 */
public class EntityCrudService {
    private final EntityPropertyEditor propertyEditor;

    public EntityCrudService() {
        this(new EntityPropertyEditor());
    }

    public EntityCrudService(EntityPropertyEditor propertyEditor) {
        this.propertyEditor = propertyEditor;
    }

    public void create(CliSession session, CampaignRepositories repositories, IdHandler idHandler, EntityType type) {
        ConsoleIO console = session.getConsole();
        console.print("Enter name: ");
        String name = console.readLine().trim();
        if (name.isEmpty()) {
            console.println("Create cancelled.");
            return;
        }

        // GameMap has a grid (List<List<GridCell>>) that the generic reflection-based
        // property editor cannot populate; build it directly via width/height instead.
        if (type.getModelClass() == GameMap.class) {
            createMap(session, repositories, idHandler, name);
            return;
        }

        Object entity;
        try {
            entity = EntityInstanceFactory.createInstanceForType(type.getModelClass(), console, type.getLabel());
            if (entity == null) {
                console.println("Failed to initialize " + type.getLabel() + ".");
                return;
            }
        } catch (Exception e) {
            console.println("Failed to initialize " + type.getLabel() + ": " + e.getMessage());
            return;
        }

        setEntityName(entity, name);
        propertyEditor.populateProperties(entity, console, false, repositories, 0);

        if (entity instanceof PlayerCharacter) {
            promptNewPassword(console, (PlayerCharacter) entity);
        }

        String id = idHandler.generateId(name, type.getRegistryPath());
        setEntityId(entity, id);

        try {
            JsonRepository repository = type.getRepository(repositories);
            repository.add(entity);
            console.println("Created " + type.getLabel() + ": " + name);
        } catch (DataAccessException | IllegalArgumentException e) {
            console.println("Failed to create " + type.getLabel() + ": " + e.getMessage());
        }
    }

    private void createMap(CliSession session, CampaignRepositories repositories, IdHandler idHandler, String name) {
        ConsoleIO console = session.getConsole();

        int width = readPositiveInt(console, "Width", 20);
        int height = readPositiveInt(console, "Height", 15);

        String id = idHandler.generateId(name, EntityType.MAP.getRegistryPath());
        GameMap map = new GameMap(id, name, width, height);

        try {
            repositories.maps().add(map);
            console.println("Created Map: " + name + " (" + width + "x" + height + ")");
        } catch (DataAccessException | IllegalArgumentException e) {
            console.println("Failed to create Map: " + e.getMessage());
        }
    }

    /**
     * Prompts for a positive integer, re-prompting on invalid input, and
     * falling back to {@code defaultValue} on a blank entry.
     */
    private int readPositiveInt(ConsoleIO console, String label, int defaultValue) {
        while (true) {
            console.print("Enter " + label + " (default " + defaultValue + "): ");
            String input = console.readLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(input);
                if (value <= 0) {
                    console.println(label + " must be a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                console.println("Enter a valid whole number.");
            }
        }
    }

    public void edit(CliSession session, CampaignRepositories repositories, EntityType type) {
        ConsoleIO console = session.getConsole();
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (DataAccessException e) {
            console.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return;
        }

        if (entities.isEmpty()) {
            console.println("No " + type.getLabel() + " entries found.");
            return;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(console, entries);
        console.print("Enter name to edit: ");
        String input = console.readLine().trim();
        if (input.isEmpty()) {
            console.println("Edit cancelled.");
            return;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            console.println("No matching entry found.");
            return;
        }

        Object existing = selected.getEntity();

        // GameMap's grid can't be safely repopulated by the generic reflection
        // editor (resizing would discard/misalign existing occupants). Only
        // allow renaming here; use "open" -> map session for grid contents.
        if (existing instanceof GameMap) {
            editMapName(session, repositories, (GameMap) existing);
            return;
        }

        printEntityDetails(console, existing);

        console.println("Enter updated values. Leave blank to keep current values.");
        propertyEditor.populateProperties(existing, console, true, repositories, 0);

        if (existing instanceof PlayerCharacter) {
            PlayerCharacter pc = (PlayerCharacter) existing;
            String prompt = pc.hasPassword() ? "Change password? (y/n, default n): " : "Set a password? (y/n, default n): ";
            console.print(prompt);
            String answer = console.readLine().trim().toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) {
                promptNewPassword(console, pc);
            }
        }

        setEntityId(existing, selected.getId());

        try {
            JsonRepository repository = type.getRepository(repositories);
            repository.update(existing);
            console.println("Updated " + type.getLabel() + " with name: " + selected.getName());
        } catch (DataAccessException | IllegalArgumentException e) {
            console.println("Failed to update " + type.getLabel() + ": " + e.getMessage());
        }
    }

    private void editMapName(CliSession session, CampaignRepositories repositories, GameMap map) {
        ConsoleIO console = session.getConsole();
        console.println("Maps only support renaming here. To place/move tokens, use Open > map.");
        console.print("Enter new name (blank to keep \"" + map.getName() + "\"): ");
        String newName = console.readLine().trim();
        if (!newName.isEmpty()) {
            map.setName(newName);
        }
        try {
            repositories.maps().update(map);
            console.println("Updated Map: " + map.getName());
        } catch (DataAccessException | IllegalArgumentException e) {
            console.println("Failed to update Map: " + e.getMessage());
        }
    }

    /**
     * Prompts twice for a new password (with confirmation) and hashes it onto
     * {@code pc} via {@link PlayerCharacter#setPassword(String)}. Leaving the
     * first prompt blank cancels without changing the existing password.
     */
    private void promptNewPassword(ConsoleIO console, PlayerCharacter pc) {
        console.print("Set a password for this character (blank to skip): ");
        String password = console.readLine();
        if (password == null || password.isEmpty()) {
            console.println("No password set.");
            return;
        }
        console.print("Confirm password: ");
        String confirm = console.readLine();
        if (!password.equals(confirm)) {
            console.println("Passwords did not match. Password not set.");
            return;
        }
        pc.setPassword(password);
        console.println("Password set.");
    }

    public void delete(CliSession session, CampaignRepositories repositories, IdHandler idHandler, EntityType type) {
        ConsoleIO console = session.getConsole();
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (DataAccessException e) {
            console.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return;
        }

        if (entities.isEmpty()) {
            console.println("No " + type.getLabel() + " entries found.");
            return;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(console, entries);
        console.print("Enter name to delete: ");
        String input = console.readLine().trim();
        if (input.isEmpty()) {
            console.println("Delete cancelled.");
            return;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            console.println("No matching entry found.");
            return;
        }

        console.print("Type DELETE to finalize: ");
        String secondConfirm = console.readLine().trim();

        if (!"DELETE".equals(secondConfirm)) {
            console.println("Delete cancelled.");
            return;
        }

        try {
            JsonRepository repository = type.getRepository(repositories);
            boolean deleted = repository.delete(selected.getId());
            if (deleted) {
                idHandler.removeId(selected.getId());
                console.println("Deleted " + type.getLabel() + ": " + selected.getName());
            } else {
                console.println("No entry found for name: " + selected.getName());
            }
        } catch (DataAccessException e) {
            console.println("Failed to delete " + type.getLabel() + ": " + e.getMessage());
        }
    }

    public void open(CliSession session, CampaignRepositories repositories, EntityType type) {
        open(session, repositories, type, null);
    }

    /**
     * Opens an entity for viewing.
     *
     * @param caller the page to use as the parent when navigating into a sub-page
     *               (e.g. a {@link MapSessionPage}). May be null.
     * @return a new {@link Page} to navigate into (e.g. a map session), or
     *         {@code null} to stay on the caller page.
     */
    public Page open(CliSession session, CampaignRepositories repositories, EntityType type, Page caller) {
        ConsoleIO console = session.getConsole();
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (DataAccessException e) {
            console.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return null;
        }

        if (entities.isEmpty()) {
            console.println("No " + type.getLabel() + " entries found.");
            return null;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(console, entries);
        console.print("Enter name to view: ");
        String input = console.readLine().trim();
        if (input.isEmpty()) {
            console.println("Open cancelled.");
            return null;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            console.println("No matching entry found.");
            return null;
        }

        // Maps get their own interactive session page instead of a flat detail dump.
        if (selected.getEntity() instanceof GameMap) {
            return new MapSessionPage((GameMap) selected.getEntity(), repositories, caller);
        }

        printEntityDetails(console, selected.getEntity());
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> listEntities(CampaignRepositories repositories, EntityType type) {
        JsonRepository repository = type.getRepository(repositories);
        return new ArrayList<>((List<Object>) repository.list());
    }

    private List<EntryDisplay> toEntryDisplay(List<Object> entities) {
        List<EntryDisplay> entries = new ArrayList<>();
        for (Object entity : entities) {
            entries.add(new EntryDisplay(
                invokeStringGetter(entity, "getId"),
                invokeStringGetter(entity, "getName"),
                entity
            ));
        }
        return entries;
    }

    private void printSummaries(ConsoleIO console, List<EntryDisplay> entries) {
        console.println("Available entries:");
        for (EntryDisplay entry : entries) {
            String name = entry.getName();
            console.println(name.isEmpty() ? "- (unnamed)" : "- " + name);
        }
    }

    private EntryDisplay resolveEntryByName(String input, List<EntryDisplay> entries) {
        String trimmed = input.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        for (EntryDisplay entry : entries) {
            if (entry.getName().equalsIgnoreCase(trimmed)) {
                return entry;
            }
        }

        EntryDisplay match = null;
        for (EntryDisplay entry : entries) {
            String name = entry.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith(normalized)) {
                if (match != null) {
                    return null;
                }
                match = entry;
            }
        }
        return match;
    }

    private void printEntityDetails(ConsoleIO console, Object entity) {
        console.println();
        List<FieldLine> lines = new ArrayList<>();
        try {
            PropertyDescriptor[] descriptors = Introspector.getBeanInfo(entity.getClass(), Object.class).getPropertyDescriptors();
            Arrays.sort(descriptors, Comparator.comparing(PropertyDescriptor::getName));
            for (PropertyDescriptor descriptor : descriptors) {
                if (descriptor.getReadMethod() == null) {
                    continue;
                }
                String name = descriptor.getName();
                if ("id".equals(name)) {
                    continue;
                }
                // Never surface the raw password hash/salt when viewing a player character.
                if ("passwordHash".equals(name) || "passwordSalt".equals(name)) {
                    continue;
                }
                Object value = descriptor.getReadMethod().invoke(entity);
                if (value instanceof CoreStats) {
                    addCoreStatsLines(lines, name, (CoreStats) value);
                } else if (value instanceof java.util.Map) {
                    addMapLines(lines, name, (java.util.Map<?, ?>) value);
                } else {
                    lines.add(new FieldLine(name, formatValue(value)));
                }
            }
        } catch (Exception e) {
            console.println("Failed to render entry: " + e.getMessage());
            return;
        }

        for (FieldLine line : lines) {
            console.println(toLabel(line.getField()) + ": " + line.getValue());
        }
        console.println();
    }

    private void addCoreStatsLines(List<FieldLine> lines, String prefix, CoreStats stats) {
        if (stats == null) {
            lines.add(new FieldLine(prefix, "null"));
            return;
        }
        lines.add(new FieldLine(prefix + ".strength", String.valueOf(stats.getStrength())));
        lines.add(new FieldLine(prefix + ".dexterity", String.valueOf(stats.getDexterity())));
        lines.add(new FieldLine(prefix + ".constitution", String.valueOf(stats.getConstitution())));
        lines.add(new FieldLine(prefix + ".intelligence", String.valueOf(stats.getIntelligence())));
        lines.add(new FieldLine(prefix + ".wisdom", String.valueOf(stats.getWisdom())));
        lines.add(new FieldLine(prefix + ".charisma", String.valueOf(stats.getCharisma())));
    }

    private void addMapLines(List<FieldLine> lines, String prefix, java.util.Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            lines.add(new FieldLine(prefix, "(none)"));
            return;
        }
        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
            lines.add(new FieldLine(prefix + "." + entry.getKey(), String.valueOf(entry.getValue())));
        }
    }

    private void setEntityId(Object entity, String id) {
        invokeStringSetter(entity, "setId", id);
    }

    private void setEntityName(Object entity, String name) {
        invokeStringSetter(entity, "setName", name);
    }

    private String invokeStringGetter(Object entity, String methodName) {
        if (entity == null) {
            return "";
        }
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object value = method.invoke(entity);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void invokeStringSetter(Object entity, String methodName, String value) {
        if (entity == null) {
            return;
        }
        try {
            Method method = entity.getClass().getMethod(methodName, String.class);
            method.invoke(entity, value);
        } catch (Exception ignored) {
            // Entity type does not expose this setter; nothing to do.
        }
    }

    private String toLabel(String name) {
        String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List) {
            List<?> items = (List<?>) value;
            List<String> rendered = new ArrayList<>();
            for (Object item : items) {
                rendered.add(String.valueOf(item));
            }
            return String.join(", ", rendered);
        }
        return String.valueOf(value);
    }

    private static class EntryDisplay {
        private final String id;
        private final String name;
        private final Object entity;

        private EntryDisplay(String id, String name, Object entity) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.entity = entity;
        }

        private String getId() {
            return id;
        }

        private String getName() {
            return name;
        }

        private Object getEntity() {
            return entity;
        }
    }

    private static class FieldLine {
        private final String field;
        private final String value;

        private FieldLine(String field, String value) {
            this.field = field;
            this.value = value == null ? "null" : value;
        }

        private String getField() {
            return field;
        }

        private String getValue() {
            return value;
        }
    }
}


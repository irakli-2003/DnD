package com.dnd.cli.pages;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.data.CampaignPaths;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.IdHandler;
import com.dnd.data.JsonRepository;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.world.Dice;
import com.dnd.model.item.Item;
import com.dnd.model.item.Weapon;
import com.dnd.model.item.alchemy.AlchemyItem;
import com.dnd.model.item.alchemy.Decoction;
import com.dnd.model.item.alchemy.Oil;
import com.dnd.model.item.alchemy.Poison;
import com.dnd.model.item.alchemy.Potion;
import com.dnd.model.item.armors.Armor;
import com.dnd.model.item.books.Book;
import com.dnd.model.item.weapons.magic_weapons.DarkWeapon;
import com.dnd.model.item.weapons.magic_weapons.DivineWeapon;
import com.dnd.model.item.weapons.magic_weapons.ElementalWeapon;
import com.dnd.model.item.weapons.magic_weapons.IllusionWeapon;
import com.dnd.model.item.weapons.magic_weapons.MagicWeapon;
import com.dnd.model.item.weapons.magic_weapons.NatureWeapon;
import com.dnd.model.item.weapons.magic_weapons.NecromancyWeapon;
import com.dnd.model.item.weapons.magic_weapons.TeleportationWeapon;
import com.dnd.model.item.weapons.magic_weapons.TransmutationWeapon;
import com.dnd.model.item.weapons.magic_weapons.WitcherSignsWeapon;
import com.dnd.model.item.weapons.physical_weapons.FinesseWeapon;
import com.dnd.model.item.weapons.physical_weapons.MeleeWeapon;
import com.dnd.model.item.weapons.physical_weapons.PhysicalWeapon;
import com.dnd.model.item.weapons.physical_weapons.RangedWeapon;
import com.dnd.model.item.weapons.physical_weapons.ThrowingWeapon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class EntitySelectionPage implements Page {
    public enum Operation {
        CREATE("Create Content", "Choose what to create."),
        EDIT("Edit Content", "Choose what to edit."),
        DELETE("Delete Content", "Choose what to delete."),
        OPEN("Open Content", "Choose what to view.");

        private final String title;
        private final String body;

        Operation(String title, String body) {
            this.title = title;
            this.body = body;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }
    }

    private static final int MAX_NESTED_DEPTH = 4;
    private static final List<Class<?>> ITEM_TYPES = Arrays.asList(
        Armor.class,
        Book.class,
        Potion.class,
        Poison.class,
        Oil.class,
        Decoction.class,
        MeleeWeapon.class,
        RangedWeapon.class,
        FinesseWeapon.class,
        ThrowingWeapon.class,
        DarkWeapon.class,
        DivineWeapon.class,
        ElementalWeapon.class,
        IllusionWeapon.class,
        NatureWeapon.class,
        NecromancyWeapon.class,
        TeleportationWeapon.class,
        TransmutationWeapon.class,
        WitcherSignsWeapon.class
    );
    private static final List<Class<?>> WEAPON_TYPES = Arrays.asList(
        MeleeWeapon.class,
        RangedWeapon.class,
        FinesseWeapon.class,
        ThrowingWeapon.class,
        DarkWeapon.class,
        DivineWeapon.class,
        ElementalWeapon.class,
        IllusionWeapon.class,
        NatureWeapon.class,
        NecromancyWeapon.class,
        TeleportationWeapon.class,
        TransmutationWeapon.class,
        WitcherSignsWeapon.class
    );
    private static final List<Class<?>> ALCHEMY_TYPES = Arrays.asList(
        Potion.class,
        Poison.class,
        Oil.class,
        Decoction.class
    );
    private static final List<Class<?>> MAGIC_WEAPON_TYPES = Arrays.asList(
        DarkWeapon.class,
        DivineWeapon.class,
        ElementalWeapon.class,
        IllusionWeapon.class,
        NatureWeapon.class,
        NecromancyWeapon.class,
        TeleportationWeapon.class,
        TransmutationWeapon.class,
        WitcherSignsWeapon.class
    );

    private final Operation operation;
    private Page parent;
    private ObjectMapper mapper;

    public EntitySelectionPage(Operation operation, Page parent) {
        this.operation = operation;
        this.parent = parent;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public Page getParent() {
        return parent;
    }

    @Override
    public String getTitle() {
        return operation.getTitle();
    }

    @Override
    public String getBody() {
        return operation.getBody();
    }

    @Override
    public List<CommandSpec> getCommands() {
        List<CommandSpec> commands = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            commands.add(new CommandSpec(type.getKey(), type.getLabel(), selectedSession -> handleEntity(selectedSession, type)));
        }
        return commands;
    }

    private Page handleEntity(CliSession selectedSession, EntityType type) {
        if (selectedSession.getCampaignContext() == null) {
            System.out.println("No campaign selected. Choose a campaign first.");
            return this;
        }

        CampaignRepositories repositories = new CampaignRepositories(selectedSession.getCampaignContext().getPath());
        CampaignPaths paths = new CampaignPaths(selectedSession.getCampaignContext().getPath());
        IdHandler idHandler = new IdHandler(paths.idRegistryFile());

        switch (operation) {
            case CREATE:
                return createEntity(selectedSession, repositories, idHandler, type);
            case EDIT:
                return editEntity(selectedSession, repositories, type);
            case DELETE:
                return deleteEntity(selectedSession, repositories, idHandler, type);
            case OPEN:
                return openEntity(selectedSession, repositories, type);
            default:
                return this;
        }
    }

    private Page createEntity(CliSession selectedSession, CampaignRepositories repositories, IdHandler idHandler, EntityType type) {
        Scanner scanner = selectedSession.getScanner();
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Create cancelled.");
            return this;
        }

        Object entity;
        try {
            entity = createInstanceForType(type.getModelClass(), scanner, type.getLabel());
            if (entity == null) {
                System.out.println("Failed to initialize " + type.getLabel() + ".");
                return this;
            }
        } catch (Exception e) {
            System.out.println("Failed to initialize " + type.getLabel() + ": " + e.getMessage());
            return this;
        }

        setEntityName(entity, name);
        populateProperties(entity, scanner, false, repositories, 0);

        String id = idHandler.generateId(name, type.getRegistryPath());
        setEntityId(entity, id);

        try {
            JsonRepository repository = type.getRepository(repositories);
            repository.add(entity);
            System.out.println("Created " + type.getLabel() + ": " + name);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to create " + type.getLabel() + ": " + e.getMessage());
        }

        return this;
    }

    private Page editEntity(CliSession selectedSession, CampaignRepositories repositories, EntityType type) {
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (IOException e) {
            System.out.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return this;
        }

        if (entities.isEmpty()) {
            System.out.println("No " + type.getLabel() + " entries found.");
            return this;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(entries);
        Scanner scanner = selectedSession.getScanner();
        System.out.print("Enter name to edit: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Edit cancelled.");
            return this;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            System.out.println("No matching entry found.");
            return this;
        }

        Object existing = selected.getEntity();

        printEntityDetails(existing);

        System.out.println("Enter updated values. Leave blank to keep current values.");
        populateProperties(existing, scanner, true, repositories, 0);

        setEntityId(existing, selected.getId());

        try {
            JsonRepository repository = type.getRepository(repositories);
            repository.update(existing);
            System.out.println("Updated " + type.getLabel() + " with name: " + selected.getName());
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to update " + type.getLabel() + ": " + e.getMessage());
        }

        return this;
    }

    private Page deleteEntity(CliSession selectedSession, CampaignRepositories repositories, IdHandler idHandler, EntityType type) {
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (IOException e) {
            System.out.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return this;
        }

        if (entities.isEmpty()) {
            System.out.println("No " + type.getLabel() + " entries found.");
            return this;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(entries);
        Scanner scanner = selectedSession.getScanner();
        System.out.print("Enter name to delete: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Delete cancelled.");
            return this;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            System.out.println("No matching entry found.");
            return this;
        }

        System.out.print("Type DELETE to finalize: ");
        String secondConfirm = scanner.nextLine().trim();

        if (!"DELETE".equals(secondConfirm)) {
            System.out.println("Delete cancelled.");
            return this;
        }

        try {
            JsonRepository repository = type.getRepository(repositories);
            boolean deleted = repository.delete(selected.getId());
            if (deleted) {
                idHandler.removeId(selected.getId());
                System.out.println("Deleted " + type.getLabel() + ": " + selected.getName());
            } else {
                System.out.println("No entry found for name: " + selected.getName());
            }
        } catch (IOException e) {
            System.out.println("Failed to delete " + type.getLabel() + ": " + e.getMessage());
        }

        return this;
    }

    private Page openEntity(CliSession selectedSession, CampaignRepositories repositories, EntityType type) {
        List<Object> entities;
        try {
            entities = listEntities(repositories, type);
        } catch (IOException e) {
            System.out.println("Failed to load " + type.getLabel() + " catalog: " + e.getMessage());
            return this;
        }

        if (entities.isEmpty()) {
            System.out.println("No " + type.getLabel() + " entries found.");
            return this;
        }

        List<EntryDisplay> entries = toEntryDisplay(entities);
        printSummaries(entries);
        Scanner scanner = selectedSession.getScanner();
        System.out.print("Enter name to view: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Open cancelled.");
            return this;
        }

        EntryDisplay selected = resolveEntryByName(input, entries);
        if (selected == null) {
            System.out.println("No matching entry found.");
            return this;
        }

        printEntityDetails(selected.getEntity());

        return this;
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

    private void printSummaries(List<EntryDisplay> entries) {
        System.out.println("Available entries:");
        for (EntryDisplay entry : entries) {
            String name = entry.getName();
            if (name.isEmpty()) {
                System.out.println("- (unnamed)");
            } else {
                System.out.println("- " + name);
            }
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

    private void printEntityDetails(Object entity) {
        System.out.println();
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
            System.out.println("Failed to render entry: " + e.getMessage());
            return;
        }

        for (FieldLine line : lines) {
            System.out.println(toLabel(line.getField()) + ": " + line.getValue());
        }
        System.out.println();
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
            String key = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());
            lines.add(new FieldLine(prefix + "." + key, value));
        }
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

    @SuppressWarnings("unchecked")
    private List<Object> listEntities(CampaignRepositories repositories, EntityType type) throws IOException {
        JsonRepository repository = type.getRepository(repositories);
        return new ArrayList<>((List<Object>) repository.list());
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
        }
    }

    private String buildPrompt(String name,
                               PropertyDescriptor descriptor,
                               Object currentValue,
                               boolean editing,
                               List<OptionEntry> options) {
        String label = toLabel(name);
        if (editing) {
            return "Enter " + label + " [" + formatValue(currentValue) + "]: ";
        }
        String hint = "Enter " + label;
        String typeHint = getTypeHint(descriptor, options);
        if (!typeHint.isEmpty()) {
            hint += " (" + typeHint + ")";
        }
        return hint + ": ";
    }

    private String getTypeHint(PropertyDescriptor descriptor, List<OptionEntry> options) {
        if (!options.isEmpty()) {
            return "indexes or custom";
        }
        Class<?> type = descriptor.getPropertyType();
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            List<String> values = new ArrayList<>();
            for (Object constant : constants) {
                values.add(String.valueOf(constant));
            }
            return "one of: " + String.join(", ", values);
        }
        if (List.class.isAssignableFrom(type)) {
            return "comma-separated";
        }
        return type == String.class ? "text" : type.getSimpleName();
    }

    private Object parseValue(String input,
                              PropertyDescriptor descriptor,
                              String fieldName,
                              List<OptionEntry> options) {
        Class<?> type = descriptor.getPropertyType();
        if (type == String.class) {
            if (!options.isEmpty()) {
                List<String> parsed = parseOptionInput(input, options);
                if (parsed.size() > 1) {
                    throw new IllegalArgumentException("Choose one option or enter a single custom value.");
                }
                return parsed.get(0);
            }
            return input;
        }
        if (type == int.class || type == Integer.class) {
            int value;
            try {
                value = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Enter a valid integer.");
            }
            validateIntRange(fieldName, value);
            return value;
        }
        if (type == long.class || type == Long.class) {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Enter a valid whole number.");
            }
        }
        if (type == double.class || type == Double.class) {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Enter a valid number.");
            }
        }
        if (type == boolean.class || type == Boolean.class) {
            String normalized = input.toLowerCase(Locale.ROOT);
            if (normalized.equals("true") || normalized.equals("yes") || normalized.equals("y")) {
                return true;
            }
            if (normalized.equals("false") || normalized.equals("no") || normalized.equals("n")) {
                return false;
            }
            throw new IllegalArgumentException("Enter true/false.");
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            for (Object constant : constants) {
                if (String.valueOf(constant).equalsIgnoreCase(input)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Unknown value. Use one of the listed options.");
        }
        if (List.class.isAssignableFrom(type)) {
            if (!options.isEmpty()) {
                return parseOptionInput(input, options);
            }
            List<String> values = new ArrayList<>();
            for (String part : input.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
            return values;
        }
        throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    }

    private void validateIntRange(String fieldName, int value) {
        if ("level".equalsIgnoreCase(fieldName)) {
            requireRange(fieldName, value, 1, 30);
        }
    }

    private void requireRange(String fieldName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Enter " + fieldName + " between " + min + " and " + max + ".");
        }
    }

    private CoreStats editCoreStats(CoreStats stats, Scanner scanner, boolean editing) {
        CoreStats working = stats == null ? new CoreStats() : stats;
        System.out.println("Enter stats (1-30). Leave blank to keep current values.");

        Integer strength = promptIntInRange(scanner, "Strength", working.getStrength(), editing, 1, 30);
        if (strength != null) {
            working.setStrength(strength);
        }
        Integer dexterity = promptIntInRange(scanner, "Dexterity", working.getDexterity(), editing, 1, 30);
        if (dexterity != null) {
            working.setDexterity(dexterity);
        }
        Integer constitution = promptIntInRange(scanner, "Constitution", working.getConstitution(), editing, 1, 30);
        if (constitution != null) {
            working.setConstitution(constitution);
        }
        Integer intelligence = promptIntInRange(scanner, "Intelligence", working.getIntelligence(), editing, 1, 30);
        if (intelligence != null) {
            working.setIntelligence(intelligence);
        }
        Integer wisdom = promptIntInRange(scanner, "Wisdom", working.getWisdom(), editing, 1, 30);
        if (wisdom != null) {
            working.setWisdom(wisdom);
        }
        Integer charisma = promptIntInRange(scanner, "Charisma", working.getCharisma(), editing, 1, 30);
        if (charisma != null) {
            working.setCharisma(charisma);
        }

        return working;
    }

    private Integer promptIntInRange(Scanner scanner,
                                    String label,
                                    int currentValue,
                                    boolean editing,
                                    int min,
                                    int max) {
        while (true) {
            String prompt = editing
                ? "Enter " + label + " [" + currentValue + "]: "
                : "Enter " + label + " (" + min + "-" + max + "): ";
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                if (editing) {
                    return null;
                }
                System.out.println("Enter a value between " + min + " and " + max + ".");
                continue;
            }
            int parsed;
            try {
                parsed = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid integer.");
                continue;
            }
            if (parsed < min || parsed > max) {
                System.out.println("Enter a value between " + min + " and " + max + ".");
                continue;
            }
            return parsed;
        }
    }

    private void printOptions(List<OptionEntry> options) {
        System.out.println("Available options:");
        for (int i = 0; i < options.size(); i++) {
            OptionEntry entry = options.get(i);
            System.out.println("  " + (i + 1) + ") " + entry.getLabel());
        }
    }

    private List<OptionEntry> getOptions(PropertyDescriptor descriptor, CampaignRepositories repositories) {
        String name = descriptor.getName();
        if ("classId".equalsIgnoreCase(name)) {
            return loadIdOptions(repositories, "Class", repositories.classes()::list);
        }
        if ("raceId".equalsIgnoreCase(name)) {
            return loadIdOptions(repositories, "Race", repositories.races()::list);
        }
        if ("itemId".equalsIgnoreCase(name)) {
            return loadIdOptions(repositories, "Item", repositories.items()::list);
        }
        if ("spellId".equalsIgnoreCase(name)) {
            return loadIdOptions(repositories, "Spell", repositories.spells()::list);
        }
        if ("languages".equalsIgnoreCase(name)) {
            return loadIdOptions(repositories, "Language", repositories.languages()::list);
        }
        return new ArrayList<>();
    }

    private List<OptionEntry> loadIdOptions(CampaignRepositories repositories,
                                            String label,
                                            RepositorySupplier supplier) {
        List<OptionEntry> entries = new ArrayList<>();
        try {
            List<?> items = supplier.get();
            for (Object item : items) {
                String id = invokeStringGetter(item, "getId");
                String name = invokeStringGetter(item, "getName");
                if (id.isEmpty()) {
                    continue;
                }
                String display = name.isEmpty() ? id : name;
                entries.add(new OptionEntry(id, display));
            }
        } catch (Exception e) {
            System.out.println("Failed to load " + label + " options: " + e.getMessage());
        }
        return entries;
    }

    private List<String> parseOptionInput(String input, List<OptionEntry> options) {
        Set<String> values = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int index = Integer.parseInt(trimmed);
                if (index < 1 || index > options.size()) {
                    throw new IllegalArgumentException("Enter option indexes between 1 and " + options.size() + ".");
                }
                values.add(options.get(index - 1).getValue());
            } catch (NumberFormatException e) {
                values.add(resolveOptionValue(trimmed, options));
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one option.");
        }
        return new ArrayList<>(values);
    }

    private String resolveOptionValue(String input, List<OptionEntry> options) {
        String normalized = input.toLowerCase(Locale.ROOT);
        OptionEntry exact = null;
        for (OptionEntry option : options) {
            if (option.getLabel().equalsIgnoreCase(input)) {
                return option.getValue();
            }
            if (option.getLabel().toLowerCase(Locale.ROOT).equals(normalized)) {
                exact = option;
            }
        }
        if (exact != null) {
            return exact.getValue();
        }

        OptionEntry match = null;
        for (OptionEntry option : options) {
            String label = option.getLabel().toLowerCase(Locale.ROOT);
            if (label.startsWith(normalized)) {
                if (match != null) {
                    throw new IllegalArgumentException("Multiple options match: " + input);
                }
                match = option;
            }
        }
        if (match != null) {
            return match.getValue();
        }
        return input;
    }

    private static class OptionEntry {
        private final String value;
        private final String label;

        private OptionEntry(String value, String label) {
            this.value = value;
            this.label = label;
        }

        private String getValue() {
            return value;
        }

        private String getLabel() {
            return label;
        }
    }

    @FunctionalInterface
    private interface RepositorySupplier {
        List<?> get() throws IOException;
    }

    private void populateProperties(Object entity, Scanner scanner, boolean editing, CampaignRepositories repositories, int depth) {
        if (entity == null) {
            return;
        }
        if (depth > MAX_NESTED_DEPTH) {
            System.out.println("Nested object depth limit reached for " + entity.getClass().getSimpleName() + ".");
            return;
        }
        PropertyDescriptor[] descriptors;
        try {
            descriptors = Introspector.getBeanInfo(entity.getClass(), Object.class).getPropertyDescriptors();
        } catch (Exception e) {
            System.out.println("Failed to inspect fields: " + e.getMessage());
            return;
        }

        Arrays.sort(descriptors, Comparator.comparing(PropertyDescriptor::getName));
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getWriteMethod() == null || descriptor.getReadMethod() == null) {
                continue;
            }
            String name = descriptor.getName();
            if ("id".equals(name)) {
                continue;
            }
            if (!editing && "name".equals(name)) {
                continue;
            }

            if (descriptor.getPropertyType() == CoreStats.class) {
                CoreStats stats = null;
                try {
                    stats = (CoreStats) descriptor.getReadMethod().invoke(entity);
                } catch (Exception ignored) {
                    stats = null;
                }
                CoreStats updated = editCoreStats(stats, scanner, editing);
                if (updated != null) {
                    try {
                        descriptor.getWriteMethod().invoke(entity, updated);
                    } catch (Exception e) {
                        System.out.println("Failed to set " + name + ": " + e.getMessage());
                    }
                }
                continue;
            }

            if (descriptor.getPropertyType() == Dice.class) {
                handleDiceField(entity, descriptor, scanner, editing, repositories);
                continue;
            }

            if (Map.class.isAssignableFrom(descriptor.getPropertyType())) {
                handleMapField(entity, descriptor, scanner, editing);
                continue;
            }

            if (List.class.isAssignableFrom(descriptor.getPropertyType())) {
                if (handleListField(entity, descriptor, scanner, editing, repositories, depth)) {
                    continue;
                }
            }

            if (!isSupportedType(descriptor)) {
                if (handleNestedObjectField(entity, descriptor, scanner, editing, repositories, depth)) {
                    continue;
                }
                System.out.println("Skipping " + name + " (unsupported type). ");
                continue;
            }

            Object currentValue = null;
            if (editing) {
                try {
                    currentValue = descriptor.getReadMethod().invoke(entity);
                } catch (Exception ignored) {
                    currentValue = null;
                }
            }

            List<OptionEntry> options = getOptions(descriptor, repositories);
            if (!options.isEmpty()) {
                printOptions(options);
            }

            while (true) {
                String prompt = buildPrompt(name, descriptor, currentValue, editing, options);
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    break;
                }

                try {
                    Object parsed = parseValue(input, descriptor, name, options);
                    descriptor.getWriteMethod().invoke(entity, parsed);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Failed to set " + name + ": " + e.getMessage());
                    break;
                }
            }
        }
    }

    private boolean handleNestedObjectField(Object entity,
                                            PropertyDescriptor descriptor,
                                            Scanner scanner,
                                            boolean editing,
                                            CampaignRepositories repositories,
                                            int depth) {
        Class<?> type = descriptor.getPropertyType();
        if (type.isPrimitive() || type.isEnum() || type == String.class) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers()) || type.isInterface()) {
            Class<?> resolved = resolveConcreteClass(type, scanner, toLabel(descriptor.getName()));
            if (resolved == null) {
                return false;
            }
            type = resolved;
        }

        Object currentValue = null;
        if (editing) {
            try {
                currentValue = descriptor.getReadMethod().invoke(entity);
            } catch (Exception ignored) {
                currentValue = null;
            }
        }

        if (!shouldCreateNested(scanner, toLabel(descriptor.getName()), editing, currentValue != null)) {
            return true;
        }

        Object nested = currentValue;
        if (nested == null || nested.getClass() != type) {
            nested = instantiateType(type);
        }
        if (nested == null) {
            System.out.println("Skipping " + descriptor.getName() + " (no concrete type available).");
            return true;
        }

        boolean nestedEditing = editing && currentValue != null;
        populateProperties(nested, scanner, nestedEditing, repositories, depth + 1);
        try {
            descriptor.getWriteMethod().invoke(entity, nested);
        } catch (Exception e) {
            System.out.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
        return true;
    }

    private boolean handleListField(Object entity,
                                    PropertyDescriptor descriptor,
                                    Scanner scanner,
                                    boolean editing,
                                    CampaignRepositories repositories,
                                    int depth) {
        Class<?> elementType = getListElementType(descriptor);
        if (elementType == null) {
            return false;
        }
        if (elementType == String.class) {
            return false;
        }

        Object currentValue = null;
        if (editing) {
            try {
                currentValue = descriptor.getReadMethod().invoke(entity);
            } catch (Exception ignored) {
                currentValue = null;
            }
        }

        if (!shouldCreateNested(scanner, toLabel(descriptor.getName()), editing, currentValue != null)) {
            return true;
        }

        List<Object> entries = new ArrayList<>();
        int count = promptCount(scanner, "How many " + toLabel(descriptor.getName()) + " entries", editing);
        for (int i = 0; i < count; i++) {
            Class<?> concreteType = resolveConcreteClass(elementType, scanner, toLabel(descriptor.getName()));
            if (concreteType == null) {
                System.out.println("Skipping entry " + (i + 1) + " (no concrete type available).");
                continue;
            }
            Object entry = instantiateType(concreteType);
            if (entry == null) {
                System.out.println("Skipping entry " + (i + 1) + " (failed to initialize).");
                continue;
            }
            System.out.println("Entering " + toLabel(descriptor.getName()) + " entry " + (i + 1) + " of " + count + ":");
            populateProperties(entry, scanner, false, repositories, depth + 1);
            entries.add(entry);
        }

        try {
            descriptor.getWriteMethod().invoke(entity, entries);
        } catch (Exception e) {
            System.out.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
        return true;
    }

    private void handleMapField(Object entity, PropertyDescriptor descriptor, Scanner scanner, boolean editing) {
        if (!shouldCreateNested(scanner, toLabel(descriptor.getName()), editing, false)) {
            return;
        }

        Class<?> keyType = getMapKeyType(descriptor);
        Class<?> valueType = getMapValueType(descriptor);
        if (keyType != String.class || (valueType != String.class && valueType != Integer.class && valueType != int.class)) {
            System.out.println("Skipping " + descriptor.getName() + " (unsupported map types).");
            return;
        }

        int count = promptCount(scanner, "How many " + toLabel(descriptor.getName()) + " entries", editing);
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < count; i++) {
            System.out.print("Enter key " + (i + 1) + ": ");
            String key = scanner.nextLine().trim();
            if (key.isEmpty()) {
                System.out.println("Key cannot be empty.");
                i--;
                continue;
            }
            Object parsedValue = promptMapValue(scanner, valueType, i + 1);
            values.put(key, parsedValue);
        }

        try {
            descriptor.getWriteMethod().invoke(entity, values);
        } catch (Exception e) {
            System.out.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
    }

    private Object promptMapValue(Scanner scanner, Class<?> valueType, int index) {
        while (true) {
            System.out.print("Enter value " + index + " (" + valueType.getSimpleName() + "): ");
            String input = scanner.nextLine().trim();
            if (valueType == String.class) {
                return input;
            }
            if (input.isEmpty()) {
                System.out.println("Value cannot be empty.");
                continue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid integer.");
            }
        }
    }

    private int promptCount(Scanner scanner, String label, boolean editing) {
        while (true) {
            System.out.print(label + " (0 to skip): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return 0;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < 0) {
                    System.out.println("Enter 0 or a positive integer.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid integer.");
            }
        }
    }

    private boolean shouldCreateNested(Scanner scanner, String label, boolean editing, boolean hasCurrent) {
        if (!editing) {
            System.out.print("Add " + label + "? (y/n, default y): ");
        } else {
            System.out.print("Edit " + label + "? (y/n, default n): ");
        }
        String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return !editing;
        }
        return input.equals("y") || input.equals("yes");
    }

    private Class<?> getListElementType(PropertyDescriptor descriptor) {
        Type generic = descriptor.getReadMethod().getGenericReturnType();
        if (!(generic instanceof ParameterizedType)) {
            return null;
        }
        Type arg = ((ParameterizedType) generic).getActualTypeArguments()[0];
        if (arg instanceof Class<?>) {
            return (Class<?>) arg;
        }
        return null;
    }

    private Class<?> getMapKeyType(PropertyDescriptor descriptor) {
        Type generic = descriptor.getReadMethod().getGenericReturnType();
        if (!(generic instanceof ParameterizedType)) {
            return null;
        }
        Type arg = ((ParameterizedType) generic).getActualTypeArguments()[0];
        if (arg instanceof Class<?>) {
            return (Class<?>) arg;
        }
        return null;
    }

    private Class<?> getMapValueType(PropertyDescriptor descriptor) {
        Type generic = descriptor.getReadMethod().getGenericReturnType();
        if (!(generic instanceof ParameterizedType)) {
            return null;
        }
        Type arg = ((ParameterizedType) generic).getActualTypeArguments()[1];
        if (arg instanceof Class<?>) {
            return (Class<?>) arg;
        }
        return null;
    }

    private Object createInstanceForType(Class<?> modelClass, Scanner scanner, String label) {
        Class<?> resolved = resolveConcreteClass(modelClass, scanner, label);
        if (resolved == null) {
            return null;
        }
        return instantiateType(resolved);
    }

    private Object instantiateType(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> resolveConcreteClass(Class<?> type, Scanner scanner, String label) {
        if (type == null) {
            return null;
        }
        if (!Modifier.isAbstract(type.getModifiers()) && !type.isInterface()) {
            return type;
        }

        List<Class<?>> options = getConcreteOptions(type);
        if (options.isEmpty()) {
            System.out.println("No concrete types available for " + label + ".");
            return null;
        }

        System.out.println("Choose " + label + " type:");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + options.get(i).getSimpleName());
        }

        while (true) {
            System.out.print("Enter choice (1-" + options.size() + ", blank to cancel): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                int index = Integer.parseInt(input);
                if (index < 1 || index > options.size()) {
                    System.out.println("Enter a number between 1 and " + options.size() + ".");
                    continue;
                }
                return options.get(index - 1);
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private List<Class<?>> getConcreteOptions(Class<?> type) {
        if (type == Item.class) {
            return ITEM_TYPES;
        }
        if (type == Weapon.class || type == PhysicalWeapon.class) {
            return WEAPON_TYPES;
        }
        if (type == AlchemyItem.class) {
            return ALCHEMY_TYPES;
        }
        if (type == MagicWeapon.class) {
            return MAGIC_WEAPON_TYPES;
        }
        return new ArrayList<>();
    }

    private boolean isSupportedType(PropertyDescriptor descriptor) {
        Class<?> type = descriptor.getPropertyType();
        if (type == String.class || type == Integer.class || type == int.class
            || type == Long.class || type == long.class || type == Double.class || type == double.class
            || type == Boolean.class || type == boolean.class || type.isEnum()) {
            return true;
        }
        if (List.class.isAssignableFrom(type)) {
            Type generic = descriptor.getReadMethod().getGenericReturnType();
            if (generic instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) generic).getActualTypeArguments()[0];
                return arg == String.class;
            }
        }
        return false;
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
            return String.join(", ", items.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        return String.valueOf(value);
    }

    private void handleDiceField(Object entity,
                                 PropertyDescriptor descriptor,
                                 Scanner scanner,
                                 boolean editing,
                                 CampaignRepositories repositories) {
        List<OptionEntry> options = loadIdOptions(repositories, "Dice", repositories.dice()::list);
        if (options.isEmpty()) {
            System.out.println("No dice available. Create dice first.");
            return;
        }

        Object currentValue = null;
        if (editing) {
            try {
                currentValue = descriptor.getReadMethod().invoke(entity);
            } catch (Exception ignored) {
                currentValue = null;
            }
        }

        printOptions(options);
        while (true) {
            String prompt = buildPrompt(descriptor.getName(), descriptor, currentValue, editing, options);
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                break;
            }
            try {
                List<String> selected = parseOptionInput(input, options);
                if (selected.isEmpty()) {
                    System.out.println("Choose a dice option.");
                    continue;
                }
                String diceId = selected.get(0);
                Dice dice = repositories.dice().getById(diceId);
                if (dice == null) {
                    System.out.println("Unknown dice: " + diceId);
                    continue;
                }
                descriptor.getWriteMethod().invoke(entity, dice);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
                break;
            }
        }
    }
}

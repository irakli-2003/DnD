package com.dnd.cli.pages.entity;

import com.dnd.cli.core.ConsoleIO;
import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.world.Dice;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interactively populates the properties of an entity via reflection,
 * prompting the user for each writable bean property. This is the "form
 * builder" half of entity create/edit; it knows nothing about persistence
 * or CRUD flow. All I/O goes through {@link ConsoleIO} so this class can be
 * unit tested with a fake console instead of capturing real stdout/stdin.
 */
public class EntityPropertyEditor {
    private static final int MAX_NESTED_DEPTH = 4;

    public void populateProperties(Object entity, ConsoleIO console, boolean editing, CampaignRepositories repositories, int depth) {
        if (entity == null) {
            return;
        }
        if (depth > MAX_NESTED_DEPTH) {
            console.println("Nested object depth limit reached for " + entity.getClass().getSimpleName() + ".");
            return;
        }
        PropertyDescriptor[] descriptors;
        try {
            descriptors = Introspector.getBeanInfo(entity.getClass(), Object.class).getPropertyDescriptors();
        } catch (Exception e) {
            console.println("Failed to inspect fields: " + e.getMessage());
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
                CoreStats stats;
                try {
                    stats = (CoreStats) descriptor.getReadMethod().invoke(entity);
                } catch (Exception ignored) {
                    stats = null;
                }
                CoreStats updated = editCoreStats(stats, console, editing);
                if (updated != null) {
                    try {
                        descriptor.getWriteMethod().invoke(entity, updated);
                    } catch (Exception e) {
                        console.println("Failed to set " + name + ": " + e.getMessage());
                    }
                }
                continue;
            }

            if (descriptor.getPropertyType() == Dice.class) {
                handleDiceField(entity, descriptor, console, editing, repositories);
                continue;
            }

            if (Map.class.isAssignableFrom(descriptor.getPropertyType())) {
                handleMapField(entity, descriptor, console, editing);
                continue;
            }

            if (List.class.isAssignableFrom(descriptor.getPropertyType())) {
                if (handleListField(entity, descriptor, console, editing, repositories, depth)) {
                    continue;
                }
            }

            if (!isSupportedType(descriptor)) {
                if (handleNestedObjectField(entity, descriptor, console, editing, repositories, depth)) {
                    continue;
                }
                console.println("Skipping " + name + " (unsupported type). ");
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
                printOptions(console, options);
            }

            while (true) {
                String prompt = buildPrompt(name, descriptor, currentValue, editing, options);
                console.print(prompt);
                String input = console.readLine().trim();
                if (input.isEmpty()) {
                    break;
                }

                try {
                    Object parsed = parseValue(input, descriptor, name, options);
                    descriptor.getWriteMethod().invoke(entity, parsed);
                    break;
                } catch (IllegalArgumentException e) {
                    console.println(e.getMessage());
                } catch (Exception e) {
                    console.println("Failed to set " + name + ": " + e.getMessage());
                    break;
                }
            }
        }
    }

    private boolean handleNestedObjectField(Object entity,
                                            PropertyDescriptor descriptor,
                                            ConsoleIO console,
                                            boolean editing,
                                            CampaignRepositories repositories,
                                            int depth) {
        Class<?> type = descriptor.getPropertyType();
        if (type.isPrimitive() || type.isEnum() || type == String.class) {
            return false;
        }
        if (Modifier.isAbstract(type.getModifiers()) || type.isInterface()) {
            Class<?> resolved = EntityInstanceFactory.resolveConcreteClass(type, console, toLabel(descriptor.getName()));
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

        if (!shouldCreateNested(console, toLabel(descriptor.getName()), editing, currentValue != null)) {
            return true;
        }

        Object nested = currentValue;
        if (nested == null || nested.getClass() != type) {
            nested = EntityInstanceFactory.instantiateType(type);
        }
        if (nested == null) {
            console.println("Skipping " + descriptor.getName() + " (no concrete type available).");
            return true;
        }

        boolean nestedEditing = editing && currentValue != null;
        populateProperties(nested, console, nestedEditing, repositories, depth + 1);
        try {
            descriptor.getWriteMethod().invoke(entity, nested);
        } catch (Exception e) {
            console.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
        return true;
    }

    private boolean handleListField(Object entity,
                                    PropertyDescriptor descriptor,
                                    ConsoleIO console,
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

        if (!shouldCreateNested(console, toLabel(descriptor.getName()), editing, currentValue != null)) {
            return true;
        }

        List<Object> entries = new ArrayList<>();
        int count = promptCount(console, "How many " + toLabel(descriptor.getName()) + " entries", editing);
        for (int i = 0; i < count; i++) {
            Class<?> concreteType = EntityInstanceFactory.resolveConcreteClass(elementType, console, toLabel(descriptor.getName()));
            if (concreteType == null) {
                console.println("Skipping entry " + (i + 1) + " (no concrete type available).");
                continue;
            }
            Object entry = EntityInstanceFactory.instantiateType(concreteType);
            if (entry == null) {
                console.println("Skipping entry " + (i + 1) + " (failed to initialize).");
                continue;
            }
            console.println("Entering " + toLabel(descriptor.getName()) + " entry " + (i + 1) + " of " + count + ":");
            populateProperties(entry, console, false, repositories, depth + 1);
            entries.add(entry);
        }

        try {
            descriptor.getWriteMethod().invoke(entity, entries);
        } catch (Exception e) {
            console.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
        return true;
    }

    private void handleMapField(Object entity, PropertyDescriptor descriptor, ConsoleIO console, boolean editing) {
        if (!shouldCreateNested(console, toLabel(descriptor.getName()), editing, false)) {
            return;
        }

        Class<?> keyType = getMapKeyType(descriptor);
        Class<?> valueType = getMapValueType(descriptor);
        if (keyType != String.class || (valueType != String.class && valueType != Integer.class && valueType != int.class)) {
            console.println("Skipping " + descriptor.getName() + " (unsupported map types).");
            return;
        }

        int count = promptCount(console, "How many " + toLabel(descriptor.getName()) + " entries", editing);
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < count; i++) {
            console.print("Enter key " + (i + 1) + ": ");
            String key = console.readLine().trim();
            if (key.isEmpty()) {
                console.println("Key cannot be empty.");
                i--;
                continue;
            }
            Object parsedValue = promptMapValue(console, valueType, i + 1);
            values.put(key, parsedValue);
        }

        try {
            descriptor.getWriteMethod().invoke(entity, values);
        } catch (Exception e) {
            console.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
        }
    }

    private Object promptMapValue(ConsoleIO console, Class<?> valueType, int index) {
        while (true) {
            console.print("Enter value " + index + " (" + valueType.getSimpleName() + "): ");
            String input = console.readLine().trim();
            if (valueType == String.class) {
                return input;
            }
            if (input.isEmpty()) {
                console.println("Value cannot be empty.");
                continue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                console.println("Enter a valid integer.");
            }
        }
    }

    private int promptCount(ConsoleIO console, String label, boolean editing) {
        while (true) {
            console.print(label + " (0 to skip): ");
            String input = console.readLine().trim();
            if (input.isEmpty()) {
                return 0;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < 0) {
                    console.println("Enter 0 or a positive integer.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                console.println("Enter a valid integer.");
            }
        }
    }

    private boolean shouldCreateNested(ConsoleIO console, String label, boolean editing, boolean hasCurrent) {
        if (!editing) {
            console.print("Add " + label + "? (y/n, default y): ");
        } else {
            console.print("Edit " + label + "? (y/n, default n): ");
        }
        String input = console.readLine().trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return !editing;
        }
        return input.equals("y") || input.equals("yes");
    }

    private CoreStats editCoreStats(CoreStats stats, ConsoleIO console, boolean editing) {
        CoreStats working = stats == null ? new CoreStats() : stats;
        console.println("Enter stats (1-30). Leave blank to keep current values.");

        Integer strength = promptIntInRange(console, "Strength", working.getStrength(), editing, 1, 30);
        if (strength != null) {
            working.setStrength(strength);
        }
        Integer dexterity = promptIntInRange(console, "Dexterity", working.getDexterity(), editing, 1, 30);
        if (dexterity != null) {
            working.setDexterity(dexterity);
        }
        Integer constitution = promptIntInRange(console, "Constitution", working.getConstitution(), editing, 1, 30);
        if (constitution != null) {
            working.setConstitution(constitution);
        }
        Integer intelligence = promptIntInRange(console, "Intelligence", working.getIntelligence(), editing, 1, 30);
        if (intelligence != null) {
            working.setIntelligence(intelligence);
        }
        Integer wisdom = promptIntInRange(console, "Wisdom", working.getWisdom(), editing, 1, 30);
        if (wisdom != null) {
            working.setWisdom(wisdom);
        }
        Integer charisma = promptIntInRange(console, "Charisma", working.getCharisma(), editing, 1, 30);
        if (charisma != null) {
            working.setCharisma(charisma);
        }

        return working;
    }

    private Integer promptIntInRange(ConsoleIO console,
                                    String label,
                                    int currentValue,
                                    boolean editing,
                                    int min,
                                    int max) {
        while (true) {
            String prompt = editing
                ? "Enter " + label + " [" + currentValue + "]: "
                : "Enter " + label + " (" + min + "-" + max + "): ";
            console.print(prompt);
            String input = console.readLine().trim();
            if (input.isEmpty()) {
                if (editing) {
                    return null;
                }
                console.println("Enter a value between " + min + " and " + max + ".");
                continue;
            }
            int parsed;
            try {
                parsed = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                console.println("Enter a valid integer.");
                continue;
            }
            if (parsed < min || parsed > max) {
                console.println("Enter a value between " + min + " and " + max + ".");
                continue;
            }
            return parsed;
        }
    }

    private void handleDiceField(Object entity,
                                 PropertyDescriptor descriptor,
                                 ConsoleIO console,
                                 boolean editing,
                                 CampaignRepositories repositories) {
        List<OptionEntry> options = loadIdOptions(repositories, "Dice", repositories.dice()::list);
        if (options.isEmpty()) {
            console.println("No dice available. Create dice first.");
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

        printOptions(console, options);
        while (true) {
            String prompt = buildPrompt(descriptor.getName(), descriptor, currentValue, editing, options);
            console.print(prompt);
            String input = console.readLine().trim();
            if (input.isEmpty()) {
                break;
            }
            try {
                List<String> selected = parseOptionInput(input, options);
                if (selected.isEmpty()) {
                    console.println("Choose a dice option.");
                    continue;
                }
                String diceId = selected.get(0);
                Dice dice = repositories.dice().getById(diceId);
                if (dice == null) {
                    console.println("Unknown dice: " + diceId);
                    continue;
                }
                descriptor.getWriteMethod().invoke(entity, dice);
                break;
            } catch (IllegalArgumentException e) {
                console.println(e.getMessage());
            } catch (Exception e) {
                console.println("Failed to set " + descriptor.getName() + ": " + e.getMessage());
                break;
            }
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

    private void printOptions(ConsoleIO console, List<OptionEntry> options) {
        console.println("Available options:");
        for (int i = 0; i < options.size(); i++) {
            OptionEntry entry = options.get(i);
            console.println("  " + (i + 1) + ") " + entry.getLabel());
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
            // Best-effort: an inability to load reference options (e.g. classes/races) shouldn't
            // block the rest of the form; the field is just presented without options.
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

    private String invokeStringGetter(Object entity, String methodName) {
        if (entity == null) {
            return "";
        }
        try {
            Object value = entity.getClass().getMethod(methodName).invoke(entity);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
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

    @FunctionalInterface
    private interface RepositorySupplier {
        List<?> get();
    }
}


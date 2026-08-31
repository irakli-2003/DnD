package com.dnd.data;

import com.dnd.ui.EntityCategory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads campaign catalog entries and renders them as formatted plain text for insertion
 * into Storyline session notes.
 *
 * <p>Entities are converted generically (via Jackson, into a property map) rather than
 * with a per-type template, so every catalog - including ones added later - is supported
 * without extra code. Empty values, internal ids and image paths are skipped because they
 * add noise to prose the DM reads at the table.</p>
 */
public final class EntityInfoFormatter {

    /** Catalog properties that are plumbing rather than content, and so are never printed. */
    private static final List<String> HIDDEN_FIELDS = List.of("id", "imagePath", "passwordHash", "passwordSalt");

    private final CampaignRepositories repos;
    private final ObjectMapper mapper = JsonMappers.create();

    public EntityInfoFormatter(CampaignRepositories repos) {
        this.repos = repos;
    }

    /** Categories offered in the "Insert Info" menu, in menu order. */
    public static List<EntityCategory> insertableCategories() {
        return List.of(
            EntityCategory.NPC, EntityCategory.MONSTER, EntityCategory.BEAST, EntityCategory.ITEM,
            EntityCategory.SPELL, EntityCategory.PLACE, EntityCategory.RACE, EntityCategory.CLASS,
            EntityCategory.EFFECT, EntityCategory.DAMAGE_TYPE, EntityCategory.ALCHEMY_INGREDIENT,
            EntityCategory.BOOK, EntityCategory.LANGUAGE, EntityCategory.PLAYER);
    }

    /** Human-readable, plural menu label for a category. */
    public static String categoryLabel(EntityCategory category) {
        return switch (category) {
            case PLAYER -> "Player Characters";
            case NPC -> "NPCs";
            case MONSTER -> "Monsters";
            case BEAST -> "Beasts";
            case ITEM -> "Items";
            case SPELL -> "Spells";
            case PLACE -> "Places";
            case MAP -> "Maps";
            case CLASS -> "Classes";
            case RACE -> "Races";
            case DAMAGE_TYPE -> "Damage Types";
            case EFFECT -> "Effects";
            case LANGUAGE -> "Languages";
            case ALCHEMY_INGREDIENT -> "Alchemy Ingredients";
            case BOOK -> "Books";
            case DICE -> "Dice";
        };
    }

    /** All entries of {@code category} in the current campaign. */
    public List<?> list(EntityCategory category) {
        if (category == null) return List.of();
        return switch (category) {
            case PLAYER -> repos.players().list();
            case NPC -> repos.npcs().list();
            case MONSTER -> repos.monsters().list();
            case BEAST -> repos.beasts().list();
            case ITEM -> repos.items().list();
            case SPELL -> repos.spells().list();
            case PLACE -> repos.places().list();
            case MAP -> repos.maps().list();
            case CLASS -> repos.classes().list();
            case RACE -> repos.races().list();
            case DAMAGE_TYPE -> repos.damageTypes().list();
            case EFFECT -> repos.effects().list();
            case LANGUAGE -> repos.languages().list();
            case ALCHEMY_INGREDIENT -> repos.alchemyIngredients().list();
            case BOOK -> repos.books().list();
            case DICE -> repos.dice().list();
        };
    }

    /** Best-effort display name for a catalog entry, falling back to its id. */
    public static String nameOf(Object entity) {
        if (entity == null) return "(unnamed)";
        Object name = readProperty(entity, "getName");
        if (name != null && !name.toString().isBlank()) return name.toString();
        Object id = readProperty(entity, "getId");
        return id == null ? entity.toString() : id.toString();
    }

    /**
     * Renders {@code entity} as an indented, human-readable block suitable for pasting
     * into a session note.
     */
    public String format(EntityCategory category, Object entity) {
        if (entity == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(singular(category)).append(": ").append(nameOf(entity)).append("]\n");
        for (Map.Entry<String, Object> entry : properties(entity).entrySet()) {
            String key = entry.getKey();
            if (HIDDEN_FIELDS.contains(key) || "name".equals(key)) continue;
            String value = renderValue(entry.getValue());
            if (value.isBlank()) continue;
            sb.append("  ").append(humanize(key)).append(": ").append(value).append('\n');
        }
        return sb.toString();
    }

    private Map<String, Object> properties(Object entity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.convertValue(entity, LinkedHashMap.class);
            return map == null ? Map.of() : map;
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private String renderValue(Object value) {
        if (value == null) return "";
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object item : list) {
                String rendered = renderValue(item);
                if (!rendered.isBlank()) parts.add(rendered);
            }
            return String.join(", ", parts);
        }
        if (value instanceof Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String rendered = renderValue(e.getValue());
                if (!rendered.isBlank()) parts.add(humanize(String.valueOf(e.getKey())) + " " + rendered);
            }
            return String.join(", ", parts);
        }
        String text = String.valueOf(value).trim();
        // Zeros and "false" are usually unset defaults in these catalogs rather than meaningful
        // values, and printing them buries the fields the DM actually filled in.
        if (text.isEmpty() || text.equals("null") || text.equals("0") || text.equals("0.0") || text.equals("false")) {
            return "";
        }
        return text;
    }

    /** Turns a camelCase property name into "Title Case" words. */
    static String humanize(String key) {
        if (key == null || key.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(key.charAt(0)));
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c) && !Character.isUpperCase(key.charAt(i - 1))) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }

    private static String singular(EntityCategory category) {
        String label = categoryLabel(category);
        if (label.endsWith("ies")) return label.substring(0, label.length() - 3) + "y";
        if (label.endsWith("es") && label.endsWith("sses")) return label.substring(0, label.length() - 2);
        if (label.endsWith("s")) return label.substring(0, label.length() - 1);
        return label;
    }

    private static Object readProperty(Object entity, String getter) {
        try {
            return entity.getClass().getMethod(getter).invoke(entity);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}

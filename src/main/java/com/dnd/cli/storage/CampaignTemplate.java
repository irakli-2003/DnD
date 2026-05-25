package com.dnd.cli.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CampaignTemplate {
    public static final String WORLD_DIR = "world";
    public static final String PLAYERS_DIR = "players";

    public static final String CLASSES_FILE = "classes.json";
    public static final String RACES_FILE = "races.json";
    public static final String ITEMS_FILE = "items.json";
    public static final String SPELLS_FILE = "spells.json";
    public static final String PLACES_FILE = "places.json";
    public static final String EFFECTS_FILE = "effects.json";
    public static final String DAMAGE_TYPES_FILE = "damage-types.json";
    public static final String NPCS_FILE = "npcs.json";
    public static final String MONSTERS_FILE = "monsters.json";
    public static final String BEASTS_FILE = "beasts.json";
    public static final String PLAYERS_FILE = "players.json";
    public static final String LANGUAGES_FILE = "languages.json";
    public static final String ALCHEMY_INGREDIENTS_FILE = "alchemy-ingredients.json";
    public static final String BOOKS_FILE = "books.json";
    public static final String ID_REGISTRY_FILE = "id-registry.json";

    private CampaignTemplate() {
    }

    public static void writeTemplate(Path basePath, boolean blank) throws IOException {
        Path worldPath = basePath.resolve(WORLD_DIR);
        Path playersPath = basePath.resolve(PLAYERS_DIR);

        Files.createDirectories(worldPath);
        Files.createDirectories(playersPath);

        Files.write(worldPath.resolve(CLASSES_FILE), (blank ? blankClassesJson() : defaultClassesJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(RACES_FILE), (blank ? blankRacesJson() : defaultRacesJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(ITEMS_FILE), (blank ? blankItemsJson() : defaultItemsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(SPELLS_FILE), (blank ? blankSpellsJson() : defaultSpellsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(PLACES_FILE), (blank ? blankPlacesJson() : defaultPlacesJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(EFFECTS_FILE), (blank ? blankEffectsJson() : defaultEffectsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(DAMAGE_TYPES_FILE), (blank ? blankDamageTypesJson() : defaultDamageTypesJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(NPCS_FILE), (blank ? blankNpcsJson() : defaultNpcsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(MONSTERS_FILE), (blank ? blankMonstersJson() : defaultMonstersJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(BEASTS_FILE), (blank ? blankBeastsJson() : defaultBeastsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(playersPath.resolve(PLAYERS_FILE), (blank ? blankPlayersJson() : defaultPlayersJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(LANGUAGES_FILE), (blank ? blankLanguagesJson() : defaultLanguagesJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(ALCHEMY_INGREDIENTS_FILE), (blank ? blankAlchemyIngredientsJson() : defaultAlchemyIngredientsJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(BOOKS_FILE), (blank ? blankBooksJson() : defaultBooksJson()).getBytes(StandardCharsets.UTF_8));
        Files.write(worldPath.resolve(ID_REGISTRY_FILE), (blank ? blankIdRegistryJson() : defaultIdRegistryJson()).getBytes(StandardCharsets.UTF_8));
    }

    public static String defaultClassesJson() {
        return "{\n" +
            "  \"classes\": [\n" +
            "    {\n" +
            "      \"id\": \"fighter\",\n" +
            "      \"name\": \"Fighter\",\n" +
            "      \"description\": \"A master of martial combat.\",\n" +
            "      \"hitDie\": 10,\n" +
            "      \"primaryAbilities\": [\"strength\", \"constitution\"],\n" +
            "      \"savingThrowBonuses\": {\n" +
            "        \"strength\": 2,\n" +
            "        \"dexterity\": 0,\n" +
            "        \"constitution\": 2,\n" +
            "        \"intelligence\": 0,\n" +
            "        \"wisdom\": 0,\n" +
            "        \"charisma\": 0\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultRacesJson() {
        return "{\n" +
            "  \"races\": [\n" +
            "    {\n" +
            "      \"id\": \"human\",\n" +
            "      \"name\": \"Human\",\n" +
            "      \"description\": \"Adaptable and ambitious.\",\n" +
            "      \"abilityBonuses\": {\n" +
            "        \"strength\": 1,\n" +
            "        \"dexterity\": 1,\n" +
            "        \"constitution\": 1,\n" +
            "        \"intelligence\": 1,\n" +
            "        \"wisdom\": 1,\n" +
            "        \"charisma\": 1\n" +
            "      },\n" +
            "      \"speed\": 30\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultItemsJson() {
        return "{\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"id\": \"longsword\",\n" +
            "      \"name\": \"Longsword\",\n" +
            "      \"type\": \"weapon\",\n" +
            "      \"description\": \"A versatile steel blade.\",\n" +
            "      \"valueGold\": 15,\n" +
            "      \"weight\": 3.0,\n" +
            "      \"damage\": {\n" +
            "        \"dice\": \"1d8\",\n" +
            "        \"type\": \"slashing\"\n" +
            "      },\n" +
            "      \"durability\": {\n" +
            "        \"max\": 100,\n" +
            "        \"current\": 100\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultSpellsJson() {
        return "{\n" +
            "  \"spells\": [\n" +
            "    {\n" +
            "      \"id\": \"frost_bolt\",\n" +
            "      \"name\": \"Frost Bolt\",\n" +
            "      \"description\": \"A chilling projectile of ice.\",\n" +
            "      \"level\": 1,\n" +
            "      \"school\": \"evocation\",\n" +
            "      \"manaCost\": 5,\n" +
            "      \"range\": 30,\n" +
            "      \"radius\": 0,\n" +
            "      \"castingMethod\": \"speech\",\n" +
            "      \"requiredConsumables\": [],\n" +
            "      \"requiredTools\": [],\n" +
            "      \"concentration\": {\n" +
            "        \"difficultyLevel\": \"easy\",\n" +
            "        \"preventsMovement\": false\n" +
            "      },\n" +
            "      \"effects\": [\n" +
            "        {\n" +
            "          \"id\": \"freeze\",\n" +
            "          \"name\": \"Freeze\",\n" +
            "          \"description\": \"Reduces movement speed and slows reactions.\",\n" +
            "          \"damaging\": false,\n" +
            "          \"healing\": false,\n" +
            "          \"damageAmount\": 0,\n" +
            "          \"healingAmount\": 0\n" +
            "        }\n" +
            "      ],\n" +
            "      \"damage\": {\n" +
            "        \"amount\": 6,\n" +
            "        \"typeId\": \"cold\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultPlayersJson() {
        return "{\n" +
            "  \"players\": [\n" +
            "    {\n" +
            "      \"id\": \"player1\",\n" +
            "      \"name\": \"Alyx\",\n" +
            "      \"classId\": \"fighter\",\n" +
            "      \"raceId\": \"human\",\n" +
            "      \"level\": 1,\n" +
            "      \"stats\": {\n" +
            "        \"strength\": 15,\n" +
            "        \"dexterity\": 12,\n" +
            "        \"constitution\": 14,\n" +
            "        \"intelligence\": 10,\n" +
            "        \"wisdom\": 11,\n" +
            "        \"charisma\": 8\n" +
            "      },\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"itemId\": \"longsword\",\n" +
            "          \"condition\": {\n" +
            "            \"durability\": 100\n" +
            "          },\n" +
            "          \"equipped\": true\n" +
            "        }\n" +
            "      ],\n" +
            "      \"spells\": [\n" +
            "        {\n" +
            "          \"spellId\": \"frost_bolt\",\n" +
            "          \"rank\": 1,\n" +
            "          \"active\": true\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultPlacesJson() {
        return "{\n" +
            "  \"places\": [\n" +
            "    {\n" +
            "      \"id\": \"stonekeep\",\n" +
            "      \"name\": \"Stonekeep\",\n" +
            "      \"description\": \"A fortified keep overlooking the valley.\",\n" +
            "      \"type\": \"fortress\",\n" +
            "      \"habitat\": \"mountain\",\n" +
            "      \"tags\": [\"defensive\", \"mountain\"],\n" +
            "      \"attributes\": {\n" +
            "        \"population\": \"120\",\n" +
            "        \"faction\": \"wardens\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultEffectsJson() {
        return "{\n" +
            "  \"effects\": [\n" +
            "    {\n" +
            "      \"id\": \"freeze\",\n" +
            "      \"name\": \"Freeze\",\n" +
            "      \"description\": \"Reduces movement speed and slows reactions.\",\n" +
            "      \"damaging\": false,\n" +
            "      \"healing\": false,\n" +
            "      \"damageAmount\": 0,\n" +
            "      \"healingAmount\": 0\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultDamageTypesJson() {
        return "{\n" +
            "  \"damageTypes\": [\n" +
            "    {\n" +
            "      \"id\": \"cold\",\n" +
            "      \"name\": \"Cold\",\n" +
            "      \"description\": \"Ice, frost, and freezing damage.\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultLanguagesJson() {
        return "{\n" +
            "  \"languages\": [\n" +
            "    {\n" +
            "      \"id\": \"common\",\n" +
            "      \"name\": \"Common\",\n" +
            "      \"dictionary\": {\n" +
            "        \"hello\": \"greetings\",\n" +
            "        \"farewell\": \"safe travels\"\n" +
            "      },\n" +
            "      \"requiredMaterial\": {\n" +
            "        \"id\": \"common_tongue\",\n" +
            "        \"name\": \"Common Tongue Primer\",\n" +
            "        \"type\": \"book\",\n" +
            "        \"description\": \"A primer for the common language.\",\n" +
            "        \"valueGold\": 5,\n" +
            "        \"weight\": 1.0,\n" +
            "        \"damage\": null,\n" +
            "        \"durability\": {\n" +
            "          \"max\": 50,\n" +
            "          \"current\": 50\n" +
            "        },\n" +
            "        \"overview\": \"Basic vocabulary and grammar.\"\n" +
            "      },\n" +
            "      \"requiredLongRests\": 3\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultAlchemyIngredientsJson() {
        return "{\n" +
            "  \"ingredients\": [\n" +
            "    {\n" +
            "      \"id\": \"wolfsbane\",\n" +
            "      \"name\": \"Wolfsbane\",\n" +
            "      \"description\": \"A bitter herb used in protective concoctions.\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultBooksJson() {
        return "{\n" +
            "  \"books\": [\n" +
            "    {\n" +
            "      \"id\": \"common_tongue\",\n" +
            "      \"name\": \"Common Tongue Primer\",\n" +
            "      \"type\": \"book\",\n" +
            "      \"description\": \"A primer for the common language.\",\n" +
            "      \"valueGold\": 5,\n" +
            "      \"weight\": 1.0,\n" +
            "      \"damage\": null,\n" +
            "      \"durability\": {\n" +
            "        \"max\": 50,\n" +
            "        \"current\": 50\n" +
            "      },\n" +
            "      \"overview\": \"Basic vocabulary and grammar.\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultIdRegistryJson() {
        return "{\n" +
            "  \"entries\": [\n" +
            "    {\n" +
            "      \"id\": \"fighter\",\n" +
            "      \"file\": \"world/classes.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"human\",\n" +
            "      \"file\": \"world/races.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"longsword\",\n" +
            "      \"file\": \"world/items.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"frost_bolt\",\n" +
            "      \"file\": \"world/spells.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"stonekeep\",\n" +
            "      \"file\": \"world/places.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"freeze\",\n" +
            "      \"file\": \"world/effects.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"cold\",\n" +
            "      \"file\": \"world/damage-types.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"warden_elya\",\n" +
            "      \"file\": \"world/npcs.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"ember_wyrm\",\n" +
            "      \"file\": \"world/monsters.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"dire_wolf\",\n" +
            "      \"file\": \"world/beasts.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"common\",\n" +
            "      \"file\": \"world/languages.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"wolfsbane\",\n" +
            "      \"file\": \"world/alchemy-ingredients.json\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"common_tongue\",\n" +
            "      \"file\": \"world/books.json\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultNpcsJson() {
        return "{\n" +
            "  \"npcs\": [\n" +
            "    {\n" +
            "      \"id\": \"warden_elya\",\n" +
            "      \"name\": \"Warden Elya\",\n" +
            "      \"description\": \"Leader of Stonekeep.\",\n" +
            "      \"role\": \"commander\",\n" +
            "      \"level\": 5,\n" +
            "      \"stats\": {\n" +
            "        \"strength\": 14,\n" +
            "        \"dexterity\": 12,\n" +
            "        \"constitution\": 13,\n" +
            "        \"intelligence\": 12,\n" +
            "        \"wisdom\": 10,\n" +
            "        \"charisma\": 15\n" +
            "      },\n" +
            "      \"traits\": [\"brave\", \"strategist\"],\n" +
            "      \"languages\": [\"common\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultMonstersJson() {
        return "{\n" +
            "  \"monsters\": [\n" +
            "    {\n" +
            "      \"id\": \"ember_wyrm\",\n" +
            "      \"name\": \"Ember Wyrm\",\n" +
            "      \"description\": \"A serpent that breathes embers.\",\n" +
            "      \"type\": \"dragon\",\n" +
            "      \"challengeRating\": \"4\",\n" +
            "      \"stats\": {\n" +
            "        \"strength\": 16,\n" +
            "        \"dexterity\": 14,\n" +
            "        \"constitution\": 15,\n" +
            "        \"intelligence\": 6,\n" +
            "        \"wisdom\": 10,\n" +
            "        \"charisma\": 8\n" +
            "      },\n" +
            "      \"abilities\": [\n" +
            "        {\n" +
            "          \"id\": \"ember_breath\",\n" +
            "          \"name\": \"Ember Breath\",\n" +
            "          \"description\": \"Exhales a cone of scorching embers.\",\n" +
            "          \"effects\": [\"burn\", \"area_damage\"],\n" +
            "          \"range\": 15.0,\n" +
            "          \"recharge\": 3\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": \"wing_buffet\",\n" +
            "          \"name\": \"Wing Buffet\",\n" +
            "          \"description\": \"Strikes nearby foes with a winged gust.\",\n" +
            "          \"effects\": [\"knockback\"],\n" +
            "          \"range\": 5.0,\n" +
            "          \"recharge\": 1\n" +
            "        }\n" +
            "      ],\n" +
            "      \"languages\": [\"common\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String defaultBeastsJson() {
        return "{\n" +
            "  \"beasts\": [\n" +
            "    {\n" +
            "      \"id\": \"dire_wolf\",\n" +
            "      \"name\": \"Dire Wolf\",\n" +
            "      \"description\": \"A large and aggressive wolf.\",\n" +
            "      \"habitat\": \"forest\",\n" +
            "      \"challengeRating\": \"1\",\n" +
            "      \"stats\": {\n" +
            "        \"strength\": 14,\n" +
            "        \"dexterity\": 15,\n" +
            "        \"constitution\": 13,\n" +
            "        \"intelligence\": 3,\n" +
            "        \"wisdom\": 12,\n" +
            "        \"charisma\": 6\n" +
            "      },\n" +
            "      \"abilities\": [\n" +
            "        {\n" +
            "          \"id\": \"pack_tactics\",\n" +
            "          \"name\": \"Pack Tactics\",\n" +
            "          \"description\": \"Gains advantage on attacks when allies are nearby.\",\n" +
            "          \"effects\": [\"advantage_on_attack\"],\n" +
            "          \"range\": 0.0,\n" +
            "          \"recharge\": 0\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
    }

    public static String blankClassesJson() {
        return "{\n  \"classes\": []\n}\n";
    }

    public static String blankRacesJson() {
        return "{\n  \"races\": []\n}\n";
    }

    public static String blankItemsJson() {
        return "{\n  \"items\": []\n}\n";
    }

    public static String blankSpellsJson() {
        return "{\n  \"spells\": []\n}\n";
    }

    public static String blankPlacesJson() {
        return "{\n  \"places\": []\n}\n";
    }

    public static String blankEffectsJson() {
        return "{\n  \"effects\": []\n}\n";
    }

    public static String blankDamageTypesJson() {
        return "{\n  \"damageTypes\": []\n}\n";
    }

    public static String blankNpcsJson() {
        return "{\n  \"npcs\": []\n}\n";
    }

    public static String blankMonstersJson() {
        return "{\n  \"monsters\": []\n}\n";
    }

    public static String blankBeastsJson() {
        return "{\n  \"beasts\": []\n}\n";
    }

    public static String blankPlayersJson() {
        return "{\n  \"players\": []\n}\n";
    }

    public static String blankLanguagesJson() {
        return "{\n  \"languages\": []\n}\n";
    }

    public static String blankAlchemyIngredientsJson() {
        return "{\n  \"ingredients\": []\n}\n";
    }

    public static String blankBooksJson() {
        return "{\n  \"books\": []\n}\n";
    }

    public static String blankIdRegistryJson() {
        return "{\n  \"entries\": []\n}\n";
    }
}

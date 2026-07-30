package com.dnd.cli.pages;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.JsonRepository;
import com.dnd.model.alchemy.AlchemyIngredient;
import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.CharacterRace;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.combat.DamageType;
import com.dnd.model.combat.Effect;
import com.dnd.model.creature.Beast;
import com.dnd.model.creature.Monster;
import com.dnd.model.creature.Npc;
import com.dnd.model.item.Item;
import com.dnd.model.item.books.Book;
import com.dnd.model.magic.Spell;
import com.dnd.model.world.Dice;
import com.dnd.model.world.Language;
import com.dnd.model.world.Place;

import java.util.function.Function;

public enum EntityType {
    CLASS("class", "Class", CharacterClass.class, CampaignRepositories::classes, "world/classes.json"),
    RACE("race", "Race", CharacterRace.class, CampaignRepositories::races, "world/races.json"),
    ITEM("item", "Item", Item.class, CampaignRepositories::items, "world/items.json"),
    SPELL("spell", "Spell", Spell.class, CampaignRepositories::spells, "world/spells.json"),
    PLACE("place", "Place", Place.class, CampaignRepositories::places, "world/places.json"),
    EFFECT("effect", "Effect", Effect.class, CampaignRepositories::effects, "world/effects.json"),
    DAMAGE_TYPE("damage-type", "Damage Type", DamageType.class, CampaignRepositories::damageTypes, "world/damage-types.json"),
    BEAST("beast", "Beast", Beast.class, CampaignRepositories::beasts, "world/beasts.json"),
    MONSTER("monster", "Monster", Monster.class, CampaignRepositories::monsters, "world/monsters.json"),
    NPC("npc", "NPC", Npc.class, CampaignRepositories::npcs, "world/npcs.json"),
    LANGUAGE("language", "Language", Language.class, CampaignRepositories::languages, "world/languages.json"),
    ALCHEMY_INGREDIENT("alchemy", "Alchemy Ingredient", AlchemyIngredient.class, CampaignRepositories::alchemyIngredients, "world/alchemy-ingredients.json"),
    BOOK("book", "Book", Book.class, CampaignRepositories::books, "world/books.json"),
    DICE("dice", "Dice", Dice.class, CampaignRepositories::dice, "world/dice.json"),
    PLAYER("player", "Player", PlayerCharacter.class, CampaignRepositories::players, "players/players.json");

    private final String key;
    private final String label;
    private final Class<?> modelClass;
    private final Function<CampaignRepositories, JsonRepository<?, ?>> repositoryProvider;
    private final String registryPath;

    EntityType(String key,
               String label,
               Class<?> modelClass,
               Function<CampaignRepositories, JsonRepository<?, ?>> repositoryProvider,
               String registryPath) {
        this.key = key;
        this.label = label;
        this.modelClass = modelClass;
        this.repositoryProvider = repositoryProvider;
        this.registryPath = registryPath;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public Class<?> getModelClass() {
        return modelClass;
    }

    public JsonRepository<?, ?> getRepository(CampaignRepositories repositories) {
        return repositoryProvider.apply(repositories);
    }

    public String getRegistryPath() {
        return registryPath;
    }
}

package com.dnd.model.rules;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.JsonMappers;
import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.CharacterRace;
import com.dnd.model.character.ClassFeature;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.Progression;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.item.Item;
import com.dnd.model.world.map.CombatState;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class FeatureRulesTest {

    private static CharacterClass barbarian() {
        CharacterClass cls = new CharacterClass();
        cls.setId("barbarian");
        cls.setName("Barbarian");
        cls.setFeatures(List.of(
            new ClassFeature(1, "Rage", "Rage."),
            new ClassFeature(1, "Unarmored Defense", "AC."),
            new ClassFeature(2, "Reckless Attack", "Reckless."),
            new ClassFeature(2, "Danger Sense", "Dex saves."),
            new ClassFeature(5, "Extra Attack", "Two attacks."),
            new ClassFeature(5, "Fast Movement", "+10 ft."),
            new ClassFeature(9, "Brutal Critical (1 die)", "Crits."),
            new ClassFeature(3, "Frenzy", "Berserker only.", "Path of the Berserker")));
        return cls;
    }

    private static CharacterClass fighter() {
        CharacterClass cls = new CharacterClass();
        cls.setId("fighter");
        cls.setName("Fighter");
        cls.setFeatures(List.of(
            new ClassFeature(1, "Second Wind", "Heal."),
            new ClassFeature(2, "Action Surge (one use)", "Extra action.")));
        return cls;
    }

    private static PlayerCharacter pc(String classId, String raceId, int level, CoreStats stats) {
        return new PlayerCharacter("pc", "Hero", classId, raceId, level, stats, new ArrayList<>(), new ArrayList<>());
    }

    private static PlayerCharacter darius() {
        return pc("barbarian", "human", 5, new CoreStats(17, 15, 16, 8, 8, 8));
    }

    private static void carry(PlayerCharacter pc, String itemId) {
        pc.getItems().add(new PlayerCharacter.PlayerItem(itemId, new PlayerCharacter.ItemCondition(100), true));
    }

    private static Item item(String json) {
        try {
            return JsonMappers.create().readValue(json, Item.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final Map<String, Item> ITEMS = Map.of(
        "greataxe", item("{\"id\":\"greataxe\",\"name\":\"Greataxe\",\"type\":\"weapon\","
            + "\"damage\":{\"dice\":\"1d12\",\"type\":\"slashing\"}}"),
        "plate", item("{\"id\":\"plate\",\"name\":\"Heavy Plate\",\"type\":\"armor\",\"armorClassBonus\":8}"),
        "leather", item("{\"id\":\"leather\",\"name\":\"Leather Armor\",\"type\":\"armor\",\"armorClassBonus\":1}"),
        "shield", item("{\"id\":\"shield\",\"name\":\"Shield\",\"type\":\"armor\",\"armorClassBonus\":2}"),
        "revolver", item("{\"id\":\"revolver\",\"name\":\"Gun\",\"type\":\"weapon\","
            + "\"description\":\"A six-shot revolver.\",\"damage\":{\"dice\":\"1d10\",\"type\":\"force\"}}"));

    @Test
    public void level5BarbarianGetsRageWithThreeUsesAndPlusTwo() {
        FeatureRules.Kit kit = FeatureRules.kit(darius(), barbarian(), null);
        FeatureRules.Action rage = kit.action(FeatureRules.RAGE);
        assertNotNull(rage);
        assertEquals(3, rage.getMaxUses());
        assertEquals(FeatureRules.Recharge.LONG_REST, rage.getRecharge());
        assertEquals(2, rage.getEffect().getMeleeDamageBonus());
        assertEquals(FeatureRules.PHYSICAL, rage.getEffect().getResistances());
        assertNotNull(kit.action("reckless-attack"));
        assertEquals(1, kit.getExtraAttacks());
        assertEquals(FeatureRules.Defense.BARBARIAN, kit.getDefense());
    }

    @Test
    public void featuresAboveLevelOrForOtherSubclassesAreLeftOut() {
        FeatureRules.Kit kit = FeatureRules.kit(darius(), barbarian(), null);
        List<String> names = kit.getPassives().stream().map(FeatureRules.Passive::name).toList();
        assertTrue(names.contains("Danger Sense"));
        assertFalse(names.contains("Brutal Critical"));
        assertFalse(names.contains("Frenzy"));
    }

    @Test
    public void ragingHalvesPhysicalDamageButNotFire() {
        FeatureRules.Kit kit = FeatureRules.kit(darius(), barbarian(), null);
        CombatState state = new CombatState();
        state.setMaxHitPoints(55);
        state.setCurrentHitPoints(55);
        String log = FeatureRules.useOnSelf(kit.action(FeatureRules.RAGE), state, "Darius", new Random(1));
        assertTrue(log.contains("Rage"));
        assertEquals(1, (int) state.getFeatureUses().get(FeatureRules.RAGE));
        assertEquals(2, state.meleeDamageBonus());

        CombatRules.Hit slash = CombatRules.damage(state, 11, "slashing");
        assertEquals(5, slash.dealt());
        assertTrue(slash.resisted());
        assertEquals(50, state.getCurrentHitPoints());
        assertEquals(10, CombatRules.damage(state, 10, "fire").dealt());
    }

    @Test
    public void unarmoredDefenseAddsConAndFastMovementNeedsNoHeavyArmor() {
        PlayerCharacter pc = darius();
        FeatureRules.Kit kit = FeatureRules.kit(pc, barbarian(), null);
        assertEquals(10 + 2 + 3, FeatureRules.armorClass(pc, kit, ITEMS::get, 0).total());
        assertEquals(10, kit.speedBonus(false, false));

        carry(pc, "plate");
        assertEquals(18, FeatureRules.armorClass(pc, kit, ITEMS::get, 0).total());
        FeatureRules.Worn worn = FeatureRules.worn(pc, ITEMS::get);
        assertTrue(worn.heavy());
        assertEquals(0, kit.speedBonus(worn.wearingArmor(), worn.heavy()));
    }

    @Test
    public void lightArmorAndShieldAddUp() {
        PlayerCharacter pc = pc("cleric", "tiefling", 5, new CoreStats(8, 14, 15, 8, 15, 15));
        carry(pc, "leather");
        carry(pc, "shield");
        assertEquals(10 + 1 + 2 + 2, FeatureRules.armorClass(pc, null, ITEMS::get, 0).total());
    }

    @Test
    public void speedBonusIsAppliedOnceAndAdjustedByDifference() {
        FeatureRules.Kit kit = FeatureRules.kit(darius(), barbarian(), null);
        CombatState state = new CombatState();
        state.setWalkSpeed(30);
        FeatureRules.seed(state, kit, new FeatureRules.Worn(0, 0, 0));
        FeatureRules.seed(state, kit, new FeatureRules.Worn(0, 0, 0));
        assertEquals(40, state.getWalkSpeed());
        FeatureRules.seed(state, kit, new FeatureRules.Worn(8, 0, 0));
        assertEquals(30, state.getWalkSpeed());
    }

    @Test
    public void attacksIncludeWeaponsAndFistsButNotFirearms() {
        PlayerCharacter pc = darius();
        carry(pc, "greataxe");
        carry(pc, "revolver");
        FeatureRules.Kit kit = FeatureRules.kit(pc, barbarian(), null);
        List<FeatureRules.Attack> attacks = FeatureRules.attacks(pc, kit, ITEMS::get);
        assertEquals(2, attacks.size());
        FeatureRules.Attack axe = attacks.get(0);
        assertEquals("Greataxe", axe.name());
        assertEquals(3 + 3, axe.toHit());
        assertEquals(3, axe.damageBonus());
        assertTrue(axe.strengthBased() && axe.melee());
        FeatureRules.Attack fist = attacks.get(1);
        assertEquals("unarmed", fist.id());
        assertEquals(4, fist.damageBonus());
    }

    @Test
    public void secondWindHealsAndComesBackOnShortRestButRageDoesNot() {
        PlayerCharacter pc = pc("fighter", "human", 5, new CoreStats(15, 15, 15, 8, 8, 8));
        FeatureRules.Action wind = FeatureRules.kit(pc, fighter(), null).action("second-wind");
        CombatState state = new CombatState();
        state.setMaxHitPoints(50);
        state.setCurrentHitPoints(20);
        FeatureRules.useOnSelf(wind, state, "Lavenos", new Random(3));
        assertTrue(state.getCurrentHitPoints() >= 26 && state.getCurrentHitPoints() <= 35);
        assertEquals(0, wind.usesLeft(state.getFeatureUses()));
        assertTrue(FeatureRules.useOnSelf(wind, state, "Lavenos", new Random()).contains("spent"));

        state.getFeatureUses().put(FeatureRules.RAGE, 2);
        Progression.shortRest(state, false, 0, 5);
        assertFalse(state.getFeatureUses().containsKey("second-wind"));
        assertEquals(2, (int) state.getFeatureUses().get(FeatureRules.RAGE));
        Progression.longRest(state);
        assertTrue(state.getFeatureUses().isEmpty());
    }

    @Test
    public void featureAlreadyOnTheSheetAsASpellIsNotDuplicated() {
        PlayerCharacter pc = pc("fighter", "human", 5, new CoreStats(15, 15, 15, 8, 8, 8));
        pc.getSpells().add(new PlayerCharacter.PlayerSpell("second-wind", 1, true));
        assertNull(FeatureRules.kit(pc, fighter(), null).action("second-wind"));
    }

    @Test
    public void halfOrcEnduranceKeepsThemUpOnce() {
        PlayerCharacter pc = pc("fighter", "half-orc", 3, new CoreStats(16, 10, 14, 8, 8, 8));
        FeatureRules.Kit kit = FeatureRules.kit(pc, fighter(),
            new CharacterRace("half-orc", "Half-Orc", "", Map.of(), 30));
        CombatState state = new CombatState();
        state.setMaxHitPoints(20);
        state.setCurrentHitPoints(5);
        FeatureRules.seed(state, kit, null);

        CombatRules.Hit first = CombatRules.damage(state, 9, null);
        assertTrue(first.endured());
        assertEquals(1, state.getCurrentHitPoints());
        assertFalse(state.isDowned());

        CombatRules.Hit second = CombatRules.damage(state, 5, null);
        assertFalse(second.endured());
        assertEquals(0, state.getCurrentHitPoints());
    }

    @Test
    public void raceTraitsResolveThroughSubraces() {
        assertEquals("dwarf", FeatureRules.raceKey("hill-dwarf"));
        assertEquals("elf", FeatureRules.raceKey("wood-elf"));
        assertEquals("half-elf", FeatureRules.raceKey("half-elf"));
        PlayerCharacter tasha = pc("cleric", "tiefling", 5, new CoreStats(8, 8, 15, 8, 15, 15));
        assertTrue(FeatureRules.kit(tasha, null, null).getResistances().contains("fire"));
    }

    @Test
    public void layOnHandsSpendsPoolPoints() {
        CharacterClass paladin = new CharacterClass();
        paladin.setId("paladin");
        paladin.setName("Paladin");
        paladin.setFeatures(List.of(new ClassFeature(1, "Lay on Hands", "Pool.")));
        PlayerCharacter pc = pc("paladin", "human", 4, new CoreStats(16, 10, 14, 8, 8, 14));
        FeatureRules.Action lay = FeatureRules.kit(pc, paladin, null).action("lay-on-hands");
        CombatState user = new CombatState();
        CombatState ally = new CombatState();
        ally.setMaxHitPoints(30);
        ally.setCurrentHitPoints(10);
        FeatureRules.useHealPool(lay, user, "Pal", ally, "Ally", 7);
        assertEquals(17, ally.getCurrentHitPoints());
        assertEquals(13, lay.usesLeft(user.getFeatureUses()));
    }

    @Test
    public void everyCampaignCharacterGetsAKitAndDariusCanRage() throws Exception {
        CampaignRepositories repos = new CampaignRepositories(
            Paths.get("src", "main", "resources", "data", "custom-campaigns", "zurabaach"));
        Map<String, Item> items = new HashMap<>();
        repos.items().list().forEach(i -> items.put(i.getId(), i));
        StringBuilder report = new StringBuilder();
        for (PlayerCharacter pc : repos.players().list()) {
            CharacterClass cls = repos.classes().list().stream()
                .filter(c -> c.getId().equals(pc.getClassId())).findFirst().orElse(null);
            CharacterRace race = repos.races().list().stream()
                .filter(r -> r.getId().equals(pc.getRaceId())).findFirst().orElse(null);
            FeatureRules.Kit kit = FeatureRules.kit(pc, cls, race);
            FeatureRules.ArmorClass ac = FeatureRules.armorClass(pc, kit, items::get, 0);
            List<FeatureRules.Attack> attacks = FeatureRules.attacks(pc, kit, items::get);
            assertFalse(pc.getId(), attacks.isEmpty());
            report.append(pc.getId()).append(" AC ").append(ac.total())
                .append(" actions ").append(kit.getActions().stream().map(FeatureRules.Action::getId).toList())
                .append(" attacks ").append(attacks.stream().map(FeatureRules.Attack::name).toList())
                .append(" res ").append(kit.getResistances()).append('\n');
            if ("darius".equals(pc.getId())) {
                assertNotNull(report.toString(), kit.action(FeatureRules.RAGE));
            }
        }
        System.out.println(report);
    }

    @Test
    public void copyKeepsFeatureState() {
        CombatState state = new CombatState();
        state.getFeatureUses().put(FeatureRules.RAGE, 1);
        state.setResistances(List.of("fire"));
        state.setEnduranceFeature("relentless-endurance");
        state.setFeatureSpeedBonus(10);
        CombatState copy = state.copy();
        assertEquals(1, (int) copy.getFeatureUses().get(FeatureRules.RAGE));
        assertTrue(copy.resists("fire"));
        assertEquals("relentless-endurance", copy.getEnduranceFeature());
        assertEquals(10, copy.getFeatureSpeedBonus());
    }
}

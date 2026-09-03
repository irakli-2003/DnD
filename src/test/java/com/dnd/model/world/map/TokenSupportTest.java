package com.dnd.model.world.map;

import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.creature.Monster;
import org.junit.Test;

import static org.junit.Assert.*;

public class TokenSupportTest {

    private PlayerToken player(String name, int level, int constitution) {
        PlayerCharacter pc = new PlayerCharacter();
        pc.setId("pc-1");
        pc.setName(name);
        pc.setLevel(level);
        CoreStats stats = new CoreStats();
        stats.setConstitution(constitution);
        pc.setStats(stats);
        return new PlayerToken(pc);
    }

    @Test
    public void nameFallsBackWhenTheModelIsMissing() {
        assertEquals("Aria", TokenSupport.nameOf(player("Aria", 1, 10)));
        assertEquals("Player", TokenSupport.nameOf(new PlayerToken()));
        assertEquals("Monster", TokenSupport.nameOf(new MonsterToken()));
        assertEquals("(none)", TokenSupport.nameOf(null));
    }

    @Test
    public void kindNamesEveryTokenType() {
        assertEquals("Player", TokenSupport.kindOf(new PlayerToken()));
        assertEquals("NPC", TokenSupport.kindOf(new NpcToken()));
        assertEquals("Monster", TokenSupport.kindOf(new MonsterToken()));
        assertEquals("Beast", TokenSupport.kindOf(new BeastToken()));
        assertEquals("Object", TokenSupport.kindOf(new MapEntity()));
    }

    @Test
    public void onlyCreatureTokensCountAsCombatants() {
        assertTrue(TokenSupport.isCreature(new PlayerToken()));
        assertTrue(TokenSupport.isCreature(new MonsterToken()));
        assertFalse(TokenSupport.isCreature(new MapEntity()));
    }

    @Test
    public void abilityModifierFollowsTheStandardFormula() {
        assertEquals(-1, TokenSupport.modifier(8));
        assertEquals(0, TokenSupport.modifier(10));
        assertEquals(0, TokenSupport.modifier(11));
        assertEquals(3, TokenSupport.modifier(16));
        assertEquals(-3, TokenSupport.modifier(4));
    }

    @Test
    public void combatStateIsCreatedOnceAndThenReused() {
        PlayerToken token = player("Aria", 3, 14);
        CombatState first = TokenSupport.combatOf(token);
        assertNotNull(first);
        assertSame("state must persist on the token, not be rebuilt each call",
            first, TokenSupport.combatOf(token));
        assertSame(first, token.getCombat());
    }

    @Test
    public void defaultHitPointsScaleWithLevelAndConstitution() {
        CombatState weak = TokenSupport.combatOf(player("Weak", 1, 8));
        CombatState tough = TokenSupport.combatOf(player("Tough", 5, 18));
        assertTrue(tough.getMaxHitPoints() > weak.getMaxHitPoints());
        assertEquals(weak.getMaxHitPoints(), weak.getCurrentHitPoints());
        assertTrue("even a frail level-1 character has at least one hit point",
            weak.getMaxHitPoints() >= 1);
    }

    @Test
    public void creaturesJoinInitiativeButObjectsDoNot() {
        assertTrue(TokenSupport.combatOf(player("Aria", 1, 10)).isInInitiative());
        assertFalse(TokenSupport.combatOf(new MapEntity()).isInInitiative());
    }

    @Test
    public void levelUsesChallengeRatingForMonsters() {
        Monster monster = new Monster();
        monster.setId("m1");
        monster.setName("Ogre");
        monster.setChallengeRating(com.dnd.model.creature.ChallengeRating.CR_5);
        assertTrue(TokenSupport.levelOf(new MonsterToken(monster)) > 1);
        assertEquals("an unknown creature is treated as level one",
            1, TokenSupport.levelOf(new MonsterToken()));
    }

    @Test
    public void initiativeRollsStayWithinTheD20PlusModifierRange() {
        PlayerToken token = player("Aria", 1, 10);
        CoreStats stats = token.getCharacter().getStats();
        stats.setDexterity(14);
        for (int i = 0; i < 200; i++) {
            int roll = TokenSupport.rollInitiative(token);
            assertTrue("roll was " + roll, roll >= 1 + 2 && roll <= 20 + 2);
        }
    }

    @Test
    public void spellAndItemIdsAreEmptyForNonPlayers() {
        assertTrue(TokenSupport.spellIdsOf(new MonsterToken()).isEmpty());
        assertTrue(TokenSupport.itemIdsOf(new MonsterToken()).isEmpty());
        assertTrue(TokenSupport.abilitiesOf(new PlayerToken()).isEmpty());
    }

    @Test
    public void levelIsSettableOnPlayersAndNpcsButNotMonstersOrBeasts() {
        PlayerToken player = player("Aria", 3, 10);
        assertTrue(TokenSupport.hasSettableLevel(player));
        TokenSupport.setLevelOf(player, 7);
        assertEquals(7, TokenSupport.levelOf(player));

        com.dnd.model.creature.Npc npc = new com.dnd.model.creature.Npc();
        npc.setId("npc-1");
        npc.setLevel(2);
        NpcToken npcToken = new NpcToken(npc);
        assertTrue(TokenSupport.hasSettableLevel(npcToken));
        TokenSupport.setLevelOf(npcToken, 9);
        assertEquals(9, TokenSupport.levelOf(npcToken));

        Monster monster = new Monster();
        monster.setId("m1");
        assertFalse(TokenSupport.hasSettableLevel(new MonsterToken(monster)));
    }

    @Test
    public void xpTracksOnlyOnPlayerCharacters() {
        PlayerToken player = player("Aria", 1, 10);
        assertEquals(0, TokenSupport.xpOf(player));
        TokenSupport.addXpOf(player, 150);
        assertEquals(150, TokenSupport.xpOf(player));
        TokenSupport.addXpOf(player, -400);
        assertEquals("XP must not go negative", 0, TokenSupport.xpOf(player));

        assertEquals("monsters don't track XP", -1, TokenSupport.xpOf(new MonsterToken()));
    }

    /**
     * A DM who set a character's vitals from the storyline editor (see
     * StorylineEditorWindow's "Manage Player" panel) before ever placing them on a map
     * should see a freshly placed token start from those numbers, not the generic
     * level-based estimate.
     */
    @Test
    public void placingATokenSeedsCombatStateFromPresetCharacterVitals() {
        PlayerToken token = player("Aria", 1, 10);
        token.getCharacter().setMaxHitPoints(40);
        token.getCharacter().setCurrentHitPoints(25);
        token.getCharacter().setMaxMana(12);
        token.getCharacter().setCurrentMana(9);

        CombatState state = TokenSupport.combatOf(token);
        assertEquals(40, state.getMaxHitPoints());
        assertEquals(25, state.getCurrentHitPoints());
        assertEquals(12, state.getMaxMana());
        assertEquals(9, state.getCurrentMana());
    }

    @Test
    public void placingATokenWithoutPresetVitalsFallsBackToTheLevelEstimate() {
        CombatState state = TokenSupport.combatOf(player("Weak", 1, 8));
        assertTrue("must fall back to the generic estimate when the sheet has no vitals set",
            state.getMaxHitPoints() > 0);
    }
}

package tong.statmod.dungeon.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DungeonSpellcasterGoalContractTest {
    @Test void casterRolesAreWiredIdempotentlyWithoutDuplicatingPartyAi() throws Exception {
        String wiring = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/DungeonTacticalGoals.java"));
        assertTrue(wiring.contains("DungeonSpellcasterGoal.class"));
        assertTrue(wiring.contains("ELEMENTAL_CASTER"));
        assertTrue(wiring.contains("BATTLE_CLERIC"));
        assertTrue(wiring.contains("NECROMANCER"));
        assertTrue(wiring.contains("HEXER"));
        assertTrue(wiring.contains("ARCANE_ARTILLERY"));
        assertTrue(wiring.contains("PartyRole.TAG"));
    }

    @Test void spellcasterUsesStrictRoomSquadsAndIndependentIntentCooldowns() throws Exception {
        String goal = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/ai/goal/DungeonSpellcasterGoal.java"));
        assertTrue(goal.contains("DungeonAiActor.SQUAD_TAG"));
        assertTrue(goal.contains("squadId.equals"));
        assertTrue(goal.contains("EnumMap<IronSpellIntent, Integer>"));
        assertTrue(goal.contains("IronSpellIntentPolicy.choose"));
        assertTrue(goal.contains("IronsCasterSpells.cast"));
        assertTrue(goal.contains("SUMMON_SQUAD_TAG"));
        assertTrue(goal.contains("MAX_SUMMONS_PER_SQUAD = 2"));
    }

    @Test void rivalPartyMageAlsoUsesTheTacticalIntentPolicy() throws Exception {
        String bridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronsCasterSpells.java"));
        assertTrue(bridge.contains("IronSpellIntentPolicy.choose"));
        assertTrue(bridge.contains("sameSquadCriticalAlly"));
    }
}

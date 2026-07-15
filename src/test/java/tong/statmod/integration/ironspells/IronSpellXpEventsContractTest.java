package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronSpellXpEventsContractTest {
    @Test
    void acceptsOnlyCommittedServerSpellbookCastsAndUsesOriginalValues()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java"));

        assertTrue(source.contains("@Mod.EventBusSubscriber"));
        assertTrue(source.contains("bus = Mod.EventBusSubscriber.Bus.FORGE"));
        assertTrue(source.contains("@SubscribeEvent"));
        assertTrue(source.contains("SpellOnCastEvent"));
        assertTrue(source.contains("instanceof ServerPlayer player"));
        assertFalse(source.contains("XpAwardService.isEligible(player)"));
        assertTrue(source.contains(
                "event.getCastSource() != CastSource.SPELLBOOK"));
        assertTrue(source.contains("event.getSpellId().isBlank()"));
        assertTrue(source.contains("event.getOriginalSpellLevel()"));
        assertTrue(source.contains("event.getOriginalManaCost()"));
        assertTrue(source.contains("XpAction.spellCast("));
        assertEquals(1, occurrences(source, "XpAwardService.awardSpellCast("));
        assertFalse(source.contains("XpAwardService.award("));
        assertFalse(source.contains("event.getSpellLevel()"));
        assertFalse(source.contains("event.getManaCost()"));
        assertFalse(source.contains("StatType."));
        for (String forbidden : new String[] {
                "SpellDamageEvent", "SpellHealEvent", "SpellSummonEvent",
                "SpellTeleportEvent", "getTarget", "HitResult", "getDamage",
                "getHealAmount", "getSchoolType"
        }) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length())
                / token.length();
    }
}

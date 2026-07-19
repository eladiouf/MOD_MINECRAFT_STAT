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
        String cast = method(source, "public static void onSpellCast(",
                "public static void onSpellInscribed(");

        assertTrue(source.contains("@Mod.EventBusSubscriber"));
        assertTrue(source.contains("bus = Mod.EventBusSubscriber.Bus.FORGE"));
        assertTrue(source.contains("@SubscribeEvent"));
        assertTrue(source.contains("SpellOnCastEvent"));
        assertTrue(source.contains("instanceof ServerPlayer player"));
        assertFalse(cast.contains("XpAwardService.isEligible(player)"));
        assertTrue(cast.contains(
                "event.getCastSource() != CastSource.SPELLBOOK"));
        assertTrue(cast.contains("event.getSpellId().isBlank()"));
        assertTrue(cast.contains("event.getOriginalSpellLevel()"));
        assertTrue(cast.contains("event.getOriginalManaCost()"));
        assertTrue(cast.contains("XpAction.spellCast("));
        assertEquals(1, occurrences(cast, "XpAwardService.awardSpellCast("));
        assertFalse(cast.contains("XpAwardService.award("));
        assertFalse(cast.contains("event.getSpellLevel()"));
        assertFalse(cast.contains("event.getManaCost()"));
        assertFalse(source.contains("StatType."));
        for (String forbidden : new String[] {
                "SpellDamageEvent", "SpellHealEvent", "SpellSummonEvent",
                "SpellTeleportEvent", "getTarget", "HitResult", "getDamage",
                "getHealAmount", "getSchoolType"
        }) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void rewardsCommittedInscriptionsAndFinalReceivedSpellDamage() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java"));
        String inscription = method(source, "@SubscribeEvent(priority = EventPriority.LOWEST)",
                "public static void onSpellDamageReceived(");
        String damage = source.substring(source.indexOf(
                "public static void onSpellDamageReceived("));

        assertTrue(inscription.contains("EventPriority.LOWEST"));
        assertTrue(inscription.contains("InscribeSpellEvent"));
        assertTrue(inscription.contains("event.isCanceled()"));
        assertTrue(inscription.contains("SpellData data = event.getSpellData()"));
        assertTrue(inscription.contains("data.getLevel() <= 0"));
        assertTrue(inscription.contains("XpAction.spellInscribed("));
        assertEquals(1, occurrences(inscription, "XpAwardService.award("));

        assertTrue(damage.contains("LivingDamageEvent"));
        assertTrue(damage.contains("instanceof SpellDamageSource"));
        assertTrue(damage.contains("event.getAmount()"));
        assertTrue(damage.contains("XpAction.magicDamageReceived("));
        assertTrue(damage.contains("action.withOpponent(attacker.getUUID())"));
        assertFalse(source.contains("import io.redspace.ironsspellbooks.api.events.SpellDamageEvent"));
        assertFalse(damage.contains("kill"));
        assertFalse(damage.contains("getSchoolType"));
    }

    private static String method(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from);
        return source.substring(from, to);
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length())
                / token.length();
    }
}

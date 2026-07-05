package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkEffectHandlerSourceTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/tong/statmod/perks/PerkEffectHandler.java");

    @Test
    void combatWeaponPerksDelegateToWeaponFamilyGate() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_ACTIVE)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BLADE_ACTIVE)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.RAPID_ACTIVE)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_MASTERY)"));
        assertTrue(source.contains("PerkCombatScaling.bruteTranscendenceDamageMultiplier"));
        assertTrue(source.contains("PerkCombatScaling.precisionMarkedProjectileDamageMultiplier"));
    }

    @Test
    void killAndParryWeaponPerksDelegateToWeaponFamilyGate() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("StatType killWeaponStat = WeaponResolver.statFor(player.getMainHandItem());"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.BLADE_MASTERY)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.BRUTE_TRANSCENDENCE)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.RAPID_SYNERGY)"));
        assertTrue(source.contains("PerkCombatScaling.canUseWeaponFamilyPerk(victimWeaponStat, Perk.BLADE_SITUATIONAL)"));
    }

    @Test
    void bladeComboIsRecordedOnlyFromRealDamageEvents() throws IOException {
        String source = Files.readString(SOURCE);
        int tickIndex = source.indexOf("public static void onPlayerTick");
        int damageIndex = source.indexOf("public static void onLivingDamagePre");
        String tickBody = source.substring(tickIndex, damageIndex);

        assertFalse(tickBody.contains("recordComboHit"),
                "BLADE_ACTIVE combo must be driven by actual hits, not passive ticks.");
    }

    @Test
    void bladeTranscendenceUsesActivePostKillWindow() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("PerkCombatScaling.bladePostKillDamageMultiplier"));
        assertFalse(source.contains("!PerkState.isOnCooldown(uuid, 11, 5000L)"),
                "The post-kill blade window is represented by an active cooldown expiry.");
    }

    @Test
    void precisionActiveUsesProjectileImpactToPierceSecondaryTarget() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("ProjectileImpactEvent"));
        assertTrue(source.contains("PerkCombatScaling.canPierceSecondaryTarget"));
        assertTrue(source.contains("findPiercingSecondaryTarget"));
    }

    @Test
    void rapidDodgePerksHaveRuntimeHooksOnActualDodges() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("Perk.RAPID_MASTERY"));
        assertTrue(source.contains("Perk.RAPID_TRANSCENDENCE"));
        assertTrue(source.contains("PerkMobilityScaling.rapidDodgeSlowdownDurationTicks"));
        assertTrue(source.contains("PerkMobilityScaling.rapidPerfectDodgeStopDurationTicks"));
        assertTrue(source.contains("applyRapidDodgeEffects"));
    }

    @Test
    void trackingAndSensePerksHaveRuntimeHooks() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("Perk.TRACK_SITUATIONAL"));
        assertTrue(source.contains("Perk.SENSE_SYNERGY"));
        assertTrue(source.contains("Perk.SENSE_TRANSCENDENCE"));
        assertTrue(source.contains("PerkPerceptionScaling.scentTrailDurationTicks"));
        assertTrue(source.contains("PerkPerceptionScaling.hiddenMobRevealDurationTicks"));
        assertTrue(source.contains("PerkPerceptionScaling.canRevealHealthReadout"));
    }

    @Test
    void forgeSituationalSharpeningWindowAffectsCombatDamage() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("Perk.FORGE_SITUATIONAL"));
        assertTrue(source.contains("CraftingSupportEffectHandler.sharpeningDamageMultiplier"));
        assertTrue(source.contains("PerkState.isOnCooldown(uuid, Perk.FORGE_SITUATIONAL.id"));
    }
}

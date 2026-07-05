package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.RaceData;
import tong.statmod.integration.RaceModifierRegistry;
import tong.statmod.integration.TensuraEventSubscriber;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.perks.PerkManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraRaceHandlerTest {

    @Test
    void normalizesNullAndBlankRaceIdsToHuman() {
        assertEquals("tensura:human", TensuraRaceHandler.normalizeRaceId(null));
        assertEquals("tensura:human", TensuraRaceHandler.normalizeRaceId("   "));
    }

    @Test
    void normalizesNamespaceAndCase() {
        assertEquals("tensura:elf", TensuraRaceHandler.normalizeRaceId("Tensura:Elf"));
        assertEquals("custom:beastfolk", TensuraRaceHandler.normalizeRaceId("custom:BeastFolk"));
    }

    @Test
    void exposesRegistryBackedRaceBonuses() {
        RaceData dwarf = RaceModifierRegistry.get("tensura:dwarf");
        assertTrue(dwarf.modifiers().stream().anyMatch(mod -> mod.statIndex() == 18 && mod.flatBonus() == 2));
        assertTrue(dwarf.modifiers().stream().anyMatch(mod -> mod.statIndex() == 4 && mod.flatBonus() == 1));
    }

    @Test
    void magicalRacesExposeCastingSpeedAndManaPoolBonuses() {
        assertHasMagicRaceBonuses("tensura:human_saint");
        assertHasMagicRaceBonuses("tensura:elf");
        assertHasMagicRaceBonuses("tensura:slime");
        assertHasMagicRaceBonuses("tensura:vampire");
        assertHasMagicRaceBonuses("tensura:lesser_daemon");
        assertHasMagicRaceBonuses("tensura:divine_dragon");
    }

    @Test
    void autoRespecRefundsPerksThatNoLongerMatchRace() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BLADE_TRANSCENDENCE.id);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        int refunded = TensuraRaceHandler.autoRespecRacePerks(data, "tensura:human");

        assertEquals(2, refunded);
        assertFalse(data.isPerkUnlocked(Perk.BLADE_TRANSCENDENCE.id));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertEquals(2, data.getPerkPointsForStat(Perk.BLADE_TRANSCENDENCE.stat.index));
    }

    @Test
    void autoRespecKeepsPerksValidForNewRace() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(Perk.BLADE_TRANSCENDENCE.id);

        int refunded = TensuraRaceHandler.autoRespecRacePerks(data, "tensura:kijin");

        assertEquals(0, refunded);
        assertTrue(data.isPerkUnlocked(Perk.BLADE_TRANSCENDENCE.id));
    }

    @Test
    void autoRespecDoesNotRefundFreeGrantedRacePerks() {
        PlayerStatData data = new PlayerStatData();
        PerkManager perks = new PerkManager(data);
        assertTrue(perks.grant(Perk.BLADE_TRANSCENDENCE));

        int refunded = TensuraRaceHandler.autoRespecRacePerks(data, "tensura:human");

        assertEquals(0, refunded);
        assertFalse(data.isPerkUnlocked(Perk.BLADE_TRANSCENDENCE.id));
        assertEquals(0, data.getPerkPointsForStat(Perk.BLADE_TRANSCENDENCE.stat.index));
    }

    @Test
    void intrinsicRacePerksAreReconciledAcrossRaceChanges() {
        PlayerStatData data = new PlayerStatData();
        PerkManager perks = new PerkManager(data);
        assertTrue(perks.grant(Perk.BRUTE_CORE));

        int changed = TensuraRaceHandler.reconcileIntrinsicPerks(
                data,
                Set.of("tensura:ogre_berserker"),
                Set.of("tensura:dragon_skin")
        );

        assertEquals(2, changed);
        assertFalse(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertTrue(data.isPerkUnlocked(Perk.RESIST_CORE.id));
        assertTrue(data.isPerkFreeGranted(Perk.RESIST_CORE.id));
        assertFalse(data.isPerkFreeGranted(Perk.BRUTE_CORE.id));
    }

    @Test
    void syncMagicRaceFromTensura_preservesSavedChoiceWhenRaceIsUnresolved() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(tong.statmod.magic.MagicRace.DWARF);
        data.setChosenStartBranch(tong.statmod.magic.MagicBranch.EARTH);

        boolean changed = TensuraRaceHandler.syncMagicRaceFromTensura(data, null);

        assertFalse(changed);
        assertEquals(tong.statmod.magic.MagicRace.DWARF, data.getMagicRace());
        assertEquals(tong.statmod.magic.MagicBranch.EARTH, data.getChosenStartBranch());
    }

    @Test
    void syncMagicRaceFromTensura_updatesRaceAndRepairsInvalidStartBranch() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(tong.statmod.magic.MagicRace.DWARF);
        data.setChosenStartBranch(tong.statmod.magic.MagicBranch.EARTH);

        boolean changed = TensuraRaceHandler.syncMagicRaceFromTensura(data, "tensura:elf");

        assertTrue(changed);
        assertEquals(tong.statmod.magic.MagicRace.ELF, data.getMagicRace());
        assertEquals(tong.statmod.magic.MagicBranch.AIR, data.getChosenStartBranch());
    }

    @Test
    void raceBonusRefreshIteratesRuntimeStatCount() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java",
                "tong", "statmod", "integration", "tensura", "TensuraRaceHandler.java"));

        assertTrue(source.contains("i < PlayerStatData.STAT_COUNT"),
                "race bonus refresh must cover the runtime stat count");
        assertFalse(source.contains("i < 23"),
                "hardcoded stat-count loops desync when stats are added or removed");
    }

    @Test
    void flatBonusAndXpMultiplierDelegateToRaceEffectApplier() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java",
                "tong", "statmod", "integration", "tensura", "TensuraRaceHandler.java"));

        assertTrue(source.contains("return RaceEffectApplier.getRaceFlatBonus(player, statIndex);"),
                "TensuraRaceHandler.getFlatBonus should delegate to the central race effect logic");
        assertTrue(source.contains("return RaceEffectApplier.getXpMultiplier(player, statIndex);"),
                "TensuraRaceHandler.getXpMultiplier should delegate to the central race effect logic");
        assertFalse(source.contains("RaceModifierRegistry.get(raceId).modifiers().stream()"),
                "TensuraRaceHandler should not duplicate race modifier scans");
    }

    @Test
    void magicRaceSyncRefreshesProgressionImmediatelyWhenItChangesGlobalLevel() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java",
                "tong", "statmod", "integration", "tensura", "TensuraRaceHandler.java"));

        assertTrue(source.contains("LevelUpHandler.grantPendingPerkTiers(data);"),
                "magic race sync should immediately grant any newly reachable global-tier perk points");
        assertTrue(source.contains("SyncHelper.syncStats(serverPlayer);"),
                "magic race sync must refresh stat UI because global level can change when magical stats unlock");
        assertTrue(source.contains("SyncHelper.syncPerks(serverPlayer);"),
                "magic race sync must refresh perk UI immediately when those newly active magical stats cross a tier");
    }

    private static void assertHasMagicRaceBonuses(String raceId) {
        RaceData race = RaceModifierRegistry.get(raceId);
        assertTrue(race.modifiers().stream().anyMatch(mod -> mod.statIndex() == 13 && mod.flatBonus() >= 1),
                () -> raceId + " should grant CASTING_SPEED");
        assertTrue(race.modifiers().stream().anyMatch(mod -> mod.statIndex() == 14 && mod.flatBonus() >= 1),
                () -> raceId + " should grant MANA_POOL");
    }
}

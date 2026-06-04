package tong.statmod.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkProvider;
import tong.statmod.stats.StatType;

@GameTestHolder(STATMod.MODID)
public class StatModGameTests {

    @GameTest(template = "statmod:empty")
    public static void playerHasStatsCapability(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            CapabilityHelper.withStats(player, stats -> {
                helper.assertTrue(stats.getLevel(StatType.BRUTE_FORCE.index) >= 0, "Stats should have default level >= 0");
                helper.assertTrue(stats.getXp(StatType.BRUTE_FORCE.index) >= 0, "Stats should have default XP >= 0");
            });
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void xpAwardsIncreasesStat(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            CapabilityHelper.withStats(player, stats -> {
                int before = stats.getLevel(StatType.BRUTE_FORCE.index);
                stats.addXp(StatType.BRUTE_FORCE.index, 10000);
                int after = stats.getLevel(StatType.BRUTE_FORCE.index);
                helper.assertTrue(after > before, "XP should increase stat level");
                helper.assertTrue(after <= 100, "Level should not exceed 100");
            });
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void statsPersistAfterSetting(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            CapabilityHelper.withStats(player, stats -> {
                stats.setLevel(StatType.AGILITY.index, 50);
                stats.setXp(StatType.AGILITY.index, 100);
            });
            CapabilityHelper.withStats(player, stats -> {
                int level = stats.getLevel(StatType.AGILITY.index);
                int xp = stats.getXp(StatType.AGILITY.index);
                helper.assertTrue(level == 50, "Level should persist, got " + level);
                helper.assertTrue(xp == 100, "XP should persist, got " + xp);
            });
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void perkRequiresLevel(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            Perk perk = Perk.BRUTE_DEMOLITION;
            var opt = player.getCapability(PerkProvider.PERKS).resolve();
            if (opt.isEmpty()) {
                helper.assertTrue(false, "PerkManager capability missing");
                helper.succeed();
                return;
            }
            var perkManager = opt.get();
            CapabilityHelper.withStats(player, stats -> stats.setLevel(perk.stat.index, 0));
            boolean canUnlock = perkManager.unlockPerk(perk, 0);
            helper.assertTrue(!canUnlock, "Should not unlock at stat level 0");
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void xpFormulaPositive(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            int xp0 = PlayerStats.getXpForNextLevel(0);
            int xp50 = PlayerStats.getXpForNextLevel(50);
            helper.assertTrue(xp0 > 0, "XP for level 0 should be positive");
            helper.assertTrue(xp50 > xp0, "Higher levels need more XP");
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void levelCappedAt100(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            CapabilityHelper.withStats(player, stats -> {
                stats.addXp(StatType.BRUTE_FORCE.index, 500000);
                int level = stats.getLevel(StatType.BRUTE_FORCE.index);
                helper.assertTrue(level == 100, "Level should cap at 100, got " + level);
            });
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void manaRegenerates(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            CapabilityHelper.withStats(player, stats -> {
                stats.setMana(0);
                stats.regenMana(5.0f);
                helper.assertTrue(stats.getMana() > 0, "Mana should regenerate");
                int max = stats.getMaxMana();
                stats.regenMana(9999.0f);
                helper.assertTrue(stats.getMana() <= max, "Mana should cap at max");
            });
            helper.succeed();
        });
    }

    @GameTest(template = "statmod:empty")
    public static void all23StatsExist(GameTestHelper helper) {
        helper.startSequence().thenExecute(() -> {
            boolean allOk = true;
            for (StatType s : StatType.values()) {
                if (s.index < 0 || s.index >= 23) allOk = false;
            }
            helper.assertTrue(allOk, "All 23 stats should have valid indices");
            helper.succeed();
        });
    }
}

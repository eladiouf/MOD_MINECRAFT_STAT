package tong.statmod.client;

import net.minecraft.client.Minecraft;
import tong.statmod.client.feedback.CombatFeedbackRenderer;
import tong.statmod.client.notification.LevelUpToast;
import tong.statmod.capability.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.weapon.WeaponMasteryManager;

import java.util.Arrays;

public class ClientStatsCache {
    private static final int[] levels = new int[PlayerStats.STAT_COUNT];
    private static final int[] xp = new int[PlayerStats.STAT_COUNT];
    private static float fatigue = 0;
    private static int maxFatigue = 500;
    private static float thirst = 100;
    private static float mana = 0;
    private static boolean syncReceived = false;
    private static final int[] weaponLevels = new int[WeaponMasteryManager.WEAPON_COUNT];
    private static final int[] weaponXp = new int[WeaponMasteryManager.WEAPON_COUNT];

    public static void updateAll(int[] newLevels, int[] newXp) {
        Arrays.fill(levels, 0);
        Arrays.fill(xp, 0);
        System.arraycopy(newLevels, 0, levels, 0, Math.min(newLevels.length, PlayerStats.STAT_COUNT));
        System.arraycopy(newXp, 0, xp, 0, Math.min(newXp.length, PlayerStats.STAT_COUNT));
        syncReceived = true;
    }

    public static void updateStat(int index, int level, int statXp) {
        if (index >= 0 && index < PlayerStats.STAT_COUNT) {
            int oldLevel = levels[index];
            int oldXp = xp[index];
            levels[index] = level;
            xp[index] = statXp;
            if (level > oldLevel) {
                LevelUpToast.onLevelUp(StatType.byIndex(index).displayName, level);
            }
            if (syncReceived && statXp > oldXp) {
                int xpGained = statXp - oldXp;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    CombatFeedbackRenderer.addXpPopup(mc.player, xpGained, StatType.byIndex(index).displayName);
                }
            }
        }
    }

    public static void updateFatigue(float f) { fatigue = f; }

    public static void updateFatigue(float f, int max) {
        fatigue = f;
        maxFatigue = Math.max(50, max);
    }

    public static void updateThirst(float t) { thirst = t; }

    public static int getLevel(int index) { return (index >= 0 && index < PlayerStats.STAT_COUNT) ? levels[index] : 0; }
    public static int getXp(int index) { return (index >= 0 && index < PlayerStats.STAT_COUNT) ? xp[index] : 0; }
    public static int getLevel(StatType type) { return levels[type.index]; }
    public static int getXp(StatType type) { return xp[type.index]; }
    public static float getFatigue() { return fatigue; }
    public static int getMaxFatigue() { return maxFatigue; }
    public static float getThirst() { return thirst; }

    public static void updateMana(float m) { mana = m; }
    public static float getMana() { return mana; }
    public static float getMaxMana() {
        int manaPoolLevel = getLevel(StatType.MANA_POOL);
        return 50.0f + tong.statmod.stats.StatCalculator.getManaBonus(manaPoolLevel);
    }

    public static void updateWeaponMastery(int[] levels, int[] xp) {
        Arrays.fill(weaponLevels, 0);
        Arrays.fill(weaponXp, 0);
        System.arraycopy(levels, 0, weaponLevels, 0, Math.min(levels.length, WeaponMasteryManager.WEAPON_COUNT));
        System.arraycopy(xp, 0, weaponXp, 0, Math.min(xp.length, WeaponMasteryManager.WEAPON_COUNT));
    }
    public static int getWeaponLevel(int index) { return (index >= 0 && index < WeaponMasteryManager.WEAPON_COUNT) ? weaponLevels[index] : 0; }
    public static int getWeaponXp(int index) { return (index >= 0 && index < WeaponMasteryManager.WEAPON_COUNT) ? weaponXp[index] : 0; }

    public static int getGlobalLevel() {
        int sum = 0;
        for (int i = 0; i < PlayerStats.STAT_COUNT; i++) sum += levels[i];
        return Math.round(sum / (float) PlayerStats.STAT_COUNT);
    }

    public static float getGlobalXpProgress() {
        float total = 0;
        for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
            int needed = tong.statmod.stats.StatCalculator.getXpForNextLevel(levels[i]);
            total += (needed > 0) ? (float) xp[i] / needed : 1.0f;
        }
        return total / (float) PlayerStats.STAT_COUNT;
    }
}

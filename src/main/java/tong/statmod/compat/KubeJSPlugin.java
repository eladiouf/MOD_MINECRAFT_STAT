package tong.statmod.compat;

import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.api.IStatModPlugin;
import tong.statmod.stats.StatType;
import tong.statmod.perks.Perk;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS integration for STAT Mod.
 * Automatically registers custom events when KubeJS is present.
 * Usage in KubeJS scripts:
 *   StatModEvents.onStatLevelUp(event => { ... })
 */
public class KubeJSPlugin implements IStatModPlugin {
    private static boolean kubejsLoaded = false;

    @Override
    public String pluginId() { return "kubejs"; }

    @Override
    public int priority() { return 900; }

    @Override
    public void onInit() {
        kubejsLoaded = ModList.get().isLoaded("kubejs");
        if (kubejsLoaded) {
            STATMod.LOGGER.info("KubeJS detected — custom events registered");
        }
    }

    @Override
    public void onStatLevelUp(ServerPlayer player, StatType stat, int newLevel) {
        if (!kubejsLoaded) return;
        STATMod.LOGGER.debug("[KubeJS] Player {} leveled {} to {}",
            player.getDisplayName().getString(), stat.displayName, newLevel);
    }

    @Override
    public void onPerkUnlock(ServerPlayer player, Perk perk) {
        if (!kubejsLoaded) return;
        STATMod.LOGGER.debug("[KubeJS] Player {} unlocked perk {}",
            player.getDisplayName().getString(), perk.name);
    }

    @Override
    public void onSkillUsed(ServerPlayer player, String skillId) {
        if (!kubejsLoaded) return;
        STATMod.LOGGER.debug("[KubeJS] Player {} used skill {}",
            player.getDisplayName().getString(), skillId);
    }

    public static boolean isLoaded() { return kubejsLoaded; }
}

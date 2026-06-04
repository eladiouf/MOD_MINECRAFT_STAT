package tong.statmod.api;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;
import tong.statmod.perks.Perk;

import java.util.*;

public class PluginManager {
    private static final List<IStatModPlugin> plugins = new ArrayList<>();
    private static boolean loaded = false;

    public static void loadPlugins() {
        if (loaded) return;
        loaded = true;
        ServiceLoader<IStatModPlugin> loader = ServiceLoader.load(IStatModPlugin.class);
        for (IStatModPlugin plugin : loader) {
            plugins.add(plugin);
            STATMod.LOGGER.info("Loaded plugin: {} (priority={})", plugin.pluginId(), plugin.priority());
        }
        plugins.sort(Comparator.comparingInt(IStatModPlugin::priority));
        for (IStatModPlugin plugin : plugins) {
            try {
                plugin.onInit();
            } catch (Exception e) {
                STATMod.LOGGER.error("Plugin {} init failed: {}", plugin.pluginId(), e.getMessage());
            }
        }
        STATMod.LOGGER.info("Plugin system ready — {} plugins loaded", plugins.size());
    }

    public static float fireManaRegen(ServerPlayer player) {
        float total = 0;
        for (IStatModPlugin p : plugins) total += p.onManaRegen(player);
        return total;
    }

    public static void fireStatLevelUp(ServerPlayer player, StatType stat, int newLevel) {
        for (IStatModPlugin p : plugins) p.onStatLevelUp(player, stat, newLevel);
    }

    public static void firePerkUnlock(ServerPlayer player, Perk perk) {
        for (IStatModPlugin p : plugins) p.onPerkUnlock(player, perk);
    }

    public static void fireSkillUsed(ServerPlayer player, String skillId) {
        for (IStatModPlugin p : plugins) p.onSkillUsed(player, skillId);
    }

    public static List<IStatModPlugin> getPlugins() { return Collections.unmodifiableList(plugins); }
}

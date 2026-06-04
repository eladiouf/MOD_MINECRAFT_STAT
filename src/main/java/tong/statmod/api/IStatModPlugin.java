package tong.statmod.api;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

public interface IStatModPlugin {
    default void onInit() {}

    default float onManaRegen(ServerPlayer player) { return 0; }

    default void onStatLevelUp(ServerPlayer player, StatType stat, int newLevel) {}

    default void onPerkUnlock(ServerPlayer player, Perk perk) {}

    default void onSkillUsed(ServerPlayer player, String skillId) {}

    default int priority() { return 100; }

    String pluginId();
}

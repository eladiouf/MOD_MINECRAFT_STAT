package tong.statmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.function.Consumer;

public class ConfigPresets {
    public enum Preset {
        EASY("Easy", "XP x1.5, Fatigue x0.5, Thirst x0.5", p -> {
            Config.xpPerLevelMultiplier = (int)(Config.xpPerLevelMultiplier * 0.67);
            Config.fatigueDayRate *= 0.5;
            Config.fatigueNightRate *= 0.5;
            Config.fatigueUndergroundRate *= 0.5;
            Config.fatigueSprintCost *= 0.5;
            Config.fatigueJumpCost *= 0.5;
            Config.thirstBaseDecay *= 0.5;
            Config.thirstSprintCost *= 0.5;
            Config.cooldownMultiplier *= 0.8;
        }),
        NORMAL("Normal", "Default values", p -> {}),
        HARD("Hard", "XP x0.5, Fatigue x2, Thirst x2, Cooldown x1.5", p -> {
            Config.xpPerLevelMultiplier *= 2;
            Config.fatigueDayRate *= 2.0;
            Config.fatigueNightRate *= 2.0;
            Config.fatigueUndergroundRate *= 2.0;
            Config.fatigueSprintCost *= 2.0;
            Config.fatigueJumpCost *= 2.0;
            Config.thirstBaseDecay *= 2.0;
            Config.thirstSprintCost *= 2.0;
            Config.cooldownMultiplier *= 1.5;
        });

        public final String name;
        public final String description;
        private final Consumer<Preset> applier;

        Preset(String name, String description, Consumer<Preset> applier) {
            this.name = name;
            this.description = description;
            this.applier = applier;
        }
        public void apply() { applier.accept(this); }
    }

    public static void applyPreset(Player player, Preset preset) {
        preset.apply();
        player.sendSystemMessage(Component.literal("§aConfig preset applied: §e" + preset.name + " §7— " + preset.description));
    }
}

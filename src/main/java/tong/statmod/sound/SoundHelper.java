package tong.statmod.sound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class SoundHelper {
    public static void playLevelUp(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    public static void playMilestone(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.MILESTONE.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    public static void playSkillCast(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.SKILL_CAST.get(), SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    public static void playPerkUnlock(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.PERK_UNLOCK.get(), SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    public static void playFatigueWarning(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.FATIGUE_WARNING.get(), SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    public static void playThirstWarning(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.THIRST_WARNING.get(), SoundSource.PLAYERS, 0.5f, 0.8f);
    }
}

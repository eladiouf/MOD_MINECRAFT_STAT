package tong.statmod.sound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class SoundHelper {
    public static void playLevelUp(ServerPlayer player) {
        player.playNotifySound(ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void playPerkUnlock(ServerPlayer player) {
        player.playNotifySound(ModSounds.PERK_UNLOCK.get(), SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    public static void playStatUp(ServerPlayer player) {
        player.playNotifySound(ModSounds.STAT_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void playPerkTreeOpen(ServerPlayer player) {
        player.playNotifySound(ModSounds.PERK_TREE_OPEN.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}

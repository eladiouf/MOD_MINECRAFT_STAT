package tong.statmod.integration.tensura;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class MagiculeScalingHandler {
    private MagiculeScalingHandler() {}

    public static double magiculeFloor(int arcanePower, int manaPool) {
        return Math.max(0, arcanePower) + Math.max(0, manaPool);
    }

    public static double scaledMagicule(double currentMagicule, int arcanePower, int manaPool) {
        return Math.max(currentMagicule, magiculeFloor(arcanePower, manaPool));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 40 != 0) return;

        var existence = PlayerDataBridge.getExistence(player);
        if (existence == null) {
            return;
        }

        int arcanePower = RaceEffectApplier.getEffectiveLevel(player, StatType.ARCANE_POWER.index);
        int manaPool = RaceEffectApplier.getEffectiveLevel(player, StatType.MANA_POOL.index);
        double scaled = scaledMagicule(existence.getMagicule(), arcanePower, manaPool);
        if (scaled > existence.getMagicule()) {
            existence.setMagicule(scaled);
            existence.markDirty();
        }
    }
}

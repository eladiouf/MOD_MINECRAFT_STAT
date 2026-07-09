package tong.statmod.integration.tensura;

import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class MagiculeScalingHandler {
    private MagiculeScalingHandler() {}

    public static double magiculeFloor(int arcanePower, int manaPool) {
        return Math.max(0, arcanePower) + Math.max(0, manaPool);
    }

    public static double requiredMaxMagicule(double currentBaseMaxMagicule, int arcanePower, int manaPool) {
        return Math.max(Math.max(0.0, currentBaseMaxMagicule), magiculeFloor(arcanePower, manaPool));
    }

    public static double scaledMagicule(double currentMagicule, double effectiveMaxMagicule, int arcanePower, int manaPool) {
        double floor = magiculeFloor(arcanePower, manaPool);
        if (effectiveMaxMagicule <= 0.0) {
            return currentMagicule;
        }
        return Math.max(currentMagicule, Math.min(floor, effectiveMaxMagicule));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 40 != 0) return;

        var existence = PlayerDataTensuraHook.getExistence(player);
        if (existence == null) return;

        double baseMax = EnergyHelper.getBaseMaxMagicule(player);
        // Cap max magicule to base (never artificially inflate beyond Tensura's natural limit)
        double currentMax = EnergyHelper.getMaxMagicule(player);
        if (currentMax > baseMax && baseMax > 0) {
            EnergyHelper.setMaxMagicule(player, baseMax);
        }

        // Cap current magicule to max (prevents overflow/poison from admin commands)
        if (existence.getMagicule() > currentMax) {
            existence.setMagicule(currentMax);
            existence.markDirty();
        }
    }
}

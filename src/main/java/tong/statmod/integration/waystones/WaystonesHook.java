package tong.statmod.integration.waystones;

import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneStyle;
import net.blay09.mods.waystones.api.WaystoneStyles;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import tong.statmod.STATMod;
import tong.statmod.dungeon.FloorPalette;

import java.util.Optional;

/**
 * Mission M6 — Appels réels à l'API Waystones (isolés du reste).
 *
 * <p>Cette classe importe directement {@code net.blay09.mods.waystones.api.*} (le jar est sur le
 * classpath de compilation). Elle n'est chargée par la JVM que lorsqu'un appel de
 * {@link WaystonesBridge} l'atteint — donc jamais si Waystones est absent, car
 * {@code WaystonesBridge.loaded()} garde chaque appel.
 */
final class WaystonesHook {

    private WaystonesHook() {}

    /**
     * Pose une waystone de checkpoint (idempotent). Si une waystone existe déjà à {@code pos}, on
     * ne fait rien. Sinon on la pose, la nomme « Trial Dungeon — Floor N » et applique un style
     * assorti au tier.
     */
    static void placeCheckpoint(ServerLevel level, BlockPos pos, FloorPalette tier, int floor) {
        Optional<Waystone> existing = WaystonesAPI.getWaystoneAt(level, pos);
        if (existing.isPresent()) return;

        Optional<Waystone> placed = WaystonesAPI.placeWaystone(level, pos, styleFor(tier));
        if (placed.isEmpty()) {
            STATMod.LOGGER.warn("[TrialDungeon] Waystones a refusé de poser un checkpoint @ {}", pos);
            return;
        }

        Waystone waystone = placed.get();
        if (waystone instanceof MutableWaystone mutable) {
            if (floor == 0) {
                mutable.setName(Component.literal("✦ Trial Dungeon — Hub"));
            } else {
                mutable.setName(Component.literal("☠ Trial Dungeon — Floor " + floor));
            }
        }
        STATMod.LOGGER.info("[TrialDungeon] Checkpoint waystone posé à l'étage {} (@ {})", floor, pos);
    }

    /** Style Waystone assorti au tier de l'étage. */
    private static WaystoneStyle styleFor(FloorPalette tier) {
        return switch (tier) {
            case EARLY -> WaystoneStyles.MOSSY;
            case MID -> WaystoneStyles.DEEPSLATE;
            case LATE -> WaystoneStyles.BLACKSTONE;
            case ABYSS -> WaystoneStyles.END_STONE;
        };
    }
}

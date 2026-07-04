package tong.statmod.integration.waystones;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.dungeon.FloorPalette;

/**
 * Mission M6 — Intégration Waystones (dépendance douce, 2026-07-04).
 *
 * <p>Waystones apporte des pierres de téléportation persistantes. Dans le Trial Dungeon, on pose
 * une waystone sur chaque étage boss (×10) : une fois activée par le joueur, elle devient un
 * <b>checkpoint</b> réutilisable depuis le réseau Waystones (retour direct sans recommencer).
 *
 * <p>Le jar Waystones est sur le classpath de compilation ({@code libs/}), donc on peut importer
 * son API directement (type-safe) — contrairement à L2 (jar-in-jar) qui exige la réflexion. Mais
 * tout appel reste protégé par {@link #loaded()} pour dégrader proprement si le mod est absent, et
 * les vrais appels à l'API sont isolés dans {@link WaystonesHook} (classe chargée par la JVM
 * seulement quand on la touche → jamais si Waystones est absent).
 */
public final class WaystonesBridge {

    private static Boolean loaded;

    private WaystonesBridge() {}

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("waystones");
        return loaded;
    }

    /**
     * Pose (idempotent) une waystone de checkpoint à {@code pos} sur un étage boss, avec un style
     * assorti au tier. No-op si Waystones absent ou si une waystone existe déjà là.
     */
    public static void placeCheckpoint(ServerLevel level, BlockPos pos, FloorPalette tier, int floor) {
        if (!loaded()) return;
        try {
            WaystonesHook.placeCheckpoint(level, pos, tier, floor);
        } catch (Throwable e) {
            // Une incompatibilité Waystones ne doit jamais casser la génération du donjon.
            STATMod.LOGGER.warn("[TrialDungeon] Waystone checkpoint placement failed @ {}: {}",
                    pos, e.toString());
        }
    }
}

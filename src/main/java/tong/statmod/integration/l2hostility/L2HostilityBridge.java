package tong.statmod.integration.l2hostility;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import tong.statmod.config.Config;

/**
 * Mission M6 — Intégration L2 Hostility (dépendance douce).
 *
 * <p>Cale la difficulté L2 Hostility des mobs de donjon sur la profondeur : plus l'étage est
 * profond, plus le niveau de hostilité est élevé (escalade Solo Leveling). Le niveau appliqué
 * est {@code floor * l2HostilityPerFloor}, plafonné à {@code l2HostilityCap} (config).
 *
 * <p><b>Dépendance douce</b> : tout appel est inerte si {@code l2hostility} n'est pas chargé.
 * Les vrais appels à l'API L2 sont isolés dans {@link L2HostilityHook}, classe qui n'est
 * chargée par la JVM que lorsqu'on la touche (donc jamais si le mod est absent).
 */
public final class L2HostilityBridge {

    private static Boolean loaded;

    private L2HostilityBridge() {}

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("l2hostility");
        return loaded;
    }

    /**
     * Niveau L2 pour un étage donné, selon la config.
     * <b>ATTENTION</b> : La config par défaut de L2 Hostility a un niveau de base de 20, donc
     * même avec un facteur de 0.1, l'étage 1 donnera niveau 21+. Recommandé : utiliser 0.0 et
     * gérer la difficulté via le système de progression STAT MOD.
     */
    public static int levelForFloor(int floor) {
        double perFloor = Config.getDungeonHostilityPerFloor();
        if (perFloor <= 0.0) return 1; // Désactivé = niveau 1 (base vanilla)
        int cap = Config.getDungeonHostilityCap();

        // Système conservateur : +1 tous les 5 étages
        // Étage 1-5 : niveau 1
        // Étage 6-10 : niveau 2
        // Étage 11-15 : niveau 3
        // ...
        int baseLevel = 1 + Math.floorDiv(floor - 1, 5);
        return Math.min(baseLevel, cap);
    }

    /**
     * Applique le niveau de hostilité correspondant à {@code floor} sur {@code mob}. No-op si L2
     * absent, si le scaling est désactivé (perFloor = 0), ou si l'entité est nulle.
     */
    public static void applyFloorLevel(LivingEntity mob, int floor) {
        if (mob == null || !loaded()) return;
        int level = levelForFloor(floor);
        if (level <= 0) return;
        L2HostilityHook.apply(mob, level);
    }
}

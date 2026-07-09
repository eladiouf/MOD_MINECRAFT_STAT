package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Salles secrètes (2026-07-05, refonte « pari » 2026-07-09).
 *
 * <p>Une salle par étage cache une <b>chambre</b> creusée SOUS une pièce de combat, accessible par
 * une <b>trappe en bois</b> dissimulée dans le sol (le seul bloc de bois d'une pièce de pierre →
 * l'indice pour l'explorateur attentif) + une échelle.
 *
 * <p><b>Le pari</b> (feedback playtest 2026-07-09) : d'en haut, impossible de savoir — la chambre
 * est soit <b>bénie</b> (~60 % : 2 coffres de trésor + plaque de bénédiction : absorption,
 * régénération, chance ou XP), soit <b>maudite</b> (~40 % : 1 seul coffre, et la plaque déclenche
 * wither, cécité ou une embuscade de vex). La plaque est au centre, entre l'échelle et les
 * coffres. Déterministe par étage. Renforcement à ratio variable : on descend TOUJOURS.
 *
 * <p>Repère : sol de pièce à y=-1. La chambre est sous l'underside, en {@code y ∈ [-6,-3]}.
 */
public final class DungeonSecretRoom {

    private static final int FLOOR_Y = -6;   // sol de la chambre
    private static final int CEIL_Y = -3;    // plafond de la chambre

    /**
     * Bénédictions possibles (chambre bénie) — une par étage, déterministe. Uniquement des effets
     * (re-marcher sur la plaque ne fait que RAFRAÎCHIR l'effet — pas de farm possible, contrairement
     * à un don d'XP/objets qui serait exploitable à répétition).
     */
    private static final String[] BLESSINGS = {
            "effect give @p[distance=..5] minecraft:absorption 120 2",
            "effect give @p[distance=..5] minecraft:regeneration 15 1",
            "effect give @p[distance=..5] minecraft:luck 300 1",
            "effect give @p[distance=..5] minecraft:strength 300 0",
    };

    /** Malédictions possibles (chambre maudite) — une par étage, déterministe. */
    private static final String[] CURSES = {
            "effect give @p[distance=..5] minecraft:wither 6 1",
            "effect give @p[distance=..5] minecraft:blindness 10 0",
            "execute at @p[distance=..5] run summon minecraft:vex ~ ~1 ~ {NeoForgeData:{"
                    + DungeonSpawnGuard.AUTHORIZED_TAG + ":1b}}",
    };

    private DungeonSecretRoom() {}

    /** Place une salle secrète si {@code r} est la pièce élue de l'étage (≈ 1 par étage). */
    public static void maybePlace(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        // Une seule pièce de combat par étage (index déterministe dans la plage combat 1..10).
        if (r.index() != 2 + Math.floorMod(floor, 7)) return;

        // Chambre sous le QUADRANT nord-ouest (pas le centre, occupé par les décors dais/bassin).
        int q = Math.max(6, Math.min(r.maxX() - r.minX(), r.maxZ() - r.minZ()) / 4);
        int cx = r.centerX() - q, cz = r.centerZ() - q;
        BlockState wall = B(t.base());

        // ── Coque de la chambre (5×5), sous la pièce ──
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                S(lv, O(sp, cx + dx, FLOOR_Y, cz + dz), wall);        // sol
                S(lv, O(sp, cx + dx, CEIL_Y, cz + dz), wall);         // plafond
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    S(lv, O(sp, cx + dx, y, cz + dz), edge ? wall : B(Blocks.AIR));
                }
            }
        }

        // ── Puits d'accès au bord nord (x=cx, z=cz-2) : trappe + échelle ──
        // Colonne d'air du sol de la pièce (y=-1) jusqu'à la chambre (y=FLOOR_Y+1).
        for (int y = FLOOR_Y + 1; y <= -1; y++) {
            S(lv, O(sp, cx, y, cz - 2), B(Blocks.AIR));
        }
        // Échelle (dos au nord, bloc support plein en cz-3), de la chambre au ras du sol.
        // On garantit le support en posant un bloc plein derrière l'échelle (l'underside peut être
        // creux à cet endroit → sinon l'échelle tomberait).
        BlockState ladder = B(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = FLOOR_Y + 1; y <= -2; y++) {
            S(lv, O(sp, cx, y, cz - 3), wall);            // support plein
            S(lv, O(sp, cx, y, cz - 2), ladder);          // échelle
        }
        // Trappe en bois au niveau du sol de la pièce (le « tell » discret), fermée.
        BlockState trap = B(Blocks.SPRUCE_TRAPDOOR)
                .setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, false)
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH);
        S(lv, O(sp, cx, -1, cz - 2), trap);

        // ── Le pari : chambre bénie (~60 %) ou maudite (~40 %), indiscernable d'en haut ──
        boolean blessed = Math.floorMod(floor * 92821 + 37, 100) < 60;

        if (blessed) {
            // 2 coffres de trésor + plaque de bénédiction au centre (sur le chemin des coffres).
            DungeonArchitect.placeChest(lv, O(sp, cx - 1, FLOOR_Y + 1, cz + 1));
            DungeonArchitect.placeChest(lv, O(sp, cx + 1, FLOOR_Y + 1, cz + 1));
            String blessing = BLESSINGS[Math.floorMod(floor * 31, BLESSINGS.length)];
            DungeonTraps.placeCommandTrap(lv, O(sp, cx, FLOOR_Y + 1, cz), blessing);
        } else {
            // 1 seul coffre — et la plaque centrale est un piège (le risque paie moins).
            DungeonArchitect.placeChest(lv, O(sp, cx + 1, FLOOR_Y + 1, cz + 1));
            String curse = CURSES[Math.floorMod(floor * 53, CURSES.length)];
            DungeonTraps.placeCommandTrap(lv, O(sp, cx, FLOOR_Y + 1, cz), curse);
        }
        S(lv, O(sp, cx, FLOOR_Y + 1, cz + 2), B(t.decorPrimary()));
        S(lv, O(sp, cx, CEIL_Y, cz), B(t.light())); // fanal au plafond
    }
}

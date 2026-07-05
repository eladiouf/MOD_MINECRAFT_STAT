package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

import static tong.statmod.dungeon.DungeonArchitect.HX;
import static tong.statmod.dungeon.DungeonArchitect.HZ;
import static tong.statmod.dungeon.DungeonArchitect.WALL_H;

/**
 * Mission M6 — Arène de boss adaptée au boss (2026-07-05).
 *
 * <p>La salle de boss n'est plus une estrade générique : sa <b>forme s'adapte au type du boss</b>
 * de l'étage (déduit de son id via {@link DungeonBossRoster}). Un boss aquatique reçoit un bassin,
 * un dragon/volant une arène haute et dégagée, un colosse une fosse large, etc. Purement décoratif
 * (le combat et le spawn restent gérés par l'autel). Thématisé par {@link BlockPalette}.
 */
public final class DungeonBossArena {

    /** Catégorie d'arène déduite du boss. */
    public enum Kind { AQUATIC, FLYING, COLOSSUS, INFERNAL, UNDEAD, ARENA }

    private DungeonBossArena() {}

    static BlockState B(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos sp, int x, int y, int z) { return sp.offset(x, y, z); }

    /** Catégorise le boss d'un étage à partir de l'id de sa première entrée de roster. */
    public static Kind kindForFloor(int floor) {
        List<DungeonBossRoster.BossEntry> roster = DungeonBossRoster.forFloor(floor);
        String id = roster.isEmpty() ? "" : roster.get(0).entityId().toLowerCase(Locale.ROOT);
        if (contains(id, "kraken", "leviathan", "wadjet", "deepling", "coral", "scylla", "sandworm", "poseid")) return Kind.AQUATIC;
        if (contains(id, "dragon", "peacock", "serpent", "phantom", "watcher", "radahn", "malenia_2")) return Kind.FLYING;
        if (contains(id, "golem", "colossus", "giant", "monstrosity", "ministrosity", "yeti", "smough", "havel", "warden", "elden_beast")) return Kind.COLOSSUS;
        if (contains(id, "infernal", "ignited", "revenant", "flame", "hell", "godskin", "soul_of_cinder", "wither")) return Kind.INFERNAL;
        if (contains(id, "draugr", "dead_king", "bonescaller", "nightmare", "hollow", "skeleton", "notch")) return Kind.UNDEAD;
        return Kind.ARENA;
    }

    private static boolean contains(String s, String... keys) {
        for (String k : keys) if (s.contains(k)) return true;
        return false;
    }

    /**
     * Façonne l'arène du boss au nord (centre {@code hz = -HZ+11}), selon la catégorie. On ne
     * touche que le sol/plafond autour de l'estrade, pas l'autel lui-même.
     */
    public static void shape(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        int hz = -HZ + 11;
        Kind kind = kindForFloor(floor);
        switch (kind) {
            case AQUATIC -> aquatic(lv, sp, t, hz);
            case FLYING -> flying(lv, sp, t, hz);
            case COLOSSUS -> colossus(lv, sp, t, hz);
            case INFERNAL -> infernal(lv, sp, t, hz);
            case UNDEAD -> undead(lv, sp, t, hz);
            case ARENA -> arena(lv, sp, t, hz);
        }
    }

    /** Bassin : anneau d'eau autour de l'estrade, sol en prismarine/thème, colonnes de corail. */
    private static void aquatic(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        for (int dx = -12; dx <= 12; dx++) for (int dz = -10; dz <= 10; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 12 * 12 && d2 > 8 * 8) {           // anneau d'eau
                S(lv, O(sp, dx, -1, hz + dz), B(Blocks.WATER));
                S(lv, O(sp, dx, -2, hz + dz), B(t.underside()));
            }
        }
        for (int[] c : new int[][]{{-10, 0}, {10, 0}, {0, -8}, {0, 8}}) {
            S(lv, O(sp, c[0], 0, hz + c[1]), B(Blocks.SEA_LANTERN));
        }
    }

    /** Volant : arène haute et dégagée (plafond retiré haut), plots d'atterrissage aux angles. */
    private static void flying(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        // Dégage un grand volume vertical pour un boss qui vole.
        for (int dx = -13; dx <= 13; dx++) for (int dz = -11; dz <= 11; dz++) {
            if (dx * dx + dz * dz > 13 * 13) continue;
            for (int y = 3; y <= WALL_H + 6; y++) S(lv, O(sp, dx, y, hz + dz), B(Blocks.AIR));
        }
        for (int[] c : new int[][]{{-11, -9}, {11, -9}, {-11, 9}, {11, 9}}) {
            for (int y = 0; y <= 2; y++) S(lv, O(sp, c[0], y, hz + c[1]), B(t.decorPrimary()));
            S(lv, O(sp, c[0], 3, hz + c[1]), B(t.light()));
        }
    }

    /** Colosse : fosse large en contrebas (gradins) — l'estrade centrale de l'autel est préservée. */
    private static void colossus(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        for (int dx = -13; dx <= 13; dx++) for (int dz = -11; dz <= 11; dz++) {
            if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) continue; // garde l'estrade de l'autel
            int d2 = dx * dx + dz * dz;
            if (d2 <= 10 * 10) {
                S(lv, O(sp, dx, -1, hz + dz), B(t.base()));   // fond de la fosse
                S(lv, O(sp, dx, 0, hz + dz), B(Blocks.AIR));
            } else if (d2 <= 13 * 13) {
                S(lv, O(sp, dx, 0, hz + dz), B(t.slab()));    // gradin
            }
        }
    }

    /** Infernal : coulées de magma décoratives + braseros. */
    private static void infernal(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        for (int i = 0; i < 14; i++) {
            int dx = ((i * 37) % 25) - 12;
            int dz = ((i * 53) % 21) - 10;
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            S(lv, O(sp, dx, -1, hz + dz), B(Blocks.MAGMA_BLOCK));
        }
        for (int[] c : new int[][]{{-9, -7}, {9, -7}, {-9, 7}, {9, 7}}) {
            S(lv, O(sp, c[0], 0, hz + c[1]), B(t.decorPrimary()));
            S(lv, O(sp, c[0], 1, hz + c[1]), B(Blocks.FIRE));
        }
    }

    /** Mort-vivant : sol en soul sand + os épars + brume lumineuse basse. */
    private static void undead(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        for (int i = 0; i < 16; i++) {
            int dx = ((i * 41) % 23) - 11;
            int dz = ((i * 29) % 19) - 9;
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            S(lv, O(sp, dx, -1, hz + dz), B(i % 3 == 0 ? Blocks.SOUL_SAND : Blocks.BONE_BLOCK));
        }
        for (int[] c : new int[][]{{-8, -6}, {8, -6}, {-8, 6}, {8, 6}}) {
            S(lv, O(sp, c[0], 0, hz + c[1]), B(Blocks.SOUL_LANTERN));
        }
    }

    /** Arène classique : cercle de colonnes autour de l'estrade. */
    private static void arena(ServerLevel lv, BlockPos sp, BlockPalette t, int hz) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int dx = (int) Math.round(Math.cos(a) * 11);
            int dz = (int) Math.round(Math.sin(a) * 9);
            for (int y = 0; y <= 3; y++) S(lv, O(sp, dx, y, hz + dz), B(t.decorPrimary()));
            S(lv, O(sp, dx, 4, hz + dz), B(t.light()));
        }
    }
}

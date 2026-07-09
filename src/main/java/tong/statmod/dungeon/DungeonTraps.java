package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Pièges du donjon (2026-07-05, refonte 2026-07-09).
 *
 * <p>Deux familles :
 * <ul>
 *   <li><b>Pièges de sol Simply Traps</b> (pointes, pieux, barbelés) — dégâts au contact,
 *       résolus en douceur (mod absent → rien, pas de crash).</li>
 *   <li><b>Pièges à command block</b> — plaque de pression au sol, command block caché dessous
 *       (crocs d'évocateur, poison, lenteur, wither en profondeur). Remplace les anciens pièges
 *       redstone distributeur+plaque, peu fiables et sans intérêt (feedback playtest 2026-07-09).
 *       ⚠ Sur un serveur dédié, {@code enable-command-block=true} est requis dans
 *       server.properties (activé par défaut en solo).</li>
 * </ul>
 * Placés à l'écart du centre et des axes de portes.
 */
public final class DungeonTraps {

    /** Pièges de sol (dégâts au contact), du plus commun au plus rare. */
    private static final List<String> FLOOR_TRAPS = List.of(
            "simply_traps:spike_trap",
            "simply_traps:stake_wall",
            "simply_traps:stake",
            "simply_traps:circular_barbed_wire_iron");

    private DungeonTraps() {}

    /** Pose quelques pièges au sol dans une salle de combat (déterministe par étage + index). */
    public static void place(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        List<BlockState> traps = resolveAll(FLOOR_TRAPS);
        if (traps.isEmpty()) return; // Simply Traps absent → aucun piège

        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w < 8 || d < 8) return;
        int count = 4 + Math.floorMod(floor + r.index(), 4); // 4..7 pièges
        int cx = r.centerX(), cz = r.centerZ();

        for (int i = 0; i < count; i++) {
            int hx = hash(floor, r.index(), i * 2);
            int hz = hash(floor, r.index(), i * 2 + 1);
            int x = r.minX() + 3 + hx % (w - 6);
            int z = r.minZ() + 3 + hz % (d - 6);
            // Évite le centre immédiat (2×2) et les axes de portes (milieux de bords).
            if (Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) continue;
            if (x == cx || z == cz) continue;
            BlockState trapState = traps.get(i % traps.size());
            if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
                trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            }
            if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                try {
                    trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP);
                } catch (IllegalArgumentException e) {
                    // Do nothing if UP is not supported for FACING (e.g. horizontal-only blocks)
                }
            }
            S(lv, O(sp, x, 0, z), trapState);
        }

        placeCommandBlockTraps(lv, sp, r, floor);
    }

    /**
     * Marqueur d'autorisation posé via NBT sur les mobs d'embuscade invoqués par les pièges :
     * NeoForge charge la clé {@code NeoForgeData} dans {@code getPersistentData()} → le mob passe
     * {@link DungeonSpawnGuard}, devient persistant et ne cible que les joueurs
     * ({@link DungeonMobDiscipline}).
     */
    private static final String AUTHORIZED_NBT = "{NeoForgeData:{" + DungeonSpawnGuard.AUTHORIZED_TAG + ":1b}}";

    /**
     * Arsenal de base, du plus commun au plus vicieux. {@code ~ ~ ~} = position du command block
     * (1 bloc sous la plaque). Pas de TNT (détruirait la forteresse) ni de foudre (le feu se
     * propagerait au mobilier en bois).
     */
    private static final String[] TRAP_COMMANDS = {
            "summon minecraft:evoker_fangs ~ ~1 ~",                                     // crocs sous la plaque
            "execute at @p[distance=..5] run summon minecraft:evoker_fangs ~ ~ ~",      // crocs sous le joueur
            "effect give @p[distance=..4] minecraft:poison 6 1",
            "effect give @p[distance=..4] minecraft:slowness 6 2",
            "execute at @p[distance=..4] run fill ~-1 ~ ~-1 ~1 ~1 ~1 minecraft:cobweb keep", // cage de toiles
            "effect give @p[distance=..4] minecraft:levitation 3 3",                    // soulève… puis lâche
            "effect give @p[distance=..4] minecraft:blindness 8 0",
            "execute at @p[distance=..4] run summon minecraft:cave_spider ~ ~ ~ " + AUTHORIZED_NBT, // embuscade
    };

    /** Pièges supplémentaires des profondeurs (étage ≥ 40) — s'ajoutent à l'arsenal de base. */
    private static final String[] DEEP_TRAP_COMMANDS = {
            "effect give @p[distance=..4] minecraft:wither 5 1",
            "execute at @p[distance=..4] run summon minecraft:falling_block ~ ~4 ~ {BlockState:{Name:\"minecraft:anvil\"},Time:1}", // enclume
            "spreadplayers ~ ~ 3 8 false @p[distance=..3]",                             // téléportation désorientante
            "execute at @p[distance=..4] run summon minecraft:vex ~ ~1 ~ " + AUTHORIZED_NBT, // vexes d'embuscade
    };

    /**
     * Pièges à command block : plaque de pression visible (esquivable — c'est le jeu), command
     * block impulsion caché à la place du bloc de sol juste dessous. La plaque alimente
     * directement le bloc qu'elle chevauche → déclenchement fiable, zéro câblage redstone.
     */
    private static void placeCommandBlockTraps(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w < 16 || d < 16) return; // Only place in large enough rooms

        int count = 3 + Math.floorMod(floor + r.index() * 7, 3); // 3..5 pièges
        int cx = r.centerX(), cz = r.centerZ();
        for (int i = 0; i < count; i++) {
            int hx = hash(floor, r.index(), 100 + i * 2);
            int hz = hash(floor, r.index(), 101 + i * 2);
            int x = r.minX() + 4 + hx % (w - 8);
            int z = r.minZ() + 4 + hz % (d - 8);
            // Jamais au centre ni sur les axes de portes (comme les pièges de sol).
            if (Math.abs(x - cx) <= 2 && Math.abs(z - cz) <= 2) continue;
            if (x == cx || z == cz) continue;

            // Arsenal : base partout, pièges des profondeurs ajoutés dès l'étage 40.
            int pool = TRAP_COMMANDS.length + (floor >= 40 ? DEEP_TRAP_COMMANDS.length : 0);
            int pick = hash(floor, r.index(), 200 + i) % pool;
            String command = pick < TRAP_COMMANDS.length
                    ? TRAP_COMMANDS[pick]
                    : DEEP_TRAP_COMMANDS[pick - TRAP_COMMANDS.length];

            placeCommandTrap(lv, O(sp, x, 0, z), command);
        }
    }

    /**
     * Pose un piège : command block impulsion à la place du sol (y-1), plaque de pression dessus.
     * Package-private : réutilisé par {@link DungeonSecretRoom} (chambres bénies/maudites).
     */
    static void placeCommandTrap(ServerLevel lv, BlockPos platePos, String command) {
        BlockPos cbPos = platePos.below();
        S(lv, cbPos, Blocks.COMMAND_BLOCK.defaultBlockState());
        if (lv.getBlockEntity(cbPos) instanceof CommandBlockEntity cbe) {
            cbe.getCommandBlock().setCommand(command);
            cbe.getCommandBlock().setTrackOutput(false); // pas de spam de log
        }
        S(lv, platePos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
    }

    private static int hash(int a, int b, int c) {
        int h = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        return Math.floorMod(h ^ (h >>> 15), 4096);
    }

    /** Tous les blocs présents parmi les ids candidats (mods installés), dans l'ordre. */
    private static List<BlockState> resolveAll(List<String> ids) {
        List<BlockState> out = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
                out.add(BuiltInRegistries.BLOCK.get(loc).defaultBlockState());
            }
        }
        return out;
    }
}

package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootTable;
import tong.statmod.integration.lootr.LootrBridge;

import java.util.Random;

import static net.minecraft.world.level.block.Blocks.*;

/**
 * DungeonArchitect — Architecture "The Descent" (2026-07-03).
 *
 * <p>Chaque étage est un <b>complexe fortifié flottant</b> traversé selon un flow lisible :
 * seuil (spawn) → galerie à colonnade → ailes de combat → la faille (traversée) → le cœur
 * (arène / chambre forte / sanctuaire de boss) → la porte (sortie). Verticalité, tours d'angle,
 * toiture à puits de lumière, éclairage scénographié, thématisé par tier.
 *
 * <p>Réutilise {@link DungeonMasterpiece#buildUnderside} pour la base flottante. Le terrain
 * naturel est généré par {@link IslandTerrainShaper} via {@link IslandGenerator} avant l'appel
 * à cette méthode. Repère : {@code sp} = spawn à {@code Y=100}. Le sol naturel varie selon le
 * relief généré.
 */
public final class DungeonArchitect {

    /** Demi-emprise de la forteresse (rectangle ~128×104, tient sur l'île R=80). Agrandie 2026-07-05. */
    static final int HX = 64, HZ = 52;
    /** Hauteur intérieure (plafond à {@code y=WALL_H}). Relevée pour des étages plus imposants. */
    static final int WALL_H = 12;

    /** Rôle de l'étage — détermine le "cœur". */
    public enum Role { COMBAT, TREASURE, BOSS }

    private DungeonArchitect() {}

    // ═══════════════ helpers ═══════════════

    static BlockState B(Block b) { return b.defaultBlockState(); }
    static BlockState AIR() { return AIR.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos sp, int x, int y, int z) { return sp.offset(x, y, z); }

    static BlockState stair(Block b, Direction f) {
        return B(b).setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    /** Colonne verticale de {@code h} blocs à partir de (x, 0, z). */
    static void column(ServerLevel lv, BlockPos sp, int x, int z, int h, BlockState s) {
        for (int y = 0; y < h; y++) S(lv, O(sp, x, y, z), s);
    }

    /** Remplit un pavé plein [x0..x1]×[y0..y1]×[z0..z1]. */
    static void fill(ServerLevel lv, BlockPos sp, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    S(lv, O(sp, x, y, z), s);
    }

    static void air(ServerLevel lv, BlockPos sp, int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(lv, sp, x0, y0, z0, x1, y1, z1, AIR());
    }

    // ═══════════════ ENTRY POINT ═══════════════

    public static void buildFloor(ServerLevel lv, BlockPos sp, int floor, Role role) {
        // Palette par THÈME/arc (glace, os, nether, corail…) → l'architecture ressemble à son
        // thème, pas juste à son tier. Fallback tier via FloorPalette si besoin ailleurs.
        BlockPalette t = ThemePalette.forFloor(floor);
        // Seed déterministe par étage : même étage → même donjon, étages différents → layouts
        // différents. C'est ce qui fait qu'aucun étage ne ressemble à un autre.
        Random rng = new Random(floor * 0x9E3779B97F4A7C15L + 0xD1CE);

        // Base flottante (underside) uniquement - le terrain naturel est déjà généré par IslandGenerator
        DungeonMasterpiece.buildUnderside(lv, sp, t);

        // Dégage tout l'intérieur (headroom pour la structure + le combat).
        air(lv, sp, -HX + 1, 0, -HZ + 1, HX - 1, WALL_H + 2, HZ - 1);

        // ── NOUVELLE GÉNÉRATION (2026-07-05) ──
        // COMBAT & TRÉSOR : chaîne de pièces reliées (spawn → … → pièce finale avec téléporteur /
        //   coffres). BOSS : arène géante à ciel ouvert ceinturée de grands piliers.
        // (La forteresse « The Descent » ci-dessous n'est plus appelée — conservée pour référence.)
        if (role == Role.COMBAT || role == Role.TREASURE) {
            DungeonRoomChain.build(lv, sp, t, floor, role, rng);
            return;
        }
        if (role == Role.BOSS) {
            // Si le boss de l'étage a sa propre arène (ex : Bosses of Mass Destruction) : on pose une
            // PLATEFORME plate, on pose la structure dessus (centrée), puis on place l'autel sur le
            // vrai sol de l'arène (scan) et waystone/téléporteur sur la plateforme hors structure.
            // Sinon : arène générique façonnée selon le type du boss.
            if (DungeonBossStructures.hasArena(lv, floor)) {
                DungeonBossArenaFloor.buildPlatform(lv, sp, t);
                DungeonBossStructures.tryPlace(lv, sp, floor);
                DungeonBossArenaFloor.finishImportedArena(lv, sp, t, floor);
            } else {
                DungeonBossArenaFloor.build(lv, sp, t, floor);
            }
            return;
        }

        // ── Coque constante (identité + flow) ── [DEAD CODE — forteresse historique] ──
        perimeterWall(lv, sp, t);
        cornerTowers(lv, sp, t);
        gatehouse(lv, sp, t);
        processionalAvenue(lv, sp, t);

        // ── Intérieur variable (choisi par seed) ──
        int wingStyle = rng.nextInt(WING_STYLES);
        int midType = rng.nextInt(MID_TYPES);
        combatWings(lv, sp, t, wingStyle);
        midSection(lv, sp, t, midType);

        theHeart(lv, sp, t, role, floor);
        // Pas de toiture pour les étages boss : le sanctuaire reste ouvert (combat épique à ciel
        // ouvert). Les autres étages gardent leur toit d'ailes à puits de lumière.
        if (role != Role.BOSS) roofing(lv, sp, t);
        lighting(lv, sp, t, floor);
        // Bâtiments dans les ailes (étages agrandis) — tours, huttes, ruines, sanctuaires, puits.
        // Étages de combat uniquement : les ×5/×10 ont leur cœur trésor/boss dégagé.
        if (role == Role.COMBAT) DungeonBuildings.place(lv, sp, t, rng);
        narrativeDressing(lv, sp, t, rng);
        // Passe de détaillage « builder pro » : une multitude de petits détails thématisés, placés
        // intelligemment contre la structure (murs, angles, plafond). Rend l'étage vivant.
        DungeonDetailing.detail(lv, sp, ThemePalette.forFloor(floor), floor, rng);
        spawnThreshold(lv, sp, t);

        // Checkpoint Waystones sur les étages boss : une pierre à côté de l'autel. Une fois activée
        // par le joueur (après avoir vaincu le boss), elle sert de point de retour rapide. No-op si
        // Waystones absent. Posée sur le dais (haut walkable y=2), décalée de l'autel (0,2,hz).
        if (role == Role.BOSS) {
            int hz = -HZ + 11;
            tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                    lv, O(sp, 4, 2, hz), FloorPalette.forFloor(floor), floor);
        }
    }

    // ═══════════════ 1. PERIMETER WALL + BATTLEMENTS ═══════════════

    static void perimeterWall(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState wall = B(t.base());
        BlockState brick = B(t.accent());
        BlockState cap = B(t.decorPrimary());

        for (int x = -HX; x <= HX; x++) {
            wallColumn(lv, sp, x, -HZ, wall, brick, cap);
            wallColumn(lv, sp, x, HZ, wall, brick, cap);
        }
        for (int z = -HZ; z <= HZ; z++) {
            wallColumn(lv, sp, -HX, z, wall, brick, cap);
            wallColumn(lv, sp, HX, z, wall, brick, cap);
        }
    }

    static void wallColumn(ServerLevel lv, BlockPos sp, int x, int z, BlockState wall, BlockState brick, BlockState cap) {
        for (int y = 0; y < WALL_H; y++) {
            S(lv, O(sp, x, y, z), (y == WALL_H - 1) ? brick : wall);
        }
        // Créneaux : un bloc sur deux au sommet.
        if (((x + z) & 1) == 0) S(lv, O(sp, x, WALL_H, z), cap);
        // Meurtrières décoratives à mi-hauteur.
        if (((x + z) % 6 == 0) && WALL_H >= 5) S(lv, O(sp, x, WALL_H - 4, z), AIR());
    }

    // ═══════════════ 2. CORNER TOWERS ═══════════════

    static void cornerTowers(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int[][] c = {{-HX, -HZ}, {HX, -HZ}, {-HX, HZ}, {HX, HZ}};
        for (int[] p : c) tower(lv, sp, p[0], p[1], t);
    }

    static void tower(ServerLevel lv, BlockPos sp, int cx, int cz, BlockPalette t) {
        BlockState wall = B(t.base());
        BlockState brick = B(t.accent());
        BlockState cap = B(t.decorPrimary());
        BlockState light = B(t.light());
        int h = WALL_H + 5;
        int dxs = Integer.signum(-cx), dzs = Integer.signum(-cz); // vers l'intérieur

        // Fût 5×5.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (edge) {
                for (int y = 0; y < h; y++)
                    S(lv, O(sp, cx + dx, y, cz + dz), (y % 4 == 3) ? brick : wall);
            } else {
                air(lv, sp, cx + dx, 0, cz + dz, cx + dx, h - 1, cz + dz);
            }
        }
        // Couronne crénelée.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && ((dx + dz) & 1) == 0)
                S(lv, O(sp, cx + dx, h, cz + dz), cap);
        }
        // Toit plancher + fanal.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            S(lv, O(sp, cx + dx, h - 1, cz + dz), brick);
        S(lv, O(sp, cx, h, cz), light);
        // Porte intérieure vers le rempart.
        air(lv, sp, cx + dxs * 2, 0, cz + dzs * 2, cx + dxs * 2, 2, cz + dzs * 2);
    }

    // ═══════════════ 3. GATEHOUSE (sud, +Z) + RETURN BEACON ═══════════════

    static void gatehouse(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState brick = B(t.accent());
        BlockState wall = B(t.base());
        BlockState light = B(t.light());

        // Arche d'entrée dans le mur sud, largeur 5, haut 5.
        for (int dx = -2; dx <= 2; dx++) {
            int top = (Math.abs(dx) == 2) ? 4 : (Math.abs(dx) == 1 ? 5 : 6);
            air(lv, sp, dx, 0, HZ, dx, top - 1, HZ);
            S(lv, O(sp, dx, top, HZ), brick);
        }
        // Piédroits saillants.
        for (int dz = 0; dz <= 3; dz++) {
            column(lv, sp, -3, HZ - dz, 6, wall);
            column(lv, sp, 3, HZ - dz, 6, wall);
        }
        S(lv, O(sp, -3, 6, HZ), light);
        S(lv, O(sp, 3, 6, HZ), light);

        // Alcôve du return beacon, juste à l'intérieur de la porte.
        BlockPos beacon = O(sp, 0, 0, HZ - 3);
        for (int dx = -1; dx <= 1; dx++) S(lv, O(beacon, dx, -1, 0), brick);
        air(lv, sp, -1, 0, HZ - 3, 1, 2, HZ - 3);
        S(lv, beacon, B(DungeonBlocks.RETURN_BEACON.get()));
        S(lv, O(beacon, -1, 2, 0), light);
        S(lv, O(beacon, 1, 2, 0), light);
    }

    // ═══════════════ 4. PROCESSIONAL AVENUE (colonnade centrale N-S) ═══════════════

    static void processionalAvenue(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState pillar = B(t.decorPrimary());
        BlockState brick = B(t.accent());
        BlockState light = B(t.light());
        BlockState slab = B(t.slab());

        // Deux rangées de colonnes hautes reliées par des arches, de z=+HZ-6 à z=-HZ+10.
        for (int z = HZ - 6; z >= -HZ + 12; z -= 5) {
            for (int side : new int[]{-6, 6}) {
                column(lv, sp, side, z, WALL_H - 1, pillar);
                S(lv, O(sp, side, WALL_H - 1, z), brick);
                // Chapiteau.
                S(lv, O(sp, side, WALL_H - 2, z), brick);
                // Lanterne suspendue à mi-portée.
                S(lv, O(sp, side, WALL_H - 3, z), B(CHAIN));
                S(lv, O(sp, side, WALL_H - 4, z), light);
            }
            // Arche transversale au sommet reliant les deux colonnes.
            for (int x = -6; x <= 6; x++)
                S(lv, O(sp, x, WALL_H - 1, z), brick);
            // Bordures de tapis surélevées (chemin central).
            S(lv, O(sp, -4, 0, z), slab);
            S(lv, O(sp, 4, 0, z), slab);
        }
    }

    // ═══════════════ 5. COMBAT WINGS (est/ouest) — 3 styles ═══════════════

    static final int WING_STYLES = 3;

    static void combatWings(ServerLevel lv, BlockPos sp, BlockPalette t, int style) {
        wing(lv, sp, -1, t, style);
        wing(lv, sp, +1, t, style);
    }

    static void wing(ServerLevel lv, BlockPos sp, int side, BlockPalette t, int style) {
        int wallX = side * HX;
        int innerX = side * (HX - 12);
        BlockState base = B(t.base());
        BlockState brick = B(t.accent());

        // Mur de refend séparant l'aile de l'avenue, percé de 3 arches (commun aux styles).
        for (int z = -HZ + 3; z <= HZ - 3; z++) {
            boolean doorway = (z > -14 && z < -8) || (z > -3 && z < 3) || (z > 8 && z < 14);
            for (int y = 0; y < WALL_H - 1; y++) {
                if (doorway && y < 4) S(lv, O(sp, innerX, y, z), AIR());
                else S(lv, O(sp, innerX, y, z), (y == WALL_H - 2) ? brick : base);
            }
        }

        switch (style) {
            case 0 -> wingColonnade(lv, sp, side, t, wallX);
            case 1 -> wingPrison(lv, sp, side, t, wallX);
            default -> wingCrypt(lv, sp, side, t, wallX);
        }
    }

    /** Style 0 — colonnade ouverte avec contreforts lumineux. */
    static void wingColonnade(ServerLevel lv, BlockPos sp, int side, BlockPalette t, int wallX) {
        BlockState pillar = B(t.decorPrimary());
        BlockState light = B(t.light());
        for (int z = -HZ + 6; z <= HZ - 6; z += 8) {
            column(lv, sp, wallX - side * 2, z, WALL_H - 2, pillar);
            S(lv, O(sp, wallX - side * 2, WALL_H - 2, z), light);
        }
    }

    /** Style 1 — rangée de cellules de prison à barreaux de fer. */
    static void wingPrison(ServerLevel lv, BlockPos sp, int side, BlockPalette t, int wallX) {
        BlockState wall = B(t.base());
        BlockState bars = B(IRON_BARS);
        int cellX = wallX - side * 4; // façade des cellules face à l'avenue
        for (int z = -HZ + 5; z <= HZ - 9; z += 7) {
            // Cloisons entre cellules.
            for (int dz = 0; dz <= 5; dz++) {
                column(lv, sp, cellX, z + dz, 4, (dz == 0 || dz == 5) ? wall : bars);
                column(lv, sp, wallX - side, z + dz, 4, wall);
            }
            // Barreaux en façade (avec une porte au milieu).
            for (int dz = 1; dz <= 4; dz++) {
                if (dz == 2) continue; // ouverture
                column(lv, sp, cellX, z + dz, 4, bars);
            }
        }
    }

    /** Style 2 — crypte à niches funéraires (os + arches basses). */
    static void wingCrypt(ServerLevel lv, BlockPos sp, int side, BlockPalette t, int wallX) {
        BlockState brick = B(t.accent());
        BlockState bone = B(BONE_BLOCK);
        int nicheX = wallX - side * 2;
        for (int z = -HZ + 6; z <= HZ - 6; z += 5) {
            // Niche voûtée creusée dans le mur.
            S(lv, O(sp, nicheX, 0, z), brick);
            S(lv, O(sp, nicheX, 1, z), bone);
            S(lv, O(sp, nicheX, 2, z), brick);
            S(lv, O(sp, nicheX, 3, z), brick);
        }
    }

    // ═══════════════ 6. MID-SECTION (la Faille) — 4 types ═══════════════

    static final int MID_TYPES = 4;

    static void midSection(ServerLevel lv, BlockPos sp, BlockPalette t, int type) {
        switch (type) {
            case 0 -> midChasm(lv, sp, t);
            case 1 -> midSunkenArena(lv, sp, t);
            case 2 -> midPillarForest(lv, sp, t);
            default -> midCollapsed(lv, sp, t);
        }
    }

    /**
     * Type 0 — canal décoratif franchi par un pont surélevé à garde-corps. Le canal est une
     * dépression peu profonde à <b>fond solide</b> (pas de vide) : on ne peut pas tomber dans
     * un trou, le sol du donjon reste plein.
     */
    static void midChasm(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState brick = B(t.accent());
        BlockState rail = B(t.wallBlock());
        BlockState bottom = B(t.underside());
        for (int x = -8; x <= 8; x++) {
            for (int z = -2; z <= 2; z++) {
                if (z == 0) continue;                 // ligne du pont, gardée pleine
                S(lv, O(sp, x, -2, z), bottom);       // fond solide du canal
                S(lv, O(sp, x, -1, z), AIR());        // creux d'un bloc (décoratif)
            }
            // Pont central surélevé + garde-corps.
            S(lv, O(sp, x, -1, 0), brick);
            S(lv, O(sp, x, 0, -1), rail);
            S(lv, O(sp, x, 0, 1), rail);
        }
    }

    /** Type 1 — arène en contrebas (fosse circulaire à gradins), cœur de combat. */
    static void midSunkenArena(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState floor = B(t.base());
        BlockState step = B(t.slab());
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 7 * 7) {
                air(lv, sp, dx, 0, dz, dx, 0, dz);
                S(lv, O(sp, dx, -2, dz), floor); // fond de l'arène (-2)
                air(lv, sp, dx, -1, dz, dx, -1, dz);
            } else if (d2 <= 9 * 9) {
                S(lv, O(sp, dx, -1, dz), step); // gradin intermédiaire
            }
        }
    }

    /** Type 2 — forêt de piliers (combat en couvert, lignes de vue cassées). */
    static void midPillarForest(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState pillar = B(t.decorPrimary());
        BlockState light = B(t.light());
        for (int x = -9; x <= 9; x += 3) for (int z = -6; z <= 6; z += 3) {
            if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue; // laisse le passage central
            int h = 4 + ((x + z) & 1) * 2;
            column(lv, sp, x, z, h, pillar);
            if (((x * 7 + z) & 3) == 0) S(lv, O(sp, x, h, z), light);
        }
    }

    /**
     * Type 3 — salle effondrée : colonnes brisées et gravats (feel ruine). Aucun trou dans le
     * sol — le donjon reste plein et sûr.
     */
    static void midCollapsed(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState rubble = B(t.decorSecondary());
        BlockState broken = B(t.base());
        Random r = new Random(0xC0113D);
        for (int i = 0; i < 16; i++) {
            int x = r.nextInt(19) - 9, z = r.nextInt(13) - 6;
            if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue;
            int h = 1 + r.nextInt(3);
            column(lv, sp, x, z, h, r.nextBoolean() ? rubble : broken);
        }
    }

    // ═══════════════ 7. THE HEART (nord, -Z) selon le rôle ═══════════════

    static void theHeart(ServerLevel lv, BlockPos sp, BlockPalette t, Role role, int floor) {
        BlockState brick = B(t.accent());
        BlockState pillar = B(t.decorPrimary());
        BlockState light = B(t.light());
        boolean boss = role == Role.BOSS;
        int hz = -HZ + 11; // centre de l'estrade, au nord

        // ── Estrade surélevée (haut walkable à y=2). Emprise x∈[-7,7], z∈[hz-6, hz+5]. ──
        for (int dx = -7; dx <= 7; dx++) for (int dz = -6; dz <= 5; dz++) {
            int z = hz + dz;
            S(lv, O(sp, dx, 0, z), brick);
            boolean rim = Math.abs(dx) == 7 || dz == -6 || dz == 5;
            S(lv, O(sp, dx, 1, z), rim ? pillar : brick);
        }
        // Headroom au-dessus de l'estrade (boss = ouvert, sinon sous baldaquin).
        air(lv, sp, -7, 2, hz - 6, 7, boss ? WALL_H + 2 : WALL_H - 2, hz + 5);

        // ── Grand escalier, face sud de l'estrade (bord avant z=hz+5, haut y=2). Largeur 9. ──
        // Marche basse au sud (y0) → marche haute (y1) → dessus de l'estrade (y2).
        for (int dx = -4; dx <= 4; dx++) {
            S(lv, O(sp, dx, 1, hz + 6), stair(t.stair(), Direction.NORTH)); // marche haute
            S(lv, O(sp, dx, 0, hz + 7), stair(t.stair(), Direction.NORTH)); // marche basse
            // Sol dégagé devant l'escalier.
            S(lv, O(sp, dx, -1, hz + 8), brick);
        }
        // Joues d'escalier (rampes latérales).
        for (int side : new int[]{-5, 5}) {
            S(lv, O(sp, side, 0, hz + 6), pillar);
            column(lv, sp, side, hz + 6, 3, pillar);
            S(lv, O(sp, side, 3, hz + 6), light);
        }

        // ── Colonnes d'angle : obélisques ouverts (boss) ou colonnes-baldaquin (autres). ──
        int colTop = boss ? WALL_H + 3 : WALL_H - 1;
        for (int cx : new int[]{-6, 6}) for (int cz : new int[]{-4, 4}) {
            for (int y = 2; y <= colTop; y++) S(lv, O(sp, cx, y, hz + cz), pillar);
            S(lv, O(sp, cx, colTop, hz + cz), light);
        }
        if (!boss) {
            // Baldaquin reliant les colonnes (pas de plafond pour le boss).
            for (int dx = -6; dx <= 6; dx++) for (int dz = -4; dz <= 4; dz++)
                if (Math.abs(dx) == 6 || Math.abs(dz) == 4)
                    S(lv, O(sp, dx, WALL_H - 1, hz + dz), brick);
        }

        switch (role) {
            case BOSS -> {
                // Arène façonnée selon le TYPE du boss (bassin aquatique, arène haute pour un
                // volant, fosse pour un colosse, magma pour un infernal, os pour un mort-vivant…).
                // Anchor = centre de l'estrade nord (hz = -HZ+11) pour la forteresse.
                DungeonBossArena.shape(lv, O(sp, 0, 0, -HZ + 11), t, floor);
                S(lv, O(sp, 0, 2, hz), B(DungeonBlocks.BOSS_ALTAR.get()));
                // Salle de boss enrichie : fontaine de soin + armor stands trophées.
                DungeonRoomDressing.dressBossRoom(lv, O(sp, 0, 2, hz), t, floor);
            }
            case TREASURE -> {
                int[][] chests = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
                for (int[] c : chests) {
                    S(lv, O(sp, c[0], 2, hz + c[1]), pillar);
                    placeChest(lv, O(sp, c[0], 3, hz + c[1]));
                }
                // Salle trésor enrichie : fontaine de soin, waypoint, armor stands, piédestaux.
                DungeonRoomDressing.dressTreasureRoom(lv, O(sp, 0, 2, hz), t, floor);
            }
            case COMBAT -> {
                S(lv, O(sp, 0, 2, hz), stair(t.stair(), Direction.SOUTH)); // trône
                S(lv, O(sp, 0, 3, hz - 1), B(t.decorSecondary()));
            }
        }

        // ── Porte de sortie (next-floor teleporter) tout au nord, encadrée. ──
        BlockPos gate = O(sp, 0, 0, -HZ + 2);
        air(lv, sp, -1, 0, -HZ + 2, 1, 3, -HZ + 2);
        column(lv, sp, -2, -HZ + 2, 4, pillar);
        column(lv, sp, 2, -HZ + 2, 4, pillar);
        S(lv, O(sp, -2, 4, -HZ + 2), light);
        S(lv, O(sp, 2, 4, -HZ + 2), light);
        S(lv, gate, B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
    }

    // ═══════════════ 8. ROOFING (toiture partielle à puits de lumière) ═══════════════

    static void roofing(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState roof = B(t.ceiling());
        BlockState beam = B(t.decorPrimary());
        // Toit sur les ailes (au-delà de |x|>=12), open sur l'avenue centrale (puits de lumière).
        for (int x = -HX + 1; x <= HX - 1; x++) {
            if (Math.abs(x) < 12) continue; // avenue ouverte
            for (int z = -HZ + 1; z <= HZ - 1; z++) {
                boolean skylight = (x % 7 == 0 && z % 7 == 0);
                S(lv, O(sp, x, WALL_H, z), skylight ? AIR() : ((x + z) % 5 == 0 ? beam : roof));
            }
        }
    }

    // ═══════════════ 9. LIGHTING (braseros au sol) ═══════════════

    static void lighting(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        BlockState post = B(t.decorPrimary());
        BlockState light = B(t.light());
        Random rng = new Random(floor * 733L + 17L);
        // Braséros en quinconce dans les ailes.
        for (int x = -HX + 6; x <= HX - 6; x += 10) {
            for (int z = -HZ + 6; z <= HZ - 6; z += 10) {
                if (Math.abs(x) < 10) continue; // pas sur l'avenue
                S(lv, O(sp, x, 0, z), post);
                S(lv, O(sp, x, 1, z), light);
            }
        }
        // Quelques torches murales aléatoires pour casser l'uniformité.
        for (int i = 0; i < 12; i++) {
            int x = rng.nextInt(2 * HX - 6) - (HX - 3);
            int z = rng.nextInt(2 * HZ - 6) - (HZ - 3);
            if (Math.abs(x) < 8 && Math.abs(z) < 8) continue;
            S(lv, O(sp, x, 3, z), light);
        }
    }

    // ═══════════════ 9b. NARRATIVE DRESSING (l'espace raconte une histoire) ═══════════════

    static void narrativeDressing(ServerLevel lv, BlockPos sp, BlockPalette t, Random rng) {
        // Restes de combats passés : ossements, toiles, gravats, poteries, cicatrices de brûlé.
        // Positionné aléatoirement (seed d'étage) en évitant l'avenue centrale et le spawn.
        int placed = 0, attempts = 0;
        while (placed < 26 && attempts < 200) {
            attempts++;
            int x = rng.nextInt(2 * HX - 8) - (HX - 4);
            int z = rng.nextInt(2 * HZ - 8) - (HZ - 4);
            if (Math.abs(x) < 8 && Math.abs(z) < 8) continue;   // épargne le centre/spawn
            if (Math.abs(x) < 5) continue;                       // épargne l'avenue
            if (!lv.getBlockState(O(sp, x, 0, z)).isAir()) continue; // pose au sol libre

            BlockPos g = O(sp, x, 0, z);
            switch (rng.nextInt(8)) {
                case 0, 1 -> S(lv, g, B(BONE_BLOCK));                         // tas d'os
                case 2 -> S(lv, g, B(SKELETON_SKULL));                        // crâne
                case 3 -> { S(lv, g, B(COBWEB)); S(lv, g.above(), B(COBWEB)); } // toiles
                case 4 -> S(lv, g, B(DECORATED_POT));                         // poterie
                case 5 -> { // colonne brisée
                    S(lv, g, B(t.decorPrimary()));
                    if (rng.nextBoolean()) S(lv, g.above(), B(t.decorSecondary()));
                }
                case 6 -> S(lv, g.below(), B(t.scar()));                      // cicatrice au sol
                default -> { S(lv, g, B(CANDLE)); }                           // bougie/veillée
            }
            placed++;
        }

        // Bannières déchirées suspendues aux murs des ailes (verticales, wool teinté).
        BlockState banner = B(t.banner());
        for (int side : new int[]{-1, 1}) {
            for (int z = -HZ + 8; z <= HZ - 8; z += 12) {
                int x = side * (HX - 1);
                S(lv, O(sp, x, WALL_H - 2, z), banner);
                S(lv, O(sp, x, WALL_H - 3, z), banner);
            }
        }
    }

    // ═══════════════ 10. SPAWN THRESHOLD (centre dégagé) ═══════════════

    static void spawnThreshold(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState floor = B(t.accent());
        BlockState light = B(t.light());
        // Dalle 5×5 + 4 blocs d'air de headroom.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            S(lv, O(sp, dx, -1, dz), floor);
            air(lv, sp, dx, 0, dz, dx, 4, dz);
        }
        // 4 bornes lumineuses aux angles.
        for (int cx : new int[]{-2, 2}) for (int cz : new int[]{-2, 2}) {
            S(lv, O(sp, cx, 0, cz), B(t.decorPrimary()));
            S(lv, O(sp, cx, 1, cz), light);
        }
    }

    // ═══════════════ util ═══════════════

    static void placeChest(ServerLevel lv, BlockPos pos) {
        ResourceKey<LootTable> lootTable = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("statmod", "chests/dungeon_treasure"));
        LootrBridge.placeIndividualChest(lv, pos, lootTable);
    }
}

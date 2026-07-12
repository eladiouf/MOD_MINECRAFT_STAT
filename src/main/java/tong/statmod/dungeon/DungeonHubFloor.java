package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;
import static tong.statmod.dungeon.DungeonArchitect.fill;
import static tong.statmod.dungeon.DungeonArchitect.air;
import static tong.statmod.dungeon.DungeonArchitect.column;

/**
 * Hub pacifique du Trial Dungeon (Étage 0) - Version Temple Majestueux & Marché RPG (2026-07-11).
 * Transforme la simple dalle plate en un magnifique sanctuaire fortifié doté de colonnades,
 * d'un dôme à ciel ouvert, de 4 boutiques thématiques uniques (Armes, Armures, Potions, Tir à l'arc),
 * d'un dortoir cosy, d'une forge sécurisée sans fluides coulants, d'un coin d'enchantement niveau 30,
 * et d'une double arche monumentale abritant le portail de retour et le téléporteur du 1er étage.
 */
public final class DungeonHubFloor {

    private DungeonHubFloor() {}

    public static void build(ServerLevel lv, BlockPos sp, BlockPalette t) {
        // 1. Dégagement complet de la zone (rayon 32) pour éviter l'eau/lave naturelle coulant de l'île
        air(lv, sp, -32, 0, -32, 32, 16, 32);
        
        // 2. Base plate de l'arène (rempart plein pour bloquer toute infiltration de liquide)
        DungeonBossArenaFloor.buildPlatform(lv, sp, t);

        // 3. Murs d'enceinte du Temple et Arches de pierre
        buildTempleShell(lv, sp, t);

        // 4. Waystone centrale (Hub) sur son estrade circulaire
        buildWaystoneCenter(lv, sp, t);

        // 5. Boutiques thématiques et personnalisées pour les marchands
        buildWeaponsmithShop(lv, sp, t); // Armes
        buildArmorerShop(lv, sp, t);     // Armures
        buildAlchemistShop(lv, sp, t);   // Potions & Alchimie
        buildFletcherShop(lv, sp, t);    // Archerie & Projectiles

        // Banque magique : échange des récompenses Bountiful physiques contre le solde FDP_cfa.
        tong.statmod.economy.MagicBanker.spawn(lv, O(sp, 7, 1, 7));

        // 6. Coin d'Enchantement Divin (Niveau 30 Garanti)
        buildEnchantingNook(lv, sp);

        // 7. La Forge Royale de l'Armurier (Sécurisée, sans coulée de lave)
        buildRoyalForge(lv, sp);

        // 8. Le Dortoir des Aventuriers (Repos Cosy)
        buildAdventurerDorm(lv, sp);

        // 9. Portail de Retour & Téléporteur d'Accès au 1er Étage
        buildPortalsZone(lv, sp, t);
    }

    private static void buildTempleShell(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState stone = B(t.base());
        BlockState brick = B(t.accent());
        BlockState light = B(t.light());

        // Piliers d'angle majeurs 3x3 de hauteur 10
        int[] coords = {-28, 28};
        for (int x : coords) {
            for (int z : coords) {
                fill(lv, sp, x - 1, 0, z - 1, x + 1, 9, z + 1, brick);
                fill(lv, sp, x - 2, 9, z - 2, x + 2, 9, z + 2, stone);
                S(lv, O(sp, x, 10, z), light);
            }
        }

        // Piliers intermédiaires N-S-E-O
        for (int coord : coords) {
            fill(lv, sp, coord - 1, 0, -1, coord + 1, 9, 1, brick);
            fill(lv, sp, -1, 0, coord - 1, 1, 9, coord + 1, brick);
        }

        // Arches reliant les piliers à y=8 et y=9
        for (int x = -27; x <= 27; x++) {
            if (x == 0) continue;
            S(lv, O(sp, x, 8, -28), brick);
            S(lv, O(sp, x, 8, 28), brick);
            S(lv, O(sp, -28, 8, x), brick);
            S(lv, O(sp, 28, 8, x), brick);
            
            if (Math.abs(x) % 6 == 0) {
                S(lv, O(sp, x, 7, -28), B(t.slab()));
                S(lv, O(sp, x, 7, 28), B(t.slab()));
                S(lv, O(sp, -28, 7, x), B(t.slab()));
                S(lv, O(sp, 28, 7, x), B(t.slab()));
            }
        }
    }

    private static void buildWaystoneCenter(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState brick = B(t.accent());
        BlockState stairN = B(t.stair()).setValue(StairBlock.FACING, Direction.NORTH);
        BlockState stairS = B(t.stair()).setValue(StairBlock.FACING, Direction.SOUTH);
        BlockState stairE = B(t.stair()).setValue(StairBlock.FACING, Direction.EAST);
        BlockState stairW = B(t.stair()).setValue(StairBlock.FACING, Direction.WEST);

        // Estrade circulaire de rayon 3
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x * x + z * z <= 9) {
                    S(lv, O(sp, x, 0, z), brick);
                }
            }
        }

        // Escalier circulaire
        for (int i = -2; i <= 2; i++) {
            S(lv, O(sp, i, 0, -4), stairS);
            S(lv, O(sp, i, 0, 4), stairN);
            S(lv, O(sp, -4, 0, i), stairE);
            S(lv, O(sp, 4, 0, i), stairW);
        }

        // Waystone au centre surélevée à y=1
        tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                lv, O(sp, 0, 1, 0), FloorPalette.forFloor(10), 0);
    }

    // ── BOUTIQUE 1 : LES ARMES ──
    private static void buildWeaponsmithShop(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int cx = -15, cz = -15;
        BlockState counter = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        BlockState wool = Blocks.RED_WOOL.defaultBlockState();

        // Comptoir en andésite polie
        for (int dx = -1; dx <= 1; dx++) {
            S(lv, O(sp, cx + dx, 0, cz), counter);
        }
        
        // Poteaux de clôture soutenant la bâche rouge
        for (int dx = -2; dx <= 2; dx += 4) {
            column(lv, sp, cx + dx, cz, 3, fence);
        }
        
        // Toit en laine rouge
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, cz), wool);
            S(lv, O(sp, cx + dx, 3, cz - 1), wool);
        }

        // Décoration : Meules d'affûtage et râteliers d'armes
        S(lv, O(sp, cx - 2, 0, cz - 1), Blocks.GRINDSTONE.defaultBlockState());
        placeArmorStandWithItem(lv, O(sp, cx + 2, 0, cz - 1), new ItemStack(Items.IRON_SWORD));

        // Spawne le marchand d'armes
        DungeonMerchant.placeStall(lv, sp, cx, cz, t, 0, 10);
    }

    // ── BOUTIQUE 2 : LES ARMURES ──
    private static void buildArmorerShop(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int cx = -15, cz = 15;
        BlockState counter = Blocks.POLISHED_DIORITE.defaultBlockState();
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        BlockState wool = Blocks.BLUE_WOOL.defaultBlockState();

        // Comptoir en diorite polie
        for (int dx = -1; dx <= 1; dx++) {
            S(lv, O(sp, cx + dx, 0, cz), counter);
        }

        // Poteaux de clôture soutenant la bâche bleue
        for (int dx = -2; dx <= 2; dx += 4) {
            column(lv, sp, cx + dx, cz, 3, fence);
        }

        // Toit en laine bleue
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, cz), wool);
            S(lv, O(sp, cx + dx, 3, cz - 1), wool);
        }

        // Décoration : Porte-armure avec armure de fer et bouclier
        placeFullArmorStand(lv, O(sp, cx - 2, 0, cz - 1), Items.IRON_CHESTPLATE, Items.IRON_HELMET);
        S(lv, O(sp, cx + 2, 0, cz - 1), Blocks.ANVIL.defaultBlockState());

        // Spawne le marchand d'armures
        DungeonMerchant.placeStall(lv, sp, cx, cz, t, 1, 20);
    }

    // ── BOUTIQUE 3 : LES POTIONS & L'ALCHIMIE ──
    private static void buildAlchemistShop(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int cx = 15, cz = -15;
        BlockState counter = Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState();
        BlockState fence = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
        BlockState wool = Blocks.PURPLE_WOOL.defaultBlockState();

        // Comptoir en bois distordu
        for (int dx = -1; dx <= 1; dx++) {
            S(lv, O(sp, cx + dx, 0, cz), counter);
        }

        // Poteaux de clôture soutenant la bâche violette
        for (int dx = -2; dx <= 2; dx += 4) {
            column(lv, sp, cx + dx, cz, 3, fence);
        }

        // Toit en laine violette
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, cz), wool);
            S(lv, O(sp, cx + dx, 3, cz - 1), wool);
        }

        // Décoration : Alambic d'alchimie, chaudron d'eau purifiée
        S(lv, O(sp, cx - 2, 0, cz - 1), Blocks.BREWING_STAND.defaultBlockState());
        S(lv, O(sp, cx + 2, 0, cz - 1), Blocks.WATER_CAULDRON.defaultBlockState());

        // Spawne le prêtre/alchimiste (potions)
        DungeonMerchant.placeStall(lv, sp, cx, cz, t, 2, 30);
    }

    // ── BOUTIQUE 4 : L'ARCHERIE ──
    private static void buildFletcherShop(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int cx = 15, cz = 15;
        BlockState counter = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        BlockState fence = Blocks.BIRCH_FENCE.defaultBlockState();
        BlockState wool = Blocks.GREEN_WOOL.defaultBlockState();

        // Comptoir en bois d'échafaudage écorcé
        for (int dx = -1; dx <= 1; dx++) {
            S(lv, O(sp, cx + dx, 0, cz), counter);
        }

        // Poteaux de clôture soutenant la bâche verte
        for (int dx = -2; dx <= 2; dx += 4) {
            column(lv, sp, cx + dx, cz, 3, fence);
        }

        // Toit en laine verte
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, cz), wool);
            S(lv, O(sp, cx + dx, 3, cz - 1), wool);
        }

        // Décoration : Cibles d'entraînement et bottes de foin
        S(lv, O(sp, cx - 2, 0, cz - 1), Blocks.TARGET.defaultBlockState());
        S(lv, O(sp, cx + 2, 0, cz - 1), Blocks.HAY_BLOCK.defaultBlockState());

        // Spawne le marchand d'archerie
        DungeonMerchant.placeStall(lv, sp, cx, cz, t, 3, 40);
    }

    private static void buildEnchantingNook(ServerLevel lv, BlockPos sp) {
        BlockPos center = O(sp, -21, 1, 0);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                S(lv, O(center, x, -1, z), Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
            }
        }

        S(lv, center, Blocks.ENCHANTING_TABLE.defaultBlockState());

        int[][] libraryCoords = {
            {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
            {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2},
            {-2, -1}, {-2, 0}, {-2, 1}
        };

        int placed = 0;
        for (int[] coord : libraryCoords) {
            S(lv, O(center, coord[0], 0, coord[1]), Blocks.BOOKSHELF.defaultBlockState());
            placed++;
            if (placed <= 15) {
                S(lv, O(center, coord[0], 1, coord[1]), Blocks.BOOKSHELF.defaultBlockState());
            } else {
                S(lv, O(center, coord[0], 1, coord[1]), Blocks.OAK_SLAB.defaultBlockState());
            }
        }

        S(lv, O(center, -2, 2, -2), Blocks.SOUL_LANTERN.defaultBlockState());
        S(lv, O(center, -2, 2, 2), Blocks.SOUL_LANTERN.defaultBlockState());
    }

    private static void buildRoyalForge(ServerLevel lv, BlockPos sp) {
        BlockPos center = O(sp, 21, 1, 0);

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                S(lv, O(center, x, -1, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            }
        }

        S(lv, center, Blocks.ANVIL.defaultBlockState());

        // Fourneaux + feu de camp d'âme (incandescent et sécurisé, pas de liquide coulant)
        S(lv, O(center, -1, 0, -2), Blocks.BLAST_FURNACE.defaultBlockState());
        S(lv, O(center, 1, 0, -2), Blocks.BLAST_FURNACE.defaultBlockState());
        fill(lv, center, -1, 1, -2, 1, 3, -2, Blocks.COBBLESTONE.defaultBlockState()); // Cheminée
        
        // Creuset de forge sécurisé (magma + feu de camp d'âme encastré au sol)
        S(lv, O(center, 0, 0, -2), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        S(lv, O(center, 0, 0, -1), Blocks.MAGMA_BLOCK.defaultBlockState());

        S(lv, O(center, -2, 0, 0), Blocks.GRINDSTONE.defaultBlockState());
        S(lv, O(center, -2, 0, 1), Blocks.SMITHING_TABLE.defaultBlockState());
        S(lv, O(center, 2, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        S(lv, O(center, 2, 0, 1), Blocks.ENDER_CHEST.defaultBlockState());
    }

    private static void buildAdventurerDorm(ServerLevel lv, BlockPos sp) {
        BlockPos center = O(sp, 0, 1, 20);

        fill(lv, center, -6, -1, -2, 6, -1, 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int x = -5; x <= 5; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockState wool = ((x + z) % 2 == 0) ? Blocks.RED_CARPET.defaultBlockState() : Blocks.BLUE_CARPET.defaultBlockState();
                S(lv, O(center, x, 0, z), wool);
            }
        }

        BlockPos bedPosL = O(center, -3, 0, 0);
        BlockPos bedPosR = O(center, 3, 0, 0);

        placeBed(lv, bedPosL, Direction.NORTH, Blocks.RED_BED);
        placeBed(lv, bedPosR, Direction.NORTH, Blocks.BLUE_BED);

        S(lv, O(center, -3, 0, 2), Blocks.BOOKSHELF.defaultBlockState());
        S(lv, O(center, -4, 0, 2), Blocks.BOOKSHELF.defaultBlockState());
        S(lv, O(center, 3, 0, 2), Blocks.BOOKSHELF.defaultBlockState());
        S(lv, O(center, 4, 0, 2), Blocks.BOOKSHELF.defaultBlockState());

        S(lv, O(center, -3, 0, -2), Blocks.ENDER_CHEST.defaultBlockState());
        S(lv, O(center, 3, 0, -2), Blocks.ENDER_CHEST.defaultBlockState());

        S(lv, O(center, -4, 1, 2), Blocks.LANTERN.defaultBlockState());
        S(lv, O(center, 4, 1, 2), Blocks.LANTERN.defaultBlockState());
    }

    private static void buildPortalsZone(ServerLevel lv, BlockPos sp, BlockPalette t) {
        // Zone des portails au sud (0, -20)
        BlockPos portalPos = O(sp, 0, 1, -20);

        BlockState brick = B(t.accent());
        BlockState stairN = B(t.stair()).setValue(StairBlock.FACING, Direction.NORTH);

        // Grande estrade de pierre
        fill(lv, portalPos, -4, -1, -2, 4, -1, 2, brick);
        for (int x = -4; x <= 4; x++) {
            S(lv, O(portalPos, x, 0, 2), stairN);
        }

        // Trois colonnes majestueuses pour séparer les deux portails
        column(lv, portalPos, -3, 0, 5, B(t.decorPrimary()));
        column(lv, portalPos, 0, 0, 5, B(t.decorPrimary()));
        column(lv, portalPos, 3, 0, 5, B(t.decorPrimary()));
        
        S(lv, O(portalPos, -3, 5, 0), B(t.light()));
        S(lv, O(portalPos, 0, 5, 0), B(t.light()));
        S(lv, O(portalPos, 3, 5, 0), B(t.light()));

        // Arches supérieures reliance
        for (int x = -3; x <= 3; x++) {
            S(lv, O(portalPos, x, 4, 0), brick);
        }

        // PORTAIL GAUCHE : Balise de retour vers l'Overworld (Return Beacon)
        S(lv, O(portalPos, -1, 0, 0), B(DungeonBlocks.RETURN_BEACON.get()));

        // PORTAIL DROITE : Téléporteur vers le 1er Étage (Next Floor Teleporter)
        S(lv, O(portalPos, 1, 0, 0), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));

        // Bougies cérémonielles
        S(lv, O(portalPos, -2, 0, 0), Blocks.CANDLE.defaultBlockState());
        S(lv, O(portalPos, 2, 0, 0), Blocks.CANDLE.defaultBlockState());
    }

    private static void placeBed(ServerLevel lv, BlockPos pos, Direction facing, net.minecraft.world.level.block.Block bedBlock) {
        BlockState footState = bedBlock.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, BedPart.FOOT);
        BlockState headState = bedBlock.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, BedPart.HEAD);

        S(lv, pos, footState);
        S(lv, pos.relative(facing), headState);
    }

    private static void placeArmorStandWithItem(ServerLevel lv, BlockPos pos, ItemStack item) {
        ArmorStand stand = new ArmorStand(lv, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, item);
        stand.setShowArms(true);
        lv.addFreshEntity(stand);
    }

    private static void placeFullArmorStand(ServerLevel lv, BlockPos pos, net.minecraft.world.item.Item chest, net.minecraft.world.item.Item helmet) {
        ArmorStand stand = new ArmorStand(lv, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(chest));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(helmet));
        lv.addFreshEntity(stand);
    }
}

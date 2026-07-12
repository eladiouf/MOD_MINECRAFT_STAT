package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;

/**
 * Porte du Donjon : passage monumental percé dans le rempart sud, flanqué de deux tours et des
 * 4 statues des peuples fondateurs. Les battants (obsidienne) sont ouverts/refermés par
 * {@link CityGateOpener} selon la proximité des joueurs. Le téléporteur vers l'étage 1 est au
 * centre du passage.
 */
final class DungeonGateBuilder {

    /** Demi-largeur du passage (aligné sur CityPlan.inGateOpening : |x| <= 6). */
    static final int HALF_W = 6;
    /** Hauteur du passage. */
    static final int PASSAGE_H = 12;

    private DungeonGateBuilder() {}

    static void build(ServerLevel lv, CityBuildQueue q) {
        BlockPos g = CityPlan.gateCenter(); // Y=100, au seuil du rempart sud
        q.add(() -> buildFrame(lv, g));
        q.add(() -> buildStatues(lv, g));
        q.add(() -> {
            // Téléporteur étage 1 au centre du passage + battants fermés au départ.
            lv.setBlock(g.offset(0, 1, 0), DungeonBlocks.NEXT_FLOOR_TELEPORTER.get().defaultBlockState(), 3);
            CityGateOpener.setDoors(lv, true);
        });
    }

    /** Tours latérales + linteau runique au-dessus du passage. */
    private static void buildFrame(ServerLevel lv, BlockPos g) {
        BlockState tower = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState rune = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState light = Blocks.SOUL_LANTERN.defaultBlockState();

        for (int side : new int[]{-1, 1}) {
            int tx = side * (HALF_W + 3);
            for (int x = tx - 2; x <= tx + 2; x++)
                for (int z = -2; z <= 2; z++)
                    for (int y = 1; y <= PASSAGE_H + 14; y++)
                        lv.setBlock(g.offset(x, y, z), tower, 2);
            lv.setBlock(g.offset(tx, PASSAGE_H + 15, 0), light, 2);
        }
        // Linteau gravé de « runes » au-dessus du passage.
        for (int x = -HALF_W - 1; x <= HALF_W + 1; x++) {
            for (int y = PASSAGE_H + 1; y <= PASSAGE_H + 4; y++) {
                lv.setBlock(g.offset(x, y, 0), (x + y) % 2 == 0 ? rune : tower, 2);
            }
        }
    }

    /** 4 statues des peuples fondateurs, deux de chaque côté de l'approche nord. */
    private static void buildStatues(ServerLevel lv, BlockPos g) {
        record Founder(int dx, int dz, ItemStack head, ItemStack hand) {}
        Founder[] founders = {
                new Founder(-14, -10, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_SWORD)),          // Humain
                new Founder(14, -10, new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.BOW)),                // Elfe
                new Founder(-14, -18, new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_AXE)),  // Nain
                new Founder(14, -18, new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.TRIDENT)),           // Homme-bête
        };
        for (Founder f : founders) {
            BlockPos base = g.offset(f.dx(), 1, f.dz());
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    lv.setBlock(base.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            lv.setBlock(base.above(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
            ArmorStand stand = new ArmorStand(lv, base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5);
            stand.setItemSlot(EquipmentSlot.HEAD, f.head());
            stand.setItemSlot(EquipmentSlot.MAINHAND, f.hand());
            stand.setShowArms(true);
            stand.setInvulnerable(true);
            lv.addFreshEntity(stand);
        }
    }
}

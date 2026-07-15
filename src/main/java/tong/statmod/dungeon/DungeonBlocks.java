package tong.statmod.dungeon;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.StatMod;

import java.util.function.Supplier;

public final class DungeonBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, StatMod.MOD_ID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StatMod.MOD_ID);

    public static final Supplier<Block> DUNGEON_PORTAL = BLOCKS.register("dungeon_portal",
            () -> new DungeonPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Item> DUNGEON_PORTAL_ITEM = BLOCK_ITEMS.register(
            "dungeon_portal",
            () -> new BlockItem(DUNGEON_PORTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final Supplier<Block> RETURN_BEACON = BLOCKS.register("return_beacon",
            () -> new DungeonReturnBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 14)
                    .noOcclusion()));

    public static final Supplier<Item> RETURN_BEACON_ITEM = BLOCK_ITEMS.register("return_beacon",
            () -> new BlockItem(RETURN_BEACON.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final Supplier<Block> NEXT_FLOOR_TELEPORTER = BLOCKS.register("next_floor_teleporter",
            () -> new DungeonNextFloorTeleporterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 13)
                    .noOcclusion()));

    public static final Supplier<Item> NEXT_FLOOR_TELEPORTER_ITEM = BLOCK_ITEMS.register("next_floor_teleporter",
            () -> new BlockItem(NEXT_FLOOR_TELEPORTER.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final Supplier<Block> BOSS_ALTAR = BLOCKS.register("boss_altar",
            DungeonBossAltarBlock::new);

    public static final Supplier<Item> BOSS_ALTAR_ITEM = BLOCK_ITEMS.register("boss_altar",
            () -> new BlockItem(BOSS_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final Supplier<Block> MAGIC_TELEPORT_CIRCLE = BLOCKS.register("magic_teleport_circle",
            () -> new MagicTeleportCircleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instabreak()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)
                    .noOcclusion()));

    public static final Supplier<Item> MAGIC_TELEPORT_CIRCLE_ITEM = BLOCK_ITEMS.register("magic_teleport_circle",
            () -> new BlockItem(MAGIC_TELEPORT_CIRCLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }

    private DungeonBlocks() {}
}

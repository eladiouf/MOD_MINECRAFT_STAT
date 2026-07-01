package tong.statmod.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;
import java.util.List;
import java.util.function.Supplier;

/**
 * Interactive magical finishing stations layered on top of Overgeared forging.
 *
 * <p>Overgeared still produces the forged `rough_*` intermediates. `statmod:infusion_forge`
 * handles rune-essence infusion into runic bases, and `statmod:enchantment_anvil` handles
 * the heavier shard / gem finishing pass for personalized magical variants.
 */
public final class ForgingBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, STATMod.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    public static final Supplier<Block> INFUSION_FORGE = BLOCKS.register("infusion_forge",
            () -> new InfusionForgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Item> INFUSION_FORGE_ITEM = BLOCK_ITEMS.register(
            "infusion_forge",
            () -> new BlockItem(INFUSION_FORGE.get(), new Item.Properties()));

    /**
     * Advanced interactive finishing station for shard- and gem-driven enchantment recipes.
     */
    public static final Supplier<Block> ENCHANTMENT_ANVIL = BLOCKS.register("enchantment_anvil",
            () -> new EnchantmentAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(6.0F, 1400.0F)
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Item> ENCHANTMENT_ANVIL_ITEM = BLOCK_ITEMS.register(
            "enchantment_anvil",
            () -> new BlockItem(ENCHANTMENT_ANVIL.get(), new Item.Properties()));

    public static List<Supplier<Item>> blockItems() {
        return List.of(INFUSION_FORGE_ITEM, ENCHANTMENT_ANVIL_ITEM);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }

    private ForgingBlocks() {}
}

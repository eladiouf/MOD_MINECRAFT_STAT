package tong.statmod.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;
import tong.statmod.block.ForgingBlocks;

import java.util.function.Supplier;

public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, STATMod.MODID);

    public static final Supplier<BlockEntityType<InfusionForgeBlockEntity>> INFUSION_FORGE =
            BLOCK_ENTITIES.register("infusion_forge",
                    () -> BlockEntityType.Builder.of(
                            InfusionForgeBlockEntity::new,
                            ForgingBlocks.INFUSION_FORGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<EnchantmentAnvilBlockEntity>> ENCHANTMENT_ANVIL =
            BLOCK_ENTITIES.register("enchantment_anvil",
                    () -> BlockEntityType.Builder.of(
                            EnchantmentAnvilBlockEntity::new,
                            ForgingBlocks.ENCHANTMENT_ANVIL.get()
                    ).build(null));

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}

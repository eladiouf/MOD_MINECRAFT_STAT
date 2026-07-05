package tong.statmod.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tong.statmod.STATMod;

import static net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY;

import java.util.function.Supplier;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, STATMod.MODID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_CHEST_LOOT =
            GLM.register("add_chest_loot", () -> AddChestLootModifier.CODEC);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_OVERGEARED_LOOT =
            GLM.register("add_overgeared_loot", () -> AddOvergearedLootModifier.CODEC);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_DUNGEON_SHARD =
            GLM.register("add_dungeon_shard", () -> AddDungeonShardModifier.CODEC);

    // Système de points (2026-07-05) : les mobs de donjon ne dropent plus rien (DungeonDropGuard),
    // donc le remplaceur de loot de mob a été supprimé.

    public static void register(IEventBus modBus) {
        GLM.register(modBus);
    }
}

package tong.statmod.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tong.statmod.STATMod;

import java.util.function.Supplier;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, STATMod.MODID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_CHEST_LOOT =
            GLM.register("add_chest_loot", () -> AddChestLootModifier.CODEC);

    public static void register(IEventBus modBus) {
        GLM.register(modBus);
    }
}

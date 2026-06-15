package tong.statmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import tong.statmod.item.ModItems;

public class AddChestLootModifier extends LootModifier {
    public AddChestLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static final MapCodec<AddChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance).apply(instance, AddChestLootModifier::new)
    );

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(new ItemStack(ModItems.PERK_TOME.get()));
        generatedLoot.add(new ItemStack(ModItems.RESEPC_STONE.get(), 1 + context.getRandom().nextInt(2)));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

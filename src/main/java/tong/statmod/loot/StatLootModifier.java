package tong.statmod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import tong.statmod.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class StatLootModifier extends LootModifier {
    public static final Codec<StatLootModifier> CODEC = RecordCodecBuilder.create(inst ->
        LootModifier.codecStart(inst).apply(inst, StatLootModifier::new));

    public StatLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < 0.05f) {
            generatedLoot.add(new ItemStack(ModItems.STAT_SCROLL.get()));
        }
        if (context.getRandom().nextFloat() < 0.02f) {
            generatedLoot.add(new ItemStack(ModItems.PERK_TOME.get()));
        }
        if (context.getRandom().nextFloat() < 0.08f) {
            generatedLoot.add(new ItemStack(ModItems.MASTERY_CRYSTAL.get()));
        }
        if (context.getRandom().nextFloat() < 0.05f) {
            generatedLoot.add(new ItemStack(ModItems.RESPEC_STONE.get()));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}

package tong.statmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.overgeared.OvergearedForgingBonus;
import tong.statmod.integration.overgeared.OvergearedMaterialGate;
import tong.statmod.stats.StatType;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackDurabilityMixin {
    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int statmod$reduceOvergearedDurability(int adjustedAmount, int amount, ServerLevel level, LivingEntity entity, Consumer<Item> onBroken) {
        if (!(entity instanceof Player player)) {
            return adjustedAmount;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!OvergearedMaterialGate.isOvergearedItem(stack)) {
            return adjustedAmount;
        }

        int forging = RaceEffectApplier.getEffectiveLevel(player, StatType.FORGING.index);
        return OvergearedForgingBonus.adjustedDurabilityDamage(amount, forging);
    }
}

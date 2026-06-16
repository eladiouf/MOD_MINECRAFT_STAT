package tong.statmod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.stirdrem.overgeared.block.entity.AbstractSmithingAnvilBlockEntity;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.overgeared.OvergearedForgingBonus;
import tong.statmod.integration.overgeared.OvergearedRecipeGate;
import tong.statmod.stats.StatType;

import java.util.Optional;

@Mixin(AbstractSmithingAnvilBlockEntity.class)
public class OvergearedSmithingMixin {
    @Shadow protected Player player;

    @Inject(method = "getCurrentRecipeHolder", at = @At("RETURN"), cancellable = true)
    private void statmod$gateSpecialForgingRecipes(CallbackInfoReturnable<Optional<RecipeHolder<ForgingRecipe>>> cir) {
        Optional<RecipeHolder<ForgingRecipe>> recipeHolder = cir.getReturnValue();
        if (recipeHolder.isEmpty() || player == null) {
            return;
        }

        int forging = RaceEffectApplier.getEffectiveLevel(player, StatType.FORGING.index);
        var self = (AbstractSmithingAnvilBlockEntity) (Object) this;
        var result = recipeHolder.get().value().getResultItem(self.getLevel().registryAccess());
        if (!OvergearedRecipeGate.isForgingRecipeResult(result)) {
            return;
        }

        if (!OvergearedForgingBonus.canCraftSpecialOutput(result, forging)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}

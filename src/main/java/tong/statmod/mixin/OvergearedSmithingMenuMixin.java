package tong.statmod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import net.stirdrem.overgeared.screen.AbstractSmithingAnvilMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.overgeared.OvergearedForgingBonus;
import tong.statmod.integration.overgeared.OvergearedRecipeGate;
import tong.statmod.stats.StatType;

@Mixin(AbstractSmithingAnvilMenu.class)
public class OvergearedSmithingMenuMixin {
    @Shadow @Final private Player player;

    @Inject(method = "recipeMatches", at = @At("HEAD"), cancellable = true)
    private void statmod$hideLockedSpecialRecipes(RecipeHolder<ForgingRecipe> recipe,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (player == null || recipe == null) {
            return;
        }

        var result = recipe.value().getResultItem(player.registryAccess());
        if (!OvergearedRecipeGate.isForgingRecipeResult(result)) {
            return;
        }

        int forging = RaceEffectApplier.getEffectiveLevel(player, StatType.FORGING.index);
        if (!OvergearedForgingBonus.canCraftSpecialOutput(result, forging)) {
            cir.setReturnValue(false);
        }
    }
}

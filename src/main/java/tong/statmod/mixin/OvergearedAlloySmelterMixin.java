package tong.statmod.mixin;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.stirdrem.overgeared.block.entity.AlloySmelterBlockEntity;
import net.stirdrem.overgeared.block.entity.NetherAlloySmelterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.overgeared.OvergearedRecipeGate;

import java.util.Optional;

@Mixin({AlloySmelterBlockEntity.class, NetherAlloySmelterBlockEntity.class})
public class OvergearedAlloySmelterMixin {
    @Inject(method = "getCurrentRecipe", at = @At("RETURN"), cancellable = true)
    private void statmod$blockSpecialOutputsOutsideForging(CallbackInfoReturnable<Optional<RecipeHolder<?>>> cir) {
        Optional<RecipeHolder<?>> recipeHolder = cir.getReturnValue();
        if (recipeHolder.isEmpty()) {
            return;
        }

        var self = (BaseContainerBlockEntity) (Object) this;
        Recipe<?> recipe = recipeHolder.get().value();
        var result = recipe.getResultItem(self.getLevel().registryAccess());
        if (OvergearedRecipeGate.isForgingRecipeResult(result)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}

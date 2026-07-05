package tong.statmod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.overgeared.StatmodBlueprintCompatibility;

@Mixin(ForgingRecipe.class)
public abstract class OvergearedForgingRecipeMixin {
    @Shadow @Final private ItemStack result;
    @Shadow @Final private boolean requiresBlueprint;

    @Inject(method = "checkBlueprint", at = @At("HEAD"), cancellable = true)
    private void statmod$allowOptionalStatmodBlueprints(RecipeInput input,
                                                        CallbackInfoReturnable<Boolean> cir) {
        ItemStack blueprintStack = input.getItem(11);
        if (blueprintStack.isEmpty()) {
            return;
        }

        if (StatmodBlueprintCompatibility.allowsOptionalForgingBypass(
                BuiltInRegistries.ITEM.getKey(blueprintStack.getItem()),
                BuiltInRegistries.ITEM.getKey(result.getItem()),
                requiresBlueprint)) {
            cir.setReturnValue(true);
        }
    }
}

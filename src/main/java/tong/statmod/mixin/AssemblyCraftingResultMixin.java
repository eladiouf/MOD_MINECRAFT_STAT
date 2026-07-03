package tong.statmod.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.overgeared.AssemblyForgingPolicy;
import tong.statmod.integration.overgeared.ForgingRaceBonus;
import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@Mixin(ResultSlot.class)
public class AssemblyCraftingResultMixin {
    @Shadow @Final private Player player;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void statmod$gateAssemblyPickup(Player clickingPlayer, CallbackInfoReturnable<Boolean> cir) {
        if (clickingPlayer == null || clickingPlayer.level().isClientSide) {
            return;
        }

        ItemStack result = ((Slot) (Object) this).getItem();
        if (result.isEmpty()) {
            return;
        }

        PlayerStatData data = clickingPlayer.getData(ModAttachments.STATS);
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        String missing = AssemblyForgingPolicy.missingRequirements(resultId, data);
        if (missing == null) {
            return;
        }

        clickingPlayer.displayClientMessage(Component.literal(
                "§c[Forge] Niveau insuffisant pour assembler " + resultId.getPath()
                        + " — manque " + missing), false);
        cir.setReturnValue(false);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void statmod$boostAssemblyDurability(Player clickingPlayer, ItemStack crafted, CallbackInfo ci) {
        if (clickingPlayer == null || clickingPlayer.level().isClientSide || crafted == null || crafted.isEmpty()) {
            return;
        }

        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        PlayerStatData data = this.player.getData(ModAttachments.STATS);
        MaterialGate gate = AssemblyForgingPolicy.effectiveGate(resultId, data);
        if (gate == null || !gate.satisfies(data)) {
            return;
        }

        float multiplier = ForgingRaceBonus.durabilityMultiplier(data);
        if (multiplier <= 1.0f) {
            return;
        }

        Integer baseMax = crafted.get(DataComponents.MAX_DAMAGE);
        if (baseMax == null || baseMax <= 0) {
            return;
        }

        crafted.set(DataComponents.MAX_DAMAGE, Math.round(baseMax * multiplier));
    }
}

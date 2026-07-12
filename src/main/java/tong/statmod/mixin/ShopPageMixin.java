package tong.statmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.sdm.ClientShopTabForcer;

@Mixin(targets = "net.sixk.sdmshop.shop.ShopPage")
public class ShopPageMixin {

    @Inject(method = "onInit", at = @At("HEAD"), remap = false)
    private void statmod$selectNPCSubTab(CallbackInfoReturnable<Boolean> cir) {
        String target = ClientShopTabForcer.targetTab;
        if (target != null && !target.isEmpty()) {
            net.sixk.sdmshop.shop.Tab.TabPanel.selectedTab = target;
        }
        ClientShopTabForcer.targetTab = null;
    }
}

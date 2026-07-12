package tong.statmod.mixin;

import net.sixk.sdmshop.shop.Tab.Tab;
import net.sixk.sdmshop.shop.Tab.TabPanel;
import net.sixk.sdmshop.shop.Tab.TabRender;
import net.sixk.sdmshop.shop.Tab.TovarTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.integration.sdm.ClientShopTabForcer;

/**
 * Verrouillage du shop SDM sur l'onglet d'un PNJ marchand spécialisé : quand
 * {@link ClientShopTabForcer#lockedTab} est posé (clic sur un PNJ tagué
 * {@code sdm_tab:<Onglet>}), le panneau d'onglets ne construit QUE le rayon de ce
 * marchand — les autres onglets n'existent pas à l'écran. Réplique la boucle
 * d'origine de {@code TabPanel.addWidgets} en filtrant sur le nom d'onglet.
 */
@Mixin(targets = "net.sixk.sdmshop.shop.Tab.TabPanel", remap = false)
public class ShopTabPanelMixin {

    @Inject(method = "addWidgets", at = @At("HEAD"), cancellable = true, remap = false)
    private void statmod$lockToMerchantTab(CallbackInfo ci) {
        String locked = ClientShopTabForcer.lockedTab;
        if (locked == null || locked.isEmpty()) return;

        TabPanel self = (TabPanel) (Object) this;
        int index = 0;
        for (Tab tab : TovarTab.CLIENT.tabList) {
            if (tab.name == null || !locked.trim().equalsIgnoreCase(tab.name.trim())) continue;
            TabRender render = new TabRender(self, tab);
            self.add(render);
            self.tabRenderList.add(render);
            render.setPos(0, 20 * index);
            render.setSize(self.width - 1, 20);
            index++;
        }
        // Onglet introuvable dans le catalogue (renommé/supprimé) → fallback shop complet
        if (index == 0) {
            ClientShopTabForcer.lockedTab = null;
            return;
        }
        ci.cancel();
    }
}

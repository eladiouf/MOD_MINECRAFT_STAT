package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class IronSpellTensuraPotionBridge {
    private IronSpellTensuraPotionBridge() {}

    public static void restoreManaFromTensuraPotion(ServerPlayer player, float restoreFraction) {
        if (player == null || restoreFraction <= 0.0f || !IronSpellsCompat.isLoaded()) {
            return;
        }

        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0.0d) {
            return;
        }

        float restoreAmount = Mth.clamp((float) maxMana * restoreFraction, 0.0f, (float) maxMana);
        IronSpellManaSyncBridge.addMana(player, restoreAmount);
    }
}

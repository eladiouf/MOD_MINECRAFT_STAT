package tong.statmod.integration.ironspells;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;

/**
 * Server-side façade that opens the virtual inscription menu for a player.
 *
 * <p>Pre-conditions:
 * <ul>
 *     <li>Iron's Spellbooks is loaded (otherwise the method is a safe no-op).</li>
 *     <li>The caller is on the server thread.</li>
 * </ul>
 *
 * <p>The filtering to learned Iron's spells is provided by the existing inscription
 * mixins which apply to every instance of {@code InscriptionTableMenu}; no extra
 * post-open sync packet is needed for that path.
 */
public final class IronInscriptionOpenerService {
    private IronInscriptionOpenerService() {}

    /**
     * Opens the virtual inscription menu for the given server player.
     * Returns {@code true} on successful enqueueing of the menu open.
     */
    public static boolean openVirtual(ServerPlayer player) {
        if (player == null) return false;
        if (!IronSpellsCompat.isLoaded()) {
            player.displayClientMessage(Component.translatable("statmod.magic.irons_not_loaded"), true);
            return false;
        }
        try {
            player.openMenu(new VirtualInscriptionMenuProvider());
            return true;
        } catch (Throwable t) {
            STATMod.LOGGER.warn("Failed to open virtual inscription menu for {}: {}",
                    player.getName().getString(), t.getMessage());
            return false;
        }
    }
}

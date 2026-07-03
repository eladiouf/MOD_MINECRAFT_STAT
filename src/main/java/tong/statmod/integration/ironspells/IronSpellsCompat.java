package tong.statmod.integration.ironspells;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import tong.statmod.STATMod;

public final class IronSpellsCompat {
    private static final String IRONS_MODID = "irons_spellbooks";
    private static boolean loaded;

    private IronSpellsCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded(IRONS_MODID);
        if (!loaded) {
            STATMod.LOGGER.info("Iron's Spellbooks not detected, skipping IronSpellsCompat");
            return;
        }
        try {
            NeoForge.EVENT_BUS.register(IronSpellEventBridge.class);
            NeoForge.EVENT_BUS.register(IronSpellAttributeBridge.class);
            STATMod.LOGGER.info("Iron's Spellbooks integration loaded");
        } catch (Throwable t) {
            loaded = false;
            STATMod.LOGGER.warn("Iron's Spellbooks bridge failed: {}", t.getMessage());
        }
    }

    public static boolean isLoaded() { return loaded; }
}

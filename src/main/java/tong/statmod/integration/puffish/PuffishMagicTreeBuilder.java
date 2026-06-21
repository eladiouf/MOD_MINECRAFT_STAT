package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class PuffishMagicTreeBuilder {
    private PuffishMagicTreeBuilder() {}

    public static void applyMirror(ServerPlayer player) {
        if (!ModList.get().isLoaded("puffish_skills") || !PuffishSkillsCompat.isLoaded()) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        try {
            PuffishMagicSyncService.sync(data, new PuffishReflectionGateway(player));
        } catch (Throwable t) {
            STATMod.LOGGER.debug("Puffish magic mirror sync failed for {}: {}",
                    player.getName().getString(), t.getMessage());
        }
    }
}

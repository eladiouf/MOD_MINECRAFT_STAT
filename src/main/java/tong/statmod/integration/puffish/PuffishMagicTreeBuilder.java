package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class PuffishMagicTreeBuilder {
    private PuffishMagicTreeBuilder() {}

    public static void applyMirror(ServerPlayer player) {
        if (!ModList.get().isLoaded("puffish_skills") || !PuffishSkillsCompat.isLoaded()) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        PuffishReflectionGateway gateway = new PuffishReflectionGateway(player);
        for (MagicNode node : MagicTreeCatalog.all()) {
            String category = PuffishMagicCategoryIds.categoryFor(node.id());
            String skill = PuffishMagicCategoryIds.toSkillId(node.id());
            if (category == null || skill == null) continue;
            boolean shouldBeUnlocked = data.hasMagicNode(node.id());
            try {
                if (shouldBeUnlocked) gateway.unlock(category, skill);
                else gateway.lock(category, skill);
            } catch (Throwable t) {
                STATMod.LOGGER.debug("Puffish mirror error on {}: {}", node.id(), t.getMessage());
            }
        }
    }
}

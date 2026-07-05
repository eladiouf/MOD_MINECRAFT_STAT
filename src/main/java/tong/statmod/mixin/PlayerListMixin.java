package tong.statmod.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.STATMod;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.network.BatchSyncPayload;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void statmod$onPlaceNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        PlayerStatData data = player.getData(ModAttachments.STATS);

        BatchSyncPayload payload = new BatchSyncPayload(
                data.getLevels(), data.getXp(), data.getPerkPoints(), data.getUnlockedPerks(), data.getSoulLevel());
        PacketDistributor.sendToPlayer(player, payload);
        SyncHelper.syncStats(player);
        SyncHelper.syncStamina(player);
        SyncHelper.syncMagic(player);
        PuffishSkillsCompat.sync(player, data);
        STATMod.LOGGER.info("Synced stat, stamina and magic data to {}", player.getName().getString());
    }
}

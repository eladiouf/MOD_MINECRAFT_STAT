package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.config.Config;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.PerkPointAllocator;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Au passage d'un palier (tier) du global level, crédite des perk points à toutes les familles.
 *
 * <p>Le dernier tier crédité est <b>persisté</b> sur {@link PlayerStatData#getLastPerkGrantTier()},
 * pas dans une HashMap statique, pour éviter le bug pré-Mission-L : au restart serveur la map
 * se vidait et un joueur déjà au tier 5 se faisait recréditer 5 × N perk points au tick
 * suivant (duplicate grant).
 */
@EventBusSubscriber(modid = STATMod.MODID)
public class LevelUpHandler {

    public static int grantPendingPerkTiers(PlayerStatData data) {
        if (data == null) {
            return 0;
        }

        int globalLevel = data.getGlobalLevel();
        int tier = globalLevel / 10;
        int already = data.getLastPerkGrantTier();
        if (tier <= already) {
            return 0;
        }

        int granted = (tier - already) * Config.getBasePerkPointsPerLevel();
        PerkPointAllocator.grantPointsToAllFamilies(data, granted);
        data.setLastPerkGrantTier(tier);
        return granted;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int granted = grantPendingPerkTiers(data);
        if (granted > 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                SyncHelper.syncPerks(serverPlayer);
            }
            int globalLevel = data.getGlobalLevel();
            STATMod.LOGGER.info("Granted {} perk point(s) to each perk family for {} (global level {})",
                    granted, player.getName().getString(), globalLevel);
        }
    }
}

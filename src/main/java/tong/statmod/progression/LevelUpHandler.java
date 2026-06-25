package tong.statmod.progression;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.config.Config;
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int globalLevel = data.getGlobalLevel();
        int tier = globalLevel / 10;
        int already = data.getLastPerkGrantTier();

        if (tier > already) {
            int granted = (tier - already) * Config.getBasePerkPointsPerLevel();
            PerkPointAllocator.grantPointsToAllFamilies(data, granted);
            data.setLastPerkGrantTier(tier);
            STATMod.LOGGER.info("Granted {} perk point(s) to each perk family for {} (global level {})",
                    granted, player.getName().getString(), globalLevel);
        }
    }
}

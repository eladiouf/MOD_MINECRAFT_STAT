package tong.statmod.progression;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.perks.PerkPointAllocator;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = STATMod.MODID)
public class LevelUpHandler {
    // Last milestone tier granted per player (e.g. 1 = 10 global lvls awarded, 2 = 20, etc.)
    private static final Map<UUID, Integer> lastGrantedTier = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int globalLevel = data.getGlobalLevel();
        int tier = globalLevel / 10;
        UUID uuid = player.getUUID();
        int already = lastGrantedTier.getOrDefault(uuid, 0);

        if (tier > already) {
            int granted = tier - already;
            PerkPointAllocator.grantPointsToAllFamilies(data, granted);
            lastGrantedTier.put(uuid, tier);
            STATMod.LOGGER.info("Granted {} perk point(s) to each perk family for {} (global level {})",
                    granted, player.getName().getString(), globalLevel);
        }
    }
}

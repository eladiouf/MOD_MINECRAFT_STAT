package tong.statmod.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mission M6 — Gestion du respawn dans le Trial Dungeon.
 *
 * <p>Quand un joueur meurt dans le donjon, il est automatiquement renvoyé à l'étage 1
 * plutôt qu'au spawn overworld. Le set {@code pendingRespawns} sert de pont entre
 * l'évènement de mort et le respawn.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonRespawnHandler {

    private static final Set<UUID> PENDING_RESPAWNS = new HashSet<>();

    private DungeonRespawnHandler() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        PENDING_RESPAWNS.add(player.getUUID());
        STATMod.LOGGER.info("[TrialDungeon] {} est mort dans le donjon — respawn étage 1 programmé",
                player.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!PENDING_RESPAWNS.remove(player.getUUID())) return;

        int floor = 1;
        DungeonTeleportHandler.enterFloor(player, floor);
        STATMod.LOGGER.info("[TrialDungeon] {} respawn au donjon étage {}",
                player.getGameProfile().getName(), floor);
    }
}

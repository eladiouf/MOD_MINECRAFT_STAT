package tong.statmod.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.dungeon.DungeonMerchant;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.OpenMagicBankPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** PNJ qui convertit les espèces FDP et le solde numérique SDM. */
public final class MagicBanker {
    public static final String TAG = "statmod_magic_banker";
    private static final long SESSION_TICKS = 20L * 30L;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MagicBanker() {}

    public static Villager spawn(ServerLevel level, BlockPos pos) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return null;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CLERIC, 5));
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.setSilent(true);
        villager.setCustomName(net.minecraft.network.chat.Component.translatable("banker.magic.name"));
        villager.setCustomNameVisible(true);
        villager.getPersistentData().putBoolean(TAG, true);
        villager.getPersistentData().putBoolean(DungeonMerchant.MERCHANT_TAG, true);
        DungeonSpawnGuard.spawnAuthorized(() -> { level.addFreshEntity(villager); return villager; });
        return villager;
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !event.getTarget().getPersistentData().getBoolean(TAG)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        SESSIONS.put(player.getUUID(), new Session(player.level().dimension().location().toString(),
                event.getTarget().getUUID(), player.level().getGameTime() + SESSION_TICKS));
        sendSnapshot(player);
    }

    public static boolean canUse(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return false;
        boolean sameDimension = player.level().dimension().location().toString().equals(session.dimension);
        Entity entity = sameDimension ? level.getEntity(session.banker) : null;
        boolean allowed = level.getGameTime() <= session.expiresAt && entity != null
                && entity.getPersistentData().getBoolean(TAG) && player.distanceToSqr(entity) <= 64.0;
        if (!allowed) SESSIONS.remove(player.getUUID());
        return allowed;
    }

    public static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenMagicBankPayload(
                SDMEconomyBridge.getCoins(player), MagicBankService.physicalTotal(player)));
    }

    private record Session(String dimension, UUID banker, long expiresAt) {}
}

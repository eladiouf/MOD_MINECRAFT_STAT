package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.OpenExchangePayload;
import tong.statmod.storage.ModAttachments;

/**
 * Mission M6 — Villageois changeur (points → coins) des étages trésor (2026-07-05).
 *
 * <p>Bloqué (NoAI, invulnérable, persistant), tagué {@link #TAG}, sans trade vanilla. Clic-droit →
 * ouvre l'écran d'échange. La monnaie SDM est créée au démarrage serveur.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonExchanger {

    /** Marqueur NBT du changeur (exempté du nettoyage des mobs, comme les marchands). */
    public static final String TAG = "statmod_dungeon_exchanger";

    private DungeonExchanger() {}

    /** Pose un villageois changeur à {@code pos}. */
    public static void spawn(ServerLevel lv, BlockPos pos) {
        Villager v = EntityType.VILLAGER.create(lv);
        if (v == null) return;
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        v.setYBodyRot(180f);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, 5));
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setSilent(true);
        v.setCustomName(net.minecraft.network.chat.Component.translatable("shop.exchanger.name"));
        v.setCustomNameVisible(true);
        v.getPersistentData().putBoolean(TAG, true);
        v.getPersistentData().putBoolean(DungeonMerchant.MERCHANT_TAG, true); // exempt du nettoyage
        DungeonSpawnGuard.spawnAuthorized(() -> { lv.addFreshEntity(v); return v; });
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SDMEconomyBridge.ensureCurrency(event.getServer());
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!event.getTarget().getPersistentData().getBoolean(TAG)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        int points = sp.getData(ModAttachments.STATS).getDungeonPoints();
        long coins = SDMEconomyBridge.getCoins(sp);
        PacketDistributor.sendToPlayer(sp, new OpenExchangePayload(points, coins,
                (float) tong.statmod.config.Config.getPointToCoinRate()));
    }
}

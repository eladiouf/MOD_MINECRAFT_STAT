package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.network.PacketDistributor;
import tong.statmod.StatMod;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mission M6 — Villageois changeur (points → coins) des étages trésor (2026-07-05).
 *
 * <p>Bloqué (NoAI, invulnérable, persistant), tagué {@link #TAG}, sans trade vanilla. Clic-droit →
 * ouvre l'écran d'échange. La monnaie SDM est créée au démarrage serveur.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonExchanger {

    /** Marqueur NBT du changeur (exempté du nettoyage des mobs, comme les marchands). */
    public static final String TAG = "statmod_dungeon_exchanger";

    private static final long SESSION_DURATION_TICKS = 20L * 30L;
    private static final Map<UUID, ExchangeSession> SESSIONS = new ConcurrentHashMap<>();

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
        // La devise FDP_cfa est déjà enregistrée via SDMShopDatabaseInitializer.onServerStarting
        // (CustomCurrencies.putIfAbsent). Appeler aussi EconomyAPI.createCurrencyOnServer ici
        // crée une entrée dupliquée visible dans le wallet SDM → supprimé.
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!event.getTarget().getPersistentData().getBoolean(TAG)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        authorize(sp, event.getTarget());
        int points = StatCapabilities.get(sp).getDungeonPoints();
        long coins;
        if (SDMEconomyBridge.available()) {
            coins = SDMEconomyBridge.getCoins(sp);
        } else {
            coins = sp.getInventory().items.stream()
                    .filter(stack -> stack.is(net.minecraft.world.item.Items.EMERALD))
                    .mapToLong(net.minecraft.world.item.ItemStack::getCount)
                    .sum();
        }
        tong.statmod.network.StatNetwork.sendOpenExchange(sp, points, coins,
                (float) tong.statmod.config.Config.getPointToCoinRate());
    }

    private static void authorize(ServerPlayer player, Entity exchanger) {
        SESSIONS.put(player.getUUID(), new ExchangeSession(
                player.level().dimension().location().toString(), exchanger.getUUID(),
                player.level().getGameTime() + SESSION_DURATION_TICKS));
    }

    /** Vérifie qu'une conversion fait suite à un clic récent sur un vrai changeur resté proche. */
    public static boolean canConvert(ServerPlayer player) {
        ExchangeSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return false;

        boolean sameDimension = player.level().dimension().location().toString().equals(session.dimension());
        Entity exchanger = sameDimension ? level.getEntity(session.exchangerId()) : null;
        boolean tagged = exchanger != null && exchanger.getPersistentData().getBoolean(TAG);
        double distanceSquared = exchanger == null ? Double.POSITIVE_INFINITY : player.distanceToSqr(exchanger);
        boolean allowed = DungeonExchangeAccess.isAllowed(level.getGameTime(), session.expiresAtTick(),
                sameDimension, tagged, distanceSquared);
        if (!allowed) SESSIONS.remove(player.getUUID());
        return allowed;
    }

    private record ExchangeSession(String dimension, UUID exchangerId, long expiresAtTick) {}
}

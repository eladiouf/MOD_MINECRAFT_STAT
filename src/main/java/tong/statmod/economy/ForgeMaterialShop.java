package tong.statmod.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonMerchant;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.OpenForgeShopPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeMaterialShop {
    public static final String TAG = "statmod_forge_merchant";
    private static final long SESSION_TICKS = 20L * 30L;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private ForgeMaterialShop() {}

    public static Villager spawn(ServerLevel level, BlockPos pos) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return null;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.WEAPONSMITH, 5));
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.setSilent(true);
        villager.setCustomName(net.minecraft.network.chat.Component.translatable("forge_shop.npc"));
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
        Entity entity = sameDimension ? level.getEntity(session.merchant) : null;
        boolean allowed = level.getGameTime() <= session.expiresAt && entity != null
                && entity.getPersistentData().getBoolean(TAG) && player.distanceToSqr(entity) <= 64.0;
        if (!allowed) SESSIONS.remove(player.getUUID());
        return allowed;
    }

    public static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenForgeShopPayload(SDMEconomyBridge.getCoins(player)));
    }

    public static boolean buyItem(ServerPlayer player, String itemId) {
        var optPrice = ForgeShopPrices.priceOf(itemId);
        if (optPrice.isEmpty()) return false;
        long price = optPrice.getAsLong();

        boolean deducted = SDMEconomyBridge.removeCoins(player, price);
        if (!deducted) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("forge_shop.insufficient_funds"), true);
            return false;
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, itemId);
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            STATMod.LOGGER.warn("ForgeMaterialShop: unknown item {}", itemId);
            SDMEconomyBridge.addCoins(player, price);
            return false;
        }

        ItemStack stack = new ItemStack(item, 1);
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("forge_shop.purchased",
                        stack.getHoverName(), price), true);
        sendSnapshot(player);
        return true;
    }

    private record Session(String dimension, UUID merchant, long expiresAt) {}
}

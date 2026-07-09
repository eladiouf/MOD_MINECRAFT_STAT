package tong.statmod.integration.parcool;

import com.alrex.parcool.api.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParcoolAttributeHandler {
    private static final ResourceLocation MAX_STAMINA_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "parcool_max_stamina");
    private static final ResourceLocation STAMINA_RECOVERY_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "parcool_stamina_recovery");
    private static final ResourceLocation JUMP_STRENGTH_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "parcool_jump_strength");

    private static final Map<UUID, Integer> lastMaxStamina = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastStaminaRecovery = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastAgility = new ConcurrentHashMap<>();

    private ParcoolAttributeHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        UUID uuid = player.getUUID();
        applyModifier(player, Attributes.MAX_STAMINA, MAX_STAMINA_ID,
                RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index), lastMaxStamina, uuid, 0.01);
        applyModifier(player, Attributes.STAMINA_RECOVERY, STAMINA_RECOVERY_ID,
                RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index), lastStaminaRecovery, uuid, 0.0);
        applyModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, JUMP_STRENGTH_ID,
                RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index), lastAgility, uuid, 0.002);

        if (player instanceof ServerPlayer serverPlayer) {
            // Keep the HUD/state in sync when stamina scaling changes.
            tong.statmod.network.SyncHelper.syncStats(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearCaches(event.getOriginal().getUUID());
        clearCaches(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    private static void applyModifier(Player player, net.minecraft.core.Holder<Attribute> attribute,
                                      ResourceLocation id, int level,
                                      Map<UUID, Integer> cache, UUID uuid, double perLevel) {
        Integer previous = cache.get(uuid);
        if (previous != null && previous == level) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);

        double amount = level * perLevel;
        if (level > 0 && Math.abs(amount) > 1.0e-6d) {
            instance.addPermanentModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        if (level == 0 || Math.abs(amount) <= 1.0e-6d) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, level);
        }
    }

    private static void clearCaches(UUID uuid) {
        lastMaxStamina.remove(uuid);
        lastStaminaRecovery.remove(uuid);
        lastAgility.remove(uuid);
    }
}

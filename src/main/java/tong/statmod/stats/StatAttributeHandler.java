package tong.statmod.stats;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = STATMod.MODID)
public class StatAttributeHandler {
    private static final ResourceLocation RAPIDITE_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "rapidite_attack_speed");
    private static final ResourceLocation AGILITY_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "agility_movement_speed");

    private static final Map<UUID, Integer> lastRapidite = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastAgility = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        UUID uuid = player.getUUID();

        applyModifier(player, Attributes.ATTACK_SPEED, RAPIDITE_ID,
                RaceEffectApplier.getEffectiveLevel(player, StatType.RAPIDITE.index), lastRapidite, uuid, 0.002);

        applyModifier(player, Attributes.MOVEMENT_SPEED, AGILITY_ID,
                RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index), lastAgility, uuid, 0.001);
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute,
                                      ResourceLocation id, int level,
                                      Map<UUID, Integer> cache, UUID uuid, double perLevel) {
        Integer previous = cache.get(uuid);
        if (previous != null && previous == level) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);

        if (level > 0) {
            instance.addPermanentModifier(
                    new AttributeModifier(id, level * perLevel, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        if (level == 0) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, level);
        }
    }
}

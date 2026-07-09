package tong.statmod.integration.tensura;

import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class SummonScalingHandler {
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "tensura_summon_health");
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "tensura_summon_damage");

    private SummonScalingHandler() {}

    public static float healthMultiplier(int arcanePower) {
        return 1.0f + Math.max(0, arcanePower) * 0.01f;
    }

    public static float damageMultiplier(int willpower) {
        return 1.0f + Math.max(0, willpower) * 0.01f;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }

        int arcanePower = RaceEffectApplier.getEffectiveLevel(player, StatType.ARCANE_POWER.index);
        int willpower = RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index);
        player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0D),
                entity -> entity != player && ExistenceStorage.isSummon(entity))
                .forEach(entity -> applyScale(player, entity, arcanePower, willpower));
    }

    private static void applyScale(ServerPlayer summoner, LivingEntity summon, int arcanePower, int willpower) {
        var existence = PlayerDataTensuraHook.getExistence(summon);
        if (existence == null || existence.getSummoner() == null || !summoner.getUUID().equals(existence.getSummoner())) {
            return;
        }

        applyModifier(summon, Attributes.MAX_HEALTH, HEALTH_ID, healthMultiplier(arcanePower) - 1.0f);
        applyModifier(summon, Attributes.ATTACK_DAMAGE, DAMAGE_ID, damageMultiplier(willpower) - 1.0f);
        summon.setHealth(Math.min(summon.getHealth(), summon.getMaxHealth()));
    }

    private static void applyModifier(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                      ResourceLocation id, double amount) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount > 0.0d) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}

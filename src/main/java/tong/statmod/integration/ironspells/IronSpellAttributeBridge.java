package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class IronSpellAttributeBridge {
    private static final ResourceLocation MAX_MANA_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_max_mana_bridge");
    private static final ResourceLocation MANA_REGEN_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_mana_regen_bridge");
    private static final ResourceLocation BASE_MANA_REGEN_SUPPRESSION_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_base_mana_regen_suppression");
    private static final ResourceLocation SPELL_POWER_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_spell_power_bridge");
    private static final ResourceLocation SPELL_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_spell_resist_bridge");
    private static final ResourceLocation CAST_TIME_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_cast_time_bridge");
    private static final ResourceLocation COOLDOWN_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_cooldown_bridge");

    private static final Map<UUID, Snapshot> LAST_APPLIED = new ConcurrentHashMap<>();

    private IronSpellAttributeBridge() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;
        if (player instanceof ServerPlayer serverPlayer) {
            apply(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_APPLIED.remove(event.getEntity().getUUID());
    }

    static void apply(ServerPlayer player) {
        suppressBaseManaRegen(player);
        Snapshot next = Snapshot.capture(player);
        UUID uuid = player.getUUID();
        Snapshot previous = LAST_APPLIED.get(uuid);
        if (next.equals(previous)) {
            return;
        }

        float previousMana = MagicData.getPlayerMagicData(player).getMana();
        double previousMaxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);

        applyModifier(player, AttributeRegistry.MAX_MANA, MAX_MANA_ID,
                IronSpellStatScaler.maxManaBonus(next.manaPoolLevel(), next.manaPoolCoreUnlocked())
                        + next.manaPoolAdvancedManaBonus());
        applyModifier(player, AttributeRegistry.MANA_REGEN, MANA_REGEN_ID,
                IronSpellStatScaler.manaRegenBonus(next.manaPoolLevel(), next.eruditionLevel()));
        applyModifier(player, AttributeRegistry.SPELL_POWER, SPELL_POWER_ID,
                IronSpellStatScaler.spellPowerBonus(next.arcanePowerLevel(), next.arcaneCoreUnlocked())
                        + next.arcaneAdvancedPowerBonus());
        applyModifier(player, AttributeRegistry.SPELL_RESIST, SPELL_RESIST_ID,
                IronSpellStatScaler.spellResistBonus(next.magicResistanceLevel(), next.magicResistanceCoreUnlocked())
                        + next.magicResistanceAdvancedBonus());
        applyModifier(player, AttributeRegistry.CAST_TIME_REDUCTION, CAST_TIME_ID,
                IronSpellStatScaler.castTimeReductionBonus(next.castingSpeedLevel(), next.castingSpeedCoreUnlocked())
                        + next.castingSpeedAdvancedBonus());
        applyModifier(player, AttributeRegistry.COOLDOWN_REDUCTION, COOLDOWN_ID,
                IronSpellStatScaler.cooldownReductionBonus(next.eruditionLevel(), next.eruditionCoreUnlocked())
                        + next.eruditionAdvancedCooldownBonus());

        double newMaxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        syncCurrentMana(player, previousMana, previousMaxMana, newMaxMana);
        LAST_APPLIED.put(uuid, next);
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);
        if (Math.abs(amount) < 1.0e-6d) {
            return;
        }

        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void suppressBaseManaRegen(Player player) {
        AttributeInstance instance = player.getAttribute(AttributeRegistry.MANA_REGEN);
        if (instance == null) return;

        instance.removeModifier(BASE_MANA_REGEN_SUPPRESSION_ID);
        double baseManaRegen = instance.getBaseValue();
        if (baseManaRegen <= 1.0e-6d) {
            return;
        }

        instance.addPermanentModifier(new AttributeModifier(
                BASE_MANA_REGEN_SUPPRESSION_ID, -baseManaRegen, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void syncCurrentMana(ServerPlayer player, float previousMana, double previousMaxMana, double newMaxMana) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        float desiredMana;
        if (previousMaxMana > 0.0d) {
            float ratio = previousMana / (float) previousMaxMana;
            desiredMana = Mth.clamp((float) newMaxMana * ratio, 0.0f, (float) newMaxMana);
        } else {
            desiredMana = Mth.clamp(previousMana, 0.0f, (float) newMaxMana);
        }

        if (Math.abs(desiredMana - magicData.getMana()) < 0.01f) {
            return;
        }

        magicData.setMana(desiredMana);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }

    record Snapshot(
            int manaPoolLevel,
            int eruditionLevel,
            int arcanePowerLevel,
            int magicResistanceLevel,
            int castingSpeedLevel,
            boolean manaPoolCoreUnlocked,
            boolean eruditionCoreUnlocked,
            boolean arcaneCoreUnlocked,
            boolean magicResistanceCoreUnlocked,
            boolean castingSpeedCoreUnlocked,
            double manaPoolAdvancedManaBonus,
            double arcaneAdvancedPowerBonus,
            double magicResistanceAdvancedBonus,
            double castingSpeedAdvancedBonus,
            double eruditionAdvancedCooldownBonus
    ) {
        static Snapshot capture(Player player) {
            PlayerStatData data = player.getData(ModAttachments.STATS);
            return new Snapshot(
                    RaceEffectApplier.getEffectiveLevel(player, StatType.MANA_POOL.index),
                    RaceEffectApplier.getEffectiveLevel(player, StatType.ERUDITION.index),
                    RaceEffectApplier.getEffectiveLevel(player, StatType.ARCANE_POWER.index),
                    RaceEffectApplier.getEffectiveLevel(player, StatType.MAGIC_RESISTANCE.index),
                    RaceEffectApplier.getEffectiveLevel(player, StatType.CASTING_SPEED.index),
                    data.isPerkUnlocked(Perk.MANA_POOL_CORE.id),
                    data.isPerkUnlocked(Perk.ERUDITION_CORE.id),
                    data.isPerkUnlocked(Perk.ARCANE_CORE.id),
                    data.isPerkUnlocked(Perk.MAGIC_RESIST_CORE.id),
                    data.isPerkUnlocked(Perk.CASTING_SPEED_CORE.id),
                    IronSpellAdvancedPerkScaling.advancedManaBonus(
                            data.isPerkUnlocked(Perk.MANA_POOL_ACTIVE.id),
                            data.isPerkUnlocked(Perk.MANA_POOL_SYNERGY.id),
                            data.isPerkUnlocked(Perk.MANA_POOL_SITUATIONAL.id),
                            data.isPerkUnlocked(Perk.MANA_POOL_MASTERY.id),
                            data.isPerkUnlocked(Perk.MANA_POOL_TRANSCENDENCE.id)),
                    IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                            data.isPerkUnlocked(Perk.ARCANE_ACTIVE.id),
                            data.isPerkUnlocked(Perk.ARCANE_SYNERGY.id),
                            data.isPerkUnlocked(Perk.ARCANE_SITUATIONAL.id),
                            data.isPerkUnlocked(Perk.ARCANE_MASTERY.id),
                            data.isPerkUnlocked(Perk.ARCANE_TRANSCENDENCE.id)),
                    IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                            data.isPerkUnlocked(Perk.MAGIC_RESIST_ACTIVE.id),
                            data.isPerkUnlocked(Perk.MAGIC_RESIST_SYNERGY.id),
                            data.isPerkUnlocked(Perk.MAGIC_RESIST_SITUATIONAL.id),
                            data.isPerkUnlocked(Perk.MAGIC_RESIST_MASTERY.id),
                            data.isPerkUnlocked(Perk.MAGIC_RESIST_TRANSCENDENCE.id)),
                    IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                            data.isPerkUnlocked(Perk.CASTING_SPEED_ACTIVE.id),
                            data.isPerkUnlocked(Perk.CASTING_SPEED_SYNERGY.id),
                            data.isPerkUnlocked(Perk.CASTING_SPEED_SITUATIONAL.id),
                            data.isPerkUnlocked(Perk.CASTING_SPEED_MASTERY.id),
                            data.isPerkUnlocked(Perk.CASTING_SPEED_TRANSCENDENCE.id)),
                    IronSpellAdvancedPerkScaling.advancedPercentAttributeBonus(
                            data.isPerkUnlocked(Perk.ERUDITION_ACTIVE.id),
                            data.isPerkUnlocked(Perk.ERUDITION_SYNERGY.id),
                            data.isPerkUnlocked(Perk.ERUDITION_SITUATIONAL.id),
                            data.isPerkUnlocked(Perk.ERUDITION_MASTERY.id),
                            data.isPerkUnlocked(Perk.ERUDITION_TRANSCENDENCE.id))
            );
        }
    }
}

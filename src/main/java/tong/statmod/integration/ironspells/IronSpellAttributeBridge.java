package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
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
    private static final ResourceLocation MANA_REGEN_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_mana_regen_bonus_bridge");
    private static final ResourceLocation SPELL_POWER_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_spell_power_bridge");
    private static final ResourceLocation SPELL_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_spell_resist_bridge");
    private static final ResourceLocation CAST_TIME_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_cast_time_bridge");
    private static final ResourceLocation COOLDOWN_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "iron_cooldown_bridge");

    private static final Map<UUID, Snapshot> LAST_APPLIED = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LOGIN_SERVER_TICK = new ConcurrentHashMap<>();

    private IronSpellAttributeBridge() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;
        if (player instanceof ServerPlayer serverPlayer) {
            apply(serverPlayer);
            // Iron's client-side mana (ClientMagicData) starts at 0 and is only updated by SyncManaPacket.
            // Our single restore packet can be lost if sent before the client is ready, and Iron's
            // won't resend while server mana is already at max. Force-sync for the first 30s after login.
            Integer loginTick = LOGIN_SERVER_TICK.get(serverPlayer.getUUID());
            if (loginTick != null) {
                int now = serverPlayer.level().getServer().getTickCount();
                if (now - loginTick < 600) {
                    MagicData md = MagicData.getPlayerMagicData(serverPlayer);
                    if (md != null) {
                        IronSpellManaSyncBridge.syncMana(serverPlayer, md.getMana(), true);
                    }
                } else {
                    LOGIN_SERVER_TICK.remove(serverPlayer.getUUID());
                }
            }
            if (player.tickCount % 100 == 0) {
                MagicData md = MagicData.getPlayerMagicData(serverPlayer);
                double mm = player.getAttributeValue(AttributeRegistry.MAX_MANA);
                float stored = player.getData(ModAttachments.STATS).getStoredMana();
                STATMod.LOGGER.info("onPlayerTick mana: magicData={}/{} stored={}",
                        md != null ? md.getMana() : -999, mm, stored);
                saveManaToAttachment(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickRegen(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;
        if (player instanceof ServerPlayer serverPlayer) {
            int manaPool = RaceEffectApplier.getEffectiveLevel(serverPlayer, StatType.MANA_POOL.index);
            float regen = (float) Math.min(15.0, IronSpellStatScaler.manaRegenBonus(manaPool));
            float before = MagicData.getPlayerMagicData(serverPlayer).getMana();
            IronSpellManaSyncBridge.addMana(serverPlayer, regen);
            float after = MagicData.getPlayerMagicData(serverPlayer).getMana();
            if (before != after) {
                STATMod.LOGGER.debug("REGEN: {} → {} (+{})", before, after, after - before);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        saveManaToAttachment(event.getEntity());
        LAST_APPLIED.remove(event.getEntity().getUUID());
        LOGIN_SERVER_TICK.remove(event.getEntity().getUUID());
    }

    private static void saveManaToAttachment(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        try {
            double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
            if (maxMana <= 0) return;
            MagicData magicData = MagicData.getPlayerMagicData(player);
            player.getData(ModAttachments.STATS).setStoredMana(magicData.getMana());
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float storedMana = player.getData(ModAttachments.STATS).getStoredMana();
            MagicData md = MagicData.getPlayerMagicData(player);
            float manaAtLogin = md != null ? md.getMana() : -999;
            double maxAtLogin = player.getAttributeValue(AttributeRegistry.MAX_MANA);
            STATMod.LOGGER.info("onPlayerLogin: storedMana={} magicData.mana={} maxMana={}",
                    storedMana, manaAtLogin, maxAtLogin);
            LAST_APPLIED.remove(event.getEntity().getUUID());
            LOGIN_SERVER_TICK.put(player.getUUID(), player.level().getServer().getTickCount());
            apply(player);
            md = MagicData.getPlayerMagicData(player);
            float manaAfterApply = md != null ? md.getMana() : -999;
            double maxAfterApply = player.getAttributeValue(AttributeRegistry.MAX_MANA);
            STATMod.LOGGER.info("onPlayerLogin after apply: mana={} maxMana={}", manaAfterApply, maxAfterApply);
            // Iron's Spellbooks overwrites MagicData on login, so delay restore by 1 tick
            player.getServer().execute(() -> restoreMana(player, storedMana));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        saveManaToAttachment(event.getOriginal());
        LAST_APPLIED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float storedMana = player.getData(ModAttachments.STATS).getStoredMana();
            LAST_APPLIED.remove(event.getEntity().getUUID());
            apply(player);
            restoreMana(player, storedMana);
        }
    }

    private static void restoreMana(ServerPlayer player, float storedMana) {
        try {
            MagicData md = MagicData.getPlayerMagicData(player);
            float manaBeforeRestore = md != null ? md.getMana() : -999;
            double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
            STATMod.LOGGER.info("restoreMana: storedMana={} currentMagicDataMana={} maxMana={}",
                    storedMana, manaBeforeRestore, maxMana);
            if (maxMana <= 0) {
                float fallback = resolveRestoredMana(storedMana, 1000.0d);
                STATMod.LOGGER.warn("restoreMana: maxMana={} <=0, using fallback={}", maxMana, fallback);
                IronSpellManaSyncBridge.syncMana(player, fallback, true);
                return;
            }
            IronSpellManaSyncBridge.syncMana(player, resolveRestoredMana(storedMana, maxMana), true);
        } catch (Exception e) {
            STATMod.LOGGER.error("restoreMana: exception restoring mana for player={} storedMana={}",
                    player.getName().getString(), storedMana, e);
        }
    }

    static float resolveRestoredMana(float storedMana, double maxMana) {
        float cappedMax = Math.max(0.0f, (float) maxMana);
        float result;
        if (storedMana < 0.0f) {
            result = cappedMax;
        } else if (storedMana <= 10.0f) {
            result = cappedMax * 0.2f;
        } else {
            result = Mth.clamp(storedMana, 0.0f, cappedMax);
        }
        STATMod.LOGGER.info("resolveRestoredMana: stored={} maxMana={} cappedMax={} result={}",
                storedMana, maxMana, cappedMax, result);
        return result;
    }

    static void apply(ServerPlayer player) {
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
        // Cancel Iron's native regen (formula: maxMana * regenRate → fills too fast with large maxMana).
        // Our onPlayerTickRegen provides the real regen, calibrated by IronSpellStatScaler.
        applyModifier(player, AttributeRegistry.MANA_REGEN, MANA_REGEN_BONUS_ID, -1.0d);
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

    private static void syncCurrentMana(ServerPlayer player, float previousMana, double previousMaxMana, double newMaxMana) {
        float desiredMana;
        if (previousMaxMana <= 0.0d) {
            // First time: fill mana to max
            desiredMana = (float) newMaxMana;
        } else if (previousMaxMana > 0.0d) {
            float ratio = previousMana / (float) previousMaxMana;
            desiredMana = Mth.clamp((float) newMaxMana * ratio, 0.0f, (float) newMaxMana);
        } else {
            desiredMana = Mth.clamp(previousMana, 0.0f, (float) newMaxMana);
        }

        IronSpellManaSyncBridge.syncMana(player, desiredMana);
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

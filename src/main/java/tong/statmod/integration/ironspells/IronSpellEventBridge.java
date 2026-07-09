package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.magic.CastContext;
import tong.statmod.magic.CastRewardPolicy;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicNodeKind;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.SchoolProgressTracker;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkState;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.UUID;

public final class IronSpellEventBridge {
    private IronSpellEventBridge() {}

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        STATMod.LOGGER.info("PreCast FIRED: spellId={} entity={}", event.getSpellId(), player.getName().getString());
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) {
            STATMod.LOGGER.warn("PreCast: null spell for id={}", event.getSpellId());
            return;
        }
        String spellId = IronSpellsApiAdapter.spellId(spell);
        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean learned = data.hasLearnedSpell(spellId);
        int manaCost = spell.getManaCost(event.getSpellLevel());
        MagicData md = MagicData.getPlayerMagicData(player);
        float currentMana = md != null ? md.getMana() : -999;
        float maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        STATMod.LOGGER.info("PreCast: {} learned={} mana={}/{} cost={} cancel={}",
                spellId, learned, currentMana, maxMana, manaCost, !learned);
        if (shouldCancelPreCast(data, spellId)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("statmod.magic.locked_spell"), true);
            return;
        }
        if (!player.isCreative() && currentMana < manaCost) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("§cNot enough mana: §f" + Math.round(currentMana)
                            + "§7/§f" + manaCost), true);
            return;
        }
    }

    @SubscribeEvent
    public static void onPostCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) return;
        MagicBranch branch = IronSpellsApiAdapter.branchOf(spell);
        if (branch == null) return;
        String canonicalId = IronSpellsApiAdapter.spellId(spell);

        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        double manaFrac = maxMana > 0 ? event.getManaCost() / maxMana : 0.0;
        boolean hadImpact = manaFrac >= 0.25;
        boolean wasFreeCast = manaFrac <= 0;
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        boolean quickCast = IronSpellPerkState.isQuickCast(uuid, now, 2500L);
        int branchChain = IronSpellPerkState.recordCast(uuid, branch, now, 6000L);

        activateAdvancedCastPerks(player, data, branch, quickCast, branchChain, event.getManaCost());

        CastContext ctx = new CastContext(canonicalId, branch, manaFrac, hadImpact, wasFreeCast, event.getSpellLevel());
        CastRewardPolicy.Reward reward = CastRewardPolicy.evaluate(ctx);
        if (reward.masteryDelta() > 0) SchoolProgressTracker.applyMastery(data, branch, reward.masteryDelta());
        if (reward.magicPointsDelta() > 0) data.addMagicPoints(reward.magicPointsDelta());
        boolean magicChanged = reward.masteryDelta() > 0 || reward.magicPointsDelta() > 0;
        if (magicChanged) SyncHelper.syncMagic(player);
        STATMod.LOGGER.debug("Cast progression: {} branch={} mastery+={} magicPoints+={}",
                canonicalId, branch, reward.masteryDelta(), reward.magicPointsDelta());
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (!(event.getSpellDamageSource().getEntity() instanceof ServerPlayer player)) return;
        AbstractSpell spell = event.getSpellDamageSource().spell();
        MagicBranch branch = IronSpellsApiAdapter.branchOf(spell);
        PlayerStatData data = player.getData(ModAttachments.STATS);
        double bonus = IronSpellStatScaler.elementalSpellPowerBonus(
                branch,
                RaceEffectApplier.getEffectiveLevel(player, StatType.FIRE_AFFINITY.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.WATER_AFFINITY.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.EARTH_AFFINITY.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.AIR_AFFINITY.index),
                data.isPerkUnlocked(Perk.FIRE_CORE.id),
                data.isPerkUnlocked(Perk.WATER_CORE.id),
                data.isPerkUnlocked(Perk.EARTH_CORE.id),
                data.isPerkUnlocked(Perk.AIR_CORE.id)
        );
        if (bonus > 0.0d) {
            event.setAmount((float) (event.getAmount() * (1.0d + bonus)));
        }

        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        int branchChain = IronSpellPerkState.currentBranchChain(uuid, branch, now, 6000L);
        boolean quickWindow = PerkState.isOnCooldown(uuid, Perk.CASTING_SPEED_ACTIVE.id, 2500L)
                || PerkState.isOnCooldown(uuid, Perk.ARCANE_SYNERGY.id, 3000L)
                || PerkState.isOnCooldown(uuid, Perk.CASTING_SPEED_SYNERGY.id, 3000L);
        LivingEntity target = event.getEntity();

        double advancedMultiplier = IronSpellAdvancedPerkScaling.arcaneDamageMultiplier(
                data.isPerkUnlocked(Perk.ARCANE_ACTIVE.id)
                        && PerkState.isOnCooldown(uuid, Perk.ARCANE_ACTIVE.id, 4000L),
                data.isPerkUnlocked(Perk.ARCANE_SYNERGY.id) && quickWindow,
                data.isPerkUnlocked(Perk.ARCANE_SITUATIONAL.id) && isControlledTarget(target),
                branchChain,
                data.isPerkUnlocked(Perk.ARCANE_MASTERY.id),
                data.isPerkUnlocked(Perk.ARCANE_TRANSCENDENCE.id));

        advancedMultiplier *= elementalMultiplierFor(player, data, branch, MagicBranch.FIRE, branchChain,
                Perk.FIRE_ACTIVE, Perk.FIRE_SYNERGY, Perk.FIRE_SITUATIONAL, Perk.FIRE_MASTERY,
                Perk.FIRE_TRANSCENDENCE, quickWindow, isLowHealth(target));
        advancedMultiplier *= elementalMultiplierFor(player, data, branch, MagicBranch.WATER, branchChain,
                Perk.WATER_ACTIVE, Perk.WATER_SYNERGY, Perk.WATER_SITUATIONAL, Perk.WATER_MASTERY,
                Perk.WATER_TRANSCENDENCE, hasDeepManaReserve(player), player.getHealth() < player.getMaxHealth() * 0.5f);
        advancedMultiplier *= elementalMultiplierFor(player, data, branch, MagicBranch.EARTH, branchChain,
                Perk.EARTH_ACTIVE, Perk.EARTH_SYNERGY, Perk.EARTH_SITUATIONAL, Perk.EARTH_MASTERY,
                Perk.EARTH_TRANSCENDENCE, true, isControlledTarget(target));
        advancedMultiplier *= elementalMultiplierFor(player, data, branch, MagicBranch.AIR, branchChain,
                Perk.AIR_ACTIVE, Perk.AIR_SYNERGY, Perk.AIR_SITUATIONAL, Perk.AIR_MASTERY,
                Perk.AIR_TRANSCENDENCE, player.getDeltaMovement().horizontalDistanceSqr() > 0.01d,
                isControlledTarget(target));

        if (Math.abs(advancedMultiplier - 1.0d) > 1.0e-6d) {
            event.setAmount((float) (event.getAmount() * advancedMultiplier));
            applyAdvancedElementalHitEffects(branch, data, target, branchChain);
        }
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        MagicBranch branch = IronSpellsApiAdapter.branchOf(event.getSpell());
        int clamped = clampSpellLevel(data, branch, event.getLevel());
        if (clamped < event.getLevel()) event.setLevel(clamped);
    }

    @SubscribeEvent
    public static void onInscribe(InscribeSpellEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = event.getSpellData().getSpell();
        String spellId = IronSpellsApiAdapter.spellId(spell);
        if (shouldCancelPreCast(data, spellId)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        IronSpellPerkState.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        IronSpellPerkState.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        IronSpellPerkState.clear(event.getEntity().getUUID());
    }

    public static boolean shouldCancelPreCast(PlayerStatData data, String spellId) {
        if (data == null || spellId == null) return true;
        return !data.hasLearnedSpell(spellId);
    }

    public static int clampSpellLevel(PlayerStatData data, MagicBranch branch, int requestedLevel) {
        if (data == null || branch == null) return requestedLevel;
        int tierLevel = highestTierReached(data, branch);
        int maxByTier = switch (tierLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 1;
        };
        return Math.min(requestedLevel, maxByTier);
    }

    private static int highestTierReached(PlayerStatData data, MagicBranch branch) {
        int max = 0;
        for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
            if (node.kind() == MagicNodeKind.BRANCH_TIER && data.hasMagicNode(node.id())) {
                int t = node.tier().ordinal() + 1;
                if (t > max) max = t;
            }
        }
        return max;
    }

    private static void activateAdvancedCastPerks(ServerPlayer player,
                                                  PlayerStatData data,
                                                  MagicBranch branch,
                                                  boolean quickCast,
                                                  int branchChain,
                                                  int manaCost) {
        UUID uuid = player.getUUID();
        if (data.isPerkUnlocked(Perk.ARCANE_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.ARCANE_ACTIVE.id, 4000L);
        }
        if (quickCast && data.isPerkUnlocked(Perk.ARCANE_SYNERGY.id)) {
            PerkState.setCooldown(uuid, Perk.ARCANE_SYNERGY.id, 3000L);
        }
        if (data.isPerkUnlocked(Perk.CASTING_SPEED_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.CASTING_SPEED_ACTIVE.id, 2500L);
        }
        if (quickCast && data.isPerkUnlocked(Perk.CASTING_SPEED_SYNERGY.id)) {
            PerkState.setCooldown(uuid, Perk.CASTING_SPEED_SYNERGY.id, 3000L);
        }

        activateElementalWindow(uuid, data, branch);
        applyElementalCastBuffs(player, data, branch);

        double refund = IronSpellAdvancedPerkScaling.manaRefund(
                manaCost,
                data.isPerkUnlocked(Perk.MANA_POOL_ACTIVE.id),
                data.isPerkUnlocked(Perk.MANA_POOL_MASTERY.id) && branchChain >= 2,
                data.isPerkUnlocked(Perk.MANA_POOL_TRANSCENDENCE.id) && branchChain >= 3);
        if (refund > 0.0d) {
            IronSpellManaSyncBridge.addMana(player, (float) refund);
        }
    }

    private static void activateElementalWindow(UUID uuid, PlayerStatData data, MagicBranch branch) {
        if (branch == MagicBranch.FIRE && data.isPerkUnlocked(Perk.FIRE_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.FIRE_ACTIVE.id, 4000L);
        } else if (branch == MagicBranch.WATER && data.isPerkUnlocked(Perk.WATER_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.WATER_ACTIVE.id, 4000L);
        } else if (branch == MagicBranch.EARTH && data.isPerkUnlocked(Perk.EARTH_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.EARTH_ACTIVE.id, 4000L);
        } else if (branch == MagicBranch.AIR && data.isPerkUnlocked(Perk.AIR_ACTIVE.id)) {
            PerkState.setCooldown(uuid, Perk.AIR_ACTIVE.id, 4000L);
        }
    }

    private static void applyElementalCastBuffs(ServerPlayer player, PlayerStatData data, MagicBranch branch) {
        if (branch == MagicBranch.WATER && data.isPerkUnlocked(Perk.WATER_ACTIVE.id)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
        } else if (branch == MagicBranch.EARTH && data.isPerkUnlocked(Perk.EARTH_ACTIVE.id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
        } else if (branch == MagicBranch.AIR && data.isPerkUnlocked(Perk.AIR_ACTIVE.id)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
        } else if (branch == MagicBranch.FIRE && data.isPerkUnlocked(Perk.FIRE_ACTIVE.id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, false, false));
        }
    }

    private static double elementalMultiplierFor(ServerPlayer player,
                                                 PlayerStatData data,
                                                 MagicBranch spellBranch,
                                                 MagicBranch perkBranch,
                                                 int branchChain,
                                                 Perk active,
                                                 Perk synergy,
                                                 Perk situational,
                                                 Perk mastery,
                                                 Perk transcendence,
                                                 boolean synergyCondition,
                                                 boolean situationalCondition) {
        UUID uuid = player.getUUID();
        return IronSpellAdvancedPerkScaling.elementalDamageMultiplier(
                perkBranch,
                spellBranch,
                data.isPerkUnlocked(active.id) && PerkState.isOnCooldown(uuid, active.id, 4000L),
                data.isPerkUnlocked(synergy.id) && synergyCondition,
                data.isPerkUnlocked(situational.id) && situationalCondition,
                branchChain,
                data.isPerkUnlocked(mastery.id),
                data.isPerkUnlocked(transcendence.id));
    }

    private static void applyAdvancedElementalHitEffects(MagicBranch branch,
                                                         PlayerStatData data,
                                                         LivingEntity target,
                                                         int branchChain) {
        if (branch == MagicBranch.EARTH && data.isPerkUnlocked(Perk.EARTH_MASTERY.id)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, branchChain >= 3 ? 2 : 1, false, false));
        } else if (branch == MagicBranch.AIR && data.isPerkUnlocked(Perk.AIR_MASTERY.id)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
        } else if (branch == MagicBranch.FIRE && data.isPerkUnlocked(Perk.FIRE_MASTERY.id)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, branchChain >= 3 ? 1 : 0, false, false));
        }
    }

    private static boolean isControlledTarget(LivingEntity target) {
        return target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                || target.hasEffect(MobEffects.WEAKNESS)
                || target.hasEffect(MobEffects.GLOWING);
    }

    private static boolean isLowHealth(LivingEntity target) {
        return target.getHealth() < target.getMaxHealth() * 0.35f;
    }

    private static boolean hasDeepManaReserve(ServerPlayer player) {
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        return maxMana > 0.0d && MagicData.getPlayerMagicData(player).getMana() / maxMana >= 0.5d;
    }
}

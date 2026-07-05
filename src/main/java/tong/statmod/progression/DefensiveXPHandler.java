package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Handlers pour les stats défensives / perception qui n'avaient aucune XP source naturelle
 * avant Mission ι : {@code PHYSICAL_RESISTANCE}, {@code KEEN_SENSES}, {@code TRACKING},
 * {@code WILLPOWER}.
 *
 * <p>Ces stats sont des gates dans la nouvelle économie magique (voir
 * {@link tong.statmod.magic.MagicNodeStatRequirements}), donc sans XP source naturelle elles
 * étaient impossibles à monter sans command admin, ce qui bloquait la progression magic gates
 * sur les écoles non-Fire.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public class DefensiveXPHandler {
    private static final int MOVEMENT_TRAINING_PERIOD_TICKS = 100;

    @SubscribeEvent
    public static void onMovementTrainingTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % MOVEMENT_TRAINING_PERIOD_TICKS != 0) return;

        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.01d;
        boolean flying = player.getAbilities().flying || player.isFallFlying();
        boolean passenger = player.isPassenger() || player.isSpectator();
        int agilityXp = ActivityXpScaling.agilityXpForMovement(
                moving, player.isSprinting(), flying, passenger);
        int enduranceXp = ActivityXpScaling.enduranceXpForMovement(
                moving, player.isSprinting(), player.isSwimming(), flying, passenger);
        if (agilityXp <= 0 && enduranceXp <= 0) {
            return;
        }

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = false;
        if (agilityXp > 0) {
            leveled |= RaceEffectApplier.addScaledXp(
                    player, StatType.AGILITY.index, agilityXp, data, false);
        }
        if (enduranceXp > 0) {
            leveled |= RaceEffectApplier.addScaledXp(
                    player, StatType.PHYSICAL_ENDURANCE.index, enduranceXp, data, false);
        }
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    /**
     * Encaisser du damage physique entraîne PHYSICAL_RESISTANCE. Filtre les sources magic
     * (qui montent MAGIC_RESISTANCE via {@code MagicXpBridge}).
     */
    @SubscribeEvent
    public static void onTakePhysicalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        // Magic damage est traité par MagicXpBridge (MAGIC_RESISTANCE). On évite le double-dip.
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return;
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int xp = Math.max(1, Math.round(event.getOriginalDamage() / 2.0f));
        boolean leveled = RaceEffectApplier.addScaledXp(
                player, StatType.PHYSICAL_RESISTANCE.index, xp, data, true);
        int enduranceXp = ActivityXpScaling.enduranceXpForPhysicalDamage(event.getOriginalDamage());
        if (enduranceXp > 0) {
            leveled |= RaceEffectApplier.addScaledXp(
                    player, StatType.PHYSICAL_ENDURANCE.index, enduranceXp, data, true);
        }
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    /**
     * Quand un status effect EXPIRE naturellement (le joueur l'a "tenu" jusqu'au bout) ou est
     * remplacé, on récompense la WILLPOWER. Pas appliqué aux effets bénéfiques (buffs) — seuls
     * les debuffs nécessitent de l'endurance mentale.
     */
    @SubscribeEvent
    public static void onMobEffectExpire(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null) return;
        if (effect.getEffect().value().isBeneficial()) return; // pas d'XP pour les buffs

        PlayerStatData data = player.getData(ModAttachments.STATS);
        // XP proportionnel à la durée totale du debuff (en secondes), capped pour éviter
        // l'exploit de boire 10 potions de poison longue durée d'affilée.
        int durationSec = Math.min(60, effect.getDuration() / 20);
        int xp = Math.max(2, durationSec / 3);
        boolean leveled = RaceEffectApplier.addScaledXp(
                player, StatType.WILLPOWER.index, xp, data, false);
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    /**
     * Tuer une cible avec un projectile à longue distance entraîne TRACKING (la stat de
     * "détection/marquage à distance"). Distance minimum pour éviter l'exploit kill-au-corps-
     * à-corps avec un arc.
     */
    public static void awardTrackingForRangedKill(Player player, LivingEntity victim, double distance) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (distance < 8.0) return; // pas d'XP pour les kills à bout portant

        PlayerStatData data = serverPlayer.getData(ModAttachments.STATS);
        int xp = Math.max(1, (int) Math.round(distance / 4.0));
        boolean leveled = RaceEffectApplier.addScaledXp(
                serverPlayer, StatType.TRACKING.index, xp, data, true);
        if (leveled) SoundHelper.playLevelUp(serverPlayer);
        SyncHelper.syncStats(serverPlayer);
    }

    /**
     * Esquiver un projectile entrant gagne KEEN_SENSES. Hook depuis {@code EpicFightCompat}
     * onDodge ou onProjectileMiss. Ici on expose juste la méthode publique ; les callsites
     * doivent appeler explicitement.
     */
    public static void awardKeenSensesForDodge(Player player, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PlayerStatData data = serverPlayer.getData(ModAttachments.STATS);
        boolean leveled = RaceEffectApplier.addScaledXp(
                serverPlayer, StatType.KEEN_SENSES.index, Math.max(1, amount), data, true);
        if (leveled) SoundHelper.playLevelUp(serverPlayer);
        SyncHelper.syncStats(serverPlayer);
    }
}

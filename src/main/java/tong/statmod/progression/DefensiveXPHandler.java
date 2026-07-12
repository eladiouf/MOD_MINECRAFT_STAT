package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

/**
 * Handlers pour les stats défensives / perception qui n'avaient aucune XP source naturelle
 * avant Mission ι : {@code PHYSICAL_RESISTANCE}, {@code KEEN_SENSES}, {@code TRACKING},
 * {@code WILLPOWER}.
 *
 * <p>Balance 30j : ajouté des sources pour WILLPOWER (debuff received + low HP),
 * KEEN_SENSES (sneak near hostiles), COOKING (manger), ALCHEMY (boire potion).
 */
@EventBusSubscriber(modid = STATMod.MODID)
public class DefensiveXPHandler {
    private static final int MOVEMENT_TRAINING_PERIOD_TICKS = 200;
    /** Période pour les checks de survie basse vie / sneak (toutes les 5s). */
    private static final int SURVIVAL_CHECK_PERIOD_TICKS = 100;

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

    // ── Balance 30j : WILLPOWER low HP + KEEN_SENSES sneak near hostiles ────────

    /**
     * Check périodique (toutes les 5s) pour :
     * - WILLPOWER : XP si le joueur est sous 30% HP (survie sous pression)
     * - KEEN_SENSES : XP si le joueur est en sneak à proximité de mobs hostiles (sans EpicFight)
     */
    @SubscribeEvent
    public static void onSurvivalCheckTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % SURVIVAL_CHECK_PERIOD_TICKS != 0) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = false;

        // WILLPOWER — XP pour survivre sous 30% HP
        int willXp = ActivityXpScaling.willpowerXpForLowHpSurvival(
                player.getHealth(), player.getMaxHealth());
        if (willXp > 0) {
            leveled |= RaceEffectApplier.addScaledXp(
                    player, StatType.WILLPOWER.index, willXp, data, false);
        }

        // KEEN_SENSES — XP pour sneak à proximité de mobs hostiles (source hors EpicFight)
        if (player.isShiftKeyDown()) {
            AABB area = player.getBoundingBox().inflate(12.0);
            List<Mob> nearbyHostiles = player.level().getEntitiesOfClass(
                    Mob.class, area,
                    mob -> mob.isAlive() && mob.getTarget() != null);
            int keenXp = ActivityXpScaling.keenSensesXpForSneakNearHostiles(nearbyHostiles.size());
            if (keenXp > 0) {
                leveled |= RaceEffectApplier.addScaledXp(
                        player, StatType.KEEN_SENSES.index, keenXp, data, false);
            }
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
        int xp = Math.max(1, Math.round(event.getOriginalDamage() / 4.0f));
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
        int xp = Math.max(2, durationSec / 5);
        boolean leveled = RaceEffectApplier.addScaledXp(
                player, StatType.WILLPOWER.index, xp, data, false);
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    // ── Balance 30j : WILLPOWER XP quand un debuff est REÇU (pas juste quand il expire) ────

    /**
     * Quand un debuff néfaste est appliqué au joueur, on donne immédiatement de la WILLPOWER XP.
     * Compense le fait que la plupart des debuffs sont cleansés (milk, etc.) avant d'expirer.
     */
    @SubscribeEvent
    public static void onDebuffReceived(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null) return;
        if (effect.getEffect().value().isBeneficial()) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int xp = ActivityXpScaling.willpowerXpForDebuffReceived(effect.getAmplifier());
        boolean leveled = RaceEffectApplier.addScaledXp(
                player, StatType.WILLPOWER.index, xp, data, false);
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    // ── Balance 30j : COOKING XP pour manger + ALCHEMY XP pour boire une potion ────

    /**
     * Quand le joueur finit de consommer un item (nourriture ou potion) :
     * - Nourriture → COOKING XP proportionnel à la nutrition
     * - Potion → ALCHEMY XP proportionnel au nombre d'effets
     */
    @SubscribeEvent
    public static void onItemConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = false;

        // COOKING — XP pour manger de la nourriture
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null) {
            int cookXp = ActivityXpScaling.cookingXpForEating(food.nutrition());
            if (cookXp > 0) {
                leveled |= RaceEffectApplier.addScaledXp(
                        player, StatType.COOKING.index, cookXp, data, false);
            }
        }

        // ALCHEMY — XP pour boire/utiliser une potion
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            PotionContents potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            int effectCount = 0;
            for (MobEffectInstance ignored : potion.getAllEffects()) {
                effectCount++;
            }
            int alchXp = ActivityXpScaling.alchemyXpForDrinking(Math.max(1, effectCount));
            if (alchXp > 0) {
                leveled |= RaceEffectApplier.addScaledXp(
                        player, StatType.ALCHEMY.index, alchXp, data, false);
            }
        }

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
        int xp = Math.max(1, (int) Math.round(distance / 6.0));
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

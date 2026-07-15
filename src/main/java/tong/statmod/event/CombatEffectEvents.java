package tong.statmod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.effects.CombatScalingRules;
import tong.statmod.effects.CombatStatScaling;
import tong.statmod.progression.xp.CombatEligibility;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.progression.xp.WeaponClassifier;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class CombatEffectEvents {
    private CombatEffectEvents() {
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        float original = event.getAmount();
        if (!Float.isFinite(original) || original <= 0F) {
            return;
        }

        CombatScalingRules rules = StatModServerConfig.snapshot();
        float scaled = applyOffense(event.getEntity(), event.getSource(), original, rules);
        scaled = applyDefense(event.getEntity(), event.getSource(), scaled, rules);
        if (Float.compare(original, scaled) != 0) {
            event.setAmount(scaled);
        }
    }

    private static float applyOffense(
            LivingEntity target, DamageSource source, float amount, CombatScalingRules rules) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || !eligiblePlayer(attacker)
                || !CombatEligibility.eligibleTarget(attacker, target)) {
            return amount;
        }

        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        boolean direct = source.getDirectEntity() == attacker;
        if (!projectile && !direct) {
            return amount;
        }

        WeaponClassification classification = WeaponClassifier.classify(
                attacker.getMainHandItem(), projectile);
        StatType stat = CombatStatScaling.offensiveStat(classification).orElse(null);
        if (stat == null) {
            return amount;
        }

        PlayerStats stats = attacker.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        if (stats == null) {
            return amount;
        }
        double multiplier = CombatStatScaling.offensiveMultiplier(
                stats.get(stat).level(), rules);
        return CombatStatScaling.applyMultiplier(amount, multiplier);
    }

    private static float applyDefense(
            LivingEntity target, DamageSource source, float amount, CombatScalingRules rules) {
        if (!(target instanceof ServerPlayer victim)
                || !eligiblePlayer(victim)
                || !CombatEligibility.physicalProfile(source, victim)) {
            return amount;
        }

        PlayerStats stats = victim.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        if (stats == null) {
            return amount;
        }
        double multiplier = CombatStatScaling.defensiveMultiplier(
                stats.get(StatType.PHYSICAL_RESISTANCE).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level(),
                rules);
        return CombatStatScaling.applyMultiplier(amount, multiplier);
    }

    private static boolean eligiblePlayer(ServerPlayer player) {
        return !(player instanceof FakePlayer)
                && !player.isCreative()
                && !player.isSpectator();
    }
}

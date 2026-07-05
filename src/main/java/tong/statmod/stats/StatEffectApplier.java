package tong.statmod.stats;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkState;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.storage.ModAttachments;

@EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        float dmg = event.getNewDamage();

        if (event.getSource().getEntity() instanceof Player attacker) {
            StatType weaponStat = WeaponResolver.statFor(attacker.getMainHandItem());
            int brute = RaceEffectApplier.getEffectiveLevel(attacker, StatType.BRUTE_FORCE.index);
            int blade = RaceEffectApplier.getEffectiveLevel(attacker, StatType.BLADE_TECHNIQUE.index);
            int precision = RaceEffectApplier.getEffectiveLevel(attacker, StatType.PRECISION.index);
            int rapidite = RaceEffectApplier.getEffectiveLevel(attacker, StatType.RAPIDITE.index);
            int arcane = RaceEffectApplier.getEffectiveLevel(attacker, StatType.ARCANE_POWER.index);
            float dmgBase = (float) tong.statmod.config.Config.getWeaponDamageBase();
            float dmgScale = (float) tong.statmod.config.Config.getWeaponDamageScale();
            dmg *= StatCombatScaling.weaponDamageMultiplier(
                    weaponStat, brute, blade, precision, rapidite, arcane, dmgBase, dmgScale);

            int intimid = RaceEffectApplier.getEffectiveLevel(attacker, StatType.INTIMIDATION.index);
            if (event.getEntity() instanceof LivingEntity target) {
                boolean marked = target.hasEffect(MobEffects.GLOWING)
                        || PerkState.isTrackedTarget(attacker.getUUID(), target.getId());
                dmg *= StatCombatScaling.intimidationDamageMultiplier(intimid, marked);
            }

            if (weaponStat == StatType.PRECISION && precision >= 50 && attacker.getHealth() >= attacker.getMaxHealth()) {
                dmg *= 1.15f;
            }

            if (weaponStat == StatType.RAPIDITE && rapidite > 0 && attacker.getRandom().nextFloat() < rapidite * 0.001f) {
                dmg *= 1.5f;
            }

            int tracking = RaceEffectApplier.getEffectiveLevel(attacker, StatType.TRACKING.index);
            if (tracking > 0 && !attacker.level().isClientSide) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.GLOWING, tracking * 2, 0, false, false));
            }

            int agility = RaceEffectApplier.getEffectiveLevel(attacker, StatType.AGILITY.index);
            if (agility > 0 && attacker.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                dmg *= 1.0f + agility * 0.002f;
            }
        }

        if (event.getEntity() instanceof Player victim) {
            int phys = RaceEffectApplier.getEffectiveLevel(victim, StatType.PHYSICAL_RESISTANCE.index);
            int magicRes = RaceEffectApplier.getEffectiveLevel(victim, StatType.MAGIC_RESISTANCE.index);
            int will = RaceEffectApplier.getEffectiveLevel(victim, StatType.WILLPOWER.index);
            int endurance = RaceEffectApplier.getEffectiveLevel(victim, StatType.PHYSICAL_ENDURANCE.index);
            dmg *= StatCombatScaling.incomingDamageMultiplier(
                    damageRole(event.getSource()), phys, magicRes, endurance, will);

            int keen = RaceEffectApplier.getEffectiveLevel(victim, StatType.KEEN_SENSES.index);
            if (keen > 0 && victim.getRandom().nextFloat() < keen * 0.002f) {
                event.setNewDamage(0);
                return;
            }
        }

        if (Math.abs(dmg - event.getNewDamage()) > 0.001f) {
            event.setNewDamage(Math.max(0.0f, dmg));
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            int agility = RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index);
            if (agility > 0) {
                event.setDistance(event.getDistance() * Math.max(0.1f, 1.0f - agility * 0.005f));
            }
            int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
            if (endurance > 0) {
                event.setDamageMultiplier(event.getDamageMultiplier() * Math.max(0.1f, 1.0f - endurance * 0.003f));
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || effect.getEffect().value().isBeneficial()) {
            return;
        }
        if (player.getData(ModAttachments.STATS).isPerkUnlocked(Perk.WILL_TRANSCENDENCE.id)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || effect.getEffect().value().isBeneficial()) {
            return;
        }

        int will = RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index);
        boolean ironWill = player.getData(ModAttachments.STATS).isPerkUnlocked(Perk.WILL_CORE.id);
        effect.mapDuration(duration -> StatCombatScaling.negativeEffectDurationTicks(duration, will, ironWill));
    }

    private static StatCombatScaling.IncomingDamageRole damageRole(DamageSource source) {
        if (source == null) {
            return StatCombatScaling.IncomingDamageRole.ENVIRONMENT;
        }
        return StatCombatScaling.damageRole(
                source.is(DamageTypes.MAGIC),
                source.is(DamageTypes.INDIRECT_MAGIC),
                source.getMsgId());
    }
}

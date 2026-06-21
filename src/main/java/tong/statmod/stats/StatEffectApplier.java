package tong.statmod.stats;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;

@EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        float dmg = event.getNewDamage();

        if (event.getSource().getEntity() instanceof Player attacker) {
            int brute = RaceEffectApplier.getEffectiveLevel(attacker, StatType.BRUTE_FORCE.index);
            int blade = RaceEffectApplier.getEffectiveLevel(attacker, StatType.BLADE_TECHNIQUE.index);
            dmg *= 1.0f + (brute + blade) * 0.005f;

            int precision = RaceEffectApplier.getEffectiveLevel(attacker, StatType.PRECISION.index);
            dmg *= 1.0f + precision * 0.005f;

            int intimid = RaceEffectApplier.getEffectiveLevel(attacker, StatType.INTIMIDATION.index);
            if (event.getEntity() instanceof LivingEntity) {
                dmg *= 1.0f + intimid * 0.003f;
            }

            if (precision >= 50 && attacker.getHealth() >= attacker.getMaxHealth()) {
                dmg *= 1.15f;
            }

            int rapidite = RaceEffectApplier.getEffectiveLevel(attacker, StatType.RAPIDITE.index);
            if (rapidite > 0 && attacker.getRandom().nextFloat() < rapidite * 0.001f) {
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
            dmg *= 1.0f - Math.min(0.5f, phys * 0.005f);

            int magicRes = RaceEffectApplier.getEffectiveLevel(victim, StatType.MAGIC_RESISTANCE.index);
            if (magicRes > 0 && event.getSource().getDirectEntity() != null
                    && event.getSource().getDirectEntity() != event.getSource().getEntity()) {
                dmg *= 1.0f - Math.min(0.5f, magicRes * 0.005f);
            }

            int will = RaceEffectApplier.getEffectiveLevel(victim, StatType.WILLPOWER.index);
            dmg *= 1.0f - Math.min(0.4f, will * 0.003f);

            int endurance = RaceEffectApplier.getEffectiveLevel(victim, StatType.PHYSICAL_ENDURANCE.index);
            dmg *= 1.0f - Math.min(0.3f, endurance * 0.002f);

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
}

package tong.statmod.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkDamageHandler {

    // ==================== ATTACK ====================

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;

        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.RAPID_ELAN)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }

            if (perks.isUnlocked(Perk.TRACKING_PACK)) {
                PerkState.noteTrackedHit(player.getUUID(), event.getTarget().getId());
            }

            if (perks.isUnlocked(Perk.BLADE_COMBO)) {
                UUID uuid = player.getUUID();
                long now = System.currentTimeMillis();
                PerkState.recordComboHit(uuid, now, 3000);
            }
        });
    }

    // ==================== HURT ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        // --- PLAYER ATTACKING ---
        if (sourceEntity instanceof ServerPlayer attacker) {
            LivingEntity target = event.getEntity();

            CapabilityHelper.withPerks(attacker, perks -> {
                float multiplier = 1.0f;

                if (perks.isUnlocked(Perk.BRUTE_DEMOLITION)) multiplier += 0.5f;

                if (perks.isUnlocked(Perk.BLADE_COMBO)) {
                    int count = PerkState.getComboCount(attacker.getUUID());
                    if (count >= 5) {
                        multiplier += 0.5f;
                        PerkState.resetCombo(attacker.getUUID());
                    }
                }

                if (perks.isUnlocked(Perk.RAPID_DOUBLE) && attacker.getRandom().nextFloat() < 0.1f) {
                    multiplier += 1.0f;
                }

                if (perks.isUnlocked(Perk.PRECISION_SNIPER) && attacker.distanceTo(target) > 15.0) {
                    multiplier += 0.1f;
                }

                if (perks.isUnlocked(Perk.PRECISION_CRIT)
                        && attacker.getAttackStrengthScale(0.5f) > 0.9f && attacker.fallDistance > 0) {
                    target.knockback(1.0f, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                }

                if (perks.isUnlocked(Perk.INTIMIDATE_FEARLESS)) {
                    long hostiles = attacker.level().getEntitiesOfClass(Mob.class,
                        attacker.getBoundingBox().inflate(10), m -> m.getTarget() == attacker).size();
                    if (hostiles >= 3) multiplier += 0.25f;
                }

                if (perks.isUnlocked(Perk.WILL_UNBREAKABLE) && attacker.getHealth() < 4.0f) {
                    multiplier += 0.3f;
                }

                if (perks.isUnlocked(Perk.TRACKING_PACK)) {
                    if (PerkState.hasTrackedHit(attacker.getUUID(), target.getId())) {
                        multiplier += 0.15f;
                    }
                }

                if (perks.isUnlocked(Perk.BLADE_BLEED) && attacker.getAttackStrengthScale(0.5f) > 0.8f) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                }

                if (perks.isUnlocked(Perk.BRUTE_ARMOR_PIERCE)) {
                    for (ItemStack armor : target.getArmorSlots()) {
                        if (!armor.isEmpty() && armor.isDamageableItem()) {
                            int extraDmg = Math.max(1, (int) (event.getAmount() * 0.15f));
                            armor.hurtAndBreak(extraDmg, target, e -> e.broadcastBreakEvent(e.getEquipmentSlotForItem(armor)));
                        }
                    }
                }

                if (perks.isUnlocked(Perk.BRUTE_STUN) && attacker.getAttackStrengthScale(0.5f) > 0.8f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 4, false, true));
                }

                if (perks.isUnlocked(Perk.PRECISION_PIERCE)) {
                    Entity projectile = event.getSource().getDirectEntity();
                    if (projectile instanceof Arrow arrow) {
                        String pierceTag = "statmod_pierced";
                        if (!arrow.getTags().contains(pierceTag)) {
                            arrow.addTag(pierceTag);
                            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(0.8));
                            arrow.setPierceLevel((byte) (arrow.getPierceLevel() + 1));
                        }
                    }
                }

                if (perks.isUnlocked(Perk.INTIMIDATE_ROAR)) {
                    if (target.getHealth() - event.getAmount() <= 0) {
                        attacker.level().getEntitiesOfClass(Mob.class,
                            attacker.getBoundingBox().inflate(8), m -> m != target).forEach(mob -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                        });
                    }
                }

                if (perks.isUnlocked(Perk.RAPID_INSTINCT) && attacker.invulnerableTime > 0) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, false));
                }

                if (perks.isUnlocked(Perk.AGILITY_SPRINT) && attacker.isSprinting() && !attacker.onGround()) {
                    multiplier += 0.3f;
                }

                if (multiplier != 1.0f) event.setAmount(event.getAmount() * multiplier);
            });
        }

        // --- PLAYER BEING HIT ---
        if (event.getEntity() instanceof ServerPlayer defender) {
            CapabilityHelper.withPerks(defender, perks -> {
                float reduction = 0.0f;

                if (perks.isUnlocked(Perk.BLADE_PARRY) && defender.isBlocking()) {
                    reduction += 0.25f;
                }

                if (perks.isUnlocked(Perk.ENDURANCE_MAGIC) && defender.isBlocking()
                        && "indirectMagic".equals(event.getSource().getMsgId())) {
                    reduction += 0.20f;
                }

                if (perks.isUnlocked(Perk.RESIST_TOUGHNESS) && defender.getRandom().nextFloat() < 0.3f) {
                    reduction += 0.5f;
                }

                if (perks.isUnlocked(Perk.WILL_UNBREAKABLE) && defender.getHealth() < 4.0f) {
                    reduction += 0.3f;
                }

                if (reduction > 0) {
                    event.setAmount(event.getAmount() * (1.0f - Math.min(reduction, 0.9f)));
                }

                if (perks.isUnlocked(Perk.RESIST_ABSORB) && !PerkState.isOnCooldown(defender.getUUID(), 13, 10000)) {
                    defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, false));
                    PerkState.setCooldown(defender.getUUID(), 13);
                }

                if (perks.isUnlocked(Perk.ENDURANCE_PERFECT) && defender.isBlocking()
                        && defender.invulnerableTime > 0 && defender.invulnerableTime < 10) {
                    event.setAmount(0);
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof LivingEntity le) {
                        le.knockback(1.5f, defender.getX() - le.getX(), defender.getZ() - le.getZ());
                    }
                }
            });
        }
    }

    // ==================== FALL ====================

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.RESIST_FALL)) {
                event.setDistance(event.getDistance() * 0.9f);
            }
        });
    }

    // ==================== CLEANUP ====================

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PerkState.clearPlayer(event.getEntity().getUUID());
    }
}

package tong.statmod.perks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;



@EventBusSubscriber(modid = STATMod.MODID)
public final class PerkEffectHandler {
    private PerkEffectHandler() {}

    private static final ResourceLocation SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "speed_mod");
    private static final ResourceLocation ATTACK_SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "attack_speed_mod");
    private static final ResourceLocation FOCUSED_KB_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "focused_mind_kb");

    private static final Map<UUID, Double> savedArmorBase = new ConcurrentHashMap<>();

    private static PerkManager managerFor(Player player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        return new PerkManager(data);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PerkManager perks = managerFor(player);
        UUID uuid = player.getUUID();

        // ENDUR_CORE id=30: Sturdy — +2 absorption hearts
        if (perks.isUnlocked(Perk.byId(30)) && player.getAbsorptionAmount() < 4f) {
            player.setAbsorptionAmount(4f);
        }

        // TRACK_CORE id=42: Tracker's Eye — Glow nearby mobs periodically
        if (perks.isUnlocked(Perk.byId(42)) && player.tickCount % 40 == 0) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), LivingEntity::isAlive)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));
        }

        // BLADE_ACTIVE id=7: Flowing Strike — record combo hits
        if (perks.isUnlocked(Perk.byId(7))) {
            PerkState.recordComboHit(uuid, System.currentTimeMillis(), 5000);
        }

        // AGIL_CORE id=18: Light Feet — +5% movement speed
        if (perks.isUnlocked(Perk.byId(18))) {
            applyMovementSpeedModifier(player, 1.05);
        } else {
            removeMovementSpeedModifier(player);
        }

        // RAPID_CORE id=12: Quick Hands — +5% attack speed
        if (perks.isUnlocked(Perk.byId(12))) {
            applyAttackSpeedModifier(player, 1.05);
        } else {
            removeAttackSpeedModifier(player);
        }

        // TRACK_MASTERY id=46: Predator — see all mobs through walls
        if (perks.isUnlocked(Perk.byId(46))) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(32), LivingEntity::isAlive)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));
        }

        // TRACK_TRANSCENDENCE id=47: Omnisight — reveal all entities
        if (perks.isUnlocked(Perk.byId(47))) {
            player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(50),
                    e -> e.isAlive() && e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));
        }

        // SENSE_MASTERY id=52: Foresight — dodge next hit once per 10s
        // (handled in damage event, tick just resets if not on cooldown)

        // ENDUR_MASTERY id=34: Overflowing Vitality — heal 0.5 HP every 5s in combat
        if (perks.isUnlocked(Perk.byId(34)) && player.tickCount % 100 == 0) {
            if (player.getLastHurtByMob() != null || player.getLastHurtMob() != null) {
                player.heal(0.5f);
            }
        }

        // COOK_ACTIVE id=61: Iron Stomach — clear bad effects periodically
        if (perks.isUnlocked(Perk.byId(61)) && player.tickCount % 40 == 0) {
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
        }

        // WILL_CORE id=78: Iron Will — reduce negative effect duration
        // Handled via onLivingDamagePre and potion effect application

        // BLADE_SYNERGY id=8: Dance of Blades — combo hits grant speed
        if (perks.isUnlocked(Perk.byId(8))) {
            int combo = PerkState.getComboCount(uuid, System.currentTimeMillis(), 5000);
            if (combo >= 3 && !player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }
        }

        // RAPID_SYNERGY id=14: Blinding Speed — kills grant speed
        // Handled via cooldown state from onLivingDeath

        // COOK_SYNERGY id=62: Nutritionist — food gives absorption
        // Handled when food is eaten (can't easily intercept, so periodic check)
        if (perks.isUnlocked(Perk.byId(62))) {
            FoodData food = player.getFoodData();
            if (food.getFoodLevel() >= 18 && player.getAbsorptionAmount() < 2f) {
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 2f));
            }
        }

        // COOK_TRANSCENDENCE id=65: Ambrosia — food gives regen
        if (perks.isUnlocked(Perk.byId(65))) {
            FoodData food = player.getFoodData();
            if (food.getFoodLevel() >= 20 && !player.hasEffect(MobEffects.REGENERATION)) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, false));
            }
        }

        // INTIM_ACTIVE id=73: Intimidating Aura — nearby mobs deal less damage
        if (perks.isUnlocked(Perk.byId(73))) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(8),
                    e -> e.isAlive() && e.getTarget() == player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false)));
        }

        // WILL_TRANSCENDENCE id=83: Transcendence — immune to all negative effects
        if (perks.isUnlocked(Perk.byId(83))) {
            player.getActiveEffects().stream()
                    .filter(inst -> !inst.getEffect().value().isBeneficial())
                    .map(MobEffectInstance::getEffect)
                    .toList()
                    .forEach(player::removeEffect);
        }

        // INTIM_SYNERGY id=74: Feared — mobs flee at low HP
        if (perks.isUnlocked(Perk.byId(74)) && player.getHealth() < player.getMaxHealth() * 0.2f) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(12),
                    e -> e.isAlive() && e.getTarget() == player)
                    .forEach(e -> {
                        e.setTarget(null);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
                    });
        }

        // RAPID_SITUATIONAL id=15: Flurry — attacks faster as combo builds
        if (perks.isUnlocked(Perk.byId(15))) {
            int frenzy = PerkState.getFrenzyStacks(uuid);
            if (frenzy > 0) {
                float speedMult = 1.0f + frenzy * 0.03f;
                applyAttackSpeedModifier(player, speedMult);
            }
        }

        // SENSE_SITUATIONAL id=51: Intuition — haste near enemies when mining
        if (perks.isUnlocked(Perk.byId(51))) {
            boolean nearEnemy = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16),
                    e -> e.isAlive() && e.getTarget() == player).isEmpty();
            if (nearEnemy && !player.hasEffect(MobEffects.DIG_SPEED)) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 0, false, false));
            }
        }

        // SENSE_CORE id=48: Sixth Sense — 5% dodge handled in damage event
        // TRACK_ACTIVE id=43: Pursuit — speed toward marked targets
        if (perks.isUnlocked(Perk.byId(43))) {
            boolean nearGlowing = !player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(20),
                    e -> e.isAlive() && e.hasEffect(MobEffects.GLOWING)).isEmpty();
            if (nearGlowing && !player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            }
        }

        // PRECI_SITUATIONAL id=39: Critical Eye — +15% crit at max health (handled in onCriticalHit)
        // RESIST_SYNERGY id=26: Fortress — armor bonus when still
        if (perks.isUnlocked(Perk.byId(26)) && player.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
            if (!player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
            }
        }

        // ENDUR_SYNERGY id=32: Unstoppable — no knockback when shielded
        if (perks.isUnlocked(Perk.byId(32)) && player.isBlocking()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
        }

        // ENDUR_TRANSCENDENCE id=35: Limit Break — double max absorption
        if (perks.isUnlocked(Perk.byId(35)) && player.getAbsorptionAmount() < 40f) {
            player.setAbsorptionAmount(Math.min(40f, player.getAbsorptionAmount() + 1f));
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        float dmg = event.getNewDamage();

        // Attacker side
        if (event.getSource().getEntity() instanceof Player attacker) {
            PerkManager perks = managerFor(attacker);
            UUID uuid = attacker.getUUID();

            if (perks.isUnlocked(Perk.byId(0))) dmg *= 1.05f;
            if (perks.isUnlocked(Perk.byId(6))) dmg *= 1.05f;
            if (perks.isUnlocked(Perk.byId(72))
                    && PerkState.isTrackedTarget(uuid, event.getEntity().getId())) {
                dmg *= 1.05f;
            }

            // BRUTE_SITUATIONAL id=3: Berserker — +20% below 30% HP
            if (perks.isUnlocked(Perk.byId(3))
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.3f) {
                dmg *= 1.20f;
            }

            // BRUTE_SYNERGY id=2: Crushing Force — +15% below 50% HP
            if (perks.isUnlocked(Perk.byId(2))
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.5f) {
                dmg *= 1.15f;
            }

            // BRUTE_ACTIVE id=1: Mighty Swing — charged attack +10%
            if (perks.isUnlocked(Perk.byId(1)) && attacker.isAutoSpinAttack()) {
                dmg *= 1.10f;
            }

            // BLADE_ACTIVE id=7: Flowing Strike — every 3rd hit double damage
            if (perks.isUnlocked(Perk.byId(7))) {
                PerkState.recordComboHit(uuid, System.currentTimeMillis(), 5000);
                if (PerkState.getComboCount(uuid, System.currentTimeMillis(), 5000) >= 3) {
                    dmg *= 2.0f;
                    PerkState.setCooldown(uuid, 7);
                }
            }

            // BLADE_TRANSCENDENCE id=11: One With the Blade — all crit for 5s after kill
            if (perks.isUnlocked(Perk.byId(11))
                    && !PerkState.isOnCooldown(uuid, 11, 5000L)) {
                // handled via onCriticalHit
            }

            // AGIL_ACTIVE id=19: Agile Strikes — moving attacks +10%
            if (perks.isUnlocked(Perk.byId(19))
                    && attacker.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                dmg *= 1.10f;
            }

            // AGIL_SYNERGY id=20: Wind Walker — dodge grants damage buff
            if (perks.isUnlocked(Perk.byId(20))
                    && !PerkState.isOnCooldown(uuid, 20, 3000L)
                    && System.currentTimeMillis() - PerkState.getLastDodgeTime(uuid) < 3000) {
                dmg *= 1.20f;
            }

            // RAPID_ACTIVE id=13: Double Strike — 10% chance to hit twice
            if (perks.isUnlocked(Perk.byId(13)) && attacker.getRandom().nextFloat() < 0.10f) {
                if (event.getEntity() instanceof LivingEntity target) {
                    target.hurt(attacker.damageSources().mobAttack(attacker), dmg * 0.5f);
                }
            }

            // BRUTE_MASTERY id=4: Colossus — stun targets below 50% HP
            if (perks.isUnlocked(Perk.byId(4)) && event.getEntity() instanceof LivingEntity target) {
                if (target.getHealth() < target.getMaxHealth() * 0.5f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
                }
            }

            // PRECI_CORE id=36: Steady Aim — +5% ranged damage
            if (perks.isUnlocked(Perk.byId(36))) {
                dmg *= 1.05f;
            }

            // PRECI_MASTERY id=40: Deadshot — headshot multiplier +50%
            if (perks.isUnlocked(Perk.byId(40)) && event.getEntity() instanceof LivingEntity target) {
                if (attacker.getEyeY() - target.getEyeY() > 1.0) {
                    dmg *= 1.5f;
                }
            }

            // PRECI_TRANSCENDENCE id=41: True Strike — ignore armor
            if (perks.isUnlocked(Perk.byId(41)) && event.getEntity() instanceof LivingEntity target) {
                AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
                if (armor != null) {
                    savedArmorBase.put(target.getUUID(), armor.getBaseValue());
                    armor.setBaseValue(0);
                }
            }

            // INTIM_SITUATIONAL id=75: Mark of Fear — marked targets take +25%
            if (perks.isUnlocked(Perk.byId(75))
                    && PerkState.isTrackedTarget(uuid, event.getEntity().getId())) {
                dmg *= 1.25f;
            }

            // PRECI_SITUATIONAL id=39: Critical Eye — +15% dmg at max health
            if (perks.isUnlocked(Perk.byId(39))
                    && attacker.getHealth() >= attacker.getMaxHealth()) {
                dmg *= 1.15f;
            }

            // BLADE_TRANSCENDENCE id=11: One With the Blade — bonus dmg for 5s after kill
            if (perks.isUnlocked(Perk.byId(11))
                    && !PerkState.isOnCooldown(uuid, 11, 5000L)) {
                dmg *= 1.5f;
            }

            // TRACK_SYNERGY id=44: Pack Hunter — extra damage near allies
            if (perks.isUnlocked(Perk.byId(44))) {
                double nearbyAllies = attacker.level().getEntitiesOfClass(Player.class,
                        attacker.getBoundingBox().inflate(10),
                        p -> p != attacker && p.isAlive()).size();
                if (nearbyAllies > 0) dmg *= 1.0f + nearbyAllies * 0.05f;
            }

            // COOK_SITUATIONAL id=63: Fast Food effect handled elsewhere (can't modify eat speed in damage)
            // RAPID_FLURRY handled in tick
        }

        // Victim side
        if (event.getEntity() instanceof Player victim) {
            PerkManager perks = managerFor(victim);
            UUID uuid = victim.getUUID();

            if (perks.isUnlocked(Perk.byId(24))) dmg *= 0.95f;

            // RESIST_SITUATIONAL id=27: Last Stand
            if (perks.isUnlocked(Perk.byId(27)) && victim.getHealth() < victim.getMaxHealth() * 0.2f) {
                dmg *= 0.7f;
            }

            // WILL_SITUATIONAL id=81: Last Breath
            if (perks.isUnlocked(Perk.byId(81))
                    && !PerkState.isOnCooldown(uuid, 81, 30_000L)
                    && victim.getHealth() - dmg <= 0) {
                dmg = Math.max(0f, victim.getHealth() - 1f);
                PerkState.setCooldown(uuid, 81, 30_000L);
            }

            // RESIST_TRANSCENDENCE id=29: Immortal
            if (perks.isUnlocked(Perk.byId(29))
                    && !PerkState.isOnCooldown(uuid, 29, 30_000L)
                    && victim.getHealth() - dmg <= 0) {
                dmg = Math.max(0f, victim.getHealth() - 1f);
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, false));
                PerkState.setCooldown(uuid, 29, 30_000L);
            }

            // RESIST_MASTERY id=28: Diamond Skin
            if (perks.isUnlocked(Perk.byId(28))
                    && !PerkState.isOnCooldown(uuid, 28, 5_000L)) {
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false));
                PerkState.setCooldown(uuid, 28, 5_000L);
            }

            // RESIST_ACTIVE id=25: Iron Guard — blocking reduces 25%
            if (perks.isUnlocked(Perk.byId(25)) && victim.isBlocking()) {
                dmg *= 0.75f;
            }

            // SENSE_CORE id=48: Sixth Sense — 5% dodge chance
            if (perks.isUnlocked(Perk.byId(48)) && victim.getRandom().nextFloat() < 0.05f) {
                dmg = 0f;
            }

            // AGIL_SITUATIONAL id=21: Evasion — 20% dodge
            if (perks.isUnlocked(Perk.byId(21)) && victim.getRandom().nextFloat() < 0.20f) {
                dmg = 0f;
                PerkState.setLastDodge(uuid);
            }

            // AGIL_TRANSCENDENCE id=23: Untouchable — 100% dodge for 3s after damage
            if (perks.isUnlocked(Perk.byId(23))
                    && !PerkState.isOnCooldown(uuid, 23, 3000L)
                    && PerkState.getLastDodgeTime(uuid) > 0
                    && System.currentTimeMillis() - PerkState.getLastDodgeTime(uuid) < 3000) {
                dmg = 0f;
            }

            // SENSE_MASTERY id=52: Foresight — dodge once per 10s
            if (perks.isUnlocked(Perk.byId(52))
                    && !PerkState.isOnCooldown(uuid, 52, 10_000L)) {
                dmg = 0f;
                PerkState.setCooldown(uuid, 52, 10_000L);
            }

            // WILL_ACTIVE id=79: Focused Mind — resist knockback when blocking
            if (perks.isUnlocked(Perk.byId(79)) && victim.isBlocking()) {
                AttributeInstance kb = victim.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null) {
                    kb.addTransientModifier(new AttributeModifier(FOCUSED_KB_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
                }
            }

            // WILL_SYNERGY id=80: Unbreakable — damage resistance with shield
            if (perks.isUnlocked(Perk.byId(80)) && victim.isBlocking()) {
                dmg *= 0.85f;
            }

            // BLADE_SITUATIONAL id=9: Parry Master — reflect 50% when blocking
            if (perks.isUnlocked(Perk.byId(9)) && victim.isBlocking()
                    && event.getSource().getEntity() instanceof LivingEntity attacker) {
                attacker.hurt(victim.damageSources().mobAttack(victim), dmg * 0.5f);
            }

            // WILL_MASTERY id=82: Indomitable — debuffs become buffs at low HP
            if (perks.isUnlocked(Perk.byId(82))
                    && victim.getHealth() < victim.getMaxHealth() * 0.2f) {
                if (victim.hasEffect(MobEffects.WEAKNESS)) {
                    victim.removeEffect(MobEffects.WEAKNESS);
                    victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false));
                }
                if (victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
                }
                if (victim.hasEffect(MobEffects.HUNGER)) {
                    victim.removeEffect(MobEffects.HUNGER);
                    victim.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, false, false));
                }
            }

            // Track the attacker for Intim/Core tracking
            if (event.getSource().getEntity() instanceof LivingEntity source) {
                PerkState.noteTrackedHit(uuid, source.getId());
                PerkState.addTrackedTarget(uuid, source.getId());
            }
        }

        if (Math.abs(dmg - event.getNewDamage()) > 0.001f) {
            event.setNewDamage(Math.max(0f, dmg));
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        UUID uuid = target.getUUID();

        Double saved = savedArmorBase.remove(uuid);
        if (saved != null) {
            AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(saved);
        }

        AttributeInstance kb = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.removeModifier(FOCUSED_KB_ID);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;

            PerkManager perks = managerFor(player);
            UUID uuid = player.getUUID();
            PerkState.recordKill(uuid);

            // ENDUR_ACTIVE id=31: Second Wind
            if (perks.isUnlocked(Perk.byId(31))) {
                player.setAbsorptionAmount(Math.min(20f, player.getAbsorptionAmount() + 4f));
            }

            // ENDUR_SITUATIONAL id=33: Adrenaline
            if (perks.isUnlocked(Perk.byId(33))) {
                FoodData food = player.getFoodData();
                food.setFoodLevel(Math.min(20, food.getFoodLevel() + 2));
                food.setSaturation(Math.min(20f, food.getSaturationLevel() + 2f));
            }

            // BLADE_MASTERY id=10: Blade Storm AOE
            if (perks.isUnlocked(Perk.byId(10))) {
                player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(4),
                        e -> e != player && e.isAlive() && e != event.getEntity())
                        .forEach(e -> e.hurt(player.damageSources().mobAttack(player), 4f));
            }

            // BRUTE_TRANSCENDENCE id=5: Titan's Wrath — enemies explode on kill
            if (perks.isUnlocked(Perk.byId(5)) && event.getEntity() != null) {
                player.level().explode(null,
                        event.getEntity().getX(),
                        event.getEntity().getY(),
                        event.getEntity().getZ(),
                        2f, false, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
            }

            // BLADE_TRANSCENDENCE id=11: One With the Blade — all crit for 5s
            if (perks.isUnlocked(Perk.byId(11))) {
                PerkState.setCooldown(uuid, 11, 5000L);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false));
            }

            // RAPID_SYNERGY id=14: Blinding Speed — kills grant speed
            if (perks.isUnlocked(Perk.byId(14))) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
            }

            // INTIM_MASTERY id=76: Dread Lord — kills cause nearby mobs to flee
            if (perks.isUnlocked(Perk.byId(76))) {
                player.level().getEntitiesOfClass(Mob.class,
                        player.getBoundingBox().inflate(16),
                        e -> e.isAlive())
                        .forEach(e -> {
                            e.setTarget(null);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false));
                        });
            }

            // INTIM_TRANSCENDENCE id=77: Absolute Dominion — control one mob
            if (perks.isUnlocked(Perk.byId(77))) {
                player.level().getEntitiesOfClass(Mob.class,
                        player.getBoundingBox().inflate(8),
                        e -> e.isAlive() && e != event.getEntity())
                        .stream().findFirst().ifPresent(mob -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false));
                            mob.setTarget(null);
                        });
            }

            // RAPID_SITUATIONAL id=15: Flurry — frenzy resets on kill, give stacks
            if (perks.isUnlocked(Perk.byId(15))) {
                PerkState.addFrenzyStack(uuid);
            }

            // SENSE_ACTIVE id=49: Treasure Hunter — double XP (handled in onLivingExperienceDrop)
            // COOK_MASTERY id=64: Feast — share food effect (handled via potion clouds)
            if (perks.isUnlocked(Perk.byId(64))) {
                player.level().getEntitiesOfClass(Player.class,
                        player.getBoundingBox().inflate(8),
                        p -> p != player && p.isAlive())
                        .forEach(p -> {
                            p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, false, false));
                            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                        });
            }
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() != null) {
            Player player = event.getAttackingPlayer();
            PerkManager perks = managerFor(player);

            // SENSE_ACTIVE id=49: Treasure Hunter — double XP
            if (perks.isUnlocked(Perk.byId(49))) {
                event.setDroppedExperience(event.getDroppedExperience() * 2);
            }
        }
    }



    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        PerkState.clearPlayer(uuid);
        savedArmorBase.remove(uuid);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            PerkManager perks = managerFor(player);

            // AGIL_MASTERY id=22: Spectral Step — no fall damage on dodge
            if (perks.isUnlocked(Perk.byId(22))) {
                long lastDodge = PerkState.getLastDodgeTime(player.getUUID());
                if (System.currentTimeMillis() - lastDodge < 2000) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private static void applyMovementSpeedModifier(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(SPEED_MOD_ID);
        attr.addTransientModifier(new AttributeModifier(
                SPEED_MOD_ID, multiplier - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeMovementSpeedModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(SPEED_MOD_ID);
    }

    private static void applyAttackSpeedModifier(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;
        attr.removeModifier(ATTACK_SPEED_MOD_ID);
        attr.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_MOD_ID, multiplier - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeAttackSpeedModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) attr.removeModifier(ATTACK_SPEED_MOD_ID);
    }
}

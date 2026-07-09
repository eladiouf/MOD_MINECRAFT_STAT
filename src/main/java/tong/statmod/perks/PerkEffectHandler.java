package tong.statmod.perks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.stats.CraftingSupportEffectHandler;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Map;
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

        // BLADE_ACTIVE id=7: Flowing Strike — combo hits are recorded from real damage events.

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

        // COOK_ACTIVE id=61: Iron Stomach — handled by CraftingSupportEffectHandler on effect application.

        // WILL_CORE id=78: Iron Will — reduce negative effect duration
        // Handled via onLivingDamagePre and potion effect application

        // BLADE_SYNERGY id=8: Dance of Blades — combo hits grant speed
        if (perks.isUnlocked(Perk.BLADE_SYNERGY)
                && PerkCombatScaling.canUseWeaponFamilyPerk(
                        WeaponResolver.statFor(player.getMainHandItem()), Perk.BLADE_SYNERGY)) {
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

        // COOK_TRANSCENDENCE id=65: Ambrosia — handled when food is eaten.

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
        if (perks.isUnlocked(Perk.RAPID_SITUATIONAL)
                && PerkCombatScaling.canUseWeaponFamilyPerk(
                        WeaponResolver.statFor(player.getMainHandItem()), Perk.RAPID_SITUATIONAL)) {
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

        // SENSE_SYNERGY id=50: Predator's Instinct — mark hidden mobs.
        if (perks.isUnlocked(Perk.SENSE_SYNERGY)) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(24),
                    e -> e.isAlive() && (e.isInvisible() || !player.hasLineOfSight(e)))
                    .forEach(e -> {
                        int revealTicks = PerkPerceptionScaling.hiddenMobRevealDurationTicks(true, true);
                        if (revealTicks > 0) {
                            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, revealTicks, 0, false, false));
                        }
                    });
        }

        // SENSE_TRANSCENDENCE id=53: Omniscience — actionbar health readout for nearest entity.
        if (perks.isUnlocked(Perk.SENSE_TRANSCENDENCE)) {
            showSenseHealthReadout(player);
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
            StatType weaponStat = WeaponResolver.statFor(attacker.getMainHandItem());

            dmg *= PerkCombatScaling.coreWeaponDamageMultiplier(
                    weaponStat,
                    perks.isUnlocked(Perk.BRUTE_CORE),
                    perks.isUnlocked(Perk.BLADE_CORE),
                    perks.isUnlocked(Perk.PRECI_CORE));
            dmg *= PerkCombatScaling.bruteTranscendenceDamageMultiplier(
                    weaponStat,
                    perks.isUnlocked(Perk.BRUTE_TRANSCENDENCE));
            if (perks.isUnlocked(Perk.byId(72))
                    && PerkState.isTrackedTarget(uuid, event.getEntity().getId())) {
                dmg *= 1.05f;
            }
            dmg *= CraftingSupportEffectHandler.sharpeningDamageMultiplier(
                    perks.isUnlocked(Perk.FORGE_SITUATIONAL),
                    attacker.getMainHandItem().isDamageableItem()
                            && PerkState.isOnCooldown(uuid, Perk.FORGE_SITUATIONAL.id, 30_000L));

            // TRACK_SITUATIONAL id=45: Sillage — hitting mobs leaves a scent trail.
            if (event.getEntity() instanceof Mob target
                    && perks.isUnlocked(Perk.TRACK_SITUATIONAL)) {
                int scentTicks = PerkPerceptionScaling.scentTrailDurationTicks(true, target.isAlive());
                if (scentTicks > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, scentTicks, 0, false, false));
                    PerkState.addTrackedTarget(uuid, target.getId());
                }
            }

            // BRUTE_SITUATIONAL id=3: Berserker — +20% below 30% HP
            if (perks.isUnlocked(Perk.BRUTE_SITUATIONAL)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_SITUATIONAL)
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.3f) {
                dmg *= 1.20f;
            }

            // BRUTE_SYNERGY id=2: Crushing Force — +15% below 50% HP
            if (perks.isUnlocked(Perk.BRUTE_SYNERGY)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_SYNERGY)
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.5f) {
                dmg *= 1.15f;
            }

            // BRUTE_ACTIVE id=1: Mighty Swing — charged attack +10%
            if (perks.isUnlocked(Perk.BRUTE_ACTIVE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_ACTIVE)
                    && attacker.isAutoSpinAttack()) {
                dmg *= 1.10f;
            }

            // BLADE_ACTIVE id=7: Flowing Strike — every 3rd hit double damage
            if (perks.isUnlocked(Perk.BLADE_ACTIVE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BLADE_ACTIVE)) {
                PerkState.recordComboHit(uuid, System.currentTimeMillis(), 5000);
                if (PerkState.getComboCount(uuid, System.currentTimeMillis(), 5000) >= 3) {
                    dmg *= 1.50f;
                    PerkState.setCooldown(uuid, 7);
                }
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
            if (perks.isUnlocked(Perk.RAPID_ACTIVE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.RAPID_ACTIVE)
                    && attacker.getRandom().nextFloat() < 0.10f) {
                if (event.getEntity() instanceof LivingEntity target) {
                    target.hurt(attacker.damageSources().mobAttack(attacker), dmg * 0.5f);
                }
            }

            // BRUTE_MASTERY id=4: Colossus — stun targets below 50% HP
            if (perks.isUnlocked(Perk.BRUTE_MASTERY)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BRUTE_MASTERY)
                    && event.getEntity() instanceof LivingEntity target) {
                if (target.getHealth() < target.getMaxHealth() * 0.5f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
                }
            }

            // PRECI_MASTERY id=40: Deadshot — headshot multiplier +50%
            if (perks.isUnlocked(Perk.PRECI_MASTERY)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_MASTERY)
                    && event.getEntity() instanceof LivingEntity target) {
                if (attacker.getEyeY() - target.getEyeY() > 1.0) {
                    dmg *= 1.5f;
                }
            }

            // PRECI_TRANSCENDENCE id=41: True Strike — ignore armor
            if (perks.isUnlocked(Perk.PRECI_TRANSCENDENCE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_TRANSCENDENCE)
                    && event.getEntity() instanceof LivingEntity target) {
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
            if (perks.isUnlocked(Perk.PRECI_SITUATIONAL)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.PRECI_SITUATIONAL)
                    && attacker.getHealth() >= attacker.getMaxHealth()) {
                dmg *= 1.15f;
            }

            boolean projectileDamage = event.getSource().getDirectEntity() instanceof Projectile;
            boolean markedProjectileTarget = projectileDamage
                    && (event.getEntity().hasEffect(MobEffects.GLOWING)
                    || PerkState.isTrackedTarget(uuid, event.getEntity().getId()));
            dmg *= PerkCombatScaling.precisionMarkedProjectileDamageMultiplier(
                    weaponStat,
                    perks.isUnlocked(Perk.PRECI_SYNERGY),
                    projectileDamage,
                    markedProjectileTarget);

            // BLADE_TRANSCENDENCE id=11: One With the Blade — bonus dmg for 5s after kill
            dmg *= PerkCombatScaling.bladePostKillDamageMultiplier(
                    weaponStat,
                    perks.isUnlocked(Perk.BLADE_TRANSCENDENCE),
                    PerkState.isOnCooldown(uuid, Perk.BLADE_TRANSCENDENCE.id, 5000L));

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
            StatType victimWeaponStat = WeaponResolver.statFor(victim.getMainHandItem());
            boolean dodgeTriggered = false;
            boolean perfectDodgeTriggered = false;

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
            if (dmg > 0.0f && perks.isUnlocked(Perk.byId(48)) && victim.getRandom().nextFloat() < 0.05f) {
                dmg = 0f;
                dodgeTriggered = true;
                perfectDodgeTriggered = true;
                PerkState.setLastDodge(uuid);
            }

            // AGIL_SITUATIONAL id=21: Evasion — 20% dodge
            if (dmg > 0.0f && perks.isUnlocked(Perk.byId(21)) && victim.getRandom().nextFloat() < 0.20f) {
                dmg = 0f;
                PerkState.setLastDodge(uuid);
                dodgeTriggered = true;
                perfectDodgeTriggered = true;
            }

            // AGIL_TRANSCENDENCE id=23: Untouchable — 100% dodge for 3s after taking damage.
            if (dmg > 0.0f && PerkMobilityScaling.agilityUntouchableDodgesIncomingHit(
                    perks.isUnlocked(Perk.AGIL_TRANSCENDENCE),
                    PerkState.isOnCooldown(uuid, Perk.AGIL_TRANSCENDENCE.id, 3000L))) {
                dmg = 0f;
                PerkState.setLastDodge(uuid);
                dodgeTriggered = true;
                perfectDodgeTriggered = true;
            }

            // SENSE_MASTERY id=52: Foresight — dodge once per 10s
            if (dmg > 0.0f
                    && perks.isUnlocked(Perk.byId(52))
                    && !PerkState.isOnCooldown(uuid, 52, 10_000L)) {
                dmg = 0f;
                PerkState.setCooldown(uuid, 52, 10_000L);
                PerkState.setLastDodge(uuid);
                dodgeTriggered = true;
                perfectDodgeTriggered = true;
            }

            if (dodgeTriggered) {
                applyRapidDodgeEffects(victim, perks, perfectDodgeTriggered);
            }

            if (PerkMobilityScaling.agilityUntouchableStartsWindow(
                    perks.isUnlocked(Perk.AGIL_TRANSCENDENCE), dmg)) {
                PerkState.setCooldown(uuid, Perk.AGIL_TRANSCENDENCE.id, 3000L);
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
            if (perks.isUnlocked(Perk.BLADE_SITUATIONAL)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(victimWeaponStat, Perk.BLADE_SITUATIONAL)
                    && victim.isBlocking()
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
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || arrow.level().isClientSide
                || !(arrow.getOwner() instanceof Player owner)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity primary)) {
            return;
        }

        ItemStack weapon = arrow.getWeaponItem();
        StatType weaponStat = weapon == null || weapon.isEmpty()
                ? WeaponResolver.statFor(owner.getMainHandItem())
                : WeaponResolver.statFor(weapon);
        PerkManager perks = managerFor(owner);
        if (!PerkCombatScaling.canPierceSecondaryTarget(
                weaponStat, perks.isUnlocked(Perk.PRECI_ACTIVE), true)) {
            return;
        }

        LivingEntity secondary = findPiercingSecondaryTarget(arrow, owner, primary, hit.getLocation());
        if (secondary == null) {
            return;
        }

        float damage = Math.max(1.0f, (float) arrow.getBaseDamage());
        secondary.hurt(arrow.damageSources().arrow(arrow, owner), damage);
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
            StatType killWeaponStat = WeaponResolver.statFor(player.getMainHandItem());
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
            if (perks.isUnlocked(Perk.BLADE_MASTERY)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.BLADE_MASTERY)) {
                player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(4),
                        e -> e != player && e.isAlive() && e != event.getEntity())
                        .forEach(e -> e.hurt(player.damageSources().mobAttack(player), 4f));
            }

            // BRUTE_TRANSCENDENCE id=5: Titan's Wrath — enemies explode on kill
            if (perks.isUnlocked(Perk.BRUTE_TRANSCENDENCE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.BRUTE_TRANSCENDENCE)
                    && event.getEntity() != null) {
                player.level().explode(null,
                        event.getEntity().getX(),
                        event.getEntity().getY(),
                        event.getEntity().getZ(),
                        2f, false, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
            }

            // BLADE_TRANSCENDENCE id=11: One With the Blade — all crit for 5s
            if (perks.isUnlocked(Perk.BLADE_TRANSCENDENCE)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.BLADE_TRANSCENDENCE)) {
                PerkState.setCooldown(uuid, 11, 5000L);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false));
            }

            // RAPID_SYNERGY id=14: Blinding Speed — kills grant speed
            if (perks.isUnlocked(Perk.RAPID_SYNERGY)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.RAPID_SYNERGY)) {
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
            if (perks.isUnlocked(Perk.RAPID_SITUATIONAL)
                    && PerkCombatScaling.canUseWeaponFamilyPerk(killWeaponStat, Perk.RAPID_SITUATIONAL)) {
                PerkState.addFrenzyStack(uuid);
            }

            // SENSE_ACTIVE id=49: Treasure Hunter — double XP (handled in onLivingExperienceDrop)
            // COOK_MASTERY id=64: Feast — handled when food is eaten.
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
    public static void onPlayerClone(PlayerEvent.Clone event) {
        UUID uuid = event.getEntity().getUUID();
        PerkState.clearPlayer(uuid);
        savedArmorBase.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
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

    private static void applyRapidDodgeEffects(Player player, PerkManager perks, boolean perfectDodgeTriggered) {
        int slowTicks = PerkMobilityScaling.rapidDodgeSlowdownDurationTicks(
                perks.isUnlocked(Perk.RAPID_MASTERY), perfectDodgeTriggered);
        if (slowTicks > 0) {
            player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(10),
                    LivingEntity::isAlive)
                    .forEach(mob -> mob.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1, false, false)));
        }

        UUID uuid = player.getUUID();
        boolean stopReady = perks.isUnlocked(Perk.RAPID_TRANSCENDENCE)
                && !PerkState.isOnCooldown(uuid, Perk.RAPID_TRANSCENDENCE.id, 10_000L);
        int stopTicks = PerkMobilityScaling.rapidPerfectDodgeStopDurationTicks(stopReady, perfectDodgeTriggered);
        if (stopTicks <= 0) {
            return;
        }

        player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(12),
                LivingEntity::isAlive)
                .forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stopTicks, 9, false, false));
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stopTicks, 4, false, false));
                });
        PerkState.setCooldown(uuid, Perk.RAPID_TRANSCENDENCE.id, 10_000L);
    }

    private static void showSenseHealthReadout(Player player) {
        player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(32),
                e -> e != player && e.isAlive())
                .stream()
                .min((left, right) -> Double.compare(player.distanceToSqr(left), player.distanceToSqr(right)))
                .ifPresent(target -> {
                    if (!PerkPerceptionScaling.canRevealHealthReadout(true, target.isAlive())) {
                        return;
                    }
                    String message = String.format(java.util.Locale.ROOT,
                            "%s: %.1f/%.1f HP",
                            target.getDisplayName().getString(),
                            target.getHealth(),
                            target.getMaxHealth());
                    player.displayClientMessage(Component.literal(message), true);
                });
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

    private static LivingEntity findPiercingSecondaryTarget(AbstractArrow arrow,
                                                            Player owner,
                                                            LivingEntity primary,
                                                            Vec3 impactLocation) {
        Vec3 direction = arrow.getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-6) {
            return null;
        }
        direction = direction.normalize();
        Vec3 end = impactLocation.add(direction.scale(5.0d));
        AABB searchBox = new AABB(impactLocation, end).inflate(1.25d);
        LivingEntity best = null;
        double bestProjection = Double.MAX_VALUE;

        for (LivingEntity candidate : arrow.level().getEntitiesOfClass(LivingEntity.class, searchBox, LivingEntity::isAlive)) {
            if (candidate == primary || candidate == owner || candidate == arrow.getOwner()) {
                continue;
            }
            double projection = candidate.position()
                    .add(0.0d, candidate.getBbHeight() * 0.5d, 0.0d)
                    .subtract(impactLocation)
                    .dot(direction);
            if (projection <= 0.25d || projection > 5.0d) {
                continue;
            }
            Vec3 closestPoint = impactLocation.add(direction.scale(projection));
            double distanceSqr = candidate.position()
                    .add(0.0d, candidate.getBbHeight() * 0.5d, 0.0d)
                    .distanceToSqr(closestPoint);
            if (distanceSqr <= 1.75d && projection < bestProjection) {
                best = candidate;
                bestProjection = projection;
            }
        }

        return best;
    }
}

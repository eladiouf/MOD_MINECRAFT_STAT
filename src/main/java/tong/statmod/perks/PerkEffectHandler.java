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

        // ── COMPLÉTION DES PERKS MAGIQUES & ÉLÉMENTAIRES (TICK EFFECTS) ──

        // WATER_CORE (id 90) : Soothing Current — boost regen under water/rain
        if (perks.isUnlocked(Perk.byId(90))) {
            if (player.isInWater() || player.level().isRainingAt(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
            }
        }

        // WATER_TRANSCENDENCE (id 95) : Abyssal Grace — resistance in water/rain
        if (perks.isUnlocked(Perk.byId(95))) {
            if (player.isInWater() || player.level().isRainingAt(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
            }
        }

        // EARTH_CORE (id 96) : Stone Skin — +2 base armor
        if (perks.isUnlocked(Perk.byId(96))) {
            applyArmorModifier(player, 2.0);
        } else {
            removeArmorModifier(player);
        }

        // EARTH_MASTERY (id 100) : World Anchor — damage boost and knockback resistance when still
        if (perks.isUnlocked(Perk.byId(100))) {
            if (player.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
                applyEarthKbResistModifier(player, 1.0);
            } else {
                removeEarthKbResistModifier(player);
            }
        } else {
            removeEarthKbResistModifier(player);
        }

        // EARTH_TRANSCENDENCE (id 101) : Mountain Throne — Force I under Y=40
        if (perks.isUnlocked(Perk.byId(101)) && player.blockPosition().getY() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
        }

        // FIRE_SYNERGY (id 104) : Accelerant — speed & dig speed when burning/in lava
        if (perks.isUnlocked(Perk.byId(104))) {
            boolean inFire = player.getRemainingFireTicks() > 0 || player.isInLava() || player.level().getBlockState(player.blockPosition()).is(net.minecraft.world.level.block.Blocks.FIRE);
            if (inFire) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false));
            }
        }

        // FIRE_TRANSCENDENCE (id 107) : Solar Cataclysm — Fire resistance permanent
        if (perks.isUnlocked(Perk.byId(107))) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        }

        // AIR_CORE (id 108) : Tailwind — +10% speed
        if (perks.isUnlocked(Perk.byId(108))) {
            applyAirSpeedModifier(player, 1.10);
        } else {
            removeAirSpeedModifier(player);
        }

        // AIR_TRANSCENDENCE (id 113) : Tempest Crown — clear levitation, Speed III
        if (perks.isUnlocked(Perk.byId(113))) {
            if (player.hasEffect(MobEffects.LEVITATION)) {
                player.removeEffect(MobEffects.LEVITATION);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
        }

        // MANA_POOL_CORE (id 126) : Deep Wells — min absorption 8.0 (4 hearts)
        if (perks.isUnlocked(Perk.byId(126)) && player.getAbsorptionAmount() < 8f) {
            player.setAbsorptionAmount(8f);
        }

        // MANA_POOL_SYNERGY (id 128) : Disciplined Reserve — regen if erudition >= 30
        if (perks.isUnlocked(Perk.byId(128))) {
            int erudition = tong.statmod.integration.RaceEffectApplier.getEffectiveLevel(player, StatType.ERUDITION.index);
            if (erudition >= 30) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
            }
        }

        // ── TICK EFFECTS DE PERKS HYBRIDES ──

        // NINJA (id 140) : Invisibility when sneaking
        if (perks.isUnlocked(Perk.byId(140))) {
            if (player.isCrouching()) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            }
        }

        // STORM_LORD (id 144) : Conduit Power under rain or in water
        if (perks.isUnlocked(Perk.byId(144))) {
            if (player.isInWater() || player.level().isRainingAt(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 40, 0, false, false));
            }
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

            // ── PASSIVES ELEMENTAIRES DE L'ATTAQUANT ──

            // WATER_MASTERY (id 94) : Tidal Control — slow target
            if (perks.isUnlocked(Perk.byId(94)) && event.getEntity() instanceof LivingEntity target) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
            }

            // FIRE_CORE (id 102) : Kindling — increase burn duration
            if (perks.isUnlocked(Perk.byId(102)) && event.getEntity() instanceof LivingEntity target) {
                if (target.getRemainingFireTicks() > 0) {
                    target.setRemainingFireTicks(target.getRemainingFireTicks() + 40);
                }
            }

            // FIRE_ACTIVE (id 103) : Flashburn — ignite non-burning targets and deal +15% damage
            if (perks.isUnlocked(Perk.byId(103)) && event.getEntity() instanceof LivingEntity target) {
                if (target.getRemainingFireTicks() <= 0) {
                    target.setRemainingFireTicks(80);
                    dmg *= 1.15f;
                }
            }

            // FIRE_SITUATIONAL (id 105) : Execution Flame — explosion chance on low health burning targets
            if (perks.isUnlocked(Perk.byId(105)) && event.getEntity() instanceof LivingEntity target) {
                if (target.getRemainingFireTicks() > 0 && target.getHealth() < target.getMaxHealth() * 0.4f) {
                    if (attacker.getRandom().nextFloat() < 0.15f) {
                        attacker.level().explode(attacker, target.getX(), target.getY(), target.getZ(), 1.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                        dmg += 5.0f;
                    }
                }
            }

            // FIRE_TRANSCENDENCE (id 107) : Solar Cataclysm — permanent fire damage and ignite
            if (perks.isUnlocked(Perk.byId(107)) && event.getEntity() instanceof LivingEntity target) {
                target.setRemainingFireTicks(60);
                dmg += 2.0f;
            }

            // AIR_SYNERGY (id 110) : Sky Dancer — bonus damage in mid-air
            if (perks.isUnlocked(Perk.byId(110)) && !attacker.onGround() && attacker.fallDistance > 0.05f) {
                dmg *= 1.20f;
            }

            // ── COMPLÉTION DES PERKS HYBRIDES DE L'ATTAQUANT (DAMAGE EFFECTS) ──

            // SPELLSWORD (id 138) : +15% magic damage and +10% lifesteal absorption with blades
            if (perks.isUnlocked(Perk.byId(138)) && PerkCombatScaling.canUseWeaponFamilyPerk(weaponStat, Perk.BLADE_CORE)) {
                dmg *= 1.15f;
                attacker.setAbsorptionAmount(Math.min(40f, attacker.getAbsorptionAmount() + dmg * 0.10f));
            }

            // NINJA (id 140) : x2.0 critical damage when sneaking/invisible
            if (perks.isUnlocked(Perk.byId(140)) && (attacker.isCrouching() || attacker.hasEffect(MobEffects.INVISIBILITY))) {
                dmg *= 2.0f;
            }

            // BATTLEMAGE (id 141) : +20% damage and +1 absorption on hit
            if (perks.isUnlocked(Perk.byId(141))) {
                dmg *= 1.20f;
                attacker.setAbsorptionAmount(Math.min(40f, attacker.getAbsorptionAmount() + 1.0f));
            }

            // ALCHEMICAL_ARCHER (id 142) : apply random debuffs with projectiles
            if (perks.isUnlocked(Perk.byId(142))
                    && event.getSource().getDirectEntity() instanceof Projectile
                    && event.getEntity() instanceof LivingEntity target) {
                int rand = attacker.getRandom().nextInt(4);
                net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect = switch (rand) {
                    case 0 -> MobEffects.POISON;
                    case 1 -> MobEffects.MOVEMENT_SLOWDOWN;
                    case 2 -> MobEffects.WEAKNESS;
                    default -> MobEffects.WITHER;
                };
                target.addEffect(new MobEffectInstance(effect, 100, 0, false, false));
            }

            // DEMOLITIONIST (id 143) : +40% explosion damage
            if (perks.isUnlocked(Perk.byId(143)) && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)) {
                dmg *= 1.40f;
            }

            // STORM_LORD (id 144) : +6 lightning damage under rain
            if (perks.isUnlocked(Perk.byId(144))
                    && (attacker.isInWater() || attacker.level().isRainingAt(attacker.blockPosition()))) {
                dmg += 6.0f;
            }

            // AVATAR_OF_ELEMENTS (id 147) : +20% global damage
            if (perks.isUnlocked(Perk.byId(147))) {
                dmg *= 1.20f;
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

            // ── PASSIVES ELEMENTAIRES ET MAGIQUES DE LA VICTIME ──

            // WATER_ACTIVE (id 91) : Healing Surge — chance of Regen II when hit
            if (perks.isUnlocked(Perk.byId(91)) && victim.getRandom().nextFloat() < 0.15f) {
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false));
            }

            // WATER_SITUATIONAL (id 93) : Cold Veil — Resistance II at low HP
            if (perks.isUnlocked(Perk.byId(93))
                    && victim.getHealth() < victim.getMaxHealth() * 0.3f
                    && !PerkState.isOnCooldown(uuid, 93, 45_000L)) {
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, false));
                PerkState.setCooldown(uuid, 93, 45_000L);
            }

            // EARTH_ACTIVE (id 97) : Earthen Rampart — chance of gaining absorption when hit
            if (perks.isUnlocked(Perk.byId(97)) && victim.getRandom().nextFloat() < 0.10f) {
                victim.setAbsorptionAmount(Math.min(victim.getAbsorptionAmount() + 4.0f, 40.0f));
            }

            // EARTH_SYNERGY (id 98) : Runic Bedrock — reduce magic damage when on ground
            boolean isMagicDmg = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                    || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
            if (perks.isUnlocked(Perk.byId(98)) && victim.onGround() && isMagicDmg) {
                dmg *= 0.80f;
            }

            // MAGIC_RESIST_CORE (id 114) : Warding Skin — -10% magic damage
            if (perks.isUnlocked(Perk.byId(114)) && isMagicDmg) {
                dmg *= 0.90f;
            }

            // MAGIC_RESIST_ACTIVE (id 115) : Spell Shear — gain haste when hit by magic
            if (perks.isUnlocked(Perk.byId(115)) && isMagicDmg) {
                victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 0, false, false));
            }

            // MAGIC_RESIST_SYNERGY (id 116) : Unbroken Ward — -20% magic damage when blocking
            if (perks.isUnlocked(Perk.byId(116)) && victim.isBlocking() && isMagicDmg) {
                dmg *= 0.80f;
            }

            // MAGIC_RESIST_SITUATIONAL (id 117) : Countercurrent — -20% magic damage when absorption is 0
            if (perks.isUnlocked(Perk.byId(117)) && victim.getAbsorptionAmount() <= 0.0f && isMagicDmg) {
                dmg *= 0.80f;
            }

            // MAGIC_RESIST_TRANSCENDENCE (id 119) : Aegis Absolute — magic damage immunity when low health
            if (perks.isUnlocked(Perk.byId(119))
                    && victim.getHealth() < victim.getMaxHealth() * 0.3f
                    && isMagicDmg
                    && !PerkState.isOnCooldown(uuid, 119, 60_000L)) {
                dmg = 0f;
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
                PerkState.setCooldown(uuid, 119, 60_000L);
            }

            // MANA_POOL_SITUATIONAL (id 129) : Last Reservoir — speed when absorption drops to 0
            if (perks.isUnlocked(Perk.byId(129))
                    && victim.getAbsorptionAmount() <= 0.0f
                    && !PerkState.isOnCooldown(uuid, 129, 30_000L)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
                PerkState.setCooldown(uuid, 129, 30_000L);
            }

            // ── PASSIVES HYBRIDES DE LA VICTIME ──

            // PALADIN (id 139) : Block heals allies and weakens nearby mobs
            if (perks.isUnlocked(Perk.byId(139)) && victim.isBlocking()) {
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
                victim.level().getEntitiesOfClass(Player.class,
                        victim.getBoundingBox().inflate(8),
                        p -> p != victim && p.isAlive())
                        .forEach(ally -> ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false)));
                victim.level().getEntitiesOfClass(Mob.class,
                        victim.getBoundingBox().inflate(8),
                        e -> e.isAlive() && e.getTarget() == victim)
                        .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false)));
            }

            // DEMOLITIONIST (id 143) : Immune to explosions
            if (perks.isUnlocked(Perk.byId(143)) && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)) {
                dmg = 0f;
            }

            // AVATAR_OF_ELEMENTS (id 147) : Immune to environmental damage (fire, fall, drown, suffocate, freeze)
            if (perks.isUnlocked(Perk.byId(147))) {
                boolean env = event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                        || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)
                        || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                        || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                        || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FREEZE);
                if (env) {
                    dmg = 0f;
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
        // LICH_SOUL (id 146) : Resurrect player on death (10 min cooldown)
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            PerkManager perks = managerFor(player);
            UUID uuid = player.getUUID();
            if (perks.isUnlocked(Perk.byId(146)) && !PerkState.isOnCooldown(uuid, 146, 600_000L)) {
                event.setCanceled(true); // Annuler la mort !
                player.setHealth(player.getMaxHealth() * 0.5f); // Soigner 50%
                player.setAbsorptionAmount(Math.min(40f, player.getAbsorptionAmount() + 20f)); // +10 cœurs
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                PerkState.setCooldown(uuid, 146, 600_000L); // Mettre en CD 10 minutes
                return;
            }
        }

        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;

            PerkManager perks = managerFor(player);
            UUID uuid = player.getUUID();
            StatType killWeaponStat = WeaponResolver.statFor(player.getMainHandItem());
            PerkState.recordKill(uuid);

            // FIRE_MASTERY (id 106) : Burn propagation on kill
            if (perks.isUnlocked(Perk.byId(106)) && event.getEntity() != null && event.getEntity().getRemainingFireTicks() > 0) {
                player.level().getEntitiesOfClass(Mob.class,
                        event.getEntity().getBoundingBox().inflate(5),
                        e -> e.isAlive() && e != event.getEntity())
                        .forEach(e -> e.setRemainingFireTicks(60));
            }

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

            // ERUDITION_CORE (id 132) — +25% XP
            if (perks.isUnlocked(Perk.byId(132))) {
                event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.25f));
            }

            // ERUDITION_TRANSCENDENCE (id 137) — +50% XP
            if (perks.isUnlocked(Perk.byId(137))) {
                event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.50f));
            }

            // ERUDITION_SITUATIONAL (id 135) — +50% XP if target has active effects
            if (perks.isUnlocked(Perk.byId(135)) && event.getEntity() != null) {
                boolean hasEffects = !event.getEntity().getActiveEffects().isEmpty();
                if (hasEffects) {
                    event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.50f));
                }
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

    private static final ResourceLocation EARTH_ARMOR_MOD_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "earth_armor_mod");
    private static final ResourceLocation EARTH_KB_MOD_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "earth_kb_mod");
    private static final ResourceLocation AIR_SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "air_speed_mod");

    private static void applyArmorModifier(Player player, double value) {
        AttributeInstance attr = player.getAttribute(Attributes.ARMOR);
        if (attr == null) return;
        attr.removeModifier(EARTH_ARMOR_MOD_ID);
        attr.addTransientModifier(new AttributeModifier(
                EARTH_ARMOR_MOD_ID, value,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeArmorModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ARMOR);
        if (attr != null) attr.removeModifier(EARTH_ARMOR_MOD_ID);
    }

    private static void applyEarthKbResistModifier(Player player, double value) {
        AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr == null) return;
        attr.removeModifier(EARTH_KB_MOD_ID);
        attr.addTransientModifier(new AttributeModifier(
                EARTH_KB_MOD_ID, value,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeEarthKbResistModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) attr.removeModifier(EARTH_KB_MOD_ID);
    }

    private static void applyAirSpeedModifier(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(AIR_SPEED_MOD_ID);
        attr.addTransientModifier(new AttributeModifier(
                AIR_SPEED_MOD_ID, multiplier - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeAirSpeedModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(AIR_SPEED_MOD_ID);
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

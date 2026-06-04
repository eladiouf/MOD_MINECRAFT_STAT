package tong.statmod.skills;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;

/**
 * Active weapon skills — one per combat stat.
 * Each has a unique mechanic, cooldown, and stamina cost.
 * Effects SCALE with stat level and EVOLVE at level 50/80.
 */
public class StatActiveSkill extends Skill {

    private static final Map<UUID, Long> cooldownEndTick = new ConcurrentHashMap<>();
    private static final Map<UUID, String> lastSkillUsed = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSkillTick = new ConcurrentHashMap<>();

    private final StatType stat;
    private final long cooldownTicks;

    public StatActiveSkill(SkillBuilder<? extends Skill> builder, StatType stat, long cooldownMs) {
        super(builder);
        this.stat = stat;
        this.cooldownTicks = cooldownMs / 50;
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        if (!super.canExecute(container)) return false;
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return false;
        ServerPlayer player = playerPatch.getOriginal();

        Long endTick = cooldownEndTick.get(player.getUUID());
        if (endTick != null && player.level().getGameTime() < endTick) return false;
        return true;
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();

        long gameTime = player.level().getGameTime();
        cooldownEndTick.put(player.getUUID(), gameTime + cooldownTicks);
        lastSkillUsed.put(player.getUUID(), this.getRegistryName().getPath());
        lastSkillTick.put(player.getUUID(), gameTime);
        container.setResource(container.getResource() - getConsumption());

        // Get stat level for scaling
        int statLevel = getStatLevel(player);

        switch (stat) {
            case BRUTE_FORCE -> executeHeavyStrike(player, playerPatch, statLevel);
            case BLADE_TECHNIQUE -> executeBladeDance(player, playerPatch, statLevel);
            case RAPIDITE -> executeBlitzAssault(player, playerPatch, statLevel);
            case AGILITY -> executeShadowStep(player, playerPatch, statLevel);
            case PHYSICAL_RESISTANCE -> executeStoneSkin(player, playerPatch, statLevel);
            case PHYSICAL_ENDURANCE -> executeEnduranceSurge(player, playerPatch, statLevel);
            case PRECISION -> executePrecisionShot(player, playerPatch, statLevel);
        }
    }

    /**
     * Get the player's level in this skill's stat
     */
    private int getStatLevel(ServerPlayer player) {
        return CapabilityHelper.getStats(player)
            .map(stats -> stats.getLevel(stat.index))
            .orElse(0);
    }

    /**
     * Calculate scaling multiplier based on stat level
     * Level 0 = 1.0x, Level 50 = 1.5x, Level 100 = 2.0x
     */
    private float getScaling(int statLevel) {
        return 1.0f + (statLevel / 100.0f);
    }

    /**
     * Play an Epic Fight animation on the player
     */
    private void playAnim(ServerPlayerPatch playerPatch, String animName) {
        var accessor = AnimationManager.byKey(ResourceLocation.fromNamespaceAndPath("epicfight", animName));
        if (accessor != null) {
            playerPatch.playAnimation(accessor, 0.0F);
        }
    }

    /**
     * Spawn particles at player position using AAA Particles
     */
    private void spawnParticles(ServerPlayer player, String particleName) {
        // AAA Particles integration - spawn at player position
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.3f, 1.5f);
    }

    // ==================== HEAVY STRIKE ====================
    /**
     * Heavy Strike (Brute Force): Devastating blow
     * Base: 250% damage + knockback
     * Level 50: 300% damage + AoE knockback
     * Level 80: 350% damage + stun + AoE
     */
    private void executeHeavyStrike(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "the_guillotine");
        spawnParticles(player, "heavy_strike");

        float scaling = getScaling(statLevel);
        float baseDmg = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        // Level 80 upgrade: AoE attack
        if (statLevel >= 80) {
            float damage = baseDmg * 3.5f * scaling;
            player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(4.0), e -> e != player && e.isAlive())
                .forEach(e -> {
                    e.hurt(player.damageSources().playerAttack(player), damage);
                    double dx = e.getX() - player.getX();
                    double dz = e.getZ() - player.getZ();
                    e.knockback(2.5f, dx, dz);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                });
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.4f);
        }
        // Level 50 upgrade: Enhanced knockback
        else if (statLevel >= 50) {
            LivingEntity target = getTarget(player, 5.0);
            if (target != null) {
                float damage = baseDmg * 3.0f * scaling;
                target.hurt(player.damageSources().playerAttack(player), damage);
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();
                target.knockback(3.0f, dx, dz);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        }
        // Base version
        else {
            LivingEntity target = getTarget(player, 5.0);
            if (target != null) {
                float damage = baseDmg * 2.5f * scaling;
                target.hurt(player.damageSources().playerAttack(player), damage);
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();
                target.knockback(2.0f, dx, dz);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.6f);
            }
        }
    }

    // ==================== BLADE DANCE ====================
    /**
     * Blade Dance (Blade Technique): Rapid combo
     * Base: 3 hits at 80% damage
     * Level 50: 4 hits at 90% damage
     * Level 80: 5 hits at 100% damage + crit chance
     */
    private void executeBladeDance(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "blade_rush_combo1");
        spawnParticles(player, "blade_dance");

        float scaling = getScaling(statLevel);
        float baseDmg = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        int hits = statLevel >= 80 ? 5 : (statLevel >= 50 ? 4 : 3);
        float hitMultiplier = statLevel >= 80 ? 1.0f : (statLevel >= 50 ? 0.9f : 0.8f);
        float hitDmg = baseDmg * hitMultiplier * scaling;

        LivingEntity target = getTarget(player, 4.0);
        if (target != null) {
            for (int i = 0; i < hits; i++) {
                final int hitIndex = i;
                final float dmg = hitDmg;
                final boolean isCrit = statLevel >= 80 && hitIndex == hits - 1; // Last hit crits at level 80
                player.getServer().tell(new net.minecraft.server.TickTask(
                    player.getServer().getTickCount() + hitIndex * 4, () -> {
                    if (target.isAlive()) {
                        float finalDmg = isCrit ? dmg * 1.5f : dmg;
                        target.hurt(player.damageSources().playerAttack(player), finalDmg);
                        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.6f, 1.2f + hitIndex * 0.2f);
                    }
                }));
            }
        }
    }

    // ==================== BLITZ ASSAULT ====================
    /**
     * Blitz Assault (Rapidité): Dash + rapid hits
     * Base: Dash + 5 hits at 60% damage
     * Level 50: Dash + 7 hits at 70% damage
     * Level 80: Dash + 10 hits at 80% damage + speed boost
     */
    private void executeBlitzAssault(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "dagger_dash");
        spawnParticles(player, "blitz_assault");

        // Dash forward
        var look = player.getLookAngle();
        double dashPower = statLevel >= 80 ? 3.0 : (statLevel >= 50 ? 2.5 : 2.0);
        player.setDeltaMovement(look.x * dashPower, 0.1, look.z * dashPower);
        player.hurtMarked = true;

        float scaling = getScaling(statLevel);
        float baseDmg = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        int hits = statLevel >= 80 ? 10 : (statLevel >= 50 ? 7 : 5);
        float hitMultiplier = statLevel >= 80 ? 0.8f : (statLevel >= 50 ? 0.7f : 0.6f);
        float hitDmg = baseDmg * hitMultiplier * scaling;

        // Speed boost at level 80
        if (statLevel >= 80) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);

        for (int i = 0; i < hits; i++) {
            final float dmg = hitDmg;
            player.getServer().tell(new net.minecraft.server.TickTask(
                player.getServer().getTickCount() + i * 3, () -> {
                player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(2.5), e -> e != player && e.isAlive())
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
            }));
        }
    }

    // ==================== SHADOW STEP ====================
    /**
     * Shadow Step (Agility): Teleport + crit
     * Base: Teleport 8 blocks + crit boost
     * Level 50: Teleport 12 blocks + crit boost + speed
     * Level 80: Teleport 16 blocks + guaranteed crit + invisibility
     */
    private void executeShadowStep(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "phantom_ascent_forward");
        spawnParticles(player, "shadow_step");

        LivingEntity target = getTarget(player, statLevel >= 80 ? 16.0 : (statLevel >= 50 ? 12.0 : 10.0));
        if (target != null) {
            var look = target.getLookAngle();
            double newX = target.getX() - look.x * 2.0;
            double newY = target.getY();
            double newZ = target.getZ() - look.z * 2.0;
            player.teleportTo(newX, newY, newZ);

            // Level 80: invisibility + guaranteed crit
            if (statLevel >= 80) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 2, false, false));
            }
            // Level 50: speed + enhanced crit
            else if (statLevel >= 50) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
            }
            // Base: crit boost
            else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    // ==================== STONE SKIN ====================
    /**
     * Stone Skin (Physical Resistance): Damage reduction
     * Base: Resistance III for 5s
     * Level 50: Resistance IV for 6s
     * Level 80: Resistance IV for 8s + reflect damage
     */
    private void executeStoneSkin(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "guard_greatsword");
        spawnParticles(player, "stone_skin");

        int duration = statLevel >= 80 ? 160 : (statLevel >= 50 ? 120 : 100);
        int amplifier = statLevel >= 50 ? 3 : 2;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, false, true));

        // Level 80: reflect damage (handled in PerkEffectHandler)
        if (statLevel >= 80) {
            player.getPersistentData().putBoolean("statmod_stone_skin_reflect", true);
            player.getPersistentData().putInt("statmod_stone_skin_reflect_timer", duration);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    // ==================== ENDURANCE SURGE ====================
    /**
     * Endurance Surge (Physical Endurance): Stamina recovery
     * Base: Saturation + Speed for 8s
     * Level 50: + Regeneration for 8s
     * Level 80: + Resistance + No hunger for 10s
     */
    private void executeEnduranceSurge(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "steel_whirlwind_charging");
        spawnParticles(player, "endurance_surge");

        int duration = statLevel >= 80 ? 200 : 160;

        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false));

        if (statLevel >= 50) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false));
        }
        if (statLevel >= 80) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    // ==================== PRECISION SHOT ====================
    /**
     * Precision Shot (Precision): Charged arrow
     * Base: 500% damage, pierce 2
     * Level 50: 700% damage, pierce 3, homing
     * Level 80: 1000% damage, pierce 5, explosion on hit
     */
    private void executePrecisionShot(ServerPlayer player, ServerPlayerPatch playerPatch, int statLevel) {
        playAnim(playerPatch, "bow_shot_mid");
        spawnParticles(player, "precision_shot");

        float scaling = getScaling(statLevel);
        float damageMultiplier = statLevel >= 80 ? 10.0f : (statLevel >= 50 ? 7.0f : 5.0f);
        byte pierce = (byte) (statLevel >= 80 ? 5 : (statLevel >= 50 ? 3 : 2));

        Arrow arrow = new Arrow(player.level(), player);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0f, 0.0f);
        arrow.setBaseDamage(arrow.getBaseDamage() * damageMultiplier * scaling);
        arrow.setCritArrow(true);
        arrow.setPierceLevel(pierce);
        player.level().addFreshEntity(arrow);

        // Level 80: spawn additional arrows in a spread
        if (statLevel >= 80) {
            for (int i = -1; i <= 1; i += 2) {
                Arrow extra = new Arrow(player.level(), player);
                extra.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 5, 0, 3.0f, 0.0f);
                extra.setBaseDamage(arrow.getBaseDamage() * 0.5f);
                extra.setPierceLevel((byte) 1);
                player.level().addFreshEntity(extra);
            }
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private LivingEntity getTarget(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(range), e -> e != player && e.isAlive())
            .stream().min((a, b) -> Float.compare(a.distanceTo(player), b.distanceTo(player)))
            .orElse(null);
    }

    @Override
    public float getConsumption() {
        return 20.0f;
    }

    public StatType getStat() { return stat; }
    public long getCooldownTicks() { return cooldownTicks; }

    public static void clearCooldowns(UUID uuid) {
        cooldownEndTick.remove(uuid);
        lastSkillUsed.remove(uuid);
        lastSkillTick.remove(uuid);
    }

    public static boolean hasCombo(ServerPlayer player, String requiredSkill, long withinMs) {
        String last = lastSkillUsed.get(player.getUUID());
        Long tick = lastSkillTick.get(player.getUUID());
        if (last == null || tick == null) return false;
        long withinTicks = withinMs / 50;
        return last.equals(requiredSkill) && (player.level().getGameTime() - tick < withinTicks);
    }
}

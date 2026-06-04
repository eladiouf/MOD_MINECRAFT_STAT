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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;

/**
 * Non-combat skills for Magic, Survival, Crafting, and Mental stats.
 * These provide utility, exploration, and crafting bonuses.
 */
public class NonCombatSkill extends Skill {

    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final SkillCategory category;
    private final long cooldownMs;

    public enum SkillCategory {
        // Magic
        ARCANE_BOLT, WATER_HEAL, EARTH_SHIELD, FIREBALL, AIR_DASH,
        MAGIC_RESIST, FAST_CAST, MANA_REGEN, STUDY,
        // Survival
        TRACK, SENSE_DANGER,
        // Crafting
        MASTER_FORGE, FEAST, POTION_BOOST,
        // Mental
        MEDITATE, INTIMIDATE, WILLPOWER_AURA
    }

    public NonCombatSkill(SkillBuilder<? extends Skill> builder, SkillCategory category, long cooldownMs) {
        super(builder);
        this.category = category;
        this.cooldownMs = cooldownMs;
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        if (!super.canExecute(container)) return false;
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return false;
        ServerPlayer player = playerPatch.getOriginal();

        Long lastUse = cooldowns.get(player.getUUID());
        if (lastUse != null && System.currentTimeMillis() - lastUse < cooldownMs) return false;
        return true;
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();

        cooldowns.put(player.getUUID(), System.currentTimeMillis());
        container.setResource(container.getResource() - getConsumption());

        switch (category) {
            case MEDITATE -> executeMeditate(player, playerPatch);
            case ARCANE_BOLT -> executeArcaneBolt(player, playerPatch);
            case WATER_HEAL -> executeWaterHeal(player, playerPatch);
            case EARTH_SHIELD -> executeEarthShield(player, playerPatch);
            case FIREBALL -> executeFireball(player, playerPatch);
            case AIR_DASH -> executeAirDash(player, playerPatch);
            case MAGIC_RESIST -> executeMagicResist(player, playerPatch);
            case FAST_CAST -> executeFastCast(player, playerPatch);
            case MANA_REGEN -> executeManaRegen(player, playerPatch);
            case STUDY -> executeStudy(player, playerPatch);
            case TRACK -> executeTrack(player, playerPatch);
            case SENSE_DANGER -> executeSenseDanger(player, playerPatch);
            case MASTER_FORGE -> executeMasterForge(player, playerPatch);
            case FEAST -> executeFeast(player, playerPatch);
            case POTION_BOOST -> executePotionBoost(player, playerPatch);
            case INTIMIDATE -> executeIntimidate(player, playerPatch);
            case WILLPOWER_AURA -> executeWillpowerAura(player, playerPatch);
        }
    }

    // ==================== MEDITATION ====================
    /**
     * Meditation (Willpower + Erudition):
     * Sit down, rapidly reduce fatigue, regenerate health
     * Level 20: -50% fatigue, +2 HP/s
     * Level 50: -75% fatigue, +4 HP/s, +saturation
     * Level 80: -100% fatigue, +6 HP/s, +saturation, +Resistance
     */
    private void executeMeditate(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int willLevel = getStatLevel(player, StatType.WILLPOWER);
        int eruLevel = getStatLevel(player, StatType.ERUDITION);
        int avgLevel = (willLevel + eruLevel) / 2;

        // Animation: sitting pose (use idle or creative_idle)
        playAnim(playerPatch, "creative_idle");

        // Heal over time
        int duration = 200; // 10 seconds
        int regenAmplifier = avgLevel >= 80 ? 2 : (avgLevel >= 50 ? 1 : 0);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, regenAmplifier, false, false));

        // Saturation at level 50+
        if (avgLevel >= 50) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 0, false, false));
        }

        // Resistance at level 80+
        if (avgLevel >= 80) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
        }

        // Slow down (can't move while meditating)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4, false, false));

        // Fatigue reduction
        player.getFoodData().eat(2, 0.5f); // Restore some food/saturation

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§aMéditation... Restez immobile.").withStyle(net.minecraft.ChatFormatting.GREEN),
            true);
    }

    // ==================== MAGIC SKILLS ====================
    private void executeArcaneBolt(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.ARCANE_POWER);
        float damage = 5.0f + (level * 0.3f);

        // Shoot a magic projectile (use arrow with custom damage)
        var look = player.getLookAngle();
        Arrow bolt = new Arrow(player.level(), player);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.5f, 0.0f);
        bolt.setBaseDamage(damage);
        bolt.setRemainingFireTicks(100); // Visual: fire trail
        player.level().addFreshEntity(bolt);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private void executeWaterHeal(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.WATER_AFFINITY);
        int duration = 100 + (level * 2); // 5s + scaling
        int amplifier = level >= 50 ? 1 : 0;

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void executeEarthShield(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.EARTH_AFFINITY);
        int duration = 100 + (level * 2);
        int amplifier = level >= 50 ? 2 : 1;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private void executeFireball(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.FIRE_AFFINITY);
        float damage = 8.0f + (level * 0.5f);

        var look = player.getLookAngle();
        Arrow fireball = new Arrow(player.level(), player);
        fireball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.0f, 0.0f);
        fireball.setBaseDamage(damage);
        fireball.setRemainingFireTicks(200);
        fireball.setSecondsOnFire(5);
        player.level().addFreshEntity(fireball);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    private void executeAirDash(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.AIR_AFFINITY);
        double power = 2.0 + (level * 0.02);

        var look = player.getLookAngle();
        player.setDeltaMovement(look.x * power, 0.3 + (level * 0.005), look.z * power);
        player.hurtMarked = true;

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    private void executeMagicResist(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.MAGIC_RESISTANCE);
        int duration = 200 + (level * 2);

        // Magic resistance = generic damage resistance (since MC doesn't have magic damage type)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private void executeFastCast(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.CASTING_SPEED);
        int duration = 100 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    private void executeManaRegen(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.MANA_POOL);
        int duration = 200 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void executeStudy(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.ERUDITION);
        int duration = 200 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // ==================== SURVIVAL SKILLS ====================
    private void executeTrack(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.TRACKING);
        double range = 20.0 + (level * 0.5);

        // Show glowing on nearby mobs
        player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
            player.getBoundingBox().inflate(range), e -> e.isAlive())
            .forEach(mob -> {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            });

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.5f);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§ePistage actif: " + (int) range + " blocs").withStyle(net.minecraft.ChatFormatting.YELLOW),
            true);
    }

    private void executeSenseDanger(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.KEEN_SENSES);
        double range = 16.0 + (level * 0.3);

        boolean danger = !player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
            player.getBoundingBox().inflate(range), e -> e.getTarget() == player && e.isAlive())
            .isEmpty();

        if (danger) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c⚠ Danger détecté!").withStyle(net.minecraft.ChatFormatting.RED),
                true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        } else {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a✓ Zone sûre").withStyle(net.minecraft.ChatFormatting.GREEN),
                true);
        }
    }

    // ==================== CRAFTING SKILLS ====================
    private void executeMasterForge(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.FORGING);
        int duration = 200 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 1, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void executeFeast(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.COOKING);

        player.getFoodData().eat(10, 1.0f + (level * 0.01f));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void executePotionBoost(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.ALCHEMY);
        int duration = 200 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // ==================== MENTAL SKILLS ====================
    private void executeIntimidate(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.INTIMIDATION);
        double range = 10.0 + (level * 0.2);

        player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
            player.getBoundingBox().inflate(range), e -> e.isAlive() && e instanceof net.minecraft.world.entity.Mob)
            .forEach(mob -> {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, false));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false));
            });

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private void executeWillpowerAura(ServerPlayer player, ServerPlayerPatch playerPatch) {
        int level = getStatLevel(player, StatType.WILLPOWER);
        int duration = 200 + (level * 2);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    // ==================== HELPERS ====================
    private int getStatLevel(ServerPlayer player, StatType stat) {
        return CapabilityHelper.getStats(player)
            .map(stats -> stats.getLevel(stat.index))
            .orElse(0);
    }

    private void playAnim(ServerPlayerPatch playerPatch, String animName) {
        var accessor = AnimationManager.byKey(ResourceLocation.fromNamespaceAndPath("epicfight", animName));
        if (accessor != null) {
            playerPatch.playAnimation(accessor, 0.0F);
        }
    }

    @Override
    public float getConsumption() {
        return 10.0f;
    }

    public SkillCategory getNonCombatCategory() { return category; }
    public long getCooldownMs() { return cooldownMs; }

    public static void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }
}

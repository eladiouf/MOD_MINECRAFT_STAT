package tong.statmod.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import tong.statmod.stats.StatType;

/**
 * Mover skills — mobility and evasion.
 * Three tiers: quick_step, shadow_leap, wind_dash.
 */
public class MoverSkill extends Skill {

    private final MoverType type;

    public enum MoverType {
        QUICK_STEP,   // Tier 1: Dash 3 blocks, i-frames 0.3s, 3s cooldown
        SHADOW_LEAP,  // Tier 2: Teleport 6 blocks, i-frames 0.5s, 8s cooldown
        WIND_DASH     // Tier 3: Speed III + jump boost 3s, 12s cooldown
    }

    public MoverSkill(SkillBuilder<? extends Skill> builder, MoverType type) {
        super(builder);
        this.type = type;
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();

        container.setResource(container.getResource() - getConsumption());

        switch (type) {
            case QUICK_STEP -> executeQuickStep(player, playerPatch);
            case SHADOW_LEAP -> executeShadowLeap(player, playerPatch);
            case WIND_DASH -> executeWindDash(player, playerPatch);
        }
    }

    /**
     * Play an Epic Fight animation on the player
     */
    private void playAnim(ServerPlayerPatch playerPatch, String animName) {
        var accessor = AnimationManager.byKey(new ResourceLocation("epicfight", animName));
        if (accessor != null) {
            playerPatch.playAnimation(accessor, 0.0F);
        }
    }

    /**
     * Quick Step: Dash 3 blocks forward, brief i-frames (invulnerability)
     * Animation: step_forward (quick forward step)
     */
    private void executeQuickStep(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "step_forward");

        var look = player.getLookAngle();
        player.setDeltaMovement(look.x * 1.5, 0.05, look.z * 1.5);
        player.hurtMarked = true;
        player.setInvulnerable(true);
        player.getServer().tell(new net.minecraft.server.TickTask(
            player.getServer().getTickCount() + 6, () -> player.setInvulnerable(false)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    /**
     * Shadow Leap: Teleport 6 blocks in look direction, longer i-frames
     * Animation: phantom_ascent_backward (teleport backward)
     */
    private void executeShadowLeap(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "phantom_ascent_backward");

        var look = player.getLookAngle();
        double newX = player.getX() + look.x * 6.0;
        double newY = player.getY();
        double newZ = player.getZ() + look.z * 6.0;
        player.teleportTo(newX, newY, newZ);

        player.setInvulnerable(true);
        player.getServer().tell(new net.minecraft.server.TickTask(
            player.getServer().getTickCount() + 10, () -> player.setInvulnerable(false)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.0f);
    }

    /**
     * Wind Dash: Speed III + Jump Boost II for 3 seconds
     * Animation: sword_dash (quick dash)
     */
    private void executeWindDash(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "sword_dash");

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    @Override
    public float getConsumption() {
        return switch (type) {
            case QUICK_STEP -> 10.0f;
            case SHADOW_LEAP -> 15.0f;
            case WIND_DASH -> 12.0f;
        };
    }

    public MoverType getMoverType() { return type; }
}

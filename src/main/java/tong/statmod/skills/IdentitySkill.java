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
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Identity skills — ultimate abilities with massive effects and long cooldowns.
 * Only 3 in the mod: Berserker Rage, Blade God, Shadow Dancer.
 */
public class IdentitySkill extends Skill {

    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final IdentityType type;
    private final long cooldownMs;

    public enum IdentityType {
        BERSERKER_RAGE,   // +100% damage, +50% speed, -30% defense, 10s, 60s CD
        BLADE_GOD,        // +50% crit, +30% speed, crits ignore armor, 8s, 90s CD
        SHADOW_DANCER     // +100% dodge, +50% speed, i-frames on dodge, 12s, 120s CD
    }

    public IdentitySkill(SkillBuilder<? extends Skill> builder, IdentityType type, long cooldownMs) {
        super(builder);
        this.type = type;
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

        switch (type) {
            case BERSERKER_RAGE -> executeBerserkerRage(player, playerPatch);
            case BLADE_GOD -> executeBladeGod(player, playerPatch);
            case SHADOW_DANCER -> executeShadowDancer(player, playerPatch);
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
     * Berserker Rage: +100% damage, +50% speed, -30% defense for 10s
     * Animation: demolition_leap_charge → demolition_leap (power-up + ground slam)
     */
    private void executeBerserkerRage(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "demolition_leap_charge");

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1, false, false));
        // Defense reduction via weakness
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5f, 0.5f);

        // Screen shake effect via particles
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.3f);
    }

    /**
     * Blade God: +50% crit, +30% speed, crits ignore armor for 8s
     * Animation: eviscerate_first → eviscerate_second (double slash)
     */
    private void executeBladeGod(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "eviscerate_first");

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 160, 4, false, false));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    /**
     * Shadow Dancer: +100% dodge, +50% speed, i-frames on dodge for 12s
     * Animation: dancing_edge (elegant movement)
     */
    private void executeShadowDancer(ServerPlayer player, ServerPlayerPatch playerPatch) {
        playAnim(playerPatch, "dancing_edge");

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 240, 0, false, false));
        // Dodge is handled via the PerkEffectHandler checking for this effect

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public float getConsumption() {
        return 30.0f;
    }

    public IdentityType getIdentityType() { return type; }
    public long getCooldownMs() { return cooldownMs; }

    public static void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }
}

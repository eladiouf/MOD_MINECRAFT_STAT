package tong.statmod.dungeon;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import tong.statmod.StatMod;
import tong.statmod.dungeon.ai.living.DungeonLivingActor;

/** Applies the final hostile-only health balance for STAT Trial Dungeon actors. */
public final class DungeonEnemyHealthBalance {
    private static final double MAX_HEALTH_MULTIPLIER = 0.5D;
    private static final ResourceLocation MODIFIER_ID = new ResourceLocation(
            StatMod.MOD_ID, "dungeon_enemy_health_balance");
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes(
            MODIFIER_ID.toString().getBytes(StandardCharsets.UTF_8));

    private DungeonEnemyHealthBalance() {}

    static double maxHealthMultiplier() {
        return MAX_HEALTH_MULTIPLIER;
    }

    static double modifierAmount() {
        return MAX_HEALTH_MULTIPLIER - 1.0D;
    }

    static double healthAtSameRatio(double oldMaxHealth, double oldHealth, double newMaxHealth) {
        double ratio = oldMaxHealth > 0.0D ? oldHealth / oldMaxHealth : 1.0D;
        return Math.max(0.0D, Math.min(newMaxHealth, newMaxHealth * ratio));
    }

    public static void apply(LivingEntity entity) {
        if (entity == null
                || entity instanceof Player
                || !entity.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)
                || entity instanceof Mob mob && DungeonLivingActor.isNonCombat(mob)) {
            return;
        }
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        double previousMaxHealth = entity.getMaxHealth();
        double previousHealth = entity.getHealth();
        maxHealth.removeModifier(MODIFIER_UUID);
        maxHealth.addPermanentModifier(new AttributeModifier(
                MODIFIER_UUID,
                MODIFIER_ID.toString(),
                modifierAmount(),
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        entity.setHealth((float) healthAtSameRatio(
                previousMaxHealth, previousHealth, entity.getMaxHealth()));
    }
}

package tong.statmod.dungeon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tong.statmod.StatMod;

import java.util.UUID;

/**
 * Scaling des attributs des mobs de donjon par étage, fallback quand L2 Hostility est absent.
 */
public final class DungeonMobScaling {

    public static final String ROLE_TAG = "statmod_dungeon_mob_role";
    private static final String HP_APPLIED_TAG = "statmod_dungeon_hp_scaled";

    private static final ResourceLocation HP_MOD_ID = new ResourceLocation(
            StatMod.MOD_ID, "dungeon_hp_scale");
    private static final ResourceLocation ATK_MOD_ID = new ResourceLocation(
            StatMod.MOD_ID, "dungeon_atk_scale");
    private static final ResourceLocation ARMOR_MOD_ID = new ResourceLocation(
            StatMod.MOD_ID, "dungeon_armor_scale");
    private static final ResourceLocation TOUGH_MOD_ID = new ResourceLocation(
            StatMod.MOD_ID, "dungeon_toughness_scale");

    private static final double ATK_PER_FLOOR = 0.05;
    private static final double ARMOR_PER_FLOOR = 0.5;
    private static final double TOUGH_PER_FLOOR = 0.1;
    private static final double ARMOR_CAP = 40.0;
    private static final double TOUGH_CAP = 15.0;

    private DungeonMobScaling() {}

    public enum MobRole {
        NORMAL("normal", 1.0),
        ELITE("elite", 1.25),
        BOSS("boss", 1.5);

        private final String id;
        private final double healthMultiplier;

        MobRole(String id, double healthMultiplier) {
            this.id = id;
            this.healthMultiplier = healthMultiplier;
        }

        public String id() {
            return id;
        }

        public static MobRole fromId(String id) {
            if (id != null) {
                for (MobRole role : values()) {
                    if (role.id.equalsIgnoreCase(id)) return role;
                }
            }
            return NORMAL;
        }
    }

    static double healthMultiplier(int floor, MobRole role) {
        int safeFloor = Math.max(1, floor);
        double base;
        if (safeFloor > 100) {
            base = Math.min(25.0, 15.0 + (safeFloor - 100) * 0.10);
        } else {
            int[] floors = {1, 10, 25, 50, 75, 100};
            double[] multipliers = {3.0, 4.0, 6.0, 9.0, 12.0, 15.0};
            base = multipliers[multipliers.length - 1];
            for (int i = 1; i < floors.length; i++) {
                if (safeFloor <= floors[i]) {
                    double progress = (safeFloor - floors[i - 1]) / (double) (floors[i] - floors[i - 1]);
                    base = multipliers[i - 1] + progress * (multipliers[i] - multipliers[i - 1]);
                    break;
                }
            }
        }
        MobRole safeRole = role == null ? MobRole.NORMAL : role;
        return base * safeRole.healthMultiplier;
    }

    static double healthModifierAmount(double finalMultiplier) {
        return Math.max(0.0, finalMultiplier - 1.0);
    }

    public static void applyFloorScaling(LivingEntity mob, int floor) {
        if (mob == null || floor <= 0) return;

        boolean firstApplication = !mob.getPersistentData().getBoolean(HP_APPLIED_TAG);
        double previousMaxHealth = mob.getMaxHealth();
        double healthRatio = previousMaxHealth > 0.0 ? mob.getHealth() / previousMaxHealth : 1.0;
        MobRole role = MobRole.fromId(mob.getPersistentData().getString(ROLE_TAG));
        double hpBonus = healthModifierAmount(healthMultiplier(floor, role));
        applyMultiplier(mob, Attributes.MAX_HEALTH, HP_MOD_ID, hpBonus);

        double atkBonus = ATK_PER_FLOOR * (floor - 1);
        applyMultiplier(mob, Attributes.ATTACK_DAMAGE, ATK_MOD_ID, atkBonus);

        double armor = Math.min(ARMOR_CAP, ARMOR_PER_FLOOR * (floor - 1));
        if (armor > 0) {
            applyFlat(mob, Attributes.ARMOR, ARMOR_MOD_ID, armor);
        }

        double toughness = Math.min(TOUGH_CAP, TOUGH_PER_FLOOR * (floor - 1));
        if (toughness > 0) {
            applyFlat(mob, Attributes.ARMOR_TOUGHNESS, TOUGH_MOD_ID, toughness);
        }

        if (firstApplication) {
            mob.setHealth(mob.getMaxHealth());
            mob.getPersistentData().putBoolean(HP_APPLIED_TAG, true);
        } else {
            mob.setHealth((float) Math.max(1.0, Math.min(mob.getMaxHealth(), mob.getMaxHealth() * healthRatio)));
        }
    }

    private static void applyMultiplier(LivingEntity mob, Attribute attr,
                                        ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        UUID uuid = UUID.nameUUIDFromBytes(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        inst.removeModifier(uuid);
        if (Math.abs(amount) > 0.001) {
            inst.addTransientModifier(
                    new AttributeModifier(uuid, id.toString(), amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void applyFlat(LivingEntity mob, Attribute attr,
                                  ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        UUID uuid = UUID.nameUUIDFromBytes(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        inst.removeModifier(uuid);
        if (Math.abs(amount) > 0.001) {
            inst.addTransientModifier(
                    new AttributeModifier(uuid, id.toString(), amount, AttributeModifier.Operation.ADDITION));
        }
    }
}

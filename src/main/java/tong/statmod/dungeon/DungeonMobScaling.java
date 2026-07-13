package tong.statmod.dungeon;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tong.statmod.STATMod;

/**
 * Scaling des attributs des mobs de donjon par étage, fallback quand L2 Hostility est absent.
 *
 * <p>Applique des multiplicateurs cumulatifs aux stats clés pour rendre la progression en
 * profondeur significative et le donjon très difficile solo dès les étages moyens.
 *
 * <p>Formules (base = floor 1) :
 * <ul>
 *   <li>{@code MAX_HEALTH} : courbe par paliers, de x3 à l'étage 1 à x22 à l'étage 100,
 *       puis progression abyssale plafonnée à x32</li>
 *   <li>{@code ATTACK_DAMAGE} : +5 %/floor, cumul additif → floor 50 : x3.5</li>
 *   <li>{@code ARMOR} : +0.5/floor, cap 40</li>
 *   <li>{@code ARMOR_TOUGHNESS} : +0.1/floor, cap 15</li>
 * </ul>
 *
 * <p>S'applique via {@link DungeonMobSpawner#flushL2Queue()} après L2, ou en fallback si L2
 * n'est pas chargé. N'empile pas avec L2 ({@link L2HostilityBridge} prioritaire).
 */
public final class DungeonMobScaling {

    public static final String ROLE_TAG = "statmod_dungeon_mob_role";
    private static final String HP_APPLIED_TAG = "statmod_dungeon_hp_scaled";

    private static final ResourceLocation HP_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            STATMod.MODID, "dungeon_hp_scale");
    private static final ResourceLocation ATK_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            STATMod.MODID, "dungeon_atk_scale");
    private static final ResourceLocation ARMOR_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            STATMod.MODID, "dungeon_armor_scale");
    private static final ResourceLocation TOUGH_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            STATMod.MODID, "dungeon_toughness_scale");

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
            base = Math.min(32.0, 22.0 + (safeFloor - 100) * 0.10);
        } else {
            int[] floors = {1, 10, 25, 50, 75, 100};
            double[] multipliers = {3.0, 4.5, 7.5, 12.0, 17.0, 22.0};
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

    /**
     * Applique le scaling basé sur l'étage à un mob du donjon.
     * La vie commence à x3 dès l'étage 1. Une réapplication conserve le ratio de vie actuel.
     */
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

    private static void applyMultiplier(LivingEntity mob, Holder<Attribute> attr,
                                        ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (Math.abs(amount) > 0.001) {
            inst.addTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void applyFlat(LivingEntity mob, Holder<Attribute> attr,
                                  ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (Math.abs(amount) > 0.001) {
            inst.addTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}

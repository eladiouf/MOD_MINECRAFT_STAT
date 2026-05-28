package tong.statmod.skills;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Weapon passive skills — automatically applied when a weapon type is equipped.
 * Each weapon has a philosophy: bonus and malus that define its combat style.
 * Applied via AttributeModifiers for clean stacking with other effects.
 */
public class WeaponPassiveSkill extends PassiveSkill {

    private static final UUID DAMAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID MOVE_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID ARMOR_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    private static final UUID CRIT_UUID = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");

    private final WeaponStyle style;

    public enum WeaponStyle {
        SWORD, AXE, GREATSWORD, DAGGER, LONGSWORD, SPEAR, TACHI, UCHIGATANA,
        FIST, BOW, PICKAXE, HOE
    }

    public WeaponPassiveSkill(SkillBuilder<? extends PassiveSkill> builder, WeaponStyle style) {
        super(builder);
        this.style = style;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();
        applyModifiers(player);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();
        removeModifiers(player);
    }

    private void applyModifiers(ServerPlayer player) {
        switch (style) {
            case SWORD -> {
                // Équilibré: +15% attack speed
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, 0.15, "sword_speed");
            }
            case AXE -> {
                // Puissance brute: +30% damage, -25% attack speed
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, 0.30, "axe_damage");
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, -0.25, "axe_speed");
            }
            case GREATSWORD -> {
                // Destruction lente: +50% damage, -40% attack speed, -10% move speed
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, 0.50, "greatsword_damage");
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, -0.40, "greatsword_speed");
                addModifier(player, Attributes.MOVEMENT_SPEED, MOVE_UUID, -0.10, "greatsword_move");
            }
            case DAGGER -> {
                // Agilité pure: +40% attack speed, -30% damage
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, 0.40, "dagger_speed");
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.30, "dagger_damage");
            }
            case LONGSWORD -> {
                // Précision technique: +20% damage, -5% attack speed
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, 0.20, "longsword_damage");
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, -0.05, "longsword_speed");
            }
            case SPEAR -> {
                // Zone control: +10% damage, -15% attack speed
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, 0.10, "spear_damage");
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, -0.15, "spear_speed");
                // TODO: +3 reach via custom attribute or mixin
            }
            case TACHI -> {
                // Vitesse élégante: +25% attack speed, -15% damage
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, 0.25, "tachi_speed");
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.15, "tachi_damage");
            }
            case UCHIGATANA -> {
                // Contre-attaque: -20% base damage
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.20, "uchigatana_damage");
                // Parry bonus handled in PerkEffectHandler or combat event
            }
            case FIST -> {
                // Close combat: +30% attack speed, +15% move speed, -50% damage
                addModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, 0.30, "fist_speed");
                addModifier(player, Attributes.MOVEMENT_SPEED, MOVE_UUID, 0.15, "fist_move");
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.50, "fist_damage");
            }
            case BOW -> {
                // Distance: -60% melee damage
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.60, "bow_damage");
                // Arrow bonus handled in projectile event
            }
            case PICKAXE -> {
                // Utilitaire: -35% combat damage
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.35, "pickaxe_damage");
                // Mining speed handled via player abilities
            }
            case HOE -> {
                // Récolte: -45% combat damage
                addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, -0.45, "hoe_damage");
                // Crop yield handled in block break event
            }
        }
    }

    private void removeModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, MOVE_UUID);
        removeModifier(player, Attributes.ARMOR, ARMOR_UUID);
    }

    private void addModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attr,
                             UUID uuid, double amount, String name) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(uuid);
            instance.addPermanentModifier(new AttributeModifier(uuid, "statmod:" + name, amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private void removeModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }

    public WeaponStyle getStyle() {
        return style;
    }
}

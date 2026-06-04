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

import tong.statmod.stats.StatType;

/**
 * Stat passive skills — permanent effects that scale with stat level.
 * 3 tiers per combat stat (20/50/80), effects are applied via AttributeModifiers.
 */
public class StatPassiveSkill extends PassiveSkill {

    private final UUID passiveUuid;
    private final StatType stat;
    private final int tier;

    public StatPassiveSkill(SkillBuilder<? extends PassiveSkill> builder, StatType stat, int tier) {
        super(builder);
        this.stat = stat;
        this.tier = tier;
        this.passiveUuid = UUID.nameUUIDFromBytes(("statmod:passive:" + stat.name() + ":tier" + tier).getBytes());
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();
        applyEffect(player);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        ServerPlayer player = playerPatch.getOriginal();
        removeEffect(player);
    }

    private void applyEffect(ServerPlayer player) {
        switch (stat) {
            case BRUTE_FORCE -> applyBruteForce(player);
            case BLADE_TECHNIQUE -> applyBladeTechnique(player);
            case RAPIDITE -> applyRapidite(player);
            case AGILITY -> applyAgility(player);
            case PHYSICAL_RESISTANCE -> applyPhysicalResistance(player);
            case PHYSICAL_ENDURANCE -> applyPhysicalEndurance(player);
            case PRECISION -> applyPrecision(player);
        }
    }

    private void removeEffect(ServerPlayer player) {
        AttributeInstance dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.removeModifier(passiveUuid);

        AttributeInstance spdAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (spdAttr != null) spdAttr.removeModifier(passiveUuid);

        AttributeInstance movAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movAttr != null) movAttr.removeModifier(passiveUuid);

        AttributeInstance armAttr = player.getAttribute(Attributes.ARMOR);
        if (armAttr != null) armAttr.removeModifier(passiveUuid);
    }

    // --- BRUTE FORCE: melee damage scaling ---
    private void applyBruteForce(ServerPlayer player) {
        double dmgBonus = switch (tier) {
            case 1 -> 0.10;  // +10% melee damage
            case 2 -> 0.20;  // +20% melee damage
            case 3 -> 0.30;  // +30% melee damage
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.ATTACK_DAMAGE, dmgBonus, "brute_passive");
    }

    // --- BLADE TECHNIQUE: attack speed + crit ---
    private void applyBladeTechnique(ServerPlayer player) {
        double spdBonus = switch (tier) {
            case 1 -> 0.05;  // +5% attack speed
            case 2 -> 0.10;  // +10% attack speed
            case 3 -> 0.15;  // +15% attack speed
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.ATTACK_SPEED, spdBonus, "blade_passive");
        // Crit handled in PerkEffectHandler via check for this skill
    }

    // --- RAPIDITE: attack speed ---
    private void applyRapidite(ServerPlayer player) {
        double spdBonus = switch (tier) {
            case 1 -> 0.10;  // +10% attack speed
            case 2 -> 0.20;  // +20% attack speed
            case 3 -> 0.30;  // +30% attack speed
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.ATTACK_SPEED, spdBonus, "rapid_passive");
    }

    // --- AGILITY: move speed ---
    private void applyAgility(ServerPlayer player) {
        double movBonus = switch (tier) {
            case 1 -> 0.10;  // +10% move speed
            case 2 -> 0.15;  // +15% move speed
            case 3 -> 0.20;  // +20% move speed
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.MOVEMENT_SPEED, movBonus, "agility_passive");
    }

    // --- PHYSICAL RESISTANCE: damage reduction via armor ---
    private void applyPhysicalResistance(ServerPlayer player) {
        double armBonus = switch (tier) {
            case 1 -> 4.0;   // +4 armor (2 hearts reduction)
            case 2 -> 8.0;   // +8 armor (4 hearts reduction)
            case 3 -> 12.0;  // +12 armor (6 hearts reduction)
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.ARMOR, armBonus / 20.0, "resist_passive");
    }

    // --- PHYSICAL ENDURANCE: health ---
    private void applyPhysicalEndurance(ServerPlayer player) {
        double hpBonus = switch (tier) {
            case 1 -> 4.0;   // +2 hearts
            case 2 -> 8.0;   // +4 hearts
            case 3 -> 12.0;  // +6 hearts
            default -> 0.0;
        };
        addAttrModifier(player, Attributes.MAX_HEALTH, hpBonus / 20.0, "endurance_passive");
    }

    // --- PRECISION: arrow damage (handled in event) ---
    private void applyPrecision(ServerPlayer player) {
        // Arrow damage bonus is handled in PerkEffectHandler
        // This skill serves as a marker for the event system
    }

    private void addAttrModifier(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  double amount, String name) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(passiveUuid);
            instance.addPermanentModifier(new AttributeModifier(
                passiveUuid, "statmod:" + name, amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public StatType getStat() { return stat; }
    public int getTier() { return tier; }
}

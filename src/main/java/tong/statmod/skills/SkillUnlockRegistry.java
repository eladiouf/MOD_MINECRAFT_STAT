package tong.statmod.skills;

import tong.statmod.stats.StatType;
import yesman.epicfight.skill.Skill;

/**
 * Maps stats to their unlockable skills at each tier.
 * New system: 3 passives per stat (tiers 1/2/3 at levels 20/50/80) + 1 active skill.
 * Also tracks movers, guards, and identity skills separately.
 */
public class SkillUnlockRegistry {
    // [stat_index][tier] → Skill
    // tier 0 = Lv.20 passive, tier 1 = Lv.50 passive, tier 2 = Lv.80 passive, tier 3 = active
    private static final Skill[][] registry = new Skill[StatType.values().length][4];

    public static void init() {
        // Skills are registered asynchronously via SkillBuildEvent.
        // Population happens in refresh() called from SkillBuildEvent.
    }

    /**
     * Called after SkillBuildEvent populates SkillRegistry fields.
     */
    public static void refresh() {
        // --- Brute Force ---
        set(StatType.BRUTE_FORCE, 0, SkillRegistry.BRUTE_POWER);
        set(StatType.BRUTE_FORCE, 1, SkillRegistry.BRUTE_RAGE);
        set(StatType.BRUTE_FORCE, 2, SkillRegistry.BRUTE_FURY);
        set(StatType.BRUTE_FORCE, 3, SkillRegistry.HEAVY_STRIKE);

        // --- Blade Technique ---
        set(StatType.BLADE_TECHNIQUE, 0, SkillRegistry.BLADE_FINESSE);
        set(StatType.BLADE_TECHNIQUE, 1, SkillRegistry.BLADE_MASTERY);
        set(StatType.BLADE_TECHNIQUE, 2, SkillRegistry.BLADE_PERFECTION);
        set(StatType.BLADE_TECHNIQUE, 3, SkillRegistry.BLADE_DANCE);

        // --- Rapidité ---
        set(StatType.RAPIDITE, 0, SkillRegistry.RAPID_SURGE);
        set(StatType.RAPIDITE, 1, SkillRegistry.RAPID_BLITZ);
        set(StatType.RAPIDITE, 2, SkillRegistry.RAPID_LIGHTNING);
        set(StatType.RAPIDITE, 3, SkillRegistry.BLITZ_ASSAULT);

        // --- Agility ---
        set(StatType.AGILITY, 0, SkillRegistry.AGILITY_FOOTWORK);
        set(StatType.AGILITY, 1, SkillRegistry.AGILITY_EVASION);
        set(StatType.AGILITY, 2, SkillRegistry.AGILITY_PHANTOM);
        set(StatType.AGILITY, 3, SkillRegistry.SHADOW_STEP);

        // --- Physical Resistance ---
        set(StatType.PHYSICAL_RESISTANCE, 0, SkillRegistry.RESIST_IRON);
        set(StatType.PHYSICAL_RESISTANCE, 1, SkillRegistry.RESIST_STEEL);
        set(StatType.PHYSICAL_RESISTANCE, 2, SkillRegistry.RESIST_DIAMOND);
        set(StatType.PHYSICAL_RESISTANCE, 3, SkillRegistry.STONE_SKIN);

        // --- Physical Endurance ---
        set(StatType.PHYSICAL_ENDURANCE, 0, SkillRegistry.ENDURANCE_VITALITY);
        set(StatType.PHYSICAL_ENDURANCE, 1, SkillRegistry.ENDURANCE_TOUGHNESS);
        set(StatType.PHYSICAL_ENDURANCE, 2, SkillRegistry.ENDURANCE_UNBREAKABLE);
        set(StatType.PHYSICAL_ENDURANCE, 3, SkillRegistry.ENDURANCE_SURGE);

        // --- Precision ---
        set(StatType.PRECISION, 0, SkillRegistry.PRECISION_FOCUS);
        set(StatType.PRECISION, 1, SkillRegistry.PRECISION_ACCURACY);
        set(StatType.PRECISION, 2, SkillRegistry.PRECISION_DEADEYE);
        set(StatType.PRECISION, 3, SkillRegistry.PRECISION_SHOT);
    }

    private static void set(StatType stat, int tier, Skill skill) {
        if (tier >= 0 && tier < 4) {
            registry[stat.index][tier] = skill;
        }
    }

    public static Skill getSkill(StatType stat, int tier) {
        if (tier < 0 || tier >= 4) return null;
        return registry[stat.index][tier];
    }

    public static boolean hasSkill(StatType stat, int tier) {
        return registry[stat.index][tier] != null;
    }
}

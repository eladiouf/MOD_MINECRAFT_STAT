package tong.statmod.skills;

import tong.statmod.skills.skills.*;
import tong.statmod.stats.StatType;
import yesman.epicfight.skill.Skill;

public class SkillUnlockRegistry {
    private static Skill[][] registry = new Skill[StatType.values().length][4];

    public static void init() {
        register(StatType.BRUTE_FORCE, 0, null);
        register(StatType.BRUTE_FORCE, 1, null);
        register(StatType.BRUTE_FORCE, 2, EarthSplitterSkill.INSTANCE);
        register(StatType.BRUTE_FORCE, 3, null);

        register(StatType.BLADE_TECHNIQUE, 0, null);
        register(StatType.BLADE_TECHNIQUE, 1, IaijutsuSkill.INSTANCE);
        register(StatType.BLADE_TECHNIQUE, 2, null);
        register(StatType.BLADE_TECHNIQUE, 3, null);

        register(StatType.AGILITY, 0, null);
        register(StatType.AGILITY, 1, ShadowStepSkill.INSTANCE);
        register(StatType.AGILITY, 2, null);
        register(StatType.AGILITY, 3, null);

        register(StatType.PHYSICAL_ENDURANCE, 0, null);
        register(StatType.PHYSICAL_ENDURANCE, 1, IronWallSkill.INSTANCE);
        register(StatType.PHYSICAL_ENDURANCE, 2, null);
        register(StatType.PHYSICAL_ENDURANCE, 3, null);

        register(StatType.PRECISION, 0, HawkEyeSkill.INSTANCE);
        register(StatType.PRECISION, 1, null);
        register(StatType.PRECISION, 2, null);
        register(StatType.PRECISION, 3, null);
    }

    public static void register(StatType stat, int tier, Skill skill) {
        if (tier >= 0 && tier < 4) {
            registry[stat.index][tier] = skill;
        }
    }

    public static Skill getSkill(StatType stat, int tier) {
        if (tier < 0 || tier >= 4) return null;
        return registry[stat.index][tier];
    }

    public static boolean hasSkill(StatType stat, int tier) {
        return getSkill(stat, tier) != null;
    }
}

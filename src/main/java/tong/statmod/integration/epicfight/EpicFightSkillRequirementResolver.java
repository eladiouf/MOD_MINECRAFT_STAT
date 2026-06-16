package tong.statmod.integration.epicfight;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.skill.Skill;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

import java.util.Map;

public final class EpicFightSkillRequirementResolver {
    private static final Map<String, Map<Integer, Integer>> MVP_REQUIREMENTS = Map.of(
            "berserker", Map.of(StatType.BRUTE_FORCE.index, 40, StatType.PHYSICAL_ENDURANCE.index, 25),
            "liechtenauer", Map.of(StatType.BLADE_TECHNIQUE.index, 40, StatType.PRECISION.index, 25),
            "rushing_tempo", Map.of(StatType.AGILITY.index, 35, StatType.RAPIDITE.index, 30),
            "roll", Map.of(StatType.AGILITY.index, 25, StatType.PHYSICAL_ENDURANCE.index, 15),
            "heartpiercer", Map.of(StatType.PRECISION.index, 40, StatType.AGILITY.index, 20)
    );

    private EpicFightSkillRequirementResolver() {}

    public static Map<String, Map<Integer, Integer>> mvpRequirements() {
        return MVP_REQUIREMENTS;
    }

    public static Map<Integer, Integer> requirementsFor(String skillId) {
        return MVP_REQUIREMENTS.getOrDefault(skillId, Map.of());
    }

    public static boolean hasRequirements(Player player, Skill skill) {
        if (player == null || skill == null || skill.getRegistryName() == null) {
            return true;
        }

        Map<Integer, Integer> requirements = requirementsFor(skill.getRegistryName().getPath());
        if (requirements.isEmpty()) {
            return true;
        }

        return requirements.entrySet().stream()
                .allMatch(entry -> RaceEffectApplier.getEffectiveLevel(player, entry.getKey()) >= entry.getValue());
    }
}

package tong.statmod.integration.tensura;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import tong.statmod.stats.StatType;

import java.util.Map;

public final class StatLevelSkillRewards {
    private static final Map<Integer, Map<Integer, String>> REWARDS = Map.of(
            StatType.BRUTE_FORCE.index, Map.of(
                    10, "tensura:berserk",
                    25, "tensura:giant_strength"
            ),
            StatType.BLADE_TECHNIQUE.index, Map.of(
                    10, "tensura:sword_meister",
                    25, "tensura:iaijutsu"
            ),
            StatType.RAPIDITE.index, Map.of(
                    10, "tensura:accelerated_thoughts"
            ),
            StatType.AGILITY.index, Map.of(
                    10, "tensura:space_manipulation",
                    25, "tensura:thought_acceleration"
            ),
            StatType.ARCANE_POWER.index, Map.of(
                    10, "tensura:magic_manipulation"
            ),
            StatType.FORGING.index, Map.of(
                    10, "tensura:blacksmithing"
            ),
            StatType.COOKING.index, Map.of(
                    10, "tensura:cooking"
            ),
            StatType.ALCHEMY.index, Map.of(
                    10, "tensura:alchemy"
            ),
            StatType.MANA_POOL.index, Map.of(
                    10, "tensura:mana_manipulation"
            )
    );

    private StatLevelSkillRewards() {}

    public static String resolveSkillId(int statIndex, int level) {
        Map<Integer, String> byLevel = REWARDS.get(statIndex);
        if (byLevel == null) return null;
        return TensuraSkillIds.canonicalize(byLevel.get(level));
    }

    public static boolean grantReward(Player player, int statIndex, int level) {
        String skillId = resolveSkillId(statIndex, level);
        if (player == null || skillId == null) {
            return false;
        }
        return SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse(skillId));
    }
}

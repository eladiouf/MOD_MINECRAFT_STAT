package tong.statmod.integration.tensura;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;

import java.util.EnumMap;
import java.util.Map;

public final class PerkToSkillMapper {
    private static final Map<Perk, String> TRANSCENDENCE_SKILLS = new EnumMap<>(Perk.class);

    static {
        TRANSCENDENCE_SKILLS.put(Perk.BRUTE_TRANSCENDENCE, "tensura:giant_strength");
        TRANSCENDENCE_SKILLS.put(Perk.BLADE_TRANSCENDENCE, "tensura:ultra_instinct");
        TRANSCENDENCE_SKILLS.put(Perk.RAPID_TRANSCENDENCE, "tensura:thought_acceleration");
        TRANSCENDENCE_SKILLS.put(Perk.AGIL_TRANSCENDENCE, "tensura:space_manipulation");
        TRANSCENDENCE_SKILLS.put(Perk.RESIST_TRANSCENDENCE, "tensura:infinite_regeneration");
        TRANSCENDENCE_SKILLS.put(Perk.ENDUR_TRANSCENDENCE, "tensura:ultra_regeneration");
        TRANSCENDENCE_SKILLS.put(Perk.PRECI_TRANSCENDENCE, "tensura:all_seeing_eye");
        TRANSCENDENCE_SKILLS.put(Perk.TRACK_TRANSCENDENCE, "tensura:universal_detect");
        TRANSCENDENCE_SKILLS.put(Perk.SENSE_TRANSCENDENCE, "tensura:magic_sense");
        TRANSCENDENCE_SKILLS.put(Perk.FORGE_TRANSCENDENCE, "tensura:godly_craftsman");
        TRANSCENDENCE_SKILLS.put(Perk.COOK_TRANSCENDENCE, "tensura:master_chef");
        TRANSCENDENCE_SKILLS.put(Perk.ALCHEM_TRANSCENDENCE, "tensura:seer");
        TRANSCENDENCE_SKILLS.put(Perk.INTIM_TRANSCENDENCE, "tensura:demon_lord_haki");
        TRANSCENDENCE_SKILLS.put(Perk.WILL_TRANSCENDENCE, "tensura:hero_haki");
    }

    private PerkToSkillMapper() {}

    public static String resolveSkillId(Perk perk) {
        if (perk == null || perk.tier != PerkTier.TRANSCENDENCE) {
            return null;
        }
        return TRANSCENDENCE_SKILLS.get(perk);
    }

    public static boolean grantReward(Player player, Perk perk) {
        String skillId = resolveSkillId(perk);
        if (player == null || skillId == null) {
            return false;
        }
        return SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse(skillId));
    }
}

package tong.statmod.perks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;

import java.util.EnumMap;
import java.util.Map;

public final class EpicFightPerkGate {
    private static final Map<Perk, String> MAPPINGS = new EnumMap<>(Perk.class);

    static {
        MAPPINGS.put(Perk.BRUTE_CORE, "berserker");
        MAPPINGS.put(Perk.BRUTE_MASTERY, "bonebreaker");
        MAPPINGS.put(Perk.BLADE_CORE, "swordmaster");
        MAPPINGS.put(Perk.BLADE_MASTERY, "liechtenauer");
        MAPPINGS.put(Perk.RAPID_CORE, "rushing_tempo");
        MAPPINGS.put(Perk.AGIL_CORE, "roll");
        MAPPINGS.put(Perk.AGIL_MASTERY, "phantom_ascent");
        MAPPINGS.put(Perk.PRECI_CORE, "heartpiercer");
        MAPPINGS.put(Perk.PRECI_MASTERY, "technician");
        MAPPINGS.put(Perk.RESIST_CORE, "endurance");
        MAPPINGS.put(Perk.ENDUR_CORE, "hypervitality");
        MAPPINGS.put(Perk.FORGE_MASTERY, "the_guillotine");
    }

    private EpicFightPerkGate() {}

    public static String resolveSkillId(Perk perk) {
        return MAPPINGS.get(perk);
    }

    public static boolean grantReward(Player player, Perk perk) {
        if (player == null || perk == null || !ModList.get().isLoaded("epicfight")) {
            return false;
        }

        Skill skill = resolveSkill(perk);
        if (skill == null) {
            return false;
        }

        PlayerSkills skills = EpicFightCapabilities.getPlayerPatch(player).getPlayerSkills();
        if (skills.hasLearned(skill)) {
            return false;
        }

        skills.addLearnedSkill(skill);
        return true;
    }

    public static boolean revokeReward(Player player, Perk perk) {
        if (player == null || perk == null || !ModList.get().isLoaded("epicfight")) {
            return false;
        }

        Skill skill = resolveSkill(perk);
        if (skill == null) {
            return false;
        }

        var patch = EpicFightCapabilities.getPlayerPatch(player);
        if (patch == null) {
            return false;
        }

        PlayerSkills skills = patch.getPlayerSkills();
        return skills != null && skills.removeLearnedSkill(skill);
    }

    private static Skill resolveSkill(Perk perk) {
        String id = resolveSkillId(perk);
        if (id == null) {
            return null;
        }

        return switch (id) {
            case "berserker" -> EpicFightSkills.BERSERKER.get();
            case "bonebreaker" -> EpicFightSkills.BONEBREAKER.get();
            case "swordmaster" -> EpicFightSkills.SWORD_MASTER.get();
            case "liechtenauer" -> EpicFightSkills.LIECHTENAUER.get();
            case "rushing_tempo" -> EpicFightSkills.RUSHING_TEMPO.get();
            case "roll" -> EpicFightSkills.ROLL.get();
            case "phantom_ascent" -> EpicFightSkills.PHANTOM_ASCENT.get();
            case "heartpiercer" -> EpicFightSkills.HEARTPIERCER.get();
            case "technician" -> EpicFightSkills.TECHNICIAN.get();
            case "endurance" -> EpicFightSkills.ENDURANCE.get();
            case "hypervitality" -> EpicFightSkills.HYPERVITALITY.get();
            case "the_guillotine" -> EpicFightSkills.THE_GUILLOTINE.get();
            default -> null;
        };
    }
}

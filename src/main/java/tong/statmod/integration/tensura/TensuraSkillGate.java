package tong.statmod.integration.tensura;

import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.perks.Perk;

import java.util.List;
import java.util.Set;

public final class TensuraSkillGate {
    private TensuraSkillGate() {}

    public static boolean canUnlock(Player player, Perk perk) {
        return SkillPerkGate.canUnlock(player, perk);
    }

    public static void requireSkill(int perkId, String... skillIds) {
        SkillPerkGate.requireSkill(perkId, skillIds);
    }

    public static void requireRace(int perkId, String raceId) {
        SkillPerkGate.requireRace(perkId, raceId);
    }

    public static Set<Integer> gatePerks() {
        return SkillPerkGate.gatePerks();
    }

    public static String skillForPerk(int perkId) {
        return SkillPerkGate.skillForPerk(perkId);
    }

    public static List<String> requiredSkills(int perkId) {
        return SkillPerkGate.requiredSkills(perkId);
    }

    public static String requiredRace(int perkId) {
        return SkillPerkGate.requiredRace(perkId);
    }

    public static boolean isGated(int perkId) {
        return SkillPerkGate.isGated(perkId);
    }
}

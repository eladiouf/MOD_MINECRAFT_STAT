package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import tong.statmod.integration.tensura.PlayerDataTensuraHook;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public final class PlayerDataBridge {
    private static Boolean loaded;

    private PlayerDataBridge() {}

    public static int getSoulLevel(Player player) {
        return loaded() && player != null ? PlayerDataTensuraHook.getSoulLevel(player) : 0;
    }

    public static Optional<String> getOptionalRaceId(Player player) {
        return loaded() && player != null
                ? PlayerDataTensuraHook.getOptionalRaceId(player)
                : Optional.empty();
    }

    public static String getRaceId(Player player) {
        return getOptionalRaceId(player).orElse("tensura:human");
    }

    public static boolean isRace(Player player, String raceId) {
        return raceId != null && raceId.equals(getRaceId(player));
    }

    public static boolean hasSkill(Player player, String skillId) {
        return loaded() && player != null && PlayerDataTensuraHook.hasSkill(player, skillId);
    }

    public static Set<String> getIntrinsicSkillIds(Player player) {
        return loaded() && player != null ? PlayerDataTensuraHook.getIntrinsicSkillIds(player) : Set.of();
    }

    static boolean hasSkillId(Collection<String> learnedSkillIds, Collection<String> intrinsicSkillIds, String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return false;
        }
        String canonical = tong.statmod.integration.tensura.TensuraSkillIds.canonicalize(skillId);

        return (learnedSkillIds != null && learnedSkillIds.contains(canonical))
                || (intrinsicSkillIds != null && intrinsicSkillIds.contains(canonical));
    }

    private static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("tensura");
        }
        return loaded;
    }
}

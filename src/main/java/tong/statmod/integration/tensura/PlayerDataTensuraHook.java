package tong.statmod.integration.tensura;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.impl.RaceStorage;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.manascore.storage.impl.StorageManager;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerDataTensuraHook {
    private PlayerDataTensuraHook() {}

    public static int getSoulLevel(Player player) {
        ExistenceStorage existenceStorage = StorageManager.getStorage(holder(player), ExistenceStorage.getKey());
        return existenceStorage != null ? existenceStorage.getSoulPoints() : 0;
    }

    public static IExistence getExistence(LivingEntity entity) {
        return StorageManager.getStorage(holder(entity), ExistenceStorage.getKey());
    }

    public static IExistence getExistence(Player player) {
        return getExistence((LivingEntity) player);
    }

    public static Optional<ManasRaceInstance> getRaceInstance(Player player) {
        RaceStorage raceStorage = StorageManager.getStorage(holder(player), RaceStorage.getKey());
        return raceStorage != null ? raceStorage.getRace() : Optional.empty();
    }

    public static Optional<String> getOptionalRaceId(Player player) {
        return getRaceInstance(player)
                .map(race -> race.getRaceId().toString())
                .filter(raceId -> raceId != null && !raceId.isBlank());
    }

    public static Set<String> getIntrinsicSkillIds(Player player) {
        RaceStorage raceStorage = StorageManager.getStorage(holder(player), RaceStorage.getKey());
        return raceStorage == null
                ? Set.of()
                : raceStorage.getRace()
                .map(instance -> instance.getIntrinsicSkills(player).stream()
                        .map(skill -> TensuraSkillIds.canonicalize(skill.getRegistryName().toString()))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public static boolean hasSkill(Player player, String skillId) {
        SkillStorage skillStorage = StorageManager.getStorage(holder(player), SkillStorage.getKey());
        Set<String> learnedSkillIds = skillStorage == null
                ? Set.of()
                : skillStorage.getLearnedSkills().stream()
                .map(skill -> TensuraSkillIds.canonicalize(skill.getSkillId().toString()))
                .collect(Collectors.toSet());
        return hasSkillId(learnedSkillIds, getIntrinsicSkillIds(player), skillId);
    }

    private static StorageHolder holder(LivingEntity entity) {
        return (StorageHolder) entity;
    }

    private static boolean hasSkillId(Collection<String> learnedSkillIds,
                                      Collection<String> intrinsicSkillIds,
                                      String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return false;
        }
        String canonical = TensuraSkillIds.canonicalize(skillId);

        return (learnedSkillIds != null && learnedSkillIds.contains(canonical))
                || (intrinsicSkillIds != null && intrinsicSkillIds.contains(canonical));
    }
}

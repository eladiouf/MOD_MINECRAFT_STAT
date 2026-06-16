package tong.statmod.integration;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.impl.RaceStorage;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.manascore.storage.impl.StorageManager;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerDataBridge {
    private PlayerDataBridge() {}

    private static StorageHolder holder(LivingEntity entity) {
        return (StorageHolder) entity;
    }

    public static int getSoulLevel(Player player) {
        ExistenceStorage es = StorageManager.getStorage(holder(player), ExistenceStorage.getKey());
        return es != null ? es.getSoulPoints() : 0;
    }

    public static IExistence getExistence(LivingEntity entity) {
        return StorageManager.getStorage(holder(entity), ExistenceStorage.getKey());
    }

    public static IExistence getExistence(Player player) {
        return getExistence((LivingEntity) player);
    }

    public static Optional<ManasRaceInstance> getRaceInstance(Player player) {
        RaceStorage rs = StorageManager.getStorage(holder(player), RaceStorage.getKey());
        return rs != null ? rs.getRace() : Optional.empty();
    }

    public static String getRaceId(Player player) {
        return getRaceInstance(player)
                .map(r -> r.getRaceId().toString())
                .orElse("tensura:human");
    }

    public static boolean isRace(Player player, String raceId) {
        return getRaceId(player).equals(raceId);
    }

    public static boolean hasSkill(Player player, String skillId) {
        RaceStorage rs = StorageManager.getStorage(holder(player), RaceStorage.getKey());
        if (rs == null) return false;
        return rs.getRace()
                .map(instance -> instance.getIntrinsicSkills(player).stream()
                        .anyMatch(s -> s.getRegistryName().toString().equals(skillId)))
                .orElse(false);
    }
}

package tong.statmod.economy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.dungeon.DungeonDimensions;

/** Garantit un banquier lorsqu'un joueur visite un village chargé. */
public final class VillageBankerSpawner {
    private VillageBankerSpawner() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 200 != 0) return;
        if (!(player.level() instanceof ServerLevel level) || level.dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (!level.isVillage(player.blockPosition())) return;
        var area = player.getBoundingBox().inflate(64.0);
        if (!level.getEntitiesOfClass(Villager.class, area,
                v -> v.getPersistentData().getBoolean(MagicBanker.TAG)).isEmpty()) return;
        var villagers = level.getEntitiesOfClass(Villager.class, area,
                v -> !v.getPersistentData().getBoolean(MagicBanker.TAG));
        if (villagers.isEmpty()) return;
        MagicBanker.spawn(level, villagers.getFirst().blockPosition().offset(2, 0, 0));
    }
}

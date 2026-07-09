package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.integration.puffish.PuffishMagicTreeBuilder;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.stamina.StaminaData;

import java.util.UUID;

public final class SyncHelper {
    private static final SyncSnapshotGate SNAPSHOT_GATE = new SyncSnapshotGate();

    private SyncHelper() {}

    public static void syncStats(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        StatUpdatePayload payload = new StatUpdatePayload(
                data.getLevels(), data.getXp(), data.getSoulLevel(),
                data.getDungeonPoints(), data.getDungeonFloorReached());
        if (!SNAPSHOT_GATE.shouldSendStats(player.getUUID(), payload.levels(), payload.xp(),
                payload.soulLevel(), payload.dungeonPoints(), payload.dungeonFloorReached())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void syncPerks(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        SyncPerksPayload payload = new SyncPerksPayload(data.getUnlockedPerks(), data.getPerkPoints());
        if (!SNAPSHOT_GATE.shouldSendPerks(player.getUUID(), payload.perkIds(), payload.perStatPoints())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        PuffishSkillsCompat.sync(player, data);
    }

    public static void syncStamina(ServerPlayer player) {
        StaminaData data = player.getData(ModAttachments.STAMINA);
        StaminaSyncPayload payload = new StaminaSyncPayload(
                data.currentStamina(), data.fatigueDebt(), data.meditating());
        if (!SNAPSHOT_GATE.shouldSendStamina(player.getUUID(), payload.currentStamina(),
                payload.fatigueDebt(), payload.meditating())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void syncMagic(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        SyncMagicPayload payload = MagicStateSyncService.payload(data);
        if (!SNAPSHOT_GATE.shouldSendMagic(player.getUUID(),
                payload.magicNodes(), payload.learnedSpells(), payload.magicPoints(),
                payload.masteryProgress(), payload.raceOrdinal(), payload.startBranchOrdinal())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        PuffishMagicTreeBuilder.applyMirror(player);
    }

    public static void syncAll(ServerPlayer player) {
        syncStats(player);
        syncPerks(player);
        syncStamina(player);
        syncMagic(player);
    }

    public static void clearPlayerCache(UUID playerId) {
        SNAPSHOT_GATE.clear(playerId);
    }
}

package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;
import tong.statmod.stamina.StaminaData;

public final class SyncHelper {
    private SyncHelper() {}

    public static void syncStats(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PacketDistributor.sendToPlayer(player,
                new StatUpdatePayload(data.getLevels(), data.getXp(), data.getSoulLevel()));
    }

    public static void syncPerks(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PacketDistributor.sendToPlayer(player,
                new SyncPerksPayload(data.getUnlockedPerks(), data.getPerkPoints()));
        PuffishSkillsCompat.sync(player, data);
    }

    public static void syncStamina(ServerPlayer player) {
        StaminaData data = player.getData(ModAttachments.STAMINA);
        PacketDistributor.sendToPlayer(player,
                new StaminaSyncPayload(data.currentStamina(), data.fatigueDebt(), data.meditating()));
    }

    public static void syncMagic(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PacketDistributor.sendToPlayer(player,
                new SyncMagicPayload(data.getMagicNodes(), data.getLearnedSpells(),
                        data.getArcanePoints(), data.getSchoolPointsArray(),
                        data.getMagicRace() != null ? data.getMagicRace().ordinal() : -1,
                        data.getChosenStartBranch() != null ? data.getChosenStartBranch().ordinal() : -1));
    }

    public static void syncAll(ServerPlayer player) {
        syncStats(player);
        syncPerks(player);
        syncStamina(player);
        syncMagic(player);
    }
}

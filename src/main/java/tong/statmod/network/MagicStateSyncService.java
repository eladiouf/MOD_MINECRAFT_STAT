package tong.statmod.network;

import tong.statmod.storage.PlayerStatData;

public final class MagicStateSyncService {
    private MagicStateSyncService() {}

    public static SyncMagicPayload payload(PlayerStatData data) {
        if (data == null) {
            return new SyncMagicPayload(new String[0], new String[0], 0, new int[0], -1, -1);
        }
        return new SyncMagicPayload(
                data.getMagicNodes(),
                data.getLearnedSpells(),
                data.getArcanePoints(),
                data.getSchoolPointsArray(),
                data.getMagicRace() != null ? data.getMagicRace().ordinal() : -1,
                data.getChosenStartBranch() != null ? data.getChosenStartBranch().ordinal() : -1
        );
    }

    public static void sync(PlayerStatData data, java.util.function.Consumer<SyncMagicPayload> payloadSink,
                            Runnable mirrorSync) {
        if (data == null) {
            return;
        }
        if (payloadSink != null) {
            payloadSink.accept(payload(data));
        }
        if (mirrorSync != null) {
            mirrorSync.run();
        }
    }
}

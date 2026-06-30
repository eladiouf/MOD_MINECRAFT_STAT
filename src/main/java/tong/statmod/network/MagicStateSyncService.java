package tong.statmod.network;

import tong.statmod.storage.PlayerStatData;

public final class MagicStateSyncService {
    private MagicStateSyncService() {}

    public static SyncMagicPayload payload(PlayerStatData data) {
        if (data == null) {
            return new SyncMagicPayload(new String[0], new String[0], 0, new int[0], -1, -1);
        }
        // Mastery progress par école — utilisé par Mage Codex pour afficher la progression.
        int[] mastery = new int[tong.statmod.magic.MagicBranch.values().length];
        for (tong.statmod.magic.MagicBranch b : tong.statmod.magic.MagicBranch.values()) {
            mastery[b.ordinal()] = data.getSchoolMasteryProgress(b);
        }
        return new SyncMagicPayload(
                data.getMagicNodes(),
                data.getLearnedSpells(),
                data.getMagicPoints(),
                mastery,
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

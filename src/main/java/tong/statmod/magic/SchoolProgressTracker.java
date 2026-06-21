package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public final class SchoolProgressTracker {
    public static final int MASTERY_PER_POINT = 100;

    private SchoolProgressTracker() {}

    public static int applyMastery(PlayerStatData data, MagicBranch branch, int amount) {
        if (data == null || branch == null || amount <= 0) return 0;
        int progress = data.getSchoolMasteryProgress(branch) + amount;
        int granted = progress / MASTERY_PER_POINT;
        int remainder = progress % MASTERY_PER_POINT;
        data.setSchoolMasteryProgress(branch, remainder);
        if (granted > 0) data.addSchoolPoints(branch, granted);
        return granted;
    }
}

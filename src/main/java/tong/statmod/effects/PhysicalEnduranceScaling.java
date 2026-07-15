package tong.statmod.effects;

public final class PhysicalEnduranceScaling {
    private PhysicalEnduranceScaling() {
    }

    public static double bonus(int level, double bonusAt100) {
        if (!Double.isFinite(bonusAt100) || bonusAt100 <= 0.0) {
            return 0.0;
        }
        int safeLevel = Math.max(0, Math.min(100, level));
        return bonusAt100 * safeLevel / 100.0;
    }
}

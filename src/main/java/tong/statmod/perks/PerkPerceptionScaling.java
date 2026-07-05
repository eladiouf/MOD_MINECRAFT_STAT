package tong.statmod.perks;

public final class PerkPerceptionScaling {
    private static final int SCENT_TRAIL_TICKS = 200;
    private static final int HIDDEN_MOB_REVEAL_TICKS = 60;

    private PerkPerceptionScaling() {}

    public static int scentTrailDurationTicks(boolean trackingSituationalUnlocked, boolean realHit) {
        return trackingSituationalUnlocked && realHit ? SCENT_TRAIL_TICKS : 0;
    }

    public static int hiddenMobRevealDurationTicks(boolean senseSynergyUnlocked, boolean hiddenMob) {
        return senseSynergyUnlocked && hiddenMob ? HIDDEN_MOB_REVEAL_TICKS : 0;
    }

    public static boolean canRevealHealthReadout(boolean senseTranscendenceUnlocked, boolean targetAlive) {
        return senseTranscendenceUnlocked && targetAlive;
    }
}

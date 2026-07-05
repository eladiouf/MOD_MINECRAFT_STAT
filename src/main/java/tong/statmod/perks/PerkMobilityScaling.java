package tong.statmod.perks;

public final class PerkMobilityScaling {
    private static final int RAPID_DODGE_SLOWDOWN_TICKS = 40;
    private static final int RAPID_PERFECT_DODGE_STOP_TICKS = 40;

    private PerkMobilityScaling() {}

    public static int rapidDodgeSlowdownDurationTicks(boolean rapidMasteryUnlocked, boolean dodgeTriggered) {
        return rapidMasteryUnlocked && dodgeTriggered ? RAPID_DODGE_SLOWDOWN_TICKS : 0;
    }

    public static int rapidPerfectDodgeStopDurationTicks(boolean rapidTranscendenceUnlocked,
                                                        boolean perfectDodgeTriggered) {
        return rapidTranscendenceUnlocked && perfectDodgeTriggered ? RAPID_PERFECT_DODGE_STOP_TICKS : 0;
    }

    public static boolean agilityUntouchableDodgesIncomingHit(boolean agilityTranscendenceUnlocked,
                                                              boolean postDamageWindowActive) {
        return agilityTranscendenceUnlocked && postDamageWindowActive;
    }

    public static boolean agilityUntouchableStartsWindow(boolean agilityTranscendenceUnlocked,
                                                         float incomingDamageAfterReductions) {
        return agilityTranscendenceUnlocked && incomingDamageAfterReductions > 0.0f;
    }
}

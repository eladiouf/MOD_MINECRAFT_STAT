package tong.statmod.stamina;

public final class MeditationManager {
    private MeditationManager() {}

    public static void start(StaminaData data) {
        if (data != null) {
            data.setMeditating(true);
        }
    }

    public static void stop(StaminaData data) {
        if (data != null) {
            data.setMeditating(false);
        }
    }
}

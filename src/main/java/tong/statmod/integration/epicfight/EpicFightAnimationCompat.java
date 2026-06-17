package tong.statmod.integration.epicfight;

public final class EpicFightAnimationCompat {
    private EpicFightAnimationCompat() {
    }

    public static boolean shouldSkipPlayback(Object animationAccessor) {
        return animationAccessor == null;
    }
}

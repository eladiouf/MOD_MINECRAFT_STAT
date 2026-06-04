package tong.statmod.util;

import tong.statmod.STATMod;

public class LagDetector {
    private static final long WARN_THRESHOLD_NS = 1_000_000L;

    public static void check(String handler, long startNs) {
        long elapsed = System.nanoTime() - startNs;
        if (elapsed > WARN_THRESHOLD_NS) {
            STATMod.LOGGER.warn("Slow tick in {}: {}ms", handler, elapsed / 1_000_000);
        }
    }
}

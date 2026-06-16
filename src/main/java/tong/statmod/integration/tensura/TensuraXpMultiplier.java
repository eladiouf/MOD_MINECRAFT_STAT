package tong.statmod.integration.tensura;

import tong.statmod.progression.ActionType;
import tong.statmod.storage.PlayerStatData;

public final class TensuraXpMultiplier {
    private TensuraXpMultiplier() {}

    public static float getEpMultiplier(PlayerStatData data, String actionType) {
        if (data == null) {
            return 1.0f;
        }
        return 1.0f + totalStatLevel(data) * 0.005f;
    }

    public static double applyEpMultiplier(PlayerStatData data, String actionType, double baseEp) {
        return Math.max(0.0d, baseEp) * getEpMultiplier(data, actionType);
    }

    public static float getEpMultiplierForAction(PlayerStatData data, ActionType actionType) {
        return actionType == null ? 1.0f : getEpMultiplier(data, actionType.name().toLowerCase());
    }

    private static int totalStatLevel(PlayerStatData data) {
        int total = 0;
        for (int level : data.getLevels()) {
            total += Math.max(0, level);
        }
        return total;
    }
}

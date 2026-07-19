package tong.statmod.progression.xp;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class XpAwardCoordinator {
    private XpAwardCoordinator() {
    }

    public static XpAwardResult apply(
            PlayerStats stats, PlayerXpState state, List<XpAction> actions, long tick) {
        if (stats == null || state == null || actions == null || actions.isEmpty()) {
            return new XpAwardResult(false, Map.of());
        }

        Map<AwardGroup, Integer> proposed = new LinkedHashMap<>();
        for (XpAction action : actions) {
            if (action == null) {
                continue;
            }
            for (StatXpAward award : XpRewardPolicy.awards(action)) {
                AwardGroup group = new AwardGroup(award.stat(), action.opponentId());
                proposed.merge(group, award.amount(), XpAwardCoordinator::saturatedAdd);
            }
        }

        EnumMap<StatType, Integer> acceptedByStat = new EnumMap<>(StatType.class);
        boolean changed = false;
        for (Map.Entry<AwardGroup, Integer> entry : proposed.entrySet()) {
            AwardGroup group = entry.getKey();
            StatValue before = stats.get(group.stat());
            if (before.level() >= 100) {
                continue;
            }
            int accepted = state.acceptXp(
                    group.stat(), entry.getValue(), tick, group.opponentId());
            if (accepted <= 0) {
                continue;
            }
            stats.addXp(group.stat(), accepted);
            StatValue after = stats.get(group.stat());
            if (!after.equals(before)) {
                acceptedByStat.merge(group.stat(), accepted, XpAwardCoordinator::saturatedAdd);
                changed = true;
            }
        }
        return new XpAwardResult(changed, acceptedByStat);
    }

    private static int saturatedAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }

    private record AwardGroup(StatType stat, UUID opponentId) {
    }
}

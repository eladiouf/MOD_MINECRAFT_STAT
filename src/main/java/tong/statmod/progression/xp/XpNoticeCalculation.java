package tong.statmod.progression.xp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class XpNoticeCalculation {
    private XpNoticeCalculation() {
    }

    public static List<XpProgressNotice> from(
            Map<StatType, StatValue> before,
            Map<StatType, StatValue> after,
            Map<StatType, Integer> accepted) {
        if (before == null || after == null || accepted == null || accepted.isEmpty()) {
            return List.of();
        }
        List<XpProgressNotice> notices = new ArrayList<>();
        for (StatType stat : StatType.values()) {
            int awardedXp = Math.max(0, accepted.getOrDefault(stat, 0));
            StatValue beforeValue = before.get(stat);
            StatValue afterValue = after.get(stat);
            if (awardedXp == 0 || beforeValue == null || afterValue == null
                    || beforeValue.equals(afterValue)) {
                continue;
            }
            notices.add(new XpProgressNotice(
                    stat,
                    awardedXp,
                    afterValue.level(),
                    Math.max(0, afterValue.level() - beforeValue.level())));
        }
        return List.copyOf(notices);
    }
}

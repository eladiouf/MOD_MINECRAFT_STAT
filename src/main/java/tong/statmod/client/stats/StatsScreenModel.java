package tong.statmod.client.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import tong.statmod.client.ClientStatsState;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class StatsScreenModel {
    private final long revision;
    private final List<FamilySection> families;
    private final Map<StatType, StatCard> cards;

    private StatsScreenModel(long revision, List<FamilySection> families,
            Map<StatType, StatCard> cards) {
        this.revision = revision;
        this.families = List.copyOf(families);
        this.cards = Collections.unmodifiableMap(new EnumMap<>(cards));
    }

    public static StatsScreenModel from(ClientStatsState state) {
        EnumMap<StatType, StatCard> cards = new EnumMap<>(StatType.class);
        List<FamilySection> families = new ArrayList<>();
        for (StatFamily family : StatFamily.values()) {
            List<StatCard> familyCards = new ArrayList<>();
            for (StatType type : StatType.values()) {
                if (type.family() != family) {
                    continue;
                }
                StatValue source = state.values().getOrDefault(type, new StatValue(0, 0));
                int level = Math.max(0, Math.min(StatProgress.MAX_LEVEL, source.level()));
                int requiredXp = StatProgress.requiredXp(level);
                int xp = requiredXp == 0 ? 0 : Math.max(0, Math.min(requiredXp, source.xp()));
                double progress = requiredXp == 0 ? 1.0 : xp / (double) requiredXp;
                StatCard card = new StatCard(type, new StatValue(level, xp), requiredXp,
                        progress, level == StatProgress.MAX_LEVEL, StatPresentation.of(type));
                cards.put(type, card);
                familyCards.add(card);
            }
            families.add(new FamilySection(family, List.copyOf(familyCards)));
        }
        return new StatsScreenModel(state.revision(), families, cards);
    }

    public long revision() {
        return revision;
    }

    public List<FamilySection> families() {
        return families;
    }

    public StatCard card(StatType type) {
        return cards.get(type);
    }

    public record FamilySection(StatFamily family, List<StatCard> cards) {
    }

    public record StatCard(StatType type, StatValue value, int requiredXp,
            double progress, boolean maxLevel, StatPresentation presentation) {
    }
}

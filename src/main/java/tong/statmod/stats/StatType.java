package tong.statmod.stats;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StatType {
    BRUTE_FORCE("brute_force", StatFamily.FRONT_LINE_PHYSICAL),
    BLADE_TECHNIQUE("blade_technique", StatFamily.FRONT_LINE_PHYSICAL),
    RAPIDITE("rapidite", StatFamily.FRONT_LINE_PHYSICAL),
    AGILITY("agility", StatFamily.FRONT_LINE_PHYSICAL),
    PHYSICAL_RESISTANCE("physical_resistance", StatFamily.FRONT_LINE_PHYSICAL),
    PHYSICAL_ENDURANCE("physical_endurance", StatFamily.FRONT_LINE_PHYSICAL),
    PRECISION("precision", StatFamily.RANGED_HUNT),
    TRACKING("tracking", StatFamily.RANGED_HUNT),
    KEEN_SENSES("keen_senses", StatFamily.RANGED_HUNT),
    ARCANE_POWER("arcane_power", StatFamily.MAGICAL_CORE),
    CASTING_SPEED("casting_speed", StatFamily.MAGICAL_CORE),
    MANA_POOL("mana_pool", StatFamily.MAGICAL_CORE),
    ERUDITION("erudition", StatFamily.MAGICAL_CORE),
    MAGIC_RESISTANCE("magic_resistance", StatFamily.MAGICAL_CORE),
    INTIMIDATION("intimidation", StatFamily.MENTAL_RESILIENCE),
    WILLPOWER("willpower", StatFamily.MENTAL_RESILIENCE),
    FORGING("forging", StatFamily.CRAFTING_SUPPORT),
    COOKING("cooking", StatFamily.CRAFTING_SUPPORT),
    ALCHEMY("alchemy", StatFamily.CRAFTING_SUPPORT);

    private static final Map<String, StatType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(StatType::id, Function.identity()));

    private final String id;
    private final StatFamily family;

    StatType(String id, StatFamily family) {
        this.id = id;
        this.family = family;
    }

    public String id() {
        return id;
    }

    public StatFamily family() {
        return family;
    }

    public static Optional<StatType> fromId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}

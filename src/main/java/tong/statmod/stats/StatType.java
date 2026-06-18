package tong.statmod.stats;

public enum StatType {
    BRUTE_FORCE(0, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Brute Force", "Damage bonus for heavy weapons"),
    BLADE_TECHNIQUE(1, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Blade Technique", "Precision and finesse with blades"),
    RAPIDITE(2, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Rapidité", "Attack speed and fluidity"),
    AGILITY(3, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Agility", "Movement speed and evasion"),
    PHYSICAL_RESISTANCE(4, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Physical Resistance", "Incoming damage reduction"),
    PHYSICAL_ENDURANCE(5, StatFamily.FRONTLINE_PHYSICAL_COMBAT, "Physical Endurance", "Stamina and absorption"),
    PRECISION(6, StatFamily.RANGED_HUNT_CONTROL, "Precision", "Ranged accuracy and critical hits"),
    ARCANE_POWER(7, StatFamily.MAGICAL_CORE, "Arcane Power", "Raw magical damage"),
    WATER_AFFINITY(8, StatFamily.ELEMENTAL_SPECIALIZATION, "Water Affinity", "Water magic effectiveness"),
    EARTH_AFFINITY(9, StatFamily.ELEMENTAL_SPECIALIZATION, "Earth Affinity", "Earth magic effectiveness"),
    FIRE_AFFINITY(10, StatFamily.ELEMENTAL_SPECIALIZATION, "Fire Affinity", "Fire magic effectiveness"),
    AIR_AFFINITY(11, StatFamily.ELEMENTAL_SPECIALIZATION, "Air Affinity", "Air magic effectiveness"),
    MAGIC_RESISTANCE(12, StatFamily.MAGICAL_CORE, "Magic Resistance", "Magic damage reduction"),
    CASTING_SPEED(13, StatFamily.MAGICAL_CORE, "Casting Speed", "Faster spell casting"),
    MANA_POOL(14, StatFamily.MAGICAL_CORE, "Mana Pool", "Maximum mana"),
    ERUDITION(15, StatFamily.MAGICAL_CORE, "Erudition", "Spell variety and learning"),
    TRACKING(16, StatFamily.RANGED_HUNT_CONTROL, "Tracking", "Mob detection and marking"),
    KEEN_SENSES(17, StatFamily.RANGED_HUNT_CONTROL, "Keen Senses", "Dodge and perception"),
    FORGING(18, StatFamily.CRAFTING_SUPPORT, "Forging", "Tool and weapon repair"),
    COOKING(19, StatFamily.CRAFTING_SUPPORT, "Cooking", "Food saturation"),
    ALCHEMY(20, StatFamily.CRAFTING_SUPPORT, "Alchemy", "Potion duration"),
    INTIMIDATION(21, StatFamily.MENTAL_PRESSURE_RESILIENCE, "Intimidation", "Bonus damage to marked targets"),
    WILLPOWER(22, StatFamily.MENTAL_PRESSURE_RESILIENCE, "Willpower", "Status effect resistance");

    private static final StatType[] BY_INDEX = new StatType[values().length];
    static {
        for (StatType s : values()) BY_INDEX[s.index] = s;
    }

    public final int index;
    public final StatFamily family;
    public final String displayName;
    public final String description;

    StatType(int index, StatFamily family, String displayName, String description) {
        this.index = index;
        this.family = family;
        this.displayName = displayName;
        this.description = description;
    }

    public static StatType byIndex(int index) {
        return index >= 0 && index < BY_INDEX.length ? BY_INDEX[index] : null;
    }

    public StatFamily family() {
        return family;
    }

    public boolean hasPerks() {
        return true;
    }
}

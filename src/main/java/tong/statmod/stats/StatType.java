package tong.statmod.stats;

public enum StatType {
    BRUTE_FORCE(0, "Brute Force", "Damage bonus for heavy weapons"),
    BLADE_TECHNIQUE(1, "Blade Technique", "Precision and finesse with blades"),
    RAPIDITE(2, "Rapidité", "Attack speed and fluidity"),
    AGILITY(3, "Agility", "Movement speed and evasion"),
    PHYSICAL_RESISTANCE(4, "Physical Resistance", "Incoming damage reduction"),
    PHYSICAL_ENDURANCE(5, "Physical Endurance", "Stamina and absorption"),
    PRECISION(6, "Precision", "Ranged accuracy and critical hits"),
    ARCANE_POWER(7, "Arcane Power", "Raw magical damage"),
    WATER_AFFINITY(8, "Water Affinity", "Water magic effectiveness"),
    EARTH_AFFINITY(9, "Earth Affinity", "Earth magic effectiveness"),
    FIRE_AFFINITY(10, "Fire Affinity", "Fire magic effectiveness"),
    AIR_AFFINITY(11, "Air Affinity", "Air magic effectiveness"),
    MAGIC_RESISTANCE(12, "Magic Resistance", "Magic damage reduction"),
    CASTING_SPEED(13, "Casting Speed", "Faster spell casting"),
    MANA_POOL(14, "Mana Pool", "Maximum mana"),
    ERUDITION(15, "Erudition", "Spell variety and learning"),
    TRACKING(16, "Tracking", "Mob detection and marking"),
    KEEN_SENSES(17, "Keen Senses", "Dodge and perception"),
    FORGING(18, "Forging", "Tool and weapon repair"),
    COOKING(19, "Cooking", "Food saturation"),
    ALCHEMY(20, "Alchemy", "Potion duration"),
    INTIMIDATION(21, "Intimidation", "Bonus damage to marked targets"),
    WILLPOWER(22, "Willpower", "Status effect resistance");

    private static final StatType[] BY_INDEX = new StatType[values().length];
    static {
        for (StatType s : values()) BY_INDEX[s.index] = s;
    }

    public final int index;
    public final String displayName;
    public final String description;

    StatType(int index, String displayName, String description) {
        this.index = index;
        this.displayName = displayName;
        this.description = description;
    }

    public static StatType byIndex(int index) {
        return index >= 0 && index < BY_INDEX.length ? BY_INDEX[index] : null;
    }

    public boolean hasPerks() {
        return index < 7 || index >= 16;
    }
}

package tong.statmod.stats;

public enum StatType {
    BRUTE_FORCE(0, StatCategory.COMBAT, "Force Brute", 0, 100),
    BLADE_TECHNIQUE(1, StatCategory.COMBAT, "Technique de Lame", 0, 100),
    RAPIDITE(2, StatCategory.COMBAT, "Rapidité", 0, 100),
    AGILITY(3, StatCategory.COMBAT, "Agilité", 0, 100),
    PHYSICAL_RESISTANCE(4, StatCategory.COMBAT, "Résistance Physique", 0, 100),
    PHYSICAL_ENDURANCE(5, StatCategory.COMBAT, "Endurance Physique", 0, 100),
    PRECISION(6, StatCategory.COMBAT, "Précision", 0, 100),
    ARCANE_POWER(7, StatCategory.MAGIC, "Puissance Arcanique", 0, 100),
    WATER_AFFINITY(8, StatCategory.MAGIC, "Affinité Aquatique", 0, 100),
    EARTH_AFFINITY(9, StatCategory.MAGIC, "Affinité Terrestre", 0, 100),
    FIRE_AFFINITY(10, StatCategory.MAGIC, "Affinité Ignée", 0, 100),
    AIR_AFFINITY(11, StatCategory.MAGIC, "Affinité Aérienne", 0, 100),
    MAGIC_RESISTANCE(12, StatCategory.MAGIC, "Résistance Magique", 0, 100),
    CASTING_SPEED(13, StatCategory.MAGIC, "Vitesse d'Incantation", 0, 100),
    MANA_POOL(14, StatCategory.MAGIC, "Réserve de Mana", 0, 100),
    ERUDITION(15, StatCategory.MAGIC, "Érudition", 0, 100),
    TRACKING(16, StatCategory.SURVIVAL, "Pistage", 0, 100),
    KEEN_SENSES(17, StatCategory.SURVIVAL, "Sens Aiguisés", 0, 100),
    FORGING(18, StatCategory.CRAFTING, "Forge", 0, 100),
    COOKING(19, StatCategory.CRAFTING, "Cuisine", 0, 100),
    ALCHEMY(20, StatCategory.CRAFTING, "Alchimie", 0, 100),
    INTIMIDATION(21, StatCategory.MENTAL, "Intimidation", 0, 100),
    WILLPOWER(22, StatCategory.MENTAL, "Volonté", 0, 100);

    public final int index;
    public final StatCategory category;
    public final String displayName;
    public final int minLevel;
    public final int maxLevel;

    StatType(int index, StatCategory category, String displayName, int minLevel, int maxLevel) {
        this.index = index;
        this.category = category;
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public static StatType byIndex(int index) {
        for (StatType s : values()) {
            if (s.index == index) return s;
        }
        return BRUTE_FORCE;
    }
}

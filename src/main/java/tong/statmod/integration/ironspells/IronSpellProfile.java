package tong.statmod.integration.ironspells;

public enum IronSpellProfile {
    FIRE_ARTILLERY,
    FROST_CONTROLLER,
    STORM_HUNTER,
    ARCANE_DUELIST,
    NECROMANTIC_PRESSURE,
    HOLY_SUPPORT;

    public static IronSpellProfile fromElement(String element) {
        return switch (element == null ? "" : element) {
            case "FIRE" -> FIRE_ARTILLERY;
            case "FROST" -> FROST_CONTROLLER;
            case "STORM" -> STORM_HUNTER;
            case "NECRO" -> NECROMANTIC_PRESSURE;
            case "HOLY" -> HOLY_SUPPORT;
            default -> ARCANE_DUELIST;
        };
    }
}

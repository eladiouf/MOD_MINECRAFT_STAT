package tong.statmod.stats;

public enum StatFamily {
    FRONT_LINE_PHYSICAL("front_line_physical"),
    RANGED_HUNT("ranged_hunt"),
    MAGICAL_CORE("magical_core"),
    ELEMENTAL_SPECIALIZATION("elemental_specialization"),
    MENTAL_RESILIENCE("mental_resilience"),
    CRAFTING_SUPPORT("crafting_support");

    private final String slug;

    StatFamily(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}

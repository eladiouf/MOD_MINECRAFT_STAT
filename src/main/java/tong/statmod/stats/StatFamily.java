package tong.statmod.stats;

public enum StatFamily {
    FRONTLINE_PHYSICAL_COMBAT("frontline_physical_combat", "Front-Line Physical Combat"),
    RANGED_HUNT_CONTROL("ranged_hunt_control", "Ranged and Hunt Control"),
    MAGICAL_CORE("magical_core", "Magical Core"),
    ELEMENTAL_SPECIALIZATION("elemental_specialization", "Elemental Specialization"),
    MENTAL_PRESSURE_RESILIENCE("mental_pressure_resilience", "Mental Pressure and Resilience"),
    CRAFTING_SUPPORT("crafting_support", "Crafting, Provisioning, and Technical Support");

    public final String slug;
    public final String displayName;

    StatFamily(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }
}

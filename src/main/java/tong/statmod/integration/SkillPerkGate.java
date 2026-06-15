package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
import tong.statmod.perks.Perk;

import java.util.*;

public final class SkillPerkGate {
    private static final Map<Integer, List<String>> PERK_SKILL_REQUIREMENTS = new HashMap<>();
    private static final Map<Integer, String> PERK_RACE_REQUIREMENTS = new HashMap<>();

    private SkillPerkGate() {}

    public static boolean canUnlock(Player player, Perk perk) {
        if (perk == null) return true;

        List<String> requiredSkills = PERK_SKILL_REQUIREMENTS.getOrDefault(perk.id, List.of());
        for (String skillId : requiredSkills) {
            if (!PlayerDataBridge.hasSkill(player, skillId)) return false;
        }

        String requiredRace = PERK_RACE_REQUIREMENTS.get(perk.id);
        if (requiredRace != null && !PlayerDataBridge.isRace(player, requiredRace)) return false;

        return true;
    }

    public static void requireSkill(int perkId, String... skillIds) {
        PERK_SKILL_REQUIREMENTS
                .computeIfAbsent(perkId, k -> new ArrayList<>())
                .addAll(Arrays.asList(skillIds));
    }

    public static void requireRace(int perkId, String raceId) {
        PERK_RACE_REQUIREMENTS.put(perkId, raceId);
    }

    static {
        // ── TRANSCENDENCE perks require high-tier Tensura skills ──
        // BRUTE_TRANSCENDENCE id=5: Titan's Wrath
        requireSkill(5, "tensura:giant_strength", "tensura:berserk");
        // BLADE_TRANSCENDENCE id=11: One With the Blade
        requireSkill(11, "tensura:sword_meister", "tensura:iaijutsu");
        // RAPID_TRANSCENDENCE id=17: Za Warudo
        requireSkill(17, "tensura:time_manipulation", "tensura:accelerated_thoughts");
        // AGIL_TRANSCENDENCE id=23: Untouchable
        requireSkill(23, "tensura:space_manipulation", "tensura:thought_acceleration");
        // RESIST_TRANSCENDENCE id=29: Immortal
        requireSkill(29, "tensura:ultra_speed_regeneration", "tensura:immortality");
        // ENDUR_TRANSCENDENCE id=35: Limit Break
        requireSkill(35, "tensura:ultimate_skill", "tensura:parallel_existence");
        // PRECI_TRANSCENDENCE id=41: True Strike
        requireSkill(41, "tensura:precision", "tensura:clairvoyance");
        // TRACK_TRANSCENDENCE id=47: Omnisight
        requireSkill(47, "tensura:clairvoyance", "tensura:magic_sense");
        // SENSE_TRANSCENDENCE id=53: Omniscience
        requireSkill(53, "tensura:clairvoyance", "tensura:analytical_appraisal");
        // FORGE_TRANSCENDENCE id=59: Creation
        requireSkill(59, "tensura:creation", "tensura:artificer");
        // COOK_TRANSCENDENCE id=65: Ambrosia
        requireSkill(65, "tensura:cooking", "tensura:gourmet");
        // ALCHEM_TRANSCENDENCE id=71: Eternal Elixir
        requireSkill(71, "tensura:alchemy", "tensura:philosophers_stone");
        // INTIM_TRANSCENDENCE id=77: Absolute Dominion
        requireSkill(77, "tensura:domination", "tensura:charisma");
        // WILL_TRANSCENDENCE id=83: Transcendence
        requireSkill(83, "tensura:ultimate_skill", "tensura:resistance_all");

        // ── MASTERY perks require advanced skills ──
        requireSkill(4, "tensura:monster_combat");         // Colossus
        requireSkill(16, "tensura:accelerated_thoughts");   // Time Skip
        requireSkill(22, "tensura:body_enhancement");       // Spectral Step
        requireSkill(34, "tensura:auto_regeneration");      // Overflowing Vitality
        requireSkill(40, "tensura:precision");              // Deadshot
        requireSkill(46, "tensura:magic_sense");            // Predator
        requireSkill(52, "tensura:intuition");              // Foresight
        requireSkill(58, "tensura:blacksmithing");          // Artificer
        requireSkill(64, "tensura:hospitality");            // Feast
        requireSkill(70, "tensura:alchemy");                // Philosopher's Stone
        requireSkill(76, "tensura:fear_aura");              // Dread Lord
        requireSkill(82, "tensura:resistance_all");         // Indomitable

        // ── Race-gated perks ──
        requireRace(5, "tensura:giant");                    // Titan's Wrath
        requireRace(11, "tensura:kijin");                   // One With the Blade
        requireRace(17, "tensura:demon_slime");             // Za Warudo
        requireRace(29, "tensura:demon_lord");              // Immortal
        requireRace(35, "tensura:demon_slime");             // Limit Break
        requireRace(53, "tensura:dragon");                  // Omniscience
        requireRace(83, "tensura:true_dragon");             // Transcendence

        // ── SYNERGY perks require extra stat skills ──
        requireSkill(2, "tensura:endurance");               // Crushing Force (synergy: PHYS_ENDURANCE)
        requireSkill(8, "tensura:agility_enhance");         // Dance of Blades (synergy: AGILITY)
        requireSkill(14, "tensura:agility_enhance");        // Blinding Speed (synergy: AGILITY)
        requireSkill(20, "tensura:speed_enhance");          // Wind Walker (synergy: RAPIDITE)
        requireSkill(26, "tensura:endurance");              // Fortress (synergy: PHYS_ENDURANCE)
        requireSkill(32, "tensura:resistance_enhance");     // Unstoppable (synergy: RESISTANCE)
        requireSkill(38, "tensura:tracking");               // Hunter's Mark (synergy: TRACKING)
        requireSkill(44, "tensura:intimidation");           // Pack Hunter (synergy: INTIM)
        requireSkill(50, "tensura:tracking");               // Predator's Instinct (synergy: TRACKING)
        requireSkill(56, "tensura:alchemy");                // Enchanting Touch (synergy: ALCHEMY)
        requireSkill(62, "tensura:endurance");              // Nutritionist (synergy: PHYS_ENDURANCE)
        requireSkill(68, "tensura:forging");                // Transmutation (synergy: FORGING)
        requireSkill(74, "tensura:willpower");              // Feared (synergy: WILLPOWER)
        requireSkill(80, "tensura:resistance_enhance");     // Unbreakable (synergy: RESISTANCE)
    }

    public static Set<Integer> gatePerks() {
        Set<Integer> all = new HashSet<>(PERK_SKILL_REQUIREMENTS.keySet());
        all.addAll(PERK_RACE_REQUIREMENTS.keySet());
        return all;
    }

    public static String skillForPerk(int perkId) {
        List<String> skills = PERK_SKILL_REQUIREMENTS.get(perkId);
        return (skills != null && !skills.isEmpty()) ? skills.get(0) : null;
    }

    public static List<String> requiredSkills(int perkId) {
        return PERK_SKILL_REQUIREMENTS.getOrDefault(perkId, List.of());
    }

    public static String requiredRace(int perkId) {
        return PERK_RACE_REQUIREMENTS.get(perkId);
    }

    public static boolean isGated(int perkId) {
        return PERK_SKILL_REQUIREMENTS.containsKey(perkId) || PERK_RACE_REQUIREMENTS.containsKey(perkId);
    }
}

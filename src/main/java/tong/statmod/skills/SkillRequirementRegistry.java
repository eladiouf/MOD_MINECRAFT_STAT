package tong.statmod.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.stats.StatType;

/**
 * Maps every skill (from any source) to its stat requirements.
 *
 * BALANCE FRAMEWORK (budget = sum of all required stat levels):
 *   Tier 0 — Budget 0       — Base skills everyone gets (roll, guard, basic_attack)
 *   Tier 1 — Budget 20-35   — Early game, accessible after a few hours
 *   Tier 2 — Budget 40-60   — Mid game, requires specialization
 *   Tier 3 — Budget 70-100  — Late game, requires heavy investment
 *   Tier 4 — Budget 110-130 — Endgame, requires near-max stats
 *
 * Rules:
 *   - Primary stat > secondary stat (reflects the skill's main nature)
 *   - Passive skills are ~10% cheaper than active skills of same tier
 *   - Movement/dodge skills are more accessible than damage skills
 *   - Magic skills use magic stats, combat skills use combat stats
 *   - Harder content (Nightfall, WoM endgame) → higher tiers
 */
public class SkillRequirementRegistry {

    public record StatRequirement(StatType stat, int minLevel) {}

    private static final Map<ResourceLocation, List<StatRequirement>> REQUIREMENTS = new HashMap<>();

    public static void init() {
        REQUIREMENTS.clear();

        // =====================================================================
        // STAT MOD SKILLS (43) — New system with weapon passives, stat passives,
        // active skills, movers, guards, and identity skills
        // =====================================================================

        // --- WEAPON PASSIVE (12) — No requirements, auto-applied ---
        // sword_mastery, axe_mastery, dagger_mastery, greatsword_mastery,
        // longsword_mastery, spear_mastery, tachi_mastery, uchigatana_mastery,
        // fist_mastery, bow_mastery, pickaxe_mastery, hoe_mastery

        // --- PASSIVE (14) — Tiers at 20/50/80 ---
        // Brute Force
        register("statmod:brute_power", StatType.BRUTE_FORCE, 20);
        register("statmod:brute_rage", StatType.BRUTE_FORCE, 50);
        register("statmod:brute_fury", StatType.BRUTE_FORCE, 80);
        // Blade Technique
        register("statmod:blade_finesse", StatType.BLADE_TECHNIQUE, 20);
        register("statmod:blade_mastery", StatType.BLADE_TECHNIQUE, 50);
        register("statmod:blade_perfection", StatType.BLADE_TECHNIQUE, 80);
        // Rapidité
        register("statmod:rapid_surge", StatType.RAPIDITE, 20);
        register("statmod:rapid_blitz", StatType.RAPIDITE, 50);
        register("statmod:rapid_lightning", StatType.RAPIDITE, 80);
        // Agility
        register("statmod:agility_footwork", StatType.AGILITY, 20);
        register("statmod:agility_evasion", StatType.AGILITY, 50);
        register("statmod:agility_phantom", StatType.AGILITY, 80);
        // Physical Resistance
        register("statmod:resist_iron", StatType.PHYSICAL_RESISTANCE, 20);
        register("statmod:resist_steel", StatType.PHYSICAL_RESISTANCE, 50);

        // --- WEAPON INNATE (7) — Active skills ---
        register("statmod:heavy_strike", StatType.BRUTE_FORCE, 25);
        register("statmod:blade_dance", StatType.BLADE_TECHNIQUE, 25);
        register("statmod:blitz_assault", StatType.RAPIDITE, 25);
        register("statmod:shadow_step", StatType.AGILITY, 30);
        register("statmod:stone_skin", StatType.PHYSICAL_RESISTANCE, 25);
        register("statmod:endurance_surge", StatType.PHYSICAL_ENDURANCE, 25);
        register("statmod:precision_shot", StatType.PRECISION, 25);

        // --- MOVER (3) ---
        register("statmod:quick_step", StatType.AGILITY, 15);
        register("statmod:shadow_leap", StatType.AGILITY, 40);
        register("statmod:wind_dash", StatType.AGILITY, 60);

        // --- GUARD (3) ---
        register("statmod:parry", StatType.PHYSICAL_RESISTANCE, 20);
        register("statmod:guard_fortress", StatType.PHYSICAL_RESISTANCE, 50);
        register("statmod:guard_iron_wall", StatType.PHYSICAL_RESISTANCE, 80);

        // --- IDENTITY (4) ---
        register("statmod:berserker_rage", StatType.BRUTE_FORCE, 80, StatType.WILLPOWER, 50);
        register("statmod:blade_god", StatType.BLADE_TECHNIQUE, 80, StatType.RAPIDITE, 50);
        register("statmod:shadow_dancer", StatType.AGILITY, 80, StatType.RAPIDITE, 50);
        register("statmod:meditation", StatType.WILLPOWER, 30, StatType.ERUDITION, 20);

        // --- PASSIVE (15) — Tiers at Lv.20/50/80 ---
        // Brute Force
        register("statmod:brute_power", StatType.BRUTE_FORCE, 20);
        register("statmod:brute_rage", StatType.BRUTE_FORCE, 50);
        register("statmod:brute_fury", StatType.BRUTE_FORCE, 80);

        // Blade Technique
        register("statmod:blade_finesse", StatType.BLADE_TECHNIQUE, 20);
        register("statmod:blade_mastery", StatType.BLADE_TECHNIQUE, 50);
        register("statmod:blade_perfection", StatType.BLADE_TECHNIQUE, 80);

        // Rapidité
        register("statmod:rapid_surge", StatType.RAPIDITE, 20);
        register("statmod:rapid_blitz", StatType.RAPIDITE, 50);
        register("statmod:rapid_lightning", StatType.RAPIDITE, 80);

        // Agility
        register("statmod:agility_footwork", StatType.AGILITY, 20);
        register("statmod:agility_evasion", StatType.AGILITY, 50);
        register("statmod:agility_phantom", StatType.AGILITY, 80);

        // Physical Resistance
        register("statmod:resist_iron", StatType.PHYSICAL_RESISTANCE, 20);
        register("statmod:resist_steel", StatType.PHYSICAL_RESISTANCE, 50);
        register("statmod:resist_diamond", StatType.PHYSICAL_RESISTANCE, 80);

        // --- WEAPON INNATE (7) — Active skills ---
        register("statmod:heavy_strike", StatType.BRUTE_FORCE, 30);
        register("statmod:blade_dance", StatType.BLADE_TECHNIQUE, 30);
        register("statmod:blitz_assault", StatType.RAPIDITE, 30);
        register("statmod:shadow_step", StatType.AGILITY, 30);
        register("statmod:stone_skin", StatType.PHYSICAL_RESISTANCE, 30);
        register("statmod:endurance_surge", StatType.PHYSICAL_ENDURANCE, 30);
        register("statmod:precision_shot", StatType.PRECISION, 30);

        // --- MOVER (3) — Movement skills ---
        register("statmod:quick_step", StatType.AGILITY, 15);
        register("statmod:shadow_leap", StatType.AGILITY, 40);
        register("statmod:wind_dash", StatType.AGILITY, 60);

        // --- GUARD (3) — Defense skills ---
        register("statmod:parry", StatType.PHYSICAL_RESISTANCE, 20);
        register("statmod:guard_fortress", StatType.PHYSICAL_RESISTANCE, 40);
        register("statmod:guard_iron_wall", StatType.PHYSICAL_RESISTANCE, 60);

        // --- IDENTITY (3) — Ultimate skills ---
        register("statmod:berserker_rage", StatType.BRUTE_FORCE, 80, StatType.WILLPOWER, 50);
        register("statmod:blade_god", StatType.BLADE_TECHNIQUE, 80, StatType.RAPIDITE, 50);
        register("statmod:shadow_dancer", StatType.AGILITY, 80, StatType.RAPIDITE, 50);

        // --- NON-COMBAT (17) — Utility skills ---
        // Magic
        register("statmod:arcane_bolt", StatType.ARCANE_POWER, 20);
        register("statmod:water_heal", StatType.WATER_AFFINITY, 20);
        register("statmod:earth_shield", StatType.EARTH_AFFINITY, 20);
        register("statmod:fireball", StatType.FIRE_AFFINITY, 20);
        register("statmod:air_dash", StatType.AIR_AFFINITY, 20);
        register("statmod:magic_resist", StatType.MAGIC_RESISTANCE, 30);
        register("statmod:fast_cast", StatType.CASTING_SPEED, 25);
        register("statmod:mana_regen", StatType.MANA_POOL, 25);
        register("statmod:study", StatType.ERUDITION, 20);
        // Survival
        register("statmod:track", StatType.TRACKING, 20);
        register("statmod:sense_danger", StatType.KEEN_SENSES, 20);
        // Crafting
        register("statmod:master_forge", StatType.FORGING, 25);
        register("statmod:feast", StatType.COOKING, 25);
        register("statmod:potion_boost", StatType.ALCHEMY, 25);
        // Mental
        register("statmod:meditate", StatType.WILLPOWER, 20, StatType.ERUDITION, 20);
        register("statmod:intimidate", StatType.INTIMIDATION, 25);
        register("statmod:willpower_aura", StatType.WILLPOWER, 30);

        // =====================================================================
        // EPIC FIGHT CORE SKILLS (42)
        // =====================================================================

        // --- Tier 0: Base skills (no requirements) ---
        // basic_attack, roll, step, guard, knockdown_wakeup

        // --- Tier 1: Defense basics (budget 25-30) ---
        register("epicfight:parrying", StatType.PHYSICAL_RESISTANCE, 15, StatType.BLADE_TECHNIQUE, 10);     // 25
        register("epicfight:impact_guard", StatType.PHYSICAL_RESISTANCE, 15, StatType.PHYSICAL_ENDURANCE, 15); // 30

        // --- Tier 2: Combat styles (budget 45-50) ---
        register("epicfight:berserker", StatType.BRUTE_FORCE, 35, StatType.WILLPOWER, 15);           // 50
        register("epicfight:swordmaster", StatType.BLADE_TECHNIQUE, 35, StatType.AGILITY, 15);       // 50
        register("epicfight:technician", StatType.RAPIDITE, 30, StatType.PRECISION, 15);             // 45
        register("epicfight:endurance", StatType.PHYSICAL_ENDURANCE, 35, StatType.PHYSICAL_RESISTANCE, 15); // 50
        register("epicfight:liechtenauer", StatType.BLADE_TECHNIQUE, 35, StatType.PRECISION, 15);    // 50

        // --- Tier 2: Active combat skills (budget 50-60) ---
        register("epicfight:blade_rush", StatType.RAPIDITE, 25, StatType.BLADE_TECHNIQUE, 25);      // 50
        register("epicfight:bonebreaker", StatType.BRUTE_FORCE, 40, StatType.PHYSICAL_ENDURANCE, 15); // 55
        register("epicfight:dancing_edge", StatType.AGILITY, 30, StatType.BLADE_TECHNIQUE, 25);     // 55
        register("epicfight:demolition_leap", StatType.BRUTE_FORCE, 35, StatType.PHYSICAL_ENDURANCE, 20); // 55
        register("epicfight:sweeping_edge", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 20);    // 50
        register("epicfight:steel_whirlwind", StatType.BRUTE_FORCE, 35, StatType.PHYSICAL_ENDURANCE, 20); // 55
        register("epicfight:relentless_combo", StatType.RAPIDITE, 35, StatType.BLADE_TECHNIQUE, 20); // 55
        register("epicfight:sharp_stab", StatType.BLADE_TECHNIQUE, 25, StatType.PRECISION, 20);     // 45
        register("epicfight:grasping_spire", StatType.BLADE_TECHNIQUE, 25, StatType.EARTH_AFFINITY, 20); // 45

        // --- Tier 3: Advanced combat (budget 80-100) ---
        register("epicfight:death_harvest", StatType.BRUTE_FORCE, 50, StatType.INTIMIDATION, 30);    // 80
        register("epicfight:eviscerate", StatType.BLADE_TECHNIQUE, 55, StatType.PRECISION, 30);     // 85
        register("epicfight:heartpiercer", StatType.PRECISION, 50, StatType.BLADE_TECHNIQUE, 35);   // 85
        register("epicfight:meteor_slam", StatType.BRUTE_FORCE, 60, StatType.PHYSICAL_RESISTANCE, 35); // 95
        register("epicfight:phantom_ascent", StatType.AGILITY, 55, StatType.RAPIDITE, 35);          // 90
        register("epicfight:the_guillotine", StatType.BRUTE_FORCE, 60, StatType.INTIMIDATION, 35);  // 95

        // --- Tier 2: Utility (budget 45-55) ---
        register("epicfight:vengeance", StatType.WILLPOWER, 30, StatType.BRUTE_FORCE, 20);          // 50
        register("epicfight:emergency_escape", StatType.AGILITY, 25, StatType.RAPIDITE, 20);        // 45
        register("epicfight:hypervitality", StatType.PHYSICAL_ENDURANCE, 35, StatType.PHYSICAL_RESISTANCE, 20); // 55
        register("epicfight:catharsis", StatType.WILLPOWER, 30, StatType.ERUDITION, 20);            // 50
        register("epicfight:stamina_pillager", StatType.PHYSICAL_ENDURANCE, 30, StatType.RAPIDITE, 20); // 50
        register("epicfight:adrenaline_fiend", StatType.BRUTE_FORCE, 30, StatType.PHYSICAL_ENDURANCE, 20); // 50
        register("epicfight:adaptive_skin", StatType.PHYSICAL_RESISTANCE, 30, StatType.EARTH_AFFINITY, 20); // 50

        // --- Tier 3: Advanced utility (budget 80-100) ---
        register("epicfight:forbidden_strength", StatType.BRUTE_FORCE, 60, StatType.WILLPOWER, 40);  // 100
        register("epicfight:revelation", StatType.ERUDITION, 50, StatType.ARCANE_POWER, 35);        // 85
        register("epicfight:everlasting_allegiance", StatType.WILLPOWER, 45, StatType.INTIMIDATION, 35); // 80

        // --- Tier 2-3: Magic (budget 50-100) ---
        register("epicfight:tsunami", StatType.ARCANE_POWER, 35, StatType.WATER_AFFINITY, 20);      // 55
        register("epicfight:wrathful_lighting", StatType.ARCANE_POWER, 55, StatType.FIRE_AFFINITY, 35); // 90

        // --- Tier 3: Battojutsu (budget 80-85) ---
        register("epicfight:battojutsu", StatType.BLADE_TECHNIQUE, 50, StatType.RAPIDITE, 35);      // 85
        register("epicfight:battojutsu_passive", StatType.BLADE_TECHNIQUE, 50, StatType.RAPIDITE, 30); // 80

        // --- Tier 2: Rushing Tempo (budget 50) ---
        register("epicfight:rushing_tempo", StatType.RAPIDITE, 35, StatType.AGILITY, 15);           // 50

        // =====================================================================
        // EPIC FIGHT EXTRA SKILLS (4)
        // =====================================================================

        // --- Tier 2: Weapon innate skills (budget 50-55) ---
        register("epicfightx:katana_skill", StatType.BLADE_TECHNIQUE, 30, StatType.RAPIDITE, 20);   // 50
        register("epicfightx:otachi_skill", StatType.BLADE_TECHNIQUE, 30, StatType.BRUTE_FORCE, 20); // 50
        register("epicfightx:pickaxe_skill", StatType.BRUTE_FORCE, 25, StatType.FORGING, 20);       // 45
        register("epicfightx:scythe_skill", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 20);    // 50

        // =====================================================================
        // NIGHTFALL SKILLS (29)
        // =====================================================================

        // --- Tier 1: Basic movement/guard (budget 25-35) ---
        register("efn:efn_step", StatType.AGILITY, 15, StatType.RAPIDITE, 10);                      // 25
        register("efn:efn_dodge", StatType.AGILITY, 20, StatType.RAPIDITE, 15);                     // 35
        register("efn:efn_parry", StatType.BLADE_TECHNIQUE, 20, StatType.PHYSICAL_RESISTANCE, 15);  // 35

        // --- Tier 1-2: Light weapons (budget 35-50) ---
        register("efn:shortsword", StatType.BLADE_TECHNIQUE, 25, StatType.RAPIDITE, 15);            // 40
        register("efn:falchion_skill", StatType.BLADE_TECHNIQUE, 25, StatType.RAPIDITE, 20);        // 45
        register("efn:kusabimaru", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 20);             // 50

        // --- Tier 2: Medium weapons (budget 50-60) ---
        register("efn:hf_blade", StatType.BLADE_TECHNIQUE, 35, StatType.RAPIDITE, 20);              // 55
        register("efn:hf_blade_passive", StatType.BLADE_TECHNIQUE, 30, StatType.RAPIDITE, 20);      // 50
        register("efn:crescentmoon", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 25);           // 55
        register("efn:meenlance", StatType.BLADE_TECHNIQUE, 30, StatType.PHYSICAL_ENDURANCE, 25);   // 55
        register("efn:broadblade", StatType.BRUTE_FORCE, 35, StatType.BLADE_TECHNIQUE, 20);         // 55
        register("efn:beastclaw", StatType.AGILITY, 30, StatType.BRUTE_FORCE, 25);                  // 55
        register("efn:pioneer", StatType.TRACKING, 30, StatType.KEEN_SENSES, 20);                   // 50

        // --- Tier 2: Murasama (budget 55-60) ---
        register("efn:murasama", StatType.BLADE_TECHNIQUE, 40, StatType.RAPIDITE, 20);              // 60
        register("efn:murasama_passive", StatType.BLADE_TECHNIQUE, 35, StatType.RAPIDITE, 20);      // 55
        register("efn:murasama_dodge", StatType.AGILITY, 35, StatType.RAPIDITE, 25);                // 60
        register("efn:murasama_parry", StatType.BLADE_TECHNIQUE, 35, StatType.PHYSICAL_RESISTANCE, 25); // 60

        // --- Tier 2: Parry master (budget 60) ---
        register("efn:parry_master", StatType.BLADE_TECHNIQUE, 40, StatType.PHYSICAL_RESISTANCE, 20); // 60

        // --- Tier 2-3: Misc (budget 45-55) ---
        register("efn:stomp", StatType.BRUTE_FORCE, 30, StatType.PHYSICAL_ENDURANCE, 20);           // 50
        register("efn:thornwheel", StatType.AGILITY, 30, StatType.RAPIDITE, 20);                    // 50
        register("efn:zansetsu", StatType.BLADE_TECHNIQUE, 40, StatType.PRECISION, 20);             // 60

        // --- Tier 3: Heavy weapons (budget 75-85) ---
        register("efn:ruinsgreatsword", StatType.BRUTE_FORCE, 50, StatType.PHYSICAL_ENDURANCE, 30); // 80

        // --- Tier 3: Yamato (budget 75-80) ---
        register("efn:yamato", StatType.BLADE_TECHNIQUE, 50, StatType.WILLPOWER, 30);               // 80
        register("efn:yamato_passive", StatType.BLADE_TECHNIQUE, 45, StatType.WILLPOWER, 30);       // 75

        // --- Tier 3: Active skills (budget 80-100) ---
        register("efn:aetherialdusk", StatType.AGILITY, 45, StatType.ARCANE_POWER, 35);             // 80
        register("efn:bloodlust", StatType.BRUTE_FORCE, 50, StatType.INTIMIDATION, 35);             // 85
        register("efn:execution", StatType.BRUTE_FORCE, 55, StatType.INTIMIDATION, 35);             // 90

        // --- Tier 3-4: Legendary (budget 90-110) ---
        register("efn:mortal_blade", StatType.BLADE_TECHNIQUE, 60, StatType.WILLPOWER, 35);         // 95
        register("efn:judgementcutend", StatType.BLADE_TECHNIQUE, 65, StatType.RAPIDITE, 40);       // 105

        // =====================================================================
        // WEAPONS OF MIRACLES SKILLS (56)
        // =====================================================================

        // --- Tier 1: Basics (budget 25-35) ---
        register("wom:natural_sprinter", StatType.AGILITY, 15, StatType.PHYSICAL_ENDURANCE, 10);    // 25
        register("wom:shooting_style", StatType.PRECISION, 15, StatType.RAPIDITE, 10);              // 25
        register("wom:precise_roll", StatType.AGILITY, 20, StatType.PRECISION, 15);                 // 35
        register("wom:spider_techniques", StatType.AGILITY, 20, StatType.EARTH_AFFINITY, 15);       // 35
        register("wom:meditation", StatType.ERUDITION, 20, StatType.CASTING_SPEED, 15);             // 35

        // --- Tier 1: Early combat (budget 40-45) ---
        register("wom:bull_charge", StatType.BRUTE_FORCE, 25, StatType.PHYSICAL_ENDURANCE, 15);     // 40
        register("wom:counter_attack", StatType.PHYSICAL_RESISTANCE, 25, StatType.BLADE_TECHNIQUE, 15); // 40
        register("wom:punishment_kick", StatType.AGILITY, 25, StatType.BRUTE_FORCE, 15);            // 40
        register("wom:ender_step", StatType.AGILITY, 20, StatType.ARCANE_POWER, 20);                // 40
        register("wom:aqua_maneuvre", StatType.WATER_AFFINITY, 25, StatType.AGILITY, 15);           // 40

        // --- Tier 1: Early passives (budget 35) ---
        register("wom:adrenaline", StatType.PHYSICAL_ENDURANCE, 20, StatType.RAPIDITE, 15);         // 35
        register("wom:dopamine", StatType.WILLPOWER, 20, StatType.PHYSICAL_ENDURANCE, 15);          // 35
        register("wom:mindset", StatType.ERUDITION, 20, StatType.WILLPOWER, 15);                    // 35
        register("wom:pain_anticipation", StatType.PHYSICAL_RESISTANCE, 20, StatType.KEEN_SENSES, 15); // 35
        register("wom:critical_knowledge", StatType.PRECISION, 20, StatType.ERUDITION, 15);         // 35
        register("wom:arrow_tenacity", StatType.PRECISION, 20, StatType.PHYSICAL_ENDURANCE, 15);    // 35
        register("wom:rechargement", StatType.PHYSICAL_ENDURANCE, 20, StatType.RAPIDITE, 15);       // 35

        // --- Tier 2: Mid combat (budget 50-60) ---
        register("wom:buster_parade", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 20);          // 50
        register("wom:dancing_blade", StatType.BLADE_TECHNIQUE, 30, StatType.AGILITY, 20);          // 50
        register("wom:back_and_forth", StatType.RAPIDITE, 30, StatType.BLADE_TECHNIQUE, 20);        // 50
        register("wom:vengeful_parry", StatType.PHYSICAL_RESISTANCE, 30, StatType.BLADE_TECHNIQUE, 20); // 50
        register("wom:agony_plunge", StatType.BRUTE_FORCE, 35, StatType.PHYSICAL_ENDURANCE, 20);    // 55

        // --- Tier 2: Ender (budget 50-70) ---
        register("wom:ender_obscuris", StatType.ARCANE_POWER, 30, StatType.AIR_AFFINITY, 20);       // 50
        register("wom:ender_blast", StatType.ARCANE_POWER, 35, StatType.AIR_AFFINITY, 25);          // 60

        // --- Tier 2: Magic (budget 50-60) ---
        register("wom:voodoo_magic", StatType.ARCANE_POWER, 30, StatType.INTIMIDATION, 20);         // 50
        register("wom:shulker_cloak", StatType.ARCANE_POWER, 30, StatType.PHYSICAL_RESISTANCE, 20); // 50

        // --- Tier 2: Passives (budget 45-55) ---
        register("wom:heart_shield", StatType.PHYSICAL_RESISTANCE, 25, StatType.WILLPOWER, 20);     // 45
        register("wom:inner_growth", StatType.WILLPOWER, 30, StatType.ERUDITION, 20);               // 50
        register("wom:lethal_focus", StatType.PRECISION, 30, StatType.BLADE_TECHNIQUE, 20);         // 50
        register("wom:latent_retribution", StatType.WILLPOWER, 30, StatType.PHYSICAL_RESISTANCE, 20); // 50
        register("wom:vampirize", StatType.BRUTE_FORCE, 30, StatType.PHYSICAL_ENDURANCE, 20);       // 50
        register("wom:proximity_exploiter", StatType.KEEN_SENSES, 25, StatType.BRUTE_FORCE, 20);    // 45
        register("wom:all_eyes_on_me", StatType.INTIMIDATION, 25, StatType.WILLPOWER, 20);          // 45
        register("wom:all_eyes_on_you", StatType.KEEN_SENSES, 25, StatType.PRECISION, 20);          // 45
        register("wom:manipulator", StatType.ERUDITION, 30, StatType.INTIMIDATION, 20);             // 50
        register("wom:dodge_master", StatType.AGILITY, 25, StatType.RAPIDITE, 20);                  // 45

        // --- Tier 2: Defense (budget 50-55) ---
        register("wom:perfect_bulwark", StatType.PHYSICAL_RESISTANCE, 30, StatType.PHYSICAL_ENDURANCE, 20); // 50
        register("wom:regierung", StatType.WILLPOWER, 30, StatType.INTIMIDATION, 20);               // 50
        register("wom:soul_protection", StatType.WILLPOWER, 30, StatType.ARCANE_POWER, 25);         // 55

        // --- Tier 2: Shadow step (budget 55) ---
        register("wom:shadow_step", StatType.AGILITY, 30, StatType.RAPIDITE, 25);                   // 55

        // --- Tier 2: Demon mark / Sakura (budget 55-60) ---
        register("wom:demon_mark_passive", StatType.INTIMIDATION, 30, StatType.WILLPOWER, 25);      // 55
        register("wom:satsujin_passive", StatType.BLADE_TECHNIQUE, 35, StatType.INTIMIDATION, 20);  // 55
        register("wom:ruine_passive", StatType.BRUTE_FORCE, 30, StatType.INTIMIDATION, 25);         // 55

        // --- Tier 2-3: Solar (budget 60-70) ---
        register("wom:solar_passive", StatType.ARCANE_POWER, 40, StatType.FIRE_AFFINITY, 25);       // 65

        // --- Tier 3: Advanced combat (budget 70-85) ---
        register("wom:plunder_perdition", StatType.BRUTE_FORCE, 45, StatType.INTIMIDATION, 30);     // 75
        register("wom:lunatic_vivacity", StatType.RAPIDITE, 40, StatType.AGILITY, 30);              // 70
        register("wom:torment_passive", StatType.INTIMIDATION, 40, StatType.WILLPOWER, 30);         // 70
        register("wom:sakura_state", StatType.BLADE_TECHNIQUE, 45, StatType.RAPIDITE, 35);          // 80
        register("wom:ender_fusion", StatType.ARCANE_POWER, 50, StatType.AIR_AFFINITY, 35);         // 85
        register("wom:charybdis", StatType.WATER_AFFINITY, 45, StatType.ARCANE_POWER, 35);          // 80
        register("wom:orbital_beam", StatType.ARCANE_POWER, 50, StatType.PRECISION, 35);            // 85

        // --- Tier 3: Solar arcano (budget 85) ---
        register("wom:solar_arcano", StatType.ARCANE_POWER, 50, StatType.FIRE_AFFINITY, 35);        // 85

        // --- Tier 3-4: Lunar (budget 90) ---
        register("wom:lunar_eclipse", StatType.ARCANE_POWER, 55, StatType.ERUDITION, 35);           // 90

        // --- Tier 3: Time travel (budget 90) ---
        register("wom:time_travel", StatType.ARCANE_POWER, 55, StatType.ERUDITION, 35);             // 90

        // --- Tier 4: Endgame (budget 100-120) ---
        register("wom:true_berserk", StatType.BRUTE_FORCE, 65, StatType.INTIMIDATION, 40);          // 105
        register("wom:demonic_ascension", StatType.INTIMIDATION, 65, StatType.BRUTE_FORCE, 50);     // 115

        // =====================================================================
        // P1NERO BOW SKILLS (1) — namespace p1nero_bow:
        // =====================================================================
        register("p1nero_bow:mortis_innate", StatType.PRECISION, 30, StatType.AGILITY, 20);          // 50

        STATMod.LOGGER.info("Registered stat requirements for {} skills", REQUIREMENTS.size());
    }

    // --- Registration helpers ---

    private static void register(String skillId, StatType stat, int level) {
        REQUIREMENTS.computeIfAbsent(new ResourceLocation(skillId), k -> new ArrayList<>())
            .add(new StatRequirement(stat, level));
    }

    private static void register(String skillId, StatType stat1, int level1, StatType stat2, int level2) {
        List<StatRequirement> reqs = REQUIREMENTS.computeIfAbsent(new ResourceLocation(skillId), k -> new ArrayList<>());
        reqs.add(new StatRequirement(stat1, level1));
        reqs.add(new StatRequirement(stat2, level2));
    }

    // --- Query methods ---

    public static List<StatRequirement> getRequirements(ResourceLocation skillId) {
        return REQUIREMENTS.getOrDefault(skillId, Collections.emptyList());
    }

    /**
     * Returns true if the player meets ALL requirements for the given skill.
     * Skills with no requirements (base skills) always return true.
     */
    public static boolean meetsRequirements(ServerPlayer player, ResourceLocation skillId) {
        List<StatRequirement> reqs = getRequirements(skillId);
        if (reqs.isEmpty()) return true;

        return player.getCapability(PlayerStatsProvider.PLAYER_STATS).map(stats -> {
            for (StatRequirement req : reqs) {
                if (stats.getLevel(req.stat().index) < req.minLevel()) {
                    return false;
                }
            }
            return true;
        }).orElse(false);
    }

    /**
     * Returns a human-readable string of all requirements for a skill.
     * Example: "Blade Technique Lv.50, Rapidite Lv.40"
     */
    public static String getRequirementText(ResourceLocation skillId) {
        List<StatRequirement> reqs = getRequirements(skillId);
        if (reqs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reqs.size(); i++) {
            if (i > 0) sb.append(", ");
            StatRequirement req = reqs.get(i);
            sb.append(req.stat().displayName).append(" Lv.").append(req.minLevel());
        }
        return sb.toString();
    }
}

package tong.statmod.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.skill.guard.GuardSkill;

/**
 * Registers ALL custom Epic Fight skills for STAT Mod.
 *
 * Total: 42 skills across 6 categories:
 * - WEAPON_PASSIVE (12): Auto-applied when weapon equipped
 * - PASSIVE (15): Stat-based permanent effects (3 per stat × 5 stats, missing 2)
 * - WEAPON_INNATE (7): Active combat skills
 * - MOVER (3): Movement abilities
 * - GUARD (3): Defensive blocking skills
 * - IDENTITY (3): Ultimate abilities
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillRegistry {

    // ========== WEAPON PASSIVE (12) ==========
    public static Skill SWORD_MASTERY;
    public static Skill AXE_MASTERY;
    public static Skill DAGGER_MASTERY;
    public static Skill GREATSWORD_MASTERY;
    public static Skill LONGSWORD_MASTERY;
    public static Skill SPEAR_MASTERY;
    public static Skill TACHI_MASTERY;
    public static Skill UCHIGATANA_MASTERY;
    public static Skill FIST_MASTERY;
    public static Skill BOW_MASTERY;
    public static Skill PICKAXE_MASTERY;
    public static Skill HOE_MASTERY;

    // ========== PASSIVE (15) ==========
    // Brute Force
    public static Skill BRUTE_POWER;
    public static Skill BRUTE_RAGE;
    public static Skill BRUTE_FURY;
    // Blade Technique
    public static Skill BLADE_FINESSE;
    public static Skill BLADE_MASTERY;
    public static Skill BLADE_PERFECTION;
    // Rapidité
    public static Skill RAPID_SURGE;
    public static Skill RAPID_BLITZ;
    public static Skill RAPID_LIGHTNING;
    // Agility
    public static Skill AGILITY_FOOTWORK;
    public static Skill AGILITY_EVASION;
    public static Skill AGILITY_PHANTOM;
    // Physical Resistance
    public static Skill RESIST_IRON;
    public static Skill RESIST_STEEL;
    public static Skill RESIST_DIAMOND;
    // Physical Endurance
    public static Skill ENDURANCE_VITALITY;
    public static Skill ENDURANCE_TOUGHNESS;
    public static Skill ENDURANCE_UNBREAKABLE;
    // Precision
    public static Skill PRECISION_FOCUS;
    public static Skill PRECISION_ACCURACY;
    public static Skill PRECISION_DEADEYE;

    // ========== WEAPON INNATE (7) ==========
    public static Skill HEAVY_STRIKE;
    public static Skill BLADE_DANCE;
    public static Skill BLITZ_ASSAULT;
    public static Skill SHADOW_STEP;
    public static Skill STONE_SKIN;
    public static Skill ENDURANCE_SURGE;
    public static Skill PRECISION_SHOT;

    // ========== MOVER (3) ==========
    public static Skill QUICK_STEP;
    public static Skill SHADOW_LEAP;
    public static Skill WIND_DASH;

    // ========== GUARD (3) ==========
    public static Skill PARRY;
    public static Skill FORTRESS;
    public static Skill IRON_WALL;

    // ========== IDENTITY (3) ==========
    public static Skill BERSERKER_RAGE;
    public static Skill BLADE_GOD;
    public static Skill SHADOW_DANCER;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onSkillBuild(SkillBuildEvent event) {
        SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker(STATMod.MODID);

        // ===== WEAPON PASSIVE (12) =====
        SWORD_MASTERY = worker.build("sword_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.SWORD),
            makePassiveBuilder("sword_mastery"));
        AXE_MASTERY = worker.build("axe_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.AXE),
            makePassiveBuilder("axe_mastery"));
        DAGGER_MASTERY = worker.build("dagger_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.DAGGER),
            makePassiveBuilder("dagger_mastery"));
        GREATSWORD_MASTERY = worker.build("greatsword_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.GREATSWORD),
            makePassiveBuilder("greatsword_mastery"));
        LONGSWORD_MASTERY = worker.build("longsword_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.LONGSWORD),
            makePassiveBuilder("longsword_mastery"));
        SPEAR_MASTERY = worker.build("spear_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.SPEAR),
            makePassiveBuilder("spear_mastery"));
        TACHI_MASTERY = worker.build("tachi_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.TACHI),
            makePassiveBuilder("tachi_mastery"));
        UCHIGATANA_MASTERY = worker.build("uchigatana_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.UCHIGATANA),
            makePassiveBuilder("uchigatana_mastery"));
        FIST_MASTERY = worker.build("fist_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.FIST),
            makePassiveBuilder("fist_mastery"));
        BOW_MASTERY = worker.build("bow_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.BOW),
            makePassiveBuilder("bow_mastery"));
        PICKAXE_MASTERY = worker.build("pickaxe_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.PICKAXE),
            makePassiveBuilder("pickaxe_mastery"));
        HOE_MASTERY = worker.build("hoe_mastery",
            builder -> new WeaponPassiveSkill(builder, WeaponPassiveSkill.WeaponStyle.HOE),
            makePassiveBuilder("hoe_mastery"));

        // ===== PASSIVE (15) =====
        // Brute Force (tiers 1/2/3)
        BRUTE_POWER = worker.build("brute_power",
            builder -> new StatPassiveSkill(builder, StatType.BRUTE_FORCE, 1),
            makePassiveBuilder("brute_power"));
        BRUTE_RAGE = worker.build("brute_rage",
            builder -> new StatPassiveSkill(builder, StatType.BRUTE_FORCE, 2),
            makePassiveBuilder("brute_rage"));
        BRUTE_FURY = worker.build("brute_fury",
            builder -> new StatPassiveSkill(builder, StatType.BRUTE_FORCE, 3),
            makePassiveBuilder("brute_fury"));

        // Blade Technique
        BLADE_FINESSE = worker.build("blade_finesse",
            builder -> new StatPassiveSkill(builder, StatType.BLADE_TECHNIQUE, 1),
            makePassiveBuilder("blade_finesse"));
        BLADE_MASTERY = worker.build("blade_mastery",
            builder -> new StatPassiveSkill(builder, StatType.BLADE_TECHNIQUE, 2),
            makePassiveBuilder("blade_mastery"));
        BLADE_PERFECTION = worker.build("blade_perfection",
            builder -> new StatPassiveSkill(builder, StatType.BLADE_TECHNIQUE, 3),
            makePassiveBuilder("blade_perfection"));

        // Rapidité
        RAPID_SURGE = worker.build("rapid_surge",
            builder -> new StatPassiveSkill(builder, StatType.RAPIDITE, 1),
            makePassiveBuilder("rapid_surge"));
        RAPID_BLITZ = worker.build("rapid_blitz",
            builder -> new StatPassiveSkill(builder, StatType.RAPIDITE, 2),
            makePassiveBuilder("rapid_blitz"));
        RAPID_LIGHTNING = worker.build("rapid_lightning",
            builder -> new StatPassiveSkill(builder, StatType.RAPIDITE, 3),
            makePassiveBuilder("rapid_lightning"));

        // Agility
        AGILITY_FOOTWORK = worker.build("agility_footwork",
            builder -> new StatPassiveSkill(builder, StatType.AGILITY, 1),
            makePassiveBuilder("agility_footwork"));
        AGILITY_EVASION = worker.build("agility_evasion",
            builder -> new StatPassiveSkill(builder, StatType.AGILITY, 2),
            makePassiveBuilder("agility_evasion"));
        AGILITY_PHANTOM = worker.build("agility_phantom",
            builder -> new StatPassiveSkill(builder, StatType.AGILITY, 3),
            makePassiveBuilder("agility_phantom"));

        // Physical Resistance
        RESIST_IRON = worker.build("resist_iron",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_RESISTANCE, 1),
            makePassiveBuilder("resist_iron"));
        RESIST_STEEL = worker.build("resist_steel",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_RESISTANCE, 2),
            makePassiveBuilder("resist_steel"));
        RESIST_DIAMOND = worker.build("resist_diamond",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_RESISTANCE, 3),
            makePassiveBuilder("resist_diamond"));

        // Physical Endurance
        ENDURANCE_VITALITY = worker.build("endurance_vitality",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_ENDURANCE, 1),
            makePassiveBuilder("endurance_vitality"));
        ENDURANCE_TOUGHNESS = worker.build("endurance_toughness",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_ENDURANCE, 2),
            makePassiveBuilder("endurance_toughness"));
        ENDURANCE_UNBREAKABLE = worker.build("endurance_unbreakable",
            builder -> new StatPassiveSkill(builder, StatType.PHYSICAL_ENDURANCE, 3),
            makePassiveBuilder("endurance_unbreakable"));

        // Precision
        PRECISION_FOCUS = worker.build("precision_focus",
            builder -> new StatPassiveSkill(builder, StatType.PRECISION, 1),
            makePassiveBuilder("precision_focus"));
        PRECISION_ACCURACY = worker.build("precision_accuracy",
            builder -> new StatPassiveSkill(builder, StatType.PRECISION, 2),
            makePassiveBuilder("precision_accuracy"));
        PRECISION_DEADEYE = worker.build("precision_deadeye",
            builder -> new StatPassiveSkill(builder, StatType.PRECISION, 3),
            makePassiveBuilder("precision_deadeye"));

        // ===== WEAPON INNATE (7) — Custom CLASS_ARTS slot =====
        HEAVY_STRIKE = worker.build("heavy_strike",
            builder -> new StatActiveSkill(builder, StatType.BRUTE_FORCE, 8000L),
            makeBuilder("heavy_strike", StatModSkillCategories.CLASS_ARTS));
        BLADE_DANCE = worker.build("blade_dance",
            builder -> new StatActiveSkill(builder, StatType.BLADE_TECHNIQUE, 10000L),
            makeBuilder("blade_dance", StatModSkillCategories.CLASS_ARTS));
        BLITZ_ASSAULT = worker.build("blitz_assault",
            builder -> new StatActiveSkill(builder, StatType.RAPIDITE, 12000L),
            makeBuilder("blitz_assault", StatModSkillCategories.CLASS_ARTS));
        SHADOW_STEP = worker.build("shadow_step",
            builder -> new StatActiveSkill(builder, StatType.AGILITY, 15000L),
            makeBuilder("shadow_step", StatModSkillCategories.CLASS_ARTS));
        STONE_SKIN = worker.build("stone_skin",
            builder -> new StatActiveSkill(builder, StatType.PHYSICAL_RESISTANCE, 20000L),
            makeBuilder("stone_skin", StatModSkillCategories.CLASS_ARTS));
        ENDURANCE_SURGE = worker.build("endurance_surge",
            builder -> new StatActiveSkill(builder, StatType.PHYSICAL_ENDURANCE, 25000L),
            makeBuilder("endurance_surge", StatModSkillCategories.CLASS_ARTS));
        PRECISION_SHOT = worker.build("precision_shot",
            builder -> new StatActiveSkill(builder, StatType.PRECISION, 18000L),
            makeBuilder("precision_shot", StatModSkillCategories.CLASS_ARTS));

        // ===== MOVER (3) =====
        QUICK_STEP = worker.build("quick_step",
            builder -> new MoverSkill(builder, MoverSkill.MoverType.QUICK_STEP),
            Skill.createMoverBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "quick_step"))
                .setCategory(SkillCategories.MOVER));
        SHADOW_LEAP = worker.build("shadow_leap",
            builder -> new MoverSkill(builder, MoverSkill.MoverType.SHADOW_LEAP),
            Skill.createMoverBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "shadow_leap"))
                .setCategory(SkillCategories.MOVER));
        WIND_DASH = worker.build("wind_dash",
            builder -> new MoverSkill(builder, MoverSkill.MoverType.WIND_DASH),
            Skill.createMoverBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "wind_dash"))
                .setCategory(SkillCategories.MOVER));

        // ===== GUARD (3) =====
        PARRY = worker.build("parry",
            builder -> new StatGuardSkill((GuardSkill.Builder) builder, StatGuardSkill.GuardType.PARRY),
            makeGuardBuilder("parry"));
        FORTRESS = worker.build("guard_fortress",
            builder -> new StatGuardSkill((GuardSkill.Builder) builder, StatGuardSkill.GuardType.FORTRESS),
            makeGuardBuilder("guard_fortress"));
        IRON_WALL = worker.build("guard_iron_wall",
            builder -> new StatGuardSkill((GuardSkill.Builder) builder, StatGuardSkill.GuardType.IRON_WALL),
            makeGuardBuilder("guard_iron_wall"));

        // ===== IDENTITY (3) =====
        BERSERKER_RAGE = worker.build("berserker_rage",
            builder -> new IdentitySkill(builder, IdentitySkill.IdentityType.BERSERKER_RAGE, 60000L),
            makeBuilder("berserker_rage", SkillCategories.IDENTITY));
        BLADE_GOD = worker.build("blade_god",
            builder -> new IdentitySkill(builder, IdentitySkill.IdentityType.BLADE_GOD, 90000L),
            makeBuilder("blade_god", SkillCategories.IDENTITY));
        SHADOW_DANCER = worker.build("shadow_dancer",
            builder -> new IdentitySkill(builder, IdentitySkill.IdentityType.SHADOW_DANCER, 120000L),
            makeBuilder("shadow_dancer", SkillCategories.IDENTITY));

        // ===== NON-COMBAT SKILLS (17) =====
        // Magic (9)
        worker.build("arcane_bolt",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.ARCANE_BOLT, 3000L),
            makeBuilder("arcane_bolt", SkillCategories.IDENTITY));
        worker.build("water_heal",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.WATER_HEAL, 5000L),
            makeBuilder("water_heal", SkillCategories.IDENTITY));
        worker.build("earth_shield",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.EARTH_SHIELD, 5000L),
            makeBuilder("earth_shield", SkillCategories.IDENTITY));
        worker.build("fireball",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.FIREBALL, 4000L),
            makeBuilder("fireball", SkillCategories.IDENTITY));
        worker.build("air_dash",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.AIR_DASH, 3000L),
            makeBuilder("air_dash", SkillCategories.IDENTITY));
        worker.build("magic_resist",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.MAGIC_RESIST, 8000L),
            makeBuilder("magic_resist", SkillCategories.IDENTITY));
        worker.build("fast_cast",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.FAST_CAST, 5000L),
            makeBuilder("fast_cast", SkillCategories.IDENTITY));
        worker.build("mana_regen",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.MANA_REGEN, 5000L),
            makeBuilder("mana_regen", SkillCategories.IDENTITY));
        worker.build("study",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.STUDY, 5000L),
            makeBuilder("study", SkillCategories.IDENTITY));

        // Survival (2)
        worker.build("track",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.TRACK, 5000L),
            makeBuilder("track", SkillCategories.IDENTITY));
        worker.build("sense_danger",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.SENSE_DANGER, 3000L),
            makeBuilder("sense_danger", SkillCategories.IDENTITY));

        // Crafting (3)
        worker.build("master_forge",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.MASTER_FORGE, 5000L),
            makeBuilder("master_forge", SkillCategories.IDENTITY));
        worker.build("feast",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.FEAST, 5000L),
            makeBuilder("feast", SkillCategories.IDENTITY));
        worker.build("potion_boost",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.POTION_BOOST, 5000L),
            makeBuilder("potion_boost", SkillCategories.IDENTITY));

        // Mental (3)
        worker.build("meditate",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.MEDITATE, 10000L),
            makeBuilder("meditate", SkillCategories.IDENTITY));
        worker.build("intimidate",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.INTIMIDATE, 5000L),
            makeBuilder("intimidate", SkillCategories.IDENTITY));
        worker.build("willpower_aura",
            builder -> new NonCombatSkill(builder, NonCombatSkill.SkillCategory.WILLPOWER_AURA, 8000L),
            makeBuilder("willpower_aura", SkillCategories.IDENTITY));

        // Populate the unlock registry
        SkillUnlockRegistry.refresh();

        STATMod.LOGGER.info("Registered 60 Epic Fight skills for STAT Mod");
    }

    // ===== Builder helpers =====

    // ===== Builder helpers =====

    @SuppressWarnings("unchecked")
    private static SkillBuilder<Skill> makeBuilder(String name, yesman.epicfight.skill.SkillCategory category) {
        SkillBuilder<Skill> builder = (SkillBuilder<Skill>) Skill.createBuilder();
        builder.setRegistryName(new ResourceLocation(STATMod.MODID, name));
        builder.setCategory(category);
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static SkillBuilder<PassiveSkill> makePassiveBuilder(String name) {
        SkillBuilder<PassiveSkill> builder = PassiveSkill.createPassiveBuilder();
        builder.setRegistryName(new ResourceLocation(STATMod.MODID, name));
        builder.setCategory(SkillCategories.PASSIVE);
        return builder;
    }

    private static GuardSkill.Builder makeGuardBuilder(String name) {
        GuardSkill.Builder builder = GuardSkill.createGuardBuilder();
        builder.setRegistryName(new ResourceLocation(STATMod.MODID, name));
        builder.setCategory(SkillCategories.GUARD);
        return builder;
    }
}

package tong.statmod.perks;

import tong.statmod.stats.StatType;

public enum Perk {
    BRUTE_CORE(0, StatType.BRUTE_FORCE, PerkTier.CORE, "Heavy Hitter", "+5% damage with axes and clubs"),
    BRUTE_ACTIVE(1, StatType.BRUTE_FORCE, PerkTier.ACTIVE, "Mighty Swing", "Charged attack deals +10% damage"),
    BRUTE_SYNERGY(2, StatType.BRUTE_FORCE, PerkTier.SYNERGY, "Crushing Force", "Synergy: +15% damage when below 50% HP", StatType.PHYSICAL_ENDURANCE),
    BRUTE_SITUATIONAL(3, StatType.BRUTE_FORCE, PerkTier.SITUATIONAL, "Berserker", "+20% damage when below 30% HP"),
    BRUTE_MASTERY(4, StatType.BRUTE_FORCE, PerkTier.MASTERY, "Colossus", "Attacks stun targets below 50% HP"),
    BRUTE_TRANSCENDENCE(5, StatType.BRUTE_FORCE, PerkTier.TRANSCENDENCE, "Titan's Wrath", "+50% damage, enemies explode on kill"),

    BLADE_CORE(6, StatType.BLADE_TECHNIQUE, PerkTier.CORE, "Sharp Edge", "+5% damage with swords"),
    BLADE_ACTIVE(7, StatType.BLADE_TECHNIQUE, PerkTier.ACTIVE, "Flowing Strike", "Every 3rd hit deals double damage"),
    BLADE_SYNERGY(8, StatType.BLADE_TECHNIQUE, PerkTier.SYNERGY, "Dance of Blades", "Synergy: combo hits grant speed", StatType.AGILITY),
    BLADE_SITUATIONAL(9, StatType.BLADE_TECHNIQUE, PerkTier.SITUATIONAL, "Parry Master", "Blocking reflects 50% damage"),
    BLADE_MASTERY(10, StatType.BLADE_TECHNIQUE, PerkTier.MASTERY, "Blade Storm", "AOE spin attack on kill"),
    BLADE_TRANSCENDENCE(11, StatType.BLADE_TECHNIQUE, PerkTier.TRANSCENDENCE, "One With the Blade", "Every hit is critical for 5s after kill"),

    RAPID_CORE(12, StatType.RAPIDITE, PerkTier.CORE, "Quick Hands", "+5% attack speed"),
    RAPID_ACTIVE(13, StatType.RAPIDITE, PerkTier.ACTIVE, "Double Strike", "10% chance to hit twice"),
    RAPID_SYNERGY(14, StatType.RAPIDITE, PerkTier.SYNERGY, "Blinding Speed", "Synergy: kills grant burst of speed", StatType.AGILITY),
    RAPID_SITUATIONAL(15, StatType.RAPIDITE, PerkTier.SITUATIONAL, "Flurry", "Attacks get faster as combo builds"),
    RAPID_MASTERY(16, StatType.RAPIDITE, PerkTier.MASTERY, "Time Skip", "Slow-motion effect on dodge"),
    RAPID_TRANSCENDENCE(17, StatType.RAPIDITE, PerkTier.TRANSCENDENCE, "Za Warudo", "Stop time for 2s on perfect dodge"),

    AGIL_CORE(18, StatType.AGILITY, PerkTier.CORE, "Light Feet", "+5% movement speed"),
    AGIL_ACTIVE(19, StatType.AGILITY, PerkTier.ACTIVE, "Agile Strikes", "Moving attacks deal +10% damage"),
    AGIL_SYNERGY(20, StatType.AGILITY, PerkTier.SYNERGY, "Wind Walker", "Synergy: dodge grants damage buff", StatType.RAPIDITE),
    AGIL_SITUATIONAL(21, StatType.AGILITY, PerkTier.SITUATIONAL, "Evasion", "20% dodge chance when hit"),
    AGIL_MASTERY(22, StatType.AGILITY, PerkTier.MASTERY, "Spectral Step", "Short teleport on dodge"),
    AGIL_TRANSCENDENCE(23, StatType.AGILITY, PerkTier.TRANSCENDENCE, "Untouchable", "100% dodge for 3s after taking damage"),

    RESIST_CORE(24, StatType.PHYSICAL_RESISTANCE, PerkTier.CORE, "Tough Skin", "-5% incoming damage"),
    RESIST_ACTIVE(25, StatType.PHYSICAL_RESISTANCE, PerkTier.ACTIVE, "Iron Guard", "Blocking reduces damage by 25%"),
    RESIST_SYNERGY(26, StatType.PHYSICAL_RESISTANCE, PerkTier.SYNERGY, "Fortress", "Synergy: armor bonus when still", StatType.PHYSICAL_ENDURANCE),
    RESIST_SITUATIONAL(27, StatType.PHYSICAL_RESISTANCE, PerkTier.SITUATIONAL, "Last Stand", "+30% resistance below 20% HP"),
    RESIST_MASTERY(28, StatType.PHYSICAL_RESISTANCE, PerkTier.MASTERY, "Diamond Skin", "Grants brief invulnerability after hit"),
    RESIST_TRANSCENDENCE(29, StatType.PHYSICAL_RESISTANCE, PerkTier.TRANSCENDENCE, "Immortal", "Survive fatal hit once per 30s"),

    ENDUR_CORE(30, StatType.PHYSICAL_ENDURANCE, PerkTier.CORE, "Sturdy", "+2 absorption hearts"),
    ENDUR_ACTIVE(31, StatType.PHYSICAL_ENDURANCE, PerkTier.ACTIVE, "Second Wind", "Regain 4 absorption on kill"),
    ENDUR_SYNERGY(32, StatType.PHYSICAL_ENDURANCE, PerkTier.SYNERGY, "Unstoppable", "Synergy: no knockback when shielded", StatType.PHYSICAL_RESISTANCE),
    ENDUR_SITUATIONAL(33, StatType.PHYSICAL_ENDURANCE, PerkTier.SITUATIONAL, "Adrenaline", "Kills restore hunger/saturation"),
    ENDUR_MASTERY(34, StatType.PHYSICAL_ENDURANCE, PerkTier.MASTERY, "Overflowing Vitality", "Heal 1 HP per 5s in combat"),
    ENDUR_TRANSCENDENCE(35, StatType.PHYSICAL_ENDURANCE, PerkTier.TRANSCENDENCE, "Limit Break", "Double max absorption"),

    PRECI_CORE(36, StatType.PRECISION, PerkTier.CORE, "Steady Aim", "+5% ranged damage"),
    PRECI_ACTIVE(37, StatType.PRECISION, PerkTier.ACTIVE, "Piercing Shot", "Arrows pierce 1 extra target"),
    PRECI_SYNERGY(38, StatType.PRECISION, PerkTier.SYNERGY, "Hunter's Mark", "Synergy: marked arrows deal +20%", StatType.TRACKING),
    PRECI_SITUATIONAL(39, StatType.PRECISION, PerkTier.SITUATIONAL, "Critical Eye", "+15% crit chance at max health"),
    PRECI_MASTERY(40, StatType.PRECISION, PerkTier.MASTERY, "Deadshot", "Headshot multiplier +50%"),
    PRECI_TRANSCENDENCE(41, StatType.PRECISION, PerkTier.TRANSCENDENCE, "True Strike", "Always hit, ignore armor"),

    TRACK_CORE(42, StatType.TRACKING, PerkTier.CORE, "Tracker's Eye", "Glow nearby mobs for 2s"),
    TRACK_ACTIVE(43, StatType.TRACKING, PerkTier.ACTIVE, "Pursuit", "Gain speed toward marked targets"),
    TRACK_SYNERGY(44, StatType.TRACKING, PerkTier.SYNERGY, "Pack Hunter", "Synergy: extra damage near allies", StatType.INTIMIDATION),
    TRACK_SITUATIONAL(45, StatType.TRACKING, PerkTier.SITUATIONAL, "Sillage", "Hitting mobs leaves a scent trail"),
    TRACK_MASTERY(46, StatType.TRACKING, PerkTier.MASTERY, "Predator", "See all mobs through walls"),
    TRACK_TRANSCENDENCE(47, StatType.TRACKING, PerkTier.TRANSCENDENCE, "Omnisight", "Reveal all entities in 50-block radius"),

    SENSE_CORE(48, StatType.KEEN_SENSES, PerkTier.CORE, "Sixth Sense", "Dodge 5% of incoming hits"),
    SENSE_ACTIVE(49, StatType.KEEN_SENSES, PerkTier.ACTIVE, "Treasure Hunter", "Doubles XP from mobs"),
    SENSE_SYNERGY(50, StatType.KEEN_SENSES, PerkTier.SYNERGY, "Predator's Instinct", "Synergy: mark hidden mobs", StatType.TRACKING),
    SENSE_SITUATIONAL(51, StatType.KEEN_SENSES, PerkTier.SITUATIONAL, "Intuition", "Mine ores faster near enemies"),
    SENSE_MASTERY(52, StatType.KEEN_SENSES, PerkTier.MASTERY, "Foresight", "Precognition: dodge next hit"),
    SENSE_TRANSCENDENCE(53, StatType.KEEN_SENSES, PerkTier.TRANSCENDENCE, "Omniscience", "See player health at all times"),

    FORGE_CORE(54, StatType.FORGING, PerkTier.CORE, "Hammer Hand", "Repairs cost 1 fewer level"),
    FORGE_ACTIVE(55, StatType.FORGING, PerkTier.ACTIVE, "Master Smith", "Chance to double repair output"),
    FORGE_SYNERGY(56, StatType.FORGING, PerkTier.SYNERGY, "Enchanting Touch", "Synergy: anvil enchants cost less", StatType.ALCHEMY),
    FORGE_SITUATIONAL(57, StatType.FORGING, PerkTier.SITUATIONAL, "Sharpening", "Weapons deal more after anvil use"),
    FORGE_MASTERY(58, StatType.FORGING, PerkTier.MASTERY, "Artificer", "Create unique anvil-only upgrades"),
    FORGE_TRANSCENDENCE(59, StatType.FORGING, PerkTier.TRANSCENDENCE, "Creation", "Repair items to perfect condition"),

    COOK_CORE(60, StatType.COOKING, PerkTier.CORE, "Home Cook", "+1 saturation from all food"),
    COOK_ACTIVE(61, StatType.COOKING, PerkTier.ACTIVE, "Iron Stomach", "Bad food effects halved"),
    COOK_SYNERGY(62, StatType.COOKING, PerkTier.SYNERGY, "Nutritionist", "Synergy: food gives absorption", StatType.PHYSICAL_ENDURANCE),
    COOK_SITUATIONAL(63, StatType.COOKING, PerkTier.SITUATIONAL, "Fast Food", "Eat 2x faster"),
    COOK_MASTERY(64, StatType.COOKING, PerkTier.MASTERY, "Feast", "Share food effects with nearby allies"),
    COOK_TRANSCENDENCE(65, StatType.COOKING, PerkTier.TRANSCENDENCE, "Ambrosia", "Food gives regeneration for 30s"),

    ALCHEM_CORE(66, StatType.ALCHEMY, PerkTier.CORE, "Mixologist", "+10% potion duration"),
    ALCHEM_ACTIVE(67, StatType.ALCHEMY, PerkTier.ACTIVE, "Brewer's Secret", "Chance to duplicate potions"),
    ALCHEM_SYNERGY(68, StatType.ALCHEMY, PerkTier.SYNERGY, "Transmutation", "Synergy: convert materials on craft", StatType.FORGING),
    ALCHEM_SITUATIONAL(69, StatType.ALCHEMY, PerkTier.SITUATIONAL, "Toxicologist", "Poison effects last 50% longer"),
    ALCHEM_MASTERY(70, StatType.ALCHEMY, PerkTier.MASTERY, "Philosopher's Stone", "Potion effects are 50% stronger"),
    ALCHEM_TRANSCENDENCE(71, StatType.ALCHEMY, PerkTier.TRANSCENDENCE, "Eternal Elixir", "Potions never expire in inventory"),

    INTIM_CORE(72, StatType.INTIMIDATION, PerkTier.CORE, "Menace", "+5% damage to same target"),
    INTIM_ACTIVE(73, StatType.INTIMIDATION, PerkTier.ACTIVE, "Intimidating Aura", "Nearby mobs deal -10% damage"),
    INTIM_SYNERGY(74, StatType.INTIMIDATION, PerkTier.SYNERGY, "Feared", "Synergy: mobs flee at low HP", StatType.WILLPOWER),
    INTIM_SITUATIONAL(75, StatType.INTIMIDATION, PerkTier.SITUATIONAL, "Mark of Fear", "Marked mobs take +25% damage"),
    INTIM_MASTERY(76, StatType.INTIMIDATION, PerkTier.MASTERY, "Dread Lord", "Kills cause nearby mobs to flee"),
    INTIM_TRANSCENDENCE(77, StatType.INTIMIDATION, PerkTier.TRANSCENDENCE, "Absolute Dominion", "Control one mob for 10s"),

    WILL_CORE(78, StatType.WILLPOWER, PerkTier.CORE, "Iron Will", "Status effects last 10% less"),
    WILL_ACTIVE(79, StatType.WILLPOWER, PerkTier.ACTIVE, "Focused Mind", "Resist knockback when blocking"),
    WILL_SYNERGY(80, StatType.WILLPOWER, PerkTier.SYNERGY, "Unbreakable", "Synergy: damage resistance with shield", StatType.PHYSICAL_RESISTANCE),
    WILL_SITUATIONAL(81, StatType.WILLPOWER, PerkTier.SITUATIONAL, "Last Breath", "Survive at 1 HP once per 30s"),
    WILL_MASTERY(82, StatType.WILLPOWER, PerkTier.MASTERY, "Indomitable", "Debuffs become buffs at low HP"),
    WILL_TRANSCENDENCE(83, StatType.WILLPOWER, PerkTier.TRANSCENDENCE, "Transcendence", "Immune to all status effects");

    private static final Perk[] BY_ID = new Perk[values().length];
    static { for (Perk p : values()) BY_ID[p.id] = p; }

    public final int id;
    public final StatType stat;
    public final PerkTier tier;
    public final String name;
    public final String description;
    public final StatType synergyStat;

    Perk(int id, StatType stat, PerkTier tier, String name, String desc) {
        this(id, stat, tier, name, desc, null);
    }

    Perk(int id, StatType stat, PerkTier tier, String name, String desc, StatType synergyStat) {
        this.id = id;
        this.stat = stat;
        this.tier = tier;
        this.name = name;
        this.description = desc;
        this.synergyStat = synergyStat;
    }

    public static Perk byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    public static Perk byStatAndTier(StatType stat, PerkTier tier) {
        for (Perk p : values()) {
            if (p.stat == stat && p.tier == tier) return p;
        }
        return null;
    }
}

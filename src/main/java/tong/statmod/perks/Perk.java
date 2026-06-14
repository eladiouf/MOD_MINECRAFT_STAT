package tong.statmod.perks;

import tong.statmod.stats.StatType;

public enum Perk {
    // === BRUTE_FORCE (ids 0-5) ===
    BRUTE_CORE(0, StatType.BRUTE_FORCE, PerkTier.CORE, "Heavy Swing", "+5% d\u00e9g\u00e2ts haches et masses", null),
    BRUTE_ACTIVE(1, StatType.BRUTE_FORCE, PerkTier.ACTIVE, "Brise-Bouclier", "15% d\u00e9sactive bouclier ennemi 2s", null),
    BRUTE_SYNERGY(2, StatType.BRUTE_FORCE, PerkTier.SYNERGY, "Frappe \u00c9crasante", "Attaques charg\u00e9es ignorent 10% armure", StatType.PHYSICAL_ENDURANCE),
    BRUTE_SITUATIONAL(3, StatType.BRUTE_FORCE, PerkTier.SITUATIONAL, "Adr\u00e9naline", "Tuer un mob restaure 2 endurance", null),
    BRUTE_MASTERY(4, StatType.BRUTE_FORCE, PerkTier.MASTERY, "Carnage", "Tuer un mob = coup en arc 180\u00b0 touche tous les ennemis", null),
    BRUTE_TRANSCENDENCE(5, StatType.BRUTE_FORCE, PerkTier.TRANSCENDENCE, "Colosse", "<50% HP + kill crit \u2192 +50% d\u00e9g\u00e2ts/knockback 4s (60s cd)", null),

    // === BLADE_TECHNIQUE (ids 6-11) ===
    BLADE_CORE(6, StatType.BLADE_TECHNIQUE, PerkTier.CORE, "Riposte", "Parade parfaite \u2192 prochain coup +30% (3s)", null),
    BLADE_ACTIVE(7, StatType.BLADE_TECHNIQUE, PerkTier.ACTIVE, "Escrimeur", "+5% vitesse d'attaque \u00e9p\u00e9es", null),
    BLADE_SYNERGY(8, StatType.BLADE_TECHNIQUE, PerkTier.SYNERGY, "Blessures", "10% chance h\u00e9morragie 4s", StatType.RAPIDITE),
    BLADE_SITUATIONAL(9, StatType.BLADE_TECHNIQUE, PerkTier.SITUATIONAL, "Danse Lames", "3 kills / 10s \u2192 +15% d\u00e9g\u00e2ts cumulable", null),
    BLADE_MASTERY(10, StatType.BLADE_TECHNIQUE, PerkTier.MASTERY, "Ma\u00eetre d'Armes", "Fen\u00eatre parade +30%, co\u00fbt parade -20%", null),
    BLADE_TRANSCENDENCE(11, StatType.BLADE_TECHNIQUE, PerkTier.TRANSCENDENCE, "Temp\u00eate d'Acier", "3 kills streak \u2192 coups en arc 180\u00b0 5s (45s cd)", null),

    // === RAPIDITÉ (ids 12-17) ===
    RAPID_CORE(12, StatType.RAPIDITE, PerkTier.CORE, "R\u00e9flexes Vifs", "+5% vitesse armes l\u00e9g\u00e8res (dagues/lances)", null),
    RAPID_ACTIVE(13, StatType.RAPIDITE, PerkTier.ACTIVE, "Frappe Per\u00e7ante", "15% chance dague ignore 15% armure", null),
    RAPID_SYNERGY(14, StatType.RAPIDITE, PerkTier.SYNERGY, "Momentum", "Coups cons\u00e9cutifs +3% d\u00e9g\u00e2ts (max +30%, reset si manqu\u00e9)", StatType.AGILITY),
    RAPID_SITUATIONAL(15, StatType.RAPIDITE, PerkTier.SITUATIONAL, "Roulade", "Roulade co\u00fbte 15% moins d'endurance", null),
    RAPID_MASTERY(16, StatType.RAPIDITE, PerkTier.MASTERY, "Vitesse Aveuglante", "Apr\u00e8s roulade, 3 coups : +10% vitesse et d\u00e9g\u00e2ts", null),
    RAPID_TRANSCENDENCE(17, StatType.RAPIDITE, PerkTier.TRANSCENDENCE, "Fr\u00e9n\u00e9sie", "<20% endurance + touche \u2192 vitesse max 5s (45s cd)", null),

    // === AGILITY (ids 18-23) ===
    AGILITY_CORE(18, StatType.AGILITY, PerkTier.CORE, "Pied L\u00e9ger", "+5% vitesse d\u00e9placement", null),
    AGILITY_ACTIVE(19, StatType.AGILITY, PerkTier.ACTIVE, "Acrobate", "Saut +15%, chute -15%", null),
    AGILITY_SYNERGY(20, StatType.AGILITY, PerkTier.SYNERGY, "Pas de Loup", "Aucun malus de terrain (sable des \u00e2mes, etc.)", StatType.RAPIDITE),
    AGILITY_SITUATIONAL(21, StatType.AGILITY, PerkTier.SITUATIONAL, "Expert \u00c9vasion", "Esquiver supprime tout effet de lenteur", null),
    AGILITY_MASTERY(22, StatType.AGILITY, PerkTier.MASTERY, "Danse des Lames", "Sprint 10 blocs \u2192 prochain coup +25%", null),
    AGILITY_TRANSCENDENCE(23, StatType.AGILITY, PerkTier.TRANSCENDENCE, "Z\u00e9phyr", "Esquiver avec 2+ ennemis \u2192 +50% vitesse 4s, traverse les mobs (45s cd)", null),

    // === PHYSICAL_RESISTANCE (ids 24-29) ===
    RESIST_CORE(24, StatType.PHYSICAL_RESISTANCE, PerkTier.CORE, "Peau Dure", "-1 d\u00e9g\u00e2t subi (min 0,5)", null),
    RESIST_ACTIVE(25, StatType.PHYSICAL_RESISTANCE, PerkTier.ACTIVE, "Estomac de Fer", "Nourriture +10% saturation", null),
    RESIST_SYNERGY(26, StatType.PHYSICAL_RESISTANCE, PerkTier.SYNERGY, "Mur de Bouclier", "Bloquer r\u00e9duit 5% d\u00e9g\u00e2ts suppl\u00e9mentaires", StatType.PHYSICAL_ENDURANCE),
    RESIST_SITUATIONAL(27, StatType.PHYSICAL_RESISTANCE, PerkTier.SITUATIONAL, "In\u00e9branlable", "<30% HP : +15% armure et knockback resist", null),
    RESIST_MASTERY(28, StatType.PHYSICAL_RESISTANCE, PerkTier.MASTERY, "V\u00e9t\u00e9ran", "D\u00e9g\u00e2ts sur dur\u00e9e r\u00e9duits 25%", null),
    RESIST_TRANSCENDENCE(29, StatType.PHYSICAL_RESISTANCE, PerkTier.TRANSCENDENCE, "Implacable", "<30% HP + touch\u00e9 \u2192 75% r\u00e9duc d\u00e9g\u00e2ts 3s (90s cd)", null),

    // === PHYSICAL_ENDURANCE (ids 30-35) ===
    ENDUR_CORE(30, StatType.PHYSICAL_ENDURANCE, PerkTier.CORE, "Infatigable", "R\u00e9g\u00e9n\u00e9ration endurance +10%", null),
    ENDUR_ACTIVE(31, StatType.PHYSICAL_ENDURANCE, PerkTier.ACTIVE, "Second Souffle", "<15% endurance \u2192 r\u00e9gen +50% 3s", null),
    ENDUR_SYNERGY(32, StatType.PHYSICAL_ENDURANCE, PerkTier.SYNERGY, "Marathonien", "Co\u00fbt sprint -15%", StatType.PHYSICAL_RESISTANCE),
    ENDUR_SITUATIONAL(33, StatType.PHYSICAL_ENDURANCE, PerkTier.SITUATIONAL, "Adr\u00e9naline", "Prendre d\u00e9g\u00e2ts \u2192 +5% endurance (10s cd)", null),
    ENDUR_MASTERY(34, StatType.PHYSICAL_ENDURANCE, PerkTier.MASTERY, "Condition de Pointe", "Aucun malus d'armure lourde sur endurance", null),
    ENDUR_TRANSCENDENCE(35, StatType.PHYSICAL_ENDURANCE, PerkTier.TRANSCENDENCE, "R\u00e9servoir Infini", "Endurance \u00e0 0 en combat \u2192 5s sans co\u00fbt (60s cd)", null),

    // === PRECISION (ids 36-41) ===
    PREC_CORE(36, StatType.PRECISION, PerkTier.CORE, "Vis\u00e9e Stable", "+5% d\u00e9g\u00e2ts \u00e0 distance", null),
    PREC_ACTIVE(37, StatType.PRECISION, PerkTier.ACTIVE, "\u0152il de Lynx", "Chance critique +5%", null),
    PREC_SYNERGY(38, StatType.PRECISION, PerkTier.SYNERGY, "Tir Vital", "Les critiques font +25% d\u00e9g\u00e2ts", StatType.TRACKING),
    PREC_SITUATIONAL(39, StatType.PRECISION, PerkTier.SITUATIONAL, "Tir Long", "V\u00e9locit\u00e9 fl\u00e8che +20%, port\u00e9e augment\u00e9e", null),
    PREC_MASTERY(40, StatType.PRECISION, PerkTier.MASTERY, "Marque du Chasseur", "Premier hit marque 10s \u2192 +15% d\u00e9g\u00e2ts sur cible marqu\u00e9e", null),
    PREC_TRANSCENDENCE(41, StatType.PRECISION, PerkTier.TRANSCENDENCE, "\u0152il de l'Aigle", "3 fl\u00e8ches cons\u00e9cutives \u2192 prochaine +100% d\u00e9g\u00e2ts (20s cd)", null),

    // === TRACKING (ids 42-47) ===
    TRACK_CORE(42, StatType.TRACKING, PerkTier.CORE, "Instinct de Traqueur", "Port\u00e9e d\u00e9tection hostiles +5 blocs", null),
    TRACK_ACTIVE(43, StatType.TRACKING, PerkTier.ACTIVE, "\u00c9claireur", "Voir la vie des mobs (<15 blocs)", null),
    TRACK_SYNERGY(44, StatType.TRACKING, PerkTier.SYNERGY, "Patience du Pr\u00e9dateur", "Accroupi 3s \u2192 prochain coup +20%", StatType.PRECISION),
    TRACK_SITUATIONAL(45, StatType.TRACKING, PerkTier.SITUATIONAL, "Sillage", "Mobs bless\u00e9s laissent trace visible 5s", null),
    TRACK_MASTERY(46, StatType.TRACKING, PerkTier.MASTERY, "Tactique de Meute", "+5%/alli\u00e9 dans 10 blocs (max +25%)", null),
    TRACK_TRANSCENDENCE(47, StatType.TRACKING, PerkTier.TRANSCENDENCE, "Territoire", "Tuer un mob \u2192 zone 15 blocs : +15% d\u00e9g\u00e2ts 20s (60s cd)", null),

    // === KEEN_SENSES (ids 48-53) ===
    KEEN_CORE(48, StatType.KEEN_SENSES, PerkTier.CORE, "Vigilant", "Immunit\u00e9 c\u00e9cit\u00e9, vision nocturne zones sombres", null),
    KEEN_ACTIVE(49, StatType.KEEN_SENSES, PerkTier.ACTIVE, "\u00c9cholocation", "Mobs accroupis dans 8 blocs d\u00e9tectables", null),
    KEEN_SYNERGY(50, StatType.KEEN_SENSES, PerkTier.SYNERGY, "Sixi\u00e8me Sens", "Alerte quand un mob vous cible par derri\u00e8re", StatType.AGILITY),
    KEEN_SITUATIONAL(51, StatType.KEEN_SENSES, PerkTier.SITUATIONAL, "Sens du Danger", "Port\u00e9e corps-\u00e0-corps +2 blocs", null),
    KEEN_MASTERY(52, StatType.KEEN_SENSES, PerkTier.MASTERY, "Sens Aiguis\u00e9s", "Tous les effets de potions +15% dur\u00e9e", null),
    KEEN_TRANSCENDENCE(53, StatType.KEEN_SENSES, PerkTier.TRANSCENDENCE, "Omniscience", "Touch\u00e9 par attaque surprise \u2192 r\u00e9v\u00e9ler tous ennemis 30 blocs 4s (30s cd)", null),

    // === FORGING (ids 54-59) ===
    FORGE_CORE(54, StatType.FORGING, PerkTier.CORE, "R\u00e9paration Efficace", "Co\u00fbt enclume -20%", null),
    FORGE_ACTIVE(55, StatType.FORGING, PerkTier.ACTIVE, "Touche de Diamant", "Minage minerais +20% yield", null),
    FORGE_SYNERGY(56, StatType.FORGING, PerkTier.SYNERGY, "Armurier", "Armures forg\u00e9es +5% durabilit\u00e9", StatType.BRUTE_FORCE),
    FORGE_SITUATIONAL(57, StatType.FORGING, PerkTier.SITUATIONAL, "Ma\u00eetre d'Armes", "Armes forg\u00e9es +5% d\u00e9g\u00e2ts base", null),
    FORGE_MASTERY(58, StatType.FORGING, PerkTier.MASTERY, "Expert en Enchantement", "Relancer enchantements -25% co\u00fbt", null),
    FORGE_TRANSCENDENCE(59, StatType.FORGING, PerkTier.TRANSCENDENCE, "L\u00e9gende Vivante", "Forger \u00e0 l'enclume : 33% chance doubler le r\u00e9sultat", null),

    // === COOKING (ids 60-65) ===
    COOK_CORE(60, StatType.COOKING, PerkTier.CORE, "Gourmand", "Nourriture donne +2 barres de faim", null),
    COOK_ACTIVE(61, StatType.COOKING, PerkTier.ACTIVE, "R\u00e9gime \u00c9quilibr\u00e9", "3 aliments diff\u00e9rents en 2min \u2192 Regen I 5s", null),
    COOK_SYNERGY(62, StatType.COOKING, PerkTier.SYNERGY, "Touche du Chef", "Nourriture cuite \u2192 buff al\u00e9atoire 15s", StatType.ALCHEMY),
    COOK_SITUATIONAL(63, StatType.COOKING, PerkTier.SITUATIONAL, "Chef de Fer", "Effets nourriture dor\u00e9e +50%", null),
    COOK_MASTERY(64, StatType.COOKING, PerkTier.MASTERY, "Repas Nourrissant", "Premier repas post-combat \u2192 Absorption II 30s", null),
    COOK_TRANSCENDENCE(65, StatType.COOKING, PerkTier.TRANSCENDENCE, "Festin Royal", "Manger a 10% chance de partager l'effet \u00e0 10 blocs", null),

    // === ALCHEMY (ids 66-71) ===
    ALCHEMY_CORE(66, StatType.ALCHEMY, PerkTier.CORE, "Brasseur Assidu", "Potions brass\u00e9es +10% dur\u00e9e", null),
    ALCHEMY_ACTIVE(67, StatType.ALCHEMY, PerkTier.ACTIVE, "Mixeur de Potions", "+1 emplacement effet b\u00e9n\u00e9fique (6 au lieu de 5)", null),
    ALCHEMY_SYNERGY(68, StatType.ALCHEMY, PerkTier.SYNERGY, "Catalyseur", "Blaze rod donne 50% plus de brassages", StatType.COOKING),
    ALCHEMY_SITUATIONAL(69, StatType.ALCHEMY, PerkTier.SITUATIONAL, "Expert Alchimiste", "Potions splash +1 rayon, lingering +3s", null),
    ALCHEMY_MASTERY(70, StatType.ALCHEMY, PerkTier.MASTERY, "Transmutation", "Brasser \u2192 15% chance d'obtenir une 2e potion gratuite", null),
    ALCHEMY_TRANSCENDENCE(71, StatType.ALCHEMY, PerkTier.TRANSCENDENCE, "Philosophe", "Brasser \u2192 10% chance d'upgrader la potion d'un palier", null),

    // === INTIMIDATION (ids 72-77) ===
    INTIM_CORE(72, StatType.INTIMIDATION, PerkTier.CORE, "Regard Noir", "Mobs hostiles dans 4 blocs : -5% d\u00e9g\u00e2ts", null),
    INTIM_ACTIVE(73, StatType.INTIMIDATION, PerkTier.ACTIVE, "Pr\u00e9sence Menagante", "Mobs faibles (<20HP) dans 6 blocs : Lenteur I", null),
    INTIM_SYNERGY(74, StatType.INTIMIDATION, PerkTier.SYNERGY, "Coup D\u00e9moralisant", "Crits \u2192 Faiblesse I aux mobs proches 3s", StatType.WILLPOWER),
    INTIM_SITUATIONAL(75, StatType.INTIMIDATION, PerkTier.SITUATIONAL, "R\u00e9putation", "Mobs touch\u00e9s : 10% chance fuir 2s", null),
    INTIM_MASTERY(76, StatType.INTIMIDATION, PerkTier.MASTERY, "Ma\u00eetre de la Peur", "Effets d'intimidation durent +50%", null),
    INTIM_TRANSCENDENCE(77, StatType.INTIMIDATION, PerkTier.TRANSCENDENCE, "Pr\u00e9sence Absolue", "Touch\u00e9 \u2192 30% paralyser l'attaquant + 6 blocs 3s (30s cd)", null),

    // === WILLPOWER (ids 78-83) ===
    WILL_CORE(78, StatType.WILLPOWER, PerkTier.CORE, "Esprit Clair", "Effets n\u00e9gatifs -10% dur\u00e9e", null),
    WILL_ACTIVE(79, StatType.WILLPOWER, PerkTier.ACTIVE, "Fortitude Mentale", "Niveau de poison r\u00e9duit de 1", null),
    WILL_SYNERGY(80, StatType.WILLPOWER, PerkTier.SYNERGY, "Volont\u00e9 de Fer", "<20% HP \u2192 15% d\u00e9g\u00e2ts en moins", StatType.INTIMIDATION),
    WILL_SITUATIONAL(81, StatType.WILLPOWER, PerkTier.SITUATIONAL, "R\u00e9solution Purifiante", "S'accroupir 3s \u2192 dissipe 1 effet n\u00e9gatif (30s cd)", null),
    WILL_MASTERY(82, StatType.WILLPOWER, PerkTier.MASTERY, "Esprit Indomptable", "Survit \u00e0 un coup mortel avec 1 HP + 2s invuln (5min cd)", null),
    WILL_TRANSCENDENCE(83, StatType.WILLPOWER, PerkTier.TRANSCENDENCE, "Ascension", "Quand la mort vous frappe (si Indomptable en cd) \u2192 10% HP, purge effets, Regen IV 3s (5min cd)", null);

    public final int id;
    public final StatType stat;
    public final PerkTier tier;
    public final String name;
    public final String description;
    public final StatType synergyStat;

    Perk(int id, StatType stat, PerkTier tier, String name, String description, StatType synergyStat) {
        this.id = id;
        this.stat = stat;
        this.tier = tier;
        this.name = name;
        this.description = description;
        this.synergyStat = synergyStat;
    }

    public static Perk byId(int id) {
        for (Perk p : values()) {
            if (p.id == id) return p;
        }
        return null;
    }

    public static Perk byStatAndTier(StatType stat, PerkTier tier) {
        for (Perk p : values()) {
            if (p.stat == stat && p.tier == tier) return p;
        }
        return null;
    }
}

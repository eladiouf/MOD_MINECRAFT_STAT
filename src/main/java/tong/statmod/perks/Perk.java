package tong.statmod.perks;

import tong.statmod.stats.StatType;

public enum Perk {
    // Force Brute
    BRUTE_DEMOLITION(0, StatType.BRUTE_FORCE, 20, "Démolition", "Dégâts aux blocs +50%"),
    BRUTE_ARMOR_PIERCE(1, StatType.BRUTE_FORCE, 50, "Perce-Armure", "Dégâts aux armures +15%"),
    BRUTE_STUN(2, StatType.BRUTE_FORCE, 80, "Étourdissement", "Coups chargés étourdissent 1s"),

    // Blade Technique
    BLADE_PARRY(3, StatType.BLADE_TECHNIQUE, 20, "Parade Améliorée", "Fenêtre de parade +25%"),
    BLADE_COMBO(4, StatType.BLADE_TECHNIQUE, 50, "Combo Mortel", "5 hits → prochain coup +50%"),
    BLADE_BLEED(5, StatType.BLADE_TECHNIQUE, 80, "Hémorragie", "3 mobs touchés en 1 coup → saignement"),

    // Rapidité
    RAPID_ELAN(6, StatType.RAPIDITE, 20, "Élan", "+10% vitesse après attaque (2s)"),
    RAPID_DOUBLE(7, StatType.RAPIDITE, 50, "Double Frappe", "10% chance double-hit"),
    RAPID_INSTINCT(8, StatType.RAPIDITE, 80, "Instinct d'Esquive", "Esquiver → +50% vitesse attaque (3s)"),

    // Agility
    AGILITY_JUMP(9, StatType.AGILITY, 20, "Saut de Combat", "+20% hauteur saut en combat"),
    AGILITY_SPRINT(10, StatType.AGILITY, 50, "Course Percutante", "Sprint-saut inflige dégâts de chute"),
    AGILITY_ELYTRA(11, StatType.AGILITY, 80, "Vol de Combat", "Elytra +10% vitesse en combat"),

    // Physical Resistance
    RESIST_FALL(12, StatType.PHYSICAL_RESISTANCE, 20, "Atterrissage", "+10% résistance chute"),
    RESIST_ABSORB(13, StatType.PHYSICAL_RESISTANCE, 50, "Endurci", "Absorption 1 coeur après dégâts (10s cd)"),
    RESIST_TOUGHNESS(14, StatType.PHYSICAL_RESISTANCE, 80, "Cœur de Pierre", "30% chance dégâts réduits de moitié"),

    // Physical Endurance
    ENDURANCE_MAGIC(15, StatType.PHYSICAL_ENDURANCE, 20, "Protection Totale", "Bloquer réduit dégâts magiques 20%"),
    ENDURANCE_MOBILE(16, StatType.PHYSICAL_ENDURANCE, 50, "Rempart Mobile", "Sprint avec bouclier levé"),
    ENDURANCE_PERFECT(17, StatType.PHYSICAL_ENDURANCE, 80, "Parade Parfaite", "Parer annule dégâts + repousse"),

    // Precision
    PRECISION_SNIPER(18, StatType.PRECISION, 20, "Sniper", "+10% dégâts à +15 blocs"),
    PRECISION_CRIT(19, StatType.PRECISION, 50, "Tir Perforant", "Crit knockback garanti"),
    PRECISION_PIERCE(20, StatType.PRECISION, 80, "Perforation", "Tir traverse +1 mob"),
    
    // Magic (standby – empty perks for now)
    // Survival
    TRACKING_SCENT(21, StatType.TRACKING, 20, "Flair", "+50% portée détection mobs"),
    TRACKING_PACK(22, StatType.TRACKING, 50, "Meute", "+15% dégâts contre mobs déjà touchés"),
    TRACKING_STALKER(23, StatType.TRACKING, 80, "Prédateur", "Les mobs vous repèrent +tard"),
    KEEN_NIGHT(24, StatType.KEEN_SENSES, 20, "Vision Nocturne", "Vision améliorée la nuit"),
    KEEN_DANGER(25, StatType.KEEN_SENSES, 50, "Danger Approche", "Alerté quand un mob vous vise"),
    KEEN_EXPLORER(26, StatType.KEEN_SENSES, 80, "Explorateur", "Cartes révèlent + de terrain"),
    // Crafting
    FORGE_REPAIR(27, StatType.FORGING, 20, "Réparation", "-20% coût réparation enclume"),
    FORGE_NETHERITE(28, StatType.FORGING, 50, "Maître Forgeron", "Les outils durent +25%"),
    FORGE_TEMPLATE(29, StatType.FORGING, 80, "Template Expert", "Les templates ne se consument pas"),
    COOK_FEAST(30, StatType.COOKING, 20, "Festin", "Les repas donnent +20% saturation"),
    COOK_GRILL(31, StatType.COOKING, 50, "Grillade", "Cuire de la viande donne parfois un extra"),
    COOK_CHEF(32, StatType.COOKING, 80, "Chef", "Les gâteaux et plats complexes donnent +50% XP"),
    ALCHEMY_BREWER(33, StatType.ALCHEMY, 20, "Brasseur", "Les potions durent +20%"),
    ALCHEMY_SPLASH(34, StatType.ALCHEMY, 50, "Splash Master", "Les potions splash ont +1 zone"),
    ALCHEMY_EXTEND(35, StatType.ALCHEMY, 80, "Alchimiste Extrême", "Les modificateurs redstone/glowstone sont +25% efficaces"),
    // Mental
    INTIMIDATE_AURA(36, StatType.INTIMIDATION, 20, "Aura Menacante", "Les mobs faibles fuient"),
    INTIMIDATE_ROAR(37, StatType.INTIMIDATION, 50, "Rugissement", "Tuer un mob effraie les autres"),
    INTIMIDATE_FEARLESS(38, StatType.INTIMIDATION, 80, "Sans Peur", "+25% dégâts quand en infériorité numérique"),
    WILL_FOCUS(39, StatType.WILLPOWER, 20, "Concentration", "Les effets négatifs durent -15%"),
    WILL_ENDURANCE(40, StatType.WILLPOWER, 50, "Endurance Mentale", "Régénération naturelle même sous poison"),
    WILL_UNBREAKABLE(41, StatType.WILLPOWER, 80, "Inébranlable", "Quand < 2 coeurs, +30% dégâts et résistance");

    public final int id;
    public final StatType stat;
    public final int levelRequired;
    public final String name;
    public final String description;

    Perk(int id, StatType stat, int levelRequired, String name, String description) {
        this.id = id;
        this.stat = stat;
        this.levelRequired = levelRequired;
        this.name = name;
        this.description = description;
    }

    public static Perk byId(int id) {
        for (Perk p : values()) {
            if (p.id == id) return p;
        }
        return null;
    }

    public int getEffectiveLevelRequired() {
        return switch (levelRequired) {
            case 20 -> tong.statmod.Config.perkTier1Level;
            case 50 -> tong.statmod.Config.perkTier2Level;
            case 80 -> tong.statmod.Config.perkTier3Level;
            default -> levelRequired;
        };
    }
}

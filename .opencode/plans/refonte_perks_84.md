# Plan d'implémentation : Refonte du système de Perks (84 perks)

## Résumé des changements

- **Perk.java** : Passe de 42 → 84 perks (6 par stat × 14 stats)
- **PerkManager.java** : Points gérés **par stat** (10 pts/stat, coût 1-2 par perk)
- **PerkEffectHandler.java** : 84 effets à implémenter
- **LevelUpHandler.java** : Accorder 1 point de perk tout les 10 niveaux (10→100)
- **GUI** : PerkScreen + TalentTreePanel + PerkNodeWidget à adapter pour coûts, tiers, capstones

---

## Phase 0 : Setup préalable — PerkTier.java

**Fichier :** `src/main/java/tong/statmod/perks/PerkTier.java`

```java
package tong.statmod.perks;

public enum PerkTier {
    CORE(10, 1),
    ACTIVE(25, 1),
    SYNERGY(40, 2),
    SITUATIONAL(55, 2),
    MASTERY(75, 2),
    TRANSCENDENCE(95, 2);

    public final int levelRequired;
    public final int cost;

    PerkTier(int levelRequired, int cost) {
        this.levelRequired = levelRequired;
        this.cost = cost;
    }
}
```

---

## Phase 1 : Perk.java — Nouvel Enum (84 entrées)

**Fichier :** `src/main/java/tong/statmod/perks/Perk.java`

Remplacer TOUT le fichier. Nouveaux champs par Perk :
- `int id` (0–83, continu)
- `StatType stat` (la stat associée)
- `PerkTier tier` (CORE, ACTIVE, SYNERGY, SITUATIONAL, MASTERY, TRANSCENDENCE)
- `String name` (français)
- `String description` (français)
- `StatType synergyStat` (null si pas de synergie — seulement pour SYNERGY tier)

**Règles :**
- Les ids doivent être contigus : stat 0 (Brute Force) ids 0-5, stat 1 (Blade Technique) ids 6-11, etc.
- Chaque stat a EXACTEMENT 6 perks : tier CORE → ACTIVE → SYNERGY → SITUATIONAL → MASTERY → TRANSCENDENCE
- Les perks SYNERGY ont un `synergyStat` non-null, les autres ont null

**Mapping stat → offset id :**
```
Brute Force: 0-5
Blade Technique: 6-11
Rapidité: 12-17
Agility: 18-23
Physical Resistance: 24-29
Physical Endurance: 30-35
Precision: 36-41
Tracking: 42-47
Keen Senses: 48-53
Forging: 54-59
Cooking: 60-65
Alchemy: 66-71
Intimidation: 72-77
Willpower: 78-83
```

**Perks détaillés :**

```java
// === BRUTE_FORCE (ids 0-5) ===
BRUTE_CORE(0, StatType.BRUTE_FORCE, PerkTier.CORE, "Heavy Swing", "+5% dégâts haches et masses", null),
BRUTE_ACTIVE(1, StatType.BRUTE_FORCE, PerkTier.ACTIVE, "Brise-Bouclier", "15% désactive bouclier ennemi 2s", null),
BRUTE_SYNERGY(2, StatType.BRUTE_FORCE, PerkTier.SYNERGY, "Frappe Écrasante", "Attaques chargées ignorent 10% armure", StatType.PHYSICAL_ENDURANCE),
BRUTE_SITUATIONAL(3, StatType.BRUTE_FORCE, PerkTier.SITUATIONAL, "Adrénaline", "Tuer un mob restaure 2 endurance", null),
BRUTE_MASTERY(4, StatType.BRUTE_FORCE, PerkTier.MASTERY, "Carnage", "Tuer un mob = coup en arc 180° touche tous les ennemis", null),
BRUTE_TRANSCENDENCE(5, StatType.BRUTE_FORCE, PerkTier.TRANSCENDENCE, "Colosse", "<50% HP + kill crit → +50% dégâts/knockback 4s (60s cd)", null),

// === BLADE_TECHNIQUE (ids 6-11) ===
BLADE_CORE(6, StatType.BLADE_TECHNIQUE, PerkTier.CORE, "Riposte", "Parade parfaite → prochain coup +30% (3s)", null),
BLADE_ACTIVE(7, StatType.BLADE_TECHNIQUE, PerkTier.ACTIVE, "Escrimeur", "+5% vitesse d'attaque épées", null),
BLADE_SYNERGY(8, StatType.BLADE_TECHNIQUE, PerkTier.SYNERGY, "Blessures", "10% chance hémorragie 4s", StatType.RAPIDITE),
BLADE_SITUATIONAL(9, StatType.BLADE_TECHNIQUE, PerkTier.SITUATIONAL, "Danse Lames", "3 kills / 10s → +15% dégâts cumulable", null),
BLADE_MASTERY(10, StatType.BLADE_TECHNIQUE, PerkTier.MASTERY, "Maître d'Armes", "Fenêtre parade +30%, coût parade -20%", null),
BLADE_TRANSCENDENCE(11, StatType.BLADE_TECHNIQUE, PerkTier.TRANSCENDENCE, "Tempête d'Acier", "3 kills streak → coups en arc 180° 5s (45s cd)", null),

// === RAPIDITÉ (ids 12-17) ===
RAPID_CORE(12, StatType.RAPIDITE, PerkTier.CORE, "Réflexes Vifs", "+5% vitesse armes légères (dagues/lances)", null),
RAPID_ACTIVE(13, StatType.RAPIDITE, PerkTier.ACTIVE, "Frappe Perçante", "15% chance dague ignore 15% armure", null),
RAPID_SYNERGY(14, StatType.RAPIDITE, PerkTier.SYNERGY, "Momentum", "Coups consécutifs +3% dégâts (max +30%, reset si manqué)", StatType.AGILITY),
RAPID_SITUATIONAL(15, StatType.RAPIDITE, PerkTier.SITUATIONAL, "Roulade", "Roulade coûte 15% moins d'endurance", null),
RAPID_MASTERY(16, StatType.RAPIDITE, PerkTier.MASTERY, "Vitesse Aveuglante", "Après roulade, 3 coups : +10% vitesse et dégâts", null),
RAPID_TRANSCENDENCE(17, StatType.RAPIDITE, PerkTier.TRANSCENDENCE, "Frénésie", "<20% endurance + touche → vitesse max 5s (45s cd)", null),

// === AGILITY (ids 18-23) ===
AGILITY_CORE(18, StatType.AGILITY, PerkTier.CORE, "Pied Léger", "+5% vitesse déplacement", null),
AGILITY_ACTIVE(19, StatType.AGILITY, PerkTier.ACTIVE, "Acrobate", "Saut +15%, chute -15%", null),
AGILITY_SYNERGY(20, StatType.AGILITY, PerkTier.SYNERGY, "Pas de Loup", "Aucun malus de terrain (sable des âmes, etc.)", StatType.RAPIDITE),
AGILITY_SITUATIONAL(21, StatType.AGILITY, PerkTier.SITUATIONAL, "Expert Évasion", "Esquiver supprime tout effet de lenteur", null),
AGILITY_MASTERY(22, StatType.AGILITY, PerkTier.MASTERY, "Danse des Lames", "Sprint 10 blocs → prochain coup +25%", null),
AGILITY_TRANSCENDENCE(23, StatType.AGILITY, PerkTier.TRANSCENDENCE, "Zéphyr", "Esquiver avec 2+ ennemis → +50% vitesse 4s, traverse les mobs (45s cd)", null),

// === PHYSICAL_RESISTANCE (ids 24-29) ===
RESIST_CORE(24, StatType.PHYSICAL_RESISTANCE, PerkTier.CORE, "Peau Dure", "-1 dégât subi (min 0,5)", null),
RESIST_ACTIVE(25, StatType.PHYSICAL_RESISTANCE, PerkTier.ACTIVE, "Estomac de Fer", "Nourriture +10% saturation", null),
RESIST_SYNERGY(26, StatType.PHYSICAL_RESISTANCE, PerkTier.SYNERGY, "Mur de Bouclier", "Bloquer réduit 5% dégâts supplémentaires", StatType.PHYSICAL_ENDURANCE),
RESIST_SITUATIONAL(27, StatType.PHYSICAL_RESISTANCE, PerkTier.SITUATIONAL, "Inébranlable", "<30% HP : +15% armure et knockback resist", null),
RESIST_MASTERY(28, StatType.PHYSICAL_RESISTANCE, PerkTier.MASTERY, "Vétéran", "Dégâts sur durée réduits 25%", null),
RESIST_TRANSCENDENCE(29, StatType.PHYSICAL_RESISTANCE, PerkTier.TRANSCENDENCE, "Implacable", "<30% HP + touché → 75% réduc dégâts 3s (90s cd)", null),

// === PHYSICAL_ENDURANCE (ids 30-35) ===
ENDUR_CORE(30, StatType.PHYSICAL_ENDURANCE, PerkTier.CORE, "Infatigable", "Régénération endurance +10%", null),
ENDUR_ACTIVE(31, StatType.PHYSICAL_ENDURANCE, PerkTier.ACTIVE, "Second Souffle", "<15% endurance → régen +50% 3s", null),
ENDUR_SYNERGY(32, StatType.PHYSICAL_ENDURANCE, PerkTier.SYNERGY, "Marathonien", "Coût sprint -15%", StatType.PHYSICAL_RESISTANCE),
ENDUR_SITUATIONAL(33, StatType.PHYSICAL_ENDURANCE, PerkTier.SITUATIONAL, "Adrénaline", "Prendre dégâts → +5% endurance (10s cd)", null),
ENDUR_MASTERY(34, StatType.PHYSICAL_ENDURANCE, PerkTier.MASTERY, "Condition de Pointe", "Aucun malus d'armure lourde sur endurance", null),
ENDUR_TRANSCENDENCE(35, StatType.PHYSICAL_ENDURANCE, PerkTier.TRANSCENDENCE, "Réservoir Infini", "Endurance à 0 en combat → 5s sans coût (60s cd)", null),

// === PRECISION (ids 36-41) ===
PREC_CORE(36, StatType.PRECISION, PerkTier.CORE, "Visée Stable", "+5% dégâts à distance", null),
PREC_ACTIVE(37, StatType.PRECISION, PerkTier.ACTIVE, "Œil de Lynx", "Chance critique +5%", null),
PREC_SYNERGY(38, StatType.PRECISION, PerkTier.SYNERGY, "Tir Vital", "Les critiques font +25% dégâts", StatType.TRACKING),
PREC_SITUATIONAL(39, StatType.PRECISION, PerkTier.SITUATIONAL, "Tir Long", "Vélocité flèche +20%, portée augmentée", null),
PREC_MASTERY(40, StatType.PRECISION, PerkTier.MASTERY, "Marque du Chasseur", "Premier hit marque 10s → +15% dégâts sur cible marquée", null),
PREC_TRANSCENDENCE(41, StatType.PRECISION, PerkTier.TRANSCENDENCE, "Œil de l'Aigle", "3 flèches consécutives → prochaine +100% dégâts (20s cd)", null),

// === TRACKING (ids 42-47) ===
TRACK_CORE(42, StatType.TRACKING, PerkTier.CORE, "Instinct de Traqueur", "Portée détection hostiles +5 blocs", null),
TRACK_ACTIVE(43, StatType.TRACKING, PerkTier.ACTIVE, "Éclaireur", "Voir la vie des mobs (<15 blocs)", null),
TRACK_SYNERGY(44, StatType.TRACKING, PerkTier.SYNERGY, "Patience du Prédateur", "Accroupi 3s → prochain coup +20%", StatType.PRECISION),
TRACK_SITUATIONAL(45, StatType.TRACKING, PerkTier.SITUATIONAL, "Sillage", "Mobs blessés laissent trace visible 5s", null),
TRACK_MASTERY(46, StatType.TRACKING, PerkTier.MASTERY, "Tactique de Meute", "+5%/allié dans 10 blocs (max +25%)", null),
TRACK_TRANSCENDENCE(47, StatType.TRACKING, PerkTier.TRANSCENDENCE, "Territoire", "Tuer un mob → zone 15 blocs : +15% dégâts 20s (60s cd)", null),

// === KEEN_SENSES (ids 48-53) ===
KEEN_CORE(48, StatType.KEEN_SENSES, PerkTier.CORE, "Vigilant", "Immunité cécité, vision nocturne zones sombres", null),
KEEN_ACTIVE(49, StatType.KEEN_SENSES, PerkTier.ACTIVE, "Écholocation", "Mobs accroupis dans 8 blocs détectables", null),
KEEN_SYNERGY(50, StatType.KEEN_SENSES, PerkTier.SYNERGY, "Sixième Sens", "Alerte quand un mob vous cible par derrière", StatType.AGILITY),
KEEN_SITUATIONAL(51, StatType.KEEN_SENSES, PerkTier.SITUATIONAL, "Sens du Danger", "Portée corps-à-corps +2 blocs", null),
KEEN_MASTERY(52, StatType.KEEN_SENSES, PerkTier.MASTERY, "Sens Aiguisés", "Tous les effets de potions +15% durée", null),
KEEN_TRANSCENDENCE(53, StatType.KEEN_SENSES, PerkTier.TRANSCENDENCE, "Omniscience", "Touché par attaque surprise → révéler tous ennemis 30 blocs 4s (30s cd)", null),

// === FORGING (ids 54-59) ===
FORGE_CORE(54, StatType.FORGING, PerkTier.CORE, "Réparation Efficace", "Coût enclume -20%", null),
FORGE_ACTIVE(55, StatType.FORGING, PerkTier.ACTIVE, "Touche de Diamant", "Minage minerais +20% yield", null),
FORGE_SYNERGY(56, StatType.FORGING, PerkTier.SYNERGY, "Armurier", "Armures forgées +5% durabilité", StatType.BRUTE_FORCE),
FORGE_SITUATIONAL(57, StatType.FORGING, PerkTier.SITUATIONAL, "Maître d'Armes", "Armes forgées +5% dégâts base", null),
FORGE_MASTERY(58, StatType.FORGING, PerkTier.MASTERY, "Expert en Enchantement", "Relancer enchantements -25% coût", null),
FORGE_TRANSCENDENCE(59, StatType.FORGING, PerkTier.TRANSCENDENCE, "Légende Vivante", "Forger à l'enclume : 33% chance doubler le résultat", null),

// === COOKING (ids 60-65) ===
COOK_CORE(60, StatType.COOKING, PerkTier.CORE, "Gourmand", "Nourriture donne +2 barres de faim", null),
COOK_ACTIVE(61, StatType.COOKING, PerkTier.ACTIVE, "Régime Équilibré", "3 aliments différents en 2min → Regen I 5s", null),
COOK_SYNERGY(62, StatType.COOKING, PerkTier.SYNERGY, "Touche du Chef", "Nourriture cuite → buff aléatoire 15s", StatType.ALCHEMY),
COOK_SITUATIONAL(63, StatType.COOKING, PerkTier.SITUATIONAL, "Chef de Fer", "Effets nourriture dorée +50%", null),
COOK_MASTERY(64, StatType.COOKING, PerkTier.MASTERY, "Repas Nourrissant", "Premier repas post-combat → Absorption II 30s", null),
COOK_TRANSCENDENCE(65, StatType.COOKING, PerkTier.TRANSCENDENCE, "Festin Royal", "Manger a 10% chance de partager l'effet à 10 blocs", null),

// === ALCHEMY (ids 66-71) ===
ALCHEMY_CORE(66, StatType.ALCHEMY, PerkTier.CORE, "Brasseur Assidu", "Potions brassées +10% durée", null),
ALCHEMY_ACTIVE(67, StatType.ALCHEMY, PerkTier.ACTIVE, "Mixeur de Potions", "+1 emplacement effet bénéfique (6 au lieu de 5)", null),
ALCHEMY_SYNERGY(68, StatType.ALCHEMY, PerkTier.SYNERGY, "Catalyseur", "Blaze rod donne 50% plus de brassages", StatType.COOKING),
ALCHEMY_SITUATIONAL(69, StatType.ALCHEMY, PerkTier.SITUATIONAL, "Expert Alchimiste", "Potions splash +1 rayon, lingering +3s", null),
ALCHEMY_MASTERY(70, StatType.ALCHEMY, PerkTier.MASTERY, "Transmutation", "Brasser → 15% chance d'obtenir une 2e potion gratuite", null),
ALCHEMY_TRANSCENDENCE(71, StatType.ALCHEMY, PerkTier.TRANSCENDENCE, "Philosophe", "Brasser → 10% chance d'upgrader la potion d'un palier", null),

// === INTIMIDATION (ids 72-77) ===
INTIM_CORE(72, StatType.INTIMIDATION, PerkTier.CORE, "Regard Noir", "Mobs hostiles dans 4 blocs : -5% dégâts", null),
INTIM_ACTIVE(73, StatType.INTIMIDATION, PerkTier.ACTIVE, "Présence Menagante", "Mobs faibles (<20HP) dans 6 blocs : Lenteur I", null),
INTIM_SYNERGY(74, StatType.INTIMIDATION, PerkTier.SYNERGY, "Coup Démoralisant", "Crits → Faiblesse I aux mobs proches 3s", StatType.WILLPOWER),
INTIM_SITUATIONAL(75, StatType.INTIMIDATION, PerkTier.SITUATIONAL, "Réputation", "Mobs touchés : 10% chance fuir 2s", null),
INTIM_MASTERY(76, StatType.INTIMIDATION, PerkTier.MASTERY, "Maître de la Peur", "Effets d'intimidation durent +50%", null),
INTIM_TRANSCENDENCE(77, StatType.INTIMIDATION, PerkTier.TRANSCENDENCE, "Présence Absolue", "Touché → 30% paralyser l'attaquant + 6 blocs 3s (30s cd)", null),

// === WILLPOWER (ids 78-83) ===
WILL_CORE(78, StatType.WILLPOWER, PerkTier.CORE, "Esprit Clair", "Effets négatifs -10% durée", null),
WILL_ACTIVE(79, StatType.WILLPOWER, PerkTier.ACTIVE, "Fortitude Mentale", "Niveau de poison réduit de 1", null),
WILL_SYNERGY(80, StatType.WILLPOWER, PerkTier.SYNERGY, "Volonté de Fer", "<20% HP → 15% dégâts en moins", StatType.INTIMIDATION),
WILL_SITUATIONAL(81, StatType.WILLPOWER, PerkTier.SITUATIONAL, "Résolution Purifiante", "S'accroupir 3s → dissipe 1 effet négatif (30s cd)", null),
WILL_MASTERY(82, StatType.WILLPOWER, PerkTier.MASTERY, "Esprit Indomptable", "Survit à un coup mortel avec 1 HP + 2s invuln (5min cd)", null),
WILL_TRANSCENDENCE(83, StatType.WILLPOWER, PerkTier.TRANSCENDENCE, "Ascension", "Quand la mort vous frappe (si Indomptable en cd) → 10% HP, purge effets, Regen IV 3s (5min cd)", null)
```

**Constructeur et helpers :**

```java
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
```

---

## Phase 2 : PerkManager.java — Points par stat

**Fichier :** `src/main/java/tong/statmod/perks/PerkManager.java`

Remplacer le point compteur global par un système par-stat.

```java
package tong.statmod.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.stats.StatType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PerkManager implements INBTSerializable<CompoundTag> {
    private static final int STAT_COUNT = 14;

    private final int[] statPoints = new int[STAT_COUNT];
    private final Set<Integer> unlockedPerks = new HashSet<>();

    public boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public boolean isUnlocked(int id) { return unlockedPerks.contains(id); }
    public Set<Integer> getUnlockedPerks() { return unlockedPerks; }

    public int getPoints(StatType stat) {
        int idx = stat.ordinal();
        return idx < statPoints.length ? statPoints[idx] : 0;
    }

    public int[] getAllPoints() { return statPoints.clone(); }

    public int getTotalPoints() { return Arrays.stream(statPoints).sum(); }

    public void addPoint(StatType stat) {
        int idx = stat.ordinal();
        if (idx < statPoints.length) {
            statPoints[idx]++;
        }
    }

    public boolean canUnlock(Perk perk, int currentStatLevel) {
        if (unlockedPerks.contains(perk.id)) return false;
        if (currentStatLevel < perk.tier.levelRequired) return false;
        int idx = perk.stat.ordinal();
        if (idx >= statPoints.length) return false;
        return statPoints[idx] >= perk.tier.cost;
    }

    public boolean unlockPerk(Perk perk, int currentStatLevel) {
        if (!canUnlock(perk, currentStatLevel)) return false;
        unlockedPerks.add(perk.id);
        statPoints[perk.stat.ordinal()] -= perk.tier.cost;
        return true;
    }

    public void resetPoints() { Arrays.fill(statPoints, 0); }

    public void resetAll() {
        unlockedPerks.clear();
        resetPoints();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("StatPoints", statPoints);
        ListTag list = new ListTag();
        for (int id : unlockedPerks) {
            list.add(IntTag.valueOf(id));
        }
        tag.put("UnlockedPerks", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loaded = tag.getIntArray("StatPoints");
        if (loaded.length == statPoints.length) {
            System.arraycopy(loaded, 0, statPoints, 0, statPoints.length);
        }
        unlockedPerks.clear();
        ListTag list = tag.getList("UnlockedPerks", 3);
        for (int i = 0; i < list.size(); i++) {
            unlockedPerks.add(list.getInt(i));
        }
    }
}
```

---

## Phase 3 : PerkEffectHandler.java — Tous les 84 effets

**Fichier :** `src/main/java/tong/statmod/perks/PerkEffectHandler.java`

Structure d'events listeners avec TOUS les perks. Voici les grandes sections :

```java
@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkEffectHandler {
    // Maps pour cooldowns, compteurs, marques
    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> comboTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> markedTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> foodTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> trackedFoods = new ConcurrentHashMap<>();

    // Helpers : hasPerk(), getStatLevel(), isOnCooldown(), setCooldown()
}
```

### SECTION A — onLivingHurt (JOUEUR ATTAQUE)

Implémenter les perks suivants (vérifier `hasPerk(attacker, Perk.XXX)`) :

| Perk | Effet |
|------|-------|
| BRUTE_CORE | +5% si hache/masse |
| BRUTE_ACTIVE | 15% désactive bouclier |
| BRUTE_SYNERGY | +7% si attaque chargée (ignore armure) |
| BRUTE_MASTERY | Kill → sweep 4 blocs 50% dégâts |
| BRUTE_TRANSCENDENCE | <50% HP + kill crit → +50% + knockback (60s cd) |
| BLADE_CORE | Après parade → +30% |
| BLADE_SYNERGY | 10% Wither 4s |
| BLADE_SITUATIONAL | 3 kills/10s → +15% cumulable |
| BLADE_TRANSCENDENCE | Kill streak 3 → sweep 180° 5s |
| RAPID_CORE | +5% si dague/lance |
| RAPID_ACTIVE | 15% +15% dégâts si dague |
| RAPID_SYNERGY | Coups consécutifs +3%/stack (max +30%) |
| AGILITY_MASTERY | Sprint → +25% |
| PRECISION_CORE | +5% si arc |
| PRECISION_SYNERGY | Attaque chargée → +25% crit |
| PRECISION_MASTERY | Cible marquée → +15% |
| TRACKING_SYNERGY | Accroupi → +20% |
| TRACKING_MASTERY | +5%/allié (max +25%) |
| INTIM_SYNERGY | Crit → Weakness aux mobs 5 blocs |
| INTIM_SITUATIONAL | 10% mob fuit |

### SECTION B — onLivingHurt (JOUEUR DÉFENSEUR)

| Perk | Effet |
|------|-------|
| RESIST_CORE | -1 flat (min 0.5) |
| RESIST_SYNERGY | Bloque +5% réduction |
| RESIST_SITUATIONAL | <30% HP → +15% réduction |
| RESIST_TRANSCENDENCE | <30% HP + touché → 75% réduction 3s (90s cd) |
| WILL_SYNERGY | <20% HP → 15% réduction |
| ENDUR_SITUATIONAL | Prendre dégâts → +5% endurance (10s cd) |

### SECTION C — onPlayerTick (chaque seconde)

| Perk | Effet |
|------|-------|
| BLADE_ACTIVE | +5% attack speed |
| RAPID_CORE | +5% attack speed (weapon) |
| AGILITY_CORE | +5% move speed |
| AGILITY_ACTIVE | Jump boost I |
| KEEN_CORE | Night vision si zone sombre |
| KEEN_ACTIVE | Glow sur mobs accroupis 8 blocs |
| KEEN_SYNERGY | Alerte si mob derrière |
| TRACKING_CORE | (déjà géré par StatPassiveEffects) |
| TRACKING_ACTIVE | (HP display — optionnel) |
| INTIM_CORE | Weakness aux mobs proches |
| INTIM_ACTIVE | Slowness aux mobs faibles proches |
| WILL_MASTERY | (death protection — géré dans LivingHurtEvent via le flag) |

### SECTION D — Autres events

- **MobEffectEvent.Added** : WILL_CORE (-10% durée négative), WILL_ACTIVE (réduit poison)
- **LivingFallEvent** : AGILITY_ACTIVE (-15% chute)
- **LivingEntityUseItemEvent.Finish** : RESIST_ACTIVE (+10% sat), COOK_CORE (+2 faim), COOK_ACTIVE (3 aliments → Regen), COOK_MASTERY (Absorption), COOK_TRANSCENDENCE (10% share)
- **AnvilUpdateEvent** : FORGE_CORE (-20%), FORGE_MASTERY (-25% re-roll)
- **PlayerEvent.PlayerLoggedOutEvent** : cleanup des maps

---

## Phase 4 : LevelUpHandler.java — Accorder des points de perk

**Fichier :** `src/main/java/tong/statmod/events/LevelUpHandler.java`

Ouvrir le fichier et REPÉRER la méthode `onLevelUp`. Ajouter APRÈS les milestones existantes :

```java
// Accorder un point de perk tout les 10 niveaux
if (newLevel % 10 == 0) {
    player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
        perks.addPoint(statType);
        STATMod.LOGGER.debug("{} a reçu 1 point de perk pour {} (niv. {})",
            player.getName().getString(), statType.name(), newLevel);
    });
}
```

---

## Phase 5 : Mise à jour de la GUI

### 5.1 PerkNodeWidget.java

**Fichier :** `src/main/java/tong/statmod/client/gui/perks/PerkNodeWidget.java`

Modifier la classe existante :

1. Changer le constructeur pour prendre `Perk` au lieu de l'id brut
2. Stocker `perk`, `unlocked`, `canAfford`, `currentLevel`
3. Rendu : couleur de fond selon le tier (bronze/argent/vert/bleu/violet/or)
4. Si `!canAfford` : overlay semi-transparent gris
5. Si `unlocked` : bordure + coche verte
6. Si `perk.tier == TRANSCENDENCE` : taille 1.5× + glow
7. Afficher le coût si > 1 : pastille rouge en haut à droite

### 5.2 TalentTreePanel.java

**Fichier :** `src/main/java/tong/statmod/client/gui/perks/TalentTreePanel.java`

Modifier pour afficher UNE colonne de 6 nodes par stat :

1. Itérer `PerkTier.values()` (6 entries)
2. Pour chaque tier : `Perk.byStatAndTier(stat, tier)`
3. Afficher un connecteur vertical entre les nodes
4. Connecteur vert si le node parent est débloqué, gris sinon
5. Node TRANSCENDENCE plus grand

### 5.3 PerkScreen.java

**Fichier :** `src/main/java/tong/statmod/client/gui/PerkScreen.java`

Ajouter l'affichage des points disponibles en haut de l'écran :
```java
int pts = perks.getPoints(currentStat);
int totalCost = 0;
for (int id : perks.getUnlockedPerks()) {
    Perk p = Perk.byId(id);
    if (p != null && p.stat == currentStat) totalCost += p.tier.cost;
}
// Afficher "Points: X | Utilisés: Y"
```

---

## Phase 6 : StatsCommands.java — Commande debug

**Fichier :** `src/main/java/tong/statmod/command/StatsCommands.java`

Ajouter dans la switch ou if-else des sous-commandes :
```java
// /statmod perkpoints <stat> <amount>
```

---

## Ordre d'implémentation recommandé

1. **PerkTier.java** — nouveau fichier, 5min
2. **Perk.java** — remplacer l'enum, 15min
3. **PerkManager.java** — rewrite, 15min
4. **LevelUpHandler.java** — 5min
5. **PerkEffectHandler.java** — 60-90min (le plus gros)
6. **PerkNodeWidget.java** — 20min
7. **TalentTreePanel.java** — 20min
8. **PerkScreen.java** — 10min
9. **StatsCommands.java** — 10min
10. **Build** — 15min

**Total estimé : ~3h**

---

## Vérifications

- `./gradlew build` passe
- `/statmod set brute_force 10` → +1 point de perk
- `/statmod set brute_force 100` → 10 points
- Écran perks → 6 nodes visibles par stat
- Débloquer → points déduits, effet actif
- Capstones auto-déclenchées sous condition

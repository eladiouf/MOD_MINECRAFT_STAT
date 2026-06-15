# Tensura Reincarnated Integration Design

> **STAT Mod** dépend désormais obligatoirement de **Tensura Reincarnated** (NeoForge 1.21.1).
> Plus de `ModList.get().isLoaded("tensura")` — Tensura est un hard dependency.

---

## 1. Dépendance & Build

```groovy
// build.gradle
repositories {
    maven { url = 'https://jitpack.io' }
    maven { url = 'https://maven.neoforged.net/releases' }
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    implementation "com.github.manasmods:TensuraMod:${minecraft_version}-${tensura_version}"
}
```

- L'API Tensura est disponible au compile-time via JitPack
- STAT Mod reste NeoForge-only (pas besoin d'Architectury — c'est Tensura qui l'utilise pour son multiloader)
- Tensura apporte ses propres dépendances transitives (ManasCore, Architectury)

---

## 2. Architecture Globale

```
STAT Mod (NeoForge) ──hard dep──> Tensura Mod (NeoForge via Architectury)
                                        │
                                  ┌──────┴──────┐
                                  │  ManasCore   │
                                  └──────────────┘
```

### Flux de données

```
Tensura Mod                          STAT Mod
┌──────────────┐               ┌─────────────────────┐
│ TensuraPlayer │───API───>     │ PlayerDataBridge    │
│ Data          │   directe     │ (wrapper lisible)    │
└──────────────┘               └─────────┬───────────┘
                                         │
┌──────────────┐               ┌─────────┴───────────┐
│ TensuraSkill │───events──>   │ TensuraEventSubscriber│
│ Events       │               │ (met à jour cache)   │
└──────────────┘               └─────────┬───────────┘
                                         │
┌──────────────┐               ┌─────────┴───────────┐
│ TensuraEntity│───events──>   │ RaceEffectApplier    │
│ Events       │               │ (applique modifieurs) │
└──────────────┘               └─────────┬───────────┘
                                         │
                               ┌─────────┴───────────┐
                               │ SoulLevelCap         │
                               │ SkillPerkGate        │
                               │ (contraignent le     │
                               │  système STAT Mod)   │
                               └─────────────────────┘
```

---

## 3. Components

### 3.1 PlayerDataBridge

**Fichier :** `src/main/java/tong/statmod/integration/PlayerDataBridge.java`

Wrapper statique qui lit `TensuraPlayerData.get(player)` et expose des méthodes typées.

```java
public final class PlayerDataBridge {
    private PlayerDataBridge() {}

    public static int getSoulLevel(Player player) {
        var data = TensuraPlayerData.getInstance(player);
        return data != null ? data.getSoulLevel() : 0;
    }

    public static Race getRace(Player player) {
        var data = TensuraPlayerData.getInstance(player);
        return data != null ? data.getRace() : Race.HUMAN;
    }

    public static boolean hasSkill(Player player, String skillId) {
        var data = TensuraPlayerData.getInstance(player);
        return data != null && data.getSkills().stream()
                .anyMatch(s -> s.getSkillId().equals(skillId));
    }

    public static boolean hasExtraSkill(Player player, String skillId) {
        // Extra Skills sont stockés dans un registre séparé
        var manager = player.getData(ModAttachments.STATS);
        // → lookup dans le mapping skill → perk
    }
}
```

**Responsabilités :**
- Être le seul point d'accès à l'API Tensura
- Gérer les nulls (joueur non chargé)
- Cacher la complexité du modèle Tensura

### 3.2 TensuraEventSubscriber

**Fichier :** `src/main/java/tong/statmod/integration/TensuraEventSubscriber.java`

Écoute les events Tensura pour invalider les caches et propager les changements.

```java
@EventBusSubscriber(modid = STATMod.MODID)
public final class TensuraEventSubscriber {
    @SubscribeEvent
    public static void onSkillLearned(TensuraSkillEvents.SkillLearnedEvent event) {
        // → vérifier si ce skill débloque un perk
        // → si oui, unlock auto du perk
        // → sync client
    }

    @SubscribeEvent
    public static void onRaceChanged(TensuraEntityEvents.RaceChangedEvent event) {
        // → recalculer les modifieurs de race
        // → réappliquer les attributs
    }

    @SubscribeEvent
    public static void onSoulLevelUp(TensuraEntityEvents.SoulLevelUpEvent event) {
        // → le nouveau plafond permet peut-être de monter des stats
        // → play sound, message
    }
}
```

**Responsabilités :**
- Réagir aux changements d'état Tensura
- Propager vers les systèmes STAT Mod
- Déclencher les mises à jour de cache et les syncs réseau

### 3.3 SoulLevelCap

**Fichier :** `src/main/java/tong/statmod/integration/SoulLevelCap.java`

Remplace la config `MAX_STAT_LEVEL` par le niveau d'âme Tensura.

**Modifications :**
- `PlayerStatData.addXp()` → si `levels[index] >= soulLevel`, l'XP est refusée (ou stockée pour plus tard)
- `PerkManager.canUnlock()` → `perk.tier.requiredStatLevel` est vérifié contre `min(statLevel, soulLevel)`
- UI : afficher `(plafonné par âme niveau X)` dans le GUI

```java
public static int getEffectiveMax(PlayerStatData data, int soulLevel) {
    return Math.min(Config.MAX_STAT_LEVEL, soulLevel);
}
```

### 3.4 RaceEffectApplier

**Fichier :** `src/main/java/tong/statmod/integration/RaceEffectApplier.java`

**Effets par race :**
| Axe | Effet |
|---|---|
| **Modifieurs fixes** | Race → +X niveaux de base à certaines stats, -Y à d'autres |
| **Scaling XP** | Race → multiplicateur d'XP pour certaines stats (0.5x à 2.0x) |
| **Perks exclusifs** | Race → débloque des perks spécifiques gratuitement |

```java
public record RaceModifiers(
    int[] baseStatBoosts,      // +X à certaines stats
    double[] xpMultipliers,    // 1.0 par défaut
    int[] freePerkIds          // perks débloqués automatiquement
) {
    public static RaceModifiers forRace(Race race) {
        return switch (race) {
            case HUMAN -> new RaceModifiers(new int[23], new double[23], new int[0]);
            case DRAGON -> new RaceModifiers(
                new int[]{5, 0, 2, 0, 3, 2, 0, 0, 0, 0, 5, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0},
                new double[]{1.5, 1.0, 0.5, 0.5, 1.2, 1.0, 1.0, 1.5, 1.0, 1.5, 1.5, 1.0, 1.2, 1.0, 1.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
                new int[]{0, 24}  // BRUTE_CORE + RESIST_CORE gratuits
            );
            case SLIME -> new RaceModifiers(
                new int[]{0, 0, 0, 3, 2, 5, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 2},
                new double[]{0.8, 1.0, 1.0, 1.5, 1.2, 1.3, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.3, 1.0, 1.0, 1.0, 1.5, 1.2, 1.0, 1.0, 1.0, 1.0, 1.2},
                new int[]{18, 48}  // AGIL_CORE + SENSE_CORE gratuits
            );
            case WOLFMAN -> new RaceModifiers(
                new int[]{2, 2, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0},
                new double[]{1.2, 1.2, 1.3, 1.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.5, 1.3, 1.0, 1.0, 1.0, 1.0, 1.0},
                new int[]{6, 12, 18}  // BLADE_CORE + RAPID_CORE + AGIL_CORE
            );
            default -> RaceModifiers.NEUTRAL;
        };
    }
}
```

### 3.5 SkillPerkGate

**Fichier :** `src/main/java/tong/statmod/integration/SkillPerkGate.java`

Mapping des compétences Tensura → perks STAT Mod.

```java
// Map<skillId, perkId>
public static final Map<String, Integer> SKILL_TO_PERK = Map.ofEntries(
    Map.entry("tensura:sword_mastery_novice", 6),     // BLADE_CORE
    Map.entry("tensura:heavy_weapon_training", 0),     // BRUTE_CORE
    Map.entry("tensura:mana_sense", 48),               // SENSE_CORE
    Map.entry("tensura:iron_body", 24),                // RESIST_CORE
    ...
);
```

**Logique :** Dans `PerkManager.canUnlock()`, vérifier que le joueur possède la compétence Tensura requise.

### 3.6 PerkEffectHandler — Adaptations

Les perks déjà câblés (10/84) doivent interagir avec le système Tensura :
- Les perks qui donnent des dégâts supplémentaires → `LivingDamageEvent.Pre` existe déjà
- Les perks qui donnent des résistances → idem
- Les perks qui augmentent la vitesse/taux d'attaque → à faire via `StatAttributeHandler`
- Les perks exclusifs de race → débloqués automatiquement au changement de race

---

## 4. Modifications aux Fichiers Existants

| Fichier | Changement |
|---|---|
| `build.gradle` | Ajouter dépendance Tensura + JitPack repo |
| `STATMod.java` | Supprimer `isLoaded()` checks, initialiser integration |
| `storage/PlayerStatData` | Ajouter `soulLevel`, `raceId`, `modifierOverrides` |
| `progression/LevelUpHandler` | Tenir compte du plafond d'âme |
| `stats/StatEffectApplier` | Ajouter modifieurs de race au calcul |
| `stats/StatCommands` | Afficher le niveau d'âme, les infos de race |
| `config/Config.java` | Supprimer `MAX_STAT_LEVEL` (remplacé par âme) |
| `network/SyncHelper` | Synchroniser race + âme avec le client |

---

## 5. Nouveaux Fichiers

| Fichier | Rôle |
|---|---|
| `integration/PlayerDataBridge.java` | Wrapper API Tensura |
| `integration/TensuraEventSubscriber.java` | Écoute les events |
| `integration/SoulLevelCap.java` | Plafond de progression |
| `integration/RaceEffectApplier.java` | Applique les modifieurs |
| `integration/SkillPerkGate.java` | Mapping skill → perk |
| `integration/RaceModifiers.java` | Data record statique |

---

## 6. Paquetage Final

```
src/main/java/tong/statmod/
├── STATMod.java
├── integration/                    ← NOUVEAU
│   ├── PlayerDataBridge.java
│   ├── TensuraEventSubscriber.java
│   ├── SoulLevelCap.java
│   ├── RaceEffectApplier.java
│   ├── SkillPerkGate.java
│   └── RaceModifiers.java
├── storage/ (inchangé)
├── stats/ (modifié)
├── perks/ (modifié)
├── progression/ (modifié)
├── network/ (modifié)
├── client/ (modifié)
├── item/ (inchangé)
├── config/ (modifié)
├── mixin/ (inchangé)
└── sound/ (inchangé)
```

---

## 7. Ordre d'Implémentation

1. **Dépendance build** → `build.gradle`, repos, tests de compilation
2. **PlayerDataBridge** → wrapper lecture API Tensura
3. **SoulLevelCap** → contraindre `addXp()` et `canUnlock()`
4. **TensuraEventSubscriber** → écouter les events, propager
5. **RaceEffectApplier** → modifieurs de race
6. **SkillPerkGate** → verrouiller les perks derrière les skills
7. **Adaptation des handlers existants** → StatEffectApplier, LevelUpHandler
8. **Sync réseau** → race + âme vers client
9. **UI** → afficher les infos Tensura dans les écrans STAT Mod

---

## 8. Non-Goals (pour ce spec)

- Le système de subordonnés Tensura → futur
- Le système de territoire Tensura → futur
- L'artisanat Tensura (forging, cooking) → futur
- Les interactions avec d'autres mods → futur
- Les Ultimate Skills → futur

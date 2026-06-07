# STAT Mod × L2Hostility — Design Spec
**Date**: 2026-06-07
**Version**: 1.0
**Statut**: Approuvé — prêt pour implémentation

---

## 1. Résumé exécutif

Ce document spécifie l'intégration complète entre **STAT Mod** (23 stats joueur, 60+ skills Epic Fight) et **L2Hostility** (difficulté dynamique des mobs, traits, niveaux).

**Objectif central** : les mobs reçoivent les 23 stats STAT Mod, peuvent utiliser les skills Epic Fight, et interagissent de manière bidirectionnelle avec le système de difficulté L2Hostility.

**Approche** : L2Hostility est une **dépendance optionnelle forte**. STAT Mod fonctionne sans lui, mais si L2H est présent, il pilote entièrement les niveaux des mobs.

---

## 2. Périmètre — 5 sous-projets

| ID | Nom | Description |
|----|-----|-------------|
| SP-1 | Mob Stats System | Donner les 23 stats STAT à tous les mobs |
| SP-2 | Mob Skills Engine | Les mobs utilisent des skills Epic Fight selon leurs stats |
| SP-3 | L2H Bridge | L2H traits ↔ STAT stats (bidirectionnel) |
| SP-4 | Player × Mob Interactions | Stats joueur résistent aux traits L2H, XP scalé |
| SP-5 | STAT Mod Hardening | Améliorations inspirées de l'architecture L2H |

**Ordre d'implémentation** : SP-1 → SP-3 → SP-4 → SP-2 → SP-5

---

## 3. SP-1 : Mob Stats System

### 3.1 Concept

Chaque `Mob` reçoit une nouvelle capability `MobStats` stockant 23 niveaux de stats. Ces niveaux sont **statiques** (fixés au spawn) — les mobs ne gagnent pas d'XP.

### 3.2 Capability `MobStats`

```java
// tong.statmod.capability.MobStats
int[] statLevels[23]   // un niveau par StatType (0–100)
serializeNBT / deserializeNBT
sanitizeState()        // clamp 0–100
```

### 3.3 Calcul des niveaux — avec L2H présent

Source : `MobTraitCap.lv` (niveau L2H du mob)

```
statLevel[i] = clamp(l2h_level * scaleFactor * statWeight[i], 0, 100)

scaleFactor = MobStatConfig.SCALE_FACTOR  (défaut: 2.0)
statWeight  = poids par catégorie (voir tableau 3.4)
```

**Bonus supplémentaires par trait L2H actif :**

| Trait L2H | Stats boostées | Bonus |
|-----------|----------------|-------|
| Fiery | FIRE_AFFINITY | +30 |
| Regen | PHYSICAL_ENDURANCE | +25 |
| Reflect | PHYSICAL_RESISTANCE | +20 |
| Invisible | AGILITY, KEEN_SENSES | +20, +15 |
| Gravity | BRUTE_FORCE | +15 |
| Aura | INTIMIDATION | +25 |
| Shulker | AGILITY | +20 |
| Drain | ARCANE_POWER, MANA_POOL | +30, +20 |
| Adapting | PHYSICAL_RESISTANCE | +10 |
| Arena | INTIMIDATION | +30 |
| Corrosion | ALCHEMY | +10 |
| Growth | PHYSICAL_ENDURANCE, BRUTE_FORCE | +30, +20 |
| Reprint | WILLPOWER | +25 |
| Split | WILLPOWER, AGILITY | +20, +15 |
| KillerAura | ARCANE_POWER, INTIMIDATION | +40, +30 |
| Undying | WILLPOWER, PHYSICAL_ENDURANCE | +50, +30 |
| Dementor | MAGIC_RESISTANCE, WILLPOWER | +40, +35 |
| Dispell | ARCANE_POWER, ERUDITION | +35, +25 |
| Master | INTIMIDATION, WILLPOWER | +40, +30 |
| Ragnarok | BRUTE_FORCE, ARCANE_POWER | +40, +30 |

### 3.4 Poids par catégorie selon type de mob

| Catégorie mob | Combat | Magic | Survival | Mental |
|---------------|--------|-------|----------|--------|
| Melee (zombie, piglin) | 1.0 | 0.2 | 0.6 | 0.4 |
| Ranged (skeleton, pillager) | 0.7 | 0.4 | 0.7 | 0.3 |
| Magic (witch, blaze) | 0.3 | 1.0 | 0.4 | 0.5 |
| Boss (wither, dragon, elder guardian) | 1.0 | 1.0 | 0.8 | 1.0 |
| Undead (skeleton, phantom) | 0.8 | 0.5 | 0.3 | 0.6 |

### 3.5 Calcul des niveaux — sans L2H

Niveaux de base définis par datapack JSON dans `data/statmod/mob_stats/<namespace>/<entity>.json` :

```json
{
  "entity": "minecraft:zombie",
  "category": "melee",
  "base_levels": {
    "BRUTE_FORCE": 15,
    "PHYSICAL_ENDURANCE": 12,
    "AGILITY": 5,
    "PHYSICAL_RESISTANCE": 8,
    "TRACKING": 10
  }
}
```

Fichiers fournis par défaut pour tous les mobs vanilla.

### 3.6 Effets sur le gameplay mob

| Stat | Attribut Minecraft | Formule |
|------|--------------------|---------|
| BRUTE_FORCE | ATTACK_DAMAGE | +level/100 × 2.0 |
| PHYSICAL_ENDURANCE | MAX_HEALTH | +level × 0.5 |
| AGILITY | MOVEMENT_SPEED | +level/100 × 0.3 |
| RAPIDITE | MOVEMENT_SPEED | +level/100 × 0.2 (cumulatif) |
| PHYSICAL_RESISTANCE | damage taken | ×(1 - level/200) |
| TRACKING | FOLLOW_RANGE | +level × 0.4 |
| PRECISION | arrow accuracy | -spread × level/100 |
| ARCANE_POWER | magic damage | +level/100 × 1.5 |
| FIRE_AFFINITY | fire damage out / in | +out × level/200, -in × level/200 |
| WATER_AFFINITY | drowning immunity + water damage | resist level/200 |
| EARTH_AFFINITY | fall damage + terrain dmg | resist level/200 |
| AIR_AFFINITY | knockback + wind | resist level/200 |
| MAGIC_RESISTANCE | magic damage taken | ×(1 - level/200) |
| MANA_POOL | mana dispo pour skills | level × 2 |
| INTIMIDATION | aura slowness | rayon = level/20, amplifier = level/40 |
| WILLPOWER | knockback resist | level/100 |
| KEEN_SENSES | détecte invisible | seuil = 100 - level |

Stats sans effet mob : BLADE_TECHNIQUE, CASTING_SPEED, ERUDITION, FORGING, COOKING, ALCHEMY (réservées joueur ou flavor uniquement).

### 3.7 Fichiers à créer

```
src/main/java/tong/statmod/
├── capability/
│   ├── MobStats.java
│   └── MobStatsProvider.java
├── stats/
│   ├── MobStatCalculator.java
│   ├── MobStatEffectApplier.java
│   └── MobStatInitializer.java
├── integration/
│   └── L2HostilityMobSync.java
└── config/
    └── MobStatConfig.java

src/main/resources/data/statmod/mob_stats/
├── minecraft/zombie.json
├── minecraft/skeleton.json
├── minecraft/creeper.json
├── minecraft/spider.json
├── minecraft/enderman.json
├── minecraft/witch.json
├── minecraft/blaze.json
├── minecraft/wither_skeleton.json
├── minecraft/piglin.json
├── minecraft/pillager.json
├── minecraft/evoker.json
├── minecraft/wither.json
└── minecraft/elder_guardian.json
```

### 3.8 Fichiers à modifier

| Fichier | Modification |
|---------|-------------|
| `CapabilityHandler.java` | Attacher `MobStats` dans `AttachCapabilitiesEvent` si entity instanceof Mob |
| `Config.java` | Déléguer vers `MobStatConfig` |
| `ARCHITECTURE.md` | Mise à jour du graphe de dépendances |

### 3.9 Config

```toml
[mobStats]
enabled = true
scaleFactor = 2.0          # points de stat par niveau L2H
globalCap = 100            # niveau max par stat
combatStatWeight = 1.0
magicStatWeight = 0.7
survivalStatWeight = 0.5
mentalStatWeight = 0.5
```

---

## 4. SP-2 : Mob Skills Engine

### 4.1 Concept

Les mobs débloquent et utilisent des skills Epic Fight en fonction de leurs niveaux `MobStats`. Basé sur `SkillUnlockRegistry` existant mais avec une liste restreinte par catégorie de mob.

### 4.2 Pool de skills par catégorie

| Catégorie mob | Skills disponibles |
|---------------|--------------------|
| Melee | Weapon Passives (12), Stat Passives combat (7), HEAVY_STRIKE, BLADE_DANCE |
| Ranged | Weapon Passives ranged, Stat Passives precision, PRECISION_SHOT |
| Magic | Stat Passives magic (9), ENDURANCE_SURGE |
| Boss (tous) | Pool complet + 1 Identity skill (BLITZ_ASSAULT, SHADOW_STEP, ou STONE_SKIN) |
| Agile (spider, enderman) | SHADOW_STEP, Agility passives |

### 4.3 Condition de déclenchement

Un mob tente d'utiliser un skill actif si :
1. Il a le level requis dans la stat associée
2. Il a assez de mana (`MobStats.statLevels[MANA_POOL] * 2 >= skill.manaCost` — mana dérivé du niveau MANA_POOL, pas de champ séparé)
3. Le cooldown est expiré (ticks serveur)
4. Il est en combat actif (target != null)

### 4.4 Fichiers à créer

```
src/main/java/tong/statmod/
├── skills/
│   ├── MobSkillEngine.java
│   ├── MobSkillPool.java
│   └── MobSkillTickHandler.java
```

### 4.5 Fichiers à modifier

| Fichier | Modification |
|---------|-------------|
| `SkillUnlockRegistry.java` | Exposer méthode `getSkillsForMobCategory()` |
| `CapabilityHandler.java` | Init skills mob après `MobStats` |

---

## 5. SP-3 : L2H Bridge

### 5.1 L2H → STAT Mod

`L2HostilityMobSync.java` (guard `ModList.isLoaded("l2hostility")`) :

1. À l'event `EntityJoinLevelEvent` : lit `MobTraitCap.lv` + traits actifs
2. Calcule les 23 niveaux via formule SP-1 Section 3.3
3. Injecte dans `MobStats` capability
4. Appelle `MobStatEffectApplier.applyAllBonuses(mob)`

### 5.2 STAT Mod → L2H (feedback)

À l'event `PlayerLoggedInEvent` :
```java
// Si L2H présent, booster PlayerDifficulty avec le niveau STAT global
int statGlobalLevel = getAverageStatLevel(player);
PlayerDifficulty.HOLDER.get(player)
    .ifPresent(pd -> pd.difficulty.addBonus(statGlobalLevel / 10));
```

### 5.3 Détection de L2H (runtime)

```java
// Dans STATMod.commonSetup()
if (ModList.get().isLoaded("l2hostility")) {
    L2HostilityMobSync.register();
    LOGGER.info("[STAT Mod] L2Hostility detected — mob stat sync enabled");
}
```

---

## 6. SP-4 : Player × Mob Interactions

### 6.1 Résistances joueur vs traits L2H

Event `LivingHurtEvent` — si attaquant est un mob avec L2H trait actif :

| Trait L2H | Stat joueur | Formule réduction |
|-----------|-------------|-------------------|
| Fiery | FIRE_AFFINITY | ×(1 - fire_affinity/200) |
| Drain | ARCANE_POWER | vol réduit de arcane/200 |
| KillerAura | MAGIC_RESISTANCE | ×(1 - magic_res/200) |
| Corrosion | ALCHEMY | durabilité réduite de alchemy/200 |
| Dementor | WILLPOWER | résistance CC = willpower/100 |
| Gravity | PHYSICAL_RESISTANCE | ×(1 - phys_res/200) |
| Arena | WILLPOWER | résistance zone = willpower/150 |

### 6.2 XP STAT scalé par niveau L2H

Dans `CombatXPHandler.java` (modification) :

```java
float xpMultiplier = 1.0f;
if (ModList.get().isLoaded("l2hostility")) {
    int mobLevel = MobTraitCap.HOLDER.get(mob)
        .map(cap -> cap.lv).orElse(0);
    xpMultiplier = 1.0f + (mobLevel * Config.L2H_XP_SCALE.get());
    // défaut: +5% XP par niveau L2H (mob niveau 20 → ×2 XP)
}
cap.addXp(skillIndex, (int)(baseXp * xpMultiplier));
```

### 6.3 Config

```toml
[l2hIntegration]
xpScalePerLevel = 0.05   # +5% XP STAT par niveau L2H du mob
playerDifficultyFeedback = true
playerDifficultyScale = 0.1  # stat_global_level / 10 → l2h difficulty
```

---

## 7. SP-5 : STAT Mod Hardening (inspiré L2H)

### 7.1 Datapack support étendu

Objectif : configs par mob type sans recompile, comme `EntityConfig` de L2H.

- `data/statmod/mob_stats/` JSON par mob (voir SP-1 Section 3.5)
- `data/statmod/skill_pools/` JSON pour définir les pools de skills par catégorie mob
- Chargement via `JsonReloadListener` (comme L2H `WorldDifficultyConfig`)

### 7.2 API `IStatModPlugin` enrichie

Nouveaux points d'extension (inspirés de `LHKJSPlugin`) :

```java
interface IStatModPlugin {
    // Existant
    void registerStats(StatRegistry registry);
    void registerPerks(PerkRegistry registry);

    // NOUVEAU
    void registerMobStatOverrides(MobStatRegistry registry);
    // Permet aux addon mods de définir des niveaux de stats custom par mob type

    void registerL2HTraitMappings(TraitMappingRegistry registry);
    // Permet de mapper un trait L2H custom vers des stats STAT custom
}
```

### 7.3 MobStatCalculator extensible

Inspiré du `MobDifficultyCollector` de L2H :

```java
// Formules remplaçables via IStatModPlugin.registerMobStatOverrides()
// au lieu d'être hardcodées
MobStatCalculator.setFormula(StatType.BRUTE_FORCE,
    (level, mobType) -> level / 100.0 * 2.0);
```

### 7.4 Fichiers à créer

```
src/main/java/tong/statmod/
├── api/
│   ├── MobStatRegistry.java
│   └── TraitMappingRegistry.java
├── datagen/
│   └── MobStatDataProvider.java   ← génère les JSON vanilla au build
└── reload/
    └── MobStatReloadListener.java  ← reload JSON en jeu
```

---

## 8. Nouvelles dépendances (build.gradle)

```groovy
// L2Hostility — optionnel (compileOnly)
// Coordonnées maven exactes à récupérer sur CurseForge/Modrinth au moment de l'impl
compileOnly fg.deobf("curse.maven:l2hostility-<id>:<fileId>")
compileOnly fg.deobf("curse.maven:l2library-<id>:<fileId>")
```

Dans `mods.toml` :
```toml
[[dependencies.statmod]]
    modId="l2hostility"
    mandatory=false
    versionRange="[2.5.0,)"
    ordering="NONE"
    side="BOTH"
```

---

## 9. Plan d'implémentation (ordre)

```
Phase 0 — Correctifs critiques (bugs existants, avant toute nouvelle feature)
  FIX-1: StatPassiveSkill — implémenter les 16 stats inertes (magic, survival, crafting, mental)
  FIX-2: RandomEvents — brancher isBonusXpActive() dans CombatXPHandler + NonCombatXPHandler
  FIX-3: ManaTickHandler — appeler regenMana() dans le tick handler serveur
  FIX-4: ServerValidator — valider que stat index ∈ [0, STAT_COUNT) dans tous les packets entrants
  FIX-5: CapabilityHandler — corriger le cleanup PartyManager (appel unique, cache borné)
  FIX-6: MobScalingHandler — ajouter cap configurable + flag NBT invalidation au respawn
  FIX-7: BossLootHandler — extraire les rewards dans data/statmod/boss_rewards/*.json
  FIX-8: DiscordPresence — supprimer entièrement (code mort, trompeur)

Phase 1 — Fondation mobs
  SP-1a: MobStats capability (données + NBT)
  SP-1b: MobStatInitializer (niveaux sans L2H, JSON vanilla)
  SP-1c: MobStatEffectApplier (bonus attributs Minecraft)
  SP-1d: CapabilityHandler update (attacher MobStats aux Mob entities)

Phase 2 — Bridge L2H
  SP-3a: L2HostilityMobSync (lecture MobTraitCap → MobStats)
  SP-3b: Feedback STAT → PlayerDifficulty L2H
  SP-4a: Résistances joueur vs traits L2H
  SP-4b: XP scalé par niveau L2H mob

Phase 3 — Skills mobs
  SP-2a: MobSkillPool (pools par catégorie mob)
  SP-2b: MobSkillEngine (déclenchement + cooldowns game ticks)
  SP-2c: MobSkillTickHandler

Phase 4 — Hardening (inspiré L2H)
  SP-5a: Datapack support (JsonReloadListener pour mob_stats/ et skill_pools/)
  SP-5b: IStatModPlugin extensions (MobStatRegistry, TraitMappingRegistry)
  SP-5c: MobStatDataProvider (génération JSON vanilla au build)
  SP-5d: Network consolidation (fusionner packets redondants)
```

---

## 10. Contraintes techniques

- **Pas de `any` / types implicites** — tout typé explicitement
- **Game ticks** pour cooldowns skills mobs (jamais `System.currentTimeMillis()`)
- **Immutable snapshots** pour les pools de skills (`Set.copyOf`)
- **Guards L2H** via `ModList.get().isLoaded("l2hostility")` — jamais de cast direct sans guard
- **Server-side only** pour le calcul des stats mobs — sync client via packet si affichage nécessaire
- **Sanitation NBT** : `clamp(0, 100)` sur tous les niveaux au chargement

---

## 11. Audit — Bugs et stubs identifiés

*Issus de l'audit comparatif STAT Mod vs L2Hostility du 2026-06-07.*

### Bugs critiques (code cassé ou inerte)

| ID | Fichier | Problème | Gravité |
|----|---------|----------|---------|
| BUG-1 | `skills/StatPassiveSkill.java` | Switch sur 7/23 stats — 16 stats magic/survival/crafting/mental inertes | CRITIQUE |
| BUG-2 | `world/RandomEvents.java` | `isBonusXpActive()` jamais vérifié dans XP handlers — bonus XP sans effet | HAUTE |
| BUG-3 | `capability/ManaTickHandler.java` | `regenMana()` jamais appelé — mana ne régénère jamais | HAUTE |
| BUG-4 | `anticheat/ServerValidator.java` | Stat index non validé dans packets (crash si index hors bornes) | HAUTE |
| BUG-5 | `capability/CapabilityHandler.java` | `PartyManager.cleanup()` appelé plusieurs fois, caches UUID non bornés | MOYENNE |
| BUG-6 | `world/MobScalingHandler.java` | Aucun cap — scaling illimité à hauts niveaux, flag NBT non invalidé au respawn | MOYENNE |
| BUG-7 | `world/BossLootHandler.java` | Rewards hardcodés pour 3 bosses seulement — pas configurable | FAIBLE |
| BUG-8 | `discord/DiscordPresence.java` | 100% vide, juste un log — affiche "initialized" (faux) | COSMÉTIQUE |

### Stubs partiels (features déclarées mais incomplètes)

| ID | Fichier | Problème |
|----|---------|----------|
| STUB-1 | `integration/EpicFightCompat.java` | Si aucun slot Epic Fight libre → skill silencieusement perdu |
| STUB-2 | `network/NetworkHandler.java` | Risque double-envoi BatchSyncPacket + SyncAllStatsPacket simultanés |
| STUB-3 | `world/RandomEvents.java` | Pas de packet client pour notifier changement de bonus XP |

### Features absentes vs L2Hostility

| ID | Feature L2H | Valeur pour STAT Mod |
|----|-------------|----------------------|
| L2H-1 | Difficulté par chunk (ChunkDifficulty) | Zones hot-spot, exploration stratégique |
| L2H-2 | EntityConfig via datapack JSON | Configuration serveur sans recompile |
| L2H-3 | Système de traits mobs (20+ traits) | Rencontres uniques, variété de combat |
| L2H-4 | Tooltips niveau mob au survol | UX combat — joueur voit la menace |
| L2H-5 | KubeJS scripting plugin | Addons sans Java |
| L2H-6 | Difficulté adaptive joueur (kills → niveau) | Replayability, escalade progressive |

---

## 12. Critères de succès

### Phase 0 (correctifs)
- [ ] Un joueur avec `ARCANE_POWER = 80` reçoit l'effet du skill magique correspondant
- [ ] Le bonus XP RandomEvent multiplie bien l'XP reçu dans `CombatXPHandler`
- [ ] La mana régénère 1pt/seconde au repos
- [ ] Un packet avec stat index=-1 est rejeté sans crash
- [ ] Pas de double-entry dans `PartyManager` après reconnexions répétées
- [ ] Boss Dragon avec rewards configurés via JSON dans `data/statmod/boss_rewards/`

### Phase 1-2 (Mob Stats + L2H Bridge)
- [ ] Un zombie a `BRUTE_FORCE = 15` sans L2H
- [ ] Un zombie niveau 20 L2H a `BRUTE_FORCE = 40` (20 × 2.0 × 1.0)
- [ ] Un mob avec trait Fiery a `FIRE_AFFINITY = 30` en plus
- [ ] Joueur avec `FIRE_AFFINITY = 80` prend 40% de dégâts en moins d'un mob Fiery
- [ ] Tuer un mob niveau 20 L2H donne ×2 XP STAT
- [ ] STAT Mod compile et tourne sans L2H installé
- [ ] STAT Mod compile et tourne avec L2H installé

### Phase 3 (Mob Skills)
- [ ] Un boss spawne avec un Identity skill si level ≥ 60
- [ ] Un zombie niveau 30 utilise HEAVY_STRIKE si mana suffisante

### Phase 4 (Hardening)
- [ ] JSON vanilla mob_stats se rechargent avec `/reload`
- [ ] Un addon mod peut override les stats d'un zombie via `IStatModPlugin`

---

*Spec mise à jour le 2026-06-07 après audit comparatif. Prochaine étape : plan d'implémentation (writing-plans).*

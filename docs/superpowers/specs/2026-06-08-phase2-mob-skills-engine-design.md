# Phase 2 — Mob Skills Engine: Design

**Date:** 2026-06-08
**Status:** Design (not implemented)
**Prerequisite:** Phase 1 (MobStats capability) complete

---

## Goal

Donner aux mobs des **actions actives** qui scalent avec leurs `MobStats`. Phase 1 a juste modifié leurs attributs passifs (damage, health, follow range). Phase 2 ajoute des **capacités** (charges, sorts, AOE, débuffs) déclenchées par les mobs en combat, dont la fréquence et la puissance dépendent de leurs stats.

## Vision gameplay

Au lieu d'un zombie générique plus solide à haut niveau, on a un zombie qui *charge* le joueur quand il a `BRUTE_FORCE ≥ 30`, une sorcière qui *maudit* à distance avec `ARCANE_POWER ≥ 45`, un Blaze qui *pleut du feu* avec `FIRE_AFFINITY ≥ 40`. Chaque mob "signature" devient reconnaissable et lisible.

## Architecture en deux couches

### Couche 1 — Engine extensible

Un système de **registre de skills** avec interface commune, manager par-mob pour cooldowns/mana, tick handler qui évalue et déclenche, et reload listener JSON pour configurer quel mob accède à quels skills.

### Couche 2 — Contenu hardcoded (8 skills livrés)

Skills écrits en Java avec effets ciselés. Leur seule particularité d'implémentation est qu'ils sont auto-enregistrés au boot, comme un contenu "officiel". Les mods/datapacks peuvent en ajouter sans rien recompiler.

## Composants

### `MobSkill` (interface)

Une action active qu'un mob peut tenter. Identité, contraintes d'exécution, effet, coûts.

```java
public interface MobSkill {
    ResourceLocation id();

    /** Stat principale + niveau minimum requis. */
    StatType requiredStat();
    int requiredLevel();

    /** Cooldown de base en ticks. Multiplié par config. */
    int baseCooldownTicks();

    /** Mana coûtée au mob. 0 si non utilisé. */
    int manaCost();

    /** Distance max au target (blocs). Le tick handler skip si hors portée. */
    double maxRange();

    /** Garde finale : peut-on l'exécuter maintenant ? */
    boolean canExecute(Mob mob, LivingEntity target, MobStats stats);

    /** Effet. Appelé côté serveur uniquement. */
    void execute(Mob mob, LivingEntity target, MobStats stats);
}
```

### `MobSkillRegistry`

Singleton statique. Skills enregistrés au `FMLCommonSetupEvent`. Lookup par `ResourceLocation`. Source de vérité unique pour le contenu.

### `MobSkillState` (Forge capability)

État runtime par mob :
- `Map<ResourceLocation, Long>` → cooldown end tick par skill
- `int currentMana`
- `int globalCooldownEndTick` (empêche un mob de spammer plusieurs skills d'affilée)

Sérialisé en NBT pour persister à travers chunk unload/reload.

### `MobSkillTickHandler`

`@SubscribeEvent ServerTickEvent` avec rate-limit à toutes les 10 ticks (0.5 s) :
1. Récupère tous les mobs chargés (parcourir `ServerLevel.getAllEntities`).
2. Pour chaque mob avec `MobStats` :
   - Régénération mana : `+1` par appel (toutes les 0.5 s).
   - Décrément cooldowns expirés (on les laisse expirer naturellement, lookup à la lecture).
   - Si pas en GCD et a un `target` → liste les skills disponibles via JSON loadout.
   - Filtre castables : `canExecute` + cooldown OK + mana ≥ coût + distance ≤ range + niveau stat ≥ requis.
   - Sélection : weighted random parmi les castables (poids dans le loadout JSON).
   - Cast : `skill.execute(...)`, met le cooldown, déduit mana, set GCD à 1 s.

### `MobSkillReloadListener`

`SimpleJsonResourceReloadListener("mob_skills")`. Charge `data/statmod/mob_skills/<ns>/<mob>.json` :
```json
{
  "entity_type": "minecraft:zombie",
  "global_cooldown_ticks": 60,
  "skills": [
    { "id": "statmod:charge", "weight": 100 },
    { "id": "statmod:battle_cry", "weight": 30 }
  ]
}
```

Map `entity_type → MobSkillLoadout`. Lookup au tick handler par `ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())`.

### `L2HostilityMobSkillSync` (optionnel)

Si L2Hostility chargé : certaines traits L2H **ajoutent** des skills au loadout du mob.

| Trait L2H | Skill ajouté |
|-----------|-------------|
| `aura` | `battle_cry` |
| `killer_aura` | `magic_missile` |
| `fiery` | `fireball` |
| `gravity` | `charge` |
| `dispell` | `curse` |
| `master` | tous les ci-dessus |

Implémenté comme un transformateur de loadout au moment du cast (lookup dynamique, pas de mutation du JSON loadout).

## Les 8 skills hardcoded

### 1. `statmod:charge` (Combat — BRUTE_FORCE ≥ 30)

Le mob sprint vers le joueur à `MOVEMENT_SPEED × 2.0` pendant 3 s. Si collision avec joueur : `BRUTE_FORCE × 0.05` dégâts bonus + knockback 2.

- Mana : 8 — Cooldown : 200 ticks (10 s) — Range : 12 blocs
- Mobs typiques : zombie, piglin, wither_skeleton, ravager

### 2. `statmod:whirlwind` (Combat — BLADE_TECHNIQUE ≥ 40)

AOE 360° autour du mob. Tous les `Player`/alliés du joueur dans 4 blocs prennent `mob.getAttribute(ATTACK_DAMAGE) × 1.5` en dégâts physiques.

- Mana : 12 — Cooldown : 240 ticks (12 s) — Range : 4 blocs (touche)
- Mobs typiques : wither_skeleton, vindicator

### 3. `statmod:lunge` (Combat — RAPIDITE ≥ 35)

Le mob saute/dash 6 blocs vers le target. Brise la line of sight, repositionne. Pas de dégâts directs (utilitaire de positionnement).

- Mana : 5 — Cooldown : 100 ticks (5 s) — Range : 10 blocs
- Mobs typiques : spider, pillager, enderman

### 4. `statmod:fireball` (Magic — FIRE_AFFINITY ≥ 40)

Le mob crache un petit `SmallFireball` vers le target. Dégâts : `FIRE_AFFINITY × 0.08` magic + 3 s feu.

- Mana : 10 — Cooldown : 150 ticks (7.5 s) — Range : 20 blocs
- Mobs typiques : blaze, wither, ghast

### 5. `statmod:curse` (Magic — ARCANE_POWER ≥ 45)

Le mob lance une `ThrownPotion` custom avec splash AOE 3 blocs : WEAKNESS II + SLOWNESS II pendant 8 s.

- Mana : 15 — Cooldown : 300 ticks (15 s) — Range : 16 blocs
- Mobs typiques : witch, evoker

### 6. `statmod:magic_missile` (Magic — CASTING_SPEED ≥ 30)

Projectile magique auto-homing (tracking lent). Dégâts : `CASTING_SPEED × 0.06` magic, ignore l'armure.

- Mana : 6 — Cooldown : 80 ticks (4 s) — Range : 24 blocs
- Mobs typiques : enderman, evoker, vex (modded mage mobs)

### 7. `statmod:battle_cry` (Mental — INTIMIDATION ≥ 40)

Burst sonore + particules `ANGRY_VILLAGER`. Tous les `Player` dans 8 blocs : MINING_FATIGUE I + WEAKNESS I pendant 6 s.

- Mana : 8 — Cooldown : 400 ticks (20 s) — Range : 8 blocs (zone propre)
- Mobs typiques : ravager, elder_guardian, wither

### 8. `statmod:reflect` (Survie — PHYSICAL_RESISTANCE ≥ 50)

**Skill réactif** (s'enregistre via `LivingHurtEvent`) : quand le mob est touché, renvoie 30 % des dégâts à l'attaquant si ce dernier est `Player` ou `Mob` non-allié. Pas de mana (gratuit), GCD interne 2 s.

- Mobs typiques : iron_golem, wither_skeleton, ravager
- Note d'implémentation : ce skill est dans la registry mais n'est pas piloté par `MobSkillTickHandler` — il a son propre `@SubscribeEvent` qui consulte le loadout du mob frappé.

## Flux de données

```
ServerTickEvent (every 10 ticks)
  ↓
MobSkillTickHandler.onServerTick()
  ↓
for each (Mob, MobStats, MobSkillState) in level:
  ↓
  regenMana(state)
  if (state.globalCooldown > now) continue
  target = mob.getTarget()
  if (target == null) continue
  ↓
  loadout = MobSkillReloadListener.get(entityType)
  if (l2hLoaded) loadout = L2HostilityMobSkillSync.augment(loadout, mob)
  ↓
  castable = filter(loadout, mob, target, stats, state)
  if (castable.empty) continue
  ↓
  chosen = weightedRandom(castable)
  chosen.execute(mob, target, stats)
  state.cooldowns[chosen.id] = now + chosen.cooldown
  state.mana -= chosen.manaCost
  state.globalCooldown = now + 20  // 1s GCD
```

## Erreurs et edge cases

- **Mob meurt pendant `execute`** : Tous les skills doivent vérifier `mob.isAlive()` en début d'effet ; les projectiles vérifient au moment du spawn.
- **Target invalide** (mort, déchargé, autre dimension) : `canExecute` doit retourner `false`. Le tick handler ne retente pas.
- **JSON malformé** : log error, mob garde un loadout vide (pas de cast). Pas de crash.
- **L2H absent** : `L2HostilityMobSkillSync` n'est pas enregistré dans `commonSetup`, pas de référence transitive.
- **Hot reload** : reload du loadout JSON via `/reload` réinitialise les map d'options mais ne touche pas aux cooldowns en cours (état runtime préservé).
- **Performance** : si > 500 mobs chargés sur un serveur, le tick handler peut consommer du CPU. Optimisation : skip les mobs sans target ; cache la lookup `getKey(mob.getType())` par MobStats.

## Tests

### Unit (sans Minecraft runtime)

- `MobSkillRegistry` : register puis get retourne le skill
- `MobSkillRegistry` : register du même ID écrase et log warn
- `MobSkillRegistry` : get d'un ID inconnu retourne null
- `MobSkillState` : NBT round-trip préserve cooldowns + mana
- `MobSkillState.sanitize` : mana négative → 0, cooldown < 0 → 0
- `MobSkillLoadout` : parsing JSON valide + weight default 100 si manquant
- `MobSkillLoadout` : parsing JSON avec skill ID inconnu → skip avec warn

### Integration (Minecraft test framework, optionnel)

- Spawn un mob avec `BRUTE_FORCE = 50` et le `charge` loadout, target = ServerPlayer fake, force un tick → vérifier que `mob.getDeltaMovement()` est non-nul (le mob a "chargé")
- Spawn un mob avec `FIRE_AFFINITY = 60` et le `fireball` loadout → vérifier qu'un `SmallFireball` est spawné dans le level

### Manuel en jeu

- Commande de debug `/statmod debug spawn_mob_with_skill <mob> <skill>` pour créer un mob avec un loadout custom et le forcer à caster (utile pour itérer sur les effets)

## Configuration runtime

Nouveau bloc dans `Config.java` :

```java
public static final ForgeConfigSpec.BooleanValue MOB_SKILLS_ENABLED = BUILDER
    .comment("Enable Phase 2 mob skill engine. False = mobs use only Phase 1 stat bonuses.")
    .define("mobSkillsEnabled", true);

public static final ForgeConfigSpec.DoubleValue MOB_SKILL_COOLDOWN_MULT = BUILDER
    .comment("Global cooldown multiplier on all mob skills (lower = mobs cast more often)")
    .defineInRange("mobSkillCooldownMult", 1.0, 0.1, 5.0);

public static final ForgeConfigSpec.IntValue MOB_SKILL_TICK_INTERVAL = BUILDER
    .comment("How often the mob skill tick handler runs, in ticks (20 = once per second)")
    .defineInRange("mobSkillTickInterval", 10, 1, 200);
```

## Fichiers à créer

| Catégorie | Fichier | Rôle |
|-----------|---------|------|
| Engine | `combat/skills/MobSkill.java` | Interface |
| Engine | `combat/skills/MobSkillRegistry.java` | Static registry |
| Engine | `combat/skills/MobSkillLoadout.java` | Record `(entityType, gcd, List<Entry>)` |
| Engine | `combat/MobSkillTickHandler.java` | Server tick driver |
| Capability | `capability/MobSkillState.java` | Per-mob cooldowns + mana |
| Capability | `capability/MobSkillStateProvider.java` | Forge provider |
| Reload | `reload/MobSkillReloadListener.java` | JSON loader |
| Skills | `combat/skills/impl/ChargeSkill.java` | #1 |
| Skills | `combat/skills/impl/WhirlwindSkill.java` | #2 |
| Skills | `combat/skills/impl/LungeSkill.java` | #3 |
| Skills | `combat/skills/impl/FireballSkill.java` | #4 |
| Skills | `combat/skills/impl/CurseSkill.java` | #5 |
| Skills | `combat/skills/impl/MagicMissileSkill.java` | #6 |
| Skills | `combat/skills/impl/BattleCrySkill.java` | #7 |
| Skills | `combat/skills/impl/ReflectSkill.java` | #8 (réactif) |
| Integration | `integration/L2HostilityMobSkillSync.java` | Bridge optionnel |
| Data | `data/statmod/mob_skills/minecraft/zombie.json` | Loadouts vanilla |
| Data | (...) wither_skeleton, witch, blaze, evoker, spider, ravager, elder_guardian | |
| Tests | `MobSkillRegistryTest.java`, `MobSkillStateTest.java`, `MobSkillLoadoutTest.java` | Unit |

**Fichiers à modifier :** `CapabilityHandler.java` (attach MobSkillState aux mobs), `STATMod.java` (register listener + registry init + L2H bridge), `Config.java` (3 entries), `ARCHITECTURE.md`.

## Critères de succès

1. ✅ Sans L2Hostility : un zombie de niveau player ≥ 30 charge le joueur via `Charge`. Visuel : sprint, knockback à l'impact.
2. ✅ Sans L2Hostility : une witch lance régulièrement `Curse`, le joueur prend WEAKNESS + SLOWNESS.
3. ✅ Sans L2Hostility : un Blaze tire des `Fireball` plus fréquemment qu'en vanilla (skill stat-driven).
4. ✅ Avec L2Hostility : un zombie L2H avec trait `aura` peut caster `Battle Cry` même si son loadout JSON ne le contient pas (augmentation L2H).
5. ✅ Désactivation via config : `mobSkillsEnabled=false` → aucun cast, fallback Phase 1 only.
6. ✅ Build sans L2H : `BUILD SUCCESSFUL`, aucune référence transitive aux classes L2H.
7. ✅ Performance : 500 mobs chargés, tick handler < 5 ms par cycle (benchmark via Spark ou JFR).
8. ✅ Tests unitaires : ≥ 8 passent (registry, state, loadout).

## Décisions clés et rationale

| Décision | Choix | Pourquoi |
|----------|-------|----------|
| AI Goal vs tick handler | Tick handler | Plus simple, debuggable. Goals peuvent être ajoutés plus tard pour mobs custom. |
| Mana mob persistante | NBT capability | Permet à un mob "fatigué" de revenir plus tard ; cohérent avec mana joueur. |
| GCD vs cooldown par skill | Les deux | GCD empêche le spam visuel, cooldowns donnent du rythme par skill. |
| Hot reload state | Reset loadouts, garder cooldowns | Moins surprenant pour le joueur, le mob ne perd pas son rythme. |
| Skill packagés vs JSON | Skills en Java | Effets complexes (projectiles, particules) sont mal exprimés en JSON. JSON = loadout uniquement. |
| Réactif (Reflect) vs actif | Cas spécial à part | Reflect n'a pas de target propre ni de coût ; intégré au registry pour cohérence mais flux séparé. |

## Risques

| Risque | Mitigation |
|--------|-----------|
| Le tick handler consomme trop sur grands serveurs | Config interval ajustable + skip mobs sans target |
| Les skills ressemblent trop entre eux | Particules + sons distincts pour chaque, validation visuelle obligatoire |
| Bug de spawn projectile crash le serveur | Try/catch dans `execute`, log error et skip |
| Mod incompatible (autre mod modifie target AI) | Tester avec L2H, Vampirism, Mowzie's Mobs avant ship |

## Hors scope (reporté à Phase 3)

- Skills basés sur stats joueur **vs** mobs (PvE asymétrique) — pas pour l'instant
- UI de visualisation des skills castés (telegraph / cast bar) — Phase 3
- Adaptive difficulty (le mob apprend à esquiver les skills joueur) — Phase 4
- Animations Epic Fight pour mobs — bloqué tant qu'Epic Fight n'expose pas l'API pour mobs custom

---

## Self-review (2026-06-08)

- [x] Aucun placeholder, TODO, ou TBD
- [x] Architecture cohérente : engine + content + state + driver = 4 responsabilités claires
- [x] Pas de contradiction interne (Reflect bien marqué comme cas spécial)
- [x] Ambiguïtés résolues : "skill réactif" défini, "GCD vs cooldown" clarifié, "loadout JSON vs Java" tranché
- [x] Scope appropriée pour un plan d'implémentation unique (~14-16 fichiers, raisonnable)
- [x] Critères de succès vérifiables en jeu et automatisés

# STAT MOD — CLAUDE.md

> Ce fichier est lu automatiquement par Claude à chaque session dans la branche `neoforge-1.21.1`.
> Dernière mise à jour gouvernée : 2026-07-03 par Onivo Studio — mission `M6` (`STAT-DEC-TRIAL-DUNGEON`). Précédentes : `M5` (`STAT-DEC-OVERGEARED-EXPANSION`), `M3` (`STAT-DEC-002`, `STAT-DEC-003`, `STAT-DEC-005`).

---

## 🎯 Objectif Global

STAT MOD est un **système de stats et de progression** sur NeoForge 1.21.1, conçu pour servir d'**autorité de progression** dans une stack multi-mods RPG. Il définit l'identité (race, stats, perks, arbre magique unifié) et délègue l'exécution runtime aux mods compagnons via des bridges évènementiels.

**Roadmap :**
1. ✅ MVP : Stats (22) + XP/Leveling + Perks (84) sur NeoForge 1.21.1 standalone
2. ✅ Intégration Tensura Reincarnated (race, soul level, skill gates)
3. ✅ Économie magique unifiée : monnaie unique `magicPoints` + 3 gates de stats par nœud (`ARCANE_POWER` + `ERUDITION` + tertiaire selon `SpellRole`) — voir `STAT-DEC-MAGIC-UNIFIED-ECONOMY`
4. 🟡 Phase 1 Magic : Iron's Spellbooks unified magic tree — **les 9 écoles sont structurellement actives** (catalog complet, 249 nœuds tagués `SpellRole`). Focus tuning Fire ; les autres écoles restent jouables sans aucun gate caché. Voir `STAT-DEC-PHASE-GATING` pour le rationale.
5. 🟡 **Mission M5 — Overgeared Universal Forge** (en cours) : extension structurelle de l'intégration Overgeared pour forger toutes les armes du modpack (~770 cibles) depuis tous les métaux (14 nouveaux `heated_*` + 84 `rough_*` intermediaires + grips + blueprints), dual-gate `FORGING` + `ERUDITION` + `ARCANE_POWER` pour les métaux magiques. Voir `STAT-DEC-OVERGEARED-EXPANSION` et `docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md`. **Bloqué** par compile cassé (refacto `MagicNode.role()` → `condition()` inachevée).
6. 🟡 **Mission M6 — Trial Dungeon** (en cours, 2026-07-03) : dimension `statmod:trial_dungeon` — donjon procédural en grille XZ horizontale avec autels de boss, roster prédéfini SLU (38 boss), HUD scoreboard droite, loot rune shards. **BUILD OK** mais **bug critique** : le `DungeonBossHandler` ne détecte pas les kills de boss → l'étage suivant reste bloqué. Voir §Trial Dungeon.
7. ⚪ Phase 2 Magic : tuning fin par école + ParCool intégrations magiques
8. ⚪ Release publique gouvernée

---

## 📦 Stack Technique

| Stack | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.133+ |
| JDK | 21 |
| Mappings | Official Mojang |
| Build | NeoGradle 7.x (`net.neoforged.gradle.userdev`) |
| Tests | JUnit Jupiter 5.10 |

**Absolument PAS :**
- ❌ ForgeGradle / Parchment Librarian
- ❌ Forge Capabilities (→ NeoForge Attachments)
- ❌ Forge SimpleChannel (→ NeoForge CustomPacketPayload)

> *(L'interdiction initiale d'Epic Fight a été levée par la décision `STAT-DEC-002` du 2026-06-21. Epic Fight est désormais une intégration **expérimentale optionnelle** — voir §Intégrations multi-mods.)*

---

## 🔌 Intégrations multi-mods

STAT MOD est l'**autorité d'identité** ; les autres mods sont des **surfaces runtime**. Communication via bridges évènementiels (ex : `IronSpellEventBridge`).

| Mod | Statut | Package | Décision |
|---|---|---|---|
| Tensura Reincarnated | **hard dependency** | `integration/tensura/` | base de la roadmap |
| Iron's Spellbooks | **active** (Phase 1) | `integration/ironspells/` | spec 2026-06-21 |
| Puffish Skills | **active** (UI miroir) | `integration/puffish/` | spec 2026-06-17 |
| Epic Fight | **experimental optional** | `integration/epicfight/` | `STAT-DEC-002` |
| ParCool | **optional** | `integration/parcool/` | master plan §3 |
| Overgeared | **optional** | `integration/overgeared/` | master plan §6 |
| Elementals | **removed** | (supprimé) | `STAT-DEC-003`, `STAT-PM-001` |

> Toute nouvelle intégration doit déclarer un bloc **Exit Conditions** dans son design spec (politique issue de `STAT-PM-001`).

---

## 📁 Architecture du Code (État Actuel)

Le port Forge 1.20.1 → NeoForge 1.21.1 est **terminé**. L'audit `M1` du 2026-06-21 n'a trouvé aucun résidu `net.minecraftforge`, `MinecraftForge.EVENT_BUS`, `FMLJavaModLoadingContext`, `SimpleChannel`, ni `Capability.` dans `src/`.

Packages présents et actifs sous `src/main/java/tong/statmod/` :

```
├── STATMod.java                  ← @Mod entry point NeoForge
├── storage/                      ← AttachmentType + PlayerStatData (arcane, school, race, learnedSpells, dungeon state)
├── stats/                        ← 23 stats + StatFamily
├── progression/                  ← XP combat / non-combat / level-up
├── perks/                        ← 84 perks + tiers + effets
├── magic/                        ← Catalog, MagicNode, MagicBranch, MagicRace, services
├── dungeon/                      ← M6 : Trial Dungeon (dimension, portal, islands, bosses, loot)
├── network/                      ← CustomPacketPayload (sync, unlock, magic)
├── client/                       ← caches + GUI (PerkScreen, cosmetic, magic mirror, dungeon HUD)
├── integration/                  ← tensura, ironspells, puffish, epicfight, parcool, overgeared
├── item/, mixin/, sound/, config/, api/, command/
```

Dossiers résiduels vides à supprimer (mission `M2`) : `integration/elementals/`.

---

## 🧠 Règles de Code

### Typage strict
- ❌ **Pas de `Object`** sans cast typé
- ❌ **Pas de `List` brute** — toujours `List<T>`
- ❌ **Pas de suppression de warnings** sans justification
- ✅ Types explicites partout

### NeoForge API (vs Forge)
```
Forge                          → NeoForge
──────────────────────────────────────────────────
net.minecraftforge             → net.neoforged.neoforge
MinecraftForge.EVENT_BUS       → NeoForge.EVENT_BUS
@Mod("modid")                  → @Mod(STATMod.MODID)
Capability + Provider          → AttachmentType<T>
SimpleChannel                  → CustomPacketPayload
PoseStack                      → GuiGraphics (avec pose())
ResourceLocation(ns, path)     → ResourceLocation.fromNamespaceAndPath(ns, path)
DeferredRegister.create(..., "modid") → DeferredRegister.create(Registry, MODID)
Mod.EventBusSubscriber         → @EventBusSubscriber
FMLJavaModLoadingContext       → @Mod constructor param (IEventBus modBus)
```

### Conventions
- Noms variables : anglais `camelCase`
- Classes : `PascalCase`
- Commentaires métier : français
- Commentaires techniques : anglais
- Indentation : 4 espaces
- `@Override` toujours présent
- `LOGGER` = `LoggerFactory.getLogger(STATMod.class)` (SLF4J, pas Log4J)

### Server vs Client (côté Minecraft)
- Toute logique de jeu → serveur uniquement (`!player.level().isClientSide`)
- Client → uniquement rendu, cache, input
- Network → payloads sérialisés, pas d'objets Minecraft directs

---

## 🛤️ Specs & Plans — Taxonomie

Décision `STAT-DEC-005` du 2026-06-21 :

- **`docs/superpowers/specs/`** — **canonique** pour les specs design
- **`docs/superpowers/plans/`** — **canonique** pour les plans d'implémentation
- **`.opencode/plans/`** — **déprécié** ; conservé en lecture pour contexte historique. Aucune création de fichier nouveau. Les plans historiques utiles sont forkés dans `docs/superpowers/plans/` avec front-matter `migrated-from`.

**Specs actifs notables :**
- `docs/superpowers/specs/2026-06-18-stat-identity-design.md` — identité des stats
- `docs/superpowers/specs/2026-06-17-puffish-perk-ui-design.md` — UI miroir Puffish
- `docs/superpowers/specs/2026-06-21-irons-spellbooks-unified-magic-tree-design.md` — arbre magique unifié

**Plans actifs notables :**
- `docs/superpowers/plans/2026-06-21-irons-spellbooks-unified-magic-tree.md` — **mission `M4` en cours**
- `docs/superpowers/plans/2026-06-18-stat-family-perk-foundation.md` — foundation identity

**Plan-cadre historique (non rejoué) :**
- `.opencode/plans/2026-06-16-multi-mod-integration-master-plan.md` — 75 points, référence intégrations

---

## 🔧 Commandes

```bash
# Build (dans la branche neoforge-1.21.1)
./gradlew build

# Run client
./gradlew runClient

# Tests
./gradlew test

# Build complet avec tests
./gradlew check
```

---

## 🎮 Intégration Tensura Reincarnated

Dépendance hard. Présent sous `integration/tensura/` :
- `PlayerDataBridge`, `RaceData`, `RaceEffectApplier`, `RaceModifier`, `RaceModifierRegistry`
- `SkillPerkGate`, `SoulLevelSyncHandler`, `TensuraEventSubscriber`
- `TensuraRaceHandler`, `TensuraSpellTaxonomy`, `TensuraSpellProfile`

**Pistes implémentées ou en cours :**
- Races Tensura → modifieurs de stats de base via `RaceModifierRegistry`
- Niveau d'âme Tensura → niveau global STAT MOD (`SoulLevelSyncHandler`)
- Compétences Tensura → gates de perks via `SkillPerkGate`
- Magie Tensura → liaison avec `MagicBranch` / `MagicRace`

---

## ❗ Points d'Attention

1. **NeoForge 1.21.1 utilise JDK 21** — pas JDK 17
2. **Mappings Mojang officiel** — pas Parchment
3. **Mixins** : `statmod.mixins.json` + déclaration dans `neoforge.mods.toml`
4. **Payload réseau** : `StreamCodec` + `CustomPacketPayload`
5. **GuiGraphics** remplace `PoseStack` direct
6. **ResourceLocation** : `fromNamespaceAndPath()`
7. **pack_format = 34** pour 1.21
8. **Registries** : `DeferredRegister<AttachmentType<?>>` via `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`

---

## 🏰 Trial Dungeon — Mission M6 (2026-07-03)

### État global

**BUILD OK, boucle MVP fermée.** Le bug `DungeonBossHandler` est **fixé** (2026-07-03) : heuristique robuste — sur un étage boss (multiple de 10), tout mob tué par le joueur unlock l'étage suivant (indépendant du tag et des mods installés, idempotent). Le `DungeonSpawnGuard` (double couche `FinalizeSpawnEvent` + `EntityJoinLevelEvent` avec marker NBT `statmod_dungeon_authorized`) bloque tous les spawns non-autorisés, y compris les spawns custom SLU.

**Island Redesign shippé** (spec + plan `2026-07-03-trial-dungeon-island-redesign`) : silhouettes organiques déterministes par étage (`IslandShaper`, bruit harmonique seedé), underside conique rocheux, spawn pad 3×3 dégagé, dais boss surélevé décentré (altar plus jamais sur le point de spawn), vault treasure à piliers, décor par tier (mousse EARLY → améthyste ABYSS), exits calculés sur la silhouette réelle. `/statdungeon regen <floor>` efface et régénère une île (même seed → même île).

### Fichiers clés

| Fichier | Rôle |
|---------|------|
| `dungeon/DungeonDimensions.java` | ResourceKeys pour `statmod:trial_dungeon` |
| `dungeon/DungeonBlocks.java` | DeferredRegister : `dungeon_portal`, `return_beacon`, `next_floor_teleporter`, `boss_altar` |
| `dungeon/DungeonPortalBlock.java` | Bloc d'entrée : right-click → tp donjon + particules PORTAL |
| `dungeon/DungeonTeleportHandler.java` | TP serveur : `enterFloor()`, `returnToOverworld()`, `floorAtPos()`, `floorSpawnPos()` |
| `dungeon/IslandGenerator.java` | Génération procédurale des îles (combat/treasure/boss) : silhouette organique, underside, pad, dais, vault, décor |
| `dungeon/IslandShaper.java` | Géométrie pure seedée (silhouette + profondeur underside) — testable sans Bootstrap |
| `dungeon/FloorPalette.java` | Tier EARLY/MID/LATE/ABYSS → base/accent/light/underside/decorPrimary/decorSecondary |
| `dungeon/DungeonSpawnGuard.java` | Anti-spawn double couche (FinalizeSpawnEvent + EntityJoinLevelEvent + marker NBT) |
| `dungeon/DungeonBossAltarBlock.java` | Bloc autel activable par le joueur pour spawner les boss du roster |
| `dungeon/DungeonBossRoster.java` | Roster prédéfini de 30 étages boss (floor → boss SLU/Vanilla) |
| `dungeon/DungeonBossHandler.java` | Handler `LivingDeathEvent` : sur boss floor, tout kill joueur → +stats + unlock étage suivant (fixé 2026-07-03) |
| `dungeon/DungeonXpMultiplier.java` | Multiplicateur d'XP par étage |
| `dungeon/DungeonNextFloorTeleporterBlock.java` | Bloc téléporteur vers étage suivant (si débloqué) |
| `dungeon/DungeonReturnBeaconBlock.java` | Bloc retour vers l'overworld |
| `dungeon/DungeonCommands.java` | `/statdungeon tp\|unlock\|info\|reset\|regen` (floors 1-10000) |
| `dungeon/DungeonRespawnHandler.java` | Mort dans le donjon → respawn étage 1 auto |
| `client/DungeonHudOverlay.java` | HUD scoreboard droite (floor, type, tier, boss, max) |
| `storage/PlayerStatData.java` | Champs `dungeonFloorReached`, `lastOverworldDimensionId`, `lastOverworldPosPacked` |
| `storage/DungeonStateSerializer.java` | Sérialisation NBT des champs dungeon |
| `loot/AddDungeonShardModifier.java` | Loot modifier injectant des RuneShards dans le donjon |
| `config/Config.java` | Catégorie `trial_dungeon` : `xpBaseMultiplier`, `xpPerFloor`, `bossStatGain` |

### Layout horizontal (étages illimités)

Grille XZ : 10 colonnes × rangées infinies, espacement 80 blocs, Y=100 constant.
```
[F1] [F2] ... [F10]      Z=0
[F11][F12]...[F20]        Z=80
...
```

### Types d'étages

| Type | Fréquence | Taille | Contenu |
|------|-----------|--------|---------|
| Combat | Sauf multiples de 5/10 | 25×25 + mur | 4→14 mobs selon tier (Zombie→Piglin Brute) |
| Trésor | Multiples de 5 | 30×30 + mur | Coffre avec loot table `dungeon_treasure` |
| Boss | Multiples de 10 | 40×40 + mur | Autel → clic-droit → spawn boss du roster |

### Roster boss (extrait)

| Floor | Boss | Mode |
|-------|------|------|
| 10 | Wither Skeleton | Simple |
| 20 | Warden | Simple |
| 30 | Artorias (SLU) | Simple |
| 40 | Iron Golem, Ornstein, Smough | Vague |
| 50 | Margit + Morgott (SLU) | Simultané |
| 60 | Godskin Apostle + Noble (SLU) | Simultané |
| 70 | Malenia (SLU) | Simple |
| ... | ... (30 étages prédéfinis, jusqu'à floor 300) | ... |

### Tag `statmod:dungeon_boss`

44 entrées : 3 Vanilla (wither_skeleton, iron_golem, warden), 38 SLU (boss_aatrox, boss_malenia, boss_radahn...), 3 Iron's Spellbooks (dead_king, citadel_keeper, apocalypse_golem).

### Assets créés

- 4 blocs avec textures, modèles, blockstates, loot tables, lang (en_us + fr_fr)
- Textures générées via `tools/` scripts Python
- Sons : `dungeon_portal_enter`, `dungeon_boss_kill`, `dungeon_floor_complete`
- Loot table : `chests/dungeon_treasure.json` (diamants, runes, livres enchantés...)
- Loot modifier : `loot_modifiers/dungeon_shard.json`
- Tag : `tags/block/mineable_with_pickaxe.json` mis à jour

### Bug boss handler — RÉSOLU (2026-07-03, validé en jeu)

**Fix** : heuristique robuste dans `DungeonBossHandler` — sur un étage boss (multiple de 10),
n'importe quel mob tué par le joueur dans la dimension unlock l'étage suivant. Indépendant du
tag `statmod:dungeon_boss` et des mods installés (fallback Pig si SLU absent → unlock quand
même). Idempotent (`floorReached > floor` → skip). Le check de tag était le point de fragilité :
les entités SLU spawnnées par l'altar ne matchaient pas toujours le tag au moment du death event.

### Commandes

```
/statdungeon info         → progression
/statdungeon tp <N>       → tp étage N (admin)
/statdungeon unlock <N>   → débloque étage N
/statdungeon reset         → reset progression
```

### Pour tester

```bash
./gradlew runClient
/statdungeon unlock 300
/statdungeon tp 10
# Clic-droit autel → tuer le boss → vérifier déblocage
```

---

## 🏛️ Gouvernance — Onivo Studio

Le projet est piloté par **Onivo Studio** (voir `.tmp-onivo-audit/`). Les décisions structurantes sont consignées en tant que **decision records** :

| Id | Sujet | Date |
|---|---|---|
| `STAT-DEC-001` | NeoForge 1.21.1 = runtime truth | 2026-06-21 |
| `STAT-DEC-002` | Epic Fight = experimental optional | 2026-06-21 |
| `STAT-DEC-003` | Mahou + Elementals supprimés, postmortem ouvert | 2026-06-21 |
| `STAT-DEC-004` | STAT Mod onboardé dans Onivo Studio | 2026-06-21 |
| `STAT-DEC-005` | `docs/superpowers/plans/` canonique | 2026-06-21 |
| `STAT-DEC-006` | Séquence missions verrouillée (M3 → M10) | 2026-06-21 |
| `STUDIO-DEC-001` | Studio autonome avec décisions documentées | 2026-06-21 |

Toute contradiction observée entre ce `CLAUDE.md` et le code doit être résolue **en faveur du code** (runtime truth), puis ce fichier mis à jour via une décision numérotée.

---

## 📝 Décisions Techniques Historiques

1. **Pourquoi NeoForge et pas Forge 1.21.1 ?** Tensura Reincarnated n'existe que sur NeoForge 1.21.1.
2. **Pourquoi Epic Fight était initialement interdit ?** Au moment du port, Epic Fight n'avait pas de version NeoForge ; le port a depuis évolué et un compat package est viable — d'où `STAT-DEC-002`.
3. **Pourquoi des attachments et pas des capabilities ?** NeoForge a remplacé les capabilities par des attachments — plus simples, sans provider boilerplate.
4. **Pourquoi 23 stats ?** 14 stats actives (couvertes par les 84 perks) + 8 stats magiques (couvertes par l'arbre magique unifié Iron's Spellbooks).
5. **84 perks ?** 6 perks × 14 stats actives.
6. **Pourquoi Iron's Spellbooks ?** Voir `STAT-PM-001` — il fournit le bridge évènementiel unifié retenu pour le cast, le niveau de sort et l'inscription dans la stack magique actuelle.

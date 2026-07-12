# STAT MOD — CLAUDE.md

> Ce fichier est lu automatiquement par Claude à chaque session dans la branche `neoforge-1.21.1`.
> Dernière mise à jour gouvernée : 2026-07-12 par Onivo Studio — `STAT-DEC-PUBLIC-DISTRIBUTION` (release publique bêta Modrinth + CurseForge). Précédentes : 2026-07-04 `M6` (refonte de fiabilité du Trial Dungeon), 2026-07-03 (`STAT-DEC-TRIAL-DUNGEON`), `M5` (`STAT-DEC-OVERGEARED-EXPANSION`), `M3` (`STAT-DEC-002`, `STAT-DEC-003`, `STAT-DEC-005`).

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
8. 🟡 **Release publique gouvernée** (2026-07-12) : v1.2.0-**beta** publiée sur **Modrinth** (`statmod`, en modération) et **CurseForge** (`stat-mod-rpg` #1583282, fichier en approbation). Pipeline : `gradlew build modrinth publishCurseForge` (tokens dans le profil utilisateur, hors repo). Voir `STAT-DEC-PUBLIC-DISTRIBUTION` (`docs/superpowers/specs/2026-07-12-public-distribution-release-record.md`). Reste : galeries, relations CurseForge, passage beta→release.

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
| Lootr | **optional** | `integration/lootr/` | coffres individuels donjon |
| L2 Hostility | **optional** | `integration/l2hostility/` | scaling difficulté donjon |
| Waystones | **optional** | `integration/waystones/` | checkpoints donjon (M6, 2026-07-04) |
| PlayerRevive | **optional** | `integration/playerrevive/` | bleed-out/réanimation donjon (M6, 2026-07-08) — hard dep interne : `CreativeCore` |
| FTB Teams | **optional** | `integration/ftbteams/` | équipes du donjon : conquête + assists réservées à l'équipe (2026-07-09) |
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

**BUILD OK.** ⚠️ L'ancienne mention « boucle MVP fermée » (2026-07-03) était **fausse** — voir audit + fix ci-dessous (2026-07-09). L'heuristique naïve « tout mob tué sur un étage boss unlock l'étage suivant » décrite ici a été remplacée par `DungeonBossTracker` (voir §« Vraie aventure » 2026-07-04) puis durcie le 2026-07-09. Le `DungeonSpawnGuard` (double couche `FinalizeSpawnEvent` + `EntityJoinLevelEvent` avec marker NBT `statmod_dungeon_authorized`) bloque tous les spawns non-autorisés, y compris les spawns custom SLU.

**Island Redesign shippé** (spec + plan `2026-07-03-trial-dungeon-island-redesign`) : silhouettes organiques déterministes par étage (`IslandShaper`, bruit harmonique seedé), underside conique rocheux, spawn pad 3×3 dégagé, dais boss surélevé décentré (altar plus jamais sur le point de spawn), vault treasure à piliers, décor par tier (mousse EARLY → améthyste ABYSS), exits calculés sur la silhouette réelle. `/statdungeon regen <floor>` efface et régénère une île (même seed → même île).

**Refonte de fiabilité 2026-07-04 (mission « donjon parfait »)** — audit complet du package + corrections vérifiées contre les vrais jars :
- **Mobs moddés enfin fonctionnels** : `ModdedMobPool` cherchait le modId inventé `souls_like` (le vrai est `slu`) et des noms d'entités inexistants → pool toujours vanilla. Corrigé avec les IDs réels extraits des jars (`slu:hollow`, `slu:knight`, `slu:elite_knight`… ; Iron's Spellbooks `cultist`, `pyromancer`, `necromancer`…). Overgeared **retiré** du pool (aucune entité de combat : juste 2 flèches de forge). `DungeonSpawnGuard` whitelist alignée (`slu` + `irons_spellbooks`).
- **L2 leveling unifié** : `L2HostilityDisabler` (ciblait `LHDifficulty` — classe inexistante, échouait en silence à chaque spawn) **supprimé**. Le calage du niveau L2 par étage passe désormais par un chokepoint unique `DungeonSpawnGuard.onEntityJoinLevel` → `DungeonMobSpawner.scheduleL2` (couvre vagues, boss ET invocations/phase-2). C'est ce qui empêche réellement « niveau 60 à l'étage 1 ». Le hook réel (`LHMiscs.MOB` + `MobTraitCap.setLevel`) est vérifié présent dans le jar.
- **Génération simplifiée** : pipeline `IslandGenerator` = `DungeonArchitect` (forteresse « The Descent », le keeper) **puis** `IslandTerrainShaper` refondu (relief organique **montant uniquement** sur le seul pourtour, jamais dans l'emprise forteresse → plus de trous, plus de terrain gaspillé). Supprimés : `ModStructurePlacer` (placeholder qui ne posait rien + spam INFO), `DungeonArchetypes` (458 lignes, zéro usage). `DungeonMasterpiece` réduit à ses 3 méthodes vivantes (`buildUnderside`, `mobPool`, `mobCount`).
- **HUD corrigé** : le fond du scoreboard était dessiné dans le `PoseStack` scalé/translaté → hors écran. Redessiné en coordonnées écran absolues.
- **Roster boss validé** : les 40 IDs `slu:boss_*` + `irons_spellbooks:*` vérifiés existants dans les jars.
- Tests : 5 nouveaux (`IslandTerrainShaperTest`) verrouillent les invariants (jamais dans l'emprise forteresse, hauteur ≥ 0, borné, déterministe). Suite dungeon 100 % verte.

**« Vraie aventure » 2026-07-04 (mission : le donjon devient une aventure, pas un couloir)** :
- **Plus d'auto-unlock à l'entrée** : entrer sur un étage ne débloquait avant que le suivant → couloir. Désormais chaque étage doit être **conquis** (`enterFloor` ne touche plus `floorReached`). `DungeonProgress.completeFloor` est l'unique autorité de déblocage (hors commandes admin).
- **Objectif par étage** (`DungeonObjective`, dérivé du rôle) : combat → `CLEAR_WAVE` (éliminer toute la vague), trésor ×5 → `LOOT_VAULT` (ouvrir le coffre, `DungeonVaultHandler` sur `RightClickBlock`), boss ×10 → `SLAY_BOSS` (roster via `DungeonBossTracker`). La sortie (`next_floor_teleporter`) reste **scellée** tant que l'objectif n'est pas rempli.
- **`DungeonBossHandler` refondu** en handler de conquête unifié : route combat ET boss vers `DungeonProgress` ; combat conquis = dernière vague de mobs `AUTHORIZED_TAG` éliminée.
- **Célébration** : `DungeonProgress` joue son (`DUNGEON_FLOOR_COMPLETE`) + particules (totem + end rod) + message ; jalon narratif tous les 10 étages (`dungeon.milestone`).
- **Une seule vague par étage** : la vague de combat est posée une fois à l'entrée (`requestWave`) et n'est **jamais réalimentée**. La nettoyer = conquérir l'étage. (La réalimentation périodique, source d'une « invasion de mobs » continue, a été retirée le 2026-07-04.)
- **HUD objectif** : 4ᵉ ligne « ⚔ Clear all enemies / ✦ Loot the vault / ☠ Slay the boss / ✔ Conquered · exit open ».

**Fix « invasion de mobs » 2026-07-04** : deux sources de spawn continu supprimées.
- `DungeonSpawnGuard` refondu en **liste blanche stricte** : n'autorise QUE les entités qu'on marque (`AUTHORIZED_TAG` via `spawnAuthorized`) + les invocations d'un boss dont le combat est suivi (`DungeonBossTracker.isTracked`). L'ancien système de **fenêtre temporelle** (10–45 s ouverte à chaque entrée, illimitée pendant un boss) laissait passer un flux continu → supprimé.
- La passe de **réalimentation** de `DungeonMobSpawner` (respawn toutes les 3 s sur étage occupé) est **retirée**. La dimension est `the_void` (aucun spawn naturel), donc plus aucune source continue.
- Tests : `DungeonObjectiveTest` (6) verrouille le mapping rôle→objectif. Suite dungeon 100 % verte.

**Audit + fix « softlock boss » 2026-07-09 (Onivo Studio)** — audit du package `dungeon/` révélant que la refonte du 2026-07-04 n'avait pas fermé la boucle :
- **Critique — softlock permanent d'un étage boss** : `DungeonRespawnHandler` appelait `DungeonMobSpawner.clearFloorMobs` sur **toute** mort de joueur, y compris sur un étage boss → le boss (marqué `AUTHORIZED_TAG`) était discard, `DungeonBossTracker.clear` vidait son suivi, mais `DungeonBossAltarBlock` ne se réactivait jamais (`ACTIVE=false` définitif) et `isCombatFloor` exclut les étages ×10 de toute réalimentation. Une seule mort avant d'achever le boss rendait l'étage **définitivement infranchissable**. **Fix** : `clearFloorMobs` est désormais un no-op tant qu'un combat de boss est suivi (`DungeonBossTracker.isTracked`) — le combat continue au lieu d'être détruit ; corrige aussi le wipe collectif en multijoueur (la mort d'un joueur ne devait pas discard le boss que d'autres combattent encore).
- **Critique — exploit « un kill quelconque déverrouille »** : le secours de `DungeonBossHandler.handleBossFloor` (tracking perdu, ex. restart serveur) complétait l'étage sur la mort de **n'importe quelle entité**, sans vérifier `AUTHORIZED_TAG` — un mob apprivoisé amené par le joueur suffisait à conquérir un étage boss sans jamais toucher l'autel. **Fix** : le secours exige désormais `AUTHORIZED_TAG`.
- **Majeur — rayons de scan incohérents** : `DungeonBossHandler.livingAuthorizedCount` scannait sur 55 blocs vs `FLOOR_SCAN_RADIUS=85` utilisé partout ailleurs pour la même notion (pièces en chaîne jusqu'à 53×61 blocs) → conquête de combat prématurée possible. **Fix** : constante partagée.
- Tests : `DungeonBossTrackerTest` (7, nouveau) verrouille le contrat boss duo/vague (0 test avant malgré l'historique de bugs sur cette classe). Suite dungeon 100 % verte, build complet vérifié.
- Dette restante non traitée (hors scope de ce fix) : `DungeonBossHandler`, `DungeonSpawnGuard`, `DungeonProgress`, `DungeonMobSpawner`, `DungeonRespawnHandler` n'ont toujours aucun test direct (logique trop liée à `ServerLevel`/`Mob` pour le JUnit pur de ce repo, pas d'infra GameTest) — vérification actuelle = manuelle en jeu (`/statdungeon tp 10`, mourir volontairement en combat, vérifier que le boss est toujours là).

**« Dungeon Rush » 2026-07-09 (mission : le donjon devient ultra addictif)** — couche de renforcement au-dessus du système de points :
- **Combo de kills** : kills enchaînés dans une fenêtre de 8 s → multiplicateur de points croissant (×2,5 max à 30 kills). Encaisser un coup **brise** le combo (risque/récompense : jouer vite ET propre). Fanfare sonore à pitch croissant tous les 5 kills, action-bar `+N pts ⚡ combo K (×M)`.
- **Jackpot** : 4 % de chance par kill → points ×4, son + message dédiés (renforcement à ratio variable).
- **Sans-faute** : conquérir un étage sans un seul coup reçu → récompense de conquête ×2 (combat ET boss), titre « ★ SANS FAUTE » + son de défi. Armé à `enterFloor`, ré-armé à la conquête.
- Architecture : `DungeonRush` (cœur pur, temps en ticks injecté, état mémoire par UUID — rien n'est persisté, un combo ne survit pas à une session) + `DungeonRushHandler` (`LivingDamageEvent.Post` brise le combo, logout purge) + hooks dans `DungeonPoints.awardMobKill` / `DungeonProgress.completeFloor`. Tests : `DungeonRushTest` (12).

**Records personnels 2026-07-09** (`DungeonRecords` + champs persistés `dungeonBestCombo`/`dungeonBestClearTicks`) : meilleur combo (célébré dès 5) et meilleur temps de nettoyage d'étage de combat (chrono à `enterFloor`, arrêt à la conquête `CLEAR_WAVE`). Record battu → titre « ★ NOUVEAU RECORD » + sons. Tests : `DungeonRecordsTest` (3). **Backlog validé mais non réalisé (arrêt demandé)** : ambiance sonore par arc, arènes de boss à phases (50 %/25 % PV), prégénération de l'étage suivant (anti-lag), infra GameTest.

**Audit architecture + co-op 2026-07-09 (Onivo Studio)** — suite de la mission « ultra addictif » :
- **Îles agrandies shippées** : grille de pièces 5×4 (was 4×3), forteresse HX/HZ 134×122, rayon d'île R=148, `FLOOR_SPACING=300`. Décorateurs **Macaw's** (`MacawDungeonDecorator`) et **Quark** (`QuarkDungeonDecorator`) soft-resolus avec fallback vanilla. Fixes géométriques : `buildUnderside` couvre désormais toute l'emprise forteresse (le sol au-delà de r=80 flottait sans épaisseur), `floorBoundingBox` de regen élargie (Y −52..+54, ±R+4 — couvrait ni la cage ni la pointe du cône), plancher de cage abaissé à −48.
- **Co-op réparé** : conquête partagée avec tous les joueurs présents sur l'étage (avant : seul le tueur du dernier mob débloquait, les autres restaient scellés) ; la mort d'un joueur ne purge plus la vague si un coéquipier combat encore ; vagues +50 % de mobs par joueur supplémentaire (cap 72) ; points d'assist 40 % du kill de base pour les coéquipiers présents. Helper central : `DungeonTeleportHandler.playersOnFloor`. Tests : `DungeonCoopTest` (6).
- **Équipes FTB Teams** (`integration/ftbteams/FTBTeamsBridge`) : la conquête partagée et les assists sont réservées à l'**équipe FTB** du déclencheur (`arePlayersInSameTeam`) ; les rivaux présents voient un message de victoire adverse et gardent leur objectif. Sans FTB Teams (ou erreur) → fallback « tout le monde co-op ». FTB Teams donnant une équipe personnelle par joueur, deux joueurs sans party ne sont PAS co-équipiers (`/ftbteams party create` pour jouer ensemble). Tests : `FTBTeamsBridgeSourceTest` (3).

**Feedback playtest 2026-07-09 (portes/pièges/échelles)** :
- **IDs Macaw's cassés corrigés** (vérifiés contre les jars) : `whisper_oak_door`→`oak_whispering_door`, `iron_jail_door`→`jail_door`, `chain_lamp`→`chain_lantern`, `wall_lantern`→`covered_wall_lantern`. L'ancien bug faisait échouer TOUTE résolution → fallback porte de fer + levier permanent en jeu.
- **Zéro redstone dans le donjon** : plus de porte de fer ni levier (portes Macaw's, fallback épicéa, tout s'ouvre à la main) ; pseudo-puzzle du vault (bouton + poudre sous le sol) supprimé ; pièges distributeur+plaque et distributeur-au-dessus-du-coffre remplacés par des **pièges à command block** cachés sous le sol/coffre (crocs d'évocateur, poison, lenteur, wither ≥ 40). ⚠ Serveur dédié : `enable-command-block=true` requis dans server.properties.
- **Échelles de mezzanine** interdites sur les axes de portes (elles bouchaient parfois le passage de 1 bloc).
- **Mobs disciplinés** : toute entité autorisée est **persistante** (les étages de ~296 blocs dépassaient la distance de despawn de 128 → vague inclearable) et `DungeonMobDiscipline` interdit à un mob autorisé de cibler/blesser un autre mob autorisé (le pool multi-mods s'entretuait : piglins vs wither squelettes, flèches perdues → représailles).
- **Portes par thème** : `MacawDungeonDecorator.themedDoor` mappe les 14 arcs → 14 portes Macaw's distinctes (ids vérifiés contre le jar).
- **Arsenal de pièges command block** : 8 de base (crocs ×2, poison, lenteur, cage de toiles, lévitation, cécité, embuscade cave spider) + 4 profondeurs ≥40 (wither, enclume, `spreadplayers`, vexes). Embuscades invoquées avec `{NeoForgeData:{statmod_dungeon_authorized:1b}}` → passent le garde. Jamais de TNT/foudre (protège le bâti).
- **Indestructible aussi par les mobs** : `EntityMobGriefingEvent` refusé (endermen, zombies casseurs de portes en bois, ravagers, feu des fireballs), `LivingDestroyBlockEvent` annulé (wither, boss moddés), outils de terrain + briquet bloqués en survie. Exception : `FallingBlockEntity` atterrit (enclume du piège, gravier).
- **Salles secrètes « pari »** (`DungeonSecretRoom`) : la chambre sous trappe est bénie (~60 % : 2 coffres + plaque de bénédiction — effets uniquement, pas farmable) ou maudite (~40 % : 1 coffre + wither/cécité/vex). Indiscernable d'en haut, déterministe par étage.
- **Chambres-fortes ULTRA-secrètes** (`DungeonUltraVault`, ~1 étage de combat/7) : **sanctuaire** caché au sol d'une pièce élue — pierre de guidage (lodestone) + cristal d'améthyste, clic droit → téléportation Java (`RightClickBlock`, son ender + particules) vers une chambre scellée flottant à +36 dans la cage. Piédestal avec **arme unique** renommée « Relique du Donjon » (pools par profondeur : runic <30, légendaires 30-69, armes de boss SLU ≥70 — ids vérifiés jars), 4 coffres `dungeon_treasure`, blocs précieux, pierre de retour. La direction du tp est déduite de l'altitude. Tests : `DungeonUltraVaultTest` (3).

### Fichiers clés

| Fichier | Rôle |
|---------|------|
| `dungeon/DungeonRush.java` | **Dungeon Rush** : combo/jackpot/sans-faute — cœur pur testable (2026-07-09) |
| `dungeon/DungeonRushHandler.java` | Câblage Rush : coup reçu → combo brisé + sans-faute perdu ; logout → purge |
| `dungeon/DungeonDimensions.java` | ResourceKeys pour `statmod:trial_dungeon` |
| `dungeon/DungeonBlocks.java` | DeferredRegister : `dungeon_portal`, `return_beacon`, `next_floor_teleporter`, `boss_altar` |
| `dungeon/DungeonPortalBlock.java` | Bloc d'entrée : right-click → tp donjon + particules PORTAL |
| `item/DungeonBeaconItem.java` | Balise « portail de poche » (`statmod:dungeon_beacon`) : clic-droit n'importe où → entre au plus haut étage ; depuis le donjon → retour overworld. Craftable |
| `dungeon/DungeonTeleportHandler.java` | TP serveur : `enterFloor()`, `returnToOverworld()`, `floorAtPos()`, `floorSpawnPos()` |
| `dungeon/IslandGenerator.java` | Pipeline : `DungeonArchitect.buildFloor` (forteresse) + `IslandTerrainShaper.buildIslandGround` (relief pourtour) |
| `dungeon/DungeonLayout.java` | **Layout serpentin** (2026-07-05) : grille 4×3 de pièces, chemin boustrophedon déterministe, portes entre pièces consécutives. Pur/testable. Spawn = 1ʳᵉ pièce, sortie = dernière |
| `dungeon/DungeonRoomChain.java` | **Génération en chaîne de pièces** (étages COMBAT & TRÉSOR) : coques (sol/murs/plafond), portes percées, pad de spawn, dernière pièce = téléporteur (combat) ou coffres+aménagement (trésor) |
| `dungeon/DungeonBossArenaFloor.java` | **Étage BOSS = arène géante à ciel ouvert** : toute l'emprise, couronne de grands piliers 3×3 aux extrémités, centre façonné par `DungeonBossArena`, estrade+autel, aménagement, waystone, téléporteur scellé. Spawn joueur au bord sud |
| `dungeon/DungeonArchitect.java` | Forteresse « The Descent » : underside, remparts, tours, avenue, ailes (3 styles), faille (4 types), cœur selon rôle (+ dressing). **Bâtie avec `BlockPalette`** (matériaux par thème) |
| `dungeon/BlockPalette.java` | Interface des 12 blocs d'un étage (base/accent/light/underside/decor×2/wall/stair/slab/ceiling/scar/banner) |
| `dungeon/DungeonDetailing.java` | Passe « builder pro » : 7 familles de détails thématisés placés intelligemment (torches murales, toiles d'angle, suspensions plafond, salissure murale, clutter au sol le long des murs, gravats, signature de thème). Blocs choisis pour tenir sans support |
| `dungeon/DungeonBuildings.java` | Bâtiments dans les ailes des étages agrandis (tours de guet, huttes, ruines, sanctuaires, puits), thématisés par palette, construits par code (pas de schematics). Étages de combat uniquement |
| `dungeon/ThemePalette.java` | Palette **par arc/thème** (10 identités : pierre, os, prismarine, **glace**, citrouille, améthyste, **nether**, obsidienne…) → l'architecture ressemble à son thème, pas juste au tier. Alignée sur les arcs de `DungeonThemes` |
| `dungeon/DungeonRoomDressing.java` | Salles trésor (×5) & boss (×10) enrichies : fontaine de soin, waypoint (waystone), armor stands équipés par tier, piédestaux présentoirs |
| `dungeon/DungeonHealHandler.java` | Points de soin : joueur proche d'une fontaine → Régén II + Résistance I (passe/seconde) |
| `dungeon/IslandTerrainShaper.java` | Relief organique **montant uniquement** sur le pourtour (hors emprise forteresse) — `rimHeightAt` pure/testable |
| `dungeon/IslandShaper.java` | Géométrie pure seedée (silhouette + profondeur underside) — testable sans Bootstrap |
| `dungeon/DungeonMasterpiece.java` | Briques partagées : `buildUnderside` (cône) + table mobs (`mobPool`/`mobCount`) |
| `dungeon/ModdedMobPool.java` | Mobs moddés (10 mods : slu, irons_spellbooks, tensura, block_factorys_bosses, cataclysm, born_in_chaos_v1, mutantmonsters, mowziesmobs, alexsmobs) + vanilla (50 %) ; pool 100 % thématique si étage à thème |
| `dungeon/DungeonThemes.java` | **100 thèmes** (1 par étage, généré par `tools/gen_dungeon_themes.py`) : 10 arcs de 10 étages, difficulté croissante, horde + mini-boss par étage, boucle au-delà de 100. Thème superposé au rôle (×5 trésor / ×10 boss conservés) |
| `dungeon/DungeonMobSpawner.java` | Une vague de combat par étage (posée à l'entrée, pas de réalimentation) + calage L2 différé |
| `dungeon/DungeonDropGuard.java` | `LivingDropsEvent` : **zéro drop** de mob dans le donjon (système de points remplace le loot en cristaux) |
| `dungeon/DungeonPoints.java` | Système de points : mob tué → points ∝ **sa difficulté** (PV/attaque/armure via `difficultyRating`) × profondeur ; conquête +25 ; boss +150 ; mort → perte punitive (25 %, min 20). Stockés dans `PlayerStatData.dungeonPoints` (persistés + sync HUD) |
| `dungeon/DungeonObjective.java` | Objectif d'un étage selon son rôle : `CLEAR_WAVE` / `LOOT_VAULT` / `SLAY_BOSS` |
| `dungeon/DungeonProgress.java` | Autorité unique de conquête : unlock étage suivant + célébration (son/particules/message) + jalons |
| `dungeon/DungeonBossHandler.java` | Handler de conquête unifié (combat = vague nettoyée, boss = roster mort) → `DungeonProgress` |
| `dungeon/DungeonVaultHandler.java` | Conquête des étages trésor : ouvrir le coffre (`RightClickBlock`) → `DungeonProgress` |
| `dungeon/FloorPalette.java` | Palette par **tier** EARLY/MID/LATE/ABYSS (implémente `BlockPalette`). Sert de fallback + pilote la difficulté (armures des salles, végétation du pourtour) |
| `dungeon/DungeonSpawnGuard.java` | Liste blanche stricte : autorise seulement `AUTHORIZED_TAG` + invocations de boss suivi ; annule tout le reste |
| `dungeon/DungeonBossAltarBlock.java` | Bloc autel activable par le joueur pour spawner les boss du roster (éclate les entrées duo/vague sur les virgules) |
| `dungeon/DungeonBossArena.java` | Arène de boss **façonnée selon le type du boss** (AQUATIC bassin, FLYING arène haute, COLOSSUS fosse, INFERNAL magma, UNDEAD os/soul sand, ARENA colonnes) déduit de l'id du roster |
| `dungeon/DungeonBossRoster.java` | Roster prédéfini de 30 étages boss (floor → boss SLU/Vanilla) |
| `dungeon/DungeonBossHandler.java` | Handler `LivingDeathEvent` : sur boss floor, tout kill joueur → +stats + unlock étage suivant (fixé 2026-07-03) |
| `dungeon/DungeonXpMultiplier.java` | Multiplicateur d'XP par étage |
| `dungeon/DungeonNextFloorTeleporterBlock.java` | Bloc téléporteur vers étage suivant (si débloqué) |
| `dungeon/DungeonReturnBeaconBlock.java` | Bloc retour vers l'overworld |
| `dungeon/DungeonCommands.java` | `/statdungeon tp\|unlock\|info\|reset\|regen` (floors 1-10000) |
| `dungeon/DungeonRespawnHandler.java` | Défaite dans le donjon → **mort annulée** (inventaire + XP préservés), soin + retour étage 1 |
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
| `STAT-DEC-PUBLIC-DISTRIBUTION` | Release publique bêta : Modrinth (`statmod`) + CurseForge (`stat-mod-rpg` #1583282), pipeline gradle gouverné, mod vérifié standalone | 2026-07-12 |

Toute contradiction observée entre ce `CLAUDE.md` et le code doit être résolue **en faveur du code** (runtime truth), puis ce fichier mis à jour via une décision numérotée.

---

## 📝 Décisions Techniques Historiques

1. **Pourquoi NeoForge et pas Forge 1.21.1 ?** Tensura Reincarnated n'existe que sur NeoForge 1.21.1.
2. **Pourquoi Epic Fight était initialement interdit ?** Au moment du port, Epic Fight n'avait pas de version NeoForge ; le port a depuis évolué et un compat package est viable — d'où `STAT-DEC-002`.
3. **Pourquoi des attachments et pas des capabilities ?** NeoForge a remplacé les capabilities par des attachments — plus simples, sans provider boilerplate.
4. **Pourquoi 23 stats ?** 14 stats actives (couvertes par les 84 perks) + 8 stats magiques (couvertes par l'arbre magique unifié Iron's Spellbooks).
5. **84 perks ?** 6 perks × 14 stats actives.
6. **Pourquoi Iron's Spellbooks ?** Voir `STAT-PM-001` — il fournit le bridge évènementiel unifié retenu pour le cast, le niveau de sort et l'inscription dans la stack magique actuelle.

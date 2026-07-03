# Plan — Trial Dungeon Island Redesign

- **Date** : 2026-07-03
- **Spec** : `docs/superpowers/specs/2026-07-03-trial-dungeon-island-redesign.md`
- **Mission** : M6, extension redesign
- **Prérequis** : build vert (581+ tests), M6 MVP α→ζ shippé, bugfix boss handler + spawn guard en place

---

## Phase A — `IslandShaper` (géométrie pure)

**Créer** `src/main/java/tong/statmod/dungeon/IslandShaper.java`
- Constructeur `(long seed, int radius)` ; `seedFor(int floor)` en splitmix64
- `radiusAt(theta)`, `isInside(dx,dz)`, `isBoundary(dx,dz)`, `depthAt(dx,dz)`, `maxDepth()`
- Zéro import Minecraft world/block — juste java.lang/Math

**Créer** `src/test/java/tong/statmod/dungeon/IslandShaperTest.java`
- Déterminisme : deux instances même seed → mêmes résultats sur toute la grille
- Variabilité : seeds différents → silhouettes différentes (au moins une cellule diffère)
- `isInside(0,0)` toujours vrai ; aucune cellule inside hors de `[-R, R]`
- `radiusAt` borné dans `[0.82R, 1.0R]`
- Boundary ⇒ inside + au moins un voisin cardinal outside
- `depthAt` : 0 outside ; ≥ 1 inside ; profondeur au centre > profondeur au bord ; ≤ maxDepth

**Verify** : `./gradlew test --tests "tong.statmod.dungeon.IslandShaperTest"`

## Phase B — `FloorPalette` étendu

**Modifier** `FloorPalette.java` : ajouter `underside()`, `decorPrimary()`, `decorSecondary()`
(résolution lazy switch, même pattern que `base()`).

**Modifier** `FloorPaletteTest.java` : la classification n'ayant pas changé, seulement vérifier
que le fichier compile — pas de test Block-dependent (Bootstrap).

**Verify** : `./gradlew compileJava test --tests "tong.statmod.dungeon.FloorPaletteTest"`

## Phase C — `IslandGenerator` refactor

**Modifier** `IslandGenerator.java` :
1. `generateFloor` crée un `IslandShaper` (`seedFor(floor)`, radius du template) et le passe aux
   générateurs de template
2. `placeCirclePlatform` + `placeArenaWall` → `placeOrganicIsland` (surface + underside + mur
   boundary), decorPrimary swap ~8 % via `RandomSource.create(seed)`
3. Spawn pad 3×3 accent + clear d'air 3×3×3
4. Boss : altar sur dais 5×5 à `(0,+1,8)`, 4 piliers lumière, spawn dégagé
5. Treasure : vault piliers à `(5,5)`, coffre surélevé (loot table inchangée)
6. Exits recalculés via `radiusAt` (beacon NW à `0.5·r(θ_NW)`, teleporter S à `0.75·r(θ_S)`)
7. Corner lights à `0.6·R`
8. Décor secondaire dispersé (~1/25 cellules, jamais sur pad/exits/vault/dais)
9. `floorBoundingBox` : Y range `[spawnY - maxDepth - 1, spawnY + 6]`

**Modifier** `IslandGeneratorTest.java` : adapter le test de bounding box au nouveau Y range ;
les tests de spans XZ et centrage restent inchangés.

**Verify** : `./gradlew test --tests "tong.statmod.dungeon.*"` — tout vert.

## Phase D — Commande `regen` + gouvernance

**Modifier** `DungeonCommands.java` :
- `regen <floor>` : clear la bounding box (fill air) puis `IslandGenerator.generateFloor`
- Étendre `tp`/`unlock` de `integer(1,100)` à `integer(1,10000)`

**Modifier** `CLAUDE.md` §Trial Dungeon : noter le redesign (IslandShaper, palette étendue,
regen), retirer la mention du bug boss handler (fixé).

**Verify final** :
1. `./gradlew build` vert
2. `./gradlew runClient` → `/statdungeon tp 1` : île organique avec underside, pad dégagé,
   mousse/feuillage ; `/statdungeon regen 1` : régénère identique (même seed)
3. `/statdungeon tp 10` : dais + altar décentré, spawn libre
4. `/statdungeon tp 5` : vault treasure à piliers
5. `/statdungeon tp 55` : ambiance ABYSS (améthyste, end rods, end stone underside)

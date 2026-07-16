---
title: Cité des Aventuriers (étage 0) — refonte visuelle
branch: forge-1.20.1
date: 2026-07-16
status: design
author: Onivo Studio
supersedes-partial: 2026-07-12-adventurer-city-floor0-design.md (plan A « Fondations »)
---

# Cité des Aventuriers — Refonte visuelle (Forge 1.20.1)

## 1. Problème

La cité de l'étage 0 (`dungeon/city/`) est **fonctionnelle mais faible
architecturalement**. Audit du code de génération (2026-07-16) :

1. **Tout est une boîte.** `hall()` = pavé (sol + murs droits + toit à peine pentu
   `height+1+(rz-|z|)/3`), `tower()` = prisme carré creux + dalle plate. Aucune
   arche, courbe, pignon, ni variété de toiture.
2. **Détail en modulo.** Fenêtres `(x+z)%5==0`, sols `(x+z)&3==0` → damier mécanique,
   signature du build généré à la main.
3. **Volume vertical gâché.** Caverne de 70 blocs de haut (sol Y=100 → plafond plat
   Y=170), tours à 18-21. Le plafond est une **dalle parfaitement plate** → lit comme
   un toit, pas une grotte. Aucun repère haut.
4. **Surtout du vide.** Disque de 600 blocs de diamètre (RADIUS=300), place R=45,
   rempart R=292 : l'immense couronne intermédiaire est du `Blocks.STONE` brut avec
   des avenues fines et ~12 districts R≈38 éparpillés. Densité très faible.

Ce qui est **bon et à conserver** : géométrie pure testable (`CityPlan`), build étalé
sur les ticks (`CityBuildQueue`), SavedData versionné (`CitySavedData` / `CITY_VERSION`),
palette de matériaux par race (`CityMaterialPalette`, Quark + fallback vanilla),
purge legacy + regen.

## 2. Objectif

Cité **resserrée et dense**, avec les 4 défauts visuels traités : plafond de grotte,
silhouette/verticalité, fin du look modulo, densité au sol. **Tous les districts
existants sont conservés** (guilde, 4 quartiers raciaux, marché, camp des artisans,
arène, terrain d'entraînement, sanctuaire, jardins suspendus, hall des héros, cour des
portails, porte du donjon). Approche **A** : upgrade en place de l'architecture
existante, aucun nouveau système. Aucune régression sur les services (waystone,
banquier, marchands, téléporteurs, PvP arène).

## 3. Contraintes

- **Déterminisme total** : mêmes constantes/seeds → même ville. Tout aléa vient de
  `Random` à seed fixe (jamais `Math.random()`).
- **Budget par tick** : la génération reste enfilée dans `CityBuildQueue` ; le build
  final ne doit pas geler le serveur (ajuster `JOBS_PER_TICK` si besoin, cible ≤ ~1-2
  min de build de fond).
- **Zone étage 0 inchangée** : la cité reste entièrement dans `z < -150`
  (`floorAtPos == 0`). Centre (0,100,-500), donc à RADIUS=160 elle couvre z∈[-660,-340]
  ⊂ z<-150. OK.
- **Compat mondes existants** : bump `CITY_VERSION` → reconstruction ; l'ancienne
  emprise (RADIUS=300) doit être **purgée intégralement** (voir §5.0), sinon anneau
  orphelin flottant.
- **Consommateurs de `CityPlan`** : `onDistrictConnector` (pavage des rues) et
  `inArenaCombat` (zone PvP, `DungeonProtectionHandler`) lisent `sites()`/`arena()` —
  rester cohérent après repositionnement.
- Convention projet : **100 % code, pas de schematics**.

## 4. Architecture cible (vue d'ensemble)

Pipeline `CityGenerator.enqueueBuild` inchangé dans son ordre
(coque → place → districts → porte → portails → intérieurs → détails), mais chaque
étape monte en gamme. Nouvelles briques de massing dans `CityDistrictBuilder`
(helpers de toitures/arches/flèche). Nouveau bruit de plafond dans `CityShell`.
Nouvelle passe de densité (`CityDetailPass` étendu). Géométrie repositionnée dans
`CityPlan` (pur).

## 5. Phases livrables

Chaque phase compile, se teste, et laisse la cité jouable.

### Phase 0 — Rétrécir proprement (`CityPlan`, `CityGenerator`, test)

**Constantes rescalées** (`CityPlan`) :

| Constante | Avant | Après |
|---|---|---|
| `RADIUS` | 300 | **160** |
| `WALL_INNER` | 292 | **152** (rempart épais 8) |
| `WALL_TOP_Y` | 140 | **128** |
| `PLAZA_RADIUS` | 45 | **28** |
| `INNER_RING_RADIUS` | 100 | **62** |
| `OUTER_RING_RADIUS` | 205 | **118** |
| `CEILING_Y` | 170 | conservé comme **plafond max** (voir Phase 1) |
| `AVENUE_HALF_WIDTH` | 3.5 | 3.5 (inchangé) |

**Layout des districts — 2 anneaux, anti-chevauchement.**
Convention d'angle du code : `atan2(dz,dx)`, sud (+z, côté porte/étage 1) = +90°.
On réserve le couloir sud (±20° autour de 90°) pour l'avenue de la porte.

- **Anneau extérieur** R=118, **8 créneaux** décalés de l'axe sud (θ ∈
  {22.5, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5, 337.5}°) : les 4 quartiers raciaux +
  guilde + marché + arène + terrain d'entraînement. Rayon de site réduit à **≤ 32**
  (espacement d'arc ≈ 92 > 64).
- **Anneau intérieur** R=62, **4 créneaux** tournés de 45° pour s'intercaler
  (θ ∈ {45, 135, 225, 315}°) : camp des artisans, sanctuaire, jardins suspendus, hall
  des héros. Rayon de site **≤ 24**.
- **Cour des portails** : flanque le couloir de la porte (sud), hors anneaux.
- **Porte du donjon** : perce le rempart sud (`gateCenter` = `WALL_INNER-12`).

`arena()` conserve un centre cohérent avec `inArenaCombat` (rayon combat 26 codé en
dur dans `CityPlan.inArenaCombat` → l'arène doit rester ≥ 27 de rayon bâti). Les
centres exacts (x,z) sont dérivés des angles ci-dessus et finalisés à
l'implémentation ; **invariant vérifié par test** : deux disques de site quelconques
ne se recouvrent pas (distance entre centres ≥ somme des rayons), et tout site ⊂
disque `WALL_INNER`.

**Purge de l'ancienne emprise** (`CityGenerator`) :
- Ajouter `LEGACY_MAX_RADIUS = 300`. Sur upgrade (`previousVersion > 0`), avant le
  rebuild, enfiler un `enqueueLegacyExtentClear` qui vide **à l'air** toute la colonne
  (Y de `GROUND_Y-2` à `CEILING_Y+2`) sur le disque de l'**ancien** rayon 300 autour
  du centre, y compris sol et rempart (scan-and-clear : jamais de `setBlock` sur de
  l'air, comme l'existant). Ainsi aucun bloc n'orpheline hors de la nouvelle cité R=160.
- `enqueueInteriorClear` : borner sur `LEGACY_MAX_RADIUS` (pas le nouveau `RADIUS`)
  tant que d'anciens mondes peuvent contenir du bâti au-delà de 160.
- `enqueueCityEntityClear` : élargir l'AABB à `LEGACY_MAX_RADIUS` (sinon armor
  stands / marchands / mannequins des anciens districts au-delà de r=160 subsistent).
- Bump `CITY_VERSION` (11 → 12).

**Test** (`src/test/.../city/CityPlanTest.java`, nouveau, JUnit pur) :
- Tous les sites ⊂ disque `WALL_INNER`.
- Aucun chevauchement de sites (paires).
- `playerSpawn`, `gateCenter`, `inCity`/`inPlaza` cohérents avec les nouvelles
  constantes.

### Phase 1 — Plafond de grotte (`CityShell`)

- Remplacer la dalle plate `CEILING_Y` par un **plafond à hauteur variable seedée** :
  bruit de valeur 2D (seed fixe, ex. `0xCEIL`), hauteur du plafond ∈ [~148, `CEILING_Y`
  (=170)], **plus haut au-dessus de la place** (effet cathédrale : interpoler vers
  `CEILING_Y` quand la distance au centre → 0), plus bas vers le rempart. `CEILING_Y`
  reste le plafond maximal (peak) ; aucune roche ne monte au-dessus.
- Sous la surface du plafond, **corps rocheux descendant** (deepslate / tuff / stone
  mélangés seedés) pour l'épaisseur, jamais de trou sur le vide (coque fermée : au
  moins 1 bloc plein sur toute l'emprise `inCity`).
- **Vraies stalactites** : garder les cristaux d'améthyste existants, ajouter des
  pointes de dripstone (`POINTED_DRIPSTONE`, `up`/`tip`) et des grappes plus longues
  sous les points bas ; lumière encastrée (sea lantern / glowstone masqué) pour un
  éclairage indirect.
- Sol : reste globalement plat (cité), mais pavage varié seedé (voir Phase 3) plutôt
  que le `Blocks.STONE` uniforme actuel du remplissage.

### Phase 2 — Silhouette & verticalité (`CityDistrictBuilder` + helpers)

Nouveaux helpers privés (déterministes, sans support flottant) :
- `pitchedRoof(...)` / `hipRoof(...)` — toitures en escaliers (pente + faîtage),
  remplacent le toit quasi-plat de `hall()`.
- `dome(...)` / `cupola(...)` — coupole pour guilde / sanctuaire / hall des héros.
- `archway(...)` — arche en escaliers (portes/galeries) au lieu du simple trou d'air.
- `spire(...)` — flèche haute.

Contenu :
- **Repère central haut** : une flèche/tour landmark (~30-40 blocs, ex. flèche de la
  Guilde ou tour du Cristal Gardien sur la place) montant vers le plafond de grotte,
  pour donner un skyline et guider le joueur.
- `hall()` retravaillé : toit pentu/hippé, piliers d'angle, arche d'entrée, quelques
  bâtiments à **2-3 étages** pour la verticalité (hauteur variée par district).
- `tower()` : chapeau en cône/coupole au lieu de la dalle plate.
- Ponts / passerelles courts entre bâtiments proches d'un même district quand la
  géométrie s'y prête (optionnel par district, seedé).

### Phase 3 — Fin du modulo (`CityDistrictBuilder`)

- **Fenêtres** : `Random` seedé par centre de bâtiment ; baies structurées régulières
  (montants + linteau en dalle/escalier), 1-2 blocs de haut, jamais `(x+z)%5`.
- **Pavages** : remplacer `(x+z)&3` par un **mélange seedé** de 2-3 blocs de pavage
  proches (andésite polie / pierre lisse / dalles) → texture composée, non gridée.
- Ce changement doit rester **déterministe** (seed dérivée de la position du site).

### Phase 4 — Densité au sol (`CityDetailPass`, `detailStreet`)

- **Maisons de remplissage** : petites structures le long des avenues resserrées entre
  place et districts (2-3 par avenue), pour combler le sol.
- **Props seedés** : lampadaires sur poteau, jardinières/fleurs, bancs, bannières,
  tonneaux, canaux/bassins d'eau, clutter de chemin — placés via `Random` seedé (pas de
  grille visible).
- **Passe d'éclairage d'ambiance** : lanternes/glowstone masqué pour que la cité se
  sente habitée (la dimension est `the_void`, aucun spawn — l'éclairage est purement
  esthétique).

## 6. Tests & vérification

- **Unitaires (JUnit pur)** : `CityPlanTest` (invariants géométriques Phase 0 : ⊂
  rempart, non-chevauchement, spawn/porte). La logique de bruit de plafond peut être
  extraite en fonction pure (`ceilingHeightAt(x,z)`) et testée (bornée, déterministe,
  ≥ sol).
- **Manuel en jeu** : `/statdungeon regen 0` sur un monde neuf ET un monde existant
  (RADIUS=300) → vérifier purge complète (pas d'anneau orphelin), plafond non-plat,
  skyline avec repère, aucune boîte modulo, densité, et **services intacts** (waystone,
  banquier, 4 marchands, téléporteur étage 1, PvP arène, respawn place, mort étage 0 =
  0 pénalité).
- **Build complet** : `./gradlew build` vert avant merge.

## 7. Risques & mitigations

- **Anneau orphelin après shrink** → purge `LEGACY_MAX_RADIUS` + `enqueueInteriorClear`
  borné sur l'ancien rayon (§5.0). Testé manuellement sur monde existant.
- **Chevauchement de districts** à emprise réduite → invariant testé (Phase 0) ; rayons
  de site réduits (≤32 ext / ≤24 int) et layout à 2 anneaux intercalés.
- **Gel de build** (plus de contenu) → rester enfilé par tick ; mesurer, ajuster
  `JOBS_PER_TICK`.
- **Incohérence PvP arène** (`inArenaCombat` en dur à 26) → arène bâtie ≥ 27 de rayon,
  centre `arena()` cohérent.
- **Rupture de déterminisme** → tout aléa à seed fixe ; pas de `Math.random`/`Date`.

## 8. Gouvernance

- Bump `CITY_VERSION` (reconstruction auto des mondes).
- Mettre à jour la section « Cité des Aventuriers » du `CLAUDE.md` après implémentation.
- Ce spec **remplace partiellement** le plan A « Fondations »
  (`2026-07-12-adventurer-city-floor0-design.md`) sur le volet visuel ; les plans B/C
  (PNJ, respec, arène de duels…) restent hors scope ici.

## 9. Hors scope

- Aucun nouveau système (pas de placeur procédural, pas de schematics).
- Pas de PNJ / gameplay nouveau (plans B/C).
- Pas de refonte des étages 1+ (donjon proprement dit).
- Boutique SDM (sujet séparé).

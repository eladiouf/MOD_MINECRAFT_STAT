# Trial Dungeon — Island Redesign

- **Date** : 2026-07-03
- **Mission** : M6 (`STAT-DEC-TRIAL-DUNGEON`) — extension redesign
- **Statut** : approuvé (posture autonome Onivo Studio)
- **Plan associé** : `docs/superpowers/plans/2026-07-03-trial-dungeon-island-redesign.md`

---

## Problème

Les îles générées par `IslandGenerator` (Phase ε) remplissent leur rôle fonctionnel mais échouent
visuellement :

1. **Disques plats parfaits** — un cercle mathématique de 1 bloc d'épaisseur. Vu de côté ou d'en
   dessous (dimension void), l'île n'a aucun volume : on voit un pancake flottant.
2. **Zéro variété** — tous les étages combat d'un même tier sont identiques au bloc près. Le
   pattern d'accent est un damier déterministe (`(dx+dz)%5`) qui se répète à l'infini.
3. **Ambiance de tier réduite au swap de bloc** — EARLY/MID/LATE/ABYSS ne changent que la
   pierre. Pas de végétation, pas de cristaux, pas de relief.
4. **Bug : altar sur le spawn** — `generateBossIsland` pose le `boss_altar` exactement sur
   `floorSpawnPos(floor)`, là où le joueur est téléporté → le joueur spawn *dans* le bloc.
5. **Exits posés au-delà du bord potentiel** — le teleporter sud à `(0, radius-2)` suppose un
   cercle parfait ; toute silhouette organique risque de le mettre dans le void.

## Objectifs

| # | Objectif | Mesure |
|---|---|---|
| O1 | Silhouette organique par étage | Rayon modulé par bruit harmonique, seed déterministe par étage — deux étages ne se ressemblent pas, un même étage se régénère identique |
| O2 | Volume flottant | Underside conique rocheux (profondeur ∝ distance au centre), lisible depuis le void |
| O3 | Ambiance par tier | Blocs de décor dispersés (mousse/feuillage EARLY, briques MID, magma LATE, améthyste ABYSS) |
| O4 | Identité par template | Spawn pad 3×3 dégagé, dais boss surélevé décentré, vault treasure à piliers |
| O5 | Itération rapide | `/statdungeon regen <floor>` efface et régénère l'île |

## Non-objectifs

- Ponts entre îles / multi-plateformes par étage (post-MVP)
- Templates NBT / jigsaw (le code programmatique reste la voie, cf. Phase ε)
- Nouveaux blocs custom (le décor n'utilise que des blocs vanilla)
- Refonte du mob scaling (inchangé, seule la géométrie bouge)

## Design

### D1 — `IslandShaper` (nouveau, pur, testable)

Classe de géométrie **sans dépendance Level/Blocks** — instanciable en JUnit sans Bootstrap
(pattern `ForgingIntermediateIds` / `RuneShardIds`).

```
IslandShaper(long seed, int radius)
  static long seedFor(int floor)      // splitmix64(floor) — stable entre runs
  double radiusAt(double theta)       // R * (0.82 + 0.18 * bruit harmonique(θ, seed))
  boolean isInside(int dx, int dz)    // dist(dx,dz) <= radiusAt(atan2)
  boolean isBoundary(int dx, int dz)  // inside && un voisin cardinal outside
  int depthAt(int dx, int dz)         // profondeur underside : 0 si outside,
                                      // sinon 1..maxDepth, conique + jitter hash
  int maxDepth()                      // radius/2 + 2
```

Bruit harmonique : `sin(3θ+φ1) + 0.5·sin(7θ+φ2)`, phases dérivées du seed. Doux, organique,
déterministe, zéro allocation.

### D2 — `FloorPalette` étendu

Deux nouvelles résolutions lazy :

| Tier | `underside()` | `decorPrimary()` | `decorSecondary()` |
|---|---|---|---|
| EARLY | STONE | MOSSY_COBBLESTONE | OAK_LEAVES |
| MID | DEEPSLATE | DEEPSLATE_BRICKS | COBBLED_DEEPSLATE |
| LATE | BASALT | MAGMA_BLOCK | POLISHED_BLACKSTONE_BRICKS |
| ABYSS | END_STONE | AMETHYST_BLOCK | CRYING_OBSIDIAN |

`underside()` = pierre du cône inférieur (contraste avec la surface). `decorPrimary` = swap
in-platform (~8 % des cellules). `decorSecondary` = éléments posés sur la plateforme (~1/25
cellules, hauteur 1-2).

### D3 — `IslandGenerator` refactor

- `placeCirclePlatform` → `placeOrganicIsland(level, center, shaper, palette)` : surface (base +
  decorPrimary seedé) + underside conique (`underside()`) + mur sur `isBoundary` uniquement.
- **Spawn pad** : 3×3 accent sous le spawn + clear 3×3×3 d'air au-dessus (garantit un tp propre).
- **Boss** : altar déplacé à `spawn.offset(0, 0, 8)` sur un dais 5×5 accent surélevé de 1,
  4 piliers de 3 avec lumière au sommet. Le spawn reste dégagé.
- **Treasure** : vault à `(5, 5)` — sol 3×3 accent, 4 piliers d'angle hauteur 2 + lumière,
  coffre au centre sur un bloc accent surélevé.
- **Exits** : positions calculées via `shaper.radiusAt(θ)` — teleporter sud à
  `radiusAt(π/2)·0.75`, jamais dans le void.
- **Corner lights** : rayon `0.6·R` (toujours à l'intérieur de la silhouette min 0.82·R).
- Décor secondaire : dispersé via le random seedé, jamais sur le pad/exits/vault/dais.

### D4 — Commande `regen`

`/statdungeon regen <floor>` (permission 2) : remplit d'air la bounding box étendue
(surface + underside + décor) puis rappelle `IslandGenerator.generateFloor`. Les mobs déjà
spawnés ne sont pas tués (hors scope — `/kill @e[distance=..40]` fait l'affaire en test).

Bonus : la limite `integer(1, 100)` de `tp`/`unlock` passe à `integer(1, 10000)` — le roster
va jusqu'à 300 et le layout grille est illimité.

### D5 — Compatibilité

- `floorBoundingBox` étend son Y : `[spawnY - maxDepth - 1, spawnY + 6]` — les tests existants
  sur les spans XZ restent valides (la silhouette ne dépasse jamais R).
- Le canari d'idempotence (`spawn.below()` non-air) reste vrai : le pad 3×3 couvre ce bloc.
- Aucun changement de layout grille, de teleport, de loot, de spawn guard.

## Exit conditions

- Si le rendu organique cause des trous de pathfinding mob bloquants → fallback : silhouette
  organique conservée mais `minFactor` remonté de 0.82 à 0.90.
- Si le coût de génération d'une île boss (~40×40×14) cause un lag spike au premier tp →
  déplacer la génération dans un tick handler étalé (budget 2000 blocs/tick).

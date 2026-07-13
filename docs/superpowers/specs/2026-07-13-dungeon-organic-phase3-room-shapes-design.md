# Phase 3 — Formes de salles organiques (remplacement 6 shapes dures)

> **Date** : 2026-07-13
> **Statut** : Design — prêt implémentation
> **Contexte** : `DungeonRoomChain` utilise 6 shapes en dur (`RECT`, `OCTAGON`, `CHAMFER`, `CROSS`, `DIAMOND`, `ROUND`, `STAR`) avec des formules géométriques exactes. Phase 3 remplace par des formes procédurales générées par OpenSimplex2.
> **Dépendances** : Phase 1 + Phase 2 (OrganicRoomLayout)

---

## Résumé

| Aspect | Actuel | Cible |
|---|---|---|
| Shapes | 6 énumérations avec `inside()` géométrique | Bruit 2D avec `inside(x,z)` = `noise > threshold` |
| Murs | Droits, angles à 90° ou 45° | Contours organiques, irréguliers |
| Variété | 6 formes × hauteurs variables | Continuum de formes (seuil du bruit = slider) |
| Piliers | Placement géométrique (grille/anneau) | Piliers où `noise > threshold_high` |
| Alcôves | Formes spéciales (CROSS, STAR) | Recoins naturels par bruit |
| Dais/Pools | Formes géométriques exactes (7×7, 5×5) | Formes adaptées au contour bruité |

---

## 1. `inside()` basé sur OpenSimplex2

### Principe
Chaque cellule de grille (ou de cluster) est échantillonnée avec un bruit 2D seedé par `(floor, roomIndex)`. Le seuil détermine la forme :

```java
private final OpenSimplex2S roomNoise;

// Seedé par (floor, roomIndex) — chaque salle a sa propre forme
public NoiseShapeRoom(long floorSeed, int roomIndex) {
    long seed = floorSeed ^ (roomIndex * 0x9E3779B97F4A7C15L);
    this.roomNoise = new OpenSimplex2S(seed);
}

public boolean inside(int lx, int lz, int w, int d) {
    // Coordonnées locales normalisées [−1, 1]
    double nx = (lx - w/2.0) / (w/2.0);
    double nz = (lz - d/2.0) / (d/2.0);

    // Bruit 2D à la position + distance du centre (forme ronde de base)
    double noise = roomNoise.noise2(nx * 3.0, nz * 3.0);
    double dist = Math.sqrt(nx * nx + nz * nz);

    // Le seuil varie pour créer différentes formes :
    // - Proche de 0.0 → forme agressive (déchiquetée)
    // - Proche de −0.3 → forme douce (arrondie)
    // - Avec dist → forme qui suit le contour circulaire
    double threshold = -0.2 + dist * 0.3;

    return noise > threshold;
}
```

Le seuil variable `−0.2 + dist·0.3` crée un **cercle bruité** : proche du centre c'est large (le bruit n'a pas besoin d'être fort), sur les bords c'est exigeant (seuil haut → contour crénelé naturel).

### Par pièce : variation de seed
Chaque pièce d'un étage a sa propre forme :

```java
// floorSeed est IslandShaper.seedFor(floor)
// roomIndex dans [0, nbRooms-1]
long seed = floorSeed ^ (roomIndex * 0x9E3779B97F4A7C15L + 0x1234);
```

---

## 2. Piliers organiques

Au lieu de `pillarHall()` / `columnRing()` / `quadPillars()` avec placement géométrique :

```java
public void placePillars(ServerLevel lv, BlockPos sp, BlockPalette t,
                         int w, int d, long seed, int ceilH) {
    OpenSimplex2S pillarNoise = new OpenSimplex2S(seed ^ 0xABCD);
    for (int x = 4; x < w - 4; x += 5) {
        for (int z = 4; z < d - 4; z += 5) {
            double nn = pillarNoise.noise2(x * 0.08, z * 0.08);
            // Piliers où le bruit dépasse un seuil (∼30% de densité)
            if (nn > 0.3) {
                int px = minX + x, pz = minZ + z;
                if (noiseShape.inside(px, pz, w, d)) {
                    buildColumn(lv, sp, px, pz, ceilH, t);
                }
            }
        }
    }
}
```

Résultat : piliers répartis organiquement, pas en grille régulière.

---

## 3. Alcôves et recoins

Les recoins sont créés par le **même bruit** qui définit la forme : quand le contour a une indentation (bruit localement bas), l'`inside()` passe à l'intérieur du bord → recoin.

Pour des alcôves prononcées, on ajoute une passe :

```java
// Si le bruit est très bas juste à l'extérieur du mur,
// on creuse une alcôve dans la direction du gradient
if (!inside(x, z) && isNearWall(x, z)) {
    // Calculer le gradient du bruit
    double gx = roomNoise.noise2(nx*3 + 0.01, nz*3) - roomNoise.noise2(nx*3 - 0.01, nz*3);
    double gz = roomNoise.noise2(nx*3, nz*3 + 0.01) - roomNoise.noise2(nx*3, nz*3 - 0.01);
    // Si le gradient pointe vers l'intérieur → creuser
    if (gx * nx + gz * nz < 0) {
        // surface de la cellule reste AIR (recule le mur)
    }
}
```

---

## 4. Plateformes et fosses

Au lieu de `centralDais()` / `themedPool()` avec formes exactes :

```java
OpenSimplex2S featureNoise = new OpenSimplex2S(seed ^ 0x5678);
for (int x = minX; x <= maxX; x++) {
    for (int z = minZ; z <= maxZ; z++) {
        if (!inside(x, z)) continue;
        double f = featureNoise.noise2(x * 0.06, z * 0.06);
        if (f > 0.5) {
            // Plateforme surélevée (bruit fort)
            for (int y = 1; y <= (int)((f - 0.5) * 4); y++) {
                setBlock(..., accent);
            }
        } else if (f < -0.5) {
            // Fosse encastrée (bruit faible)
            for (int y = -1; y >= (int)((f + 0.5) * 4); y--) {
                setBlock(..., fluid ou base);
            }
        }
    }
}
```

---

## 5. Changements dans `DungeonRoomChain`

- **Supprimer** `enum Shape` (RECT, OCTAGON, CHAMFER, CROSS, DIAMOND, ROUND, STAR)
- **Supprimer** `shapeFor()`, `inside(r, x, z, shape)`, `getWallDistance()`
- **Remplacer** `shell()` par version utilisant `NoiseShapeRoom.inside()`
- **Remplacer** `pillarHall/centralDais/columnRing/themedPool/quadPillars` par versions organiques
- **Conserver** `lightRoom`, `spawnPad`, `exitRoom`, `treasureRoom`, `safehouseRoom`, `archiveRoom`, etc. (le contenu fonctionnel)
- **Conserver** `decorateCombatRoom` comme dispatcher (choisit archétype via `Math.floorMod`)

### Nouveau flux dans `shell()`
```java
private static void shell(ServerLevel lv, BlockPos sp, BlockPalette t, 
                          OrganicRoomLayout.OrganicRoom r, int floor) {
    NoiseShapeRoom shape = new NoiseShapeRoom(IslandShaper.seedFor(floor), r.index());
    // ... utiliser shape.inside(x-local, z-local, w, d) partout
}
```

---

## 6. Tests

| Test | Description |
|---|---|
| `shapeDeterministic` | Même seed → même `inside()` |
| `centerAlwaysInside` | Centre de la pièce toujours accessible |
| `shapeHasReasonableArea` | Au moins 40% de la bounding box est intérieur |
| `doorwaysRemainOpen` | Les 4 milieux de bords restent toujours inside |
| `pillarsNeverBlockDoor` | Piliers placés loin des portes |
| `uniqueShapePerRoom` | Deux pièces différentes → formes différentes |

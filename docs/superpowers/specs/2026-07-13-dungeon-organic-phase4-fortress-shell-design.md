# Phase 4 — Shell forteresse organique (remplacement rectangle HX×HZ)

> **Date** : 2026-07-13
> **Statut** : Design — prêt implémentation
> **Contexte** : `DungeonArchitect` utilise un rectangle fixe HX=134, HZ=122. Phase 4 remplace par une empreinte organique suivant les contours du bruit de l'île.
> **Dépendances** : Phase 1-3

---

## Résumé

| Aspect | Actuel | Cible |
|---|---|---|
| Emprise forteresse | Rectangle 268×244 blocs | Polygone organique ⊆ île |
| Murs extérieurs | Lignes droites N/S/E/W | Lignes bruitées suivant OpenSimplex2 |
| Tours | Angles fixes (4 coins) | Tours positionnées par bruit |
| Créneaux | Hauteur uniforme | Hauteur variable par bruit |
| Portes | Entrée unique (devant) | Entrées multiples selon bruit |

---

## 1. `OrganicFortressShell` — Nouveau shell organique

```java
public final class OrganicFortressShell {
    // Taille cible : l'emprise de la forteresse suit la silhouette de l'île
    // mais à une échelle réduite (même ratio surface qu'avant)
    
    // Facteur de réduction par rapport au rayon de l'île R
    private static final double FORTRESS_RATIO = 0.65; 
    // Nombre de tours générées
    private static final int MIN_TOWERS = 4;
    private static final int MAX_TOWERS = 8;

    private final IslandShaper shaper;
    private final OpenSimplex2S wallNoise;
    private final int floor;
    private final double baseRadius;

    public OrganicFortressShell(long floorSeed, int islandRadius, int floor) {
        this.shaper = new IslandShaper(floorSeed, islandRadius);
        this.wallNoise = new OpenSimplex2S(floorSeed ^ 0xCAFE);
        this.floor = floor;
        this.baseRadius = islandRadius * FORTRESS_RATIO;
    }
```

### Empreinte organique

Au lieu d'un `air(lv, sp, -HX+1, 0, -HZ+1, HX-1, WALL_H+2, HZ-1)` qui vide tout un rectangle, on vide seulement l'empreinte organique :

```java
public boolean isInsideFootprint(int dx, int dz) {
    double dist = Math.sqrt(dx * dx + dz * dz);
    if (dist == 0) return true;
    double theta = Math.atan2(dz, dx);

    // Rayon organique = rayon de base * modulation par bruit
    double modulation = 1.0 + wallNoise.noise2(
        Math.cos(theta) * 3.0, Math.sin(theta) * 3.0) * 0.15;
    double rFootprint = baseRadius * modulation;

    return dist <= rFootprint;
}
```

Pour l'air-clear (remplace la méthode `air()` de DungeonArchitect) :

```java
public void clearInterior(ServerLevel lv, BlockPos sp, BlockPalette t) {
    for (int x = -(int)baseRadius - 10; x <= (int)baseRadius + 10; x++) {
        for (int z = -(int)baseRadius - 10; z <= (int)baseRadius + 10; z++) {
            if (isInsideFootprint(x, z)) {
                DungeonArchitect.air(lv, sp, x, 0, z, x, WALL_H + 2, z);
            }
        }
    }
}
```

### Murs extérieurs organiques

Les murs suivent le contour du footprint, avec une épaisseur de 2 blocs :

```java
public void buildWalls(ServerLevel lv, BlockPos sp, BlockPalette t) {
    int R = (int)baseRadius + 10;
    for (int dx = -R; dx <= R; dx++) {
        for (int dz = -R; dz <= R; dz++) {
            if (!isInsideFootprint(dx, dz)) continue;
            // Vérifier si la cellule est sur le bord du footprint
            boolean edge = !isInsideFootprint(dx + 1, dz) || !isInsideFootprint(dx - 1, dz)
                        || !isInsideFootprint(dx, dz + 1) || !isInsideFootprint(dx, dz - 1);
            if (!edge) continue;

            // Mur épaisseur 2
            for (int w = 0; w < 2; w++) {
                int wx = dx + (isInsideFootprint(dx + 1, dz) ? -w : w);
                int wz = dz + (isInsideFootprint(dx, dz + 1) ? -w : w);
                for (int y = 0; y <= WALL_H; y++) {
                    lv.setBlock(sp.offset(wx, y, wz), wallBlock(t, wx, y, wz), 3);
                }
            }

            // Créneaux de hauteur variable
            double crenelNoise = wallNoise.noise2(dx * 0.1, dz * 0.1);
            int crenelH = (int)(2 + crenelNoise * 1.5);
            for (int y = WALL_H + 1; y <= WALL_H + crenelH; y++) {
                lv.setBlock(sp.offset(dx, y, dz), t.base().defaultBlockState(), 3);
            }
        }
    }
}
```

### Tours positionnées par bruit

Les tours sont placées aux points où le bruit atteint un maximum local sur le contour :

```java
public List<TowerPos> findTowerPositions() {
    List<TowerPos> towers = new ArrayList<>();
    for (int theta = 0; theta < 360; theta += 5) {
        double rad = Math.toRadians(theta);
        double nx = Math.cos(rad);
        double nz = Math.sin(rad);
        double noise = wallNoise.noise2(nx * 3.0, nz * 3.0);

        // Maximum local : tour
        double prev = wallNoise.noise2(Math.cos(rad-0.1)*3, Math.sin(rad-0.1)*3);
        double next = wallNoise.noise2(Math.cos(rad+0.1)*3, Math.sin(rad+0.1)*3);
        if (noise > prev && noise > next && noise > 0.2) {
            int dx = (int)(nx * baseRadius);
            int dz = (int)(nz * baseRadius);
            towers.add(new TowerPos(dx, dz, 3 + (int)(noise * 3))); // rayon tour 3-5
        }
    }
    return towers;
}
```

---

## 2. Ailes organiques

Les ailes (`DungeonArchitect` construit des ailes latérales) ne sont plus des rectangles fixés mais des **extensions bruitées** du footprint principal, positionnées là où le bruit sur le pourtour dépasse un second seuil :

```java
// Dans DungeonArchitect.buildFloor, remplacer la construction d'ailes
if (role != Role.BOSS) {
    OrganicAileBuilder.build(lv, sp, shell, t, floor, rng);
}
```

---

## 3. Modifications dans `DungeonArchitect`

### Méthodes impactées

| Méthode | Changement |
|---|---|
| `buildFloor()` | Remplacer `air(...)` par `shell.clearInterior()` |
| `buildWalls()` | Remplacer par `shell.buildWalls()` |
| `buildTowers()` | Remplacer par `shell.findTowerPositions()` + `buildTowerAt()` |
| `HX`, `HZ` constantes |  Dépréciées (conservées pour compat Phase 2) mais plus utilisées |
| `fill()`, `air()` helpers | Conservés inchangés |

### Nouvelle dépendance : `IslandShaper`
`OrganicFortressShell` prend un `IslandShaper` pour garantir que l'empreinte forteresse reste à l'intérieur de l'île :

```java
public boolean isInsideFootprint(int dx, int dz) {
    // Vérifier d'abord qu'on est dans l'île
    if (!shaper.isInside(dx, dz)) return false;
    // ... détermination de l'empreinte forteresse
}
```

---

## 4. `IslandTerrainShaper` — Adaptation

Actuellement `rimHeightAt` rend 0 pour `|dx|≤HX && |dz|≤HZ`. Le nouveau test devient :

```java
private final OrganicFortressShell shell;

public int rimHeightAt(int dx, int dz) {
    // L'intérieur de la forteresse (empreinte organique) reste plat
    if (shell.isInsideFootprint(dx, dz)) return 0;
    // ... reste inchangé (taper + noise)
}
```

Cela nécessite que `IslandTerrainShaper` reçoive une instance de shell, ou que l'empreinte soit testable via une interface.

---

## 5. Tests

| Test | Description |
|---|---|
| `footprintInsideIsland` | Toute cellule footprint est sur l'île |
| `footprintNonEmpty` | Au moins 40% de l'île est forteresse |
| `wallsAreClosed` | Le contour des murs est une boucle fermée |
| `noGapsInWalls` | Pas de trou dans les murs (test de connectivité) |
| `towersOnPerimeter` | Toutes les tours sont sur le bord du footprint |
| `deterministicShell` | Même seed → même empreinte |

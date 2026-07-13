# Phase 2 — Layout de pièces organique (remplacement grille 5×4)

> **Date** : 2026-07-13
> **Statut** : Design — prêt implémentation
> **Contexte** : `DungeonLayout` utilise une grille fixe 5×4 avec chemin serpentin (boustrophedon). Phase 2 remplace par un placement de pièces organique piloté par OpenSimplex2.
> **Dépendances** : Phase 1 (OpenSimplex2.java disponible dans `tong.statmod.dungeon.noise`)

---

## Résumé

| Composant | Actuel | Cible |
|---|---|---|
| `DungeonLayout` | Grille COLS=5, ROWS=4, 20 pièces | Placement organique par bruit 2D |
| `DungeonLayout.rooms()` | Toujours 20 pièces identiques | Liste variable par étage (12-28) |
| `DungeonLayout.Room` | Bornes définies par la grille | Bornes définies par placement bruit |
| Connexions | Serpentin boustrophedon | Graphe MST + corridors bruités |
| `exitDoor`, `Dir` | Porte unique vers la suivante | 1-3 connexions par pièce (graphe) |

---

## 1. `OrganicRoomLayout` — Nouveau layout organique

Nouvelle classe dans `tong.statmod.dungeon.layout` :

```java
public final class OrganicRoomLayout {
    // -- paramètres de génération --
    /** Taille de cellule en blocs dans le shell forteresse (±HX, ±HZ). */
    private static final int CELL = 24;
    /** Valeurs min/max de pièces par grille. */
    private static final int MIN_ROOMS = 12;
    private static final int MAX_ROOMS = 28;

    /** Room : interface identique à DungeonLayout.Room pour compatibilité. */
    public record OrganicRoom(int index, int col, int row,
                              int minX, int maxX, int minZ, int maxZ,
                              int[] connectedTo,  // indices des pièces voisines
                              boolean isFirst, boolean isLast) {
        public int centerX() { return (minX + maxX) / 2; }
        public int centerZ() { return (minZ + maxZ) / 2; }
    }
}
```

### Algorithme de placement

```
1. Subdiviser la zone forteresse [−HX, HX]×[−HZ, HZ] en cellules de CELL×CELL blocs
   → grille ∼10×9 cellules
2. Pour chaque cellule (cx, cz), échantillonner OpenSimplex2 noise à son centre :
   double v = noise.noise2(cx * 0.1, cz * 0.1);
3. Si v > seuil(floor) → cellule « active » (forme une pièce)
   Le seuil baisse avec la profondeur de l'étage : 
   threshold = 0.1 - floor * 0.001 (plus d'étages → plus de pièces)
4. Cluster les cellules adjacentes actives → régions connexes
   Chaque cluster devient une pièce candidate
5. Si trop peu de clusters (< MIN_ROOMS), baisser le seuil
   Si trop de clusters (> MAX_ROOMS), relever le seuil
6. Assigner chaque cluster à une pièce OrganicRoom :
   - minX/maxX/minZ/maxZ = étendue du cluster
   - centre = centroïde du cluster
7. Calculer le graphe de connexion : deux pièces sont connectées si leurs clusters
   partagent une arête de cellule → connectedTo[]
8. Pièce d'apparition = la plus proche de (0,0) (centre de l'île)
   Pièce de sortie = la plus éloignée de (0,0) (à l'opposé)
9. Chemin principal = Dijkstra de spawn → exit (poids = distance)
```

### Invariants par rapport à `DungeonLayout.Room`
| Invariant | Préservé |
|---|---|
| `minX ≤ maxX`, `minZ ≤ maxZ` | Oui |
| Toutes les cellules de la grille sont couvertes | Non : seules les cellules actives sont utilisées |
| Pièces contiguës adjacentes | Oui (connexion par arête de cellule) |
| `centerX()`/`centerZ()` | Oui |
| Une seule pièce first/last | Oui |

### Changements dans `DungeonRoomChain`

`DungeonRoomChain` référence `DungeonLayout.rooms()`, `DungeonLayout.ROOM_COUNT`, `DungeonLayout.Room`, `DungeonLayout.spawnRoom()`, `exitRoom()`.

**Approche migration** : Créer `OrganicRoomLayout` comme alternative. Modifier `DungeonRoomChain` pour accepter une interface commune :

```java
// Nouvelle interface dans DungeonRoomChain
public interface RoomProvider {
    List<? extends RoomLike> rooms();
    RoomLike spawnRoom();
    RoomLike exitRoom();
    int roomCount();
}
```

Faire implémenter l'interface par `DungeonLayout` (legacy) et `OrganicRoomLayout` (nouveau). Le `build()` de `DungeonRoomChain` prend un `RoomProvider` :

```java
public static void build(ServerLevel lv, BlockPos sp, BlockPalette t, int floor,
                         DungeonArchitect.Role role, Random rng,
                         RoomProvider provider)
```

L'appel dans `DungeonArchitect.buildFloor` utilisera `OrganicRoomLayout.forFloor(floor)`.

### Corridors entre pièces

Au lieu de portes entre cellules voisines de la grille, on crée des **corridors bruités** entre pièces connectées :

```java
// Dans un nouveau builder de corridors
boolean hasCorridor = corridorNoise.noise2(
    (x1 + x2) * 0.5 * 0.05, (z1 + z2) * 0.5 * 0.05) > 0.0;
```

Les corridors sont creusés dans le shell forteresse (PAD→AIR) avec la même largeur (3 blocs) mais un tracé légèrement sinueux.

---

## 2. Tests

| Test | Description |
|---|---|
| `deterministicSameSeed` | Même floor → même layout |
| `differentFloorDifferentLayout` | Floors différents → layouts différents |
| `roomCountInRange` | `12 ≤ rooms.size() ≤ 28` |
| `roomsFitInFortress` | Toutes les pièces dans `[-HX, HX]×[-HZ, HZ]` |
| `pathExistsFromSpawnToExit` | Dijkstra trouve un chemin |
| `firstIsSpawnLastIsExit` | Invariant inchangé |
| `consecutiveRoomsAreAdjacent` | Pièces connectées partagent une arête |

---

## 3. Non-changements (hors scope Phase 2)

- `DungeonRoomChain.shell()` (formes de pièces) — Phase 3
- `DungeonArchitect` (shell forteresse) — Phase 4
- Templates NBT — Phase 5
- IslandShaper, IslandTerrainShaper — Phase 1 (déjà fait)

# Phase 1 — OpenSimplex2 Noise dans IslandShaper / IslandTerrainShaper

> **Date** : 2026-07-13
> **Statut** : Design — prêt implémentation
> **Contexte** : Mission donjon organique — Phase 1 remplace le bruit harmonique sin/cos par du bruit 2D OpenSimplex2 (KdotJPG, domaine public).
> **Dépendances** : Ajout d'un fichier unique `OpenSimplex2S.java` dans `tong.statmod.dungeon.noise`. Aucune dépendance Maven/Gradle.

---

## Résumé

| Composant | Bruit actuel | Bruit cible |
|---|---|---|
| `IslandShaper.radiusAt(theta)` | `sin(3θ) + 0.5·sin(7θ)` | 2D OpenSimplex2 le long d'un cercle |
| `IslandTerrainShaper.rimHeightAt(dx, dz)` | `sin(x·0.11)·cos(z·0.11)` | 2D OpenSimplex2 direct |
| `IslandTerrainShaper.shouldDecorate(dx, dz)` | `sin(x·0.15)·cos(z·0.15)` | 2D OpenSimplex2 threshold |

---

## 1. Ajout : `OpenSimplex2S.java`

**Source** : Implémentation OpenSimplex2S par KdotJPG (domaine public, 2023).
**Package** : `tong.statmod.dungeon.noise`.

Un seul fichier, zéro dépendance. La classe expose :

```java
// Constructeur seedé
public OpenSimplex2S(long seed)

// 2D noise, standard [−1, 1]
public double noise2(double x, double y)
```

Aucune modification. On utilise la variante **S** (smooth) plutôt que **F** (fast) pour son meilleur rendu visuel. La graine est passée au constructeur — chaque `IslandShaper` / `IslandTerrainShaper` aura sa propre instance seedée.

---

## 2. `IslandShaper` — Silhouette organique

### Problème
`radiusAt(theta)` utilise deux harmoniques pures. La silhouette est trop régulière — les caps et baies sont périodiques (3 lobes + 7 sous-lobes). Un œil humain repère la symétrie.

### Solution
Au lieu de `sin(3θ) + 0.5·sin(7θ)`, on échantillonne un bruit 2D le long d'un cercle de rayon unité :

```java
private final OpenSimplex2S noise;

public double radiusAt(double theta) {
    double nx = Math.cos(theta);
    double nz = Math.sin(theta);
    // Bruit 2D sur le cercle unité + détail haute fréquence superposé
    double low = noise.noise2(nx * 2.0, nz * 2.0);   // vague large
    double high = noise.noise2(nx * 5.0 + 100.0, nz * 5.0 + 100.0); // détail fin
    // low ∈ [-1,1], high ∈ [-1,1],  combinaison
    double raw = low * 0.7 + high * 0.3;               // ∈ [-1, 1]
    double normalized = (raw + 1.0) / 2.0;              // ∈ [0, 1]
    return radius * (MIN_FACTOR + AMPLITUDE * normalized);
}
```

Les constantes `2.0` / `5.0` contrôlent la fréquence angulaire :
- `2.0` → ~2 vagues autour du cercle (forme générale)
- `5.0` → ~5 indentations fines (détail de bord de rivage)

Les décalages `+100.0` évitent la corrélation entre octaves (pratique standard).

### Invariants préservés
- `radiusAt(θ) ∈ [MIN_FACTOR·R, 1.0·R]` — inchangé
- Déterministe (même seed → même silhouette) — inchangé
- Centre toujours inside — inchangé (radiusAt n'est jamais nul, MIN_FACTOR > 0)
- `isBoundary` garantit au moins un voisin outside — inchangé (l'algo de boundary ne change pas)
- `depthAt` garde sa formule conique + jitter splitmix64 — inchangé
- `maxDepth` conserve `radius/2 + 2` — inchangé

### Tests impactés
- `sameSeedIsDeterministic` — inchangé, passe toujours
- `differentSeedsGiveDifferentSilhouettes` — inchangé, passe toujours
- `centerIsAlwaysInside` — inchangé
- `nothingOutsideRadiusBound` — inchangé
- `radiusAtStaysWithinModulationBounds` — inchangé, le nouveau bruit est aussi borné
- `boundaryCellsHaveAnOutsideNeighbor` — inchangé
- `depthIsZeroOutsideAndPositiveInside` — inchangé
- `depthIsDeeperAtCenterThanNearEdge` — inchangé
- `seedForIsStablePerFloorAndDistinct` — inchangé

---

## 3. `IslandTerrainShaper` — Relief du pourtour

### Problème
`rimHeightAt(dx, dz)` utilise `sin(x·scale)·cos(z·scale)`. Ce produit crise des motifs en diagonale (quadrants visibles) et l'amplitude n'est pas uniforme en direction.

### Solution
Remplacer par 2D OpenSimplex2 direct :

```java
public int rimHeightAt(int dx, int dz) {
    // Intérieur forteresse → plat
    if (Math.abs(dx) <= fx && Math.abs(dz) <= fz) return 0;

    int outX = Math.max(0, Math.abs(dx) - fx);
    int outZ = Math.max(0, Math.abs(dz) - fz);
    int out = Math.max(outX, outZ);
    double taper = Math.min(1.0, out / (double) TAPER_WIDTH);

    // Bruit 2D organique
    double scale = 0.04;  // ajusté pour OpenSimplex2 (sin·cos utilisait 0.11)
    double raw = rimNoise.noise2(dx * scale, dz * scale); // [-1, 1]
    double noise = (raw + 1.0) / 2.0;                     // [0, 1]

    return (int) Math.round(RIM_MAX_HEIGHT * noise * taper);
}
```

**Pourquoi `scale = 0.04` ?** OpenSimplex2 a un contenu fréquentiel différent de `sin·cos` (plus riche, plus naturel). Un scale plus bas donne la même densité visuelle de collines.

### `shouldDecorate` — Placement des décors
Remplacer le sin·cos par un seuil sur le bruit 2D (même instance) :

```java
private boolean shouldDecorate(int dx, int dz) {
    double raw = rimNoise.noise2(dx * 0.05, dz * 0.05);
    return raw > 0.15; // ~42% des sommets, similaire à l'ancien seuil
}
```

### Invariants préservés
- `neverTouchesFortressFootprint()` (test) — préservé (le check `|dx|≤HX && |dz|≤HZ` → return 0)
- `heightNeverNegative()` (test) — préservé (noise ∈ [-1,1] → raw+1)/2 ∈ [0,1])
- `heightBoundedByMax()` (test) — préservé (RIM_MAX_HEIGHT = 4 cap)
- `deterministicForSameSeed()` (test) — préservé (OpenSimplex2 est déterministe)
- `taperRisesFromWall()` (test) — préservé (la fonction `taper` est inchangée)

---

## 4. `rimNoise` — Instance de bruit partagée

Une seule instance `OpenSimplex2S` par `IslandTerrainShaper`, seedée avec le seed de l'étage. Créée dans le constructeur.

```java
private final OpenSimplex2S rimNoise;

public IslandTerrainShaper(long seed, int floor, int radius) {
    this.seed = seed;
    this.shaper = new IslandShaper(seed, radius);
    this.rimNoise = new OpenSimplex2S(seed);
}
```

---

## 5. Tests additionnels

### Nouveaux tests dans `IslandShaperTest`

| Test | Description |
|---|---|
| `silhouetteHasAngularVariation` | Vérifie que `radiusAt` n'est pas constant : `max(radiusAt(θ)) - min(radiusAt(θ)) > AMPLITUDE * R * 0.5` |
| `noiseIsContinuous` | Vérifie que `radiusAt(θ)` et `radiusAt(θ+ε)` diffèrent de moins de 0.1 (lissage) |

### Nouveaux tests dans `IslandTerrainShaperTest`

| Test | Description |
|---|---|
| `heightVariesOrganically` | Deux colonnes proches ont des hauteurs corrélées (pas de sel pur) ; deux colonnes éloignées ont des hauteurs décorrélées |

---

## 6. Stratégie de déploiement

1. Copier `OpenSimplex2S.java` (KdotJPG) dans `src/main/java/tong/statmod/dungeon/noise/`
2. Modifier `IslandShaper.java` : ajouter champ `OpenSimplex2S`, remplacer `radiusAt`
3. Modifier `IslandTerrainShaper.java` : ajouter `rimNoise`, remplacer `rimHeightAt` + `shouldDecorate`
4. Lancer les tests existants (25 tests dans le package dungeon)
5. Vérifier le build complet (`./gradlew build`)

---

## 7. Non-changements (hors scope Phase 1)

- `DungeonLayout` (grille 5×4) — Phase 5
- `DungeonRoomChain` (pièces rectangulaires) — Phase 5
- `DungeonArchitect` (forteresse shell) — Phase 5
- `DungeonBossArena` (arène boss) — Phase 5
- `IslandShaper.depthAt` / `maxDepth` — inchangé
- `seedFor` (splitmix64) — inchangé

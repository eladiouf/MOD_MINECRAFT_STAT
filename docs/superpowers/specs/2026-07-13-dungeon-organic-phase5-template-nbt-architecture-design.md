# Phase 5 — Architecture templates NBT (inspiration wild-dungeons)

> **Date** : 2026-07-13
> **Statut** : Design — prêt implémentation
> **Contexte** : `DungeonRoomChain`, `DungeonDetailing`, `DungeonProps`, `DungeonTraps`, etc. placent le contenu des pièces avec du code Java en dur (coffres, piliers, étagères, pièges, fontaines). Phase 5 ajoute un système de templates NBT permettant de charger/déposer des structures préfabriquées (format `.nbt`), avec un système de connexion, remplacement de matériaux, et sélection pondérée par bruit — inspiré de l'architecture de wild-dungeons (danielkrafft).
> **Dépendances** : Phases 1-4. Aucune nouvelle dépendance Gradle (NBT = format natif Minecraft).

---

## Résumé

| Concept wild-dungeons | Équivalent Phase 5 |
|---|---|
| `DungeonTemplate` | `FloorTemplate` (NBT chargé depuis `data/statmod/dungeon/template/`) |
| `FloorTemplate` | Non applicable (1 template = tout l'étage) |
| `BranchTemplate` | `RoomTemplate` (NBT d'une salle individuelle) |
| `RoomTemplate` | Pièce NBT placée dans une cellule/pièce organique |
| `ConnectionPoint` | Même concept — points d'entrée/sortie de template |
| `DungeonMaterial` | `DungeonMaterial` — mapping blocs palette → matériaux de l'étage |
| `HierarchicalProperty` | `TemplateProperty` — cascade étage→salle→élément |
| `WeightedPool` | `WeightedPool` — sélection probabiliste de templates |
| Registres (`HashMap<String, T>`) | `TemplateRegistry` — lookup par nom |

---

## 1. `RoomTemplate` — Template de salle NBT

### Format NBT

Fichiers dans `data/statmod/dungeon/template/room/` :

```
data/statmod/dungeon/template/
├── room/
│   ├── combat/
│   │   ├── barracks.nbt
│   │   ├── library.nbt
│   │   ├── armory.nbt
│   │   ├── treasury.nbt
│   │   ├── prison.nbt
│   │   ├── crypt.nbt
│   │   ├── forge.nbt
│   │   └── arena_void.nbt
│   ├── treasure/
│   │   ├── vault_small.nbt
│   │   ├── vault_large.nbt
│   │   └── shrine.nbt
│   └── boss/
│       ├── boss_arena_circle.nbt
│       ├── boss_arena_columns.nbt
│       └── boss_throne.nbt
└── corridor/
    ├── straight.nbt
    ├── corner.nbt
    └── junction.nbt
```

Chaque fichier NBT utilise le format standard Minecraft **BlockEntity** (comme une structure sauvegardée avec `/save structure`). La structure NBT :

```nbt
{
  format_version: 1,
  size: [15, 8, 15],          // dimensions (x, y, z)
  blocks: [Byte],              // palette indices
  palette: [Compound],         // block states palette
  entities: [List],            // entities à placer
  connection_points: [         // points d'entrée/sortie (optionnel)
    { rel_x: 7, rel_y: 0, rel_z: 0, type: "entrance", pool: "main" },
    { rel_x: 7, rel_y: 0, rel_z: 14, type: "exit", pool: "main" }
  ],
  weight: 1.0,                 // poids pour sélection aléatoire
  properties: {                 // propriétés hiérarchiques
    "difficulty": "medium",
    "light_level": 0.7,
    "mob_density": 1.0
  }
}
```

### Classe Java

```java
// tong.statmod.dungeon.template
public final class RoomTemplate {
    private final int width, height, depth;       // dimensions
    private final int[] blocks;                   // indices palette
    private final List<TemplateBlockState> palette;
    private final List<ConnectionPoint> connections;
    private final double weight;
    private final Map<String, String> properties;
    private final ResourceLocation id;

    // Chargement depuis NBT
    public static RoomTemplate load(ResourceLocation id, CompoundTag tag) { ... }

    // Placement dans le monde (avec remplacement de matériaux)
    public void place(ServerLevel lv, BlockPos origin, Direction rotation,
                      DungeonMaterial material, TemplateProperty props) { ... }
}
```

---

## 2. `ConnectionPoint` — Points d'entrée/sortie

```java
// tong.statmod.dungeon.template
public record ConnectionPoint(
    int relX, int relY, int relZ,
    String type,       // "entrance", "exit", "corridor", "balcony", "secret"
    String pool        // "main", "side", "boss", "treasure"
) {
    public BlockPos worldPos(BlockPos origin, Direction rotation) {
        // Appliquer la rotation au point relatif
        return origin.offset(rotateX(relX, relZ, rotation), relY, rotateZ(relX, relZ, rotation));
    }
}
```

### Scoring de connexion (inspiré wild-dungeons)
Quand on connecte deux templates, un score évalue la compatibilité :

```java
public interface ConnectionScorer {
    double score(ConnectionPoint a, ConnectionPoint b);
}

// Scoring de base : même type et même pool
public double score(ConnectionPoint a, ConnectionPoint b) {
    if (!a.type().equals(b.type())) return 0.0;
    if (!a.pool().equals(b.pool())) return 0.25;
    return 1.0;
}
```

---

## 3. `DungeonMaterial` — Remplacement contextuel de blocs

Inspiré de wild-dungeons : 7 catégories de matériaux, remplacés au placement selon la palette de l'étage.

```java
// tong.statmod.dungeon.template
public record DungeonMaterial(
    Block wall,              // murs solides
    Block floor,             // sol
    Block ceiling,           // plafond
    Block support,           // piliers, colonnes
    Block decoration,        // décor (toiles, tapisseries)
    Block light,             // sources lumineuses
    Block special            // blocs spéciaux (spawners, coffres, autels)
) {
    // Création depuis une BlockPalette existante
    public static DungeonMaterial fromPalette(BlockPalette t) {
        return new DungeonMaterial(
            t.base(),           // wall
            t.base(),           // floor
            t.ceiling(),        // ceiling
            t.decorPrimary(),   // support
            t.accent(),         // decoration
            t.light(),          // light
            t.scar()            // special
        );
    }

    // Remplacement d'un bloc template → bloc réel
    public Block resolve(String templateCategory) {
        return switch (templateCategory) {
            case "wall" -> wall;
            case "floor" -> floor;
            case "ceiling" -> ceiling;
            case "support" -> support;
            case "decoration" -> decoration;
            case "light" -> light;
            case "special" -> special;
            default -> wall;
        };
    }
}
```

Les templates NBT utilisent des **blocs abstraits** dans leur palette (ex: `statmod:template/wall`, `statmod:template/floor`, `statmod:template/light`) qui sont résolus en blocs réels au placement via `DungeonMaterial.resolve()`.

---

## 4. `TemplateProperty` — Propriétés hiérarchiques

Inspiré des `HierarchicalProperty` de wild-dungeons : une cascade de remplacement étage → salle → élément.

```java
// tong.statmod.dungeon.template
public class TemplateProperty {
    private final Map<String, String> values;

    // Propriétés avec valeur par défaut
    public String get(String key, String defaultValue) { ... }
    public int getInt(String key, int defaultValue) { ... }
    public double getDouble(String key, double defaultValue) { ... }

    // Fusion : child surcharge parent
    public static TemplateProperty merge(TemplateProperty parent, TemplateProperty child) {
        Map<String, String> merged = new HashMap<>(parent.values);
        merged.putAll(child.values);
        return new TemplateProperty(merged);
    }
}
```

Hiérarchie :
```
Étage (floor) → RoomTemplate → Élément individuel dans le template
  ↓                ↓                   ↓
"tier=early"    "mob=zombie"      "chest_loot=village"
```

---

## 5. `WeightedPool` — Sélection probabiliste

```java
// tong.statmod.dungeon.template
public class WeightedPool<T> {
    private final List<Entry<T>> entries;
    private final OpenSimplex2S noise; // bruit pour sélection déterministe

    public record Entry<T>(T value, double weight) {}

    public T select(long seed, double x, double y) {
        // Mélanger par bruit plutôt que Random.nextDouble
        double noiseVal = (noise.noise2(x, y) + 1.0) / 2.0; // [0, 1]
        double totalWeight = entries.stream().mapToDouble(Entry::weight).sum();
        double target = noiseVal * totalWeight;
        double cumulative = 0.0;
        for (Entry<T> entry : entries) {
            cumulative += entry.weight;
            if (target <= cumulative) return entry.value();
        }
        return entries.get(entries.size() - 1).value();
    }
}
```

Inspiré de `WeightedPool.getNoisyRandom()` de wild-dungeons (SimplexNoise) — la sélection est déterministe mais naturelle.

---

## 6. `TemplateRegistry` — Registre de templates

```java
// tong.statmod.dungeon.template
public final class TemplateRegistry {
    private static final Map<ResourceLocation, RoomTemplate> ROOMS = new HashMap<>();
    private static final Map<String, WeightedPool<ResourceLocation>> POOLS = new HashMap<>();

    public static void register(ResourceLocation id, RoomTemplate template) { ... }
    public static RoomTemplate get(ResourceLocation id) { ... }
    public static RoomTemplate select(String pool, long seed, double x, double y) {
        return get(POOLS.get(pool).select(seed, x, y));
    }

    // Chargement au démarrage (FMLCommonSetupEvent / RegisterEvent)
    public static void loadTemplates() {
        // Scanner data/statmod/dungeon/template/room/ pour tous les *.nbt
        // Les charger, les enregistrer, et construire les pools
    }
}
```

---

## 7. Intégration avec `DungeonRoomChain`

### Nouveau flux pour une pièce de combat

```java
// Dans DungeonRoomChain.build() — une pièce de combat
private static void buildCombatRoomFromTemplate(
    ServerLevel lv, BlockPos sp, BlockPalette t,
    OrganicRoomLayout.OrganicRoom r, int floor) {

    // 1. Sélection du template
    ResourceLocation templateId = TemplateRegistry.select(
        "combat", floor, r.centerX(), r.centerZ());
    RoomTemplate template = TemplateRegistry.get(templateId);

    // 2. Création du matériau depuis la palette
    DungeonMaterial material = DungeonMaterial.fromPalette(t);

    // 3. Propriétés hiérarchiques
    TemplateProperty props = new TemplateProperty(Map.of(
        "tier", FloorPalette.forFloor(floor).name(),
        "floor", String.valueOf(floor)
    ));

    // 4. Placement du template
    BlockPos origin = sp.offset(r.minX(), 0, r.minZ());
    template.place(lv, origin, Direction.NORTH, material, props);
}
```

### Pour les corridors
Les corridors entre pièces utilisent aussi des templates :

```java
// Au lieu de drawPathSegment() avec des blocs posés un par un
private static void buildCorridorFromTemplate(
    ServerLevel lv, BlockPos sp, BlockPalette t,
    OrganicRoomLayout.OrganicRoom from, OrganicRoom to, int floor) {

    ResourceLocation corrId = TemplateRegistry.select(
        "corridor_straight", floor, from.centerX(), from.centerZ());
    RoomTemplate corr = TemplateRegistry.get(corrId);
    DungeonMaterial material = DungeonMaterial.fromPalette(t);

    // Placer le corridor entre les deux centres de pièce
    BlockPos mid = sp.offset(
        (from.centerX() + to.centerX()) / 2,
        0,
        (from.centerZ() + to.centerZ()) / 2);
    corr.place(lv, mid, Direction.NORTH, material, TemplateProperty.EMPTY);
}
```

---

## 8. Génération des templates NBT

Les templates sont créés de deux façons :

1. **Manuellement avec `/place template`** : construire une salle dans le jeu, la sauvegarder avec `/save structure statmod:dungeon/template/room/combat/barracks`, ajouter les `connection_points` manuellement (via script datafixer ou éditeur NBT).

2. **Générés par code de procédure** : un `RoomTemplateGenerator` en Java peut générer des templates NBT à chaud si un fichier NBT est manquant, en utilisant les anciennes méthodes de `DungeonRoomChain` comme fallback :

```java
public class RoomTemplateGenerator {
    public static CompoundTag generateFallback(int w, int d, int h, BlockPalette t) {
        // Produit un CompoundTag NBT compatible avec le format RoomTemplate
        // en utilisant les anciennes méthodes de DungeonRoomChain
    }
}
```

---

## 9. `DungeonRoomChain` — Refactoring global

| Méthode | Devenir |
|---|---|
| `shell()` | Devient optionnelle (si template trouvé → utiliser le template ; sinon → shell procédural) |
| `shell()` avec `NoiseShapeRoom` | Devient le fallback procédural quand aucun template NBT n'est trouvé |
| `carveDoor()` | Remplacé par `ConnectionPoint` matching entre templates |
| `pillarHall()`, `centralDais()`, etc. | Devenu contenu possible des templates |
| `archiveRoom()`, `forgeRoom()`, etc. | Devenu contenu possible des templates |
| `spawnPad()`, `exitRoom()`, `treasureRoom()` | Conservés (trop spécifiques pour un template générique) |
| `lightRoom()` | Conservé (éclairage adaptatif post-placement) |
| `buildMezzaninesAndBridges()` | Conservé (détection de hauteur) |

---

## 10. Tests

| Test | Description |
|---|---|
| `templateLoadsCorrectly` | Chargement d'un NBT → dimensions + palette correctes |
| `templatePlacesBlocks` | Placement dans un ServerLevel factice |
| `materialReplacesCorrectly` | `statmod:template/wall` → block réel |
| `connectionPointsMatch` | Deux templates connectés ont des points alignés |
| `weightedPoolSelection` | Templates plus lourds sélectionnés plus souvent |
| `propertyCascade` | Propriété enfant surcharge parent |
| `registryLookup` | Enregistrement + lookup par ResourceLocation |

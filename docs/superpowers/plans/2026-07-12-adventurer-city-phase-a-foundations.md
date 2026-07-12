# Cité des Aventuriers — Plan A : Fondations — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer le temple 64×64 de l'étage 0 par la caverne-cité 600×600 praticable : coque (sol/remparts/plafond à cristaux), Grande Place avec tous les services rebranchés, Porte du Donjon monumentale qui s'ouvre à l'approche, Cour des Portails, génération étagée en tâche de fond, mort sans pénalité.

**Architecture:** Nouveau package `tong.statmod.dungeon.city`. `CityPlan` = géométrie pure testable (centre `(0,100,-500)`, rayon 300, tout dans la zone étage 0 `z<-150`). `CityBuildQueue` = file de jobs pure testable, drainée à budget fixe par tick serveur. `CityGenerator` = orchestrateur (`ServerStartedEvent` + `ServerTickEvent.Post`), marqueur d'achèvement en `SavedData` versionné. Les builders (`CityShell`, `PlazaBuilder`, `ArtisanCampBuilder`, `DungeonGateBuilder`, `PortalCourtBuilder`) enfilent des jobs. Aucun schematic — 100 % code, déterministe.

**Tech Stack:** NeoForge 1.21.1, JDK 21, mappings Mojang, JUnit Jupiter 5.10. Réutilise `DungeonBlocks`, `DungeonMerchant`, `MagicBanker`, `WaystonesBridge`, `FloorPalette`.

**Spec:** `docs/superpowers/specs/2026-07-12-adventurer-city-floor0-design.md` (§2, §3 partiel, §6)

---

## Contexte pour un exécutant sans historique

- Le donjon vit dans la dimension void `statmod:trial_dungeon`. Les étages 1+ sont une grille XZ
  (`DungeonTeleportHandler.FLOOR_SPACING=300`), l'étage 0 est **tout** `z < -150`
  (`DungeonTeleportHandler.floorAtPos`).
- L'étage 0 actuel = île R=148 en `(0,100,-300)` avec un temple (`DungeonHubFloor`) + une **cage
  de blocs barrière** (`DungeonBarrierCage`) — les deux sont DANS l'emprise de la future cité et
  doivent être nettoyés sur les mondes existants.
- Convention verticale : sol plein à Y=100, **pieds du joueur à Y=101**.
- Style de code : helpers statiques `B/S/O/fill/air/column` (voir `DungeonArchitect.java:48-72`),
  commentaires métier en français, indentation 4 espaces, SLF4J via `STATMod.LOGGER`.
- Les tests sont du JUnit pur : **jamais** de `ServerLevel`/`Mob` (pas d'infra GameTest). On teste
  les cœurs purs (`CityPlan`, `CityBuildQueue`).
- Build : `./gradlew build` ; tests : `./gradlew test --tests "tong.statmod.dungeon.city.*"`.

### Fichiers créés / modifiés (vue d'ensemble)

| Action | Fichier | Responsabilité |
|---|---|---|
| Create | `src/main/java/tong/statmod/dungeon/city/CityPlan.java` | Géométrie pure (constantes, zones, prédicats) |
| Create | `src/main/java/tong/statmod/dungeon/city/CityBuildQueue.java` | File de jobs à budget, pure |
| Create | `src/main/java/tong/statmod/dungeon/city/CitySavedData.java` | Marqueur « cité construite » versionné |
| Create | `src/main/java/tong/statmod/dungeon/city/CityShell.java` | Sol, remparts, plafond, cristaux |
| Create | `src/main/java/tong/statmod/dungeon/city/PlazaBuilder.java` | Grande Place (fontaine, statue, cristal, services) |
| Create | `src/main/java/tong/statmod/dungeon/city/ArtisanCampBuilder.java` | Camp provisoire (enchant/forge/dortoir) — évite toute régression de service |
| Create | `src/main/java/tong/statmod/dungeon/city/DungeonGateBuilder.java` | Porte du Donjon + 4 statues + téléporteur |
| Create | `src/main/java/tong/statmod/dungeon/city/CityGateOpener.java` | Ouverture de la porte à l'approche (tick) |
| Create | `src/main/java/tong/statmod/dungeon/city/PortalCourtBuilder.java` | Cour des Portails (1 actif retour, 4 scellés) |
| Create | `src/main/java/tong/statmod/dungeon/city/CityGenerator.java` | Orchestrateur : file, ticks, SavedData, regen, nettoyage legacy |
| Modify | `src/main/java/tong/statmod/dungeon/IslandGenerator.java` | Étage 0 → délègue à `CityGenerator` |
| Modify | `src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java` | Spawn étage 0 → place ; entrée étage 0 sans vague/rush/records |
| Modify | `src/main/java/tong/statmod/dungeon/DungeonRespawnHandler.java` | Mort étage 0 : zéro pénalité, respawn place |
| Modify | `src/main/java/tong/statmod/dungeon/DungeonCommands.java` | `regen 0` autorisé |
| Modify | `src/main/java/tong/statmod/dungeon/DungeonArchitect.java` | Retrait du chemin `Role.HUB` |
| Delete | `src/main/java/tong/statmod/dungeon/DungeonHubFloor.java` | Remplacé par la cité |
| Modify | `src/main/resources/assets/statmod/lang/en_us.json` + `fr_fr.json` | 3 clés `dungeon.city.*` |
| Test | `src/test/java/tong/statmod/dungeon/city/CityPlanTest.java` | Invariants géométriques |
| Test | `src/test/java/tong/statmod/dungeon/city/CityBuildQueueTest.java` | Budget/progression/idempotence |

---

### Task 1: CityPlan — géométrie pure

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityPlan.java`
- Test: `src/test/java/tong/statmod/dungeon/city/CityPlanTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import tong.statmod.dungeon.DungeonTeleportHandler;

import static org.junit.jupiter.api.Assertions.*;

class CityPlanTest {

    /** Toute l'emprise de la cité est dans la zone étage 0 (z < -150) — jamais sur la grille 1+. */
    @Test
    void entireCityIsInFloorZeroZone() {
        for (int x = -CityPlan.RADIUS; x <= CityPlan.RADIUS; x += 25) {
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z += 25) {
                if (!CityPlan.inCity(x, z)) continue;
                assertEquals(0, DungeonTeleportHandler.floorAtPos(x, z),
                        "(" + x + "," + z + ") doit être étage 0");
            }
        }
    }

    @Test
    void playerSpawnIsInsidePlaza() {
        BlockPos sp = CityPlan.playerSpawn();
        assertTrue(CityPlan.inPlaza(sp.getX(), sp.getZ()));
        assertEquals(CityPlan.GROUND_Y + 1, sp.getY());
    }

    /** La Porte du Donjon est au sud (z max) de la cité, dans l'emprise. */
    @Test
    void gateIsAtSouthEdge() {
        BlockPos gate = CityPlan.gateCenter();
        assertTrue(CityPlan.inCity(gate.getX(), gate.getZ()));
        assertTrue(gate.getZ() > CityPlan.CENTER_Z + CityPlan.WALL_INNER - 40,
                "la porte perce le rempart sud");
    }

    /** L'ouverture de la porte troue bien le rempart ; ailleurs le rempart est plein. */
    @Test
    void gateOpeningPiercesWallOnlyAtSouth() {
        BlockPos gate = CityPlan.gateCenter();
        assertTrue(CityPlan.inGateOpening(gate.getX(), gate.getZ()));
        assertFalse(CityPlan.inGateOpening(CityPlan.CENTER_X, CityPlan.CENTER_Z - CityPlan.WALL_INNER - 2),
                "pas d'ouverture au nord");
        assertTrue(CityPlan.inWallRing(CityPlan.CENTER_X, CityPlan.CENTER_Z - CityPlan.RADIUS + 4));
    }

    /** L'avenue sud relie la place à la porte (continuité tous les 10 blocs). */
    @Test
    void southAvenueConnectsPlazaToGate() {
        for (int z = CityPlan.CENTER_Z + CityPlan.PLAZA_RADIUS + 1;
             z < CityPlan.CENTER_Z + CityPlan.WALL_INNER - 4; z += 10) {
            assertTrue(CityPlan.onAvenue(CityPlan.CENTER_X, z),
                    "avenue sud interrompue en z=" + z);
        }
    }

    /** Les avenues ne mordent ni sur la place ni sur le rempart. */
    @Test
    void avenuesStayBetweenPlazaAndWall() {
        assertFalse(CityPlan.onAvenue(CityPlan.CENTER_X, CityPlan.CENTER_Z + 10));
        assertFalse(CityPlan.onAvenue(CityPlan.CENTER_X, CityPlan.CENTER_Z + CityPlan.RADIUS - 1));
    }

    /** La cour des portails et le camp des artisans sont dans la cité, hors de la place. */
    @Test
    void annexesAreInsideCityOutsidePlaza() {
        for (BlockPos p : new BlockPos[]{CityPlan.portalCourt(), CityPlan.artisanCamp()}) {
            assertTrue(CityPlan.inCity(p.getX(), p.getZ()));
            assertFalse(CityPlan.inPlaza(p.getX(), p.getZ()));
        }
    }

    /** L'ancienne île hub (0,-300) est bien dans l'emprise (elle sera nettoyée/écrasée). */
    @Test
    void legacyHubIsInsideCityFootprint() {
        assertTrue(CityPlan.inCity(0, -300));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityPlanTest"`
Expected: FAIL — `CityPlan` n'existe pas (erreur de compilation).

- [ ] **Step 3: Write the implementation**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;

/**
 * Cité des Aventuriers (Étage 0) — géométrie PURE de la ville (aucun ServerLevel).
 *
 * <p>La cité est un disque de rayon {@value #RADIUS} centré en ({@value #CENTER_X},
 * {@value #CENTER_Z}), entièrement dans la zone étage 0 du donjon (z &lt; -150 →
 * {@code floorAtPos} = 0). Sol plein à Y={@value #GROUND_Y}, pieds du joueur à +1.
 * Toutes les positions sont déterministes : mêmes constantes → même ville.
 */
public final class CityPlan {

    public static final int CENTER_X = 0;
    public static final int CENTER_Z = -500;
    /** Y du bloc de sol : le joueur marche à GROUND_Y+1. */
    public static final int GROUND_Y = 100;
    /** Rayon extérieur de la caverne-cité. */
    public static final int RADIUS = 300;
    /** Rayon intérieur du rempart périmétral (épaisseur RADIUS-WALL_INNER). */
    public static final int WALL_INNER = 292;
    /** Sommet du rempart. */
    public static final int WALL_TOP_Y = 140;
    /** Plafond de la caverne (dalle + cristaux suspendus dessous). */
    public static final int CEILING_Y = 170;
    /** Rayon de la Grande Place. */
    public static final int PLAZA_RADIUS = 45;
    /** Demi-largeur des avenues radiales. */
    public static final double AVENUE_HALF_WIDTH = 3.5;

    private CityPlan() {}

    public static BlockPos center() { return new BlockPos(CENTER_X, GROUND_Y, CENTER_Z); }

    /** Spawn joueur : bord sud de la Grande Place, face au cœur de la cité. */
    public static BlockPos playerSpawn() {
        return new BlockPos(CENTER_X, GROUND_Y + 1, CENTER_Z + PLAZA_RADIUS - 6);
    }

    /** Porte du Donjon : perce le rempart SUD (côté grille des étages 1+). */
    public static BlockPos gateCenter() {
        return new BlockPos(CENTER_X, GROUND_Y, CENTER_Z + WALL_INNER - 12);
    }

    /** Cour des Portails : à l'ouest de la place. */
    public static BlockPos portalCourt() {
        return new BlockPos(CENTER_X - 100, GROUND_Y, CENTER_Z);
    }

    /** Camp des artisans (provisoire, plan A) : à l'est de la place. */
    public static BlockPos artisanCamp() {
        return new BlockPos(CENTER_X + 100, GROUND_Y, CENTER_Z);
    }

    public static boolean inCity(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        return dx * dx + dz * dz <= (long) RADIUS * RADIUS;
    }

    public static boolean inPlaza(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        return dx * dx + dz * dz <= (long) PLAZA_RADIUS * PLAZA_RADIUS;
    }

    /** Anneau du rempart périmétral (plein, sauf ouverture de la porte). */
    public static boolean inWallRing(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        long d2 = dx * dx + dz * dz;
        return d2 <= (long) RADIUS * RADIUS && d2 >= (long) WALL_INNER * WALL_INNER;
    }

    /** Couloir de la Porte du Donjon dans le rempart sud (largeur 13). */
    public static boolean inGateOpening(int x, int z) {
        return Math.abs(x - CENTER_X) <= 6 && z >= CENTER_Z + WALL_INNER - 24;
    }

    /**
     * 6 avenues radiales de la place au rempart. Angle 90° = sud (vers la porte),
     * puis une avenue tous les 60°.
     */
    public static boolean onAvenue(int x, int z) {
        double dx = x - CENTER_X, dz = z - CENTER_Z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= PLAZA_RADIUS || dist >= WALL_INNER) return false;
        double angle = Math.toDegrees(Math.atan2(dz, dx)); // sud (+z) = +90°
        for (int k = 0; k < 6; k++) {
            double a = 90.0 + k * 60.0;
            double diff = Math.abs(normalizeDeg(angle - a));
            if (diff < 90.0 && dist * Math.sin(Math.toRadians(diff)) <= AVENUE_HALF_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private static double normalizeDeg(double a) {
        a %= 360.0;
        if (a > 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityPlanTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityPlan.java src/test/java/tong/statmod/dungeon/city/CityPlanTest.java
git commit -m "feat(city): CityPlan - geometrie pure de la Cite des Aventuriers"
```

---

### Task 2: CityBuildQueue — file de jobs à budget

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityBuildQueue.java`
- Test: `src/test/java/tong/statmod/dungeon/city/CityBuildQueueTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package tong.statmod.dungeon.city;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CityBuildQueueTest {

    @Test
    void tickRespectsBudgetAndOrder() {
        CityBuildQueue q = new CityBuildQueue();
        StringBuilder order = new StringBuilder();
        for (char c : "abcde".toCharArray()) q.add(() -> order.append(c));
        assertEquals(2, q.tick(2));
        assertEquals("ab", order.toString());
        assertEquals(3, q.tick(99));
        assertEquals("abcde", order.toString());
        assertEquals(0, q.tick(5), "file vide → 0 job exécuté");
    }

    @Test
    void progressPercentIsMonotonic() {
        CityBuildQueue q = new CityBuildQueue();
        AtomicInteger runs = new AtomicInteger();
        for (int i = 0; i < 4; i++) q.add(runs::incrementAndGet);
        assertEquals(0, q.progressPercent());
        q.tick(1);
        assertEquals(25, q.progressPercent());
        q.tick(3);
        assertEquals(100, q.progressPercent());
        assertTrue(q.isDone());
        assertEquals(4, runs.get());
    }

    @Test
    void emptyQueueIsDoneAt100Percent() {
        CityBuildQueue q = new CityBuildQueue();
        assertTrue(q.isDone());
        assertEquals(100, q.progressPercent());
    }

    @Test
    void clearResetsEverything() {
        CityBuildQueue q = new CityBuildQueue();
        q.add(() -> {});
        q.tick(1);
        q.clear();
        assertTrue(q.isDone());
        assertEquals(0, q.totalJobs());
        assertEquals(0, q.completedJobs());
    }

    /** Un job qui lève ne bloque pas définitivement la file (log + on continue). */
    @Test
    void throwingJobDoesNotStallTheQueue() {
        CityBuildQueue q = new CityBuildQueue();
        AtomicInteger after = new AtomicInteger();
        q.add(() -> { throw new IllegalStateException("boom"); });
        q.add(after::incrementAndGet);
        assertEquals(2, q.tick(2));
        assertEquals(1, after.get());
        assertTrue(q.isDone());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityBuildQueueTest"`
Expected: FAIL — `CityBuildQueue` n'existe pas.

- [ ] **Step 3: Write the implementation**

```java
package tong.statmod.dungeon.city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * File de jobs de construction de la cité, drainée à budget fixe par tick serveur.
 *
 * <p>PURE (aucun import Minecraft) : testable en JUnit. Un job = une unité de travail bornée
 * (une bande de terrain, un monument). Un job qui lève est loggé puis sauté — la construction
 * ne doit jamais se bloquer sur un segment défectueux.
 */
public final class CityBuildQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(CityBuildQueue.class);

    /** Unité de travail bornée (quelques milliers de blocs max). */
    @FunctionalInterface
    public interface Job {
        void run();
    }

    private final List<Job> jobs = new ArrayList<>();
    private int cursor = 0;

    public void add(Job job) { jobs.add(job); }

    public boolean isDone() { return cursor >= jobs.size(); }

    public int totalJobs() { return jobs.size(); }

    public int completedJobs() { return cursor; }

    public int progressPercent() {
        return jobs.isEmpty() ? 100 : (int) (100L * cursor / jobs.size());
    }

    /**
     * Exécute jusqu'à {@code budget} jobs (dans l'ordre d'ajout).
     *
     * @return le nombre de jobs réellement exécutés.
     */
    public int tick(int budget) {
        int ran = 0;
        while (ran < budget && cursor < jobs.size()) {
            Job job = jobs.get(cursor++);
            try {
                job.run();
            } catch (RuntimeException e) {
                LOGGER.error("[City] Job de construction {} en échec — segment sauté", cursor - 1, e);
            }
            ran++;
        }
        return ran;
    }

    public void clear() {
        jobs.clear();
        cursor = 0;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityBuildQueueTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityBuildQueue.java src/test/java/tong/statmod/dungeon/city/CityBuildQueueTest.java
git commit -m "feat(city): CityBuildQueue - file de jobs a budget par tick"
```

---

### Task 3: CitySavedData — marqueur d'achèvement versionné

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CitySavedData.java`

Pas de test JUnit (dépend de `SavedData`/`ServerLevel`) — vérifié en jeu (Task 9).

- [ ] **Step 1: Write the implementation**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Marqueur persistant « la cité est construite », versionné : incrémenter
 * {@link CityGenerator#CITY_VERSION} force une reconstruction au prochain démarrage
 * (utile quand la génération évolue entre deux versions du mod).
 */
public final class CitySavedData extends SavedData {

    private static final String NAME = "statmod_city";
    private static final String TAG_VERSION = "builtVersion";

    private int builtVersion = 0;

    public static CitySavedData get(ServerLevel dungeonLevel) {
        return dungeonLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CitySavedData::new, CitySavedData::load, null), NAME);
    }

    private static CitySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CitySavedData data = new CitySavedData();
        data.builtVersion = tag.getInt(TAG_VERSION);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(TAG_VERSION, builtVersion);
        return tag;
    }

    public int builtVersion() { return builtVersion; }

    public void setBuiltVersion(int version) {
        this.builtVersion = version;
        setDirty();
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CitySavedData.java
git commit -m "feat(city): CitySavedData - marqueur de construction versionne"
```

---

### Task 4: CityShell — sol, remparts, plafond, cristaux

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityShell.java`

Builder `ServerLevel` (pas de test JUnit — les invariants géométriques sont déjà verrouillés par
`CityPlanTest` ; le rendu se vérifie en jeu, Task 9).

- [ ] **Step 1: Write the implementation**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Coque de la caverne-cité : sol (2 épaisseurs), rempart périmétral, dalle de plafond et
 * cristaux lumineux suspendus. Enfilée en bandes de 4 blocs en X pour rester sous le budget
 * par tick. Déterministe (seed fixe pour les cristaux).
 */
final class CityShell {

    /** Flag 2 : pas de neighbor updates — indispensable pour un build de masse. */
    private static final int FLAG = 2;

    private CityShell() {}

    static void enqueue(ServerLevel lv, CityBuildQueue q) {
        for (int x0 = -CityPlan.RADIUS; x0 <= CityPlan.RADIUS; x0 += 4) {
            final int xs = x0;
            final int xe = Math.min(x0 + 3, CityPlan.RADIUS);
            q.add(() -> buildStrip(lv, xs, xe));
        }
        // Cristaux suspendus — positions déterministes (même seed → même ciel).
        Random rng = new Random(0xC17ADE5L);
        for (int i = 0; i < 70; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * (CityPlan.WALL_INNER - 40);
            final int cx = CityPlan.CENTER_X + (int) (Math.cos(angle) * dist);
            final int cz = CityPlan.CENTER_Z + (int) (Math.sin(angle) * dist);
            final int len = 3 + rng.nextInt(6);
            q.add(() -> buildCrystal(lv, cx, cz, len));
        }
    }

    private static void buildStrip(ServerLevel lv, int x0, int x1) {
        BlockState deep = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState pave = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState plazaPave = Blocks.POLISHED_DIORITE.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState ceiling = Blocks.DEEPSLATE_TILES.defaultBlockState();

        for (int x = x0; x <= x1; x++) {
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z++) {
                if (!CityPlan.inCity(x, z)) continue;

                // Sol : soubassement + surface (pavage sur place/avenues, pierre ailleurs).
                lv.setBlock(new BlockPos(x, CityPlan.GROUND_Y - 1, z), deep, FLAG);
                BlockState surface = CityPlan.inPlaza(x, z) ? plazaPave
                        : CityPlan.onAvenue(x, z) ? pave
                        : stone;
                lv.setBlock(new BlockPos(x, CityPlan.GROUND_Y, z), surface, FLAG);

                // Rempart périmétral plein (sauf couloir de la porte).
                if (CityPlan.inWallRing(x, z) && !CityPlan.inGateOpening(x, z)) {
                    for (int y = CityPlan.GROUND_Y + 1; y <= CityPlan.WALL_TOP_Y; y++) {
                        lv.setBlock(new BlockPos(x, y, z), wall, FLAG);
                    }
                }

                // Dalle de plafond.
                lv.setBlock(new BlockPos(x, CityPlan.CEILING_Y, z), ceiling, FLAG);
            }
        }
    }

    /** Stalactite de cristal : gaine d'améthyste, cœur lumineux, pointe en cluster. */
    private static void buildCrystal(ServerLevel lv, int x, int z, int len) {
        for (int i = 0; i < len; i++) {
            int y = CityPlan.CEILING_Y - 1 - i;
            lv.setBlock(new BlockPos(x, y, z), Blocks.AMETHYST_BLOCK.defaultBlockState(), FLAG);
            if (i == len / 2) {
                lv.setBlock(new BlockPos(x + 1, y, z), Blocks.SEA_LANTERN.defaultBlockState(), FLAG);
            }
        }
        lv.setBlock(new BlockPos(x, CityPlan.CEILING_Y - 1 - len, z),
                Blocks.AMETHYST_CLUSTER.defaultBlockState(), FLAG);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityShell.java
git commit -m "feat(city): CityShell - sol, remparts, plafond a cristaux"
```

---

### Task 5: PlazaBuilder + ArtisanCampBuilder — Grande Place et services

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/PlazaBuilder.java`
- Create: `src/main/java/tong/statmod/dungeon/city/ArtisanCampBuilder.java`

- [ ] **Step 1: Vérifier les signatures des services réutilisés**

Run:
```bash
grep -n "public static void placeStall" src/main/java/tong/statmod/dungeon/DungeonMerchant.java
grep -n "public static void spawn" src/main/java/tong/statmod/economy/MagicBanker.java
grep -n "public static .* placeCheckpoint" src/main/java/tong/statmod/integration/waystones/WaystonesBridge.java
```
Expected (usages connus de `DungeonHubFloor` avant suppression) :
`placeStall(ServerLevel, BlockPos sp, int cx, int cz, BlockPalette t, int profil, int prixFloor)`,
`MagicBanker.spawn(ServerLevel, BlockPos)`,
`WaystonesBridge.placeCheckpoint(ServerLevel, BlockPos, FloorPalette, int)`.
Si une signature diffère, adapter les appels des étapes suivantes à la vraie signature.

- [ ] **Step 2: Write PlazaBuilder**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;
import tong.statmod.dungeon.DungeonMerchant;
import tong.statmod.dungeon.FloorPalette;
import tong.statmod.economy.MagicBanker;
import tong.statmod.integration.waystones.WaystonesBridge;

/**
 * Grande Place : fontaine magique, statue du Premier Aventurier, Cristal Gardien,
 * waystone, balise de retour, banquier magique et les 4 étals de marchands
 * (provisoirement sur la place — le plan B les redistribue dans les quartiers).
 */
final class PlazaBuilder {

    private PlazaBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.center().above(); // Y=101 : niveau de marche

        buildFountain(lv, c.offset(0, 0, -18));
        buildFirstAdventurerStatue(lv, c.offset(-18, 0, 0));
        buildGuardianCrystal(lv, c);
        buildLeaderboardWall(lv, c.offset(18, 0, 0));

        // Services fonctionnels (aucune régression vs l'ancien temple).
        WaystonesBridge.placeCheckpoint(lv, c.offset(0, 0, 12), FloorPalette.forFloor(10), 0);
        lv.setBlock(c.offset(-3, 0, 12), DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
        MagicBanker.spawn(lv, c.offset(6, 0, 12));

        // Étals de marchands provisoires aux quatre coins de la place.
        BlockPos sp = CityPlan.center();
        var palette = FloorPalette.forFloor(10);
        DungeonMerchant.placeStall(lv, sp, -30, -30, palette, 0, 10); // armes
        DungeonMerchant.placeStall(lv, sp, -30, 30, palette, 1, 20);  // armures
        DungeonMerchant.placeStall(lv, sp, 30, -30, palette, 2, 30);  // potions
        DungeonMerchant.placeStall(lv, sp, 30, 30, palette, 3, 40);   // archerie
    }

    /** Bassin circulaire r=5 à margelle, jet central lumineux. L'eau est contenue par la margelle. */
    private static void buildFountain(ServerLevel lv, BlockPos c) {
        BlockState rim = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                int d2 = x * x + z * z;
                if (d2 > 25) continue;
                if (d2 >= 16) {
                    lv.setBlock(c.offset(x, 0, z), rim, 3);
                } else {
                    lv.setBlock(c.offset(x, -1, z), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 3);
                    lv.setBlock(c.offset(x, 0, z), water, 3);
                }
            }
        }
        for (int y = 0; y <= 3; y++) {
            lv.setBlock(c.offset(0, y, 0), Blocks.PRISMARINE_WALL.defaultBlockState(), 3);
        }
        lv.setBlock(c.offset(0, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
    }

    /** Statue : socle 5×5, armor stand paré netherite, plaque. */
    private static void buildFirstAdventurerStatue(ServerLevel lv, BlockPos c) {
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                lv.setBlock(c.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 1, 0), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 3);
        ArmorStand stand = new ArmorStand(lv, c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        stand.setShowArms(true);
        stand.setInvulnerable(true);
        lv.addFreshEntity(stand);
    }

    /** Cristal Gardien : pilier d'améthyste flottant au-dessus du centre de la place. */
    private static void buildGuardianCrystal(ServerLevel lv, BlockPos c) {
        for (int y = 8; y <= 14; y++) {
            lv.setBlock(c.offset(0, y, 0), Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
        }
        lv.setBlock(c.offset(0, 11, 0), Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
        lv.setBlock(c.offset(1, 11, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        lv.setBlock(c.offset(-1, 11, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 15, 0), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 7, 0), Blocks.AMETHYST_CLUSTER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING,
                        net.minecraft.core.Direction.DOWN), 3);
    }

    /** Mur des classements : décor en plan A (panneaux réels branchés au plan C). */
    private static void buildLeaderboardWall(ServerLevel lv, BlockPos c) {
        for (int z = -3; z <= 3; z++) {
            for (int y = 0; y <= 3; y++) {
                lv.setBlock(c.offset(0, y, z), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 3);
            }
        }
        lv.setBlock(c.offset(0, 4, 0), Blocks.LANTERN.defaultBlockState(), 3);
    }
}
```

- [ ] **Step 3: Write ArtisanCampBuilder**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Camp des artisans PROVISOIRE (plan A) : regroupe les services utilitaires de l'ancien temple
 * (enchantement niv. 30, forge, dortoir) pour qu'aucun service ne régresse. Le plan B les
 * redistribue dans les vrais quartiers (Elfique / Nain / Humain) et supprime ce camp.
 */
final class ArtisanCampBuilder {

    private ArtisanCampBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.artisanCamp().above(); // Y=101

        // Coin enchantement : table + 15 bibliothèques (niveau 30 garanti).
        BlockPos ench = c.offset(0, 0, -12);
        lv.setBlock(ench, Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
        int placed = 0;
        for (int x = -2; x <= 2 && placed < 15; x++) {
            for (int z = -2; z <= 2 && placed < 15; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;
                lv.setBlock(ench.offset(x, 0, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                if (placed + 1 < 15) lv.setBlock(ench.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                placed += 2;
            }
        }

        // Forge : enclume, table de forge, meule, fourneau, table de craft, coffre de l'Ender.
        BlockPos forge = c.offset(0, 0, 12);
        lv.setBlock(forge, Blocks.ANVIL.defaultBlockState(), 3);
        lv.setBlock(forge.offset(-1, 0, 0), Blocks.SMITHING_TABLE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(1, 0, 0), Blocks.GRINDSTONE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(0, 0, 1), Blocks.BLAST_FURNACE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(-1, 0, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(1, 0, 1), Blocks.ENDER_CHEST.defaultBlockState(), 3);

        // Dortoir : 2 lits sur tapis.
        BlockPos dorm = c.offset(12, 0, 0);
        placeBed(lv, dorm, Direction.NORTH, Blocks.RED_BED.defaultBlockState());
        placeBed(lv, dorm.offset(3, 0, 0), Direction.NORTH, Blocks.BLUE_BED.defaultBlockState());
        lv.setBlock(dorm.offset(1, 0, 2), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void placeBed(ServerLevel lv, BlockPos foot, Direction facing, BlockState bed) {
        lv.setBlock(foot, bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT), 3);
        lv.setBlock(foot.relative(facing),
                bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD), 3);
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL. (Si `DungeonMerchant`/`MagicBanker`/`WaystonesBridge` ont d'autres
signatures — cf. Step 1 — adapter les appels, pas les services.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/PlazaBuilder.java src/main/java/tong/statmod/dungeon/city/ArtisanCampBuilder.java
git commit -m "feat(city): Grande Place + camp des artisans (services rebranches)"
```

---

### Task 6: DungeonGateBuilder + CityGateOpener + PortalCourtBuilder

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/DungeonGateBuilder.java`
- Create: `src/main/java/tong/statmod/dungeon/city/CityGateOpener.java`
- Create: `src/main/java/tong/statmod/dungeon/city/PortalCourtBuilder.java`

- [ ] **Step 1: Write DungeonGateBuilder**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;

/**
 * Porte du Donjon : passage monumental percé dans le rempart sud, flanqué de deux tours et des
 * 4 statues des peuples fondateurs. Les battants (obsidienne) sont ouverts/refermés par
 * {@link CityGateOpener} selon la proximité des joueurs. Le téléporteur vers l'étage 1 est au
 * centre du passage.
 */
final class DungeonGateBuilder {

    /** Demi-largeur du passage (aligné sur CityPlan.inGateOpening : |x| <= 6). */
    static final int HALF_W = 6;
    /** Hauteur du passage. */
    static final int PASSAGE_H = 12;

    private DungeonGateBuilder() {}

    static void build(ServerLevel lv, CityBuildQueue q) {
        BlockPos g = CityPlan.gateCenter(); // Y=100, au seuil du rempart sud
        q.add(() -> buildFrame(lv, g));
        q.add(() -> buildStatues(lv, g));
        q.add(() -> {
            // Téléporteur étage 1 au centre du passage + battants fermés au départ.
            lv.setBlock(g.offset(0, 1, 0), DungeonBlocks.NEXT_FLOOR_TELEPORTER.get().defaultBlockState(), 3);
            CityGateOpener.setDoors(lv, true);
        });
    }

    /** Tours latérales + linteau runique au-dessus du passage. */
    private static void buildFrame(ServerLevel lv, BlockPos g) {
        BlockState tower = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState rune = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState light = Blocks.SOUL_LANTERN.defaultBlockState();

        for (int side : new int[]{-1, 1}) {
            int tx = side * (HALF_W + 3);
            for (int x = tx - 2; x <= tx + 2; x++)
                for (int z = -2; z <= 2; z++)
                    for (int y = 1; y <= PASSAGE_H + 14; y++)
                        lv.setBlock(g.offset(x, y, z), tower, 2);
            lv.setBlock(g.offset(tx, PASSAGE_H + 15, 0), light, 2);
        }
        // Linteau gravé de « runes » au-dessus du passage.
        for (int x = -HALF_W - 1; x <= HALF_W + 1; x++) {
            for (int y = PASSAGE_H + 1; y <= PASSAGE_H + 4; y++) {
                lv.setBlock(g.offset(x, y, 0), (x + y) % 2 == 0 ? rune : tower, 2);
            }
        }
    }

    /** 4 statues des peuples fondateurs, deux de chaque côté de l'approche nord. */
    private static void buildStatues(ServerLevel lv, BlockPos g) {
        record Founder(int dx, int dz, ItemStack head, ItemStack hand) {}
        Founder[] founders = {
                new Founder(-14, -10, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_SWORD)),          // Humain
                new Founder(14, -10, new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.BOW)),                // Elfe
                new Founder(-14, -18, new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_AXE)),  // Nain
                new Founder(14, -18, new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.TRIDENT)),           // Homme-bête
        };
        for (Founder f : founders) {
            BlockPos base = g.offset(f.dx(), 1, f.dz());
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    lv.setBlock(base.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            lv.setBlock(base.above(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
            ArmorStand stand = new ArmorStand(lv, base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5);
            stand.setItemSlot(EquipmentSlot.HEAD, f.head());
            stand.setItemSlot(EquipmentSlot.MAINHAND, f.hand());
            stand.setShowArms(true);
            stand.setInvulnerable(true);
            lv.addFreshEntity(stand);
        }
    }
}
```

- [ ] **Step 2: Write CityGateOpener**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonDimensions;

/**
 * Ouvre lentement la Porte du Donjon quand un joueur approche (« la porte s'ouvre en grondant »)
 * et la referme quand plus personne n'est à proximité. Les battants sont des blocs d'obsidienne
 * dans le passage — pas de redstone (règle du donjon).
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class CityGateOpener {

    private static final int CHECK_EVERY_TICKS = 20;
    private static final double OPEN_RADIUS = 14.0;
    private static final double CLOSE_RADIUS = 24.0;

    private static boolean closed = true;

    private CityGateOpener() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_EVERY_TICKS != 0) return;
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty() || !CityGenerator.isBuilt(lv)) return;

        BlockPos g = CityPlan.gateCenter();
        boolean playerNear = false, playerFar = true;
        for (ServerPlayer p : lv.players()) {
            double d = p.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(g));
            if (d <= OPEN_RADIUS) playerNear = true;
            if (d <= CLOSE_RADIUS) playerFar = false;
        }

        if (closed && playerNear) {
            setDoors(lv, false);
            lv.playSound(null, g, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.5F);
        } else if (!closed && playerFar) {
            setDoors(lv, true);
            lv.playSound(null, g, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
    }

    /** Pose ({@code close=true}) ou retire les battants d'obsidienne du passage. */
    static void setDoors(ServerLevel lv, boolean close) {
        BlockPos g = CityPlan.gateCenter();
        var state = close ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int x = -DungeonGateBuilder.HALF_W; x <= DungeonGateBuilder.HALF_W; x++) {
            for (int y = 1; y <= DungeonGateBuilder.PASSAGE_H; y++) {
                // Battants sur le plan z du seuil, en épargnant la colonne du téléporteur (x=0,y=1).
                if (x == 0 && y == 1) continue;
                lv.setBlock(g.offset(x, y, 3), state, 3);
            }
        }
        closed = close;
    }
}
```

- [ ] **Step 3: Write PortalCourtBuilder**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;

/**
 * Cour des Portails : 5 arches runiques en arc de cercle. Une seule est active (retour
 * overworld — balise de retour au centre), les 4 autres sont scellées d'obsidienne pleureuse
 * (« bientôt » : villes futures, dimensions, événements — crochets v2 du spec §8).
 */
final class PortalCourtBuilder {

    private PortalCourtBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.portalCourt().above(); // Y=101
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(140 + i * 20); // arc orienté vers la place
            BlockPos base = c.offset((int) (Math.cos(angle) * 14), 0, (int) (Math.sin(angle) * 14));
            buildArch(lv, base, i == 2); // seule l'arche centrale est active
        }
    }

    private static void buildArch(ServerLevel lv, BlockPos base, boolean active) {
        BlockState frame = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState seal = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        for (int y = 0; y <= 5; y++) {
            lv.setBlock(base.offset(-2, y, 0), frame, 3);
            lv.setBlock(base.offset(2, y, 0), frame, 3);
        }
        for (int x = -2; x <= 2; x++) lv.setBlock(base.offset(x, 6, 0), frame, 3);
        if (active) {
            lv.setBlock(base.offset(0, 0, 0), DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
            lv.setBlock(base.offset(-1, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            lv.setBlock(base.offset(1, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        } else {
            for (int x = -1; x <= 1; x++)
                for (int y = 0; y <= 5; y++)
                    lv.setBlock(base.offset(x, y, 0), seal, 3);
        }
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/DungeonGateBuilder.java src/main/java/tong/statmod/dungeon/city/CityGateOpener.java src/main/java/tong/statmod/dungeon/city/PortalCourtBuilder.java
git commit -m "feat(city): Porte du Donjon (ouverture a l'approche) + Cour des Portails"
```

---

### Task 7: CityGenerator — orchestrateur (ticks, SavedData, legacy clean, regen)

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityGenerator.java`

- [ ] **Step 1: Write the implementation**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.dungeon.IslandGenerator;

/**
 * Orchestrateur de la Cité des Aventuriers : enfile la construction (coque → place → camp →
 * porte → portails) dans une {@link CityBuildQueue} drainée à budget fixe par tick. Démarre
 * en tâche de fond au démarrage du serveur ; marqueur d'achèvement versionné en SavedData
 * ({@link CitySavedData}) — incrémenter {@link #CITY_VERSION} force une reconstruction.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class CityGenerator {

    /** Incrémenter à chaque évolution de la génération pour reconstruire les mondes existants. */
    public static final int CITY_VERSION = 1;
    /** ~2 bandes de 4×600 par tick ≈ 15-30k blocs/tick : invisible en jeu, cité en ~1-2 min. */
    private static final int JOBS_PER_TICK = 2;

    private static final CityBuildQueue QUEUE = new CityBuildQueue();
    private static boolean building = false;

    private CityGenerator() {}

    public static boolean isBuilt(ServerLevel dungeonLevel) {
        return CitySavedData.get(dungeonLevel).builtVersion() >= CITY_VERSION;
    }

    public static boolean isBuilding() { return building; }

    public static int progressPercent() { return QUEUE.progressPercent(); }

    /** Programme la construction si nécessaire. Idempotent (no-op si construite ou en cours). */
    public static void ensureCity(ServerLevel lv) {
        if (building || isBuilt(lv)) return;
        QUEUE.clear();
        enqueueLegacyCleanupIfNeeded(lv, QUEUE);
        enqueueBuild(lv, QUEUE);
        building = true;
        STATMod.LOGGER.info("[City] Construction de la Cité des Aventuriers programmée ({} segments)",
                QUEUE.totalJobs());
    }

    /** Regen forcée (/statdungeon regen 0) : purge l'intérieur puis reconstruit. */
    public static void regen(ServerLevel lv) {
        QUEUE.clear();
        CitySavedData.get(lv).setBuiltVersion(0);
        enqueueInteriorClear(lv, QUEUE);
        enqueueBuild(lv, QUEUE);
        building = true;
        STATMod.LOGGER.info("[City] Regen de la cité programmée ({} segments)", QUEUE.totalJobs());
    }

    private static void enqueueBuild(ServerLevel lv, CityBuildQueue q) {
        CityShell.enqueue(lv, q);
        q.add(() -> PlazaBuilder.build(lv));
        q.add(() -> ArtisanCampBuilder.build(lv));
        DungeonGateBuilder.build(lv, q);
        q.add(() -> PortalCourtBuilder.build(lv));
    }

    /**
     * Mondes existants : l'ancienne île hub (temple + cage barrière + relief, bbox ±152 autour
     * de (0,100,-300)) est DANS l'emprise de la cité. Détectée par un bloc non-air sous l'ancien
     * spawn → purge ciblée de sa bounding box (scan-and-clear, jamais de setBlock sur de l'air).
     */
    private static void enqueueLegacyCleanupIfNeeded(ServerLevel lv, CityBuildQueue q) {
        BlockPos oldSpawn = new BlockPos(0, 100, -300);
        boolean legacy = !lv.getBlockState(oldSpawn.below()).isAir()
                || !lv.getBlockState(oldSpawn.offset(150, 0, 0)).isAir(); // coin de cage barrière
        if (!legacy) return;
        var box = IslandGenerator.floorBoundingBox(0);
        for (int x0 = box.minX(); x0 <= box.maxX(); x0 += 8) {
            final int xs = x0, xe = Math.min(x0 + 7, box.maxX());
            q.add(() -> {
                for (int x = xs; x <= xe; x++)
                    for (int y = box.minY(); y <= box.maxY(); y++)
                        for (int z = box.minZ(); z <= box.maxZ(); z++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!lv.getBlockState(p).isAir()) {
                                lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
            });
        }
        STATMod.LOGGER.info("[City] Ancien temple/cage détectés — purge legacy programmée");
    }

    /** Purge de l'intérieur de la cité (au-dessus du sol, sous le plafond) pour la regen. */
    private static void enqueueInteriorClear(ServerLevel lv, CityBuildQueue q) {
        for (int x0 = -CityPlan.RADIUS; x0 <= CityPlan.RADIUS; x0 += 4) {
            final int xs = x0, xe = Math.min(x0 + 3, CityPlan.RADIUS);
            q.add(() -> {
                for (int x = xs; x <= xe; x++)
                    for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z++) {
                        if (!CityPlan.inCity(x, z)) continue;
                        for (int y = CityPlan.GROUND_Y + 1; y <= CityPlan.CEILING_Y + 2; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!lv.getBlockState(p).isAir()) {
                                lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
            });
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv != null) ensureCity(lv);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!building) return;
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null) { building = false; return; }

        QUEUE.tick(JOBS_PER_TICK);

        // Progression aux joueurs présents dans la cité, toutes les 2 s.
        if (event.getServer().getTickCount() % 40 == 0) {
            for (ServerPlayer p : DungeonTeleportHandler.playersOnFloor(lv, 0)) {
                p.displayClientMessage(Component.translatable("dungeon.city.building",
                        QUEUE.progressPercent()), true);
            }
        }

        if (QUEUE.isDone()) {
            building = false;
            CitySavedData.get(lv).setBuiltVersion(CITY_VERSION);
            STATMod.LOGGER.info("[City] Cité des Aventuriers construite ({} segments)", QUEUE.totalJobs());
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityGenerator.java
git commit -m "feat(city): CityGenerator - construction etagee, legacy clean, regen"
```

---

### Task 8: Câblage — IslandGenerator, TeleportHandler, RespawnHandler, Commands, suppression du temple

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/IslandGenerator.java:30-33`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java:40-48,65-74,169-206`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonRespawnHandler.java:38-40`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonCommands.java:97-123`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonArchitect.java:42,96-99`
- Delete: `src/main/java/tong/statmod/dungeon/DungeonHubFloor.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`, `fr_fr.json`

- [ ] **Step 1: IslandGenerator — l'étage 0 délègue à la cité**

Dans `IslandGenerator.generateFloor`, ajouter en tête (avant le check `sp.below()`) :

```java
public static boolean generateFloor(ServerLevel lv, int floor) {
    // Étage 0 = Cité des Aventuriers : pipeline dédié, étalé sur les ticks (jamais synchrone).
    if (floor == 0) {
        tong.statmod.dungeon.city.CityGenerator.ensureCity(lv);
        return true;
    }
    BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
    ...
```

Et dans le même fichier, supprimer la branche `Role.HUB` du calcul du rôle (ligne 37) :

```java
DungeonArchitect.Role role =
        (floor % 10 == 0) ? DungeonArchitect.Role.BOSS
      : (floor % 5 == 0) ? DungeonArchitect.Role.TREASURE
      : DungeonArchitect.Role.COMBAT;
```

- [ ] **Step 2: DungeonTeleportHandler — spawn et entrée cité**

Remplacer le cas `floor == 0` de `floorSpawnPos` :

```java
public static BlockPos floorSpawnPos(int floor) {
    if (floor == 0) {
        return tong.statmod.dungeon.city.CityPlan.center();
    }
    ...
```

Remplacer le cas `floor == 0` de `floorPlayerSpawnPos` :

```java
public static BlockPos floorPlayerSpawnPos(int floor) {
    if (floor == 0) {
        return tong.statmod.dungeon.city.CityPlan.playerSpawn();
    }
    BlockPos island = floorSpawnPos(floor);
    ...
```

Dans `enterFloor`, encadrer les mécaniques de combat (elles n'ont pas de sens dans la cité) :

```java
        player.teleportTo(dungeon, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());

        if (floor > 0) {
            DungeonMobSpawner.requestWave(dungeon, floor);
            DungeonRush.beginFloor(player.getUUID());
            DungeonRecords.onFloorEnter(player, floor, dungeon.getGameTime());
        }

        tong.statmod.network.SyncHelper.syncStats(player);

        if (floor > 0) {
            DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    theme.displayName()), false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "dungeon.enter.objective",
                    net.minecraft.network.chat.Component.translatable(
                            DungeonObjective.forFloor(floor).translationKey())), true);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "dungeon.city.welcome"), false);
        }
```

(Le reste — sauvegarde overworld, `sendIntroIfFirstTime`, log — ne change pas.)

- [ ] **Step 3: DungeonRespawnHandler — mort dans la cité sans pénalité**

Après le calcul de `floor` (ligne 39), remplacer `if (floor < 1) floor = 1;` par :

```java
        // Mort dans la CITÉ (étage 0) : zone sûre — aucune pénalité de points, aucun wipe de
        // vague. On annule la mort et on redépose le joueur sur la Grande Place.
        if (floor == 0) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
            player.getFoodData().setFoodLevel(20);
            var sp = tong.statmod.dungeon.city.CityPlan.playerSpawn();
            player.teleportTo((ServerLevel) player.level(),
                    sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "dungeon.city.respawn"), false);
            return;
        }
```

- [ ] **Step 4: DungeonCommands — regen 0**

Élargir la plage de l'argument (ligne 98) : `IntegerArgumentType.integer(0, 10000)`, puis router
le cas 0 au début de l'executes :

```java
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    ServerLevel dungeon = ctx.getSource().getServer()
                                            .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                                    if (dungeon == null) { ... inchangé ... }
                                    if (floor == 0) {
                                        tong.statmod.dungeon.city.CityGenerator.regen(dungeon);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Regen de la Cité programmée (progression en tâche de fond)"), true);
                                        return 1;
                                    }
                                    // ... boucle de clear + generateFloor inchangés pour floor >= 1
```

- [ ] **Step 5: Supprimer le temple**

1. Supprimer le fichier `src/main/java/tong/statmod/dungeon/DungeonHubFloor.java`.
2. Dans `DungeonArchitect.buildFloor`, supprimer le bloc `if (role == Role.HUB) { ... }`
   (lignes 96-99) et retirer `HUB` de l'enum `Role` (ligne 42).
3. Vérifier qu'aucune autre référence ne subsiste :

Run: `grep -rn "DungeonHubFloor\|Role.HUB" src/main/java/`
Expected: aucune occurrence. S'il en reste (ex. `DungeonBossArenaFloor.buildPlatform` appelé par
le hub uniquement — le garder, il sert aux boss), corriger chaque site vers le pipeline cité.

- [ ] **Step 6: Clés de langue**

Dans `src/main/resources/assets/statmod/lang/en_us.json`, ajouter :

```json
"dungeon.city.building": "The city awakens… %s%%",
"dungeon.city.welcome": "§bWelcome to the City of Adventurers — the last safe haven before the Descent.",
"dungeon.city.respawn": "§7The Guardian Crystal shielded you. You wake up on the Grand Plaza."
```

Dans `fr_fr.json` :

```json
"dungeon.city.building": "La cité s'éveille… %s %%",
"dungeon.city.welcome": "§bBienvenue à la Cité des Aventuriers — dernier havre avant la Descente.",
"dungeon.city.respawn": "§7Le Cristal Gardien vous a protégé. Vous vous réveillez sur la Grande Place."
```

- [ ] **Step 7: Full test suite + build**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL, aucune régression (les suites dungeon existantes restent vertes —
en particulier `DungeonTeleportHandlerTest` : si un test verrouille l'ancien spawn `(0,100,-300)`
de l'étage 0, le mettre à jour vers `CityPlan.center()` — c'est un changement voulu du contrat).

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/tong/statmod/dungeon src/main/resources/assets/statmod/lang
git commit -m "feat(city): cablage etage 0 -> Cite des Aventuriers, suppression du temple"
```

---

### Task 9: Vérification en jeu + documentation

**Files:**
- Modify: `CLAUDE.md` (section Trial Dungeon)

- [ ] **Step 1: Vérification manuelle en jeu**

Run: `./gradlew runClient`

Checklist (créer un monde neuf) :
1. Les logs serveur montrent `[City] Construction de la Cité des Aventuriers programmée` dès le
   démarrage, puis `construite` après ~1-2 min — sans freeze perceptible en jeu.
2. Poser le bloc portail (`statmod:dungeon_portal`), entrer → arrivée sur la Grande Place
   (message de bienvenue). Si la construction est en cours : action-bar « La cité s'éveille… ».
3. Grande Place : fontaine, statue, Cristal Gardien, waystone cliquable, balise de retour
   fonctionnelle, banquier présent, 4 étals de marchands opérationnels (clic droit → trades).
4. Camp des artisans : table d'enchantement niveau 30 (vérifier l'option max), forge, lits.
5. Marcher vers le sud : la Porte du Donjon s'ouvre à l'approche (son), le téléporteur envoie à
   l'étage 1 ; s'éloigner → la porte se referme.
6. Cour des Portails : l'arche centrale ramène à l'overworld ; les 4 autres sont scellées.
7. `/kill @s` dans la cité → réapparition Grande Place, aucun point perdu (`/statdungeon info`).
8. `/statdungeon regen 0` → purge + reconstruction en tâche de fond.
9. Monde EXISTANT (avec l'ancien temple) : au premier démarrage, la purge legacy retire temple
   et cage barrière, la cité les remplace.

- [ ] **Step 2: Mettre à jour CLAUDE.md**

Dans la section `## 🏰 Trial Dungeon`, ajouter au tableau « Fichiers clés » les entrées
`dungeon/city/CityPlan.java`, `dungeon/city/CityGenerator.java` (+ 1 ligne par builder), et un
paragraphe daté « Cité des Aventuriers (plan A, 2026-07-12) » décrivant : remplacement du temple,
génération étagée, `regen 0`, mort sans pénalité étage 0. Mentionner que les plans B (quartiers +
PNJ) et C (mécaniques) suivent.

- [ ] **Step 3: Commit final**

```bash
git add CLAUDE.md
git commit -m "docs: Cite des Aventuriers plan A livre (CLAUDE.md a jour)"
```

---

## Self-review (fait à l'écriture du plan)

- **Couverture spec (périmètre plan A)** : §2.1 coordonnées ✔ (Task 1), §2.2 package ✔ (Tasks 1-7),
  §2.3 génération étagée ✔ (Tasks 2, 7), §2.4 sécurité/mort ✔ (Task 8), §3 partiel — Place, Porte,
  Portails, camp provisoire ✔ (Tasks 5-6) ; les autres zones = plan B ; §4 mécaniques = plan C ;
  §5 PNJ = plan B ; §6 restart/regen/legacy ✔ (Task 7).
- **Placeholders** : aucun TBD ; le seul point ouvert (signatures `placeStall`/`spawn`/
  `placeCheckpoint`) a une étape de vérification dédiée avec commande exacte (Task 5 Step 1).
- **Cohérence de types** : `CityBuildQueue.Job` utilisé partout ; `CityPlan` constantes référencées
  par `CityShell`/`CityGateOpener`/`CityGenerator` ; `DungeonGateBuilder.HALF_W`/`PASSAGE_H`
  partagés avec `CityGateOpener` ; `enqueue(lv, q)` signature uniforme.

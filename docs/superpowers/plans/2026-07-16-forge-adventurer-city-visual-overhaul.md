# Cité des Aventuriers — Refonte visuelle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformer la cité de l'étage 0 en une ville resserrée et dense (RADIUS 300→160) avec plafond de grotte non-plat, silhouette verticale, fin du look modulo et densité au sol — sans nouveau système.

**Architecture:** Upgrade en place du package `dungeon/city/`. Géométrie pure repositionnée dans `CityPlan` (2 anneaux, anti-chevauchement testé). Plafond via fonction pure `CityCeiling.heightAt` testable, consommée par `CityShell`. Nouveaux helpers de massing (toits/arche/flèche) dans `CityRoofs`. Détail seedé déterministe. Build toujours enfilé par tick (`CityBuildQueue`), versionné (`CITY_VERSION`).

**Tech Stack:** Java 17, Forge 1.20.1, JUnit Jupiter 5.10, Gradle (ForgeGradle). Blocs vanilla + Quark soft-resolus (`CityMaterialPalette`).

**Convention de test du repo :** la géométrie pure (`CityPlan`, `CityCeiling`) est testée en JUnit. Les builders (`CityShell`, `CityDistrictBuilder`, `CityDetailPass`) posent des blocs via `ServerLevel` et n'ont **pas** de test unitaire (pas d'infra GameTest ici) → vérification **manuelle en jeu** via `/statdungeon regen 0`. Les tâches builder incluent donc une étape de vérif manuelle explicite au lieu d'un test JUnit.

**Coordonnées de référence (centre (0,100,-500)) — déjà vérifiées non-chevauchantes ⊂ rempart :**

```
OUTER  R=118, rayon de site ≤32 :
  human   (-109,-455)   dwarven (-45,-391)   guild   ( 45,-609)   arena   (109,-545)
  elven   (-109,-545)   beast   ( 45,-391)   market  (109,-455)   training(-45,-609)
INNER  R=62, rayon de site ≤24 :
  artisans( 44,-456)    sanctuary(-44,-456)  gardens (-44,-544)   heroes  ( 44,-544)
plaza (0,-500) R=28   gate (0,-360)   spawn (0,101,-478)   portalCourt (40,-370)
```

---

## Phase 0 — Rétrécir proprement

### Task 0.1 : CityPlan rescalé + repositionné (TDD)

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityPlan.java`
- Test: `src/test/java/tong/statmod/dungeon/city/CityPlanTest.java` (créer)

- [ ] **Step 1 : Écrire le test qui échoue**

Créer `CityPlanTest.java` :

```java
package tong.statmod.dungeon.city;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CityPlanTest {

    @Test
    void allSitesFitInsideWall() {
        for (CityPlan.CitySite s : CityPlan.sites()) {
            double d = Math.hypot(s.center().getX() - CityPlan.CENTER_X,
                    s.center().getZ() - CityPlan.CENTER_Z);
            assertTrue(d + s.radius() <= CityPlan.WALL_INNER,
                    s.id() + " dépasse le rempart (reach=" + (d + s.radius()) + ")");
        }
    }

    @Test
    void noTwoSitesOverlap() {
        List<CityPlan.CitySite> sites = CityPlan.sites();
        for (int i = 0; i < sites.size(); i++) {
            for (int j = i + 1; j < sites.size(); j++) {
                CityPlan.CitySite a = sites.get(i), b = sites.get(j);
                if (a.radius() == 0 || b.radius() == 0) continue; // gate = repère ponctuel
                double d = Math.hypot(a.center().getX() - b.center().getX(),
                        a.center().getZ() - b.center().getZ());
                assertTrue(d >= a.radius() + b.radius(),
                        a.id() + " chevauche " + b.id() + " (d=" + d + ")");
            }
        }
    }

    @Test
    void spawnInsidePlazaGateOnWall() {
        BlockPos spawn = CityPlan.playerSpawn();
        assertTrue(CityPlan.inPlaza(spawn.getX(), spawn.getZ()), "spawn hors place");
        BlockPos gate = CityPlan.gateCenter();
        double dg = Math.hypot(gate.getX() - CityPlan.CENTER_X, gate.getZ() - CityPlan.CENTER_Z);
        assertTrue(dg >= CityPlan.WALL_INNER - 14 && dg <= CityPlan.RADIUS, "porte pas sur le rempart sud");
    }
}
```

- [ ] **Step 2 : Lancer le test → échec attendu**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityPlanTest"`
Expected: FAIL (chevauchements / dépassements avec les anciennes constantes RADIUS=300).

- [ ] **Step 3 : Rescaler les constantes et repositionner les districts**

Dans `CityPlan.java`, remplacer les constantes de dimension :

```java
    public static final int CENTER_X = 0;
    public static final int CENTER_Z = -500;
    public static final int GROUND_Y = 100;
    public static final int RADIUS = 160;
    public static final int WALL_INNER = 152;
    public static final int WALL_TOP_Y = 128;
    public static final int CEILING_Y = 170;
    public static final int PLAZA_RADIUS = 28;
    public static final double AVENUE_HALF_WIDTH = 3.5;
    public static final int INNER_RING_RADIUS = 62;
    public static final int OUTER_RING_RADIUS = 118;
    private static final double RING_HALF_WIDTH = 4.0;
```

Remplacer les positions de districts (les corps des méthodes) par les coordonnées vérifiées :

```java
    public static BlockPos gateCenter() { return new BlockPos(CENTER_X, GROUND_Y, CENTER_Z + WALL_INNER - 12); }
    public static BlockPos portalCourt() { return new BlockPos(CENTER_X + 40, GROUND_Y, CENTER_Z + 130); }
    public static BlockPos artisanCamp() { return artisanDistrict(); }

    public static BlockPos guild()        { return new BlockPos(CENTER_X + 45,  GROUND_Y, CENTER_Z - 109); }
    public static BlockPos humanQuarter() { return new BlockPos(CENTER_X - 109, GROUND_Y, CENTER_Z + 45); }
    public static BlockPos elvenQuarter() { return new BlockPos(CENTER_X - 109, GROUND_Y, CENTER_Z - 45); }
    public static BlockPos dwarvenQuarter(){ return new BlockPos(CENTER_X - 45, GROUND_Y, CENTER_Z + 109); }
    public static BlockPos beastQuarter() { return new BlockPos(CENTER_X + 45,  GROUND_Y, CENTER_Z + 109); }
    public static BlockPos market()       { return new BlockPos(CENTER_X + 109, GROUND_Y, CENTER_Z + 45); }
    public static BlockPos artisanDistrict(){ return new BlockPos(CENTER_X + 44, GROUND_Y, CENTER_Z + 44); }
    public static BlockPos arena()        { return new BlockPos(CENTER_X + 109, GROUND_Y, CENTER_Z - 45); }
    public static BlockPos trainingGround(){ return new BlockPos(CENTER_X - 45, GROUND_Y, CENTER_Z - 109); }
    public static BlockPos sanctuary()    { return new BlockPos(CENTER_X - 44, GROUND_Y, CENTER_Z + 44); }
    public static BlockPos hangingGardens(){ return new BlockPos(CENTER_X - 44, GROUND_Y, CENTER_Z - 44); }
    public static BlockPos hallOfHeroes() { return new BlockPos(CENTER_X + 44, GROUND_Y, CENTER_Z - 44); }
```

> Note : la convention d'angle du code place +z (CENTER_Z + n) au **sud** (côté porte). Les coordonnées ci-dessus correspondent aux angles calculés (human θ=157.5 → x−109 z+45, etc.).

Réduire les rayons dans `sites()` pour respecter l'invariant (≤32 extérieur, ≤24 intérieur, place 28) :

```java
    public static List<CitySite> sites() {
        return List.of(
                new CitySite("plaza", center(), PLAZA_RADIUS),
                new CitySite("guild", guild(), 30),
                new CitySite("human", humanQuarter(), 30),
                new CitySite("elven", elvenQuarter(), 30),
                new CitySite("dwarven", dwarvenQuarter(), 30),
                new CitySite("beast", beastQuarter(), 30),
                new CitySite("market", market(), 30),
                new CitySite("artisans", artisanDistrict(), 22),
                new CitySite("arena", arena(), 30),
                new CitySite("training", trainingGround(), 30),
                new CitySite("sanctuary", sanctuary(), 20),
                new CitySite("gardens", hangingGardens(), 20),
                new CitySite("heroes", hallOfHeroes(), 20),
                new CitySite("portals", portalCourt(), 18),
                new CitySite("gate", gateCenter(), 0));
    }
```

- [ ] **Step 4 : Lancer le test → succès attendu**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityPlanTest"`
Expected: PASS (3 tests).

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityPlan.java \
        src/test/java/tong/statmod/dungeon/city/CityPlanTest.java
git commit -m "feat(city): resserre l'emprise (R=160) et repositionne les districts sur 2 anneaux"
```

---

### Task 0.2 : Purge de l'ancienne emprise + bump version

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityGenerator.java`

- [ ] **Step 1 : Ajouter la constante d'ancienne emprise et le bump de version**

Dans `CityGenerator.java` :

```java
    public static final int CITY_VERSION = 12;
    /** Ancienne emprise (RADIUS pré-shrink) à purger sur les mondes existants. */
    private static final int LEGACY_MAX_RADIUS = 300;
```

- [ ] **Step 2 : Ajouter la purge de l'ancienne emprise et l'appeler sur upgrade**

Ajouter la méthode :

```java
    /**
     * Vide à l'air toute la colonne (sol → plafond) sur l'ancien disque RADIUS=300, afin
     * qu'aucun bloc n'orpheline hors de la nouvelle cité R=160. Scan-and-clear : jamais de
     * setBlock sur de l'air. Enfilé en bandes de 8 en X pour le budget par tick.
     */
    private static void enqueueLegacyExtentClear(ServerLevel lv, CityBuildQueue q) {
        final int cx = CityPlan.CENTER_X, cz = CityPlan.CENTER_Z;
        for (int x0 = cx - LEGACY_MAX_RADIUS; x0 <= cx + LEGACY_MAX_RADIUS; x0 += 8) {
            final int xs = x0, xe = Math.min(x0 + 7, cx + LEGACY_MAX_RADIUS);
            q.add(() -> {
                for (int x = xs; x <= xe; x++)
                    for (int z = cz - LEGACY_MAX_RADIUS; z <= cz + LEGACY_MAX_RADIUS; z++) {
                        long dx = x - cx, dz = z - cz;
                        if (dx * dx + dz * dz > (long) LEGACY_MAX_RADIUS * LEGACY_MAX_RADIUS) continue;
                        if (CityPlan.inCity(x, z)) continue; // la nouvelle cité sera rebâtie par-dessus
                        for (int y = CityPlan.GROUND_Y - 2; y <= CityPlan.CEILING_Y + 2; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!lv.getBlockState(p).isAir())
                                lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
            });
        }
    }
```

Dans `ensureCity`, ajouter l'appel sur upgrade (`previousVersion > 0`), avant l'interior clear :

```java
        if (previousVersion > 0) {
            enqueueLegacyExtentClear(lv, QUEUE);
            enqueueInteriorClear(lv, QUEUE);
            enqueueCityEntityClear(lv, QUEUE);
        } else {
            enqueueLegacyCleanupIfNeeded(lv, QUEUE);
        }
```

Idem dans `regen` (ajouter `enqueueLegacyExtentClear(lv, QUEUE);` avant `enqueueInteriorClear`).

- [ ] **Step 3 : Élargir l'AABB de purge d'entités à l'ancienne emprise**

Dans `enqueueCityEntityClear`, remplacer `CityPlan.RADIUS` par `LEGACY_MAX_RADIUS` dans les bornes de l'`AABB` (pour discard les armor stands/marchands/mannequins des anciens districts au-delà de r=160) :

```java
            AABB city = new AABB(
                    c.getX() - LEGACY_MAX_RADIUS, CityPlan.GROUND_Y,
                    c.getZ() - LEGACY_MAX_RADIUS,
                    c.getX() + LEGACY_MAX_RADIUS, CityPlan.CEILING_Y,
                    c.getZ() + LEGACY_MAX_RADIUS);
```

- [ ] **Step 4 : Vérif compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL (warnings de dépréciation `ResourceLocation` tolérés).

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityGenerator.java
git commit -m "feat(city): purge l'ancienne emprise R=300 au shrink + bump CITY_VERSION 12"
```

---

## Phase 1 — Plafond de grotte

### Task 1.1 : Fonction de hauteur de plafond pure (TDD)

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityCeiling.java`
- Test: `src/test/java/tong/statmod/dungeon/city/CityCeilingTest.java`

- [ ] **Step 1 : Écrire le test qui échoue**

```java
package tong.statmod.dungeon.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CityCeilingTest {

    @Test
    void boundedBetweenFloorAndPeak() {
        for (int x = -CityPlan.RADIUS; x <= CityPlan.RADIUS; x += 7)
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z += 7) {
                int h = CityCeiling.heightAt(x, z);
                assertTrue(h >= 148 && h <= CityPlan.CEILING_Y, "plafond hors bornes: " + h);
                assertTrue(h > CityPlan.GROUND_Y + 10, "plafond trop bas au sol: " + h);
            }
    }

    @Test
    void deterministic() {
        assertEquals(CityCeiling.heightAt(12, -488), CityCeiling.heightAt(12, -488));
    }

    @Test
    void higherAtCenterThanRim() {
        int center = CityCeiling.heightAt(CityPlan.CENTER_X, CityPlan.CENTER_Z);
        int rim = CityCeiling.heightAt(CityPlan.CENTER_X + CityPlan.WALL_INNER - 4, CityPlan.CENTER_Z);
        assertTrue(center > rim, "le centre doit être plus haut que le pourtour");
    }
}
```

- [ ] **Step 2 : Lancer → échec (classe absente)**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityCeilingTest"`
Expected: FAIL (compilation : `CityCeiling` n'existe pas).

- [ ] **Step 3 : Implémenter la fonction pure**

```java
package tong.statmod.dungeon.city;

/**
 * Hauteur du plafond de la caverne-cité (pur, déterministe, sans ServerLevel).
 * Effet cathédrale : haut au centre ({@link CityPlan#CEILING_Y}), plus bas vers le rempart,
 * ondulé par une pseudo-onde trigonométrique (déterministe, jamais Random/Math.random).
 */
public final class CityCeiling {

    private static final int FLOOR_MIN = 148;

    private CityCeiling() {}

    public static int heightAt(int x, int z) {
        double dx = x - CityPlan.CENTER_X, dz = z - CityPlan.CENTER_Z;
        double t = Math.min(1.0, Math.hypot(dx, dz) / CityPlan.WALL_INNER); // 0 centre → 1 rempart
        double base = CityPlan.CEILING_Y + t * (FLOOR_MIN - CityPlan.CEILING_Y); // lerp haut→bas
        double wave = Math.sin(x * 0.08) * Math.cos(z * 0.07) + 0.5 * Math.sin((x + z) * 0.05);
        int h = (int) Math.round(base + wave * 4.0);
        return Math.max(FLOOR_MIN, Math.min(CityPlan.CEILING_Y, h));
    }
}
```

- [ ] **Step 4 : Lancer → succès**

Run: `./gradlew test --tests "tong.statmod.dungeon.city.CityCeilingTest"`
Expected: PASS (3 tests).

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityCeiling.java \
        src/test/java/tong/statmod/dungeon/city/CityCeilingTest.java
git commit -m "feat(city): fonction pure CityCeiling.heightAt (plafond de grotte ondulé)"
```

---

### Task 1.2 : CityShell — plafond ondulé + stalactites

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityShell.java`

- [ ] **Step 1 : Remplacer le plafond plat par le corps rocheux ondulé**

Dans `buildStrip`, supprimer la ligne `lv.setBlock(new BlockPos(x, CityPlan.CEILING_Y, z), ceiling, FLAG);` et la remplacer, à la fin de la boucle `z`, par le remplissage rocheux de `heightAt(x,z)` jusqu'à `CEILING_Y` :

```java
                // Plafond de grotte : corps rocheux ondulé de heightAt → CEILING_Y (coque fermée).
                int ch = CityCeiling.heightAt(x, z);
                BlockState rock = ((x * 7 + z * 13) & 3) == 0
                        ? Blocks.TUFF.defaultBlockState()
                        : ((x + z) & 1) == 0 ? Blocks.DEEPSLATE.defaultBlockState()
                        : Blocks.STONE.defaultBlockState();
                for (int y = ch; y <= CityPlan.CEILING_Y; y++) {
                    lv.setBlock(new BlockPos(x, y, z), rock, FLAG);
                }
```

> `deep`/`stone`/`pave`/`plazaPave`/`wall` restent utilisés pour le sol/rempart existants ; retirer la variable `ceiling` devenue inutile.

- [ ] **Step 2 : Enrichir les stalactites (dripstone + améthyste, accrochées au plafond réel)**

Remplacer la boucle de cristaux dans `enqueue` (le `for (int i = 0; i < 70; i++)`) pour accrocher chaque suspension sous `heightAt` au lieu de `CEILING_Y`, et alterner améthyste / dripstone :

```java
        Random rng = new Random(0xC17ADE5L);
        for (int i = 0; i < 90; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * (CityPlan.WALL_INNER - 30);
            final int cx = CityPlan.CENTER_X + (int) (Math.cos(angle) * dist);
            final int cz = CityPlan.CENTER_Z + (int) (Math.sin(angle) * dist);
            final int len = 3 + rng.nextInt(6);
            final boolean dripstone = rng.nextBoolean();
            q.add(() -> buildStalactite(lv, cx, cz, len, dripstone));
        }
```

Remplacer `buildCrystal` par `buildStalactite` :

```java
    /** Suspension accrochée sous le plafond réel : gaine + pointe (améthyste ou dripstone). */
    private static void buildStalactite(ServerLevel lv, int x, int z, int len, boolean dripstone) {
        int top = CityCeiling.heightAt(x, z);
        for (int i = 0; i < len; i++) {
            int y = top - 1 - i;
            if (y <= CityPlan.GROUND_Y + 4) break;
            BlockState body = dripstone ? Blocks.DRIPSTONE_BLOCK.defaultBlockState()
                    : Blocks.AMETHYST_BLOCK.defaultBlockState();
            lv.setBlock(new BlockPos(x, y, z), body, FLAG);
            if (i == len / 2 && !dripstone)
                lv.setBlock(new BlockPos(x + 1, y, z), Blocks.SEA_LANTERN.defaultBlockState(), FLAG);
        }
        int tipY = top - 1 - len;
        if (tipY > CityPlan.GROUND_Y + 4) {
            BlockState tip = dripstone
                    ? Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    : Blocks.AMETHYST_CLUSTER.defaultBlockState();
            lv.setBlock(new BlockPos(x, tipY, z), tip, FLAG);
        }
    }
```

> Import à ajouter si absent : la classe utilise déjà `Blocks`, `BlockPos`, `Random`, `BlockState`.

- [ ] **Step 3 : Vérif compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4 : Vérif manuelle en jeu**

```
./gradlew runClient
/statdungeon regen 0
/statdungeon tp 0    (ou aller à l'étage 0)
```
Attendu : plafond **non-plat** (haut au centre, descend vers le rempart), roche mêlée (tuff/deepslate/stone), stalactites d'améthyste ET dripstone accrochées au relief. Aucun trou sur le vide (lever la tête partout).

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityShell.java
git commit -m "feat(city): plafond de grotte ondulé + stalactites dripstone/améthyste"
```

---

## Phase 2 — Silhouette & verticalité

### Task 2.1 : Helpers de massing (toits, arche, coupole, flèche)

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/city/CityRoofs.java`

- [ ] **Step 1 : Créer les helpers (déterministes, sans support flottant)**

```java
package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Briques de massing pour casser le "tout en boîtes" : toits pentus, coupoles, arches, flèches. */
final class CityRoofs {
    private static final int FLAG = 3;

    private CityRoofs() {}

    /** Toit à deux pentes (pignon sur l'axe X) au-dessus d'un bâtiment rx×rz, base à baseY. */
    static void gableRoof(ServerLevel lv, BlockPos c, int rx, int rz, int baseY, Block stair, Block ridge) {
        for (int z = -rz; z <= rz; z++) {
            int rise = rz - Math.abs(z);
            int y = baseY + Math.min(rise, rz);
            Direction face = z < 0 ? Direction.SOUTH : Direction.NORTH;
            for (int x = -rx; x <= rx; x++) {
                if (z == 0) { set(lv, c.offset(x, baseY + rz, 0), ridge); continue; }
                set(lv, c.offset(x, y, z), stairState(stair, face));
                set(lv, c.offset(x, y - 1, z), Blocks.POLISHED_ANDESITE.defaultBlockState()); // sous-toit plein
            }
        }
    }

    /** Toit hippé (4 pentes) : pyramide en escaliers. */
    static void hipRoof(ServerLevel lv, BlockPos c, int r, int baseY, Block stair) {
        for (int layer = 0; layer <= r; layer++) {
            int rr = r - layer;
            int y = baseY + layer;
            for (int x = -rr; x <= rr; x++) for (int z = -rr; z <= rr; z++) {
                boolean edge = Math.abs(x) == rr || Math.abs(z) == rr;
                if (!edge) continue;
                Direction f = Math.abs(x) >= Math.abs(z) ? (x < 0 ? Direction.EAST : Direction.WEST)
                                                         : (z < 0 ? Direction.SOUTH : Direction.NORTH);
                set(lv, c.offset(x, y, z), stairState(stair, f));
            }
        }
        set(lv, c.offset(0, baseY + r, 0), Blocks.LANTERN.defaultBlockState());
    }

    /** Coupole hémisphérique (guilde / sanctuaire / hall des héros). */
    static void dome(ServerLevel lv, BlockPos c, int r, int baseY, Block shell) {
        for (int y = 0; y <= r; y++) {
            double rr = Math.sqrt(Math.max(0, (double) r * r - (double) y * y));
            int ri = (int) Math.round(rr);
            for (int x = -ri; x <= ri; x++) for (int z = -ri; z <= ri; z++) {
                int d2 = x * x + z * z;
                if (d2 <= ri * ri && d2 >= (ri - 1) * (ri - 1))
                    set(lv, c.offset(x, baseY + y, z), shell.defaultBlockState());
            }
        }
        set(lv, c.offset(0, baseY + r, 0), Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** Arche en escaliers dans un mur (ouverture largeur w, hauteur h) centrée en c. */
    static void archway(ServerLevel lv, BlockPos c, int w, int h, Block stair) {
        for (int x = -w; x <= w; x++)
            for (int y = 1; y <= h; y++)
                set(lv, c.offset(x, y, 0), Blocks.AIR.defaultBlockState());
        set(lv, c.offset(-w, h, 0), stairState(stair, Direction.WEST));
        set(lv, c.offset(w, h, 0), stairState(stair, Direction.EAST));
        set(lv, c.offset(0, h + 1, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
    }

    /** Flèche-repère : fût carré + toit conique + fanal sommital. */
    static void spire(ServerLevel lv, BlockPos c, int radius, int height, Block wall, Block roof) {
        for (int y = 0; y <= height; y++)
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                if (Math.abs(x) == radius || Math.abs(z) == radius)
                    set(lv, c.offset(x, y, z), wall.defaultBlockState());
        for (int layer = 0; layer <= radius + 2; layer++) {
            int rr = radius + 1 - layer;
            for (int x = -rr; x <= rr; x++) for (int z = -rr; z <= rr; z++)
                if (Math.abs(x) == rr || Math.abs(z) == rr)
                    set(lv, c.offset(x, height + 1 + layer, z), roof.defaultBlockState());
        }
        set(lv, c.offset(0, height + radius + 4, 0), Blocks.SEA_LANTERN.defaultBlockState());
        set(lv, c.offset(0, height + radius + 5, 0), Blocks.END_ROD.defaultBlockState());
    }

    private static BlockState stairState(Block stair, Direction facing) {
        BlockState s = stair.defaultBlockState();
        if (stair instanceof StairBlock)
            s = s.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
        return s;
    }

    private static void set(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, FLAG); }
}
```

- [ ] **Step 2 : Vérif compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityRoofs.java
git commit -m "feat(city): helpers de massing (gable/hip/dome/archway/spire)"
```

---

### Task 2.2 : Repère central sur la place + toitures des monuments

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/PlazaBuilder.java`
- Modify: `src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java`

- [ ] **Step 1 : Flèche-repère au centre de la place**

Dans `PlazaBuilder.build`, après la pose de la fontaine/statue existante, ajouter une flèche haute décalée pour ne pas bloquer le spawn (spawn au sud de la place à z=-478 ; flèche au nord du centre) :

```java
        // Flèche-repère du Cristal Gardien : skyline de la cité, montée vers le plafond.
        BlockPos spireBase = new BlockPos(CityPlan.CENTER_X, CityPlan.GROUND_Y + 1, CityPlan.CENTER_Z - 10);
        CityRoofs.spire(lv, spireBase, 2, 34,
                net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE,
                net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES);
```

- [ ] **Step 2 : Coiffer guilde / hall des héros d'une coupole**

Dans `CityDistrictBuilder.buildGuild`, remplacer le toit implicite de `hall(...)` par une coupole. Après l'appel `hall(lv, c, 20, 14, 11, ...)`, ajouter :

```java
        CityRoofs.dome(lv, c.offset(0, 12, 0), 10, 0, Blocks.CHISELED_DEEPSLATE);
```

Dans `buildHallOfHeroes`, après le `hall(...)`, ajouter :

```java
        CityRoofs.dome(lv, c.offset(0, 10, 0), 9, 0, Blocks.POLISHED_BLACKSTONE);
```

- [ ] **Step 3 : Vérif compilation + jeu**

Run: `./gradlew compileJava` → SUCCESSFUL.
Puis `runClient` + `/statdungeon regen 0` : une flèche haute visible de loin au centre, coupoles sur guilde et hall des héros.

- [ ] **Step 4 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/PlazaBuilder.java \
        src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java
git commit -m "feat(city): flèche-repère centrale + coupoles sur les monuments"
```

---

### Task 2.3 : `hall()` et `tower()` — toitures et étages

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java`

- [ ] **Step 1 : Remplacer le toit quasi-plat de `hall()` par un vrai toit + arche d'entrée**

Dans `hall(...)`, supprimer la ligne du toit en escalier fractionnaire :
`set(lv, c.offset(x, height + 1 + Math.max(0, (rz - Math.abs(z)) / 3), z), roof);`
et l'ouverture d'air `for (int y = 1; y <= 4; y++) ... set(...AIR)`.
Puis, à la fin de `hall(...)`, ajouter un toit pignon et une arche :

```java
        CityRoofs.gableRoof(lv, c, rx, rz, height + 1, roof, Blocks.EXPOSED_COPPER);
        CityRoofs.archway(lv, c.offset(0, 0, rz), 2, 4, Blocks.STONE_BRICK_STAIRS);
```

> `roof` était un `Block` opaque (planks) ; garder l'appelant tel quel. `gableRoof` accepte n'importe quel `Block` comme escalier — pour un rendu correct, passer un escalier quand disponible : voir Step 2.

- [ ] **Step 2 : Fournir un escalier de toit dans la palette**

Dans `CityMaterialPalette.Style`, l'appelant passe `s.roof()` (planks). Ajouter un accès escalier générique dans `CityDistrictBuilder` (helper local) pour convertir un thème en escalier :

```java
    private static Block roofStair(Block roof) {
        if (roof == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_STAIRS;
        if (roof == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_STAIRS;
        if (roof == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_STAIRS;
        return Blocks.STONE_BRICK_STAIRS;
    }
```

Et dans `hall(...)`, utiliser `roofStair(roof)` au lieu de `roof` pour l'appel `gableRoof`.

- [ ] **Step 3 : `tower()` — chapeau hippé au lieu de la dalle plate**

Dans `tower(...)`, remplacer les deux boucles du `cap` plat par :

```java
        CityRoofs.hipRoof(lv, c.offset(0, height + 1, 0), radius + 1, 0,
                cap == Blocks.BLUE_WOOL ? Blocks.STONE_BRICK_STAIRS : Blocks.DEEPSLATE_BRICK_STAIRS);
```

- [ ] **Step 4 : Vérif jeu**

`runClient` + `/statdungeon regen 0` : les halls ont un toit à deux pentes + porte en arche ; les tours ont un chapeau pyramidal. Aucun toit plat résiduel.

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java
git commit -m "feat(city): toitures pignon/hippé + arches sur halls et tours"
```

---

## Phase 3 — Fin du look modulo

### Task 3.1 : Fenêtres seedées par bâtiment

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java`

- [ ] **Step 1 : Remplacer `(x+z)%5==0` par des baies structurées seedées**

Dans `hall(...)`, remplacer la logique de fenêtre :
`boolean glazed = y >= 3 && y <= 5 && ((x + z) % 5 == 0);`
par des baies régulières espacées, avec une graine dérivée du centre pour varier légèrement d'un bâtiment à l'autre :

```java
        long seed = ((long) c.getX() * 73856093L) ^ ((long) c.getZ() * 19349663L);
        int spacing = 4 + (int) (Math.floorMod(seed, 3)); // 4,5 ou 6 selon le bâtiment
        // ... dans la boucle edge :
                boolean bay = (Math.floorMod(x + z + (int) seed, spacing) == 0);
                boolean glazed = y >= 2 && y <= 4 && bay;
```

> Retire le caractère « damier » : l'espacement varie par bâtiment (`spacing` seedé) et le décalage `+seed` casse l'alignement global `(x+z)`.

- [ ] **Step 2 : Vérif jeu**

`runClient` + `/statdungeon regen 0` : les fenêtres forment des baies verticales régulières mais différentes d'un bâtiment à l'autre, plus le damier diagonal uniforme.

- [ ] **Step 3 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityDistrictBuilder.java
git commit -m "feat(city): fenêtres en baies seedées par bâtiment (fin du modulo)"
```

---

### Task 3.2 : Pavage seedé au sol

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityShell.java`

- [ ] **Step 1 : Mélange de pavage seedé pour le sol de remplissage**

Dans `buildStrip`, remplacer le choix de `surface` (actuellement `stone` par défaut hors place/routes) par un mélange déterministe de 3 blocs proches, sans grille visible :

```java
                BlockState surface;
                if (CityPlan.inPlaza(x, z)) surface = plazaPave;
                else if (CityPlan.onAvenue(x, z) || CityPlan.onRingRoad(x, z) || CityPlan.onDistrictConnector(x, z))
                    surface = pave;
                else {
                    int h = Math.floorMod(x * 31 + z * 17 + ((x >> 2) * (z >> 2)), 7);
                    surface = h < 4 ? Blocks.STONE.defaultBlockState()
                            : h < 6 ? Blocks.COBBLESTONE.defaultBlockState()
                            : Blocks.GRAVEL.defaultBlockState();
                }
```

- [ ] **Step 2 : Vérif jeu**

`/statdungeon regen 0` : le sol hors routes a une texture mêlée (pierre/cobble/gravier) irrégulière, plus l'aspect uniforme.

- [ ] **Step 3 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityShell.java
git commit -m "feat(city): pavage de sol seedé (fin du modulo)"
```

---

## Phase 4 — Densité au sol

### Task 4.1 : Maisons de remplissage le long des avenues

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityDetailPass.java`

- [ ] **Step 1 : Poser de petites maisons seedées le long des 6 avenues**

Dans `CityDetailPass.build`, ajouter une passe qui place des maisons compactes le long de chaque avenue radiale, entre la place (R=PLAZA_RADIUS+6) et l'anneau intérieur (R=INNER_RING_RADIUS-6), décalées de part et d'autre :

```java
        java.util.Random rng = new java.util.Random(0x4055EL);
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(90.0 + k * 60.0);
            for (int r = CityPlan.PLAZA_RADIUS + 10; r <= CityPlan.INNER_RING_RADIUS - 8; r += 12) {
                for (int side : new int[]{-6, 6}) {
                    int x = (int) Math.round(CityPlan.CENTER_X + Math.cos(a) * r - Math.sin(a) * side);
                    int z = (int) Math.round(CityPlan.CENTER_Z + Math.sin(a) * r + Math.cos(a) * side);
                    if (!CityPlan.inCity(x, z) || CityPlan.onAvenue(x, z)) continue;
                    fillerHouse(lv, new BlockPos(x, CityPlan.GROUND_Y + 1, z), rng.nextInt(4));
                }
            }
        }
```

Et le helper (maison 5×5 à toit pignon, réutilise `CityRoofs`) :

```java
    private static void fillerHouse(ServerLevel lv, BlockPos c, int variant) {
        Block wall = switch (variant) {
            case 0 -> Blocks.STONE_BRICKS; case 1 -> Blocks.MUD_BRICKS;
            case 2 -> Blocks.DEEPSLATE_BRICKS; default -> Blocks.COBBLESTONE;
        };
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            lv.setBlock(c.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
            if (edge) for (int y = 0; y <= 3; y++)
                lv.setBlock(c.offset(x, y, z), wall.defaultBlockState(), 3);
        }
        for (int y = 1; y <= 2; y++) lv.setBlock(c.offset(0, y, 3), Blocks.AIR.defaultBlockState(), 3); // porte
        lv.setBlock(c.offset(2, 2, 3), Blocks.GLASS_PANE.defaultBlockState(), 3);
        lv.setBlock(c.offset(-2, 2, 3), Blocks.GLASS_PANE.defaultBlockState(), 3);
        CityRoofs.gableRoof(lv, c.offset(0, 0, 0), 3, 3, 4, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
        lv.setBlock(c.offset(0, 1, 0), Blocks.LANTERN.defaultBlockState(), 3);
    }
```

- [ ] **Step 2 : Vérif jeu**

`/statdungeon regen 0` : de petites maisons bordent chaque avenue entre la place et les districts, sans bloquer les rues.

- [ ] **Step 3 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityDetailPass.java
git commit -m "feat(city): maisons de remplissage le long des avenues"
```

---

### Task 4.2 : Props seedés + éclairage d'ambiance

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/city/CityDetailPass.java`

- [ ] **Step 1 : Semer lampadaires, jardinières, bancs, bassins**

Ajouter à `CityDetailPass.build` une passe de mobilier urbain le long des routes d'anneau, seedée (pas de grille) :

```java
        java.util.Random pr = new java.util.Random(0xB3C4L);
        for (int deg = 0; deg < 360; deg += 9) {
            double a = Math.toRadians(deg);
            int r = CityPlan.INNER_RING_RADIUS;
            int x = (int) Math.round(CityPlan.CENTER_X + Math.cos(a) * r);
            int z = (int) Math.round(CityPlan.CENTER_Z + Math.sin(a) * r);
            if (!CityPlan.inCity(x, z)) continue;
            BlockPos p = new BlockPos(x, CityPlan.GROUND_Y + 1, z);
            switch (pr.nextInt(4)) {
                case 0 -> { // lampadaire
                    lv.setBlock(p, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
                    lv.setBlock(p.above(), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
                    lv.setBlock(p.above(2), Blocks.LANTERN.defaultBlockState(), 3);
                }
                case 1 -> { // jardinière
                    lv.setBlock(p, Blocks.FLOWER_POT.defaultBlockState(), 3);
                }
                case 2 -> { // banc
                    lv.setBlock(p, Blocks.OAK_STAIRS.defaultBlockState(), 3);
                }
                default -> { /* laisse respirer */ }
            }
        }
```

- [ ] **Step 2 : Vérif jeu**

`/statdungeon regen 0` : lampadaires allumés, jardinières et bancs répartis irrégulièrement le long de l'anneau intérieur ; la cité paraît habitée et éclairée.

- [ ] **Step 3 : Build complet + suite de tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, tous les tests verts (dont `CityPlanTest`, `CityCeilingTest`).

- [ ] **Step 4 : Commit**

```bash
git add src/main/java/tong/statmod/dungeon/city/CityDetailPass.java
git commit -m "feat(city): props urbains seedés + éclairage d'ambiance"
```

---

## Finalisation

### Task 5 : Gouvernance & doc

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1 : Mettre à jour la section « Cité des Aventuriers »**

Documenter la refonte visuelle (R=160, 2 anneaux, plafond de grotte, silhouette, fin du modulo, densité), le bump `CITY_VERSION=12`, et les nouveaux fichiers `CityCeiling`, `CityRoofs`. Ajouter les tests `CityPlanTest`/`CityCeilingTest`.

- [ ] **Step 2 : Vérif finale en jeu (checklist services)**

`/statdungeon regen 0` sur monde neuf ET monde existant (ancien R=300) :
- purge complète (pas d'anneau orphelin flottant hors du rempart)
- plafond non-plat, flèche centrale visible, aucune boîte modulo, sol dense
- **services intacts** : waystone, banquier (FDP), 4 marchands, téléporteur étage 1, PvP arène, respawn place, mort étage 0 = 0 pénalité

- [ ] **Step 3 : Commit**

```bash
git add CLAUDE.md
git commit -m "docs(city): documente la refonte visuelle de la Cité des Aventuriers"
```

---

## Notes d'exécution

- **Ordre impératif** : Phase 0 avant tout (les autres dépendent des nouvelles constantes/positions).
- **Déterminisme** : toute graine est un littéral fixe ; aucun `Math.random()`/`new Date()`.
- **Budget par tick** : si le build gèle visiblement, monter `JOBS_PER_TICK` (CityGenerator) de 2 à 3-4, ou découper les passes de détail en plus de jobs.
- **`CITY_VERSION`** déjà bumpé en Task 0.2 → toute regen ultérieure de ce plan n'a pas besoin de re-bump (le contenu change mais la reconstruction est déjà forcée pour les testeurs via `/statdungeon regen 0`).
```

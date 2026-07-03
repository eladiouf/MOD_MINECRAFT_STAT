# Magic Forge VFX + Occlusion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger l'occlusion des blocs de support sous `infusion_forge` / `enchantment_anvil`, ajouter des particules distinctes, et éclaircir leurs textures.

**Architecture:** Introduire un bloc décoratif dédié aux enclumes magiques avec `VoxelShape`, `noOcclusion`, et `animateTick` configuré par type de particule. Conserver les models JSON existants autant que possible et traiter les teintes au niveau des PNGs.

**Tech Stack:** NeoForge 1.21.1, Java 21, JUnit 5, PNG assets under `src/main/resources/assets/statmod/textures/block`

## Global Constraints

- Cible runtime: NeoForge `1.21.1`
- Java: `21`
- Logique gameplay côté serveur, rendu/particules côté client uniquement
- Commentaires métier en français, techniques en anglais
- Pas de changement de recettes ou de gates Overgeared dans ce lot

---

### Task 1: Add Block Regression Tests

**Files:**
- Create: `src/test/java/tong/statmod/block/MagicForgeBlockBehaviorTest.java`

**Interfaces:**
- Consumes: `tong.statmod.block.ForgingBlocks`
- Produces: tests that assert non-full block behavior expectations

- [ ] Write failing tests for non-occluding forge blocks and expected shape bounds
- [ ] Run `.\gradlew.bat test --tests tong.statmod.block.MagicForgeBlockBehaviorTest`
- [ ] Implement only what is needed later to satisfy these expectations
- [ ] Re-run the same test until green

### Task 2: Implement Magic Forge Block Runtime

**Files:**
- Create: `src/main/java/tong/statmod/block/MagicForgeAnvilBlock.java`
- Modify: `src/main/java/tong/statmod/block/ForgingBlocks.java`

**Interfaces:**
- Consumes: `ParticleTypes`, `VoxelShape`, `BlockBehaviour.Properties`
- Produces: `MagicForgeAnvilBlock` constructor accepting particle flavor and registered forge block instances

- [ ] Implement a reusable custom block with `noOcclusion`, explicit `VoxelShape`, and client `animateTick`
- [ ] Register both forge blocks through the new class
- [ ] Keep collision/outline easy to target in-world
- [ ] Run targeted block tests

### Task 3: Brighten Block Textures

**Files:**
- Modify: `src/main/resources/assets/statmod/textures/block/infusion_forge.png`
- Modify: `src/main/resources/assets/statmod/textures/block/enchantment_anvil.png`

**Interfaces:**
- Consumes: existing texture silhouettes
- Produces: brighter, higher-contrast variants with the same footprint

- [ ] Lighten the purple/magenta accents and improve value separation
- [ ] Preserve silhouette and UV compatibility with current models
- [ ] Re-open textures for a quick visual sanity check

### Task 4: Verify Resources and Regression Suite

**Files:**
- Modify: `src/test/java/tong/statmod/block/ForgingBlocksTest.java` if resource expectations need extension
- Modify: `src/test/java/tong/statmod/block/EnchantmentAnvilResourcesTest.java` if resource expectations need extension

**Interfaces:**
- Consumes: Tasks 1-3 outputs
- Produces: final verification coverage for forge block runtime + resources

- [ ] Run `.\gradlew.bat test --tests tong.statmod.block.*`
- [ ] Run `.\gradlew.bat compileJava`
- [ ] Report any manual in-game validation still pending

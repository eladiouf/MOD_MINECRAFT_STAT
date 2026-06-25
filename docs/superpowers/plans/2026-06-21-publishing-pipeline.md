# Publishing Pipeline — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix CI pipeline, bump version to 1.2.0, update CHANGELOG, and prepare store metadata for CurseForge/Modrinth publishing

**Architecture:** In-place fixes to `.github/workflows/build.yml` (JDK 21, game versions, loader, dependencies) + version bump in `gradle.properties` + CHANGELOG update + store metadata docs

**Tech Stack:** GitHub Actions, NeoForge 1.21.1, Gradle 8.10.2, CurseForge API, Modrinth API

---

### Task 1: Fix CI — JDK Version

**Files:**
- Modify: `.github/workflows/build.yml:18-22`

- [ ] **Step 1: Change JDK 17 to JDK 21**

Edit `.github/workflows/build.yml` lines 18-22:

```
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
```

→

```
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
```

- [ ] **Step 2: Verify the change**

Run: `git diff .github/workflows/build.yml`
Expected: JDK 21 visible in the diff, no other changes yet

- [ ] **Step 3: Commit**

```
git add .github/workflows/build.yml
git commit -m "fix(ci): upgrade JDK 17 to JDK 21 for NeoForge 1.21.1 compatibility"
```

---

### Task 2: Fix CI — CurseForge Versions & Dependencies

**Files:**
- Modify: `.github/workflows/build.yml:67-73`

- [ ] **Step 1: Update CurseForge game_versions and relations**

Edit `.github/workflows/build.yml` lines 67-73:

```
          game_versions: '1.20.1'
          release_type: release
          file_path: "statmod-*.jar"
          changelog: "See CHANGELOG.md"
          display_name: "STAT Mod ${{ github.ref_name }}"
          relations: |
            epic-fight:requiredDependency
```

→

```
          game_versions: '1.21.1'
          release_type: release
          file_path: "statmod-*.jar"
          changelog: "See CHANGELOG.md"
          display_name: "STAT Mod ${{ github.ref_name }}"
          relations: |
            tensura-reincarnated:requiredDependency
            irons-spellbooks:optionalDependency
            epic-fight:optionalDependency
```

- [ ] **Step 2: Verify the change**

Run: `git diff .github/workflows/build.yml`
Expected: `game_versions` now `1.21.1`, relations updated with Tensura/Iron's/Epic Fight

- [ ] **Step 3: Commit**

```
git add .github/workflows/build.yml
git commit -m "fix(ci): update CurseForge to MC 1.21.1 with correct dependencies"
```

---

### Task 3: Fix CI — Modrinth Versions, Loader & Dependencies

**Files:**
- Modify: `.github/workflows/build.yml:94-96`

- [ ] **Step 1: Update Modrinth game-versions, loaders and dependencies**

Edit `.github/workflows/build.yml` lines 94-96:

```
          game-versions: '1.20.1'
          loaders: forge
          dependencies: epic-fight(required)
```

→

```
          game-versions: '1.21.1'
          loaders: neoforge
          dependencies: |
            tensura-reincarnated(required)
            irons-spellbooks(optional)
            epic-fight(optional)
```

- [ ] **Step 2: Verify the change**

Run: `git diff .github/workflows/build.yml`
Expected: All three Modrinth fields updated

- [ ] **Step 3: Commit**

```
git add .github/workflows/build.yml
git commit -m "fix(ci): update Modrinth to NeoForge 1.21.1 with correct deps"
```

---

### Task 4: Bump Version to 1.2.0

**Files:**
- Modify: `gradle.properties:13`

- [ ] **Step 1: Change mod_version**

Edit `gradle.properties` line 13:

```
mod_version=1.0.0
```

→

```
mod_version=1.2.0
```

- [ ] **Step 2: Verify**

Run: `git diff gradle.properties`
Expected: `1.0.0` → `1.2.0`

- [ ] **Step 3: Commit**

```
git add gradle.properties
git commit -m "bump: v1.2.0"
```

---

### Task 5: Update CHANGELOG

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add v1.2.0 entry at top of CHANGELOG.md**

Insert after line 1 in `CHANGELOG.md`:

```
## [1.2.0] - 2026-06-21

### Added
- Unified magic tree — Iron's Spellbooks Phase 1 (common trunk + Fire school active)
- 8 magic schools structurally present (locked, pending Phase 2)
- Arcane and school point system with persistence
- IronSpellEventBridge for event-driven spell casting
- Puffish Skills mirror UI for magic tree
- `/magic` command for tree management
- Magic node unlock/gating with race affinity cost adjustment
- Client-side magic tree cache with sync on join

### Fixed
- Audit M1 — 10 critical/important/minor fixes from code review (065d57a)
- SwordSoaringClientModEventsMixin cleanup
- Magic state sync on player join/respawn

### Changed
- Port NeoForge 1.21.1 finalized (zero Forge residuals in src/)
- Perk gating integrates with Tensura skill level
- Build CI targets JDK 21, NeoForge 1.21.1

---
```

- [ ] **Step 2: Verify**

Run: `git diff CHANGELOG.md`
Expected: v1.2.0 entry at top with all listed changes

- [ ] **Step 3: Commit**

```
git add CHANGELOG.md
git commit -m "docs: add v1.2.0 changelog entry"
```

---

### Task 6: Create Store Metadata Files

**Files:**
- Create: `docs/stores/curseforge-description.md`
- Create: `docs/stores/modrinth-description.md`

- [ ] **Step 1: Create CurseForge metadata file**

Create `docs/stores/curseforge-description.md`:

```markdown
# STAT Mod

**STAT Mod** is a comprehensive stat and progression system for Minecraft NeoForge 1.21.1. It serves as the **progression authority** in a multi-mod RPG stack.

## Features

- **22 Stats** — 14 active stats (covered by 84 perks) + 8 magic stats (covered by the unified magic tree)
- **84 Perks** — 6 perks per active stat, with tiered progression
- **XP & Leveling** — Combat and non-combat XP with balanced progression curves
- **Unified Magic Tree** — Integrated with Iron's Spellbooks (Phase 1: common trunk + Fire school)
- **Racial System** — Full integration with Tensura Reincarnated (race modifiers, soul level sync, skill gates)
- **Puffish Skills UI** — Mirror UI for perk and magic tree management

## Dependencies

- **Tensura Reincarnated** (required)
- **Iron's Spellbooks** (recommended — enables magic tree)
- **Puffish Skills** (recommended — enables UI mirror)
- **Epic Fight** (optional — experimental)

## Installation

1. Install NeoForge 1.21.1
2. Drop the JAR into your `mods/` folder
3. Install required dependencies
4. Launch Minecraft

## License

MIT — Free to use, modify, and distribute.
```

- [ ] **Step 2: Create Modrinth metadata file**

Create `docs/stores/modrinth-description.md` with the same content as above.

- [ ] **Step 3: Create a store metadata README**

Create `docs/stores/README.md`:

```markdown
# Store Metadata

This directory contains prepared text for CurseForge and Modrinth project pages.

## Files

| File | Purpose |
|------|---------|
| `curseforge-description.md` | Long description for CurseForge project page |
| `modrinth-description.md` | Long description for Modrinth project page |

## Usage

Copy-paste the content into the respective store's project creation/edit page.

## Required Dependencies (on stores)

| Mod | Type | Platform |
|-----|------|----------|
| Tensura Reincarnated | Required | Both |
| Iron's Spellbooks | Optional | Both |
| Epic Fight | Optional | Both |
| Puffish Skills | Optional | Both |

## GitHub Secrets Required

| Secret | Source |
|--------|--------|
| `CURSEFORGE_TOKEN` | CurseForge API Keys |
| `MODRINTH_TOKEN` | Modrinth Settings → Tokens |
| `CURSEFORGE_PROJECT_ID` | From CurseForge project URL |
| `MODRINTH_PROJECT_ID` | From Modrinth project URL |
```

- [ ] **Step 4: Commit**

```
git add docs/stores/
git commit -m "docs: add store metadata for CurseForge and Modrinth"
```

---

### Task 7: Verify Build Passes

- [ ] **Step 1: Run build and tests locally**

Run: `./gradlew build test --no-daemon`
Expected: `BUILD SUCCESSFUL`, all tests pass

If the build fails, investigate and fix before proceeding.

- [ ] **Step 2: Final status check**

Run: `git status`
Expected: Clean working tree (no uncommitted changes)

Run: `git log --oneline -6`
Expected: Clean commit history showing all 6 commits:
```
docs: add store metadata for CurseForge and Modrinth
docs: add v1.2.0 changelog entry
bump: v1.2.0
fix(ci): update Modrinth to NeoForge 1.21.1 with correct deps
fix(ci): update CurseForge to MC 1.21.1 with correct dependencies
fix(ci): upgrade JDK 17 to JDK 21 for NeoForge 1.21.1 compatibility
```

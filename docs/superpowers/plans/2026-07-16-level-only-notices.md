# Level-only Notices Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep only compact, subdued level-up notices and correct the learned-spell search width.

**Architecture:** Filter ordinary XP notices at the authoritative server calculation and defensively at the client queue. Keep the existing wire format for compatibility, simplify the overlay to one level-up rendering path, and adjust only the search widget width.

**Tech Stack:** Java 17, Forge 47.4.10, Minecraft 1.20.1, JUnit 5, Gradle 8.8.

## Global Constraints

- Ordinary XP gain notices must never enter the normal server-to-client path.
- Only messages with `levelsGained() > 0` may enter the client queue.
- The level overlay is 160 by 20 pixels, lasts 60 ticks, and uses reduced opacity.
- The network message stays wire-compatible.
- The spell search width is exactly 112 pixels.

---

### Task 1: Filter notices to level-ups

**Files:**
- Modify: `src/test/java/tong/statmod/progression/xp/XpNoticeCalculationTest.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpNoticeCalculation.java`
- Modify: `src/test/java/tong/statmod/client/notice/ProgressNoticeQueueTest.java`
- Modify: `src/main/java/tong/statmod/client/notice/ProgressNoticeQueue.java`

**Interfaces:**
- Consumes: `XpNoticeCalculation.from(before, after, accepted)` and `ProgressNoticeQueue.offer(message, tick)`.
- Produces: unchanged method signatures with level-only output.

- [ ] **Step 1: Write failing server and client tests**

Change the stable-order calculation assertion to expect an empty list when XP
changes without a level, and add a queue test that offers a message with
`levelsGained == 0` and expects an empty snapshot.

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.progression.xp.XpNoticeCalculationTest --tests tong.statmod.client.notice.ProgressNoticeQueueTest --rerun-tasks
```

Expected: both new assertions fail because XP-only notices are still created.

- [ ] **Step 3: Implement minimal filtering**

In `XpNoticeCalculation.from`, compute `levelsGained` before adding and skip
when it is zero. In `ProgressNoticeQueue.offer`, return before queue mutation
when `!message.valid() || message.levelsGained() <= 0`.

- [ ] **Step 4: Run focused tests to verify GREEN**

Run the Step 2 command. Expected: all focused tests pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp/XpNoticeCalculation.java src/main/java/tong/statmod/client/notice/ProgressNoticeQueue.java src/test/java/tong/statmod/progression/xp/XpNoticeCalculationTest.java src/test/java/tong/statmod/client/notice/ProgressNoticeQueueTest.java
git commit -m "feat(ui): show progress notices only for levels"
```

### Task 2: Subdue the overlay and fit the search box

**Files:**
- Modify: `src/test/java/tong/statmod/client/notice/ProgressNoticeOverlayContractTest.java`
- Modify: `src/main/java/tong/statmod/client/notice/ProgressNoticeOverlay.java`
- Modify: `src/test/java/tong/statmod/client/notice/ProgressNoticeQueueTest.java`
- Modify: `src/main/java/tong/statmod/client/notice/ProgressNoticeQueue.java`
- Modify: `src/test/java/tong/statmod/mixin/IronInscriptionTableScreenMixinContractTest.java`
- Modify: `src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java`

**Interfaces:**
- Consumes: `ProgressNotice.alpha()` and the existing `EditBox` creation.
- Produces: compact level-only rendering and a 112-pixel search field.

- [ ] **Step 1: Write failing visual contract tests**

Assert the overlay source contains `WIDTH = 160`, `HEIGHT = 20`, separate
maximum alpha constants `110`, `150`, and `200`, and no reference to
`notice.statmod.xp`. Assert `LEVEL_DURATION == 60`. Assert the inscription
screen constructs its `EditBox` with `112, 14`.

- [ ] **Step 2: Run tests to verify RED**

```powershell
.\gradlew.bat test --tests tong.statmod.client.notice.ProgressNoticeOverlayContractTest --tests tong.statmod.client.notice.ProgressNoticeQueueTest --tests tong.statmod.mixin.IronInscriptionTableScreenMixinContractTest --rerun-tasks
```

Expected: failures report the old 190 by 26 overlay, 80-tick duration, XP
rendering branch, and 116-pixel search width.

- [ ] **Step 3: Implement compact rendering**

Set width/height/gap to `160/20/3`, set level duration to `60`, calculate
background/border/foreground alpha from `notice.alpha()` with maxima
`110/150/200`, render only `notice.statmod.level_up`, and draw text at
`x + 6, y + 6`. Change the `EditBox` width to `112`.

- [ ] **Step 4: Run focused tests to verify GREEN**

Run the Step 2 command. Expected: all focused tests pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/client/notice/ProgressNoticeOverlay.java src/main/java/tong/statmod/client/notice/ProgressNoticeQueue.java src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java src/test/java/tong/statmod/client/notice/ProgressNoticeOverlayContractTest.java src/test/java/tong/statmod/client/notice/ProgressNoticeQueueTest.java src/test/java/tong/statmod/mixin/IronInscriptionTableScreenMixinContractTest.java
git commit -m "feat(ui): subdue level notices and fit spell search"
```

### Task 3: Verify, publish, and deploy

**Files:**
- Verify: `build/libs/statmod-0.1.0+1.20.1.jar`
- Deploy: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods\statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: completed commits from Tasks 1 and 2.
- Produces: audited remote commit and hash-identical deployed JAR.

- [ ] **Step 1: Run full verification**

```powershell
.\gradlew.bat clean test build
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: Gradle reports `BUILD SUCCESSFUL` and smoke reports
`OK required-provider Forge GameTest server smoke`.

- [ ] **Step 2: Inspect the production JAR**

Confirm one production JAR, no duplicate entries, the notice and inscription
classes are present, and record SHA-256.

- [ ] **Step 3: Publish without force**

Fetch `origin/forge-1.20.1`, merge only if it advanced, then push
`HEAD:forge-1.20.1` without force.

- [ ] **Step 4: Deploy atomically**

Verify the resolved paths are inside `test-vrai`, move only the existing
`statmod-*.jar` into `test-vrai/statmod-backups`, copy the production JAR, and
require identical source and destination SHA-256 hashes.

- [ ] **Step 5: Verify final state**

Require local HEAD equals `origin/forge-1.20.1`, the worktree is clean, exactly
one STAT Mod JAR is deployed, and its hash equals the audited build hash.

# Dungeon Point Economy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scattered fixed dungeon point constants with a tested depth-aware reward and death-risk policy.

**Architecture:** Introduce pure `DungeonPointBalance` formulas, then make `DungeonPoints` a server adapter. Pass the actual floor through completion and death call sites so every calculation uses the same authoritative depth.

**Tech Stack:** Java 17, Forge 47.4.10, Minecraft 1.20.1, JUnit Jupiter 5.10.2.

## Global Constraints

- No entry tax or forced point deduction on ascent.
- Point-to-coin conversion remains 1:1.
- Existing combo, jackpot, flawless, FTB assist, persistence, sync, and zero-point ejection behavior remains.
- All formulas clamp invalid input safely and remain monotonic by floor.

---

### Task 1: Pure point balance policy

**Files:** Create `src/main/java/tong/statmod/dungeon/DungeonPointBalance.java` and `src/test/java/tong/statmod/dungeon/DungeonPointBalanceTest.java`.

- [ ] Write exact-value, monotonicity, cap, and clamp tests for mob, clear, boss, assist, and death calculations.
- [ ] Run the focused test and confirm compilation fails because the policy does not exist.
- [ ] Implement the formulas exactly as specified in the design.
- [ ] Run the focused test and confirm every calculation passes.

### Task 2: Server wiring

**Files:** Modify `DungeonPoints.java`, `DungeonProgress.java`, `DungeonRespawnHandler.java`; create `DungeonPointWiringContractTest.java`.

- [ ] Write a source contract requiring floor-aware `awardFloorClear`, `awardBoss`, and `applyDeathPenalty` calls.
- [ ] Run the contract and confirm it fails against fixed constants/signatures.
- [ ] Delegate reward/loss calculations to `DungeonPointBalance` and pass the known floor through all call sites.
- [ ] Run focused policy and wiring tests.

### Task 3: Documentation and release verification

**Files:** Modify `CHANGELOG.md`; verify all point economy changes.

- [ ] Document depth-scaled rewards and death penalties.
- [ ] Run `git diff --check` and scan for obsolete fixed constants.
- [ ] Run the complete test suite.
- [ ] Run `clean build`, count failures, inspect the final JAR, and commit the completed economy.


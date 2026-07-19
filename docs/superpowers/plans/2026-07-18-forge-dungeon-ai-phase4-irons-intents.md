# Dungeon AI Phase 4 — Iron's Spell Intents Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make dungeon casters choose real enabled Iron's Spells by tactical intent instead of treating every spell as a hostile projectile.

**Architecture:** Pure Java policy objects classify combat state into one of seven intents and map tactical profiles to ordered registry IDs. The Forge bridge resolves IDs at runtime, rejects missing or disabled spells, clamps spell level to both spell configuration and dungeon depth, and casts self/support actions without requiring an enemy hit. A focused room spellcaster goal supplies squad-local context and independent intent cooldowns.

**Tech Stack:** Java 17, Forge 47.4.4, Minecraft 1.20.1, Iron's Spells 3.16.2 API, JUnit 5.

## Global Constraints

- Iron's Spells remains a required dependency; addon spell classes are never imported directly.
- Squad support queries require the exact same `statmod_ai_squad` value and never cross chamber boundaries.
- Missing or disabled spells fall back to another spell in the same intent, then to direct damage.
- Defensive, mobility, support and summon actions do not require a hostile target hit.
- Spell levels stay between `getMinLevel()` and `getMaxLevel()` and scale from floor 1 through floor 100.
- Summons are limited to two living summons per squad and tagged for cleanup.
- Existing `MageRangedGoal` remains compatible through the original `cast(Mob, LivingEntity, String)` entry point.

---

### Task 1: Pure tactical intent policy

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellIntent.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellTacticalContext.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellIntentPolicy.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronSpellIntentPolicyTest.java`

**Interfaces:**
- Produces `IronSpellIntent choose(IronSpellTacticalContext context)`.
- Context fields are `role`, `healthFraction`, `criticalAlly`, `nearbyEnemyCount`, `targetDistance`, `summonBudgetAvailable`, and `hasHostileTarget`.

- [ ] **Step 1: Write the failing policy test.** Assert low health chooses `DEFENSE`, a critical ally chooses `ALLY_SUPPORT`, a threatened caster chooses `MOBILITY`, clusters choose `AREA_DAMAGE`, a budgeted necromancer chooses `SUMMON`, a hexer chooses `CONTROL`, and the safe default is `DIRECT_DAMAGE`.
- [ ] **Step 2: Verify red.** Run `.\gradlew.bat test --tests "tong.statmod.integration.ironspells.IronSpellIntentPolicyTest" --console=plain`; expect compilation failure because the intent types do not exist.
- [ ] **Step 3: Implement the minimal pure types.** Use the priority order defense, ally support, close-range mobility, clustered area, necromancer summon, controller control, direct damage.
- [ ] **Step 4: Verify green.** Run the focused Gradle test; expect all seven assertions to pass.
- [ ] **Step 5: Commit.** Commit the test and pure policy as `feat: choose tactical Iron spell intents`.

### Task 2: Registry-backed tactical profiles

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellProfile.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellCatalog.java`
- Modify: `src/main/java/tong/statmod/integration/ironspells/IronsCasterSpells.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronSpellCatalogTest.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronsCasterSpellsContractTest.java`

**Interfaces:**
- Produces `List<String> spellIds(IronSpellProfile profile, IronSpellIntent intent)`.
- Produces `boolean cast(Mob caster, LivingEntity hostileTarget, IronSpellProfile profile, IronSpellIntent intent, int floor)`.
- Preserves `boolean cast(Mob mage, LivingEntity target, String element)`.

- [ ] **Step 1: Write failing catalog and source-contract tests.** Require all seven intents, base IDs for fire/frost/storm/arcane/necromantic/holy profiles, `SpellRegistry.getSpell`, `spell.isEnabled()`, min/max clamping, and no direct addon imports.
- [ ] **Step 2: Verify red.** Run both focused tests; expect missing catalog/profile types and missing runtime validation.
- [ ] **Step 3: Implement deterministic ID profiles.** Include direct, area and control spells per school; shared defense (`shield`, `fortify`, `oakskin`), mobility (`teleport`, school steps/dashes), support (`heal`, `healing_circle`, `cleanse`, `haste`), and summon (`raise_dead`, `summon_vex`, `summon_polar_bear`, `summon_swords`).
- [ ] **Step 4: Expand the casting bridge.** Resolve each ID through the registry, skip `none`/disabled entries, choose a deterministic random enabled candidate, orient only intents that need aiming, and clamp the floor-derived level to spell bounds.
- [ ] **Step 5: Verify green.** Run both focused tests and the existing Iron integration tests.
- [ ] **Step 6: Commit.** Commit as `feat: cast registry-backed tactical Iron spells`.

### Task 3: Chamber-local spellcaster behavior

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/DungeonSpellcasterGoal.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonTacticalGoals.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonSpellcasterGoalContractTest.java`

**Interfaces:**
- Attaches to `ELEMENTAL_CASTER`, `BATTLE_CLERIC`, `NECROMANCER`, `HEXER`, and `ARCANE_ARTILLERY`.
- Counts allies only when their `SQUAD_TAG` matches exactly.
- Uses separate cooldown tags for attack, defense, support, mobility and summon categories.

- [ ] **Step 1: Write the failing wiring contract.** Require all five caster roles, exact squad filtering, context construction, the tactical cast overload, independent cooldown categories, and a two-summon squad cap.
- [ ] **Step 2: Verify red.** Run the focused contract; expect failure because the goal does not exist and caster roles are not wired.
- [ ] **Step 3: Implement the minimal goal.** Acquire visible non-creative players within 32 blocks, assess same-squad allies within 18 blocks, choose intent/profile, telegraph high-impact area/summon casts, and retain the current target only for hostile intents.
- [ ] **Step 4: Wire idempotently.** Map caster roles to `DungeonSpellcasterGoal` without changing non-caster roles or mobs outside managed Trial Dungeon actors.
- [ ] **Step 5: Verify green.** Run the focused contract and every dungeon AI test.
- [ ] **Step 6: Commit.** Commit as `feat: coordinate tactical Iron casters by chamber`.

### Task 4: Full phase validation

**Files:**
- Modify: `docs/forge-1.20.1-validation.md`

- [ ] **Step 1: Run focused tests.** Run `IronSpellIntentPolicyTest`, `IronSpellCatalogTest`, `IronsCasterSpellsContractTest`, and `DungeonSpellcasterGoalContractTest`.
- [ ] **Step 2: Run the clean build.** Run `.\gradlew.bat clean test build --console=plain`; expect `BUILD SUCCESSFUL`.
- [ ] **Step 3: Check exclusions.** Run `rg -n -i "cataclysm:" src/main/java/tong/statmod/config src/main/java/tong/statmod/dungeon src/main/resources/data/statmod`; expect no matches.
- [ ] **Step 4: Document evidence.** Record the commands, passing result, Iron's Spells version and strict chamber-squad behavior.
- [ ] **Step 5: Commit and push.** Commit as `docs: validate tactical Iron spell AI` and push branch `forge-1.20.1`.

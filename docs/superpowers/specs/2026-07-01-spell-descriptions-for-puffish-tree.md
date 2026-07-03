# Spell Descriptions for Puffish Magic Tree

**Date:** 2026-07-01
**Status:** Draft
**Mission:** M4 (Magic Tree — Phase 1)

## Problem

Puffish magic tree signature spell nodes show `"Learn Firebolt."` as description — no information about what the spell actually does. Players must exit the tree to discover spell effects.

## Solution

Replace the formulaic description with the actual spell guide text from each mod's language file, falling back to formulaic text when unavailable.

## Design

### 1. `SpellDescriptionProvider` (new class)

Package: `tong.statmod.integration.puffish`

Loads spell guide descriptions from all mod JARs in `libs/` at class initialization.

**Data sources:**

| JAR | Entries | Key format |
|---|---|---|
| `irons_spellbooks-1.21.1-3.16.1.jar` | 113 | `spell.irons_spellbooks.<name>.guide` |
| `wind_spellbooks-1.0.4.jar` | 7 | `spell.wind_spellbooks.<name>.guide` |
| `legendarymage-1.0.9.jar` | 10 | `spell.legendarymage.<name>.guide` |
| `gametechbcs_spellbooks-3.0.0-1.21.1.jar` | 18 | `spell.gametechbcs_spellbooks.<name>.guide` |
| `darkdoppelganger-3.3.0-1.21.1.jar` | 2 | `spell.irons_spellbooks.<name>.guide` (uses Iron's ns) |

**Extraction:** Read each JAR's `assets/<modid>/lang/en_us.json`, match keys with regex `"spell\.([a-z_]+)\.([a-z_]+)\.guide"\s*:\s*"(.+)"`, store as `modid:name → description` map.

**Hardcoded overrides** (static map, higher priority than JAR extraction):
- All ~30 `tensura:*` spells (Tensura JAR has no `.guide` entries)
- Both `spells_gone_wrong` spells (`nucreeper_strike`, `shotgun_creeper`)
- Any other spells missing from JAR extraction

**API:**
```java
public static String get(String spellId) // returns description or null
public static boolean has(String spellId)
```

### 2. `PuffishMagicTreeBuilder` changes

**`baseDescriptionFor()` — `SIGNATURE_SPELL` case:**
- Iterate `node.learnedSpells()` in order
- For each spellId, call `SpellDescriptionProvider.get(spellId)`
- First non-null result → use as base description
- No result found → keep current `"Learn " + titleFor(node) + "."` as fallback

**`requirementsLineFor()`** unchanged — appended to base description with `\n\n`.

### 3. DarkDoppelganger multi-ID handling

Nodes `darkdoppelganger:doppel_portal` and `darkdoppelganger:summon_doppel_minion` have `learnedSpells = {"darkdoppelganger:doppel_portal", "irons_spellbooks:doppel_portal"}`. The guide entry is under `irons_spellbooks:doppel_portal`. Iterating `learnedSpells()` in order handles this: `darkdoppelganger:` fails (no map entry), `irons_spellbooks:` succeeds.

## Files changed

| File | Change |
|---|---|
| `src/main/java/tong/statmod/integration/puffish/SpellDescriptionProvider.java` | **New** — JAR extraction + overrides |
| `src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java` | Update `baseDescriptionFor()` SIGNATURE_SPELL case |
| `src/test/java/tong/statmod/integration/puffish/SpellDescriptionProviderTest.java` | **New** — verify extraction + overrides |

## Out of scope

- Perk descriptions (existing system works independently)
- `extraDescription` Puffish field (future enhancement)
- French translation of descriptions (user chose English)
- Non-Puffish UI (SpellDescriptionsProvider is reusable but only Puffish consumes it now)

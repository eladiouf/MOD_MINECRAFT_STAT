# Forge 1.20.1 Iron's Spells core attribute bridge design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Iron's Spells target:** 3.16.2  
**Status:** approved direction

## 1. Goal

Make the first four core magical statistics visibly affect the pinned Iron's
Spells runtime without introducing a second mana system or depending on Iron's
Java classes. This is the first slice of the Iron's Spells milestone and a
prerequisite for magical XP and Curios grimoire casting.

The bridge covers Arcane Power, Casting Speed, Mana Pool, and Magic Resistance.
Erudition remains a synchronized foundation. Fire, Water, Earth, and Air
Affinity are removed completely because they do not provide a distinct useful
progression axis for this modpack. The screen must describe the remaining
statistics and newly active effects truthfully after the bridge ships.

## 2. Approaches considered

### 2.1 Registry-only attribute bridge — selected

Resolve Iron's public attributes through `ForgeRegistries.ATTRIBUTES`, apply
stable transient modifiers, and reuse STAT Mod's existing refresh lifecycle.
This avoids direct class loading, preserves optional-mod safety, and follows the
same proven pattern as Epic Fight, Puffish Attributes, and ParCool attributes.

### 2.2 Full magical XP and effects in one slice — rejected for now

This would also compile against `SpellOnCastEvent`, migrate the roster, apply
anti-farm rules, and introduce magical rewards in the same change. It creates a
larger failure surface and requires a reproducible Iron API compile dependency
before the attribute formulas have been validated independently.

### 2.3 Curios `V` casting first — rejected for now

This addresses the original casting symptom sooner, but it would build a
server-side casting coordinator on top of magical statistics that still have
no runtime meaning. Casting remains the next major integration after core
magical progression.

## 3. Attribute mapping and supported defaults

All curves are linear from zero bonus at stat level 0 to the configured maximum
at level 100. Levels are defensively bounded to `[0, 100]` by the existing
scaling utility. Amounts below are `MULTIPLY_BASE` modifier amounts.

| STAT Mod stat | Iron's Spells registry ID | Default at level 100 | Result with Iron's default base |
|---|---|---:|---|
| Arcane Power | `irons_spellbooks:spell_power` | `+1.00` | 1.0 → 2.0 spell-power multiplier |
| Casting Speed | `irons_spellbooks:cast_time_reduction` | `+0.30` | 1.0 → 1.30 reduction attribute |
| Casting Speed | `irons_spellbooks:cooldown_reduction` | `+0.20` | 1.0 → 1.20 reduction attribute |
| Mana Pool | `irons_spellbooks:max_mana` | `+2.00` | 100 → 300 maximum mana |
| Mana Pool | `irons_spellbooks:mana_regen` | `+0.50` | 1.0 → 1.50 regeneration multiplier |
| Magic Resistance | `irons_spellbooks:spell_resist` | `+0.50` | 1.0 → 1.50 resistance attribute |

The bridge deliberately excludes `summon_damage` and `casting_movespeed`.
Summon damage needs a summoner-specific policy, while casting movement speed
must not silently duplicate Agility. School-specific power and resistance
attributes are intentionally excluded: STAT Mod will not recreate elemental
affinities under another name.

## 4. Roster simplification and save compatibility

The same implementation slice removes `FIRE_AFFINITY`, `WATER_AFFINITY`,
`EARTH_AFFINITY`, and `AIR_AFFINITY` from `StatType`, then removes the now-empty
`ELEMENTAL_SPECIALIZATION` family. The canonical roster becomes 19 statistics
in five families.

The player-stat schema increments from 1 to 2. Loading an older save remains
safe: the deserializer reads only current `StatType` entries, so the four old
NBT compounds are ignored and every unrelated value is preserved. Saving the
player again writes only the 19 current entries. No replacement stat, refund,
conversion, or hidden compatibility field is created because affinities never
had spendable points or an implemented gameplay effect.

Commands stop accepting the four removed IDs automatically through enum-backed
parsing. Snapshots, screen sections, presentation catalogs, translations, and
tests are updated to expect 19 statistics and five families. The elemental
family button disappears instead of rendering an empty section.

## 5. Server configuration

`StatModServerConfig` gains a `magic` section with six bounded values:

- `arcanePowerSpellPowerBonusAt100`, default `1.00`, range `[0, 10]`;
- `castingSpeedCastTimeBonusAt100`, default `0.30`, range `[0, 0.90]`;
- `castingSpeedCooldownBonusAt100`, default `0.20`, range `[0, 0.90]`;
- `manaPoolCapacityBonusAt100`, default `2.00`, range `[0, 20]`;
- `manaPoolRegenBonusAt100`, default `0.50`, range `[0, 10]`;
- `magicResistanceBonusAt100`, default `0.50`, range `[0, 0.90]`.

Reduction and resistance caps remain below `1.0` to avoid degenerate instant
casts, zero cooldowns, or complete immunity. Configuration reload schedules a
refresh for every connected server player on the server thread. Invalid values
are handled by Forge's bounded config values and never enter modifier math.

## 6. Components and boundaries

### 6.1 `MagicAttributeTarget`

An enum owns each registry ID, a unique stable modifier UUID, its source stat,
and its configuration-backed maximum. It exposes a pure amount calculation from
`PlayerStats`. No Iron class or `RegistryObject` appears in this type.

### 6.2 `AttributeEffectLevels`

The level snapshot expands to include the four affected magical statistics.
`XpAwardService` can therefore detect a magical level change later without a
second refresh mechanism. Existing physical fields and equality semantics stay
unchanged.

### 6.3 `PlayerAttributeEffects`

The existing refresh service iterates `MagicAttributeTarget` after stamina and
mobility targets. For every target it:

1. resolves the attribute by registry ID;
2. skips a missing attribute or missing player instance;
3. removes STAT Mod's stable UUID;
4. adds one transient `MULTIPLY_BASE` modifier only when the computed amount is
   positive.

Repeated refreshes are idempotent. The bridge contains no direct Iron API
reference, but Iron's Spells is an explicit mandatory runtime dependency for
the Forge remake. `mods.toml` requires `irons_spellbooks` 3.16.2 or newer on
both sides and orders STAT Mod after it. Forge therefore rejects a missing or
unsupported Iron installation before world loading.

### 6.4 Refresh lifecycle

The bridge reuses all existing refresh points:

- player login;
- respawn and capability copy;
- dimension change;
- operator stat mutation;
- accepted automatic XP that changes an affected level.

A small config-reload event handler adds the missing global refresh point. It
acts only for STAT Mod's server config and schedules one pass over the current
server player list.

## 7. Client presentation

The French and English descriptions for Arcane Power, Casting Speed, Mana Pool,
and Magic Resistance change from future-foundation language to their actual
Iron attribute effects. Their `StatDisplayState` becomes `ACTIVE`.

Erudition remains `FOUNDATION`. The four affinities and their elemental family
are absent. The UI must not claim school-specific progression, magical XP,
spell unlocks, or Curios casting in this slice.

No new client packet is required. The authoritative level snapshot already
drives the screen; Iron synchronizes its player attributes through Forge.

## 8. Error handling and compatibility

- A missing Iron installation is rejected by Forge's mandatory dependency gate.
- An unexpectedly missing Iron attribute is skipped without logging every refresh.
- A registry ID mismatch is detected by contract tests against the audited
  3.16.2 identifiers and by the full-profile smoke test.
- Stable UUIDs are unique across stamina, mobility, and magic targets.
- Zero-level or zero-config bonuses remove any previous STAT Mod modifier.
- The bridge never writes current mana, refills mana on refresh, or creates a
  STAT Mod mana capability.
- No Puffish magic attribute is applied, preventing duplicate spell scaling.
- No spell school is classified and no addon compatibility claim is added.
- Schema-1 affinity NBT is ignored without affecting the 19 retained stats.

## 9. Verification

Focused tests cover:

- the six exact target IDs, source stats, UUID uniqueness, and default maxima;
- zero, mid-level, level-100, negative, and over-cap scaling inputs;
- all four affected stats in `AttributeEffectLevels`;
- modifier replacement and `MULTIPLY_BASE` wiring;
- config bounds and config-reload refresh wiring;
- exactly 19 stats in five non-empty families, with no affinity IDs;
- schema-1 loading preserves every retained stat and schema-2 saving omits the
  four retired affinity keys;
- `ACTIVE` presentation for the four implemented stats and `FOUNDATION` for
  Erudition;
- absence of direct `io.redspace.ironsspellbooks` imports in the bridge.

Release gates are the complete JUnit suite, clean Forge build, JAR surface
inspection, required Epic Fight/Puffish GameTest, and a full `test-vrai` launch
with Iron's Spells 3.16.2. In the client, compare Iron attribute values at level
0, an intermediate level, and level 100; relog and change dimension to confirm
that bonuses neither disappear nor stack. Current mana must not be refilled by
any refresh action.

## 10. Explicit exclusions and next slices

This slice does not add magical XP, school progression, spell perks, spell
unlock rules, mana spending rules, new HUD elements, or Curios casting.
The next delivery order is:

1. magical XP from validated Iron cast events;
2. equipment-independent current-spell casting through the Curios grimoire;
3. perk and magic-tree gates.

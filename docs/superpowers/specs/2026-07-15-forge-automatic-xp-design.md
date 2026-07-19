# Forge 1.20.1 Automatic XP Design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** approved design

## 1. Goal

Make the existing player statistics foundation progress from observable Forge
and vanilla gameplay. This slice activates the 14 non-magical stats, calculates
XP in proportion to meaningful effort, limits common farming patterns, and
documents a data-driven compatibility contract for large Epic Fight addon
collections.

The nine magical stats remain inactive until the dedicated Iron's Spells
integration. Epic Fight and every Epic Fight addon remain optional.

## 2. Active and deferred stats

### 2.1 Active in this slice

- `brute_force`
- `blade_technique`
- `rapidite`
- `agility`
- `physical_resistance`
- `physical_endurance`
- `precision`
- `tracking`
- `keen_senses`
- `intimidation`
- `willpower`
- `forging`
- `cooking`
- `alchemy`

### 2.2 Deferred until Iron's Spells

- `arcane_power`
- `casting_speed`
- `mana_pool`
- `erudition`
- `magic_resistance`
- `fire_affinity`
- `water_affinity`
- `earth_affinity`
- `air_affinity`

No vanilla proxy action may award XP to a deferred stat.

## 3. Architecture

### 3.1 Normalized actions

Forge event adapters emit immutable `XpAction` values rather than mutating
player stats directly. The action model contains only normalized data required
by reward calculation, such as final damage, blocked damage, weapon class,
combo length, target kind, maximum target health, fall distance, crafted output
properties, or potion effects.

The supported action kinds are:

- melee damage dealt;
- projectile or explicitly ranged damage dealt;
- physical combat damage received;
- shield damage blocked;
- qualifying combo hit;
- controlled landing;
- hostile kill;
- dangerous-target kill;
- first biome discovery;
- equipment crafted;
- cooked food collected;
- brewed potion collected;
- low-health survival.

### 3.2 Pure reward policy

`XpRewardPolicy` converts one action into zero or more immutable
`StatXpAward(stat, amount, reason)` values. It has no player, world, Forge event,
clock, capability, or network dependency. Formula boundary tests can therefore
exercise every reward without launching Minecraft.

### 3.3 Player progression state

A separate `PlayerXpState` capability owns automatic-XP bookkeeping. It does not
duplicate stat levels or XP.

Persistent state:

- the set of biome resource identifiers already rewarded.

Transient state:

- current melee combo length and last qualifying hit tick;
- peak fall distance and whether fall damage occurred before landing;
- Willpower and Agility cooldown deadlines;
- per-stat rolling XP windows;
- per-stat and per-opponent PvP rolling XP windows.

Only biome discoveries are serialized. Transient state resets on reconnect,
server restart, respawn, or dimension replacement. This is acceptable because
each transient window is short and all per-event rewards are independently
capped.

### 3.4 Award service

`XpAwardService` is the sole automatic mutation entry point. For one normalized
action it:

1. verifies the server-side player is eligible;
2. asks the pure policy for proposed awards;
3. applies cooldown and rolling-window limits through `PlayerXpState`;
4. batches all accepted awards into the existing `PlayerStats` capability;
5. sends one complete `StatsSnapshotMessage` when at least one value changed.

An action that grants both Tracking and Intimidation still produces one network
sync.

### 3.5 Forge adapters

Event subscribers are split by responsibility:

- `CombatXpEvents` handles damage, shields, combos, low health, and kills;
- `ExplorationXpEvents` handles fall tracking and biome discovery;
- `CraftingXpEvents` handles crafting, smelting, and brewed potions.

Adapters perform side checks and translate Forge objects into normalized data.
They contain no XP formula constants.

## 4. Reward sources and formulas

All fractional results round upward after validating that the input is finite
and strictly positive.

### 4.1 Combat offense

#### Brute Force

A successful direct melee hit whose held item is classified as a heavy weapon
awards:

```text
clamp(ceil(finalDamage * 2), 1, 20)
```

#### Blade Technique

A successful direct melee hit whose held item is classified as a blade awards:

```text
clamp(ceil(finalDamage * 2), 1, 20)
```

A direct melee item classified as both heavy and blade is ambiguous and awards
neither Brute Force nor Blade Technique.

#### Precision

Damage whose direct source is a projectile owned by the player, or whose held
item is explicitly classified as a precision weapon, awards:

```text
clamp(ceil(finalDamage * 2), 1, 20)
```

The same damage event cannot also award a melee weapon stat.

#### Rapidité

Only successful direct melee hits participate. A combo starts at one and resets
when more than 40 server ticks elapse between qualifying hits. Starting with the
third hit, the award is:

```text
clamp(2 + comboLength - 3, 2, 6)
```

Changing targets does not reset the combo. A blocked, canceled, zero-damage, or
ineligible-target hit does not advance it.

### 4.2 Combat defense

#### Physical Resistance

Final post-mitigation physical combat damage received from a valid hostile,
projectile owner, PvP opponent, or owned explosion awards:

```text
clamp(ceil(finalDamage * 2), 1, 20)
```

Fire, magic, drowning, starvation, void, fall, self-inflicted, and ownerless
environmental damage are excluded.

#### Physical Endurance

A successful shield event with positive blocked damage awards:

```text
clamp(ceil(blockedDamage * 2), 1, 20)
```

#### Willpower

After an eligible combat hit, if the player survives and has at most 30 percent
of maximum health, award:

```text
clamp(ceil(finalDamage * 2), 1, 20)
```

The reward has a 400-tick cooldown. Damage that does not qualify for Physical
Resistance is also ineligible for Willpower.

### 4.3 Movement

#### Agility

The exploration adapter records peak `fallDistance` while the player is
airborne and records any final fall damage received. On the first grounded tick,
a peak distance of at least five blocks with zero fall damage awards:

```text
clamp(floor(peakFallDistance - 3), 1, 15)
```

The reward has a 100-tick cooldown. Teleportation, creative flight, elytra
flight, spectator flight, and dimension transfer clear the tracked fall without
rewarding it.

### 4.4 Hunting and mental stats

#### Tracking

Killing a hostile mob awards:

```text
clamp(ceil(targetMaxHealth / 5), 2, 20)
```

#### Intimidation

The same kill also awards Intimidation when the target is a recognized boss or
has at least 100 maximum health:

```text
clamp(ceil(targetMaxHealth / 4), 1, 50)
```

#### Keen Senses

Once every 20 server ticks, the exploration adapter reads the player's current
biome resource identifier. A valid identifier absent from the persistent
discovery set awards 10 XP and is immediately added to that set. The first
biome observed after a new player joins is a valid discovery.

### 4.5 Crafting and provisioning

#### Forging

`PlayerEvent.ItemCraftedEvent` awards Forging only when the output belongs to
the public forgeable-equipment tag. Per crafted output stack:

```text
clamp(ceil(max(1, maxDamage) / 100) * outputCount, 1, 20)
```

#### Cooking

`PlayerEvent.ItemSmeltedEvent` awards Cooking only when the output is edible:

```text
clamp(outputCount * 2, 1, 20)
```

#### Alchemy

`PlayerBrewedPotionEvent` awards nothing for a potion with no resolved effects.
Otherwise:

```text
clamp(5 + 2 * effectCount + sum(effectAmplifier), 1, 15)
```

## 5. Eligibility and anti-farm rules

Automatic rewards require a real `ServerPlayer` in survival or adventure mode.
Creative players, spectators, fake players, and client-side events are ignored.

Combat targets are ineligible when they are:

- armor stands or non-living targets;
- owned or tamed by the acting player;
- allied to the acting player;
- invulnerable, already dead, or associated with canceled/zero damage.

Each stat has a rolling budget of 200 accepted XP over 1,200 server ticks. When
an award exceeds the remaining budget, only the remaining amount is accepted.
An exhausted budget rejects further XP until older entries leave the window.

PvP additionally has a rolling budget of 25 accepted XP per source player,
target player, and stat over 1,200 ticks. The global per-stat budget still
applies. PvP XP is therefore allowed for real duels but bounded for repeated
boosting with one opponent.

Reward amounts, cooldowns, and budgets are constants in this slice. A server
configuration is explicitly deferred until real playtest data exists.

## 6. Data-driven weapon and equipment classification

STAT Mod publishes four item tags:

- `statmod:heavy_weapons`
- `statmod:blade_weapons`
- `statmod:precision_weapons`
- `statmod:forgeable_equipment`

Default tag resources include the appropriate vanilla weapons, tools, and armor.
External datapacks and mods extend them with `replace: false`.

Classification rules:

1. projectile causality has priority over held-item melee classification;
2. a direct item in both heavy and blade tags is ambiguous and grants neither;
3. the precision tag handles addon ranged attacks that arrive as direct damage;
4. the forgeable tag is independent and may overlap any combat tag;
5. an unclassified item functions normally but grants no weapon-specialization
   or Forging XP.

Ambiguous registry identifiers are logged at most once per server session to
avoid log spam.

## 7. Epic Fight addon compatibility contract

Epic Fight is not referenced by Java imports, reflection, mixins, metadata
dependencies, or mandatory runtime checks in this slice. Standard Forge damage
events are the compatibility boundary.

Create `docs/compatibility/epic-fight-addon-xp.md` containing:

- the four tag meanings and exact precedence rules;
- complete datapack JSON examples using `replace: false`;
- examples for a heavy weapon, blade, ranged weapon, and forgeable armor;
- client and dedicated-server test steps for damage, combo, projectile,
  crafting, death, relog, and dimension change;
- instructions for identifying an addon that bypasses standard Forge damage
  events;
- the information required in a compatibility report.

Create `docs/compatibility/epic-fight-addon-matrix.csv` with these columns:

```text
addon,version,weapon_tags,melee_xp,combo_xp,projectile_xp,crafting_xp,client_test,server_test,status,notes
```

Allowed status values are `untested`, `compatible`, `partial`, and
`incompatible`. The initial document is a reusable template; this implementation
does not claim that untested addons are compatible.

## 8. Error handling

- Non-finite, non-positive, or structurally invalid action magnitudes yield no
  proposed award.
- Every formula clamps its result before it reaches player data.
- Missing stats or XP-state capabilities cause the action to be ignored without
  a crash or partial mutation.
- Unknown item registry identifiers remain unclassified.
- Invalid biome identifiers are neither stored nor rewarded.
- Missing or empty potion effects produce no Alchemy reward.
- One failed proposed award does not prevent other valid awards from the same
  action.
- Network synchronization happens only after at least one positive mutation.

## 9. Testing and verification

Automated tests cover:

- every formula at its minimum, typical value, and maximum;
- invalid, zero, negative, infinite, and oversized magnitudes;
- heavy/blade ambiguity and classification precedence;
- combo start, third-hit activation, cap, timeout, and invalid-hit behavior;
- Agility minimum fall, cooldown, fall-damage rejection, and state reset;
- Willpower threshold and cooldown;
- global and per-opponent rolling-window partial acceptance and expiry;
- single-sync batching for multi-stat actions;
- persistent biome discovery NBT round trips and duplicate rejection;
- default item tag resource presence and JSON structure;
- no automatic awards for all nine deferred magical stats;
- no active Epic Fight or addon Java dependency;
- compatibility guide examples and matrix header/status documentation.

A fresh `gradlew clean build` and the existing clean-foundation guard must pass.
The final JAR must contain the reward policy, XP state provider, award service,
event adapters, and four tag resources. The 74 downloaded Iron's Spells addon
JARs and their hashes must remain unchanged.

## 10. Out of scope

- Iron's Spells XP, elemental affinity XP, mana, spell learning, or casting;
- direct Epic Fight API integration, skill events, dodges, guards beyond Forge's
  standard shield event, or weapon capability inspection;
- automatic scanning or automatic classification of installed addon JARs;
- a server configuration GUI or config file for XP values;
- HUD, toast, sound, chat, or level-up presentation;
- passive stat effects, perks, fatigue, or weapon mastery;
- claims of compatibility for addons not exercised by the documented matrix.

## 11. Acceptance criteria

1. All 14 active stats gain XP from their specified valid actions.
2. No deferred magical stat gains automatic XP.
3. Rewards use the exact formulas, caps, cooldowns, and rolling windows in this
   specification.
4. Multi-stat actions mutate atomically and send one client snapshot.
5. Biome discoveries survive save/reload and cannot reward twice.
6. Default vanilla equipment is classified by the four shipped tags.
7. Epic Fight addons can classify items using only documented datapack files.
8. Ambiguous, unknown, canceled, allied, fake-player, and invalid actions fail
   safely without double rewards.
9. The compatibility guide and honest untested matrix template ship with the
   source.
10. All tests, the Forge build, JAR inspection, and the 74-addon preservation
    guard pass from a clean tree.

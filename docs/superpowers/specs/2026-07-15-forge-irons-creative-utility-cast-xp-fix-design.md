# Forge 1.20.1 Iron creative and utility cast XP fix design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Iron's Spells target:** `1.20.1-3.16.2`  
**Status:** approved design

## 1. Problem and confirmed cause

The deployed cast-XP bridge produced no notification during the user's client
test. The `test-vrai` log records that player `tata` entered Creative mode at
15:06:07. The event adapter then called the general automatic-XP eligibility
path, whose contract rejects every creative player.

The missing notification was therefore caused by the creative-mode gate, not
by target acquisition, damage, or the type of spell cast.

The current integration already observes Iron's committed `SpellOnCastEvent`.
That event represents a completed cast and does not require a hit. Offensive
and non-offensive spells use the same event.

## 2. Goal

Allow a real player to receive Iron spellbook-cast XP in Creative mode while
preserving the anti-cheat rules for every other automatic-XP source.

Every eligible committed spellbook cast counts, including:

- direct damage and projectile spells;
- shields, barriers, and defensive wards;
- healing and buff spells;
- teleportation and movement spells;
- summons and constructs;
- crowd control and other utility spells that deal no damage.

No enemy, target, damage event, successful hit, heal amount, summon result, or
other secondary effect is required.

## 3. Chosen approach

Add a dedicated spell-cast entry point to `XpAwardService` instead of weakening
the general `isEligible` rule.

### General automatic XP

`XpAwardService.isEligible(ServerPlayer)` remains unchanged. It rejects:

- null players;
- Forge `FakePlayer` instances;
- Creative players;
- Spectator players.

Combat, exploration, crafting, cooking, alchemy, and every existing
non-magical adapter continue to use this rule.

### Iron spell-cast XP

Add `XpAwardService.awardSpellCast(ServerPlayer, XpAction, long)`. It accepts
only an action whose kind is exactly `SPELL_CAST` and uses a narrower player
eligibility rule:

- null players are rejected;
- `FakePlayer` instances are rejected;
- Spectator players are rejected;
- Creative and Survival/Adventure players are accepted.

The method delegates accepted actions to the same internal mutation pipeline
used by `award`. It cannot be used to grant combat, exploration, or crafting XP
to a creative player.

## 4. Data flow

```text
Iron validates and commits a spellbook cast
  -> SpellOnCastEvent
  -> IronSpellXpEvents validates server player, SPELLBOOK source,
     non-blank spell ID, and positive original spell level
  -> XpAction.spellCast(original level, original mana cost)
  -> XpAwardService.awardSpellCast
  -> spell-only eligibility (Creative allowed; Fake/Spectator rejected)
  -> existing reward policy and rolling limits
  -> existing PlayerStats mutation
  -> existing attribute refresh, snapshot, and XP notifications
```

`IronSpellXpEvents` no longer calls the general `isEligible` method before the
spell-specific service. Eligibility must have one owner so the adapter cannot
contradict the service.

## 5. Utility-spell contract

STAT Mod does not classify spells as offensive, defensive, healing, mobility,
summoning, or utility. It does not subscribe to `SpellDamageEvent`,
`SpellHealEvent`, `SpellSummonEvent`, or `SpellTeleportEvent` for cast XP.

The only success signal is Iron's committed `SpellOnCastEvent`. Consequently,
a shield spell and a damage spell with the same original level and mana cost
receive the same core cast rewards. This keeps addon spells compatible without
maintaining an incomplete spell-ID table.

Rejected pre-casts, insufficient mana, active cooldown, silence, invalid
target, canceled casts, and casts that never reach `SpellOnCastEvent` receive
no XP.

The existing source filter remains exact: only `CastSource.SPELLBOOK` counts.
Scrolls, swords, commands, mobs, and `NONE` remain excluded.

## 6. Rewards, limits, and notifications

The existing reward formulas remain unchanged:

- Arcane Power: `clamp(2 + originalSpellLevel, 3, 12)`;
- Casting Speed: `clamp(1 + ceil(originalSpellLevel / 2), 2, 6)`;
- Mana Pool for positive cost: `clamp(ceil(originalManaCost / 10), 1, 15)`.

Creative spell casts still pass through:

- the level-100 ceiling;
- the 200 XP per statistic over 1,200 ticks rolling budget;
- defensive input validation;
- the existing one-snapshot mutation batch;
- the existing per-stat XP notices.

No notice is expected when all proposed statistics are already level 100 or
their rolling budgets are exhausted. A successful accepted cast normally
produces two notices, or three when its original mana cost is positive.

## 7. Testing

Pure/service contract tests must prove:

- general eligibility still rejects Creative players;
- spell-cast eligibility does not reject Creative players;
- spell-cast eligibility still rejects Spectator and `FakePlayer` players;
- `awardSpellCast` rejects null and non-`SPELL_CAST` actions;
- the generic `award` path still uses general eligibility;
- the Iron adapter delegates exactly once to `awardSpellCast` and no longer
  pre-filters through general eligibility.

The Iron adapter contract must also prove it contains no dependency on:

- target lookup;
- damage, heal, summon, or teleport events;
- spell school;
- entity hit results.

The full build, JAR isolation inspection, and required-provider GameTest smoke
remain mandatory. The rebuilt JAR replaces only STAT Mod in `test-vrai` after
a timestamped backup.

Manual acceptance uses Creative mode and verifies:

1. a damage spell produces cast XP without requiring a hit;
2. a shield or barrier produces the same categories of cast XP;
3. a healing, teleport, or summon spell also produces cast XP;
4. an invalid/cooldown cast produces none;
5. switching to Spectator prevents the reward;
6. non-spell automatic XP remains disabled in Creative;
7. values and notifications remain synchronized after save/reload.

## 8. Out of scope

- rewarding actual damage, healing, blocked damage, summon lifetime, or target
  count separately;
- allowing general combat/crafting/exploration XP in Creative;
- scroll or spellblade cast XP;
- school-specific progression or retired affinities;
- changing Iron's targeting, mana, cooldown, or cast execution;
- Curios-slot `V` casting behavior;
- Erudition learning XP or Magic Resistance damage-received XP.

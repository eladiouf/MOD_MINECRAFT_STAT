# Adventurer Healer Regeneration Balance

## Goal

Reduce the excessive regeneration granted by rival adventurer healers without weakening their direct healing or unrelated support abilities.

## Validated balance

- A normal direct heal grants Regeneration I for 2 seconds (40 ticks).
- Sanctuary, the emergency group heal, grants Regeneration I for 4 seconds (80 ticks).
- Regeneration amplifier `0` is used in both cases; Minecraft displays this as Regeneration I.
- Direct healing amounts, cooldowns, activation thresholds, absorption, cleanse, resistance, strength and speed remain unchanged.

## Scope

Only `HealPartyGoal` regeneration effects change. No other adventurer role, dungeon enemy, player regeneration source or Iron's Spellbooks spell is affected.

## Verification

A focused automated contract test must fail against the old durations/amplifiers and pass after the change. The complete Forge 1.20.1 test suite and build must then pass before deployment. The resulting JAR must be identical on the development build, client and dedicated server.

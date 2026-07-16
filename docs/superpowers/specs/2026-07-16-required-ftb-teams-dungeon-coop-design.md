# Required FTB Teams Dungeon Co-op Design

**Date:** 2026-07-16
**Target:** Minecraft Forge 1.20.1, FTB Teams Forge 2001.3.2+

## Goal

Make FTB Teams authoritative and mandatory for every cooperative Trial Dungeon rule.

## Product rules

- `ftbteams` is a required client/server dependency; STAT Mod refuses to load without it.
- Two players are teammates only when the server-side FTB Teams manager says so.
- Floor 0 remains a public city. Every challenge floor can contain only one FTB team.
- Completion, boss rewards, assist points, death/retry protection, and boss cooldowns apply only to the triggering player's team members on that floor.
- Players enter voluntarily. Joining a team never forcibly teleports its members.
- If the FTB Teams manager is unexpectedly unavailable, dungeon admission fails closed and no unrelated players are treated as teammates.

## Architecture

`FTBTeamsBridge` directly imports the 1.20.1 API. It exposes manager readiness, strict team equality, a display name, and a filtered list of teammates on a floor. Dungeon systems consume that single boundary instead of duplicating API calls.

Challenge-floor admission is a pure, unit-tested policy. The teleport handler applies it before generation or encounter mutation. Existing progression and reward code keeps per-player persistence while selecting recipients through the bridge.

## Verification

- Unit tests cover strict team identity and floor admission policy independently of Minecraft runtime objects.
- Contract tests ensure the required dependency and direct API wiring remain present.
- Full Gradle tests and a clean production build must pass.


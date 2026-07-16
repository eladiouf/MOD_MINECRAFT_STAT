# Required FTB Teams Dungeon Co-op Design

**Date:** 2026-07-16
**Target:** Minecraft Forge 1.20.1, FTB Teams Forge 2001.3.2+

## Goal

Make FTB Teams authoritative and mandatory for every cooperative Trial Dungeon rule.

## Product rules

- `ftbteams` is a required client/server dependency; STAT Mod refuses to load without it.
- Two players are teammates only when the server-side FTB Teams manager says so.
- Floor 0 and every challenge floor may contain multiple FTB teams simultaneously.
- A wave or boss is a shared encounter. Completion, boss rewards, death/retry protection, and boss cooldowns apply to every living player present on the floor, regardless of team.
- Kill points remain personal. Assist points are shared only with present members of the killer's FTB team.
- Players enter voluntarily. Joining a team never forcibly teleports its members.
- If the FTB Teams manager is unexpectedly unavailable, players may still enter and complete shared encounters, but no assist points are shared with unrelated players.

## Architecture

`FTBTeamsBridge` directly imports the 1.20.1 API. It exposes manager readiness, strict team equality, and a filtered list of teammates on a floor. Only team-specific assist distribution consumes that boundary.

Challenge-floor entry is public and never filters by team. Existing progression remains per player, while shared encounter completion iterates over every player on the floor. Wave scaling already counts all players present.

## Verification

- Unit and contract tests cover public challenge-floor entry, shared completion recipients, team-only assists, death continuity, and shared boss cooldowns.
- Contract tests ensure the required dependency and direct API wiring remain present.
- Full Gradle tests and a clean production build must pass.

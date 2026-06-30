# Portfolio Backlog

This file is the top-level durable backlog surface for the studio.

## Backlog Buckets

It should group work by:

- studio core work
- project onboarding work
- active project initiatives
- release preparation work
- innovation work

## Operating Rule

Every meaningful backlog item should connect to at least one of:

- a project note
- a mission note
- a risk note
- a strategy review

## Live Backlog Snapshot — 2026-06-21

### Studio Core Work

- `STUDIO-DEC-001` Studio autonomy expansion applied. Next: tooling to render decision records in dashboards.
- Pattern library to gain `Integration Lifecycle` entry once `STAT-PM-001` closes.
- Spec template needs an `Exit Conditions` block.
- Compatibility matrix template needs a `removed` row pattern.

### Project Onboarding Work

- `STAT Mod` onboarding declared complete by `STAT-DEC-004`. No other projects in pipeline.

### Active Project Initiatives — STAT Mod

Ordered per `STAT-DEC-006`:

Closed:

1. `M3` Realign project `CLAUDE.md` to runtime truth
2. `M6` Mahou + Elementals removal postmortem and code sweep — `STAT-PM-001`
3. `M1` Branch state audit for residual Forge 1.20.1 wiring
4. `M8` `.opencode/plans/` deprecation notice

Active:

5. `M4` Iron's Spellbooks Phase 1 build continuation
6. `M9` Re-verify `065d57a` code review claims

Queued:

7. `M5` Iron's Spellbooks runtime integration tests + save migration tests
8. `M2` Worktree cleanup
9. `M10` Mod compatibility matrix

### Release Preparation Work

- Gated on `M5`, `M9`, and a curated compatibility view under `M10`. Release Readiness Review must be triggered separately, see `vault/07 Publishing/2026-06-21 - STAT Mod Release Readiness Track.md`.

### Innovation Work

- After Phase 1 magic ships, evaluate promoting Epic Fight from `experimental optional` to `production optional`.
- After Phase 1 magic ships, evaluate next integration candidate using the new `Exit Conditions` discipline.

---
type: meeting-minutes
status: Closed
session_id: STAT-MTG-001
date: 2026-06-21
session_type: Milestone Review + Exception Council
related_projects:
  - STAT Mod
related_decisions:
  - STAT-DEC-002
  - STAT-DEC-003
  - STAT-DEC-004
  - STAT-DEC-005
  - STAT-DEC-006
  - STUDIO-DEC-001
tags:
  - meeting
  - statmod
  - plenary
---

# 2026-06-21 — STAT Mod Plenary Session Minutes

## Mandate

Founder Desk convened a full plenary on `STAT Mod` with mandate to surface the entire state of the project, expose contradictions, and resolve them.

A complementary Founder directive issued the same session granted the studio expanded autonomy to take its own decisions in plenary, provided all decisions are documented (see `STUDIO-DEC-001`).

## Present

- Founder Desk
- Executive Orchestrator
- Strategy Council
- Innovation & Research Lead
- Product Cell
- Build Cell
- Verification Cell
- Visual Creation Cell
- Publishing & Release Cell

## Inventory Snapshot

- Active branch: `neoforge-1.21.1`
- Specs accumulated: 15 files in `docs/superpowers/specs/`
- Plans accumulated: 17 files in `docs/superpowers/plans/` plus 4 parallel files in `.opencode/plans/`
- Recent code review pass: commit `065d57a` claims 4 critical, 3 important, 3 minor issues fixed
- Recent removal: Mahou and Elementals integrations dropped in commit `22f7cff`
- Iron's Spellbooks unified magic tree Phase 1 build is the active mission
- Project dashboard reports `Integration Complete` but contradicts both the project `CLAUDE.md` and the residual backlog

## Tensions Identified

1. **Constitutional contradiction** — project `CLAUDE.md` forbids Epic Fight, but the code already integrates Epic Fight and the master plan reserves a full `integration/epicfight/` package
2. **Undocumented major removal** — Mahou and Elementals were removed without a postmortem
3. **Plan directory fragmentation** — `docs/superpowers/plans/` and `.opencode/plans/` coexist without a documented boundary
4. **Unverified audit claims** — commit `065d57a` lists 10 issues fixed without an attached catalogue
5. **Worktree pollution** — `.bak` files, `io/`, `libs/`, `runs/`, `yesman/`, `tensura_explore/`, and modified log archives appear in `git status`
6. **Optimistic dashboard** — `project-home.md` reports complete integration while pending unresolved tensions exist

## Discussion Summary

The Strategy Council noted that the runtime truth of the project (multi-mod integration including Epic Fight) has overtaken the original `CLAUDE.md` posture written in a context where Epic Fight had no NeoForge port. The studio must prefer runtime truth over outdated documents, then realign documentation.

The Innovation & Research Lead recommended treating removed integrations (Mahou, Elementals) as paid learning, extracting reusable signals into `pattern-library.md` rather than letting the work evaporate.

The Verification Cell insisted no release readiness can be declared while the constitution disagrees with the code.

The Publishing & Release Cell noted that release identity has been aligned to NeoForge already (`STAT-DEC-001`), so the remaining publication-blocking item is documentation truth, not platform.

The Build Cell confirmed Iron's Spellbooks Phase 1 has shipped its bridge, persistence, network, and UI mirror layers; the next blocking items are integration tests against the real `irons_spellbooks` runtime and save migration tests.

## Autonomous Decisions Taken

The studio resolved the following under the Founder Amendment 2026-06-21 to the autonomy policy.

| Id | Subject | Outcome |
|---|---|---|
| `STUDIO-DEC-001` | Studio autonomy expansion | Recorded |
| `STAT-DEC-002` | Epic Fight integration status | Recognized as `experimental optional integration`, removed from `CLAUDE.md` prohibition list |
| `STAT-DEC-003` | Mahou + Elementals removal | Postmortem opened, learnings to be captured |
| `STAT-DEC-004` | Project onboarding completion | Onboarding declared complete with one remaining sweep |
| `STAT-DEC-005` | Plan directory taxonomy | `docs/superpowers/plans/` is canonical; `.opencode/plans/` is deprecated for new work |
| `STAT-DEC-006` | Mission prioritization | Ordered sequence locked for the next operating cycle |

## Mission Roster Produced

The following mission packages are now authorized and tracked in `04-execution/current-initiatives.md`:

1. `M3` — Realign project `CLAUDE.md` to match runtime truth (Epic Fight, mahou/elementals removal, plan taxonomy)
2. `M6` — Mahou + Elementals removal postmortem
3. `M9` — Catalogue the 10 issues fixed in commit `065d57a` and verify each rejoue
4. `M4` — Continue Iron's Spellbooks Phase 1 plan (`docs/superpowers/plans/2026-06-21-irons-spellbooks-unified-magic-tree.md`)
5. `M5` — Iron's Spellbooks 3.16.1 runtime integration tests + `PlayerStatData` save migration tests
6. `M1` — Branch state audit (NeoForge port vs residual Forge wiring)
7. `M2` — Worktree cleanup (`.bak`, generated artefacts, unrelated log diffs)
8. `M8` — Migrate or archive `.opencode/plans/` references
9. `M10` — Mod compatibility matrix (Iron's, Tensura, Epic Fight, ParCool, Overgeared)

## Open Items Escalated to Founder

None. All escalated points from the prior round were resolved in this session under the Founder Amendment.

## Next Cadence

- Daily Ops Review: track `M3` and `M6` progress
- Weekly Strategy Council: revisit Phase 1 closure timing after `M4` and `M5`
- Release Readiness Review: trigger only after `M3`, `M5`, and `M9` are closed

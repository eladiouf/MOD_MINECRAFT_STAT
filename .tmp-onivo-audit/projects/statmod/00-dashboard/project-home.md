# STAT Mod Dashboard

## Identity

- Project id: `statmod`
- Name: `STAT Mod`
- Repo: `https://github.com/eladiouf/MOD_MINECRAFT_STAT`
- Local branch: `neoforge-1.21.1`
- Primary platform: `NeoForge 1.21.1`

## Current Phase

- Phase: `Production`
- Status: `Phase 1 Magic Active, Pre-Release Documentation Hardening`
- Primary studio theme: multi-mod progression authority operational with experimental optional integrations

## Current Focus

- Iron's Spellbooks unified magic tree Phase 1 — **active build** (`M4`)
- Code-review re-verification — **active audit** (`M9`)
- XP handlers test suite — **active** (`M11`)
- Hotfix batch — True Strike, tooltips, kodachi, memory leaks — **active** (`M12`)
- release-readiness is gated on closure of `M5`, `M9`, `M11`
- governance-resolution missions `M3`, `M6`, `M1`, `M8` closed 2026-06-21

## Last Ops Cycle (2026-06-21)

Four missions closed in same-day pass : `M3`, `M6`, `M1`, `M8`. See `vault/03 Operations/2026-06-21 - STAT Mod Daily Ops Review.md`.

## Integration Status

| Mod | Status | Notes |
|---|---|---|
| Iron's Spellbooks | active | bridge + tree state shipped, Phase 1 in progress |
| Tensura Reincarnated | active | hard dependency, race handler under modification |
| Puffish Skills | active | UI mirror layer |
| Epic Fight | `experimental optional` | reclassified by `STAT-DEC-002` |
| ParCool | optional | covered by master plan, not Phase 1 priority |
| Overgeared | optional | covered by master plan, not Phase 1 priority |
| Mahou Tsukai | removed | postmortem `STAT-PM-001` |
| Elementals | removed | postmortem `STAT-PM-001` |

## Immediate Risks

- ~~residual contradictions between project `CLAUDE.md` and runtime~~ — **resolved** by `M3` 2026-06-21
- worktree pollution + empty `integration/mahou/`, `integration/elementals/` dirs — `M2` queued
- prior code-review claims unverified — `M9` active
- no integration tests against real Iron's Spellbooks 3.16.1 runtime — `M5` queued
- deprecated `Run.getProgramArguments()` in build config remains
- mixin diff (`statmod.mixins.json` + `SwordSoaringClientModEventsMixin` deletion) appeared during sweep — to investigate
- **`True Strike` bug** (armor corruption) — `M12` hotfix in progress — `STAT-DEC-009`
- **5 perk tooltip mismatches** — `M12` hotfix in progress — `STAT-DEC-009`
- **4 config values dead** — `M13` queued after `M11` — `STAT-DEC-008`
- **No XP path for 9 magic stats** — `M4.5` queued after `M12` — `STAT-DEC-011`
- **Global level broken for non-magic builds** — formula fix in `M4.5` — `STAT-DEC-010`
- **Memory leaks in 3 static maps** — `M12` hotfix — `STAT-DEC-014`

## Main Entry Surfaces

- `01-contracts/`
- `02-product/`
- `03-architecture/`
- `04-execution/`
- `05-quality/`
- `06-release/`

## Live Studio Control Surfaces

- `vault/02 Strategy/2026-06-21 - STAT Mod Weekly Strategy Review.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Plenary Session Minutes.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod NeoForge Runtime Truth.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Epic Fight Status.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Mahou Elementals Postmortem Opened.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Onboarding Completion.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Plan Directory Taxonomy.md`
- `vault/08 Decisions/2026-06-21 - STAT Mod Mission Prioritization.md`
- `vault/08 Decisions/2026-06-21 - Studio Autonomy Expansion.md`
- `vault/09 Postmortems/2026-06-21 - STAT Mod Mahou Elementals Removal.md`
- `vault/03 Operations/2026-06-21 - STAT Mod Release Identity Alignment Mission.md`
- `vault/03 Operations/2026-06-21 - STAT Mod Verification Baseline Sweep Mission.md`
- `vault/03 Operations/2026-06-21 - STAT Mod Iron's Spellbooks Vertical Slice Mission.md`
- `vault/07 Publishing/2026-06-21 - STAT Mod Release Readiness Track.md`

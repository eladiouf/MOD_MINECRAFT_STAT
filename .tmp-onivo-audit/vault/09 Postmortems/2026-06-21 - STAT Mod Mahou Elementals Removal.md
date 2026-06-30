---
type: postmortem
status: Closed-Provisional
postmortem_id: STAT-PM-001
date: 2026-06-21
area: integration-lifecycle
owners:
  - Innovation & Research Lead
  - Verification Cell
related_projects:
  - STAT Mod
related_decisions:
  - STAT-DEC-003
tags:
  - postmortem
  - statmod
  - integration
---

# 2026-06-21 — STAT Mod Mahou + Elementals Removal Postmortem

## Status

`Closed-Provisional` — sweep executed in same-day plenary follow-up. Final closure depends on root-cause confirmation by the Founder Desk when convenient.

## What Happened

Commit `22f7cff` removed the Mahou Tsukai and Elementals integrations from the active codebase. The removal followed a sequence of investment commits on Elementals reward provenance and ownerless damage attribution (`ee18f1c`, `fffaabf`, `dd2d74a`, `35d0ccf`, `328f155`).

The `2026-06-16-multi-mod-integration-master-plan.md` had reserved a full `integration/mahou/` package with 4 files and listed Mahou Tsukai as Phase 2 of the integration sequence.

## Code Sweep Findings (2026-06-21)

Sweep executed against `src/` with patterns: `WaterHelmet`, `ElementalsMage`, `elementals_mage`, `ElementalsCompat`, `MahouCompat`, `mahou_tsukai`, `tw.elementals`.

- ✅ Zero matches in `src/main/java/`
- ✅ Zero matches in `src/test/java/`
- ✅ Zero matches in `src/main/resources/`
- ⚠️ Residue: empty package directories `src/main/java/tong/statmod/integration/elementals/` and `src/main/java/tong/statmod/integration/mahou/` — to clean under mission `M2`
- ℹ️ Historical references intact in `.opencode/plans/` master plan and `docs/superpowers/audits/2026-06-17-multi-mod-master-plan-audit.md` — preserved on purpose

False-positive note: occurrences of `elemental` / `Elemental` in the codebase belong to the STAT identity model (`elemental_specialization` Puffish category, `MagicBranch.FIRE/WATER/EARTH/AIR`, `TensuraSpellTaxonomy`). They are **not** orphans of the removed `Elementals` mod integration.

## Impact

- player-facing impact — minimal. The `PlayerStatData` serializer no longer carries Mahou/Elementals fields ; existing saves do not stall. Test `Elementals reward provenance` (commit `328f155`) is no longer present.
- codebase impact — clean except two empty package directories
- tests impact — Elementals-specific tests removed
- documentation impact — master plan still mentions both integrations (intentional, historical)
- compatibility-matrix impact — Mahou and Elementals must appear as `removed` rows once `M10` ships

## Root Cause (provisional)

Best inference from commit pattern :

- **Primary** — Iron's Spellbooks unified magic tree spec (2026-06-21) absorbed the magic system role both integrations were filling. The Founder strategically converged the magic stack on one runtime layer.
- **Secondary** — *Pre-removal investment trap* : Elementals received intensive provenance work in the week before removal because that very work revealed the unviability of maintaining two parallel magic stacks.

## Why The Studio Allowed Investment Before Removal

- the master plan `2026-06-16` declared 5 integrations in parallel without a `prove one before the next` gate
- the Strategy Council had no integration kill-switch criteria written down
- specs for both integrations lacked an `Exit Conditions` block

## Corrective Action

Approved in plenary, to be implemented :

1. ✅ `STAT-DEC-002` — Epic Fight reclassified as experimental optional ; signals studio willingness to mark integrations clearly
2. 🟡 Spec template gains an `Exit Conditions` block — pending
3. 🟡 `Integration Kill-Switch Criteria` section added to `operating-system/playbook-spec-flow.md` — pending
4. ⚪ Mission `M2` to delete the two empty package directories
5. ⚪ Mission `M10` to add `removed` rows for Mahou and Elementals in the compatibility matrix
6. ⚪ Pattern `Integration Lifecycle` added to `intelligence/pattern-library.md`

## Reusable Patterns

- **Pre-removal investment trap** — teams often invest more in a feature shortly before removing it ; that investment is what surfaces the unviability. Mitigation : a Strategy Council checkpoint after first integration tests, not after full polish.
- **Magic stack convergence** — when multiple integrations cover the same player-facing system (spells, magic), default to converging on one and let the others be lateral surfaces.
- **Reward provenance tagging** — the Elementals provenance work (commit `35d0ccf`) is a reusable pattern for general perk reward tracking ; salvage into the generic perk reward system rather than letting the design evaporate.
- **Ownerless damage attribution** — the `WaterHelmet` ownerless fallback was a generic Minecraft pattern ; document in pattern library before all source memory of it is lost.

## Sweep Tasks Status

- [x] grep for `mahou`, `elementals`, `WaterHelmet` references in active code — clean
- [x] check `PlayerStatData` serializer for orphan fields — clean
- [x] check `statmod.mixins.json` for orphan mixins — unrelated change observed (`SwordSoaringClientModEventsMixin` removed)
- [x] check locale files for orphan keys — clean (no mahou/elementals matches)
- [x] check resource paths for orphan entries — clean
- [x] confirm tests for these integrations are removed — confirmed
- [x] empty package directories identified — flagged to `M2`
- [ ] orphan migration policy for player saves — not required (no surviving fields)

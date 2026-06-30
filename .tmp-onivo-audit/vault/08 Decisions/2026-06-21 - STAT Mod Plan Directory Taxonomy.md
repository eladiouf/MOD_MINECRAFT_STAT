---
type: decision
status: Accepted
decision_id: STAT-DEC-005
date: 2026-06-21
area: documentation-taxonomy
owners:
  - Product Cell
  - Executive Orchestrator
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - documentation
---

# 2026-06-21 — STAT Mod Plan Directory Taxonomy

## Context

The `STAT Mod` repository carries two parallel plan directories:

- `docs/superpowers/plans/` — 17 files, including the active `2026-06-21-irons-spellbooks-unified-magic-tree.md` plan with explicit reference to the superpowers skill flow (`subagent-driven-development`, `executing-plans`)
- `.opencode/plans/` — 4 files, including a 75-point multi-mod master plan from 2026-06-16

Same project, two plan trees. No documented boundary between them. New contributors and AI agents cannot tell which is authoritative.

## Decision

`docs/superpowers/plans/` is declared the **canonical plan directory** for `STAT Mod`. `.opencode/plans/` is deprecated for new work.

Migration rule:

- existing `.opencode/plans/` files remain readable as historical context
- the `2026-06-16-multi-mod-integration-master-plan.md` master plan is preserved in place as a reference document and is not rewritten
- if a `.opencode/plans/` plan becomes the basis for active work it is forked into `docs/superpowers/plans/` with a `migrated-from` front-matter field
- no new plan files are created in `.opencode/plans/`

Same rule applies symmetrically to specs: `docs/superpowers/specs/` is canonical, `.opencode/plans/*-design.md` style specs are deprecated.

## Consequences

- mission `M3` adds a paragraph to `CLAUDE.md` documenting this taxonomy
- mission `M8` adds a `README.md` to `.opencode/plans/` pointing to the canonical directory
- AI agents reading `CLAUDE.md` now have a single source of truth for plans

## Follow-up

- `M8` to add the deprecation notice
- `M3` to document the rule in `CLAUDE.md`

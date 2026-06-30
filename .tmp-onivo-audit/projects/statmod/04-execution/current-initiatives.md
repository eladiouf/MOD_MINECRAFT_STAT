# Current Initiatives

> Sequence locked by `STAT-DEC-006` on 2026-06-21. Run in order unless overridden by Founder or Verification red flag.
>
> Use this file and the linked mission notes as the durable source of truth for current sequencing.

## Active (this cycle)

- **M4** — Iron's Spellbooks unified magic tree Phase 1 continuation — owner Build Cell — plan `docs/superpowers/plans/2026-06-21-irons-spellbooks-unified-magic-tree.md`
- **M9** — Catalogue and re-verify the 10 issues fixed in commit `065d57a` — owner Verification Cell

## Queued

- **M5** — Iron's Spellbooks 3.16.1 runtime integration tests + `PlayerStatData` save migration tests — owner Verification Cell
- **M2** — Worktree cleanup (`.bak`, generated artefacts, unrelated log diffs, empty package dirs `integration/mahou/`, `integration/elementals/`) — owner Build Cell
- **M10** — Mod compatibility matrix with `removed` rows for Mahou and Elementals — owner Product Cell

## Just Closed (2026-06-22)

- **M11** — Tests XP handlers (60 tests, tous passent) — closed per STAT-MTG-002 — `STAT-DEC-007`
- **M12** — Hotfix batch (True Strike, 5 tooltips, Kodachi, 3 memory leaks) — closed per STAT-MTG-002 — `STAT-DEC-009`, `STAT-DEC-012`, `STAT-DEC-014`
- **M4.5** — XP non-combat pour 9 stats magiques + global level fix — closed per STAT-MTG-002 — `STAT-DEC-010`, `STAT-DEC-011`
- **M13** — Wiring 4 configs + dead code cleanup + CLAUDE.md — closed per STAT-MTG-002 — `STAT-DEC-008`, `STAT-DEC-013`

## Just Closed (2026-06-21)

- **M3** — Realign project `CLAUDE.md` to runtime truth — closed by Onivo Studio plenary follow-up — `CLAUDE.md` rewritten to remove Epic Fight prohibition, add integrations status table, document Mahou/Elementals removal, document plan taxonomy, link decision records.
- **M6** — Mahou + Elementals removal postmortem — closed-provisional — sweep clean except 2 empty package directories forwarded to `M2` — postmortem at `vault/09 Postmortems/2026-06-21 - STAT Mod Mahou Elementals Removal.md`.
- **M1** — Branch state audit for residual Forge 1.20.1 wiring — closed — zero matches for `net.minecraftforge`, `MinecraftForge.EVENT_BUS`, `FMLJavaModLoadingContext`, `SimpleChannel`, `Capability.` in `src/`. The NeoForge port is effectively complete ; the prior CLAUDE.md claim was stale.
- **M8** — `.opencode/plans/` deprecation notice posted as `.opencode/plans/README.md` — `docs/superpowers/plans/` is canonical going forward.
- [[2026-06-21 - STAT Mod Release Identity Alignment Mission]]
- [[2026-06-21 - STAT Mod Verification Baseline Sweep Mission]]
- [[2026-06-21 - STAT Mod Multi-Mod Integration Completion Mission]] — archived as a historical completion claim, not the current operating source of truth

## Reference Decisions

- `STUDIO-DEC-001` — Studio autonomy expansion (Founder Amendment)
- `STAT-DEC-001` — NeoForge 1.21.1 runtime truth
- `STAT-DEC-002` — Epic Fight reclassified as experimental optional
- `STAT-DEC-003` — Mahou + Elementals postmortem opened
- `STAT-DEC-004` — Onboarding into Onivo Studio complete
- `STAT-DEC-005` — `docs/superpowers/plans/` canonical, `.opencode/plans/` deprecated
- `STAT-DEC-006` — Mission prioritization sequence locked

## Postmortems

- `STAT-PM-001` — Mahou + Elementals removal — Closed-Provisional

## Follow Soon

- spec template gains an `Exit Conditions` block (postmortem follow-up)
- `Integration Kill-Switch Criteria` section in `operating-system/playbook-spec-flow.md`
- `Integration Lifecycle` pattern in `intelligence/pattern-library.md`
- fix `Run.getProgramArguments()` deprecation in build config
- prepare release surface for NeoForge 1.21.1 — gated on `M5`, `M9`

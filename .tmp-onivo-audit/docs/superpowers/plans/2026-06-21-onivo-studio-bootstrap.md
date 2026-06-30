# Onivo Studio Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap the central `Onivo Studio` repository as a reusable Minecraft mod studio operating system with governance, operations, memory, publication, and project-adapter foundations.

**Architecture:** The bootstrap creates a document-first studio core. The repository becomes a durable control plane with versioned governance, strategy, operating-system, intelligence, delivery, publishing, project templates, and an Obsidian-compatible vault, while `.onivo-runtime/` is reserved as a non-versioned runtime layer. This first slice intentionally does not onboard any real project repo yet; it builds the studio shell that later projects will plug into.

**Tech Stack:** GitHub repository, Markdown documentation, Obsidian-compatible vault structure, PowerShell file verification commands, Git for commits.

---

## File Structure

### New directories

- `governance/` — studio constitution, authority model, councils, and autonomy rules
- `strategy/` — studio thesis, portfolio model, and innovation direction
- `operating-system/` — mission lifecycle, mission types, playbooks, and operating policies
- `intelligence/` — pattern library, research ledger, and postmortem doctrine
- `delivery/` — portfolio backlog, initiative states, and scoreboard definitions
- `publishing/` — release governance, store checklist, and release artifact policy
- `projects/_templates/` — reusable project adapter package templates
- `vault/00 Home/` through `vault/09 Postmortems/` — Obsidian-compatible studio memory
- `docs/superpowers/plans/` — implementation plans

### New files

- `README.md` — repo landing page explaining what `Onivo Studio` is and is not
- `.gitignore` — excludes `.onivo-runtime/`, Obsidian transient files, and local OS/editor noise
- `governance/charter.md`
- `governance/roles.md`
- `governance/councils.md`
- `governance/autonomy-policy.md`
- `governance/authority-boundaries.md`
- `strategy/studio-thesis.md`
- `strategy/portfolio-model.md`
- `strategy/innovation-charter.md`
- `operating-system/workflow-lifecycle.md`
- `operating-system/mission-types.md`
- `operating-system/playbook-spec-flow.md`
- `operating-system/playbook-release-flow.md`
- `operating-system/playbook-incident-flow.md`
- `intelligence/pattern-library.md`
- `intelligence/research-ledger.md`
- `intelligence/postmortem-standard.md`
- `delivery/portfolio-backlog.md`
- `delivery/initiative-status-model.md`
- `delivery/studio-scoreboards.md`
- `publishing/release-governance.md`
- `publishing/store-surface-checklist.md`
- `publishing/release-asset-policy.md`
- `projects/_templates/project-adapter-template.md`
- `projects/_templates/project-local-bridge-template.md`
- `vault/00 Home/Studio Home.md`
- `vault/00 Home/Current Portfolio.md`
- `vault/01 Governance/Governance Index.md`
- `vault/02 Strategy/Strategy Index.md`
- `vault/03 Operations/Operations Index.md`
- `vault/04 Projects/Projects Index.md`
- `vault/05 Research/Research Atlas.md`
- `vault/06 Visual/Visual Index.md`
- `vault/07 Publishing/Release Board.md`
- `vault/08 Decisions/Decision Index.md`
- `vault/09 Postmortems/Postmortem Index.md`
- `operating-system/mcp-registry.md`
- `operating-system/skills-registry.md`
- `operating-system/visual-system.md`
- `operating-system/runtime-conventions.md`

### Existing files to modify

- `docs/superpowers/specs/2026-06-21-onivo-studio-operating-system-design.md` — no content changes expected; only referenced for validation

---

### Task 1: Create repo root scaffolding and ignore policy

**Files:**
- Create: `.gitignore`
- Create: `README.md`

- [ ] **Step 1: Write the root files**

Create `.gitignore` with:

```gitignore
# Onivo Studio runtime
.onivo-runtime/

# Obsidian local state
.obsidian/workspace*.json
.obsidian/cache/
.obsidian/plugins/

# Editor / OS noise
.DS_Store
Thumbs.db
desktop.ini
*.tmp
*.log
```

Create `README.md` with:

```markdown
# Onivo Studio

`Onivo Studio` is a central operating-system repository for running a Minecraft mod studio with one founder and multiple AI agents.

It is responsible for:

- governance
- strategy
- operations
- intelligence and memory
- delivery
- publication
- project adapter standards

This repository is not a single mod project.

It is the durable control plane that future mod repositories plug into.

## Structure

- `governance/` — constitution, roles, councils, authority model
- `strategy/` — studio thesis, portfolio direction, innovation model
- `operating-system/` — lifecycle, playbooks, runtime rules, tool registries
- `intelligence/` — research, postmortems, reusable patterns
- `delivery/` — backlog and portfolio execution views
- `publishing/` — release governance and store-facing policies
- `projects/` — project adapter packages and templates
- `vault/` — Obsidian-compatible studio memory

## Non-Goals

This bootstrap does not yet onboard a real mod repository.

That work begins only after the studio core is established.
```

- [ ] **Step 2: Verify the files exist**

Run: `Get-ChildItem -Force`
Expected: output includes `.gitignore` and `README.md`

- [ ] **Step 3: Commit**

```bash
git add .gitignore README.md
git commit -m "chore: bootstrap Onivo Studio root scaffolding"
```

---

### Task 2: Create governance core

**Files:**
- Create: `governance/charter.md`
- Create: `governance/roles.md`
- Create: `governance/councils.md`
- Create: `governance/autonomy-policy.md`
- Create: `governance/authority-boundaries.md`

- [ ] **Step 1: Write `governance/charter.md`**

```markdown
# Onivo Studio Charter

## Purpose

`Onivo Studio` exists to direct, execute, verify, publish, and improve Minecraft mod projects through a founder-led, AI-assisted operating model.

## Studio Principles

1. Full visibility by default
2. Bounded authority by policy
3. Specs before plans when direction is still fuzzy
4. Verification before closure
5. Publication is governed, not improvised
6. Postmortems must improve the studio

## Founder Authority

The founder retains final authority on:

- long-term direction
- project selection
- irreversible product pivots
- major publication approval
- constitutional changes to the studio

## Studio Mandate

The studio may autonomously prepare and execute work inside approved mission boundaries, but it may not assume total authority over product, security, or publication decisions.
```

- [ ] **Step 2: Write `governance/roles.md`**

```markdown
# Onivo Studio Roles

## Core Roles

- `Founder Desk`
- `Executive Orchestrator`
- `Strategy Council`
- `Innovation & Research Lead`
- `Product Cell`
- `Build Cell`
- `Verification Cell`
- `Visual Creation Cell`
- `Publishing & Release Cell`

## Role Summaries

### Founder Desk
Final authority on strategic, constitutional, and irreversible decisions.

### Executive Orchestrator
Turns goals into mission packages, routes work, and escalates when needed.

### Strategy Council
Decides where the studio should go next and arbitrates priorities.

### Innovation & Research Lead
Explores opportunities, integrations, and emerging ideas before formalization.

### Product Cell
Produces briefs, exploration notes, specs, plans, and scoped backlog slices.

### Build Cell
Executes production work across code, data, assets, and tooling.

### Verification Cell
Owns tests, audits, runtime validation, and release readiness.

### Visual Creation Cell
Produces concept visuals, production assets, and release-facing imagery.

### Publishing & Release Cell
Prepares and governs releases and store publication surfaces.
```

- [ ] **Step 3: Write `governance/councils.md`**

```markdown
# Onivo Studio Councils

## Recurring Councils

### Daily Ops Review
- short operational review
- active missions
- blockers
- next actions

### Weekly Strategy Council
- priorities
- opportunities
- freezes
- accelerations
- portfolio direction

## Triggered Councils

### Milestone Review
Used when a major initiative completes or stalls.

### Release Readiness Review
Required before external publication.

### Post-Release Review
Used to inspect regressions, support load, and process misses.

### Exception Council
Used for major crashes, architectural conflicts, or direction uncertainty.
```

- [ ] **Step 4: Write `governance/autonomy-policy.md`**

```markdown
# Onivo Studio Autonomy Policy

## Autonomy Model

The studio is quasi autonomous.

It may:

- inspect connected projects broadly
- generate mission packages
- execute bounded work
- improve non-constitutional internal artifacts

It may not autonomously:

- rewrite constitutional rules
- assume final publication authority
- redefine founder authority
- bypass escalation rules

## Default Rule

Strong autonomy in execution. Bounded autonomy in control.
```

- [ ] **Step 5: Write `governance/authority-boundaries.md`**

```markdown
# Onivo Studio Authority Boundaries

## Read Access

Studio systems should default to broad read access across connected projects.

## Write Access

Write authority is mission-scoped and policy-bounded.

## Publish Access

The studio may prepare releases, but publication remains a governed action.

## Escalation Cases

Escalation is required for:

- irreversible product direction changes
- critical security policy changes
- foundational governance edits
- ambiguous release decisions
- repeated verification failures without clear resolution
```

- [ ] **Step 6: Verify governance core exists**

Run: `rg --files governance`
Expected: output lists the five governance markdown files

- [ ] **Step 7: Commit**

```bash
git add governance
git commit -m "docs: add Onivo Studio governance core"
```

---

### Task 3: Create strategy layer

**Files:**
- Create: `strategy/studio-thesis.md`
- Create: `strategy/portfolio-model.md`
- Create: `strategy/innovation-charter.md`

- [ ] **Step 1: Write `strategy/studio-thesis.md`**

```markdown
# Onivo Studio Thesis

## Studio Identity

`Onivo Studio` is a Minecraft mod studio operating system designed for a founder plus AI agents.

It is optimized for:

- multi-project reuse
- design-to-release continuity
- high-context strategic memory
- bounded autonomy

## Studio Promise

The studio should be able to think, execute, verify, publish, and improve.
```

- [ ] **Step 2: Write `strategy/portfolio-model.md`**

```markdown
# Portfolio Model

## Portfolio View

The studio manages a portfolio of Minecraft mod projects rather than one monolithic codebase.

Each project should eventually expose:

- maturity phase
- current focus
- health summary
- release readiness
- risk profile
- innovation potential

## Standard Project Phases

- `Incubation`
- `Discovery`
- `Pre-Production`
- `Production`
- `Hardening`
- `Release Active`
- `Maintenance`
- `Dormant`
- `Retired`
```

- [ ] **Step 3: Write `strategy/innovation-charter.md`**

```markdown
# Innovation Charter

## Purpose

Innovation work exists to discover strong directions before they become expensive commitments.

## Innovation Inputs

- new mods
- new gameplay loops
- new technical integrations
- new studio capabilities
- release and product observations

## Innovation Outputs

- exploration notes
- opportunity summaries
- risk assessments
- recommendations to the Strategy Council
```

- [ ] **Step 4: Verify strategy layer exists**

Run: `rg --files strategy`
Expected: output lists the three strategy markdown files

- [ ] **Step 5: Commit**

```bash
git add strategy
git commit -m "docs: add Onivo Studio strategy layer"
```

---

### Task 4: Create operating-system core

**Files:**
- Create: `operating-system/workflow-lifecycle.md`
- Create: `operating-system/mission-types.md`
- Create: `operating-system/playbook-spec-flow.md`
- Create: `operating-system/playbook-release-flow.md`
- Create: `operating-system/playbook-incident-flow.md`

- [ ] **Step 1: Write `operating-system/workflow-lifecycle.md`**

```markdown
# Workflow Lifecycle

## Standard Lifecycle

1. strategy decision or exploration request
2. exploration note if direction is fuzzy
3. brief
4. spec
5. implementation plan
6. execution
7. verification
8. release preparation when relevant
9. publication when relevant
10. postmortem and learning extraction

## Rule

The studio must not collapse all work into a single build step.
```

- [ ] **Step 2: Write `operating-system/mission-types.md`**

```markdown
# Mission Types

## Supported Mission Classes

- `Research Mission`
- `Exploration Mission`
- `Spec Mission`
- `Plan Mission`
- `Build Mission`
- `Verification Mission`
- `Visual Mission`
- `Release Mission`
- `Recovery Mission`

## Required Fields

Every mission must define:

- owner cell
- explicit scope
- target project
- allowed action surface
- success condition
- verification path
- escalation rule
```

- [ ] **Step 3: Write `operating-system/playbook-spec-flow.md`**

```markdown
# Playbook - Spec Flow

## Use When

Direction is material and still not executable.

## Flow

1. gather project context
2. clarify constraints
3. compare approaches
4. produce draft design
5. validate with founder
6. commit the spec
7. hand off to planning
```

- [ ] **Step 4: Write `operating-system/playbook-release-flow.md`**

```markdown
# Playbook - Release Flow

## Flow

1. confirm implementation scope is closed
2. confirm verification gates passed
3. assemble changelog and release metadata
4. verify release assets
5. run Release Readiness Review
6. publish through governed release action
7. run post-release checks
```

- [ ] **Step 5: Write `operating-system/playbook-incident-flow.md`**

```markdown
# Playbook - Incident Flow

## Trigger

Use when a release, runtime, build, or workflow failure creates a meaningful disruption.

## Flow

1. capture the failure
2. stabilize immediate impact
3. open recovery mission
4. verify the fix
5. run postmortem
6. feed the lesson back into the studio
```

- [ ] **Step 6: Verify operating-system core exists**

Run: `rg --files operating-system`
Expected: output lists the lifecycle, mission, and three playbook markdown files

- [ ] **Step 7: Commit**

```bash
git add operating-system/workflow-lifecycle.md operating-system/mission-types.md operating-system/playbook-spec-flow.md operating-system/playbook-release-flow.md operating-system/playbook-incident-flow.md
git commit -m "docs: add Onivo Studio operating-system core"
```

---

### Task 5: Create intelligence layer

**Files:**
- Create: `intelligence/pattern-library.md`
- Create: `intelligence/research-ledger.md`
- Create: `intelligence/postmortem-standard.md`

- [ ] **Step 1: Write `intelligence/pattern-library.md`**

```markdown
# Pattern Library

Reusable patterns belong here once they have been validated in real work.

Examples:

- planning patterns
- verification patterns
- release patterns
- adapter patterns
- visual review patterns
```

- [ ] **Step 2: Write `intelligence/research-ledger.md`**

```markdown
# Research Ledger

Track durable research outcomes here.

Each entry should eventually capture:

- topic
- why it mattered
- recommendation
- relevant projects
- follow-up needed
```

- [ ] **Step 3: Write `intelligence/postmortem-standard.md`**

```markdown
# Postmortem Standard

Every major incident or repeated process failure should capture:

- what happened
- impact
- root cause
- why the studio allowed it
- corrective action
- policy, template, or playbook updates
```

- [ ] **Step 4: Verify intelligence layer exists**

Run: `rg --files intelligence`
Expected: output lists the three intelligence markdown files

- [ ] **Step 5: Commit**

```bash
git add intelligence
git commit -m "docs: add Onivo Studio intelligence layer"
```

---

### Task 6: Create delivery layer

**Files:**
- Create: `delivery/portfolio-backlog.md`
- Create: `delivery/initiative-status-model.md`
- Create: `delivery/studio-scoreboards.md`

- [ ] **Step 1: Write `delivery/portfolio-backlog.md`**

```markdown
# Portfolio Backlog

This file is the top-level durable backlog surface for the studio.

It should eventually group work by:

- studio core work
- project onboarding work
- active project initiatives
- release preparation work
- innovation work
```

- [ ] **Step 2: Write `delivery/initiative-status-model.md`**

```markdown
# Initiative Status Model

Recommended initiative states:

- `Proposed`
- `Exploring`
- `Specified`
- `Planned`
- `Executing`
- `Verifying`
- `Ready for Release`
- `Released`
- `Blocked`
- `Archived`
```

- [ ] **Step 3: Write `delivery/studio-scoreboards.md`**

```markdown
# Studio Scoreboards

The studio scoreboard layer should eventually track:

- active mission count
- blocked mission count
- release readiness count
- postmortem backlog
- project phase distribution
- quality gate pressure
```

- [ ] **Step 4: Verify delivery layer exists**

Run: `rg --files delivery`
Expected: output lists the three delivery markdown files

- [ ] **Step 5: Commit**

```bash
git add delivery
git commit -m "docs: add Onivo Studio delivery layer"
```

---

### Task 7: Create publishing layer

**Files:**
- Create: `publishing/release-governance.md`
- Create: `publishing/store-surface-checklist.md`
- Create: `publishing/release-asset-policy.md`

- [ ] **Step 1: Write `publishing/release-governance.md`**

```markdown
# Release Governance

## Rule

Publication is governed, not improvised.

## Required Conditions

- implementation scope closed
- verification complete
- release metadata complete
- release assets ready
- release review completed

## Boundary

The studio may prepare release material autonomously, but final publish authority remains gated.
```

- [ ] **Step 2: Write `publishing/store-surface-checklist.md`**

```markdown
# Store Surface Checklist

Use this checklist for store-facing release surfaces:

- title
- summary
- supported Minecraft version
- loader metadata
- dependency metadata
- changelog
- cover image
- gallery assets
- release classification
```

- [ ] **Step 3: Write `publishing/release-asset-policy.md`**

```markdown
# Release Asset Policy

Release assets should be:

- product-accurate
- readable
- brand-consistent
- reusable across stores where possible
- archived in a recoverable structure
```

- [ ] **Step 4: Verify publishing layer exists**

Run: `rg --files publishing`
Expected: output lists the three publishing markdown files

- [ ] **Step 5: Commit**

```bash
git add publishing
git commit -m "docs: add Onivo Studio publishing layer"
```

---

### Task 8: Create project adapter templates

**Files:**
- Create: `projects/_templates/project-adapter-template.md`
- Create: `projects/_templates/project-local-bridge-template.md`

- [ ] **Step 1: Write `projects/_templates/project-adapter-template.md`**

```markdown
# Project Adapter Template

Use this template for `projects/<project-id>/`.

## Required Directories

- `00-dashboard/`
- `01-contracts/`
- `02-product/`
- `03-architecture/`
- `04-execution/`
- `05-quality/`
- `06-release/`
- `07-history/`
- `08-links/`

## Required Contracts

- `Identity Contract`
- `Execution Contract`
- `Decision Contract`
- `Quality Contract`
- `Publishing Contract`
```

- [ ] **Step 2: Write `projects/_templates/project-local-bridge-template.md`**

```markdown
# Project Local Bridge Template

Every connected project repository should eventually expose:

- `.onivo-project/project-manifest.md`
- `.onivo-project/execution-contract.md`
- `.onivo-project/quality-contract.md`
- `.onivo-project/publishing-contract.md`
- `.onivo-project/current-state.md`
- `.onivo-project/entrypoints.md`
- `.onivo-project/risks.md`
- `.onivo-project/studio-links.md`

These files are the studio-readable bridge, not the full studio itself.
```

- [ ] **Step 3: Verify template files exist**

Run: `rg --files projects`
Expected: output lists both template markdown files

- [ ] **Step 4: Commit**

```bash
git add projects
git commit -m "docs: add Onivo Studio project adapter templates"
```

---

### Task 9: Create Obsidian-compatible vault skeleton

**Files:**
- Create: `vault/00 Home/Studio Home.md`
- Create: `vault/00 Home/Current Portfolio.md`
- Create: `vault/01 Governance/Governance Index.md`
- Create: `vault/02 Strategy/Strategy Index.md`
- Create: `vault/03 Operations/Operations Index.md`
- Create: `vault/04 Projects/Projects Index.md`
- Create: `vault/05 Research/Research Atlas.md`
- Create: `vault/06 Visual/Visual Index.md`
- Create: `vault/07 Publishing/Release Board.md`
- Create: `vault/08 Decisions/Decision Index.md`
- Create: `vault/09 Postmortems/Postmortem Index.md`

- [ ] **Step 1: Write `vault/00 Home/Studio Home.md`**

```markdown
# Studio Home

Welcome to the `Onivo Studio` vault.

## Main Navigation

- [[Current Portfolio]]
- [[Governance Index]]
- [[Strategy Index]]
- [[Operations Index]]
- [[Projects Index]]
- [[Research Atlas]]
- [[Visual Index]]
- [[Release Board]]
- [[Decision Index]]
- [[Postmortem Index]]
```

- [ ] **Step 2: Write `vault/00 Home/Current Portfolio.md`**

```markdown
# Current Portfolio

This page tracks the active and planned project portfolio for the studio.

No projects are connected yet in the bootstrap phase.
```

- [ ] **Step 3: Write the index pages**

Create these files with the following content:

`vault/01 Governance/Governance Index.md`

```markdown
# Governance Index

- Studio charter
- role definitions
- councils
- authority boundaries
- autonomy policy
```

`vault/02 Strategy/Strategy Index.md`

```markdown
# Strategy Index

- studio thesis
- portfolio model
- innovation direction
```

`vault/03 Operations/Operations Index.md`

```markdown
# Operations Index

- workflow lifecycle
- mission types
- spec flow
- release flow
- incident flow
```

`vault/04 Projects/Projects Index.md`

```markdown
# Projects Index

Connected project adapters will be indexed here once onboarding begins.
```

`vault/05 Research/Research Atlas.md`

```markdown
# Research Atlas

This page is the navigation surface for durable research notes and synthesis.
```

`vault/06 Visual/Visual Index.md`

```markdown
# Visual Index

This page tracks visual-system references, prompts, outputs, and decisions.
```

`vault/07 Publishing/Release Board.md`

```markdown
# Release Board

This page is the navigation surface for release readiness and publication tracking.
```

`vault/08 Decisions/Decision Index.md`

```markdown
# Decision Index

This page indexes important studio and project decisions.
```

`vault/09 Postmortems/Postmortem Index.md`

```markdown
# Postmortem Index

This page indexes studio and project postmortems.
```

- [ ] **Step 4: Verify the vault skeleton exists**

Run: `rg --files vault`
Expected: output lists the ten vault markdown files

- [ ] **Step 5: Commit**

```bash
git add vault
git commit -m "docs: add Onivo Studio Obsidian vault skeleton"
```

---

### Task 10: Create MCP, skills, visual, and runtime registries

**Files:**
- Create: `operating-system/mcp-registry.md`
- Create: `operating-system/skills-registry.md`
- Create: `operating-system/visual-system.md`
- Create: `operating-system/runtime-conventions.md`

- [ ] **Step 1: Write `operating-system/mcp-registry.md`**

```markdown
# MCP Registry

## Bootstrap Connectors

- `GitHub MCP`
- `Filesystem MCP`
- `Git MCP`
- `Sequential Thinking MCP`

## Policy

- minimal connector set first
- least privilege
- no dependency on obscure community MCP tooling for core governance
```

- [ ] **Step 2: Write `operating-system/skills-registry.md`**

```markdown
# Skills Registry

## Standard Skill Use

- `brainstorming` — strategic or fuzzy work
- `writing-plans` — approved-spec to implementation-plan handoff
- `verification-before-completion` — required before completion claims
- `minecraft-modding` — production mod work
- `skill-creator` — improving studio capabilities
- `plugin-creator` — studio-specific plugin creation when justified
```

- [ ] **Step 3: Write `operating-system/visual-system.md`**

```markdown
# Visual System

## Output Layers

- concept visuals
- production assets
- release visuals

## Governance

- `Visual Creation Cell`
- `Visual QA`
- `Brand & Presentation Lead`

## Operating Mode

- manual generation first
- partial API-driven automation later
```

- [ ] **Step 4: Write `operating-system/runtime-conventions.md`**

```markdown
# Runtime Conventions

## Runtime Layer

The non-versioned runtime directory is `.onivo-runtime/`.

## Expected Contents

- active mission material
- temporary reports
- evidence bundles
- scratch work
- temporary workspaces

## Rule

Runtime artifacts are operational aids, not durable source of truth.
```

- [ ] **Step 5: Verify the registries exist**

Run: `rg --files operating-system`
Expected: output includes `mcp-registry.md`, `skills-registry.md`, `visual-system.md`, and `runtime-conventions.md`

- [ ] **Step 6: Commit**

```bash
git add operating-system/mcp-registry.md operating-system/skills-registry.md operating-system/visual-system.md operating-system/runtime-conventions.md
git commit -m "docs: add Onivo Studio tool and runtime registries"
```

---

### Task 11: Add bootstrap plan location and verify spec-plan coherence

**Files:**
- Create: `docs/superpowers/plans/2026-06-21-onivo-studio-bootstrap.md`

- [ ] **Step 1: Verify the plan file is present**

Run: `Test-Path docs/superpowers/plans/2026-06-21-onivo-studio-bootstrap.md`
Expected: `True`

- [ ] **Step 2: Check spec-plan coverage manually**

Run:

```powershell
Get-Content -LiteralPath docs\superpowers\specs\2026-06-21-onivo-studio-operating-system-design.md
Get-Content -LiteralPath docs\superpowers\plans\2026-06-21-onivo-studio-bootstrap.md
```

Expected:

- spec sections `4` through `14` all have a corresponding bootstrap artifact or an explicit “not yet” boundary
- no bootstrap task tries to onboard a real project repo yet

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/plans/2026-06-21-onivo-studio-bootstrap.md
git commit -m "docs: add Onivo Studio bootstrap implementation plan"
```

---

### Task 12: Final bootstrap validation sweep

**Files:**
- Modify: none

- [ ] **Step 1: Verify directory map**

Run:

```powershell
Get-ChildItem governance,strategy,operating-system,intelligence,delivery,publishing,projects,vault | Select-Object FullName
```

Expected: all eight top-level studio directories are listed

- [ ] **Step 2: Verify all tracked markdown bootstrap artifacts**

Run:

```powershell
rg --files | rg "README.md|\\.gitignore|governance/|strategy/|operating-system/|intelligence/|delivery/|publishing/|projects/_templates/|vault/|docs/superpowers/specs/2026-06-21-onivo-studio-operating-system-design.md|docs/superpowers/plans/2026-06-21-onivo-studio-bootstrap.md"
```

Expected: output lists the full bootstrap surface

- [ ] **Step 3: Final commit**

```bash
git commit --allow-empty -m "Bootstrap Onivo Studio operating system foundation"
```

---

## Self-Review

### Spec coverage

- Spec §3.1–3.5: covered by governance, authority, autonomy, and README bootstrap tasks
- Spec §4: covered by governance role files
- Spec §5: covered by councils file
- Spec §6–§7: covered by lifecycle, mission types, and playbooks
- Spec §8: covered by root layer creation tasks
- Spec §9: covered by project adapter template tasks
- Spec §10: covered by vault skeleton tasks
- Spec §11: covered by MCP and skills registry tasks
- Spec §12: covered by visual-system registry
- Spec §13: covered by publishing layer tasks
- Spec §14: covered by autonomy and postmortem standard tasks
- Spec §15: partially covered through incident playbook and postmortem standard; runtime fault handling implementation remains future work
- Spec §16: fully aligned with bootstrap-only scope

### Placeholder scan

This plan avoids `TODO`/`TBD` placeholders and gives exact file paths, concrete content, and explicit verification commands.

### Type consistency

Naming is consistent between the spec and the plan for:

- `Founder Desk`
- `Executive Orchestrator`
- `Strategy Council`
- `Innovation & Research Lead`
- `Product Cell`
- `Build Cell`
- `Verification Cell`
- `Visual Creation Cell`
- `Publishing & Release Cell`
- `.onivo-runtime/`
- project adapter contract names


# Onivo Studio - Operating System Design

**Date:** 2026-06-21  
**Project:** `Onivo Studio`  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Define `Onivo Studio` as a reusable, multi-project Minecraft mod studio operating system for:

- one human founder
- multiple AI agents
- multiple future mod projects
- full-cycle work from strategy to release

`Onivo Studio` is not a single mod and not a project-local helper folder. It is a separate studio repository that:

- governs how projects are researched, planned, built, verified, and published
- stores durable studio memory
- standardizes project adapters
- improves itself over time within bounded rules

The first bootstrap is **studio-generic but Minecraft-mod-specific**. It must not be hardcoded around `MOD_MINECRAFT_STAT`, even though that mod is expected to become the first connected project later.

## 2. Scope Shape

This design covers the studio operating system itself:

- governance
- strategy
- operations
- intelligence and memory
- project adapters
- delivery and publication
- visual production capability
- Obsidian vault organization
- MCP, plugin, and skill integration policy
- self-improvement rules

This design does **not** yet onboard a concrete mod project. Project onboarding is a follow-up planning slice once the studio core exists.

## 3. Core Design Commitments

### 3.1 Studio Type

`Onivo Studio` is a **Minecraft mod studio OS**.

It should be specialized from day one for:

- NeoForge / Fabric style project realities
- Minecraft version compatibility
- assets, datapacks, GUIs, balancing, and mod integrations
- mod release channels such as CurseForge, Modrinth, and GitHub

It is not meant to be a generic software company framework.

### 3.2 Studio Topology

The architecture is **federated**:

- one central `Onivo Studio` repository
- many external project repositories
- a lightweight adapter contract in each project repo

This is preferred over embedding a full studio in every mod repo because:

- the studio must improve across projects
- the memory must remain centralized
- governance and playbooks should not fork per project

### 3.3 Full Visibility, Bounded Authority

`Onivo Studio` must have **full project visibility by default**.

For connected projects, the studio should be able to inspect:

- code
- data and assets
- tests
- docs
- plans
- specs
- audits
- logs and run surfaces
- release metadata
- risks and project state

However, full visibility does **not** imply unlimited authority.

The studio should operate under:

- broad read access
- gated write access
- gated publish access
- explicit escalation rules for irreversible decisions

### 3.4 Passive Global View

The studio should maintain a **full but passive** project view.

That means:

- it can see the entire project model
- it refreshes understanding when needed
- it does not require a heavy real-time telemetry system

This is the right tradeoff for a mod studio:

- broad awareness
- low operational overhead
- less noise

### 3.5 Bounded Autonomy

The studio should be **quasi autonomous**, not fully sovereign.

It should autonomously execute within mission boundaries, but not freely rewrite product direction, governance, or release authority.

The guiding rule is:

- strong autonomy in execution
- bounded autonomy in control

## 4. Studio Operating Model

### 4.1 Founder Desk

The founder remains the final authority on:

- long-term direction
- project selection
- irreversible product shifts
- major release approval
- constitutional studio changes

The studio exists to amplify the founder, not replace the founder.

### 4.2 Executive Orchestrator

The `Executive Orchestrator` is the central operational AI role.

It is responsible for:

- turning high-level goals into missions
- routing work to specialized cells
- keeping scope bounded
- deciding when to escalate
- deciding when a mission is ready for verification

It should always understand:

- current priorities
- active missions
- current risks
- blocked paths
- next recommended moves

### 4.3 Strategy Council

`Onivo Studio` must not jump directly from vague desire to implementation every time.

It needs a recurring strategic forum: the `Strategy Council`.

The council is responsible for:

- deciding where the studio should go next
- choosing which bets are worth pursuing
- opening or freezing initiatives
- resolving uncertainty before spec writing
- reviewing innovation proposals

It exists because a studio must be able to think, not only execute.

### 4.4 Innovation and Research Lead

The studio needs a dedicated `Innovation & Research Lead`.

This role is responsible for:

- exploring new mod ideas
- researching integrations and mechanics
- scanning technical opportunities
- producing exploration notes before formal specs
- identifying promising long-term studio capabilities

This role should feed the `Strategy Council`, not bypass it.

### 4.5 Product Cell

The `Product Cell` turns strategic direction into buildable work.

It produces:

- briefs
- exploration notes
- specs
- plans
- backlog slices

It owns clarity of intent and scope quality.

### 4.6 Build Cell

The `Build Cell` executes production work across:

- code
- data
- assets
- compatibility layers
- tooling

It should work only from a sufficiently clear mission package.

### 4.7 Verification Cell

The `Verification Cell` is a distinct quality authority.

It owns:

- tests
- builds
- audits
- runtime validation
- crash triage
- release readiness verification

It should be independent enough to reject weak implementations.

### 4.8 Visual Creation Cell

The studio needs a dedicated visual production function.

The `Visual Creation Cell` is responsible for:

- concept visuals
- production icons and mockups
- key art and branding support
- release presentation visuals

This cell must support both:

- manual human-in-the-loop visual generation
- later studio-automated visual generation workflows

### 4.9 Publishing and Release Cell

The studio must be able to ship, not only build.

The `Publishing & Release Cell` owns:

- version packaging
- changelog production
- release metadata
- store publishing preparation
- post-release checks

It is the cell that converts “done in code” into “released product”.

## 5. Councils and Rituals

The studio should use a mixed cadence model:

- recurring meetings
- event-triggered meetings

### 5.1 Daily Ops Review

A short operational ritual covering:

- active missions
- blockers
- handoffs
- next actions

### 5.2 Weekly Strategy Council

A weekly strategic checkpoint covering:

- priorities
- opportunities
- frozen or accelerated initiatives
- changes in portfolio direction

### 5.3 Milestone Review

Triggered when:

- a major initiative completes
- an initiative stalls
- a major pivot becomes likely

### 5.4 Release Readiness Review

Required before any external publication.

It should confirm:

- quality gates passed
- packaging complete
- metadata complete
- rollback path understood

### 5.5 Post-Release Review

Triggered after shipping.

It should inspect:

- regressions
- support load
- quality misses
- what to improve in the studio process itself

### 5.6 Exception Council

Triggered for:

- major crashes
- major architectural conflicts
- product confusion
- newly added mods or large integrations
- major studio-level risks

## 6. Workflow Lifecycle

The standard lifecycle should be:

1. strategy decision or exploration request
2. exploration note if the space is still fuzzy
3. brief
4. spec
5. implementation plan
6. execution
7. verification
8. release preparation when relevant
9. publication when relevant
10. postmortem and learning extraction

The studio should not collapse all work into one giant “build task”.

## 7. Mission System

`Onivo Studio` should standardize a finite set of mission types.

Primary mission classes:

- `Research Mission`
- `Exploration Mission`
- `Spec Mission`
- `Plan Mission`
- `Build Mission`
- `Verification Mission`
- `Visual Mission`
- `Release Mission`
- `Recovery Mission`

Each mission must contain:

- owner cell
- explicit scope
- project target
- allowed action surface
- success condition
- verification path
- escalation rule

## 8. Studio Repository Structure

The central repository should be organized into stable layers.

### 8.1 Durable Versioned Layers

The bootstrap target is:

- `governance/`
- `strategy/`
- `operating-system/`
- `intelligence/`
- `delivery/`
- `publishing/`
- `projects/`
- `vault/`

These layers have distinct purposes:

- `governance/` defines studio constitution, authority, and policies
- `strategy/` stores direction, portfolio thinking, and innovation priorities
- `operating-system/` stores workflows, playbooks, templates, and mission patterns
- `intelligence/` stores reusable knowledge, postmortems, patterns, and research summaries
- `delivery/` stores cross-project backlog, priorities, and execution portfolio views
- `publishing/` stores release frameworks, checklists, metadata templates, and release operations
- `projects/` stores project adapter packages
- `vault/` stores the Obsidian-first studio memory layer

### 8.2 Runtime Layer

The studio also needs a non-versioned operational layer:

- `.onivo-runtime/`

This layer should hold:

- active mission material
- temporary reports
- evidence bundles
- scratch work
- temporary workspaces

It should not become the permanent source of truth.

## 9. Project Adapter System

Every connected mod project must be represented by a strong project adapter, not a loose note.

### 9.1 Project Adapter Purpose

The adapter is both:

- a project contract
- a project mirror

It tells the studio:

- what the project is
- how to operate it
- how to verify it
- how to release it

### 9.2 Adapter Structure

Inside `projects/<project-id>/`, the recommended layout is:

- `00-dashboard/`
- `01-contracts/`
- `02-product/`
- `03-architecture/`
- `04-execution/`
- `05-quality/`
- `06-release/`
- `07-history/`
- `08-links/`

#### 9.2.1 Dashboard

`00-dashboard/` should expose the living project summary:

- project phase
- current focus
- active missions
- known blockers
- health summary

#### 9.2.2 Contracts

`01-contracts/` should define the operating contracts:

- `Identity Contract`
- `Execution Contract`
- `Decision Contract`
- `Quality Contract`
- `Publishing Contract`

#### 9.2.3 Product

`02-product/` should hold:

- project identity
- product direction
- scope boundaries
- roadmap slices

#### 9.2.4 Architecture

`03-architecture/` should hold:

- system maps
- technical constraints
- integration surfaces
- important structure notes

#### 9.2.5 Execution

`04-execution/` should hold:

- backlog slices
- mission packages
- work packages
- initiative states

#### 9.2.6 Quality

`05-quality/` should hold:

- test strategy
- recurring failure classes
- validation procedures
- known quality debt

#### 9.2.7 Release

`06-release/` should hold:

- version policy
- release train notes
- publishing surface
- store-specific obligations

#### 9.2.8 History

`07-history/` should hold:

- milestones
- decision history
- postmortems
- major transitions

#### 9.2.9 Links

`08-links/` should hold:

- repo links
- store links
- CI links
- environment links
- docs links

### 9.3 Local Project Bridge

Each project repository should expose a lightweight local bridge:

- `.onivo-project/project-manifest.md`
- `.onivo-project/execution-contract.md`
- `.onivo-project/quality-contract.md`
- `.onivo-project/publishing-contract.md`
- `.onivo-project/current-state.md`
- `.onivo-project/entrypoints.md`
- `.onivo-project/risks.md`
- `.onivo-project/studio-links.md`

This gives the studio a reliable reading surface without embedding the whole studio in the project repo.

## 10. Obsidian Vault Design

The Obsidian vault should be versioned in the studio repository.

Its purpose is not to replace formal engineering artifacts. Its purpose is to act as the **navigable studio memory**.

### 10.1 Vault Role

The vault should hold:

- relationship-rich notes
- meeting notes
- decision maps
- research syntheses
- visual identity references
- cross-project links

Formal specs and plans may live outside the vault or be mirrored into it, but the vault is the thinking and navigation layer.

### 10.2 Vault Structure

Recommended top-level structure:

- `00 Home/`
- `01 Governance/`
- `02 Strategy/`
- `03 Operations/`
- `04 Projects/`
- `05 Research/`
- `06 Visual/`
- `07 Publishing/`
- `08 Decisions/`
- `09 Postmortems/`

### 10.3 Key Pages

The studio should have a small number of high-value entry pages:

- `Studio Home`
- `Current Portfolio`
- `Weekly Strategy Board`
- `Mission Radar`
- `Risk Register`
- `Decision Index`
- `Research Atlas`
- `Release Board`

The vault should remain understandable even to someone who only navigates via folders and home pages.

### 10.4 Obsidian Tooling Policy

The initial vault should assume:

- Markdown is the durable substrate
- Obsidian is the preferred human client
- the studio must still remain usable without deep Obsidian knowledge

Community plugins may be added later, but the vault should not require exotic plugins to remain usable.

## 11. MCP, Plugins, and Skills Architecture

The studio should use a small governed connector set, not an uncontrolled MCP zoo.

### 11.1 Core MCP Stack

Recommended bootstrap connectors:

- `GitHub MCP`
- `Filesystem MCP`
- `Git MCP`
- `Sequential Thinking MCP`

These are enough to support:

- portfolio and release operations
- repo inspection
- file-backed studio memory
- bounded strategic reasoning workflows

### 11.2 Obsidian Integration

The studio should not depend on an obscure Obsidian-specific MCP server at bootstrap.

Instead, initial Obsidian integration should be:

- vault files in the repo
- manipulation through normal filesystem tooling
- optional use of `Obsidian URI` for launch/open/create/search actions

This is more stable and easier to reason about than making the studio dependent on a fragile community connector.

### 11.3 Skill Policy

The studio should explicitly standardize when to use key skills:

- `brainstorming` for fuzzy or strategic work
- `writing-plans` after approved specs
- `verification-before-completion` before claiming finished work
- `minecraft-modding` for actual mod implementation work
- `skill-creator` for improving the studio itself
- `plugin-creator` for future studio-specific plugins if needed

### 11.4 Security Rule

Connector policy must stay conservative:

- minimal connector set first
- least privilege configuration
- no unnecessary third-party live systems
- no dependency on unreviewed community MCP tooling for core governance

## 12. Visual Creation System

The studio must support visual generation for production, not only concept moodboards.

### 12.1 Visual Output Layers

The visual system should support:

- concept visuals
- production assets
- release and publishing visuals

### 12.2 Production Asset Classes

Target classes include:

- icons
- UI mockups
- release covers
- banners
- documentation visuals
- selected stylized textures or texture bases when appropriate

### 12.3 Operating Mode

The visual system should evolve in two phases:

- initial manual generation through ChatGPT-assisted workflows
- later partial studio automation through an image-generation API workflow

### 12.4 Visual Governance

The visual system should include:

- `Visual Creation Cell`
- `Visual QA`
- `Brand & Presentation Lead`

And durable studio surfaces such as:

- `visual-system/brand/`
- `visual-system/prompts/`
- `visual-system/review/`
- `visual-system/exports/`

## 13. Publishing System

The studio must support product publication as a first-class function.

### 13.1 Publishing Scope

The publishing layer should eventually support:

- build artifact packaging
- version metadata
- changelog preparation
- release-ready store assets
- release checklists
- post-release checks

### 13.2 Publishing Cells and Rituals

Publishing should always involve:

- `Publishing & Release Cell`
- `Release Readiness Review`
- `Post-Release Review`

### 13.3 Publication Boundaries

Publication authority should remain gated.

The studio may prepare most release material autonomously, but the final publish action should remain a governed step.

## 14. Self-Improvement Model

The studio should improve itself over time, but in a bounded way.

### 14.1 Allowed Self-Improvement

The studio may autonomously improve non-constitutional internal assets such as:

- templates
- playbooks
- mission forms
- internal prompts
- checklists
- vault organization
- review rubrics
- scoreboards

### 14.2 Gated Self-Improvement

The studio must not unilaterally change foundational elements such as:

- core charter
- top-level authority policy
- critical security policy
- foundational escalation rules
- founder authority model

Those changes must go through governance review.

### 14.3 Improvement Loop

The improvement loop should be:

1. mission result or incident
2. postmortem
3. extracted lesson
4. proposed studio change
5. direct application if non-constitutional
6. council escalation if constitutional

This gives the studio real learning power without governance drift.

## 15. Error Handling and Failure Modes

The studio design should assume failures will happen in:

- mission framing
- agent routing
- stale project mirrors
- weak release prep
- weak validation
- connector instability

The studio should therefore require:

- explicit stop conditions
- bounded mission scopes
- separate verification
- durable postmortems
- policy-level learning from repeated failure modes

## 16. Bootstrap Phase

The first implementation slice should bootstrap the studio itself, not a connected mod project.

Bootstrap target:

- create central repository structure
- create studio governance core
- create studio operating-system core
- create Obsidian vault skeleton
- create project adapter template system
- create MCP / skills / visual / publishing registry docs
- create runtime conventions

It should **not** yet attempt:

- full project onboarding
- live project dashboards for external repos
- store publishing automation
- aggressive self-editing governance

## 17. Non-Goals

This first studio design does not attempt to provide:

- full real-time portfolio telemetry
- uncontrolled swarm orchestration
- generalized non-Minecraft studio support
- hard dependency on advanced Obsidian plugins
- hard dependency on experimental community MCP servers

## 18. Next Planning Slice

After this spec is approved, the next implementation plan should cover:

1. bootstrap file/folder structure for `Onivo Studio`
2. core governance artifacts
3. core operating-system artifacts
4. vault skeleton and home pages
5. project adapter templates
6. MCP / skills / visual / publishing registry docs
7. runtime and gitignore policy

Only after that bootstrap is stable should the next plan onboard the first real project repository.

# FTB Core Pack Integration Design

## Objective

Add the FTB core utility mods needed for the modpack on NeoForge 1.21.1 without
changing STAT Mod gameplay code or introducing direct Java dependencies.

Target mods:

- FTB Library
- FTB Teams
- FTB Chunks
- FTB Ultimine
- FTB XMod Compat

## Current Context

The project already loads external mods from `libs/*.jar` and
`libs/META-INF/jars/*.jar` through `build.gradle`. No FTB-specific code,
configs, or integration hooks currently exist in the repository.

## Recommended Approach

Treat the FTB additions as pack/runtime dependencies only.

- Place the compatible NeoForge 1.21.1 jars in `libs/`
- Do not add Maven coordinates or Java compile-time references
- Do not modify STAT Mod systems unless a concrete compatibility issue appears
- Validate by checking jar presence, Gradle resolution, and client startup

This keeps the integration aligned with the current pack architecture, where
third-party content mods are distributed locally through the workspace.

## Alternatives Considered

### 1. Local jars in `libs/` (recommended)

Pros:

- Matches the existing dependency model
- No additional repository management
- Easy to swap exact tested versions

Cons:

- Manual jar management

### 2. Maven-managed FTB dependencies

Pros:

- Cleaner dependency declaration in Gradle

Cons:

- Diverges from the current pack structure
- Adds repository/version resolution work that the pack does not need today

### 3. Add only FTB Library first

Pros:

- Lowest-risk incremental rollout

Cons:

- Does not deliver the actual utility stack requested for the pack

## Architecture Impact

No Java architecture changes are planned.

- `build.gradle` remains the loader mechanism through `fileTree`
- `libs/` becomes the source of truth for the selected FTB jars
- Runtime config files can be added later under `runs/client/config` if needed

## Error Handling

Expected risks:

- Wrong loader or wrong Minecraft version jars
- Missing transitive FTB companion jars
- Startup conflicts with chunking, teams, or utility hotkeys

Mitigation:

- Use only NeoForge 1.21.1-compatible jars
- Validate with targeted build/test commands first
- Escalate to runtime config changes only if the client reports conflicts

## Verification

Minimum verification:

- Confirm the five jars exist in `libs/`
- Run a Gradle command that resolves the mod classpath cleanly
- If needed, run `runClient` and inspect startup logs for missing dependencies

## Scope

In scope:

- Add the five core FTB mods to the workspace pack
- Verify they are loadable in the current project setup

Out of scope:

- FTB Quests content
- Quest data authoring
- Teams/chunks permission tuning
- STAT Mod feature integration with FTB APIs

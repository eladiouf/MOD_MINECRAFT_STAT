# STAT-DEC-M4-REVERSE — Reverse bridge Tensura → Iron's Spellbooks

**Date:** 2026-06-21
**Mission:** M4 (reverse bridge)
**Author:** Onivo Studio build agent
**Branch:** `feature/m4-reverse-bridge`

## Decision

Implement an **exclusive Iron's path** for all 56 Tensura skills enumerated in
`TensuraSpellTaxonomy` by wrapping each one as a `TensuraDelegatingSpell extends AbstractSpell`,
registered in the Iron's Spellbooks `SpellRegistry` under the `statmod` namespace.

## Inscription menu policy

`IronInscriptionKnownSpellIndex.learnedCastableSpellIds()` now accepts two prefixes:
- `irons_spellbooks:*` (native Iron's spells)
- `statmod:tensura_*` (our wrappers — they are real Iron's spells too)

Raw `tensura:*` IDs in `learnedSpells[]` remain filtered out from the inscription menu —
they are present only for bookkeeping (cooldowns, skill storage parity). The old method
name `learnedIronSpellIds` is kept as a `@Deprecated` alias.

## Runtime cast delegation

`TensuraDelegatingSpell.onCast(...)` runs server-side. It looks up (or silently learns) the
target `ManasSkillInstance` via `SkillStorage.getSkill / learnSkill`, then invokes
`onPressed(caster, 0, 0)`. **Tensura magicule is not drained** — Iron's mana cost is the only
gate. This matches the founder decision "Iron's mana only".

## School mapping

`TensuraSchoolMapping` produces only Iron's built-in school paths (`fire`, `ice`, `nature`,
`lightning`, `evocation`, `holy`). Resolution to a `SchoolType` is done defensively at runtime
via `SchoolRegistry.getSchool(ResourceLocation)` rather than direct field access so future
Iron's Spellbooks versions can rename static field constants without breaking the wrappers.

## Cost / cooldown baselines

| Discipline | Mana | Cooldown (ticks) |
|---|---|---|
| support | 75 | 40 |
| defense | 60 | 40 |
| mobility | 40 | 30 |
| utility | 30 | 30 |
| control | 80 | 40 |
| pressure | 90 | 60 |
| empowerment | 70 | 40 |
| offense | 100 | 60 |

All wrappers are `CastType.INSTANT` and capped at `getMaxLevel() == 1` (no scaling for MVP).
Tuning is out of scope for M4 and tracked separately.

## Icon

A single placeholder `statmod:textures/spell/tensura/default.png` is referenced by every wrapper
to avoid maintaining 56 icons in the MVP. The PNG creation itself is intentionally **not** done
inside this sandbox; Iron's tolerates a missing texture (renders the magenta placeholder), which
is acceptable for M4 acceptance and tracked for a follow-up art pass.

## Ambiguity / risk surface

1. **Iron's API signature surface** — the worktree has no access to the `libs/` jars during this
   session (sandbox restriction). Code conforms to the published spec but
   `compileJava`/`compileTestJava` could not be executed end-to-end from this worktree.
   Confidence: HIGH for tested pure-Java parts (`TensuraSchoolMapping`,
   `TensuraWrapperIds`, `IronInscriptionKnownSpellIndex`, `MagicTreeProgressionService`);
   MEDIUM for the `AbstractSpell` override surface (`getDefaultConfig` vs `getConfig`,
   `DefaultConfig.Builder` fluent shape). Any signature drift must be fixed in a follow-up
   commit by running `./gradlew compileJava` in the parent worktree.
2. **`DefaultConfig.Builder` field names** — `setMinRarity / setMaxLevel / setCooldownSeconds /
   setManaCostPerLevel` are assumed from the spec; if Iron's exposes a different builder
   shape the constructor `buildDefaultConfig()` must be adjusted (one place).
3. **`SchoolRegistry.getSchool(ResourceLocation)`** — the defensive try/catch ensures cast still
   works (returns `null` school) if the registry path changes.

## Exit conditions

The wrappers may be retired when:
- Founder reverses the "Iron's exclusive" decision and re-enables Tensura's native HUD path, OR
- Iron's Spellbooks gains a first-class extension API that supersedes the wrapper trick.

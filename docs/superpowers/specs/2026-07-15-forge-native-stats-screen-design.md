# Forge 1.20.1 native stats screen design

**Date:** 2026-07-15
**Branch:** `forge-1.20.1`
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17
**Status:** approved direction

## 1. Goal

Finish the visible portion of the first STAT Mod milestone with a native,
read-only character screen and discreet progression notices. A player must be
able to understand all 23 statistics without using operator commands, while the
server remains the only authority for levels and XP.

This slice does not restore the old NeoForge interface. It builds a small Forge
1.20.1 client layer on top of the synchronized snapshot that already exists.

## 2. Player experience

- `P` opens the character statistics screen by default. The key is configurable
  in Minecraft's Controls menu under a localized `STAT Mod` category.
- The screen uses a restrained medieval parchment palette rendered with
  Minecraft primitives. It does not require external generated textures.
- A family selector exposes the six existing stat families. The first family is
  selected when the screen opens, and the last selection remains active while
  the client session lasts.
- The selected family shows its statistics as compact cards in two columns.
  Each card contains the localized name, level, XP values, progress bar, and a
  short description of the stat's current role.
- Level 100 cards display `MAX` instead of an empty XP requirement.
- `Esc`, the inventory key, or `P` closes the screen. The screen pauses only in
  single-player when Minecraft would normally pause.
- Normal gameplay has no permanent all-stat overlay. Accepted progression
  awards produce a short notice near the upper-right corner instead.

The screen is informational. It contains no level setter, XP button, perk
unlock, or client-to-server mutation packet.

## 3. Presentation truthfulness

The UI must not claim that every planned stat effect already exists. A client
presentation catalog maps every stable `StatType` to translation keys and one
of these display states:

- `ACTIVE`: automatic progression and/or a runtime effect exists now;
- `FOUNDATION`: the stat persists and synchronizes, but its dedicated gameplay
  integration is scheduled for a later milestone.

Descriptions state only current behavior. The magical stats therefore remain
visible but are marked as awaiting the Iron's Spells milestone. The catalog is
exhaustive: a contract test fails if a new stat lacks a name, description, or
display state.

## 4. Client architecture

### 4.1 Input and screen routing

`ClientKeyMappings` owns the `P` mapping and registers it on the mod event bus.
`ClientInputEvents` consumes clicks on the Forge client event bus and opens one
`StatsOverviewScreen`. Input is ignored when no local player exists.

The key handler reads no server capability and sends no open-screen packet.
Opening a read-only screen is entirely client-side because the authoritative
snapshot is already synchronized on login, respawn, dimension change, and stat
mutation.

### 4.2 Read-only view model

`StatsScreenModel` converts a `ClientStatsCache.snapshot()` into immutable
family and card records. It owns ordering, level-100 formatting, bounded XP
progress, and presentation lookup. The Minecraft screen renders this model but
does not reproduce progression formulas in widget code.

The model uses `StatProgress.requiredXp(level)` as the canonical requirement.
Progress is clamped to `[0, 1]` defensively when a malformed or stale snapshot
is encountered. Missing values render as level 0 with 0 XP, matching the cache's
existing safe default.

### 4.3 Screen components

`StatsOverviewScreen` owns responsive layout, the family selector, cards,
tooltips, and scrolling. Small GUI scales retain one readable card column;
normal widths use two columns. Only the card area scrolls, so the title and
family selector remain reachable.

`StatCardRenderer` is a focused rendering helper rather than an interactive
widget. Family selection uses vanilla buttons for keyboard narration and mouse
accessibility. All visible and narrated text comes from translation keys.

No third-party UI classes are referenced. Epic Fight and Pufferfish's
Attributes may be required runtime providers, but the screen remains owned by
STAT Mod.

## 5. Progression notices

Snapshot comparison alone cannot reliably distinguish login synchronization
from a new award. The server therefore sends a dedicated bounded clientbound
notice only after an accepted XP mutation:

`StatProgressNoticeMessage(statId, awardedXp, newLevel, levelsGained)`

The payload rejects unknown IDs and clamps numeric fields to domain limits.
It never changes client stats; the normal full snapshot remains the data source
of truth.

`ClientProgressNotices` keeps at most four notices. Consecutive notices for the
same stat within 20 ticks merge their XP amount. A level gain receives stronger
color and stays visible for 80 ticks; an XP-only notice stays for 50 ticks.
Notices fade during their final 15 ticks and are suppressed while the debug
screen is visible. Disconnect clears both the notice queue and stats cache.

The progression coordinator emits the notice and refreshed snapshot from the
same accepted server result, ensuring the displayed level matches the award.
Administrative level assignment does not create a gameplay progression notice.

## 6. Localization

French and English resources cover:

- key and key category;
- screen title, family names, level, XP, `MAX`, and status labels;
- names and current-behavior descriptions for all 23 stats;
- XP-gain and level-gain notice formats;
- accessibility narration for family buttons and stat cards.

Stable code identifiers stay unchanged. Display names may be improved through
translations without migrating saved data.

## 7. Error handling

- An empty cache opens a complete level-0 screen rather than failing.
- Unknown notice stat IDs are discarded without changing state.
- Invalid notice counts or values are bounded during decoding.
- The screen takes a fresh immutable cache snapshot when opened and refreshes
  its model when the cache revision changes; it never iterates mutable network
  state during rendering.
- Closing or reopening the screen cannot send packets or duplicate awards.
- Client-only classes are isolated behind `Dist.CLIENT` registration so a
  dedicated server never loads rendering or input classes.

## 8. Verification

Focused plain-Java tests cover:

- all six families and all 23 stats appear exactly once in stable enum order;
- each stat has complete French/English presentation keys;
- XP formatting and progress at levels 0, 99, and 100;
- malformed values are clamped without division by zero;
- notice validation, queue bounds, merging, duration, and clearing;
- snapshot replacement increments the client revision used by the screen.

Forge contract tests cover key registration, client-only event wiring, packet
direction, bounded decoding, logout clearing, and the absence of mutation calls
from screen code.

Runtime validation covers opening and closing with `P`, every family at small
and normal GUI scales, one XP award, one level gain, reconnect behavior, and a
dedicated-server startup. The final gate remains `gradlew clean test build` plus
the clean-foundation verifier.

## 9. Exclusions

- perk tree, perk buttons, respec, and stat spending;
- permanent health, mana, stamina, combo, or survival HUD replacement;
- Iron's Spells descriptions or magical progression;
- imported 1.21.1 GUI code or NeoForge APIs;
- AI-generated textures and custom shaders;
- changes to stat IDs, XP curves, NBT schema, or server authority.

These systems remain separate milestones so this screen stays small, honest,
and testable.

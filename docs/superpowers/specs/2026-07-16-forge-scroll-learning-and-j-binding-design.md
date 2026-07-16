# Forge 1.20.1 Scroll Learning and J-Binding Design

**Date:** 2026-07-16  
**Status:** Validated design  
**Target:** `forge-1.20.1`  
**Required provider:** Iron's Spells 'n Spellbooks `1.20.1-3.16.2`

## 1. Goal

On Forge 1.20.1, Iron's spell scrolls stop being disposable casting
implements. Right-clicking a scroll permanently teaches its contained spell to
the player. Learned spells then appear in a binding interface opened with `J`,
where they can be searched, filtered, and inscribed into a spellbook without
using another scroll.

The feature ports the established NeoForge 1.21.1 workflow while improving
addon coverage: school filters are derived from the learned spells instead of
being limited to Iron's nine native schools.

## 2. User-visible rules

### 2.1 Learning from a scroll

- Every item implementing Iron's `IScroll` is treated as a learning scroll,
  including scroll items supplied by compatible Iron's addons.
- Right-clicking a valid scroll does not initiate a cast.
- The server reads the single `SpellData` stored in the scroll, resolves its
  registered spell identifier, and records the spell and level permanently.
- Learning a new spell consumes one scroll, except in Creative mode.
- A higher-level scroll upgrades the player's learned level and is consumed,
  except in Creative mode.
- A scroll whose spell is already learned at the same or a higher level is not
  consumed. The player receives an `already learned` action-bar message.
- Empty, malformed, unknown, or unregistered scroll data never consumes the
  item. Such a scroll still cannot cast and produces a concise error message.
- Learned levels are clamped to the registered spell's legal level range.

The learned level is the highest valid level the player has studied for that
spell. Learning a lower level can never reduce it.

### 2.2 Direct-cast removal

STAT Mod intercepts scroll right-click before `Scroll.use` reaches Iron's cast
path. The interaction is canceled on both logical sides, and only the server
may mutate learned state or the held stack. This applies to all `IScroll`
implementations, not only the native `irons_spellbooks:scroll` item.

No scroll can bypass the rule because the player has sufficient mana, because
the spell has no target, or because an addon supplies the contained spell.
Iron's normal casting from spellbooks, staffs, curios, and STAT Mod's future
learned-spell selection remains unaffected.

### 2.3 The `J` binding interface

- `J` is registered as `Open spell binding` in the STAT Mod key category.
- Pressing `J` while in-game asks the server to open a virtual Iron's
  inscription menu.
- The menu uses Iron's native spellbook slot and inscription presentation, so
  spellbook compatibility remains owned by Iron's.
- A panel on the left displays only the current player's learned, registered,
  bindable spells.
- Each spell is represented by its real Iron's icon and a tooltip containing
  name, learned level, school, rarity, mana cost, cooldown, and whether it is
  already present in the slotted spellbook.
- The panel provides text search, dynamic school filters, deterministic sorting,
  and paged icon navigation. Filtering is case-insensitive and accepts both the
  translated display name and registry identifier.
- School filters are generated from the visible learned-spell registry data.
  Native and addon schools therefore work without a hard-coded namespace list.
- Selecting a learned spell and an empty target slot enables the native
  inscription action.
- Binding uses the player's highest learned level. It consumes neither a scroll
  nor a second copy of the spell.
- A spellbook must be placed in the menu's spellbook slot. Merely having one in
  inventory or in a Curios slot is not enough for this first Forge port.
- The left panel also appears when the player opens a physical Iron's
  inscription table, keeping the same binding behavior in both entry points.

The ordinary Iron's crafting and loot systems that create scrolls remain
available. This scope removes scroll casting and makes right-click their
learning action; it does not delete scroll recipes or loot.

## 3. Architecture

### 3.1 Persistent learned-spell state

`PlayerStats` gains a bounded learned-spell map:

```text
canonical spell registry id -> highest learned level
```

The map is serialized inside the existing player capability NBT, copied on
player clone, and retained across death and dimension changes alongside the
other STAT Mod state. Invalid identifiers, blank values, duplicates, excessive
entries, and non-positive levels are rejected or sanitized during load.

The initial safety bounds are:

- at most 512 learned spells per player;
- at most 128 UTF-8 characters per spell identifier;
- one canonical entry per registered identifier;
- levels clamped again against the live spell registry when used.

Old Forge saves without the new NBT entry load with an empty learned-spell map.
No affinity or magic-tree state is introduced by this feature.

### 3.2 Scroll learning boundary

The feature is split into focused units:

- `LearnedSpellState` owns canonical map rules, upgrades, snapshots, and NBT
  sanitization.
- `IronScrollDescriptor` extracts and validates registry ID and level from an
  `IScroll` stack through Iron's public spell-container API.
- `ScrollLearningService` makes the pure decision: new learn, level upgrade,
  duplicate, or invalid.
- `IronScrollLearningEvents` owns Forge interaction cancellation, server-side
  capability mutation, stack consumption, sound/message feedback, and sync.

The event handler runs at high priority. It cancels recognized scroll use even
when learning fails, which is necessary to prevent Iron's original direct-cast
fallback.

### 3.3 Synchronization and networking

The existing stats snapshot is extended with a bounded list of learned spell
entries. The network protocol version is incremented. The client cache exposes
an immutable learned-spell snapshot for rendering only; it never authorizes a
learn or binding operation.

Two client-to-server actions are added:

1. open the virtual binding menu;
2. select/bind a learned spell through the active inscription container.

The open request is throttled per player. Every binding request is validated
against the server's current menu, container ID, learned-spell map, live spell
registry, spellbook stack, legal target slot, and current contents. Client
indices, displayed levels, and filter results are never trusted.

### 3.4 Virtual inscription menu

The virtual menu subclasses Iron's `InscriptionTableMenu` and overrides only
the physical-block validity requirement. It retains Iron's inventory ownership,
spellbook rules, slot synchronization, and close behavior.

A narrowly scoped screen mixin adds the learned-spell panel to Iron's
`InscriptionTableScreen`. A corresponding server-menu bridge handles the
selected learned spell and performs the final inscription through Iron's
`ISpellContainerMutable` API. No Iron's classes are copied into STAT Mod.

Mixin registration is limited to the two inscription integration points and is
guarded by the required Iron's dependency. Injection failures must stop in a
development build rather than silently leaving an unsecured half-feature.

## 4. Data flow

### 4.1 Learning

1. Player right-clicks an `IScroll` stack.
2. Client and server interaction handlers cancel normal item use.
3. Server extracts the contained `SpellData` and resolves the canonical ID.
4. The learning service compares the scroll level with persistent state.
5. On a new spell or upgrade, the server records the level and consumes one
   scroll outside Creative mode.
6. The server sends feedback and a fresh learned-spell snapshot.
7. On duplicate or invalid data, state and inventory remain unchanged.

### 4.2 Binding

1. Player presses `J`; the server opens the virtual inscription menu.
2. The client renders its synchronized learned-spell list on the left.
3. Player slots a compatible spellbook, filters the list, and chooses a spell.
4. Selection is sent to the server with the active container context.
5. Server resolves the canonical spell and learned level from its own state.
6. Server validates the target spellbook slot and Iron's inscription event.
7. Iron's mutable spell container receives the learned spell at that level.
8. Menu contents synchronize normally; the UI marks the spell as bound.

## 5. Error handling and security

- Client-side learned data is presentation-only.
- Unknown namespaces are allowed when the live Iron's registry resolves them;
  compatibility is registry-driven, not a hard-coded addon allowlist.
- Unknown registry IDs, removed addon spells, malformed NBT, illegal levels,
  full spellbooks, occupied slots, stale containers, mismatched container IDs,
  and non-spellbook targets are rejected without item loss.
- Repeated `J` packets are rate-limited and do not open overlapping menus.
- Repeated binding packets are idempotent where possible and cannot duplicate
  or delete inventory items.
- Existing Iron's `InscribeSpellEvent` cancellation remains authoritative.
- Disconnect and client-world changes clear the client cache and request state.

## 6. Feedback and localization

The implementation adds English and French translations for:

- spell learned;
- learned level upgraded;
- spell already learned at this level;
- invalid or empty scroll;
- binding menu key and title;
- search placeholder, school filter, learned level, and already-bound tooltip;
- missing/full/incompatible spellbook errors.

Learning success uses a short magical sound and an action-bar message. It does
not add a toast, persistent HUD notification, particle storm, or affinity UI.

## 7. Verification strategy

### 7.1 Pure and serialization tests

- new spell, upgrade, equal duplicate, lower duplicate, invalid ID, and invalid
  level outcomes;
- highest-level-wins and bounded-map behavior;
- learned-spell NBT round trip, old-save default, malformed-entry sanitization,
  clone copy, and no aliasing;
- deterministic search, name/ID matching, dynamic school filtering, addon
  namespace acceptance, sorting, paging, and selection normalization;
- packet bounds, encode/decode symmetry, and protocol registration.

### 7.2 Integration contract tests

- every `IScroll` use path is canceled before direct casting;
- only successful server-side learn/upgrade consumes a scroll;
- Creative mode learns without consumption;
- `J` is registered once and sends only while in-game;
- virtual-menu requests are throttled;
- binding revalidates learned state, learned level, menu identity, spellbook,
  target slot, and registry entry;
- screen and menu mixins target the expected Iron's 3.16.2 methods;
- required-provider smoke still starts Forge with Iron's, Epic Fight, Puffish,
  and Lootr.

### 7.3 Manual acceptance

1. Learn native and addon scrolls at multiple levels.
2. Confirm none of them casts directly, including instant, charged, targeted,
   non-targeted, shield, and utility spells.
3. Confirm duplicate and lower scrolls remain in hand.
4. Reconnect and verify learned spells and levels persist.
5. Open `J`, search by translated name and registry ID, and filter native and
   addon schools.
6. Bind learned spells into several Iron's-compatible spellbooks and confirm
   their learned levels are retained.
7. Verify full books, occupied slots, invalid addon removal, and stale packets
   fail without consuming or corrupting items.
8. Cast the bound spells through Iron's normal controls and verify mana,
   cooldown, targeting, and STAT Mod XP behavior remain intact.

## 8. Out of scope

- affinities or an affinity tree;
- automatic spell unlocks from stat levels;
- a replacement for Iron's spellbook casting system;
- automatic binding into a Curios spellbook without opening the menu;
- changing scroll loot rates, recipes, rarity, or the scroll forge;
- changing spell balance, mana costs, cooldowns, targeting, or damage;
- granting Erudition XP for scroll learning in this feature;
- porting unrelated 1.21.1 magic-tree, Tensura, or custom-race systems.

## 9. Completion criteria

The feature is complete when all valid native and addon `IScroll` items teach
their registered spell instead of casting it, learned spells and highest levels
persist and synchronize safely, `J` opens the searchable/filterable binding
interface, server-authorized binding works without a scroll, regression and
provider smoke tests pass, and the verified JAR is deployed to the clean
`test-vrai` client.

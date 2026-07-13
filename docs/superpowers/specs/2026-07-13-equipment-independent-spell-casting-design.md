# Equipment-independent spell casting

## Goal

Allow every spell learned through STAT Mod to be cast with Iron's Spells' existing
`Cast active spell` key regardless of the item held in either hand. Swords, food,
blocks, empty hands, and other equipment must not prevent casting. Staffs remain
useful because their attributes and spell bonuses still apply, but they are not a
casting requirement.

## Scope

- Keep Iron's Spells' spell wheel, active-cast key, quick-cast keys, animations,
  mana, cooldowns, and cast lifecycle.
- Apply equipment-independent behavior only to spells learned and stored in
  `PlayerStatData`.
- Preserve native Iron's Spells behavior for spells supplied by spellbooks,
  weapons, curios, and other equipment.
- Do not turn arbitrary held items into spell containers or casting implements.
- Do not add right-click casting to ordinary items.

## Design

### Virtual learned-spell slot

`SpellSelectionManagerMixin` exposes learned spells through one stable virtual
slot named `statmod_learned`. The slot is independent of every Minecraft
equipment slot. Its selection options exist on both logical sides because Iron's
Spells constructs a `SpellSelectionManager` independently on the client and the
server.

The client continues to send Iron's Spells' normal selection and cast packets.
No STAT Mod key binding or duplicate cast protocol is introduced.

### Server-authoritative cast resolution

When Iron's Spells handles an active or quick cast whose selection belongs to
`statmod_learned`, STAT Mod resolves the selected spell against the server's
`PlayerStatData` before initiating it. A spell is castable only when:

- the spell identifier is valid and registered;
- the identifier is present in the player's learned-spell set;
- Iron's Spells' normal mana, cooldown, level, and spell-specific checks pass.

The cast is initiated with `ItemStack.EMPTY`, so the current main-hand and
off-hand stacks cannot become prerequisites. The spell still reads the player's
active attributes, allowing staff bonuses to work normally while equipped.

Selections originating from native equipment continue through Iron's Spells'
unchanged code path.

### Equipment changes

Changing the held item must not invalidate a virtual learned-spell selection or
cancel its cast merely because the old or new item differs. Genuine lifecycle
events such as death, teleportation, opening a container, selecting another
spell, or an Iron's Spells cancellation condition retain their native behavior.

## Compatibility and failure handling

- The implementation remains optional behind the existing `irons_spellbooks`
  mixin compatibility gate.
- An unknown or no-longer-learned spell is rejected server-side without mana
  consumption or cast startup.
- Native equipment spells are not promoted into the virtual slot and retain
  their equipment requirements.
- Existing staff attribute scaling is not changed.

## Verification

Automated tests cover:

- recognition of the stable virtual slot;
- server authorization of learned and unlearned spell identifiers;
- equipment-independent selection/cast policy for an empty hand, sword, food,
  block, and staff;
- preservation of native equipment-slot behavior;
- source-level mixin registration and optional-mod gating.

The final build must pass the complete Gradle test suite. A client smoke test
must confirm active-key and quick-key casting while switching between an empty
hand, a sword, and a staff, and confirm that staff attributes still improve the
resulting spell as before.

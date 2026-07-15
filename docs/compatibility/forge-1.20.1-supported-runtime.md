# Forge 1.20.1 supported runtime

This record separates required runtime providers from integrations that remain
prepared, optional, or excluded.

| Component | Version | Status | Notes |
|---|---:|---|---|
| Minecraft | 1.20.1 | verified | Compiles and passes the automated suite. The native stats screen targets this client version. |
| Forge | 47.4.10 | verified | Compiles and passes the automated suite and required-provider GameTest profile. |
| Java | 17 | verified | Required build and runtime toolchain. |
| Iron's Spells 'n Spellbooks | 3.16.2 | required | Mandatory provider for spell power, casting, mana, and magic-resistance attributes; successful spellbook casts now award Arcane Power, Casting Speed, and Mana Pool XP. |
| Epic Fight | 20.14.17 | required | Mandatory provider for stamina and attack-speed attributes. |
| Pufferfish's Attributes | 0.8.2 | required | Mandatory provider for specialized movement attributes. |
| ParCool | 3.4.3.3 | prepared | Optional provider for the existing Endurance stamina targets; minimal runtime is server-smoked. |
| Curios API | 5.14.1 | dependency | Required by Iron's Spells. |
| GeckoLib | 4.8.4 | dependency | Required by Iron's Spells and Iron's Lib. |
| Iron's Lib | 2.1.0 | dependency | Required by Iron's Spells. |
| Player Animator | 1.0.2-rc1 | dependency | Required by Iron's Spells. |
| Patchouli | 85 | dependency | Retained because ParCool's guide recipe and loot reference Patchouli resources. |
| Tensura | excluded | unsupported | Deliberately absent from the Forge remake. |

## Status meanings

- `verified`: the stated scope has passed the build and automated tests.
- `required`: Forge must reject world loading when this provider or its minimum
  supported version is missing.
- `dependency`: support library required by one component in the selected
  development profile.
- `prepared`: files and dependencies are catalogued, but the integration is not
  implemented or validated end to end.
- `untested`: no compatibility claim is made.
- `unsupported`: the component is deliberately excluded.

Client startup, normal world creation, dedicated-server gameplay, Iron's
Spells, and addon profiles are promoted to `verified` only after
their matching smoke procedures pass. Reaching the title screen alone is not a
compatibility result.

## Verified runtime profiles

The required-provider Forge GameTest dedicated-server profile is verified. It
loads STAT Mod with Epic Fight 20.14.17, Pufferfish's Attributes 0.8.2, Iron's
Spells 3.16.2, and Iron's mandatory libraries,
creates all three vanilla dimensions, completes the finite GameTest run, saves
them, and shuts down without a fatal log signature. It does not verify normal
client world creation, combat feel, or Iron's Spells gameplay.

Run the reproducible profile with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 `
  -ProviderModsDirectory "C:\path\to\the\validated\client\mods"
```

The script requires the exact audited provider filenames, remaps their classes
and Mixin refmaps for the ForgeGradle development runtime, and removes its
temporary copies after the run.

## Minimal `test-vrai` development profile

The supported STAT Mod development profile consists of these ten JARs:

- STAT Mod 0.1.0+1.20.1;
- Epic Fight 20.14.17;
- Pufferfish's Attributes 0.8.2;
- ParCool 3.4.3.3;
- Iron's Spells 3.16.2;
- Iron's Lib 2.1.0;
- Curios API 5.14.1;
- GeckoLib 4.8.4;
- Player Animator 1.0.2-rc1;
- Patchouli 85.

Launcher-owned or user-owned utility JARs may coexist in `test-vrai`; they are
outside this compatibility claim and must not be silently removed during STAT
Mod deployment. The deployment operation replaces only the STAT Mod JAR.

The complete set passes the Forge GameTest startup smoke: all mod IDs load,
the overworld and Iron's Spells pocket dimension are created and saved, and the
server shuts down normally. This is a server-startup result, not a completed
client gameplay validation.

Iron's Spells 3.16.2 currently logs two non-fatal loot-table parse errors for
`chests/catacombs/crypt_loot` and `chests/citadel/citadel_tomes`. Both invalid
entries are present inside the unmodified upstream JAR. STAT Mod does not patch
that third-party JAR. Patchouli removes the separate ParCool guide recipe and
loot errors that occur when ParCool is loaded without its guide provider.

## Native stats client surface

Pressing `P` opens STAT Mod's read-only statistics screen. It displays all five
families and all 19 server-authoritative values, refreshes from revisioned
snapshots, and closes with `P`, Escape, or the inventory key. Automatic XP
awards publish bounded, mergeable notifications; administrative mutations
synchronize the screen without presenting them as gameplay rewards.

Player-stat schema 2 retires the unused Fire, Water, Earth, and Air affinity
entries. Schema-1 saves retain every remaining stat; retired affinity compounds
are ignored and omitted from the next save.

Successful server-side Iron spellbook casts use the provider's original spell
level and mana cost to progress Arcane Power, Casting Speed, and Mana Pool.
Scrolls, spellblades, commands, mobs, schools, and retired affinities do not
produce cast XP. Erudition and Magic Resistance await their dedicated gameplay
sources.

Creative players are accepted only by the dedicated spell-cast award path so
the development client can validate progression; Spectator and fake players
remain excluded. Cast XP is committed-event based: shields, healing, movement,
summons, control, and other non-damaging spells count without a target or hit.
All other automatic XP remains disabled in Creative.

Erudition also progresses from committed Iron's Spells inscriptions and from a
40-tick held-use study of vanilla enchanted books. Every stored enchantment is
counted as `level * rarity weight * 5` with weights 1/2/4/8 and a 160 XP
per-book clamp before the existing 200 XP per-stat, 1,200-tick rolling limit.
Successful study consumes one book outside Creative, displays that exact book
with the vanilla item-activation motion, and plays the Totem sound; interruption
or a fully rejected reward consumes nothing. Creative study retains the book
but observes the same rolling cap.

Magic Resistance progresses only from final positive `LivingDamageEvent`
damage whose source is Iron's Spells `SpellDamageSource`. It does not trust the
earlier mutable `SpellDamageEvent`, require a kill, or classify a spell school.

## Automatic perks

The supported runtime contains 33 automatic perks: the original 21 perks for
the seven mature attribute-backed stats plus 12 classified combat perks for
Brute Force, Blade Technique, Precision, and Physical Resistance. Three
cumulative milestones activate at levels 25, 50, and 75.
Activation is derived from the current server-authoritative levels, so
lowering a level below a requirement immediately removes the corresponding
bonus; no separate unlock state is saved.

The offensive defaults add 5% classified damage per milestone after the
continuous Brute Force, Blade Technique, or Precision multiplier. The defensive
default adds 2 physical-reduction percentage points per milestone before the
0.95 resistance safety cap, then composes multiplicatively with Physical
Endurance. At level 100 in both defensive stats with all three resistance perks,
the final multiplier is `0.1885`, for `81.15%` total reduction.
Tracking and Keen Senses are deferred from this combat batch.

There is no tree, perk points, purchases, respecs, or affinities. The server
synchronizes known IDs for the active-perk list in the native `P` screen.
Pufferfish's Attributes remains an attribute provider and never owns perk
progression. Existing stable transient modifier UUIDs combine continuous stat
scaling and milestone bonuses without stacking.

The bounded snapshot transport uses protocol 6 and accepts at most the 33
canonical perk IDs in catalog order.

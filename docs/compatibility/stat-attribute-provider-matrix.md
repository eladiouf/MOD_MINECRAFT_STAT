# STAT Mod attribute provider matrix — Forge 1.20.1

**Audit date:** 2026-07-15  
**Client:** `test-1.20.1`  
**Method:** local JAR class/bytecode inspection; no compatibility is inferred
from filenames alone.

## Integration policy

STAT Mod owns stat levels, progression, balance curves, and the decision that
an effect exists. Existing mod attributes are preferred as optional output
channels when they already implement the matching gameplay mechanic.

Rules:

1. Core STAT Mod does not import provider internals when a registry attribute
   is sufficient, even for required providers.
2. Provider attributes are resolved by registry ID after registries exist.
3. Each STAT Mod effect has one numerical owner; equivalent attributes from
   two providers are not applied together.
4. Modifiers use stable STAT Mod UUIDs, are removed before replacement, and
   never stack after relog, death, dimension change, or stat mutation.
5. Epic Fight and Puffish Attributes are required providers. Forge rejects a
   missing/unsupported required provider before world load. ParCool and other
   providers remain optional; a missing optional output is skipped.
6. Weapon damage and physical reduction remain owned by the current STAT Mod
   damage policy; Puffish damage/resistance attributes must not duplicate them.
7. Direct APIs are used only in a guarded integration module when registry
   attributes cannot express the required behavior.
8. Iron's public typed `SpellOnCastEvent` is used only inside
   `integration.ironspells` because registry attributes cannot report a
   committed cast. Original level and mana values feed the provider-neutral XP
   policy; school data is discarded.

## Audited providers

| Provider | Tested local JAR | Registry/API evidence |
|---|---|---|
| Epic Fight | `epic-fight-20.14.17-mc1.20.1-forge.jar` | `EpicFightAttributes` and `PlayerPatch` |
| ParCool | `ParCool-1.20.1-3.4.3.3.jar` | `Attributes`, `Stamina`, and `EpicFightStamina` |
| Iron's Spells | `irons_spellbooks-1.20.1-3.16.2.jar` | public `api.registry.AttributeRegistry` |
| Apothic Attributes | `ApothicAttributes-1.20.1-1.3.7.jar` | public `ALObjects.Attributes` |
| Puffish Attributes | `puffish_attributes-0.8.2-1.20-forge.jar` | public `PuffishAttributes` IDs and attributes |

## Exact useful attributes

### Epic Fight

The installed version registers:

- `epicfight:staminar` — maximum stamina; the misspelling is the real runtime
  registry ID and must not be corrected in code;
- `epicfight:stamina_regen`;
- `epicfight:stun_armor`;
- `epicfight:weight`;
- `epicfight:max_strikes`;
- `epicfight:armor_negation`;
- `epicfight:impact`;
- `epicfight:execution_resistance`;
- `epicfight:offhand_attack_speed`;
- `epicfight:offhand_max_strikes`;
- `epicfight:offhand_armor_negation`;
- `epicfight:offhand_impact`.

### ParCool

The installed version registers:

- `parcool:max_stamina`;
- `parcool:stamina_recovery`.

Its built-in `EpicFightStamina` adapter already reads Epic Fight's current and
maximum stamina while Epic Fight mode is active. Parkour stamina consumption is
buffered and applied to the Epic Fight `PlayerPatch` on the server. Outside
Epic Fight mode, ParCool falls back to its own stamina capability. Therefore
STAT Mod must scale both providers' capacity attributes but must not implement
a third stamina pool or mirror current stamina values itself.

### Iron's Spells

The installed public registry exposes:

- `irons_spellbooks:max_mana`;
- `irons_spellbooks:mana_regen`;
- `irons_spellbooks:cooldown_reduction`;
- `irons_spellbooks:spell_power`;
- `irons_spellbooks:spell_resist`;
- `irons_spellbooks:cast_time_reduction`;
- `irons_spellbooks:summon_damage`;
- `irons_spellbooks:casting_movespeed`;
- school power and resistance attributes for `fire`, `ice`, `lightning`,
  `holy`, `ender`, `blood`, `evocation`, `nature`, and `eldritch`.

School attributes follow the installed registry's `<school>_spell_power` and
`<school>_magic_resist` naming. STAT Mod deliberately does not map them to
separate affinity statistics; the four unused affinities are retired from the
Forge 1.20.1 roster.

STAT Mod also compiles against the pinned public `SpellOnCastEvent` and
`CastSource` API. Only committed `SPELLBOOK` events reach automatic XP; the
provider JAR remains compile-only and is never embedded.

### Apothic Attributes

Useful installed IDs include:

- `attributeslib:armor_pierce`, `armor_shred`;
- `attributeslib:arrow_damage`, `arrow_velocity`, `draw_speed`;
- `attributeslib:crit_chance`, `crit_damage`;
- `attributeslib:dodge_chance`;
- `attributeslib:experience_gained`;
- `attributeslib:healing_received`, `life_steal`, `overheal`;
- `attributeslib:mining_speed`;
- `attributeslib:fire_damage`, `cold_damage`.

### Puffish Attributes

Useful installed IDs in namespace `puffish_attributes` include:

- `stamina`;
- `magic_damage`, `melee_damage`, `ranged_damage`, `sword_damage`,
  `axe_damage`, `trident_damage`;
- `resistance`, `magic_resistance`, `melee_resistance`,
  `ranged_resistance`;
- `jump`, `sprinting_speed`, `mount_speed`, `fall_reduction`;
- `breaking_speed`, `mining_speed`, `consuming_speed`;
- `fortune`, `experience`, `healing`, `natural_regeneration`;
- `knockback`, `stealth`, `damage_reflection`, `life_steal`;
- `bow_projectile_speed`, `crossbow_projectile_speed`;
- armor, toughness, protection, and resistance shred variants.

Puffish damage and resistance attributes are not selected for the five combat
effects already owned by `CombatEffectEvents`, because using both would double
scale the same hit.

## Planned stat mapping

| STAT Mod stat | Preferred output | Fallback / note |
|---|---|---|
| Brute Force | STAT Mod heavy-hit policy | Later Epic Fight `impact` may add posture identity, never duplicate raw damage. |
| Blade Technique | STAT Mod blade-hit policy | `max_strikes` or armor negation requires an Epic Fight-specific balance slice. |
| Precision | STAT Mod precision-hit policy | Prefer Apothic draw speed/arrow velocity later; do not add arrow damage twice. |
| Rapidité | **Implemented:** `minecraft:generic.attack_speed` and `epicfight:offhand_attack_speed` | +30% base at level 100 by default; stable transient modifiers. |
| Agility | **Implemented:** `minecraft:generic.movement_speed` and `puffish_attributes:sprinting_speed` | +20% general movement and +10% specialized sprinting at level 100 by default. |
| Physical Resistance | STAT Mod physical damage policy | Epic Fight stun armor is complementary; Puffish resistance is not stacked. |
| Physical Endurance | **Implemented:** Epic Fight max stamina/regen and ParCool fallback max/recovery | Registry-only transient modifiers; +100% capacity and +50% recovery at level 100 by default. Existing ParCool adapter chooses the active pool. |
| Tracking | No direct output selected | Fortune is not assumed to represent tracking until loot behavior is designed. |
| Keen Senses | Optional Apothic dodge chance | Must remain capped and distinct from Agility. |
| Arcane Power | **Implemented:** Iron's Spells spell power | +100% base at level 100 by default; Puffish magic damage is not stacked. |
| Casting Speed | **Implemented:** Iron's cast-time and cooldown reduction | +30% cast-time and +20% cooldown attributes at level 100; casting move speed is excluded. |
| Mana Pool | **Implemented:** Iron's max mana and mana regeneration | 500 base mana; +200% capacity at level 100; regen from 1/s to a strict 17/s cap; no separate STAT Mod mana pool. |
| Erudition | No direct output selected | Apothic/Puffish experience bonuses require a progression-economy design. |
| Magic Resistance | **Implemented:** Iron's spell resist | +50% base at level 100; Puffish magic resistance is not stacked. |
| Intimidation | Epic Fight impact candidate | Execution effects require perk and boss-safety rules. |
| Willpower | Epic Fight execution resistance candidate | Negative-effect duration still needs its own bounded event rule. |
| Forging | Puffish repair-cost candidate | Actual forging quality remains owned by STAT Mod recipes/stations. |
| Cooking | Puffish consuming speed/healing candidate | Food quality requires the provisioning slice. |
| Alchemy | Puffish healing candidate | Potion strength/duration requires an alchemy-specific policy. |

## Implementation order

1. **Implemented:** Physical Endurance → existing Epic Fight/ParCool stamina
   attributes. Refresh occurs on login, respawn, dimension change, stat commands,
   and automatic XP level-up; stable UUID replacement prevents stacking.
2. **Implemented:** Rapidité and Agility → vanilla plus required-provider
   combat/movement attributes. Jump and fall reduction remain excluded from
   this slice.
3. **Implemented:** Iron's Spells core → mana, spell power/resist, and casting
   attributes, with Iron's Spells 3.16.2+ required on both sides.
4. Mental and crafting candidates only after their gameplay policies exist.

Each step must test absence of every optional provider and verify that switching
Epic Fight battle mode does not create, refill, or duplicate stamina. Required
Iron, Epic Fight, and Puffish versions are enforced by Forge metadata.

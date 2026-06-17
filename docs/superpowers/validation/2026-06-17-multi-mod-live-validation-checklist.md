# STAT Mod Multi-Mod Live Validation Checklist

**Goal:** validate the current implementation of `.opencode/plans/2026-06-16-multi-mod-integration-master-plan.md`
against the current mod baseline in `runs/client/mods`.

## Baseline

- Run `.\gradlew.bat runClient`
- Ignore known third-party noise already seen in `latest.log` unless it blocks the exact scenario under test:
  - `xbbsroaringknightmod` invalid asset paths
  - `magistuarmory` missing models
  - `epicfight_dd`, `indestructible`, `sword_soaring` animation datapack errors
  - missing subtitle translations from addon mods

## Common Setup

- Use a fresh creative test world
- Enable cheats
- Keep `logs/latest.log` and `runs/client/logs/latest.log` after each scenario
- Use the built-in debug commands when useful:
  - `/statlevel <stat> <value>`
  - `/statxp <stat> <value>`
  - `/statmod sync`

## Tensura

- Race modifiers:
  - switch to `human_saint`, `elf_saint`, `kijin`, `mystic_oni`, `slime`, `divine_dragon`, `vampire`, and `arch_daemon`
  - verify `CASTING_SPEED` and `MANA_POOL` each gain the planned race bonus
- Intrinsic perks:
  - trigger a naming/race flow that grants intrinsic skills
  - confirm mapped free perks unlock automatically
- Soul XP multiplier:
  - set soul level to `0`, gain one fixed stat XP source, record result
  - repeat at soul level `50` and confirm the gain is higher
- Parallel Existence:
  - learn `tensura:parallel_existence`
  - repeat the same XP gain and confirm the result doubles against the same baseline
- Awakening:
  - trigger awakening
  - verify temporary `+10` effective stat bonus during the buff window, then expiration
- Stat level rewards:
  - level stats through configured thresholds and confirm the mapped Tensura skills are learned
- Craft quality:
  - validate reduced durability loss, stronger food output, and stronger potion output on relevant Tensura interactions
- Summon scaling:
  - summon a supported entity at low stats and high stats
  - confirm health and damage increase with `ARCANE_POWER` and `WILLPOWER`

## Epic Fight

- Weight scaling:
  - equip heavier gear at low and high `PHYSICAL_ENDURANCE`
  - confirm weight burden is reduced at higher endurance
- Air attack bonus:
  - compare grounded hit vs aerial hit with the same weapon
  - confirm aerial damage/posture gain scales with `AGILITY`
- Stun resistance:
  - receive short, long, and knockdown stuns with low vs high `WILLPOWER`
  - confirm shorter stun duration at high values
- Execute threshold:
  - raise `INTIMIDATION`
  - bring a target under the expected HP threshold and confirm execute availability
- Perk reward bridge:
  - unlock mapped perk rewards and confirm the matching Epic Fight skills appear as learned

## ParCool

- Stamina scaling:
  - compare max stamina and recovery at low vs high `PHYSICAL_ENDURANCE`
- Jump scaling:
  - compare jump strength at low vs high `AGILITY`
- Granular XP:
  - perform `vault`, `walljump`, `dodge`, `roll`, `slide`, `wallrun`
  - confirm the expected stats gain XP for each action family
- Combo bonus:
  - chain 5 actions inside 3 seconds
  - confirm the boosted XP payout over the same actions performed slowly

## Mahou Tsukai

- Element detection:
  - cast or use representative fire, water/ice, wind, earth, and arcane scrolls
  - confirm the expected affinity stats gain XP
- Spell tier gate:
  - test basic, advanced, and ultimate/master spells below and above the configured `ARCANE_POWER` thresholds
  - confirm low-stat casts are blocked and valid casts succeed
- Earth damage:
  - use an earth-aligned spell recently, then deal damage
  - confirm bonus damage scales with `EARTH_AFFINITY`
- Reflection:
  - receive magical/indirect damage at low and high `WILLPOWER + MAGIC_RESISTANCE`
  - confirm partial reflection only occurs on the stronger setup

## Overgeared

- FORGING XP:
  - break Overgeared blocks/items and confirm `FORGING` XP is awarded
- Tool speed:
  - compare break speed on Overgeared tools at low and high `FORGING`
- Durability:
  - consume durability on Overgeared items at low and high `FORGING`
  - confirm reduced durability loss on the stronger setup
- Recipe gates:
  - try to craft `statmod:respec_stone` below and above its required `FORGING` level
  - try to craft `statmod:perk_tome` below and above its required `FORGING` level
- Loot modifier:
  - open relevant loot sources repeatedly and confirm the Overgeared bonus loot entries can appear
- Quality XP:
  - compare XP from lower-quality vs higher-quality forging outputs
  - confirm the expected multiplier trend from `POOR` up to `MASTER`

## Pass Criteria

- No `STAT Mod` crash, `FATAL`, `NoSuchMethodError`, or `NoClassDefFoundError`
- The behavior under test matches the intended plan item
- Any failure is recorded with:
  - exact scenario
  - affected mods
  - relevant command or item used
  - log snippet or screenshot reference

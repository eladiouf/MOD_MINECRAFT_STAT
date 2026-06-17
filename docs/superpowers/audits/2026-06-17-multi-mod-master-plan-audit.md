# STAT Mod Multi-Mod Master Plan Audit

**Date:** 2026-06-17
**Plan audited:** `.opencode/plans/2026-06-16-multi-mod-integration-master-plan.md`
**Workspace baseline:** includes additional user-added runtime mods detected on 2026-06-17

## Runtime baseline update

The current `runs/client/mods` set is no longer the same as the earlier validation baseline.
Notable newly-added or newly-dated jars observed during this audit include:

- `sword_soaring-21.14.2.5-mc1.21.1-neoforge.jar`
- `simplyswords-neoforge-1.63.0-1.21.1.jar`
- `epicfightcompat-1.1.0-mc1.21.1-neoforge.jar`
- `epic-fight-dawn-day-21.17.1.0-mc1.21.1-neoforge.jar`
- `kotlinforforge-5.11.0-all.jar`
- `SimplyTooltips-neoforge-0.1.3.jar`
- `invincible-21.15.8.1-mc1.21.1-neoforge.jar`

Two local source files also contain user-side edits and should be treated carefully:

- `src/main/java/tong/statmod/mixin/ItemKeywordReloadListenerMixin.java`
- `src/main/java/tong/statmod/stats/StatCommands.java`

## High-level audit status

### Phase 1 - Tensura Easy Wins

- `1.1 CASTING_SPEED + MANA_POOL race modifiers`: implemented in `RaceModifierRegistry`
- `1.2 Soul level XP multiplier`: implemented in `RaceEffectApplier.scaleXpAmount`
- `1.3 Intrinsic skills -> free perks`: implemented in `TensuraEventSubscriber`
- `1.4 Parallel Existence -> double XP`: implemented in `RaceEffectApplier.hasParallelExistence`
- `1.4a Learned Tensura skills are now consulted via SkillStorage in PlayerDataBridge.hasSkill`
- `1.5 MAGIC_RESISTANCE reduces magic damage`: implemented in `StatEffectApplier`

Status: implemented, partially unit-tested, runtime still needs in-game proof

### Phase 2 - Tensura Medium

- `2.1 EP multiplier from stats`: implemented in `TensuraEpHandler` + `TensuraXpMultiplier`
- `2.2 Evolution -> auto-respec`: implemented in `TensuraRaceHandler` + `PlayerStatData.removeUnlockedPerk`
- `2.3 Ultimate skills -> unlock all TRANSCENDENCE`: implemented in `TensuraEventSubscriber`
- `2.4 Awakening -> temporary buff`: implemented in `TempBuffManager`
- `2.5 TRANSCENDENCE perk -> Tensura skill`: implemented in `PerkToSkillMapper`, wired from `PerkManager`
- `2.6 MP/Magicule scaling`: implemented in `MagiculeScalingHandler`

Status: implemented, unit-tested at helper level, runtime still needs in-game proof

### Phase 3 - Tensura Hard

- `3.1 Stat level gates -> Tensura skills`: implemented in `StatLevelSkillRewards`
- `3.2 Artisan stats -> Tensura craft quality`: implemented in `TensuraCraftQualityHandler`
- `3.3 Summons scaling`: implemented in `SummonScalingHandler`

Status: implemented, helper-level tests present, runtime still needs in-game proof

### Phase 4 - Epic Fight

- `4.1 Armor weight reduction`: implemented in `EpicFightCompat.weightModifierAmount`
- `4.2 Air attack bonus`: implemented in `EpicFightCompat.airAttackMultiplier`
- `4.3 Stun resistance`: implemented in `EpicFightStunResistanceHandler`
- `4.4 Posture damage`: implemented in `EpicFightCompat.impactModifierAmount`
- `4.5 Execute threshold`: implemented in `EpicFightExecuteHandler`

Status: implemented and unit-tested, plus the later unified stamina bridge MVP is also present

### Phase 5 - ParCool

- `5.1 Stamina stats`: implemented in `ParcoolAttributeHandler`
- `5.2 Jump height`: implemented in `ParcoolAttributeHandler`
- `5.3 Granular XP`: implemented in `ParcoolCompat.xpRewardsForAction`
- `5.4 Combo bonus`: implemented in `ParcoolCompat` + `PerkState`

Status: implemented and unit-tested, runtime still needs in-game proof

### Phase 6 - Mahou Tsukai

- `6.1 Element detection`: implemented in `MahouElementMapper`
- `6.2 Spell tier gate`: implemented in `MahouSpellTier` + `MahouCompat`
- `6.3 Earth damage bonus`: implemented in `StatEffectApplier` + `MahouCompat`
- `6.4 Magic reflection`: implemented in `StatEffectApplier` + `MahouCompat`

Status: implemented and unit-tested, runtime still needs in-game proof

### Phase 7 - Overgeared

- `7.1 Block detection -> FORGING XP`: implemented in `OvergearedCompat`
- `7.2 Tool speed`: implemented in `OvergearedCompat` + `OvergearedForgingBonus`
- `7.3 Durability`: implemented in `ItemStackDurabilityMixin`
- `7.4 Crafting recipes`: implemented via JSON recipes
- `7.5 Loot`: implemented in `AddOvergearedLootModifier`
- `7.6 Quality XP bonus`: implemented in `OvergearedStatScaling` + `OvergearedForgingBonus`

Status: implemented and unit-tested, runtime still needs in-game proof

## Verification evidence gathered on 2026-06-17

- `.\gradlew.bat test`: PASS
- `.\gradlew.bat build`: PASS
- `.\gradlew.bat runClient`: `STAT Mod` integrations all initialize and the client reaches the in-game runtime loop until timeout:
  - `Tensura EP integration loaded`
  - `Tensura race integration loaded`
  - `Epic Fight integration loaded`
  - `Mahou Tsukai integration loaded`
  - `ParCool integration loaded`
  - `Overgeared integration loaded`
  - `STAT Mod initialized on NeoForge 1.21.1`
- targeted regression verification after the learned-skill lookup fix:
  - `.\gradlew.bat test --tests tong.statmod.integration.PlayerDataBridgeTest --tests tong.statmod.integration.RaceEffectApplierTest --tests tong.statmod.integration.TensuraIntrinsicPerkTest --tests tong.statmod.integration.tensura.TensuraEventSubscriberTest`: PASS
  - `.\gradlew.bat build`: PASS
  - `.\gradlew.bat runClient`: reaches timeout with no `FATAL`, `NoSuchMethodError`, or `NoClassDefFoundError`

## Current runtime state

The current `runClient` baseline is no longer blocked by the earlier `sword_soaring` / Epic Fight API mismatch.

What remains in `runs/client/logs/latest.log` is mostly third-party content noise and addon data issues, for example:

- `ClassNotFoundException: reascer.wom.skill.guard.DreadFullBusterSkill`
- many Epic Fight datapack animation errors such as `No constructor information has provided: epicfight_dd:...`
- subtitle / sound / skin warnings from addon mods

These warnings may still impact addon behavior, but they do not currently prevent the game from launching with `STAT Mod` active.

## Audit conclusion

The codebase currently appears to contain the planned integration units for all seven phases of the master plan, and the unit/build verification is green.

What is still missing for a full completion claim is stronger runtime proof across the current mod baseline, because:

- the user-added mod set changed after earlier verification work
- several plan items are only proven by helper-level tests, not by live gameplay validation
- third-party addon warnings remain in the runtime baseline and should be separated from true `STAT Mod` regressions during validation

## Recommended next step

Use the now-stable `runClient` baseline to re-run gameplay validation for:

- Tensura race / intrinsic / awakening / EP / summon flow
- Epic Fight combat flow
- ParCool action XP and stamina scaling
- Mahou casting gates and reflection
- Overgeared forging, speed, durability, and recipe gates

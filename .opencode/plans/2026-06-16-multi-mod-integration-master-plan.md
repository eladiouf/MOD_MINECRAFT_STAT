# STAT MOD — Plan d'Intégration Multi-Mods (75 Points)

**Date:** 2026-06-16
**Plateforme:** NeoForge 1.21.1, JDK 21, Mojang Mappings
**Package racine:** `tong.statmod`

---

## Architecture Générale

```
STAT Mod (NeoForge)
├── integration/tensura/          ← Hard dep (Tensura Reincarnated)
│   ├── PlayerDataBridge.java         ✅ Existant
│   ├── TensuraEventSubscriber.java   ✅ + 🔧 Étendre
│   ├── SoulLevelSyncHandler.java     ✅ Existant
│   ├── SkillPerkGate.java            ✅ + 🔧 CASTING_SPEED/MANA_POOL
│   ├── RaceModifierRegistry.java     ✅ + 🔧 Étendre
│   ├── RaceEffectApplier.java        ✅ + 🔧 Soul XP mult
│   ├── TensuraXpMultiplier.java      🆕
│   ├── PerkToSkillMapper.java        🆕
│   ├── MagiculeScalingHandler.java   🆕
│   ├── TempBuffManager.java          🆕
│   ├── TensuraCraftQualityHandler.java 🆕
│   └── SummonScalingHandler.java     🆕
│
├── integration/epicfight/        ← Optionnel (Epic Fight)
│   ├── EpicFightCompat.java          ✅ Existant
│   ├── EpicFightPerkGate.java        🆕
│   ├── EpicFightStunResistanceHandler.java 🆕
│   ├── EpicFightExecuteHandler.java  🆕
│   ├── EpicFightCooldownHandler.java 🆕
│   ├── EpicFightHyperArmorHandler.java 🆕
│   └── EpicFightWeaponReachHandler.java 🆕
│
├── integration/parcool/          ← Optionnel (ParCool)
│   ├── ParcoolCompat.java            ✅ + 🔧 Refactor XP granular
│   └── ParcoolAttributeHandler.java  🆕
│
├── integration/mahou/            ← Optionnel (Mahou Tsukai)
│   ├── MahouCompat.java              ✅ Existant
│   ├── MahouElementMapper.java       🆕
│   ├── MahouSpellTier.java           🆕
│   └── MahouPerkMap.java             🆕
│
├── integration/overgeared/       ← Optionnel (Overgeared)
│   ├── OvergearedCompat.java         🆕
│   ├── OvergearedRecipeGate.java     🆕
│   ├── OvergearedMaterialGate.java   🆕
│   └── OvergearedStatScaling.java    🆕
│
├── stats/
│   ├── StatType.java                 ✅ + 🔧 LIGHT/DARK affinities ?
│   └── StatEffectApplier.java        ✅ + 🔧 MAGIC_RESIST, reflection
│
├── perks/
│   └── PerkEffectHandler.java        ✅ + 🔧 Perk → skill
│
├── storage/
│   └── PlayerStatData.java           ✅ + 🔧 removeUnlockedPerk()
│
├── loot/
│   └── AddOvergearedLootModifier.java 🆕
│
└── mixin/ (Overgeared uniquement)
    ├── OvergearedSmithingMixin.java      🆕
    ├── OvergearedSmithingScreenMixin.java 🆕
    └── OvergearedAlloySmelterMixin.java  🆕
```

---

## PHASE 1 — Tensura Easy Wins (5 points)

### 1.1 CASTING_SPEED + MANA_POOL dans RaceModifierRegistry

**Fichier:** `RaceModifierRegistry.java`
**Tâche:** Ajouter des modifieurs de race pour CASTING_SPEED (13) et MANA_POOL (14) aux familles de races magiques.

Races concernées: Elf, Daemon, Vampire, Kijin/Mystic Oni, Slime, Dragon, Human Saint.

```java
// Dans chaque bloc de race magique
mod(13, 1, 1.0),   // CASTING_SPEED +1
mod(14, 1, 1.0),   // MANA_POOL +1
```

### 1.2 Soul Level → XP Multiplier Global

**Fichier:** `RaceEffectApplier.java` — méthode `addScaledXp()`

```java
double soulMult = 1.0 + (data.getSoulLevel() / 100.0);
int scaled = (int) Math.round(baseXp * getXpMultiplier(player, statIndex) * soulMult);
```

Effet: +1% d'XP par soul level (level 50 = 1.5x, level 100 = 2x).

### 1.3 Intrinsic Skills → Free Perks

**Mapping** (dans `SkillPerkGate.java`): 5-10 skills intrinsèques → perks gratuits.
**Handler** (dans `TensuraEventSubscriber.java`): Sur `NAMING_EVENT`, itérer les skills intrinsèques de la nouvelle race et auto-unlock les perks correspondants.

### 1.4 Parallel Existence → Double XP

**Fichier:** `PlayerDataBridge.java` (nouvelle méthode) + `RaceEffectApplier.java`.
**Logique:** Si le joueur a Parallel Existence, XP doublée.

### 1.5 MAGIC_RESISTANCE → Réduit les Dégâts Magiques

**Fichier:** `StatEffectApplier.java` — dans `onLivingDamage()` côté victime.

```java
int magicResist = getEffectiveLevel(victim, StatType.MAGIC_RESISTANCE.index);
if (magicResist > 0 && event.getSource().isIndirect()) {
    dmg *= 1.0f - Math.min(0.5f, magicResist * 0.005f);
}
```

---

## PHASE 2 — Tensura Medium (6 points)

### 2.1 Stats → Multiplicateur EP Tensura

**Nouveau fichier:** `TensuraXpMultiplier.java`
**Hook:** Réflexif sur `ExistenceStorage.addEP()` ou `ENERGY_DRAIN_EVENT`.
**Logique:** Multiplier gain d'EP par `1.0 + totalStatLevel * 0.005`.

### 2.2 Evolution → Auto-Respec

**Fichier:** `PlayerStatData.java` — ajouter `removeUnlockedPerk(int perkId)`.
**Fichier:** `RaceEffectApplier.java` — tick handler race change: revoke ancienne race, grant nouvelle.

### 2.3 Ultimate Skills → Unlock All TRANSCENDENCE

**Fichier:** `TensuraEventSubscriber.java` — sur `SKILL_LEARNING` d'un Ultimate Skill, unlock tous les perks TRANSCENDENCE.

### 2.4 Awakening → Buff Temporaire

**Nouveau fichier:** `TempBuffManager.java` (`Map<UUID, Long>`).
**Logique:** Sur `AWAKENING_EVENT`, +10 à toutes les stats pendant 30s.

### 2.5 Perk TRANSCENDENCE → Extra Skill Tensura

**Nouveau fichier:** `PerkToSkillMapper.java`
**Mapping:** 14 mappings (un par perk TRANSCENDENCE) vers des skills Tensura (Giant Strength, Ultra Instinct, Infinite Regeneration, etc.).

### 2.6 MP/Magicule Scaling via Stats

**Nouveau fichier:** `MagiculeScalingHandler.java`
**Logique:** Tick handler (40 ticks). ARCANE_POWER + MANA_POOL → maxMagicule. Appliquer via `ExistenceStorage.setMagicule()`.

---

## PHASE 3 — Tensura Hard (3 points)

### 3.1 Stat Level Gates → Skills Tensura

**Nouveau fichier:** `StatLevelSkillRewards.java`
**Mapping:** `(statIndex, levelThreshold) -> ManasSkill`
**Integration:** Hook dans `addXp()` après level-up.

### 3.2 Artisan Stats → Qualité Items Tensura

**Nouveau fichier:** `TensuraCraftQualityHandler.java`
**Logique:** FORGING → réduction perte durabilité. COOKING → saturation. ALCHEMY → durée potion.

### 3.3 Summons Scaling

**Nouveau fichier:** `SummonScalingHandler.java`
**Hook:** `ISummoning.onPostSummon()` (Mixin probable).
**Logique:** ARCANE_POWER → santé, WILLPOWER → dégâts.

---

## PHASE 4 — EpicFight (5 points)

### 4.1 Armor Weight Reduction
`EpicFightCompat.java` — PHYSICAL_ENDURANCE → Weight modifié: `base * (1 - endurance * 0.02)`.

### 4.2 Air Attack Bonus
`EpicFightCompat.java` — Si en l'air, `impact *= 1 + agility * 0.005`.

### 4.3 Stun Resistance
`EpicFightStunResistanceHandler.java` — WILLPOWER réduit stun time: `time * (1 - will * 0.01)`.

### 4.4 Posture Damage
`EpicFightCompat.java` — BRUTE_FORCE → `baseImpact *= 1 + bruteForce * 0.02`.

### 4.5 Execute Threshold
`EpicFightExecuteHandler.java` — INTIMIDATION → si HP < `0.1 + intimi * 0.002`, set execute.

---

## PHASE 5 — ParCool (4 points)

### 5.1 Stamina Stats
`StatAttributeHandler.java` — ENDURANCE → MAX_STAMINA (+1%/lvl), STAMINA_RECOVERY (+0.5%/lvl).

### 5.2 Jump Height
`StatAttributeHandler.java` — AGILITY → JUMP_STRENGTH (+0.2%/lvl).

### 5.3 Granular XP
`ParcoolCompat.java` — Vault→AGIL, WallJump→AGIL+BRUTE, Dodge→AGIL+RAPID, etc.

### 5.4 Combo Bonus
`ParcoolCompat.java` + `PerkState.java` — Si 5 actions en <3s, +50% XP.

---

## PHASE 6 — Mahou Tsukai (4 points)

### 6.1 Element Detection
`MahouElementMapper.java` — Fire→FIRE_AFF, Water→WATER_AFF, Wind→AIR_AFF, etc.

### 6.2 Spell Tier Gate
`MahouSpellTier.java` — Patterns de noms → ARCANE_POWER requis. Cancel si < requis.

### 6.3 Earth Damage Bonus
`StatEffectApplier.java` — Si scroll terre récent, `dmg *= 1 + EARTH_AFF * 0.01`.

### 6.4 Magic Reflection
`StatEffectApplier.java` — WILLPOWER+MAGIC_RESIST → `reflect% = min(30%, will*0.2% + resist*0.3%)`.

---

## PHASE 7 — Overgeared (6 points)

### 7.1 Block Detection → FORGING XP
`OvergearedCompat.java` — `BlockEvent.BreakEvent` si namespace `overgeared`.

### 7.2 Tool Speed
`OvergearedCompat.java` — FORGING level → bonus dig speed sur outils Overgeared.

### 7.3 Durability
`OvergearedCompat.java` — `ItemDamageEvent`: `dmg *= 1 - 0.1 * FORGING_level`.

### 7.4 Crafting Recipes
2 JSON files: `perk_tome_forging.json`, `respec_stone_forging.json`.

### 7.5 Loot
`AddOvergearedLootModifier.java` — PerkTome/RespecStone dans coffres Overgeared.

### 7.6 Quality XP Bonus
`OvergearedCompat.java` — XP * `1 + quality * 0.2` (POOR→MASTER).

---

## Fichiers Récapitulatifs

### Nouveaux (22)
- `TensuraXpMultiplier.java`, `PerkToSkillMapper.java`, `MagiculeScalingHandler.java`, `TempBuffManager.java`, `TensuraCraftQualityHandler.java`, `SummonScalingHandler.java`, `StatLevelSkillRewards.java`
- `EpicFightStunResistanceHandler.java`, `EpicFightExecuteHandler.java`, `EpicFightCooldownHandler.java`, `EpicFightHyperArmorHandler.java`, `EpicFightWeaponReachHandler.java`
- `ParcoolAttributeHandler.java`
- `MahouElementMapper.java`, `MahouSpellTier.java`, `MahouPerkMap.java`
- `OvergearedCompat.java`, `OvergearedRecipeGate.java`, `OvergearedMaterialGate.java`, `OvergearedStatScaling.java`, `OvergearedForgingBonus.java`
- `AddOvergearedLootModifier.java`

### Modifiés (15+)
- `RaceModifierRegistry.java`, `RaceEffectApplier.java`, `SkillPerkGate.java`, `TensuraEventSubscriber.java`, `PlayerDataBridge.java`, `PlayerStatData.java`, `StatEffectApplier.java`, `StatAttributeHandler.java`, `PerkManager.java`, `PerkEffectHandler.java`, `PerkState.java`, `CombatXPHandler.java`, `NonCombatXPHandler.java`, `EpicFightCompat.java`, `ParcoolCompat.java`, `MahouCompat.java`

---

## Ordre Recommandé

```
Phase 1: Tensura Easy    (5pts, ~30min)
Phase 2: Tensura Medium  (6pts, ~2h)
Phase 4: EpicFight Easy  (5pts, ~1h)
Phase 5: ParCool Easy    (4pts, ~45min)
Phase 6: Mahou Easy      (4pts, ~1h)
Phase 7: Overgeared      (6pts, ~2h)
Phase 3: Tensura Hard    (3pts, ~3h)
```

**Total estimé:** ~22h de développement.

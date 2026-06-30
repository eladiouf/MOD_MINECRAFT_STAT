---
type: decision
id: STAT-DEC-007
date: 2026-06-22
area: quality
status: Active
related_projects:
  - STAT Mod
related_meeting: STAT-MTG-002
supersedes: []
---

# STAT-DEC-007 — Priorité test vs fix pour XP handlers

## Context

L'audit STAT-MTG-002 a révélé que 4 handlers critiques (CombatXPHandler, NonCombatXPHandler, LevelUpHandler, PerkEffectHandler) ont zéro test unitaire, tandis que des bugs CRITICAL (True Strike — corruption de save) et HIGH (5 tooltip mismatches) existent.

## Decision

Les bugs CRITICAL/HIGH sont hotfixés immédiatement (M12) sans attendre les tests. Les handlers P0 recoivent des tests de contrat avant toute modification fonctionnelle (M11).

## Rationale

- True Strike corrompt les attributs des entités de manière permanente — c'est un data corruption bug
- Bloquer les hotfixes derrière des tests retarderait la stabilisation du mod
- Les tests sont écrits en parallèle (M11) pour couvrir les handlers avant le wiring config (M13)

## Consequences

- M11 et M12 peuvent courir en parallèle
- M13 (wiring config) est bloquée sur M11

---

# STAT-DEC-008 — Wiring des 4 configs mortes

## Context

`Config.java` définit `combatXpMultiplier`, `nonCombatXpMultiplier`, `maxStatLevel`, `basePerkPointsPerLevel` mais aucun n'est lu au runtime.

## Decision

Les 4 valeurs sont connectées au runtime dans une mission dédiée (M13), précédée par les tests de contrat (M11). `ProgressionRouting.java` supprimé simultanément.

## Rationale

- Les admins doivent pouvoir tuner la progression
- La config sans effet est trompeuse
- Les tests M11 assurent la non-régression

---

# STAT-DEC-009 — Perk tooltip mismatches + True Strike fix

## Context

6 bugs identifiés dans PerkEffectHandler : True Strike (CRITICAL), Dread Lord, Feared, Focused Mind, Absolute Dominion (tooltip mismatches), WILL_TRANSCENDENCE (remove buffs).

## Decision

Hotfix immédiat (M12) :

1. **True Strike** — appliquer `armor.setBaseValue(originalValue)` après l'hit, ne pas muter l'attribut de manière permanente
2. **Dread Lord + Feared** — remplacer SLOWNESS + WEAKNESS par `MobEffect.MOB_FLEEING` (ou équivalent)
3. **Focused Mind** — remplacer `DAMAGE_RESISTANCE` par `KNOCKBACK_RESISTANCE`
4. **Absolute Dominion** — retooltip pour refléter le comportement réel (clear aggro + regen) OU implémenter le contrôle si faisable
5. **WILL_TRANSCENDENCE** — exclure les effets bénéfiques du remove tick

---

# STAT-DEC-010 — Global level formula

## Context

`globalLevel = sum(allStatLevels) / STAT_COUNT(23)`. Les joueurs non-magic ne peuvent pas atteindre les milestones car 9 stats magiques sont lockées (Phase 1 — seule Fire active).

## Decision

Le dénominateur `STAT_COUNT` s'ajuste dynamiquement : les stats magiques lockées (level = 0, MagicRace non défini) sont exclues du calcul. For each locked magic stat, decrement the divisor.

---

# STAT-DEC-011 — Sources XP non-combat pour stats magiques

## Context

9 stats magiques (Arcane Power, 4 affinités, Casting Speed, Mana Pool, Erudition, Magic Resistance) ont 0 source de progression non-combat.

## Decision

Mission dédiée (M4.5) via event bridges Iron's Spellbooks :

| Source | Stat |
|--------|------|
| Craft spell | ARCANE POWER |
| Cast Fire spell | FIRE_AFFINITY |
| Discover scroll | ERUDITION |
| Spend mana | MANA_POOL |
| Spell level up | CASTING_SPEED |
| Resist magic damage | MAGIC_RESISTANCE |

---

# STAT-DEC-012 — Kodachi → RAPIDITE

## Context

`WeaponResolver.java` pattern `"odachi"` (BRUTE_FORCE) précède `"kodachi"` (RAPIDITE). `"kodachi".contains("odachi")` = true → misclassification.

## Decision

Réordonner les patterns : checker `kodachi` avant `odachi`. Dans hotfix M12.

---

# STAT-DEC-013 — Suppression code mort

## Context

`ProgressionRouting.java`, `ActionType.java` (zero callers). `Config.register()` (vide). Tests associés à supprimer.

## Decision

Supprimer dans M13 (nettoyage). Le test `ProgressionRoutingTest` est supprimé avec la classe.

---

# STAT-DEC-014 — Fuites mémoire static maps

## Context

3 static `ConcurrentHashMap`/`HashMap` ne sont jamais nettoyées : `StatAttributeHandler.lastRapidite/lastAgility`, `LevelUpHandler.lastGrantedTier`, `PerkState` (cooldowns, combo, dodge, tracked targets, frenzy stacks).

## Decision

Ajouter cleanup sur `ServerPlayerConnectionUpdate` pour chaque map. Dans hotfix M12.

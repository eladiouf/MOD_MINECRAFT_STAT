# STAT-DEC-MAGICULE-MANA-RATIO — Conversion magicule Tensura → mana Iron's

**Date :** 2026-06-22
**Mission :** M4 follow-up A
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Décision

Ratio initial : `MAGICULE_TO_MANA_RATIO = 1.0` (identité).

Pour chaque wrapper Tensura, `getManaCost(int spellLevel)` retourne :
```
max(1, ceil(baselineMagiculeCost × 1.0))
```

si `baselineMagiculeCost > 0`, sinon retombe sur la baseline par discipline héritée de
`STAT-DEC-M4-REVERSE` (30 à 100 par tier de discipline).

## Méthode de query

Diagnostic bytecode des `Magic` subclasses :

- **~85% des classes** : `getMagiculeCost(entity, instance, mode)` est `getstatic
  CONFIG.magiculeCost / dreturn` → ignore tous les args, null-safe.
- **~15% (barriers)** : utilisent `EnergyHelper.getBaseMaxMagicule(entity)` → NPE sur
  entity null.

Implémentation `TensuraSpellMetadata.invokeMagiculeCostGetter` :
1. Lookup la méthode via reflection sur le singleton `ManasSkill`.
2. Invoke avec `entity = null`, `instance = skill.createDefaultInstance()`, `mode = 0`.
3. Si Throwable (NPE des barriers, ou autre) → return `-1.0`.

Le wrapper consulte `forSkill(skillId).baselineMagiculeCost()`. Valeur `-1.0` signifie
"non résoluable" → fallback discipline. Valeur ≥ 0 signifie "valeur fiable" → conversion.

## Pourquoi ratio 1.0 ?

Pas de mesure in-game disponible au moment de la décision. Choix conservateur :
- Si trop bas en jeu (sorts cassent les barres Iron's) : monter à 1.5 ou 2.0
- Si trop haut (sorts wrappés moins chers que les natifs Iron's) : descendre à 0.5

Constante exposée comme `MAGICULE_TO_MANA_RATIO` dans `TensuraDelegatingSpell` — une seule
ligne à éditer. Décision révisable par mission séparée après mesure.

## Limites assumées

- `getManaCost` est appelé sans contexte joueur → la valeur reflète UN sort par UN mode
  (mode=0) hors influence des stats/race du caster. Acceptable car Iron's compense via
  `getSpellPower(level, entity)` côté dégâts.
- Pour les sorts qui scalaient leur coût sur le joueur (barriers), on conserve la baseline
  discipline → la fidélité est dégradée mais pas régressive vs `STAT-DEC-M4-REVERSE`.

## Exit conditions

Constante migrée vers un config NeoForge (`MagicBridgeConfig.magiculeToManaRatio`) quand le
mod aura un système de config TOML, OU décision de fondateur de remplacer la conversion par
un mapping table-driven par skillId.

# STAT-DEC-MASTERY-MAPPING — Mastery Tensura → spell level Iron's

**Date :** 2026-06-22
**Mission :** M4 follow-up B
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Décision

Mastery Tensura (0 à `getMaxMastery()`) mappée linéairement sur le niveau de sort affiché
par Iron's (1 à `MAX_LEVEL = 5`).

Formule :
```
ratio = clamp(mastery / max(1, maxMastery), 0.0, 1.0)
level = max(1, floor(1 + ratio × (MAX_LEVEL - 1)))
```

| Mastery | Ratio | Level Iron's affiché |
|---|---|---|
| 0       | 0.00  | 1 |
| 25%     | 0.25  | 2 |
| 50%     | 0.50  | 3 |
| 75%     | 0.75  | 4 |
| 100%    | 1.00  | 5 |

## Mécanisme

`AbstractSpell.getLevelFor(int, LivingEntity)` est `final` dans Iron's, mais lance un
`ModifySpellLevelEvent` que NeoForge propage sur l'event bus. `TensuraSpellLevelModifier`
subscriber écoute :

```java
@SubscribeEvent
public static void onModifySpellLevel(ModifySpellLevelEvent event) {
    if (event.getSpell() instanceof TensuraDelegatingSpell wrapper) {
        int level = computeLevelFromMastery(wrapper, event.getEntity());
        if (level > 0) event.setLevel(level);
    }
}
```

Évite le pattern Mixin déjà utilisé pour l'icône (mission conformance) — préfère l'event bus
quand l'API le permet, conformément aux conventions NeoForge documentées dans `CLAUDE.md`
§Règles de Code.

## Hors scope volontaire

Le **scaling effectif des dégâts** reste piloté par Tensura natif côté serveur dans
`Magic.onRelease` qui lit `instance.getMastery()` du joueur. Iron's level reste **purement
informatif et UX** :

- Affiché dans le grimoire / spellbook UI : "Lv 3" sur Fire Bolt
- Affiché dans la tooltip d'inscription
- Lu par les sorts vanilla Iron's qui scalent avec le level — non applicable ici car nos
  wrappers délèguent à Tensura

On ne duplique **pas** la logique de scaling. Tensura est l'autorité du dégât ; Iron's est
l'autorité de l'UX.

## Pourquoi level 5 max ?

- Sweet spot psychologique (Lv 1/5 → Lv 5/5 lisible)
- Aligne sur la convention de plusieurs sorts vanilla Iron's qui ont `getMaxLevel() == 10`
  mais affichent leur niveau efficace dans cette tranche
- Révisable trivialement via la constante `TensuraDelegatingSpell.MAX_LEVEL`

## Exit conditions

- Migrer le mapping vers une table par discipline si la linéarité s'avère mal calibrée
  (ex : control / pressure méritent peut-être un palier différent)
- Ajouter un override config TOML quand le mod aura un système de config
- Si Iron's expose une API native pour bind un niveau à une stat externe, migrer dessus et
  retirer le subscriber

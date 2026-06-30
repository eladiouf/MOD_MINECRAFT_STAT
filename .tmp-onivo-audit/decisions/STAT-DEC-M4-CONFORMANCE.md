# STAT-DEC-M4-CONFORMANCE — Conformance Tensura du reverse bridge

**Date :** 2026-06-22
**Mission :** M4 (extension de `STAT-DEC-M4-REVERSE`)
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Contexte

`STAT-DEC-M4-REVERSE` (2026-06-21) a livré 56 wrappers `TensuraDelegatingSpell` mais avec
un design "porte minimale" :
- `CastType` fixe à `INSTANT` pour tous les sorts
- Cast time non querysé sur Tensura (compression press+release en 1 frame)
- Mana cost / cooldown hardcodés par discipline générique
- Icône placeholder unique pour les 56 wrappers
- Cercle magique Tensura jamais rendu

Test utilisateur 2026-06-22 : "les sorts ne sont pas bien faits — il y a des choses qui
manquent, les cercles magiques, les projectiles, le fait de devoir hold certains sorts."

## Décision

Étendre les wrappers à un design **runtime-conformant** qui interroge les valeurs natives
Tensura plutôt que d'inventer des défauts.

### Changements actés

1. **Cast type dynamique** — `getCastType()` lit `Magic.getDefaultCastTime()` via reflection
   sur le singleton `ManasSkill`. Retour `CastType.LONG` si > 1 tick, sinon `INSTANT`.
2. **Cast time réel** — `getCastTime(int)` retourne la même valeur. Iron's affiche donc la
   barre de cast correcte par sort.
3. **Cycle press → hold (per tick) → release** — pour les `LONG` :
   - `onServerPreCast` → `instance.onPressed(caster, 0, 0)`
   - `onServerCastTick` → `instance.onHeld(caster, heldTicks, 0)` chaque tick
   - `onCast` → `instance.onRelease(caster, totalCastTime, 0, 0)`
4. **Cercle magique natif** — `Magic.onHeld` appelle `applyCastingVisual` qui spawn une
   entité `MagicCircle` (bytecode confirmé). L'entité est server-side avec sync auto vers
   les clients. Pas de packet custom requis.
5. **Icônes natives Tensura** — Mixin `IronSpellIconResolverMixin` détourne
   `AbstractSpell.getSpellIconResource()` (marqué `final` en Java, accessible via injection
   HEAD cancellable). Renvoie `ManasSkill.getSkillIcon()` pour les instances `TensuraDelegatingSpell`.
6. **Indicateur "déjà inscrit"** dans le menu d'inscription — bordure verte + point + ligne
   tooltip "Déjà inscrit dans ce grimoire". Récupéré via `ISpellContainer.get(spellBookStack)`
   du slot grimoire actif.

### Architecture

| Composant | Rôle |
|---|---|
| `TensuraSpellMetadata` | Cache lazy keyed par `skillId`. Query Tensura via reflection sur le registre `SkillAPI.getSkillRegistry()`. Hold : `defaultCastTimeTicks`, `tensuraIconLocation`. |
| `TensuraDelegatingSpell` | Wrapper Iron's qui consulte le cache au premier `getCastType()` / `getCastTime()`. Bridge press/hold/release via `withSkillInstance(caster, action)`. |
| `IronSpellIconResolverMixin` | Détour le getter d'icône d'Iron's pour les wrappers seulement. |
| `KnownSpellIconButton` | Nouveau champ `bound` + effet visuel + tooltip enrichie. |
| `IronInscriptionTableScreenMixin` | Énumère les sorts inscrits via `ISpellContainer.getSpellAtIndex(i)`. |

### Régression bonus corrigée

`PuffishSkillsCompat.runGuarded(player, action)` exposé + `PuffishMagicTreeBuilder.applyMirror`
désormais wrappé dans le guard. Évite la récursion infinie lock→listener→sync→lock observée
lors de l'ouverture du tree (plante client confirmé dans `runs/client/logs/latest.log` à
20:07).

## Points hors scope (suivi dans le plan M4-FOLLOWUPS)

- Mana cost réel par sort (limite Iron's : `getManaCost(int)` sans contexte joueur)
- Mastery Tensura → spell level Iron's
- Polish UX (filtre / recherche / scrollbar)
- Phase 2 magic branches
- Bug latent `KnownSpellUiState` qui n'inclut pas le set de sorts inscrits dans sa clé de
  cache → refresh manqué si l'utilisateur inscrit sans bouger autre chose

## Risques

1. **Magicule cost natif Tensura** — `EnergyHelper.isOutOfEnergy` peut bloquer `onRelease`
   silencieusement si le joueur n'a pas de magicule. Décision fondateur "Iron's mana only"
   doit être réaffirmée si le test runtime le montre. Solutions de repli :
   - Bypass via reflection sur `EnergyHelper` (impose une dépendance fragile)
   - Auto-grant magicule à la pression du grimoire (modifie les attachments Tensura)
   - Accepter la dépendance magicule comme contrainte cohérente (statu quo)
2. **Sorts Tensura non-`Magic`** — `TensuraSpellMetadata.query` tolère l'absence de
   `getDefaultCastTime()` (catch `NoSuchMethodException` → cast time 0 → INSTANT). Pas de
   régression pour les skills non-Magic enveloppés par erreur.
3. **Drift API Iron's** — le Mixin injecte sur `getSpellIconResource` par nom de méthode. Si
   Iron's rename ce getter dans une future version, le Mixin échoue silencieusement (le
   wrapper retombe sur le PNG auto-dérivé, donc placeholder magenta).

## Exit conditions

Les wrappers conformance restent en place tant que :
- L'API Iron's `AbstractSpell` expose `getCastType` / `getCastTime` / `onServerCastTick` /
  `getSpellIconResource`, ET
- L'API ManasCore `ManasSkillInstance` expose `onPressed` / `onHeld` / `onRelease`, ET
- Tensura's `Magic.onHeld` continue d'appeler `applyCastingVisual` qui spawn `MagicCircle`.

Si Iron's expose un jour une API d'extension de sort permettant cast type variable + icône
custom sans reflection ni Mixin, migrer dessus et retirer le Mixin.

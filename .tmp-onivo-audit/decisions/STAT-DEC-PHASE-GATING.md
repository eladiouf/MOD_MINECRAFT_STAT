# STAT-DEC-PHASE-GATING — Aligner doc sur état effectif (catalog complet)

**Date :** 2026-06-23
**Mission :** I
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Contexte

`CLAUDE.md` pré-Mission-I déclarait :

> "Phase 1 Magic : Iron's Spellbooks unified magic tree (commun + Fire actif, **autres écoles
> structurellement présentes mais verrouillées**)"

Mais l'inspection du `MagicTreeCatalog` montre :
- 249 nœuds enregistrés répartis sur **les 9 écoles** (COMMON + 8 schools)
- Aucun nœud Water/Air/Earth/Holy/Blood/Ender/Evocation/Eldritch n'utilise
  `MagicTreeCatalog.LOCKED_SENTINEL` en prerequisite
- Économie unifiée + 3 gates de stats (Mission γ) s'appliquent à toutes les écoles

**Conséquence** : un joueur Tensura race compatible peut débloquer n'importe quelle école
dès maintenant via les commandes admin ou la progression naturelle. Le "Fire only" de la
doc est un fantôme historique.

## Décision

**Option B** : aligner la doc sur la réalité du code. Pas de re-verrouillage.

`CLAUDE.md` mis à jour :
- Étape 3 (économie unifiée) marquée ✅ done
- Étape 4 (Phase 1 Magic) reformulée : "les 9 écoles sont structurellement actives, focus
  tuning Fire"
- Étape 5 (Phase 2 Magic) reformulée : "tuning fin par école + ParCool / Overgeared
  intégrations magiques" (plus de "ouvrir les autres branches" puisqu'elles sont déjà ouvertes)

## Pourquoi pas l'option A (re-verrouiller)

- Régression UX flagrante : 245 nœuds disparaîtraient du tree
- Aucune valeur ajoutée gameplay vs ce qui existe maintenant
- Force un travail de tuning artificiel école par école avant déblocage
- Le tuning peut se faire en parallèle de la disponibilité — pas besoin d'un gate binaire

## Pourquoi pas l'option C (mécanisme progressif)

- Trigger global (level / sorts appris / time spent) ajoute de la complexité de progression
  sans rationale narratif clair
- Si le besoin émerge plus tard (ex : interdire les écoles `ELDRITCH` avant un milestone),
  on pourra l'ajouter ponctuellement via `LOCKED_SENTINEL` sur les nœuds concernés

## Conséquences immédiates

- Plus de "Phase 2 = ouvrir les écoles" — c'est déjà fait
- "Phase 2 = polish" : tuning des coûts en magic points, équilibrage des gates de stats,
  cohérence des effets de sorts d'écoles non-Fire (qui peuvent avoir des bugs cachés faute
  de tests in-game)
- "Phase 3" implicite : intégration ParCool / Overgeared sur les sorts (cooldowns liés à
  stamina, qualité forge → bonus damage spell), à scoper plus tard

## Exit conditions

Re-verrouillage si :
- Le fondateur décide que certaines écoles (Eldritch, Blood) doivent être lategame
  uniquement → ajouter `LOCKED_SENTINEL` sur les openers concernés + trigger d'unlock
- Découverte d'un sort cassé qui crash le client / serveur → désactivation ponctuelle
  en attendant fix

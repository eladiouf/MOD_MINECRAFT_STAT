# `.opencode/plans/` — DÉPRÉCIÉ

> Statut : **deprecated** — décision `STAT-DEC-005` du 2026-06-21
> Référence : `.tmp-onivo-audit/vault/08 Decisions/2026-06-21 - STAT Mod Plan Directory Taxonomy.md`

## Règle

Le répertoire canonique pour les plans d'implémentation et les specs design est désormais :

- **Plans** → `docs/superpowers/plans/`
- **Specs** → `docs/superpowers/specs/`

## Ce qui reste autorisé ici

- Lecture des fichiers existants comme **contexte historique**
- Référencement par les nouvelles specs ou plans (en lien)

## Ce qui n'est plus autorisé

- ❌ Création de **nouveaux** fichiers dans ce dossier
- ❌ Modification active des plans existants pour faire avancer du travail nouveau

## Migration

Si un plan historique de ce dossier devient la base d'un travail actif, il doit être **forké** dans `docs/superpowers/plans/` avec un front-matter :

```yaml
---
migrated-from: .opencode/plans/<filename>.md
migrated-on: <YYYY-MM-DD>
---
```

## Fichiers conservés en archive

- `2026-06-16-multi-mod-integration-master-plan.md` — plan-cadre 75 points, référence intégrations multi-mods
- `armes-epicfight-keyword-json.md`
- `stat-integration-analysis.md`
- `weapon-classification-fix-plan.md`

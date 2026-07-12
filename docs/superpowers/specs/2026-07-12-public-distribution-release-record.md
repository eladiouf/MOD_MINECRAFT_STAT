# STAT-DEC-PUBLIC-DISTRIBUTION — Distribution publique Modrinth + CurseForge

- **Date** : 2026-07-12
- **Décideur** : Onivo Studio (session gouvernée, opérateur eladiouf)
- **Statut** : appliqué — v1.2.0-beta publiée sur les deux plateformes
- **Références** : `release-governance.md`, `playbook-release-flow.md`, `store-surface-checklist.md` (Onivo OS)

## Décision

STAT Mod entre en **distribution publique bêta** sur les deux surfaces de distribution
standard de l'écosystème Minecraft moddé :

| Surface | Identifiants | Page |
|---|---|---|
| **Modrinth** | slug `statmod`, id `s6kZZpsn`, compte `eladiouf` | modrinth.com/mod/statmod |
| **CurseForge** | projet `1583282`, slug `stat-mod-rpg` | curseforge.com/minecraft/mc-mods/stat-mod-rpg |

Canal : **beta** (le donjon est en équilibrage actif). Passage en `release` = décision future.

## Conformité au Release Flow (playbook)

| Étape | État | Preuve |
|---|---|---|
| 1. Scope d'implémentation clos | ✅ | v1.2.0 committée/poussée (`4c0a7df`…`d4b9491`), branche `neoforge-1.21.1` |
| 2. Gates de vérification | ✅ | `gradlew check` vert (compilation + suite JUnit complète) avant chaque publication ; IA des mages validée en jeu (casts observés dans les logs de session du 2026-07-12) |
| 3. Changelog + métadonnées | ✅ | `CHANGELOG.md` §1.2.0-public ; `neoforge.mods.toml` complet (licence MIT, logoFile) |
| 4. Assets de release | ✅ | jar `statmod-1.2.0.jar` avec logo embarqué ; icône projet 512px (<256 Ko) ; description `MODRINTH_DESCRIPTION.md` |
| 5. Release Readiness Review | ✅ | audit dépendances : mod vérifié **standalone** (toutes intégrations soft-gated `ModList.isLoaded` + mixin plugin conditionnel) — la mention « Tensura hard dependency » du CLAUDE.md est une intention design, pas une contrainte runtime |
| 6. Publication gouvernée | ✅ | publication déclenchée par l'opérateur (tokens fournis explicitement) — l'autorité finale de publish est restée gated conformément à `release-governance.md` |
| 7. Post-release checks | 🟡 | en attente : modération Modrinth (statut `processing`) + approbation fichier CurseForge |

## Pipeline de release (reproductible)

```bash
# 1. bump mod_version dans gradle.properties
# 2. mettre à jour CHANGELOG.md (repris comme notes de version par les 2 plateformes)
# 3. publier :
MODRINTH_TOKEN=... CURSEFORGE_TOKEN=... gradlew build modrinth publishCurseForge
# description de la page Modrinth (source: MODRINTH_DESCRIPTION.md) :
gradlew modrinthSyncBody
```

- **Tokens** (hors repo, profil utilisateur) : `~\.modrinth_token`, `~\.curseforge_token`.
  Incident mineur : un premier token Modrinth a transité en clair → révoqué et remplacé
  immédiatement (politique : tout secret exposé est brûlé).
- **Outils** : Minotaur 2.x (Modrinth), CurseForgeGradle 1.1.26 (CurseForge), config dans `build.gradle`.

## Dépendances déclarées (Modrinth, toutes optionnelles)

Slugs vérifiés contre l'API : `irons-spells-n-spellbooks`, `skills` (Pufferfish's Skills),
`epic-fight`, `waystones`, `lootr`, `l2hostility`, `parcool`, `playerrevive`.
**Absents de Modrinth** (CurseForge only, mentionnés dans la description) : Tensura
Reincarnated, FTB Teams, Overgeared, SLU.

## Reste à faire (backlog release)

1. Galeries des deux pages (captures d'écran — opérateur)
2. Relations CurseForge via le site (slugs CF ≠ slugs Modrinth)
3. Vérifier description/icône CurseForge sur la page (créée manuellement)
4. Post-release checks à la levée des modérations
5. Décision future : passage beta → release

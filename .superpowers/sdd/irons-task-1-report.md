# Rapport Task 1 — catalogue Iron's Spells

## Statut

Implémentation terminée sur la base `eb7b808` avec le message de commit demandé `docs: catalog Iron's Spells Forge addons`.

## Livrables

- `external-mods/irons-spells-forge-1.20.1/catalog.csv` : 74 projets Forge 1.20.1, schéma exact à neuf colonnes, URL officielles HTTPS et fichiers JAR.
- `external-mods/irons-spells-forge-1.20.1/README.md` : catégories, règle de déduplication, comportement des URL CurseForge et avertissement de compatibilité.

## Construction du catalogue

Les 63 entrées Forge 1.20.1 valides du rapport Modrinth ont servi de base. Les projets uniquement présents dans les deux rapports CurseForge ont ensuite été ajoutés. Les versions CurseForge plus récentes de Dark Doppelganger (9.8.3), IronsArms (3.0.6) et Wind's Spellbooks (1.0.2) remplacent leurs anciennes sélections Modrinth. Pour un projet/version identique vérifié sur les deux plateformes, l'URL CDN Modrinth a été conservée. Mobbility Core a été inclus comme dépendance JAR explicitement relevée.

Les métadonnées communes ont été enrichies avec les dépendances CurseForge connues. Lorsqu'un rapport signalait une incertitude de branche, de maturité ou de métadonnées, `needs-testing` a été retenu de façon conservatrice. Aucun resource pack, datapack ou projet sans JAR Forge 1.20.1 n'a été ajouté.

## TDD adapté aux données

### RED

Le contrôle minimal a été exécuté avant création. Il a échoué avec le message attendu indiquant que `catalog.csv` était absent.

### GREEN

Le script PowerShell prescrit dans le brief a été exécuté après création et a retourné `74` sans autre erreur. Des contrôles supplémentaires ont confirmé l'en-tête exact, l'ensemble fermé des classifications et la sélection Dark Doppelganger 9.8.3. `git diff --check` ne remonte aucune erreur sur les livrables.

## Auto-revue

- Tous les champs `loader` valent `forge` et tous les champs `minecraft` valent `1.20.1`.
- Toutes les URL commencent par `https://` et tous les noms de fichiers finissent par `.jar`.
- Aucun doublon `project,version` et aucune entrée exclue n'ont été détectés.
- Les trois rapports sources ont été confrontés afin de ne pas limiter le catalogue à la liste minimale.
- Les changements préexistants hors périmètre n'ont pas été modifiés ni inclus intentionnellement.

## Préoccupations

Les routes CurseForge `/download/<fileId>` sont les endpoints officiels fournis par les rapports et peuvent nécessiter le suivi d'une redirection. La compatibilité runtime des entrées, en particulier celles marquées `needs-testing`, reste à valider dans l'instance du modpack.

# Catalogue Iron's Spells — Forge 1.20.1

`catalog.csv` recense les JAR officiels vérifiés pour Minecraft 1.20.1 avec Forge. Une ligne décrit le projet, la version retenue, le fichier, ses dépendances connues et son état d'intégration.

## Classifications

- `active` : version publiée jugée exploitable dans le lot actif, sous réserve du test global du modpack.
- `needs-testing` : addon incertain, jeune, en alpha/bêta, aux métadonnées incomplètes ou sensible à la version d'Iron's Spells. Il doit être testé isolément avant activation.
- `dependencies` : socle, bibliothèque, API ou intégration dont les dépendances directes et transitives doivent être résolues.

## Sources et déduplication

Le catalogue combine les relevés officiels Modrinth et CurseForge. Lorsqu'un même projet et une même version existent sur les deux plateformes, l'URL directe Modrinth est préférée si elle a été vérifiée. Une version plus récente vérifiée remplace l'ancienne sélection : Dark Doppelganger utilise ainsi la version CurseForge 9.8.3. Les fichiers CurseForge utilisent les URL directes officielles `mediafilez.forgecdn.net`; chaque URL a été contrôlée par requête partielle avec succès HTTP et signature ZIP/JAR `PK`.

Les resource packs, datapacks et publications sans JAR Forge 1.20.1 sont exclus. Le champ `dependencies` ne contient pas de Markdown : les éléments sont séparés par des points-virgules, `optional:` signale les dépendances facultatives et `unknown` l'absence de métadonnées déclarées. Les identifiants de projet et versions sont conservés après le nom lorsqu'ils sont connus. Cette liste peut ne pas couvrir toute la chaîne transitive.

## Limite de compatibilité

La présence dans ce catalogue ne garantit pas la compatibilité avec Iron's Spells 3.16.2, entre addons, ni avec le modpack complet. Toute mise à jour ou entrée `needs-testing` doit passer par une instance de test avant déploiement.

# Catalogue des addons Iron's Spells pour Forge 1.20.1

## Objectif

Télécharger le plus grand nombre possible d'addons publics d'Iron's Spells 'n Spellbooks pour Minecraft Forge 1.20.1, y compris ceux dont la compatibilité avec la version retenue d'Iron's Spells n'est pas garantie. Tensura et ses intégrations sont exclus.

## Organisation

Les téléchargements restent isolés des dépendances NeoForge 1.21.1 et sont rangés dans `external-mods/irons-spells-forge-1.20.1/` :

- `active/` contient Iron's Spells et les addons directement exploitables ;
- `needs-testing/` contient les addons dont la compatibilité est incertaine ou liée à une autre version d'Iron's Spells ;
- `dependencies/` contient les bibliothèques et mods obligatoires récupérés séparément ;
- `manifest.csv` recense chaque fichier, sa version, sa source, sa catégorie et ses dépendances connues.

Un addon n'est jamais supprimé uniquement parce que sa compatibilité n'est pas confirmée. Il est placé dans `needs-testing/` pour éviter qu'il fasse échouer le premier démarrage du pack.

## Sources et sélection

Les fichiers proviennent en priorité des pages officielles CurseForge ou Modrinth. Seules les versions explicitement publiées pour Minecraft 1.20.1 et Forge sont retenues. Les doublons entre plateformes sont dédupliqués par projet et version. Les fichiers Fabric, NeoForge et ceux destinés à une autre version de Minecraft sont rejetés.

## Dépendances

Les dépendances obligatoires identifiables sont téléchargées lorsque leur version Forge 1.20.1 est disponible. Les gros mods de contenu requis par une intégration sont conservés dans `dependencies/` et ne sont pas activés automatiquement. Une dépendance absente ou ambiguë est signalée dans le manifeste au lieu d'être remplacée au hasard.

## Contrôles

Chaque JAR téléchargé doit être non vide, ouvrir comme archive ZIP et contenir des métadonnées de mod. Le manifeste conserve l'URL officielle et le nom du fichier. Un contrôle final recherche les doublons, les fichiers non-Forge, les mauvaises versions de Minecraft et toute référence à Tensura.

## Limites

La présence d'un addon dans le catalogue ne garantit pas qu'il puisse fonctionner avec tous les autres simultanément. La validation complète se fera ensuite par lots dans un client Forge 1.20.1, à partir de `active/`, puis en ajoutant progressivement les éléments de `needs-testing/`.

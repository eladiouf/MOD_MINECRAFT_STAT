# Socle propre de STAT Mod pour Forge 1.20.1

## Objectif

Remplacer, uniquement sur la branche `forge-1.20.1`, l'ancien projet NeoForge 1.21.1 par un socle minimal Minecraft Forge 1.20.1 prêt à recevoir la nouvelle implémentation de STAT Mod. Le projet NeoForge 1.21.1 reste intact sur sa branche. Tensura est entièrement exclu du nouveau socle.

## Plateforme

- Minecraft : `1.20.1`
- Loader : Minecraft Forge `47.4.10` (version recommandée officielle)
- Java : `17`
- Mod ID : `statmod`
- Groupe Java : `tong.statmod`
- Version initiale : `0.1.0+1.20.1`
- Nom affiché : `STAT Mod`

Le squelette doit provenir du MDK officiel Forge 1.20.1 correspondant, puis être réduit à la structure minimale utile au projet.

## Éléments conservés

- l'historique Git et la branche `forge-1.20.1` ;
- `.github/`, `.gitignore`, `.gitattributes` et `.editorconfig` après adaptation ;
- `LICENSE` et `CONTRIBUTING.md` lorsqu'ils restent génériques ;
- `external-mods/irons-spells-forge-1.20.1/`, y compris les 74 JAR locaux ignorés par Git, le catalogue, le manifeste et l'ordre de test ;
- `scripts/irons-addons/` ;
- les spécifications et plans Forge 1.20.1 sous `docs/superpowers/` ;
- les fichiers racine explicitement nécessaires au nouveau build Forge.

L'ancien contenu reste récupérable depuis l'historique Git et la branche `neoforge-1.21.1`; aucun dossier `legacy/` ne sera créé.

## Éléments supprimés ou remplacés

- tout le code Java et les ressources actuels de `src/` ;
- les configurations NeoForge, mixins et métadonnées 1.21.1 ;
- toutes les intégrations, recettes, données, tests et références Tensura ;
- `libs/` et ses dépendances NeoForge 1.21.1 ;
- `build/`, `.gradle/`, `runs/`, `logs/`, `tmp/` et les sorties générées ;
- les classes décompilées ou extraites aux racines `io/`, `META-INF/` et fichiers `.class` isolés ;
- les scripts d'analyse temporaires, images temporaires, archives de travail et anciens outils propres au projet 1.21.1 ;
- la documentation produit devenue fausse pour le nouveau socle, à l'exception des documents Forge conservés explicitement.

Chaque suppression récursive doit être limitée au worktree `.worktrees/forge-1.20.1` et précédée d'une vérification du chemin absolu.

## Nouveau socle

Le résultat contient :

- le wrapper Gradle et les fichiers de build du MDK Forge 1.20.1 ;
- `src/main/java/tong/statmod/StatMod.java`, point d'entrée minimal annoté `@Mod("statmod")` ;
- `src/main/resources/META-INF/mods.toml` avec les dépendances Minecraft, Forge et Java correctes ;
- `src/main/resources/pack.mcmeta` ;
- deux fichiers de langue minimaux, `assets/statmod/lang/en_us.json` et `assets/statmod/lang/fr_fr.json` ;
- un test structurel léger vérifiant l'identité du mod et l'absence de références interdites dans les sources actives.

Aucune fonctionnalité de statistiques, capacité, réseau, interface ou intégration Iron's Spells ne sera réimplémentée pendant cette préparation. Aucun des 74 addons ne sera ajouté au classpath de compilation initial.

## Dépendances et exécution

Le premier build utilise uniquement Minecraft Forge et les dépendances apportées par le MDK. Les JAR externes restent catalogués mais isolés sous `external-mods/`; ils seront introduits progressivement dans les configurations de développement lors des phases fonctionnelles suivantes.

Le répertoire d'exécution de développement sera neuf. Il ne réutilisera pas les anciens mondes, configurations ou logs NeoForge.

## Validation

La préparation est acceptée lorsque :

1. Gradle s'exécute sous Java 17 ;
2. `gradlew clean build` termine avec succès ;
3. le JAR produit contient `META-INF/mods.toml` et le point d'entrée `tong.statmod.StatMod` ;
4. `rg` ne trouve aucune référence active à `net.neoforged`, `neoforge`, `tensura` ou `tensura_iron_spells` dans `src/`, les fichiers Gradle et les métadonnées du mod ;
5. aucun JAR externe n'est committé ;
6. les 74 JAR Iron's Spells locaux et leur manifeste sont toujours présents et inchangés ;
7. la branche `neoforge-1.21.1` et son worktree principal ne sont pas modifiés.

## Séquence de travail suivante

Après validation de ce socle, les fonctionnalités de STAT Mod seront redéfinies et portées par sous-projets indépendants : modèle de statistiques, persistance et synchronisation, commandes, interfaces, puis intégration Iron's Spells et addons. Chaque sous-projet aura sa propre spécification et ses tests.

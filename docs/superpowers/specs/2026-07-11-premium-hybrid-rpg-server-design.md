# Serveur RPG hybride premium — Spécification maîtresse

## Vision

Transformer le serveur NeoForge 1.21.1 et STAT Mod en une expérience RPG complète de la qualité d'un grand modpack, jouable aussi bien par 2 à 10 amis que par une communauté publique allant jusqu'à 50 joueurs.

L'identité retenue est un monde médiéval sombre qui évolue progressivement vers la haute magie et des pouvoirs de niveau anime. La campagne principale doit durer entre 80 et 120 heures. Le monde de développement est généré normalement et rapidement ; une map personnalisée remplacera ce monde pour la production sans imposer de réécrire la progression.

## Principes directeurs

1. La puissance se mérite par la maîtrise de plusieurs systèmes, pas par une simple possession d'objet.
2. L'exploration reste libre, mais les systèmes capables de casser l'équilibre sont débloqués par actes.
3. La coopération est avantageuse sans rendre le jeu solo impossible.
4. La mort est significative, récupérable et ne détruit jamais définitivement des dizaines d'heures de progression.
5. L'économie facilite l'activité communautaire sans vendre directement l'endgame.
6. Tout ajout de mod doit combler un manque précis et réussir une validation sur serveur dédié.
7. Les performances, sauvegardes, permissions et procédures d'exploitation font partie du produit.

## Public et exploitation

- Petit groupe : expérience complète et équilibrée à 2–10 joueurs.
- Communauté : capacité cible de 10–50 joueurs avec protections, économie et anti-abus.
- Monde principal permanent.
- Saisons limitées aux classements, primes, événements et variantes de donjons ; aucune remise à zéro des constructions du monde principal.
- PvP consensuel hors zones explicitement dédiées.

## Progression en cinq actes

### Acte I — Les Cendres (0–10 heures)

- Survie médiévale et exploration locale.
- Équipement commun et premières statistiques.
- Initiation à Epic Fight : endurance, esquive, posture et fenêtres d'attaque.
- Magie avancée, races puissantes et équipement Apotheosis supérieur verrouillés.

### Acte II — Les Serments (10–30 heures)

- Spécialisation souple : guerrier, agile, arcaniste, spirituel ou hybride.
- Premiers donjons et boss régionaux.
- Forge avancée et démarrage de l'économie entre joueurs.
- Les spécialisations ont des forces et faiblesses identifiables sans classe irréversible.

### Acte III — L'Éveil (30–60 heures)

- Accès structuré à Iron's Spellbooks et aux systèmes Tensura.
- Déblocages conditionnés par statistiques, exploits, boss et maîtrises.
- La magie offre dégâts, contrôle et soutien avec des coûts empêchant le spam.

### Acte IV — Les Royaumes Brisés (60–90 heures)

- Dimensions et donjons difficiles.
- Boss majeurs, coopération renforcée et équipement légendaire.
- Les groupes sont encouragés, mais le scaling des ennemis reste plafonné.

### Acte V — L'Ascension (90–120 heures)

- Pouvoirs anime élevés, raids et forge ultime.
- Boss adaptatifs à mécaniques lisibles plutôt que simples sacs à points de vie.
- Endgame fondé sur variantes de donjons, classements saisonniers, défis et récompenses cosmétiques.
- Aucune inflation infinie des statistiques.

Chaque acte comporte une quête narrative d'entrée, plusieurs objectifs libres, deux ou trois preuves de maîtrise obligatoires, un défi final et une annonce claire des déblocages obtenus. Les validations importantes sont attachées au joueur et ne peuvent pas être contournées en recevant un objet d'un joueur avancé.

## Combat et difficulté

- Le monde de test commence en difficulté `normal`.
- Le passage en `hard` intervient uniquement après validation de l'équilibrage des deux premiers actes.
- La difficulté progresse par régions et actes grâce aux capacités, comportements et équipements ennemis, pas uniquement par leurs points de vie.
- Epic Fight constitue le langage de combat principal.
- Les boss exposent des zones dangereuses, phases, interruptions et fenêtres d'attaque compréhensibles.
- La taille du groupe influence les rencontres importantes avec un plafond qui conserve l'intérêt de coopérer.
- Les builds hybrides sont viables ; la puissance maximale exige une spécialisation réelle.
- Les zones, quêtes et portails indiquent leur niveau de danger avant l'engagement.

## Mort et récupération avec Corpse

Le mod Corpse, dans une version compatible avec NeoForge 1.21.1 et le serveur dédié, remplace tout système de tombe abstrait.

- Un cadavre apparaît à la mort et conserve équipement et inventaire.
- Le propriétaire dispose d'une période de protection exclusive configurable.
- Les membres de son équipe peuvent ensuite participer à la récupération.
- Le cadavre ne disparaît pas naturellement et résiste aux dangers environnementaux tels que la lave.
- L'historique des morts fournit les coordonnées, sans téléportation gratuite pour les joueurs.
- La mort retire une faible part d'XP et de monnaie non bancaire.
- Une blessure temporaire modérée est appliquée après réapparition.
- Les administrateurs disposent d'une procédure de restauration en cas de bug confirmé.
- Aucune perte permanente de statistiques, compétences ou objets uniques.

## Quêtes et verrouillage

Un mod de quêtes compatible NeoForge 1.21.1 est nécessaire ; FTB Quests est le candidat privilégié puisqu'il s'intègre à l'écosystème FTB déjà présent.

Les quêtes donnent la direction narrative et expliquent les systèmes. Les verrous réels restent contrôlés par STAT Mod et les données serveur : actes atteints, statistiques, boss vaincus, maîtrises, objets clés et autres exploits vérifiables. Une quête cochée seule ne doit jamais suffire à accorder un pouvoir critique.

Les recettes, dimensions et pouvoirs verrouillés affichent la condition manquante au joueur. JEI masque ou explique les objets techniques et recettes inutilisables.

## Économie

- SDM fournit la monnaie et les commerces administratifs.
- Les sources principales sont quêtes, primes, donjons et échanges entre joueurs.
- Les services, voyages, réparations, forge et améliorations servent de puits monétaires.
- Les objets communs et matériaux de confort sont commercialisables.
- Les artefacts de progression et équipements ultimes restent liés aux exploits.
- Les prix administratifs sont contrôlés ; les prix entre joueurs restent libres.
- Les récompenses répétables diminuent temporairement lorsqu'une activité est exploitée en boucle.
- La monnaie bancaire est protégée à la mort ; seule une fraction de la monnaie transportée peut être perdue.

## Coopération et communauté

- FTB Teams gère les groupes.
- FTB Chunks gère les claims et protections.
- Lootr individualise le butin important lorsque techniquement possible.
- Les participants éligibles reçoivent collectivement le crédit des boss.
- Les villes, hubs et lieux narratifs de la map finale sont protégés.
- Les téléportations gratuites, commandes dangereuses et contournements de progression sont limités par permissions.
- Les donjons et événements saisonniers sont réinitialisables indépendamment du monde permanent.

## Mods complémentaires

De nouveaux mods peuvent être ajoutés après vérification, en priorité pour :

1. Corpse et ses dépendances éventuelles.
2. Quêtes et chapitres.
3. Profiling et diagnostic des ticks.
4. Pré-génération de monde.
5. Sauvegardes automatiques avec rotation.
6. Permissions, administration et sécurité communautaire.

Un candidat n'est accepté que s'il :

- fournit une fonction absente ;
- cible Minecraft 1.21.1 et NeoForge ;
- fonctionne sur serveur dédié ;
- ne duplique ni ne casse STAT Mod, Epic Fight, Tensura, Iron's Spellbooks ou SDM ;
- possède une source de téléchargement et une licence identifiables ;
- passe le build du pack, le démarrage complet et un test de connexion client.

## Expérience joueur

- Identité, nom, MOTD et présentation cohérents.
- Guide de démarrage intégré aux quêtes.
- Descriptions claires des statistiques, compétences, objets et conditions de verrouillage.
- Audit et résolution des conflits de raccourcis clavier.
- Harmonisation des recettes concurrentes et des doublons de ressources.
- Nettoyage de JEI pour les éléments techniques ou volontairement inaccessibles.
- Retour clair après chaque jalon de progression et chaque échec de condition.

## Monde de test et map personnalisée

Le monde de test utilise une génération standard avec seed enregistrée et une zone pré-générée limitée. Il sert à valider rapidement les cinq actes, les performances et les migrations.

La map de production fournira villes, routes, hubs et lieux narratifs. Les références de contenu utilisent des identifiants de lieux, marqueurs, structures ou fichiers de configuration. Aucune quête ou règle centrale ne dépend de coordonnées codées en dur avant livraison de la map.

Une procédure d'import doit :

1. arrêter et sauvegarder le serveur ;
2. installer le monde personnalisé ;
3. associer les marqueurs de lieux ;
4. pré-générer la zone jouable ;
5. vérifier portails, claims, quêtes et points d'apparition ;
6. permettre un retour à la sauvegarde précédente.

## Exploitation serveur

- Sauvegardes automatiques avec rétention et restauration testée.
- Redémarrages planifiés avec avertissements.
- Profiling des ticks, entités et chunks.
- Distances de simulation et de vue adaptées après test de charge.
- Configurations distinctes pour développement, validation et production.
- Journalisation des actions administratives sensibles.
- Procédure de mise à jour avec staging, sauvegarde et retour arrière.
- Matrice exacte des mods requis côté client, serveur ou les deux.

## Validation et critères d'acceptation

Le produit n'est pas considéré terminé sur la seule preuve que le serveur démarre.

### Technique

- Build Gradle vert et tests automatisés de STAT Mod verts.
- Démarrage serveur dédié jusqu'au message `Done` sans erreur fatale.
- Connexion réussie avec le pack client exact.
- Aucun mod client-only dans le dossier serveur.
- Sauvegarde, redémarrage et restauration validés sur une copie du monde.
- Pré-génération terminée sans corruption.
- Test de charge représentatif documenté pour les distances et limites retenues.

### Gameplay

- Chaque acte est jouable de bout en bout sur un nouveau profil.
- Les verrous critiques résistent au don d'objets et aux téléportations anticipées.
- Un parcours solo et un parcours en groupe sont validés.
- La mort crée un cadavre récupérable et respecte les protections prévues.
- L'économie possède des sources et puits mesurés sans duplication connue.
- Les boss majeurs donnent correctement le crédit à tous les participants éligibles.
- La durée observée de la campagne se situe dans la cible de 80–120 heures après équilibrage.

### Qualité

- Aucune condition critique n'est expliquée uniquement dans une documentation externe.
- Les raccourcis par défaut ne comportent pas de conflit bloquant.
- Les recettes clés sont visibles et cohérentes dans JEI.
- Les avertissements connus sont triés entre acceptables, corrigés et bloquants.
- Les procédures administratives sont reproductibles par une autre personne.

## Découpage d'implémentation

La réalisation se fera en sous-projets séquentiels, chacun disposant de son propre plan et de ses vérifications :

1. **Fondation serveur** : dépendances complémentaires, profils de configuration, sauvegardes, diagnostic, pré-génération et matrice client/serveur.
2. **Progression des actes** : modèle d'état, conditions, verrous et commandes de diagnostic.
3. **Quêtes et onboarding** : chapitres, guide intégré, annonces et retours d'erreur.
4. **Combat et équilibrage** : difficulté régionale, boss, groupe, mort et Corpse.
5. **Économie et communauté** : SDM, claims, permissions, anti-abus et saisons.
6. **Contenu et harmonisation** : recettes, JEI, loot, raccourcis et cohérence inter-mods.
7. **Map personnalisée** : marqueurs, import, protections, pré-génération et validation finale.
8. **Qualification de production** : parcours complets, charge, sauvegarde/restauration et documentation d'exploitation.

La première phase à planifier et réaliser est la fondation serveur. Les phases ultérieures dépendent des outils, profils et procédures qu'elle met en place.

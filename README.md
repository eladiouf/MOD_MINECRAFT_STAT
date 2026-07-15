# STAT Mod — Forge 1.20.1

Fondation propre de **STAT Mod** pour Minecraft **1.20.1**, Forge
**47.4.10** et Java **17**.

## État actuel

Cette branche contient le socle technique et la progression automatique des
statistiques physiques, d'exploration et d'artisanat. Les autres systèmes seront
réintroduits progressivement. Aucun code Tensura ni NeoForge n’est actif dans
cette base.

## Fondation des statistiques joueur

STAT Mod fournit 19 statistiques réparties en cinq familles : combat physique,
chasse, magie fondamentale, mental et artisanat. Chaque
stat possède un niveau de 0 à 100 et une progression suivant la formule
`10 × (niveau + 1)²`.

Les données sont autoritaires côté serveur, sauvegardées en NBT, copiées après
la mort et synchronisées à la connexion, au respawn, au changement de dimension
et après une modification administrative.

Commandes disponibles :

```text
/statmod stats
/statmod stats <joueur>
/statmod stat get <joueur> <stat>
/statmod stat set <joueur> <stat> <niveau>
/statmod stat addxp <joueur> <stat> <quantité>
```

La consultation personnelle est libre. Consulter un autre joueur ou modifier
une statistique exige le niveau opérateur 2.

## Écran des statistiques et notifications

La touche `P` ouvre l'écran natif de STAT Mod. Il présente les 19 statistiques
dans cinq familles, avec leur niveau, leur XP et leur progression vers le niveau
suivant. Les cinq statistiques magiques sont actives et reliées aux actions
d'Iron's Spells ou à l'étude de livres enchantés.

Les gains automatiques affichent une notification discrète en haut à droite.
Les gains rapprochés de la même statistique sont regroupés et un passage de
niveau reste affiché plus longtemps. Les commandes administratives continuent
de synchroniser l'écran, mais ne produisent pas de fausse notification de gain
de gameplay.

## XP automatique et addons Epic Fight

Les dix-neuf statistiques progressent automatiquement. Quatorze utilisent les
actions physiques, d'exploration et d'artisanat : Brute Force, Blade
Technique, Rapidité, Agility, Physical Resistance, Physical Endurance,
Precision, Tracking, Keen Senses, Intimidation, Willpower, Forging, Cooking et
Alchemy. Arcane Power, Casting Speed et Mana Pool progressent au lancement d'un
sort depuis un grimoire. Érudition progresse par inscription de sorts et par
étude de livres enchantés ; Magic Resistance progresse sur les dégâts de sorts
réellement reçus.

Maintenir le clic d'utilisation pendant deux secondes avec un livre enchanté
étudie tous ses enchantements. Le gain dépend de leur niveau et de leur rareté,
le livre est ensuite consommé hors Créatif ; le livre étudié apparaît avec le
mouvement d'activation vanilla et le son du Totem confirme la réussite. Une
lecture interrompue ou entièrement refusée par la limite anti-farm
ne consomme rien.

La compatibilité des équipements d'addons est pilotée par quatre tags publics,
sans dépendance Java envers Epic Fight. Voir le
[guide XP des addons Epic Fight](docs/compatibility/epic-fight-addon-xp.md) et
la [matrice de validation](docs/compatibility/epic-fight-addon-matrix.csv).

## Effets de combat des statistiques

Brute Force, Blade Technique et Precision multiplient uniquement les attaques
classées pour leur spécialisation. La courbe reste neutre au niveau 0 (`x1`)
et atteint `x10` au niveau 100. Les tags publics d'équipement déterminent la
classification, y compris pour les armes d'addons.
Chaque palier automatique 25/50/75 ajoute ensuite `5 %` aux dégâts de la même
classe seulement. Au niveau 100 avec les trois perks, le multiplicateur total
atteint donc `x11,5`.

Physical Resistance et Physical Endurance réduisent uniquement les dégâts de
combat physiques éligibles. Leurs réductions sont multiplicatives. Les trois
perks de Physical Resistance ajoutent chacun 2 points de pourcentage avant le
plafond de sécurité; au niveau 100 pour les deux statistiques, la réduction
totale atteint `81,15 %`.

Arcane Power, Casting Speed, Mana Pool et Magic Resistance renforcent les six
attributs correspondants d'Iron's Spells. STAT Mod ne crée aucune seconde
réserve de mana et ne remplit jamais le mana lors d'un rafraîchissement.

## Perks automatiques

STAT Mod fournit 33 perks passifs automatiques : les 21 perks d'attributs
existants, plus 12 perks de combat classifié pour Brute Force, Blade Technique,
Precision et Physical Resistance. Trois perks cumulatifs s'activent aux niveaux
25, 50 et 75 de la statistique correspondante. Ils se désactivent
automatiquement si le niveau repasse sous leur prérequis. Tracking et Keen
Senses restent reportés à un lot ultérieur.

Il n'existe aucun arbre, point de perk, achat, respec ou affinité. Le serveur
déduit les perks actifs directement des niveaux et les affiche dans une liste
non interactive sur l'écran `P`. Puffish Attributes reste uniquement un
fournisseur d'attributs compatibles.

Le protocole réseau interne est la version `6` et borne la liste synchronisée à
33 identifiants canoniques.

## Compiler

Sous Windows :

```powershell
.\gradlew.bat clean build
```

Sous Linux ou macOS :

```bash
./gradlew clean build
```

Le JAR est généré dans `build/libs/`.

## Addons Iron's Spells

Iron's Spells 3.16.2 ou plus récent est une dépendance obligatoire de STAT Mod,
comme Epic Fight et Puffish Attributes. Ses bibliothèques Curios, GeckoLib,
Iron's Lib et Player Animator doivent être présentes dans le profil.

Le catalogue local des mods et addons Forge 1.20.1 est conservé dans
`external-mods/irons-spells-forge-1.20.1/`. Les JAR sont volontairement
ignorés par Git ; leur manifeste et leurs sommes SHA-256 permettent de
contrôler qu’aucun fichier n’a été perdu pendant la migration.

Tensura est explicitement exclu de cette nouvelle version.

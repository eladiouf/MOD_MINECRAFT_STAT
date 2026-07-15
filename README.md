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
suivant. Les quatre statistiques reliées aux attributs d'Iron's Spells sont
actives ; Érudition reste clairement indiquée comme fondation préparée.

Les gains automatiques affichent une notification discrète en haut à droite.
Les gains rapprochés de la même statistique sont regroupés et un passage de
niveau reste affiché plus longtemps. Les commandes administratives continuent
de synchroniser l'écran, mais ne produisent pas de fausse notification de gain
de gameplay.

## XP automatique et addons Epic Fight

Quatorze statistiques progressent automatiquement : Brute Force, Blade
Technique, Rapidité, Agility, Physical Resistance, Physical Endurance,
Precision, Tracking, Keen Senses, Intimidation, Willpower, Forging, Cooking et
Alchemy. La progression XP des cinq statistiques magiques reste différée :
Arcane Power, Casting Speed, Mana Pool, Erudition et Magic Resistance.

La compatibilité des équipements d'addons est pilotée par quatre tags publics,
sans dépendance Java envers Epic Fight. Voir le
[guide XP des addons Epic Fight](docs/compatibility/epic-fight-addon-xp.md) et
la [matrice de validation](docs/compatibility/epic-fight-addon-matrix.csv).

## Effets de combat des statistiques

Brute Force, Blade Technique et Precision multiplient uniquement les attaques
classées pour leur spécialisation. La courbe reste neutre au niveau 0 (`x1`)
et atteint `x10` au niveau 100. Les tags publics d'équipement déterminent la
classification, y compris pour les armes d'addons.

Physical Resistance et Physical Endurance réduisent uniquement les dégâts de
combat physiques éligibles. Leurs réductions sont multiplicatives et atteignent
ensemble `77,25 %` lorsque les deux statistiques sont au niveau 100.

Arcane Power, Casting Speed, Mana Pool et Magic Resistance renforcent les six
attributs correspondants d'Iron's Spells. STAT Mod ne crée aucune seconde
réserve de mana et ne remplit jamais le mana lors d'un rafraîchissement.

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

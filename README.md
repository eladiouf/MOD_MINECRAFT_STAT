# STAT Mod — Forge 1.20.1

Fondation propre de **STAT Mod** pour Minecraft **1.20.1**, Forge
**47.4.10** et Java **17**.

## État actuel

Cette branche contient le socle technique et la progression automatique des
statistiques physiques, d'exploration et d'artisanat. Les autres systèmes seront
réintroduits progressivement. Aucun code Tensura ni NeoForge n’est actif dans
cette base.

## Fondation des statistiques joueur

STAT Mod fournit 23 statistiques réparties en six familles : combat physique,
chasse, magie fondamentale, affinités élémentaires, mental et artisanat. Chaque
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

## XP automatique et addons Epic Fight

Quatorze statistiques progressent automatiquement : Brute Force, Blade
Technique, Rapidité, Agility, Physical Resistance, Physical Endurance,
Precision, Tracking, Keen Senses, Intimidation, Willpower, Forging, Cooking et
Alchemy. Les neuf statistiques magiques restent différées : Arcane Power,
Casting Speed, Mana Pool, Erudition, Magic Resistance et les affinités Fire,
Water, Earth et Air.

La compatibilité des équipements d'addons est pilotée par quatre tags publics,
sans dépendance Java envers Epic Fight. Voir le
[guide XP des addons Epic Fight](docs/compatibility/epic-fight-addon-xp.md) et
la [matrice de validation](docs/compatibility/epic-fight-addon-matrix.csv).

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

Le catalogue local des mods et addons Forge 1.20.1 est conservé dans
`external-mods/irons-spells-forge-1.20.1/`. Les JAR sont volontairement
ignorés par Git ; leur manifeste et leurs sommes SHA-256 permettent de
contrôler qu’aucun fichier n’a été perdu pendant la migration.

Tensura est explicitement exclu de cette nouvelle version.

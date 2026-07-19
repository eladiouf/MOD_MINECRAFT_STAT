# Epic Fight addon XP compatibility — Forge 1.20.1

STAT Mod reste indépendant d'Epic Fight et de ses addons. Il observe les
événements Forge standards et classe les objets avec quatre tags publics. Un
addon non testé ne doit jamais être présenté comme compatible : **an untested addon is not claimed compatible**.

## Tags publics

Un datapack peut compléter les tags dans
`data/statmod/tags/items/<nom>.json`. Garder `"replace": false` afin de
préserver les valeurs de STAT Mod et celles des autres datapacks.

### Arme lourde — `statmod:heavy_weapons`

Chemin : `data/statmod/tags/items/heavy_weapons.json`

```json
{
  "replace": false,
  "values": [
    "#minecraft:axes",
    "addon_id:great_hammer"
  ]
}
```

### Lame — `statmod:blade_weapons`

Chemin : `data/statmod/tags/items/blade_weapons.json`

```json
{
  "replace": false,
  "values": [
    "#minecraft:swords",
    "addon_id:katana"
  ]
}
```

### Arme de précision — `statmod:precision_weapons`

Chemin : `data/statmod/tags/items/precision_weapons.json`

```json
{
  "replace": false,
  "values": [
    "minecraft:bow",
    "minecraft:crossbow",
    "minecraft:trident",
    "addon_id:longbow"
  ]
}
```

### Équipement forgeable — `statmod:forgeable_equipment`

Chemin : `data/statmod/tags/items/forgeable_equipment.json`

```json
{
  "replace": false,
  "values": [
    "#minecraft:axes",
    "#minecraft:swords",
    "#minecraft:pickaxes",
    "#minecraft:shovels",
    "#minecraft:hoes",
    "#minecraft:trimmable_armor",
    "minecraft:bow",
    "minecraft:crossbow",
    "minecraft:trident",
    "minecraft:shield",
    "addon_id:great_hammer"
  ]
}
```

Un identifiant d'objet peut être remplacé par un tag d'addon, par exemple
`#addon_id:great_weapons`. Après modification, exécuter `/reload`, vérifier le
message de fin de rechargement, puis retester avec un objet nouvellement créé.

## Priorité et conflits

1. Une source marquée projectile donne toujours la classification Precision.
2. Hors projectile, un objet présent à la fois dans heavy et blade est ambigu :
   aucun XP Brute Force ou Blade Technique n'est attribué et un avertissement
   unique est écrit dans le journal serveur.
3. Sinon heavy, blade puis precision sont évalués dans cet ordre.
4. Le tag forgeable est indépendant et peut chevaucher les trois tags d'arme.
5. Un objet non classé fonctionne normalement mais ne donne aucun XP de
   spécialisation d'arme ou de Forging.

## Contrôle de l'XP

Utiliser les commandes suivantes avant et après chaque action :

```text
/statmod stats
/statmod stat get @s brute_force
/statmod stat get @s blade_technique
/statmod stat get @s precision
/statmod stat get @s rapidite
/statmod stat get @s forging
```

Les commandes d'inspection détaillée exigent le niveau opérateur 2. Les caps
anti-farm peuvent limiter un test répété : attendre 1 200 ticks ou utiliser un
nouveau monde/joueur de test pour distinguer un cap d'une incompatibilité.

## Procédure client intégré

1. Installer Forge 47.4.10, STAT Mod, Epic Fight, l'addon et ses dépendances.
2. Créer un monde de test en survie et exécuter `/reload`.
3. Vérifier séparément une arme heavy, blade et precision avec les commandes
   ci-dessus ; confirmer aussi le combo au troisième coup.
4. Tester un vrai projectile, une fabrication forgeable et la mort d'un mob
   hostile.
5. Se déconnecter/reconnecter, changer de dimension et vérifier que les niveaux
   persistent sans récompense doublée.
6. Conserver `latest.log`, les versions exactes et les identifiants d'objets.

## Procédure dedicated server

1. Installer exactement les mêmes versions côté serveur et sur le client de
   test, puis démarrer le dedicated server sans erreur de chargement.
2. Placer le datapack dans `world/datapacks/`, lancer `/reload` depuis la console
   ou avec un opérateur et contrôler `latest.log`.
3. Refaire les tests dégâts mêlée, combo, projectile, fabrication et mort.
4. Refaire les contrôles après relog et changement de dimension.
5. Redémarrer complètement le serveur et confirmer la persistance des stats et
   des biomes découverts.

Si l'attaque inflige des dégâts mais qu'aucun adaptateur STAT Mod ne réagit,
l'addon peut contourner `LivingDamageEvent` ou remplacer la source/son
propriétaire. Joindre un journal de niveau debug et un cas minimal ; aucune
compatibilité ne doit être inventée à partir du seul fait que le jeu démarre.

## Rapport de compatibilité

Indiquer : nom et version de l'addon, URL/source, version d'Epic Fight, Forge,
STAT Mod, dépendances, identifiants testés, tags ajoutés, type d'attaque,
résultats mêlée/combo/projectile/fabrication, client et serveur, étapes de
reproduction, résultat attendu/réel, logs et éventuel crash report.

## Statuts de la matrice

- `untested` : aucune procédure complète n'a été exécutée ; aucune compatibilité
  n'est revendiquée.
- `compatible` : toutes les cases applicables passent sur client et dedicated
  server pour les versions consignées.
- `partial` : le chargement fonctionne mais au moins une source XP applicable
  échoue ou requiert un datapack/contournement documenté.
- `incompatible` : chargement impossible, crash, corruption ou sources XP
  essentielles impossibles à rendre fonctionnelles.

La matrice est un relevé versionné, pas une promesse pour les versions futures.

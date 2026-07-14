# STAT Mod — Forge 1.20.1

Fondation propre de **STAT Mod** pour Minecraft **1.20.1**, Forge
**47.4.10** et Java **17**.

## État actuel

Cette branche contient uniquement le socle technique du mod : point d’entrée
Forge, métadonnées, ressources minimales, tests et automatisation de build.
Les systèmes de statistiques et de lancement de sorts seront réintroduits
progressivement. Aucun code Tensura ni NeoForge n’est actif dans cette base.

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

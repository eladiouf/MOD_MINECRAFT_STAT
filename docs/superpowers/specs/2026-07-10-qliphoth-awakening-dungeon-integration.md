# Qliphoth Awakening — Intégration Donjon (Salles Secrètes Piégées)

> **Spec :** Intégration des boss de Qliphoth Awakening dans les salles secrètes et ultra-vaults du Trial Dungeon.
> **Décision :** Onivo Studio — 2026-07-10
> **Statut :** Design

## Résumé

Qliphoth Awakening (modid `fdbosses`) ajoute des boss basés sur l'Arbre de Vie (Sephiroth). Chaque boss possède des attaques uniques, des mécaniques de phase et un spawner avec GUI de description. Dans le Trial Dungeon, ces boss deviennent des **combats secrets optionnels** : ils n'apparaissent que dans les salles secrètes (trappes) et les chambres ultra-secrètes (ultra-vaults), jamais dans le roster d'étage standard. Le joueur explore, trouve la salle, interagit avec le spawner Qliphoth → GUI → summon → combat. **Pas de loot natif** (les drops du mod sont désactivés dans le donjon) — seulement des points donjon (`DungeonPoints`).

## Boss Disponibles

| Sephiroth | Registry ID | Taille | Notes |
|-----------|-------------|--------|-------|
| Chesed | `fdbosses:chesed` | 5×3 | Premier boss, manipulations gravitationnelles |
| Malkuth | `fdbosses:malkuth` | 1.5×3 | Feu + glace, guerriers adds |
| Geburah | `fdbosses:geburah` | 2×2 | Jugement, chaînes, oiseaux de jugement |
| Netzach | `fdbosses:netzach` | ? | Nouveau (victoire/éternité), engrenages |

Chaque boss a son spawner dédié : `fdbosses:chesed_boss_spawner`, `fdbosses:malkuth_boss_spawner`, etc. (entités, pas des blocs).

## Architecture des Changements

### Whitelist (`DungeonSpawnGuard.java`)
Ajouter `"fdbosses"` à `isControlledModEntity()`. Les spawners et boss Qliphoth passeront le guard.

### Élection des combats Qliphoth
Nouvelle méthode utilitaire :
```java
static boolean isQliphothFloor(int floor) {
    return Math.floorMod(floor * 7919 + 11, 17) == 0;
}
```

- Fréquence : **~1 floor/17** (plus rare que les ultra-vaults qui sont 1/7)
- Combinable avec `isSecretRoomFloor()` et `isVaultFloor()` : quand les deux conditions sont vraies, la salle devient Qliphoth
- Applicable aux étages de combat uniquement (pas ×5 trésor ni ×10 boss)

### DungeonSecretRoom.java — Nouvelle variante « arène de boss »

**Avant (9×9, labyrinthe + coffres) :** une salle souterraine avec grille de murs, 2 coffres, pièges.
**Après :** Quand `isQliphothFloor(floor)` :

1. **Taille agrandie :** 15×15 au lieu de 9×9 (coque creusée sous la pièce, +2 étages en profondeur)
2. **Architecture :** 
   - Sol dallé uni (aucun labyrinthe)
   - 4 piliers aux coins intérieurs pour le repère visuel
   - Plafond voûté (hauteur 5 blocs au lieu de 3)
   - Lanternes aux 4 coins
   - Entrée : trappe + échelle (inchangée)
3. **Spawner boss :** Au centre de la salle, sur un piédestal (bloc de la palette d'accent), le spawner entité Qliphoth (`type = aléatoire parmi les 4 boss`). Le joueur right-clic dessus → GUI Qliphoth.
4. **Pas de coffres, pas de pièges** — seulement le combat de boss.
5. **Boss tracker :** Le handler enregistre l'UUID du mob via `DungeonBossTracker.register(floor, uuid)` pour attribution de points.

Quand la condition Qliphoth n'est pas remplie : comportement actuel inchangé (labyrinthe + coffres).

### DungeonUltraVault.java — Variante « chambre de boss »

**Avant :** coffre-relique + 4 coffres trésor + blocs précieux + pierre de retour.
**Après :** Quand `isQliphothFloor(floor)` :

1. **Intérieur vidé :** pas de coffres, pas de piédestal, pas de blocs précieux
2. **Spawner boss :** Au centre du sol, le spawner Qliphoth aléatoire
3. **Pierre de retour conservée :** le joueur doit pouvoir revenir (après la mort ou la victoire)
4. **Murs renforcés :** ajout de `DungeonProtectionHandler` (indestructible) sur toute la coque
5. **Pas de loot** — points donjon seulement.

### Handler de points (`DungeonBossHandler.java` modification)

Ajout d'un bloc dans `LivingDeathEvent` :
- Si le mort est une entité Qliphoth (`fdbosses:` namespace) ET que le floor est un `isQliphothFloor(floor)`
- Alors : attribuer **+200 points** (`DungeonPoints.awardMobKill`) sans compléter l'étage (c'est un combat optionnel)
- Le spawner Qliphoth doit aussi être **réactivable** (le mod natif le fait : le spawner réapparaît après la mort du boss)

### ModdedMobPool.java

Ajout des 4 boss aux pools thématiques :
- GESTE / TRÔNE DU NÉANT (haut niveau, ABYSS tier)
- Les minions (fire_malkuth_warrior, ice_malkuth_warrior, judgement_bird) ajoutés comme adds LATE/ABYSS

## Fichiers Impactés

| Fichier | Changement |
|---------|-----------|
| `dungeon/DungeonSpawnGuard.java` | +`"fdbosses"` dans `isControlledModEntity()` |
| `dungeon/DungeonSecretRoom.java` | Nouveau `maybePlaceQliphoth()` (arène 15×15 + spawner) |
| `dungeon/DungeonUltraVault.java` | Nouvelle branche Qliphoth dans `build()` |
| `dungeon/DungeonBossHandler.java` | Points pour kill Qliphoth (sans `completeFloor`) |
| `dungeon/ModdedMobPool.java` | + Qliphoth boss pool (ABYSS tier) |

## Non-Impactés

- `DungeonBossRoster.java` — les boss Qliphoth ne sont PAS dans le roster standard
- `DungeonBossAltarBlock.java` — pas d'autel, les spawners Qliphoth sont des entités autonomes
- `DungeonBossArena.java` / `DungeonBossArenaFloor.java` — les salles secrètes ont leur propre geometrie
- `DungeonThemes.java` — aucune modification des thèmes
- `DungeonProgress.java` — les Qliphoth ne complètent pas d'étage
- `DungeonLayout.java` / `DungeonRoomChain.java` — pas de changement de layout

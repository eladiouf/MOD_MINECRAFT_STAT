# Plan — Cohérence des Boss avec le Redesign Thématique

> **Mise à jour 2026-07-10 :** Ce plan fusionne l'intégration Qliphoth Awakening ET la réorganisation des boss de palier (×10) pour qu'ils soient cohérents avec les 10 arcs thématiques.
> **Mise à jour 2026-07-10 (v2) :** Ice and Fire (`iceandfire`) ajouté au modpack — intégration dans les 10 arcs + boss pools.

## Distribution Ice and Fire dans les Arcs

| Arc | Mobs Ice&Fire | Rôle |
|-----|--------------|------|
| 0 — DÉCHARNÉS | ghost, dread_thrall, dread_ghoul, dread_beast | Adds sur étages 6-10 |
| 1 — FAUVES | cyclops, troll, hippogryph, myrmex_worker, myrmex_soldier, death_worm, amphithere | Adds + mini-boss (cyclops) |
| 2 — TRIBUS | cyclops, troll | Adds (guerriers) |
| 3 — LÉGION NOIRE | dread_knight, dread_beast, ghost | Adds (élite) |
| 4 — ABYSSES | sea_serpent, hydra, siren, hippocampus | Adds + mini-boss (sea_serpent, hydra) |
| 5 — CERCLE DES MAGES | gorgon, cockatrice, dread_lich | Adds + mini-boss (gorgon) |
| 6 — MOISSON | stymphalian_bird, cockatrice | Adds |
| 7 — FOURNAISE | fire_dragon, amphithere | Boss pool (fire_dragon) + adds |
| 8 — GESTE DÉMONIAQUE | ice_dragon, lightning_dragon, dread_lich | Boss pool + adds |
| 9 — TRÔNE DU NÉANT | lightning_dragon, ghost | Boss pool + adds |

**Boss de palier :** toutes les créatures ≥ taille cyclops peuvent être boss de palier dans l'arc correspondant.
> **Spec :** `docs/superpowers/specs/2026-07-10-qliphoth-awakening-dungeon-integration.md`
> **Build :** `./gradlew build` attendu après chaque étape.

## Architecture

Deux niveaux de cohérence :

1. **Boss de palier (×10)** — chaque étage ×10 pioche dans le pool du mod majoritaire de son arc (ex : Arc 1 DÉCHARNÉS → boss SLU/BFB squelettiques). Le `DungeonBossRoster` est restructuré en **10 pools par arc** au lieu d'une Map plate par numéro d'étage.

2. **Boss Qliphoth (salles secrètes)** — chaque Sephiroth est assigné à un arc spécifique. Le choix du spawner dans la salle dépend de l'arc de l'étage, pas du hasard.

## Mapping Arc → Boss (10 arcs)

Le `DungeonBossRoster` passe d'une `Map<Integer, List<BossEntry>>` à un tableau `List<BossEntry>[10]` (un pool par arc). Chaque pool a 10+ boss entries (pour 10+ passes à travers le cycle). La méthode `forFloor(floor)` calcule `arcIndex = ((floor / 10) - 1) % 10` et `pass = (floor / 10 - 1) / 10`, puis pioche dans le pool de l'arc à l'index `pass % pool.size()`.

| Arc | Floors ×10 | Boss Pool (ordre croissant de difficulté) |
|-----|-----------|------------------------------------------|
| **0 — DÉCHARNÉS** | 10, 110, 210... | wither_skeleton, soul_of_cinder, gundyr, fallen_lord, boss_gael, beast_clergyman, lich(BoMD), artorias, ancient_warrior, soul_of_cinder |
| **1 — FAUVES** | 20, 120, 220... | warden, void_blossom(BoMD), yeti(BFB), sandworm(BFB), kraken(BFB), naga(Mowzie), umvuthi(Mowzie), aatrox |
| **2 — TRIBUS** | 30, 130, 230... | artorias, ornstein+smough, looking_glass_knight, crucible_knight, dragon_slayer_armour, count_robert, gauntlet(BoMD), notch+minecraft_lord |
| **3 — LÉGION NOIRE** | 40, 140, 240... | margit+morgott, maliketh, mohg, elemer, godfrey+hoarah_loux, fallen_lord, dread_king(Iron's), underworld_knight(BFB) |
| **4 — ABYSSES** | 50, 150, 250... | coralssus(Cata), wadjet(Cata), scylla(Cata), the_leviathan(Cata), obsidilith(BoMD), pirate_captain(BFB), lord_of_depths(BiC) |
| **5 — CERCLE DES MAGES** | 60, 160, 260... | godskin_apostle+godskin_noble, dead_king(Iron's), citadel_keeper(Iron's), gwyndolin, lich(BoMD), wukong, soul_of_cinder |
| **6 — MOISSON DE L'EFFROI** | 70, 170, 270... | malenia, malenia_2, sir_pumpkinhead(BiC), lord_pumpkinhead(BiC), pumpkin_bruiser(BiC), abyss_watcher, nameless_king |
| **7 — FOURNAISE** | 80, 180, 280... | radahn, radahn_2, infernal_dragon(BFB), netherite_monstrosity(Cata), godskin_noble, aatrox, jax, warden+darius |
| **8 — GESTE DÉMONIAQUE** | 90, 190, 290... | maliketh, dead_king(Iron's), citadel_keeper(Iron's), pantheon, underworld_knight(BFB), mohg, harbinger(Cata), el_leviathan(Cata) |
| **9 — TRÔNE DU NÉANT** | 100, 200, 300... | gael+radagon+elden_beast, pantheon, wukong, notch+minecraft_lord, monster_crucible_knight, scylla(Cata), ender_golem(Cata) |

**Note :** Les boss SLU polyvalents remplissent les arcs qui manquent de boss natifs. L'environnement (palette, architecture) assure la cohérence visuelle — le boss n'est jamais seul dans son arène.

## Mapping Arc → Boss Qliphoth (secret/ultra-vault)

| Arc | Sephiroth | Spawner ID | Justification |
|-----|-----------|-----------|---------------|
| 0 — DÉCHARNÉS | Geburah | `fdbosses:geburah_boss_spawner` | Jugement des morts, chaînes des damnés |
| 1 — FAUVES | Netzach | `fdbosses:netzach_boss_spawner` | Éternité de la nature, cycles vitaux |
| 2 — TRIBUS | Malkuth | `fdbosses:malkuth_boss_spawner` | Guerriers élémentaires, le feu et la glace du combat |
| 3 — LÉGION NOIRE | Geburah | `fdbosses:geburah_boss_spawner` | Jugement sans pitié, puissance obscure |
| 4 — ABYSSES | Chesed | `fdbosses:chesed_boss_spawner` | Gravité des profondeurs, pression abyssale |
| 5 — CERCLE DES MAGES | Malkuth | `fdbosses:malkuth_boss_spawner` | Dualité magique feu/glace |
| 6 — MOISSON DE L'EFFROI | Netzach | `fdbosses:netzach_boss_spawner` | Victoire des récoltes, cycle éternel |
| 7 — FOURNAISE | Malkuth | `fdbosses:malkuth_boss_spawner` | Guerriers de feu, brasier infernal |
| 8 — GESTE DÉMONIAQUE | Chesed | `fdbosses:chesed_boss_spawner` | Gravité démoniaque, poids des âmes |
| 9 — TRÔNE DU NÉANT | Chesed | `fdbosses:chesed_boss_spawner` | Gravité du vide, attraction du néant |

Utilitaire :
```java
private static String qliphothSpawnerForArc(int arcIndex) {
    return switch (arcIndex) {
        case 0, 3 -> "fdbosses:geburah_boss_spawner";
        case 1, 6 -> "fdbosses:netzach_boss_spawner";
        case 2, 5, 7 -> "fdbosses:malkuth_boss_spawner";
        case 4, 8, 9 -> "fdbosses:chesed_boss_spawner";
        default -> "fdbosses:chesed_boss_spawner";
    };
}
```

## Fichiers Impactés

### 1. `DungeonSpawnGuard.java` — Whitelist
Ajouter `"fdbosses"` à `isControlledModEntity()`.

### 2. `DungeonBossRoster.java` — Refonte complète
- Remplacer `Map<Integer, List<BossEntry>>` par `List<BossEntry>[10]` (un pool par arc)
- Chaque pool contient ~10 boss entries ordonnées par difficulté croissante
- `forFloor(floor)` calcule `arcIndex` + `pass`, pioche dans le pool
- Garder les helpers `s()`, `d()`, `w()` inchangés

### 3. `ModdedMobPool.java` — Qliphoth pool
Ajouter `addQliphothMobs()` avec minions Qliphoth dans les vagues ABYSS.

### 4. `DungeonSecretRoom.java` — Salle secrète Qliphoth
- Helper `isQliphothFloor(floor)` (1/17, primes)
- Helper `qliphothSpawnerForArc(arcIndex)` (mapping ci-dessus)
- Nouvelle branche arène 15×15 avec spawner au centre + 4 piliers + lanternes

### 5. `DungeonUltraVault.java` — Ultra-vault Qliphoth
- Branche vide : spawner au centre, pierre de retour, pas de loot

### 6. `DungeonBossHandler.java` — Points Qliphoth
- Early return quand namespace `fdbosses` : award points sans completeFloor

## Ordre d'implémentation

1. `DungeonBossRoster.java` — refonte arc-indexée
2. `DungeonSpawnGuard.java` — +`"fdbosses"`
3. `ModdedMobPool.java` — +addQliphothMobs
4. `DungeonSecretRoom.java` — helpers + arène Qliphoth
5. `DungeonUltraVault.java` — branche Qliphoth
6. `DungeonBossHandler.java` — early return points
7. `./gradlew build` + `./gradlew test`

## OKR

1. **Build OK** — `./gradlew build` passe
2. **Tests verts** — `./gradlew test` passe (existants + nouveaux)
3. **Mapping correct** — tout étage ×10 a un boss de son arc
4. **Qliphoth cohérent** — la salle secrète affiche le bon Sephiroth pour l'arc
5. **Pas de softlock** — tuer un Qliphoth ne complète pas l'étage
6. **Points** — kill Qliphoth donne +200 points

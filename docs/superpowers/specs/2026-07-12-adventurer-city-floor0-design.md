# Étage 0 — La Cité des Aventuriers (design)

- **Date** : 2026-07-12
- **Statut** : validé par l'utilisateur (brainstorming complet, 4 sections approuvées)
- **Mission** : extension M6 (Trial Dungeon) — remplacement du hub temple par une capitale souterraine
- **Référence visuelle** : image concept « Étage 0 — La Cité des Aventuriers » (plan 600×600, 15 zones numérotées, alliance des 4 peuples)

---

## 1. Vision

L'étage 0 du Trial Dungeon devient une **capitale souterraine vivante** : une immense caverne
abritant une cité bâtie par l'alliance des quatre peuples fondateurs (Humains, Elfes, Nains,
Hommes-bêtes), totalement sûre (aucun spawn hostile), servant de lieu de préparation, de
commerce et de rencontre avant la descente. Le contraste voulu : étage 0 = civilisation,
étage 1+ = hostilité.

**Décisions de cadrage (validées une à une)** :

| Question | Décision |
|---|---|
| Temple 64×64 existant (`DungeonHubFloor`) | **Remplacé intégralement** — les services sont réinstallés dans les quartiers |
| Échelle | **~600×600 blocs**, fidèle à la vision (plafond ~150 blocs) |
| PNJ | **Mixte** : villageois vanilla + ambassadeurs Tensura + mod **Guard Villagers** (gardes) |
| Sanctuaire | **Respec perks + magie, payant** en points de donjon ; stats/niveaux/XP jamais reset |
| Arène | **Système de duel complet** (défi, téléportation, restauration, zéro perte) |
| Guilde | **Tableau de bord vivant** (progression + classements réels) ; pas de quêtes en v1 |
| Génération | **100 % code procédural** (zéro schematic), file de segments étalée sur les ticks |

---

## 2. Implantation et architecture

### 2.1 Coordonnées

- Centre de la cité : `(0, 100, -500)` dans `statmod:trial_dungeon`.
- Emprise : `x ∈ [-300, +300]`, `z ∈ [-800, -200]` — entièrement dans la zone étage 0
  (`floorAtPos` renvoie 0 pour tout `z < -150`), aucune interaction avec la grille des étages 1+
  (`FLOOR_SPACING=300`, étage 1 à `(0,0)`).
- Verticalité : sol urbain Y=100 (constante des îles), dôme de caverne jusqu'à Y≈245,
  égouts Y≈90, underside rocheux sous la ville.
- Le spawn de l'étage 0 (`floorSpawnPos(0)` / `floorPlayerSpawnPos(0)`) est déplacé sur la
  **Grande Place** ; le portail overworld et la balise de poche (`DungeonBeaconItem`) y déposent
  le joueur.

### 2.2 Nouveau package `tong.statmod.dungeon.city`

| Classe | Rôle |
|---|---|
| `CityPlan` | Géométrie **pure et testable** (modèle `DungeonLayout`/`IslandShaper`) : place centrale, rues radiales, secteurs angulaires des quartiers, positions des monuments. Seed fixe → plan déterministe. |
| `CityShell` | La caverne : sol, falaises périmétrales, dôme, cristaux lumineux bleutés suspendus, cascades tombant des parois. |
| `CityGenerator` | Orchestrateur : remplace `IslandGenerator` pour l'étage 0. File de segments, budget de blocs/tick, marqueur d'achèvement (SavedData), bounding box de regen dédiée. |
| `*Builder` (un par zone) | `PlazaBuilder`, `GuildTowerBuilder`, `HumanQuarterBuilder`, `ElvenQuarterBuilder`, `DwarvenQuarterBuilder`, `BeastQuarterBuilder`, `MarketBuilder`, `ArtisanDistrictBuilder`, `ArenaBuilder`, `TrainingGroundsBuilder`, `GardensBuilder`, `SewersBuilder`, `HeroHallBuilder`, `SanctuaryBuilder`, `PortalCourtBuilder`, `DungeonGateBuilder` |
| `CityPalettes` | Palettes de blocs par identité raciale (pierre/bois/cuivre, bois blanc/cristal, pierre taillée/lave, cuir/totems) réutilisant l'interface `BlockPalette`. |
| `CityNpcSpawner` | Peuplement idempotent (villageois, ambassadeurs Tensura, gardes) par quartier. |
| `CityAmbience` | Sons positionnels périodiques par quartier (modèle `DungeonAmbience`). |
| `CityLeaderboard` | SavedData serveur : agrégats des records (§4.1). |
| `DuelManager` + `DuelCommands` | Machine à états des duels d'arène (§4.4), cœur pur avec temps injecté. |
| `SanctuaryAltars` | Handlers des 3 autels du Sanctuaire (§4.3). |
| `TrainingDummies` | Mannequins + affichage des dégâts (§4.4). |

### 2.3 Génération étagée (point critique)

600×600×~150 ≈ plusieurs millions de placements : impossible en un tick.

1. `CityGenerator` découpe la construction en **segments** (shell d'abord, puis place, rues,
   quartiers, monuments, détails, PNJ) traités à budget fixe (~20–50k blocs/tick, à calibrer).
2. La construction démarre **au premier démarrage du serveur** (tâche de fond sur le tick
   serveur) — pas à la première entrée. Dans la plupart des cas, la cité est prête avant que
   quiconque n'ouvre le portail.
3. Entrée pendant la construction : le joueur arrive sur la **Grande Place** (segment prioritaire),
   action-bar « La cité s'éveille… N % ».
4. **Idempotent + déterministe** : restart en cours de build → la file repart de zéro et écrase à
   l'identique (marqueur SavedData « cité achevée » absent → re-run). `/statdungeon regen 0`
   régénère toute la cité.
5. Les mondes possédant l'ancien temple 64×64 le voient remplacé : la bounding box de l'étage 0
   couvre l'ancienne emprise.

### 2.4 Sécurité de zone

- Dimension void + `DungeonSpawnGuard` (liste blanche) : déjà aucun spawn hostile.
- **Exemption étage 0** : la mort dans la cité ne coûte aucun point de donjon et respawn sur la
  Grande Place (jamais le retour punitif de `DungeonRespawnHandler`).
- Bâti protégé par `DungeonProtectionHandler` (déjà en place pour la dimension).
- Lore : le **Cristal Gardien** de la Grande Place matérialise la barrière magique.

---

## 3. Les zones (plan urbain)

Place centrale ronde → 6 avenues radiales pavées → quartiers en secteurs. Du sud (entrée) au nord :

| Zone | Contenu bâti | Services réinstallés / mécanique |
|---|---|---|
| **Porte du Donjon** (sud) | Porte monumentale ~30 blocs, 4 statues des races fondatrices (socles + armor stands équipés), ouverture à l'approche (échange de blocs sur détection de proximité) | `next_floor_teleporter` (étage 1) au centre du passage |
| **Grande Place** | Statue du Premier Aventurier, fontaine magique, **Cristal Gardien** (améthyste + faisceau), panneaux de classements | Waystone centrale, `return_beacon`, `MagicBanker` |
| **Tour de la Guilde** | Bâtiment le plus haut, visible partout ; rez = hall, étages = cartes/archives/stratégie, sommet = Maître de Guilde | Tableau de bord vivant (§4.2) |
| **Quartier Humain** (le plus vaste) | Auberges, banque, habitations, entrepôts, écuries — pierre/bois/cuivre | Dortoir (lits), marchand généraliste |
| **Quartier Elfique** | Arbre-mère géant, bibliothèque, jardins lumineux, serres — bois blanc/feuilles/cristaux | Coin enchantement niv. 30, marchand potions (`DungeonMerchant` profil 2) |
| **Quartier Nain** | Creusé dans la falaise : grandes forges, fonderies, lave encastrée sécurisée, tailleurs de gemmes | Forge Royale complète, marchands armes + armures (profils 0 et 1) |
| **Quartier Hommes-bêtes** | Chenils, écuries, totems, rivière, tentes de cuir, arbres | Marchand archerie (profil 3) |
| **Grand Marché** | Bazar d'étals colorés (laine multicolore) | `PointExchange` / `DungeonExchanger` (points → monnaie), accès boutique SDM |
| **District des Artisans** | Ateliers alignés : meules, tables de forge, alambics, métiers à tisser | Postes de craft utilitaires |
| **Arène** | Colisée circulaire à gradins, deux extrémités de combat | Duels (§4.4) |
| **Terrain d'entraînement** | Stands de tir, zones de test | Mannequins chiffrés (§4.4), cibles vanilla |
| **Jardins Suspendus** | Lacs, cascades, cerisiers, bancs, plantes lumineuses | Repos ; fontaine de soin (`DungeonHealHandler`) |
| **Hall des Héros** | Galerie de statues + plaques + trophées | Records réels (§4.1) |
| **Sanctuaire** | Temple silencieux à l'écart | 3 autels (§4.3) |
| **Les Égouts** | Tunnels sous la ville (Y≈90), accès par grilles | **Décor pur en v1** ; crochet v2 (quêtes, mini-boss, contrebandiers) |
| **Cour des Portails** | Arches runiques violettes | 1 active = retour overworld ; les autres **scellées** (« bientôt ») pour extensions futures |

---

## 4. Mécaniques nouvelles

### 4.1 Hall des Héros (records)

- `CityLeaderboard` (SavedData) agrège à chaque événement : meilleur étage atteint,
  meilleur combo (`dungeonBestCombo`), meilleur temps de nettoyage (`dungeonBestClearTicks`),
  points de donjon, victoires d'arène.
- Le Hall expose : statues des **3 meilleurs aventuriers** (armor stands + têtes de joueur +
  équipement d'apparat), plaques (panneaux) des records, **un trophée par boss du roster vaincu**
  sur le serveur.
- Rafraîchissement des panneaux à l'entrée d'un joueur dans le Hall.

### 4.2 Guilde — tableau de bord vivant

- **Réceptionniste** (villageois nommé, clic droit) → bilan personnel en chat : étage atteint,
  boss vaincus, points, records, rang serveur.
- Murs du hall : top 5 par catégorie (mêmes données que §4.1) + liste des étages boss conquis.
- **Pas de système de quêtes en v1** (crochet v2 explicite).

### 4.3 Sanctuaire — 3 autels

| Autel | Interaction | Effet |
|---|---|---|
| **Bénédiction** | Clic droit, paye des points de donjon | 1 des 3 effets longue durée (~15 min) : Force, Résistance, Célérité |
| **Purification** | Clic droit, gratuit | Dissipe tous les effets négatifs |
| **Renaissance (respec)** | Sneak+clic droit, confirmation, coût proportionnel aux points investis | **Rembourse intégralement** points de perks + `magicPoints` (désapprentissage des nœuds via les services existants). Retire d'abord tous les modifieurs d'attributs actifs. Stats/niveaux/XP jamais touchés. |

### 4.4 Arène et entraînement

**Duels** (`DuelManager`, cœur pur, temps injecté, états `IDLE → PENDING → COUNTDOWN → FIGHTING`) :

- `/statduel <joueur>` → défi (expire 60 s) ; `/statduel accept` / `deny`.
- Accept → téléportation des deux aux extrémités de l'arène, **état sauvegardé** (position, PV,
  faim, effets), compte à rebours 5 s (titres), combat.
- « Mort » en duel → interceptée/annulée : perdant restauré et déposé dans les gradins,
  **zéro perte** (inventaire, XP, points), annonce de victoire à la cité, victoires/défaites
  enregistrées dans `PlayerStatData`.
- Garde-fous : timeout 5 min = nul ; déconnexion ou sortie d'arène = forfait ; un seul duel actif
  (file d'attente) ; PvP impossible partout ailleurs dans la cité ; l'état sauvegardé est la
  source de vérité de la restauration.

**Mannequins** : entités neutralisées (IA vidée, invulnérables à la mort, re-soignées en continu,
persistantes) — chaque coup affiche les dégâts en **chiffres flottants** (Text Display, ~1 s)
+ cumul DPS sur 5 s en action-bar.

---

## 5. Habitants

- `CityNpcSpawner`, peuplement **idempotent** : à chaque passage, si l'effectif d'un quartier est
  incomplet, les manquants réapparaissent. Tous persistants (anti-despawn), IA de flânerie
  bornée à leur quartier.
- **Villageois vanilla** : foule de base, profession assortie (forgeron chez les Nains,
  bibliothécaire chez les Elfes…), noms thématiques.
- **Ambassadeurs Tensura** : quelques humanoïdes Tensura par quartier racial (identité visuelle).
  IDs **à vérifier dans les jars** à l'implémentation ; IA hostile neutralisée ; fallback villageois.
- **Gardes** : mod **Guard Villagers** en patrouille sur les avenues (ID à vérifier dans les jars) ;
  fallback golems de fer pacifiés.
- `CityAmbience` : marteaux (Nains), eau/oiseaux (Jardins, Hommes-bêtes), cloches (Place),
  crépitements de forge — sons positionnels périodiques.

### Exit Conditions — intégration Guard Villagers (politique STAT-PM-001)

- **Entrée** : soft dependency, résolution par registre au runtime, fallback golem pacifié.
- **Sortie** : si le mod disparaît du pack, est abandonné upstream, ou casse au-delà de
  1 version de maintenance → suppression du pont (`integration/guardvillagers/` ou résolution
  inline dans `CityNpcSpawner`) sans autre impact que le fallback golem. Aucune donnée persistée
  ne dépend du mod.

---

## 6. Cas limites et gestion d'erreurs

- **Mods absents** : chaque dépendance douce (Guard Villagers, Tensura, Waystones, SDM) passe
  par une résolution soft avec fallback vanilla (modèle `MacawDungeonDecorator`). Aucun crash.
- **Restart pendant la construction** : reprise par re-run complet idempotent (déterminisme).
- **Mort dans la cité** : aucune pénalité, respawn Grande Place.
- **Duel interrompu** (déco, sortie d'arène, dégât externe létal) : forfait propre + restauration
  systématique des deux participants.
- **Respec** : retrait des modifieurs avant remboursement ; idempotent (pas de double
  remboursement) ; refus si points de donjon insuffisants.
- **PNJ perdus/morts** : repeuplement idempotent au passage suivant.
- **Anciens mondes** : l'ancien temple est écrasé par la regen étage 0.

---

## 7. Tests

Cœurs purs uniquement (pas de `ServerLevel` — philosophie du package dungeon) :

| Suite | Invariants verrouillés |
|---|---|
| `CityPlanTest` | Districts dans l'emprise 600×600 ; aucun chevauchement ; rues connectant chaque quartier à la place ; déterminisme ; Porte au sud, Sanctuaire à l'écart |
| `DuelManagerTest` | Machine à états complète : défi→expiration, accept→countdown→combat, forfait (déco/sortie), timeout, unicité du duel actif, restauration exigée à chaque sortie d'état |
| `CityLeaderboardTest` | Agrégation, tri, tie-break, sérialisation NBT |
| `SanctuaryRespecTest` | Calcul du coût, remboursement intégral perks+magie, idempotence |

Vérification manuelle en jeu (checklist) : génération complète sans lag bloquant, PNJ présents
et confinés, duel réel de bout en bout, respec réel, records affichés, fallbacks sans les mods.

---

## 8. Hors périmètre v1 (crochets v2)

- Quêtes de Guilde (tableau, PNJ donneurs, récompenses).
- Contenu des Égouts (mini-boss, passages secrets, contrebandiers).
- Marchands saisonniers du Grand Marché.
- Portails vers d'autres villes/dimensions/événements.
- Événements temporaires de la Grande Place.

L'espace physique de chacun existe dès la v1.

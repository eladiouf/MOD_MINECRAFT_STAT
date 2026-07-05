# Trial Dungeon — Système de shop (intégration SDM)

- **Date** : 2026-07-05
- **Mission** : M6 (`STAT-DEC-TRIAL-DUNGEON`) — débouché économique du système de points
- **Statut** : approuvé (posture autonome Onivo Studio)
- **Plan associé** : `docs/superpowers/plans/2026-07-05-dungeon-shop-sdm.md` (à créer)

---

## Problème

Le donjon accorde des `dungeonPoints` (tués de mobs pondérés par la difficulté, conquêtes, boss),
affichés au HUD et pénalisés à la mort — mais ils n'ont **aucun débouché** : on ne peut rien en
faire. C'était la feature en attente (« l'échange à la sortie ») évoquée depuis l'étude de Wild
Dungeons.

L'utilisateur a installé la suite **SDM** (`sdmcore`, `sdmeconomy`, `sdmshop`, `sdm_ui_library`),
qui fournit une économie (monnaies + soldes par joueur, API `EconomyAPI` /
`CurrencyPlayerData.SERVER.addCurrencyValue/getBalance`) et un shop configurable en jeu. On s'appuie
dessus plutôt que de réécrire un shop.

## Décisions (issues du brainstorming)

1. **Monnaies séparées** : les `dungeonPoints` restent un **score** (HUD, pénalité de mort). Le
   donjon n'accorde **pas** de coins automatiquement.
2. **Gain des coins par échange** : on obtient les **coins** (monnaie SDM) en **convertissant ses
   points** chez un **villageois changeur** présent dans les étages récompense (trésor).
3. **Interface d'échange = vrai écran GUI** : contrôle du montant converti (on ne veut pas tomber à
   0 par accident).
4. **Règle « 0 point → éjection »** : dès que les points tombent à 0 dans le donjon (mort **ou**
   conversion), le joueur est **renvoyé à l'overworld**. Les coins, eux, sont toujours à l'abri.
5. **Contenu du shop non codé** : l'utilisateur configure les articles/prix en jeu via l'admin GUI
   de SDM Shop (prix en « Dungeon Coins »), et l'ouvre par l'accès natif de SDM (`/shop`).

## Architecture

Tout côté `tong.statmod.dungeon` (+ network, client, config). SDM est une **dépendance optionnelle**
adressée par **réflexion** guardée par `ModList.isLoaded` — convention des bridges existants
(`L2HostilityBridge`, `WaystonesBridge`).

### Composants

| Composant | Rôle |
|---|---|
| `integration/sdm/SDMEconomyBridge` | Bridge réflexion optionnel : `available()`, `ensureCurrency(server)` (crée « Dungeon Coins » si absente), `addCoins(player, n)`, `getCoins(player)`. No-op sûr si SDM absent. |
| `dungeon/DungeonExchanger` | Pose le villageois changeur (NoAI, invulnérable, tag `statmod_dungeon_exchanger`, aucun trade). Handler `PlayerInteractEntityEvent` : clic-droit sur un changeur → ouvre l'écran (packet S2C). Exempté du nettoyage des mobs. |
| `network/OpenExchangePayload` (S2C) | Ouvre/rafraîchit l'écran côté client, transporte `points` + `coins` courants. |
| `network/ConvertPointsPayload` (C2S) | Demande la conversion de `amount` points. Le serveur valide, convertit, ré-émet `OpenExchangePayload`. |
| `client/PointExchangeScreen` | Écran : affiche points + coins, champ de saisie du montant, bouton **Convertir**, bouton **Max sûr** (= `points − 1`). |
| `dungeon/DungeonPointsEjection` | Règle « 0 point → overworld » : après toute baisse de points dans le donjon, si `points == 0` → `DungeonTeleportHandler.returnToOverworld` + message. |
| `config/Config` (trial_dungeon) | `shopCurrencyName` (défaut `dungeon_coins`), `pointToCoinRate` (défaut 1.0). |

### Flux de conversion

```
Joueur clic-droit changeur
  └─ PlayerInteractEntityEvent (serveur) : entité taguée exchanger → cancel + envoie OpenExchangePayload(points, coins)
       └─ Client : ouvre PointExchangeScreen(points, coins)
            └─ Joueur saisit montant N, clique Convertir → ConvertPointsPayload(N) (C2S)
                 └─ Serveur : n = clamp(N, 0, points) ; coins += floor(n * rate) ; points -= n
                      ├─ SDMEconomyBridge.addCoins(player, floor(n*rate))
                      ├─ SyncHelper.syncStats(player)  (points + coins au HUD/cache)
                      ├─ si points == 0 → DungeonPointsEjection.check → returnToOverworld
                      └─ sinon → renvoie OpenExchangePayload(points', coins') → l'écran se rafraîchit
```

### Conversion & clamps

- `n = clamp(demande, 0, pointsCourants)` — jamais négatif, jamais plus que ce qu'on a.
- `coinsGagnés = floor(n * pointToCoinRate)`.
- `points -= n` (via `PlayerStatData.addDungeonPoints(-n)`, déjà borné ≥ 0).
- Si SDM absent (`!available()`) : le changeur affiche un message « shop indisponible » et **ne
  touche pas** aux points (pas de conversion dans le vide).

### Sync des coins au client

Les coins vivent dans SDM (solde serveur). Pour l'affichage, l'écran reçoit les coins **dans le
payload** (`OpenExchangePayload`) — pas besoin de les stocker dans `PlayerStatData`. Le HUD peut
éventuellement afficher les coins plus tard (hors scope de ce spec ; l'écran suffit).

### Règle d'éjection

- Centralisée dans `DungeonPointsEjection.check(ServerPlayer)` : appelée après **toute** baisse de
  points dans le donjon — les deux sources sont `DungeonPoints.applyDeathPenalty` et la conversion.
- Condition : joueur dans `statmod:trial_dungeon` **et** `getDungeonPoints() == 0`.
- Action : message (`dungeon.ejected.no_points`) + `DungeonTeleportHandler.returnToOverworld(player)`.
- Idempotent : si déjà hors donjon, no-op.

## Interfaces (contrats)

- `SDMEconomyBridge.addCoins(ServerPlayer, long) : boolean` — `true` si crédité, `false` si SDM
  absent/erreur. Ne jette jamais.
- `SDMEconomyBridge.getCoins(ServerPlayer) : long` — solde, `0` si absent.
- `DungeonExchanger.spawn(ServerLevel, BlockPos)` — pose un changeur à une position.
- `DungeonPointsEjection.check(ServerPlayer) : void` — applique la règle 0→overworld.

## Gestion d'erreurs

- SDM absent : bridge no-op, changeur informe, aucune conversion. Le donjon reste jouable.
- Réflexion SDM échoue (version API différente) : capturé, loggé une fois, `available()` → false.
- Montant invalide (négatif, > points, non numérique) : clampé/rejeté serveur, l'écran ne crash pas.
- Conversion menant à 0 : autorisée (choix du joueur) → éjection immédiate après crédit des coins.

## Tests

- `PointExchangeMathTest` (pur) : clamp du montant, `floor(n*rate)`, points ne descendent jamais < 0.
- `DungeonPointsEjectionTest` (pur/léger) : condition `points==0 && inDungeon` → éjection ; sinon non.
- Bridge SDM : non testé unitairement (réflexion / mod runtime) — vérifié au run.

## Hors scope

- Le **contenu** du shop (articles, prix, catégories) : configuré en jeu par l'utilisateur.
- L'accès au shop (`/shop`, raccourci) : fourni par SDM.
- L'affichage des coins au HUD : possible plus tard, non requis ici.

## Exit Conditions (politique STAT-PM-001)

- Si l'utilisateur retire la suite SDM : le bridge devient no-op, les changeurs informent « shop
  indisponible », les points/le donjon restent pleinement fonctionnels. Aucune régression bloquante.

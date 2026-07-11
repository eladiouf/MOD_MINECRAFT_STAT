# Bountiful Physical FDP Bank Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter huit coupures FDP physiques, les distribuer par Bountiful et fournir un Banquier magique bidirectionnel dans le hub et les villages.

**Architecture:** `FdpDenomination` centralise les valeurs et objets. `MagicBankService` effectue les calculs et transactions serveur via `SDMEconomyBridge`. `MagicBanker` gère les PNJ et sessions, tandis que des payloads dédiés ouvrent et rafraîchissent une interface client. Les ressources Bountiful restent des datapacks JSON optionnels.

**Tech Stack:** Java 21, NeoForge 21.1.232, Minecraft 1.21.1, JUnit 5, SDM Economy/Shop, Bountiful 8.0.0-beta.2.

## Global Constraints

- Devise numérique exacte : `FDP_cfa`.
- Coupures exactes : 50, 100, 200, 500, 1 000, 2 000, 5 000, 10 000.
- Bountiful doit rester optionnel au classloading.
- Toute transaction est validée et exécutée sur le thread serveur.
- Aucun objet ni solde ne peut être perdu si une étape échoue.
- Le banquier est distinct du changeur, des marchands vanilla et de SDM Shop.

---

### Task 1: Catalogue et objets FDP

**Files:**
- Create: `src/main/java/tong/statmod/economy/FdpDenomination.java`
- Modify: `src/main/java/tong/statmod/item/ModItems.java`
- Create: `src/test/java/tong/statmod/economy/FdpDenominationTest.java`
- Create: `src/main/resources/assets/statmod/models/item/fdp_*.json`
- Create: `src/main/resources/assets/statmod/textures/item/fdp_*.png`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`

**Interfaces:**
- Produces: `FdpDenomination.ascending()`, `descending()`, `valueOf(Item)`, `itemFor(long)`.

- [ ] Écrire un test exigeant les huit valeurs triées et une correspondance objet/valeur bijective.
- [ ] Exécuter `./gradlew test --tests tong.statmod.economy.FdpDenominationTest` et constater l’échec par absence du catalogue.
- [ ] Enregistrer les huit `Supplier<Item>` dans `ModItems`, construire l’enum `FdpDenomination` et ajouter les objets à l’onglet créatif.
- [ ] Générer huit textures lisibles, modèles `item/generated` et traductions en/fr.
- [ ] Réexécuter le test et valider les JSON.

### Task 2: Pool monétaire Bountiful

**Files:**
- Create: `src/main/resources/data/statmod/bounty_pools/statmod/fdp_rewards.json`
- Create/Modify: `src/main/resources/data/statmod/bounty_decrees/bountiful/*.json`
- Create: `src/test/java/tong/statmod/integration/bountiful/BountifulFdpResourcesTest.java`

**Interfaces:**
- Consumes: identifiants `statmod:fdp_coin_*` et `statmod:fdp_note_*`.
- Produces: pool Bountiful `statmod:fdp_rewards` référencé par les décrets standards.

- [ ] Écrire un test de ressources qui exige `currency: true`, les huit valeurs `unitWorth` et la référence du pool dans chaque décret surchargé.
- [ ] Exécuter le test et observer l’échec par ressources absentes.
- [ ] Ajouter le pool JSON au format confirmé dans `currency_example.json` de Bountiful 8.0.0-beta.2.
- [ ] Copier fidèlement les listes des décrets standards et y ajouter `statmod:fdp_rewards` sans retirer les pools existants.
- [ ] Réexécuter le test et parser tous les JSON avec `ConvertFrom-Json`.

### Task 3: Calculs et transactions bancaires

**Files:**
- Create: `src/main/java/tong/statmod/economy/FdpCashMath.java`
- Create: `src/main/java/tong/statmod/economy/MagicBankService.java`
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java`
- Create: `src/test/java/tong/statmod/economy/FdpCashMathTest.java`

**Interfaces:**
- Produces: `FdpCashMath.breakdown(long)`, `isWithdrawable(long)`, `MagicBankService.depositAll(ServerPlayer)`, `withdraw(ServerPlayer,long)` et `SDMEconomyBridge.removeCoins(ServerPlayer,long)`.

- [ ] Écrire les tests pour 18 850 → 10 000+5 000+2 000+1 000+500+200+100+50, montants non multiples de 50 et valeurs négatives.
- [ ] Exécuter les tests et observer l’échec par classes absentes.
- [ ] Implémenter la décomposition gloutonne pure et les résultats typés de transaction.
- [ ] Étendre le bridge SDM avec un débit vérifié, puis implémenter dépôt et retrait avec simulation de capacité d’inventaire.
- [ ] Réexécuter les tests ciblés.

### Task 4: PNJ Banquier magique et génération

**Files:**
- Create: `src/main/java/tong/statmod/economy/MagicBanker.java`
- Create: `src/main/java/tong/statmod/economy/VillageBankerSavedData.java`
- Create: `src/main/java/tong/statmod/economy/VillageBankerSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonHubFloor.java`
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopNPCBridge.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`
- Create: `src/test/java/tong/statmod/economy/MagicBankerSourceTest.java`

**Interfaces:**
- Produces: `MagicBanker.spawn(ServerLevel,BlockPos)`, `canUse(ServerPlayer)` et marqueur `statmod_magic_banker`.

- [ ] Écrire un test source exigeant marqueur, session, distance maximale, exclusion SDM et placement au hub.
- [ ] Exécuter le test et observer l’échec.
- [ ] Implémenter le PNJ immobile/invulnérable/persistant et sa session de 30 secondes à huit blocs.
- [ ] Le placer une fois dans `DungeonHubFloor`.
- [ ] Enregistrer le spawner de villages et son `SavedData` keyed par dimension+cloche `meeting`.
- [ ] Exclure le marqueur dans `SDMShopNPCBridge` au clic et au tag automatique.
- [ ] Réexécuter le test.

### Task 5: Réseau et écran bancaire

**Files:**
- Create: `src/main/java/tong/statmod/network/OpenMagicBankPayload.java`
- Create: `src/main/java/tong/statmod/network/MagicBankActionPayload.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/network/ServerPayloadHandler.java`
- Modify: `src/main/java/tong/statmod/network/ClientPayloadHandler.java`
- Create: `src/main/java/tong/statmod/client/MagicBankScreen.java`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Create: `src/test/java/tong/statmod/network/MagicBankNetworkSourceTest.java`

**Interfaces:**
- `OpenMagicBankPayload(long balance,long physical)` est S2C.
- `MagicBankActionPayload(Action action,long amount)` est C2S avec `DEPOSIT_ALL` et `WITHDRAW`.

- [ ] Écrire un test source exigeant enregistrement S2C/C2S, validation `MagicBanker.canUse` et rafraîchissement après transaction.
- [ ] Exécuter le test et observer l’échec.
- [ ] Ajouter les codecs et enregistrements réseau.
- [ ] Implémenter l’écran avec soldes, Tout déposer, montant, Retirer, Retrait max et Fermer.
- [ ] Implémenter le handler serveur qui ignore toute donnée de solde envoyée par le client et renvoie un snapshot frais.
- [ ] Ajouter les messages d’erreur traduits et réexécuter le test.

### Task 6: Vérification et déploiement

**Files:**
- Build output: `build/libs/statmod-1.2.0.jar`
- Client: `C:/Users/El Hadji/AppData/Roaming/.minecraft/versions/test/mods/`

- [ ] Copier `libs/bountiful-neoforge-8.0.0-beta.2.jar` dans le dossier `mods` du client.
- [ ] Exécuter `./gradlew test` et obtenir `BUILD SUCCESSFUL`.
- [ ] Exécuter `./gradlew build` et obtenir `BUILD SUCCESSFUL`.
- [ ] Copier `build/libs/statmod-1.2.0.jar` dans le client.
- [ ] Vérifier les empreintes SHA-256 des deux JAR copiés.
- [ ] Contrôler en jeu le flux quête → espèces → dépôt → achat SDM → retrait.

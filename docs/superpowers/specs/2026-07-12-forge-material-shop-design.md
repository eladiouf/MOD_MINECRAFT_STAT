# Forge Material Shop — Design Spec

**Date** : 2026-07-12
**Mission** : Overgeared Economy Phase 1
**Décision** : `STAT-DEC-FORGE-SHOP`

---

## 1. Objectif

Fournir un shop standalone (sans dépendre du mod `sdmshop`) permettant aux joueurs d'acheter les matériaux de forge Overgeared avec la monnaie FDP_cfa. Pas de heated ingots en vente. Les blueprints sont volontairement chers.

---

## 2. Architecture

```
ForgeMaterialShop (NPC Villager)
    │  PlayerInteractEvent → cancel + OpenForgeShopPayload (S2C)
    ▼
ForgeMaterialScreen (GUI client)
    │  Catégories : Roughs / Blueprints / Grips / Components / Tools
    │  Clic [Acheter] → BuyForgeItemPayload (C2S)
    ▼
ServerPayloadHandler
    │  Valide session → SDMEconomyBridge.removeCoins() → give item → refresh
    ▼
ForgeShopPrices (pure, testable)
    │  Map<String, Long> : itemId → prix FDP
    ▼
VillageForgeMerchantSpawner (auto-spawn villages)
```

### Composants

| Fichier | Rôle |
|---------|------|
| `economy/ForgeMaterialShop.java` | NPC villager + interaction handler |
| `economy/ForgeShopPrices.java` | Définition des prix (pure/testable) |
| `client/ForgeMaterialScreen.java` | GUI screen avec catégories et boutons achat |
| `network/OpenForgeShopPayload.java` | S2C — ouvre le screen |
| `network/BuyForgeItemPayload.java` | C2S — achète un item |
| `network/ServerPayloadHandler.java` | Traite l'achat |
| `network/ClientPayloadActions.java` | Ouvre le screen côté client |
| `economy/VillageForgeMerchantSpawner.java` | Auto-spawn dans les villages |

---

## 3. NPC — ForgeMaterialShop

- Villager avec **VillagerProfession.WEAPONSMITH**, niveau 5
- NoAI, invulnerable, persistent, custom name "Marchand de Forge"
- Tag NBT : `statmod_forge_merchant`
- `PlayerInteractEvent.EntityInteract` → cancel + crée session (30s, rayon 8 blocs) + `OpenForgeShopPayload`
- Calqué sur `MagicBanker.java` (session UUID-dimension-tick, `canUse()`)

### Spawn

- `spawn(ServerLevel, BlockPos)` → crée et tagge le villager
- `VillageForgeMerchantSpawner` : `PlayerTickEvent.Post` toutes les 200 ticks dans un village, si pas déjà présent

---

## 4. Prix — ForgeShopPrices

Classe pure stateless avec une méthode `priceOf(String itemId) → OptionalLong`.

### Roughs (90 items : 6 classes × 15 materials)

| Tier | Materials | Prix/unité |
|------|-----------|:----------:|
| Low | tin, bronze, gold, iron | 50, 80, 100, 150 FDP |
| Mid | silver, steel, diamond, pyrium | 200, 250, 300, 400 FDP |
| High | arcane, mithril, orichalcum, low_magisteel | 500, 600, 700, 900 FDP |
| Very High | magisteel, netherite, pure_magisteel | 1000, 1200, 1500 FDP |
| Top | high_magisteel, adamantite, hihiirokane | 2000, 2500, 3000 FDP |

**Logique :** extrait le matériau depuis `rough_<class>_<material>` → lookup dans `MATERIAL_PRICES`.

### Blueprints

| Blueprint | Prix |
|-----------|:----:|
| blueprint_universal_blade | 500 FDP |
| blueprint_universal_pole | 1 500 FDP |
| blueprint_runic_blade | 5 000 FDP |
| blueprint_legendary | 10 000 FDP |

### Grips

| Grip | Prix |
|------|:----:|
| wooden_grip | 50 FDP |
| leather_wrap | 200 FDP |
| wire_wrap | 500 FDP |
| runic_grip | 1000 FDP |

### Form Components (6)

`katana_tsuba`, `rapier_guard`, `claymore_pommel`, `halberd_socket`, `warhammer_core`, `staff_focus` → **200 FDP** chacun.

### Tools (2)

`basic_forge_tongs`, `basic_smithing_hammer` → **100 FDP** chacun.

---

## 5. GUI — ForgeMaterialScreen

- `extends Screen` (comme `MagicBankScreen`, `PointExchangeScreen`)
- Style glassmorphism : `fillGradient` fond sombre, bordure dorée
- **Haut** : titre translatable + solde FDP_cfa (depuis SDMEconomyBridge)
- **Catégories** : tabs horizontaux (Roughs, Blueprints, Grips, Components, Tools)
  - Chaque catégorie définit sa liste d'items
  - Clic sur un tab → change la liste affichée
- **Liste** : défilement vertical (widget `ScrollPanel` ou `AbstractSelectionList`)
  - Chaque ligne : icône item + nom + prix + bouton [Acheter]
  - Bouton Acheter → grisé si solde insuffisant → `PacketDistributor.sendToServer(new BuyForgeItemPayload(itemId))`
- **Bas** : bouton Done (close screen)
- `openOrRefresh()` → met à jour solde + état des boutons
- `isPauseScreen() = false`

---

## 6. Network

### OpenForgeShopPayload (S2C)

```java
record OpenForgeShopPayload(long balance) implements CustomPacketPayload {
    TYPE = "statmod:open_forge_shop"
    CODEC = StreamCodec.composite(VAR_LONG, ...)
}
```

### BuyForgeItemPayload (C2S)

```java
record BuyForgeItemPayload(String itemId) implements CustomPacketPayload {
    TYPE = "statmod:buy_forge_item"
    CODEC = StreamCodec.composite(UTF_STRING, ...)
}
```

### Server Handling

```java
handleBuyForgeItem(BuyForgeItemPayload, context) {
    // 1. Valider session NPC (ForgeMaterialShop.canUse)
    // 2. Rechercher prix (ForgeShopPrices.priceOf)
    // 3. Vérifier fonds suffisants (SDMEconomyBridge.getCoins >= price)
    // 4. Débiter (SDMEconomyBridge.removeCoins)
    // 5. Donner l'item au joueur (inventory add ou drop si plein)
    // 6. Message de confirmation
    // 7. Rafraîchir solde (OpenForgeShopPayload)
}
```

---

## 7. Registration

### Items

Aucun nouvel item — le shop référence les items existants via leurs `Supplier<Item>` statiques :
- `ForgingIntermediates.byId("rough_blade_gold")`
- `ForgingBlueprints.BLUEPRINT_UNIVERSAL_BLADE`
- `ForgingGrips.WOODEN_GRIP`
- etc.

### Joueur — Changements

Aucun changement dans `PlayerStatData`. Pas de nouveau champ.

### Event Bus

- `ForgeMaterialShop` → registré sur `NeoForge.EVENT_BUS` (comme `MagicBanker`)
- `VillageForgeMerchantSpawner` → registré sur `NeoForge.EVENT_BUS`

---

## 8. Tests

| Test | Scope |
|------|-------|
| `ForgeShopPricesTest` | 90 roughs ont un prix, blueprints, grips, components, tools — tous les items connus ont un prix > 0 |
| `ForgeMaterialScreenTest` | (pas d'infra GameTest — manuel) |

---

## 9. Non-Goals

- Pas de heated ingots en vente
- Pas de vente joueur → shop (one-way)
- Pas de stock / quantité limitée
- Pas de connexion avec dungeonPoints ou magicPoints
- Pas de listing dans le SDM Shop (standalone uniquement)
- Pas de Bountiful integration
- Pas de GameTest automatique pour le screen

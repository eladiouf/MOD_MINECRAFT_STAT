# Plan — Armes + EpicFight (Keyword JSON Data-Driven)

## Découverte

EpicFight a un **système data-driven par regex** dans `data/epicfight/capabilities/weapons/item_keyword/*.json`.

**Format :**
```json
// sword.json (dans epicfight.jar)
{"regexes": [".*_sword"]}

// greatsword.json
{"regexes": [".*_greatsword"]}
```

**14 fichiers existants :** axe, bow, crossbow, dagger, fist, greatsword, hoe, longsword, pickaxe, shovel, spear, sword, tachi, uchigatana

**Classes clés decompilées :**
- `ItemKeywordReloadListener` — charge les regex JSON, stocke dans `Map<ResourceLocation, ItemRegex> REGEXES`
- `scanDirectory(ResourceManager, String, Gson, Map)` — scanne tous les mods pour des fichiers keyword
- `ItemCapabilityReloadListener` — per-item explicit caps dans `data/epicfight/capabilities/weapons/<item>.json`

## Problèmes Identifiés

1. **Bug pattern EpicFight :** `.*_greatsword` (sans underscore) ne match PAS `tensura:.*_great_sword` (avec underscore) → catché par `.*_sword` → anims SWORD au lieu de GREATSWORD
2. **Pas de patterns Tensura/Mahou :** les items moddés n'ont pas de capabilities → pas d'animations EpicFight
3. **Pas de catégorie STAFF dans EpicFight :** les bâtons/wands n'ont pas d'animations adaptées

## Solution

Créer des fichiers keyword JSON dans notre mod pour matcher les items Tensura/Mahou avec des regex **namespace-specific**.

## 🇫 Mapping Fichier → Catégorie EpicFight

**Découverte critique :** Le **nom du fichier** (sans extension) est la catégorie EpicFight assignée.

**Preuve :** Les 14 fichiers existants dans epicfight.jar ont TOUS un nom qui correspond exactement à une catégorie `WeaponCategories` :
- `sword.json` → `WeaponCategories.SWORD`
- `greatsword.json` → `WeaponCategories.GREATSWORD`
- `dagger.json` → `WeaponCategories.DAGGER`
- etc.

→ **On ne peut PAS créer un fichier `st_tensura.json` unique** (ça assignerait la catégorie inexistante `epicfight:st_tensura`)

→ **Il faut créer un fichier PAR catégorie EpicFight cible.**

## Fichiers à créer

### `st_greatsword.json` → GREATSWORD
```json
{
  "regexes": [
    "tensura:.*_great_sword",
    "tensura:.*_odachi",
    "tensura:.*_hammer"
  ]
}
```

### `st_axe.json` → AXE
```json
{
  "regexes": [
    "tensura:.*_scythe",
    "tensura:.*_mace",
    "tensura:.*_club",
    "tensura:.*_axe"
  ]
}
```

### `st_sword.json` → SWORD
```json
{
  "regexes": [
    "tensura:.*_staff",
    "tensura:.*_wand",
    "tensura:.*_short_sword",
    "tensura:.*_needle_sword",
    "tensura:.*_sickle",
    "tensura:.*_rapier",
    "mahoutsukai:.*_wand",
    "mahoutsukai:.*_staff",
    "mahoutsukai:.*_rod"
  ]
}
```

### `st_longsword.json` → LONGSWORD
```json
{
  "regexes": [
    "tensura:.*_long_sword"
  ]
}
```

### `st_uchigatana.json` → UCHIGATANA
```json
{
  "regexes": [
    "tensura:.*_katana"
  ]
}
```

### `st_tachi.json` → TACHI
```json
{
  "regexes": [
    "tensura:.*_tachi"
  ]
}
```

### `st_dagger.json` → DAGGER
```json
{
  "regexes": [
    "tensura:.*_kodachi",
    "tensura:.*_dagger"
  ]
}
```

### `st_spear.json` → SPEAR
```json
{
  "regexes": [
    "tensura:.*_spear",
    "tensura:.*_throwing"
  ]
}
```

### `st_bow.json` → BOW
```json
{
  "regexes": [
    "tensura:.*_bow"
  ]
}
```

### `st_crossbow.json` → CROSSBOW
```json
{
  "regexes": [
    "tensura:.*_crossbow"
  ]
}
```

### `st_fist.json` → FIST
```json
{
  "regexes": [
    "tensura:.*_knuckle",
    "tensura:.*_gauntlet",
    "tensura:.*_fist"
  ]
}
```

## Ordre de priorité des regex

L'ordre alphabétique des fichiers garantit que les patterns namespace-specific (`tensura:.*_great_sword`) sont checkés AVANT les patterns génériques d'EpicFight (`.*_sword`) :

| Fichier | Pattern | Match |
|---|---|---|
| `...dagger.json` | `.*_dagger` | dagues vanilla |
| `st_spear.json` | `tensura:.*_spear` | ✅ lances Tensura |
| `st_sword.json` | `tensura:.*_staff` | ✅ staffs Tensura (pas catché par sword) |
| `st_...` | ... | ... |
| `sword.json` | `.*_sword` | ❌ ne catch PLUS `tensura:.*_great_sword` |
| `tachi.json` | `.*_tachi` | tachi vanilla |

## Implémentation

### Fichiers à créer

```
src/main/resources/data/epicfight/capabilities/weapons/item_keyword/
├── st_greatsword.json
├── st_axe.json
├── st_sword.json
├── st_longsword.json
├── st_uchigatana.json
├── st_tachi.json
├── st_dagger.json
├── st_spear.json
├── st_bow.json
├── st_crossbow.json
├── st_fist.json
```

### Aucun code Java nécessaire

Pas de mixin, pas de register. EpicFight `ItemKeywordReloadListener` scanne automatiquement les dossiers `data/*/capabilities/weapons/item_keyword/` de TOUS les mods au démarrage. Chaque fichier assigne la catégorie correspondant à son nom.

### Premier-match

Si EpicFight utilise le premier-match (ce qui est attendu), les patterns namespace-specific dans nos fichiers `st_*.json` matcheront avant les patterns génériques des fichiers EpicFight. Les items vanilla ne sont pas affectés car nos patterns ont le préfixe `tensura:` ou `mahoutsukai:`.

## Vérifications

1. Lancer le jeu avec les fichiers keyword
2. Vérifier que `tensura:adamantite_great_sword` a l'animation GREATSWORD (pas SWORD)
3. Vérifier que `tensura:hihiironkane_katana` a l'animation UCHIGATANA
4. Vérifier que `tensura:iron_spear` a l'animation SPEAR
5. Vérifier que les items vanilla sont toujours corrects (épée → SWORD, etc.)

## Vérifications

1. Lancer le jeu avec les fichiers keyword
2. Vérifier que `tensura:adamantite_great_sword` a l'animation GREATSWORD (pas SWORD)
3. Vérifier que `tensura:hihiironkane_katana` a l'animation UCHIGATANA
4. Vérifier que `tensura:iron_spear` a l'animation SPEAR
5. Vérifier que les items vanilla sont toujours corrects

## Références

- `yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener` — classe qui charge les regex
- `yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener` — classe pour les per-item explicit caps
- `data/epicfight/capabilities/weapons/item_keyword/*.json` — dossier dans epicfight.jar (14 fichiers)
- `data/epicfight/capabilities/weapons/*.json` — per-item explicit caps (iron_greatsword.json, glove.json, etc.)
- `CapabilityItem$WeaponCategories` — enum des catégories disponibles

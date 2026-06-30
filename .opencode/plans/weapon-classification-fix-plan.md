# Plan de Correction — Classification des Armes + Animations EpicFight

---

## Résumé du Problème

3 bugs dans la détection d'armes, qui causent :
- Mauvaises stats XP (greatsword → BLADE au lieu de BRUTE)
- Pas d'animations EpicFight pour les armes moddées
- 3 résolveurs parallèles incohérents

---

## Partie A — Catalogue des Armes Tensura (~150 items)

**8 matériaux :** adamantite, diamond, golden, hihiirokane, high_magisteel, iron, low_magisteel, mithril, netherite, orichalcum, pure_magisteel, silver, stone

**15 types × matériaux :**

| Type | Stat cible | Stat actuelle (bug) |
|---|---|---|
| `sword` | BLADE_TECHNIQUE | ✅ BLADE |
| **`great_sword`** | **BRUTE_FORCE** | ❌ catché "sword" → **BLADE** |
| `long_sword` | BLADE_TECHNIQUE | ❌ catché "sword" → BLADE (OK dans ce cas) |
| `short_sword` | BLADE_TECHNIQUE | ❌ catché "sword" → BLADE (OK) |
| `katana` | BLADE_TECHNIQUE | ✅ catché "katana" |
| `tachi` | BLADE_TECHNIQUE | ✅ catché "tachi" |
| `odachi` | BRUTE_FORCE | ✅ fallthrough → BRUTE (correct par accident) |
| `kodachi` | RAPIDITE (ou BLADE) | ✅ catché "kodachi" |
| `spear` | PRECISION | ✅ catché "spear" |
| `axe` | BRUTE_FORCE | ✅ catché |
| `scythe` | BRUTE_FORCE | ❌ pas de pattern → BRUTE (correct par accident) |
| `sickle` | BLADE_TECHNIQUE | ❌ pas de pattern → BRUTE (FAUX) |
| `dagger` | BLADE_TECHNIQUE | ✅ catché |
| `bow` | PRECISION | ✅ catché |
| `crossbow` | PRECISION | ✅ catché |
| `staff` | ARCANE_POWER | ❌ pas de pattern → BRUTE (FAUX) |
| `club` | BRUTE_FORCE | ❌ pas de pattern → BRUTE (OK) |
| `gauntlet`/`knuckle` | BRUTE_FORCE/FIST | ❌ pas de pattern → BRUTE (OK) |
| `throwing` spear | PRECISION | ❌ pas de pattern → BRUTE si main vide (FAUX) |

---

## Partie B — Fix Immédiats (5 min)

### B1. `TensuraEpHandler.resolveActionTypeId()` — Réordonner les checks

Actuel (BUG) :
```java
if (path.contains("sword") || ...) return "melee_sword";  // ligne 94
if (path.contains("axe") || path.contains("greatsword") || path.contains("great_sword")) return "melee_axe"; // ligne 109
```

Fix — déplacer "great_sword" et "axe" AVANT "sword", ajouter les types manquants :
```java
// BRUTE d'abord
if (path.contains("greatsword") || path.contains("great_sword")
    || path.contains("odachi") || path.contains("axe")
    || path.contains("scythe") || path.contains("club")
    || path.contains("hammer") || path.contains("mace")) return "melee_axe";

// BLADE ensuite
if (path.contains("sword") || path.contains("katana") || path.contains("tachi")
    || path.contains("kodachi") || path.contains("dagger") || path.contains("sickle")) return "melee_sword";

// PRECISION
if (path.contains("bow") || path.contains("crossbow") || path.contains("spear")
    || path.contains("trident") || path.contains("ranged") || path.contains("throwing")) return "ranged";

// MAGIC
if (path.contains("staff") || path.contains("wand") || path.contains("spell") || path.contains("magic")) return "magic_fire";
```

### B2. `CombatXPHandler.resolveWeaponStat()` — Ajouter les types manquants

Ajouter dans le bon ordre :
```java
// BRUTE
if (path.contains("greatsword") || path.contains("great_sword")
    || path.contains("odachi") || path.contains("scythe")
    || path.contains("club") || path.contains("hammer")
    || path.contains("mace")) return StatType.BRUTE_FORCE;

// BLADE
if (path.contains("sickle")) return StatType.BLADE_TECHNIQUE;

// MAGIC
if (path.contains("staff") || path.contains("wand") || path.contains("spell")) return StatType.ARCANE_POWER;

// Default reste BRUTE_FORCE
```

### B3. `ProgressionRouting.combatStatForWeaponCategory()` — Ajouter KODACHI + SCYTHE + CLUB + STAFF

```java
case "KODACHI" -> StatType.RAPIDITE;  // ou BLADE_TECHNIQUE
case "SCYTHE" -> StatType.BRUTE_FORCE;
case "CLUB" -> StatType.BRUTE_FORCE;
case "STAFF", "WAND" -> StatType.ARCANE_POWER;
case "GAUNTLET" -> StatType.AGILITY;  // ou split BRUTE+AGIL
```

---

## Partie C — Fallback dans EpicFightCompat (quand cap est null)

Actuellement dans `EpicFightCompat.onDealDamage()` (ligne 182-206) :
```java
CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(used);
if (cap != null) {
    WeaponCategory cat = cap.getWeaponCategory();
    // mapping XP...
} // si null → 0 XP via EpicFight
```

Fix — ajouter un `else` avec fallback :
```java
if (cap != null) {
    // mapping existant...
} else {
    // Fallback: path matching comme CombatXPHandler
    String path = BuiltInRegistries.ITEM.getKey(used.getItem()).getPath();
    if (matchesBlade(path)) leveled = addXp(player, StatType.BLADE_TECHNIQUE, dmg);
    else if (matchesBrute(path)) leveled = addXp(player, StatType.BRUTE_FORCE, dmg);
    else if (matchesPrecision(path)) leveled = addXp(player, StatType.PRECISION, dmg);
    else if (matchesArcane(path)) leveled = addXp(player, StatType.ARCANE_POWER, dmg);
    else leveled = addXp(player, StatType.BRUTE_FORCE, dmg/2);
}
```

---

## Partie D — Injection de Capabilities EpicFight (animations réelles)

### D1. Comprendre le système EpicFight

EpicFight utilise `CapabilityItem` pour déterminer les animations et le style de combat. Les capabilities sont soit :
- **Hardcodées** : pour les items vanilla (épée = SWORD, hache = AXE, etc.)
- **Data pack JSON** : fichiers dans `data/epicfight/epicfight_weapon_type/`
- **Runtime** : via `EpicFightCapabilities.getItemStackCapability()`

Pour les items moddés sans capability, EpicFight retourne soit null, soit une capability GENERIC (sans animations).

### D2. Approche recommandée : Data Pack JSON

Créer un fichier JSON qui mappe TOUS les items moddés aux bonnes catégories :

**`src/main/resources/data/epicfight/epicfight_weapon_type/statmod_tensura_weapons.json`**
```json
{
  "values": {
    "tensura:*great_sword": "greatsword",
    "tensura:*odachi": "greatsword",  
    "tensura:*scythe": "axe",
    "tensura:*katana": "katana",
    "tensura:*tachi": "tachi",
    "tensura:*kodachi": "dagger",
    "tensura:*short_sword": "sword",
    "tensura:*long_sword": "longsword",
    "tensura:*spear": "spear",
    "tensura:long_bow": "bow",
    "tensura:short_bow": "bow",
    "tensura:*sickle": "sword",
    "tensura:*sickle": "sword",
    "tensura:*club": "axe",
    "tensura:*staff": "sword",
    "tensura:*knuckle": "fist",
    "tensura:*gauntlet": "fist",
    "mahoutsukai:*staff": "sword"
  }
}
```

**Problème :** EpicFight ne supporte pas les patterns `*` glob dans ses data packs. Il faut soit :
a. Lister chaque item explicitement (trop long, ~150 entrées)
b. Utiliser un Mixin pour injecter les capabilities à chaud
c. Utiliser l'API EpicFight pour enregistrer les capabilities programmatiquement

### D3. Approche recommandée : Mixin + Register

Créer un mixin qui modifie `EpicFightCapabilities.getItemStackCapability()` :

```java
@Mixin(EpicFightCapabilities.class)
public class EpicFightCapabilityMixin {
    @Inject(method = "getItemStackCapability", at = @At("HEAD"), cancellable = true)
    private static void statmod$injectCapability(ItemStack stack, CallbackInfoReturnable<CapabilityItem> cir) {
        if (stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("tensura") || id.getNamespace().equals("mahoutsukai")) {
            WeaponCategory cat = WeaponResolver.epicFightCategoryFor(id.getPath());
            if (cat != null) {
                cir.setReturnValue(CapabilityItem.get(cat));
            }
        }
    }
}
```

Mais pour que ça marche, `CapabilityItem.get(WeaponCategory)` doit exister dans EpicFight. Si non, il faut utiliser le système d'enregistrement d'EpicFight.

### D4. Alternative : EpicFight ProvableItem API

EpicFight a une API pour enregistrer des capabilities via `EpicFightItemProvider` ou `ItemCapabilityProvider`. Vérifier si :

```java
EpicFightCapabilities.registerItemCapability(
    new ResourceLocation("tensura", "adamantite_great_sword"),
    () -> new BasicItemCapability(WeaponCategories.GREATSWORD)
);
```

Si cette API existe, on peut enregistrer TOUS les items Tensura/Mahou au démarrage dans `EpicFightCompat.init()`.

---

## Partie E — WeaponResolver.java (Classe Unifiée)

Nouvelle classe centrale utilisée par TOUS les handlers :

```java
public final class WeaponResolver {
    private WeaponResolver() {}

    // Mapping central : registry path → WeaponInfo
    private static final List<WeaponPattern> PATTERNS = List.of(
        // BRUTE_FORCE / melee_axe
        pattern("greatsword", BRUTE_FORCE, "melee_axe", GREATSWORD),
        pattern("great_sword", BRUTE_FORCE, "melee_axe", GREATSWORD),
        pattern("odachi", BRUTE_FORCE, "melee_axe", GREATSWORD),
        pattern("scythe", BRUTE_FORCE, "melee_axe", AXE),
        pattern("club", BRUTE_FORCE, "melee_axe", AXE),
        pattern("hammer", BRUTE_FORCE, "melee_axe", AXE),
        pattern("mace", BRUTE_FORCE, "melee_axe", AXE),
        pattern("axe", BRUTE_FORCE, "melee_axe", AXE),

        // BLADE_TECHNIQUE / melee_sword
        pattern("katana", BLADE_TECHNIQUE, "melee_sword", KATANA),
        pattern("kodachi", RAPIDITE, "melee_sword", DAGGER),
        pattern("tachi", BLADE_TECHNIQUE, "melee_sword", TACHI),
        pattern("dagger", BLADE_TECHNIQUE, "melee_sword", DAGGER),
        pattern("sickle", BLADE_TECHNIQUE, "melee_sword", SWORD),
        pattern("long_sword", BLADE_TECHNIQUE, "melee_sword", LONGSWORD),
        pattern("longsword", BLADE_TECHNIQUE, "melee_sword", LONGSWORD),
        pattern("short_sword", BLADE_TECHNIQUE, "melee_sword", SWORD),
        pattern("sword", BLADE_TECHNIQUE, "melee_sword", SWORD),

        // PRECISION / ranged
        pattern("throwing", PRECISION, "ranged", RANGED),
        pattern("spear", PRECISION, "ranged", SPEAR),
        pattern("bow", PRECISION, "ranged", RANGED),
        pattern("crossbow", PRECISION, "ranged", RANGED),
        pattern("trident", PRECISION, "ranged", TRIDENT),

        // ARCANE_POWER / magic
        pattern("staff", ARCANE_POWER, "magic_fire", SWORD),
        pattern("wand", ARCANE_POWER, "magic_fire", SWORD),
        pattern("spell", ARCANE_POWER, "magic_fire", SWORD),

        // FIST
        pattern("fist", BRUTE_FORCE, "melee_axe", FIST),
        pattern("knuckle", BRUTE_FORCE, "melee_axe", FIST),
        pattern("gauntlet", BRUTE_FORCE, "melee_axe", FIST)
    );

    public record WeaponInfo(StatType stat, String tensuraAction, WeaponCategory epicCategory) {}
    public record WeaponPattern(String keyword, StatType stat, String tensuraAction, WeaponCategory epicCategory) {}

    public static WeaponInfo resolve(ItemStack stack) { ... }
    public static StatType statFor(ItemStack stack) { ... }
    public static String tensuraActionFor(ItemStack stack) { ... }
    public static WeaponCategory epicCategoryFor(String path) { ... }
}
```

**Tous les handlers existants sont refactorés pour appeler `WeaponResolver` :**
- `CombatXPHandler.resolveWeaponStat()` → `WeaponResolver.statFor(stack)`
- `TensuraEpHandler.resolveActionType(ItemStack)` → `WeaponResolver.tensuraActionFor(stack)`
- `EpicFightCompat.onDealDamage()` → `WeaponResolver.resolve(stack)` pour le fallback
- `ProgressionRouting.combatStatForWeaponCategory()` → peut devenir `WeaponResolver.statForCategory(String)`

---

## Ordre d'Implémentation

```
Étape 1: Fix ordre dans TensuraEpHandler       (2 min)
Étape 2: Ajouter staff/wand dans CombatXPHandler (2 min)
Étape 3: Ajouter types manquants dans ProgressionRouting (2 min)
Étape 4: Créer WeaponResolver.java              (30 min)
Étape 5: Refactorer les 4 handlers → WeaponResolver (15 min)
Étape 6: Fallback EpicFightCompat quand cap null (10 min)
Étape 7: EpicFight capability injection (mixin ou API) (45 min)
Étape 8: Tests unitaires                        (20 min)
```

**Total: ~2h**

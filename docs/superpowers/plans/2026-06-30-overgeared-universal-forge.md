---
title: Overgeared Universal Forge — Implementation Plan
status: ready
phase: implementation
author: Onivo Studio runtime agent
date: 2026-06-30
mission: M5 (Overgeared expansion)
spec: docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md
decision: .tmp-onivo-audit/decisions/STAT-DEC-OVERGEARED-EXPANSION.md
---

# Overgeared Universal Forge — Implementation Plan

Suit la spec `2026-06-30-overgeared-universal-forge-design.md`. Découpé en 6 phases
livrables indépendamment ; chaque phase a ses own exit conditions et ses tests.

## Pré-requis bloquants

### B1 — Compile vert

Le compile est cassé sur 53 erreurs liées à une refacto `MagicNode.role()` → `condition()`
inachevée. Tant que ça ne build pas, on ne peut pas ajouter de nouveaux items en Java.

**Action requise (escalade humaine) :**
- soit revert `src/main/java/tong/statmod/magic/{MagicNode,MagicRace}.java` à `HEAD`
  (commit `2d7ffd6`)
- soit finir la refacto Condition (porte sur tous les call-sites : `MagicTreeCatalog`,
  `MagicNodeStatRequirements`, `MagicCatalogAuditTool`, `SpellRoleInferenceTest`, etc.)

**Décision provisoire studio :** revert recommandé. La refacto Condition est large, son
scope sort de la mission M5, et elle bloque tout le développement runtime.

### B2 — `libs/` présent

Confirmé : 56 jars dans `libs/`, dont les jars cibles (overgeared, tensura, irons_spellbooks,
magistuarmory, simplyswords, slu, …). Build classpath OK.

### B3 — Branche `neoforge-1.21.1`

Confirmé.

---

## Phase α — Materials Foundation

### Tâches

#### α.1 — Items registry

Créer `src/main/java/tong/statmod/item/ForgingMaterials.java` :

```java
public final class ForgingMaterials {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(STATMod.MODID);

    public static final DeferredItem<Item> HEATED_GOLD_INGOT =
            ITEMS.registerSimpleItem("heated_gold_ingot");
    // ... 13 autres
}
```

Et brancher l'enregistrement dans le constructeur `@Mod STATMod` :
```java
ForgingMaterials.ITEMS.register(modBus);
```

#### α.2 — Modèles & textures placeholder

Pour chaque `heated_*` :
- `src/main/resources/assets/statmod/models/item/heated_<name>.json`
  (`parent: "minecraft:item/generated"`, layer0 = texture du même nom)
- `src/main/resources/assets/statmod/textures/item/heated_<name>.png`
  (placeholder = copie de `assets/overgeared/textures/item/heated_iron_ingot.png`
   tintée selon la couleur du tableau dans la spec — script Python)

#### α.3 — Lang

Ajouter à `src/main/resources/assets/statmod/lang/en_us.json` et `fr_fr.json` :
```json
"item.statmod.heated_gold_ingot": "Heated Gold Ingot",
"item.statmod.heated_tin_ingot": "Heated Tin Ingot",
... etc
```
Idem en français.

#### α.4 — Recettes `minecraft:blasting`

14 fichiers dans `src/main/resources/data/statmod/recipe/heating/` :

```json
{
  "type": "minecraft:blasting",
  "category": "misc",
  "cookingtime": 200,
  "experience": 0.5,
  "ingredient": { "item": "tensura:hihiirokane_ingot" },
  "result": { "count": 1, "id": "statmod:heated_hihiirokane_ingot" }
}
```

CookingTime ajusté par tier :
- copper, tin, bronze : 100 ticks
- iron, silver, gold : 150
- steel, diamond, pyrium : 200
- arcane, mithril : 250
- magisteel, pure_magisteel : 300
- high_magisteel, orichalcum : 350
- adamantite, hihiirokane : 400, netherite : 350

#### α.5 — Recettes `overgeared:cooling` (retour heated → ingot)

14 fichiers dans `src/main/resources/data/statmod/recipe/cooling/`.

Le schéma exact à valider en inspectant Overgeared. Par défaut : `minecraft:campfire_cooking`
en fallback si `overgeared:cooling` n'expose pas un type recipe accessible.

#### α.6 — Gate table dans `OvergearedRecipeGate`

Étendre `OvergearedRecipeGate.java` :

```java
private static final Map<ResourceLocation, MaterialGate> MATERIAL_GATES = Map.ofEntries(
    Map.entry(ResourceLocation.parse("tensura:hihiirokane_ingot"),
              new MaterialGate(70, 30, 22)),
    Map.entry(ResourceLocation.parse("tensura:adamantite_ingot"),
              new MaterialGate(60, 25, 18)),
    // ... 14 entries
);

public static boolean canHeat(ResourceLocation ingotId, PlayerStatData data) {
    MaterialGate gate = MATERIAL_GATES.get(ingotId);
    if (gate == null) return true;
    return data.getStatLevel(StatType.FORGING.index) >= gate.forging()
        && data.getStatLevel(StatType.ERUDITION.index) >= gate.erudition()
        && data.getStatLevel(StatType.ARCANE_POWER.index) >= gate.arcanePower();
}

private record MaterialGate(int forging, int erudition, int arcanePower) {}
```

#### α.7 — Hook qui bloque heating si stats insuffisantes

Approche : mixin sur `AbstractFurnaceBlockEntity` ou `BlastFurnaceBlockEntity` qui
intercepte le check de recipe match et le rejette si `OvergearedRecipeGate.canHeat(...)`
retourne false.

```java
@Mixin(BlastFurnaceBlockEntity.class)
public abstract class HeatingGateMixin {
    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private void statmod$blockIfStatsInsufficient(..., CallbackInfoReturnable<Boolean> cir) {
        // si recipe = heating d'un metal gated et que le joueur le plus proche
        // n'a pas les stats, on cancel et on log feedback
    }
}
```

Ajouter le mixin à `src/main/resources/statmod.mixins.json`.

#### α.8 — Tests

`src/test/java/tong/statmod/integration/overgeared/MaterialGateTest.java` :

```java
@Test void copper_no_requirements()
@Test void hihiirokane_requires_full_stack()
@Test void allHeatedMaterials_haveGateEntry()
@Test void gateOrdering_isMonotonic()  // chaque tier > tier précédent
```

### Exit conditions α

- [ ] 14 items `heated_*` enregistrés et accessibles via `/give`
- [ ] Blast furnace produit le heated correspondant pour les 14 metals
- [ ] Joueur sans stats ne peut pas produire `heated_adamantite_ingot`
- [ ] Feedback chat visible "FORGING insuffisant: 60 requis"
- [ ] `./gradlew test` vert
- [ ] Décision record mis à jour avec exit_α=closed

### Estimation effort α

- α.1–α.3 (registry, modèles, lang) : 2h
- α.4–α.5 (recettes JSON) : 1h (script Python pour générer)
- α.6–α.7 (gates + mixin) : 3h
- α.8 (tests) : 1h
- Validation in-game : 1h

**Total Phase α : ~8h de focus dev**

---

## Phase β — Intermediates

### Tâches

#### β.1 — Items registry (84 ou 78 items)

`src/main/java/tong/statmod/item/ForgingIntermediates.java` :

Pattern :
```java
public static final DeferredItem<Item> ROUGH_BLADE_IRON =
        ITEMS.registerSimpleItem("rough_blade_iron");
public static final DeferredItem<Item> ROUGH_BLADE_HIHIIROKANE =
        ITEMS.registerSimpleItem("rough_blade_hihiirokane");
// ... 82 autres
```

Décision sous-arbitrage à prendre en début β : **collapse `low_magisteel` + `magisteel`
en un seul rough_*_magisteel** (économise 6 items, simplifie sans perdre identité).

#### β.2 — Grips & Blueprints (8 items)

`src/main/java/tong/statmod/item/ForgingGrips.java` :
- `WOODEN_GRIP`, `LEATHER_WRAP`, `WIRE_WRAP`, `RUNIC_GRIP`

`src/main/java/tong/statmod/item/ForgingBlueprints.java` :
- `BLUEPRINT_UNIVERSAL_BLADE`, `BLUEPRINT_UNIVERSAL_POLE`,
  `BLUEPRINT_RUNIC_BLADE`, `BLUEPRINT_LEGENDARY`

#### β.3 — Recettes `overgeared:forging`

~84 fichiers dans `src/main/resources/data/statmod/recipe/forging/<class>/` :

```json
{
  "type": "overgeared:forging",
  "blueprint": ["sword"],
  "category": "TOOL_HEADS",
  "hammering": 8,
  "key": { "#": { "item": "statmod:heated_hihiirokane_ingot" } },
  "pattern": ["#", "#"],
  "result": { "count": 1, "id": "statmod:rough_blade_hihiirokane" },
  "show_notification": false,
  "tier": "diamond"
}
```

Tier anvil (stone/iron/steel/diamond) déduit du tier matériau (table dans spec).
Hammering scale avec tier (3 copper → 12 hihiirokane).

#### β.4 — Recettes blueprints

`crafting_shaped` pour fabriquer les blueprints à partir de papier + traces matériau.

#### β.5 — Recettes grips

`crafting_shapeless` :
- wooden_grip : 2 sticks → 1 wooden_grip
- leather_wrap : 1 leather + 1 string → 1 leather_wrap
- wire_wrap : 1 iron_nugget + 1 string → 1 wire_wrap
- runic_grip : 1 leather + 1 amethyst_shard → 1 runic_grip

#### β.6 — Modèles & textures

Stratégie : utiliser `overgeared:rough_iron_blade` comme base, recoloriser par tint script
Python. Chaque item = ~1KB PNG + 200 octets JSON model.

#### β.7 — Lang (~92 entries × 2 langues)

#### β.8 — Tests

```java
@Test void allRoughIntermediates_haveForgingRecipe()
@Test void forgingTier_matchesAnvilTier()
@Test void blueprintRequirements_match_spec()
```

### Exit conditions β

- [ ] 84 intermédiaires rough_* enregistrés
- [ ] Smithing anvil produit chaque rough depuis le heated correspondant
- [ ] 4 grips craftables
- [ ] 4 blueprints craftables, gates respectés
- [ ] Mini-game Overgeared fonctionnel sur tous les nouveaux forges
- [ ] `./gradlew test` vert
- [ ] Décision record mis à jour exit_β=closed

### Estimation effort β

- β.1–β.2 (items 84+8) : 3h (long mais répétitif via script)
- β.3 (recettes forging) : 2h (script)
- β.4–β.5 (blueprints, grips) : 1h
- β.6 (textures placeholder) : 1h
- β.7 (lang) : 1h (script qui auto-fabrique l'entrée depuis l'id)
- β.8 (tests) : 2h
- Validation in-game : 2h

**Total Phase β : ~12h**

---

## Phase γ — Universal Assembly (script-driven)

### Tâches

#### γ.1 — Script Python `tools/generate_assembly_recipes.py`

Logique :
1. Parse `libs/*.jar`, extraire pour chaque mod : modid + liste items
2. Pour chaque item ID dans `<modid>:<path>` :
   - Match path contre regex famille : `sword|katana|rapier|cutlass|saber → blade`,
     `axe|hammer|mace|warhammer → axe_head`, etc.
   - Match path contre regex matériau : `iron_ → iron`, `adamantite_ → adamantite`, etc.
   - Si pas de match : skip
3. Générer `data/statmod/recipe/assembly/<modid>/<item>.json` :
   ```json
   {
     "type": "minecraft:crafting_shapeless",
     "category": "equipment",
     "ingredients": [
       { "item": "statmod:rough_blade_adamantite" },
       { "item": "statmod:wooden_grip" }
     ],
     "result": { "count": 1, "id": "tensura:adamantite_long_sword" }
   }
   ```
4. Maintenir une whitelist Tensura `tools/tensura_whitelist.txt` (vide par défaut)
5. Output stats : nombre de recettes générées par mod

#### γ.2 — Gradle task `generateAssemblyRecipes`

```gradle
tasks.register('generateAssemblyRecipes', Exec) {
    group = 'statmod'
    description = 'Generate ~770 assembly recipes from libs/ scan'
    workingDir = project.projectDir
    commandLine 'python', 'tools/generate_assembly_recipes.py'
}
processResources.dependsOn generateAssemblyRecipes
```

#### γ.3 — Hook `ItemCraftedEvent` qui revalide FORGING

`src/main/java/tong/statmod/integration/overgeared/AssemblyForgingGate.java` :

```java
@SubscribeEvent
public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
    ItemStack result = event.getCrafting();
    ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
    int required = OvergearedRecipeGate.requiredForgingLevel(resultId);
    if (required == 0) return;
    Player player = event.getEntity();
    int forging = player.getData(ModAttachments.STATS).getStatLevel(StatType.FORGING.index);
    if (forging < required) {
        // Revert craft : return items to inventory
        // Feedback chat
    }
}
```

Subtilité : `ItemCraftedEvent` est post-craft, donc on doit retirer le résultat + rendre
les ingrédients. Alternativement, on intercepte au niveau du `RecipeManager` via mixin.
À trancher à l'écriture (mixin si revert pose problème).

#### γ.4 — Tests

```java
@Test void allMods_yieldAtLeastOneRecipe()
@Test void noRecipe_targetsTensuraOutsideWhitelist()
@Test void noRecipe_overridesExistingModRecipe()
```

### Exit conditions γ

- [ ] Script génère ≥ 600 recettes assembly
- [ ] Au moins 5 armes de chaque mod cible présentes
- [ ] FORGING gate hook bloque correctement
- [ ] `./gradlew test` vert
- [ ] Validation in-game : crafter une simplyswords:netherite_cutlass (FORGING 50 req)

### Estimation effort γ

- γ.1 (script Python) : 4h
- γ.2 (gradle task) : 1h
- γ.3 (hook ItemCrafted) : 2h
- γ.4 (tests) : 1h
- Validation in-game : 2h

**Total Phase γ : ~10h**

---

## Phase δ — Magic Forge (option, plus tard)

(non détaillé pour cette session — voir spec §Phase δ)

## Phase ε — Essences (option, plus tard)

(non détaillé pour cette session — voir spec §Phase ε)

## Phase ζ — Race + Perk Crossover (option, plus tard)

(non détaillé pour cette session — voir spec §Phase ζ)

---

## Ordre d'exécution recommandé

1. **B1 — Débloquer compile** (escalade humaine, 5 min décision)
2. **Phase α — Materials Foundation** (~8h)
3. ✋ checkpoint utilisateur — playtest in-game des 14 metals
4. **Phase β — Intermediates** (~12h)
5. ✋ checkpoint utilisateur — playtest forge des 6 classes d'armes
6. **Phase γ — Universal Assembly** (~10h)
7. ✋ checkpoint utilisateur — playtest crafting d'armes externe
8. Phases δ/ε/ζ optionnelles selon retour

**Total Phase α+β+γ : ~30h de focus dev**.

## Critères de promotion entre phases

Une phase ne passe à la suivante que si :
- Tous ses tests passent (`./gradlew test`)
- L'utilisateur a validé un playtest minimum 30min
- Aucun TODO bloquant n'est laissé dans le code

## Suivi

Décision record `STAT-DEC-OVERGEARED-EXPANSION.md` met à jour son `phase_state` à chaque
transition. Chaque phase shipped → un commit conventionnel `feat(forge): phase α / …`.

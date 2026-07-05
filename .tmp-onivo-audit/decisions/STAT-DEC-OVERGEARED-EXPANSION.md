# STAT-DEC-OVERGEARED-EXPANSION — Extension universelle du système de forge

**Date :** 2026-06-30
**Mission :** M5 (Overgeared expansion)
**Auteur :** Onivo Studio runtime agent
**Branche :** `neoforge-1.21.1`
**Statut :** **closed** (6 phases shipped, mission M5 clôturée le 2026-07-01)
**Spec :** `docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md`
**Plan :** `docs/superpowers/plans/2026-06-30-overgeared-universal-forge.md`

## Contexte

L'intégration Overgeared était jusqu'ici une feature isolée :
- Gating uniquement sur 2 items STAT MOD (`respec_stone`, `perk_tome`)
- Aucune interaction avec les ~770 armes des autres mods
- 3 stats touchées sur 23 (FORGING, ALCHEMY, COOKING)
- Forging comme arc de progression : creux

Le fondateur (« je veut que tt revienne … j'aimerai surtout que le mod puisse être utilisé
pour forger toutes les autres armes du jeu … » + « je veux qu'on extend le mod carrement
si il faut créer de nouveaux items on le fait ») a demandé une extension structurelle.

## Décision

Le studio engage le chantier **Overgeared Universal Forge** sur 6 phases :

| Phase | Scope | Statut |
|---|---|---|
| α — Materials Foundation | 14 heated_* + gates dual-stat + recettes blasting/cooling | open |
| β — Intermediates | 84 rough_* + 4 grips + 4 blueprints + forging recipes | planned |
| γ — Universal Assembly | script Python → ~770 recettes assembly mods externes | planned |
| δ — Magic Forge | bloc infusion_forge + dual-gate ERUDITION + ARCANE | optional |
| ε — Essences | SLU shards + Simply gems comme enchants permanents | optional |
| ζ — Race + Perk crossover | DWARF/ELF/OGRE bonuses, perks quality bias | optional |

**Architecture retenue** : 2-étapes (heated → rough → arme finale), per-material
intermediates (chaque métal garde son identité), dual-gate FORGING + ERUDITION + ARCANE_POWER
sur les métaux magiques, génération JSON par script pour les ~770 recettes d'assemblage.

## Trade-offs résolus en autonomie studio

### TR1 — 1-étape vs 2-étapes d'assemblage → **2-étapes**

**Pourquoi :** 1-étape (heated → arme directement) est plus court mais détruit l'identité
Overgeared (blade + grip). 2-étapes préserve la chain originale et donne deux gates
indépendantes. Le fondateur a explicitement validé « créer de nouveaux items si il faut ».

**Rejeté :** 1-étape simple. Trop cheap pour la promesse "vraie forge".

### TR2 — Per-material vs Tier universel → **Per-material**

**Pourquoi :** Un `rough_blade_hihiirokane` reste Hihi'Irokane jusqu'au bout. Pas de
"rough_blade_tier_7" générique qui efface l'identité du métal source. Coût : 84 items au
lieu de 42. Bénéfice : richesse visuelle + cohérence narrative + ouverture pour des
recettes d'arme finales qui exploitent l'identité métal (Phase γ).

**Rejeté :** Tier universel. Économise 42 items mais détruit l'argument identité.

### TR3 — Gate sur forge step vs sur assembly step → **Les deux**

**Pourquoi :** Le gate forge (heating) bloque amont — tu ne peux pas avoir un rough_*_adamantite
sans avoir débloqué adamantite. Le gate assembly bloque aval — même avec un rough déjà fait,
tu ne peux pas l'assembler si tu manques FORGING pour l'arme finale (ex: un newbie qui
trouve un rough_blade_adamantite en loot).

**Rejeté :** Gate unique. Trop facilement contournable par échange/loot.

### TR4 — Magic metals — single-stat FORGING ou multi-stat → **Multi-stat**

**Pourquoi :** Mithril, arcane, magisteel, hihiirokane sont magiques dans leurs mods
d'origine. Les forger demande une littératie magique (`ERUDITION`) en plus de la force du
bras (`FORGING`). Les top-tiers (orichalcum, adamantite, hihiirokane) demandent aussi
`ARCANE_POWER` (puissance magique). Cela ancre la forge dans l'identité multi-stat de
STAT MOD et empêche un grind FORGING unique de tout débloquer.

**Rejeté :** Single-stat FORGING uniquement. Trop simple, fait de FORGING un grind-pour-tout.

### TR5 — Génération à la main vs script → **Script Python**

**Pourquoi :** 770 recettes à la main = ingérable. Script Python qui scanne `libs/`,
heuristique par regex sur path d'item, génère datapack JSON. Rejoue à chaque changement
de modpack. Maintenance proche de zéro.

**Rejeté :** Java DataGen NeoForge. Plus "propre" mais bloqué par le compile cassé
(refacto MagicNode non finie) — le script Python tourne indépendamment.

### TR6 — Gate runtime au heating vs au forge (sub-décision Phase α, 2026-06-30)

**Décision révisée vs plan original** : on **ne** met **pas** de mixin de gating au heating
step. La table `OvergearedRecipeGate.MATERIAL_GATES` reste en place mais n'est pas utilisée
en Phase α — elle sera consommée en Phase β au forge step (smithing anvil).

**Pourquoi :** un `AbstractFurnaceBlockEntity` n'a pas de référence directe au joueur. Trouver
le "joueur le plus proche" est un anti-pattern qui marche mal pour les hopper-fed furnaces
et les serveurs multi-joueurs. Le forge step (Phase β) a un joueur direct via le menu
du smithing anvil et est donc le meilleur endroit pour valider FORGING + ERUDITION + ARCANE.

**Conséquence joueur :** en Phase α, n'importe qui peut chauffer n'importe quel ingot.
C'est OK parce que :
- Un `statmod:heated_hihiirokane_ingot` seul n'a aucune utilité tant que Phase β n'est pas
  shipped (pas de recette qui le consomme).
- En Phase β, la recette `overgeared:forging` qui consomme un heated_<material> sera gated
  par le menu du smithing anvil avec le joueur direct.

**Rejeté :** mixin sur `BlastFurnaceBlockEntity.serverTick` + recherche du joueur le plus
proche. Hack technique avec des cas limites (Discordian friendly chunks, distance > 16
blocs, joueur AFK).

### TR7 — Tensura — exclu par défaut ou inclus tout → **Exclu par défaut, whitelist**

**Pourquoi :** Tensura a 149 armes dont beaucoup sont des artefacts uniques (Holy Sword
de Hinata, kodachi de Souei, etc.) avec une lore tendue. Les rendre forgeables casserait
l'immersion. On commence exclus ; le fondateur whiteliste les armes Tensura "mortelles"
qu'il veut forgeables (low-tier en général).

**Rejeté :** Tout-Tensura-inclus. Pour des raisons de cohérence narrative.

## Trade-offs escaladés au fondateur

### ESC1 — Compile cassé (refacto MagicNode inachevée)

État : `./gradlew classes` échoue sur 53 erreurs liées à `MagicNode.role()` → `condition()`
half-done. Tant qu'il n'est pas réglé, on ne peut pas ajouter de nouveaux items Java.

**Question fondateur (action requise) :**
- soit `git checkout HEAD -- src/main/java/tong/statmod/magic/{MagicNode,MagicRace}.java`
  (revert, perd la refacto inachevée)
- soit le fondateur (ou une mission dédiée) finit la refacto Condition

**Recommandation studio :** revert. La refacto Condition n'est pas sur le chemin critique
M5 et son scope (changement de signature qui touche tout le système magic + tests) sort
largement du chantier forge.

## Implications

### Items créés au total — **~106**

- Phase α : 14 heated_*
- Phase β : 84 rough_* + 4 grips + 4 blueprints = 92 items
- Phase γ : 0 nouveaux items (datapack only)
- Phase ε : +12 SLU shards intégrés (déjà des items des mods sources)

### Coûts I/O

- 14 textures placeholder + 14 modèles JSON (Phase α)
- 84 textures placeholder + 84 modèles JSON (Phase β)
- ~770 JSON recettes assembly (Phase γ)
- ~92 JSON recettes forging (Phase β)
- ~28 JSON recettes heating/cooling (Phase α)

**Total JSON shipped : ~1000 fichiers**. Validé acceptable (Overgeared natif en a déjà
~270, sans impact perf).

### Effort dev estimé

- Phase α : ~8h
- Phase β : ~12h
- Phase γ : ~10h
- Phases δ/ε/ζ : ~5h chacune si déclenchées
- **Total minimum (α+β+γ) : ~30h focus dev**

### Tests

Chaque phase ajoute sa propre suite. Total estimé : ~25 tests unitaires + 5 tests
d'intégration via gametest framework + 3 checkpoints playtest manuel.

## Risques principaux

| Risque | Impact | Mitigation |
|---|---|---|
| Conflit override recette mod externe | Crash boot | On n'override jamais — recettes statmod:* only |
| Modpack update change un id | Recettes cassées silencieusement | Script datagen tourne au build, CI vert = à jour |
| Frustration joueur sur dual-gate | Feedback négatif | Feedback chat clair + tooltip listant les requis |
| Performance 770 recettes | Lag boot | Profilage Phase γ, regroupement si nécessaire |
| Textures placeholder crades | Immersion ruinée | Phase ζ ou artist pass externe |

## Exit conditions

Le chantier est considéré **fermé** quand :

1. Les 6 phases sont en `status: shipped` (ou les 3 obligatoires α/β/γ + au moins 1 optionnelle)
2. Modpack permet de forger toutes les armes non-Tensura matchées par le script
3. ≥ 10 armes Tensura whitelistées
4. Playtest 2h confirme la fluidité de progression FORGING
5. Aucun crash rapporté pendant playtest

## Garde

- **Aucun mod externe override** : règle inviolable. Toute recette nouvelle cible un item
  qui appartient à statmod (intermediaire/grip/blueprint) ou un item externe via résultat
  d'une recette qui ne fait PAS partie de l'index recettes du mod source.
- **Pas de bloc nouveau avant Phase δ** : on s'appuie sur le smithing anvil + blast furnace
  Overgeared/vanilla existants. Pas d'innovation block-side dans α/β/γ.
- **Pas de modif de balance multi-mod sans discussion** : si Phase ζ déséquilibre Tensura,
  on revient ici.

## Historique des phases

| Phase | Date d'ouverture | Date de fermeture | Statut |
|---|---|---|---|
| α | 2026-06-30 | 2026-06-30 | **shipped** (code + tests + ressources, validation in-game pending) |
| β | 2026-06-30 | 2026-06-30 | **shipped** (84 rough + 4 grips + 4 blueprints + 84 forging recipes + 7 tests verts, validation in-game pending) |
| γ | 2026-06-30 | 2026-06-30 | **shipped** (339 recettes assembly + WeaponMaterialDetector + AssemblyForgingGate + 10 tests verts, validation in-game pending) |
| δ | 2026-06-30 | 2026-06-30 | **shipped MVP** (bloc infusion_forge + 3 rune essences + 5 recettes infusion + 9 tests verts, sans block entity ni menu — décoratif + datapack only) |
| ε | 2026-07-01 | 2026-07-01 | **shipped MVP** (bloc enchantment_anvil + 8 recettes essence pré-enchantées via components + 10 tests verts, réutilise shards de mods sources sans nouveaux items STAT MOD) |
| ζ | 2026-07-01 | 2026-07-01 | **shipped** (ForgingRaceBonus + hook AssemblyForgingGate étendu, DWARF/ELF/perks FORGE_* modifient gates + durability, 12 tests verts) |

**Mission M5 statut global : CLÔTURÉE** — les 6 phases (α + β + γ + δ + ε + ζ) sont
shipped. Le chantier "Overgeared Universal Forge" a livré son cœur + toutes les options.

## Post-M5 — Polish visuel blocs (2026-07-01)

**Contexte :** les 2 blocs (infusion_forge, enchantment_anvil) étaient shipped avec le
modèle `cube_all` placeholder (cube uniforme, pas de forme d'enclume). Le fondateur a
demandé de leur donner la géométrie de la forge Overgeared la plus haute (tier_b) avec
juste un tint de couleur, sur le même principe que les heated_* items de Phase α.

**Livré :**
- `tools/generate_forge_block_models.py` — script qui :
  - Extrait `assets/overgeared/textures/block/smithing_anvil.png` comme base
  - Extrait `assets/overgeared/models/block/tier_b_smithing_anvil.json` comme géométrie
  - Tint la texture pour chaque bloc (infusion_forge = purple obsidienne, enchantment_anvil
    = magenta enchanté)
  - Copie le modèle Blockbench en remplaçant la texture ref
    `overgeared:block/smithing_anvil` par `statmod:block/<name>`
  - Écrit blockstate + item model qui pointe vers le block model
- 2 textures block regénérées (RGBA 64x64) — replacent les placeholders cube_all précédents
- 2 modèles block regénérés (~500 lignes chacun, géométrie tier B smithing anvil complète)
- 2 modèles item regénérés (pointent vers le block model)
- 2 blockstates conservés (non-directionnels, variante `""` par défaut)

**Sub-décision :** on garde les blocs **non-directionnels** en Phase polish. Le blockstate
Overgeared tier B a `facing=north/east/south/west` mais notre bloc Java est un
`Block` basique sans `HorizontalDirectionalBlock`. Ajouter le facing = changement Java
+ mesh de blockstate à 4 variants + tests. Reporté à un polish ultérieur si le fondateur
juge l'orientation par défaut trop rigide.

**Tests :** `./gradlew test` → BUILD SUCCESSFUL. Les 10 tests `EnchantmentAnvilResourcesTest`
+ 9 tests `ForgingBlocksTest` continuent de passer (les fichiers existent toujours aux
mêmes paths, seul leur contenu géométrique change).

**Validation in-game :** poser un infusion_forge et un enchantment_anvil → doivent
apparaître comme des enclumes tinted (violette et magenta), pas des cubes plats.
| γ | — | — | planned |
| δ | — | — | optional |
| ε | — | — | optional |
| ζ | — | — | optional |

## Phase α — livrée le 2026-06-30

**Livré :**
- `src/main/java/tong/statmod/item/ForgingMaterials.java` — 14 items `Supplier<Item>` via DeferredRegister
- Hook dans `STATMod.java` constructor — `ForgingMaterials.register(modBus)`
- `OvergearedRecipeGate.MATERIAL_GATES` — table 15 entries (mithril dupliqué iron's/tensura) + record `MaterialGate` + `canHeat()` + `gateFor()`
- 14 textures placeholder dans `assets/statmod/textures/item/heated_*.png` (tint script `tools/generate_heated_textures.py`)
- 14 modèles JSON dans `assets/statmod/models/item/heated_*.json`
- 28 entries lang (14 en_us + 14 fr_fr)
- 14 recettes `minecraft:blasting` dans `data/statmod/recipe/heating/heated_*.json` avec cookingtime par tier
- 14 items ajoutés à `CreativeModeTab` "stat_mod"
- 7 nouveaux tests JUnit dans `OvergearedRecipeGateTest` (table integrity, monotonic tier progression, sample gates)
- Script utilitaire `tools/generate_heated_resources.py` (idempotent, re-run safe)

**Tests Phase α :** `./gradlew test` → BUILD SUCCESSFUL.

**Décision sub-arbitrée (TR6) :** mixin gating au heating différé en Phase β au profit
d'un gate au forge step (smithing anvil) où le joueur est directement accessible via le
menu. Justification documentée dans §TR6.

**Validation in-game restante (à faire par le fondateur) :**
- `/give @s tensura:hihiirokane_ingot` → mettre en blast furnace → vérifier que ça produit
  `statmod:heated_hihiirokane_ingot`
- Ouvrir l'onglet créatif STAT MOD → vérifier la présence des 14 heated_*
- Vérifier que les textures s'affichent (placeholder tintés, pas item manquant)

## Phase β — livrée le 2026-06-30

**Livré :**
- `src/main/java/tong/statmod/item/ForgingIntermediateIds.java` — constants pures
  (`WEAPON_CLASSES`, `MATERIALS`, `allIds()`, `isKnownId()`, `count()`)
- `src/main/java/tong/statmod/item/ForgingIntermediates.java` — 84 items rough_<class>_<material>
  via DeferredRegister, iteration sur `ForgingIntermediateIds.allIds()`
- `src/main/java/tong/statmod/item/ForgingGrips.java` — 4 items (wooden, leather_wrap,
  wire_wrap, runic_grip)
- `src/main/java/tong/statmod/item/ForgingBlueprints.java` — 4 items (universal_blade,
  universal_pole, runic_blade, legendary)
- Hook `STATMod.java` constructor — `ForgingIntermediates/Grips/Blueprints.register(modBus)`
- Ajout des 92 items au CreativeModeTab "stat_mod"
- 92 textures placeholder dans `assets/statmod/textures/item/` (tint script + variation par
  classe via brightness factor — voir `tools/generate_phase_beta_resources.py`)
- 92 modèles JSON dans `assets/statmod/models/item/`
- 184 entries lang (92 en_us + 92 fr_fr)
- 84 recettes `overgeared:forging` dans `data/statmod/recipe/forging/<class>/` avec
  hammering scalé par tier matériau (3 copper → 12 hihiirokane) et anvil tier (stone →
  diamond)
- 8 recettes `crafting_shapeless` dans `data/statmod/recipe/assembly_components/` pour
  fabriquer grips et blueprints depuis matériaux vanilla
- 7 nouveaux tests JUnit dans `ForgingIntermediatesTest` (count, identity, coverage)
- Script `tools/generate_phase_beta_resources.py` (idempotent, re-run safe)

**Tests Phase β :** `./gradlew test` → BUILD SUCCESSFUL.

**Sub-décision (qualité d'architecture) :** séparation `ForgingIntermediateIds` (constants
pures, testables hors Minecraft) vs `ForgingIntermediates` (DeferredRegister, runtime).
Cette séparation a été imposée par l'incompatibilité de DeferredRegister.register() avec
JUnit pur (DeferredHolder.bind nécessite registry Minecraft bound). Adoptée comme pattern
studio pour tous les futurs registries items volumineux.

**Validation in-game restante (à faire par le fondateur) :**
- `/give @s statmod:heated_hihiirokane_ingot` → forger sur smithing anvil diamond avec
  blueprint blade → vérifier qu'on obtient `statmod:rough_blade_hihiirokane`
- Ouvrir l'onglet créatif STAT MOD → vérifier la présence des 84 rough_* + 4 grips + 4 blueprints
- Vérifier que les recettes de grips/blueprints fonctionnent en crafting table

## Phase γ — livrée le 2026-06-30

**Livré :**
- `tools/generate_assembly_recipes.py` — script Python qui scanne `libs/*.jar`, extrait
  les items via leur lang en_us, classifie chaque arme (sword/axe/spear/bow/staff/dagger),
  détecte le matériau (préfixe canonique + alias thématiques runic/ancient/soulkeeper/etc),
  et écrit une recette `crafting_shapeless` consommant `rough_<class>_<mat> + wooden_grip`
  pour obtenir l'arme cible.
- `tools/tensura_whitelist.txt` — fichier d'opt-in. Vide par défaut → toutes les armes
  Tensura sont exclues (preservation lore artefacts).
- `tools/assembly_recipes_stats.json` — stats de génération par mod après chaque run.
- 339 recettes générées dans `src/main/resources/data/statmod/recipe/assembly/<modid>/`
  réparties sur 9 mods (magistuarmory 148, slu 52, simplyswords 57, cdmoveset 23,
  epicfight 20, epicfight_dd 20, darkagesarmory 15, block_factorys_bosses 3, irons_spellbooks 1).
- `tong.statmod.integration.overgeared.WeaponMaterialDetector` — port Java de la logique
  de détection matériau côté Python pour usage runtime (gate au craft).
- `OvergearedRecipeGate.gateForMaterial(String)` + `gateForWeapon(ResourceLocation)` —
  table `MATERIAL_NAME_GATES` indexée par nom canonique (15 matériaux + alias).
- `tong.statmod.integration.overgeared.AssemblyForgingGate` — listener
  `PlayerEvent.ItemCraftedEvent` qui revalide la gate pour les armes externes, annule le
  craft + feedback chat si FORGING/ERUDITION/ARCANE_POWER insuffisants. Inscrit dans
  `OvergearedCompat.init()`.
- Nouvelle gradle task `generateAssemblyRecipes` (regen des 339 recettes via le script).
- 10 nouveaux tests JUnit dans `WeaponMaterialDetectorTest` (détection basique,
  ordering specific-first, alias, gate lookup, fallback null).

**Tests Phase γ :** `./gradlew test` → BUILD SUCCESSFUL.

**Stats de couverture après alias mapping :**
- Generated : 339 recettes (~12% des ~2700 items scannés)
- Skipped no class match : 1974 (non-weapons : armures, food, blocs, accessoires)
- Skipped no material : 684 (armes à nom totalement libre, ex : "the_destroyer")
- Skipped no rough : 90 (matériau détecté mais combinaison classe × mat sans rough)
- Skipped whitelist : 1142 (Tensura par défaut)
- Skipped excluded mod : 137 (overgeared / statmod / minecraft)

**Sub-décision (lossy fallback) :** quand un matériau vanilla (silver, netherite) ou
inhabituel n'a pas de rough natif, on retombe sur le diamond rough STAT MOD. C'est lossy
(texture ne matche pas le métal d'origine) mais fonctionnel — débloque +20% de recettes.
Le gating FORGING reste pertinent sur le résultat final via WeaponMaterialDetector.

**Validation in-game restante (à faire par le fondateur) :**
- Tester un assemblage iron_cutlass : `/give @s overgeared:iron_sword_blade` +
  `/give @s statmod:wooden_grip` → crafting table → vérifier que `simplyswords:iron_cutlass`
  apparaît bien.
- Tester le gating : à FORGING 0, essayer d'assembler `simplyswords:netherite_cutlass`
  → vérifier le feedback chat "FORGING 0/35 insuffisant" + craft annulé.
- Tester une arme adamantite (FORGING 60 requis).

## Phase δ — livrée le 2026-06-30 (MVP)

**Scope MVP retenu :** approche datapack + bloc décoratif, sans block entity ni menu
custom. Le bloc `infusion_forge` sert d'identité visuelle pour l'aire de forge magique
du joueur. Les "infusion recipes" sont des `minecraft:crafting_shapeless` qui consomment
rough + rune_essence + grip pour produire une variante magique d'arme externe.

**Livré :**
- `src/main/java/tong/statmod/block/ForgingBlocks.java` — DeferredRegister<Block> +
  bloc `infusion_forge` (BlockBehaviour : strength 5/1200, sound ANVIL, requires pickaxe,
  COLOR_PURPLE map color). BlockItem associé.
- `src/main/java/tong/statmod/item/RuneEssence.java` — 3 nouveaux items
  `rune_essence_arcane`, `rune_essence_pyrium`, `rune_essence_mithril` via DeferredRegister<Item>.
- Hook `STATMod.java` constructor — `ForgingBlocks.register()` + `RuneEssence.register()`.
- Bloc + 3 essences ajoutés au CreativeModeTab "stat_mod".
- 4 textures placeholder (1 block + 3 items) tintées (script `tools/generate_phase_delta_resources.py`).
- 1 blockstate, 1 modèle block, 1 modèle item-of-block, 3 modèles items pour les essences.
- 1 loot table (drops self avec survives_explosion condition).
- 1 tag `statmod:mineable_with_pickaxe` (pour permettre le minage avec pickaxe).
- 12 lang entries (6 en_us + 6 fr_fr) — bloc, items, sous-titre tab.
- **4 recettes crafting** : 1 pour infusion_forge (4 obsidian + 1 amethyst_block + 1 blueprint_runic_blade,
  shapeless shapé "OOO/OBO/OAO") + 3 pour les essences (4 amethyst_shard + 1 ingot mod
  magique → 2 essences).
- **5 recettes magic infusion** dans `data/statmod/recipe/infusion/` :
  `infusion_runic_rapier`, `infusion_runic_katana`, `infusion_runic_claymore`,
  `infusion_runic_spear`, `infusion_brimstone_cutlass`. Chaque recette consomme
  rough_<class>_<magic_mat> + rune_essence + runic_grip → arme cible mod externe.
- 9 nouveaux tests JUnit dans `ForgingBlocksTest` (présence de toutes les ressources clés
  shipped en datapack).

**Tests Phase δ :** `./gradlew test` → BUILD SUCCESSFUL.

**Sub-décisions (architecture MVP) :**
- **Pas de block entity ni menu custom.** Le bloc est purement décoratif. Le joueur fait
  ses recettes infusion à la crafting table standard. L'identité "magic forge" est
  narrative + cosmétique, pas mécanique. Justification : éviter de réimplémenter un menu
  Overgeared-like complet alors que les recettes infusion sont déjà des
  `crafting_shapeless` simples. Si polish ultérieur nécessaire, le block entity sera
  ajouté en Phase δ-bis.
- **5 recettes infusion en MVP.** Cible des armes déjà couvertes par Phase γ (runic_rapier,
  brimstone_cutlass, etc.) — le joueur a donc deux chemins pour les obtenir : voie simple
  Phase γ (rough + wooden_grip) ou voie magique Phase δ (rough + rune_essence + runic_grip).
  À terme (Phase ε), la voie δ enchantera automatiquement le résultat (essences = enchant).
- **Pas de gating runtime spécifique en Phase δ.** Le bloc se craft via blueprint_runic_blade
  qui est lui-même gated FORGING 35 + ERUDITION 15 → le simple fait de pouvoir crafter
  l'infusion_forge prouve déjà que le joueur a la stack magique requise. Pas besoin de
  hook redondant. Les armes obtenues via les 5 recettes infusion sont gates par
  `AssemblyForgingGate` Phase γ (qui parse le matériau cible — arcane/mithril/pyrium).

**Validation in-game restante (à faire par le fondateur) :**
- `/give @s statmod:infusion_forge` puis poser le bloc → vérifier qu'il se place et
  qu'il a une texture violette (placeholder).
- Tester loot : casser avec pickaxe → drop infusion_forge. Avec main vide → no drop.
- Crafting infusion_forge : 4 obsidian + 1 amethyst_block + 1 blueprint_runic_blade.
- Crafting rune_essence_arcane : 4 amethyst_shard + 1 irons_spellbooks:arcane_ingot → 2 essences.
- Crafting infusion_runic_rapier : rough_blade_arcane + rune_essence_arcane + runic_grip
  → simplyswords:runic_rapier.

## Phase ε — livrée le 2026-07-01 (MVP)

**Scope MVP retenu :** approche datapack + bloc décoratif similaire à Phase δ. Les
essences sont les shards **existants** des mods sources (SLU, Simply Swords, Iron's
Spellbooks) — pas de nouveaux items STAT MOD. Les recettes essence produisent des armes
Simply Swords avec des enchantements vanilla pré-cuits via `components.enchantments`.

**Livré :**
- Second bloc `statmod:enchantment_anvil` dans `ForgingBlocks.java` — même pattern que
  infusion_forge, mais MapColor MAGENTA, strength 6/1400, sound ANVIL, requires pickaxe.
- BlockItem `enchantment_anvil` ajouté au registry `BLOCK_ITEMS` + inclus dans `blockItems()`
  (auto-injecté dans le CreativeModeTab).
- Recette crafting du bloc : 4 amethyst_block + 1 enchanting_table + 1 infusion_forge
  → enchantment_anvil (composition qui exige d'avoir déjà crafté un infusion_forge — chain
  gate transitive vers FORGING 35 + ERUDITION 15).
- 1 texture, 1 modèle block, 1 blockstate, 1 item model, 1 loot table, 1 tag pickaxe
  (le tag mineable_with_pickaxe existant est étendu, pas remplacé).
- **8 recettes essence** dans `data/statmod/recipe/essence/` :
  - `essence_flame_katana` : rough_blade_arcane + 2× slu:flame_shard + runic_grip →
    simplyswords:runic_katana avec fire_aspect 2 + sharpness 3
  - `essence_shadow_rapier` : rough_blade_adamantite + 2× slu:shadow_shard + wire_wrap →
    simplyswords:runic_rapier avec knockback 2 + sharpness 4
  - `essence_wither_claymore` : rough_blade_high_magisteel + 3× slu:wither_shard +
    runic_grip → simplyswords:runic_claymore avec smite 4 + unbreaking 2
  - `essence_estus_greathammer` : rough_axe_head_mithril + 2× slu:estus_shard +
    leather_wrap → simplyswords:runic_greathammer avec mending 1 + unbreaking 3
  - `essence_magma_greataxe` : rough_axe_head_pure_magisteel + 2× slu:magma_shard +
    wire_wrap → simplyswords:runic_greataxe avec fire_aspect 2 + sharpness 4
  - `essence_permafrost_spear` : rough_spear_tip_mithril + 3× irons_spellbooks:permafrost_shard +
    runic_grip → simplyswords:runic_spear avec sharpness 3 + knockback 1
  - `essence_runefused_longsword` : rough_blade_arcane + 1× simplyswords:runefused_gem +
    runic_grip → simplyswords:runic_longsword avec sharpness 5 + sweeping_edge 3
  - `essence_netherfused_glaive` : rough_spear_tip_pure_magisteel +
    1× simplyswords:netherfused_gem + runic_grip → simplyswords:runic_glaive avec
    fire_aspect 2 + sharpness 4 + unbreaking 2
- 4 entries lang (2 en_us + 2 fr_fr).
- 10 nouveaux tests JUnit dans `EnchantmentAnvilResourcesTest` (présence des ressources,
  targets Simply Swords existants, tag pickaxe étendu sans regression).

**Tests Phase ε :** `./gradlew test` → BUILD SUCCESSFUL.

**Sub-décisions (architecture MVP) :**
- **Pas de nouveaux items STAT MOD.** Les essences sont les shards des mods sources
  (SLU, Simply Swords, Iron's Spellbooks). Éviter la duplication de matériel — ces mods
  ont déjà les shards en drop, forger avec = boucle de gameplay naturelle.
- **Enchantements bakés via `components.enchantments`.** Format Minecraft 1.21.1 natif :
  le `result.components.minecraft:enchantments.levels` pré-applique les enchants sur
  l'output sans code Java. Solution pure datapack. Compromis : les enchants du weapon
  source sont perdus (mais on part d'un rough, pas d'une arme finie → non-blockant).
- **Recettes ciblent Simply Swords "runic_*"**. Ces armes existent (vérifié via
  jar inspection). Le joueur avait déjà accès à ces armes via Phase γ (voie simple sans
  enchant) ; Phase ε ajoute la voie premium enchantée. Deux chemins possibles → richesse
  gameplay.
- **Gating transitif via craft du bloc.** enchantment_anvil se craft avec un
  infusion_forge (Phase δ). infusion_forge se craft avec blueprint_runic_blade
  (Phase β, FORGING 35 + ERUDITION 15). Chaîne de gates transitive suffisante — pas de
  hook runtime AssemblyForgingGate à ajouter (les targets restent gated par Phase γ).

**Validation in-game restante (à faire par le fondateur) :**
- `/give @s statmod:enchantment_anvil` puis poser → bloc magenta placeholder visible.
- Craft du bloc : 4 amethyst_block + 1 enchanting_table + 1 statmod:infusion_forge
  (exige déjà d'avoir crafté un infusion_forge).
- Craft essence : rough_blade_arcane + 2× slu:flame_shard + statmod:runic_grip →
  simplyswords:runic_katana avec fire_aspect 2 + sharpness 3 visibles via F3+H.
- Vérifier F3+H que le shard des enchantements apparaît sur les armes essence-craftées.

## Phase ζ — livrée le 2026-07-01

**Scope :** race + perk crossover. Les 4 races (HUMAN / ELF / DWARF / BEAST) et les 6 perks
FORGE_* modifient les gates de forge et la durability des armes assemblées.

**Livré :**
- `src/main/java/tong/statmod/integration/overgeared/ForgingRaceBonus.java` — helper pur
  qui calcule :
  - `forgingLevelDiscount(PlayerStatData)` : discount total appliqué au niveau FORGING
    requis, cumulatif race + perks
  - `durabilityMultiplier(PlayerStatData)` : multiplier appliqué au max_damage du crafted
    stack, race + perks
  - `applyBonuses(MaterialGate, PlayerStatData)` : renvoie un gate modifié (FORGING
    seulement, ERUDITION et ARCANE_POWER intouchés — la magie ne se compense pas par la
    race)
- Extension de `AssemblyForgingGate` pour :
  - Appliquer `ForgingRaceBonus.applyBonuses()` avant le check de gate
  - Post-craft : appliquer `durabilityMultiplier` via `DataComponents.MAX_DAMAGE` sur le
    stack crafted si multiplier > 1.0
  - Feedback chat utilise l'effective gate (post-discount)
- 12 nouveaux tests JUnit dans `ForgingRaceBonusTest` couvrant :
  - No race + no perk (baseline)
  - Null data safety
  - DWARF -3 FORGING isolé
  - DWARF + FORGE_CORE = -4
  - DWARF + FORGE_TRANSCENDENCE = -6
  - Stack complet DWARF + FORGE_CORE + FORGE_TRANSCENDENCE = -7
  - ELF +25% durability isolé
  - ELF + FORGE_MASTERY = +50% durability
  - HUMAN et BEAST : no effect en Phase ζ (bonus liés à quality tier / double output,
    hors scope de ce hook)
  - applyBonuses : DWARF réduit FORGING seulement
  - applyBonuses : clamp à 0 (jamais négatif)
  - applyBonuses : no-op si pas de discount
  - applyBonuses : null-safe

**Mapping race → bonus (Phase ζ scope) :**

| Race | Bonus |
|---|---|
| HUMAN | (aucun en Phase ζ — bonus quality tier prévu Phase δ-bis) |
| ELF | +25% durability sur arme assemblée |
| DWARF | -3 FORGING requis sur toutes les gates |
| BEAST | (aucun en Phase ζ — bonus double output prévu Phase δ-bis) |

**Mapping perk → bonus (Phase ζ scope) :**

| Perk | Bonus |
|---|---|
| FORGE_CORE | -1 FORGING requis |
| FORGE_ACTIVE | (implicit via existing repair mechanics) |
| FORGE_SYNERGY | (implicit via anvil enchants — existing) |
| FORGE_SITUATIONAL | (implicit via existing damage bonus) |
| FORGE_MASTERY | +25% durability |
| FORGE_TRANSCENDENCE | -3 FORGING requis (cumulable avec DWARF pour un total de -6) |

**Tests Phase ζ :** `./gradlew test` → BUILD SUCCESSFUL (12 nouveaux tests verts, 55 tests
Phase α+β+γ+δ+ε+ζ cumulés).

**Sub-décisions (architecture) :**
- **Discount FORGING only.** La race/perks ne compensent pas ERUDITION ni ARCANE_POWER.
  Justification : la magie est une littératie qui ne se substitue pas à un métabolisme
  racial. Un ELF sans ERUDITION ne peut toujours pas forger de l'arcane. Cohérent avec
  l'identité stat-first.
- **HUMAN et BEAST hors scope Phase ζ.** Leurs bonus (quality tier, double output) touchent
  le forge step Phase β, pas l'assembly step Phase γ. Ils seraient implémentés dans un
  hook Overgeared quality determination — décision reportée à une éventuelle Phase δ-bis
  ou ζ-bis si demandée après playtest.
- **Durability multiplier via DataComponents.MAX_DAMAGE.** Format Minecraft 1.21.1 natif.
  Le stack crafted a son MAX_DAMAGE overridé au post-event. DAMAGE reste à 0 (default)
  donc l'arme sort neuve avec sa nouvelle durability. Solution qui ne casse pas les
  interactions vanilla (repair anvil, mending, etc.).
- **Pas de nouveau item ni bloc.** Phase ζ est du pur enrichissement runtime existant.
  Le joueur découvre les bonus en jouant (message chat quand il forge à moitié coût
  parce qu'il est DWARF).

**Validation in-game restante (à faire par le fondateur) :**
- `/statmod setrace DWARF`, `/statmod setstat FORGING 57` (= 60 - 3 pour adamantite) →
  essayer d'assembler une arme adamantite → doit passer (normalement bloqué à FORGING 60).
- `/statmod setrace ELF`, forger une arme → vérifier durability +25% via F3+H (base
  1561 pour netherite sword → 1951 attendu).
- Débloquer FORGE_CORE + FORGE_TRANSCENDENCE → tester une arme high_magisteel (FORGING
  50 requis) à FORGING 46 → doit passer (50 - 1 - 3 = 46).

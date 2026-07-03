---
title: Overgeared Universal Forge — Design Spec
status: active
phase: design
author: Onivo Studio runtime agent
date: 2026-06-30
mission: M5 (Overgeared expansion)
supersedes: none
references:
  - .opencode/plans/2026-06-16-multi-mod-integration-master-plan.md (master plan §6 Overgeared)
  - .tmp-onivo-audit/decisions/STAT-DEC-OVERGEARED-EXPANSION.md
  - docs/superpowers/specs/2026-06-18-stat-identity-design.md (identity foundation)
---

# Overgeared Universal Forge — Design Spec

## Vision en une phrase

**STAT MOD étend Overgeared pour faire de la forge un système universel qui produit toutes
les armes du modpack** (Simply Swords, Epic Knights, Tensura, SLU, etc.) à partir des
métaux de tous les mods (vanilla, magistuarmory, Iron's Spellbooks, Tensura), gated par
les stats `FORGING`, `ERUDITION` et `ARCANE_POWER`.

## Pourquoi maintenant

Avant aujourd'hui, l'intégration Overgeared se limitait à :
- Trois stats touchées (FORGING, ALCHEMY, COOKING)
- Un gating sur deux items STAT MOD seulement (`respec_stone`, `perk_tome`)
- Aucune interaction avec les ~770 armes des autres mods du pack

Le résultat : être blacksmith dans le mod, c'était grinder pour deux items. Aucun arc de
progression vers les armes des autres mods. Overgeared était une feature isolée, pas un
système d'identité.

**Le but : faire de la forge un vrai pilier de gameplay**, à hauteur de l'arbre magique
unifié. Forger devient une route d'identité du joueur, pas une mini-feature.

## Principes de design

### P1 — Identity-first, surface-second

STAT MOD reste l'autorité d'identité (FORGING level, perks, race). Overgeared reste le
moteur d'exécution UI (smithing anvil, mini-game qualité, ForgingQuality). On n'écrit
**aucun nouveau bloc/menu** : on étend par items + datapack + hooks runtime.

### P2 — Chaque métal garde son identité

Pas de "tier 5 générique". Un `heated_hihiirokane_ingot` reste Hihi'Irokane jusqu'au bout
de la chaîne, avec sa couleur, son nom, son lore. La forge ne sera pas un blender qui
écrase les particularités des mods sources.

### P3 — 2-étapes : Forge puis Assemble

```
Étape 1 — FORGE (smithing anvil, hammering, mini-game qualité)
  N× heated_<material>_ingot + blueprint_universal_blade
    → statmod:rough_<class>_<material>          (intermédiaire identitaire)

Étape 2 — ASSEMBLE (crafting table, shapeless)
  statmod:rough_blade_iron + statmod:wooden_grip
    → <modid>:iron_<weapon_name>                (arme finale du mod cible)
```

Cette structure respecte l'identité d'Overgeared (deux étapes claires) et donne deux
gates indépendantes (forge gating sur la production de `heated_*`, assembly gating sur le
choix de l'arme finale).

### P4 — Dual-gate pour métaux magiques

Les métaux non-physiques (mithril, arcane, magisteel, hihiirokane, orichalcum, adamantite)
requièrent à la fois `FORGING` **et** `ERUDITION` (+ `ARCANE_POWER` pour les top-tiers).
La justification narrative : ces métaux ne se laissent pas chauffer/forger par la simple
force du bras — il faut savoir lire leur essence.

| Métal | FORGING req | ERUDITION req | ARCANE_POWER req |
|---|---|---|---|
| copper, tin | 0 | 0 | 0 |
| iron, bronze, silver | 3 | 0 | 0 |
| steel, gold | 8 | 0 | 0 |
| diamond | 15 | 0 | 0 |
| pyrium | 12 | 0 | 0 |
| arcane | 15 | 5 | 0 |
| mithril | 20 | 8 | 0 |
| low_magisteel | 25 | 10 | 5 |
| magisteel | 30 | 12 | 8 |
| netherite | 35 | 0 | 0 |
| pure_magisteel | 40 | 15 | 10 |
| high_magisteel | 50 | 20 | 12 |
| orichalcum | 55 | 22 | 15 |
| adamantite | 60 | 25 | 18 |
| hihiirokane | 70 | 30 | 22 |

### P5 — Génération automatique des recettes d'assemblage

770 armes cibles → impossible à écrire à la main. Un **script Python** scanne `libs/`,
extrait chaque arme par regex + heuristique de path, déduit son matériau (préfixe), et
génère la recette JSON shapeless `rough_<class>_<material> + grip → <arme>`.

Output : `src/main/resources/data/statmod/recipe/assembly/<modid>/<item>.json`.

### P6 — Phasing strict

Le chantier est trop gros pour une seule session. Découpé en 6 phases livrables indépendamment :
voir §Phasing.

## Architecture détaillée

### Items à créer (~106 nouveaux items STAT MOD)

#### Métaux chauffés (heated_*) — **14 items**

Pour chaque métal du modpack pas déjà couvert par Overgeared natif (qui fournit copper,
iron, steel, silver, netherite_alloy) :

| ID STAT MOD | Source ingot | Couleur tint |
|---|---|---|
| `heated_gold_ingot` | minecraft:gold_ingot | #FFD700 |
| `heated_tin_ingot` | magistuarmory:tin_ingot | #D0D0D0 |
| `heated_bronze_ingot` | magistuarmory:bronze_ingot | #CD7F32 |
| `heated_diamond` | minecraft:diamond | #B9F2FF |
| `heated_pyrium_ingot` | irons_spellbooks:pyrium_ingot | #FF6B35 |
| `heated_arcane_ingot` | irons_spellbooks:arcane_ingot | #9D4EDD |
| `heated_mithril_ingot` | irons_spellbooks ou tensura mithril | #C0C0E0 |
| `heated_low_magisteel_ingot` | tensura:low_magisteel | #708090 |
| `heated_magisteel_ingot` | tensura:magisteel | #4682B4 |
| `heated_pure_magisteel_ingot` | tensura:pure_magisteel | #1E90FF |
| `heated_high_magisteel_ingot` | tensura:high_magisteel | #00BFFF |
| `heated_orichalcum_ingot` | tensura:orichalcum | #FFC107 |
| `heated_adamantite_ingot` | tensura:adamantite | #6A0DAD |
| `heated_hihiirokane_ingot` | tensura:hihiirokane | #DC143C |

Recette de production : `minecraft:blasting` (blast furnace, comme Overgeared natif),
cookingtime ajusté par tier matériau (100 ticks copper → 400 ticks hihiirokane).

#### Intermédiaires rough_* — **84 items** (Phase β)

6 classes d'armes × 14 matériaux :

Classes : `blade`, `axe_head`, `spear_tip`, `bow_limb`, `staff_core`, `dagger_blade`.
Matériaux : copper, tin, iron, bronze, silver, steel, gold, diamond, pyrium, arcane,
mithril, magisteel (collapse low/medium en un seul rough), pure_magisteel, high_magisteel,
orichalcum, adamantite, hihiirokane, netherite.

(Si je collapse low_magisteel + magisteel en un seul `rough_*_magisteel`, ça fait 13 matériaux
× 6 classes = 78 items. À trancher en Phase β selon retour utilisateur. Pour la spec
on prévoit 84 max.)

#### Grips — **4 items** (Phase β)

- `wooden_grip` (any log) — neutre
- `leather_wrap` (leather + string) — +10% durability sur arme finale
- `wire_wrap` (iron + string) — +5% damage sur arme finale
- `runic_grip` (leather + amethyst) — débloque slot enchant runic (Phase ε)

#### Blueprints — **4 items** (Phase β)

- `blueprint_universal_blade` (free) — débloque rough_blade_*, rough_dagger_blade_*
- `blueprint_universal_pole` (FORGING 8) — débloque rough_spear_tip_*, rough_axe_head_*
- `blueprint_runic_blade` (FORGING 35 + ERUDITION 15) — débloque rough_*_arcane, rough_*_mithril
- `blueprint_legendary` (FORGING 65) — débloque rough_*_orichalcum, rough_*_adamantite, rough_*_hihiirokane

### Hooks à étendre

#### `OvergearedRecipeGate` (existant)

```java
// Phase α — ajoute la map material → forging/erudition/arcane req
public static int requiredForgingLevel(ResourceLocation ingotId) { ... }
public static int requiredEruditionLevel(ResourceLocation ingotId) { ... }
public static int requiredArcanePower(ResourceLocation ingotId) { ... }
public static boolean canHeat(ResourceLocation ingotId, PlayerStatData data) { ... }
```

#### Nouveau hook `ItemCraftedEvent` listener

Phase γ — intercept tous les crafts dont le résultat est dans une recette `statmod:assembly/*`,
revalide le FORGING level via `OvergearedRecipeGate`. Si insuffisant, **annule le craft**
et envoie un `PerkFeedbackPayload` au joueur (réutilise le système existant).

#### Mixin `BlastFurnaceMenu` ou hook `PlayerInteractEvent` sur blast furnace

Phase α — quand le joueur place un ingot dans le blast furnace et qu'il manque les stats
requises pour le `heated_*` équivalent, on **bloque le recipe match** silencieusement (ou
on affiche un feedback overlay "Forging level too low: 25 required").

### Datapack JSON

```
src/main/resources/data/statmod/recipe/
├── heating/
│   ├── heated_gold_ingot.json          (Phase α)
│   ├── heated_tin_ingot.json
│   ├── ... (14 fichiers)
│   └── heated_hihiirokane_ingot.json
├── forging/                            (Phase β)
│   ├── blade/
│   │   ├── rough_blade_iron.json       (heated_iron × pattern → rough_blade_iron)
│   │   └── ... (~84 fichiers)
│   ├── axe_head/
│   ├── spear_tip/
│   └── ...
├── assembly/                           (Phase γ, auto-généré)
│   ├── simplyswords/
│   │   ├── iron_cutlass.json           (rough_blade_iron + wooden_grip → simplyswords:iron_cutlass)
│   │   └── ... (~300 fichiers Simply Swords)
│   ├── magistuarmory/                  (~150 fichiers Epic Knights)
│   ├── tensura/                        (whitelist ~20 fichiers)
│   └── ... (~770 total)
└── cooling/                            (Phase α)
    └── ... (14 fichiers : heated_* → ingot froid retour)
```

## Phasing

### Phase α — Materials Foundation (1-2 sessions)

**Scope**
- 14 nouveaux items `heated_<material>_ingot`
- Modèles 3D + 14 textures placeholder (tint de iron_ingot par couleur)
- Lang en_us + fr_fr
- 14 recettes `minecraft:blasting` (ingot → heated)
- 14 recettes `overgeared:cooling` (heated → ingot retour)
- Extension `OvergearedRecipeGate` avec table material → FORGING/ERUDITION/ARCANE req
- Hook qui bloque les recettes heating si stats insuffisantes
- Tests unitaires sur la table de gates (JUnit)

**Exit conditions α**
- Le joueur peut chauffer un `tensura:hihiirokane_ingot` UNIQUEMENT s'il a FORGING ≥ 70 + ERUDITION ≥ 30 + ARCANE_POWER ≥ 22
- `./gradlew test` passe vert sur la suite gates
- Vu en jeu : ouvrir blast furnace avec adamantite_ingot et un joueur lvl 5 FORGING → pas de heated_* produit + chat feedback

### Phase β — Intermediates (1-2 sessions)

**Scope**
- 84 (ou 78) nouveaux items `rough_<class>_<material>`
- 4 grips + 4 blueprints
- ~92 recettes `overgeared:forging` (heated_<material> + pattern → rough_<class>_<material>)
- Modèles + textures par tier
- Lang en/fr × 92
- Tests : à FORGING 50 + ARCANE 10, le joueur ne peut forger que jusqu'à mithril

**Exit conditions β**
- Le joueur peut forger les 6 classes d'armes intermédiaires depuis tous les heated_*
- Mini-game qualité Overgeared affecte la qualité du rough
- Quality du rough propage à l'arme finale (Phase γ)

### Phase γ — Universal Assembly (1 session)

**Scope**
- Script Python `tools/generate_assembly_recipes.py` qui scanne `libs/`
- Génère ~770 recettes `crafting_shapeless` dans `data/statmod/recipe/assembly/`
- Heuristique tier + classe basée sur path d'item
- Whitelist Tensura (par défaut, exclus → tu listes ce que tu veux inclure)
- Hook `ItemCraftedEvent` qui revalide FORGING level
- Test : crafter une simplyswords:diamond_rapier en cliquant sur le bench

**Exit conditions γ**
- 600+ armes des mods accessibles via le craft d'assemblage
- Aucune arme accessible si FORGING insuffisant
- Le script tourne en CI (gradle task `generateAssemblyRecipes`)

### Phase δ — Magic Forge (option, 1 session)

**Scope**
- Nouveau bloc `statmod:infusion_forge` (ré-skin de smithing anvil)
- Recettes spéciales : pyrium + arcane → arme magic-infused
- Dual-gate stricte ERUDITION + ARCANE_POWER pour cast spell pendant forge
- Lien direct avec arbre magique unifié (sort débloqué = bonus craft)

### Phase ε — Essences (option, 1 session)

**Scope**
- SLU shards (12 types) intégrés comme essences applicables à des armes finies
- Simply Swords gems (runefused, netherfused) comme essences
- Nouveau bloc `statmod:enchantement_anvil`
- Essences = enchants permanents qui se stack pas avec vanilla enchant

### Phase ζ — Race + Perk Crossover (option, 1 session)

**Scope**
- DWARF : -1 niveau FORGING requis sur tous les heating + assembly
- ELF : +25% durability sur arme assemblée
- OGRE : +1 quality tier (POOR → WELL automatique)
- BEAST : +0.05 chance de quality EXPERT
- Perks `forging_synergy`, `forging_mastery` modifient quality bias

## Tests & Verification

### Tests unitaires (Phase α)

```java
@Test void canHeat_copper_at_forging_0_OK()
@Test void cannotHeat_hihiirokane_at_forging_50_blocked()
@Test void canHeat_hihiirokane_at_forging_70_erudition_30_arcane_22_OK()
@Test void allHeatedIngots_haveValidStatGateMap()
@Test void allHeatedIngots_haveBlastingRecipe()
```

### Tests d'intégration (Phase α)

- Spinup serveur via gametest framework
- Place un blast furnace
- Donne adamantite_ingot au joueur, vérifie blocage
- Setstat FORGING 60 ERUDITION 25 ARCANE 18 → autorise

### Validation manuelle in-game

- Spawner un joueur avec /statmod setstat FORGING 70 ERUDITION 30 ARCANE_POWER 22
- Vérifier qu'il peut chauffer hihiirokane
- Vérifier le feedback chat pour les niveaux insuffisants

## Risques identifiés

### R1 — Conflit de namespace recipe override

Si un mod du pack ajoute déjà une recette pour un item Overgeared, on peut overrider sans
le vouloir. **Mitigation** : on n'override jamais. Toutes nos recettes ciblent des items
NOUS (statmod:heated_*, statmod:rough_*) ou en cible final unique (modid:item).

### R2 — Modpack updates qui changent les ids

Si Tensura renomme adamantite_ingot → hyper_adamantite_ingot dans une version future, nos
recettes cassent silencieusement. **Mitigation** : datagen tourne au build, donc tout
update modpack régénère les recettes. CI vert = recettes à jour.

### R3 — Textures placeholder ridicules

Si on shippe avec des tints crades, ça ruine l'immersion. **Mitigation** : Phase α
accepte les placeholders (focus mécanique), Phase ζ ajoute polish texture par un artiste
ou via une feuille concertée.

### R4 — Performance du datagen sur 770 recettes

Charger 770 JSONs à chaque world load = lag boot. **Mitigation** : profiler en Phase γ,
réduire si nécessaire en groupant par mod.

### R5 — Joueur frustré par les double-gates

Un joueur full-FORGING qui découvre qu'il ne peut pas forger orichalcum parce qu'il a 0
ERUDITION → frustration. **Mitigation** : feedback chat clair "Forging 60 ✓, Erudition
25 ✗ (requires 22)", + tooltip sur l'item heated qui liste les requis.

## Exit conditions globales

Le chantier "Overgeared Universal Forge" est considéré comme **livré** quand :

1. Les 6 phases α–ζ sont en `status: shipped`
2. Le modpack permet de forger TOUTES les armes non-Tensura listées dans
   `tools/generate_assembly_recipes.py` output
3. Au moins 10 armes Tensura whitelistées sont forgeables
4. Le décision record `STAT-DEC-OVERGEARED-EXPANSION` est en `status: closed`
5. Un playtest de 2h confirme que la progression FORGING reste fluide (pas de softlock,
   pas de niveau de matériau injoignable)

## Hors scope

- Pas de refonte du mini-game Overgeared (on l'utilise tel quel)
- Pas de nouveau bloc smithing anvil avant Phase δ
- Pas de support multi-loader (Fabric) — NeoForge 1.21.1 only, comme tout le mod
- Pas de support backward-compat avec saves qui n'ont pas les nouveaux items (ils seront
  juste invisibles dans les recipe book jusqu'à update)

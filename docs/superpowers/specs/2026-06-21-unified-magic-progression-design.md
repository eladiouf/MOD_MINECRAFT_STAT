---
title: Unified Magic Progression (Iron's Spellbooks + Tensura) + Virtual Inscription Binding
date: 2026-06-21
status: approved
project: statmod
branch: neoforge-1.21.1
decision-refs: [STAT-DEC-002, STAT-DEC-005, STAT-DEC-006]
mission-refs: [M4]
owner: Onivo Studio — Build Cell
approved-by: Founder (GO, 2026-06-21)
---

# Unified Magic Progression + Virtual Inscription Binding

## 1. Objectif

Offrir une **progression magique unifiée** dans laquelle le joueur débloque, via **un seul arbre Puffish**, à la fois des **sorts Iron's Spellbooks** et des **compétences Tensura**. Une fois la progression en place, permettre le **binding et le casting** des sorts Iron's via un **menu d'inscription virtuel** (ouvrable sans le bloc physique).

Priorité explicite du Founder : **la progression d'abord, le binding/casting ensuite**.

## 2. Contrainte structurante — Deux chemins de cast distincts

L'arbre est unifié pour le **déblocage**, mais le **cast** diverge selon la source :

| Source | Déblocage | Stockage | Cast |
|---|---|---|---|
| Iron's Spellbooks | nœud d'arbre → `learnedSpells[]` | grimoire (spellbook tenu) | système Iron's, après inscription manuelle |
| Tensura | nœud d'arbre → `learnedTensuraSkills[]` | `SkillAPI` du joueur | **système natif Tensura** (keybinds/slots) |

Conséquence : le **menu d'inscription virtuel ne concerne QUE les sorts Iron's**. Les skills Tensura sont actifs dès apprentissage, sans grimoire ni inscription.

## 3. État actuel (discovery 2026-06-21)

- `magic/MagicTreeCatalog.java` : seules `common/foundation/*` et `fire/*` sont réelles. Les 8 autres branches sont verrouillées (`prerequisites = ["__never__"]`, `LOCKED_SENTINEL`).
- `storage/PlayerStatData.java` : champs `arcanePoints`, `schoolPoints[10]`, `schoolMasteryProgress[10]`, `magicNodes[]`, `learnedSpells[]` (RL Iron's), `magicRace`, `chosenStartBranch`. Sérialisé via `MagicStateSerializer` (NBT ListTag/StringTag).
- `integration/ironspells/IronSpellEventBridge.java` : `onPreCast` (annule si `!hasLearnedSpell`), `onPostCast` (récompense), `onModifySpellLevel` (clamp T1=2/T2=4/T3=6), `onInscribe` (annule si non appris).
- `integration/puffish/*` : miroir réflexif de l'arbre, `SkillUnlock → MagicTreeProgressionService.tryUnlock`.
- `integration/tensura/TensuraSpellGate.java` : grant via `SkillAPI.getSkillsFrom(player).learnSkill(...)`. `TensuraSpellTaxonomy.java` : 48 skill IDs.
- Points (`arcanePoints`/`schoolPoints`) : alimentés **uniquement** par récompenses de cast + commande admin → pas d'amorçage organique.
- Race : posable **uniquement** par commande admin → pas de choix joueur en jeu.

## 4. Phase A — Progression (priorité)

### A1 — Étendre les branches Iron's (Water, Air, Earth)
Activer trois nouvelles branches avec contenu réel, sur le même patron que `fire/*` :
`opener` → tiers (paths) → `signature` accordant `irons_spellbooks:*`.
- Modifs : `magic/MagicTreeCatalog.java`, ressources `data/statmod/puffish_skills/` (categories + skills + connections JSON).
- Les branches lategame restent verrouillées (hors scope).
- Source de vérité des spell IDs : `libs/irons_spellbooks-1.21.1-3.16.1.jar` (`SpellRegistry`).

### A2 — Sélection de race/affinité en jeu
- Nouveau `ChooseRaceAffinityPayload` (`CustomPacketPayload` + `StreamCodec`).
- Écran d'onboarding au premier login (ou item de choix), écrit `magicRace` côté serveur après validation.
- Gate : un joueur ne peut ouvrir les branches non-COMMON que si sa race les autorise.

### A3 — Skills Tensura comme nœuds d'arbre
- Nouveau champ `learnedTensuraSkills[]` dans `PlayerStatData` (+ sérialisation NBT, + sync payload).
- Nouvelle catégorie Puffish dédiée Tensura.
- Au déblocage : `SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse("tensura:..."))` via `TensuraSpellGate`.
- Cast : **natif Tensura**, aucune intervention de notre part.

### A4 — Boucle d'acquisition de points organique
- Brancher un gain de `arcanePoints`/`schoolPoints` sur la progression des stats magiques / leveling existant, pour amorcer la boucle sans commande admin.
- Ne remplace pas le système de points ; l'**alimente**.

## 5. Phase B — Binding & Casting (idée Founder)

### B1 — Menu d'inscription virtuel (sans bloc)
- `MenuProvider` custom serveur, ouvert via `player.openMenu(...)` avec `ContainerLevelAccess.NULL`.
- Liste `PlayerStatData.learnedSpells` (sorts **appris dans l'arbre**, pas l'inventaire).
- Inscription du sort choisi dans le spellbook tenu :
  `ISpellContainer.getOrCreate(stack).mutableCopy().addSpell(spell, level, false)` → `ISpellContainer.set(stack, immutable)`.
- Respect de `MAX_SLOTS` et du gating de niveau (T1/T2/T3).

### B2 — Ouverture
- Keybind dédié et/ou item craftable (décision d'implémentation au plan).

### B3 — Gestion du grimoire
- Ajout/retrait/remplacement de sorts dans les slots disponibles.

### B4 — Cast
- Chemin normal Iron's, déjà gated par `IronSpellEventBridge.onPreCast`.

## 6. Découpage en agents

- **Agent 1** — A1 + A3 (contenu d'arbre Iron's étendu + nœuds Tensura).
- **Agent 2** — A2 + A4 (race en jeu + boucle de points).
- **Agent 3** — Phase B (menu virtuel + grimoire + cast).
- Chacun : TDD, branche `feature/*`, revue `code-reviewer`.
- A3/Phase B dépendent de A1 pour le schéma de nœuds ; séquencer au plan.

## 7. Hors scope (YAGNI)

- Branches lategame Iron's.
- Chemin de cast custom pour Tensura (système natif respecté).
- Refonte du système de points.

## 8. Exit Conditions (politique STAT-PM-001)

- **Iron's** : si Iron's Spellbooks devient indisponible, les nœuds `irons_spellbooks:*` se désactivent proprement (nœuds masqués, `learnedSpells` conservé en NBT mais inerte) sans crash de chargement.
- **Tensura** : hard dependency ; si absente, le mod ne charge pas (statu quo).
- **Puffish** : déjà optionnel via réflexion ; absence → arbre non miroité, progression interne intacte.

## 9. Risques

- IDs de sorts Iron's à valider contre `SpellRegistry` 3.16.1 (éviter RL invalides).
- Sérialisation : ajout de `learnedTensuraSkills[]` doit rester rétro-compatible avec les sauvegardes existantes (migration testée — lié à M5).
- `ContainerLevelAccess.NULL` : vérifier le comportement de fermeture/quick-move sans bloc.
- Sync client : nouveaux champs doivent être inclus dans les payloads de sync existants.

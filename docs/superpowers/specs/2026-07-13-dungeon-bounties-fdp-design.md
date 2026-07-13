---
title: Bounties de donjon en FDP
date: 2026-07-13
status: draft
mission: Trial Dungeon (M6) — couche bounties
integration: bountiful
---

# Spec — Bounties de donjon payés en FDP

## 1. Contexte & objectif

Les tableaux de bounty (`bountiful:bountyboard`) sont posés dans un district de la
**Cité des Aventuriers** (étage 0) par `dungeon/city/CityDistrictBuilder` (2 tableaux).
Aujourd'hui ils génèrent des bounties **génériques de métiers vanilla** (ramasser du blé,
tuer un poulet…) via les 13 décrees `data/bountiful/bounty_decrees/bountiful/*.json`, que le
mod override déjà pour y injecter le pool de récompenses `fdp_rewards`.

**But** : rendre les bounties **difficiles et thématisés donjon**, payés **uniquement en
monnaie FDP physique** (les items `statmod:fdp_coin_*` / `statmod:fdp_note_*` déjà créés),
avec des montants relevés proportionnels à la difficulté.

## 2. Décisions de cadrage (brainstorm 2026-07-13)

| Décision | Choix |
|---|---|
| Objectifs | Tuer des mobs de donjon + tuer des boss + rapporter des matériaux de coffre + **atteindre un étage N** |
| Remplacer vs ajouter | **Remplacer** — les décrees ne pointent plus que vers des pools donjon |
| Récompense | **FDP uniquement**, montants relevés (auto via `unitWorth`) |
| « Atteindre l'étage » | **Inclus maintenant** (advancement custom + hook Java) |
| Portée | **Globale assumée** — tout tableau de bounty du monde devient donjon+FDP |

## 3. Mécanique Bountiful (faits vérifiés dans `bountiful-neoforge-8.0.0-beta.2.jar`)

- Une **décree** liste des *pools* d'objectifs et de récompenses ; chaque bounty pioche
  dedans. Réécrire les décrees = changer tout le contenu des tableaux (= « remplacer »).
- **Types d'objectifs** : `item` (collecte), `itemTag`, `entity` (kill), `criteria`
  (advancement), `command`. Les objectifs `entity` **ne supportent pas les tags** →
  on énumère les entités. Les kills sont traqués automatiquement (`handleEntityKills`).
- **Équilibrage des récompenses** : Bountiful choisit la valeur de récompense pour **égaler**
  la valeur totale des objectifs (`unitWorth`). ⇒ « payer plus » = mettre un `unitWorth` élevé
  sur les objectifs durs ; le montant FDP monte **automatiquement**. Pas de tuning manuel.
- **`criteria`** : se complète quand le joueur **gagne un advancement** dont l'id correspond
  au `content` (Bountiful enregistre un `SimpleCriterionTrigger` via `registerCriterionStuff`).
- Format objectif `entity` (extrait `_all_objs.json`) :
  ```json
  "zombie": { "type": "entity", "timeMult": 6.0, "content": "minecraft:zombie",
              "amount": {"min":1,"max":4}, "unitWorth": 250, "weightMult": 0.2 }
  ```
- Format pool de devise (extrait `fdp_rewards.json` / `currency_example.json`) : clé
  racine `"currency": true`, entrées `item` avec `unitWorth` = valeur faciale.

## 4. Architecture

### 4.1 Nouveaux pools d'objectifs — `data/bountiful/bounty_pools/statmod/`

Tous les pools gardent une **base vanilla garantie** (entités/items toujours présents) pour
qu'un bounty se génère même sans SLU / Iron's Spellbooks ; les entrées moddées sont ajoutées
en plus (Bountiful ignore les `content` inconnus via `isValid`, à confirmer au run).

**`dungeon_slay_objs.json`** — `entity`, tuer des mobs de donjon, réparti par rareté :
- COMMON : `minecraft:zombie`, `minecraft:skeleton`, `slu:hollow`, `slu:armed_hollow`,
  `slu:thief` — `amount` 3–8, `unitWorth` ~200–350.
- UNCOMMON : `slu:knight`, `slu:castle_guard`, `slu:dungeon_knight`,
  `irons_spellbooks:cultist`, `minecraft:wither_skeleton` — 2–5, ~500–800.
- RARE : `slu:elite_knight`, `slu:dark_knight`, `slu:ringed_knight`,
  `irons_spellbooks:necromancer`, `irons_spellbooks:archevoker` — 1–3, ~1200–2000.

**`dungeon_boss_objs.json`** — `entity`, tuer un boss (×1), EPIC, `timeMult` élevé :
- `minecraft:warden` (garanti), `slu:boss_artorias`, `slu:boss_malenia`, `slu:boss_radahn`,
  `slu:boss_gael`, `irons_spellbooks:dead_king`, `irons_spellbooks:citadel_keeper`.
- `unitWorth` très élevé (~6000–15000) ⇒ gros paiement FDP.

**`dungeon_haul_objs.json`** — `item`, rapporter des matériaux **signature du donjon**
(issus de `chests/dungeon_treasure.json`) :
- UNCOMMON : `statmod:rune_essence_arcane`, `statmod:rune_essence_pyrium`,
  `statmod:rune_essence_mithril` — 2–6, `unitWorth` ~600.
- RARE : `statmod:perk_tome`, `statmod:respec_stone`, `minecraft:netherite_scrap` — 1–3.
- EPIC : `minecraft:nether_star`, `minecraft:netherite_ingot` — 1, worth élevé.

**`dungeon_delve_objs.json`** — `criteria`, atteindre un palier d'étage :
- `content` = `statmod:dungeon/delve_10` | `delve_25` | `delve_50` | `delve_100`.
- Rareté croissante RARE→EPIC, `unitWorth` croissant (le palier profond paie le plus).

### 4.2 Récompense FDP — `data/bountiful/bounty_pools/statmod/fdp_rewards.json`

Conservé (`currency: true`, 8 coupures). On **relève les plafonds** des grosses coupures
(`fdp_note_5000` max 4, `fdp_note_10000` max 3) pour que les bounties EPIC puissent être
soldés en billets sans empiler 40 stacks. Les montants effectifs restent pilotés par la
valeur de l'objectif.

### 4.3 Override des 13 décrees — `data/bountiful/bounty_decrees/bountiful/*.json`

Chaque décree devient :
```json
{
  "linkedProfessions": ["<métier d'origine>"],
  "objectives": ["dungeon_slay_objs","dungeon_boss_objs","dungeon_haul_objs","dungeon_delve_objs"],
  "rewards": ["fdp_rewards"]
}
```
`linkedProfessions` est conservé (les tableaux se peuplent par métier des villageois proches).
Plus aucun pool vanilla (`_all_objs`, `*_rews`, `_equip_rews`) → « remplacer » + « FDP only ».

### 4.4 « Atteindre l'étage » — advancements + hook Java

- **Advancements** `data/statmod/advancement/dungeon/delve_{10,25,50,100}.json` : sans parent
  affiché, `announce_to_chat:false`, critère unique déclenché par `impossible` (jamais
  auto-obtenu) — l'octroi se fait **uniquement par code**.
- **Hook** : dans `DungeonTeleportHandler.enterFloor` (autorité d'entrée d'étage), après mise
  à jour de `dungeonFloorReached`, appeler un helper pur `DungeonBountyMilestones.reached(floor)`
  → renvoie la liste des paliers franchis ; pour chacun, `serverPlayer.getAdvancements()
  .award(advancement, "reached")`. Idempotent (award ne re-déclenche pas s'il est déjà obtenu).
- Le `criteria` objectif de Bountiful se complète quand l'advancement est gagné (trigger
  Bountiful). **À verrouiller au run** : forme exacte que Bountiful attend dans `content`
  (id d'advancement vs id de criterion) — plan à valider en jeu, fallback documenté.

## 5. Composants & responsabilités

| Unité | Rôle | Dépend de |
|---|---|---|
| `bounty_pools/statmod/dungeon_*_objs.json` | Contenu des objectifs (datapack) | ids entités/items réels |
| `bounty_pools/statmod/fdp_rewards.json` | Devise de récompense (datapack) | items FDP |
| `bounty_decrees/bountiful/*.json` (×13) | Câblage décree → pools donjon | pools ci-dessus |
| `advancement/dungeon/delve_*.json` | Cible des objectifs `criteria` | — |
| `DungeonBountyMilestones` (Java, pur) | `reached(floor)` → paliers franchis | — (testable JUnit) |
| `DungeonTeleportHandler.enterFloor` (hook) | Octroie les advancements de palier | `DungeonBountyMilestones`, advancements |

## 6. Tests

- `DungeonBountyResourcesTest` (source, style `BountifulFdpResourcesTest`) :
  - les 4 pools `statmod/dungeon_*` existent et contiennent les ids garantis attendus ;
  - les 13 décrees ne référencent QUE des pools `statmod:` + `fdp_rewards` (zéro pool vanilla) ;
  - les 4 advancements `delve_*` existent.
- `DungeonBountyMilestonesTest` (unitaire pur) : franchissement des paliers 10/25/50/100
  (entrer étage 24 ne franchit que 10 ; étage 50 franchit 50 ; idempotence entre appels).

## 7. Exit Conditions (politique `STAT-PM-001`)

Bountiful reste **optionnel** : s'il est retiré, les tableaux et bounties disparaissent, mais
la monnaie FDP reste gagnable via le Banquier (points → FDP) et les coffres. Aucun code du
donjon ne dépend en dur de Bountiful ; le hook advancement est un no-op sûr côté STAT MOD
(l'advancement est accordé même si Bountiful est absent, sans effet). Pas de hard dependency
introduite.

## 8. Risques / à valider au run

1. **Skip des entités moddées absentes** : on suppose que Bountiful ignore un `content`
   d'entité inconnu au lieu d'invalider tout le pool (base vanilla garantit un fallback).
2. **Forme du `content` de `criteria`** : id d'advancement exact attendu par le trigger
   Bountiful — à confirmer en jeu.
3. **Portée globale** : tout tableau du monde devient donjon+FDP (assumé).
4. **Difficulté non scalée au joueur** : un joueur faible peut tirer un bounty « boss » (EPIC).
   Voulu (« plus dur »), atténué par le système de rareté des slots de tableau.

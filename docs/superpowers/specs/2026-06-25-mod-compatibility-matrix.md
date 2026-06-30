# Mod Compatibility Matrix

> **Mission M10** — Catalogue complet des intégrations multi-mods de STAT MOD.
> Statut : **canonique** (décision `STAT-DEC-005`).

## Table

| Mod | Statut | Package | Features Integrated | Test Coverage | Risques |
|---|---|---|---|---|---|
| Tensura Reincarnated | **hard dependency** | `integration/tensura/` | Race → stat modifiers (`RaceModifierRegistry`), soul level → global level (`SoulLevelSyncHandler`), skills → perk gates (`SkillPerkGate`), spells → magic tree linkage, physical effects via `RaceEffectApplier` | Yes | Reflection interne fragile (sous-version de Tensura), dépendance binaire bloquante sur NeoForge 1.21.1 |
| Iron's Spellbooks | **active** (Phase 1) | `integration/ironspells/` | Reverse bridge (`IronSpellEventBridge`), inscription (`InscriptionHandler`), arbre magique unifié, XP sync (`IronSpellXpHandler`) | Yes | Mixin fragile (`@Mod.EventBusSubscriber` + interface injectée), API verrouillée sur version spécifique, 3 écoles late-game verrouillées |
| Puffish Skills | **active** (UI miroir) | `integration/puffish/` | Perk tree mirror, magic tree dual generator, custom JSON generator pour le Skill Tree Puffish | Yes | Nécessite regénération manuelle du JSON Puffish à chaque ajout de perk/nœud ; pas de hot-reload |
| Epic Fight | **experimental optional** | `integration/epicfight/` | Stamina bridge, cooldown sync, execute sous < seuil, scale d'armes via `EpicFightScaleHandler` | Yes | `@mixin require=0` ajouté pour non-crash si Epic Fight absent ; comportement non testé en multi avec d'autres mods de combat |
| ParCool | **optional** | `integration/parcool/` | Parkour → XP physique, stamina bridge, attribute modifiers | Partial (intégration basique) | Faible risque technique ; le bridge ne couvre pas encore wall-run ni double-saut avancé |
| Overgeared | **optional** | `integration/overgeared/` | Forging gating (niveau requis), durability bridge, XP crafting | Partial (intégration basique) | Faible risque ; Overgeared a peu d'API publiques stables, dépend de listeners d'events |
| Mahou Tsukai | **REMOVED** | — | — | — | Postmortem `STAT-PM-001` : API parallèle incompatible, blessait le modèle unifié |
| Elementals | **REMOVED** | — | — | — | Postmortem `STAT-PM-001` : doublon fonctionnel avec l'arbre Iron's Spellbooks |

## Légende

- **hard dependency** : le mod doit être présent au runtime ; STAT MOD ne se charge pas sans.
- **active** : intégration sous développement/maintenance actif (Phase 1 Magic).
- **experimental optional** : intégration présente mais désactivée par défaut ; `@mixin require=0` garantit le non-crash.
- **optional** : intégration présente, activable via config ; pas de régression si absent.

## Décisions de Design

1. **Toute intégration doit déclarer un bloc Exit Conditions** dans son design spec (politique `STAT-PM-001`).
2. **Les bridges sont évènementiels** : pas de dépendance directe au code interne des mods intégrés (sauf Tensura, hard dep).
3. **Les tests d'intégration sont isolés** : mock/minimal environment, pas de lancement Minecraft.
4. **`@mixin require=0`** obligatoire pour toute intégration optionnelle/expérimentale.

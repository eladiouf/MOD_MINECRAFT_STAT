---
type: meeting-minutes
status: Closed
session_id: STAT-MTG-002
session_type: Technical Audit Plenary
date: 2026-06-22
owners:
  - Build Cell
  - Verification Cell
attendees:
  - Build Cell
  - Verification Cell
  - Product Cell
  - Strategy Council
related_projects:
  - STAT Mod
related_decisions:
  - STAT-DEC-007
  - STAT-DEC-008
  - STAT-DEC-009
  - STAT-DEC-010
  - STAT-DEC-011
  - STAT-DEC-012
  - STAT-DEC-013
  - STAT-DEC-014
tags:
  - meeting
  - statmod
  - audit
  - progression
  - quality
---

# STAT-MTG-002 — STAT Mod : Audit du Système de Niveaux et Progression

## Mandate

Examiner le système de stats/XP/leveling/perks **fond en comble**, confronter le code à la réalité runtime, identifier les écarts, et produire des décisions exécutoires.

## Present

- **Build Cell** — périmètre : stats/, progression/, config/, PlayerStatData
- **Verification Cell** — périmètre : tests/, ModAttachments, serialization, network/
- **Product Cell** — périmètre : perks/, PerkEffectHandler, LevelUpHandler, RaceEffectApplier, WeaponResolver
- **Strategy Council** — arbitrage et synthese

---

## Rapports d'Audit

### Build Cell — Findings

| # | File | Issue | Severity |
|---|------|-------|----------|
| B1 | `Config.java` + `RaceEffectApplier.java:60` | 4 config values defined but **never wired** — `scaleXpAmount` ignores config multipliers, `maxStatLevel` ignores `MAX_STAT_LEVEL` config | **HIGH** |
| B2 | `progression/ProgressionRouting.java` | Entire class dead code — **zero callers** | **HIGH** |
| B3 | `progression/ActionType.java` | Entire enum dead code — **zero callers** | **HIGH** |
| B4 | `stats/StatAttributeHandler.java:27-28` | Memory leak — player UUIDs persist in static `ConcurrentHashMap` forever | **HIGH** |
| B5 | `progression/LevelUpHandler.java:19` | Memory leak — `lastGrantedTier` `HashMap` never cleaned | **MEDIUM** |
| B6 | `progression/NonCombatXPHandler.java:50-57` | ALCHEMY XP farm via repeated brewing-stand right-clicks without brewing | **MEDIUM** |
| B7 | `stats/StatEffectApplier.java:22-52` | Missing `isClientSide` guard on attacker-side damage modifiers | **MEDIUM** |
| B8 | `progression/NonCombatXPHandler.java:59-69` | `isOre()` should use `BlockTags.ORES` instead of hardcoded block list | **MEDIUM** |
| B9 | `progression/WeaponResolver.java:59` | `path.contains(keyword)` can produce false positives for substring matches | **MEDIUM** |
| B10 | `stats/StatCommands.java` | Commands exist, no functional bugs | ✅ |

### Verification Cell — Findings

| # | File | Issue | Severity |
|---|------|-------|----------|
| V1 | `PerkEffectHandler.java` (600 lignes) | **Zéro tests** — 30+ effets runtime, 25+ modifications de dégâts, 10+ effets on-kill | **P0** |
| V2 | `CombatXPHandler.java` (52 lignes) | **Zéro tests** — resolveAttacker, XP calc, race/soul scaling | **P0** |
| V3 | `NonCombatXPHandler.java` (77 lignes) | **Zéro tests** — ore mining, crafting, smelting, brewing | **P0** |
| V4 | `LevelUpHandler.java` (41 lignes) | **Zéro tests** — perk point milestone distribution | **P0** |
| V5 | `ModAttachments.java:53-62` | Migration PerkPoints ambigüe — array length entre 6 et 22 traité comme format famille | **P1** |
| V6 | 11/12 network payloads | **Zéro round-trip codec tests** — seul `OpenVirtualInscriptionPayload` est testé | **P1** |
| V7 | `RaceEffectApplier.java` | **Pas de test direct** — `addScaledXp`, `scaleXpAmount`, `getEffectiveLevel` | **P1** |
| V8 | `MagicStateSerializer.java:63-64` | `IllegalArgumentException` swallow — race corrompue → perte silencieuse | **P2** |
| V9 | All network handlers | **Aucune validation payload** — levels[], perkIds[], schoolPoints[] non vérifiés | **P2** |
| V10 | `ServerPayloadHandler` | **Pas de feedback client** sur échec unlock (perk ou magic) | **P2** |
| V11 | `PerkFeedbackPayload` | Code mort — défini mais jamais envoyé | **P3** |

### Product Cell — Findings

| # | File | Issue | Severity |
|---|------|-------|----------|
| P1 | `PerkEffectHandler.java:308` | `True Strike` (PRECI_TRANSCENDENCE) — **zero permanent mob armor**, jamais restauré. Corrompt les attributs des entités. | **CRITICAL** |
| P2 | `WeaponResolver.java:18,31` | `kodachi` contient `"odachi"` → classifié BRUTE_FORCE au lieu de RAPIDITE | **HIGH** |
| P3 | `PerkEffectHandler.java:410` | `Focused Mind` donne DAMAGE_RESISTANCE au lieu de knockback resistance — tooltip wrong | **HIGH** |
| P4 | `PerkEffectHandler.java:503-511` | `Dread Lord` slow les mobs au lieu de les faire fuir — tooltip wrong | **HIGH** |
| P5 | `PerkEffectHandler.java:161-166` | `Feared` slow les mobs au lieu de les faire fuir — tooltip wrong | **HIGH** |
| P6 | `PerkEffectHandler.java:514-522` | `Absolute Dominion` clear aggro + regen, pas de contrôle — tooltip wrong | **HIGH** |
| P7 | `PerkEffectHandler.java:156-158` | WILL_TRANSCENDENCE remove **tous** les effets (inclut buffs) chaque tick | **MEDIUM** |
| P8 | `LevelUpHandler.java:29` | Global level = sum/23 — **impossible** pour les builds non-magic d'atteindre le moindre milestone | **HIGH** |
| P9 | Non-combat XP system | XP non-combat 6-30× plus lent que combat — 9 stats magiques sans aucune source XP | **HIGH** |
| P10 | Perk point economy | ~10 points/famille au cap, besoin de 54 pour maxer — forçe la hyperspécialisation, pas de respec | **HIGH** |
| P11 | `NonCombatXPHandler.java` | FISHING, ENCHANTING, PARKOUR, SWIMMING définis dans ActionType mais jamais awardés | **MEDIUM** |
| P12 | `PlayerStatData.java` | TRANSCENDENCE (lvl 95) nécessite ~98k zombie kills — unreachable sans race endgame ×10 | **MEDIUM** |

---

## Débat et Résolution

### Sujet 1 : Config morte — 4 valeurs jamais lues

**Build Cell** : "Je wire tout en 15 minutes. `maxStatLevel` → `Config.MAX_STAT_LEVEL`, les multipliers → `RaceEffectApplier`. C'est une honte d'avoir du code mort."

**Verification Cell** : "Zéro tests sur les handlers XP. Tu wires la config, tu changes le comportement runtime, tu n'as aucun filet. Tests d'abord."

**Product Cell** : "Les admins ont besoin de contrôler la progression. On peut pas livrer avec des configs factices. Mais d'accord avec Verification — au moins un test heureux."

**Résolution** : Wired dans une mission dédiée, avec tests d'abord. **STAT-DEC-008**.

### Sujet 2 : Tests manquants sur les XP handlers (P0 × 4)

**Build Cell** : "On a 138 perks qui marchent, le mod tourne. Les tests sont importants mais pas au prix de bloquer le fix du `True Strike`."

**Verification Cell** : "Tu changes le moindre multiplicateur dans `scaleXpAmount` et tu casses l'équilibre sans t'en rendre compte. Je bloque tout changement sur les handlers sans tests."

**Product Cell** : "Le `True Strike` corrompt les saves — c'est un data corruption bug, ça passe avant tout. Mais je veux des tests sur les nouveaux chemins XP magiques."

**Résolution** : Tests critiques d'abord (scaleXpAmount, addXp loop, WeaponResolver), puis corrections. **STAT-DEC-007**.

### Sujet 3 : Perk effects — tooltip mismatches × 5 + True Strike bug

**Build Cell** : "Les tooltips sont dans le code des perks, pas dans les fichiers lang. Je peux corriger les effets en 30 min. Le `True Strike` est un one-liner."

**Verification Cell** : "J'ai besoin d'au moins un test qui vérifie que True Strike restore l'armor après le combat."

**Product Cell** : "5 tooltips qui mentent + 1 corruption de save. Priorité absolue. Les joueurs font confiance au texte des perks."

**Résolution** : Hotfix immédiat pour True Strike (CRITICAL). Corrections tooltips dans la même mission. **STAT-DEC-009**.

### Sujet 4 : Global level inaccessible pour builds non-magic

**Build Cell** : "Le vrai fix c'est d'ignorer les stats magiques du calcul si elles sont lockées. Ou utiliser `max(levels)` au lieu de `average(levels)`."

**Product Cell** : "Le global level devrait compter les stats que le joueur peut effectivement leveler. Un guerrier pur avec 10 stats à 40+ ne devrait pas être puni parce que 9 stats magiques sont à 0."

**Verification Cell** : "Changer la formule du global level casse `LevelUpHandler` sans qu'on le sache. Tests d'abord."

**Résolution** : La formule `sum / STAT_COUNT` reste, mais les stats magiques lockées (Phase 1 — seules Fire active) ne comptent pas dans le dénominateur. **STAT-DEC-010**.

### Sujet 5 : Sources XP non-combat pour stats magiques

**Build Cell** : "Ajout via event bridges Iron's — craft de spell → ARCANE, cast Fire → FIRE_AFFINITY, etc. Architecture déjà en place."

**Product Cell** : "Bug live. Les joueurs mages ne peuvent pas progresser sans combat. Priorité haute."

**Verification Cell** : "D'accord mais tests avant merge."

**Résolution** : Mission dédiée dans M4.5 — ajout XP non-combat pour les 9 stats magiques via event bridges. **STAT-DEC-011**.

### Sujet 6 : Kodachi misclassification

**Build Cell** : "Réordonner les patterns pour que `kodachi` soit checké avant `odachi`. 1 ligne."

**Verification Cell** : `WeaponResolverTest` gratuit après ce fix ? Non. OK, accepté sans test vu l'évidence."

**Résolution** : Fix immédiat, inclus dans le hotfix. **STAT-DEC-012**.

### Sujet 7 : Legacy mobilitée — code mort à supprimer

**Build Cell** : "ProgressionRouting.java, ActionType.java, PerkFeedbackPayload (mort pour l'instant). Config.register() vide."

**Verification Cell** : "ProgressionRouting a des tests dans `ProgressionRoutingTest`. Si on supprime la classe, on supprime les tests aussi."

**Product Cell** : "Pas d'impact joueur. Nettoyage bienvenu."

**Résolution** : Supprimer ProgressionRouting.java, ActionType.java, tests associés, Config.register(). **STAT-DEC-013**.

### Sujet 8 : Mémoire — static maps non nettoyées

**Build Cell** : "Listener `ServerPlayerConnectionUpdate` ou `PlayerEvent.PlayerRespawnEvent` pour cleaner les maps. 20 min."

**Verification Cell** : "Test de fuite mémoire difficile. Accepté sans test spécifique, mais vérification manuelle."

**Résolution** : Cleanup sur disconnect pour les 3 static maps identifiées. **STAT-DEC-014**.

---

## Décisions

| Id | Sujet | Outcome | Assigné |
|----|-------|---------|---------|
| **STAT-DEC-007** | Priorité test vs fix | Les P0 (CombatXPHandler, NonCombatXPHandler, LevelUpHandler, PerkEffectHandler) recoivent des tests de contrat avant modification. Les bugs CRITICAL/HIGH (True Strike, tooltips) sont hotfixés immédiatement sans attendre les tests. La mission M11 couvre les tests, M12 couvre les hotfixes. | Verification Cell, Build Cell |
| **STAT-DEC-008** | Wiring des 4 configs mortes | Les 4 valeurs de Config.java sont connectées au runtime dans une mission dédiée (M13) avec tests de régression. `ProgressionRouting.java` supprimé simultanément. | Build Cell |
| **STAT-DEC-009** | Perk tooltip mismatches + True Strike | CRITICAL : `True Strike` — restaurer l'armor après l'hit, pas de mutation permanente. HIGH : `Dread Lord` + `Feared` → remplacer slow par mob flee. `Focused Mind` → remplacer DAMAGE_RESISTANCE par knockback resistance. `Absolute Dominion` → actual mob control (ou retooltip). Hotfix — mission M12. | Build Cell |
| **STAT-DEC-010** | Global level formula | Les stats magiques lockées (Phase 1 — toutes sauf Fire) sont exclues du dénominateur `STAT_COUNT` dans le calcul du global level. Le dénominateur devient `STAT_COUNT - lockedMagicStatsCount`. Applicable seulement quand la stat est au niveau 0 et que le MagicRace n'est pas défini. | Build Cell |
| **STAT-DEC-011** | Sources XP non-combat pour stats magiques | Mission dédiée (M4.5) : craft de spell → ARCANE XP, cast élémentaire → AFFINITY XP, découverte de sort → ERUDITION XP, niveau de sort → CASTING_SPEED XP, mana utilisé → MANA_POOL XP, résister à un sort → MAGIC_RESISTANCE XP. Via event bridges Iron's Spellbooks. | Build Cell |
| **STAT-DEC-012** | Kodachi → RAPIDITE | Réordonner `WeaponResolver.java` : checker `kodachi` avant `odachi`. Dans le hotfix M12. | Build Cell |
| **STAT-DEC-013** | Suppression code mort | Supprimer `ProgressionRouting.java`, `ActionType.java`, `ProgressionRoutingTest`, `ActionTypeTest`, `Config.register()` vide. Dans M13 (nettoyage). | Build Cell |
| **STAT-DEC-014** | Fuites mémoire static maps | Ajouter cleanup sur `ServerPlayerConnectionUpdate` pour `StatAttributeHandler.lastRapidite/lastAgility`, `LevelUpHandler.lastGrantedTier`, `PerkState`. Hotfix M12. | Build Cell |

---

## Nouvelles Missions Créées

| Id | Mission | Cell | Dépend de |
|----|---------|------|-----------|
| **M11** | Tests XP handlers (Combat, NonCombat, LevelUp, RaceEffectApplier) | Verification | — |
| **M12** | Hotfix : True Strike, tooltips, kodachi, memory leaks | Build | — |
| **M13** | Wiring config + dead code cleanup | Build | M11 (tests d'abord) |
| **M4.5** | XP non-combat pour stats magiques | Build | M12 |

Les missions M11 et M12 sont indépendantes et peuvent courir en parallèle. M13 est bloquée sur M11.

## Mise à jour des surfaces

- `current-initiatives.md` — ajouter M11, M12, M4.5, M13 dans l'ordre séquentiel après M4
- `project-home.md` — update risks et status
- `Decision Index` — ajouter STAT-DEC-007 à STAT-DEC-014

## Next Cadence

- **M12 (hotfix)** — livraison immédiate, revue Build Cell
- **M11 (tests)** — démarrage parallèle, revue Verification Cell
- Prochaine plenary : après fermeture de M11 + M12

---

*Document finalisé le 2026-06-22 par Onivo Studio — séance plénière STAT-MTG-002.*

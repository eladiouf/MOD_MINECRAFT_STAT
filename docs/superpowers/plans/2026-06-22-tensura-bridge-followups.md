# Tensura Bridge — Conformance Follow-ups

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continuer le travail de conformance du reverse bridge Tensura → Iron's Spellbooks après `STAT-DEC-M4-CONFORMANCE`. Valider in-game, fermer les bugs latents, puis enrichir la fidélité (mana réelle, mastery → spell level) et l'UX du menu d'inscription.

**Architecture :** Le bridge actuel (`TensuraDelegatingSpell` + `TensuraSpellMetadata` + `IronSpellIconResolverMixin`) reste l'autorité d'exécution. Aucune réécriture structurelle dans ce plan — uniquement des extensions ciblées.

**Tech Stack :** NeoForge 1.21.1, Java 21, Mixin 0.8, JUnit Jupiter 5.10, branche `neoforge-1.21.1`.

**Décisions parents :** `STAT-DEC-M4-REVERSE`, `STAT-DEC-M4-B`, `STAT-DEC-M4-CONFORMANCE`.

---

## Ordre de priorité (autonomous studio decision)

Justification : on ne stack pas du code par-dessus une feature non validée. Polish léger ensuite, puis fidélité, puis grosses extensions.

1. **F** — Validation runtime du conformance work (gate avant tout le reste)
2. **C0** — Fix `KnownSpellUiState` bound-aware (30 minutes, débloque l'UX inscrite)
3. **A** — Mana cost réel par sort Tensura
4. **B** — Mastery Tensura → spell level Iron's
5. **C1** — Polish UX inscription menu (recherche / filtre)
6. **D** — Étendre wrappers aux Skills / BattleWill Tensura (gros chantier, hors M4)
7. **E** — Phase 2 magic branches (Water/Air/Earth/...) (gros chantier, mission séparée)

---

## Mission F — Validation runtime du conformance work

**But :** Confirmer que le cercle magique Tensura apparaît, que le projectile spawn, et que l'icône native s'affiche dans le menu d'inscription. Sans ça, tout le reste est de l'air.

### Files to inspect (no edits expected)

- `src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java`
- `src/main/java/tong/statmod/integration/ironspells/bridge/TensuraSpellMetadata.java`
- `src/main/java/tong/statmod/mixin/IronSpellIconResolverMixin.java`
- `runs/client/logs/latest.log` (capture des cas de blocage)

### Tasks

- [ ] **F.1 — Run client** : `./gradlew runClient`, charger un monde avec un joueur Tensura race actif.
- [ ] **F.2 — Test Fire Bolt depuis grimoire** : inscrire `statmod:tensura_fire_bolt`, équiper grimoire, lancer. Observer :
  - barre de cast Iron's apparaît (durée ≠ 0)
  - entité `MagicCircle` Tensura spawn pendant le hold
  - projectile fire bolt sort à la fin
- [ ] **F.3 — Si pas de projectile** : grep le log pour `isOutOfEnergy`, `MagicCircle`, `FireBoltMagic`. Décision binaire :
  - magicule = 0 et bloque → poser `STAT-DEC-MAGICULE-POLICY` (3 options : bypass / auto-grant / accepter contrainte)
  - autre cause → diagnostiquer séparément
- [ ] **F.4 — Test icône native** : ouvrir codex / menu d'inscription, vérifier que les icônes affichées sont celles de Tensura (pas placeholder magenta). Si magenta : ajouter log dans `TensuraSpellMetadata.invokeIconGetter` et regrep.
- [ ] **F.5 — Test sort INSTANT** (sort Tensura à cast time 0) : confirmer comportement immédiat sans cast bar.

### Acceptance

✅ Au moins un sort `LONG` (Fire Bolt) produit cercle + projectile depuis grimoire.
✅ Au moins un sort `INSTANT` Tensura fire sans cast bar.
✅ Toutes les icônes Tensura rendues correctement (pas de magenta).

### Exit conditions

Mission close quand les 3 cases d'acceptance sont vertes. Si F.3 révèle un blocage runtime, créer mission séparée `STAT-DEC-MAGICULE-POLICY` et ne pas passer à C0/A/B tant que non résolu.

---

## Mission C0 — `KnownSpellUiState` bound-aware

**But :** Fermer le bug latent identifié dans `STAT-DEC-M4-CONFORMANCE` — quand le joueur inscrit un sort, l'effet visuel "déjà inscrit" n'apparaît qu'au prochain événement qui change l'état UI (page, sélection, slot grimoire). Le cache de l'écran ignore le set des sorts inscrits.

### Files to modify

- `src/main/java/tong/statmod/client/inscription/KnownSpellUiState.java`
- `src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java`
- `src/test/java/tong/statmod/client/inscription/` (test couvrant l'égalité du record)

### Tasks

- [ ] **C0.1** — Ajouter `Set<String> boundSpellIds` au record `KnownSpellUiState`.
- [ ] **C0.2** — Adapter `KnownSpellUiState.capture(...)` pour accepter le set.
- [ ] **C0.3** — Dans le mixin, calculer `boundSpellIds` **avant** la comparaison `nextState.equals(this.statmod$lastKnownSpellState)` et le passer à `capture(...)`.
- [ ] **C0.4** — Test unitaire : deux `KnownSpellUiState` identiques sauf `boundSpellIds` doivent renvoyer `equals == false`.
- [ ] **C0.5** — `./gradlew test` vert.

### Acceptance

Inscrire un sort en gardant le menu ouvert → la bordure verte apparaît au tick suivant sans avoir à bouger autre chose.

---

## Mission A — Mana cost réel par sort Tensura

**But :** Aligner `getManaCost(int)` sur `Magic.getMagiculeCost(entity, instance, mode)` au lieu des défauts génériques par discipline.

### Challenge

Iron's appelle `getManaCost(int spellLevel)` SANS contexte joueur (UI tooltip, validation). `getMagiculeCost(entity, instance, mode)` exige un `LivingEntity`. Solution : utiliser une "valeur de référence" calculée au boot avec un entity stub (`null` tolerated ? sinon utiliser `Minecraft.getInstance().player` côté client + valeur par défaut côté serveur).

### Files to modify

- `src/main/java/tong/statmod/integration/ironspells/bridge/TensuraSpellMetadata.java` — query `getMagiculeCost`
- `src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java` — `getManaCost(int)`

### Tasks

- [ ] **A.1** — Inspecter bytecode `Magic.getMagiculeCost` — vérifier si tolère `entity == null` ou s'appuie sur ses attributs.
- [ ] **A.2** — Cas tolérant null : query au boot dans `TensuraSpellMetadata.query(skillId)`, stocker `double baselineMagiculeCost`.
- [ ] **A.3** — Cas non tolérant : alternative — query **à la première vraie utilisation** (premier `onServerPreCast`), cacher par skillId.
- [ ] **A.4** — Adapter `getManaCost(int)` pour retourner `(int) Math.ceil(baselineMagiculeCost * MAGICULE_TO_MANA_RATIO)`. Le ratio commence à 1.0 — calibrer après test.
- [ ] **A.5** — Test : `TensuraSpellMetadataTest` couvre le cache + valeurs non-zéro pour les sorts connus.
- [ ] **A.6** — Document du ratio dans `STAT-DEC-MAGICULE-MANA-RATIO`.

### Acceptance

Fire Bolt coûte ≈ son vrai magicule cost converti en mana Iron's. Sort gros (Hellfire-style) coûte plus que sort léger.

### Exit conditions

Si A.1 montre que `getMagiculeCost` panique sur null, replier sur A.3. Si A.3 introduit trop de complexité (lazy + thread-safety), revenir au statu quo discipline-based et documenter le pourquoi.

---

## Mission B — Mastery Tensura → spell level Iron's

**But :** Plus tu maîtrises une compétence côté Tensura, plus elle est puissante côté Iron's. Branche `ManasSkillInstance.getMastery()` (0.0 à `getMaxMastery()`) sur le `spellLevel` Iron's (1 à `getMaxLevel()`).

### Files to modify

- `src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java`
- Tests existants

### Tasks

- [ ] **B.1** — Décider du mapping : linéaire (`level = 1 + mastery / maxMastery * (maxLevel - 1)`) ou tier-based.
- [ ] **B.2** — Faire passer `getMaxLevel()` de 1 à 5 (ou autre — décision dans `STAT-DEC-MASTERY-MAPPING`).
- [ ] **B.3** — Override `getLevelFor(int spellLevel, LivingEntity caster)` ? À vérifier — la signature dans Iron's montre `getLevelFor(int, LivingEntity)`, ça peut être le bon point d'extension.
- [ ] **B.4** — Test couvrant : mastery 0 → level 1, mastery max → level max.

### Acceptance

Un joueur avec mastery élevée sur Fire Bolt voit `getLevelFor` retourner un niveau > 1 dans Iron's, ce qui scale les dégâts via `getSpellPower(level, entity)`.

### Risque

Iron's `getLevelFor` est `final` — si oui, on retombe sur un Mixin (pattern déjà éprouvé pour l'icône). Documenter dans `STAT-DEC-MASTERY-MAPPING`.

---

## Mission C1 — Polish UX inscription menu

**But :** Recherche par nom, filtre par école, tri par tier. Optionnel : scrollbar à la place de la pagination par flèches.

### Files to modify

- `src/main/java/tong/statmod/client/inscription/` (nouveau widget search bar)
- `src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java`

### Tasks

- [ ] **C1.1** — Widget `EditBox` pour recherche, ancré au-dessus de la colonne d'icônes.
- [ ] **C1.2** — Filtre par école : un bouton-icône par école (Fire/Ice/...) qui toggle.
- [ ] **C1.3** — Refactor `IronInscriptionKnownSpellIndex.page(...)` pour accepter un prédicat.
- [ ] **C1.4** — Test unitaire couvrant filtre + recherche combinés.

### Acceptance

Taper "fire" dans la barre filtre la liste à Fire Bolt + Fireball + Fire Storm + etc. en moins d'une frame.

### Hors scope

Scrollbar — tracking séparé si l'utilisateur le demande après C1.4.

---

## Missions reportées (D, E)

### D — Wrappers pour Skills / BattleWill Tensura

Gros chantier (hors périmètre Magic). Demande de réécrire `TensuraSpellTaxonomy` pour couvrir les disciplines non-magic. Document à part : `2026-06-XX-tensura-non-magic-wrappers.md`.

### E — Phase 2 magic branches (Water/Air/Earth/Holy/Blood/...)

Mission gouvernée séparément — voir `docs/superpowers/specs/2026-06-21-irons-spellbooks-unified-magic-tree-design.md` §Phase 2. Demande du contenu (spells Iron's mappés par branche) avant code.

---

## Done definition globale

✅ Mission F validée (cercle + projectile + icône)
✅ Mission C0 fermée (bug visuel inscription)
✅ Mission A décidée (live OR statu quo documenté)
✅ Mission B décidée (live OR statu quo documenté)
✅ Mission C1 livrée (recherche + filtre)
✅ Decision records pour chaque mission archivés dans `.tmp-onivo-audit/decisions/`

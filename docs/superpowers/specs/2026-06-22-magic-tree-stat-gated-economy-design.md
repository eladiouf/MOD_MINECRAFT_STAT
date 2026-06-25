# Magic Tree — Unified Points + Stat-Gated Economy (v2)

**Date :** 2026-06-22
**Status :** Draft for fondateur confirmation
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Vision fondateur (verbatim 2026-06-22)

> "l'économie devrait être des points unified mais que le déblocage des sorts nécessite certaines stats à certains niveaux"
>
> "arcane power, erudition sont obligatoires pour chaque sort. Quelle stat tertiaire est nécessaire, cela dépend du sort."

## Modèle final

Chaque nœud magique passe **3 gates de stats** :

1. **`ARCANE_POWER`** ≥ seuil(tier) — universel
2. **`ERUDITION`** ≥ seuil(tier) — universel
3. **Stat tertiaire** ≥ seuil(tier) — **dépend du rôle du sort** (pas de la branche)

Plus :
- **Monnaie unique `magicPoints`** (remplace `arcanePoints` + `Map<Branch, schoolPoints>`)
- **Prerequisites de nœuds** (le tree topologique reste — Fire signatures requièrent l'opener Fire, etc.)
- **Race** : modifie les seuils tertiaires, pas les coûts

## Décisions

### D1 — Taxonomie des rôles de sort (`SpellRole`)

Chaque `MagicNode` est tagué avec **un seul rôle dominant**. La tertiaire est dérivée de ce rôle via une table statique.

| Rôle | Stat tertiaire | Identité de jeu | Exemples |
|---|---|---|---|
| `ELEMENTAL_DAMAGE_FIRE` | `FIRE_AFFINITY` | Damage élémental pur, signature de l'élément | Firebolt, Fire Arrow, Tensura Fire Bolt, Scorch |
| `ELEMENTAL_DAMAGE_WATER` | `WATER_AFFINITY` | Damage glace/eau pur | Icicle, Snowball, Ray of Frost, Cone of Cold |
| `ELEMENTAL_DAMAGE_AIR` | `AIR_AFFINITY` | Damage foudre/vent pur | Lightning Bolt, Wind Blade, Iron Slash |
| `ELEMENTAL_DAMAGE_EARTH` | `EARTH_AFFINITY` | Damage terre/poison pur | Stomp, Poison Arrow, Acid Orb |
| `AOE_BLAST` | `INTIMIDATION` | Explosion / large zone instantanée | Fireball, Earthquake, Blizzard, Meteor Storm, Hellfire |
| `DOT_ZONE` | `WILLPOWER` | Zone persistante DOT (>5s) | Wall of Fire, Acid Rain, Cloud of Regeneration, Blight |
| `MOBILITY` | `AGILITY` | Déplacement actif (dash, step, leap) | Burning Dash, Frost Step, Wind Jump, Thunder Step, Blood Step |
| `BUFF_EMPOWERMENT` | `PHYSICAL_ENDURANCE` | Buff self ou allié | Haste, Oakskin, Strength, Fortify, Tailwind |
| `HEAL` | `WILLPOWER` | Restauration HP | Heal, Greater Heal, Cleanse, Healing Circle, Cloud of Regeneration |
| `SUMMON` | `TRACKING` | Invocation entité contrôlée | Summon Polar Bear, Vex, Wisp, Spider Aspect, Firefly Swarm |
| `CONTROL_BIND` | `CASTING_SPEED` | Immobilisation / bind / banish | Root, Bind, Ensnare, Shadow Bind, Banish, Curse Bind |
| `BARRIER_DEFENSIVE` | `MAGIC_RESISTANCE` | Mur / bulle / protection | Ice Block, Angel Wings, Magic Wall, Reinforced Barrier |
| `PRECISION_STRIKE` | `PRECISION` | Tir précis longue portée | Lightning Lance, Guiding Bolt, Ice Spikes (targeted), Lightning Bolt (when single target) |
| `SPATIAL_VOID` | `KEEN_SENSES` | Téléport, portail, vide | Black Hole, Portal, Aspect Change, Ender skills |
| `CORRUPTION_CURSE` | `WILLPOWER` | Eldritch, drain, malédiction | Eldritch spells, Devour, Blood Slash (HP drain) |
| `TRUNK_FOUNDATION` | — (aucune tertiaire) | Fondation commune, pas un sort | Arcane Focus, Mana Well, Cast Discipline, Multi-School Gate |
| `BRANCH_OPENER` | (matching element affinity, ou stat thématique) | Ouverture d'école | Fire Ignition, Water Awakening, Sanguine Awakening |
| `BRANCH_TIER` | (escalating identity stat) | Progression interne | Ember Path, Flame Path, Inferno Path |

#### Règles de classification pour les ambiguïtés

Quand un sort coche plusieurs cases, l'ordre de priorité est :
1. **MOBILITY** > tout (si le sort déplace le caster, c'est sa signature)
2. **HEAL** > tout autre (un sort qui soigne est définitionnellement un heal)
3. **SUMMON** > AOE/DOT (l'invocation reste l'identité même si l'entité spam des AOE)
4. **BARRIER_DEFENSIVE** > DOT_ZONE (Wall of Fire = barrière si elle bloque, DOT si traversable)
5. **ELEMENTAL_DAMAGE_<X>** > AOE_BLAST si single-target / petit cone
6. **AOE_BLAST** > ELEMENTAL_DAMAGE si rayon ≥ 4 blocs
7. **CORRUPTION_CURSE** > tout pour les écoles Eldritch/Blood (override thématique)

Documenté dans `SpellRole.classificationRules()` (commentaire JavaDoc).

#### Cas particuliers Branch opener / Branch tier

Les opener et tier nodes ne sont **pas des sorts**, ce sont des paliers de structure. Leur tertiaire est défini séparément :

| Branche | Opener tertiaire | Tier T1 | Tier T2 | Tier T3 |
|---|---|---|---|---|
| FIRE | `FIRE_AFFINITY` (≥ 2) | `FIRE_AFFINITY` (≥ 1) | `FIRE_AFFINITY` (≥ 3) | `INTIMIDATION` (≥ 5) |
| WATER | `WATER_AFFINITY` (≥ 2) | `WATER_AFFINITY` (≥ 1) | `WATER_AFFINITY` (≥ 3) | `MAGIC_RESISTANCE` (≥ 5) |
| AIR | `AIR_AFFINITY` (≥ 2) | `AIR_AFFINITY` (≥ 1) | `AIR_AFFINITY` (≥ 3) | `AGILITY` (≥ 5) |
| EARTH | `EARTH_AFFINITY` (≥ 2) | `EARTH_AFFINITY` (≥ 1) | `EARTH_AFFINITY` (≥ 3) | `PHYSICAL_ENDURANCE` (≥ 5) |
| HOLY | `WILLPOWER` (≥ 2) | `WILLPOWER` (≥ 1) | `ERUDITION` (≥ 3) | `WILLPOWER` (≥ 5) |
| BLOOD | `PHYSICAL_ENDURANCE` (≥ 2) | `PHYSICAL_ENDURANCE` (≥ 1) | `INTIMIDATION` (≥ 3) | `WILLPOWER` (≥ 5) |
| ENDER | `KEEN_SENSES` (≥ 2) | `KEEN_SENSES` (≥ 1) | `KEEN_SENSES` (≥ 3) | `KEEN_SENSES` (≥ 5) |
| EVOCATION | `TRACKING` (≥ 2) | `ERUDITION` (≥ 1) | `TRACKING` (≥ 3) | `ERUDITION` (≥ 5) |
| ELDRITCH | `WILLPOWER` (≥ 2) | `MAGIC_RESISTANCE` (≥ 1) | `WILLPOWER` (≥ 3) | `WILLPOWER` (≥ 5) |
| COMMON | — | — | — | — |

Note : pour Tier T3 le tertiaire **bascule** sur la stat d'archétype identitaire de l'école (INTIMIDATION pour Fire, MAGIC_RESISTANCE pour Water, etc.) parce qu'à ce stade le mage n'a plus besoin de prouver son affinité élémentale (déjà acquis aux tiers inférieurs) mais son intégration dans l'identité de l'école.

### D2 — Seuils universels (ARCANE_POWER + ERUDITION)

| Node kind / Tier | `ARCANE_POWER` ≥ | `ERUDITION` ≥ |
|---|---|---|
| `TRUNK_FOUNDATION` T1 | 1 | 1 |
| `TRUNK_FOUNDATION` T2 | 2 | 2 |
| `TRUNK_FOUNDATION` T3 | 3 | 3 |
| `BRANCH_OPENER` T1 | 2 | 1 |
| `BRANCH_TIER` T1 | 2 | 2 |
| `BRANCH_TIER` T2 | 4 | 3 |
| `BRANCH_TIER` T3 | 6 | 5 |
| `SIGNATURE_SPELL` T1 | 2 | 2 |
| `SIGNATURE_SPELL` T2 | 4 | 3 |
| `SIGNATURE_SPELL` T3 | 6 | 5 |
| `LATEGAME_GATE` | 10 | 7 |

### D3 — Rôle de la race (révisé)

Plus de modificateur de coût. La race agit sur les **seuils tertiaires** uniquement :

- **Affinité naturelle élémentale matching** : `-1` sur le seuil tertiaire si le tertiaire est l'affinité élémentale correspondante
  - Exemple : Dwarf (affinité Fire+Earth) sur un sort où tertiaire = `FIRE_AFFINITY` → seuil −1
- **Branche de départ choisie** : `-1` sur le seuil tertiaire pour tous les nœuds de cette branche, quel que soit le tertiaire
- **Purity penalty (Beast)** : `+1` sur le seuil tertiaire des nœuds dans une branche non-affine

Les effets **se cumulent** algébriquement, minimum 0 (jamais en-dessous). Race n'affecte **jamais** les seuils ARCANE_POWER ou ERUDITION (ce sont les gates "tu es un mage", non-négociables).

### D4 — Monnaie unique `magicPoints`

- `PlayerStatData` : nouveau champ `int magicPoints`. Anciens `arcanePoints` et `schoolPoints` conservés en lecture seule pour migration mais non écrits.
- `MagicNode.cost` reste un entier. Plus de `currency` enum.
- `MagicCurrency` enum supprimé en Phase 2 (après migration vérifiée).

### D5 — Gains de `magicPoints`

Trois sources :

| Source | Trigger | Gain |
|---|---|---|
| **Cast event** | `IronSpellEventBridge.onPostCast` quand `manaFrac ≥ 0.25` | +1 (impact cast) |
| **Mastery palier** | `SchoolProgressTracker` atteint mastery 10 dans une école | +1 |
| **Mastery palier** | mastery 25 | +2 (cumulé +3) |
| **Mastery palier** | mastery 50 | +3 (cumulé +6) |
| **Quest reward** | hooks ouverts pour `IronSpellEventBridge` ou commande admin | variable |

Total visible pour le joueur : un seul compteur `magicPoints` dans l'UI Puffish.

### D6 — Migration data save

Au premier chargement après update :

```java
if (data.magicPoints == 0 && (data.arcanePoints > 0 || !data.schoolPoints.isEmpty())) {
    int migrated = data.arcanePoints + data.schoolPoints.values().stream().mapToInt(Integer::intValue).sum();
    data.magicPoints = Math.min(migrated, MIGRATION_CAP);  // MIGRATION_CAP = 100
    data.arcanePoints = 0;
    data.schoolPoints.clear();
    LOGGER.info("Migrated {} legacy magic points for player {}", migrated, player.getName().getString());
}
```

`MIGRATION_CAP = 100` pour éviter les valeurs absurdes accumulées par les vétérans en dev.

### D7 — UI Puffish

Chaque nœud du tree affiche dans son tooltip (additif à l'existant) :

```
🪄 Arcane Power : ≥ 4 (tu as 5) ✓
📜 Erudition : ≥ 3 (tu as 2) ✗
⚡ Agility : ≥ 3 (tu as 4) ✓
```

Si **toutes** les conditions passent : le bouton "déverrouiller" est actif. Sinon il est grisé avec affichage de la ou des conditions manquantes.

`MagicEligibilityResolver.evaluate` retourne désormais une `Result` enrichie qui contient :
- `failure` (existing)
- `adjustedCost` (existing)
- `missingStats` : `List<StatGate>` — liste des stats sous le seuil (vide si tout passe)

Le tooltip est rendu via une nouvelle méthode dans `MagicUnlockFeedbackMessageFactory`.

### D8 — Schéma `MagicNode` mis à jour

```java
public record MagicNode(
    String id,
    MagicBranch branch,
    MagicNodeKind kind,
    MagicTier tier,
    SpellRole role,           // NEW — détermine la tertiaire
    int cost,                 // unchanged (monnaie unique)
    List<String> prerequisites,
    Set<String> learnedSpells
) {}
```

Le champ `currency` (`MagicCurrency`) **supprimé**.

### D9 — Helper de seuils

```java
public final class MagicNodeStatRequirements {
    public record StatGate(StatType stat, int minLevel) {}
    public record Requirements(StatGate arcane, StatGate erudition, StatGate tertiary) {}

    public static Requirements forNode(MagicNode node) { ... }
    public static int effectiveThreshold(StatGate gate, PlayerStatData data) { ... }  // applique D3
}
```

Évite de polluer `MagicEligibilityResolver` avec les tables de mapping ; il se contente de consulter `MagicNodeStatRequirements.forNode(node)` puis de comparer aux stats du joueur.

## Exemples de bout-en-bout

### Firebolt (FIRE signature T1, role = ELEMENTAL_DAMAGE_FIRE)

Requirements bruts :
- ARCANE_POWER ≥ 2
- ERUDITION ≥ 2
- FIRE_AFFINITY ≥ 1

Pour un joueur Dwarf (affinité Fire+Earth) avec start branch = Fire :
- ARCANE_POWER ≥ 2 (race n'affecte pas)
- ERUDITION ≥ 2 (race n'affecte pas)
- FIRE_AFFINITY ≥ 1 - 1 (race affinité) - 1 (start branch) = **0** → toujours OK

### Tensura Hellfire (FIRE signature T3, role = AOE_BLAST)

Requirements bruts :
- ARCANE_POWER ≥ 6
- ERUDITION ≥ 5
- INTIMIDATION ≥ 5

Pour le même Dwarf start branch Fire :
- ARCANE_POWER ≥ 6 (unchanged)
- ERUDITION ≥ 5 (unchanged)
- INTIMIDATION ≥ 5 - 1 (start branch Fire) = 4 (affinité race n'aide pas, INTIMIDATION n'est pas un affinity stat)

### Frost Step (WATER signature T1, role = MOBILITY)

Requirements bruts :
- ARCANE_POWER ≥ 2
- ERUDITION ≥ 2
- AGILITY ≥ 1

Pour Beast (affinité Water+Air, purity penalty), pas start branch Water :
- ARCANE_POWER ≥ 2 (unchanged)
- ERUDITION ≥ 2 (unchanged)
- AGILITY ≥ 1 (AGILITY n'est pas une affinité, purity penalty Beast s'applique aux écoles non-affines mais Water EST affine pour Beast → pas de pénalité)

## Compatibilité avec l'existant

- `MagicTreeProgressionService.tryUnlock` reste l'entry point, mais le check d'éligibilité passe par la nouvelle `Requirements`.
- Les `learnedSpells` (incluant le wrapper Tensura) restent grant via `MagicNodeRuntimeRewards.apply()`.
- L'event bus `onSkillUnlock` côté Puffish reste l'entrée client → serveur.
- Le sync Puffish UI doit pousser le nouveau total `magicPoints` au lieu de la somme actuelle.

## Tests à refondre / créer

| Test | Action |
|---|---|
| `MagicEligibilityResolverTest` | Refondre — tester chacun des 3 gates + race interactions |
| `MagicTreeCatalogTest` | Vérifier que chaque node a un `SpellRole` non-null (sauf trunk) |
| `MagicTreeProgressionServiceTest` | Adapter aux nouveaux flux |
| `MagicNodeStatRequirementsTest` | **Nouveau** — couvre toutes les combinaisons role × tier × race |
| `SpellRoleClassificationTest` | **Nouveau** — vérifie les règles de priorité D1 |
| `PlayerStatDataMigrationTest` | **Nouveau** — vérifie la migration D6 |
| `PuffishMagicResourceConsistencyTest` | À mettre à jour (peut survivre si on ne change pas le layout JSON) |

## Risques résumés

1. **249 nœuds à tagger** — pour la majorité, le rôle peut être inféré automatiquement par mot-clé sur l'ID (`mobility` si "dash"/"step", `heal` si "heal"/"cleanse", etc.). Pour les ambiguïtés, classification manuelle. Estimation : 1h30 de revue manuelle.
2. **Race purity penalty (Beast)** — peut rendre certaines branches frustrantes. Acceptable comme identité jeu de la race "instinct vs raison".
3. **Migration data save** — `MIGRATION_CAP = 100` est arbitraire. À vérifier sur du save réel.
4. **Tooltip Puffish** — la table des stats nécessite une intégration UI propre. Si Puffish refuse les tooltips custom, fallback : afficher la liste dans la title du nœud (moins propre).

## Plan d'implémentation

Découpé en 6 missions dépendantes :

1. **Mission α** — Modèle de données (record `MagicNode` étendu, `SpellRole` enum, `MagicNodeStatRequirements` helper)
2. **Mission β** — Catalog tagging (les 249 nœuds reçoivent leur `SpellRole`)
3. **Mission γ** — `MagicEligibilityResolver` refondu + tests
4. **Mission δ** — `PlayerStatData` migration + `magicPoints` unifiés
5. **Mission ε** — `IronSpellEventBridge` + `SchoolProgressTracker` adaptés à la nouvelle économie
6. **Mission ζ** — UI Puffish tooltip avec requirements visibles

Chaque mission close avec un decision record dédié.

## Exit conditions

Refonte rejouée si :
- Fondateur change la liste des stats
- Fondateur revient à un système multi-monnaie
- Phase 2 du master plan ajoute une nouvelle branche → ajouter mapping dans D1 et seuils opener/tier dans D1 cas particuliers

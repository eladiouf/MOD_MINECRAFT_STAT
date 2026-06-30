# Per-Spell Boolean Condition Gates (v3)

**Date :** 2026-06-30
**Status :** Draft for review
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`
**Deprecates :** `2026-06-22-magic-tree-stat-gated-economy-design.md` (D1/D2/D3 tables replaced)

## Vision fondateur (verbatim 2026-06-30)

> "le fait de débloquer les sorts sous conditions des stats est trop simpliste, chaque sort doit avoir ses propres conditions, pas des conditions générales de tier."
>
> "je veux aussi un système de OU : race elf OU érudition 80."
>
> "conditions : stat level ≥ X, race == X, possède un sort / nœud spécifique, tier minimum dans une branche, niveau global ≥ X."

## Résumé

Les tables universelles D1/D2/D3 qui calculent les 3 portes stats par tier/nœud sont supprimées. Chaque `MagicNode` définit son propre **arbre d'expressions booléennes** avec AND/OR arbitraires. Les conditions feuilles incluent stats, race, sorts possédés, nœuds débloqués, tier de branche, niveau global.

---

## Section 1 — Modèle de données

### 1.1 Interface `Condition`

```java
public interface Condition {
    boolean evaluate(PlayerStatData data, MagicNodeResolver resolver);
}
```

Les implémentations concrètes :

| Type | Java Record | Évalue |
|---|---|---|
| AND | `And(List<Condition>)` | true ssi tous les enfants sont true |
| OR | `Or(List<Condition>)` | true ssi ≥1 enfant est true |
| Stat | `StatCondition(StatType, int minLevel)` | `data.getLevel(stat) >= minLevel` |
| Race | `RaceCondition(MagicRace race)` | `data.getMagicRace() == race` |
| Has spell | `HasSpellCondition(String spellId)` | `data.hasLearnedSpell(spellId)` |
| Has node | `HasNodeCondition(String nodeId)` | `data.hasMagicNode(nodeId)` |
| Branch tier | `BranchTierCondition(MagicBranch, MagicTier minTier)` | le plus haut tier débloqué dans `branch` ≥ `minTier` (résolu depuis les nœuds possédés) |
| Global level | `GlobalLevelCondition(int minLevel)` | `data.getLevel(StatType.GLOBAL) >= minLevel` |

### 1.2 Factory helpers

Pour la lisibilité dans le catalogue :

```java
public static Condition and(Condition... cs) { ... }
public static Condition or(Condition... cs) { ... }
public static Condition stat(StatType s, int min) { ... }
public static Condition race(MagicRace r) { ... }
public static Condition hasSpell(String id) { ... }
public static Condition hasNode(String id) { ... }
public static Condition globalLevel(int min) { ... }
public static Condition branchTier(MagicBranch b, MagicTier t) { ... }
```

### 1.3 Exemple concret en catalogue

```java
// Meteor (nœud FIRE T2 signature) :
// (Race(ELF) AND FIRE_AFFINITY ≥ 5) OR (ARCANE_POWER ≥ 10 AND ERUDITION ≥ 7)
node("fire/signature/meteor", FIRE, SIGNATURE_SPELL, T2, 12,
    List.of("fire/signature/firebolt"),
    Set.of("irons_spellbooks:meteor"),
    or(
        and(race(MagicRace.ELF), stat(FIRE_AFFINITY, 5)),
        and(stat(ARCANE_POWER, 10), stat(ERUDITION, 7))
    )
)
```

---

## Section 2 — Changements dans MagicNode

```java
public record MagicNode(
    String id,
    MagicBranch branch,
    MagicNodeKind kind,
    MagicTier tier,
    // int cost, ← inchangé
    // List<String> prerequisites, ← inchangé
    // Set<String> learnedSpells, ← inchangé
    // SpellRole role ← SUPPRIMÉ (remplacé par l'arbre de conditions, plus besoin de rôle pour calculer le tertiaire)
    Condition condition  // NOUVEAU : remplace D1/D2/D3 + SpellRole
)
```

- `SpellRole` et `SpellRoleInference` sont **supprimés** (plus besoin de mapper rôle → stat tertiaire)
- `MagicNodeStatRequirements` est **remplacé** par `MagicNodeConditionEvaluator` qui appelle `node.condition().evaluate(data, resolver)`
- `MagicNodeKind` et `MagicTier` **conservés** pour la topologie, le rendu UI et le clamp de niveau de sort

---

## Section 3 — Évaluateur

```java
public class MagicNodeConditionEvaluator {
    public static boolean evaluate(MagicNode node, PlayerStatData data) {
        if (node.condition() == null) return true; // pas de conditions spéciales
        return node.condition().evaluate(data, resolver);
    }

    public static List<String> describeMissing(MagicNode node, PlayerStatData data) {
        // Parcourt l'arbre, collecte les feuilles qui échouent
        // Retourne des messages lisibles pour l'UI
    }
}
```

Le résolveur `MagicNodeResolver` sait :
- Trouver le plus haut tier débloqué dans une branche (scan des nœuds possédés dans la branche)
- Résoudre les IDs de nœuds

---

## Section 4 — Remplacements

| Ancien (D1/D2/D3) | Nouveau |
|---|---|
| `MagicNodeStatRequirements.forNode(node)` → `Requirements(arcane, erudition, tertiary)` | `node.condition()` → un arbre `Condition` explicite |
| `SpellRole.tertiaryStat()` → mapping rôle → stat tertiaire | Plus nécessaire : la condition écrit directement la stat voulue |
| `RaceModifier` / `raceArcaneDelta` / `raceErudtionDelta` etc. | Remplacé par des conditions explicites du type `Or(Race(ELF), stat(ERUDITION, 3))` si un sort est plus facile pour les Elfes |
| `effectiveThreshold(gate, data, node)` avec ajustement racial | Disparu : chaque nœud exprime ses propres seuils sans magic race delta global |

---

## Section 5 — Migration des 249 nœuds

Pour chaque nœud existant, une condition est générée automatiquement depuis les anciennes tables D1/D2/D3 :

```java
// Généré depuis l'ancien système :
Condition autoMigrate(MagicNode node) {
    var req = MagicNodeStatRequirements.forNode(node); // ancienne table
    return and(
        stat(ARCANE_POWER, req.arcane().minLevel()),
        stat(ERUDITION, req.erudition().minLevel()),
        req.tertiary() != null ? stat(req.tertiary().stat(), req.tertiary().minLevel()) : null
    );
}
```

Ces conditions générées sont **écrites dans le catalogue** et peuvent être affinées sort par sort plus tard. Le comportement runtime est identique à l'ancien système après migration.

---

## Section 6 — Résumé des fichiers impactés

| Fichier | Action |
|---|---|
| `magic/Condition.java` | **NOUVEAU** — interface + records |
| `magic/ConditionEvaluator.java` | **NOUVEAU** — évaluateur + describeMissing |
| `magic/MagicNode.java` | **MODIFIÉ** — ajout `Condition condition`, suppression `SpellRole role` |
| `magic/MagicNodeStatRequirements.java` | **SUPPRIMÉ** |
| `magic/MagicNodeStatRequirementsTest.java` | **SUPPRIMÉ** |
| `magic/SpellRole.java` | **SUPPRIMÉ** |
| `magic/SpellRoleInference.java` | **SUPPRIMÉ** |
| `magic/SpellRoleTest.java` | **SUPPRIMÉ** |
| `magic/MagicTreeCatalog.java` | **MODIFIÉ** — chaque appel `add()` reçoit une `Condition` |
| `magic/MagicEligibilityResolver.java` | **MODIFIÉ** — utilise `ConditionEvaluator` au lieu de `MagicNodeStatRequirements` |
| `magic/MagicEligibilityResolverTest.java` | **MODIFIÉ** — adapté aux nouvelles conditions |
| `magic/MagicTreeProgressionService.java` | **MODIFIÉ** — utilise le nouvel évaluateur |
| `integration/puffish/PuffishMagicTreeBuilder.java` | **MODIFIÉ** — rendu UI des conditions (arbre → texte lisible) |
| `integration/ironspells/IronSpellEventBridge.java` | Inchangé (utilise `hasLearnedSpell`) |

---

## Section 7 — Considérations UI (Puffish)

Le rendu des conditions dans l'arbre Puffish doit devenir un texte lisible et structuré. Exemple :

```
Conditions requises :
  • ARCANE_POWER ≥ 10
  • ERUDITION ≥ 7
  • Race : Elfe OU FIRE_AFFINITY ≥ 5
```

`ConditionEvaluator.describeMissing()` sera utilisé pour mettre en évidence les conditions non remplies en rouge.

---

## Section 8 — Non-changements

- **Prerequisites de nœuds** : restent dans `MagicNode.prerequisites()` (topologie du tree)
- **Coût en magicPoints** : reste dans `MagicNode.cost()`
- **Tier / Kind** : conservés pour le rendu UI et le clamp de niveau de sort (`onModifySpellLevel`)
- **MagicEligibilityResolver** : garde son flow, seule l'étape STAT_REQUIREMENT_NOT_MET change
- **IronSpellEventBridge.onPreCast** : inchangé

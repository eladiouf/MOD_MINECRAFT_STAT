# STAT-DEC-MAGIC-UNIFIED-ECONOMY — Refonte économie + gates stats

**Date :** 2026-06-22
**Mission :** M5 (refonte économie magique)
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Vision validée par le fondateur

> "L'économie doit être des points unified. Le déblocage des sorts nécessite certaines stats à
> certains niveaux. ARCANE_POWER + ERUDITION sont obligatoires pour chaque sort. La stat
> tertiaire dépend du sort. Les races doivent intervenir."

## Modèle final acté

Chaque nœud du tree magique passe **3 gates de stats** :

1. **ARCANE_POWER** ≥ seuil(tier, race) — universel
2. **ERUDITION** ≥ seuil(tier, race) — universel
3. **Stat tertiaire** ≥ seuil(tier, race, affinité) — dépend du **rôle du sort** (taxonomy `SpellRole`)

Plus la monnaie unique `magicPoints` (remplace arcanePoints + schoolPoints).

## Spec complet

Détails design dans `docs/superpowers/specs/2026-06-22-magic-tree-stat-gated-economy-design.md` :
- D1 : Taxonomie `SpellRole` (15 rôles)
- D2 : Seuils universels par tier
- D3 : Race intervient sur **les 3 gates** (delta sur ARCANE_POWER/ERUDITION ET sur tertiaire)
- D4 : Monnaie unique `magicPoints`
- D5 : 3 sources de gains (cast + paliers mastery + admin)
- D6 : Migration save avec cap 100
- D7 : Tooltip Puffish avec requirements visibles
- D8 : `MagicNode` étendu avec `SpellRole role`, retire `MagicCurrency`
- D9 : Nouveau helper `MagicNodeStatRequirements`

## Plan d'exécution (6 missions séquencées)

1. **Mission α** — Modèle de données : `SpellRole` enum, `MagicNode` étendu, `MagicNodeStatRequirements` helper
2. **Mission β** — Catalog tagging : tous les 249 nœuds reçoivent leur `SpellRole`
3. **Mission γ** — `MagicEligibilityResolver` refondu pour consulter les 3 gates + race
4. **Mission δ** — `PlayerStatData` migration `arcane + school → magicPoints`, suppression `MagicCurrency`
5. **Mission ε** — `IronSpellEventBridge` + `SchoolProgressTracker` adaptés à la nouvelle économie
6. **Mission ζ** — UI Puffish tooltip avec stats requirements affichés

Chaque mission close avec ses propres tests et un decision record dédié si elle introduit une décision nouvelle.

## Exit conditions

Refonte rejouée si :
- Fondateur change la liste des 22 stats
- Fondateur revient à un système multi-monnaie
- Phase 2 master plan ajoute une nouvelle branche (mapping D1 cas spécial à compléter)
- Tests playtest révèlent un mauvais pacing (Tier 3 trop accessible ou trop bloqué)

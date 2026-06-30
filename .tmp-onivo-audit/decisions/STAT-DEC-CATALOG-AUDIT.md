# STAT-DEC-CATALOG-AUDIT — Audit Mission O des classifications SpellRole

**Date :** 2026-06-23
**Mission :** O — Phase 2 polish (audit + fine-tuning)
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Méthode

Audit manuel des 249 nœuds du catalog en confrontant l'inférence automatique
(`SpellRoleInference`) au comportement réel des sorts Iron's / Tensura connus.

Tool de support : `MagicCatalogAuditTool` (main qui dump distribution par branche + détail
signature). Run via `./gradlew runMagicCatalogAudit` (nécessite libs/ pour les mods runtime,
mais le tool lui-même ne dépend pas des libs au compile).

## Findings — Mauvaises classifications corrigées

### Sorts Fire mal classés en damage direct

| Nœud | Avant | Après | Raison |
|---|---|---|---|
| `fire/signature/pyromaniac` | ELEMENTAL_DAMAGE_FIRE | `BUFF_EMPOWERMENT` | "Pyromaniac" = transformation/obsession self-buff, pas un sort de damage |
| `fire/signature/heat_surge` | ELEMENTAL_DAMAGE_FIRE | `BUFF_EMPOWERMENT` | "Surge" = burst énergétique temporaire, buff |
| `fire/signature/flaming_strike` | ELEMENTAL_DAMAGE_FIRE | `BUFF_EMPOWERMENT` | Weapon enhancement, pas un projectile |

### Reclassement SUMMON → BUFF

| Nœud | Avant | Après | Raison |
|---|---|---|---|
| `earth/signature/spider_aspect` | SUMMON | `BUFF_EMPOWERMENT` | Self-transform (gain spider features), pas une invocation d'entité externe |

### Reclassement BUFF → PRECISION

| Nœud | Avant | Après | Raison |
|---|---|---|---|
| `holy/signature/divine_smite` | BUFF_EMPOWERMENT | `PRECISION_STRIKE` | Single-target holy damage ranged, l'identité gameplay est précision divine |

## Findings — Classifications confirmées correctes

L'audit a validé l'inférence pour ces cas notables :
- `frost_step`, `burning_dash`, `thunder_step`, `blood_step` → `MOBILITY` ✓
- `wall_of_fire`, `acid_rain`, `cloud_of_regeneration` → `DOT_ZONE` ✓
- `polar_bear`, `vex`, `wisp`, `doppelganger`, `firefly_swarm` → `SUMMON` ✓
- `magic_wall`, `barrier`, `multilayer_barrier`, `ice_block` → `BARRIER_DEFENSIVE` ✓
- `fireball`, `meteor_storm`, `hellfire`, `blizzard`, `earthquake` → `AOE_BLAST` ✓
- `lightning_lance`, `guiding_bolt`, `fire_arrow`, `ice_spikes` → `PRECISION_STRIKE` ✓
- Tous les sorts Ender + `_portal` / `void` / `dimension` → `SPATIAL_VOID` ✓
- Tous les sorts Eldritch + Blood `_drain` / `_siphon` → `CORRUPTION_CURSE` ✓

## Décision tuning costs / thresholds

**Pas de changement** sur les coûts magic points par défaut (1/2/3 par tier) ni sur les
seuils `CastRewardPolicy` (0.05 spam, 0.25 impact) ni sur les paliers
`SchoolProgressTracker` (1000/2500/5000).

Raison : ces valeurs sont calibrées par design pour donner ~200 casts impact pour le
premier palier (1 point bonus) et 1000 casts pour le dernier (3 points bonus cumulés).
C'est cohérent avec un pacing mid-to-late game. Une vraie session playtest est requise
pour valider ou ajuster — pas de tuning à l'aveugle.

## Hors scope confirmé

- Pas d'ajout de nouveau rôle (les 15 existants couvrent l'identité gameplay)
- Pas de refonte du SpellRoleInference priority order (les 12 priorités tiennent)
- Pas de modification des seuils D2 (économie) ni D3 (race deltas) — voir
  `STAT-DEC-MAGIC-UNIFIED-ECONOMY` qui acte ça

## Tests ajoutés

`SpellRoleInferenceTest` enrichi de 4 assertions ciblées sur les reclassements :
- `pyromaniac_classified_as_BUFF_EMPOWERMENT`
- `heat_surge_classified_as_BUFF_EMPOWERMENT`
- `flaming_strike_classified_as_BUFF_EMPOWERMENT`
- `divine_smite_classified_as_PRECISION_STRIKE`

Ces tests verrouillent les décisions de l'audit. Si une future modif de `SpellRoleInference`
les casse, c'est un signal à investiguer.

## Exit conditions

Re-audit complet si :
- Iron's Spellbooks ajoute des sorts → revue de leur classification
- Nouvelles écoles ajoutées au catalog → audit branch-spécifique
- Playtest révèle qu'une stat tertiaire est systématiquement sous- ou sur-utilisée
- Le fondateur veut introduire des sous-rôles plus fins (ex : MOBILITY_TELEPORT vs
  MOBILITY_DASH)

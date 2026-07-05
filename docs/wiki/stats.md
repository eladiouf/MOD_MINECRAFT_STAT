---
title: Stats
description: "Liste complète des 23 stats"
nav_order: 4
---

# Stats

STAT MOD expose 23 stats runtime : 14 stats de base combat/magie et 9 stats de support/progression.

## Stats Actives (14)
| Stat | Catégorie | Index |
|------|-----------|-------|
| Brute Force | FRONTLINE_PHYSICAL_COMBAT | 0 |
| Blade Technique | FRONTLINE_PHYSICAL_COMBAT | 1 |
| Rapidité | FRONTLINE_PHYSICAL_COMBAT | 2 |
| Agility | FRONTLINE_PHYSICAL_COMBAT | 3 |
| Physical Resistance | FRONTLINE_PHYSICAL_COMBAT | 4 |
| Physical Endurance | FRONTLINE_PHYSICAL_COMBAT | 5 |
| Precision | RANGED_HUNT_CONTROL | 6 |
| Arcane Power | MAGICAL_CORE | 7 |
| Water Affinity | ELEMENTAL_SPECIALIZATION | 8 |
| Earth Affinity | ELEMENTAL_SPECIALIZATION | 9 |
| Fire Affinity | ELEMENTAL_SPECIALIZATION | 10 |
| Air Affinity | ELEMENTAL_SPECIALIZATION | 11 |
| Magic Resistance | MAGICAL_CORE | 12 |
| Casting Speed | MAGICAL_CORE | 13 |

## Stats Magiques (9)
| Stat | Catégorie | Index |
|------|-----------|-------|
| Mana Pool | MAGICAL_CORE | 14 |
| Erudition | MAGICAL_CORE | 15 |
| Tracking | RANGED_HUNT_CONTROL | 16 |
| Keen Senses | RANGED_HUNT_CONTROL | 17 |
| Forging | CRAFTING_SUPPORT | 18 |
| Cooking | CRAFTING_SUPPORT | 19 |
| Alchemy | CRAFTING_SUPPORT | 20 |
| Intimidation | MENTAL_PRESSURE_RESILIENCE | 21 |
| Willpower | MENTAL_PRESSURE_RESILIENCE | 22 |

## Pools Mana et Stamina

- Stamina STATMod : reserve longue de base 300, +3 par niveau de Physical Endurance. Pas de regeneration passive en idle; sprint draine lentement, Epic Fight recoit un `MAX_STAMINA` aligne sur ce gros pool et ses couts sont consommes par STATMod, les drains maintenus ParCool restent bas, et le sommeil remet la reserve a plein.
- Mana Iron's Spellbooks : Mana Pool augmente surtout la reserve maximale (+3 par niveau, +50 avec le core perk). STATMod neutralise la regeneration passive de base de l'attribut Iron's `MANA_REGEN`, mais conserve les remboursements de mana apres cast, les restores explicites et les bonus/perks qui ne sont pas de la regen passive. Les sources XP de cast/inscription Iron's sont enregistrees uniquement si Iron's Spellbooks est charge.
- Respawn : les niveaux, XP, perks, progression magique, progression donjon, stamina et fatigue sont copies explicitement sur `PlayerEvent.Clone`, donc mourir ne remet pas les stats a zero.

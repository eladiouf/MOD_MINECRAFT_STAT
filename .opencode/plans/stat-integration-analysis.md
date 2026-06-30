# Analyse d'Intégration par Stat

Ce document détaille pour chaque stat ce dont elle a besoin pour être **complètement intégrée** : mécanique, events, attributs, XP, perks, races, réseau, UI.

---

## Structure d'une Stat

Chaque stat a besoin de ces 8 couches :

```
1. DATA          → PlayerStatData (level, xp, perkPoints)
2. EFFECT        → StatEffectApplier (event hook → effet immédiat)
3. ATTRIBUTE     → StatAttributeHandler (attribute modifier périodique)
4. XP SOURCE     → CombatXPHandler / NonCombatXPHandler (comment on gagne XP)
5. PERKS         → PerkEffectHandler (6 perks × effets)
6. RACE BONUS    → RaceModifierRegistry (flat + XP multi par race)
7. INTEGRATION   → Mod-specific (Tensura, EpicFight, etc.)
8. UI            → StatTabScreen + PerkScreen (affichage)
```

---

## 1. BRUTE_FORCE (index 0) — ✅ Bien intégré

### Mécanique
Bonus dégâts armes lourdes (haches, masses, épées lourdes).

### Implémenté
- **Effect:** `StatEffectApplier:23-25` → `dmg *= 1 + brute*0.005`
- **XP:** `CombatXPHandler:67-68` → axes → BRUTE
- **Perks:** 6 perks (0-5) dans `PerkEffectHandler`
- **Race:** Ogre/Dragon/Giant/Orc → +BRUTE

### Manque
- ❌ **Attribute:** Aucun attribut Vanilla. Pourrait lier `Attributes.ATTACK_DAMAGE` ? Non, mieux vaut rester dans l'event damage.
- ❌ **EpicFight:** `EpicFightCompat.java` utilise déjà `onDealDamage` pour l'impact (posture damage). Manque lien BRUTE → `baseImpact`.
- ❌ **ParCool:** ChargeJump → BRUTE + AGIL (granular XP)
- ❌ **Tensura:** Giant Strength Extra Skill si BRUTE_TRANSCENDENCE

### Dépendances
- `LivingDamageEvent.Pre` ✅
- `CombatXPHandler.resolveWeaponStat()` ✅

---

## 2. BLADE_TECHNIQUE (index 1) — ✅ Bien intégré

### Mécanique
Dégâts avec épées/katana/dagger, précision et finesse.

### Implémenté
- **Effect:** Fusionné avec BRUTE dans `StatEffectApplier:23-25`
- **XP:** `CombatXPHandler:70-75` → swords/katana/dagger → BLADE
- **Perks:** 6 perks (6-11)
- **Race:** Ogre/Kijin → +BLADE

### Manque
- ❌ **EpicFight combo:** `EpicFightCompat` a déjà `ON_DODGE`, `COMBO_ATTACK`. Manque hook BLADE → combo damage bonus.
- ❌ **Tensura:** Strengthen Body skill si BLADE_TRANSCENDENCE

---

## 3. RAPIDITE (index 2) — ✅ Bien intégré

### Mécanique
Attack speed, fluidité, double strikes.

### Implémenté
- **Effect:** `StatEffectApplier:39-42` → 0.1% chance double damage par niveau
- **Attribute:** `StatAttributeHandler:39-40` → `ATTACK_SPEED` +0.2%/niveau
- **XP:** `CombatXPHandler` → pas encore de mapping dédié (fallback BRUTE)
- **Perks:** 6 perks (12-17)
- **Race:** Goblin → +RAPIDITE

### Manque
- ❌ **XP source:** Aucune arme ne donne RAPIDITE actuellement ! `resolveWeaponStat` ne retourne jamais RAPIDITE. Besoin d'un mapping fin (daggers, rapides → RAPIDITE)
- ❌ **ParCool:** FastRun, Slide → RAPIDITE
- ❌ **Tensura:** Thought Acceleration skill si RAPID_TRANSCENDENCE

### Dépendances critiques
- `CombatXPHandler.resolveWeaponStat()` à enrichir

---

## 4. AGILITY (index 3) — ✅ Bien intégré

### Mécanique
Mouvement, esquive, dégâts en mouvement.

### Implémenté
- **Effect:** `StatEffectApplier:49-52` → moving damage +0.2%/niveau ; `LivingFallEvent:86-89` → fall distance -0.5%/niveau
- **Attribute:** `StatAttributeHandler:42-43` → `MOVEMENT_SPEED` +0.1%/niveau
- **XP:** `NonCombatXPHandler` → `ActionType.PARKOUR` → AGILITY
- **Perks:** 6 perks (18-23)
- **Race:** Beastfolk/Harpy/Goblin → +AGILITY

### Manque
- ❌ **ParCool:** Vault, WallJump, Dodge, Roll → AGILITY (granular XP)
- ❌ **JUMP_STRENGTH attribute:** Pas de modifieur jump actuellement !
- ❌ **Tensura:** Ultra Instinct skill si AGIL_TRANSCENDENCE

### Dépendances critiques
- `Attributes.JUMP_STRENGTH` → `StatAttributeHandler` à étendre

---

## 5. PHYSICAL_RESISTANCE (index 4) — ✅ Bien intégré

### Mécanique
Réduction de dégâts physiques entrants.

### Implémenté
- **Effect:** `StatEffectApplier:56-57` → `dmg *= 1 - min(0.5, phys*0.005)`
- **XP:** Aucune source directe (passive)
- **Perks:** 6 perks (24-29)
- **Race:** Ogre/Dwarf/Dragon → +PHYS_RESIST

### Manque
- ❌ **XP source:** Aucune ! « Être frappé » devrait donner un peu d'XP (quantité basée sur dégâts reçus). `LivingDamageEvent.Pre` côté victime.
- ❌ **EpicFight:** Stun resistance via WILLPOWER, pas PHYS_RESIST.
- ❌ **Tensura:** Infinite Regeneration → RESIST_TRANSCENDENCE

### Dépendances
- `LivingDamageEvent.Pre` côté victime ✅ (existe, manque juste XP)

---

## 6. PHYSICAL_ENDURANCE (index 5) — ✅ Intégré partiel

### Mécanique
Stamina, absorption, saturation.

### Implémenté
- **Effect:** `StatEffectApplier:68-69` → `dmg *= 1 - min(0.3, endur*0.002)`
- **Effect:** `LivingFallEvent:90-93` → fall damage multi -0.3%/niveau
- **Perks:** 6 perks (30-35)
- **Race:** Slime/Orc/Giant → +ENDURANCE

### Manque
- ❌ **XP source:** `ActionType.SWIMMING` → ENDURANCE (existe dans `ActionType` mais pas branché dans `NonCombatXPHandler` !)
- ❌ **Attribute:** `MAX_HEALTH` modifier ? Ou absorption hearts via tick ?
- ❌ **ParCool:** ClingToCliff → ENDURANCE (granular XP)
- ❌ **Tensura:** Ultaspeed Regeneration → ENDUR_TRANSCENDENCE

### Dépendances critiques
- Brancher `SWIMMING` → XP handler
- `PlayerTickEvent` pour absorption

---

## 7. PRECISION (index 6) — ✅ Intégré partiel

### Mécanique
Dégâts à distance, headshots, critiques.

### Implémenté
- **Effect:** `StatEffectApplier:27-28` → `dmg *= 1 + precision*0.005`
- **Effect:** `StatEffectApplier:35-37` → +15% à max HP si precision ≥ 50
- **XP:** `CombatXPHandler:58-65` → bows/crossbows/trident → PRECISION
- **Perks:** 6 perks (36-41)

### Manque
- ❌ **XP source:** `NonCombatXPHandler` → `ActionType.FISHING` → PRECISION (existe dans ActionType mais pas branché !)
- ❌ **EpicFight:** Air attacks → PRECISION
- ❌ **Tensura:** Heavenly Eye skill → PRECI_TRANSCENDENCE

### Dépendances critiques
- Brancher `FISHING` → XP handler

---

## 8. ARCANE_POWER (index 7) — 🟡 Magic (pas de perks)

### Mécanique
Dégâts magiques bruts, mana pool.

### Implémenté
- **Effect:** Aucun hook magique direct (pas de système de magie dans le mod)
- **XP:** Aucune source
- **Race:** Daemon/Elf/Ogre → +ARCANE_POWER

### Manque
- ❌ **Effect:** `LivingDamageEvent.Pre` devrait détecter dégâts magiques → `dmg *= 1 + arcane*0.005`
- ❌ **XP source:** Casting (Mahou Tsukai) → ARCANE_POWER. Ou enchantement ?
- ❌ **Mahou:** Detection de scroll → XP ARCANE
- ❌ **Tensura:** Magicule scaling via ARCANE_POWER

### Dépendances
- Système de détection "est-ce un dégât magique ?" → `event.getSource().isIndirect()` ou `getType()` ou NBT du damager

---

## 9-12. AFFINITÉS (WATER, EARTH, FIRE, AIR) — 🟡 Magic (pas de perks)

### Mécanique
Efficacité des sorts élémentaires.

### Implémenté
- **Perks:** `hasPerks()` retourne `false` pour index 7-15
- **Race:** Ajoutées dans certaines races (Elf → WATER, Dragon → FIRE, Harpy → AIR)

### Manque
- ❌ **Effect:** Aucun. Devraient multiplier les dégâts élémentaires spécifiques
- ❌ **XP source:** Mahou Tsukai scroll detection → XP par élément spécifique
- ❌ **Detection:** Nécessite de pouvoir identifier l'élément d'un projectile/sort
- ❌ **Mahou:** `MahouElementMapper.java` → mapping scroll → affinité → XP + damage

### Dépendances
- Mahou Tsukai ou autre système de magie pour donner du sens

---

## 13. MAGIC_RESISTANCE (index 12) — 🟡 Pas d'XP, pas de perks

### Mécanique
Réduction dégâts magiques.

### Implémenté
- **Effect:** `StatEffectApplier:59-63` → `dmg *= 1 - min(0.5, magicRes*0.005)` si attaque indirecte
- **Race:** Elf/Daemon → +MAGIC_RESISTANCE

### Manque
- ❌ **XP source:** Devrait XP quand on subit des dégâts magiques
- ❌ **Detection:** `isIndirect()` est trop large (flèches aussi). Mieux : détection du type de dégât magique.

### Dépendances
- `DamageType` tag `is_magic` dans Minecraft

---

## 14. CASTING_SPEED (index 13) — 🟡 Pas d'XP, pas de perks

### Mécanique
Lancer de sorts plus rapide.

### Implémenté
- **Race:** Ajouté dans races magiques

### Manque
- ❌ **Effect:** Aucun. Devrait affecter le charge time des items (crossbows, tridents, etc.)
- ❌ **Attribute:** Possible via `AttributeModifier` sur le `charge_time` ou via event
- ❌ **XP source:** Via Mahou Tsukai ou pratique de la magie
- ❌ **Mahou:** `MahouSpellTier.java` → CASTING_SPEED réduit le temps de cast

### Dépendances
- Mahou Tsukai pour détecter le cast

---

## 15. MANA_POOL (index 14) — 🟡 Pas d'XP, pas de perks

### Mécanique
Mana maximum.

### Implémenté
- **Race:** Ajouté dans races magiques

### Manque
- ❌ **Effect:** Aucun. Devrait étendre le mana Tensura/Mahou
- ❌ **XP source:** Via utilisation de mana
- ❌ **Tensura:** `MagiculeScalingHandler.java` → MANA_POOL → maxMagicule
- ❌ **Mahou:** `MahouCompat` → MANA_POOL → maxMana

---

## 16. ERUDITION (index 15) — 🟡 Pas d'XP, pas de perks

### Mécanique
Variété de sorts, apprentissage.

### Implémenté
- **Race:** Elf saint → +ERUDITION

### Manque
- ❌ **Effect:** Aucun. Devrait débloquer plus de sorts, XP bonus sur enchantement
- ❌ **XP source:** Enchantement, lecture de livres, découverte de nouveaux crafts
- ❌ **ActionType.**`ENCHANTING` → ERUDITION (actuellement lié à ALCHEMY !)

---

## 17. TRACKING (index 16) — ✅ Intégré

### Mécanique
Détection de mobs, marking, tracking.

### Implémenté
- **Effect:** `StatEffectApplier:44-47` → GLOWING sur la cible pendant `tracking*2` ticks
- **Perks:** 6 perks (42-47)
- **Race:** Beastfolk → +TRACKING

### Manque
- ❌ **XP source:** Aucune ! Détecter des mobs (les voir en premier, les marquer) devrait donner XP
- ❌ **Event:** `Tracking` manque un event source. Peut-être quand le joueur est le premier à voir un mob ?

---

## 18. KEEN_SENSES (index 17) — ✅ Intégré

### Mécanique
Dodge, perception, esquive.

### Implémenté
- **Effect:** `StatEffectApplier:71-75` → dodge % = `keen * 0.002` (0.2% par niveau)
- **Perks:** 6 perks (48-53)
- **Race:** Beastfolk → +KEEN_SENSES

### Manque
- ❌ **XP source:** Aucune. Esquiver des attaques → KEEN_SENSES XP. `LivingDamageEvent.Pre` côté victime avec damage = 0 ?
- ❌ **ParCool:** Roll → KEEN_SENSES (granular XP)

---

## 19. FORGING (index 18) — ✅ Intégré partiel

### Mécanique
Réparation d'outils, forge.

### Implémenté
- **XP:** `NonCombatXPHandler:22-27` → ore break → FORGING ; `ItemCraftedEvent` → FORGING
- **Perks:** 6 perks (54-59)
- **Race:** Dwarf → +FORGING

### Manque
- ❌ **Effect:** Aucun effet direct sur le gameplay. Devrait réduire le coût de réparation à l'enclume, augmenter la durabilité des items craftés.
- ❌ **Attribute:** `Attributes.ATTACK_DAMAGE` sur outils forgés ?
- ❌ **Overgeared:** Detection de crafts Overgeared → FORGING XP
- ❌ **Tensura:** Durabilité améliorée sur items Tensura

---

## 20. COOKING (index 19) — ✅ Intégré partiel

### Mécanique
Saturation alimentaire.

### Implémenté
- **XP:** `ItemSmeltedEvent` → si food → COOKING
- **Perks:** 6 perks (60-65)

### Manque
- ❌ **Effect:** Aucun effet direct. Devrait augmenter la saturation donnée par la nourriture.
- ❌ **Event:** `PlayerEvent.ItemSmeltedEvent` déjà branché ✅
- ❌ **Tensura:** Gourmet skill → COOK_TRANSCENDENCE

---

## 21. ALCHEMY (index 20) — ✅ Intégré partiel

### Mécanique
Durée et puissance des potions.

### Implémenté
- **XP:** `RightClickBlock` (brewing stand) → ALCHEMY
- **Perks:** 6 perks (66-71)

### Manque
- ❌ **Effect:** `PotionBrewEvent` ou `ItemAlchemistEvent` devrait étendre la durée des potions : `duration *= 1 + alchemy*0.01`
- ❌ **Event:** `BrewingStandRecipeEvent` ou inject via mixin sur le brewing stand
- ❌ **Tensura:** Degenerate skill → ALCHEM_TRANSCENDENCE

---

## 22. INTIMIDATION (index 21) — ✅ Intégré

### Mécanique
Dégâts bonus aux cibles marquées, peur.

### Implémenté
- **Effect:** `StatEffectApplier:30-33` → `dmg *= 1 + intimid*0.003` sur les vivants
- **Perks:** 6 perks (72-77)
- **Race:** Ogre/Vampire/Daemon → +INTIMIDATION

### Manque
- ❌ **XP source:** Aucune. Intimidation devrait XP quand on : tue des mobs, fait fuir des mobs, inflige dégâts à des mobs marqués.
- ❌ **EpicFight:** Execute threshold → INTIMIDATION

---

## 23. WILLPOWER (index 22) — ✅ Intégré

### Mécanique
Résistance aux effets de statut, volonté.

### Implémenté
- **Effect:** `StatEffectApplier:65-66` → `dmg *= 1 - min(0.4, will*0.003)`
- **Perks:** 6 perks (78-83)
- **Race:** Human/Wight → +WILLPOWER

### Manque
- ❌ **XP source:** Aucune. Résister à des effets, survivre à bas HP.
- ❌ **Effect potion:** Réduction de durée des effets négatifs : `MobEffectInstance` durée réduite par WILLPOWER. Nécessite mixin ou event `MobEffectEvent.Added`.
- ❌ **EpicFight:** Stun resistance → WILLPOWER (fichier planifié)
- ❌ **Tensura:** Majesty skill → WILL_TRANSCENDENCE

---

## Récapitulatif par Couche

### XP Sources Manquantes
| Stat | XP Source Manquante |
|---|---|
| RAPIDITE | Daggers/rapides → resolveWeaponStat à enrichir |
| PHYSICAL_RESISTANCE | Prendre des dégâts (basé sur dmg reçus) |
| PHYSICAL_ENDURANCE | SWIMMING dans ActionType non branché |
| PRECISION | FISHING dans ActionType non branché |
| ARCANE_POWER | Lancer de sorts / enchantement |
| AFFINITÉS | Mahou tsukai scroll detection |
| MAGIC_RESISTANCE | Subir dégâts magiques |
| CASTING_SPEED | Casting actions |
| MANA_POOL | Utilisation de mana |
| ERUDITION | Enchantement / découverte |
| TRACKING | Détection de mobs |
| KEEN_SENSES | Esquives réussies |
| INTIMIDATION | Kills / marquage |
| WILLPOWER | Résistance aux effets |

### Events Pas Encore Branchés
| Event | Stats Concernées |
|---|---|
| `PotionBrewEvent` / brewing finish | ALCHEMY |
| `MobEffectEvent.Added` | WILLPOWER (réduction durée) |
| Joueur reçoit dégâts magiques | MAGIC_RESISTANCE, ARCANE_POWER |
| Joueur esquive (dodge) | KEEN_SENSES |

### Attributes Vanilla Pas Encore Liés
| Attribute | Stat |
|---|---|
| `JUMP_STRENGTH` | AGILITY |
| `MAX_HEALTH` (via absorption) | PHYSICAL_ENDURANCE |
| Armor reduction via potion | FORGING (qualité items) |

### Intégration Mods → Ce Qui Relie Quoi
| Mod | Fournit | Consommé Par |
|---|---|---|
| Tensura Extra Skills | PerkToSkillMapper | TRANSCENDENCE perks |
| Tensura Race | RaceModifierRegistry | Toutes stats (flat + XP multi) |
| Tensura Soul Level | SoulLevelSyncHandler | XP multi global |
| EpicFight Attributes | EpicFightCompat | BRUTE (impact), WILL (stun resist), AGIL (air attack) |
| ParCool Stamina | ParcoolCompat | ENDURANCE (max stamina) |
| ParCool Actions | ParcoolCompat | AGIL, RAPIDITE, BRUTE (granular XP) |
| Mahou Scrolls | MahouCompat | ARCANE, AFFINITÉS, CASTING_SPEED |
| Overgeared Craft | OvergearedCompat | FORGING |

---

## Points d'Architecture Clés

1. **resolveWeaponStat()** est trop simpliste — besoin d'un système de mapping item → stat basé sur registry tags ou configurable
2. **ActionType** a 4 entrées non branchées dans `NonCombatXPHandler` : PARKOUR, SWIMMING, FISHING, ENCHANTING
3. **Aucune XP pour les stats défensives** (PHYS_RESIST, MAGIC_RESIST, KEEN_SENSES) — besoin d'un système "recevoir dégâts → XP"
4. **Stats magiques (index 7-15)** ont `hasPerks()=false` mais n'ont aucun effet non plus — coquilles vides
5. **Detection de type de dégât** (`isMagic`, `isProjectile`, `isExplosion`) est fragile — mieux via `DamageType` tags Minecraft

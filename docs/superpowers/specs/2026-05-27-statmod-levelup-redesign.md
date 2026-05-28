# STAT Mod – Refonte du Level-Up & Systèmes

## 1. Soif – Rebalance "une bouteille ne remplit pas toute la jauge"

### Changements
- **Eau (Water Bottle)** : restaure **8/100** au lieu de 20/100 (configurable)
- **Actions qui augmentent la fatigue → coûtent aussi de la soif** :
  - Sprint → soif -0.03/tick
  - Saut → soif -0.05/saut
  - Dégâts subis → soif -1.0/coup
  - Casser un bloc → soif -0.3/bloc
- **Manger réduit la fatigue** : quand un joueur finit de manger, `fatigue.reduceFatigue(5 + foodHeal * 2)` (ex: pain → 5+2.5*2=10 fatigue, steak → 5+4*2=13 fatigue)

### Fichiers modifiés
- `FatigueHandler.java` – ajouter coût soif sur chaque action fatigue + listener manger
- `ThirstHandler.java` – réduire décay passif (compensé par les nouveaux coûts)
- `ThirstManager.java` – pas de changement
- `Config.java` – ajouter options soif si nécessaire

---

## 2. Actions XP enrichies

Chaque stat a 3 paliers d'actions : **★ Commun (2-3 XP)**, **★★ Intermédiaire (5-8 XP)**, **★★★ Rare (12-20 XP)**.
L'XP gagnée scale avec la difficulté de l'action.

### COMBAT (7 stats)

#### Force Brute
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Coup avec hache/greatsword / Casser pierre, deepslate, obsidienne | 2-3 |
| ★★ Intermédiaire | Sprint-attaque / Overkill (tuer mob 1-hit qui avait >20 PV) / Casser bloc en dessous Y=0 | 5-8 |
| ★★★ Rare | Tuer mob large (Ravager, Warden, Iron Golem) / Casser ancient debris / Dégâts deux mains simultanés | 12-20 |

#### Technique de Lame
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Coup avec arme de lame (épée, dague, katana, tachi, longsword, trident) | 2-3 |
| ★★ Intermédiaire | Combo 3 hits en 2s / Parade (bouclier ou arme) / Toucher sans être touché pendant 5s | 5-8 |
| ★★★ Rare | Toucher 3 mobs en 1 coup / Tuer joueur / Dégâts par derrière | 12-20 |

#### Rapidité
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Coup poings/dague / Courir pendant l'attaque | 2-3 |
| ★★ Intermédiaire | 5 hits en 2s / Frapper sans être touché / Arme rapide entre 2 mobs | 5-8 |
| ★★★ Rare | Changer d'arme et refrapper en 1s / Esquiver projectile / Toucher 3 mobs en 1s | 12-20 |

#### Agilité
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Coup avec lance / Sauter | 2-3 |
| ★★ Intermédiaire | Sprint-saut / Elytra / Parkour (saut >2 blocs) / Toucher mob en sprintant | 5-8 |
| ★★★ Rare | 10s sans toucher sol en combat / Esquiver 3 attaques suite / Combat aérien | 12-20 |

#### Résistance Physique
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Prendre dégâts / Subir effet négatif | 2-3 |
| ★★ Intermédiaire | Survivre <4 coeurs / Dégâts de chute / Poison ou wither | 5-8 |
| ★★★ Rare | Survivre explosion / Dégâts 3 sources en 10s / Tomber du ciel et survivre | 12-20 |

#### Endurance Physique
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Bloquer bouclier / 4 pièces armure équipées | 2-3 |
| ★★ Intermédiaire | Bloquer coup puissant / Sprint armure lourde / Immobile sous dégâts | 5-8 |
| ★★★ Rare | Bloquer projectile / Parer critique / Séquence coups sans reculer | 12-20 |

#### Précision
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Tir arc/arbalète / Coup critique (chute) | 2-3 |
| ★★ Intermédiaire | Tir à +15 blocs / Crit en sprintant / Toucher cible mobile | 5-8 |
| ★★★ Rare | Tir à +30 blocs / Tuer mob volant / Tir traverse 2 mobs | 12-20 |

### MAGIE (9 stats – en stand-by, futur mod de magie)
Non implémenté pour l'instant. Les stats existent mais sans actions XP avancées.

### SURVIE (2 stats)

#### Pistage
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Tuer monstre / Nourrir/apprivoiser animal / Tondre mouton | 2-3 |
| ★★ Intermédiaire | Tuer 3 types mobs / Chasser de nuit / Élever 2 animaux / Attacher en laisse | 5-8 |
| ★★★ Rare | Tuer mob rare / Boussole longue-vue / Tuer mob qui t'avait repéré / Collecter tête de mob | 12-20 |

#### Sens Aiguisés
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Explorer nouveau biome / Marcher dans le noir | 2-3 |
| ★★ Intermédiaire | Trouver structure / Sprint dans le noir / Esquiver projectile | 5-8 |
| ★★★ Rare | Survivre explosion / Toucher ennemi +20 blocs / Combat sans dégâts subis / Explorer biome rare / Découvrir tous biomes d'une catégorie | 12-20 |

### ARTISANAT (3 stats)

#### Forge
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Crafter outil/arme / Utiliser enclume | 2-3 |
| ★★ Intermédiaire | Réparer objet / Forger template / Combiner enchantements | 5-8 |
| ★★★ Rare | Créer équipement netherite / Template armure / Réparer avec netherite | 12-20 |

#### Cuisine
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Cuire aliment / Fumer au smoker | 2-3 |
| ★★ Intermédiaire | Crafter gâteau / Repas varié / Composter | 5-8 |
| ★★★ Rare | Découvrir recette / Cuire bloc nourriture / Nourrir animal | 12-20 |

#### Alchimie
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Brasser potion / Utiliser alambic | 2-3 |
| ★★ Intermédiaire | Modifier potion (redstone/glowstone) / Potion splash | 5-8 |
| ★★★ Rare | Potion persistante / Potion respiration max / Flèche potion / Dragon's breath | 12-20 |

### MENTAL (2 stats)

#### Intimidation
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Tuer mob / Porter armure complète (fer+) | 2-3 |
| ★★ Intermédiaire | Tuer boss / Tue en infériorité numérique / Tue sans dégâts / Armure or/diamant | 5-8 |
| ★★★ Rare | Tuer Wither / Tuer Ender Dragon / Tue joueur / 5 mobs sans dégâts | 12-20 |

#### Volonté
| Palier | Actions | XP |
|--------|---------|----|
| ★ Commun | Survivre <6 coeurs / Avoir faim | 2-3 |
| ★★ Intermédiaire | Survivre <4 coeurs / Poison/wither / Nuit sans dormir | 5-8 |
| ★★★ Rare | Survivre 1/2 coeur / Poison sans guérir / 3 nuits sans dormir / Survivre dans le vide | 12-20 |

---

## 3. Bonus passifs continus (par niveau)

Chaque niveau donne un micro-bonus passif cumulable jusqu'à 100.

| Stat | Bonus/niveau | Max (100) |
|------|-------------|-----------|
| Brute Force | +0.2% dégâts | +20% |
| Technique Lame | +0.15% dégâts | +15% |
| Rapidité | +0.3% vitesse attaque | +30% |
| Agilité | +0.2% vitesse mouvement | +20% |
| Résistance Physique | -0.3% dégâts subis | -30% |
| Endurance Physique | +0.2 coeur | +20 coeurs |
| Précision | +0.3% critique | +30% |
| Puissance Arcanique | +0.3% dégâts magiques | +30% |
| Affinité Aquatique | +0.5% vitesse nage | +50% |
| Affinité Terrestre | +0.3% vitesse minage | +30% |
| Affinité Ignée | +0.5% dégâts feu | +50% |
| Affinité Aérienne | +0.3% hauteur saut | +30% |
| Résistance Magique | -0.3% dégâts magiques subis | -30% |
| Vitesse d'Incantation | +0.3% vitesse utilisation item | +30% |
| Réserve Mana | +1 mana max | +100 mana |
| Érudition | +0.5% XP bonus | +50% |
| Pistage | +0.3% portée détection | +30% |
| Sens Aiguisés | +0.3% portée entités | +30% |
| Forge | +0.3% durabilité outils | +30% |
| Cuisine | +0.3% saturation nourriture | +30% |
| Alchimie | +0.3% durée potions | +30% |
| Intimidation | +0.3% portée peur | +30% |
| Volonté | -0.5% durée effets négatifs | -50% |

Ces bonus s'appliquent automatiquement via `StatEffectApplier` et/ou `AttributeModifier` sur le joueur à la connexion et au level-up.

---

## 4. Paliers tous les 10 niveaux

| Palier | Récompenses |
|-------|-------------|
| 10 | Son + particules + bonus passif ×1.05 + déblocage skill tier 1 |
| 20 | Son + effet visuel + bonus passif ×1.05 |
| 30 | Son + particules + bonus ×1.05 + déblocage skill tier 2 |
| 40 | Son + bonus ×1.05 |
| 50 | Animation + screen shake + bonus ×1.10 + skill tier 3 + **1 point de perk** |
| 60 | Son + bonus ×1.05 |
| 70 | Son + particules + bonus ×1.05 + skill tier 4 |
| 80 | Son + bonus ×1.05 |
| 90 | Son + particules + bonus ×1.05 |
| 100 | Feux d'artifice + bonus ×1.10 + **3 points de perk** + titre "Maître [Stat]" |

---

## 5. Système de perks (hybride complet)

### Principe
- Chaque stat a **3 perks** spécifiques déblocables
- Les **points de perks** s'obtiennent aux paliers : 50 (1pt) et 100 (3pts) de chaque stat
- Coût : **1 point par perk**
- Chaque perk a un **level minimum requis** sur la stat
- Les perks sont persistants (stockés dans la capability)

### Structure par stat
Chaque stat → 3 perks :
- **Perk 1** (requis Lv. 20) – bonus modeste
- **Perk 2** (requis Lv. 50) – bonus significatif
- **Perk 3** (requis Lv. 80) – bonus puissant ou mécanique unique

### Exemples (à implémenter)

**Force Brute** :
1. Lv.20 – Dégâts aux blocs +50% (bois, pierre)
2. Lv.50 – Dégâts aux armures +15%
3. Lv.80 – Coups chargés étourdissent 1 seconde

**Technique de Lame** :
1. Lv.20 – Fenêtre de parade +25%
2. Lv.50 – Combo bonus : 5 hits → prochain coup +50% dégâts
3. Lv.80 – Toucher 3 mobs d'un coup inflige saignement

**Rapidité** :
1. Lv.20 – +10% vitesse déplacement après une attaque (2s)
2. Lv.50 – Les attaques rapides ont 10% de chance de double-hit
3. Lv.80 – Esquiver déclenche +50% vitesse attaque (3s)

**Agilité** :
1. Lv.20 – +20% hauteur de saut en combat
2. Lv.50 – Sprint-saut inflige dégâts de chute à l'ennemi touché
3. Lv.80 – Elytra +10% vitesse en combat

**Résistance Physique** :
1. Lv.20 – +10% résistance aux dégâts de chute
2. Lv.50 – Absorption de 1 coeur après avoir pris des dégâts (10s cd)
3. Lv.80 – 30% de chance de réduire les dégâts de moitié

**Endurance Physique** :
1. Lv.20 – Bloquer réduit aussi les dégâts magiques de 20%
2. Lv.50 – Sprint avec bouclier levé possible
3. Lv.80 – Parer parfait annule tous les dégâts et repousse l'attaquant

**Précision** :
1. Lv.20 – +10% dégâts à +15 blocs
2. Lv.50 – Tir critique garantit knockback
3. Lv.80 – Tir traverse les mobs (piercing +1)

*(Les perks des stats Magie, Survie, Artisanat, Mental suivront le même pattern)*

---

## Implémentation technique

### Nouvelles classes
- `Perk.java` – enum ou record représentant un perk (id, stat, levelReq, effet)
- `PerkManager.java` – stocke les perks débloqués par le joueur (capability)
- `PerkProvider.java` – capability provider
- `PerkScreen.java` – écran GUI pour voir/dépenser les points de perks
- `ActionXpHelper.java` – classe utilitaire pour évaluer le palier d'une action et attribuer l'XP

### Fichiers modifiés
- `PlayerStats.java` – ajouter méthode `addXpWithTier(index, tier)` qui donne XP selon le palier
- `CombatXPHandler.java` – réécrire avec les nouveaux paliers d'actions
- `NonCombatXPHandler.java` – réécrire avec les nouveaux paliers d'actions
- `StatEffectApplier.java` – ajouter tous les bonus passifs
- `CapabilityHandler.java` – enregistrer PerkProvider
- `STATMod.java` – initialiser PerkManager
- `ClientSetup.java` – ajouter touche pour écran perks
- `HUDManager.java` – ajouter overlay notification perk

### Stockage
- Perks débloqués : stockés dans `CompoundTag` via capability, liste d'entiers (ids)
- Points de perks disponibles : stockés dans `CompoundTag` via capability, int
- Synchronisés au joueur via packet dédié (`SyncPerksPacket`)

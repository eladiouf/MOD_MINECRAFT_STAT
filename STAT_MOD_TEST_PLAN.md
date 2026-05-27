# STAT Mod - Plan de Test

## 📦 Installation
1. Copie `statmod-1.0.0.jar` dans le dossier `mods` de Minecraft Forge 1.20.1
2. **Requis** : Epic Fight 20.14.17 (dans `mods` aussi)
3. Lance le jeu

---

## ✅ Tests à effectuer

### 1. Stats de Combat
| Stat | Comment tester | XP visible ? |
|------|---------------|-------------|
| **Force Brute** | Tape des mobs avec une hache ou greatsword | Oui / Non |
| **Technique de Lame** | Tape avec épée, katana, dague, trident | Oui / Non |
| **Rapidité** | Enchaîne 5 coups rapides en 2 secondes (n'importe quelle arme) | Oui / Non |
| **Agilité** | Utilise l'esquive d'Epic Fight (touche double) | Oui / Non |
| **Résistance Physique** | Laisse un mob te taper | Oui / Non |
| **Endurance Physique** | Bloque avec un bouclier | Oui / Non |
| **Précision** | Tire à l'arc/arbalète ou fait un critique | Oui / Non |

### 2. Stats de Magie
| Stat | Comment tester | XP visible ? |
|------|---------------|-------------|
| **Puissance Arcanique** | Tape avec une épée enchantée (dégâts magiques) | Oui / Non |
| **Affinité Aquatique** | Reste dans l'eau 5 secondes | Oui / Non |
| **Affinité Terrestre** | Mine avec une pioche | Oui / Non |
| **Affinité Ignée** | Reste dans la lave ou en feu 5 secondes | Oui / Non |
| **Affinité Aérienne** | Saute d'une falaise | Oui / Non |
| **Résistance Magique** | Subis une potion ou dégâts magiques | Oui / Non |
| **Vitesse d'Incantation** | Utilise un objet (clic droit) | Oui / Non |
| **Mana Pool** | XP gagné automatiquement via les autres stats magiques | Oui / Non |
| **Érudition** | Fabrique un objet enchanté (table d'enchantement) | Oui / Non |

### 3. Stats de Survie
| Stat | Comment tester | XP visible ? |
|------|---------------|-------------|
| **Pistage** | Tue des mobs hostiles (zombie, creeper, etc.) | Oui / Non |
| **Sens Aiguisés** | Change de dimension (Nether / End) | Oui / Non |

### 4. Stats d'Artisanat
| Stat | Comment tester | XP visible ? |
|------|---------------|-------------|
| **Forge** | Fabrique un outil ou une arme (établi) | Oui / Non |
| **Cuisine** | Cuit de la nourriture dans un four | Oui / Non |
| **Alchimie** | Utilise un alambic (brew) | Oui / Non |

### 5. Stats Mentales
| Stat | Comment tester | XP visible ? |
|------|---------------|-------------|
| **Intimidation** | Tue le Wither ou l'Ender Dragon | Oui / Non |
| **Volonté** | Laisse ta vie tomber sous 30% | Oui / Non |

---

## 🎮 Tests Système

### HUD & Interface
- [ ] **Barre de fatigue** visible dans le HUD quand tu combats
- [ ] **Compteur de combo** visible après 5 coups rapides
- [ ] Touche **P** → écran de stats avec 5 onglets (Combat, Magie, Survie, Artisanat, Mental)
- [ ] Les niveaux et barres d'XP s'affichent correctement dans l'écran P

### Fatigue
- [ ] La fatigue monte quand tu frappes/cours/sautes
- [ ] La fatigue descend quand tu restes immobile ou accroupi
- [ ] La fatigue se reset quand tu dors
- [ ] À 50%+ fatigue : debuff de lenteur
- [ ] À 100% fatigue : impossibilité de courir/sauter

### Weapon Mastery
- [ ] Changer d'arme donne de l'XP de maîtrise pour cette arme
- [ ] Les niveaux de maîtrise d'arme montent

### Progression Générale
- [ ] Les stats montent bien de 0 à 100
- [ ] L'XP ne se perd pas en mourant (vérifier avec `/stats`)
- [ ] Les dégâts augmentent avec le niveau des stats (Force Brute / Technique)

### Commandes
- [ ] `/stats` → affiche toutes tes stats
- [ ] `/stats get brute_force` → affiche une stat précise
- [ ] `/fatigue` → affiche ta fatigue actuelle

---

## 🐛 Bugs connus (à ignorer)
- Les warnings `Mixin apply failed` d'Epic Fight dans les logs sont normaux (mods optionnels manquants)
- `skinlayers`, `vampirism`, `werewolves` → pas installés, c'est normal

---

## 📝 Retour
Noter ici les stats qui ne montent PAS :
```
- Force Brute :
- Technique de Lame :
- Rapidité :
- Agilité :
- Résistance Physique :
- Endurance Physique :
- Précision :
- (etc.)
```

# Sihriya Mod — Spécification de Design

## Aperçu
**Sihriya** (سحرية, "magique" en arabe) est un mod Forge 1.20.1 qui ajoute un système de magie complet avec écoles élémentaires, mana, apprentissage progressif et compatibilité Epic Fight. Bridge optionnel vers STAT Mod.

## 1. Architecture

```
Sihriya Mod (Forge 1.20.1)
│
├── Bridge STAT Mod (optionnel)
│   └── SihriyaAPI ←→ STAT Mod stats (Arcane Power, Affinités...)
│
├── Core
│   ├── ManaManager — capability joueur
│   ├── SpellRegistry — charge spells.json
│   ├── SchoolRegistry — charge schools.json
│   ├── UnlockRegistry — charge unlocks.json
│   └── CastingHandler — logique de lancement
│
├── Systèmes
│   ├── ProgressionManager — XP, niveaux, déblocages
│   ├── SpellExecutor — exécute les effets des sorts
│   ├── SpellWheel — roue de sélection circulaire
│   └── Network — synchro mana, déblocages, cooldowns
│
├── Data (JSON)
│   ├── schools.json
│   ├── spells.json
│   └── unlocks.json
│
└── Intégration Epic Fight
    └── SihriyaCompat — effets élémentaires sans conflit
```

## 2. Écoles de magie (V1 — 6 écoles)

| École | Départ | Déblocage |
|-------|--------|-----------|
| Feu | ✅ Oui (1/4 aléatoire) | — |
| Eau | ✅ Oui (1/4 aléatoire) | — |
| Vent | ✅ Oui (1/4 aléatoire) | — |
| Terre | ✅ Oui (1/4 aléatoire) | — |
| Foudre | ❌ Non | Feu ≥ 50 OU Vent ≥ 50 |
| Glace | ❌ Non | Eau ≥ 50 |

Conditions de déblocage définies dans `schools.json` et vérifiées runtime.

## 3. Système de Mana

Ressource unique attachée au joueur via capability Forge.
- **Mana max** : 50 de base + bonus via STAT Mod si présent
- **Aucun regen passif**
- **Régénération** : méditation (touche V), sommeil, potions
- **Blocage ultime** : après un sort ultime, mana locked 30 secondes
- **HUD** : barre de mana affichée côté client

## 4. Sorts (data-driven via JSON)

Chaque sort dans `spells.json` :
```json
{
  "id": "fireball",
  "school": "fire",
  "tier": 1,
  "manaCost": 10,
  "cooldown": 30,
  "type": "projectile",
  "effects": [
    { "type": "damage", "base": 5, "scaling": 0.1 },
    { "type": "burn", "duration": 60 }
  ]
}
```

Types de sorts : `projectile`, `zone`, `buff`, `summon`, `ultimate`.

Scaling via STAT Mod si présent (Fire Affinity → dégâts feu, Water Affinity → soins, etc.).

## 5. Casting & Contrôles

| Action | Entrée |
|--------|--------|
| Ouvrir la roue des sorts | R maintenu |
| Sélectionner un sort | Souris dans la roue → relâcher R |
| Lancer le sort | Clic droit (main vide) |
| Méditer | V |
| Menu écoles | Bouton dans l'inventaire |

## 6. Progression

### Départ
- Stats d'affinité générées aléatoirement
- École dominante déterminée
- 2 sorts T1 aléatoires de cette école

### Niveaux d'école

| Niveau école | Déblocage |
|-------------|-----------|
| 1 | 2 sorts T1 aléatoires |
| 25 | Tous les T1 + accès T2 |
| 50 | Accès T3 + école avancée |
| 75 | Accès T4 |
| 100 | Sort ultime |

### Apprentissage des sorts

| Tier | Acquisition |
|------|------------|
| T1 | Gratuit en montant de niveau |
| T2 | Parchemin (coffres/donjons) |
| T3 | Grimoire (boss) |
| T4/Ultime | Grimoire rare + condition spéciale |

## 7. Intégration Epic Fight

- Clic droit pour lancer (conflit zéro avec Epic Fight sur clic gauche)
- Effets élémentaires ajoutés via LivingHurtEvent

| École | Effet combat |
|-------|-------------|
| Feu | Brûlure (dégâts durée) |
| Eau | Ralentissement |
| Vent | Knockback |
| Terre | Stun court |
| Foudre | Chaîne (ennemis proches) |
| Glace | Gel (immobilisation) |

## 8. Bridge STAT Mod (optionnel)

- Vérifie la présence de `tong.statmod.STATMod` au chargement
- Si présent : les stats magiques de STAT Mod influencent le scaling des sorts
- Mapping : Fire Affinity → Feu, Water Affinity → Eau, Air Affinity → Vent, Earth Affinity → Terre, Arcane Power → Foudre, Water Affinity → Glace
- Si absent : scaling linéaire basé sur le niveau de l'école uniquement

## 9. Paquetage & Conventions

- Package : `net.sihriya` ou `tong.sihriya`
- Dépendances : Forge 47.4.20, Epic Fight 20.14.17 (opt)
- Bridge STAT Mod : réflexion ou interface dédiée
- Licence : à définir

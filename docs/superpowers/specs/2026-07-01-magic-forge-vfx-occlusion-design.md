---
title: Magic Forge VFX + Occlusion Fix Design
date: 2026-07-01
status: approved-in-chat
owners:
  - Codex
  - El Hadji
related:
  - docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md
---

# Magic Forge VFX + Occlusion Fix Design

## Scope

Ce lot couvre uniquement les deux blocs décoratifs de forge magique :

- `statmod:infusion_forge`
- `statmod:enchantment_anvil`

Objectifs :

1. corriger le rendu où le bloc posé en dessous devient visuellement masqué/invisible
2. ajouter des particules client-side distinctes pour chaque bloc
3. éclaircir les textures pour améliorer la lisibilité en jeu

Hors scope :

- ajout de nouvelles pinces / grips
- ajout de nouveaux marteaux
- changement de recipes ou de gates Overgeared

## Root Cause Hypothesis

Le bug de bloc support masqué vient du fait que les blocs sont actuellement déclarés comme
des `Block` décoratifs simples sans shape dédiée ni réglage d'occlusion. Le moteur les traite
comme suffisamment pleins pour occlure le voisinage malgré un modèle visuel d'enclume non cube.

La correction doit venir du code block runtime :

- `noOcclusion()`
- `VoxelShape` correspondant à la silhouette réelle
- shape de collision / outline cohérente avec le mesh

Le modèle JSON peut rester quasi identique.

## Runtime Behavior

### Infusion Forge

- bloc non plein (`noOcclusion`)
- silhouette d'enclume via `VoxelShape`
- particules `END_ROD` en `animateTick`
- émission modérée, localisée vers le plateau supérieur et les épaules
- mouvement léger vers le haut

### Enchantment Anvil

- même traitement d'occlusion / shape
- particules `ENCHANT` en `animateTick`
- émission plus diffuse que l'infusion forge
- lecture visuelle "rituelle" autour du plateau

## Visual Direction

### Infusion Forge

- base sombre conservée
- violets moins bouchés
- lignes magiques plus claires
- contraste supérieur entre pierre/metal et accents arcaniques

### Enchantment Anvil

- magenta moins terne
- séparation plus nette entre masse métallique et accents enchantés
- pas de glow cartoon

## Implementation Notes

### Java

Créer un petit bloc dédié pour éviter d'alourdir `ForgingBlocks` :

- classe block réutilisable pour les enclumes magiques
- propriété de particule configurable (`END_ROD` vs `ENCHANT`)
- `getShape(...)`
- `getCollisionShape(...)` si nécessaire
- `animateTick(...)`

### Resources

- retouche des textures bloc uniquement
- blockstates et models conservés sauf ajustement minimal si une face parasite doit être supprimée

## Testing

Tests automatisés :

- présence des resources inchangée
- nouveau test sur la shape/non-occlusion des deux blocs

Validation manuelle attendue ensuite :

1. poser `infusion_forge` sur plusieurs blocs pleins et vérifier que le bloc support reste visible
2. poser `enchantment_anvil` idem
3. vérifier la densité des particules à distance normale de jeu
4. vérifier que les nouvelles teintes restent lisibles en intérieur et extérieur

## Risks

- une shape trop fine peut rendre le ciblage pénible
- une densité de particules trop forte peut nuire à la lisibilité
- retoucher les textures sans test in-game peut sur-éclaircir certains détails

## Recommendation

Implémenter un bloc magique d'enclume dédié avec shape explicite et VFX client-side modérés.
Cette approche corrige proprement l'occlusion sans refonte lourde du mesh et garde les deux
blocs dans la même famille visuelle.

---
title: JEI Forging Visibility Design
status: active
phase: design
author: Codex
date: 2026-07-03
---

# Objectif

Rendre visibles dans JEI tout le pipeline de forge utile au joueur :

- recettes des stations `statmod` existantes
- recettes d'assemblage `statmod`
- recettes de chauffe `heated_*`
- recettes de forge Overgeared pour les intermédiaires et sorties pertinentes

Le but est que le joueur puisse découvrir les crafts sans quitter JEI, y compris pour les
anvils et le système Overgeared.

# État actuel

Le plugin JEI `StatModJeiPlugin` enregistre déjà 3 catégories :

- `infusion_forge`
- `infusion_forge_assembly`
- `enchantment_anvil`

Il manque encore l'exposition JEI des recettes Overgeared natives. Résultat : la partie
interactive `statmod` est visible, mais le pipeline amont de forge Overgeared reste opaque
dans JEI.

# Approches

## A — Ne montrer que les stations statmod

Avantages :

- zéro travail côté Overgeared
- très faible risque

Inconvénients :

- ne répond pas à l'objectif utilisateur
- les recettes d'anvil/smithing Overgeared restent invisibles

## B — Ajouter une catégorie JEI Overgeared côté statmod

Avantages :

- contrôle total sur ce qu'on affiche
- unifie la visibilité JEI du pipeline complet
- pas de dépendance à un éventuel plugin JEI fourni par Overgeared

Inconvénients :

- demande un loader dédié pour les recettes Overgeared
- impose de choisir une représentation JEI simplifiée d'une forge interactive

## C — Essayer de s'appuyer sur un plugin JEI externe Overgeared

Avantages :

- moins de code si le plugin existe et couvre déjà le besoin

Inconvénients :

- fragile
- couverture inconnue
- expérience JEI non maîtrisée

# Recommandation

Approche B.

`statmod` reste l'autorité d'affichage JEI pour tout le pipeline de forge du modpack :

- stations `statmod`
- assemblage `statmod`
- forge Overgeared pertinente au contenu `statmod`

# Design retenu

## Nouvelles capacités JEI

Ajouter une 4e catégorie JEI :

- `overgeared_forging`

Cette catégorie affichera les recettes utiles du smithing anvil Overgeared dans un format
JEI lisible : ingrédient(s), blueprint si présent, sortie.

## Source de données

Lire les recettes depuis le `RecipeManager` connecté quand JEI tourne en jeu.

Fallback de dev :

- si nécessaire, lire les JSON `data/statmod/recipe/forging/**`
- rester compatible avec les recettes de développement déjà présentes dans le repo

## Périmètre de visibilité

Afficher dans JEI :

- `statmod` infusion forge
- `statmod` enchantment anvil
- `statmod` assembly recipes encore valides
- recettes Overgeared qui produisent :
  - des `statmod:rough_*`
  - des sorties forgeables pertinentes au pipeline `statmod`

Ne pas essayer d'afficher toutes les recettes du mod Overgeared si elles ne servent pas au
pipeline actuel.

## Filtrage

Le loader Overgeared doit filtrer au minimum :

- namespace ou type recette Overgeared
- sorties `statmod:rough_*`
- éventuellement d'autres sorties Overgeared reconnues par des règles explicites

Le filtrage doit rester déterministe et testable.

## Catalysts JEI

Conserver les catalysts existants :

- `statmod:infusion_forge`
- `statmod:enchantment_anvil`

Ajouter un catalyst pour la catégorie `overgeared_forging` si l'item/bloc Overgeared visé
est accessible côté runtime. Sinon, au minimum, enregistrer la catégorie et ses recettes.

## Tests

Ajouter des tests source/ressources pour verrouiller :

- enregistrement de la nouvelle catégorie dans `StatModJeiPlugin`
- enregistrement des recettes Overgeared dans `registerRecipes`
- présence d'un loader dédié
- filtrage des recettes Overgeared pertinentes

# Risques

- Le type exact des recettes Overgeared peut varier selon les classes exposées au runtime.
- Le catalyst Overgeared peut demander un accès direct à des registres externes.
- Une représentation JEI trop large pourrait noyer le joueur sous des recettes inutiles.

# Critères de réussite

- Le joueur voit dans JEI les recettes des deux stations `statmod`.
- Le joueur voit dans JEI les recettes d'assemblage encore actives.
- Le joueur voit dans JEI les recettes de forge Overgeared nécessaires aux `rough_*`.
- Les tests source/ressources JEI passent.

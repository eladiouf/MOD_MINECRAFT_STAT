# STAT-DEC-MAGE-CODEX — Codex du Mage screen

**Date :** 2026-06-23
**Mission :** J
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Vision

> "Codex du Mage" : écran récap pour que le joueur voie d'un coup d'œil son état magique
> sans avoir à fouiller dans l'arbre Puffish.

## Décision

Nouveau Screen client `MageCodexScreen` accessible via `/magic codex`. Affiche 4 sections :

1. **Identité** : race + branche de départ choisie
2. **Économie** : magic points unifiés + niveaux `ARCANE_POWER` / `ERUDITION` (les 2 gates universels)
3. **Mastery par école** : progression en cours par école avec indication du prochain palier
   (1000 / 2500 / 5000 mastery)
4. **Progression** : compteurs nœuds débloqués + sorts appris

## Architecture

- `MageCodexScreen` (client-only) extends `Screen` — rendu via `GuiGraphics`
- `OpenMageCodexPayload` (S→C) — signal pur sans données, fait juste ouvrir l'écran
- `MagicStateSyncService.payload(data)` réécrit pour porter `magicPoints` + `masteryProgress[]`
  au lieu des anciens `arcanePoints` + `schoolPoints[]` legacy
- `SyncMagicPayload` refondu : champs renommés sémantiquement, anciens noms exposés en
  `@Deprecated` shims pour compat avec d'éventuels callers résiduels
- `ClientMagicCache` refondu pour stocker `magicPoints` + `masteryProgress[]`

## Pourquoi via packet plutôt que keybind ?

- Keybind nécessite enregistrement KeyMapping + handler client séparé (futur travail)
- Commande `/magic codex` est immédiate et utile pour debug / test in-game
- Le packet S→C permet aussi à un mod tiers (Tensura, futurs intégrations) de déclencher
  l'ouverture du codex depuis le serveur

## Design visuel

- Panel centré 360×280 sur fond `#0A0A0A` semi-transparent
- Titre en `volt yellow` (#D4FF00) — couleur signature mod
- Sections séparées avec headers `OFF_WHITE` sur fond noir
- Bordure subtile + bandeau Volt en haut pour le titre
- Pas d'images / icônes — texte stylisé uniquement (sobre, lisible, modulable)

## Lang keys ajoutées

```
statmod.codex.title      — "Mage Codex" / "Codex du Mage"
statmod.codex.identity   — "IDENTITY" / "IDENTITÉ"
statmod.codex.economy    — "ECONOMY" / "ÉCONOMIE"
statmod.codex.mastery    — "SCHOOL MASTERY" / "MAÎTRISE DES ÉCOLES"
statmod.codex.unlocked   — "PROGRESS" / "PROGRESSION"
statmod.codex.hint_close — "Esc to close" / "Échap pour fermer"
```

## Hors scope

- Pas d'action depuis le codex (lecture seule) — futur travail : bouton "Open Magic Tree",
  "Change Start Branch" (lié à mission K)
- Pas de keybind dédiée — utilisateur peut bind `/magic codex` via Minecraft's macro shortcut

## Exit conditions

Refonte du codex si :
- Le fondateur veut un design plus riche (icônes par école, barres de progression visuelles)
- L'économie magic évolue (passage du tracker mastery à autre chose)
- Phase 2 magic ajoute de nouvelles dimensions à afficher (lateGameGate progression, etc.)

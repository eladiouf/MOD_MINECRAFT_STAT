# STAT-DEC-START-BRANCH-CHOOSER — UI de chooser de branche de départ

**Date :** 2026-06-23
**Mission :** K
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Décision

Nouveau sous-écran `StartBranchChooserScreen` accessible depuis le `MageCodexScreen` via
un bouton "Change Branch". Liste les branches compatibles avec la race du joueur
({@code MagicRace.canChooseStartBranch}) sous forme de boutons cliquables.

## Architecture

- **Client** : `StartBranchChooserScreen extends Screen` — liste les branches compatibles
- **Packet C→S** : `ChangeStartBranchPayload(branchOrdinal)` — envoyé au click
- **Server** : `ServerPayloadHandler.handleChangeStartBranch` — valide compat race + applique
- **Sync** : `SyncHelper.syncMagic(player)` re-renvoie l'état au client → re-render du Codex

## Sécurité

Validation **server-side autoritative** :
1. Player must be ServerPlayer (cast check)
2. Player must have a magic race set
3. Ordinal must be in valid range `[0, MagicBranch.values().length)`
4. `race.canChooseStartBranch(target)` must return true

Si l'un échoue → no-op silencieux + debug log. Le client ne peut pas forcer une branche
incompatible avec sa race.

## Pourquoi un sous-écran et pas un menu déroulant inline ?

- Le Codex est dense (4 sections), un dropdown encombrerait
- Un sous-écran isole l'action et donne plus de place pour expliquer (race + affinités
  affichées en header)
- Pattern réutilisable pour d'autres actions futures (changer de race ? respec ?)

## Conséquences gameplay

- Le joueur peut switcher sa branche de départ **librement** entre les affinités naturelles
  de sa race
- Sous l'économie unifiée + race deltas D3, changer de branche modifie le seuil tertiaire
  des nœuds de l'ancienne et de la nouvelle branche (−1 sur la nouvelle, plus de discount sur
  l'ancienne) → léger re-balance instantané sans coût de respec

## Lang keys ajoutées

```
statmod.codex.change_branch  — "Change Branch" / "Changer de branche"
statmod.codex.choose_branch  — "Choose Start Branch" / "Choisir la branche de départ"
statmod.codex.no_race        — "No race chosen yet" / "Aucune race choisie"
```

## Hors scope

- Pas de cooldown / coût sur le changement (libre)
- Pas de notification "you changed branch" — le Codex re-render visuellement suffit
- Pas de keybind dédiée

## Exit conditions

Refonte si :
- Le fondateur décide qu'il faut un coût (respec stone, magic points) au changement
- La liste des races change ou les affinités sont remappées
- Le Codex évolue vers un design plus riche (dropdown intégré devient viable)

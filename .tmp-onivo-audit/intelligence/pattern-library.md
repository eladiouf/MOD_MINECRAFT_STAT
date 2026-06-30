# Pattern Library — Onivo Studio

Ce fichier capture les patterns réutilisables identifiés par la cellule Innovation.

## Patterns Émergents

### P1: Surface Discovery Gap (2026-06-21)
**Observation :** Les documents stratégiques (Weekly Strategy Review, Risk Register) vivent dans le vault mais ne sont pas visibles depuis le project bridge. Sur 3 sessions simulées, 3/3 ont sauté cette lecture.

**Resolution :** Ajouter `Required Session Reads` dans le project bridge.

### P2: No Hard Verification Gate (2026-06-21)
**Observation :** `quality-contract.md` dit "build success before completion claims" mais rien ne bloque le `Closed` si le build n'a pas tourné. Risque de fermeture prématurée.

**Resolution :** Créer un `verification-gate.md` avec checklist exécutable.

### P3: Triple Tracking Overhead (2026-06-21)
**Observation :** Les missions vivent dans `vault/03 Operations/`, sont dupliquées dans `current-initiatives.md`, et `delivery/` a un tiers point de vue. 3 sources de vérité.

**Resolution :** Dataview vault comme source unique, project bridge comme vue statique régénérée.

---
id: STAT-DEC-M4-B
title: Virtual Inscription opening mechanism — keybind over craftable item
date: 2026-06-21
status: accepted
owner: Onivo Studio — Build Cell (Agent 3 / Phase B)
mission: M4
spec-ref: docs/superpowers/specs/2026-06-21-unified-magic-progression-design.md (§5 B2)
---

# Décision

L'ouverture du **menu d'inscription virtuel** Iron's Spellbooks se fait via un **keybind** (Option A) plutôt que par un **item craftable** (Option B).

# Choix : Option A (keybind)

- Touche par défaut : `J` ("J"ournal / Magic codex), catégorie `key.categories.statmod`.
- Client → serveur : `OpenVirtualInscriptionPayload` (no-op record + `StreamCodec.unit`).
- Serveur : `IronInscriptionOpenerService.openVirtual(ServerPlayer)` →
  `player.openMenu(new VirtualInscriptionMenuProvider())`.
- Le menu réutilise `InscriptionTableMenu(int, Inventory, ContainerLevelAccess)` avec
  `ContainerLevelAccess.create(player.level(), player.blockPosition())`. **Pas** de
  `ContainerLevelAccess.NULL` (risque identifié par la spec §9) : ancrer à la position
  du joueur garantit que `stillValid` et le drop des items à la fermeture restent
  cohérents.

# Pourquoi pas Option B (item)

- Pas de modèle 3D / texture livrable dans ce sprint — coût d'art > valeur immédiate.
- Recette nécessiterait des choix d'équilibrage (ingrédients, gating Tensura)
  qu'aucune autre source ne couvre encore.
- Pas de conflit de keybind détecté avec Iron's Spellbooks (qui n'enregistre pas `J`
  par défaut) ni avec Tensura (touches `,` / `.` / `;` / `R`).
- L'item pourra être ajouté ultérieurement comme déclencheur **alternatif** sans
  changer l'API serveur (le service reste appelable depuis n'importe quel hook).

# Risques pris en compte

- **Filtrage déjà transparent** : `IronInscriptionTableScreenMixin` +
  `IronInscriptionTableMenuMixin` injectent sur `InscriptionTableMenu.class` — donc
  le menu virtuel hérite automatiquement de la liste filtrée et du flux d'inscription
  "sans parchemin". Aucun packet de synchro supplémentaire n'est nécessaire.
- **Iron's absent** : `IronInscriptionOpenerService` gate sur `IronSpellsCompat.isLoaded()`
  et affiche `statmod.magic.irons_not_loaded` au lieu d'ouvrir un menu cassé.
- **Cast déjà gated** : `IronSpellEventBridge.onPreCast` reste en charge ; aucun code
  dupliqué côté Phase B.

# Suivi

- Si une demande Founder pour l'item craftable arrive plus tard, l'ajouter
  comme `Item.use()` → `IronInscriptionOpenerService.openVirtual(player)`.

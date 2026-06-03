# STAT Mod Overhaul Plan

> **For agentic workers:** Inline execution — tasks executed in session with checkpoints.

**Goal:** Corriger courbe d'XP, distribution perks, split PerkEffectHandler, refactor UI screens.

**Architecture:** Changements progressifs par fichier, validation build après chaque commit.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, Epic Fight 20.14.17

---

### Task 1: Courbe d'XP — `StatCalculator.java` + `CombatLevels.java`

**Problème:** `(level+1)² × 10` → level 99→100 = 1M XP. Injouable.

**Solution:** `(level+1) × 50` (linéaire) → level 100 = 5,050 XP total. Plus 2 sources passives par stat.

**Files:**
- Modify: `src/main/java/tong/statmod/stats/StatCalculator.java`
- Modify: `src/main/java/tong/statmod/capability/CombatLevels.java` (même formule dupliquée)

### Task 2: Perk points plus fréquents — `LevelUpHandler.java`

**Problème:** Points seulement à level 50 (+1) et 100 (+3). 22 stats × 4 points = 88 points max pour 42 perks.

**Solution:** +1 point tous les 20 niveaux (20/40/60/80) +3 à 100 = +7 points/stat = 154 points max.

**Files:**
- Modify: `src/main/java/tong/statmod/progression/LevelUpHandler.java`

### Task 3: Split PerkEffectHandler

**Problème:** 518 lignes, 14 event handlers, tout mélangé.

**Solution:** 5 fichiers par domaine (combat, crafting, magic, survival, mental).

**Files:**
- Create: `src/main/java/tong/statmod/perks/combat/CombatPerkHandler.java`
- Create: `src/main/java/tong/statmod/perks/crafting/CraftingPerkHandler.java`
- Create: `src/main/java/tong/statmod/perks/magic/MagicPerkHandler.java`
- Create: `src/main/java/tong/statmod/perks/survival/SurvivalPerkHandler.java`
- Create: `src/main/java/tong/statmod/perks/mental/MentalPerkHandler.java`
- Delete: `src/main/java/tong/statmod/perks/PerkEffectHandler.java`

### Task 4: Base class TabbedScreen — CharacterScreen + PerkScreen

**Problème:** Duplication tabs, background, titre.

**Solution:** `TabbedScreen` abstrait avec template method.

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/TabbedScreen.java`
- Modify: `src/main/java/tong/statmod/client/gui/CharacterScreen.java`
- Modify: `src/main/java/tong/statmod/client/gui/PerkScreen.java`

### Task 5: CapabilityHelper + UUIDs constants

**Problème:** Boilerplate `getCapability(...).ifPresent(...)` 50× répété.

**Solution:** Helper statique + constantes UUIDs centralisées.

**Files:**
- Create: `src/main/java/tong/statmod/util/CapabilityHelper.java`
- Create: `src/main/java/tong/statmod/util/ModUUIDs.java`
- Modify: tous les event handlers

### Task 6: Nettoyage orphelins

**Problème:** 62 PNGs orphelins dans `textures/gui/skill/`.

**Solution:** Supprimer le dossier flat.

**Files:**
- Delete: `src/main/resources/assets/statmod/textures/gui/skill/`

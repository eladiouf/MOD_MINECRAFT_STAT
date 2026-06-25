# STAT MOD — Architecture d'Intégration Multi-Mods

**Date:** 2026-06-16
**Version:** 2.0 (détaillée pour implémentation)
**Plateforme:** NeoForge 21.1.219+, JDK 21, Mojang Official Mappings
**Package racine:** `tong.statmod`

---

## 1. Principe Fondamental

STAT MOD est le **cerveau central** qui orchestre Tensura, Epic Fight, Mahou Tsukai, Parcool et Overgeared via ses 22 stats et 84 perks.

**Règles :**
- **STAT MOD ne remplace rien** — il s'interface avec les API publiques existantes
- **Aucun mixin** dans Tensura (crashes à répétition — interdit définitivement)
- Toute intégration utilise réflexion ou API publique
- Chaque compat est optionnelle via `ModList.get().isLoaded("modid")`

---

## 2. Fichiers d'Intégration Existants

### 2.1 EpicFightCompat.java
**Chemin:** `src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java`

- Utilise **EpicFight EventHook system** (PAS NeoForge bus)
- Souscrit à: `EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE`, `.KILL_ENTITY`, `.ON_DODGE`, `EpicFightEventHooks.Player.COMBO_ATTACK`
- Détection d'arme via `EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory()`
- Mapping: `Sword/Longsword/Uchigatana/Dagger/Tachi → BLADE_TECHNIQUE`, `Axe/Greatsword → BRUTE_FORCE`, `Spear/Trident/Ranged → PRECISION`, `Fist → BRUTE_FORCE + AGILITY`

### 2.2 ParcoolCompat.java
**Chemin:** `src/main/java/tong/statmod/integration/parcool/ParcoolCompat.java`

- Utilise **NeoForge EVENT_BUS** (les events Parcool sont dessus)
- Souscrit à: `ParCoolActionEvent.Start$Post`, `ParCoolActionEvent.Tick$Post`
- Identification par `action.getClass().getSimpleName().toLowerCase()`
- Mapping: `WallRun/Dodge/Roll/Flipping/Vault/WallJump → AGILITY + PHYSICAL_ENDURANCE`, `Slide/FastSwim/QuickTurn → AGILITY`
- Tick prolongé: `WallRun/ClingToCliff/HangDown/WallSlide → PHYSICAL_ENDURANCE/s`

### 2.3 MahouCompat.java
**Chemin:** `src/main/java/tong/statmod/integration/mahou/MahouCompat.java`

- Utilise **NeoForge EVENT_BUS** + réflexion
- Détection sort: `PlayerInteractEvent.RightClickItem` + `instanceof SpellScroll` (via `Class.forName("stepsword.mahoutsukai.item.spells.SpellScroll")`)
- Tracking mana: `Utils.getPlayerMahou(player)` → `IMahou.getStoredMana()` via réflexion, diff tick/40s
- Mapping: `spell cast → ARCANE_POWER +3`, `mana consommé → MANA_POOL + ARCANE_POWER`

---

## 3. Système de Races (via Tensura) — À Implémenter

### 3.1 Fichier: `src/main/java/tong/statmod/integration/tensura/TensuraRaceHandler.java`

```java
package tong.statmod.integration.tensura;

// Classe utilitaire pour lire la race Tensura et appliquer les bonus stats

public final class TensuraRaceHandler {
    private static boolean loaded = false;

    public static void init() {
        loaded = ModList.get().isLoaded("tensura");
        if (!loaded) return;
        // S'abonner aux events Architectury via réflexion
        NeoForge.EVENT_BUS.register(TensuraRaceHandler.class);
    }

    // Lecteur de race via réflexion (pas de dépendance compile-time)
    public static String getRaceName(Player player) {
        // RaceAPI.getRaceFrom(player) -> Races -> getRace() -> ManasRaceInstance -> getRaceId()
        // Retourne "tensura:dwarf", "tensura:human", "tensura:elf", "tensura:beastfolk"...
    }

    // Applique les bonus stats selon la race
    public static void applyRaceBonuses(Player player) { ... }
}
```

### 3.2 Détection d'évolution

Via `RaceEvents.SET_RACE` (Architectury event bus) en réflexion:
```java
// RaceEvents.SET_RACE.register((oldRace, entity, newRace, forced, cancel, msg) -> { ... })
// Quand un joueur évolue, appeler applyRaceBonuses() avec les nouvelles stats
```

### 3.3 Mapping Races → Stats

Fichier `data/statmod/race_bonuses.json` (ou codé en dur dans `TensuraRaceHandler`) :

| Race Tensura (path) | Bonus Stats |
|--------------------|-------------|
| `dwarf` | FORGING +2, PHYSICAL_ENDURANCE +2 |
| `enlightened_dwarf` | FORGING +3, PHYSICAL_ENDURANCE +3, ERUDITION +1 |
| `dwarf_saint` | FORGING +4, PHYSICAL_ENDURANCE +4, ERUDITION +2 |
| `divine_dwarf` | FORGING +6, PHYSICAL_ENDURANCE +6, ERUDITION +3, BRUTE_FORCE +2 |
| `human` | ERUDITION +2, CASTING_SPEED +2 |
| `enlightened_human` | ERUDITION +3, CASTING_SPEED +3, ARCANE_POWER +1 |
| `elf` | PRECISION +2, ARCANE_POWER +2 |
| `enlightened_elf` | PRECISION +3, ARCANE_POWER +3, CASTING_SPEED +1 |
| `beastfolk` | BRUTE_FORCE +2, AGILITY +2 |
| `enlightened_beastfolk` | BRUTE_FORCE +3, AGILITY +3, PHYSICAL_ENDURANCE +1 |

Toutes les 70+ races Tensura peuvent avoir un mapping. Si non trouvé → pas de bonus.

---

## 4. Arbre de Sorts Unifié (Tensura + Mahou) — À Implémenter

### 4.1 Architecture

Chaque sort Tensura ou Mahou Tsukai est représenté par un **perk STAT MOD**.

**Fichier:** `src/main/java/tong/statmod/perks/Perk.java` (existant — 84 entrées)
**Fichier:** `src/main/java/tong/statmod/perks/PerkManager.java` (existant)
**Fichier:** `src/main/java/tong/statmod/integration/tensura/TensuraSpellGate.java` (NOUVEAU)
**Fichier:** `src/main/java/tong/statmod/integration/tensura/TensuraSkillGate.java` (existant — renommé depuis SkillPerkGate)

### 4.2 Flow: Perk → Apprentissage du sort

```
Le joueur débloque un perk (ex: "Fire Magic I")
    → PerkManager vérifie les prérequis stats (FIRE_AFFINITY >= 10)
    → PerkEffectHandler appelle TensuraSpellGate.learnSpell(player, spellId)
    → TensuraSpellGate utilise SkillAPI.getSkillsFrom(player).learnSkill(spellId) [réflexion]
    → Le sort est appris dans Tensura
```

### 4.3 Mapping Perks → Sorts Tensura

```java
// Dans TensuraSpellGate.java
private static final Map<String, String> PERK_TO_SPELL = Map.of(
    "fire_bolt", "tensura:fire_bolt",           // Magic Aspectual Fire
    "water_heal", "tensura:healing_rain",       // Magic Aspectual Water
    "wind_blade", "tensura:wind_cutter",        // Magic Aspectual Wind
    "earth_wall", "tensura:earth_barrier",      // Magic Aspectual Earth
    "darkness_veil", "tensura:darkness",        // Magic Spiritual Darkness
    "light_bind", "tensura:light_binding",      // Magic Spiritual Light
    "space_shift", "tensura:spatial_movement",  // Magic Spiritual Space
    // + tous les autres sorts Tensura (Aspectual/Spiritual/Summoning)
);
```

### 4.4 Mapping Perks → Items/Sorts Mahou Tsukai

```java
// Dans TensuraSpellGate.java (même classe, section Mahou)
private static final Map<String, String> PERK_TO_MAHOU = Map.of(
    "mystic_staff", "mahoutsukai:mystic_staff_spell_scroll",
    "gandr", "mahoutsukai:gandr_spell_scroll",
    "rho_aias", "mahoutsukai:rho_aias_spell_scroll",
    "fallen_down", "mahoutsukai:fallen_down_spell_scroll",
    // + les 45 sorts Mahou Tsukai
);
```

Quand le perk est débloqué:
- Tensura: `SkillAPI.getSkillsFrom(player).learnSkill(resourceLocation)` [réflexion]
- Mahou: donner l'item `SpellScroll` correspondant dans l'inventaire du joueur

### 4.5 Détection de gain d'XP magique

```java
// Tensura: via SkillEvents.ACTIVATE_SKILL (Architectury event bus)
// Quand un joueur active un skill Tensura de type Magic:
//   → ARCANE_POWER +XP selon le coût en magicule
//   → AFFINITY correspondante +XP selon l'élément du sort

// Mahou: via PlayerInteractEvent.RightClickItem
// Quand le joueur utilise un item instanceof SpellScroll [réflexion]:
//   → ARCANE_POWER +3
```

---

## 5. Interaction Stats → EP Tensura — À Implémenter

### 5.1 Fichier: `src/main/java/tong/statmod/integration/tensura/TensuraXpMultiplier.java`

```java
// Multiplie le gain d'EP Tensura selon les stats STAT MOD
// Appelé à chaque fois que Tensura donne de l'EP à un joueur

public float getEpMultiplier(Player player, String actionType) {
    PlayerStatData data = player.getData(ModAttachments.STATS);
    float mult = 1.0f;

    switch (actionType) {
        case "melee_sword":
            mult += data.getLevel(StatType.BLADE_TECHNIQUE.index) * 0.05f;
            break;
        case "melee_axe":
            mult += data.getLevel(StatType.BRUTE_FORCE.index) * 0.05f;
            break;
        case "magic_fire":
            mult += data.getLevel(StatType.FIRE_AFFINITY.index) * 0.05f;
            break;
        // ...
    }
    return mult;
}
```

**Intégration :** Via réflexion sur `IExistence.setEP()` → avant de set, multiplier par le coef.

### 5.2 Formule
```
EP_gain_final = EP_gain_base × (1 + stat_level × 0.05)
```

---

## 6. Refonte des Handlers XP — À Implémenter

### 6.1 CombatXPHandler.java
**Chemin:** `src/main/java/tong/statmod/progression/CombatXPHandler.java`

**Nouveau design :** Distribue l'XP stat par stat selon l'arme.

```java
@SubscribeEvent
public static void onLivingDeath(LivingDeathEvent event) {
    // Déjà existant: détecte l'arme du tueur
    // Nouveau: va chercher la WeaponCategory Epic Fight si présent,
    //   sinon détermine le type d'arme via l'item vanilla
    // Distribue XP à UNE SEULE stat:
    //   Sword → BLADE_TECHNIQUE
    //   Axe → BRUTE_FORCE
    //   Bow/Crossbow → PRECISION
    //   Trident/Spear → PRECISION
}
```

### 6.2 NonCombatXPHandler.java
**Chemin:** `src/main/java/tong/statmod/progression/NonCombatXPHandler.java`

**Nouveau design :** XP pour les actions non-combat.

```java
// Mining → FORGING (quand on mine du minerai)
// Crafting → FORGING (quand on craft à l'établi)
// Brewing → ALCHEMY (quand on brew)
// Cooking → COOKING (quand on cuit)
// Saut/WallRun → AGILITY (via ParcoolCompat)
// Nager → PHYSICAL_ENDURANCE
```

---

## 7. Arbre de Compétences Epic Fight → À Implémenter

### 7.1 Fichier: `src/main/java/tong/statmod/perks/EpicFightPerkGate.java`

```java
// Quand un perk de combat est débloqué:
// - Donne un skill Epic Fight via SkillAPI [réflexion]
// - OU augmente les stats Epic Fight (dégâts, stun, etc.)

private static final Map<String, ResourceLocation> PERK_TO_EF_SKILL = Map.of(
    "brute_force_mastery", ResourceLocation.parse("epicfight:heavy_attack"),
    "blade_master", ResourceLocation.parse("epicfight:sword_mastery"),
    "precision_shot", ResourceLocation.parse("epicfight:bow_mastery"),
    "agile_fighter", ResourceLocation.parse("epicfight:dodge_mastery"),
    // ...
);
```

---

## 8. Interface Overgeared — À Implémenter

### 8.1 Fichier: `src/main/java/tong/statmod/integration/overgeared/OvergearedCompat.java`

```java
// Détection d'actions de forge via PlayerInteractEvent
// Quand le joueur interagit avec un block Overgeared (anvil, furnace):
//   → FORGING +XP

// Lecture de la qualité des items forgés (ForgingQuality) via réflexion
// pour donner +XP si qualité élevée
```

**Pas d'API Overgeared** → utiliser réflexion pour :
- Détecter les blocks Overgeared par leur nom (`overgeared:smithing_anvil_*`, `overgeared:casting_furnace`)
- Lire le composant `forging_quality` sur les items finis

---

## 9. Répartition des 84 Perks (Détaillé)

### Combat Physique (24 perks)
```
BRUTE_FORCE:   HeavySwing1-4, ArmorBreaker, ShieldBash, WarCry          (6)
BLADE_TECH:    BladeDance, ParryMaster, CounterStrike, BleedWounds      (4)
PRECISION:     EagleEye, QuickShot, PiercingArrow, HeadshotMaster       (4)
RAPIDITE:      SwiftAttack, DoubleStrike, SpeedDemon, Blitz             (4)
AGILITY:       NimbleFoot, Acrobat, ParkourMaster, ShadowStep           (4)
INTIMIDATION:  FrightfulPresence, BattleCry                             (2)
```

### Magie Tensura (16 perks)
```
ARCANE_POWER:  ManaWell1-3, Archmage                                      (4)
FIRE_AFFINITY: FireBolt, FireStorm, Inferno, Phoenix                      (4)
WATER_AFFINITY:WaterBolt, IcePrison, HealingRain, Tsunami                 (4)
WIND_AFFINITY: WindCutter, LightningBolt, Storm, Tempest                  (4)
```

### Magie Mahou (8 perks)
```
EARTH_AFFINITY:EarthWall, Fissure, GravityWell, Meteor                     (4)
AFFINITIES:    MysticStaff, Gandr, RhoAias, FallenDown                    (4)
```

### Parkour (8 perks)
```
AGILITY:       WallRunner, CatFall, Aerialist, SpeedSprinter              (4)
PHYS_ENDURANCE:EndlessRun, CliffHanger, IronLungs, Marathoner             (4)
```

### Artisanat (10 perks)
```
FORGING:      MasterSmith, RapidForge, PerfectQuality, DurableRepair     (4)
COOKING:      Chef, Gourmet, NourishingMeal, Feast                        (4)
ALCHEMY:      Alchemist, PotionMaster                                     (2)
```

### Passives Générales (18 perks)
```
MAGIC_RESIST: MagicShell, Nullify, AntiMagic, SpellAbsorb                (4)
CASTING_SPEED:QuickCast, InstantCast, ChantMaster, DualCast               (4)
ERUDITION:    Scholar, Librarian, AncientKnowledge, Wisdom                (4)
MANA_POOL:    DeepPool, ManaBattery, ManaSurge, ManaSpring               (4)
PHYS_ENDURANCE:ToughSkin, Regenerator                                      (2)
```

---

## 10. Dépendances et Event Buses

### Vérification de présence
```java
boolean hasTensura   = ModList.get().isLoaded("tensura");
boolean hasEpicFight = ModList.get().isLoaded("epicfight");
boolean hasMahou     = ModList.get().isLoaded("mahoutsukai");
boolean hasParcool   = ModList.get().isLoaded("parcool");
boolean hasOvergeared= ModList.get().isLoaded("overgeared");
```

### Event Buses utilisés par intégration

| Intégration | Event Bus | Méthode d'abonnement |
|------------|-----------|---------------------|
| Tensura races | Architectury EventBus | Réflexion sur `RaceEvents.SET_RACE` |
| Tensura skills | Architectury EventBus | Réflexion sur `SkillEvents.*` |
| Tensura EP | Réflexion directe | Wrap `IExistence.setEP()` |
| Epic Fight | EpicFight `EventHook` | `EpicFightEventHooks.Entity.*.registerEvent()` |
| Mahou Tsukai | NeoForge EVENT_BUS | `PlayerInteractEvent.RightClickItem` |
| Parcool | NeoForge EVENT_BUS | `@SubscribeEvent` direct |
| Overgeared | NeoForge EVENT_BUS | `PlayerInteractEvent` + blocks |

---

## 11. Ordre d'Implémentation Recommandé

### Phase 1: Refonte Handlers XP
- `CombatXPHandler.java` → XP stat par stat
- `NonCombatXPHandler.java` → XP non-combat
- Ajouter `ActionType.java` mapping pour toutes les actions

### Phase 2: Interface Tensura Races
- `TensuraRaceHandler.java` → lecture race, bonus stats
- Réflexion sur `RaceAPI`, `RaceEvents.SET_RACE`
- Tests avec toutes les 70+ races

### Phase 3: Interface Tensura EP
- `TensuraXpMultiplier.java` → stats → multiplicateur EP
- Hook réflexif sur `IExistence.setEP()`

### Phase 4: Arbre de sorts unifié
- `TensuraSpellGate.java` → perks → sorts Tensura + Mahou
- Mapping complet des 45+ sorts Mahou + sorts Tensura

### Phase 5: Arbre Epic Fight
- `EpicFightPerkGate.java` → perks → compétences Epic Fight

### Phase 6: Interface Overgeared
- `OvergearedCompat.java` → artisanat → XP FORGING/COOKING/ALCHEMY

---

## 12. Classes et Fichiers de Référence

### Existant (déjà implémenté)
```
src/main/java/tong/statmod/
├── stats/StatType.java                     (22 stats, enum)
├── stats/StatEffectApplier.java            (effets stats sur gameplay)
├── progression/LevelUpHandler.java         (milestones, perk points)
├── progression/CombatXPHandler.java        (XP combat — À REFONDRE)
├── progression/NonCombatXPHandler.java     (XP non-combat — À REFONDRE)
├── progression/ActionType.java             (enum types actions)
├── perks/Perk.java                         (84 perks)
├── perks/PerkManager.java                  (unlock logic)
├── perks/PerkEffectHandler.java            (84 effets)
├── perks/PerkTier.java                     (6 tiers)
├── perks/PerkState.java                    (runtime tracking)
├── integration/epicfight/EpicFightCompat.java   (OK)
├── integration/parcool/ParcoolCompat.java       (OK)
├── integration/mahou/MahouCompat.java           (OK)
├── integration/tensura/TensuraEventSubscriber.java  (existant)
├── integration/tensura/SoulLevelSyncHandler.java    (existant)
├── integration/tensura/SkillPerkGate.java          (existant — À RENOMMER)
├── storage/PlayerStatData.java             (attachment)
├── storage/ModAttachments.java             (registration)
├── network/SyncHelper.java                 (sync stats)
├── command/StatsCommands.java               (commandes)
└── STATMod.java                             (entry point)
```

### À créer
```
src/main/java/tong/statmod/integration/
├── tensura/TensuraRaceHandler.java          (NOUVEAU)
├── tensura/TensuraSpellGate.java            (NOUVEAU)
├── tensura/TensuraSkillGate.java            (RENOMMÉ depuis SkillPerkGate)
├── tensura/TensuraXpMultiplier.java         (NOUVEAU)
├── epicfight/EpicFightPerkGate.java         (NOUVEAU)
└── overgeared/OvergearedCompat.java         (NOUVEAU)
```

---

## 13. Tests

```bash
./gradlew build          # Compilation + tests unitaires
./gradlew runClient     # Test en jeu
```

Les tests vérifient:
- `EpicFightCompat` → weapon category mapping
- `MahouCompat` → réflexion SpellScroll
- `ParcoolCompat` → action mapping
- `TensuraRaceHandler` → lecture race via réflexion
- `CombatXPHandler` → XP stat unique par arme

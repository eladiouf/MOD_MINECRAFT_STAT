# STAT MOD — CLAUDE.md

> Ce fichier est lu automatiquement par Claude à chaque session dans la branche `neoforge-1.21.1`.

---

## 🎯 Objectif Global

STAT MOD → **Standalone Stats System sur NeoForge 1.21.1**, sans Epic Fight, pour intégration future avec **Tensura Reincarnated**.

**Roadmap :**
1. MVP : Stats (22) + XP/Leveling + Perks (84) sur NeoForge 1.21.1 standalone
2. Intégration Tensura Reincarnated (hooks dans leurs systèmes de race/skill/level)

---

## 📦 Stack Technique

| Stack | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.133+ |
| JDK | 21 |
| Mappings | Official Mojang |
| Build | NeoGradle 7.x (`net.neoforged.gradle.userdev`) |
| Tests | JUnit Jupiter 5.10 |

**Absolument PAS :**
- ❌ Epic Fight ou ses addons
- ❌ ForgeGradle / Parchment Librarian
- ❌ Forge Capabilities (→ NeoForge Attachments)
- ❌ Forge SimpleChannel (→ NeoForge CustomPacketPayload)

---

## 📁 Architecture du Code (État Actuel)

La branche contient encore le code Forge 1.20.1. Voici ce qui doit être porté, réécrit, ou supprimé :

### 🔵 À PORTER (conversion Forge → NeoForge)

| Package | Fichiers | Statut |
|---|---|---|
| `stats/` | `StatType.java` (enum 22 stats) | ✅ Pur Java, port direct |
| `stats/` | `StatEffectApplier.java` | 🔧 Remplacer events Forge → NeoForge |
| `stats/` | `StatCalculator.java` | ✅ Pur Java |
| `perks/` | `PerkTier.java` | ✅ Créé, pur Java |
| `perks/` | `Perk.java` (84 entries) | ✅ Créé, pur Java |
| `perks/` | `PerkManager.java` | 🔧 Adapter pour NeoForge attachments |
| `perks/` | `PerkState.java` | ✅ Créé, pur Java |
| `perks/` | `PerkEffectHandler.java` | 🔧 Créé, à adapter (events vanilla) |
| `progression/` | `LevelUpHandler.java` | 🔧 Adapter events |
| `progression/` | `CombatXPHandler.java` | 🔧 Remplacer Epic Fight events |
| `command/` | `StatsCommands.java` | 🔧 Adapter CommandSourceStack NeoForge |
| `client/` | `ClientPerkCache.java` | 🔧 Adapter payloads |
| `client/` | `ClientStatsCache.java` | 🔧 Adapter payloads |
| `client/gui/` | `PerkScreen.java`, `TalentTreePanel.java`, `PerkNodeWidget.java` | 🔧 Adapter GuiGraphics API |
| `network/` | Tous les packets | 🔧 Remplacer SimpleChannel → CustomPacketPayload |
| `mixin/` | `PlayerListMixin.java` | 🔧 Adapter au login sync NeoForge |

### 🟢 À GARDER TEL QUEL

Ces fichiers sont purs Java (Forge-indépendants) et peuvent être copiés sans changement :
- `stats/StatType.java`
- `perks/PerkTier.java`
- `perks/Perk.java`
- `perks/PerkState.java`
- `capability/PlayerStats.java` → à réécrire en `PlayerStatData.java` (attachment)
- `fatigue/`, `thirst/` → décision ultérieure

### 🔴 À SUPPRIMER (dépendent d'Epic Fight)

Supprimer complètement (pas de port) :
- `integration/EpicFightCompat.java`
- `integration/EpicParcoolCompat.java`
- `integration/L2Hostility*.java`
- `integration/Ftb*.java`
- `skills/` (tout le package — lié aux SkillSlots Epic Fight)
- `weapon/` (lié aux WeaponCategories Epic Fight)
- `combat/MobSkill*.java`
- `capability/MobSkillState*.java`
- `capability/MobStats*.java`
- `reload/MobSkillReloadListener.java`

---

## 📁 Architecture CIBLE (après MVP)

```
src/main/java/tong/statmod/
├── STATMod.java                    ← @Mod entry point (NeoForge)
├── storage/
│   ├── PlayerStatData.java         ← int[23] levels, xp, perkPoints
│   └── ModAttachments.java         ← AttachmentType<PlayerStatData> registration
├── stats/
│   ├── StatType.java               ← Enum 22 stats (0-22)
│   ├── StatEffectApplier.java      ← Applique effets via events vanilla
│   └── StatCommands.java           ← /statlevel command
├── progression/
│   ├── LevelUpHandler.java         ← Milestones, perk points grants
│   ├── CombatXPHandler.java        ← Gain d'XP via events de combat
│   ├── NonCombatXPHandler.java     ← Gain d'XP via craft/minage/etc
│   └── ActionType.java             ← Enum des actions XP
├── perks/
│   ├── PerkTier.java               ← 6 tiers (CORE → TRANSCENDENCE)
│   ├── Perk.java                   ← 84 entries
│   ├── PerkManager.java            ← Unlock logic, per-stat points
│   ├── PerkState.java              ← Runtime tracking maps
│   └── PerkEffectHandler.java      ← 84 effets
├── network/
│   ├── SyncPerksPayload.java       ← CustomPacketPayload
│   ├── UnlockPerkPayload.java
│   ├── BatchSyncPayload.java
│   ├── StatUpdatePayload.java
│   └── NetworkHandler.java         ← PayloadRegistrar
├── client/
│   ├── ClientStatCache.java
│   ├── ClientPerkCache.java
│   ├── StatModKeyMappings.java
│   └── gui/
│       ├── PerkScreen.java         ← Multitab perk tree
│       ├── StatsOverviewScreen.java← Liste stats + niveaux
│       └── perks/
│           ├── PerkNodeWidget.java ← Widget nœud avec couleurs
│           └── TalentTreePanel.java← 6 nœuds par stat
├── item/
│   ├── ModItems.java               ← Items registration
│   ├── PerkTomeItem.java           ← +1 point de perk
│   └── RespecStoneItem.java        ← Reset perks
├── mixin/
│   └── PlayerListMixin.java        ← Sync data on join
├── sound/
│   ├── ModSounds.java
│   └── SoundHelper.java
├── config/
│   ├── Config.java
│   └── ConfigPresets.java
└── api/
    ├── IStatModPlugin.java
    └── PluginManager.java
```

---

## 🧠 Règles de Code

### TypeScript-strict pour Java
- ❌ **Pas de `Object`** sans cast typé
- ❌ **Pas de `List` brute** — toujours `List<T>`
- ❌ **Pas de suppression de warnings** sans justification
- ✅ Types explicites partout

### NeoForge API (vs Forge)
```
Forge                          → NeoForge
──────────────────────────────────────────────────
net.minecraftforge             → net.neoforged.neoforge
MinecraftForge.EVENT_BUS       → NeoForge.EVENT_BUS
@Mod("modid")                  → @Mod(STATMod.MODID)
Capability + Provider          → AttachmentType<T>
SimpleChannel                  → CustomPacketPayload
PoseStack                      → GuiGraphics (avec pose())
ResourceLocation(ns, path)     → ResourceLocation.fromNamespaceAndPath(ns, path)
DeferredRegister.create(..., "modid") → DeferredRegister.create(Registry, MODID)
Mod.EventBusSubscriber         → @EventBusSubscriber
FMLJavaModLoadingContext       → @Mod constructor param (IEventBus modBus)
```

### Conventions
- Noms variables : anglais `camelCase`
- Classes : `PascalCase`
- Commentaires métier : français
- Commentaires techniques : anglais
- Indentation : 4 espaces
- `@Override` toujours présent
- `LOGGER` = `LoggerFactory.getLogger(STATMod.class)` (SLF4J, pas Log4J)

### Server Components (côté Minecraft)
- Toute logique de jeu → serveur uniquement (`!player.level().isClientSide`)
- Client → uniquement rendu, cache, input
- Network → payloads sérialisés, pas d'objets Minecraft directs

---

## 🛤️ Design Doc & Plan

Deux documents ont été créés pour guider l'implémentation :

1. **Design spec** : `docs/superpowers/specs/2026-06-14-neoforge-standalone-stats-design.md`
   - Architecture complète, décisions, mapping des stats, ordre d'implémentation

2. **Implementation plan** : `docs/superpowers/plans/2026-06-14-neoforge-standalone-stats.md`
   - 11 tasks détaillées avec code complet, de la scaffold à la vérification

---

## 🔧 Commandes

```bash
# Build (dans la branche neoforge-1.21.1)
./gradlew build

# Run client
./gradlew runClient

# Tests
./gradlew test

# Lancer un build complet avec tests
./gradlew check
```

---

## 🎮 Intégration Tensura Reincarnated (FUTUR)

Après le MVP, on intégrera STAT MOD avec Tensura Reincarnated.

**Pistes d'intégration :**
- Races Tensura → stats de base modifiées (ex: Dragon → +Brute Force, Slime → +Agility)
- Compétences Tensura (Unique/Extra/Résistance) → débloquées via perks
- Niveau d'âme Tensura → niveau global STAT MOD
- Magie Tensura → stats magiques (ARCANE_POWER, AFFINITÉS)
- Artisanat Tensura → stats FORGING/COOKING/ALCHEMY
- L'XP se gagne via les actions du jeu (combat, craft, minage) — comme dans l'original Tensura

**Dépendance :** Tensura Reincarnated sur NeoForge 1.21.1 (à confirmer)

---

## ❗ Points d'Attention

1. **NeoForge 1.21.1 utilise JDK 21** — pas JDK 17 comme le Forge 1.20.1
2. **Les mappings sont Mojang officiel** — pas Parchment
3. **Les mixins** nécessitent `statmod.mixins.json` + déclaration dans `neoforge.mods.toml`
4. **Payload réseau** : `StreamCodec` + `CustomPacketPayload` — plus de simple channel
5. **GuiGraphics** remplace `PoseStack` direct — l'API de rendu a changé
6. **ResourceLocation** : utiliser `fromNamespaceAndPath()` au lieu du constructeur direct
7. **Le pack_format pour 1.21** est `34` (pas `15` comme 1.20.1)
8. **Les enregistrements** (registries) : `DeferredRegister<AttachmentType<?>>` via `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`

---

## 📝 Décisions Techniques

1. **Pourquoi NeoForge et pas Forge 1.21.1 ?** Tensura Reincarnated n'existe que sur NeoForge 1.21.1
2. **Pourquoi supprimer Epic Fight ?** Epic Fight n'a pas de version NeoForge
3. **Pourquoi des attachments et pas des capabilities ?** NeoForge a remplacé les capabilities par des attachments — plus simples, sans provider boilerplate
4. **Pourquoi 22 stats ?** Conservation des stats existantes, dont 8 sans perks (stats magiques) pour le futur système de magie
5. **84 perks ?** 6 perks × 14 stats actives, exactement comme dans la refonte récente
6. **Pas d'XP gagnée dans le MVP ?** L'architecture data-driven est prioritaire, les events d'acquisition d'XP seront branchés après

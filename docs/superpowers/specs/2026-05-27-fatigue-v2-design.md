# STAT Mod — Système Fatigue V2

**Date:** 2026-05-27
**Package:** tong.statmod

---

## 1. Cycle jour/nuit (24h → 48 min)

| | Vanilla | Fatigue V2 |
|---|---|---|
| 1 jour MC | 20 min (24000 ticks) | **48 min (48000 ticks)** |
| Jour (soleil) | 10 min | **24 min** |
| Nuit | 7 min | **16 min** |

Multiplicateur `0.4167` — le jeu avance d'1 tick toutes les 2.4 sec.

### Implémentation

`DayLengthHandler` intercepte `ServerTickEvent.END` et ajoute du temps pour compenser la lenteur.

---

## 2. Accumulation passive de fatigue

| Condition | Taux/sec | Perte sur 24 min |
|-----------|---------|-----------------|
| Ciel ouvert jour | 0.03 | 43.2 |
| Ciel ouvert nuit | 0.05 | — |
| Abrité / sous terre | 0.02 | 28.8 |

---

## 3. Récupération (seulement 4 façons)

| Méthode | Récupération |
|---------|-------------|
| Sneak | -0.3/sec |
| Fiole d'eau (clic droit) | -20 par fiole |
| Dormir | Reset à 0 |
| Potion Adrénaline | +1/sec pendant 45s |
| ~~Repos debout~~ | **SUPPRIMÉ** |

---

## 4. Coûts actifs

| Action | Fatigue v1 | Fatigue v2 |
|--------|-----------|-----------|
| Coup porté | +3 | +5 |
| Sprint (par sec) | +1 | +3 |
| Saut | +2 | +5 |
| Dégât subi | +5 | +8 |
| Miner | +1 | +3 |
| Skill EF | +10-20 | ×2 |

---

## 5. Débuffs par palier

| Seuil | Effets |
|-------|--------|
| 10% | -5% speed, notif "fatigué" |
| 25% | -10% dmg, -10% speed attaque |
| 50% | -25% dmg, sprint bloqué, Lenteur I |
| 75% | -40% dmg, marche forcée, vignette rouge, Mining Fatigue |
| 90% | -70% dmg, skills EF bloqués, saut bloqué, écran noir progressif |
| 100% | -90% dmg, **0.5❤/sec**, mort par épuisement |

---

## 6. Privation de sommeil

| Nuits sans dormir | Pénalité au matin |
|-------------------|-----------------|
| 1 | 25% (× réduction Willpower) |
| 2 | 50% |
| 3+ | 80% |

---

## 7. Potion d'Adrénaline

Swiftness + Blaze Powder → Adrénaline (45s, +1 fatigue/sec)

| Variante | Ingédient | Durée | Total récupéré |
|----------|-----------|-------|---------------|
| Base | Blaze powder | 45s | 45 |
| Longue | Redstone | 90s | 90 |
| Forte | Glowstone | 20s | 40 |

---

## 8. Config

Toutes les valeurs sont configurables dans `statmod-server.toml` (section fatigue).

---

## 9. Fichiers

| Fichier | Action |
|---------|--------|
| `world/DayLengthHandler.java` | Nouveau |
| `fatigue/FatigueHandler.java` | Modifier |
| `fatigue/FatigueEffects.java` | Modifier |
| `fatigue/FatigueManager.java` | Modifier |
| `world/effect/AdrenalineEffect.java` | Nouveau |
| `world/effect/ModEffects.java` | Nouveau |
| `world/effect/ModPotions.java` | Nouveau |
| `client/hud/FatigueOverlay.java` | Modifier |
| `client/ClientEventHandler.java` | Nouveau |
| `Config.java` | Modifier |
| `STATMod.java` | Modifier |

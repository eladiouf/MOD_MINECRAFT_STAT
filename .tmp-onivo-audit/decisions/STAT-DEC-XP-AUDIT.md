# STAT-DEC-XP-AUDIT — Audit complet des sources XP par stat

**Date :** 2026-06-23
**Mission :** M6/M7 (audit + fixes leveling)
**Author :** Onivo Studio runtime agent
**Branch :** `neoforge-1.21.1`

## Contexte

Test utilisateur 2026-06-23 : "le leveling up des stats il est bizarre — endurance qui monte vite, casting speed aussi."

Audit complet des XP sources réalisé sur les 23 stats, révélant **3 bugs majeurs** + **6 stats sans source XP naturelle**.

## Bugs identifiés et corrigés

### Mission η — CASTING_SPEED spam

`MagicXpBridge.onModifySpellLevel` fired +15 XP CASTING_SPEED à **chaque appel** de
`AbstractSpell.getLevelFor()`, lui-même invoqué par :
- Rendu du tooltip du spellbook (à chaque hover)
- Validation pre-cast
- Calcul du cast time
- **Chaque tick du cast pour les LONG** (20× par seconde)

Effet : +300 XP/sec pendant un cast LONG. Le joueur passait Lv 0→5 en quelques secondes.

**Fix** : déplacé l'XP CASTING_SPEED dans `onPostCast` (1× par cast complet). Amount =
`max(2, castTime/4)` pour récompenser les sorts difficiles à canaliser.

### Mission θ — PHYSICAL_ENDURANCE wallrun farm + courbe XP incohérente

**Bug 1** : `ParcoolCompat.onParkourTick` fired +1 XP toutes les secondes tant que le joueur
faisait du wallrun/cling/hangdown. 60 sec de wallrun = 60 XP, suffisant pour 6 niveaux base 0.

**Fix 1** : throttler server-side de 100 ticks (5 sec) via map UUID → lastGain.

**Bug 2** : `PlayerStatData.addXp` calculait `requiredXp(baseLevel)` sans tenir compte du
race flat bonus. Un Ogre +5 ENDURANCE avait affiché Lv 5 mais payait `requiredXp(0) = 10`
pour Lv 5→6 au lieu de `requiredXp(5) = 360`.

**Fix 2** : nouvelle méthode `addXpWithEffectiveStartLevel(index, amount, startLevel)`.
`RaceEffectApplier.addScaledXp` passe désormais le `raceFlatBonus` à la courbe. Le coût XP
est cohérent avec ce que le joueur voit affiché.

### Mission ι — 6 stats sans XP source naturelle

Sous la nouvelle économie magique (gates ARCANE_POWER + ERUDITION + tertiaire), plusieurs
stats étaient inaccessibles → blocage de gameplay critique.

| Stat | Nouvelle source | Quantité |
|---|---|---|
| `PHYSICAL_RESISTANCE` | `DefensiveXPHandler.onTakePhysicalDamage` | `dmg/2` par hit non-magic subi |
| `WATER_AFFINITY` | `MagicXpBridge.onPostCast` extension | +2 par cast Water |
| `EARTH_AFFINITY` | idem | +2 par cast Earth |
| `AIR_AFFINITY` | idem | +2 par cast Air |
| `TRACKING` | `CombatXPHandler.onKill` (projectile + distance ≥ 8) | `distance/4` |
| `KEEN_SENSES` | `EpicFightCompat.onDodge` | +3 par esquive |
| `WILLPOWER` | `DefensiveXPHandler.onMobEffectExpire` (debuffs only) | `durationSec/3` capé 60s |

## Polish complémentaire

### Mission L — Fix duplicate perk grants

`LevelUpHandler.lastGrantedTier` était une HashMap statique en mémoire. Au restart serveur
la map se vidait → joueur tier 5 recevait 5 × N perk points en double au tick suivant.

**Fix** : persisté sur `PlayerStatData.lastPerkGrantTier`, sérialisé via `ModAttachments`.

### Mission M — Toast feedback level-up

Actionbar message `✦ Stat Name → Lv. N` (effective level) déclenché automatiquement depuis
`RaceEffectApplier.addScaledXp` quand un niveau est franchi. Nouveau helper
`LevelUpFeedback.notify(player, statIndex, newEffectiveLevel)`.

### Mission N — Re-sync race au respawn / reincarnation

`TensuraRaceHandler` hooks ajoutés :
- `onLogin` : re-sync inconditionnel (auparavant seulement si `magicRace == null`)
- `onClone` : re-sync au respawn / dimension change
- `onPlayerTick` (100 ticks) : polling pour détecter reincarnation Tensura mid-game

### Mission Q — /magic info enrichi

Audit éligibilité affiché : compteurs `unlockable now`, `blocked by points / stats / prereqs`.
Permet diagnostic rapide en jeu.

### Mission S — Fix globalLevel formula

`getGlobalLevel` faisait la moyenne arithmétique sur les 23 stats. Un joueur full-mage avec
FIRE_AFFINITY Lv 10 et le reste à 0 avait `globalLevel ≈ 0` → progression ridiculement lente
vers les tiers de perk grants.

**Fix** : moyenne uniquement sur les stats ≥ 1 (les jamais-montées sont exclues du
diviseur). Préserve l'exclusion "magic locked si pas de race".

## Exit conditions

Reset des fixes XP si :
- Iron's Spellbooks change la signature de `ModifySpellLevelEvent` (peu probable)
- ParCool ajoute son propre throttler natif (rendre redondant le notre)
- Le fondateur veut un global level non pondéré (régression vers ancienne formule)
- Phase 2 magic content révèle un besoin de scaling XP différent par école

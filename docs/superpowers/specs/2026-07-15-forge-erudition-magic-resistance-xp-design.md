# Forge Erudition and Magic Resistance XP Design

**Date:** 2026-07-15  
**Target:** Minecraft 1.20.1, Forge 47.4.10, Java 17, Iron's Spells 3.16.2

## 1. Goal

Finish STAT Mod's Iron's Spells progression milestone by activating the two
remaining magical statistics:

- Erudition progresses when a player successfully inscribes or learns a spell
  through the supported Iron's Spells event, and when a player studies a
  vanilla enchanted book;
- Magic Resistance progresses when a player actually receives eligible spell
  damage.

The implementation reuses the existing authoritative XP, persistence,
synchronization, rolling-limit, and notification pipeline. It adds no affinity
system and does not classify progression by spell school.

## 2. Enchanted-book study interaction

Holding use with a vanilla `Enchanted Book` starts a 40-tick study action.
Because the vanilla enchanted-book item has no continuous-use lifecycle, a
small client input bridge sends bounded begin, heartbeat, and release messages
for the vanilla use key. The messages never contain reward data. The server
captures the hand and an immutable identity of the held stack at the start,
requires a recent heartbeat throughout the action, and rate-limits the input.
Completion succeeds only if the player is alive, is still reporting the held
use key, and still holds the same book with the same stored enchantments.

Releasing use early, changing the held item, changing its enchantments, dying,
or otherwise interrupting item use cancels the study. Cancellation grants no
XP, consumes no item, and plays no completion effect.

On successful completion, the server reads every valid stored enchantment and
sums its contribution:

`contribution = enchantment level * rarity weight * 5`

Rarity weights are:

| Vanilla rarity | Weight |
| --- | ---: |
| Common | 1 |
| Uncommon | 2 |
| Rare | 4 |
| Very rare | 8 |

Invalid registry entries and non-positive levels contribute zero. The combined
raw reward is clamped to 160 XP per book before it enters the existing rolling
limit of 200 accepted XP per statistic per 1,200 ticks.

The study handler uses one dedicated transactional award entry point. Its order
is `validate -> calculate -> reserve accepted XP -> mutate stat and limiter ->
consume -> synchronize -> play completion effect`. Capability mutation and
consumption run on the same server thread before any observer is notified.
Exactly one book is removed outside Creative mode. Creative mode retains the
book but uses the same rolling limit. If Erudition is already level 100 or the
limiter accepts zero XP, the book is not consumed and the completion effect is
not played.

Successful study broadcasts the Totem of Undying activation animation and
sound for the studying player. The effect is presentation only: it never drives
the reward or consumption decision.

## 3. Iron's Spells Erudition XP

STAT Mod listens to the supported committed inscription/learning event rather
than clicks, menu packets, or inventory changes. A reward is issued once only
after Iron's Spells confirms a successful inscription. The server derives the
reward from trusted event data such as the learned spell level and clamps it to
a bounded policy value.

Cancelled, invalid, duplicated, client-only, or non-player events do not grant
XP. The adapter delegates to the shared XP service and does not mutate STAT Mod
data directly.

## 4. Magic Resistance XP

Iron's Spells 3.16.2 exposes `SpellDamageEvent` before its amount is fully
committed. STAT Mod therefore confirms progression on Forge's
`LivingDamageEvent` only when its damage source is an Iron's Spells
`SpellDamageSource`. The damaged server player is rewarded from the final
positive damage received. No kill, target selection, spell school, or attacker
type is required. Cancelled damage, zero or non-finite damage, self-generated
bookkeeping calls, spectators, and fake players grant nothing.

The reward policy is bounded and passes an opponent identifier to the existing
per-opponent limiter when a real attacking player exists. This prevents two
players from farming repeated spell hits while retaining the global rolling
cap. Creative players do not gain Magic Resistance XP because ordinary
damage-received progression remains disabled in Creative.

## 5. Components and boundaries

- A small enchanted-book study handler owns the server-side session,
  heartbeat timeout, validation, consumption, and completion effect.
- A client input bridge reports only begin, heartbeat, and release state for
  the vanilla use key; messages are bounded and rate-limited server-side.
- A dependency-free reward calculator converts stored enchantment level and
  rarity into bounded Erudition XP.
- The Iron's Spells adapter owns only event translation for inscription and
  spell damage.
- `XpAction`, `XpActionKind`, and `XpRewardPolicy` own reward semantics.
- `XpAwardService` remains the only Minecraft-facing mutation gateway.
- Existing capability, synchronization, level-effect refresh, notification,
  and rolling-limit code remains shared.

Client code never supplies enchantment rarity, level, damage, or reward values.
No mixin is planned because Forge item-use hooks and Iron's Spells API events
provide the required boundaries.

## 6. Failure behavior

All invalid or incomplete interactions fail closed. A failed study leaves the
inventory unchanged. Missing or incompatible Iron event data results in no XP
and a bounded diagnostic log rather than a crash. Reward calculation uses wide
intermediate arithmetic and clamps before converting to an integer.

The book is consumed only after the XP service reports a positive accepted
award. If the award pipeline rejects the action, no completion animation is
sent. The implementation must not introduce a second mana store, affinity
table, or spell-school progression path.

## 7. Verification

Unit tests cover rarity weights, levels, multi-enchantment sums, invalid data,
overflow, and the 160 XP per-book clamp. Contract and service tests cover the
40-tick lifecycle, interruption, stack identity, single consumption, Creative
retention, level 100, exhausted rolling limit, and one completion effect.

Iron adapter tests prove inscription is committed-event based and spell-damage
progression uses `LivingDamageEvent` with `SpellDamageSource`, without requiring
a kill or trusting the earlier mutable `SpellDamageEvent`. Regression tests
prove ordinary Creative XP remains disabled and spellbook casting keeps its
dedicated Creative exception.

Completion requires a clean build, the full unit-test suite, JAR inspection,
and the required-provider Forge GameTest smoke using the `test-vrai` mod set.
The resulting JAR is deployed only after Minecraft is closed, with a
time-stamped backup and a matching SHA-256 verification.

## 8. Out of scope

- affinities or per-school XP;
- consuming ordinary writable or written books;
- a custom study screen or progress bar beyond vanilla use animation;
- changing Iron's Spells spell-learning rules;
- the Curios grimoire casting coordinator from Milestone 4;
- perks and magic-tree gating from Milestone 5.

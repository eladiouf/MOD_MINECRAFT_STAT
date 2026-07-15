# Forge Erudition and Magic Resistance XP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the Forge 1.20.1 Iron's Spells progression milestone with enchanted-book study, committed spell-inscription Erudition XP, and actual spell-damage Magic Resistance XP.

**Architecture:** Pure reward calculators feed the existing `XpAction` and `XpAwardCoordinator` pipeline. A rate-limited C2S input bridge drives an authoritative 40-tick enchanted-book study session, while the Iron adapter translates `InscribeSpellEvent` and final Forge `LivingDamageEvent` data. A dedicated transactional award method consumes the book only after positive XP acceptance and before synchronization/effects.

**Tech Stack:** Java 17, Minecraft 1.20.1 official mappings, Forge 47.4.10, Iron's Spells 1.20.1-3.16.2, JUnit 5.10.2, Gradle 8.8.

## Global Constraints

- Work only on the existing `forge-1.20.1` worktree and preserve unrelated user files.
- Iron's Spells, Epic Fight, and Puffish Attributes remain required runtime dependencies.
- No affinity or per-spell-school progression is introduced.
- All reward values and inventory mutations are authoritative on the logical server.
- Enchanted-book study lasts 40 server ticks and requires begin plus heartbeats while use remains held.
- Book XP is `sum(level * rarity weight * 5)`, with weights Common 1, Uncommon 2, Rare 4, Very Rare 8, clamped to 160 per book.
- The existing 200 XP per-stat rolling 1,200-tick cap remains the final anti-farm authority.
- Creative study retains the book but observes the same XP cap; ordinary Creative XP remains disabled.
- Fake players and spectators never receive these rewards.
- No mixin and no second stat, mana, or notification pipeline.
- Every production change follows RED, GREEN, refactor, then a focused commit.

---

## File Structure

### New files

- `src/main/java/tong/statmod/progression/xp/EnchantmentStudyXp.java` — dependency-free multi-enchantment XP arithmetic.
- `src/main/java/tong/statmod/event/BookStudyTimeline.java` — dependency-free completion and heartbeat-timeout rules.
- `src/main/java/tong/statmod/event/EnchantedBookStudySessions.java` — server study state, validation, timeout, completion, consumption request, and Totem effect.
- `src/main/java/tong/statmod/network/BookStudyInputMessage.java` — bounded C2S begin/heartbeat/release payload.
- `src/main/java/tong/statmod/client/ClientBookStudyInput.java` — observes the vanilla use key and sends state only while an enchanted book is held.
- Matching JUnit or contract tests under `src/test/java/tong/statmod/`.

### Modified files

- `XpActionKind`, `XpAction`, and `XpRewardPolicy` — three new reward semantics.
- `XpAwardService` — transactional book-study entry point and shared post-award synchronization.
- `IronSpellXpEvents` — inscription and final spell-damage adapters.
- `StatNetwork` and `ClientInputEvents` — register and send study input.
- `PlayerStatsEvents` — clear study sessions on logout/clone lifecycle.
- English/French language files, README, master milestone, and supported-runtime record.

---

### Task 1: Pure magical reward semantics

**Files:**
- Create: `src/main/java/tong/statmod/progression/xp/EnchantmentStudyXp.java`
- Create: `src/test/java/tong/statmod/progression/xp/EnchantmentStudyXpTest.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpActionKind.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpAction.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpRewardPolicy.java`
- Modify: `src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java`
- Modify: `src/test/java/tong/statmod/AutomaticXpContractTest.java`

**Interfaces:**
- Produces: `EnchantmentStudyXp.calculate(List<Entry>) -> int`.
- Produces: `XpAction.bookStudied(int)`, `spellInscribed(int,int)`, and `magicDamageReceived(double)`.
- Produces: `BOOK_STUDIED`, `SPELL_INSCRIBED`, and `MAGIC_DAMAGE_RECEIVED` action kinds.

- [ ] **Step 1: Write failing calculator and policy tests**

```java
class EnchantmentStudyXpTest {
    @Test void sumsEveryEnchantmentAndUsesApprovedWeights() {
        assertEquals(5, EnchantmentStudyXp.calculate(List.of(new Entry(1, 1))));
        assertEquals(40, EnchantmentStudyXp.calculate(List.of(new Entry(4, 2))));
        assertEquals(85, EnchantmentStudyXp.calculate(List.of(
                new Entry(1, 1), new Entry(2, 4), new Entry(1, 8))));
    }

    @Test void rejectsInvalidEntriesAndClampsOverflow() {
        assertEquals(0, EnchantmentStudyXp.calculate(null));
        assertEquals(0, EnchantmentStudyXp.calculate(List.of(new Entry(0, 8))));
        assertEquals(160, EnchantmentStudyXp.calculate(List.of(
                new Entry(Integer.MAX_VALUE, 8))));
    }
}
```

Extend `XpRewardPolicyTest` to assert:

```java
assertEquals(85, amountFor(
        XpRewardPolicy.awards(XpAction.bookStudied(85)), StatType.ERUDITION));
assertEquals(160, amountFor(
        XpRewardPolicy.awards(XpAction.bookStudied(999)), StatType.ERUDITION));
assertEquals(18, amountFor(
        XpRewardPolicy.awards(XpAction.spellInscribed(2, 3)), StatType.ERUDITION));
assertEquals(15, amountFor(
        XpRewardPolicy.awards(XpAction.magicDamageReceived(7.1)),
        StatType.MAGIC_RESISTANCE));
```

The inscription formula is `clamp(5 + 2 * spellLevel + 3 * rarityValue, 5, 30)`.
Magic Resistance reuses `ceil(damage * 2)` clamped to 1..20.

- [ ] **Step 2: Run the tests and observe RED**

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.progression.xp.EnchantmentStudyXpTest" --tests "tong.statmod.progression.xp.XpRewardPolicyTest" --console=plain
```

Expected: compilation fails because the calculator, action kinds, and factories do not exist.

- [ ] **Step 3: Implement the pure calculator and reward actions**

```java
public final class EnchantmentStudyXp {
    public static final int MAX_PER_BOOK = 160;
    private EnchantmentStudyXp() {}

    public static int calculate(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        long total = 0;
        for (Entry entry : entries) {
            if (entry == null || entry.level() <= 0 || entry.rarityWeight() <= 0) continue;
            long contribution = Math.min(MAX_PER_BOOK,
                    Math.min((long) entry.level() * entry.rarityWeight(), MAX_PER_BOOK) * 5L);
            total = Math.min(MAX_PER_BOOK, total + contribution);
        }
        return (int) total;
    }

    public record Entry(int level, int rarityWeight) {}
}
```

Add the three enum constants, factories, and switch branches. `BOOK_STUDIED` returns the supplied positive quantity clamped to 160 for Erudition. `SPELL_INSCRIBED` applies the formula above using `quantity=level` and `secondary=rarityValue`. `MAGIC_DAMAGE_RECEIVED` calls `damageAward(action, StatType.MAGIC_RESISTANCE)`.

- [ ] **Step 4: Run focused and progression regression tests**

```powershell
.\gradlew.bat test --tests "tong.statmod.progression.xp.EnchantmentStudyXpTest" --tests "tong.statmod.progression.xp.XpRewardPolicyTest" --tests "tong.statmod.AutomaticXpContractTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`; all three new actions reward only their intended stat.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp src/test/java/tong/statmod/progression/xp src/test/java/tong/statmod/AutomaticXpContractTest.java
git commit -m "feat: model remaining magic XP rewards"
```

---

### Task 2: Atomic enchanted-book award boundary

**Files:**
- Modify: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`
- Modify: `src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java`

**Interfaces:**
- Consumes: `XpAction.bookStudied(int)`.
- Produces: `awardBookStudy(ServerPlayer, XpAction, long, Runnable) -> boolean`.
- Contract: callback runs exactly once after a positive accepted mutation and before snapshot/notice synchronization.

- [ ] **Step 1: Write the failing service contract**

Add source assertions that isolate the new method and verify it:

```java
assertTrue(method.contains("action.kind() != XpActionKind.BOOK_STUDIED"));
assertTrue(method.contains("player instanceof FakePlayer"));
assertTrue(method.contains("player.isSpectator()"));
assertFalse(method.contains("player.isCreative()"));
assertTrue(method.contains("beforeSync"));
assertTrue(method.contains("awardEligible("));
```

Also assert the general `award` method still calls `isEligible`, preserving the Creative rejection for inscription and damage.

- [ ] **Step 2: Run the contract test and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.progression.xp.XpAwardServiceContractTest" --console=plain
```

Expected: FAIL because `awardBookStudy` does not exist.

- [ ] **Step 3: Refactor the shared award body and add the dedicated method**

```java
public static boolean awardBookStudy(ServerPlayer player, XpAction action,
        long tick, Runnable beforeSync) {
    if (player == null || player instanceof FakePlayer || player.isSpectator()
            || action == null || action.kind() != XpActionKind.BOOK_STUDIED
            || beforeSync == null) {
        return false;
    }
    return awardEligible(player, List.of(action), tick, beforeSync);
}
```

Keep existing overloads delegating with `() -> {}`. In the shared body, call `beforeSync.run()` only after `result.changed()` and before attribute refresh, snapshot, and progress notices. Do not catch callback exceptions; the book callback is deterministic and must fail loudly in development rather than report a false successful transaction.

- [ ] **Step 4: Run service and coordinator regressions**

```powershell
.\gradlew.bat test --tests "tong.statmod.progression.xp.XpAwardServiceContractTest" --tests "tong.statmod.progression.xp.XpAwardCoordinatorTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`; existing award entry points retain their eligibility behavior.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp/XpAwardService.java src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java
git commit -m "feat: add transactional book study awards"
```

---

### Task 3: Secure study input protocol

**Files:**
- Create: `src/main/java/tong/statmod/network/BookStudyInputMessage.java`
- Create: `src/test/java/tong/statmod/network/BookStudyInputMessageTest.java`
- Create: `src/main/java/tong/statmod/client/ClientBookStudyInput.java`
- Create: `src/test/java/tong/statmod/client/ClientBookStudyInputContractTest.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/main/java/tong/statmod/client/ClientInputEvents.java`
- Modify: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/client/StatsScreenWiringContractTest.java`

**Interfaces:**
- Produces: `BookStudyInputMessage(Action action, InteractionHand hand)` with `BEGIN`, `HEARTBEAT`, `RELEASE`.
- Produces: `StatNetwork.sendBookStudyInput(Action, InteractionHand)`.
- Calls: `EnchantedBookStudySessions.input(ServerPlayer, Action, InteractionHand, long)` from Task 4.

- [ ] **Step 1: Write failing payload round-trip and client wiring tests**

```java
@Test void roundTripsEveryActionAndHand() {
    for (Action action : Action.values()) for (InteractionHand hand : InteractionHand.values()) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new BookStudyInputMessage(action, hand).encode(buffer);
        assertEquals(new BookStudyInputMessage(action, hand), BookStudyInputMessage.decode(buffer));
    }
}
```

Contract assertions require `keyUse.isDown()`, `Items.ENCHANTED_BOOK`, one begin on transition, heartbeat interval `5`, release on transition, and no reward/enchantment/damage integer in the payload.

- [ ] **Step 2: Run tests and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.network.BookStudyInputMessageTest" --tests "tong.statmod.client.ClientBookStudyInputContractTest" --console=plain
```

Expected: compilation fails because the input types do not exist.

- [ ] **Step 3: Implement bounded payload, registration, and client state transitions**

```java
public record BookStudyInputMessage(Action action, InteractionHand hand) {
    public BookStudyInputMessage {
        Objects.requireNonNull(action);
        Objects.requireNonNull(hand);
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeEnum(hand);
    }
    public static BookStudyInputMessage decode(FriendlyByteBuf buffer) {
        return new BookStudyInputMessage(buffer.readEnum(Action.class),
                buffer.readEnum(InteractionHand.class));
    }
    public enum Action { BEGIN, HEARTBEAT, RELEASE }
}
```

Raise `StatModRuntime.NETWORK_PROTOCOL` from `"2"` to `"3"`, then register message ID `2` as `PLAY_TO_SERVER`. This prevents an old client with the same mod version from silently joining without the new C2S handler. Update `StatModRuntimeTest` to require protocol `3`. In the packet handler, reject a null sender, enqueue on the server thread, then call the Task 4 session input method. `ClientBookStudyInput.tick(Minecraft)` chooses main hand first, then offhand, only for `Items.ENCHANTED_BOOK`; it emits begin once, a heartbeat every five client ticks, and release when the key, hand, item, world, player, or active screen state stops matching.

- [ ] **Step 4: Run network and existing client tests**

```powershell
.\gradlew.bat test --tests "tong.statmod.network.*" --tests "tong.statmod.client.*" --console=plain
```

Expected: `BUILD SUCCESSFUL`; snapshot and notification payload IDs remain unchanged.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/network src/main/java/tong/statmod/client src/main/java/tong/statmod/StatModRuntime.java src/test/java/tong/statmod/network src/test/java/tong/statmod/client src/test/java/tong/statmod/StatModRuntimeTest.java
git commit -m "feat: report enchanted book study input"
```

---

### Task 4: Authoritative enchanted-book study sessions

**Files:**
- Create: `src/main/java/tong/statmod/event/EnchantedBookStudySessions.java`
- Create: `src/main/java/tong/statmod/event/BookStudyTimeline.java`
- Create: `src/test/java/tong/statmod/event/EnchantedBookStudySessionsContractTest.java`
- Create: `src/test/java/tong/statmod/event/BookStudyTimelineTest.java`
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`

**Interfaces:**
- Consumes: Task 3 input messages and Task 1 calculator.
- Calls: `XpAwardService.awardBookStudy(player, action, tick, consumeCallback)`.
- Produces: `input`, `tick`, and `clear(UUID)` session lifecycle methods.

- [ ] **Step 1: Write failing rules and lifecycle tests**

`BookStudyTimelineTest` asserts completion at 40 ticks, heartbeat timeout after 8 ticks, and no completion before tick 40. Contract tests assert rarity mapping `COMMON=1`, `UNCOMMON=2`, `RARE=4`, `VERY_RARE=8`, server `PlayerTickEvent` phase END, `Items.ENCHANTED_BOOK`, `EnchantedBookItem.getEnchantments`, `EnchantmentHelper.deserializeEnchantments`, `ItemStack.isSameItemSameTags`, callback `shrink(1)` only outside Creative, and entity event byte `35` only after `awardBookStudy` succeeds.

- [ ] **Step 2: Run tests and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.event.BookStudyTimelineTest" --tests "tong.statmod.event.EnchantedBookStudySessionsContractTest" --console=plain
```

Expected: compilation/contract failure because the session implementation is absent.

- [ ] **Step 3: Implement session validation and completion**

Use constants:

```java
public final class BookStudyTimeline {
    public static final long STUDY_TICKS = 40L;
    public static final long HEARTBEAT_TIMEOUT_TICKS = 8L;
    private BookStudyTimeline() {}
    public static boolean complete(long startTick, long now) {
        return now >= startTick && now - startTick >= STUDY_TICKS;
    }
    public static boolean timedOut(long heartbeatTick, long now) {
        return now < heartbeatTick || now - heartbeatTick > HEARTBEAT_TIMEOUT_TICKS;
    }
}

// EnchantedBookStudySessions
private static final byte TOTEM_EVENT = 35;
```

On `BEGIN`, accept only a live non-spectator server player holding an enchanted book in the declared hand. Store UUID, hand, `stack.copyWithCount(1)`, start tick, and heartbeat tick. `HEARTBEAT` updates only an existing same-hand session and is ignored more than once per server tick. `RELEASE` removes the session.

On player tick END, remove the session if the player died, changed hand content/tags, timed out, or no longer holds the enchanted book. At 40 ticks, deserialize stored enchantments, map each enchantment rarity explicitly to 1/2/4/8, call `EnchantmentStudyXp.calculate`, and invoke:

```java
boolean awarded = XpAwardService.awardBookStudy(player,
        XpAction.bookStudied(rawXp), tick,
        () -> {
            if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
        });
if (awarded) player.serverLevel().broadcastEntityEvent(player, TOTEM_EVENT);
```

Remove the session before invoking the transaction so duplicate ticks cannot complete it twice. Clear on logout and clone/respawn lifecycle through `PlayerStatsEvents`.

- [ ] **Step 4: Run study, service, and progression tests**

```powershell
.\gradlew.bat test --tests "tong.statmod.event.EnchantedBookStudy*" --tests "tong.statmod.progression.xp.*" --console=plain
```

Expected: `BUILD SUCCESSFUL`; interrupted and capped studies consume nothing and play no effect.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/event/EnchantedBookStudySessions.java src/main/java/tong/statmod/event/BookStudyTimeline.java src/main/java/tong/statmod/event/PlayerStatsEvents.java src/test/java/tong/statmod/event/EnchantedBookStudySessionsContractTest.java src/test/java/tong/statmod/event/BookStudyTimelineTest.java
git commit -m "feat: study enchanted books for Erudition"
```

---

### Task 5: Iron inscription and received spell-damage XP

**Files:**
- Modify: `src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java`
- Modify: `src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java`

**Interfaces:**
- Consumes: `InscribeSpellEvent.getSpellData()`, `SpellData.getLevel()`, `SpellData.getRarity().getValue()`.
- Consumes: Forge `LivingDamageEvent` whose source is `SpellDamageSource`.
- Calls: general `XpAwardService.award`, preserving Creative rejection.

- [ ] **Step 1: Write failing Iron adapter contracts**

Assert inscription uses `EventPriority.LOWEST`, checks `event.isCanceled()`, validates `ServerPlayer`, non-null `SpellData`, positive level, and sends exactly one `spellInscribed` action. Assert damage uses `LivingDamageEvent`, requires `SpellDamageSource`, reads `event.getAmount()`, sends `magicDamageReceived`, optionally attaches the attacking `ServerPlayer` UUID, and does not subscribe to mutable `SpellDamageEvent`, require a kill, or inspect spell school.

- [ ] **Step 2: Run the contract test and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.integration.ironspells.IronSpellXpEventsContractTest" --console=plain
```

Expected: FAIL because inscription and damage handlers are missing.

- [ ] **Step 3: Implement the two event translations**

```java
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void onSpellInscribed(InscribeSpellEvent event) {
    SpellData data = event.getSpellData();
    if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
            || data == null || data.getSpell() == null || data.getRarity() == null
            || data.getLevel() <= 0) return;
    XpAwardService.award(player, List.of(XpAction.spellInscribed(
            data.getLevel(), data.getRarity().getValue())),
            player.serverLevel().getGameTime());
}

@SubscribeEvent
public static void onSpellDamageReceived(LivingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)
            || !(event.getSource() instanceof SpellDamageSource)
            || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0F) return;
    XpAction action = XpAction.magicDamageReceived(event.getAmount());
    if (event.getSource().getEntity() instanceof ServerPlayer attacker
            && attacker != player) action = action.withOpponent(attacker.getUUID());
    XpAwardService.award(player, List.of(action), player.serverLevel().getGameTime());
}
```

- [ ] **Step 4: Run Iron, reward, and eligibility regressions**

```powershell
.\gradlew.bat test --tests "tong.statmod.integration.ironspells.IronSpellXpEventsContractTest" --tests "tong.statmod.progression.xp.XpRewardPolicyTest" --tests "tong.statmod.progression.xp.XpAwardServiceContractTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`; casting XP remains committed-event based and Creative-only exceptions do not spread.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java
git commit -m "feat: complete Iron magic stat XP"
```

---

### Task 6: Player-facing documentation and full deployment gate

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-07-15-statmod-forge-1.20.1-master-remake-design.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`

**Interfaces:**
- Produces accurate UI descriptions and a completed Milestone 3 record.

- [ ] **Step 1: Write the failing language assertions**

Require the French Erudition description to mention enchanted books and spell inscription, the English description to mention enchanted-book study and inscription, and Magic Resistance descriptions to mention received spell damage.

- [ ] **Step 2: Run language tests and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.client.stats.StatsLanguageResourcesTest" --console=plain
```

Expected: FAIL because the current Erudition description says the system is awaiting implementation.

- [ ] **Step 3: Update localized descriptions and milestone records**

Use these exact descriptions:

```json
"stat.statmod.erudition.description": "Progresse en étudiant des livres enchantés et en inscrivant de nouveaux sorts.",
"stat.statmod.magic_resistance.description": "Progresse en survivant aux dégâts réellement infligés par des sorts."
```

```json
"stat.statmod.erudition.description": "Progresses by studying enchanted books and inscribing new spells.",
"stat.statmod.magic_resistance.description": "Progresses by enduring damage actually dealt by spells."
```

Record the 40-tick study, rarity formula, 160-book cap, 200/minute rolling cap, Creative retention, Totem effect, inscription event, and final `LivingDamageEvent` source check. Mark Milestone 3 complete; leave Milestone 4 unchanged.

- [ ] **Step 4: Run the full clean verification**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both invocations print `BUILD SUCCESSFUL`, with zero JUnit failures/errors.

- [ ] **Step 5: Inspect the production JAR**

```powershell
$jar='build/libs/statmod-0.1.0+1.20.1.jar'
$entries = & jar tf $jar
@($entries | Where-Object { $_ -eq 'tong/statmod/event/EnchantedBookStudySessions.class' }).Count
@($entries | Where-Object { $_ -like 'io/redspace/ironsspellbooks/*' }).Count
```

Expected: study class count `1`; bundled Iron classes count `0`.

- [ ] **Step 6: Run required-provider Forge smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke` and exit code 0.

- [ ] **Step 7: Deploy only STAT Mod with backup and hash verification**

Confirm no active Minecraft `java/javaw` process whose command line contains ModLauncher or the Minecraft client. Back up the existing target under:

```text
C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\<timestamp>-before-erudition-magic-resistance-xp\
```

Copy `build/libs/statmod-0.1.0+1.20.1.jar` to `test-vrai/mods/statmod-0.1.0+1.20.1.jar`, require exactly one `statmod-*.jar`, and require identical SHA-256 hashes. Preserve every other client JAR.

- [ ] **Step 8: Commit documentation and record final state**

```powershell
git add README.md src/main/resources/assets/statmod/lang docs/compatibility/forge-1.20.1-supported-runtime.md docs/superpowers/specs/2026-07-15-statmod-forge-1.20.1-master-remake-design.md src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java
git commit -m "docs: complete Forge magic XP milestone"
git status --short
git log -8 --oneline --decorate
```

Expected: clean status; local `forge-1.20.1` branch and worktree remain preserved without push or merge.

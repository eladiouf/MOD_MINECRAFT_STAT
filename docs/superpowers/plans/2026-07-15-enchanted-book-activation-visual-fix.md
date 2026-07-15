# Enchanted Book Activation Visual Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the incorrect Totem item in the enchanted-book study completion animation with the exact studied enchanted book while retaining the Totem sound.

**Architecture:** A validated S2C payload carries a count-one enchanted-book snapshot to a client-only effect handler that invokes vanilla `GameRenderer.displayItemActivation`. The server sends it and plays `TOTEM_USE` only after the existing XP transaction succeeds; entity event `35` is removed.

**Tech Stack:** Java 17, Minecraft 1.20.1 official mappings, Forge 47.4.10 SimpleChannel, JUnit 5.10.2, Gradle 8.8.

## Global Constraints

- Work only in the existing isolated `forge-1.20.1` worktree.
- Preserve the XP formula, 40-tick duration, anti-farm cap, consumption, and Creative behavior.
- The animation must render `Items.ENCHANTED_BOOK`, never a Totem.
- Retain `SoundEvents.TOTEM_USE` with `SoundSource.PLAYERS`.
- Do not change real Totem behavior and do not add a mixin or custom renderer.
- Keep client classes behind the existing `DistExecutor` boundary.
- Raise `StatModRuntime.NETWORK_PROTOCOL` from `"3"` to `"4"`.
- Follow RED, GREEN, refactor, focused commit, then full build/smoke/deploy.

---

### Task 1: Book activation S2C payload and client effect

**Files:**
- Create: `src/main/java/tong/statmod/network/BookStudyCompletionMessage.java`
- Create: `src/test/java/tong/statmod/network/BookStudyCompletionMessageTest.java`
- Create: `src/main/java/tong/statmod/client/ClientBookStudyEffects.java`
- Create: `src/test/java/tong/statmod/client/ClientBookStudyEffectsContractTest.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/network/StatProgressNoticeMessageTest.java`

**Interfaces:**
- Produces: `BookStudyCompletionMessage(ItemStack book)`, `valid()`, `encode`, and `decode`.
- Produces: `ClientBookStudyEffects.show(BookStudyCompletionMessage)`.
- Produces: `StatNetwork.sendBookStudyCompletion(ServerPlayer, ItemStack)`.

- [ ] **Step 1: Write failing payload and client contract tests**

```java
@Test void roundTripsExactEnchantedBookAndNormalizesCount() {
    ItemStack book = EnchantedBookItem.createForEnchantment(
            new EnchantmentInstance(Enchantments.SHARPNESS, 4));
    book.setCount(7);
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    BookStudyCompletionMessage.encode(new BookStudyCompletionMessage(book), buffer);
    BookStudyCompletionMessage decoded = BookStudyCompletionMessage.decode(buffer);
    assertTrue(decoded.valid());
    assertEquals(1, decoded.book().getCount());
    assertTrue(ItemStack.isSameItemSameTags(book, decoded.book()));
}

@Test void rejectsEmptyAndNonBookStacks() {
    assertFalse(new BookStudyCompletionMessage(ItemStack.EMPTY).valid());
    assertFalse(new BookStudyCompletionMessage(new ItemStack(Items.TOTEM_OF_UNDYING)).valid());
}
```

The client contract requires `message.valid()`,
`Minecraft.getInstance().gameRenderer.displayItemActivation(message.book())`, and forbids `TOTEM_OF_UNDYING`, event byte `35`, and `broadcastEntityEvent`.

- [ ] **Step 2: Run tests and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.network.BookStudyCompletionMessageTest" --tests "tong.statmod.client.ClientBookStudyEffectsContractTest" --console=plain
```

Expected: compilation fails because the payload and client effect do not exist.

- [ ] **Step 3: Implement the payload and client effect**

```java
public record BookStudyCompletionMessage(ItemStack book) {
    public BookStudyCompletionMessage {
        book = book == null || book.isEmpty() ? ItemStack.EMPTY : book.copyWithCount(1);
    }
    public boolean valid() { return book.is(Items.ENCHANTED_BOOK); }
    public static void encode(BookStudyCompletionMessage message, FriendlyByteBuf buffer) {
        buffer.writeItem(message.book);
    }
    public static BookStudyCompletionMessage decode(FriendlyByteBuf buffer) {
        return new BookStudyCompletionMessage(buffer.readItem());
    }
}
```

```java
public final class ClientBookStudyEffects {
    private ClientBookStudyEffects() {}
    public static void show(BookStudyCompletionMessage message) {
        if (message != null && message.valid()) {
            Minecraft.getInstance().gameRenderer.displayItemActivation(message.book());
        }
    }
}
```

- [ ] **Step 4: Register message ID 3 and bump the protocol**

Set `NETWORK_PROTOCOL = "4"`. Register `BookStudyCompletionMessage` with ID `3`, direction `PLAY_TO_CLIENT`, `context.enqueueWork`, and `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBookStudyEffects.show(message))`. Add:

```java
public static void sendBookStudyCompletion(ServerPlayer player, ItemStack book) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new BookStudyCompletionMessage(book));
}
```

Update both protocol assertions to `"4"`.

- [ ] **Step 5: Run focused network/client regressions**

```powershell
.\gradlew.bat test --tests "tong.statmod.network.*" --tests "tong.statmod.client.ClientBookStudyEffectsContractTest" --tests "tong.statmod.StatModRuntimeTest" --console=plain
```

Expected: `BUILD SUCCESSFUL`; the book round-trips with count one and invalid stacks are rejected.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/tong/statmod/network src/main/java/tong/statmod/client/ClientBookStudyEffects.java src/main/java/tong/statmod/StatModRuntime.java src/test/java/tong/statmod/network src/test/java/tong/statmod/client/ClientBookStudyEffectsContractTest.java src/test/java/tong/statmod/StatModRuntimeTest.java
git commit -m "feat: animate studied enchanted book"
```

---

### Task 2: Replace entity event 35 with the book visual and Totem sound

**Files:**
- Modify: `src/main/java/tong/statmod/event/EnchantedBookStudySessions.java`
- Modify: `src/test/java/tong/statmod/event/EnchantedBookStudySessionsContractTest.java`
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `docs/superpowers/specs/2026-07-15-forge-erudition-magic-resistance-xp-design.md`

**Interfaces:**
- Consumes: `StatNetwork.sendBookStudyCompletion(ServerPlayer, ItemStack)`.
- Preserves: `XpAwardService.awardBookStudy` transaction and existing book snapshot.

- [ ] **Step 1: Change the server contract test first**

Require the study handler to copy `held.copyWithCount(1)` before the award, call `sendBookStudyCompletion(player, visualBook)` only inside `if (awarded)`, and play `SoundEvents.TOTEM_USE` through `SoundSource.PLAYERS`. Forbid `TOTEM_EVENT`, byte `35`, and `broadcastEntityEvent`.

- [ ] **Step 2: Run the contract and observe RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.event.EnchantedBookStudySessionsContractTest" --console=plain
```

Expected: FAIL because the current handler still broadcasts event `35`.

- [ ] **Step 3: Implement the server-side visual trigger**

Immediately before `awardBookStudy`, create:

```java
ItemStack visualBook = held.copyWithCount(1);
```

Replace the successful branch with:

```java
if (awarded) {
    StatNetwork.sendBookStudyCompletion(player, visualBook);
    player.serverLevel().playSound(null,
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
}
```

Delete `TOTEM_EVENT` and every call to `broadcastEntityEvent`.

- [ ] **Step 4: Update documentation and run focused tests**

State explicitly that the vanilla activation motion renders the studied book while retaining the Totem sound. Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.event.EnchantedBookStudySessionsContractTest" --tests "tong.statmod.network.BookStudyCompletionMessageTest" --tests "tong.statmod.progression.xp.*" --console=plain
```

Expected: `BUILD SUCCESSFUL`; XP, consumption, and limits are unchanged.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/event/EnchantedBookStudySessions.java src/test/java/tong/statmod/event/EnchantedBookStudySessionsContractTest.java README.md docs/compatibility/forge-1.20.1-supported-runtime.md docs/superpowers/specs/2026-07-15-forge-erudition-magic-resistance-xp-design.md
git commit -m "fix: render book in study activation"
```

---

### Task 3: Full verification, smoke, and deployment

**Files:**
- Verify: `build/libs/statmod-0.1.0+1.20.1.jar`
- Deploy: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods\statmod-0.1.0+1.20.1.jar`

- [ ] **Step 1: Run the complete clean suite twice**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both commands print `BUILD SUCCESSFUL`, zero failures and zero errors.

- [ ] **Step 2: Inspect the JAR**

Require exactly one `BookStudyCompletionMessage.class`, one `ClientBookStudyEffects.class`, one `EnchantedBookStudySessions.class`, and zero `io/redspace/ironsspellbooks/*` entries. Record SHA-256.

- [ ] **Step 3: Run the required-provider Forge smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`.

- [ ] **Step 4: Deploy safely**

Require no active Minecraft ModLauncher process. Back up the old target under `statmod-backups/test-vrai/<timestamp>-before-enchanted-book-visual-fix`, replace only Stat Mod, require one `statmod-*.jar`, preserve all other JARs, and verify source/target SHA-256 equality.

- [ ] **Step 5: Record final Git state**

Run `git status --short` and `git log -8 --oneline --decorate`. Expected: clean `forge-1.20.1` worktree preserved locally without merge or push.

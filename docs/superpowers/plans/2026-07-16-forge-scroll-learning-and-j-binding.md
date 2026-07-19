# Forge Scroll Learning and J-Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every Iron's-compatible scroll teach its contained spell instead of casting it, then expose learned spells through a secure searchable binding interface opened with `J`.

**Architecture:** Persist a bounded `spell id -> highest learned level` map inside the existing player capability, synchronize it through the authoritative stats snapshot, and intercept `IScroll` right-clicks before Iron's `Scroll.use`. Reuse Iron's inscription menu and screen through a virtual menu plus two narrow mixins; all learning and binding mutations are revalidated server-side through Iron's public registry and spell-container APIs.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Iron's Spells 'n Spellbooks 3.16.2, Forge SimpleChannel, Sponge Mixin bundled by Forge, JUnit 5.10.2, Gradle/ForgeGradle 6.

## Global Constraints

- Work only in the isolated `forge-1.20.1-combat-perks` worktree and preserve unrelated dungeon work.
- Iron's Spells, Epic Fight, Puffish Skills, and Lootr remain required runtime providers.
- Recognize scrolls through `io.redspace.ironsspellbooks.api.item.IScroll`, including addon implementations.
- A scroll never directly casts; only a successful new learn or level upgrade consumes one outside Creative mode.
- Learned spell capacity is 512 entries, identifiers are at most 128 UTF-8 characters, and learned levels are clamped to the live spell's `[getMinLevel(), getMaxLevel()]` range.
- The server is authoritative for learning, levels, menu identity, spellbook compatibility, target slots, and binding.
- `J` opens the virtual binding menu; the left panel also appears on physical Iron's inscription tables.
- Search and school filters must discover addon namespaces and schools from live registry data.
- Do not add affinities, a magic tree, Tensura systems, automatic Curios binding, scroll-loot changes, spell balance changes, or Erudition rewards.
- End with `clean test build`, required-provider Forge GameTest smoke, JAR content checks, and deployment to `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods`.

## File Structure

### Persistent state and learning

- `src/main/java/tong/statmod/magic/LearnedSpellState.java` — bounded canonical learned-level map and NBT codec.
- `src/main/java/tong/statmod/magic/ScrollLearningService.java` — pure learn/upgrade/duplicate decision.
- `src/main/java/tong/statmod/integration/ironspells/IronScrollDescriptor.java` — adapter from `IScroll` stack to canonical spell data.
- `src/main/java/tong/statmod/integration/ironspells/IronScrollLearningEvents.java` — Forge right-click cancellation, mutation, consumption, feedback, and sync.
- `src/main/java/tong/statmod/stats/PlayerStats.java` — owns one `LearnedSpellState` and includes it in save/copy.

### Synchronization and menu opening

- `src/main/java/tong/statmod/network/LearnedSpellEntry.java` — bounded wire value.
- `src/main/java/tong/statmod/network/OpenSpellBindingMessage.java` — throttled server request to open `J` menu.
- `src/main/java/tong/statmod/network/SpellBindingRequestThrottle.java` — pure per-player request policy.
- `src/main/java/tong/statmod/network/StatsSnapshotMessage.java` — carries learned entries.
- `src/main/java/tong/statmod/network/StatNetwork.java` — protocol registrations and send helpers.
- `src/main/java/tong/statmod/client/ClientStatsState.java` and `ClientStatsCache.java` — immutable client mirror.
- `src/main/java/tong/statmod/client/ClientKeyMappings.java` and `ClientInputEvents.java` — `J` input.

### Iron's binding integration

- `src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionTableMenu.java` — native menu without physical block validity.
- `src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuProvider.java` — safe server menu provider.
- `src/main/java/tong/statmod/integration/ironspells/IronKnownSpellIndex.java` — deterministic learned list, search/filter/page rules.
- `src/main/java/tong/statmod/integration/ironspells/IronLearnedSpellBindingService.java` — final server validation and container mutation.
- `src/main/java/tong/statmod/client/inscription/KnownSpellIconButton.java` — spell icon, bound state, and tooltip.
- `src/main/java/tong/statmod/mixin/IronInscriptionTableMenuMixin.java` — selection/button bridge.
- `src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java` — left-side learned spell UI.
- `src/main/resources/statmod.mixins.json` and `META-INF/MANIFEST.MF` generation — Mixin registration.
- `src/main/resources/assets/statmod/lang/en_us.json` and `fr_fr.json` — UI and learning messages.

---

### Task 1: Persistent learned-spell state

**Files:**
- Create: `src/main/java/tong/statmod/magic/LearnedSpellState.java`
- Modify: `src/main/java/tong/statmod/stats/PlayerStats.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Test: `src/test/java/tong/statmod/magic/LearnedSpellStateTest.java`
- Test: `src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java`

**Interfaces:**
- Produces: `LearnedSpellState.LearnResult`, `learn(String,int)`, `level(String)`, `snapshot()`, `save()`, `load(CompoundTag)`, and `copyFrom(LearnedSpellState)`.
- Produces: `PlayerStats.learnedSpells()` returning the owned mutable server state.

- [ ] **Step 1: Write failing state-policy tests**

```java
class LearnedSpellStateTest {
    @Test void highestLevelWinsAndDuplicatesDoNotMutate() {
        LearnedSpellState state = new LearnedSpellState();
        assertEquals(LearnedSpellState.LearnResult.NEW, state.learn("irons_spellbooks:fireball", 2));
        assertEquals(LearnedSpellState.LearnResult.DUPLICATE, state.learn("irons_spellbooks:fireball", 2));
        assertEquals(LearnedSpellState.LearnResult.DUPLICATE, state.learn("irons_spellbooks:fireball", 1));
        assertEquals(LearnedSpellState.LearnResult.UPGRADED, state.learn("irons_spellbooks:fireball", 4));
        assertEquals(4, state.level("irons_spellbooks:fireball"));
    }

    @Test void rejectsInvalidIdsLevelsAndCapacityOverflow() {
        LearnedSpellState state = new LearnedSpellState();
        assertEquals(LearnedSpellState.LearnResult.INVALID, state.learn("", 1));
        assertEquals(LearnedSpellState.LearnResult.INVALID, state.learn("bad id", 1));
        assertEquals(LearnedSpellState.LearnResult.INVALID, state.learn("irons_spellbooks:fireball", 0));
        for (int i = 0; i < LearnedSpellState.MAX_ENTRIES; i++) {
            assertEquals(LearnedSpellState.LearnResult.NEW, state.learn("addon:spell_" + i, 1));
        }
        assertEquals(LearnedSpellState.LearnResult.FULL, state.learn("addon:overflow", 1));
    }
}
```

- [ ] **Step 2: Run the focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.magic.LearnedSpellStateTest`

Expected: compilation fails because `LearnedSpellState` does not exist.

- [ ] **Step 3: Add the bounded canonical state**

```java
public final class LearnedSpellState {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_ID_LENGTH = 128;
    private static final String LIST_KEY = "learnedSpells";
    private final LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();

    public enum LearnResult { NEW, UPGRADED, DUPLICATE, FULL, INVALID }

    public LearnResult learn(String rawId, int level) {
        ResourceLocation id = ResourceLocation.tryParse(rawId == null ? "" : rawId);
        if (id == null || rawId.length() > MAX_ID_LENGTH || level <= 0) return LearnResult.INVALID;
        String canonical = id.toString();
        Integer old = levels.get(canonical);
        if (old != null && old >= level) return LearnResult.DUPLICATE;
        if (old == null && levels.size() >= MAX_ENTRIES) return LearnResult.FULL;
        levels.put(canonical, level);
        return old == null ? LearnResult.NEW : LearnResult.UPGRADED;
    }

    public int level(String id) { return levels.getOrDefault(id, 0); }
    public Map<String, Integer> snapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(levels)); }
    public void copyFrom(LearnedSpellState source) { levels.clear(); source.levels.forEach(this::learn); }

    public ListTag save() {
        ListTag list = new ListTag();
        levels.forEach((id, level) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putInt("level", level);
            list.add(entry);
        });
        return list;
    }

    public void load(CompoundTag root) {
        levels.clear();
        ListTag list = root.getList(LIST_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && levels.size() < MAX_ENTRIES; i++) {
            CompoundTag entry = list.getCompound(i);
            learn(entry.getString("id"), entry.getInt("level"));
        }
    }
}
```

Embed this state in `PlayerStats`, write `root.put("learnedSpells", learnedSpells.save())`, load it from the root, copy it in `copyFrom`, expose `learnedSpells()`, and increment `PLAYER_STATS_SCHEMA` from `2` to `3`.

- [ ] **Step 4: Add NBT/copy regression tests and run GREEN**

```java
@Test void learnedSpellsRoundTripAndCopyWithoutAliasing() {
    PlayerStats source = new PlayerStats();
    source.learnedSpells().learn("irons_spellbooks:fireball", 4);
    PlayerStats restored = new PlayerStats();
    restored.deserializeNbt(source.serializeNbt());
    assertEquals(4, restored.learnedSpells().level("irons_spellbooks:fireball"));
    PlayerStats copy = new PlayerStats();
    copy.copyFrom(restored);
    restored.learnedSpells().learn("addon:wind_blade", 2);
    assertEquals(0, copy.learnedSpells().level("addon:wind_blade"));
}
```

Run: `.\gradlew.bat test --tests tong.statmod.magic.LearnedSpellStateTest --tests tong.statmod.stats.PlayerStatsNbtTest`

Expected: all focused tests pass.

- [ ] **Step 5: Commit persistent state**

```powershell
git add src/main/java/tong/statmod/magic/LearnedSpellState.java src/main/java/tong/statmod/stats/PlayerStats.java src/main/java/tong/statmod/StatModRuntime.java src/test/java/tong/statmod/magic/LearnedSpellStateTest.java src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java
git commit -m "feat(magic): persist learned spell levels"
```

### Task 2: Learned-spell snapshot synchronization

**Files:**
- Create: `src/main/java/tong/statmod/network/LearnedSpellEntry.java`
- Modify: `src/main/java/tong/statmod/network/StatsSnapshotMessage.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsState.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsCache.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Test: `src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java`
- Test: `src/test/java/tong/statmod/client/ClientStatsCacheTest.java`

**Interfaces:**
- Consumes: `PlayerStats.learnedSpells().snapshot()`.
- Produces: `LearnedSpellEntry(String id, int level)` and `ClientStatsState.learnedSpells()`.

- [ ] **Step 1: Write failing packet and immutable-cache tests**

```java
@Test void packetRoundTripsBoundedLearnedEntries() {
    StatsSnapshotMessage original = fixture(List.of(
            new LearnedSpellEntry("irons_spellbooks:fireball", 4),
            new LearnedSpellEntry("addon:wind_blade", 2)));
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    StatsSnapshotMessage.encode(original, buffer);
    assertEquals(original.learnedSpells(), StatsSnapshotMessage.decode(buffer).learnedSpells());
}

@Test void clientCacheCopiesLearnedMap() {
    Map<String, Integer> mutable = new HashMap<>(Map.of("addon:wind_blade", 2));
    ClientStatsCache.replace(values(), List.of(), 0, 1, mutable);
    mutable.put("addon:changed", 9);
    assertEquals(Map.of("addon:wind_blade", 2), ClientStatsCache.state().learnedSpells());
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.network.StatsSnapshotMessageTest --tests tong.statmod.client.ClientStatsCacheTest`

Expected: compilation fails on missing learned-spell fields/signatures.

- [ ] **Step 3: Extend the wire model with explicit bounds**

```java
public record LearnedSpellEntry(String id, int level) {
    public LearnedSpellEntry {
        if (id == null || id.isBlank() || id.length() > LearnedSpellState.MAX_ID_LENGTH || level <= 0) {
            throw new IllegalArgumentException("invalid learned spell entry");
        }
    }
}
```

Append learned entries after dungeon fields in `StatsSnapshotMessage`:

```java
buffer.writeVarInt(message.learnedSpells().size());
for (LearnedSpellEntry entry : message.learnedSpells()) {
    buffer.writeUtf(entry.id(), LearnedSpellState.MAX_ID_LENGTH);
    buffer.writeVarInt(entry.level());
}
```

Reject counts outside `0..512` during decode, convert `PlayerStats` snapshots to sorted entries, extend the immutable client record/cache, pass the decoded map in packet handler `0`, and increment `NETWORK_PROTOCOL` from `8` to `9`.

- [ ] **Step 4: Verify bounds, sync lifecycle, and GREEN**

Add tests asserting count `513` throws, duplicate IDs keep the highest level, `ClientStatsCache.clear()` removes learned spells, and `StatsSnapshotMessage.from(stats)` sorts IDs.

Run: `.\gradlew.bat test --tests tong.statmod.network.StatsSnapshotMessageTest --tests tong.statmod.client.ClientStatsCacheTest --tests tong.statmod.event.PlayerStatsWiringTest`

Expected: all focused tests pass.

- [ ] **Step 5: Commit synchronization**

```powershell
git add src/main/java/tong/statmod/network/LearnedSpellEntry.java src/main/java/tong/statmod/network/StatsSnapshotMessage.java src/main/java/tong/statmod/network/StatNetwork.java src/main/java/tong/statmod/client/ClientStatsState.java src/main/java/tong/statmod/client/ClientStatsCache.java src/main/java/tong/statmod/StatModRuntime.java src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java src/test/java/tong/statmod/client/ClientStatsCacheTest.java
git commit -m "feat(network): sync learned spell levels"
```

### Task 3: Scroll descriptor and pure learning decision

**Files:**
- Create: `src/main/java/tong/statmod/magic/ScrollLearningService.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/IronScrollDescriptor.java`
- Test: `src/test/java/tong/statmod/magic/ScrollLearningServiceTest.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronScrollDescriptorContractTest.java`

**Interfaces:**
- Consumes: `LearnedSpellState.learn(String,int)`.
- Produces: `ScrollLearningService.Outcome(status,id,oldLevel,newLevel,consume)`.
- Produces: `IronScrollDescriptor.describe(ItemStack)` returning `Optional<Descriptor>`.

- [ ] **Step 1: Write failing pure decision tests**

```java
@Test void newAndUpgradeConsumeButDuplicatesDoNot() {
    LearnedSpellState state = new LearnedSpellState();
    Outcome first = ScrollLearningService.apply(state, "irons_spellbooks:fireball", 2, 1, 5);
    assertEquals(Status.LEARNED, first.status());
    assertTrue(first.consume());
    Outcome lower = ScrollLearningService.apply(state, "irons_spellbooks:fireball", 1, 1, 5);
    assertEquals(Status.ALREADY_KNOWN, lower.status());
    assertFalse(lower.consume());
    Outcome higher = ScrollLearningService.apply(state, "irons_spellbooks:fireball", 99, 1, 5);
    assertEquals(Status.UPGRADED, higher.status());
    assertEquals(5, higher.newLevel());
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.magic.ScrollLearningServiceTest --tests tong.statmod.integration.ironspells.IronScrollDescriptorContractTest`

Expected: compilation fails on missing services.

- [ ] **Step 3: Add pure clamp/outcome logic**

```java
public static Outcome apply(LearnedSpellState state, String id, int rawLevel, int min, int max) {
    if (state == null || id == null || min <= 0 || max < min) return Outcome.invalid();
    int level = Mth.clamp(rawLevel, min, max);
    int old = state.level(id);
    LearnedSpellState.LearnResult result = state.learn(id, level);
    return switch (result) {
        case NEW -> new Outcome(Status.LEARNED, id, old, level, true);
        case UPGRADED -> new Outcome(Status.UPGRADED, id, old, level, true);
        case DUPLICATE -> new Outcome(Status.ALREADY_KNOWN, id, old, old, false);
        case FULL -> new Outcome(Status.FULL, id, old, old, false);
        case INVALID -> Outcome.invalid();
    };
}
```

Adapt Iron's stack without namespace assumptions:

```java
public static Optional<Descriptor> describe(ItemStack stack) {
    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IScroll)) return Optional.empty();
    ISpellContainer container = ISpellContainer.get(stack);
    SpellData data = container == null ? SpellData.EMPTY : container.getSpellAtIndex(0);
    if (data == null || data == SpellData.EMPTY || data.getSpell() == null) return Optional.empty();
    AbstractSpell spell = data.getSpell();
    ResourceLocation id = SpellRegistry.REGISTRY.get().getKey(spell);
    if (id == null || spell == SpellRegistry.none()) return Optional.empty();
    return Optional.of(new Descriptor(id.toString(), data.getLevel(), spell.getMinLevel(), spell.getMaxLevel()));
}
```

- [ ] **Step 4: Verify adapter source contract and GREEN**

The contract test must assert the source uses `instanceof IScroll`, `ISpellContainer.get`, registry lookup, `getMinLevel`, and `getMaxLevel`, and contains no hard-coded addon namespaces.

Run: `.\gradlew.bat test --tests tong.statmod.magic.ScrollLearningServiceTest --tests tong.statmod.integration.ironspells.IronScrollDescriptorContractTest`

Expected: all focused tests pass.

- [ ] **Step 5: Commit learning policy**

```powershell
git add src/main/java/tong/statmod/magic/ScrollLearningService.java src/main/java/tong/statmod/integration/ironspells/IronScrollDescriptor.java src/test/java/tong/statmod/magic/ScrollLearningServiceTest.java src/test/java/tong/statmod/integration/ironspells/IronScrollDescriptorContractTest.java
git commit -m "feat(magic): define scroll learning policy"
```

### Task 4: Replace direct scroll casting at runtime

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronScrollLearningEvents.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronScrollLearningEventsContractTest.java`
- Test: `src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java`

**Interfaces:**
- Consumes: descriptor, learning service, player capability, and `StatNetwork.sendSnapshot`.
- Produces: high-priority `PlayerInteractEvent.RightClickItem` cancellation for every `IScroll`.

- [ ] **Step 1: Write the failing event contract**

```java
@Test void scrollUseIsAlwaysCanceledBeforeIronCastAndOnlyServerConsumes() throws Exception {
    String source = Files.readString(Path.of("src/main/java/tong/statmod/integration/ironspells/IronScrollLearningEvents.java"));
    assertTrue(source.contains("EventPriority.HIGHEST"));
    assertTrue(source.contains("instanceof IScroll"));
    assertTrue(source.contains("event.setCanceled(true)"));
    assertTrue(source.contains("if (event.getLevel().isClientSide())"));
    assertTrue(source.contains("outcome.consume() && !player.isCreative()"));
    assertTrue(source.contains("StatNetwork.sendSnapshot(player)"));
    assertFalse(source.contains("attemptInitiateCast"));
}
```

- [ ] **Step 2: Run the contract and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronScrollLearningEventsContractTest`

Expected: failure because the event class is absent.

- [ ] **Step 3: Add authoritative right-click handling**

```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void rightClickScroll(PlayerInteractEvent.RightClickItem event) {
    ItemStack stack = event.getItemStack();
    if (!(stack.getItem() instanceof IScroll)) return;
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.CONSUME);
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;

    Optional<IronScrollDescriptor.Descriptor> descriptor = IronScrollDescriptor.describe(stack);
    if (descriptor.isEmpty()) {
        player.displayClientMessage(Component.translatable("statmod.scroll.invalid"), true);
        return;
    }
    player.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(stats -> {
        var d = descriptor.orElseThrow();
        var outcome = ScrollLearningService.apply(stats.learnedSpells(), d.id(), d.level(), d.minLevel(), d.maxLevel());
        if (outcome.consume() && !player.isCreative()) stack.shrink(1);
        sendOutcomeMessage(player, outcome);
        if (outcome.consume()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
            StatNetwork.sendSnapshot(player);
        }
    });
}
```

Register through `@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)` and add exact English/French keys for learned, upgraded, already known, full library, and invalid scroll.

- [ ] **Step 4: Run event/language/regression tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronScrollLearningEventsContractTest --tests tong.statmod.client.stats.StatsLanguageResourcesTest --tests tong.statmod.integration.ironspells.IronSpellXpEventsContractTest`

Expected: all tests pass and the existing spell-cast XP bridge still excludes scroll casts.

- [ ] **Step 5: Commit direct-cast replacement**

```powershell
git add src/main/java/tong/statmod/integration/ironspells/IronScrollLearningEvents.java src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json src/test/java/tong/statmod/integration/ironspells/IronScrollLearningEventsContractTest.java src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java
git commit -m "feat(magic): make scrolls teach spells"
```

### Task 5: Learned-spell list, search, school filters, and paging

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronKnownSpellIndex.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronKnownSpellIndexTest.java`

**Interfaces:**
- Consumes: client/server learned spell maps and resolver functions for name/school/registry validity.
- Produces: `visible(...)`, `schools(...)`, `page(...)`, `maxPage(...)`, and stable `optionIndexOf(...)`.

- [ ] **Step 1: Write failing dynamic-filter tests**

```java
@Test void filtersNativeAndAddonSchoolsWithoutAllowlist() {
    Map<String, Integer> learned = Map.of(
            "irons_spellbooks:fireball", 3,
            "wind_spellbooks:air_blade", 2,
            "addon:void_shield", 1);
    List<Entry> visible = IronKnownSpellIndex.visible(learned, "blade", Set.of("wind"),
            id -> Map.of("wind_spellbooks:air_blade", "Air Blade").get(id),
            id -> Map.of("wind_spellbooks:air_blade", "wind").get(id), id -> true);
    assertEquals(List.of(new Entry("wind_spellbooks:air_blade", 2)), visible);
    assertEquals(Set.of("fire", "wind", "void"), IronKnownSpellIndex.schools(learned, schoolResolver(), id -> true));
}
```

- [ ] **Step 2: Run and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronKnownSpellIndexTest`

Expected: compilation fails because the index is absent.

- [ ] **Step 3: Add deterministic index rules**

```java
public static List<Entry> visible(Map<String, Integer> learned, String query, Set<String> schools,
        Function<String, String> name, Function<String, String> school, Predicate<String> registered) {
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    Set<String> selected = schools == null ? Set.of() : schools.stream()
            .map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    return learned.entrySet().stream()
            .filter(e -> registered.test(e.getKey()))
            .filter(e -> selected.isEmpty() || selected.contains(normalize(school.apply(e.getKey()))))
            .filter(e -> q.isEmpty() || e.getKey().toLowerCase(Locale.ROOT).contains(q)
                    || normalize(name.apply(e.getKey())).contains(q))
            .map(e -> new Entry(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing((Entry e) -> normalize(name.apply(e.id()))).thenComparing(Entry::id))
            .toList();
}
```

Use page size `6`, clamp pages, return immutable copies, and derive schools only from registered learned entries.

- [ ] **Step 4: Verify nulls, stable sorting, paging, and GREEN**

Add assertions for null resolvers, removed addon spells, duplicate display names, out-of-range pages, and immutable results.

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronKnownSpellIndexTest`

Expected: all tests pass.

- [ ] **Step 5: Commit UI model**

```powershell
git add src/main/java/tong/statmod/integration/ironspells/IronKnownSpellIndex.java src/test/java/tong/statmod/integration/ironspells/IronKnownSpellIndexTest.java
git commit -m "feat(magic): index learned spells for binding UI"
```

### Task 6: `J` key and secure virtual inscription menu

**Files:**
- Create: `src/main/java/tong/statmod/network/OpenSpellBindingMessage.java`
- Create: `src/main/java/tong/statmod/network/SpellBindingRequestThrottle.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionTableMenu.java`
- Create: `src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuProvider.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/client/ClientKeyMappings.java`
- Modify: `src/main/java/tong/statmod/client/ClientInputEvents.java`
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Modify: language JSON files
- Test: `src/test/java/tong/statmod/network/SpellBindingRequestThrottleTest.java`
- Test: `src/test/java/tong/statmod/client/SpellBindingKeyContractTest.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuContractTest.java`

**Interfaces:**
- Produces: `StatNetwork.sendOpenSpellBinding()` and server handler for message ID `7`.
- Produces: `SpellBindingRequestThrottle.allow(UUID,long)` with a 10-tick interval.

- [ ] **Step 1: Write failing throttle, key, and virtual-menu tests**

```java
@Test void throttleAllowsFirstThenWaitsTenTicks() {
    UUID id = UUID.randomUUID();
    assertTrue(SpellBindingRequestThrottle.allow(id, 100));
    assertFalse(SpellBindingRequestThrottle.allow(id, 109));
    assertTrue(SpellBindingRequestThrottle.allow(id, 110));
    SpellBindingRequestThrottle.clear(id);
    assertTrue(SpellBindingRequestThrottle.allow(id, 110));
}
```

The source contracts assert `GLFW_KEY_J`, one registration, `screen == null`, packet direction `PLAY_TO_SERVER`, and `VirtualInscriptionTableMenu.stillValid` returns `!player.isRemoved()`.

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.network.SpellBindingRequestThrottleTest --tests tong.statmod.client.SpellBindingKeyContractTest --tests tong.statmod.integration.ironspells.VirtualInscriptionMenuContractTest`

Expected: missing classes/key cause failure.

- [ ] **Step 3: Add key, packet, throttle, and provider**

```java
public static final KeyMapping OPEN_SPELL_BINDING = new KeyMapping(
        "key.statmod.open_spell_binding", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.statmod");
```

Consume it only with a live player and no open screen, then call `StatNetwork.sendOpenSpellBinding()`.

```java
public static void handle(OpenSpellBindingMessage ignored, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
        long tick = player.serverLevel().getGameTime();
        if (SpellBindingRequestThrottle.allow(player.getUUID(), tick)) {
            NetworkHooks.openScreen(player, new VirtualInscriptionMenuProvider());
        }
    });
    context.setPacketHandled(true);
}
```

Provider `createMenu` returns `new VirtualInscriptionTableMenu(id, inventory, ContainerLevelAccess.NULL)`. Clear throttle state on logout.

- [ ] **Step 4: Run focused tests and compile against Iron's 3.16.2**

Run: `.\gradlew.bat test --tests tong.statmod.network.SpellBindingRequestThrottleTest --tests tong.statmod.client.SpellBindingKeyContractTest --tests tong.statmod.integration.ironspells.VirtualInscriptionMenuContractTest`

Expected: all tests pass and main source compilation succeeds.

- [ ] **Step 5: Commit the virtual-menu entry point**

```powershell
git add src/main/java/tong/statmod/network/OpenSpellBindingMessage.java src/main/java/tong/statmod/network/SpellBindingRequestThrottle.java src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionTableMenu.java src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuProvider.java src/main/java/tong/statmod/network/StatNetwork.java src/main/java/tong/statmod/client/ClientKeyMappings.java src/main/java/tong/statmod/client/ClientInputEvents.java src/main/java/tong/statmod/event/PlayerStatsEvents.java src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json src/test/java/tong/statmod/network/SpellBindingRequestThrottleTest.java src/test/java/tong/statmod/client/SpellBindingKeyContractTest.java src/test/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuContractTest.java
git commit -m "feat(magic): open spell binding menu with J"
```

### Task 7: Server-authoritative learned spell binding

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronLearnedSpellBindingService.java`
- Create: `src/main/java/tong/statmod/mixin/IronInscriptionTableMenuMixin.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronLearnedSpellBindingPolicyTest.java`
- Test: `src/test/java/tong/statmod/mixin/IronInscriptionTableMenuMixinContractTest.java`

**Interfaces:**
- Consumes: selected global learned-spell option and server `PlayerStats`.
- Produces: reserved button IDs `1000..1511`, `select(...)`, and `bindSelected(...)`.

- [ ] **Step 1: Write failing binding-policy tests**

```java
@Test void selectionRequiresServerLearnedEntryAndUsesServerLevel() {
    Map<String, Integer> learned = Map.of("irons_spellbooks:fireball", 4);
    assertEquals(4, IronLearnedSpellBindingService.authorizedLevel(
            learned, "irons_spellbooks:fireball", 1, 1, 5));
    assertEquals(0, IronLearnedSpellBindingService.authorizedLevel(
            learned, "addon:not_learned", 5, 1, 5));
}

@Test void reservedButtonIdsRoundTripWithinBound() {
    assertEquals(0, IronLearnedSpellBindingService.optionFromButton(1000));
    assertEquals(511, IronLearnedSpellBindingService.optionFromButton(1511));
    assertEquals(-1, IronLearnedSpellBindingService.optionFromButton(1512));
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronLearnedSpellBindingPolicyTest --tests tong.statmod.mixin.IronInscriptionTableMenuMixinContractTest`

Expected: missing service and mixin cause failure.

- [ ] **Step 3: Add final server validation and Iron mutation**

```java
public static boolean bind(InscriptionTableMenu menu, Player player, String spellId, int targetSlot,
        Map<String, Integer> learned) {
    if (menu == null || player == null || targetSlot < 0) return false;
    AbstractSpell spell = SpellRegistry.getSpell(spellId);
    if (spell == null || spell == SpellRegistry.none()) return false;
    int level = authorizedLevel(learned, spellId, 0, spell.getMinLevel(), spell.getMaxLevel());
    if (level <= 0) return false;
    ItemStack book = menu.getSpellBookSlot().getItem();
    if (book.isEmpty() || !ISpellContainer.isSpellContainer(book)) return false;
    ISpellContainer current = ISpellContainer.get(book);
    if (targetSlot >= current.getMaxSpellCount() || current.getSpellAtIndex(targetSlot) != SpellData.EMPTY) return false;
    InscribeSpellEvent event = new InscribeSpellEvent(player, new SpellData(spell, level));
    if (MinecraftForge.EVENT_BUS.post(event)) return false;
    ISpellContainerMutable mutable = current.mutableCopy();
    if (!mutable.addSpellAtIndex(spell, level, targetSlot, false)) return false;
    ISpellContainer.set(book, mutable.toImmutable());
    menu.getSpellBookSlot().setChanged();
    menu.broadcastChanges();
    return true;
}
```

The menu mixin stores only the selected canonical ID. On button `1000+n`, resolve the server's deterministic full learned list and store the ID. On the dedicated bind button, use the shadowed `selectedSpellIndex`, reread the capability, and call `bind`; never trust a client level.

- [ ] **Step 4: Run policy/source tests and compilation**

Run: `.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronLearnedSpellBindingPolicyTest --tests tong.statmod.mixin.IronInscriptionTableMenuMixinContractTest`

Expected: tests pass; the mixin targets `clickMenuButton(Player,int)` and the Iron's 3.16.2 menu fields/methods.

- [ ] **Step 5: Commit secure binding**

```powershell
git add src/main/java/tong/statmod/integration/ironspells/IronLearnedSpellBindingService.java src/main/java/tong/statmod/mixin/IronInscriptionTableMenuMixin.java src/test/java/tong/statmod/integration/ironspells/IronLearnedSpellBindingPolicyTest.java src/test/java/tong/statmod/mixin/IronInscriptionTableMenuMixinContractTest.java
git commit -m "feat(magic): bind learned spells server-side"
```

### Task 8: Left-side inscription UI and Mixin registration

**Files:**
- Create: `src/main/java/tong/statmod/client/inscription/KnownSpellIconButton.java`
- Create: `src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java`
- Create: `src/main/resources/statmod.mixins.json`
- Modify: `build.gradle`
- Modify: language JSON files
- Test: `src/test/java/tong/statmod/client/inscription/KnownSpellIconButtonContractTest.java`
- Test: `src/test/java/tong/statmod/mixin/IronInscriptionTableScreenMixinContractTest.java`
- Test: `src/test/java/tong/statmod/MixinConfigurationContractTest.java`

**Interfaces:**
- Consumes: `ClientStatsCache.state().learnedSpells()` and `IronKnownSpellIndex`.
- Produces: six icon buttons, search field, dynamic multi-select school filters, pager, bound markers, and native inscription-button integration.

- [ ] **Step 1: Write failing screen and Mixin contracts**

```java
@Test void screenUsesLearnedCacheDynamicSchoolsAndRealSpellIcons() throws Exception {
    String source = Files.readString(Path.of("src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java"));
    assertTrue(source.contains("ClientStatsCache.state().learnedSpells()"));
    assertTrue(source.contains("IronKnownSpellIndex.schools"));
    assertTrue(source.contains("IronKnownSpellIndex.visible"));
    assertTrue(source.contains("EditBox"));
    assertFalse(source.contains("new SchoolFilterSpec(\"fire\""));
}

@Test void mixinConfigRegistersOnlyInscriptionTargets() throws Exception {
    JsonObject root = JsonParser.parseString(Files.readString(Path.of("src/main/resources/statmod.mixins.json"))).getAsJsonObject();
    assertEquals(Set.of("IronInscriptionTableMenuMixin"), strings(root.getAsJsonArray("mixins")));
    assertEquals(Set.of("IronInscriptionTableScreenMixin"), strings(root.getAsJsonArray("client")));
}
```

- [ ] **Step 2: Run UI contracts and confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.client.inscription.KnownSpellIconButtonContractTest --tests tong.statmod.mixin.IronInscriptionTableScreenMixinContractTest --tests tong.statmod.MixinConfigurationContractTest`

Expected: absent UI/mixin files fail.

- [ ] **Step 3: Add icon button and dynamic left panel**

`KnownSpellIconButton` extends `AbstractButton`, renders `spell.getSpellIconResource()`, uses gold for selected and green for bound, and rebuilds a localized tooltip from the current learned level.

Inject at `InscriptionTableScreen.init` tail to create:

```java
for (int i = 0; i < IronKnownSpellIndex.PAGE_SIZE; i++) {
    int visibleIndex = i;
    knownButtons.add(addRenderableWidget(new KnownSpellIconButton(0, 0,
            () -> statmod$selectVisible(visibleIndex))));
}
search = addRenderableWidget(new EditBox(font, 0, 0, 70, 14,
        Component.translatable("statmod.spell.search")));
search.setResponder(value -> { query = value; page = 0; statmod$refresh(); });
```

On refresh, resolve registered spells from `SpellRegistry`, derive dynamic schools, rebuild filter buttons only when the discovered school set changes, compute the six visible entries, position the two-column grid at `leftPos - 76`, collect bound IDs from the slotted `ISpellContainer`, and send only the selected global option button ID. Inject into `isValidInscription` and `onInscription` only when a learned selection exists and no scroll occupies Iron's scroll slot.

- [ ] **Step 4: Register Mixins and manifest**

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "tong.statmod.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "statmod.refmap.json",
  "mixins": ["IronInscriptionTableMenuMixin"],
  "client": ["IronInscriptionTableScreenMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

Add `'MixinConfigs': 'statmod.mixins.json'` to the existing JAR manifest attributes and add the ForgeGradle Mixin annotation processor configuration needed to emit `statmod.refmap.json`. Keep `required: true` so incompatible Iron's method changes fail visibly.

- [ ] **Step 5: Run UI, resource, and compile verification**

Run: `.\gradlew.bat clean test --tests tong.statmod.client.inscription.KnownSpellIconButtonContractTest --tests tong.statmod.mixin.IronInscriptionTableScreenMixinContractTest --tests tong.statmod.MixinConfigurationContractTest --tests tong.statmod.client.stats.StatsLanguageResourcesTest`

Expected: focused tests pass, main source compiles against Iron's 3.16.2, and refmap/config are present under `build/resources/main`.

- [ ] **Step 6: Commit the binding UI**

```powershell
git add build.gradle src/main/java/tong/statmod/client/inscription/KnownSpellIconButton.java src/main/java/tong/statmod/mixin/IronInscriptionTableScreenMixin.java src/main/resources/statmod.mixins.json src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json src/test/java/tong/statmod/client/inscription/KnownSpellIconButtonContractTest.java src/test/java/tong/statmod/mixin/IronInscriptionTableScreenMixinContractTest.java src/test/java/tong/statmod/MixinConfigurationContractTest.java
git commit -m "feat(magic): add learned spell binding panel"
```

### Task 9: Documentation, full verification, integration, and deployment

**Files:**
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `docs/superpowers/specs/2026-07-16-forge-scroll-learning-and-j-binding-design.md` only if implementation evidence requires a precise correction.
- Test: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`
- Test: `src/test/java/tong/statmod/RequiredProviderSmokeContractTest.java`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: a verified remote branch and matching deployed JAR in `test-vrai`.

- [ ] **Step 1: Update runtime documentation and contracts**

Document these exact user rules:

```text
Right-click Iron's-compatible scroll -> learn or upgrade; never direct-cast.
Duplicate/lower scroll -> no consumption.
J -> virtual learned-spell binding menu.
Binding -> server-authorized learned level, no scroll consumed.
Filters -> live native and addon schools.
```

Extend the runtime contract to require `IScroll`, `GLFW_KEY_J`, protocol `9`, learned-spell NBT, both Mixin entries, and English/French keys.

- [ ] **Step 2: Fetch and integrate remote dungeon work safely**

```powershell
git fetch origin forge-1.20.1
git status --short
git merge --no-edit origin/forge-1.20.1
```

Expected: clean merge or already up to date. Resolve only genuine overlaps without deleting either dungeon or learned-spell behavior; never force push.

- [ ] **Step 3: Run the complete clean verification suite**

```powershell
.\gradlew.bat clean test build
.\gradlew.bat test --rerun-tasks
```

Expected: both commands exit `0`, all tests pass with zero failures/errors/skips, and exactly one production JAR is created.

- [ ] **Step 4: Inspect the production JAR**

```powershell
$jar = Get-ChildItem build\libs\statmod-*.jar | Where-Object { $_.Name -notmatch 'sources|javadoc|plain' }
if (@($jar).Count -ne 1) { throw "Expected exactly one production JAR" }
$entries = jar tf $jar.FullName
@(
  'tong/statmod/magic/LearnedSpellState.class',
  'tong/statmod/integration/ironspells/IronScrollLearningEvents.class',
  'tong/statmod/integration/ironspells/VirtualInscriptionTableMenu.class',
  'tong/statmod/mixin/IronInscriptionTableMenuMixin.class',
  'tong/statmod/mixin/IronInscriptionTableScreenMixin.class',
  'statmod.mixins.json',
  'statmod.refmap.json'
) | ForEach-Object { if ($_ -notin $entries) { throw "Missing JAR entry: $_" } }
Get-FileHash $jar.FullName -Algorithm SHA256
```

Expected: all required entries exist, no duplicate classes, and a SHA-256 is recorded.

- [ ] **Step 5: Run required-provider Forge GameTest smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected log evidence:

```text
Forge 1.20.1 server reaches started state.
STAT Mod, Iron's Spells, Epic Fight, Puffish Skills, and Lootr load.
No Mixin apply error, missing class, registry failure, or duplicate mod is present.
```

- [ ] **Step 6: Deploy atomically to `test-vrai`**

```powershell
$mods = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
$backup = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\statmod-backups'
New-Item -ItemType Directory -Force -Path $backup | Out-Null
$existing = Get-ChildItem -LiteralPath $mods -Filter 'statmod-*.jar'
foreach ($old in $existing) {
    Copy-Item -LiteralPath $old.FullName -Destination (Join-Path $backup ($old.BaseName + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + $old.Extension))
    Remove-Item -LiteralPath $old.FullName
}
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $mods $jar.Name)
if ((Get-FileHash $jar.FullName).Hash -ne (Get-FileHash (Join-Path $mods $jar.Name)).Hash) { throw 'Deployed hash mismatch' }
```

Expected: exactly one STAT Mod JAR in `test-vrai\mods`, with a hash matching the build artifact and the previous JAR preserved in `statmod-backups`.

- [ ] **Step 7: Commit docs, push non-force, and record remote equality**

```powershell
git add README.md docs/compatibility/forge-1.20.1-supported-runtime.md src/test/java/tong/statmod/SupportedRuntimeContractTest.java src/test/java/tong/statmod/RequiredProviderSmokeContractTest.java
git commit -m "docs: document scroll learning and binding"
git fetch origin forge-1.20.1
git merge --no-edit origin/forge-1.20.1
git push origin HEAD:forge-1.20.1
git rev-parse HEAD
git rev-parse origin/forge-1.20.1
git status --short
```

Expected: local and remote hashes are identical, the push is non-force, and the isolated worktree is clean.

## Manual acceptance after launch

- Learn one native targeted spell, one native shield/utility spell, and at least two addon spells from different schools.
- Confirm no scroll initiates casting and successful scrolls disappear only outside Creative.
- Confirm equal/lower duplicates remain and a higher-level scroll upgrades.
- Reconnect, open `J`, and confirm learned names, levels, icons, search, pagination, native schools, and addon schools.
- Insert a compatible spellbook, bind at least three learned spells, verify the green bound marker, and confirm the inscribed levels.
- Try a full book, occupied slot, malformed scroll, removed-addon learned ID, and rapid repeated `J`; confirm no loss, duplication, crash, or unauthorized binding.
- Cast the bound targeted, shield, and utility spells through Iron's normal controls and verify mana, cooldown, targeting, and STAT Mod XP remain correct.

package tong.statmod.progression.xp;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.stats.StatType;

public final class PlayerXpState {
    private static final String DISCOVERED_BIOMES_KEY = "discovered_biomes";
    private static final int GLOBAL_CAP = 200;
    private static final int PVP_CAP = 25;
    private static final long COMBO_TICKS = 40L;
    private static final long WILLPOWER_COOLDOWN = 400L;
    private static final long AGILITY_COOLDOWN = 100L;

    private final RollingXpLimiter globalLimiter = new RollingXpLimiter();
    private final Map<OpponentStatKey, ArrayDeque<WindowEntry>> opponentEntries = new HashMap<>();
    private final Set<ResourceLocation> discoveredBiomes = new HashSet<>();

    private long lastMeleeHitTick = Long.MIN_VALUE;
    private int comboLength;
    private long nextWillpowerTick = Long.MIN_VALUE;
    private long nextAgilityTick = Long.MIN_VALUE;
    private float peakFallDistance;
    private boolean fallDamaged;

    public int acceptXp(StatType stat, int requested, long tick, UUID opponentId) {
        if (stat == null || requested <= 0) {
            return 0;
        }
        int accepted = Math.min(requested, globalLimiter.remaining(stat, tick, GLOBAL_CAP));
        OpponentStatKey opponentKey = null;
        if (opponentId != null) {
            opponentKey = new OpponentStatKey(opponentId, stat);
            accepted = Math.min(accepted, opponentRemaining(opponentKey, tick));
        }
        if (accepted <= 0) {
            return 0;
        }
        globalLimiter.record(stat, accepted, tick);
        if (opponentKey != null) {
            opponentEntries.computeIfAbsent(opponentKey, ignored -> new ArrayDeque<>())
                    .addLast(new WindowEntry(tick, accepted));
        }
        return accepted;
    }

    public int recordMeleeHit(long tick) {
        if (lastMeleeHitTick != Long.MIN_VALUE && tick - lastMeleeHitTick <= COMBO_TICKS) {
            comboLength++;
        } else {
            comboLength = 1;
        }
        lastMeleeHitTick = tick;
        return comboLength;
    }

    public boolean tryWillpower(long tick) {
        if (tick < nextWillpowerTick) {
            return false;
        }
        nextWillpowerTick = tick + WILLPOWER_COOLDOWN;
        return true;
    }

    public boolean tryAgility(long tick) {
        if (tick < nextAgilityTick) {
            return false;
        }
        nextAgilityTick = tick + AGILITY_COOLDOWN;
        return true;
    }

    public void observeAirborne(float fallDistance) {
        if (Float.isFinite(fallDistance) && fallDistance > peakFallDistance) {
            peakFallDistance = fallDistance;
        }
    }

    public void markFallDamage() {
        fallDamaged = true;
    }

    public OptionalDouble finishLanding() {
        OptionalDouble result = peakFallDistance >= 5F && !fallDamaged
                ? OptionalDouble.of(peakFallDistance)
                : OptionalDouble.empty();
        clearFall();
        return result;
    }

    public void clearFall() {
        peakFallDistance = 0F;
        fallDamaged = false;
    }

    public boolean discoverBiome(ResourceLocation biomeId) {
        return biomeId != null && discoveredBiomes.add(biomeId);
    }

    public boolean markBiomeDiscovered(ResourceLocation biomeId) {
        return discoverBiome(biomeId);
    }

    public boolean hasDiscoveredBiome(ResourceLocation biomeId) {
        return discoveredBiomes.contains(biomeId);
    }

    public CompoundTag serializeNbt() {
        CompoundTag root = new CompoundTag();
        ListTag biomeTags = new ListTag();
        discoveredBiomes.stream().map(ResourceLocation::toString).sorted()
                .map(StringTag::valueOf).forEach(biomeTags::add);
        root.put(DISCOVERED_BIOMES_KEY, biomeTags);
        return root;
    }

    public void deserializeNbt(CompoundTag root) {
        discoveredBiomes.clear();
        ListTag biomeTags = root.getList(DISCOVERED_BIOMES_KEY, Tag.TAG_STRING);
        for (int index = 0; index < biomeTags.size(); index++) {
            ResourceLocation biomeId = ResourceLocation.tryParse(biomeTags.getString(index));
            if (biomeId != null) {
                discoveredBiomes.add(biomeId);
            }
        }
    }

    public void copyPersistentFrom(PlayerXpState source) {
        discoveredBiomes.clear();
        discoveredBiomes.addAll(source.discoveredBiomes);
    }

    private int opponentRemaining(OpponentStatKey key, long tick) {
        ArrayDeque<WindowEntry> window = opponentEntries.get(key);
        if (window == null) {
            return PVP_CAP;
        }
        while (!window.isEmpty()
                && tick - window.peekFirst().tick() >= RollingXpLimiter.WINDOW_TICKS) {
            window.removeFirst();
        }
        int used = window.stream().mapToInt(WindowEntry::amount).sum();
        return Math.max(0, PVP_CAP - used);
    }

    private record OpponentStatKey(UUID opponentId, StatType stat) {
    }

    private record WindowEntry(long tick, int amount) {
    }
}

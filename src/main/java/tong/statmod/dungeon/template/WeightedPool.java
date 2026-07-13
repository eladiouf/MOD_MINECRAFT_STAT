package tong.statmod.dungeon.template;

import tong.statmod.dungeon.noise.OpenSimplex2S;
import java.util.List;

public class WeightedPool<T> {
    private final List<Entry<T>> entries;

    public record Entry<T>(T value, double weight) {}

    public WeightedPool(List<Entry<T>> entries) {
        this.entries = entries;
    }

    public T select(long seed, double x, double y) {
        if (entries.isEmpty()) throw new IllegalStateException("Empty pool");
        double totalWeight = entries.stream().mapToDouble(Entry::weight).sum();
        double noiseVal = (OpenSimplex2S.noise2(seed, x, y) + 1.0) / 2.0;
        double target = noiseVal * totalWeight;
        double cumulative = 0.0;
        for (Entry<T> e : entries) {
            cumulative += e.weight;
            if (target <= cumulative) return e.value();
        }
        return entries.get(entries.size() - 1).value();
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    public int size() { return entries.size(); }
}

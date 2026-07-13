package tong.statmod.dungeon.template;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.dungeon.BlockPalette;
import java.util.List;

public final class RoomTemplateGenerator {
    private RoomTemplateGenerator() {}

    public static ResourceLocation generateAndRegister(String pool, long seed, double x, double z,
                                                        int w, int h, int d, BlockPalette t) {
        ResourceLocation id = ResourceLocation.parse("statmod:dungeon/template/fallback/" + pool
                + "_" + w + "x" + h + "x" + d + "_" + (seed & 0xFFFF));
        CompoundTag tag = generateFallbackNbt(w, h, d);
        RoomTemplate tpl = RoomTemplate.load(id, tag);
        TemplateRegistry.register(id, tpl);
        TemplateRegistry.registerPool(pool, List.of(
                new WeightedPool.Entry<>(id, 1.0)));
        return id;
    }

    public static CompoundTag generateFallbackNbt(int w, int h, int d) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("size", new int[]{w, h, d});

        ListTag palette = new ListTag();
        palette.add(state("statmod:template/wall"));    // 0
        palette.add(state("statmod:template/floor"));   // 1
        palette.add(state("statmod:template/ceiling")); // 2
        palette.add(state("statmod:template/support")); // 3
        palette.add(state("statmod:template/light"));   // 4
        tag.put("palette", palette);

        byte[] blocks = new byte[w * h * d];
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < d; z++) {
                for (int x = 0; x < w; x++) {
                    if (y == 0) blocks[i] = 1; // floor
                    else if (y == h - 1) blocks[i] = 2; // ceiling
                    else if (x == 0 || x == w - 1 || z == 0 || z == d - 1) blocks[i] = 0; // wall
                    else if ((x == 1 && z == 1) || (x == w - 2 && z == d - 2)
                            || (x == 1 && z == d - 2) || (x == w - 2 && z == 1)) blocks[i] = 3; // pillar
                    else blocks[i] = 1; // floor
                    i++;
                }
            }
        }
        tag.putByteArray("blocks", blocks);
        return tag;
    }

    private static CompoundTag state(String name) {
        CompoundTag t = new CompoundTag();
        t.putString("Name", name);
        return t;
    }
}

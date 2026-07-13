package tong.statmod.dungeon.template;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class RoomTemplate {
    private final int width, height, depth;
    private final int[] blocks;
    private final List<String> palette;
    private final List<ConnectionPoint> connections;
    private final double weight;
    private final Map<String, String> properties;
    private final ResourceLocation id;

    public RoomTemplate(int width, int height, int depth, int[] blocks, List<String> palette,
                         List<ConnectionPoint> connections, double weight,
                         Map<String, String> properties, ResourceLocation id) {
        this.width = width; this.height = height; this.depth = depth;
        this.blocks = blocks; this.palette = palette;
        this.connections = connections; this.weight = weight;
        this.properties = properties; this.id = id;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public double weight() { return weight; }
    public ResourceLocation id() { return id; }
    public List<ConnectionPoint> connections() { return connections; }
    public Map<String, String> properties() { return properties; }

    public static RoomTemplate load(ResourceLocation id, CompoundTag tag) {
        int[] size = tag.getIntArray("size");
        int w = size[0], h = size[1], d = size[2];

        byte[] blockBytes = tag.getByteArray("blocks");
        int[] blocks = new int[blockBytes.length];
        for (int i = 0; i < blockBytes.length; i++) blocks[i] = blockBytes[i] & 0xFF;

        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        List<String> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(paletteTag.getCompound(i).getString("Name"));
        }

        List<ConnectionPoint> conns = new ArrayList<>();
        if (tag.contains("connection_points", Tag.TAG_LIST)) {
            ListTag connTag = tag.getList("connection_points", Tag.TAG_COMPOUND);
            for (int i = 0; i < connTag.size(); i++) {
                CompoundTag c = connTag.getCompound(i);
                conns.add(new ConnectionPoint(
                        c.getInt("rel_x"), c.getInt("rel_y"), c.getInt("rel_z"),
                        c.getString("type"), c.getString("pool")));
            }
        }

        double wgt = tag.contains("weight", Tag.TAG_ANY_NUMERIC) ? tag.getDouble("weight") : 1.0;

        Map<String, String> props = new HashMap<>();
        if (tag.contains("properties", Tag.TAG_COMPOUND)) {
            CompoundTag pTag = tag.getCompound("properties");
            for (String key : pTag.getAllKeys()) props.put(key, pTag.getString(key));
        }

        return new RoomTemplate(w, h, d, blocks, palette, conns, wgt, props, id);
    }

    public void place(ServerLevel lv, BlockPos origin, Direction rotation,
                      DungeonMaterial material, TemplateProperty props) {
        int ix = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int palIdx = blocks[ix++];
                    String palEntry = palette.get(palIdx);
                    BlockState state = resolveBlock(palEntry, material);
                    if (state.isAir()) continue;
                    BlockPos worldPos = rotatePos(origin, x, y, z, rotation);
                    lv.setBlock(worldPos, state, 3);
                }
            }
        }
    }

    private BlockState resolveBlock(String palEntry, DungeonMaterial material) {
        if (palEntry.startsWith("statmod:template/")) {
            String cat = palEntry.substring("statmod:template/".length());
            return material.resolve(cat).defaultBlockState();
        }
        String fullName = palEntry.contains(":") ? palEntry : "minecraft:" + palEntry;
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(ResourceLocation.parse(fullName))
                .orElse(Blocks.STONE).defaultBlockState();
    }

    private BlockPos rotatePos(BlockPos origin, int x, int y, int z, Direction rot) {
        return switch (rot) {
            case NORTH -> origin.offset(x, y, z);
            case SOUTH -> origin.offset(width - 1 - x, y, depth - 1 - z);
            case EAST -> origin.offset(depth - 1 - z, y, x);
            case WEST -> origin.offset(z, y, width - 1 - x);
            default -> origin.offset(x, y, z);
        };
    }
}

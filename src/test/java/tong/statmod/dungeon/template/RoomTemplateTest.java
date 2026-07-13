package tong.statmod.dungeon.template;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RoomTemplateTest {

    @Test
    void loadFromNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("size", new int[]{5, 3, 5});
        byte[] data = new byte[5 * 3 * 5];
        for (int i = 0; i < data.length; i++) data[i] = (byte)(i % 2);
        tag.putByteArray("blocks", data);
        ListTag palette = new ListTag();
        palette.add(stateTag("statmod:template/wall"));
        palette.add(stateTag("statmod:template/floor"));
        tag.put("palette", palette);

        RoomTemplate tpl = RoomTemplate.load(
                ResourceLocation.parse("statmod:dungeon/template/test"), tag);
        assertEquals(5, tpl.width());
        assertEquals(3, tpl.height());
        assertEquals(5, tpl.depth());
        assertEquals(1.0, tpl.weight(), 1e-9);
    }

    @Test
    void loadWithMetadata() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("size", new int[]{3, 3, 3});
        tag.putByteArray("blocks", new byte[27]);
        ListTag palette = new ListTag();
        palette.add(stateTag("minecraft:stone"));
        tag.put("palette", palette);
        tag.putDouble("weight", 2.5);

        ListTag conns = new ListTag();
        CompoundTag cp = new CompoundTag();
        cp.putInt("rel_x", 1); cp.putInt("rel_y", 0); cp.putInt("rel_z", 0);
        cp.putString("type", "entrance"); cp.putString("pool", "main");
        conns.add(cp);
        tag.put("connection_points", conns);

        CompoundTag props = new CompoundTag();
        props.putString("difficulty", "hard");
        tag.put("properties", props);

        RoomTemplate tpl = RoomTemplate.load(
                ResourceLocation.parse("statmod:dungeon/template/test2"), tag);
        assertEquals(2.5, tpl.weight(), 1e-9);
        assertEquals(1, tpl.connections().size());
        assertEquals("hard", tpl.properties().get("difficulty"));
    }

    private static CompoundTag stateTag(String name) {
        CompoundTag t = new CompoundTag();
        t.putString("Name", name);
        return t;
    }
}

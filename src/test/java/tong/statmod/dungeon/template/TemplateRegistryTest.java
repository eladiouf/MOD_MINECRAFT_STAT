package tong.statmod.dungeon.template;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TemplateRegistryTest {

    @AfterEach
    void cleanUp() { TemplateRegistry.clear(); }

    @Test
    void registerAndLookup() {
        ResourceLocation id = ResourceLocation.parse("statmod:dungeon/template/test");
        RoomTemplate tpl = makeTemplate(id, 3, 3, 3);
        TemplateRegistry.register(id, tpl);
        assertSame(tpl, TemplateRegistry.get(id));
    }

    @Test
    void poolSelection() {
        ResourceLocation a = ResourceLocation.parse("statmod:dungeon/template/a");
        ResourceLocation b = ResourceLocation.parse("statmod:dungeon/template/b");
        TemplateRegistry.register(a, makeTemplate(a, 3, 3, 3));
        TemplateRegistry.register(b, makeTemplate(b, 3, 3, 3));
        TemplateRegistry.registerPool("test_pool", List.of(
                new WeightedPool.Entry<>(a, 1.0),
                new WeightedPool.Entry<>(b, 1.0)));

        RoomTemplate result = TemplateRegistry.select("test_pool", 42L, 0.5, 0.5);
        assertNotNull(result);
        assertTrue(result.id().equals(a) || result.id().equals(b));
    }

    @Test
    void emptyPoolReturnsNull() {
        assertNull(TemplateRegistry.select("nonexistent", 1L, 0, 0));
    }

    @Test
    void generateAndRegisterFallback() {
        ResourceLocation id = RoomTemplateGenerator.generateAndRegister(
                "combat", 42L, 0.5, 0.5, 7, 5, 7, null);
        assertNotNull(TemplateRegistry.get(id));
        assertNotNull(TemplateRegistry.select("combat", 42L, 0.5, 0.5));
    }

    private static RoomTemplate makeTemplate(ResourceLocation id, int w, int h, int d) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("size", new int[]{w, h, d});
        tag.putByteArray("blocks", new byte[w * h * d]);
        ListTag pal = new ListTag();
        CompoundTag s = new CompoundTag();
        s.putString("Name", "minecraft:stone");
        pal.add(s);
        tag.put("palette", pal);
        return RoomTemplate.load(id, tag);
    }
}

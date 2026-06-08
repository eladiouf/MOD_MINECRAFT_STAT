package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillStateTest {

    @Test
    void defaultMana_isZero() {
        assertEquals(0, new MobSkillState().getMana());
    }

    @Test
    void setMana_clampsToZero() {
        MobSkillState s = new MobSkillState();
        s.setMana(-5);
        assertEquals(0, s.getMana());
    }

    @Test
    void cooldown_get_unknownId_returnsZero() {
        assertEquals(0L, new MobSkillState().getCooldownEndTick(
            new ResourceLocation("statmod", "x")));
    }

    @Test
    void cooldown_setThenGet_roundTrip() {
        MobSkillState s = new MobSkillState();
        ResourceLocation id = new ResourceLocation("statmod", "x");
        s.setCooldownEndTick(id, 1234L);
        assertEquals(1234L, s.getCooldownEndTick(id));
    }

    @Test
    void globalCooldown_default_isZero() {
        assertEquals(0L, new MobSkillState().getGlobalCooldownEndTick());
    }

    @Test
    void nbtRoundTrip_preservesAllFields() {
        MobSkillState original = new MobSkillState();
        original.setMana(42);
        original.setGlobalCooldownEndTick(500L);
        ResourceLocation id = new ResourceLocation("statmod", "charge");
        original.setCooldownEndTick(id, 750L);

        CompoundTag tag = original.serializeNBT();
        MobSkillState loaded = new MobSkillState();
        loaded.deserializeNBT(tag);

        assertEquals(42, loaded.getMana());
        assertEquals(500L, loaded.getGlobalCooldownEndTick());
        assertEquals(750L, loaded.getCooldownEndTick(id));
    }

    @Test
    void deserialize_sanitizesNegativeValues() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", -10);
        tag.putLong("GlobalCD", -50L);
        MobSkillState s = new MobSkillState();
        s.deserializeNBT(tag);
        assertEquals(0, s.getMana());
        assertEquals(0L, s.getGlobalCooldownEndTick());
    }
}

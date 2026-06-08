package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tong.statmod.capability.MobStats;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillRegistryTest {

    @AfterEach
    void clear() { MobSkillRegistry.clear(); }

    private static MobSkill stub(String id) {
        return new MobSkill() {
            public ResourceLocation id() { return new ResourceLocation("statmod", id); }
            public StatType requiredStat() { return StatType.BRUTE_FORCE; }
            public int requiredLevel() { return 0; }
            public int baseCooldownTicks() { return 20; }
            public int manaCost() { return 0; }
            public double maxRange() { return 10; }
            public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }
            public void execute(Mob mob, LivingEntity target, MobStats stats) {}
        };
    }

    @Test
    void register_thenGet_returnsSameInstance() {
        MobSkill s = stub("alpha");
        MobSkillRegistry.register(s);
        assertSame(s, MobSkillRegistry.get(new ResourceLocation("statmod", "alpha")));
    }

    @Test
    void get_unknownId_returnsNull() {
        assertNull(MobSkillRegistry.get(new ResourceLocation("statmod", "unknown")));
    }

    @Test
    void register_duplicateId_overwritesAndLogsWarn() {
        MobSkill first  = stub("dup");
        MobSkill second = stub("dup");
        MobSkillRegistry.register(first);
        MobSkillRegistry.register(second);
        assertSame(second, MobSkillRegistry.get(new ResourceLocation("statmod", "dup")));
    }

    @Test
    void values_returnsAllRegistered() {
        MobSkillRegistry.register(stub("a"));
        MobSkillRegistry.register(stub("b"));
        assertEquals(2, MobSkillRegistry.values().size());
    }
}

package tong.statmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ItemStackDurabilityMixinTest {
    @Test
    void hurtAndBreakHandlerKeepsTheCurrentModifyVariableSignature() {
        assertDoesNotThrow(() -> ItemStack.class.getDeclaredMethod(
                "hurtAndBreak",
                int.class,
                ServerLevel.class,
                LivingEntity.class,
                Consumer.class
        ));

        assertDoesNotThrow(() -> ItemStackDurabilityMixin.class.getDeclaredMethod(
                "statmod$reduceOvergearedDurability",
                int.class,
                int.class,
                ServerLevel.class,
                LivingEntity.class,
                Consumer.class
        ));
    }
}

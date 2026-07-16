package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class IronScrollDescriptor {
    private IronScrollDescriptor() {
    }

    public record Descriptor(String id, int level, int minLevel, int maxLevel) {
    }

    public static Optional<Descriptor> describe(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IScroll)) {
            return Optional.empty();
        }
        ISpellContainer container = ISpellContainer.get(stack);
        SpellData data = container == null ? SpellData.EMPTY : container.getSpellAtIndex(0);
        if (data == null || data == SpellData.EMPTY || data.getSpell() == null) {
            return Optional.empty();
        }
        AbstractSpell spell = data.getSpell();
        ResourceLocation id = SpellRegistry.REGISTRY.get().getKey(spell);
        if (id == null || spell == SpellRegistry.none()) {
            return Optional.empty();
        }
        return Optional.of(new Descriptor(
                id.toString(), data.getLevel(), spell.getMinLevel(), spell.getMaxLevel()));
    }
}

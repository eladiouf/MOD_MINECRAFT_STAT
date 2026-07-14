package tong.statmod.mixin;

import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

/**
 * Fait reconnaître n'importe quelle <b>arme de mêlée tenue</b> comme objet de cast valide.
 *
 * <p>Dans {@code ServerPlayerEvents.onUseItem} (clic-droit), Iron's ne lance le sort sélectionné
 * que si l'objet en main porte le composant {@code CASTING_IMPLEMENT} (grimoire, staff, focus).
 * On redirige ce test pour qu'une épée/hache/trident/masse compte aussi → le joueur caste ses
 * sorts appris (injectés en slot MAINHAND par {@code SpellSelectionManagerMixin}) au clic-droit
 * en tenant son arme. Iron's a alors l'objet support qu'il exige pour initier ET maintenir le
 * cast (l'arme reste en main), donc le cast se complète.
 */
@Mixin(ServerPlayerEvents.class)
public class IronWeaponCastImplementMixin {

    @Redirect(method = "onUseItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;has(Ljava/util/function/Supplier;)Z"))
    private static boolean statmod$weaponCountsAsImplement(ItemStack stack, Supplier<?> component) {
        @SuppressWarnings("unchecked")
        Supplier<? extends DataComponentType<?>> typed =
                (Supplier<? extends DataComponentType<?>>) component;
        if (stack.has(typed)) return true; // objet de cast natif (grimoire, focus…)
        if (component == ComponentRegistry.CASTING_IMPLEMENT && isMeleeWeapon(stack)) return true;
        return false;
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof TridentItem || item instanceof MaceItem;
    }
}

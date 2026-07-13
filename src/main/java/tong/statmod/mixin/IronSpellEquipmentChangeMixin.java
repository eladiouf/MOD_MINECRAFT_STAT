package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps casts alive when another mod only refreshes components of the same equipped item. */
@Mixin(ServerPlayerEvents.class)
public class IronSpellEquipmentChangeMixin {

    @Redirect(method = "onLivingEquipmentChangeEvent",
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;serverSideCancelCast(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private static void statmod$ignoreSameItemRefresh(ServerPlayer player,
                                                       LivingEquipmentChangeEvent event) {
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        if (!from.isEmpty() && !to.isEmpty() && ItemStack.isSameItem(from, to)) {
            return;
        }
        Utils.serverSideCancelCast(player);
    }
}

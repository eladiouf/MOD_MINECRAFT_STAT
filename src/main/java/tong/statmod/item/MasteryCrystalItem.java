package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.weapon.WeaponXPHandler;
import tong.statmod.weapon.WeaponType;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class MasteryCrystalItem extends Item {
    public MasteryCrystalItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        WeaponType weaponType = determineWeaponType(sp);
        if (weaponType == null) {
            sp.sendSystemMessage(Component.literal("\u00a7cEquip a weapon to use this crystal"));
            return InteractionResultHolder.fail(stack);
        }

        CapabilityHelper.withWeaponMastery(sp, wm -> {
            wm.addXp(weaponType.ordinal(), 500);
            sp.sendSystemMessage(Component.literal("\u00a7a+500 weapon XP for " + weaponType.name()));
        });

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    private static WeaponType determineWeaponType(ServerPlayer player) {
        WeaponType[] ref = new WeaponType[1];
        CapabilityHelper.withEpicFight(player, cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    yesman.epicfight.world.capabilities.item.WeaponCategory cat = itemCap.getWeaponCategory();
                    for (WeaponType type : WeaponType.values()) {
                        if (type.epicFightCategory == cat) {
                            ref[0] = type;
                            return;
                        }
                    }
                }
            }
        });
        return ref[0];
    }
}

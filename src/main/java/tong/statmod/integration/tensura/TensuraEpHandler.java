package tong.statmod.integration.tensura;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class TensuraEpHandler {
    private TensuraEpHandler() {}

    public static void init() {
        if (!ModList.get().isLoaded("tensura")) {
            return;
        }
        TensuraEntityEvents.ENERGY_DRAIN_EVENT.register(TensuraEpHandler::onEnergyDrain);
        STATMod.LOGGER.info("Tensura EP integration loaded");
    }

    public static EventResult onEnergyDrain(
            LivingEntity entity,
            Entity source,
            Changeable<EnergyHelper.DrainType> drainType,
            Changeable<EnergyHelper.GainType> gainType,
            Changeable<Double> amount,
            Changeable<Boolean> cancelled) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return EventResult.pass();
        }

        if (drainType == null || gainType == null || amount == null) {
            return EventResult.pass();
        }

        if (!isEpDrainType(drainType.get()) || gainType.get() == EnergyHelper.GainType.NONE) {
            return EventResult.pass();
        }

        PlayerStatData data = player.getData(ModAttachments.STATS);
        double baseAmount = amount.get();
        double scaledAmount = TensuraXpMultiplier.applyEpMultiplier(data, resolveActionType(source), baseAmount);
        amount.set(scaledAmount);
        return EventResult.pass();
    }

    public static boolean isEpDrainType(EnergyHelper.DrainType drainType) {
        return drainType == EnergyHelper.DrainType.EP || drainType == EnergyHelper.DrainType.MAX_EP;
    }

    public static String resolveActionType(Entity source) {
        if (source instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return resolveActionType(owner.getMainHandItem());
        }

        if (source instanceof Player player) {
            return resolveActionType(player.getMainHandItem());
        }

        if (source instanceof LivingEntity livingEntity) {
            return resolveActionType(livingEntity.getMainHandItem());
        }

        return "melee_axe";
    }

    public static String resolveActionType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "melee_axe";
        }

        if (stack.getItem() instanceof TridentItem || stack.getItem() instanceof ProjectileWeaponItem) {
            return "ranged";
        }

        return resolveActionTypeId(stack.getItem().getDescriptionId());
    }

    public static String resolveActionTypeId(String itemId) {
        return WeaponResolver.tensuraActionFor(itemId);
    }
}

package tong.statmod.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tong.statmod.integration.sdm.SDMEconomyBridge;

import java.util.ArrayList;
import java.util.List;

/** Transactions atomiques entre espèces FDP et solde SDM. */
public final class MagicBankService {
    public enum Status { SUCCESS, NOTHING_TO_DEPOSIT, INVALID_AMOUNT, INSUFFICIENT_BALANCE, INVENTORY_FULL, SDM_UNAVAILABLE }
    public record Result(Status status, long amount) {}

    private MagicBankService() {}

    public static long physicalTotal(ServerPlayer player) {
        long total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            total += FdpDenomination.valueOf(stack.getItem()) * stack.getCount();
        }
        return total;
    }

    public static Result depositAll(ServerPlayer player) {
        long total = physicalTotal(player);
        if (total <= 0) return new Result(Status.NOTHING_TO_DEPOSIT, 0);
        if (!SDMEconomyBridge.addCoins(player, total)) return new Result(Status.SDM_UNAVAILABLE, 0);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (FdpDenomination.valueOf(stack.getItem()) > 0) stack.setCount(0);
        }
        player.getInventory().setChanged();
        return new Result(Status.SUCCESS, total);
    }

    public static Result withdraw(ServerPlayer player, long amount) {
        if (!FdpCashMath.isWithdrawable(amount)) return new Result(Status.INVALID_AMOUNT, 0);
        if (SDMEconomyBridge.getCoins(player) < amount) return new Result(Status.INSUFFICIENT_BALANCE, 0);
        List<ItemStack> payout = payout(amount);
        if (!canFit(player, payout)) return new Result(Status.INVENTORY_FULL, 0);
        if (!SDMEconomyBridge.removeCoins(player, amount)) return new Result(Status.SDM_UNAVAILABLE, 0);
        for (ItemStack stack : payout) player.getInventory().add(stack);
        return new Result(Status.SUCCESS, amount);
    }

    private static List<ItemStack> payout(long amount) {
        List<ItemStack> stacks = new ArrayList<>();
        var parts = FdpCashMath.breakdown(amount);
        for (var denomination : FdpDenomination.descending()) {
            int remaining = parts.getOrDefault(denomination.value(), 0);
            while (remaining > 0) {
                int count = Math.min(64, remaining);
                stacks.add(new ItemStack(denomination.item(), count));
                remaining -= count;
            }
        }
        return stacks;
    }

    private static boolean canFit(ServerPlayer player, List<ItemStack> payout) {
        List<ItemStack> simulated = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            simulated.add(player.getInventory().getItem(i).copy());
        }
        for (ItemStack incoming : payout) {
            int remaining = incoming.getCount();
            for (ItemStack slot : simulated) {
                if (remaining == 0) break;
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, incoming)) {
                    int moved = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                    slot.grow(moved);
                    remaining -= moved;
                }
            }
            for (int i = 0; i < simulated.size() && remaining > 0; i++) {
                if (simulated.get(i).isEmpty()) {
                    int moved = Math.min(remaining, incoming.getMaxStackSize());
                    simulated.set(i, incoming.copyWithCount(moved));
                    remaining -= moved;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
}

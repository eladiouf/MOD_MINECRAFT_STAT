package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsCompat;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.integration.elementals.ElementalsMageRules;
import tong.statmod.integration.elementals.ElementalsPerkBindings;
import tong.statmod.integration.elementals.ElementalsRaceAffinity;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumSet;
import java.util.Locale;

public class ElementalGrimoireItem extends Item {
    private final ElementalBranch branch;

    public ElementalGrimoireItem(ElementalBranch branch, Properties properties) {
        super(properties);
        this.branch = branch;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        PlayerStatData statData = serverPlayer.getData(ModAttachments.STATS);
        ElementalsMageData mageData = serverPlayer.getData(ModAttachments.ELEMENTALS_MAGE);
        var profile = ElementalsRaceAffinity.resolve(PlayerDataBridge.getRaceId(serverPlayer));
        if (!profile.supported() || !tryUnlock(profile, branch, statData, mageData)) {
            serverPlayer.displayClientMessage(deniedMessage(branch), true);
            return InteractionResultHolder.fail(serverPlayer.getItemInHand(hand));
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        stack.shrink(1);
        ElementalsCompat.reconcilePlayer(serverPlayer);
        SyncHelper.syncPerks(serverPlayer);
        serverPlayer.displayClientMessage(unlockedMessage(branch), true);
        return InteractionResultHolder.success(stack);
    }

    static boolean tryUnlockForTests(ElementalBranch branch, PlayerStatData statData, ElementalsMageData mageData) {
        return tryUnlock(null, branch, statData, mageData);
    }

    static boolean tryUnlockForTests(tong.statmod.integration.elementals.MageRaceProfile profile,
                                     ElementalBranch branch,
                                     PlayerStatData statData,
                                     ElementalsMageData mageData) {
        return tryUnlock(profile, branch, statData, mageData);
    }

    private static boolean tryUnlock(tong.statmod.integration.elementals.MageRaceProfile profile,
                                     ElementalBranch branch,
                                     PlayerStatData statData,
                                     ElementalsMageData mageData) {
        if (!mageData.mageAwakened()) {
            return false;
        }
        if (!ElementalsMageRules.canUseRareGrimoire(profile, branch, statData::getLevel)) {
            return false;
        }
        if (mageData.rewardedRareBranches().contains(branch)) {
            return false;
        }

        EnumSet<ElementalBranch> rewards = mageData.rewardedRareBranches();
        rewards.add(branch);
        mageData.setRewardedRareBranches(rewards);

        EnumSet<ElementalBranch> unlocked = mageData.unlockedBranches();
        unlocked.add(branch);
        mageData.setUnlockedBranches(unlocked);

        statData.markPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(branch).id);
        return true;
    }

    private static Component deniedMessage(ElementalBranch branch) {
        return Component.translatable(messageKey(branch, "denied"));
    }

    private static Component unlockedMessage(ElementalBranch branch) {
        return Component.translatable(messageKey(branch, "unlocked"));
    }

    private static String messageKey(ElementalBranch branch, String suffix) {
        return "item.statmod." + branch.name().toLowerCase(Locale.ROOT) + "_grimoire." + suffix;
    }
}

package tong.statmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tong.statmod.block.ForgingBlocks;
import tong.statmod.block.entity.EnchantmentAnvilBlockEntity;
import tong.statmod.forge.EnchantmentAnvilRecipeCatalog;
import tong.statmod.forge.ForgeStationItemRules;

public class EnchantmentAnvilMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT_COUNT = 4;
    public static final int STATION_SLOT_COUNT = 5;

    private static final int OUTPUT_SLOT_INDEX = 4;

    private final Container inputSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private EnchantmentAnvilRecipeCatalog.RecipeSpec currentRecipe;
    private EnchantmentAnvilRecipeCatalog.InputFeedback inputFeedback =
            new EnchantmentAnvilRecipeCatalog.InputFeedback(EnchantmentAnvilRecipeCatalog.InputState.NONE, "");

    public EnchantmentAnvilMenu(int containerId, Inventory playerInventory, EnchantmentAnvilBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getContainer(),
                ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos()));
    }

    public EnchantmentAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(INPUT_SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    private EnchantmentAnvilMenu(int containerId, Inventory playerInventory, Container inputSlots, ContainerLevelAccess access) {
        super(ModMenuTypes.ENCHANTMENT_ANVIL_MENU.get(), containerId);
        this.inputSlots = inputSlots;
        this.access = access;

        this.addSlot(new Slot(inputSlots, 0, 17, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isRoughIntermediate(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentAnvilMenu.this.slotsChanged(EnchantmentAnvilMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(inputSlots, 1, 44, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isShardLikeCatalyst(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentAnvilMenu.this.slotsChanged(EnchantmentAnvilMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(inputSlots, 2, 71, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isShardLikeCatalyst(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentAnvilMenu.this.slotsChanged(EnchantmentAnvilMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(inputSlots, 3, 98, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isAnvilSupport(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentAnvilMenu.this.slotsChanged(EnchantmentAnvilMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(resultSlots, 0, 134, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                consumeInputs();
                super.onTake(player, stack);
                EnchantmentAnvilMenu.this.slotsChanged(EnchantmentAnvilMenu.this.inputSlots);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();
            if (slotIndex == OUTPUT_SLOT_INDEX) {
                if (!this.moveItemStackTo(slotStack, STATION_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, stack);
            } else if (slotIndex < STATION_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, STATION_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, INPUT_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return stack;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != this.inputSlots) {
            return;
        }

        currentRecipe = EnchantmentAnvilRecipeCatalog.match(
                ForgeStationItemRules.itemId(inputSlots.getItem(0)),
                inputSlots.getItem(0).getCount(),
                ForgeStationItemRules.itemId(inputSlots.getItem(1)),
                inputSlots.getItem(1).getCount(),
                ForgeStationItemRules.itemId(inputSlots.getItem(2)),
                inputSlots.getItem(2).getCount(),
                ForgeStationItemRules.itemId(inputSlots.getItem(3)),
                inputSlots.getItem(3).getCount());
        inputFeedback = EnchantmentAnvilRecipeCatalog.describeInputFeedback(
                ForgeStationItemRules.itemId(inputSlots.getItem(0)),
                ForgeStationItemRules.itemId(inputSlots.getItem(1)),
                ForgeStationItemRules.itemId(inputSlots.getItem(2)),
                ForgeStationItemRules.itemId(inputSlots.getItem(3)));
        resultSlots.setItem(0, EnchantmentAnvilRecipeCatalog.createResult(currentRecipe));
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ForgingBlocks.ENCHANTMENT_ANVIL.get());
    }

    private void consumeInputs() {
        if (currentRecipe == null) {
            return;
        }

        inputSlots.removeItem(0, 1);
        inputSlots.removeItem(1, currentRecipe.primaryCount());
        if (currentRecipe.secondaryCount() > 0) {
            inputSlots.removeItem(2, currentRecipe.secondaryCount());
        }
        inputSlots.removeItem(3, currentRecipe.supportCount());
    }

    public boolean hasCraftableResult() {
        return currentRecipe != null;
    }

    public EnchantmentAnvilRecipeCatalog.InputFeedback getInputFeedback() {
        return inputFeedback;
    }

    public ItemStack getExpectedSupportStack() {
        String supportId = inputFeedback.expectedSupportId();
        if (supportId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(supportId)));
    }
}

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
import tong.statmod.block.entity.InfusionForgeBlockEntity;
import tong.statmod.forge.ForgeStationItemRules;
import tong.statmod.forge.InfusionForgeRecipeCatalog;
import tong.statmod.integration.overgeared.AssemblyForgingPolicy;
import tong.statmod.storage.ModAttachments;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

public class InfusionForgeMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT_COUNT = 3;
    public static final int STATION_SLOT_COUNT = 4;

    private static final int OUTPUT_SLOT_INDEX = 3;

    private final Container inputSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player owner;
    private CraftMode currentCraftMode = CraftMode.NONE;

    public InfusionForgeMenu(int containerId, Inventory playerInventory, InfusionForgeBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getContainer(),
                ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos()));
    }

    public InfusionForgeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(INPUT_SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    private InfusionForgeMenu(int containerId, Inventory playerInventory, Container inputSlots, ContainerLevelAccess access) {
        super(ModMenuTypes.INFUSION_FORGE_MENU.get(), containerId);
        this.inputSlots = inputSlots;
        this.access = access;
        this.owner = playerInventory.player;

        this.addSlot(new Slot(inputSlots, 0, 26, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isRoughIntermediate(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                InfusionForgeMenu.this.slotsChanged(InfusionForgeMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(inputSlots, 1, 62, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isRuneEssence(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                InfusionForgeMenu.this.slotsChanged(InfusionForgeMenu.this.inputSlots);
            }
        });
        this.addSlot(new Slot(inputSlots, 2, 98, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeStationItemRules.isGrip(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                InfusionForgeMenu.this.slotsChanged(InfusionForgeMenu.this.inputSlots);
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
                InfusionForgeMenu.this.slotsChanged(InfusionForgeMenu.this.inputSlots);
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

        ItemStack result = InfusionForgeRecipeCatalog.resultForIds(
                ForgeStationItemRules.itemId(inputSlots.getItem(0)),
                ForgeStationItemRules.itemId(inputSlots.getItem(1)),
                ForgeStationItemRules.itemId(inputSlots.getItem(2)));
        currentCraftMode = CraftMode.NONE;
        if (!result.isEmpty()) {
            currentCraftMode = CraftMode.INFUSION;
        } else {
            result = findAssemblyResult();
            if (!result.isEmpty()) {
                currentCraftMode = CraftMode.ASSEMBLY;
            }
        }
        resultSlots.setItem(0, result);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ForgingBlocks.INFUSION_FORGE.get());
    }

    private void consumeInputs() {
        switch (currentCraftMode) {
            case INFUSION -> {
                for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
                    inputSlots.removeItem(i, 1);
                }
            }
            case ASSEMBLY -> {
                inputSlots.removeItem(0, 1);
                inputSlots.removeItem(2, 1);
            }
            default -> {
            }
        }
    }

    private ItemStack findAssemblyResult() {
        if (!inputSlots.getItem(1).isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack base = inputSlots.getItem(0);
        ItemStack grip = inputSlots.getItem(2);
        if (base.isEmpty() || grip.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return access.evaluate((level, pos) -> {
            CraftingInput input = CraftingInput.of(2, 1, List.of(
                    base.copyWithCount(1),
                    grip.copyWithCount(1)));
            Optional<RecipeHolder<CraftingRecipe>> optional =
                    level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (optional.isEmpty()) {
                return ItemStack.EMPTY;
            }

            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (!"statmod".equals(holder.id().getNamespace()) || !holder.id().getPath().startsWith("assembly/")) {
                return ItemStack.EMPTY;
            }

            ItemStack assembled = holder.value().assemble(input, level.registryAccess());
            if (assembled.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(assembled.getItem());
            if (!AssemblyForgingPolicy.canAssemble(resultId, owner.getData(ModAttachments.STATS))) {
                return ItemStack.EMPTY;
            }
            return assembled;
        }, ItemStack.EMPTY);
    }

    private enum CraftMode {
        NONE,
        INFUSION,
        ASSEMBLY
    }
}

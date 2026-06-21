package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.client.ClientMagicCache;
import tong.statmod.integration.ironspells.IronInscriptionKnownSpellIndex;

import java.util.ArrayList;
import java.util.List;

@Mixin(InscriptionTableScreen.class)
public abstract class IronInscriptionTableScreenMixin extends AbstractContainerScreen<InscriptionTableMenu> {
    @Shadow protected Button inscribeButton;
    @Shadow private int selectedSpellIndex;
    @Shadow private void setSelectedIndex(int index) {}

    @Unique
    private List<Button> statmod$knownSpellButtons = new ArrayList<>();
    @Unique
    private Button statmod$prevKnownSpellPageButton;
    @Unique
    private Button statmod$nextKnownSpellPageButton;
    @Unique
    private int statmod$knownSpellPage;
    @Unique
    private Integer statmod$selectedKnownSpellOption;

    private IronInscriptionTableScreenMixin(InscriptionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void statmod$addKnownSpellButtons(CallbackInfo ci) {
        this.statmod$knownSpellButtons = new ArrayList<>();
        for (int i = 0; i < IronInscriptionKnownSpellIndex.PAGE_SIZE; i++) {
            final int visibleIndex = i;
            Button button = this.addRenderableWidget(Button.builder(Component.empty(), b -> statmod$selectKnownSpell(visibleIndex))
                    .bounds(0, 0, 86, 16)
                    .build());
            this.statmod$knownSpellButtons.add(button);
        }

        this.statmod$prevKnownSpellPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            this.statmod$knownSpellPage = Math.max(0, this.statmod$knownSpellPage - 1);
            this.statmod$refreshKnownSpellButtons();
        }).bounds(0, 0, 12, 12).build());

        this.statmod$nextKnownSpellPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            this.statmod$knownSpellPage = Math.min(statmod$maxKnownSpellPage(), this.statmod$knownSpellPage + 1);
            this.statmod$refreshKnownSpellButtons();
        }).bounds(0, 0, 12, 12).build());

        this.statmod$refreshKnownSpellButtons();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void statmod$refreshKnownSpellUi(net.minecraft.client.gui.GuiGraphics guiGraphics,
                                             float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        this.statmod$refreshKnownSpellButtons();
    }

    @Inject(method = "isValidInscription", at = @At("HEAD"), cancellable = true)
    private void statmod$allowKnownSpellInscription(CallbackInfoReturnable<Boolean> cir) {
        if (statmod$hasKnownSpellSelection() && this.menu.getSpellBookSlot().hasItem() && !this.menu.getScrollSlot().hasItem()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onInscription", at = @At("HEAD"), cancellable = true)
    private void statmod$inscribeKnownSpellWithoutScroll(CallbackInfo ci) {
        if (!statmod$hasKnownSpellSelection() || this.menu.getScrollSlot().hasItem()) {
            return;
        }

        ItemStack spellBookStack = this.menu.getSpellBookSlot().getItem();
        ISpellContainer container = ISpellContainer.get(spellBookStack);
        int targetIndex = this.selectedSpellIndex;
        if (targetIndex < 0 || container.getSpellAtIndex(targetIndex) != SpellData.EMPTY) {
            targetIndex = container.getNextAvailableIndex();
        }
        if (targetIndex < 0) {
            ci.cancel();
            return;
        }

        this.setSelectedIndex(targetIndex);
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, -1);
        }
        ci.cancel();
    }

    @Unique
    private void statmod$selectKnownSpell(int visibleIndex) {
        List<String> page = IronInscriptionKnownSpellIndex.page(statmod$knownIronSpells(), this.statmod$knownSpellPage);
        if (visibleIndex < 0 || visibleIndex >= page.size()) {
            return;
        }

        int globalIndex = this.statmod$knownSpellPage * IronInscriptionKnownSpellIndex.PAGE_SIZE + visibleIndex;
        this.statmod$selectedKnownSpellOption = globalIndex;
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                    IronInscriptionKnownSpellIndex.buttonIdForOption(globalIndex));
        }
        this.statmod$refreshKnownSpellButtons();
    }

    @Unique
    private void statmod$refreshKnownSpellButtons() {
        List<String> known = statmod$knownIronSpells();
        this.statmod$knownSpellPage = Mth.clamp(this.statmod$knownSpellPage, 0, statmod$maxKnownSpellPage());
        List<String> page = IronInscriptionKnownSpellIndex.page(known, this.statmod$knownSpellPage);

        int baseX = this.leftPos + 150;
        int baseY = this.topPos + 18;
        boolean spellBookSlotted = this.menu.getSpellBookSlot().hasItem();

        for (int i = 0; i < this.statmod$knownSpellButtons.size(); i++) {
            Button button = this.statmod$knownSpellButtons.get(i);
            button.setX(baseX);
            button.setY(baseY + i * 18);
            if (i < page.size()) {
                int globalIndex = this.statmod$knownSpellPage * IronInscriptionKnownSpellIndex.PAGE_SIZE + i;
                String spellId = page.get(i);
                button.visible = true;
                button.active = spellBookSlotted;
                button.setMessage(statmod$buttonLabel(spellId, globalIndex == (this.statmod$selectedKnownSpellOption == null ? -1 : this.statmod$selectedKnownSpellOption)));
            } else {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.empty());
            }
        }

        if (this.statmod$prevKnownSpellPageButton != null) {
            this.statmod$prevKnownSpellPageButton.setX(baseX);
            this.statmod$prevKnownSpellPageButton.setY(baseY + IronInscriptionKnownSpellIndex.PAGE_SIZE * 18);
            this.statmod$prevKnownSpellPageButton.visible = known.size() > IronInscriptionKnownSpellIndex.PAGE_SIZE;
            this.statmod$prevKnownSpellPageButton.active = this.statmod$knownSpellPage > 0;
        }
        if (this.statmod$nextKnownSpellPageButton != null) {
            this.statmod$nextKnownSpellPageButton.setX(baseX + 74);
            this.statmod$nextKnownSpellPageButton.setY(baseY + IronInscriptionKnownSpellIndex.PAGE_SIZE * 18);
            this.statmod$nextKnownSpellPageButton.visible = known.size() > IronInscriptionKnownSpellIndex.PAGE_SIZE;
            this.statmod$nextKnownSpellPageButton.active = this.statmod$knownSpellPage < statmod$maxKnownSpellPage();
        }
    }

    @Unique
    private boolean statmod$hasKnownSpellSelection() {
        List<String> known = statmod$knownIronSpells();
        return this.statmod$selectedKnownSpellOption != null
                && this.statmod$selectedKnownSpellOption >= 0
                && this.statmod$selectedKnownSpellOption < known.size();
    }

    @Unique
    private int statmod$maxKnownSpellPage() {
        return IronInscriptionKnownSpellIndex.maxPage(statmod$knownIronSpells());
    }

    @Unique
    private List<String> statmod$knownIronSpells() {
        return IronInscriptionKnownSpellIndex.learnedIronSpellIds(ClientMagicCache.getLearnedSpells());
    }

    @Unique
    private Component statmod$buttonLabel(String spellId, boolean selected) {
        Component base = Component.literal(statmod$fallbackLabel(spellId));
        if (spellId != null && this.minecraft != null && this.minecraft.player != null) {
            AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
            if (spell != null) {
                base = spell.getDisplayName(this.minecraft.player);
            }
        }
        return selected ? Component.literal("> ").append(base) : base;
    }

    @Unique
    private static String statmod$fallbackLabel(String spellId) {
        int sep = spellId == null ? -1 : spellId.indexOf(':');
        return sep >= 0 ? spellId.substring(sep + 1) : String.valueOf(spellId);
    }
}

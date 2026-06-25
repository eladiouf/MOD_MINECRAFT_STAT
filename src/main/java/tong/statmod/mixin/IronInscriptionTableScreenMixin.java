package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import tong.statmod.STATMod;
import tong.statmod.client.ClientMagicCache;
import tong.statmod.client.inscription.KnownSpellIconButton;
import tong.statmod.client.inscription.KnownSpellUiState;
import tong.statmod.integration.ironspells.IronInscriptionKnownSpellIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(InscriptionTableScreen.class)
public abstract class IronInscriptionTableScreenMixin extends AbstractContainerScreen<InscriptionTableMenu> {
    @Shadow protected Button inscribeButton;
    @Shadow private int selectedSpellIndex;
    @Shadow private void setSelectedIndex(int index) {}

    @Unique
    private List<KnownSpellIconButton> statmod$knownSpellButtons = new ArrayList<>();
    @Unique
    private Button statmod$prevKnownSpellPageButton;
    @Unique
    private Button statmod$nextKnownSpellPageButton;
    @Unique
    private int statmod$knownSpellPage;
    @Unique
    private Integer statmod$selectedKnownSpellOption;
    @Unique
    private KnownSpellUiState statmod$lastKnownSpellState;
    @Unique
    private EditBox statmod$searchBox;
    @Unique
    private String statmod$searchQuery = "";

    private IronInscriptionTableScreenMixin(InscriptionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void statmod$addKnownSpellButtons(CallbackInfo ci) {
        this.statmod$knownSpellButtons = new ArrayList<>();
        for (int i = 0; i < IronInscriptionKnownSpellIndex.PAGE_SIZE; i++) {
            final int visibleIndex = i;
            KnownSpellIconButton button = this.addRenderableWidget(
                    new KnownSpellIconButton(0, 0, () -> statmod$selectKnownSpell(visibleIndex)));
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

        this.statmod$searchBox = new EditBox(this.font, 0, 0, 84, 12,
                Component.translatable("statmod.spell.search.placeholder"));
        this.statmod$searchBox.setMaxLength(32);
        this.statmod$searchBox.setHint(Component.translatable("statmod.spell.search.placeholder"));
        this.statmod$searchBox.setResponder(value -> {
            this.statmod$searchQuery = value == null ? "" : value;
            this.statmod$knownSpellPage = 0;
            this.statmod$refreshKnownSpellButtons();
        });
        this.addRenderableWidget(this.statmod$searchBox);

        this.statmod$refreshKnownSpellButtons();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void statmod$refreshKnownSpellUi(net.minecraft.client.gui.GuiGraphics guiGraphics,
                                             int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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
        List<String> known = statmod$filterBySearch(statmod$knownIronSpells());
        int maxPage = IronInscriptionKnownSpellIndex.maxPage(known);
        this.statmod$knownSpellPage = Mth.clamp(this.statmod$knownSpellPage, 0, maxPage);
        boolean spellBookSlotted = this.menu.getSpellBookSlot().hasItem();
        int selectedIndex = this.statmod$selectedKnownSpellOption == null ? -1 : this.statmod$selectedKnownSpellOption;
        Set<String> boundSpellIds = statmod$collectBoundSpellIds();
        KnownSpellUiState nextState = KnownSpellUiState.capture(
                known,
                this.statmod$knownSpellPage,
                selectedIndex,
                spellBookSlotted,
                boundSpellIds);
        if (nextState.equals(this.statmod$lastKnownSpellState)) {
            return;
        }
        this.statmod$lastKnownSpellState = nextState;

        if (STATMod.LOGGER.isDebugEnabled()) {
            STATMod.LOGGER.debug("KnownSpellButtons refreshed: {} filtered irons_spellbooks spells, page={}/{}",
                    known.size(), this.statmod$knownSpellPage, maxPage);
        }

        List<String> page = IronInscriptionKnownSpellIndex.page(known, this.statmod$knownSpellPage);
        int baseX = this.leftPos + this.imageWidth + 6;
        int searchBoxHeight = 14;
        int baseY = this.topPos + 18 + searchBoxHeight + 4;
        int rowStep = KnownSpellIconButton.SIZE + 2;

        if (this.statmod$searchBox != null) {
            this.statmod$searchBox.setX(baseX);
            this.statmod$searchBox.setY(this.topPos + 18);
            this.statmod$searchBox.setWidth(KnownSpellIconButton.SIZE);
        }

        for (int i = 0; i < this.statmod$knownSpellButtons.size(); i++) {
            KnownSpellIconButton button = this.statmod$knownSpellButtons.get(i);
            button.setX(baseX);
            button.setY(baseY + i * rowStep);
            if (i < page.size()) {
                int globalIndex = this.statmod$knownSpellPage * IronInscriptionKnownSpellIndex.PAGE_SIZE + i;
                String spellId = page.get(i);
                AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
                button.visible = spell != null;
                button.active = spellBookSlotted && spell != null;
                button.setSelected(globalIndex == selectedIndex);
                button.setBound(boundSpellIds.contains(spellId));
                if (this.minecraft != null) {
                    button.setSpell(spell, 1, this.minecraft.player);
                }
            } else {
                button.visible = false;
                button.active = false;
                button.setSelected(false);
                button.setBound(false);
                button.setSpell(null, 1, this.minecraft != null ? this.minecraft.player : null);
            }
        }

        int pagerY = baseY + IronInscriptionKnownSpellIndex.PAGE_SIZE * rowStep;
        if (this.statmod$prevKnownSpellPageButton != null) {
            this.statmod$prevKnownSpellPageButton.setX(baseX);
            this.statmod$prevKnownSpellPageButton.setY(pagerY);
            this.statmod$prevKnownSpellPageButton.visible = known.size() > IronInscriptionKnownSpellIndex.PAGE_SIZE;
            this.statmod$prevKnownSpellPageButton.active = this.statmod$knownSpellPage > 0;
        }
        if (this.statmod$nextKnownSpellPageButton != null) {
            this.statmod$nextKnownSpellPageButton.setX(baseX + KnownSpellIconButton.SIZE - 12);
            this.statmod$nextKnownSpellPageButton.setY(pagerY);
            this.statmod$nextKnownSpellPageButton.visible = known.size() > IronInscriptionKnownSpellIndex.PAGE_SIZE;
            this.statmod$nextKnownSpellPageButton.active = this.statmod$knownSpellPage < maxPage;
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
        return IronInscriptionKnownSpellIndex.learnedCastableSpellIds(ClientMagicCache.getLearnedSpells());
    }

    /**
     * Filtre la liste de sorts par sous-chaîne (case-insensitive) sur le nom du sort
     * (display name si possible, sinon last segment de l'ID). Sortie inchangée si la query
     * est vide. Utilisé par le widget de recherche.
     */
    @Unique
    private List<String> statmod$filterBySearch(List<String> all) {
        String query = this.statmod$searchQuery;
        if (query == null || query.isBlank()) {
            return all;
        }
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String spellId : all) {
            if (statmod$matchesQuery(spellId, needle)) {
                filtered.add(spellId);
            }
        }
        return List.copyOf(filtered);
    }

    @Unique
    private boolean statmod$matchesQuery(String spellId, String needle) {
        if (spellId == null) return false;
        if (spellId.toLowerCase(java.util.Locale.ROOT).contains(needle)) return true;
        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
        if (spell == null) return false;
        if (this.minecraft == null || this.minecraft.player == null) {
            return spell.getSpellName().toLowerCase(java.util.Locale.ROOT).contains(needle);
        }
        String display = spell.getDisplayName(this.minecraft.player).getString();
        return display.toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    /**
     * Énumère les sorts déjà inscrits dans le grimoire actuellement déposé dans le slot. Vide
     * si aucun grimoire n'est slotté ou s'il n'expose pas de {@link ISpellContainer} (ex. item
     * non-grimoire mais accepté par le slot).
     */
    @Unique
    private Set<String> statmod$collectBoundSpellIds() {
        ItemStack spellBookStack = this.menu.getSpellBookSlot().getItem();
        if (spellBookStack == null || spellBookStack.isEmpty()) {
            return Set.of();
        }
        ISpellContainer container = ISpellContainer.get(spellBookStack);
        if (container == null) {
            return Set.of();
        }
        Set<String> bound = new HashSet<>();
        int slots = container.getMaxSpellCount();
        for (int i = 0; i < slots; i++) {
            SpellData data = container.getSpellAtIndex(i);
            if (data == null || data == SpellData.EMPTY) continue;
            AbstractSpell spell = data.getSpell();
            if (spell == null) continue;
            ResourceLocation rl = spell.getSpellResource();
            if (rl != null) {
                bound.add(rl.toString());
            }
        }
        return bound;
    }

}

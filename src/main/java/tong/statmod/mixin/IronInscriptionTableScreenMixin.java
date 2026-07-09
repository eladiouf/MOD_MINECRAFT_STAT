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
import tong.statmod.client.inscription.KnownSpellFilters;
import tong.statmod.client.inscription.KnownSpellIconButton;
import tong.statmod.client.inscription.KnownSpellUiState;
import tong.statmod.integration.ironspells.IronInscriptionKnownSpellIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mixin(InscriptionTableScreen.class)
public abstract class IronInscriptionTableScreenMixin extends AbstractContainerScreen<InscriptionTableMenu> {
    @Unique private static final int statmod$schoolButtonWidth = 20;
    @Unique private static final int statmod$schoolButtonHeight = 14;
    @Unique private static final int statmod$schoolButtonGap = 2;
    @Unique private static final SchoolFilterSpec[] statmod$schoolFilters = new SchoolFilterSpec[] {
            new SchoolFilterSpec("fire", "Fi"),
            new SchoolFilterSpec("ice", "Ic"),
            new SchoolFilterSpec("nature", "Na"),
            new SchoolFilterSpec("lightning", "Li"),
            new SchoolFilterSpec("evocation", "Ev"),
            new SchoolFilterSpec("holy", "Ho"),
            new SchoolFilterSpec("blood", "Bl"),
            new SchoolFilterSpec("ender", "En"),
            new SchoolFilterSpec("eldritch", "El")
    };

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
    private List<Button> statmod$schoolFilterButtons = new ArrayList<>();
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
    @Unique
    private Set<String> statmod$activeSchoolFilters = new LinkedHashSet<>();

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

        this.statmod$schoolFilterButtons = new ArrayList<>();
        for (SchoolFilterSpec filter : statmod$schoolFilters) {
            Button button = Button.builder(statmod$schoolButtonLabel(filter), b -> {
                statmod$toggleSchoolFilter(filter.id());
                b.setMessage(statmod$schoolButtonLabel(filter));
            }).bounds(0, 0, statmod$schoolButtonWidth, statmod$schoolButtonHeight).build();
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("statmod.spell.filter.school", statmod$schoolDisplayName(filter.id()))));
            this.statmod$schoolFilterButtons.add(this.addRenderableWidget(button));
        }

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
        List<String> known = statmod$knownIronSpells();
        List<String> page = IronInscriptionKnownSpellIndex.page(
                known,
                this.statmod$knownSpellPage,
                this::statmod$matchesActiveFilters);
        if (visibleIndex < 0 || visibleIndex >= page.size()) {
            return;
        }

        String spellId = page.get(visibleIndex);
        int globalIndex = IronInscriptionKnownSpellIndex.optionIndexOf(known, spellId);
        if (globalIndex < 0) {
            return;
        }
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
        int maxPage = IronInscriptionKnownSpellIndex.maxPage(known, this::statmod$matchesActiveFilters);
        this.statmod$knownSpellPage = Mth.clamp(this.statmod$knownSpellPage, 0, maxPage);
        boolean spellBookSlotted = this.menu.getSpellBookSlot().hasItem();
        int selectedIndex = this.statmod$selectedKnownSpellOption == null ? -1 : this.statmod$selectedKnownSpellOption;
        selectedIndex = IronInscriptionKnownSpellIndex.normalizeSelectedOption(
                known,
                selectedIndex,
                this::statmod$matchesActiveFilters);
        this.statmod$selectedKnownSpellOption = selectedIndex >= 0 ? selectedIndex : null;
        Set<String> boundSpellIds = statmod$collectBoundSpellIds();
        List<String> visible = IronInscriptionKnownSpellIndex.page(known, this.statmod$knownSpellPage, this::statmod$matchesActiveFilters);
        KnownSpellUiState nextState = KnownSpellUiState.capture(
                visible,
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
                    visible.size(), this.statmod$knownSpellPage, maxPage);
        }

        List<String> page = visible;
        int columnWidth = 70;
        int baseX = this.leftPos - columnWidth - 6;
        int searchBoxHeight = statmod$schoolButtonHeight;
        int schoolRows = (int) Math.ceil(statmod$schoolFilters.length / 3.0);
        int schoolFiltersHeight = schoolRows * statmod$schoolButtonHeight + Math.max(0, schoolRows - 1) * statmod$schoolButtonGap;
        int baseY = this.topPos + 18 + searchBoxHeight + 4 + schoolFiltersHeight + 4;
        int rowStep = KnownSpellIconButton.SIZE + 2;
        int cols = 2;
        int colWidth = KnownSpellIconButton.SIZE + 4;

        if (this.statmod$searchBox != null) {
            this.statmod$searchBox.setX(baseX);
            this.statmod$searchBox.setY(this.topPos + 18);
            this.statmod$searchBox.setWidth(columnWidth);
        }

        for (int i = 0; i < this.statmod$schoolFilterButtons.size(); i++) {
            Button button = this.statmod$schoolFilterButtons.get(i);
            int col = i % 3;
            int row = i / 3;
            button.setX(baseX + col * (statmod$schoolButtonWidth + statmod$schoolButtonGap));
            button.setY(this.topPos + 18 + searchBoxHeight + 4 + row * (statmod$schoolButtonHeight + statmod$schoolButtonGap));
            button.visible = true;
            button.active = true;
        }

        for (int i = 0; i < this.statmod$knownSpellButtons.size(); i++) {
            KnownSpellIconButton button = this.statmod$knownSpellButtons.get(i);
            int col = i % cols;
            int row = i / cols;
            button.setX(baseX + col * colWidth);
            button.setY(baseY + row * rowStep);
            if (i < page.size()) {
                String spellId = page.get(i);
                int globalIndex = IronInscriptionKnownSpellIndex.optionIndexOf(known, spellId);
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

        int pagerRow = (IronInscriptionKnownSpellIndex.PAGE_SIZE + cols - 1) / cols;
        int pagerY = baseY + pagerRow * rowStep;
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

    @Unique
    private boolean statmod$matchesActiveFilters(String spellId) {
        return KnownSpellFilters.matches(
                spellId,
                new KnownSpellFilters.Criteria(this.statmod$searchQuery, this.statmod$activeSchoolFilters),
                this::statmod$displayNameForFilter,
                this::statmod$schoolIdForFilter);
    }

    @Unique
    private String statmod$displayNameForFilter(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
        if (spell == null) return null;
        if (this.minecraft == null || this.minecraft.player == null) {
            return spell.getSpellName();
        }
        return spell.getDisplayName(this.minecraft.player).getString();
    }

    @Unique
    private String statmod$schoolIdForFilter(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
        if (spell == null || spell.getSchoolType() == null || spell.getSchoolType().getId() == null) {
            return null;
        }
        return spell.getSchoolType().getId().getPath();
    }

    @Unique
    private void statmod$toggleSchoolFilter(String schoolId) {
        if (!this.statmod$activeSchoolFilters.add(schoolId)) {
            this.statmod$activeSchoolFilters.remove(schoolId);
        }
        this.statmod$knownSpellPage = 0;
        for (int i = 0; i < statmod$schoolFilters.length; i++) {
            SchoolFilterSpec filter = statmod$schoolFilters[i];
            this.statmod$schoolFilterButtons.get(i).setMessage(statmod$schoolButtonLabel(filter));
        }
        this.statmod$refreshKnownSpellButtons();
    }

    @Unique
    private Component statmod$schoolButtonLabel(SchoolFilterSpec filter) {
        boolean active = this.statmod$activeSchoolFilters.contains(filter.id());
        return Component.literal(active ? "[" + filter.shortLabel() + "]" : filter.shortLabel());
    }

    @Unique
    private Component statmod$schoolDisplayName(String schoolId) {
        return Component.translatable("statmod.spell.school." + schoolId);
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

    @Unique
    private record SchoolFilterSpec(String id, String shortLabel) {}
}

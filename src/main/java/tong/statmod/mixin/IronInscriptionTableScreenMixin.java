package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.inscription.KnownSpellIconButton;
import tong.statmod.integration.ironspells.IronKnownSpellIndex;
import tong.statmod.integration.ironspells.LearnedSpellBindingPolicy;

@Mixin(value = InscriptionTableScreen.class, remap = false)
public abstract class IronInscriptionTableScreenMixin
        extends AbstractContainerScreen<InscriptionTableMenu> {
    @Shadow
    private int selectedSpellIndex;

    @Shadow
    private void setSelectedIndex(int index) {
    }

    @Unique
    private final List<KnownSpellIconButton> statmod$knownButtons = new ArrayList<>();
    @Unique
    private final List<Button> statmod$schoolButtons = new ArrayList<>();
    @Unique
    private final Set<String> statmod$activeSchools = new LinkedHashSet<>();
    @Unique
    private List<String> statmod$renderedSchools = List.of();
    @Unique
    private EditBox statmod$search;
    @Unique
    private Button statmod$previous;
    @Unique
    private Button statmod$next;
    @Unique
    private int statmod$page;
    @Unique
    private String statmod$selectedSpellId;
    @Unique
    private long statmod$lastRevision = Long.MIN_VALUE;
    @Unique
    private String statmod$lastUiSignature = "";

    protected IronInscriptionTableScreenMixin(
            InscriptionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void statmod$addKnownSpellPanel(CallbackInfo ci) {
        statmod$knownButtons.clear();
        for (int index = 0; index < IronKnownSpellIndex.PAGE_SIZE; index++) {
            int visibleIndex = index;
            statmod$knownButtons.add(addRenderableWidget(new KnownSpellIconButton(
                    0, 0, () -> statmod$selectVisible(visibleIndex))));
        }
        statmod$search = addRenderableWidget(new EditBox(
                font, 0, 0, 112, 14,
                Component.translatable("statmod.spell.search.placeholder")));
        statmod$search.setMaxLength(64);
        statmod$search.setHint(Component.translatable("statmod.spell.search.placeholder"));
        statmod$search.setResponder(value -> {
            statmod$page = 0;
            statmod$forceRefresh();
        });
        statmod$previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            statmod$page = Math.max(0, statmod$page - 1);
            statmod$forceRefresh();
        }).bounds(0, 0, 14, 14).build());
        statmod$next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            statmod$page++;
            statmod$forceRefresh();
        }).bounds(0, 0, 14, 14).build());
        statmod$forceRefresh();
    }

    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"))
    private void statmod$refreshDuringRender(
            net.minecraft.client.gui.GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        statmod$refresh(false);
    }

    @Inject(method = "isValidInscription", at = @At("HEAD"), cancellable = true)
    private void statmod$allowLearnedSpellInscription(
            CallbackInfoReturnable<Boolean> cir) {
        if (statmod$selectedSpellId != null
                && menu.getSpellBookSlot().hasItem()
                && !menu.getScrollSlot().hasItem()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onInscription", at = @At("HEAD"), cancellable = true)
    private void statmod$inscribeLearnedSpellWithoutScroll(CallbackInfo ci) {
        if (statmod$selectedSpellId == null || menu.getScrollSlot().hasItem()) {
            return;
        }
        ItemStack book = menu.getSpellBookSlot().getItem();
        if (book.isEmpty() || !ISpellContainer.isSpellContainer(book)) {
            ci.cancel();
            return;
        }
        ISpellContainer container = ISpellContainer.get(book);
        int target = selectedSpellIndex;
        if (container == null) {
            ci.cancel();
            return;
        }
        if (target < 0 || target >= container.getMaxSpellCount()
                || container.getSpellAtIndex(target) != SpellData.EMPTY) {
            target = container.getNextAvailableIndex();
        }
        if (target < 0) {
            ci.cancel();
            return;
        }
        setSelectedIndex(target);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, -1);
        }
        ci.cancel();
    }

    @Unique
    private void statmod$selectVisible(int visibleIndex) {
        List<IronKnownSpellIndex.Entry> visible = statmod$visibleEntries();
        List<IronKnownSpellIndex.Entry> pageEntries = IronKnownSpellIndex.page(visible, statmod$page);
        if (visibleIndex < 0 || visibleIndex >= pageEntries.size()) {
            return;
        }
        statmod$selectedSpellId = pageEntries.get(visibleIndex).id();
        List<IronKnownSpellIndex.Entry> canonical = statmod$canonicalEntries();
        int option = IronKnownSpellIndex.optionIndexOf(canonical, statmod$selectedSpellId);
        int buttonId = LearnedSpellBindingPolicy.buttonForOption(option);
        Minecraft client = minecraft;
        if (buttonId >= 0 && client != null && client.gameMode != null) {
            client.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
        statmod$forceRefresh();
    }

    @Unique
    private void statmod$forceRefresh() {
        statmod$lastUiSignature = "";
        statmod$refresh(true);
    }

    @Unique
    private void statmod$refresh(boolean force) {
        if (statmod$search == null) {
            return;
        }
        Map<String, Integer> learned = ClientStatsCache.state().learnedSpells();
        Set<String> discovered = IronKnownSpellIndex.schools(
                learned, this::statmod$schoolId, this::statmod$isRegistered);
        List<String> schoolList = List.copyOf(discovered);
        if (!schoolList.equals(statmod$renderedSchools)) {
            statmod$rebuildSchoolButtons(schoolList);
        }
        List<IronKnownSpellIndex.Entry> visible = statmod$visibleEntries();
        int maxPage = IronKnownSpellIndex.maxPage(visible);
        statmod$page = Math.max(0, Math.min(maxPage, statmod$page));
        if (statmod$selectedSpellId != null
                && !ClientStatsCache.state().learnedSpells().containsKey(statmod$selectedSpellId)) {
            statmod$selectedSpellId = null;
        }
        Set<String> bound = statmod$boundSpellIds();
        String signature = ClientStatsCache.state().revision() + "|" + visible + "|"
                + statmod$page + "|" + statmod$selectedSpellId + "|" + bound + "|"
                + menu.getSpellBookSlot().hasItem() + "|" + statmod$activeSchools;
        if (!force && signature.equals(statmod$lastUiSignature)
                && statmod$lastRevision == ClientStatsCache.state().revision()) {
            return;
        }
        statmod$lastUiSignature = signature;
        statmod$lastRevision = ClientStatsCache.state().revision();

        int baseX = Math.max(4, leftPos - 122);
        int top = topPos + 8;
        statmod$search.setX(baseX);
        statmod$search.setY(top);
        int schoolRows = (statmod$schoolButtons.size() + 2) / 3;
        for (int index = 0; index < statmod$schoolButtons.size(); index++) {
            Button button = statmod$schoolButtons.get(index);
            button.setX(baseX + (index % 3) * 39);
            button.setY(top + 18 + (index / 3) * 16);
        }
        int iconsY = top + 20 + schoolRows * 16;
        List<IronKnownSpellIndex.Entry> pageEntries = IronKnownSpellIndex.page(visible, statmod$page);
        boolean hasBook = menu.getSpellBookSlot().hasItem();
        for (int index = 0; index < statmod$knownButtons.size(); index++) {
            KnownSpellIconButton button = statmod$knownButtons.get(index);
            button.setX(baseX + (index % 2) * 24);
            button.setY(iconsY + (index / 2) * 22);
            if (index >= pageEntries.size()) {
                button.active = false;
                button.setSelected(false);
                button.setBound(false);
                button.setSpell(null, 1, minecraft == null ? null : minecraft.player);
                continue;
            }
            IronKnownSpellIndex.Entry entry = pageEntries.get(index);
            AbstractSpell spell = SpellRegistry.getSpell(entry.id());
            button.active = hasBook && spell != null && spell != SpellRegistry.none();
            button.setSelected(entry.id().equals(statmod$selectedSpellId));
            button.setBound(bound.contains(entry.id()));
            button.setSpell(spell, entry.level(), minecraft == null ? null : minecraft.player);
        }
        int pagerY = iconsY + 68;
        statmod$previous.setX(baseX);
        statmod$previous.setY(pagerY);
        statmod$previous.visible = maxPage > 0;
        statmod$previous.active = statmod$page > 0;
        statmod$next.setX(baseX + 34);
        statmod$next.setY(pagerY);
        statmod$next.visible = maxPage > 0;
        statmod$next.active = statmod$page < maxPage;
    }

    @Unique
    private void statmod$rebuildSchoolButtons(List<String> schools) {
        statmod$schoolButtons.forEach(this::removeWidget);
        statmod$schoolButtons.clear();
        statmod$renderedSchools = List.copyOf(schools);
        statmod$activeSchools.retainAll(schools);
        for (String school : schools) {
            Button button = Button.builder(statmod$schoolLabel(school), pressed -> {
                if (!statmod$activeSchools.add(school)) {
                    statmod$activeSchools.remove(school);
                }
                pressed.setMessage(statmod$schoolLabel(school));
                statmod$page = 0;
                statmod$forceRefresh();
            }).bounds(0, 0, 37, 14).build();
            button.setTooltip(Tooltip.create(Component.literal(school)));
            statmod$schoolButtons.add(addRenderableWidget(button));
        }
    }

    @Unique
    private Component statmod$schoolLabel(String school) {
        String shortName = school.length() <= 3 ? school : school.substring(0, 3);
        return Component.literal(statmod$activeSchools.contains(school)
                ? "[" + shortName + "]" : shortName);
    }

    @Unique
    private List<IronKnownSpellIndex.Entry> statmod$visibleEntries() {
        return IronKnownSpellIndex.visible(
                ClientStatsCache.state().learnedSpells(),
                statmod$search == null ? "" : statmod$search.getValue(),
                statmod$activeSchools,
                this::statmod$displayName,
                this::statmod$schoolId,
                this::statmod$isRegistered);
    }

    @Unique
    private List<IronKnownSpellIndex.Entry> statmod$canonicalEntries() {
        return ClientStatsCache.state().learnedSpells().entrySet().stream()
                .filter(entry -> statmod$isRegistered(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new IronKnownSpellIndex.Entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Unique
    private boolean statmod$isRegistered(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        return spell != null && spell != SpellRegistry.none();
    }

    @Unique
    private String statmod$displayName(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return spellId;
        }
        return minecraft == null || minecraft.player == null
                ? spell.getSpellName()
                : spell.getDisplayName(minecraft.player).getString();
    }

    @Unique
    private String statmod$schoolId(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        return spell == null || spell == SpellRegistry.none()
                || spell.getSchoolType() == null || spell.getSchoolType().getId() == null
                ? ""
                : spell.getSchoolType().getId().toString();
    }

    @Unique
    private Set<String> statmod$boundSpellIds() {
        ItemStack book = menu.getSpellBookSlot().getItem();
        if (book.isEmpty() || !ISpellContainer.isSpellContainer(book)) {
            return Set.of();
        }
        ISpellContainer container = ISpellContainer.get(book);
        if (container == null) {
            return Set.of();
        }
        Set<String> bound = new HashSet<>();
        for (int index = 0; index < container.getMaxSpellCount(); index++) {
            SpellData data = container.getSpellAtIndex(index);
            if (data != null && data != SpellData.EMPTY && data.getSpell() != null
                    && data.getSpell().getSpellResource() != null) {
                bound.add(data.getSpell().getSpellResource().toString());
            }
        }
        return bound;
    }
}

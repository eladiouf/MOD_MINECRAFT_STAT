package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public final class IronLearnedSpellBindingService {
    private IronLearnedSpellBindingService() {
    }

    public static List<String> knownRegisteredSpellIds(Map<String, Integer> learned) {
        if (learned == null || learned.isEmpty()) {
            return List.of();
        }
        return learned.keySet().stream()
                .filter(id -> {
                    AbstractSpell spell = SpellRegistry.getSpell(id);
                    return spell != null && spell != SpellRegistry.none();
                })
                .sorted()
                .limit(tong.statmod.magic.LearnedSpellState.MAX_ENTRIES)
                .toList();
    }

    public static boolean bind(
            InscriptionTableMenu menu,
            Player player,
            String spellId,
            int targetSlot,
            Map<String, Integer> learned) {
        if (menu == null || player == null || spellId == null || targetSlot < 0) {
            return false;
        }
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }
        int level = LearnedSpellBindingPolicy.authorizedLevel(
                learned, spellId, spell.getMinLevel(), spell.getMaxLevel());
        if (level <= 0) {
            return false;
        }
        ItemStack book = menu.getSpellBookSlot().getItem();
        if (book.isEmpty() || !ISpellContainer.isSpellContainer(book)) {
            return false;
        }
        ISpellContainer current = ISpellContainer.get(book);
        if (current == null || targetSlot >= current.getMaxSpellCount()
                || current.getSpellAtIndex(targetSlot) != SpellData.EMPTY) {
            return false;
        }
        InscribeSpellEvent event = new InscribeSpellEvent(
                player, new SpellData(spell, level));
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        ISpellContainerMutable mutable = current.mutableCopy();
        if (!mutable.addSpellAtIndex(spell, level, targetSlot, false)) {
            return false;
        }
        ISpellContainer.set(book, mutable.toImmutable());
        menu.getSpellBookSlot().setChanged();
        menu.broadcastChanges();
        return true;
    }
}

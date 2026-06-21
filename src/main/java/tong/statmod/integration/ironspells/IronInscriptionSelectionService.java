package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import tong.statmod.storage.PlayerStatData;

import java.util.Arrays;
import java.util.List;

public final class IronInscriptionSelectionService {
    private IronInscriptionSelectionService() {}

    public static List<String> knownIronSpellIds(PlayerStatData data) {
        if (data == null) {
            return List.of();
        }
        return IronInscriptionKnownSpellIndex.learnedIronSpellIds(Arrays.asList(data.getLearnedSpells()));
    }

    public static String resolveSelectedSpellId(PlayerStatData data, int optionIndex) {
        if (optionIndex < 0) {
            return null;
        }
        List<String> known = knownIronSpellIds(data);
        return optionIndex < known.size() ? known.get(optionIndex) : null;
    }

    public static boolean inscribeSelectedSpell(InscriptionTableMenu menu, int targetSlot, String spellId, Player player) {
        if (menu == null || player == null || targetSlot < 0 || spellId == null || spellId.isBlank()) {
            return false;
        }

        ItemStack spellBookStack = menu.getSpellBookSlot().getItem();
        if (!(spellBookStack.getItem() instanceof SpellBook)) {
            return false;
        }

        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(spellId));
        if (spell == null) {
            return false;
        }

        SpellData spellData = new SpellData(spell, 1);
        InscribeSpellEvent event = NeoForge.EVENT_BUS.post(new InscribeSpellEvent(player, spellData));
        if (event.isCanceled()) {
            return false;
        }

        ISpellContainer spellBookContainer = ISpellContainer.get(spellBookStack);
        ISpellContainerMutable mutable = spellBookContainer.mutableCopy();
        if (!mutable.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), targetSlot, false)) {
            return false;
        }

        ISpellContainer.set(spellBookStack, mutable.toImmutable());
        menu.getSpellBookSlot().setChanged();
        menu.broadcastChanges();
        return true;
    }
}

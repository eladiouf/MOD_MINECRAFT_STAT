package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.STATMod;
import tong.statmod.integration.ironspells.LearnedSpellCastPolicy;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

@Mixin(SpellSelectionManager.class)
public class SpellSelectionManagerMixin {

    @Shadow
    private Player player;

    @Shadow
    private List<SpellSelectionManager.SelectionOption> selectionOptionList;

    @Shadow
    private SpellSelection spellSelection;

    @Shadow
    private int selectionIndex;

    @Shadow
    private boolean selectionValid;

    @Inject(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;",
            shift = At.Shift.BEFORE), require = 1)
    private void statmod$addLearnedSpells(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        String[] learned = data.getLearnedSpells();
        if (learned == null || learned.length == 0) return;

        // Capture le sort actuellement sélectionné (résolu par Iron's pendant initItem, ex. le
        // sort du grimoire en main) AVANT de muter la liste : le removeIf ci-dessous décale les
        // positions, or selectionIndex est une POSITION → sans cette restauration il pointerait
        // ensuite sur un autre sort (un sort appris injecté). C'est la cause du "il lance le sort
        // de l'arbre au lieu de celui du grimoire".
        AbstractSpell previouslySelected = null;
        if (selectionValid && selectionIndex >= 0 && selectionIndex < selectionOptionList.size()) {
            previouslySelected = selectionOptionList.get(selectionIndex).spellData.getSpell();
        }

        for (int i = 0; i < learned.length; i++) {
            String spellId = learned[i];
            if (spellId == null || spellId.isBlank()) continue;
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spell == null) {
                STATMod.LOGGER.warn("SpellSelectionManager: unknown learned spell '{}'", spellId);
                continue;
            }
            SpellData spellData = new SpellData(spell, 1);
            // Le sort appris prime sur le doublon fourni par l'équipement : sans ça, la
            // sélection retombe silencieusement sur "mainhand" et le cast exige un staff.
            selectionOptionList.removeIf(existing ->
                    existing.spellData.getSpell().equals(spell));
            // globalIndex temporaire : renuméroté ci-dessous pour respecter l'invariant d'Iron's.
            SpellSelectionManager.SelectionOption option = new SpellSelectionManager.SelectionOption(
                    spellData,
                    LearnedSpellCastPolicy.SLOT,
                    i,
                    0);
            selectionOptionList.add(option);
        }

        // (a) Invariant Iron's : globalIndex == position dans la liste. Le removeIf ci-dessus
        // décale les positions sans toucher aux globalIndex des options d'équipement → la roue
        // (getGlobalSelectionIndex/HUD) et le matching de sélection se désynchronisaient. On
        // renumérote tout après injection.
        for (int gi = 0; gi < selectionOptionList.size(); gi++) {
            selectionOptionList.get(gi).globalIndex = gi;
        }

        // (b) Restaure la sélection sur le MÊME sort qu'avant la mutation (grimoire, staff, ou
        // appris) — indépendamment de sa nouvelle position/slot. Corrige le "lance le mauvais
        // sort" causé par le décalage de positions du removeIf.
        if (previouslySelected != null) {
            for (int pos = 0; pos < selectionOptionList.size(); pos++) {
                if (selectionOptionList.get(pos).spellData.getSpell().equals(previouslySelected)) {
                    selectionIndex = pos;
                    selectionValid = true;
                    break;
                }
            }
        } else if (spellSelection != null && LearnedSpellCastPolicy.isVirtualSlot(spellSelection.equipmentSlot)) {
            // Sélection persistée sur un sort appris (slot virtuel) jamais validée par Iron's
            // (les appris sont ajoutés après initItem) → on la retrouve par slotIndex.
            int wantSlotIndex = spellSelection.index;
            for (int pos = 0; pos < selectionOptionList.size(); pos++) {
                SpellSelectionManager.SelectionOption opt = selectionOptionList.get(pos);
                if (LearnedSpellCastPolicy.isVirtualSlot(opt.slot) && opt.slotIndex == wantSlotIndex) {
                    selectionIndex = pos;
                    selectionValid = true;
                    break;
                }
            }
        }
    }
}

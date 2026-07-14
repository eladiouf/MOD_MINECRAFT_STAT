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
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

/**
 * Injecte les sorts appris de l'arbre dans la roue de sélection, castables depuis la <b>main</b>
 * (slot {@link SpellSelectionManager#MAINHAND} → source de cast {@code SWORD} → l'objet support
 * est l'arme tenue). Couplé à {@code IronWeaponCastImplementMixin} qui fait reconnaître n'importe
 * quelle arme comme objet de cast valide, on caste ses sorts appris au <b>clic-droit</b> en tenant
 * son arme de combat — sans staff ni grimoire en main.
 *
 * <p>On ne remplace jamais un sort déjà proposé par un objet équipé (grimoire curio, staff) : ce
 * sort-là reste castable nativement. On ne fait qu'<b>ajouter</b> les sorts appris manquants.
 */
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
        if (player == null) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        String[] learned = data.getLearnedSpells();
        if (learned == null || learned.length == 0) return;

        // Capture la sélection courante (résolue par Iron's) AVANT de muter la liste, pour la
        // restaurer ensuite sur le même sort (l'ajout décale sinon selectionIndex → mauvais sort).
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
            // Déjà proposé par un objet équipé (grimoire curio, staff en main) → laisser Iron's
            // le caster nativement, ne pas dupliquer.
            final AbstractSpell learnedSpell = spell;
            boolean alreadyOffered = selectionOptionList.stream()
                    .anyMatch(existing -> existing.spellData.getSpell().equals(learnedSpell));
            if (alreadyOffered) continue;
            // Slot MAINHAND → getCastSource() = SWORD → le cast utilise l'objet en main (l'arme),
            // rendu valide par IronWeaponCastImplementMixin. globalIndex renuméroté plus bas.
            selectionOptionList.add(new SpellSelectionManager.SelectionOption(
                    new SpellData(spell, 1),
                    SpellSelectionManager.MAINHAND,
                    i,
                    0));
        }

        // Invariant Iron's : globalIndex == position dans la liste (la roue/HUD indexent dessus).
        for (int gi = 0; gi < selectionOptionList.size(); gi++) {
            selectionOptionList.get(gi).globalIndex = gi;
        }

        // Restaure la sélection sur le même sort qu'avant la mutation.
        if (previouslySelected != null) {
            for (int pos = 0; pos < selectionOptionList.size(); pos++) {
                if (selectionOptionList.get(pos).spellData.getSpell().equals(previouslySelected)) {
                    selectionIndex = pos;
                    selectionValid = true;
                    break;
                }
            }
        }
    }
}

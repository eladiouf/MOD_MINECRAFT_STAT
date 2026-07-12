package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicBranch;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

@Mixin(SpellSelectionManager.class)
public class SpellSelectionManagerMixin {

    @Shadow
    private Player player;

    @Shadow
    private List<SpellSelectionManager.SelectionOption> selectionOptionList;

    @Inject(method = "init", at = @At("TAIL"))
    private void statmod$addLearnedSpells(CallbackInfo ci) {
        if (player == null || player.level() == null || player.level().isClientSide) {
            return;
        }
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data == null) return;
        String[] learned = data.getLearnedSpells();
        if (learned == null || learned.length == 0) return;

        for (int i = 0; i < learned.length; i++) {
            String spellId = learned[i];
            if (spellId == null || spellId.isBlank()) continue;
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spell == null) {
                STATMod.LOGGER.warn("SpellSelectionManager: unknown learned spell '{}'", spellId);
                continue;
            }
            SpellData spellData = new SpellData(spell, 1);
            SpellSelectionManager.SelectionOption option = new SpellSelectionManager.SelectionOption(
                    spellData, "statmod", i, selectionOptionList.size());
            selectionOptionList.add(option);
        }
    }
}

package tong.statmod.mixin;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.magic.MagicRace;

import java.util.List;

@Mixin(ReincarnationMenu.class)
public abstract class ReincarnationMenuRaceFilterMixin {
    @Shadow private int racePool;

    @Inject(method = "getRacePool", at = @At("RETURN"), cancellable = true)
    private void statmod$restrictStartingRacePool(CallbackInfoReturnable<List<ManasRace>> cir) {
        if (this.racePool == 1 || this.racePool == 2) {
            return;
        }
        List<ManasRace> filtered = filterStartingRaces(cir.getReturnValue());
        if (!filtered.isEmpty()) {
            cir.setReturnValue(filtered);
        }
    }

    @Inject(method = "getRandomRacePool", at = @At("RETURN"), cancellable = true)
    private void statmod$restrictStartingRandomPool(CallbackInfoReturnable<List<ManasRace>> cir) {
        if (this.racePool == 1 || this.racePool == 2) {
            return;
        }
        List<ManasRace> filtered = filterStartingRaces(cir.getReturnValue());
        if (!filtered.isEmpty()) {
            cir.setReturnValue(filtered);
        }
    }

    private static List<ManasRace> filterStartingRaces(List<ManasRace> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(race -> race != null && MagicRace.isAllowedStartingRace(race.getRegistryName()))
                .toList();
    }
}

package tong.statmod.mixin;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(ReincarnationMenu.class)
public abstract class ReincarnationMenuRaceFilterMixin {
    @Shadow private int racePool;

    private static final Set<ResourceLocation> STATMOD$ALLOWED_STARTING_RACES = Set.of(
            ResourceLocation.fromNamespaceAndPath("tensura", "human"),
            ResourceLocation.fromNamespaceAndPath("tensura", "elf"),
            ResourceLocation.fromNamespaceAndPath("tensura", "dwarf"),
            ResourceLocation.fromNamespaceAndPath("tensura", "beastfolk")
    );

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
                .filter(race -> race != null && STATMOD$ALLOWED_STARTING_RACES.contains(race.getRegistryName()))
                .toList();
    }
}

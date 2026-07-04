package tong.statmod.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import tong.statmod.STATMod;

import java.util.Iterator;

/**
 * Mission M6 — Contrôle total du loot des mobs dans le donjon (2026-07-04).
 *
 * <p>Le {@link tong.statmod.loot.DungeonMobLootReplacer} (Global Loot Modifier) ne voit que les
 * drops qui passent par le système de <b>loot table</b> vanilla. Or beaucoup de mods souls-like —
 * <b>SLU en particulier</b> (entités MCreator) — droppent leurs items via l'équipement porté
 * ({@code dropCustomDeathLoot}) ou du code de mort custom, <b>sans loot table</b>. Ces drops
 * échappaient donc au replacer → armes/armures SLU par terre dans le donjon.
 *
 * <p>{@link LivingDropsEvent} capture <b>tous</b> les drops (équipement + custom + loot table),
 * quelle qu'en soit la source. Ici, dans la dimension donjon, on retire tout drop qui n'est pas un
 * item {@code statmod:} (nos Rune Shards). Les shards, eux, sont injectés par le replacer/altar.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonDropGuard {

    private DungeonDropGuard() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }

        int removed = 0;
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next().getItem();
            if (!isAllowedInDungeon(stack)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            STATMod.LOGGER.debug("[TrialDungeon] {} drops non-statmod retirés (mob {})",
                    removed, event.getEntity().getType());
        }
    }

    /** Seuls nos items {@code statmod:} (Rune Shards & co.) sont autorisés à drop dans le donjon. */
    private static boolean isAllowedInDungeon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return STATMod.MODID.equals(id.getNamespace());
    }
}

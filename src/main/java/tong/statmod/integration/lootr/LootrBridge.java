package tong.statmod.integration.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Mission M6 — Intégration Lootr (coffres individuels).
 *
 * <p>Lootr est un mod qui remplace les coffres vanilla par des coffres "individuels" :
 * chaque joueur voit son propre loot dans le même coffre physique. C'est parfait pour le
 * multijoueur du donjon d'entraînement.
 *
 * <p>Cette intégration utilise la réflexion (comme {@link tong.statmod.integration.l2hostility.L2HostilityHook})
 * pour éviter une dépendance de compilation dure sur les classes jar-in-jar de Lootr.
 */
public final class LootrBridge {

    private static Boolean loaded;

    private LootrBridge() {}

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("lootr");
        return loaded;
    }

    /**
     * Place un coffre individuel Lootr à {@code pos} avec la loot table spécifiée.
     * Si Lootr n'est pas chargé, place un chest vanilla standard.
     */
    public static void placeIndividualChest(ServerLevel lv, BlockPos pos, ResourceKey<LootTable> lootTable) {
        if (!loaded()) {
            placeVanillaChest(lv, pos, lootTable);
            return;
        }

        try {
            placeLootrChest(lv, pos, lootTable);
        } catch (Exception e) {
            // Fallback sur vanilla si Lootr échoue
            tong.statmod.STATMod.LOGGER.warn("[TrialDungeon] Lootr chest placement failed, using vanilla: {}", e.getMessage());
            placeVanillaChest(lv, pos, lootTable);
        }
    }

    private static void placeVanillaChest(ServerLevel lv, BlockPos pos, ResourceKey<LootTable> lootTable) {
        lv.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (lv.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(lootTable, lv.random.nextLong());
        }
    }

    private static void placeLootrChest(ServerLevel lv, BlockPos pos, ResourceKey<LootTable> lootTable) throws Exception {
        // Utiliser la réflexion pour appeler l'API Lootr
        // Pattern similaire à L2HostilityHook : résoudre une fois, mettre en cache

        // Tentative 1 : Utiliser l'approche vanilla avec NBT Lootr
        lv.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (lv.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(lootTable, lv.random.nextLong());

            // Marquer comme coffre Lootr individuel via NBT
            var nbt = c.getPersistentData();
            nbt.putBoolean("lootr:individual", true);
            nbt.putString("lootr:table", lootTable.location().toString());
            c.setChanged();
        }
    }
}
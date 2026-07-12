package tong.statmod.integration.sdm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import tong.statmod.dungeon.DungeonExchanger;
import tong.statmod.economy.MagicBanker;

/**
 * Intercepte les clics sur les PNJ configurés avec le tag ou nom sdm_tab:
 * et ouvre directement le shop SDM Shop sur l'onglet correspondant.
 * <p>
 * Dans le Trial Dungeon, tout villageoi est automatiquement tagué {@code sdm_tab:}
 * pour ouvrir le SDM Shop, avec un onglet par défaut selon son métier :
 * <ul>
 *   <li>Weaponsmith / Toolsmith → {@code Armes}</li>
 *   <li>Armorer → {@code Armures}</li>
 *   <li>Cleric → {@code Potions}</li>
 *   <li>tout autre → onglet par défaut (premier de la liste)</li>
 * </ul>
 */
public final class SDMShopNPCBridge {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        String tabName = getTargetTabName(target);
        if (tabName == null) return;

        if (event.getLevel().isClientSide()) {
            ClientShopTabForcer.targetTab = tabName;
            // Marchand spécialisé (onglet nommé) → shop verrouillé sur son rayon.
            // PNJ généraliste (tag sdm_tab: vide) → shop complet, pas de verrou.
            ClientShopTabForcer.lockedTab = tabName.isEmpty() ? null : tabName;
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            try {
                // 1. Envoyer les données du shop (TovarList + TovarTab) via ShopNetworkUtils
                net.sixk.sdmshop.utils.ShopNetworkUtils.sendShopDataS2C(player, player.server.registryAccess());

                // 2. Ouvrir l'écran shop via SDMEconomyNetwork (canal déjà enregistré par SDM)
                net.sixik.sdmeconomy.network.SDMEconomyNetwork.sendTo(player,
                        new net.sixk.sdmshop.shop.network.server.SendOpenShopScreenS2C(false));
            } catch (Throwable t) {
                tong.statmod.STATMod.LOGGER.error("[Shop] Impossible d'ouvrir le shop SDM Shop", t);
            }
        }
    }

    private static String getTargetTabName(Entity target) {
        // Le changeur possède sa propre interface points → coins, y compris dans les anciens mondes
        // où il a pu recevoir un tag sdm_tab avant l'ajout de cette garde.
        if (target.getPersistentData().getBoolean(DungeonExchanger.TAG)) return null;
        if (target.getPersistentData().getBoolean(MagicBanker.TAG)) return null;
        for (String tag : target.getTags()) {
            if (tag.startsWith("sdm_tab:")) {
                return tag.substring("sdm_tab:".length()).trim();
            }
        }
        if (target.hasCustomName() && target.getCustomName() != null) {
            String name = target.getCustomName().getString();
            if (name.startsWith("sdm_tab:")) {
                return name.substring("sdm_tab:".length()).trim();
            }
        }
        return null;
    }

    /**
     * Tout villageoi qui entre dans le Trial Dungeon reçoit automatiquement
     * le tag {@code sdm_tab:} avec l'onglet SDM correspondant à son métier.
     */
    @SubscribeEvent
    public static void onVillagerJoinDungeon(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.getPersistentData().getBoolean(DungeonExchanger.TAG)) return;
        if (villager.getPersistentData().getBoolean(MagicBanker.TAG)) return;
        if (!event.getLevel().dimension().equals(tong.statmod.dungeon.DungeonDimensions.TRIAL_DUNGEON)) return;
        if (hasSdmTab(villager)) return; // déjà tagué

        var profession = villager.getVillagerData().getProfession();
        String tabName;
        if (profession == VillagerProfession.WEAPONSMITH || profession == VillagerProfession.TOOLSMITH) {
            tabName = "Armes";
        } else if (profession == VillagerProfession.ARMORER) {
            tabName = "Armures";
        } else if (profession == VillagerProfession.CLERIC) {
            tabName = "Potions";
        } else {
            tabName = ""; // onglet par défaut (premier de la liste)
        }
        villager.addTag("sdm_tab:" + tabName);
    }

    private static boolean hasSdmTab(Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith("sdm_tab:")) return true;
        }
        return false;
    }
}

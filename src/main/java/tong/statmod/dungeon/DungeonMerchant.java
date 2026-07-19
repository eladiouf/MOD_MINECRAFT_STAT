package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;
import static tong.statmod.dungeon.DungeonArchitect.stair;

/**
 * Mission M6 — Stands de vente du donjon (2026-07-05).
 *
 * <p>Les étages TRÉSOR ont des pièces libres → on en fait un <b>marché</b> : des stands avec un
 * <b>villageois marchand bloqué</b> (NoAI, invulnérable, ne despawn pas) derrière un comptoir, avec
 * qui le joueur peut <b>trader</b> (émeraudes trouvées dans les coffres → équipement/consommables
 * utiles). Chaque stand a un métier + un stock différents. Thématisé par {@link BlockPalette}.
 */
public final class DungeonMerchant {

    private DungeonMerchant() {}

    /** Nombre de métiers/stocks disponibles (rotation par index de stand). */
    public static final int STALL_KINDS = 6;

    /**
     * Marqueur NBT des marchands. Ils sont autorisés (AUTHORIZED_TAG) pour passer le garde de spawn,
     * mais ce tag les EXEMPTE du nettoyage/comptage des mobs de combat ({@link DungeonMobSpawner}) —
     * sinon une mort sur l'étage les supprimerait définitivement (l'étage n'est généré qu'une fois).
     */
    public static final String MERCHANT_TAG = "statmod_dungeon_merchant";

    /**
     * Bâtit un stand de marché au centre d'une pièce (repère local {@code cx,cz}) et y installe un
     * marchand bloqué. {@code kind} choisit le métier + le stock (0..{@link #STALL_KINDS}-1).
     */
    public static void placeStall(ServerLevel lv, BlockPos sp, int cx, int cz, BlockPalette t, int kind, int floor) {
        BlockState counter = B(t.slab());
        BlockState post = B(t.decorPrimary());
        BlockState roof = B(t.ceiling());
        BlockState light = B(t.light());

        // Comptoir 3 de large (dalles), face au sud (côté joueur).
        for (int dx = -1; dx <= 1; dx++) {
            S(lv, O(sp, cx + dx, 0, cz), counter);
        }
        // 2 poteaux + auvent + fanaux.
        for (int dx = -2; dx <= 2; dx += 4) {
            S(lv, O(sp, cx + dx, 0, cz), post);
            S(lv, O(sp, cx + dx, 1, cz), post);
            S(lv, O(sp, cx + dx, 2, cz), post);
            S(lv, O(sp, cx + dx, 3, cz - 1), light);
        }
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, cz), roof);
            S(lv, O(sp, cx + dx, 3, cz - 1), stair(t.stair(), Direction.SOUTH));
        }

        // Marchand bloqué DERRIÈRE le comptoir (côté nord), face au joueur (sud).
        BlockPos standPos = O(sp, cx, 1, cz - 1);
        spawnLockedTrader(lv, standPos, kind, floor);
    }

    /** Villageois marchand bloqué (NoAI, invulnérable, persistant) avec un stock fixe. */
    private static void spawnLockedTrader(ServerLevel lv, BlockPos pos, int kind, int floor) {
        Villager v = EntityType.VILLAGER.create(lv);
        if (v == null) return;
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f); // face sud
        v.setYBodyRot(180f);
        v.setYHeadRot(180f);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, professionFor(kind), 5));
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setSilent(true);
        v.setOffers(offersFor(kind, floor)); // stock fixe (après setVillagerData → non écrasé)
        v.getPersistentData().putBoolean(MERCHANT_TAG, true); // exempt du nettoyage des mobs de combat
        DungeonSpawnGuard.spawnAuthorized(() -> { lv.addFreshEntity(v); return v; });
    }

    private static VillagerProfession professionFor(int kind) {
        return switch (kind % STALL_KINDS) {
            case 0 -> VillagerProfession.WEAPONSMITH;
            case 1 -> VillagerProfession.ARMORER;
            case 2 -> VillagerProfession.CLERIC;
            case 3 -> VillagerProfession.FLETCHER;
            case 4 -> VillagerProfession.LIBRARIAN;
            default -> VillagerProfession.FARMER;
        };
    }

    /** Stock fixe par métier — émeraudes → objets utiles (plus profond = un peu plus cher). */
    private static MerchantOffers offersFor(int kind, int floor) {
        MerchantOffers offers = new MerchantOffers();
        int tier = Math.min(4, floor / 20); // 0..4 selon la profondeur
        switch (kind % STALL_KINDS) {
            case 0 -> { // WEAPONSMITH
                offers.add(buy(Items.DIAMOND_SWORD, 1, 12 + tier * 2));
                offers.add(buy(Items.DIAMOND_AXE, 1, 12 + tier * 2));
                offers.add(sell(Items.IRON_INGOT, 4, 1));
            }
            case 1 -> { // ARMORER
                offers.add(buy(Items.DIAMOND_CHESTPLATE, 1, 16 + tier * 3));
                offers.add(buy(Items.DIAMOND_HELMET, 1, 10 + tier * 2));
                offers.add(buy(Items.SHIELD, 1, 6));
            }
            case 2 -> { // CLERIC
                offers.add(buy(Items.ENDER_PEARL, 2, 6 + tier));
                offers.add(buy(Items.GOLDEN_APPLE, 1, 8 + tier * 2));
                offers.add(buy(Items.EXPERIENCE_BOTTLE, 4, 4));
            }
            case 3 -> { // FLETCHER
                offers.add(buy(Items.ARROW, 16, 2));
                offers.add(buy(Items.BOW, 1, 6));
                offers.add(buy(Items.SPECTRAL_ARROW, 8, 4));
            }
            case 4 -> { // LIBRARIAN
                offers.add(buy(Items.ENCHANTED_BOOK, 1, 20 + tier * 4));
                offers.add(buy(Items.BOOKSHELF, 2, 3));
                offers.add(buy(Items.NAME_TAG, 1, 8));
            }
            default -> { // FARMER
                offers.add(buy(Items.GOLDEN_CARROT, 6, 4));
                offers.add(buy(Items.GOLDEN_APPLE, 1, 8 + tier));
                offers.add(buy(Items.COOKED_BEEF, 8, 2));
            }
        }
        return offers;
    }

    /** Offre d'achat : {@code emeralds} émeraudes → {@code count}×{@code item}. */
    private static MerchantOffer buy(net.minecraft.world.item.Item item, int count, int emeralds) {
        return new MerchantOffer(new ItemStack(Items.EMERALD, Math.max(1, emeralds)),
                new ItemStack(item, count), 8, 2, 0.05f);
    }

    /** Offre de vente : {@code count}×{@code item} → {@code emeralds} émeraudes. */
    private static MerchantOffer sell(net.minecraft.world.item.Item item, int count, int emeralds) {
        return new MerchantOffer(new ItemStack(item, count),
                new ItemStack(Items.EMERALD, Math.max(1, emeralds)), 12, 1, 0.05f);
    }
}

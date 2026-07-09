package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Chambre-forte ULTRA-SECRÈTE (feedback playtest 2026-07-09 ; entrée « sanctuaire » au lieu d'une
 * plaque générique, 2ᵉ feedback du même jour).
 *
 * <p>~1 étage de combat sur 7 cache, quelque part au sol d'une pièce, un <b>sanctuaire</b> : une
 * pierre de guidage (lodestone) surmontée d'un cristal d'améthyste — le seul bloc « magnétique »
 * du donjon, l'anomalie que l'explorateur attentif remarque. <b>Clic droit sur la pierre</b> →
 * téléportation (son d'enderman + particules de portail) dans une chambre-forte scellée flottant
 * haut au-dessus de l'île (dans la cage barrière → impossible de s'en échapper autrement) :
 * <ul>
 *   <li>au centre, un <b>piédestal</b> portant un coffre avec une <b>arme unique</b> du modpack
 *       (Simply Swords runic → légendaires → armes de boss SLU selon la profondeur), renommée en
 *       relique ;</li>
 *   <li>4 coffres de trésor riches ({@code dungeon_treasure}) aux coins + blocs précieux ;</li>
 *   <li>une seconde pierre de guidage pour <b>revenir</b> au pad d'apparition de l'étage.</li>
 * </ul>
 *
 * <p>La téléportation est gérée en Java ({@link #onUseLodestone}) : toute lodestone du donjon est
 * forcément à nous (la pose de blocs y est interdite aux joueurs). La direction est déduite de
 * l'altitude : en bas → on entre ; dans la chambre → on ressort.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = tong.statmod.STATMod.MODID)
public final class DungeonUltraVault {

    /** Position de la chambre relative au centre d'île : haut dans la cage, décalée du donjon. */
    private static final int VX = 60, VY = 36, VZ = 60;
    /** Demi-taille intérieure de la chambre (9×9 intérieur). */
    private static final int HALF = 5;

    /** Armes runiques (étages < 30). Ids vérifiés contre simplyswords-1.63.0. */
    private static final List<String> WEAPONS_MID = List.of(
            "simplyswords:runic_katana", "simplyswords:runic_greataxe", "simplyswords:runic_scythe",
            "simplyswords:runic_twinblade", "simplyswords:runic_halberd", "simplyswords:runic_longsword");
    /** Légendaires (étages 30-69). Ids vérifiés (simplyswords + slu). */
    private static final List<String> WEAPONS_HIGH = List.of(
            "simplyswords:brimstone_claymore", "simplyswords:hearthflame", "simplyswords:soulkeeper",
            "simplyswords:soulpyre", "slu:black_katana", "slu:executioner_sword", "slu:farron_greatsword");
    /** Armes de boss (étages ≥ 70). Ids vérifiés contre slu_9992.3. */
    private static final List<String> WEAPONS_ABYSS = List.of(
            "slu:aatrox_sword", "slu:darkmoon_greatsword", "slu:cinder_king_greatsword",
            "slu:berserk_greatsword", "slu:godfrey_axe", "slu:dragon_slayer_axe");

    private DungeonUltraVault() {}

    /** ~1 étage de combat sur 7 possède une chambre-forte ultra-secrète. Pur/testable. */
    public static boolean isVaultFloor(int floor) {
        return DungeonTeleportHandler.isCombatFloor(floor)
                && Math.floorMod(floor * 7919 + 11, 7) == 0;
    }

    /** Pose le sanctuaire d'entrée : pierre de guidage + cristal d'améthyste au sommet. */
    static void placeShrine(ServerLevel lv, BlockPos pos) {
        S(lv, pos, B(Blocks.LODESTONE));
        S(lv, pos.above(), B(Blocks.AMETHYST_CLUSTER));
    }

    /**
     * Clic droit sur une pierre de guidage du donjon → téléportation. En bas (étage) → on entre
     * dans la chambre ; en haut (chambre) → on ressort au pad d'apparition. Toute lodestone du
     * donjon est à nous : la pose de blocs y est interdite aux joueurs en survie.
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onUseLodestone(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!event.getLevel().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.LODESTONE)) return;

        int floor = DungeonTeleportHandler.floorAtPos(event.getPos().getX(), event.getPos().getZ());
        if (!isVaultFloor(floor)) return;

        BlockPos islandSp = DungeonTeleportHandler.floorSpawnPos(floor);
        boolean inVault = player.getBlockY() >= islandSp.getY() + VY - 2;
        BlockPos dest = inVault
                ? DungeonTeleportHandler.floorPlayerSpawnPos(floor)
                : O(islandSp, VX, VY + 1, VZ - HALF + 2); // bord sud de la chambre

        ServerLevel lv = player.serverLevel();
        lv.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        player.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, inVault ? 1.2f : 0.7f);
        player.displayClientMessage(Component.translatable(
                inVault ? "dungeon.ultravault.return" : "dungeon.ultravault.enter"), true);
        event.setCanceled(true);
    }

    /** Construit la chambre-forte au-dessus de l'île (appelé une fois par étage élu). */
    static void build(ServerLevel lv, BlockPos islandSp, BlockPalette t, int floor) {
        BlockPos c = O(islandSp, VX, VY, VZ); // centre du SOL de la chambre
        BlockState base = B(t.base());
        BlockState wall = B(t.wallBlock());
        BlockState accent = B(t.accent());

        // Coque scellée : sol, murs, plafond (aucune ouverture — on entre et sort par plaque).
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                boolean edge = Math.abs(dx) == HALF + 1 || Math.abs(dz) == HALF + 1;
                S(lv, c.offset(dx, 0, dz), Math.abs(dx) == Math.abs(dz) ? accent : base); // sol
                S(lv, c.offset(dx, 5, dz), base);                                          // plafond
                for (int y = 1; y <= 4; y++) {
                    S(lv, c.offset(dx, y, dz), edge ? wall : B(Blocks.AIR));
                }
            }
        }
        // Lumière aux 4 coins intérieurs.
        for (int[] k : new int[][]{{-HALF + 1, -HALF + 1}, {HALF - 1, -HALF + 1},
                                   {-HALF + 1, HALF - 1}, {HALF - 1, HALF - 1}}) {
            S(lv, c.offset(k[0], 4, k[1]), B(t.light()));
        }

        // ── Piédestal central : colonne décorée + coffre-relique au sommet ──
        S(lv, c.offset(0, 1, 0), B(t.decorPrimary()));
        S(lv, c.offset(0, 2, 0), B(Blocks.GOLD_BLOCK));
        BlockPos relicChest = c.offset(0, 3, 0);
        S(lv, relicChest, B(Blocks.CHEST));
        if (lv.getBlockEntity(relicChest) instanceof ChestBlockEntity chest) {
            chest.setItem(13, relicWeapon(floor)); // slot central du coffre
        }

        // ── 4 coffres de trésor riches aux coins + blocs précieux ──
        DungeonArchitect.placeChest(lv, c.offset(-HALF + 1, 1, -HALF + 1));
        DungeonArchitect.placeChest(lv, c.offset(HALF - 1, 1, -HALF + 1));
        DungeonArchitect.placeChest(lv, c.offset(-HALF + 1, 1, HALF - 1));
        DungeonArchitect.placeChest(lv, c.offset(HALF - 1, 1, HALF - 1));
        S(lv, c.offset(-2, 1, 0), B(Blocks.DIAMOND_BLOCK));
        S(lv, c.offset(2, 1, 0), B(Blocks.GOLD_BLOCK));
        S(lv, c.offset(0, 1, 2), B(Blocks.EMERALD_BLOCK));

        // ── Pierre de RETOUR (bord nord) : clic droit → ramène au pad d'apparition de l'étage ──
        placeShrine(lv, c.offset(0, 1, HALF - 1));
    }

    /** L'arme-relique de l'étage : pool selon la profondeur, renommée, premier id présent. */
    private static ItemStack relicWeapon(int floor) {
        List<String> pool = floor >= 70 ? WEAPONS_ABYSS : floor >= 30 ? WEAPONS_HIGH : WEAPONS_MID;
        Item item = resolveItem(pool.get(Math.floorMod(floor * 131, pool.size())));
        if (item == null) {
            for (String id : pool) { // l'élu est absent → premier disponible du pool
                item = resolveItem(id);
                if (item != null) break;
            }
        }
        if (item == null) item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("netherite_sword"));
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("§6⚔ ").append(stack.getHoverName().copy().withStyle(s -> s.withColor(0xFFAA00).withItalic(false)))
                        .append(Component.literal(" §7— Relique du Donjon")));
        return stack;
    }

    private static Item resolveItem(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) return null;
        return BuiltInRegistries.ITEM.get(loc);
    }
}

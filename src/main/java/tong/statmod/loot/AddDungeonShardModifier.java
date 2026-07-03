package tong.statmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.item.RuneShardIds;
import tong.statmod.item.RuneShards;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Mission M6 — Phase δ.
 *
 * <p>Loot modifier qui injecte un {@code RuneShard} dans le loot d'un mob ou d'un coffre
 * <b>uniquement si le contexte est dans la dimension {@code statmod:trial_dungeon}</b>. La rareté
 * du shard est pondérée par l'étage (via le même mapping XZ->floor que le téléporteur) :
 * <ul>
 *   <li>Étages 1-10 : COMMON principalement, UNCOMMON ponctuellement</li>
 *   <li>Étages 11-25 : COMMON/UNCOMMON, RARE occasionnel</li>
 *   <li>Étages 26-50 : UNCOMMON/RARE, EPIC ponctuel</li>
 *   <li>Étages 51-100 : RARE/EPIC/LEGENDARY</li>
 * </ul>
 *
 * <p>La famille du shard est aléatoire — ça garantit que le joueur amasse un panier varié plutôt
 * qu'une seule pile monolithique.
 */
public class AddDungeonShardModifier extends LootModifier {

    public AddDungeonShardModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static final MapCodec<AddDungeonShardModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, AddDungeonShardModifier::new)
    );

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        Level level = ctx.getLevel();
        if (level == null || !DungeonDimensions.TRIAL_DUNGEON.equals(level.dimension())) {
            return loot;
        }

        int x = originX(ctx);
        int z = originZ(ctx);
        int floor = floorAtPos(x, z);

        RuneShardIds.Rarity rarity = pickRarity(floor, ctx);
        RuneShardIds.Family family = pickFamily(ctx);
        String shardId = RuneShardIds.id(rarity, family);

        Supplier<Item> supplier = RuneShards.SHARDS.get(shardId);
        if (supplier != null) {
            loot.add(new ItemStack(supplier.get()));
        }
        return loot;
    }

    /** Position X du contexte (mob ou coffre). Fallback à 0 si indisponible. */
    private static int originX(LootContext ctx) {
        try {
            var origin = ctx.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if (origin != null) return (int) origin.x;
        } catch (Exception ignored) {
        }
        return 0;
    }

    /** Position Z du contexte (mob ou coffre). Fallback à 0 si indisponible. */
    private static int originZ(LootContext ctx) {
        try {
            var origin = ctx.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if (origin != null) return (int) origin.z;
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int floorAtPos(int x, int z) {
        int col = Math.floorDiv(x + 40, 80);
        int row = Math.floorDiv(z + 40, 80);
        int floor = row * 10 + col + 1;
        return Math.max(1, floor);
    }

    /**
     * Rareté pondérée par étage. Utilise le random du contexte pour déterminisme (respect des
     * seeds).
     */
    private static RuneShardIds.Rarity pickRarity(int floor, LootContext ctx) {
        List<RuneShardIds.Rarity> pool = new ArrayList<>();
        if (floor <= 10) {
            for (int i = 0; i < 6; i++) pool.add(RuneShardIds.Rarity.COMMON);
            for (int i = 0; i < 3; i++) pool.add(RuneShardIds.Rarity.UNCOMMON);
            pool.add(RuneShardIds.Rarity.RARE);
        } else if (floor <= 25) {
            for (int i = 0; i < 4; i++) pool.add(RuneShardIds.Rarity.COMMON);
            for (int i = 0; i < 4; i++) pool.add(RuneShardIds.Rarity.UNCOMMON);
            for (int i = 0; i < 2; i++) pool.add(RuneShardIds.Rarity.RARE);
        } else if (floor <= 50) {
            for (int i = 0; i < 2; i++) pool.add(RuneShardIds.Rarity.COMMON);
            for (int i = 0; i < 4; i++) pool.add(RuneShardIds.Rarity.UNCOMMON);
            for (int i = 0; i < 3; i++) pool.add(RuneShardIds.Rarity.RARE);
            pool.add(RuneShardIds.Rarity.EPIC);
        } else {
            for (int i = 0; i < 3; i++) pool.add(RuneShardIds.Rarity.RARE);
            for (int i = 0; i < 3; i++) pool.add(RuneShardIds.Rarity.EPIC);
            for (int i = 0; i < 2; i++) pool.add(RuneShardIds.Rarity.LEGENDARY);
        }
        return pool.get(ctx.getRandom().nextInt(pool.size()));
    }

    /** Famille uniformément aléatoire. */
    private static RuneShardIds.Family pickFamily(LootContext ctx) {
        RuneShardIds.Family[] fams = RuneShardIds.Family.values();
        return fams[ctx.getRandom().nextInt(fams.length)];
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

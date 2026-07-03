package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mission M6 — Phase δ.
 *
 * <p>25 shards {@code RuneShard} enregistrés via {@link DeferredRegister} — currency STAT MOD
 * du Trial Dungeon, complémentaire aux gems Apotheosis. Alimentera l'échange guilde en phase η.
 *
 * <p>Les IDs sont générés depuis {@link RuneShardIds#allIds()}. La map {@link #SHARDS} permet
 * de récupérer le supplier à partir de l'ID (utile pour le loot modifier).
 */
public final class RuneShards {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    /** Résolvable par ID string, l'ordre suit {@link RuneShardIds#allIds()}. */
    public static final Map<String, Supplier<Item>> SHARDS = new HashMap<>();

    static {
        for (String id : RuneShardIds.allIds()) {
            Rarity mcRarity = mapRarity(RuneShardIds.rarityOf(id));
            SHARDS.put(id, ITEMS.register(id,
                    () -> new Item(new Item.Properties().rarity(mcRarity))));
        }
    }

    private static Rarity mapRarity(RuneShardIds.Rarity r) {
        if (r == null) return Rarity.COMMON;
        return switch (r) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case EPIC -> Rarity.EPIC;
            case LEGENDARY -> Rarity.EPIC; // EPIC est la rareté vanilla max
        };
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private RuneShards() {}
}

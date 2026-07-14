package tong.statmod.economy;

import net.minecraft.world.item.Item;
import tong.statmod.item.ModItems;

import java.util.Arrays;
import java.util.List;

/** Source unique des coupures physiques de la devise FDP_cfa. */
public enum FdpDenomination {
    COIN_50("fdp_coin_50", 50),
    COIN_100("fdp_coin_100", 100),
    COIN_200("fdp_coin_200", 200),
    COIN_500("fdp_coin_500", 500),
    NOTE_1000("fdp_note_1000", 1_000),
    NOTE_2000("fdp_note_2000", 2_000),
    NOTE_5000("fdp_note_5000", 5_000),
    NOTE_10000("fdp_note_10000", 10_000);

    /**
     * Nom canonique de la devise SDM. Source de vérité unique partagée par le pont
     * ({@code SDMEconomyBridge}) et l'enregistrement du shop ({@code SDMShopDatabaseInitializer}) :
     * les deux DOIVENT référencer cette constante pour ne jamais diverger.
     */
    public static final String CURRENCY_ID = "FDP_cfa";

    private final String id;
    private final long value;

    FdpDenomination(String id, long value) {
        this.id = id;
        this.value = value;
    }

    public String id() { return id; }
    public long value() { return value; }
    public Item item() {
        return switch (this) {
            case COIN_50 -> ModItems.FDP_COIN_50.get();
            case COIN_100 -> ModItems.FDP_COIN_100.get();
            case COIN_200 -> ModItems.FDP_COIN_200.get();
            case COIN_500 -> ModItems.FDP_COIN_500.get();
            case NOTE_1000 -> ModItems.FDP_NOTE_1000.get();
            case NOTE_2000 -> ModItems.FDP_NOTE_2000.get();
            case NOTE_5000 -> ModItems.FDP_NOTE_5000.get();
            case NOTE_10000 -> ModItems.FDP_NOTE_10000.get();
        };
    }

    public static List<FdpDenomination> ascending() { return List.of(values()); }
    public static List<FdpDenomination> descending() {
        return Arrays.stream(values()).sorted((a, b) -> Long.compare(b.value, a.value)).toList();
    }

    public static long valueOf(Item item) {
        for (var denomination : values()) if (denomination.item() == item) return denomination.value;
        return 0L;
    }
}

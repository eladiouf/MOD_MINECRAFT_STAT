package tong.statmod.integration.sdmshop;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class MagicShopIds {
    private static final String PREFIX = "statmod:magic_shop:";

    private MagicShopIds() {
    }

    public static UUID tab(String schoolId) {
        return stable("tab", schoolId);
    }

    public static UUID scroll(String spellId, int level) {
        return stable("scroll", spellId + ":level:" + level);
    }

    public static UUID sale(String offerId) {
        return stable("sale", offerId);
    }

    private static UUID stable(String kind, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("identifier must not be blank");
        }
        String key = PREFIX + kind + ":" + value.trim();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}

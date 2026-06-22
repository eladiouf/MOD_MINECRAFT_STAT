package tong.statmod.integration.ironspells.bridge;

import tong.statmod.STATMod;

/**
 * Helpers de sanitisation / construction d'IDs pour les wrappers Tensura → Iron's.
 *
 * <p>Convention : <code>tensura:fire_bolt</code> → <code>statmod:tensura_fire_bolt</code>.
 * Le path retenu pour Iron's est <code>tensura_&lt;skill&gt;</code> ; tout caractère
 * non conforme à un {@code ResourceLocation} path est normalisé.
 */
public final class TensuraWrapperIds {
    public static final String WRAPPER_NAMESPACE = STATMod.MODID; // "statmod"
    public static final String WRAPPER_PREFIX = WRAPPER_NAMESPACE + ":tensura_";

    private TensuraWrapperIds() {}

    /** Convertit un ID Tensura ({@code "tensura:xxx"}) en path Iron's ({@code "tensura_xxx"}). */
    public static String pathFor(String tensuraSkillId) {
        if (tensuraSkillId == null || tensuraSkillId.isBlank()) {
            return "tensura_unknown";
        }
        return "tensura_" + sanitize(stripNamespace(tensuraSkillId));
    }

    /** Wrapper full ID e.g. {@code "statmod:tensura_fire_bolt"}. */
    public static String wrapperIdFor(String tensuraSkillId) {
        return WRAPPER_NAMESPACE + ":" + pathFor(tensuraSkillId);
    }

    /** True if the spell id is a known Tensura wrapper produced by this bridge. */
    public static boolean isWrapperId(String spellId) {
        return spellId != null && spellId.startsWith(WRAPPER_PREFIX);
    }

    /** Localisation key for the wrapper display name. */
    public static String displayNameTranslationKey(String tensuraSkillId) {
        return "statmod.spell.tensura." + sanitize(stripNamespace(tensuraSkillId)) + ".name";
    }

    /** Texture resource path used by Iron's for the spell icon (placeholder). */
    public static String iconTextureKey(String tensuraSkillId) {
        // Pointe sur un placeholder commun pour éviter d'avoir à fournir 56 PNG distincts.
        // Iron's accepte n'importe quel ResourceLocation textures/* — voir TensuraDelegatingSpell.
        return "statmod:textures/spell/tensura/default";
    }

    private static String stripNamespace(String resourceLike) {
        int sep = resourceLike.indexOf(':');
        return sep >= 0 ? resourceLike.substring(sep + 1) : resourceLike;
    }

    private static String sanitize(String raw) {
        // ResourceLocation path: [a-z0-9_/.-]
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 'A' && c <= 'Z') c = (char) (c + 32);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '/' || c == '.' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "unknown" : sb.toString();
    }
}

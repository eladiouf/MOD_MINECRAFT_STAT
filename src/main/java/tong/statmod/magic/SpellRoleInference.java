package tong.statmod.magic;

import java.util.Locale;
import java.util.Set;

/**
 * Classifier automatique qui infère un {@link SpellRole} pour un nœud du catalog à partir
 * de son id, sa branche et son kind. Applique les règles de priorité documentées dans
 * {@code SpellRole} (Mobility > Heal > Summon > Barrier > AOE > DOT > Buff > Control >
 * Precision > Spatial > Corruption > Elemental damage par défaut).
 *
 * <p>Couvre la majorité des 249 nœuds du catalog automatiquement. Les cas qui résistent à
 * l'inférence (sorts ambigus, noms exotiques) doivent être tagués manuellement dans
 * {@link MagicTreeCatalog} en passant le {@link SpellRole} explicitement au constructeur de
 * {@link MagicNode}.
 */
public final class SpellRoleInference {
    private SpellRoleInference() {}

    public static SpellRole infer(MagicBranch branch, MagicNodeKind kind, String id, Set<String> learnedSpells) {
        return switch (kind) {
            case TRUNK_FOUNDATION -> SpellRole.TRUNK_FOUNDATION;
            case BRANCH_OPENER -> SpellRole.BRANCH_OPENER;
            case BRANCH_TIER -> SpellRole.BRANCH_TIER;
            case LATEGAME_GATE -> SpellRole.BRANCH_TIER; // traité comme tier pour seuils
            case SIGNATURE_SPELL -> inferSignatureRole(branch, id, learnedSpells);
        };
    }

    private static SpellRole inferSignatureRole(MagicBranch branch, String id, Set<String> learnedSpells) {
        String slug = lastSegment(id).toLowerCase(Locale.ROOT);
        String spellSlug = spellSlug(learnedSpells);
        String haystack = slug + "|" + spellSlug;

        // P1 - Mobility (dash/step/jump/leap)
        if (containsAny(haystack, "dash", "step", "jump", "leap", "frost_step", "wind_jump",
                "blood_step", "thunder_step", "burning_dash", "ascension", "warp_portal",
                "float", "escape", "lighten")) {
            // float/escape/lighten/warp_portal are SPATIAL_VOID for Ender branch.
            if (branch == MagicBranch.ENDER) {
                return SpellRole.SPATIAL_VOID;
            }
            return SpellRole.MOBILITY;
        }

        // P2 - Heal
        if (containsAny(haystack, "heal", "cleanse", "regeneration", "healthcare", "healing_circle",
                "greater_heal", "cloud_of_regeneration")) {
            return SpellRole.HEAL;
        }

        // P3 - Summon
        if (containsAny(haystack, "summon", "polar_bear", "vex", "wisp", "spider_aspect",
                "firefly_swarm", "doppelganger", "raise_dead", "raise_hell")) {
            // raise_hell is AOE despite "raise" keyword
            if (haystack.contains("raise_hell")) {
                return SpellRole.AOE_BLAST;
            }
            return SpellRole.SUMMON;
        }

        // P4 - Barrier / Defensive
        if (containsAny(haystack, "barrier", "ice_block", "shield", "angel_wings", "magic_wall",
                "anti_magic_area", "anti_shock_area", "multilayer_barrier", "fortify",
                "oakskin", "reinforced_barrier", "wall_of_fire")) {
            // wall_of_fire is more DOT than barrier - override
            if (haystack.contains("wall_of_fire")) {
                return SpellRole.DOT_ZONE;
            }
            return SpellRole.BARRIER_DEFENSIVE;
        }

        // P5 - AOE blast (large area / explosion)
        if (containsAny(haystack, "earthquake", "meteor", "storm", "fireball", "blizzard",
                "tornado", "shockwave", "hellfire", "magma_bomb", "thunderstorm",
                "almighty_push", "blaze_storm", "fire_storm", "earth_storm", "aerial_collapse",
                "darkness_cannon", "dimension_cutter", "true_darkness")) {
            return SpellRole.AOE_BLAST;
        }

        // P6 - DOT Zone
        if (containsAny(haystack, "acid_rain", "cloud_of", "blight", "ashen_breath",
                "fire_breath", "poison_breath", "poison_splash", "scorch")) {
            return SpellRole.DOT_ZONE;
        }

        // P7 - Buff / Empowerment
        if (containsAny(haystack, "haste", "strength", "tailwind", "charge", "magic_aura",
                "blessing_of_life", "divine_smite", "flames_reborn")) {
            // divine_smite is BUFF if self-empowerment, DAMAGE if direct hit — treat as buff
            return SpellRole.BUFF_EMPOWERMENT;
        }

        // P8 - Control / Bind
        if (containsAny(haystack, "root", "bind", "ensnare", "shadow_bind", "banish",
                "curse_bind", "earth_jail", "earth_lock", "frostbite", "ice_tomb",
                "shatterpoint", "stomp")) {
            // stomp is also a damage spell but binds in place; lean BIND
            return SpellRole.CONTROL_BIND;
        }

        // P9 - Precision strike (single target ranged)
        if (containsAny(haystack, "lance", "guiding_bolt", "ray_of", "lightning_lance",
                "lightning_bolt", "fire_arrow", "poison_arrow", "ice_spikes", "icicle",
                "magic_bullet", "maximum_magic_bullet", "wind_blade", "iron_slash",
                "blood_slash", "blood_needles", "acupuncture")) {
            return SpellRole.PRECISION_STRIKE;
        }

        // P10 - Spatial / Void
        if (branch == MagicBranch.ENDER
                || containsAny(haystack, "portal", "void", "teleport", "spatial",
                "dimension", "black_hole", "gate", "spatial_storage", "clairvoyance",
                "search_enemy", "analyze", "ender")) {
            return SpellRole.SPATIAL_VOID;
        }

        // P11 - Corruption / Curse (Eldritch + some Blood)
        if (branch == MagicBranch.ELDRITCH
                || (branch == MagicBranch.BLOOD && containsAny(haystack, "drain",
                "siphon", "devour", "darkness", "shadow", "curse"))) {
            return SpellRole.CORRUPTION_CURSE;
        }

        // P12 - Elemental damage (default for elemental branches)
        return switch (branch) {
            case FIRE -> SpellRole.ELEMENTAL_DAMAGE_FIRE;
            case WATER -> SpellRole.ELEMENTAL_DAMAGE_WATER;
            case AIR -> SpellRole.ELEMENTAL_DAMAGE_AIR;
            case EARTH -> SpellRole.ELEMENTAL_DAMAGE_EARTH;
            // Non-elemental fallback: default to AOE_BLAST so the spell still gates on INTIMIDATION.
            case HOLY, BLOOD, ENDER, EVOCATION, ELDRITCH, COMMON -> SpellRole.AOE_BLAST;
        };
    }

    private static String lastSegment(String id) {
        if (id == null || id.isBlank()) return "";
        int slash = id.lastIndexOf('/');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static String spellSlug(Set<String> learnedSpells) {
        if (learnedSpells == null || learnedSpells.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String spell : learnedSpells) {
            if (spell == null) continue;
            int colon = spell.indexOf(':');
            sb.append(colon >= 0 ? spell.substring(colon + 1) : spell).append('|');
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null) return false;
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}

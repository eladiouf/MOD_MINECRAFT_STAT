package tong.statmod.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicTreeCatalog {
    public static final String LOCKED_SENTINEL = "__never__";

    private static final Map<String, MagicNode> BY_ID = new LinkedHashMap<>();
    private static final Map<MagicBranch, List<MagicNode>> BY_BRANCH = new LinkedHashMap<>();

    static {
        // Common trunk - four arcane foundations, linear chain
        add(new MagicNode("common/foundation/arcane_focus",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), Set.of()));
        add(new MagicNode("common/foundation/mana_well",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("common/foundation/cast_discipline",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T2,
                MagicCurrency.ARCANE, 2, List.of("common/foundation/mana_well"), Set.of()));
        add(new MagicNode("common/foundation/multi_school_gate",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T3,
                MagicCurrency.ARCANE, 3, List.of("common/foundation/cast_discipline"), Set.of()));

        // Fire branch - opener, T1 / T2 / T3 tier nodes, 4 signature spells
        add(new MagicNode("fire/opener/ignition",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"),
                Set.of()));
        add(new MagicNode("fire/tier/ember_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/opener/ignition"), Set.of()));
        add(new MagicNode("fire/tier/flame_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/ember_path"), Set.of()));
        add(new MagicNode("fire/tier/inferno_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("fire/tier/flame_path"), Set.of()));

        add(new MagicNode("fire/signature/firebolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                Set.of("irons_spellbooks:firebolt")));
        add(new MagicNode("fire/signature/tensura_fire_bolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                Set.of("tensura:fire_bolt")));
        add(new MagicNode("fire/signature/burning_dash",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:burning_dash")));
        add(new MagicNode("fire/signature/fireball",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:fireball")));
        add(new MagicNode("fire/signature/tensura_fire_storm",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/flame_path"),
                Set.of("tensura:fire_storm")));
        add(new MagicNode("fire/signature/fire_breath",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3,
                MagicCurrency.SCHOOL, 3, List.of("fire/tier/inferno_path"),
                Set.of("irons_spellbooks:fire_breath")));
        add(new MagicNode("fire/signature/tensura_hellfire",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3,
                MagicCurrency.SCHOOL, 3, List.of("fire/tier/inferno_path"),
                Set.of("tensura:hellfire")));
        add(sig("fire/signature/fire_arrow", MagicBranch.FIRE, MagicTier.T1, 1, "fire/tier/ember_path", "irons_spellbooks:fire_arrow"));
        add(sig("fire/signature/scorch", MagicBranch.FIRE, MagicTier.T1, 1, "fire/tier/ember_path", "irons_spellbooks:scorch"));
        add(sig("fire/signature/heat_surge", MagicBranch.FIRE, MagicTier.T2, 1, "fire/tier/flame_path", "irons_spellbooks:heat_surge"));
        add(sig("fire/signature/flaming_strike", MagicBranch.FIRE, MagicTier.T2, 1, "fire/tier/flame_path", "irons_spellbooks:flaming_strike"));
        add(sig("fire/signature/wall_of_fire", MagicBranch.FIRE, MagicTier.T2, 2, "fire/tier/flame_path", "irons_spellbooks:wall_of_fire"));
        add(sig("fire/signature/magma_bomb", MagicBranch.FIRE, MagicTier.T3, 2, "fire/tier/inferno_path", "irons_spellbooks:magma_bomb"));
        add(sig("fire/signature/blaze_storm", MagicBranch.FIRE, MagicTier.T3, 2, "fire/tier/inferno_path", "irons_spellbooks:blaze_storm"));
        add(sig("fire/signature/flaming_barrage", MagicBranch.FIRE, MagicTier.T3, 3, "fire/tier/inferno_path", "irons_spellbooks:flaming_barrage"));
        add(sig("fire/signature/raise_hell", MagicBranch.FIRE, MagicTier.T3, 3, "fire/tier/inferno_path", "irons_spellbooks:raise_hell"));
        add(sig("fire/signature/flames_reborn", MagicBranch.FIRE, MagicTier.T2, 2, "fire/tier/flame_path", "gametechbcs_spellbooks:flames_reborn"));
        add(sig("fire/signature/ashen_breath", MagicBranch.FIRE, MagicTier.T2, 2, "fire/tier/flame_path", "gametechbcs_spellbooks:ashen_breath"));
        add(sig("fire/signature/meteor_storm", MagicBranch.FIRE, MagicTier.T3, 3, "fire/tier/inferno_path", "gametechbcs_spellbooks:meteor_storm"));
        add(sig("fire/signature/pyromaniac", MagicBranch.FIRE, MagicTier.T3, 3, "fire/tier/inferno_path", "legendarymage:pyromaniac"));

        // Water branch (Ice)
        add(new MagicNode("water/opener/ice_awakening",
                MagicBranch.WATER, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"),
                Set.of()));
        add(new MagicNode("water/tier/frost_path",
                MagicBranch.WATER, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("water/opener/ice_awakening"), Set.of()));
        add(new MagicNode("water/tier/chill_path",
                MagicBranch.WATER, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("water/tier/frost_path"), Set.of()));
        add(new MagicNode("water/tier/glacier_path",
                MagicBranch.WATER, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("water/tier/chill_path"), Set.of()));
        add(sig("water/signature/snowball", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "irons_spellbooks:snowball"));
        add(sig("water/signature/icicle", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "irons_spellbooks:icicle"));
        add(sig("water/signature/cone_of_cold", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "irons_spellbooks:cone_of_cold"));
        add(sig("water/signature/frost_step", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "irons_spellbooks:frost_step"));
        add(sig("water/signature/ray_of_frost", MagicBranch.WATER, MagicTier.T2, 1, "water/tier/chill_path", "irons_spellbooks:ray_of_frost"));
        add(sig("water/signature/frostwave", MagicBranch.WATER, MagicTier.T2, 1, "water/tier/chill_path", "irons_spellbooks:frostwave"));
        add(sig("water/signature/ice_spikes", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "irons_spellbooks:ice_spikes"));
        add(sig("water/signature/ice_block", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "irons_spellbooks:ice_block"));
        add(sig("water/signature/blizzard", MagicBranch.WATER, MagicTier.T3, 2, "water/tier/glacier_path", "irons_spellbooks:blizzard"));
        add(sig("water/signature/frostbite", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "irons_spellbooks:frostbite"));
        add(sig("water/signature/ice_tomb", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "irons_spellbooks:ice_tomb"));
        add(sig("water/signature/summon_polar_bear", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "irons_spellbooks:summon_polar_bear"));
        add(sig("water/signature/shatterpoint", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "gametechbcs_spellbooks:shatterpoint"));
        add(sig("water/signature/legendary_blizzard", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "legendarymage:blizzard"));
        add(sig("water/signature/focused_ice_cone", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "legendarymage:focused_ice_cone"));
        add(sig("water/signature/giant_snowball", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "legendarymage:giant_snowball"));
        add(sig("water/signature/ice_explosion_cone", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "legendarymage:ice_explosion_cone"));
        add(sig("water/signature/living_ice_sculpture", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "legendarymage:living_ice_sculpture"));

        // Air branch (Lightning)
        add(new MagicNode("air/opener/spark_awakening",
                MagicBranch.AIR, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"),
                Set.of()));
        add(new MagicNode("air/tier/spark_path",
                MagicBranch.AIR, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("air/opener/spark_awakening"), Set.of()));
        add(new MagicNode("air/tier/storm_path",
                MagicBranch.AIR, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("air/tier/spark_path"), Set.of()));
        add(new MagicNode("air/tier/thunder_path",
                MagicBranch.AIR, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("air/tier/storm_path"), Set.of()));
        add(sig("air/signature/charge", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "irons_spellbooks:charge"));
        add(sig("air/signature/volt_strike", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "irons_spellbooks:volt_strike"));
        add(sig("air/signature/lightning_bolt", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "irons_spellbooks:lightning_bolt"));
        add(sig("air/signature/chain_lightning", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "irons_spellbooks:chain_lightning"));
        add(sig("air/signature/electrocute", MagicBranch.AIR, MagicTier.T2, 2, "air/tier/storm_path", "irons_spellbooks:electrocute"));
        add(sig("air/signature/shockwave", MagicBranch.AIR, MagicTier.T2, 2, "air/tier/storm_path", "irons_spellbooks:shockwave"));
        add(sig("air/signature/lightning_lance", MagicBranch.AIR, MagicTier.T2, 2, "air/tier/storm_path", "irons_spellbooks:lightning_lance"));
        add(sig("air/signature/thunderstorm", MagicBranch.AIR, MagicTier.T3, 2, "air/tier/thunder_path", "irons_spellbooks:thunderstorm"));
        add(sig("air/signature/ball_lightning", MagicBranch.AIR, MagicTier.T3, 3, "air/tier/thunder_path", "irons_spellbooks:ball_lightning"));
        add(sig("air/signature/ascension", MagicBranch.AIR, MagicTier.T3, 3, "air/tier/thunder_path", "irons_spellbooks:ascension"));
        add(sig("air/signature/thunder_step", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "irons_spellbooks:thunder_step"));
        add(sig("air/signature/wind_jump", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "wind_spellbooks:wind_jump"));
        add(sig("air/signature/wind_blade", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "wind_spellbooks:wind_blade"));
        add(sig("air/signature/iron_slash", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "wind_spellbooks:iron_slash"));
        add(sig("air/signature/tailwind", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "wind_spellbooks:tailwind"));
        add(sig("air/signature/aeropic", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "wind_spellbooks:aeropic"));
        add(sig("air/signature/almighty_push", MagicBranch.AIR, MagicTier.T2, 2, "air/tier/storm_path", "wind_spellbooks:almighty_push"));
        add(sig("air/signature/tornado", MagicBranch.AIR, MagicTier.T3, 3, "air/tier/thunder_path", "wind_spellbooks:tornado"));

        // Earth branch (Nature)
        add(new MagicNode("earth/opener/nature_awakening",
                MagicBranch.EARTH, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"),
                Set.of()));
        add(new MagicNode("earth/tier/poison_path",
                MagicBranch.EARTH, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("earth/opener/nature_awakening"), Set.of()));
        add(new MagicNode("earth/tier/toxin_path",
                MagicBranch.EARTH, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("earth/tier/poison_path"), Set.of()));
        add(new MagicNode("earth/tier/grand_nature_path",
                MagicBranch.EARTH, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("earth/tier/toxin_path"), Set.of()));
        add(sig("earth/signature/poison_arrow", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "irons_spellbooks:poison_arrow"));
        add(sig("earth/signature/poison_splash", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "irons_spellbooks:poison_splash"));
        add(sig("earth/signature/root", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "irons_spellbooks:root"));
        add(sig("earth/signature/oakskin", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "irons_spellbooks:oakskin"));
        add(sig("earth/signature/acid_orb", MagicBranch.EARTH, MagicTier.T2, 1, "earth/tier/toxin_path", "irons_spellbooks:acid_orb"));
        add(sig("earth/signature/poison_breath", MagicBranch.EARTH, MagicTier.T2, 1, "earth/tier/toxin_path", "irons_spellbooks:poison_breath"));
        add(sig("earth/signature/stomp", MagicBranch.EARTH, MagicTier.T2, 2, "earth/tier/toxin_path", "irons_spellbooks:stomp"));
        add(sig("earth/signature/spider_aspect", MagicBranch.EARTH, MagicTier.T2, 2, "earth/tier/toxin_path", "irons_spellbooks:spider_aspect"));
        add(sig("earth/signature/firefly_swarm", MagicBranch.EARTH, MagicTier.T2, 2, "earth/tier/toxin_path", "irons_spellbooks:firefly_swarm"));
        add(sig("earth/signature/blight", MagicBranch.EARTH, MagicTier.T3, 2, "earth/tier/grand_nature_path", "irons_spellbooks:blight"));
        add(sig("earth/signature/earthquake", MagicBranch.EARTH, MagicTier.T3, 3, "earth/tier/grand_nature_path", "irons_spellbooks:earthquake"));
        add(sig("earth/signature/gluttony", MagicBranch.EARTH, MagicTier.T3, 3, "earth/tier/grand_nature_path", "irons_spellbooks:gluttony"));
        add(sig("earth/signature/touch_dig", MagicBranch.EARTH, MagicTier.T3, 3, "earth/tier/grand_nature_path", "irons_spellbooks:touch_dig"));
        add(sig("earth/signature/ensnare", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "gametechbcs_spellbooks:ensnare"));
        add(sig("earth/signature/acid_rain", MagicBranch.EARTH, MagicTier.T2, 2, "earth/tier/toxin_path", "gametechbcs_spellbooks:acid_rain"));
        add(sig("earth/signature/aerial_collapse", MagicBranch.EARTH, MagicTier.T3, 3, "earth/tier/grand_nature_path", "gametechbcs_spellbooks:aerial_collapse"));

        // Holy branch (lategame)
        add(new MagicNode("holy/opener/light_awakening",
                MagicBranch.HOLY, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("holy/tier/bless_path",
                MagicBranch.HOLY, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("holy/opener/light_awakening"), Set.of()));
        add(new MagicNode("holy/tier/grace_path",
                MagicBranch.HOLY, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("holy/tier/bless_path"), Set.of()));
        add(new MagicNode("holy/tier/divine_path",
                MagicBranch.HOLY, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("holy/tier/grace_path"), Set.of()));
        add(sig("holy/signature/heal", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "irons_spellbooks:heal"));
        add(sig("holy/signature/wisp", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "irons_spellbooks:wisp"));
        add(sig("holy/signature/guiding_bolt", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "irons_spellbooks:guiding_bolt"));
        add(sig("holy/signature/cleanse", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "irons_spellbooks:cleanse"));
        add(sig("holy/signature/healing_circle", MagicBranch.HOLY, MagicTier.T2, 1, "holy/tier/grace_path", "irons_spellbooks:healing_circle"));
        add(sig("holy/signature/haste", MagicBranch.HOLY, MagicTier.T2, 1, "holy/tier/grace_path", "irons_spellbooks:haste"));
        add(sig("holy/signature/divine_smite", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "irons_spellbooks:divine_smite"));
        add(sig("holy/signature/fortify", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "irons_spellbooks:fortify"));
        add(sig("holy/signature/blessing_of_life", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "irons_spellbooks:blessing_of_life"));
        add(sig("holy/signature/cloud_of_regeneration", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "irons_spellbooks:cloud_of_regeneration"));
        add(sig("holy/signature/greater_heal", MagicBranch.HOLY, MagicTier.T3, 2, "holy/tier/divine_path", "irons_spellbooks:greater_heal"));
        add(sig("holy/signature/sunbeam", MagicBranch.HOLY, MagicTier.T3, 3, "holy/tier/divine_path", "irons_spellbooks:sunbeam"));
        add(sig("holy/signature/angel_wings", MagicBranch.HOLY, MagicTier.T3, 3, "holy/tier/divine_path", "irons_spellbooks:angel_wings"));
        add(sig("holy/signature/angel_wing", MagicBranch.HOLY, MagicTier.T3, 3, "holy/tier/divine_path", "irons_spellbooks:angel_wing"));
        add(sig("holy/signature/banish", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "gametechbcs_spellbooks:banish"));
        add(sig("holy/signature/nullflare", MagicBranch.HOLY, MagicTier.T3, 3, "holy/tier/divine_path", "gametechbcs_spellbooks:nullflare"));

        // Blood branch (lategame)
        add(new MagicNode("blood/opener/sanguine_awakening",
                MagicBranch.BLOOD, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("blood/tier/hemo_path",
                MagicBranch.BLOOD, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("blood/opener/sanguine_awakening"), Set.of()));
        add(new MagicNode("blood/tier/sanguine_path",
                MagicBranch.BLOOD, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("blood/tier/hemo_path"), Set.of()));
        add(new MagicNode("blood/tier/exsanguine_path",
                MagicBranch.BLOOD, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("blood/tier/sanguine_path"), Set.of()));
        add(sig("blood/signature/acupuncture", MagicBranch.BLOOD, MagicTier.T1, 1, "blood/tier/hemo_path", "irons_spellbooks:acupuncture"));
        add(sig("blood/signature/blood_needles", MagicBranch.BLOOD, MagicTier.T1, 1, "blood/tier/hemo_path", "irons_spellbooks:blood_needles"));
        add(sig("blood/signature/blood_slash", MagicBranch.BLOOD, MagicTier.T2, 2, "blood/tier/sanguine_path", "irons_spellbooks:blood_slash"));
        add(sig("blood/signature/blood_step", MagicBranch.BLOOD, MagicTier.T2, 1, "blood/tier/sanguine_path", "irons_spellbooks:blood_step"));
        add(sig("blood/signature/devour", MagicBranch.BLOOD, MagicTier.T2, 2, "blood/tier/sanguine_path", "irons_spellbooks:devour"));
        add(sig("blood/signature/heartstop", MagicBranch.BLOOD, MagicTier.T2, 2, "blood/tier/sanguine_path", "irons_spellbooks:heartstop"));
        add(sig("blood/signature/raise_dead", MagicBranch.BLOOD, MagicTier.T3, 3, "blood/tier/exsanguine_path", "irons_spellbooks:raise_dead"));
        add(sig("blood/signature/ray_of_siphoning", MagicBranch.BLOOD, MagicTier.T3, 2, "blood/tier/exsanguine_path", "irons_spellbooks:ray_of_siphoning"));
        add(sig("blood/signature/wither_skull", MagicBranch.BLOOD, MagicTier.T3, 3, "blood/tier/exsanguine_path", "irons_spellbooks:wither_skull"));
        add(sig("blood/signature/sacrifice", MagicBranch.BLOOD, MagicTier.T3, 3, "blood/tier/exsanguine_path", "irons_spellbooks:sacrifice"));
        add(sig("blood/signature/crimson_downpour", MagicBranch.BLOOD, MagicTier.T2, 2, "blood/tier/sanguine_path", "gametechbcs_spellbooks:crimson_downpour"));
        add(sig("blood/signature/call_forth_the_dead_king", MagicBranch.BLOOD, MagicTier.T3, 3, "blood/tier/exsanguine_path", "gametechbcs_spellbooks:call_forth_the_dead_king"));
        add(sig("blood/signature/resurrection_rune", MagicBranch.BLOOD, MagicTier.T3, 3, "blood/tier/exsanguine_path", "legendarymage:resurrection_rune"));

        // Ender branch (lategame)
        add(new MagicNode("ender/opener/void_awakening",
                MagicBranch.ENDER, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("ender/tier/void_path",
                MagicBranch.ENDER, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("ender/opener/void_awakening"), Set.of()));
        add(new MagicNode("ender/tier/rift_path",
                MagicBranch.ENDER, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("ender/tier/void_path"), Set.of()));
        add(new MagicNode("ender/tier/abyss_path",
                MagicBranch.ENDER, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("ender/tier/rift_path"), Set.of()));
        add(sig("ender/signature/magic_arrow", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "irons_spellbooks:magic_arrow"));
        add(sig("ender/signature/magic_missile", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "irons_spellbooks:magic_missile"));
        add(sig("ender/signature/teleport", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "irons_spellbooks:teleport"));
        add(sig("ender/signature/summon_ender_chest", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "irons_spellbooks:summon_ender_chest"));
        add(sig("ender/signature/starfall", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "irons_spellbooks:starfall"));
        add(sig("ender/signature/recall", MagicBranch.ENDER, MagicTier.T2, 1, "ender/tier/rift_path", "irons_spellbooks:recall"));
        add(sig("ender/signature/portal", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "irons_spellbooks:portal"));
        add(sig("ender/signature/evasion", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "irons_spellbooks:evasion"));
        add(sig("ender/signature/shadow_slash", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "irons_spellbooks:shadow_slash"));
        add(sig("ender/signature/echoing_strikes", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "irons_spellbooks:echoing_strikes"));
        add(sig("ender/signature/dragon_breath", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "irons_spellbooks:dragon_breath"));
        add(sig("ender/signature/black_hole", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "irons_spellbooks:black_hole"));
        add(sig("ender/signature/summon_swords", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "irons_spellbooks:summon_swords"));
        add(sig("ender/signature/counterspell", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "irons_spellbooks:counterspell"));
        add(sig("ender/signature/gravity_fissure", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "irons_spellbooks:gravity_fissure"));
        add(sig("ender/signature/astral_sense", MagicBranch.ENDER, MagicTier.T2, 1, "ender/tier/rift_path", "gametechbcs_spellbooks:astral_sense"));
        add(sig("ender/signature/displacement", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "gametechbcs_spellbooks:displacement"));
        add(sig("ender/signature/doppel_portal", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "darkdoppelganger:doppel_portal", "irons_spellbooks:doppel_portal"));
        add(sig("ender/signature/implosion", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "legendarymage:implosion"));

        // Evocation branch (lategame)
        add(new MagicNode("evocation/opener/trick_awakening",
                MagicBranch.EVOCATION, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("evocation/tier/trick_path",
                MagicBranch.EVOCATION, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("evocation/opener/trick_awakening"), Set.of()));
        add(new MagicNode("evocation/tier/illusion_path",
                MagicBranch.EVOCATION, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("evocation/tier/trick_path"), Set.of()));
        add(new MagicNode("evocation/tier/mastery_path",
                MagicBranch.EVOCATION, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("evocation/tier/illusion_path"), Set.of()));
        add(sig("evocation/signature/gust", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "irons_spellbooks:gust"));
        add(sig("evocation/signature/shield", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "irons_spellbooks:shield"));
        add(sig("evocation/signature/slow", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "irons_spellbooks:slow"));
        add(sig("evocation/signature/throw", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "irons_spellbooks:throw"));
        add(sig("evocation/signature/firecracker", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "irons_spellbooks:firecracker"));
        add(sig("evocation/signature/invisibility", MagicBranch.EVOCATION, MagicTier.T2, 2, "evocation/tier/illusion_path", "irons_spellbooks:invisibility"));
        add(sig("evocation/signature/spectral_hammer", MagicBranch.EVOCATION, MagicTier.T2, 1, "evocation/tier/illusion_path", "irons_spellbooks:spectral_hammer"));
        add(sig("evocation/signature/fang_strike", MagicBranch.EVOCATION, MagicTier.T2, 2, "evocation/tier/illusion_path", "irons_spellbooks:fang_strike"));
        add(sig("evocation/signature/fang_ward", MagicBranch.EVOCATION, MagicTier.T2, 2, "evocation/tier/illusion_path", "irons_spellbooks:fang_ward"));
        add(sig("evocation/signature/arrow_volley", MagicBranch.EVOCATION, MagicTier.T2, 2, "evocation/tier/illusion_path", "irons_spellbooks:arrow_volley"));
        add(sig("evocation/signature/summon_horse", MagicBranch.EVOCATION, MagicTier.T2, 1, "evocation/tier/illusion_path", "irons_spellbooks:summon_horse"));
        add(sig("evocation/signature/chain_creeper", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:chain_creeper"));
        add(sig("evocation/signature/lob_creeper", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:lob_creeper"));
        add(sig("evocation/signature/summon_vex", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:summon_vex"));
        add(sig("evocation/signature/arcane_shackle", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:arcane_shackle"));
        add(sig("evocation/signature/wololo", MagicBranch.EVOCATION, MagicTier.T3, 2, "evocation/tier/mastery_path", "irons_spellbooks:wololo"));
        add(sig("evocation/signature/fang_swirl", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:fang_swirl"));
        add(sig("evocation/signature/lingering_strain", MagicBranch.EVOCATION, MagicTier.T2, 1, "evocation/tier/illusion_path", "gametechbcs_spellbooks:lingering_strain"));
        add(sig("evocation/signature/creeper_revenge", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "irons_spellbooks:creeper_revenge"));
        add(sig("evocation/signature/nucreeper_strike", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "spells_gone_wrong:nucreeper_strike"));
        add(sig("evocation/signature/shotgun_creeper", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "spells_gone_wrong:shotgun_creeper"));
        add(sig("evocation/signature/elemental_burst", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "legendarymage:elemental_burst"));
        add(sig("evocation/signature/magic_shotgun", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "legendarymage:magic_shotgun"));

        // Eldritch branch (lategame)
        add(new MagicNode("eldritch/opener/dark_awakening",
                MagicBranch.ELDRITCH, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("eldritch/tier/dark_path",
                MagicBranch.ELDRITCH, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("eldritch/opener/dark_awakening"), Set.of()));
        add(new MagicNode("eldritch/tier/void_gaze_path",
                MagicBranch.ELDRITCH, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("eldritch/tier/dark_path"), Set.of()));
        add(new MagicNode("eldritch/tier/abyssal_path",
                MagicBranch.ELDRITCH, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("eldritch/tier/void_gaze_path"), Set.of()));
        add(sig("eldritch/signature/eldritch_blast", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "irons_spellbooks:eldritch_blast"));
        add(sig("eldritch/signature/planar_sight", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "irons_spellbooks:planar_sight"));
        add(sig("eldritch/signature/telekinesis", MagicBranch.ELDRITCH, MagicTier.T2, 2, "eldritch/tier/void_gaze_path", "irons_spellbooks:telekinesis"));
        add(sig("eldritch/signature/sonic_boom", MagicBranch.ELDRITCH, MagicTier.T2, 2, "eldritch/tier/void_gaze_path", "irons_spellbooks:sonic_boom"));
        add(sig("eldritch/signature/abyssal_shroud", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "irons_spellbooks:abyssal_shroud"));
        add(sig("eldritch/signature/sculk_tentacles", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "irons_spellbooks:sculk_tentacles"));
        add(sig("eldritch/signature/pocket_dimension", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "irons_spellbooks:pocket_dimension"));
        add(sig("eldritch/signature/blackout", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "gametechbcs_spellbooks:blackout"));
        add(sig("eldritch/signature/psychic_bolt", MagicBranch.ELDRITCH, MagicTier.T2, 1, "eldritch/tier/void_gaze_path", "gametechbcs_spellbooks:psychic_bolt"));
        add(sig("eldritch/signature/spectral_blink", MagicBranch.ELDRITCH, MagicTier.T2, 2, "eldritch/tier/void_gaze_path", "gametechbcs_spellbooks:spectral_blink"));
        add(sig("eldritch/signature/reversal", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "gametechbcs_spellbooks:reversal"));
        add(sig("eldritch/signature/void_tentacles", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "irons_spellbooks:void_tentacles"));
        add(sig("eldritch/signature/summon_doppel_minion", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "darkdoppelganger:summon_doppel_minion", "irons_spellbooks:summon_doppel_minion"));

        addTensuraSignatures();
    }

    private MagicTreeCatalog() {}

    private static MagicNode sig(String id, MagicBranch branch, MagicTier tier, int cost, String prereq, String... spells) {
        return new MagicNode(id, branch, MagicNodeKind.SIGNATURE_SPELL, tier, MagicCurrency.SCHOOL, cost, List.of(prereq), Set.of(spells));
    }

    private static void addTensuraSignatures() {
        // Elemental schools
        add(tensuraSig("fire/signature/tensura_fire_aspectual", MagicBranch.FIRE, MagicTier.T1, 1, "fire/tier/ember_path", "tensura:fire_aspectual"));
        add(tensuraSig("fire/signature/tensura_fire_ball", MagicBranch.FIRE, MagicTier.T1, 1, "fire/tier/ember_path", "tensura:fire_ball"));
        add(tensuraSig("fire/signature/tensura_fire_lance", MagicBranch.FIRE, MagicTier.T2, 1, "fire/tier/flame_path", "tensura:fire_lance"));
        add(tensuraSig("fire/signature/tensura_fire_wall", MagicBranch.FIRE, MagicTier.T2, 1, "fire/tier/flame_path", "tensura:fire_wall"));

        add(tensuraSig("water/signature/tensura_healing", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "tensura:healing"));
        add(tensuraSig("water/signature/tensura_recovery", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "tensura:recovery"));
        add(tensuraSig("water/signature/tensura_antidote", MagicBranch.WATER, MagicTier.T1, 1, "water/tier/frost_path", "tensura:antidote"));
        add(tensuraSig("water/signature/tensura_water_jail", MagicBranch.WATER, MagicTier.T2, 1, "water/tier/chill_path", "tensura:water_jail"));
        add(tensuraSig("water/signature/tensura_healing_rain", MagicBranch.WATER, MagicTier.T2, 2, "water/tier/chill_path", "tensura:healing_rain"));
        add(tensuraSig("water/signature/tensura_full_recovery", MagicBranch.WATER, MagicTier.T3, 3, "water/tier/glacier_path", "tensura:full_recovery"));

        add(tensuraSig("air/signature/tensura_wind_gust", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "tensura:wind_gust"));
        add(tensuraSig("air/signature/tensura_wind_protection", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "tensura:wind_protection"));
        add(tensuraSig("air/signature/tensura_wind_blade", MagicBranch.AIR, MagicTier.T1, 1, "air/tier/spark_path", "tensura:wind_blade"));
        add(tensuraSig("air/signature/tensura_tornado_blade", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "tensura:tornado_blade"));
        add(tensuraSig("air/signature/tensura_wind_cutter", MagicBranch.AIR, MagicTier.T2, 1, "air/tier/storm_path", "tensura:wind_cutter"));
        add(tensuraSig("air/signature/tensura_lightning_lance", MagicBranch.AIR, MagicTier.T3, 2, "air/tier/thunder_path", "tensura:lightning_lance"));
        add(tensuraSig("air/signature/tensura_aerial_blade", MagicBranch.AIR, MagicTier.T3, 3, "air/tier/thunder_path", "tensura:aerial_blade"));

        add(tensuraSig("earth/signature/tensura_earth_wall", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "tensura:earth_wall"));
        add(tensuraSig("earth/signature/tensura_earth_lock", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "tensura:earth_lock"));
        add(tensuraSig("earth/signature/tensura_earth_spikes", MagicBranch.EARTH, MagicTier.T1, 1, "earth/tier/poison_path", "tensura:earth_spikes"));
        add(tensuraSig("earth/signature/tensura_earth_jail", MagicBranch.EARTH, MagicTier.T2, 1, "earth/tier/toxin_path", "tensura:earth_jail"));
        add(tensuraSig("earth/signature/tensura_earth_storm", MagicBranch.EARTH, MagicTier.T2, 2, "earth/tier/toxin_path", "tensura:earth_storm"));
        add(tensuraSig("earth/signature/tensura_magma_surge", MagicBranch.EARTH, MagicTier.T3, 3, "earth/tier/grand_nature_path", "tensura:magma_surge"));

        // Advanced non-elemental Tensura schools
        add(tensuraSig("holy/signature/tensura_magic_wall", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "tensura:magic_wall"));
        add(tensuraSig("holy/signature/tensura_barrier", MagicBranch.HOLY, MagicTier.T1, 1, "holy/tier/bless_path", "tensura:barrier"));
        add(tensuraSig("holy/signature/tensura_reinforced_barrier", MagicBranch.HOLY, MagicTier.T2, 1, "holy/tier/grace_path", "tensura:reinforced_barrier"));
        add(tensuraSig("holy/signature/tensura_anti_shock_area", MagicBranch.HOLY, MagicTier.T2, 1, "holy/tier/grace_path", "tensura:anti_shock_area"));
        add(tensuraSig("holy/signature/tensura_magic_barrier", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "tensura:magic_barrier"));
        add(tensuraSig("holy/signature/tensura_healthcare", MagicBranch.HOLY, MagicTier.T2, 2, "holy/tier/grace_path", "tensura:healthcare"));
        add(tensuraSig("holy/signature/tensura_multilayer_barrier", MagicBranch.HOLY, MagicTier.T3, 2, "holy/tier/divine_path", "tensura:multilayer_barrier"));
        add(tensuraSig("holy/signature/tensura_anti_magic_area", MagicBranch.HOLY, MagicTier.T3, 3, "holy/tier/divine_path", "tensura:anti_magic_area"));

        add(tensuraSig("ender/signature/tensura_lighten", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "tensura:lighten"));
        add(tensuraSig("ender/signature/tensura_float", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "tensura:float"));
        add(tensuraSig("ender/signature/tensura_escape", MagicBranch.ENDER, MagicTier.T1, 1, "ender/tier/void_path", "tensura:escape"));
        add(tensuraSig("ender/signature/tensura_warp_portal", MagicBranch.ENDER, MagicTier.T2, 1, "ender/tier/rift_path", "tensura:warp_portal"));
        add(tensuraSig("ender/signature/tensura_teleport", MagicBranch.ENDER, MagicTier.T2, 2, "ender/tier/rift_path", "tensura:teleport"));
        add(tensuraSig("ender/signature/tensura_gate", MagicBranch.ENDER, MagicTier.T3, 3, "ender/tier/abyss_path", "tensura:gate"));

        add(tensuraSig("evocation/signature/tensura_analyze", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "tensura:analyze"));
        add(tensuraSig("evocation/signature/tensura_search_enemy", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "tensura:search_enemy"));
        add(tensuraSig("evocation/signature/tensura_doppelganger", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "tensura:doppelganger"));
        add(tensuraSig("evocation/signature/tensura_magic_aura", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "tensura:magic_aura"));
        add(tensuraSig("evocation/signature/tensura_magic_bullet", MagicBranch.EVOCATION, MagicTier.T1, 1, "evocation/tier/trick_path", "tensura:magic_bullet"));
        add(tensuraSig("evocation/signature/tensura_clairvoyance", MagicBranch.EVOCATION, MagicTier.T2, 1, "evocation/tier/illusion_path", "tensura:clairvoyance"));
        add(tensuraSig("evocation/signature/tensura_spatial_storage", MagicBranch.EVOCATION, MagicTier.T2, 1, "evocation/tier/illusion_path", "tensura:spatial_storage"));
        add(tensuraSig("evocation/signature/tensura_magic_space_transform", MagicBranch.EVOCATION, MagicTier.T2, 2, "evocation/tier/illusion_path", "tensura:magic_space_transform"));
        add(tensuraSig("evocation/signature/tensura_dimension_cutter", MagicBranch.EVOCATION, MagicTier.T3, 2, "evocation/tier/mastery_path", "tensura:dimension_cutter"));
        add(tensuraSig("evocation/signature/tensura_maximum_magic_bullet", MagicBranch.EVOCATION, MagicTier.T3, 3, "evocation/tier/mastery_path", "tensura:maximum_magic_bullet"));

        add(tensuraSig("eldritch/signature/tensura_darkness", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "tensura:darkness"));
        add(tensuraSig("eldritch/signature/tensura_shadow_bind", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "tensura:shadow_bind"));
        add(tensuraSig("eldritch/signature/tensura_dark_cube", MagicBranch.ELDRITCH, MagicTier.T1, 1, "eldritch/tier/dark_path", "tensura:dark_cube"));
        add(tensuraSig("eldritch/signature/tensura_curse_bind", MagicBranch.ELDRITCH, MagicTier.T2, 1, "eldritch/tier/void_gaze_path", "tensura:curse_bind"));
        add(tensuraSig("eldritch/signature/tensura_darkness_cannon", MagicBranch.ELDRITCH, MagicTier.T2, 2, "eldritch/tier/void_gaze_path", "tensura:darkness_cannon"));
        add(tensuraSig("eldritch/signature/tensura_true_darkness", MagicBranch.ELDRITCH, MagicTier.T3, 3, "eldritch/tier/abyssal_path", "tensura:true_darkness"));
    }

    private static MagicNode tensuraSig(String id, MagicBranch branch, MagicTier tier, int cost, String prereq, String skillId) {
        return sig(id, branch, tier, cost, prereq, skillId);
    }

    private static void add(MagicNode node) {
        MagicNode finalNode = node.condition() == null
                ? new MagicNode(node.id(), node.branch(), node.kind(), node.tier(),
                        node.currency(), node.cost(), node.prerequisites(),
                        node.learnedSpells(), MagicNodeMigration.defaultCondition(node))
                : node;
        if (BY_ID.put(finalNode.id(), finalNode) != null) {
            throw new IllegalStateException("duplicate node id " + finalNode.id());
        }
        BY_BRANCH.computeIfAbsent(finalNode.branch(), k -> new ArrayList<>()).add(finalNode);
    }

    public static MagicNode byId(String id) {
        return BY_ID.get(id);
    }

    public static List<MagicNode> all() {
        return List.copyOf(BY_ID.values());
    }

    public static List<MagicNode> byBranch(MagicBranch branch) {
        return Collections.unmodifiableList(BY_BRANCH.getOrDefault(branch, List.of()));
    }
}

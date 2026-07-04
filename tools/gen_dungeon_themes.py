#!/usr/bin/env python3
"""Génère DungeonThemes.java : 100 thèmes (1 par étage), difficulté croissante.

Chaque thème = nom d'affichage + liste d'adds (IDs vérifiés) + mini-boss.
Organisé en 10 ARCS de 10 étages. Dans un arc, la difficulté monte (adds + boss
plus forts). Tous les IDs proviennent du catalogue vérifié contre les vrais jars.
"""

# ── Familles de mobs vérifiées (IDs confirmés présents dans les jars) ──
# Chaque famille : liste ordonnée du plus faible au plus fort.
F = {
    "goblin":   ["tensura:goblin", "tensura:lizardman", "tensura:orc", "tensura:orc", "tensura:orc_lord"],
    "vanilla_early": ["minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:husk", "minecraft:cave_spider"],
    "vanilla_ill": ["minecraft:pillager", "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager", "minecraft:witch"],
    "slu_hollow": ["slu:hollow", "slu:armed_hollow", "slu:hollow_soldier_sword", "slu:hollow_soldier_spear", "slu:thief"],
    "slu_knight": ["slu:knight", "slu:castle_guard", "slu:dungeon_knight", "slu:elite_knight", "slu:noble_knight"],
    "slu_dark":   ["slu:dark_knight", "slu:ringed_knight", "slu:mad_knight", "slu:shadow_assassin", "slu:wither_skeleton_knight"],
    "slu_monster":["slu:monster_godrick_soldier", "slu:monster_godrick_knight", "slu:monster_tower_knight", "slu:monster_crucible_knight", "slu:monster_blasphemy_knight"],
    "bic_zombie": ["born_in_chaos_v1:decaying_zombie", "born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:zombie_fisherman", "born_in_chaos_v1:zombie_lumberjack", "born_in_chaos_v1:zombie_bruiser"],
    "bic_skele":  ["born_in_chaos_v1:baby_skeleton", "born_in_chaos_v1:decrepit_skeleton", "born_in_chaos_v1:skeleton_thrasher", "born_in_chaos_v1:skeleton_demoman", "born_in_chaos_v1:bonescaller"],
    "bic_pumpkin":["born_in_chaos_v1:pumpkin_dunce", "born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:zombie_clown", "born_in_chaos_v1:pumpkin_bruiser", "born_in_chaos_v1:senor_pumpkin"],
    "bic_spirit": ["born_in_chaos_v1:restless_spirit", "born_in_chaos_v1:seared_spirit", "born_in_chaos_v1:infernal_spirit", "born_in_chaos_v1:pumpkin_spirit", "born_in_chaos_v1:dread_hound"],
    "bic_chaos":  ["born_in_chaos_v1:fallen_chaos_knight", "born_in_chaos_v1:door_knight", "born_in_chaos_v1:missionary_raider", "born_in_chaos_v1:scarlet_persecutor", "born_in_chaos_v1:krampus_henchman"],
    "cat_draugr": ["cataclysm:draugr", "cataclysm:koboleton", "cataclysm:elite_draugr", "cataclysm:royal_draugr", "cataclysm:ignited_berserker"],
    "cat_deep":   ["cataclysm:deepling", "cataclysm:deepling_angler", "cataclysm:deepling_brute", "cataclysm:deepling_warlock", "cataclysm:deepling_priest"],
    "cat_coral":  ["cataclysm:amethyst_crab", "cataclysm:urchinkin", "cataclysm:coral_golem", "cataclysm:hippocamtus", "cataclysm:drowned_host"],
    "tensura_beast": ["tensura:horned_rabbit", "tensura:hound_dog", "tensura:direwolf", "tensura:giant_bear", "tensura:barghest"],
    "tensura_insect":["tensura:army_wasp", "tensura:giant_ant", "tensura:black_spider", "tensura:hell_moth", "tensura:knight_spider"],
    "tensura_demon": ["tensura:lesser_daemon", "tensura:greater_daemon", "tensura:arch_daemon", "tensura:hell_caterpillar", "tensura:evil_centipede"],
    "tensura_elem":  ["tensura:ifrit", "tensura:undine", "tensura:salamander", "tensura:sylphide", "tensura:metal_slime"],
    "iron_mage":  ["irons_spellbooks:cultist", "irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:archevoker", "irons_spellbooks:necromancer"],
    "bfb_pirate": ["block_factorys_bosses:crossbow_pirate", "block_factorys_bosses:pirate_rook", "block_factorys_bosses:soul_skeleton", "block_factorys_bosses:pirate_captain", "block_factorys_bosses:underworld_knight"],
    "bfb_frost":  ["block_factorys_bosses:frozen_skeleton", "block_factorys_bosses:soul_skeleton", "block_factorys_bosses:soul_knight_wither_skeleton", "block_factorys_bosses:flaming_skeleton_guard_sword", "block_factorys_bosses:dragon_guard_sword"],
    "mutant":     ["mutantmonsters:mutant_zombie", "mutantmonsters:mutant_skeleton", "mutantmonsters:mutant_creeper", "mutantmonsters:mutant_enderman", "mutantmonsters:mutant_zombie"],
    "mowzie":     ["mowziesmobs:foliaath", "mowziesmobs:umvuthana_raptor", "mowziesmobs:umvuthana_crane", "mowziesmobs:grottol", "mowziesmobs:naga"],
}

# Mini-boss par famille, du plus faible au plus fort (candidats : premier dispo utilisé)
BOSS = {
    "goblin":   [["tensura:orc_lord"], ["tensura:orc_lord"], ["tensura:orc_disaster"]],
    "vanilla_ill": [["minecraft:ravager"], ["minecraft:ravager"], ["minecraft:warden"]],
    "slu_hollow": [["slu:havel"], ["slu:executor"], ["slu:executor"]],
    "slu_knight": [["slu:havel"], ["slu:monster_tower_knight"], ["slu:monster_crucible_knight"]],
    "slu_dark":   [["slu:monster_crucible_knight"], ["slu:monster_successor"], ["slu:monster_blasphemy_knight"]],
    "slu_monster":[["slu:monster_crucible_knight_2"], ["slu:bad_omen_giant"], ["slu:magma_giant"]],
    "bic_zombie": [["born_in_chaos_v1:lifestealer"], ["born_in_chaos_v1:lifestealer"], ["born_in_chaos_v1:lifestealer_true_form"]],
    "bic_skele":  [["born_in_chaos_v1:bonescaller"], ["born_in_chaos_v1:supreme_bonescaller"], ["born_in_chaos_v1:supreme_bonescaller"]],
    "bic_pumpkin":[["born_in_chaos_v1:sir_pumpkinhead"], ["born_in_chaos_v1:lord_pumpkinhead"], ["born_in_chaos_v1:lord_pumpkinhead"]],
    "bic_spirit": [["born_in_chaos_v1:nightmare_stalker"], ["born_in_chaos_v1:nightmare_stalker"], ["born_in_chaos_v1:krampus"]],
    "bic_chaos":  [["born_in_chaos_v1:dire_hound_leader"], ["born_in_chaos_v1:mother_spider"], ["born_in_chaos_v1:krampus"]],
    "cat_draugr": [["cataclysm:royal_draugr"], ["cataclysm:the_harbinger"], ["cataclysm:the_harbinger"]],
    "cat_deep":   [["cataclysm:wadjet"], ["cataclysm:wadjet"], ["cataclysm:the_leviathan", "cataclysm:wadjet"]],
    "cat_coral":  [["cataclysm:coralssus"], ["cataclysm:coralssus"], ["cataclysm:scylla", "cataclysm:coralssus"]],
    "tensura_beast": [["tensura:barghest"], ["tensura:charybdis"], ["tensura:charybdis"]],
    "tensura_insect":[["tensura:evil_centipede"], ["tensura:knight_spider"], ["tensura:evil_centipede"]],
    "tensura_demon": [["tensura:arch_daemon"], ["tensura:arch_daemon"], ["tensura:elemental_colossus"]],
    "tensura_elem":  [["tensura:elemental_colossus"], ["tensura:elemental_colossus"], ["tensura:supermassive_slime"]],
    "iron_mage":  [["irons_spellbooks:archevoker"], ["irons_spellbooks:necromancer"], ["irons_spellbooks:priest"]],
    "bfb_pirate": [["block_factorys_bosses:pirate_captain"], ["block_factorys_bosses:underworld_knight"], ["block_factorys_bosses:infernal_dragon"]],
    "bfb_frost":  [["block_factorys_bosses:yeti"], ["block_factorys_bosses:underworld_knight"], ["block_factorys_bosses:infernal_dragon"]],
    "mutant":     [["mutantmonsters:mutant_skeleton"], ["mutantmonsters:mutant_enderman"], ["mutantmonsters:mutant_enderman"]],
    "mowzie":     [["mowziesmobs:ferrous_wroughtnaut"], ["mowziesmobs:naga"], ["mowziesmobs:umvuthi"]],
    # Cataclysm apex bosses pour les tout derniers étages
    "apex":       [["cataclysm:ignited_revenant"], ["cataclysm:the_prowler"], ["cataclysm:ender_golem"],
                   ["cataclysm:netherite_monstrosity"], ["cataclysm:the_leviathan"]],
}

# ── Les 10 ARCS (chacun 10 étages) : nom d'arc + couleur + familles ──
# color codes Minecraft §
ARCS = [
    ("The Awakening",   "§7", ["vanilla_early", "slu_hollow", "goblin"]),
    ("Restless Dead",   "§8", ["bic_zombie", "bic_skele", "slu_hollow"]),
    ("Warband",         "§2", ["goblin", "vanilla_ill", "slu_knight"]),
    ("The Hunt",        "§6", ["tensura_beast", "tensura_insect", "mowzie"]),
    ("Sunken Halls",    "§3", ["cat_deep", "cat_coral", "bfb_pirate"]),
    ("Frozen Reach",    "§b", ["bfb_frost", "cat_draugr", "mutant"]),
    ("Hollow Harvest",  "§e", ["bic_pumpkin", "bic_spirit", "slu_dark"]),
    ("Arcane Storm",    "§d", ["iron_mage", "tensura_elem", "slu_monster"]),
    ("Infernal Legion", "§4", ["tensura_demon", "bic_chaos", "cat_draugr"]),
    ("The Abyss",       "§5", ["slu_monster", "tensura_demon", "cat_coral"]),
]

# Noms d'étage par position dans l'arc (10) — donne un nom distinct à chacun
STAGE_NAMES = ["Threshold", "Descent", "Warren", "Gauntlet", "Crossing",
               "Bastion", "Hollow", "Sanctum", "Crucible", "Throne"]


def clamp_tier(i):  # 0..2 selon la position (early/mid/late de l'arc)
    if i <= 3: return 0
    if i <= 7: return 1
    return 2


def esc(s):
    return s.replace('"', '\\"')


def java_list(ids):
    return ", ".join(f'"{i}"' for i in ids)


lines = []
for floor in range(1, 101):
    arc_idx = (floor - 1) // 10
    pos = (floor - 1) % 10
    arc_name, color, fams = ARCS[arc_idx]
    # famille : varie selon la position pour la variété intra-arc
    fam = fams[pos % len(fams)]
    tier = clamp_tier(pos)
    # adds : sous-ensemble croissant de la famille (plus dur = derniers mobs)
    fam_mobs = F[fam]
    if tier == 0:
        adds = fam_mobs[:3]
    elif tier == 1:
        adds = fam_mobs[1:4]
    else:
        adds = fam_mobs[2:]
    # mini-boss : pour les étages ×10 (climax d'arc) on prend un apex ; sinon boss de famille
    if floor % 10 == 0:
        apex = BOSS["apex"]
        boss = apex[min(arc_idx // 2, len(apex) - 1)]
        stage = "§l" + STAGE_NAMES[pos]
    else:
        bopts = BOSS.get(fam, [["minecraft:iron_golem"]])
        boss = bopts[min(tier, len(bopts) - 1)]
        stage = STAGE_NAMES[pos]
    disp = f"{color}{arc_name} — {stage}"
    lines.append((floor, disp, adds, boss))

# ── Générer le Java ──
out = []
out.append("package tong.statmod.dungeon;")
out.append("")
out.append("import java.util.List;")
out.append("import java.util.Map;")
out.append("")
out.append("/**")
out.append(" * Mission M6 — 100 thèmes de donjon (1 par étage), générés (2026-07-04).")
out.append(" *")
out.append(" * <p>Chaque étage 1-100 a son thème : un nom d'affichage, une horde d'adds et un mini-boss,")
out.append(" * organisés en 10 arcs de 10 étages à difficulté croissante. Tous les IDs sont vérifiés")
out.append(" * contre les vrais jars ; les absents sont ignorés à la résolution (dégradation propre).")
out.append(" *")
out.append(" * <p>Au-delà de l'étage 100, on boucle sur le thème (floor-1)%100+1 avec un préfixe de")
out.append(" * profondeur — le donjon reste infini.")
out.append(" *")
out.append(" * <p><b>Généré</b> par {@code scratchpad/gen_themes.py} — ne pas éditer à la main ; régénérer.")
out.append(" */")
out.append("public final class DungeonThemes {")
out.append("")
out.append("    /** Un thème d'étage : nom, adds (horde), mini-boss (premier dispo utilisé). */")
out.append("    public record Theme(String displayName, List<String> adds, List<String> miniBoss) {}")
out.append("")
out.append("    private DungeonThemes() {}")
out.append("")
out.append("    private static final Theme[] THEMES = {")
for floor, disp, adds, boss in lines:
    out.append(f'        new Theme("{esc(disp)}", List.of({java_list(adds)}), List.of({java_list(boss)})), // floor {floor}')
out.append("    };")
out.append("")
out.append("    /** Nombre de thèmes définis (100). */")
out.append("    public static int count() { return THEMES.length; }")
out.append("")
out.append("    /**")
out.append("     * Thème de l'étage {@code floor} (jamais null pour floor≥1). Boucle au-delà de 100.")
out.append("     */")
out.append("    public static Theme forFloor(int floor) {")
out.append("        if (floor < 1) floor = 1;")
out.append("        int idx = (floor - 1) % THEMES.length;")
out.append("        return THEMES[idx];")
out.append("    }")
out.append("}")
out.append("")

import io
with io.open("src/main/java/tong/statmod/dungeon/DungeonThemes.java", "w", encoding="utf-8") as f:
    f.write("\n".join(out))
print(f"Généré 100 thèmes -> DungeonThemes.java ({len(lines)} entrées)")

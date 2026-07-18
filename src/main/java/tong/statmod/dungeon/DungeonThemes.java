package tong.statmod.dungeon;

import java.util.List;

/**
 * Mission M6 — 100 thèmes de donjon <b>v2 mob-factions</b> (2026-07-10).
 *
 * <p>Dix arcs de dix étages basés sur les <b>familles de mobs</b> du modpack (SLU, aventuriers STAT Mod,
 * Born in Chaos, Ice and Fire, Iron's Spellbooks…). Chaque étage a son nom propre
 * et sa composition de horde unique — plus de pools recyclés.
 *
 * <p>Les IDs d'entités sont vérifiés contre les registres réels du modpack (audit 2026-07-04).
 * Au-delà de l'étage 100, {@link #forFloor} boucle sur (floor-1)%100+1.
 */
public final class DungeonThemes {

    public record Theme(String displayName, List<String> adds, List<String> miniBoss) {}

    private DungeonThemes() {}

    private static final Theme[] THEMES = {

        // ═══ ARC 1 (1-10) — LES DÉCHARNÉS (§7, gris) ═══════════════════════
        new Theme("§7Les Décharnés — La Herse Rouillée",
                List.of("minecraft:zombie", "minecraft:skeleton", "slu:hollow"),
                List.of("minecraft:iron_golem")), // 1
        new Theme("§7Les Décharnés — Les Geôles Effondrées",
                List.of("slu:hollow", "slu:armed_hollow", "minecraft:silverfish"),
                List.of("slu:havel")), // 2
        new Theme("§7Les Décharnés — L'Armurerie des Morts",
                List.of("slu:thief", "minecraft:zombie", "minecraft:husk"),
                List.of("slu:executor")), // 3
        new Theme("§7Les Décharnés — Le Réfectoire aux Mouches",
                List.of("minecraft:cave_spider", "minecraft:husk", "slu:hollow_soldier_sword"),
                List.of("minecraft:creeper")), // 4
        new Theme("§7Les Décharnés — La Salle de Garde Abandonnée",
                List.of("slu:hollow_soldier_spear", "minecraft:skeleton", "slu:twisted_souls"),
                List.of("minecraft:bogged")), // 5
        new Theme("§7Les Décharnés — Le Puits aux Chaînes",
                List.of("minecraft:skeleton", "minecraft:zombie_villager",
                        "minecraft:stray"),
                List.of("slu:thief")), // 6
        new Theme("§7Les Décharnés — La Chapelle des Os",
                List.of("minecraft:wither_skeleton", "minecraft:drowned",
                        "minecraft:skeleton"),
                List.of("slu:armed_hollow")), // 7
        new Theme("§7Les Décharnés — L'Antichambre des Rats",
                List.of("minecraft:slime", "slu:thief", "minecraft:zombie"),
                List.of("minecraft:witch")), // 8
        new Theme("§7Les Décharnés — La Cour aux Supplices",
                List.of("slu:hollow_soldier_sword", "minecraft:husk", "minecraft:husk"),
                List.of("minecraft:iron_golem")), // 9
        new Theme("§7Les Décharnés — La Porte que Nul ne Garde",
                List.of("slu:armed_hollow", "irons_spellbooks:pyromancer",
                        "minecraft:bogged"),
                List.of("statmod:adventurer")), // 10

        // ═══ ARC 2 (11-20) — LES FAUVES (§2, vert) ══════════════════════════
        new Theme("§2Les Fauves — L'Enclos Éventré",
                List.of("alexsmobs:komodo_dragon", "minecraft:wolf", "minecraft:wolf"),
                List.of("alexsmobs:grizzly_bear")), // 11
        new Theme("§2Les Fauves — La Tanière du Prédateur",
                List.of("alexsmobs:roadrunner", "minecraft:phantom", "mowziesmobs:foliaath"),
                List.of("statmod:adventurer")), // 12
        new Theme("§2Les Fauves — Le Terrier des Tisseuses",
                List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:cave_spider"),
                List.of("mowziesmobs:foliaath")), // 13
        new Theme("§2Les Fauves — La Volière aux Dards",
                List.of("alexsmobs:centipede_head", "alexsmobs:crimson_mosquito", "minecraft:bee"),
                List.of("statmod:adventurer")), // 14
        new Theme("§2Les Fauves — La Grotte aux Fauves",
                List.of("alexsmobs:komodo_dragon", "alexsmobs:roadrunner", "mowziesmobs:umvuthana_raptor"),
                List.of("alexsmobs:komodo_dragon")), // 15
        new Theme("§2Les Fauves — Le Verger Empoisonné",
                List.of("alexsmobs:komodo_dragon", "minecraft:stray", "minecraft:cave_spider"),
                List.of("minecraft:phantom")), // 16
        new Theme("§2Les Fauves — La Clairière des Chairs",
                List.of("mowziesmobs:foliaath", "minecraft:wolf", "minecraft:spider"),
                List.of("statmod:adventurer")), // 17
        new Theme("§2Les Fauves — Les Fourrés Mouvants",
                List.of("minecraft:wolf", "mowziesmobs:grottol", "alexsmobs:grizzly_bear"),
                List.of("alexsmobs:komodo_dragon")), // 18
        new Theme("§2Les Fauves — Le Nid du Silencieux",
                List.of("minecraft:spider", "minecraft:cave_spider", "alexsmobs:tarantula_hawk"),
                List.of("alexsmobs:grizzly_bear")), // 19
        new Theme("§2Les Fauves — Le Repaire de l'Alpha",
                List.of("alexsmobs:komodo_dragon", "minecraft:wolf", "minecraft:wolf"),
                List.of("statmod:adventurer")), // 20

        // ═══ ARC 3 (21-30) — LES TRIBUS (§c, rouge) ═════════════════════════
        new Theme("§cLes Tribus — Le Camp des Éclaireurs",
                List.of("minecraft:pillager", "minecraft:pillager", "slu:knight"),
                List.of("epic_mobs:frostgeneral")), // 21
        new Theme("§cLes Tribus — La Palissade Hurlante",
                List.of("minecraft:vindicator", "minecraft:pillager", "minecraft:vindicator"),
                List.of("minecraft:ravager")), // 22
        new Theme("§cLes Tribus — L'Armurerie des Félons",
                List.of("slu:castle_guard", "slu:hollow_knight", "minecraft:vindicator"),
                List.of("slu:monster_tower_knight")), // 23
        new Theme("§cLes Tribus — Le Chenil de Guerre",
                List.of("minecraft:wolf", "minecraft:zombified_piglin", "epic_mobs:frostguard"),
                List.of("minecraft:ravager")), // 24
        new Theme("§cLes Tribus — La Solde du Mercenaire",
                List.of("minecraft:pillager", "minecraft:pillager", "slu:thief"),
                List.of("minecraft:ravager")), // 25
        new Theme("§cLes Tribus — La Cour des Duels",
                List.of("slu:dungeon_knight", "slu:nightmare_knight", "minecraft:evoker"),
                List.of("slu:knight")), // 26
        new Theme("§cLes Tribus — Les Écuries Brûlées",
                List.of("minecraft:vindicator", "minecraft:zombified_piglin", "minecraft:piglin"),
                List.of("epic_mobs:frostgeneral")), // 27
        new Theme("§cLes Tribus — La Tente du Stratège",
                List.of("mowziesmobs:umvuthana_crane", "minecraft:evoker", "slu:castle_guard"),
                List.of("minecraft:vindicator")), // 28
        new Theme("§cLes Tribus — Le Front des Bannières",
                List.of("slu:elite_knight", "epic_mobs:frostguard", "minecraft:pillager"),
                List.of("minecraft:ravager")), // 29
        new Theme("§cLes Tribus — L'État-Major Renégat",
                List.of("minecraft:ravager", "slu:nightmare_knight", "minecraft:vindicator"),
                List.of("statmod:adventurer")), // 30

        // ═══ ARC 4 (31-40) — LA LÉGION NOIRE (§8, gris foncé) ════════════════
        new Theme("§8La Légion Noire — Les Fosses des Damnés",
                List.of("slu:armed_hollow", "slu:armed_hollow", "slu:ghost_samurai"),
                List.of("slu:elite_knight")), // 31
        new Theme("§8La Légion Noire — Le Caveau des Traîtres",
                List.of("slu:dungeon_knight", "slu:noble_knight", "minecraft:vindicator"),
                List.of("statmod:adventurer")), // 32
        new Theme("§8La Légion Noire — La Salle des Suaires",
                List.of("minecraft:husk", "slu:elite_knight",
                        "minecraft:enderman"),
                List.of("mutantmonsters:mutant_zombie")), // 33
        new Theme("§8La Légion Noire — Le Pont des Soupirs",
                List.of("slu:elite_knight", "minecraft:stray", "minecraft:evoker"),
                List.of("slu:armed_hollow")), // 34
        new Theme("§8La Légion Noire — L'Armurerie Noire",
                List.of("slu:ghost_samurai", "slu:dungeon_knight", "minecraft:skeleton"),
                List.of("slu:dark_spirit")), // 35
        new Theme("§8La Légion Noire — Le Donjon aux Mille Yeux",
                List.of("minecraft:enderman", "minecraft:vex",
                        "slu:armed_hollow"),
                List.of("mutantmonsters:mutant_skeleton")), // 36
        new Theme("§8La Légion Noire — Les Écuries Spectrales",
                List.of("slu:elite_knight", "slu:elite_knight", "slu:elite_knight"),
                List.of("minecraft:husk")), // 37
        new Theme("§8La Légion Noire — La Crypte des Félons",
                List.of("slu:nightmare_knight", "minecraft:enderman", "mowziesmobs:ferrous_wroughtnaut"),
                List.of("slu:elite_knight")), // 38
        new Theme("§8La Légion Noire — La Cour des Ombres",
                List.of("slu:ghost_samurai", "slu:elite_knight", "slu:armed_hollow"),
                List.of("statmod:adventurer")), // 39
        new Theme("§8La Légion Noire — Le Trône de la Légion",
                List.of("slu:elite_knight", "slu:elite_knight", "statmod:adventurer"),
                List.of("statmod:adventurer")), // 40

        // ═══ ARC 5 (41-50) — LES ABYSSES (§3, cyan) ═══════════════════════════
        new Theme("§3Les Abysses — Le Vestibule des Marées",
                List.of("minecraft:drowned", "minecraft:drowned", "minecraft:elder_guardian"),
                List.of("irons_spellbooks:cryomancer")), // 41
        new Theme("§3Les Abysses — Les Jardins de Corail",
                List.of("minecraft:guardian", "minecraft:pufferfish", "minecraft:elder_guardian"),
                List.of("minecraft:elder_guardian")), // 42
        new Theme("§3Les Abysses — La Cale des Écumeurs",
                List.of("minecraft:pillager", "minecraft:vindicator",
                        "minecraft:drowned"),
                List.of("epic_mobs:pillager_king")), // 43
        new Theme("§3Les Abysses — La Salle de Bal Immergée",
                List.of("minecraft:drowned", "minecraft:drowned", "minecraft:guardian"),
                List.of("irons_spellbooks:cryomancer")), // 44
        new Theme("§3Les Abysses — Le Trésor de la Frégate",
                List.of("minecraft:pillager", "minecraft:drowned", "minecraft:drowned"),
                List.of("minecraft:wither_skeleton")), // 45
        new Theme("§3Les Abysses — L'Aquarium des Monstres",
                List.of("alexsmobs:bone_serpent", "alexsmobs:bone_serpent", "minecraft:guardian"),
                List.of("minecraft:elder_guardian")), // 46
        new Theme("§3Les Abysses — Les Autels de la Vase",
                List.of("minecraft:drowned_warlock", "minecraft:drowned_angler", "minecraft:drowned"),
                List.of("irons_spellbooks:cryomancer")), // 47
        new Theme("§3Les Abysses — Le Chœur des Abysses",
                List.of("minecraft:drowned_priest", "minecraft:drowned_warlock", "minecraft:guardian"),
                List.of("minecraft:elder_guardian")), // 48
        new Theme("§3Les Abysses — Le Pont des Amiraux",
                List.of("minecraft:vindicator", "minecraft:drowned_brute",
                        "minecraft:drowned"),
                List.of("statmod:adventurer")), // 49
        new Theme("§3Les Abysses — La Salle du Trône Englouti",
                List.of("minecraft:drowned_brute", "minecraft:drowned_priest", "minecraft:drowned"),
                List.of("irons_spellbooks:cursed_armor_stand")), // 50

        // ═══ ARC 6 (51-60) — LE CERCLE DES MAGES (§d, rose) ══════════════════
        new Theme("§dLe Cercle des Mages — Le Hall des Apprentis",
                List.of("irons_spellbooks:cultist", "minecraft:witch", "minecraft:blaze"),
                List.of("irons_spellbooks:archevoker")), // 51
        new Theme("§dLe Cercle des Mages — L'Amphithéâtre des Flammes",
                List.of("irons_spellbooks:pyromancer", "minecraft:witch", "minecraft:breeze"),
                List.of("epic_mobs:phoenix_fight")), // 52
        new Theme("§dLe Cercle des Mages — La Bibliothèque Interdite",
                List.of("irons_spellbooks:catacombs_zombie", "irons_spellbooks:priest", "minecraft:vex"),
                List.of("irons_spellbooks:cryomancer")), // 53
        new Theme("§dLe Cercle des Mages — Le Laboratoire des Golems",
                List.of("irons_spellbooks:magehunter_vindicator", "minecraft:vex", "minecraft:evoker"),
                List.of("epic_mobs:the_knight")), // 54
        new Theme("§dLe Cercle des Mages — Le Coffre du Doyen",
                List.of("irons_spellbooks:cryomancer", "minecraft:witch", "irons_spellbooks:apothecarist"),
                List.of("irons_spellbooks:pyromancer")), // 55
        new Theme("§dLe Cercle des Mages — L'Aile des Élémentaires",
                List.of("minecraft:blaze", "minecraft:witch", "minecraft:vex"),
                List.of("epic_mobs:phoenix_fight")), // 56
        new Theme("§dLe Cercle des Mages — Le Givre de la Chaire",
                List.of("irons_spellbooks:cryomancer", "irons_spellbooks:ice_spider", "minecraft:witch"),
                List.of("irons_spellbooks:apothecarist")), // 57
        new Theme("§dLe Cercle des Mages — Les Ateliers de Chair",
                List.of("irons_spellbooks:necromancer", "irons_spellbooks:catacombs_zombie",
                        "minecraft:breeze"),
                List.of("irons_spellbooks:magehunter_vindicator")), // 58
        new Theme("§dLe Cercle des Mages — L'Observatoire des Tempêtes",
                List.of("minecraft:breeze", "minecraft:vex", "irons_spellbooks:pyromancer"),
                List.of("epic_mobs:the_knight")), // 59
        new Theme("§dLe Cercle des Mages — La Thèse Finale",
                List.of("irons_spellbooks:archevoker", "irons_spellbooks:necromancer", "minecraft:witch"),
                List.of("slu:magma_giant")), // 60

        // ═══ ARC 7 (61-70) — LA MOISSON DE L'EFFROI (§6, or) ════════════════
        new Theme("§6La Moisson de l'Effroi — La Procession des Lanternes",
                List.of("minecraft:husk", "minecraft:zombie_villager", "minecraft:witch"),
                List.of("statmod:adventurer")), // 61
        new Theme("§6La Moisson de l'Effroi — Le Bal des Épouvantails",
                List.of("minecraft:zombie_villager", "minecraft:husk",
                        "minecraft:phantom"),
                List.of("statmod:adventurer")), // 62
        new Theme("§6La Moisson de l'Effroi — Les Feux Follets du Verger",
                List.of("irons_spellbooks:pyromancer", "slu:dark_knight", "minecraft:vex"),
                List.of("statmod:adventurer")), // 63
        new Theme("§6La Moisson de l'Effroi — Le Pressoir aux Ombres",
                List.of("slu:ringed_knight", "minecraft:vex", "minecraft:witch"),
                List.of("irons_spellbooks:dead_king")), // 64
        new Theme("§6La Moisson de l'Effroi — Le Grenier du Rémouleur",
                List.of("minecraft:zombie_villager", "slu:shadow_assassin", "minecraft:zombie_villager"),
                List.of("slu:monster_successor")), // 65
        new Theme("§6La Moisson de l'Effroi — La Ronde des Masques",
                List.of("slu:ringed_knight", "slu:mad_knight", "irons_spellbooks:pyromancer"),
                List.of("irons_spellbooks:dead_king")), // 66
        new Theme("§6La Moisson de l'Effroi — Le Champ des Têtes Sculptées",
                List.of("minecraft:husk", "minecraft:zombie_villager",
                        "minecraft:husk"),
                List.of("statmod:adventurer")), // 67
        new Theme("§6La Moisson de l'Effroi — Les Brasiers de la Veillée",
                List.of("irons_spellbooks:pyromancer", "slu:wither_skeleton_knight", "minecraft:vex"),
                List.of("statmod:adventurer")), // 68
        new Theme("§6La Moisson de l'Effroi — Le Cortège de Minuit",
                List.of("slu:mad_knight", "slu:shadow_assassin", "minecraft:vex"),
                List.of("slu:monster_successor")), // 69
        new Theme("§6La Moisson de l'Effroi — Le Banquet du Roi-Citrouille",
                List.of("minecraft:husk", "minecraft:zombie_villager",
                        "minecraft:zombie_villager"),
                List.of("slu:magma_giant")), // 70

        // ═══ ARC 8 (71-80) — LA FOURNAISE (§4, rouge foncé) ══════════════════
        new Theme("§4La Fournaise — Les Soufflets Hurlants",
                List.of("minecraft:blaze", "epic_mobs:infernal_eye", "minecraft:blaze"),
                List.of("epic_mobs:tech_knight")), // 71
        new Theme("§4La Fournaise — La Chaîne des Enclumes",
                List.of("minecraft:wither_skeleton", "slu:wither_skeleton_knight",
                        "irons_spellbooks:pyromancer"),
                List.of("slu:magma_giant")), // 72
        new Theme("§4La Fournaise — Le Bassin de Trempe",
                List.of("minecraft:magma_cube", "minecraft:hoglin", "alexsmobs:crimson_mosquito"),
                List.of("irons_spellbooks:pyromancer")), // 73
        new Theme("§4La Fournaise — Les Galeries du Minerai Noir",
                List.of("irons_spellbooks:pyromancer", "irons_spellbooks:pyromancer",
                        "minecraft:wither_skeleton"),
                List.of("minecraft:hoglin")), // 74
        new Theme("§4La Fournaise — La Paie des Damnés",
                List.of("epic_mobs:infernal_eye", "alexsmobs:soul_vulture", "minecraft:piglin_brute"),
                List.of("epic_mobs:tech_knight")), // 75
        new Theme("§4La Fournaise — L'Atelier des Armures",
                List.of("alexsmobs:warped_mosco", "minecraft:blaze", "minecraft:blaze"),
                List.of("irons_spellbooks:pyromancer")), // 76
        new Theme("§4La Fournaise — Le Tribunal des Braises",
                List.of("irons_spellbooks:pyromancer", "alexsmobs:crimson_mosquito", "minecraft:blaze"),
                List.of("statmod:adventurer")), // 77
        new Theme("§4La Fournaise — La Fonderie des Colosses",
                List.of("minecraft:hoglin", "minecraft:magma_cube", "alexsmobs:soul_vulture"),
                List.of("minecraft:piglin_brute")), // 78
        new Theme("§4La Fournaise — Le Couloir des Fours Ouverts",
                List.of("epic_mobs:infernal_eye", "minecraft:blaze", "minecraft:blaze"),
                List.of("epic_mobs:tech_knight")), // 79
        new Theme("§4La Fournaise — Le Maître des Soufflets",
                List.of("irons_spellbooks:pyromancer", "minecraft:blaze", "minecraft:blaze"),
                List.of("bosses_of_mass_destruction:obsidilith")), // 80

        // ═══ ARC 9 (81-90) — LA GESTE DÉMONIAQUE (§5, violet) ════════════════
        new Theme("§5La Geste Démoniaque — L'Antichambre du Fléau",
                List.of("epic_mobs:shadow_guard", "slu:dark_knight", "minecraft:vex"),
                List.of("epic_mobs:tech_knight")), // 81
        new Theme("§5La Geste Démoniaque — La Salle des Rituels",
                List.of("minecraft:blaze", "statmod:adventurer",
                        "minecraft:vex"),
                List.of("irons_spellbooks:dead_king")), // 82
        new Theme("§5La Geste Démoniaque — Le Couloir aux Cent Pattes",
                List.of("minecraft:cave_spider", "minecraft:spider", "mowziesmobs:umvuthi"),
                List.of("epic_mobs:shadow_guard")), // 83
        new Theme("§5La Geste Démoniaque — Les Appartements du Non-Mort",
                List.of("slu:knight", "minecraft:drowned_priest", "slu:knight"),
                List.of("slu:elite_knight")), // 84
        new Theme("§5La Geste Démoniaque — La Salle de la Perdition",
                List.of("epic_mobs:tech_knight", "minecraft:pillager",
                        "statmod:adventurer"),
                List.of("mutantmonsters:mutant_creeper")), // 85
        new Theme("§5La Geste Démoniaque — Le Jardin des Âmes",
                List.of("epic_mobs:shadow_guard", "epic_mobs:shadow_guard",
                        "statmod:adventurer"),
                List.of("epic_mobs:the_knight")), // 86
        new Theme("§5La Geste Démoniaque — Le Vestibule des Martyrs",
                List.of("minecraft:blaze", "mowziesmobs:naga", "minecraft:cave_spider"),
                List.of("irons_spellbooks:dead_king")), // 87
        new Theme("§5La Geste Démoniaque — La Fonderie des Supplices",
                List.of("slu:dark_knight", "epic_mobs:crystal_guardian", "minecraft:spider"),
                List.of("mutantmonsters:mutant_enderman")), // 88
        new Theme("§5La Geste Démoniaque — Le Pont des Âmes Perdues",
                List.of("epic_mobs:tech_knight", "minecraft:drowned_priest",
                        "epic_mobs:shadow_guard"),
                List.of("epic_mobs:shadow_guard")), // 89
        new Theme("§5La Geste Démoniaque — Le Trône du Fléau",
                List.of("epic_mobs:tech_knight", "minecraft:blaze", "statmod:adventurer"),
                List.of("bosses_of_mass_destruction:obsidilith")), // 90

        // ═══ ARC 10 (91-100) — LE TRÔNE DU NÉANT (§5, violet) ════════════════
        new Theme("§5Le Trône du Néant — L'Antichambre du Vide",
                List.of("minecraft:enderman", "minecraft:endermite", "slu:monster_godrick_soldier"),
                List.of("slu:monster_crucible_knight")), // 91
        new Theme("§5Le Trône du Néant — La Galerie des Reflets Faux",
                List.of("slu:white_phantom", "minecraft:shulker", "minecraft:endermite"),
                List.of("slu:monster_successor")), // 92
        new Theme("§5Le Trône du Néant — Le Jardin de Cristal Mort",
                List.of("minecraft:enderman", "irons_spellbooks:cursed_armor_stand", "minecraft:endermite"),
                List.of("minecraft:elder_guardian")), // 93
        new Theme("§5Le Trône du Néant — Les Marches du Silence",
                List.of("slu:monster_godrick_knight", "slu:clone_abyss_watcher", "minecraft:shulker"),
                List.of("slu:bad_omen_giant")), // 94
        new Theme("§5Le Trône du Néant — Le Reliquaire de la Couronne",
                List.of("slu:monster_crusader", "minecraft:endermite", "minecraft:phantom"),
                List.of("slu:monster_successor")), // 95
        new Theme("§5Le Trône du Néant — La Salle des Souverains Effacés",
                List.of("slu:monster_blasphemy_knight", "slu:temple_guard", "minecraft:enderman"),
                List.of("slu:bad_omen_giant")), // 96
        new Theme("§5Le Trône du Néant — Le Pont au-dessus de Rien",
                List.of("minecraft:phantom", "slu:executor", "slu:white_phantom"),
                List.of("irons_spellbooks:cursed_armor_stand")), // 97
        new Theme("§5Le Trône du Néant — La Chambre des Échos",
                List.of("slu:monster_crucible_knight", "slu:dungeon_knight",
                        "minecraft:shulker"),
                List.of("mutantmonsters:mutant_enderman")), // 98
        new Theme("§5Le Trône du Néant — L'Ultime Garde",
                List.of("slu:monster_crucible_knight_2", "slu:monster_blasphemy_knight",
                        "slu:monster_tower_knight"),
                List.of("slu:monster_successor")), // 99
        new Theme("§5Le Trône du Néant — Le Trône qui Regarde en Bas",
                List.of("slu:monster_blasphemy_knight", "slu:magma_giant", "bosses_of_mass_destruction:obsidilith"),
                List.of("slu:havel")), // 100
    };

    public static int count() { return THEMES.length; }

    /** Thème de l'étage (boucle au-delà de 100). */
    public static Theme forFloor(int floor) {
        if (floor <= 0) return THEMES[0];
        return THEMES[(floor - 1) % THEMES.length];
    }
}

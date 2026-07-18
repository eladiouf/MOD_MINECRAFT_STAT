package tong.statmod.dungeon;

import java.util.List;

/**
 * Mission M6 — 100 thèmes de donjon <b>v2 mob-factions</b> (2026-07-10).
 *
 * <p>Dix arcs de dix étages basés sur les <b>familles de mobs</b> du modpack (SLU, Cataclysm,
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
                List.of("slu:armed_hollow", "cataclysm:ignited_berserker",
                        "minecraft:bogged"),
                List.of("cataclysm:ignited_revenant")), // 10

        // ═══ ARC 2 (11-20) — LES FAUVES (§2, vert) ══════════════════════════
        new Theme("§2Les Fauves — L'Enclos Éventré",
                List.of("alexsmobs:komodo_dragon", "minecraft:wolf", "minecraft:wolf"),
                List.of("alexsmobs:grizzly_bear")), // 11
        new Theme("§2Les Fauves — La Tanière du Prédateur",
                List.of("alexsmobs:roadrunner", "minecraft:phantom", "mowziesmobs:foliaath"),
                List.of("cataclysm:royal_draugr")), // 12
        new Theme("§2Les Fauves — Le Terrier des Tisseuses",
                List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:cave_spider"),
                List.of("mowziesmobs:foliaath")), // 13
        new Theme("§2Les Fauves — La Volière aux Dards",
                List.of("alexsmobs:centipede_head", "alexsmobs:crimson_mosquito", "minecraft:bee"),
                List.of("cataclysm:koboleton")), // 14
        new Theme("§2Les Fauves — La Grotte aux Fauves",
                List.of("alexsmobs:komodo_dragon", "alexsmobs:roadrunner", "mowziesmobs:umvuthana_raptor"),
                List.of("alexsmobs:komodo_dragon")), // 15
        new Theme("§2Les Fauves — Le Verger Empoisonné",
                List.of("alexsmobs:komodo_dragon", "minecraft:stray", "minecraft:cave_spider"),
                List.of("minecraft:phantom")), // 16
        new Theme("§2Les Fauves — La Clairière des Chairs",
                List.of("mowziesmobs:foliaath", "minecraft:wolf", "minecraft:spider"),
                List.of("cataclysm:koboleton")), // 17
        new Theme("§2Les Fauves — Les Fourrés Mouvants",
                List.of("minecraft:wolf", "mowziesmobs:grottol", "alexsmobs:grizzly_bear"),
                List.of("alexsmobs:komodo_dragon")), // 18
        new Theme("§2Les Fauves — Le Nid du Silencieux",
                List.of("minecraft:spider", "minecraft:cave_spider", "alexsmobs:tarantula_hawk"),
                List.of("alexsmobs:grizzly_bear")), // 19
        new Theme("§2Les Fauves — Le Repaire de l'Alpha",
                List.of("alexsmobs:komodo_dragon", "minecraft:wolf", "minecraft:wolf"),
                List.of("cataclysm:the_prowler")), // 20

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
                List.of("cataclysm:the_prowler")), // 30

        // ═══ ARC 4 (31-40) — LA LÉGION NOIRE (§8, gris foncé) ════════════════
        new Theme("§8La Légion Noire — Les Fosses des Damnés",
                List.of("cataclysm:draugr", "cataclysm:draugr", "slu:ghost_samurai"),
                List.of("cataclysm:elite_draugr")), // 31
        new Theme("§8La Légion Noire — Le Caveau des Traîtres",
                List.of("slu:dungeon_knight", "slu:noble_knight", "minecraft:vindicator"),
                List.of("cataclysm:royal_draugr")), // 32
        new Theme("§8La Légion Noire — La Salle des Suaires",
                List.of("minecraft:husk", "cataclysm:elite_draugr",
                        "minecraft:enderman"),
                List.of("mutantmonsters:mutant_zombie")), // 33
        new Theme("§8La Légion Noire — Le Pont des Soupirs",
                List.of("slu:elite_knight", "minecraft:stray", "minecraft:evoker"),
                List.of("cataclysm:draugr")), // 34
        new Theme("§8La Légion Noire — L'Armurerie Noire",
                List.of("slu:ghost_samurai", "slu:dungeon_knight", "minecraft:skeleton"),
                List.of("slu:dark_spirit")), // 35
        new Theme("§8La Légion Noire — Le Donjon aux Mille Yeux",
                List.of("minecraft:enderman", "minecraft:vex",
                        "cataclysm:draugr"),
                List.of("mutantmonsters:mutant_skeleton")), // 36
        new Theme("§8La Légion Noire — Les Écuries Spectrales",
                List.of("cataclysm:elite_draugr", "slu:elite_knight", "cataclysm:elite_draugr"),
                List.of("minecraft:husk")), // 37
        new Theme("§8La Légion Noire — La Crypte des Félons",
                List.of("slu:nightmare_knight", "minecraft:enderman", "mowziesmobs:ferrous_wroughtnaut"),
                List.of("cataclysm:elite_draugr")), // 38
        new Theme("§8La Légion Noire — La Cour des Ombres",
                List.of("slu:ghost_samurai", "cataclysm:elite_draugr", "cataclysm:draugr"),
                List.of("cataclysm:royal_draugr")), // 39
        new Theme("§8La Légion Noire — Le Trône de la Légion",
                List.of("cataclysm:elite_draugr", "slu:elite_knight", "cataclysm:koboleton"),
                List.of("cataclysm:the_prowler")), // 40

        // ═══ ARC 5 (41-50) — LES ABYSSES (§3, cyan) ═══════════════════════════
        new Theme("§3Les Abysses — Le Vestibule des Marées",
                List.of("cataclysm:deepling", "minecraft:drowned", "minecraft:elder_guardian"),
                List.of("cataclysm:wadjet")), // 41
        new Theme("§3Les Abysses — Les Jardins de Corail",
                List.of("cataclysm:amethyst_crab", "cataclysm:urchinkin", "cataclysm:coral_golem"),
                List.of("cataclysm:coralssus")), // 42
        new Theme("§3Les Abysses — La Cale des Écumeurs",
                List.of("minecraft:pillager", "minecraft:vindicator",
                        "minecraft:drowned"),
                List.of("epic_mobs:pillager_king")), // 43
        new Theme("§3Les Abysses — La Salle de Bal Immergée",
                List.of("cataclysm:drowned_host", "cataclysm:deepling", "minecraft:guardian"),
                List.of("cataclysm:wadjet")), // 44
        new Theme("§3Les Abysses — Le Trésor de la Frégate",
                List.of("minecraft:pillager", "minecraft:drowned", "cataclysm:deepling"),
                List.of("minecraft:wither_skeleton")), // 45
        new Theme("§3Les Abysses — L'Aquarium des Monstres",
                List.of("cataclysm:hippocamtus", "alexsmobs:bone_serpent", "cataclysm:amethyst_crab"),
                List.of("cataclysm:coralssus")), // 46
        new Theme("§3Les Abysses — Les Autels de la Vase",
                List.of("cataclysm:deepling_warlock", "cataclysm:deepling_angler", "minecraft:drowned"),
                List.of("cataclysm:wadjet")), // 47
        new Theme("§3Les Abysses — Le Chœur des Abysses",
                List.of("cataclysm:deepling_priest", "cataclysm:deepling_warlock", "minecraft:guardian"),
                List.of("cataclysm:coralssus")), // 48
        new Theme("§3Les Abysses — Le Pont des Amiraux",
                List.of("minecraft:vindicator", "cataclysm:deepling_brute",
                        "cataclysm:deepling"),
                List.of("cataclysm:the_prowler")), // 49
        new Theme("§3Les Abysses — La Salle du Trône Englouti",
                List.of("cataclysm:deepling_brute", "cataclysm:deepling_priest", "minecraft:drowned"),
                List.of("cataclysm:ender_golem")), // 50

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
                List.of("cataclysm:netherite_monstrosity")), // 60

        // ═══ ARC 7 (61-70) — LA MOISSON DE L'EFFROI (§6, or) ════════════════
        new Theme("§6La Moisson de l'Effroi — La Procession des Lanternes",
                List.of("minecraft:husk", "minecraft:zombie_villager", "minecraft:witch"),
                List.of("cataclysm:the_prowler")), // 61
        new Theme("§6La Moisson de l'Effroi — Le Bal des Épouvantails",
                List.of("minecraft:zombie_villager", "minecraft:husk",
                        "minecraft:phantom"),
                List.of("cataclysm:the_prowler")), // 62
        new Theme("§6La Moisson de l'Effroi — Les Feux Follets du Verger",
                List.of("cataclysm:ignited_berserker", "slu:dark_knight", "minecraft:vex"),
                List.of("cataclysm:royal_draugr")), // 63
        new Theme("§6La Moisson de l'Effroi — Le Pressoir aux Ombres",
                List.of("slu:ringed_knight", "minecraft:vex", "minecraft:witch"),
                List.of("cataclysm:the_harbinger")), // 64
        new Theme("§6La Moisson de l'Effroi — Le Grenier du Rémouleur",
                List.of("minecraft:zombie_villager", "slu:shadow_assassin", "minecraft:zombie_villager"),
                List.of("slu:monster_successor")), // 65
        new Theme("§6La Moisson de l'Effroi — La Ronde des Masques",
                List.of("slu:ringed_knight", "slu:mad_knight", "cataclysm:ignited_berserker"),
                List.of("cataclysm:the_harbinger")), // 66
        new Theme("§6La Moisson de l'Effroi — Le Champ des Têtes Sculptées",
                List.of("minecraft:husk", "minecraft:zombie_villager",
                        "minecraft:husk"),
                List.of("cataclysm:the_prowler")), // 67
        new Theme("§6La Moisson de l'Effroi — Les Brasiers de la Veillée",
                List.of("cataclysm:ignited_berserker", "slu:wither_skeleton_knight", "minecraft:vex"),
                List.of("cataclysm:royal_draugr")), // 68
        new Theme("§6La Moisson de l'Effroi — Le Cortège de Minuit",
                List.of("slu:mad_knight", "slu:shadow_assassin", "minecraft:vex"),
                List.of("slu:monster_successor")), // 69
        new Theme("§6La Moisson de l'Effroi — Le Banquet du Roi-Citrouille",
                List.of("minecraft:husk", "minecraft:zombie_villager",
                        "minecraft:zombie_villager"),
                List.of("cataclysm:netherite_monstrosity")), // 70

        // ═══ ARC 8 (71-80) — LA FOURNAISE (§4, rouge foncé) ══════════════════
        new Theme("§4La Fournaise — Les Soufflets Hurlants",
                List.of("minecraft:blaze", "epic_mobs:infernal_eye", "minecraft:blaze"),
                List.of("epic_mobs:tech_knight")), // 71
        new Theme("§4La Fournaise — La Chaîne des Enclumes",
                List.of("minecraft:wither_skeleton", "slu:wither_skeleton_knight",
                        "cataclysm:ignited_berserker"),
                List.of("cataclysm:netherite_monstrosity")), // 72
        new Theme("§4La Fournaise — Le Bassin de Trempe",
                List.of("minecraft:magma_cube", "minecraft:hoglin", "alexsmobs:crimson_mosquito"),
                List.of("cataclysm:ignited_berserker")), // 73
        new Theme("§4La Fournaise — Les Galeries du Minerai Noir",
                List.of("cataclysm:ignited_berserker", "cataclysm:ignited_berserker",
                        "minecraft:wither_skeleton"),
                List.of("minecraft:hoglin")), // 74
        new Theme("§4La Fournaise — La Paie des Damnés",
                List.of("epic_mobs:infernal_eye", "alexsmobs:soul_vulture", "minecraft:piglin_brute"),
                List.of("epic_mobs:tech_knight")), // 75
        new Theme("§4La Fournaise — L'Atelier des Armures",
                List.of("alexsmobs:warped_mosco", "minecraft:blaze", "minecraft:blaze"),
                List.of("cataclysm:ignited_berserker")), // 76
        new Theme("§4La Fournaise — Le Tribunal des Braises",
                List.of("cataclysm:ignited_berserker", "alexsmobs:crimson_mosquito", "minecraft:blaze"),
                List.of("cataclysm:ignited_revenant")), // 77
        new Theme("§4La Fournaise — La Fonderie des Colosses",
                List.of("minecraft:hoglin", "minecraft:magma_cube", "alexsmobs:soul_vulture"),
                List.of("minecraft:piglin_brute")), // 78
        new Theme("§4La Fournaise — Le Couloir des Fours Ouverts",
                List.of("epic_mobs:infernal_eye", "minecraft:blaze", "minecraft:blaze"),
                List.of("epic_mobs:tech_knight")), // 79
        new Theme("§4La Fournaise — Le Maître des Soufflets",
                List.of("cataclysm:ignited_berserker", "minecraft:blaze", "minecraft:blaze"),
                List.of("cataclysm:the_leviathan")), // 80

        // ═══ ARC 9 (81-90) — LA GESTE DÉMONIAQUE (§5, violet) ════════════════
        new Theme("§5La Geste Démoniaque — L'Antichambre du Fléau",
                List.of("epic_mobs:shadow_guard", "slu:dark_knight", "minecraft:vex"),
                List.of("epic_mobs:tech_knight")), // 81
        new Theme("§5La Geste Démoniaque — La Salle des Rituels",
                List.of("minecraft:blaze", "cataclysm:royal_draugr",
                        "minecraft:vex"),
                List.of("cataclysm:the_harbinger")), // 82
        new Theme("§5La Geste Démoniaque — Le Couloir aux Cent Pattes",
                List.of("minecraft:cave_spider", "minecraft:spider", "mowziesmobs:umvuthi"),
                List.of("epic_mobs:shadow_guard")), // 83
        new Theme("§5La Geste Démoniaque — Les Appartements du Non-Mort",
                List.of("slu:knight", "cataclysm:deepling_priest", "slu:knight"),
                List.of("cataclysm:elite_draugr")), // 84
        new Theme("§5La Geste Démoniaque — La Salle de la Perdition",
                List.of("epic_mobs:tech_knight", "minecraft:pillager",
                        "cataclysm:royal_draugr"),
                List.of("mutantmonsters:mutant_creeper")), // 85
        new Theme("§5La Geste Démoniaque — Le Jardin des Âmes",
                List.of("epic_mobs:shadow_guard", "epic_mobs:shadow_guard",
                        "cataclysm:royal_draugr"),
                List.of("epic_mobs:the_knight")), // 86
        new Theme("§5La Geste Démoniaque — Le Vestibule des Martyrs",
                List.of("minecraft:blaze", "mowziesmobs:naga", "minecraft:cave_spider"),
                List.of("cataclysm:the_harbinger")), // 87
        new Theme("§5La Geste Démoniaque — La Fonderie des Supplices",
                List.of("cataclysm:aptrgangr", "epic_mobs:crystal_guardian", "minecraft:spider"),
                List.of("mutantmonsters:mutant_enderman")), // 88
        new Theme("§5La Geste Démoniaque — Le Pont des Âmes Perdues",
                List.of("epic_mobs:tech_knight", "cataclysm:deepling_priest",
                        "epic_mobs:shadow_guard"),
                List.of("epic_mobs:shadow_guard")), // 89
        new Theme("§5La Geste Démoniaque — Le Trône du Fléau",
                List.of("epic_mobs:tech_knight", "minecraft:blaze", "cataclysm:royal_draugr"),
                List.of("cataclysm:the_leviathan")), // 90

        // ═══ ARC 10 (91-100) — LE TRÔNE DU NÉANT (§5, violet) ════════════════
        new Theme("§5Le Trône du Néant — L'Antichambre du Vide",
                List.of("minecraft:enderman", "minecraft:endermite", "slu:monster_godrick_soldier"),
                List.of("slu:monster_crucible_knight")), // 91
        new Theme("§5Le Trône du Néant — La Galerie des Reflets Faux",
                List.of("slu:white_phantom", "minecraft:shulker", "cataclysm:endermaptera"),
                List.of("slu:monster_successor")), // 92
        new Theme("§5Le Trône du Néant — Le Jardin de Cristal Mort",
                List.of("minecraft:enderman", "cataclysm:ender_golem", "minecraft:endermite"),
                List.of("cataclysm:coralssus")), // 93
        new Theme("§5Le Trône du Néant — Les Marches du Silence",
                List.of("slu:monster_godrick_knight", "slu:clone_abyss_watcher", "minecraft:shulker"),
                List.of("slu:bad_omen_giant")), // 94
        new Theme("§5Le Trône du Néant — Le Reliquaire de la Couronne",
                List.of("slu:monster_crusader", "cataclysm:endermaptera", "minecraft:phantom"),
                List.of("slu:monster_successor")), // 95
        new Theme("§5Le Trône du Néant — La Salle des Souverains Effacés",
                List.of("slu:monster_blasphemy_knight", "slu:temple_guard", "minecraft:enderman"),
                List.of("slu:bad_omen_giant")), // 96
        new Theme("§5Le Trône du Néant — Le Pont au-dessus de Rien",
                List.of("minecraft:phantom", "slu:executor", "slu:white_phantom"),
                List.of("cataclysm:ender_golem")), // 97
        new Theme("§5Le Trône du Néant — La Chambre des Échos",
                List.of("slu:monster_crucible_knight", "slu:dungeon_knight",
                        "minecraft:shulker"),
                List.of("mutantmonsters:mutant_enderman")), // 98
        new Theme("§5Le Trône du Néant — L'Ultime Garde",
                List.of("slu:monster_crucible_knight_2", "slu:monster_blasphemy_knight",
                        "slu:monster_tower_knight"),
                List.of("cataclysm:scylla")), // 99
        new Theme("§5Le Trône du Néant — Le Trône qui Regarde en Bas",
                List.of("slu:monster_blasphemy_knight", "slu:magma_giant", "cataclysm:the_leviathan"),
                List.of("slu:havel")), // 100
    };

    public static int count() { return THEMES.length; }

    /** Thème de l'étage (boucle au-delà de 100). */
    public static Theme forFloor(int floor) {
        if (floor <= 0) return THEMES[0];
        return THEMES[(floor - 1) % THEMES.length];
    }
}

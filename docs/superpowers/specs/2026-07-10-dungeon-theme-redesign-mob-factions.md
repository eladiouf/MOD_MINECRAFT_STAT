# Dungeon Theme Redesign — Mob-Faction Arcs

> **Spec :** Redesign complet des 100 thèmes du Trial Dungeon basé sur les factions de mobs du modpack.
> **Décision :** Onivo Studio — 2026-07-10
> **Statut :** Validé

## Résumé

Les 10 arcs narratifs actuels sont remplacés par 10 arcs basés sur les **familles de mobs** des 12 mods du modpack (+ vanilla). Chaque arc pioche dans 2-5 mods partageant une **vibe commune**, assurant cohérence visuelle et diversité de gameplay.

## Architecture

```
DungeonThemes.forFloor(floor)     → Theme (noms + mobs)
ThemePalette.forFloor(floor)      → BlockPalette (12 blocs)
MacawDungeonDecorator.themedDoor() → porte Macaw assortie
```

Seuls changent : `DungeonThemes.java`, `ThemePalette.java`, `MacawDungeonDecorator.java`.
Le reste (`DungeonArchitect`, `DungeonRoomChain`, `ModdedMobPool`, etc.) reste intact.

## Les 10 Arcs

### Arc 1 — LES DÉCHARNÉS (Floors 1-10)

**Identité :** Ce qui reste d'une garnison oubliée — pierre craquelée, rouille, os blanchis.

| Mod | Mobs |
|-----|------|
| SLU | hollow, armed_hollow, thief, hollow_soldier_sword, hollow_soldier_spear, twisted_souls |
| Vanilla | zombie, skeleton, husk, bogged, zombie_villager, slime, creeper, silverfish, iron_golem |
| Born in Chaos | decaying_zombie, decrepit_skeleton, baby_skeleton, barrel_zombie, zombie_fisherman, zombie_lumberjack, withered_corpse |
| Block Factory | soul_skeleton, frozen_skeleton, flaming_skeleton_guard_sword |
| Ice and Fire | ghost, dread_thrall, dread_ghoul, dread_beast |
| **Total** | **29 types** |

**Palette (ThemePalette.DECHARNES):**
- base=COBBLESTONE, accent=CRACKED_STONE_BRICKS, light=TORCH, underside=STONE
- decorPrimary=MOSSY_COBBLESTONE, decorSecondary=GRAVEL
- wallBlock=COBBLESTONE_WALL, stair=COBBLESTONE_STAIRS, slab=COBBLESTONE_SLAB
- ceiling=OAK_PLANKS, scar=GRAVEL, banner=RED_WOOL

**Door (Macaw):** `oak_whispering_door`

---

### Arc 2 — LES FAUVES (Floors 11-20)

**Identité :** La ménagerie — la nature a repris ses droits, mousse, lianes, racines.

| Mod | Mobs |
|-----|------|
| Tensura | direwolf, giant_bat, horned_rabbit, hound_dog, black_spider, army_wasp, giant_bear, knight_spider, barghest |
| Vanilla | spider, cave_spider, wolf, stray |
| Mowzie's | foliaath, umvuthana_raptor, grottol |
| Alex's Mobs | komodo_dragon, cave_centipede, crimson_mosquito, tarantula_hawk |
| Cataclysm | koboleton, the_prowler |
| Ice and Fire | cyclops, troll, hippogryph, myrmex_worker, myrmex_soldier, death_worm, amphithere |
| **Total** | **29 types** |

**Palette (ThemePalette.FAUVES):**
- base=MOSSY_COBBLESTONE, accent=STRIPPED_JUNGLE_LOG, light=TORCH, underside=STONE
- decorPrimary=JUNGLE_LEAVES, decorSecondary=MOSS_BLOCK
- wallBlock=MOSSY_COBBLESTONE_WALL, stair=MOSSY_COBBLESTONE_STAIRS, slab=MOSSY_COBBLESTONE_SLAB
- ceiling=JUNGLE_PLANKS, scar=MOSS_BLOCK, banner=GREEN_WOOL

**Door:** `jungle_swamp_door`

---

### Arc 3 — LES TRIBUS (Floors 21-30)

**Identité :** Camp de guerre pétrifié — palissades, tentes, étendards déchirés.

| Mod | Mobs |
|-----|------|
| Tensura | goblin, orc, lizardman, hound_dog, orc_lord, orc_disaster |
| Vanilla | pillager, vindicator, ravager, piglin, zombified_piglin |
| SLU | hollow_knight, knight, castle_guard |
| Mowzie's | umvuthana_crane |
| Ice and Fire | cyclops, troll |
| **Total** | **17 types** |

**Palette (ThemePalette.TRIBUS):**
- base=STONE_BRICKS, accent=DARK_OAK_PLANKS, light=LANTERN, underside=STONE
- decorPrimary=CHISELED_STONE_BRICKS, decorSecondary=COBBLESTONE
- wallBlock=STONE_BRICK_WALL, stair=STONE_BRICK_STAIRS, slab=STONE_BRICK_SLAB
- ceiling=DARK_OAK_PLANKS, scar=COBBLESTONE, banner=GRAY_WOOL

**Door:** `spruce_classic_door`

---

### Arc 4 — LA LÉGION NOIRE (Floors 31-40)

**Identité :** Forteresse des ténèbres — ardoise profonde, fers rouillés, âmes en peine.

| Mod | Mobs |
|-----|------|
| SLU | dungeon_knight, nightmare_knight, ghost_samurai, dark_spirit, elite_knight, noble_knight |
| Cataclysm | draugr, elite_draugr |
| Born in Chaos | skeleton_thrasher, zombie_bruiser, dread_hound, bonescaller, skeleton_demoman, restless_spirit, lifestealer, supreme_bonescaller, nightmare_stalker |
| Mutant | mutant_zombie, mutant_skeleton |
| Vanilla | vindicator, evoker, enderman |
| Mowzie's | ferrous_wroughtnaut |
| Ice and Fire | dread_knight, dread_beast, ghost |
| **Total** | **26 types** |

**Palette (ThemePalette.LEGION):**
- base=DEEPSLATE_TILES, accent=POLISHED_DEEPSLATE, light=SOUL_LANTERN, underside=DEEPSLATE
- decorPrimary=DEEPSLATE_BRICKS, decorSecondary=CRACKED_DEEPSLATE_TILES
- wallBlock=DEEPSLATE_BRICK_WALL, stair=DEEPSLATE_TILE_STAIRS, slab=DEEPSLATE_TILE_SLAB
- ceiling=POLISHED_DEEPSLATE, scar=COBBLED_DEEPSLATE, banner=BLUE_WOOL

**Door:** `dark_oak_mystic_door`

---

### Arc 5 — LES ABYSSES (Floors 41-50)

**Identité :** Palais englouti — corail, prismarine, épaves, marées noires.

| Mod | Mobs |
|-----|------|
| Cataclysm | deepling, deepling_brute, deepling_warlock, deepling_angler, amethyst_crab, urchinkin, coral_golem, hippocamtus, drowned_host, coralssus, wadjet |
| Vanilla | drowned, guardian, elder_guardian |
| Block Factory | crossbow_pirate, pirate_rook, soul_skeleton, pirate_captain, underworld_knight |
| Alex's Mobs | bone_serpent |
| Ice and Fire | sea_serpent, hydra, siren, hippocampus |
| **Total** | **24 types** |

**Palette (ThemePalette.ABYSSES):**
- base=PRISMARINE_BRICKS, accent=DARK_PRISMARINE, light=SEA_LANTERN, underside=PRISMARINE
- decorPrimary=WET_SPONGE, decorSecondary=DRIED_KELP_BLOCK
- wallBlock=PRISMARINE_WALL, stair=PRISMARINE_BRICK_STAIRS, slab=PRISMARINE_BRICK_SLAB
- ceiling=PRISMARINE, scar=GRAVEL, banner=CYAN_WOOL

**Door:** `warped_beach_door`

---

### Arc 6 — LE CERCLE DES MAGES (Floors 51-60)

**Identité :** Académie de magie en ruine — calcite, améthyste, livres brûlés, expériences libérées.

| Mod | Mobs |
|-----|------|
| Iron's Spellbooks | cultist, catacombs_zombie, ice_spider, pyromancer, cryomancer, magehunter_vindicator, apothecarist, priest, archevoker, necromancer |
| Tensura | salamander, undine, sylphide, ifrit, elemental_colossus |
| Cataclysm | netherite_monstrosity |
| Vanilla | witch, evoker, breeze, vex |
| Ice and Fire | gorgon, cockatrice, dread_lich |
| **Total** | **23 types** |

**Palette (ThemePalette.MAGES):**
- base=CALCITE, accent=AMETHYST_BLOCK, light=AMETHYST_CLUSTER, underside=STONE
- decorPrimary=BOOKSHELF, decorSecondary=CHISELED_STONE_BRICKS
- wallBlock=CALCITE, stair=CALCITE_STAIRS, slab=CALCITE_SLAB (fallback stone)
- ceiling=POLISHED_ANDESITE, scar=CRYING_OBSIDIAN, banner=MAGENTA_WOOL

**Door:** `cherry_mystic_door`

---

### Arc 7 — LA MOISSON DE L'EFFROI (Floors 61-70)

**Identité :** Fête des récoltes devenue cauchemar — citrouilles, chaume, masques, lanternes grimaçantes.

| Mod | Mobs |
|-----|------|
| Born in Chaos | pumpkin_bruiser, sir_pumpkinhead, pumpkin_dunce, mr_pumpkin, pumpkin_spirit, lord_pumpkinhead, senor_pumpkin, zombie_clown, seared_spirit |
| SLU | dark_knight, ringed_knight, shadow_assassin, mad_knight, wither_skeleton_knight |
| Vanilla | witch, phantom, vex |
| Ice and Fire | stymphalian_bird, cockatrice |
| **Total** | **19 types** |

**Palette (ThemePalette.MOISSON):**
- base=STRIPPED_DARK_OAK_LOG, accent=CARVED_PUMPKIN, light=JACK_O_LANTERN, underside=DARK_OAK_LOG
- decorPrimary=HAY_BLOCK, decorSecondary=COARSE_DIRT
- wallBlock=DARK_OAK_FENCE, stair=DARK_OAK_STAIRS, slab=DARK_OAK_SLAB
- ceiling=SPRUCE_PLANKS, scar=PODZOL, banner=ORANGE_WOOL

**Door:** `dark_oak_cottage_door`

---

### Arc 8 — LA FOURNAISE (Floors 71-80)

**Identité :** Forges infernales — lave, magma, chaînes, soufflets rugissants.

| Mod | Mobs |
|-----|------|
| Cataclysm | ignited_berserker, ignited_revenant, netherite_monstrosity |
| Vanilla | blaze, magma_cube, wither_skeleton, piglin_brute, hoglin |
| Tensura | lesser_daemon, hell_moth, salamander |
| Born in Chaos | firelight, seared_spirit |
| Alex's Mobs | crimson_mosquito, warped_mosco, soul_vulture |
| Block Factory | soul_knight_wither_skeleton |
| Ice and Fire | fire_dragon, amphithere |
| **Total** | **19 types** |

**Palette (ThemePalette.FOURNAISE):**
- base=POLISHED_BLACKSTONE_BRICKS, accent=NETHER_BRICKS, light=SHROOMLIGHT, underside=BASALT
- decorPrimary=MAGMA_BLOCK, decorSecondary=GILDED_BLACKSTONE
- wallBlock=POLISHED_BLACKSTONE_BRICK_WALL, stair=POLISHED_BLACKSTONE_BRICK_STAIRS, slab=POLISHED_BLACKSTONE_BRICK_SLAB
- ceiling=CRIMSON_PLANKS, scar=CRIMSON_NYLIUM, banner=RED_WOOL

**Door:** `dark_oak_nether_door`

---

### Arc 9 — LA GESTE DÉMONIAQUE (Floors 81-90)

**Identité :** Invasion démoniaque — la réalité se fissure, le vide suinte à travers les murs.

| Mod | Mobs |
|-----|------|
| Tensura | greater_daemon, arch_daemon, evil_centipede, basilisk, bone_golem, hell_caterpillar, charybdis |
| Born in Chaos | fallen_chaos_knight, door_knight, infernal_spirit, dire_hound_leader, mother_spider, missionary_raider, scarlet_persecutor, dark_vortex, lord_of_depths |
| Cataclysm | royal_draugr, the_harbinger, aptrgangr, deepling_priest |
| Mutant | mutant_creeper, mutant_enderman |
| Mowzie's | naga, umvuthi |
| Ice and Fire | ice_dragon, lightning_dragon, dread_lich |
| **Total** | **27 types** |

**Palette (ThemePalette.GESTE):**
- base=CRYING_OBSIDIAN, accent=SOUL_SAND, light=SOUL_TORCH, underside=END_STONE
- decorPrimary=BONE_BLOCK, decorSecondary=SHROOMLIGHT
- wallBlock=CRYING_OBSIDIAN, stair=STONE_BRICK_STAIRS, slab=STONE_BRICK_SLAB
- ceiling=CRIMSON_PLANKS, scar=CRYING_OBSIDIAN, banner=PURPLE_WOOL

**Door:** `warped_nether_door`

---

### Arc 10 — LE TRÔNE DU NÉANT (Floors 91-100)

**Identité :** La cour du vide — les gardiens de la fin, le trône qui regarde en bas.

| Mod | Mobs |
|-----|------|
| SLU | monster_crucible_knight, monster_crucible_knight_2, monster_tower_knight, monster_godrick_knight, monster_godrick_soldier, monster_blasphemy_knight, monster_crusader, magma_giant, bad_omen_giant, executor, white_phantom, havel, monster_successor, clone_abyss_watcher, temple_guard |
| Vanilla | enderman, endermite, shulker, phantom |
| Cataclysm | endermaptera, ender_golem, the_leviathan, scylla |
| Block Factory | dragon_guard_sword |
| Ice and Fire | lightning_dragon, ghost |
| **Total** | **26 types** |

**Palette (ThemePalette.NEANT):**
- base=END_STONE, accent=PURPUR_BLOCK, light=END_ROD, underside=END_STONE
- decorPrimary=AMETHYST_BLOCK, decorSecondary=CHORUS_FLOWER
- wallBlock=END_STONE_BRICK_WALL, stair=END_STONE_BRICK_STAIRS, slab=END_STONE_BRICK_SLAB
- ceiling=PURPUR_PILLAR, scar=CRYING_OBSIDIAN, banner=PURPLE_WOOL

**Door:** `cherry_whispering_door`

---

## Définition des 100 Thèmes

Chaque thème suit le format :
```java
new Theme("§<color>Nom de l'Arc — Nom de l'Étage",
    List.of("mod:mob1", "mod:mob2", "mod:mob3"),
    List.of("mod:miniboss"))
```

### Arc 1 — LES DÉCHARNÉS (§7, gris)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 1 | La Herse Rouillée | zombie, skeleton, slu:hollow | minecraft:iron_golem |
| 2 | Les Geôles Effondrées | slu:hollow, slu:armed_hollow, minecraft:silverfish | slu:havel |
| 3 | L'Armurerie des Morts | slu:thief, born_in_chaos_v1:decaying_zombie, minecraft:husk | slu:executor |
| 4 | Le Réfectoire aux Mouches | minecraft:cave_spider, born_in_chaos_v1:barrel_zombie, slu:hollow_soldier_sword | minecraft:creeper |
| 5 | La Salle de Garde Abandonnée | slu:hollow_soldier_spear, minecraft:skeleton, slu:twisted_souls | minecraft:bogged |
| 6 | Le Puits aux Chaînes | born_in_chaos_v1:decrepit_skeleton, minecraft:zombie_villager, block_factorys_bosses:frozen_skeleton | slu:thief |
| 7 | La Chapelle des Os | block_factorys_bosses:soul_skeleton, born_in_chaos_v1:zombie_fisherman, born_in_chaos_v1:baby_skeleton | slu:armed_hollow |
| 8 | L'Antichambre des Rats | minecraft:slime, slu:thief, born_in_chaos_v1:zombie_lumberjack | minecraft:witch |
| 9 | La Cour aux Supplices | slu:hollow_soldier_sword, born_in_chaos_v1:withered_corpse, minecraft:husk | minecraft:iron_golem |
| 10 | La Porte que Nul ne Garde | slu:armed_hollow, block_factorys_bosses:flaming_skeleton_guard_sword, minecraft:bogged | cataclysm:ignited_revenant |

### Arc 2 — LES FAUVES (§2, vert)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 11 | L'Enclos Éventré | tensura:direwolf, minecraft:wolf, tensura:hound_dog | tensura:barghest |
| 12 | La Tanière du Prédateur | tensura:horned_rabbit, tensura:giant_bat, mowziesmobs:foliaath | born_in_chaos_v1:nightmare_stalker |
| 13 | Le Terrier des Tisseuses | minecraft:spider, minecraft:cave_spider, tensura:black_spider | mowziesmobs:foliaath |
| 14 | La Volière aux Dards | alexsmobs:cave_centipede, alexsmobs:crimson_mosquito, tensura:army_wasp | cataclysm:koboleton |
| 15 | La Grotte aux Fauves | tensura:direwolf, tensura:horned_rabbit, mowziesmobs:umvuthana_raptor | alexsmobs:komodo_dragon |
| 16 | Le Verger Empoisonné | alexsmobs:komodo_dragon, minecraft:stray, tensura:black_spider | tensura:giant_bat |
| 17 | La Clairière des Chairs | mowziesmobs:foliaath, tensura:hound_dog, minecraft:spider | cataclysm:koboleton |
| 18 | Les Fourrés Mouvants | minecraft:wolf, mowziesmobs:grottol, tensura:giant_bear | alexsmobs:komodo_dragon |
| 19 | Le Nid du Silencieux | tensura:knight_spider, tensura:black_spider, alexsmobs:tarantula_hawk | tensura:barghest |
| 20 | Le Repaire de l'Alpha | tensura:direwolf, minecraft:wolf, tensura:hound_dog | cataclysm:the_prowler |

### Arc 3 — LES TRIBUS (§c, rouge)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 21 | Le Camp des Éclaireurs | tensura:goblin, minecraft:pillager, slu:knight | tensura:orc_lord |
| 22 | La Palissade Hurlante | minecraft:vindicator, minecraft:pillager, tensura:orc | minecraft:ravager |
| 23 | L'Armurerie des Félons | slu:castle_guard, slu:hollow_knight, minecraft:vindicator | slu:monster_tower_knight |
| 24 | Le Chenil de Guerre | tensura:hound_dog, minecraft:zombified_piglin, tensura:lizardman | tensura:orc_disaster |
| 25 | La Solde du Mercenaire | minecraft:pillager, tensura:goblin, slu:thief | minecraft:ravager |
| 26 | La Cour des Duels | slu:dungeon_knight, slu:nightmare_knight, minecraft:evoker | slu:knight |
| 27 | Les Écuries Brûlées | tensura:orc, minecraft:zombified_piglin, minecraft:piglin | tensura:orc_lord |
| 28 | La Tente du Stratège | mowziesmobs:umvuthana_crane, minecraft:evoker, slu:castle_guard | minecraft:vindicator |
| 29 | Le Front des Bannières | slu:elite_knight, tensura:lizardman, minecraft:pillager | tensura:orc_disaster |
| 30 | L'État-Major Renégat | minecraft:ravager, slu:nightmare_knight, tensura:orc | cataclysm:the_prowler |

### Arc 4 — LA LÉGION NOIRE (§8, gris foncé)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 31 | Les Fosses des Damnés | cataclysm:draugr, born_in_chaos_v1:skeleton_thrasher, slu:ghost_samurai | cataclysm:elite_draugr |
| 32 | Le Caveau des Traîtres | slu:dungeon_knight, slu:noble_knight, minecraft:vindicator | born_in_chaos_v1:nightmare_stalker |
| 33 | La Salle des Suaires | born_in_chaos_v1:zombie_bruiser, born_in_chaos_v1:supreme_bonescaller, minecraft:enderman | mutantmonsters:mutant_zombie |
| 34 | Le Pont des Soupirs | slu:elite_knight, born_in_chaos_v1:skeleton_demoman, minecraft:evoker | cataclysm:draugr |
| 35 | L'Armurerie Noire | slu:ghost_samurai, slu:dungeon_knight, born_in_chaos_v1:bonescaller | slu:dark_spirit |
| 36 | Le Donjon aux Mille Yeux | minecraft:enderman, born_in_chaos_v1:restless_spirit, born_in_chaos_v1:skeleton_thrasher | mutantmonsters:mutant_skeleton |
| 37 | Les Écuries Spectrales | born_in_chaos_v1:lifestealer, slu:elite_knight, cataclysm:elite_draugr | born_in_chaos_v1:zombie_bruiser |
| 38 | La Crypte des Félons | slu:nightmare_knight, minecraft:enderman, mowziesmobs:ferrous_wroughtnaut | cataclysm:elite_draugr |
| 39 | La Cour des Ombres | slu:ghost_samurai, born_in_chaos_v1:supreme_bonescaller, cataclysm:draugr | born_in_chaos_v1:nightmare_stalker |
| 40 | Le Trône de la Légion | cataclysm:elite_draugr, slu:elite_knight, born_in_chaos_v1:dread_hound | cataclysm:the_prowler |

### Arc 5 — LES ABYSSES (§3, cyan)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 41 | Le Vestibule des Marées | cataclysm:deepling, minecraft:drowned, minecraft:elder_guardian | cataclysm:wadjet |
| 42 | Les Jardins de Corail | cataclysm:amethyst_crab, cataclysm:urchinkin, cataclysm:coral_golem | cataclysm:coralssus |
| 43 | La Cale des Écumeurs | block_factorys_bosses:crossbow_pirate, block_factorys_bosses:pirate_rook, minecraft:drowned | block_factorys_bosses:pirate_captain |
| 44 | La Salle de Bal Immergée | cataclysm:drowned_host, cataclysm:deepling, minecraft:guardian | cataclysm:wadjet |
| 45 | Le Trésor de la Frégate | block_factorys_bosses:crossbow_pirate, minecraft:drowned, cataclysm:deepling | block_factorys_bosses:soul_skeleton |
| 46 | L'Aquarium des Monstres | cataclysm:hippocamtus, alexsmobs:bone_serpent, cataclysm:amethyst_crab | cataclysm:coralssus |
| 47 | Les Autels de la Vase | cataclysm:deepling_warlock, cataclysm:deepling_angler, minecraft:drowned | cataclysm:wadjet |
| 48 | Le Chœur des Abysses | cataclysm:deepling_priest, cataclysm:deepling_warlock, minecraft:guardian | cataclysm:coralssus |
| 49 | Le Pont des Amiraux | block_factorys_bosses:pirate_rook, cataclysm:deepling_brute, cataclysm:deepling | block_factorys_bosses:underworld_knight |
| 50 | La Salle du Trône Englouti | cataclysm:deepling_brute, cataclysm:deepling_priest, minecraft:drowned | cataclysm:ender_golem |

### Arc 6 — LE CERCLE DES MAGES (§d, rose)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 51 | Le Hall des Apprentis | irons_spellbooks:cultist, minecraft:witch, tensura:salamander | irons_spellbooks:archevoker |
| 52 | L'Amphithéâtre des Flammes | irons_spellbooks:pyromancer, tensura:undine, minecraft:breeze | tensura:ifrit |
| 53 | La Bibliothèque Interdite | irons_spellbooks:catacombs_zombie, irons_spellbooks:priest, minecraft:vex | irons_spellbooks:cryomancer |
| 54 | Le Laboratoire des Golems | irons_spellbooks:magehunter_vindicator, tensura:sylphide, minecraft:evoker | tensura:elemental_colossus |
| 55 | Le Coffre du Doyen | irons_spellbooks:cryomancer, minecraft:witch, irons_spellbooks:apothecarist | irons_spellbooks:pyromancer |
| 56 | L'Aile des Élémentaires | tensura:salamander, tensura:undine, tensura:sylphide | tensura:ifrit |
| 57 | Le Givre de la Chaire | irons_spellbooks:cryomancer, irons_spellbooks:ice_spider, tensura:undine | irons_spellbooks:apothecarist |
| 58 | Les Ateliers de Chair | irons_spellbooks:necromancer, irons_spellbooks:catacombs_zombie, minecraft:breeze | irons_spellbooks:magehunter_vindicator |
| 59 | L'Observatoire des Tempêtes | minecraft:breeze, tensura:sylphide, irons_spellbooks:pyromancer | tensura:elemental_colossus |
| 60 | La Thèse Finale | irons_spellbooks:archevoker, irons_spellbooks:necromancer, tensura:undine | cataclysm:netherite_monstrosity |

### Arc 7 — LA MOISSON DE L'EFFROI (§6, or)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 61 | La Procession des Lanternes | born_in_chaos_v1:pumpkin_dunce, born_in_chaos_v1:mr_pumpkin, minecraft:witch | born_in_chaos_v1:sir_pumpkinhead |
| 62 | Le Bal des Épouvantails | born_in_chaos_v1:zombie_clown, born_in_chaos_v1:pumpkin_dunce, minecraft:phantom | born_in_chaos_v1:sir_pumpkinhead |
| 63 | Les Feux Follets du Verger | born_in_chaos_v1:seared_spirit, slu:dark_knight, minecraft:vex | born_in_chaos_v1:nightmare_stalker |
| 64 | Le Pressoir aux Ombres | slu:ringed_knight, born_in_chaos_v1:pumpkin_spirit, minecraft:witch | born_in_chaos_v1:lord_pumpkinhead |
| 65 | Le Grenier du Rémouleur | born_in_chaos_v1:mr_pumpkin, slu:shadow_assassin, born_in_chaos_v1:zombie_clown | slu:monster_successor |
| 66 | La Ronde des Masques | slu:ringed_knight, slu:mad_knight, born_in_chaos_v1:seared_spirit | born_in_chaos_v1:lord_pumpkinhead |
| 67 | Le Champ des Têtes Sculptées | born_in_chaos_v1:pumpkin_bruiser, born_in_chaos_v1:senor_pumpkin, born_in_chaos_v1:pumpkin_dunce | born_in_chaos_v1:sir_pumpkinhead |
| 68 | Les Brasiers de la Veillée | born_in_chaos_v1:seared_spirit, slu:wither_skeleton_knight, minecraft:vex | born_in_chaos_v1:nightmare_stalker |
| 69 | Le Cortège de Minuit | slu:mad_knight, slu:shadow_assassin, born_in_chaos_v1:pumpkin_spirit | slu:monster_successor |
| 70 | Le Banquet du Roi-Citrouille | born_in_chaos_v1:pumpkin_bruiser, born_in_chaos_v1:senor_pumpkin, born_in_chaos_v1:zombie_clown | cataclysm:netherite_monstrosity |

### Arc 8 — LA FOURNAISE (§4, rouge foncé)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 71 | Les Soufflets Hurlants | minecraft:blaze, tensura:lesser_daemon, born_in_chaos_v1:firelight | tensura:arch_daemon |
| 72 | La Chaîne des Enclumes | minecraft:wither_skeleton, block_factorys_bosses:soul_knight_wither_skeleton, cataclysm:ignited_berserker | cataclysm:netherite_monstrosity |
| 73 | Le Bassin de Trempe | minecraft:magma_cube, minecraft:hoglin, alexsmobs:crimson_mosquito | cataclysm:ignited_berserker |
| 74 | Les Galeries du Minerai Noir | cataclysm:ignited_berserker, born_in_chaos_v1:seared_spirit, minecraft:wither_skeleton | minecraft:hoglin |
| 75 | La Paie des Damnés | tensura:lesser_daemon, alexsmobs:soul_vulture, minecraft:piglin_brute | tensura:arch_daemon |
| 76 | L'Atelier des Armures | alexsmobs:warped_mosco, tensura:hell_moth, minecraft:blaze | born_in_chaos_v1:seared_spirit |
| 77 | Le Tribunal des Braises | cataclysm:ignited_berserker, alexsmobs:crimson_mosquito, tensura:salamander | cataclysm:ignited_revenant |
| 78 | La Fonderie des Colosses | minecraft:hoglin, minecraft:magma_cube, alexsmobs:soul_vulture | minecraft:piglin_brute |
| 79 | Le Couloir des Fours Ouverts | tensura:lesser_daemon, minecraft:blaze, born_in_chaos_v1:firelight | tensura:arch_daemon |
| 80 | Le Maître des Soufflets | cataclysm:ignited_berserker, minecraft:blaze, tensura:hell_moth | cataclysm:the_leviathan |

### Arc 9 — LA GESTE DÉMONIAQUE (§5, violet)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 81 | L'Antichambre du Fléau | tensura:greater_daemon, born_in_chaos_v1:fallen_chaos_knight, minecraft:vex | tensura:arch_daemon |
| 82 | La Salle des Rituels | born_in_chaos_v1:infernal_spirit, cataclysm:royal_draugr, born_in_chaos_v1:dark_vortex | cataclysm:the_harbinger |
| 83 | Le Couloir aux Cent Pattes | tensura:evil_centipede, tensura:hell_caterpillar, mowziesmobs:umvuthi | tensura:greater_daemon |
| 84 | Les Appartements du Non-Mort | born_in_chaos_v1:door_knight, cataclysm:deepling_priest, tensura:bone_golem | born_in_chaos_v1:dire_hound_leader |
| 85 | La Salle de la Perdition | tensura:arch_daemon, born_in_chaos_v1:missionary_raider, born_in_chaos_v1:lord_of_depths | mutantmonsters:mutant_creeper |
| 86 | Le Jardin des Âmes | born_in_chaos_v1:scarlet_persecutor, tensura:greater_daemon, cataclysm:royal_draugr | tensura:charybdis |
| 87 | Le Vestibule des Martyrs | born_in_chaos_v1:infernal_spirit, mowziesmobs:naga, tensura:evil_centipede | cataclysm:the_harbinger |
| 88 | La Fonderie des Supplices | cataclysm:aptrgangr, tensura:basilisk, born_in_chaos_v1:mother_spider | mutantmonsters:mutant_enderman |
| 89 | Le Pont des Âmes Perdues | tensura:arch_daemon, cataclysm:deepling_priest, born_in_chaos_v1:scarlet_persecutor | tensura:greater_daemon |
| 90 | Le Trône du Fléau | tensura:arch_daemon, born_in_chaos_v1:infernal_spirit, cataclysm:royal_draugr | cataclysm:the_leviathan |

### Arc 10 — LE TRÔNE DU NÉANT (§5, violet)

| # | Nom | Adds | Mini-boss |
|---|-----|------|-----------|
| 91 | L'Antichambre du Vide | minecraft:enderman, minecraft:endermite, slu:monster_godrick_soldier | slu:monster_crucible_knight |
| 92 | La Galerie des Reflets Faux | slu:white_phantom, minecraft:shulker, cataclysm:endermaptera | slu:monster_successor |
| 93 | Le Jardin de Cristal Mort | minecraft:enderman, cataclysm:ender_golem, minecraft:endermite | cataclysm:coralssus |
| 94 | Les Marches du Silence | slu:monster_godrick_knight, slu:clone_abyss_watcher, minecraft:shulker | slu:bad_omen_giant |
| 95 | Le Reliquaire de la Couronne | slu:monster_crusader, cataclysm:endermaptera, minecraft:phantom | slu:monster_successor |
| 96 | La Salle des Souverains Effacés | slu:monster_blasphemy_knight, slu:temple_guard, minecraft:enderman | slu:bad_omen_giant |
| 97 | Le Pont au-dessus de Rien | minecraft:phantom, slu:executor, slu:white_phantom | cataclysm:ender_golem |
| 98 | La Chambre des Échos | slu:monster_crucible_knight, block_factorys_bosses:dragon_guard_sword, minecraft:shulker | mutantmonsters:mutant_enderman |
| 99 | L'Ultime Garde | slu:monster_crucible_knight_2, slu:monster_blasphemy_knight, slu:monster_tower_knight | cataclysm:scylla |
| 100 | Le Trône qui Regarde en Bas | slu:monster_blasphemy_knight, slu:magma_giant, cataclysm:the_leviathan | slu:havel |

## Fichiers Impactés

| Fichier | Changement |
|---------|-----------|
| `dungeon/DungeonThemes.java` | Réécriture complète des 100 thèmes |
| `dungeon/ThemePalette.java` | 10 nouvelles palettes (suppression des 4 Quark) |
| `dungeon/MacawDungeonDecorator.java` | `themedDoor()` switch mis à jour avec les 10 nouveaux enums |
| `dungeon/DungeonDetailing.java` | Switch mis à jour (cosmétique, code actuellement mort) |

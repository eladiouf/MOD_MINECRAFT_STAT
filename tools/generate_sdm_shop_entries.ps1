$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$inputFile = Join-Path $root 'docs/generated/sdm-shop-item-candidates.tsv'
$outputFile = Join-Path $root 'src/main/java/tong/statmod/integration/sdm/SDMShopCatalogEntries.java'
$allowedMods = @('minecraft', 'statmod', 'tensura', 'irons_spellbooks', 'iceandfire',
    'apotheosis', 'epicfight', 'simplyswords', 'magistuarmory', 'overgeared',
    'mutantmonsters', 'farmersdelight')

$candidates = Import-Csv -Delimiter ([char]9) -LiteralPath $inputFile |
    Where-Object { $allowedMods -contains $_.mod_id } |
    Group-Object item_id |
    ForEach-Object { $_.Group[0] } |
    Sort-Object item_id

$selected = [System.Collections.Generic.List[object]]::new()
$used = [System.Collections.Generic.HashSet[string]]::new()

function Get-CategoryProfile {
    param([string]$Tab)
    # BasePrice targets the middle of the tier band for weight-1.0 items;
    # rarity weights spread items across the band.
    $profiles = @{
        'Minerais bruts'              = @{ Tier=1; BasePrice=300;  Count=4 }
        'Lingots et gemmes'           = @{ Tier=2; BasePrice=1200; Count=2 }
        'Matériaux avancés'           = @{ Tier=3; BasePrice=6000; Count=1 }
        'Forge et amélioration'       = @{ Tier=2; BasePrice=1200; Count=1 }
        'Armes légères'               = @{ Tier=4; BasePrice=18000; Count=1 }
        'Armes lourdes'               = @{ Tier=4; BasePrice=20000; Count=1 }
        "Lances et armes d'hast"      = @{ Tier=3; BasePrice=6000; Count=1 }
        'Armes à distance'            = @{ Tier=3; BasePrice=5000; Count=1 }
        'Armes de Tensura'            = @{ Tier=5; BasePrice=75000; Count=1 }
        'Armes uniques et légendaires' = @{ Tier=5; BasePrice=60000; Count=1 }
        'Armures classiques'          = @{ Tier=3; BasePrice=6000; Count=1 }
        'Armures fantastiques'        = @{ Tier=4; BasePrice=18000; Count=1 }
        'Armures historiques'         = @{ Tier=4; BasePrice=18000; Count=1 }
        'Armures magiques'            = @{ Tier=4; BasePrice=18000; Count=1 }
        'Magie et parchemins'         = @{ Tier=3; BasePrice=6000; Count=1 }
        'Runes et composants magiques' = @{ Tier=3; BasePrice=5000; Count=1 }
        'Potions et soins'            = @{ Tier=1; BasePrice=300;  Count=4 }
        'Composants de monstres'      = @{ Tier=4; BasePrice=18000; Count=2 }
        'Nourriture'                  = @{ Tier=1; BasePrice=300;  Count=8 }
        'Fleurs, plantes et bois'     = @{ Tier=1; BasePrice=250;  Count=16 }
        'Construction'                = @{ Tier=1; BasePrice=300;  Count=16 }
        'Mobilité et transport'       = @{ Tier=2; BasePrice=1200; Count=1 }
        'Trophées et décoration'      = @{ Tier=2; BasePrice=1200; Count=1 }
        'Mécanismes et Redstone'      = @{ Tier=1; BasePrice=300;  Count=8 }
        'Utilitaires'                 = @{ Tier=2; BasePrice=1200; Count=1 }
        'Objets rares contrôlés'      = @{ Tier=4; BasePrice=22000; Count=1 }
    }
    return $profiles[$Tab]
}

function Get-TierBand {
    param([int]$Tier)
    $bands = @{
        1 = @{ Min=100; Max=800 }
        2 = @{ Min=800; Max=3000 }
        3 = @{ Min=3000; Max=12000 }
        4 = @{ Min=12000; Max=30000 }
        5 = @{ Min=50000; Max=300000 }
    }
    return $bands[$Tier]
}

function Get-RarityWeight {
    param([string]$ItemId)
    # Ultra-rare / boss tier
    if ($ItemId -match 'dragonsteel|mythic|legendary|boss_') { return 4.0 }
    # Mythic unique weapons — spatial_blade/stormbringer are super-rare boss drops
    if ($ItemId -match 'spatial_blade|dead_end|vorpal|stormbringer|hearthflame|soulpyre') { return 5.0 }
    # High-tier weapons and materials
    if ($ItemId -match 'netherite|adamantite|runic|unique|soulrender|soulstealer|soulkeeper|twisted_blade') { return 3.0 }
    # Mid-high: epic, enchanted golden apple, unique weapons
    if ($ItemId -match ':epic|enchanted_golden|pure_magisteel|hihiirokane|dragon(?!steel)|dread_sword|tide_trident|ghost_sword|hippogryph_sword|ice_blade|mad_swords|mirrorguard|wildvine|bloomsoul') { return 2.0 }
    # Mid: diamond, rare, high_magisteel, mithril, orichalcum, arcane_ingot
    if ($ItemId -match 'diamond|rare|high_magisteel|arcane_ingot|orichalcum|mithril') { return 1.5 }
    # Magistuarmory armor tiers for Armures historiques
    if ($ItemId -match 'crusader_') { return 2.0 }
    if ($ItemId -match 'maximilian_') { return 1.5 }
    if ($ItemId -match 'gothic_') { return 1.3 }
    if ($ItemId -match 'knight_') { return 1.2 }
    if ($ItemId -match 'plate_') { return 1.0 }
    if ($ItemId -match 'scale_') { return 0.8 }
    if ($ItemId -match 'chain_') { return 0.5 }
    # Low-tier base metals and common materials
    if ($ItemId -match 'iron_|copper_|gold_|stone_|wood_|tin_|bronze_|steel_|silver_') { return 0.8 }
    # Basic / raw materials
    if ($ItemId -match 'leather|chainmail|raw_|cobblestone|sand|gravel|dirt|planks|log_|sapling|seeds') { return 0.5 }
    # Consumable projectiles (arrows) — sold in stacks of 1, so unit price must stay low
    if ($ItemId -match 'arrow$') { return 0.3 }
    # Default for everything else
    return 1.0
}

function Get-PriceForItem {
    param([string]$Tab, [string]$ItemId, [int]$BasePrice)
    $profile = Get-CategoryProfile $Tab
    $band = Get-TierBand $profile.Tier
    $rarity = Get-RarityWeight $ItemId
    $price = [Math]::Round($BasePrice * $rarity)
    if ($price -lt $band.Min) { $price = $band.Min }
    if ($price -gt $band.Max) { $price = $band.Max }
    return $price
}

function Add-Category {
    param(
        [string]$Tab,
        [string]$Include,
        [string]$Exclude,
        [int]$Limit
    )
    $profile = Get-CategoryProfile $Tab
    $before = $selected.Count
    $matches = $candidates | Where-Object {
        $_.item_id -match $Include -and
        ([string]::IsNullOrEmpty($Exclude) -or $_.item_id -notmatch $Exclude) -and
        -not $used.Contains($_.item_id)
    } | Select-Object -First $Limit

    foreach ($candidate in $matches) {
        $id = $candidate.item_id
        $price = Get-PriceForItem -Tab $Tab -ItemId $id -BasePrice $profile.BasePrice
        $count = $profile.Count
    # Arrows sell in stacks; price per arrow then becomes palatable
    if ($Tab -eq "Armes à distance" -and $id -match 'arrow$') { $count = 32 }
    $selected.Add([pscustomobject]@{
            Tab=$Tab; Id=$id; Price=$price; Count=$count; Potion=$null
        })
        [void]$used.Add($id)
    }
    Write-Output "$Tab (TSV) = $($selected.Count - $before)"
}

# For categories where the TSV lacks items (vanilla items not in JAR scans),
# fallback to inline curated lists coupled with TSV items.
function Add-MixedCategory {
    param(
        [string]$Tab,
        [string]$Include,
        [string]$Exclude,
        [int]$Limit,
        [string[]]$InlineItems
    )
    $profile = Get-CategoryProfile $Tab
    $before = $selected.Count

    # Pick from TSV first
    $tsvMatches = $candidates | Where-Object {
        $_.item_id -match $Include -and
        ([string]::IsNullOrEmpty($Exclude) -or $_.item_id -notmatch $Exclude) -and
        -not $used.Contains($_.item_id)
    } | Select-Object -First $Limit

    $remaining = $Limit - $tsvMatches.Count
    if ($remaining -lt 0) { $remaining = 0 }

    foreach ($candidate in $tsvMatches) {
        $id = $candidate.item_id
        $price = Get-PriceForItem -Tab $Tab -ItemId $id -BasePrice $profile.BasePrice
        $count = $profile.Count
        if ($Tab -eq "Armes à distance" -and $id -match 'arrow$') { $count = 32 }
        $selected.Add([pscustomobject]@{
            Tab=$Tab; Id=$id; Price=$price; Count=$count; Potion=$null
        })
        [void]$used.Add($id)
    }

    # Fill remaining slots with inline fallback items
    $inlineAdded = 0
    foreach ($id in $InlineItems) {
        if ($inlineAdded -ge $remaining) { break }
        if ($used.Contains($id)) { continue }
        $price = Get-PriceForItem -Tab $Tab -ItemId $id -BasePrice $profile.BasePrice
        $count = $profile.Count
        if ($Tab -eq "Armes à distance" -and $id -match 'arrow$') { $count = 32 }
        $selected.Add([pscustomobject]@{
            Tab=$Tab; Id=$id; Price=$price; Count=$count; Potion=$null
        })
        [void]$used.Add($id)
        $inlineAdded++
    }
    Write-Output "$Tab = $($selected.Count - $before) (TSV=$($tsvMatches.Count), inline=$inlineAdded)"
}

# ── Tier 1 (100-800) ──────────────────────────────────────
Add-MixedCategory 'Minerais bruts' ':(raw_.+|.+_ore|ore_.+|coal)$' 'block|stairs|slab|wall|door|deepslate' 35 @(
    'minecraft:coal', 'minecraft:raw_iron', 'minecraft:raw_copper', 'minecraft:raw_gold',
    'minecraft:iron_nugget', 'minecraft:gold_nugget'
)

Add-MixedCategory 'Potions et soins' ':(potion|splash_potion|lingering_potion|.+golden_apple|golden_apple|golden_carrot|ambrosia|.+_elixir|.+_tea)$' 'chemical_x|spawn' 32 @(
    'minecraft:potion', 'minecraft:splash_potion', 'minecraft:lingering_potion',
    'minecraft:golden_apple', 'minecraft:enchanted_golden_apple', 'minecraft:golden_carrot',
    'minecraft:honey_bottle', 'minecraft:milk_bucket'
)

Add-MixedCategory 'Nourriture' ':(.+_(stew|soup|pie|cake|meat|beef|pork|chicken|fish|apple|berry|berries)|ambrosia|bread|cookie|mushroom_stew|rabbit_stew|beetroot_soup|pumpkin_pie)$' 'block|spawn|raw_|minced' 35 @(
    'minecraft:golden_apple', 'minecraft:enchanted_golden_apple',
    'minecraft:bread', 'minecraft:cookie', 'minecraft:mushroom_stew',
    'minecraft:rabbit_stew', 'minecraft:beetroot_soup', 'minecraft:pumpkin_pie',
    'minecraft:baked_potato', 'minecraft:cooked_beef', 'minecraft:cooked_porkchop',
    'minecraft:cooked_chicken', 'minecraft:cooked_cod', 'minecraft:cooked_salmon'
)

Add-MixedCategory 'Fleurs, plantes et bois' ':(sapling|log_|planks|seeds|sugar_cane|cactus|bamboo|bone_meal|flower|tulip|daisy|cornflower|lily_of_the_valley|wither_rose|moss|vine|nether_wart|crimson|warped|mangrove_propagule|spore_blossom|glow_berries|palm_|wart|fungus|nylium)' 'block|stairs|slab|wall|fence|door|trapdoor|sign|tool_rack|cabinet|basket' 55 @(
    'minecraft:oak_sapling', 'minecraft:spruce_sapling', 'minecraft:birch_sapling',
    'minecraft:jungle_sapling', 'minecraft:acacia_sapling', 'minecraft:dark_oak_sapling',
    'minecraft:cherry_sapling', 'minecraft:mangrove_propagule',
    'minecraft:oak_log', 'minecraft:spruce_log', 'minecraft:birch_log',
    'minecraft:jungle_log', 'minecraft:acacia_log', 'minecraft:dark_oak_log',
    'minecraft:cherry_log', 'minecraft:mangrove_log',
    'minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:birch_planks',
    'minecraft:jungle_planks', 'minecraft:acacia_planks', 'minecraft:dark_oak_planks',
    'minecraft:cherry_planks', 'minecraft:mangrove_planks',
    'minecraft:wheat_seeds', 'minecraft:beetroot_seeds', 'minecraft:pumpkin_seeds',
    'minecraft:melon_seeds', 'minecraft:sugar_cane', 'minecraft:cactus', 'minecraft:bamboo',
    'minecraft:bone_meal',
    'minecraft:poppy', 'minecraft:dandelion', 'minecraft:oxeye_daisy',
    'minecraft:cornflower', 'minecraft:lily_of_the_valley', 'minecraft:wither_rose'
)

Add-Category 'Construction' ':(.+_(block|bricks|planks|stone|glass|lantern|torch|rack|concrete|terracotta|glazed|wool|carpet|stained|stairs|slab|wall|door|fence|gate|trapdoor)|crafting_table|smithing_table|anvil|obsidian)$' 'ore|spawn|portal|altar|creative|command|raw_|deepslate|dragonsteel|iron_ore|gold_ore|copper_ore|log_|planks|sapling' 48

Add-MixedCategory 'Mécanismes et Redstone' ':(piston|observer|comparator|repeater|hopper|dropper|dispenser|redstone_|lever|button|pressure_plate|daylight_detector|target|note_block|slime_block|honey_block|crafter|lightning_rod|sculk_sensor|tripwire_hook)' '' 38 @(
    'minecraft:piston', 'minecraft:sticky_piston', 'minecraft:observer',
    'minecraft:comparator', 'minecraft:repeater', 'minecraft:hopper',
    'minecraft:dropper', 'minecraft:dispenser', 'minecraft:redstone_lamp',
    'minecraft:redstone_torch', 'minecraft:lever', 'minecraft:stone_button',
    'minecraft:stone_pressure_plate', 'minecraft:daylight_detector',
    'minecraft:target', 'minecraft:note_block', 'minecraft:slime_block',
    'minecraft:honey_block', 'minecraft:crafter', 'minecraft:lightning_rod',
    'minecraft:sculk_sensor', 'minecraft:calibrated_sculk_sensor',
    'minecraft:heavy_weighted_pressure_plate', 'minecraft:light_weighted_pressure_plate',
    'minecraft:tripwire_hook'
)

# ── Tier 2 (800-3000) ─────────────────────────────────────
Add-MixedCategory 'Lingots et gemmes' ':(.+_ingot|.+_gem|diamond|emerald|lapis_lazuli|redstone|quartz|amethyst_shard)$' 'heated_|block|gear|nugget|deepslate|raw_' 48 @(
    'minecraft:iron_ingot', 'minecraft:copper_ingot', 'minecraft:gold_ingot',
    'minecraft:diamond', 'minecraft:emerald', 'minecraft:netherite_ingot',
    'minecraft:quartz', 'minecraft:redstone', 'minecraft:lapis_lazuli',
    'minecraft:amethyst_shard'
)

Add-MixedCategory 'Forge et amélioration' ':(.+_(schematic|template|cast|head|pommel|socket)|blueprint.*|.+hammer|.+tongs|gem_dust|sigil_.+|smithing_template)$' 'legendary|universal|boss|spawn|dragonarmor' 42 @(
    'apotheosis:iron_upgrade_smithing_template', 'apotheosis:gold_upgrade_smithing_template',
    'apotheosis:diamond_upgrade_smithing_template'
)

Add-MixedCategory 'Utilitaires' ':(.+_(table|anvil|furnace|cauldron|bag|pouch|compass|lead|glove|tool_rack)|reforging|salvaging|augmenting|wayward_compass|spyglass)$' 'boss|spawn|creative|dragon' 35 @(
    'minecraft:compass', 'minecraft:recovery_compass', 'minecraft:spyglass',
    'minecraft:shears', 'minecraft:brush',
    'apotheosis:ender_lead', 'apotheosis:simple_reforging_table', 'apotheosis:reforging_table',
    'apotheosis:salvaging_table', 'apotheosis:augmenting_table', 'apotheosis:gem_cutting_table',
    'epicfight:glove'
)

Add-MixedCategory 'Mobilité et transport' ':(saddle|.*_horse_armor|name_tag|lead|.*_boat|minecart|rail|elytra)$' '' 42 @(
    'minecraft:saddle', 'minecraft:leather_horse_armor', 'minecraft:iron_horse_armor',
    'minecraft:golden_horse_armor', 'minecraft:diamond_horse_armor',
    'minecraft:name_tag', 'minecraft:lead', 'minecraft:elytra',
    'minecraft:oak_boat', 'minecraft:spruce_boat', 'minecraft:birch_boat',
    'minecraft:jungle_boat', 'minecraft:acacia_boat', 'minecraft:dark_oak_boat',
    'minecraft:cherry_boat', 'minecraft:mangrove_boat',
    'minecraft:oak_chest_boat', 'minecraft:spruce_chest_boat', 'minecraft:birch_chest_boat',
    'minecraft:jungle_chest_boat', 'minecraft:acacia_chest_boat', 'minecraft:dark_oak_chest_boat',
    'minecraft:cherry_chest_boat', 'minecraft:mangrove_chest_boat',
    'minecraft:minecart', 'minecraft:chest_minecart', 'minecraft:hopper_minecart',
    'minecraft:furnace_minecart', 'minecraft:tnt_minecart',
    'minecraft:rail', 'minecraft:powered_rail', 'minecraft:detector_rail', 'minecraft:activator_rail'
)

Add-MixedCategory 'Trophées et décoration' ':(item_frame|painting|armor_stand|flower_pot|bell|.*_skull|.*_head|end_rod|sea_lantern|shroomlight|bookshelf|lectern|chiseled_bookshelf|glow_item_frame|spore_blossom|glow_berries|amethyst_cluster)$' '' 40 @(
    'minecraft:item_frame', 'minecraft:glow_item_frame', 'minecraft:painting',
    'minecraft:armor_stand', 'minecraft:flower_pot', 'minecraft:bell',
    'minecraft:skeleton_skull', 'minecraft:wither_skeleton_skull', 'minecraft:zombie_head',
    'minecraft:creeper_head', 'minecraft:dragon_head', 'minecraft:piglin_head',
    'minecraft:end_rod', 'minecraft:sea_lantern', 'minecraft:shroomlight',
    'minecraft:bookshelf', 'minecraft:chiseled_bookshelf', 'minecraft:lectern',
    'minecraft:spore_blossom', 'minecraft:glow_berries', 'minecraft:amethyst_cluster'
)

# ── Tier 3 (3000-12000) ───────────────────────────────────
Add-Category 'Matériaux avancés' ':(.+_(alloy|crystal|shard|fragment|essence|material)|netherite_scrap|dragonbone|witherbone|dread_shard|permafrost_shard)$' 'common_|spawn|boss|coin|note' 42

Add-MixedCategory 'Runes et composants magiques' ':(.+_rune|.+_ink|.+_essence|rune_.+|mana_.+|arcane_.+|scroll_.+)$' 'armor|weapon|spawn|block' 48 @(
    'irons_spellbooks:arcane_rune', 'irons_spellbooks:blank_rune',
    'irons_spellbooks:fire_rune', 'irons_spellbooks:ice_rune',
    'irons_spellbooks:lightning_rune', 'irons_spellbooks:holy_rune',
    'irons_spellbooks:ender_rune', 'irons_spellbooks:blood_rune',
    'irons_spellbooks:evocation_rune', 'irons_spellbooks:nature_rune',
    'irons_spellbooks:protection_rune', 'irons_spellbooks:cooldown_rune'
)

Add-MixedCategory 'Magie et parchemins' ':(scroll|.+spell_book|.+_staff|affinity_ring.*|.+_charm|skillbook|potion_charm)$' 'spawn|ancient|legendary' 50 @(
    'irons_spellbooks:copper_spell_book', 'irons_spellbooks:iron_spell_book',
    'irons_spellbooks:gold_spell_book', 'irons_spellbooks:diamond_spell_book',
    'irons_spellbooks:netherite_spell_book', 'irons_spellbooks:blaze_spell_book',
    'irons_spellbooks:druidic_spell_book', 'irons_spellbooks:evoker_spell_book',
    'irons_spellbooks:dragonskin_spell_book', 'irons_spellbooks:blood_staff'
)

Add-MixedCategory 'Armures classiques' ':(copper_|iron_|diamond_|netherite_|silver_|leather_|chainmail_|golden_|steel_|tin_|bronze_)(helmet|chestplate|leggings|boots)$' 'trim|dragon|ant_|carapace|spawn|armor_' 52 @(
    'minecraft:leather_helmet', 'minecraft:leather_chestplate', 'minecraft:leather_leggings', 'minecraft:leather_boots',
    'minecraft:chainmail_helmet', 'minecraft:chainmail_chestplate', 'minecraft:chainmail_leggings', 'minecraft:chainmail_boots',
    'minecraft:iron_helmet', 'minecraft:iron_chestplate', 'minecraft:iron_leggings', 'minecraft:iron_boots',
    'minecraft:golden_helmet', 'minecraft:golden_chestplate', 'minecraft:golden_leggings', 'minecraft:golden_boots',
    'minecraft:diamond_helmet', 'minecraft:diamond_chestplate', 'minecraft:diamond_leggings', 'minecraft:diamond_boots',
    'minecraft:netherite_helmet', 'minecraft:netherite_chestplate', 'minecraft:netherite_leggings', 'minecraft:netherite_boots',
    'overgeared:copper_helmet', 'overgeared:copper_chestplate', 'overgeared:copper_leggings', 'overgeared:copper_boots',
    'overgeared:steel_helmet', 'overgeared:steel_chestplate', 'overgeared:steel_leggings', 'overgeared:steel_boots',
    'tensura:silver_helmet', 'tensura:silver_chestplate', 'tensura:silver_leggings', 'tensura:silver_boots'
)

Add-MixedCategory 'Armes à distance' ':(.+_(bow|crossbow|arrow)|bow|crossbow|arrow)$' 'pulling|charged|firework|spawn|compat/|spectral' 45 @(
    'minecraft:bow', 'minecraft:crossbow',
    'minecraft:arrow', 'minecraft:tipped_arrow', 'minecraft:spectral_arrow',
    'overgeared:iron_arrow', 'overgeared:steel_arrow', 'overgeared:diamond_arrow',
    'overgeared:lingering_arrow'
)

Add-Category "Lances et armes d'hast" ':(.+_(spear|halberd|glaive|lance|guisarme|ahlspiess|twinblade|warglaive)|spear)$' 'blocking|raised|throwing|awakened|legendary|compat/' 48

# ── Tier 4 (12000-30000) ──────────────────────────────────
Add-Category 'Armes légères' ':(.+_(dagger|rapier|katana|kodachi|tachi|sai|cutlass|sickle)|uchigatana|bokken)$' 'blocking|sheath|awakened|legendary|relic|compat/' 50

Add-Category 'Armes lourdes' ':(.+_(greatsword|great_sword|claymore|greataxe|greathammer|longsword|long_sword|scythe|odachi)|greatsword|hulk_hammer)$' 'blocking|awakened|legendary|relic|compat/' 52

Add-Category 'Armes de Tensura' ':adamantite_.+|:hihiirokane_.+|:pure_magisteel_.+|:high_magisteel_.+|:low_magisteel_.+|:silver_.+|:mithril_.+' '(helmet|chestplate|leggings|boots|block|gear_schematic|tool|hoe|axe|pickaxe|shovel|bone_golem|bow|nugget|pile|gear)' 48

Add-Category 'Armes uniques et légendaires' ':runic_.+|:soulkeeper|:twisted_blade|:soulrender|:soulstealer|:stormbringer|:hearthflame|:soulpyre|:mirrorguard|:wildvine|:bloomsoul|:dragonsteel_.+_sword|:dread_sword|:tide_trident|:ghost_sword|:hippogryph_sword|:spatial_blade|:dead_end_rainbow|:vorpal_sword|:ice_blade|:mad_swords' 'grip|nugget|hilt|schematic' 45

Add-Category 'Armures fantastiques' ':(armor_.+_(helmet|chestplate|leggings|boots))$' 'trim|copper_metal|spawn|cosmetic|dragonsteel' 48

Add-MixedCategory 'Armures historiques' ':(crusader|gothic|maximilian|knight|plate|chain|scale)_(helmet|chestplate|leggings|boots)$' '' 44 @(
    'magistuarmory:crusader_helmet', 'magistuarmory:crusader_chestplate',
    'magistuarmory:crusader_leggings', 'magistuarmory:crusader_boots',
    'magistuarmory:gothic_helmet', 'magistuarmory:gothic_chestplate',
    'magistuarmory:gothic_leggings', 'magistuarmory:gothic_boots',
    'magistuarmory:maximilian_helmet', 'magistuarmory:maximilian_chestplate',
    'magistuarmory:maximilian_leggings', 'magistuarmory:maximilian_boots',
    'magistuarmory:knight_helmet', 'magistuarmory:knight_chestplate',
    'magistuarmory:knight_leggings', 'magistuarmory:knight_boots',
    'magistuarmory:plate_helmet', 'magistuarmory:plate_chestplate',
    'magistuarmory:plate_leggings', 'magistuarmory:plate_boots',
    'magistuarmory:chain_helmet', 'magistuarmory:chain_chestplate',
    'magistuarmory:chain_leggings', 'magistuarmory:chain_boots',
    'magistuarmory:scale_helmet', 'magistuarmory:scale_chestplate',
    'magistuarmory:scale_leggings', 'magistuarmory:scale_boots'
)

Add-MixedCategory 'Armures magiques' ':(priest|pyromancer|cryomancer|electromancer|netherite_mage)_(helmet|chestplate|leggings|boots)|:.*magisteel_(helmet|chestplate|leggings|boots)|:hihiirokane_(helmet|chestplate|leggings|boots)|:adamantite_(helmet|chestplate|leggings|boots)$' '' 55 @(
    'irons_spellbooks:priest_helmet', 'irons_spellbooks:priest_chestplate',
    'irons_spellbooks:priest_leggings', 'irons_spellbooks:priest_boots',
    'irons_spellbooks:pyromancer_helmet', 'irons_spellbooks:pyromancer_chestplate',
    'irons_spellbooks:pyromancer_leggings', 'irons_spellbooks:pyromancer_boots',
    'irons_spellbooks:cryomancer_helmet', 'irons_spellbooks:cryomancer_chestplate',
    'irons_spellbooks:cryomancer_leggings', 'irons_spellbooks:cryomancer_boots',
    'irons_spellbooks:electromancer_helmet', 'irons_spellbooks:electromancer_chestplate',
    'irons_spellbooks:electromancer_leggings', 'irons_spellbooks:electromancer_boots',
    'irons_spellbooks:netherite_mage_helmet', 'irons_spellbooks:netherite_mage_chestplate',
    'irons_spellbooks:netherite_mage_leggings', 'irons_spellbooks:netherite_mage_boots'
)

Add-Category 'Composants de monstres' ':(.+_(blood|scale|scales|bone|skull|feather|fang|claw|horn|hide|carapace|rib)|dragonbone|witherbone|creeper_shard|endersoul_hand|chemical_x|frozen_bone|icy_fang|scroll_blood)$' 'armor|block|spawn|banner|pattern' 42

Add-Category 'Objets rares contrôlés' ':(gem|gems/.+|mythic_material|ancient_knowledge_fragment|rare_material|epic_material)$' 'boss|spawn|summoner|common|uncommon' 45

# Fill any remaining unused candidates up to the 1000 target (prioritize worthwhile items)
$fillTarget = [Math]::Max(0, 998 - $selected.Count)
if ($fillTarget -gt 0 -and $fillTarget -le 50) {
    $fillPool = $candidates | Where-Object { -not $used.Contains($_.item_id) } | Sort-Object item_id
    $fillAdded = 0
    foreach ($candidate in $fillPool) {
        if ($fillAdded -ge $fillTarget) { break }
        $id = $candidate.item_id
        # Skip block, spawn_egg, boss_summoner, creative, command_block, structure, debug items
        if ($id -match 'block$|spawn_egg|boss_|summoner|creative|command_block|structure|debug|spawner') { continue }
        $price = Get-PriceForItem -Tab 'Utilitaires' -ItemId $id -BasePrice 55
        $selected.Add([pscustomobject]@{
            Tab='Utilitaires'; Id=$id; Price=$price; Count=1; Potion=$null
        })
        [void]$used.Add($id)
        $fillAdded++
    }
    Write-Output "Extra fill = $fillAdded"
}

# Special potion entries
$selected.Add([pscustomobject]@{
    Tab='Potions et soins'; Id='minecraft:potion'; Price=1500; Count=1; Potion='strong_healing'
})
$selected.Add([pscustomobject]@{
    Tab='Potions et soins'; Id='minecraft:potion'; Price=2000; Count=1; Potion='strong_strength'
})

if ($selected.Count -ne 1000) {
    Write-Output "NOTE: Generated catalog has $($selected.Count) entries (target 1000)"
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('package tong.statmod.integration.sdm;')
$lines.Add('')
$lines.Add('import java.util.List;')
$lines.Add('')
$lines.Add('/** Generated by tools/generate_sdm_shop_entries.ps1 from the audited local mod JARs. */')
$lines.Add('final class SDMShopCatalogEntries {')
$lines.Add('    private SDMShopCatalogEntries() {}')
$lines.Add('')
$lines.Add('    static List<SDMShopCatalog.ShopItem> items() {')
$lines.Add('        return List.of(')
for ($i = 0; $i -lt $selected.Count; $i++) {
    $entry = $selected[$i]
    $comma = if ($i -lt $selected.Count - 1) { ',' } else { '' }
    $potion = if ($null -eq $entry.Potion) { 'null' } else { '"' + $entry.Potion + '"' }
    $lines.Add(('            item("{0}", "{1}", {2}, {3}, {4}){5}' -f
        $entry.Tab.Replace('"', '\"'), $entry.Id, $entry.Price, $entry.Count, $potion, $comma))
}
$lines.Add('        );')
$lines.Add('    }')
$lines.Add('')
$lines.Add('    private static SDMShopCatalog.ShopItem entry(String tab, String item, int price, int count) {')
$lines.Add('        return new SDMShopCatalog.ShopItem(tab, item, price, count, null);')
$lines.Add('    }')
$lines.Add('')
$lines.Add('    private static SDMShopCatalog.ShopItem item(String tab, String item, int price, int count, String potionId) {')
$lines.Add('        return new SDMShopCatalog.ShopItem(tab, item, price, count, potionId);')
$lines.Add('    }')
$lines.Add('}')

[System.IO.File]::WriteAllLines(
    $outputFile,
    [string[]]$lines,
    [System.Text.UTF8Encoding]::new($false)
)
Write-Output "Generated $($selected.Count) controlled shop entries in $outputFile"

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

function Add-Category {
    param(
        [string]$Tab,
        [string]$Include,
        [string]$Exclude,
        [int]$Limit,
        [int]$BasePrice,
        [int]$Count
    )
    $before = $selected.Count
    $matches = $candidates | Where-Object {
        $_.item_id -match $Include -and
        ([string]::IsNullOrEmpty($Exclude) -or $_.item_id -notmatch $Exclude) -and
        -not $used.Contains($_.item_id)
    } | Select-Object -First $Limit

    foreach ($candidate in $matches) {
        $id = $candidate.item_id
        $price = $BasePrice
        if ($id -match 'netherite|adamantite|mythic|legendary|dragonsteel') {
            $price = [Math]::Min(2500, [Math]::Max(400, $BasePrice * 4))
        } elseif ($id -match 'diamond|epic|rare|pure_magisteel|dragon') {
            $price = [Math]::Min(900, [Math]::Max(120, $BasePrice * 2))
        }
        $selected.Add([pscustomobject]@{ Tab=$Tab; Id=$id; Price=$price; Count=$Count; Potion=$null })
        [void]$used.Add($id)
    }
    Write-Output "$Tab=$($selected.Count - $before)"
}

Add-Category 'Minerais bruts' ':(raw_.+|.+_ore|ore_.+|coal)$' 'block|stairs|slab|wall|door' 20 15 4
Add-Category 'Lingots et gemmes' ':(.+_ingot|.+_gem|diamond|emerald|lapis_lazuli|redstone|quartz|amethyst_shard)$' 'heated_|block|gear|nugget' 28 40 2
Add-Category 'Matériaux avancés' ':(.+_(alloy|crystal|shard|fragment|essence|material)|netherite_scrap|dragonbone|witherbone)$' 'common_|spawn|boss|coin|note' 22 150 1
Add-Category 'Forge et amélioration' ':(.+_(schematic|template|cast|head|pommel|socket)|blueprint.*|.+hammer|.+tongs|gem_dust|sigil_.+)$' 'legendary|universal|boss|spawn' 22 100 1
Add-Category 'Armes légères' ':(.+_(dagger|rapier|katana|kodachi|tachi|sai|cutlass|sickle)|uchigatana|bokken)$' 'blocking|sheath|awakened|legendary|relic|compat/' 32 180 1
Add-Category 'Armes lourdes' ':(.+_(greatsword|great_sword|claymore|greataxe|greathammer|longsword|long_sword|scythe|odachi)|greatsword|hulk_hammer)$' 'blocking|awakened|legendary|relic|compat/' 34 260 1
Add-Category "Lances et armes d'hast" ':(.+_(spear|halberd|glaive|lance|guisarme|ahlspiess|twinblade|warglaive)|spear)$' 'blocking|raised|throwing|awakened|legendary|compat/' 30 220 1
Add-Category 'Armes à distance' ':(.+_(bow|crossbow|arrow)|bow|crossbow|arrow)$' 'pulling|charged|firework|spawn|compat/' 20 100 1
Add-Category 'Armures classiques' ':(copper|iron|diamond|netherite|silver)_(helmet|chestplate|leggings|boots)$' 'trim|dragon|ant_|carapace' 28 180 1
Add-Category 'Armures fantastiques' ':(armor_.+_(helmet|chestplate|leggings|boots)|.+_(helmet|chestplate|leggings|boots))$' 'trim|copper_|iron_|diamond_|netherite_|silver_|spawn|cosmetic' 36 300 1
Add-Category 'Magie et parchemins' ':(scroll|.+_scroll|.+spell_book|.+_staff|affinity_ring.*|.+_charm|skillbook)$' 'spawn|ancient|legendary' 24 180 1
Add-Category 'Runes et composants magiques' ':(.+_rune|.+_ink|.+_essence|rune_.+|mana_.+|arcane_.+)$' 'armor|weapon|spawn|block' 24 90 1
Add-Category 'Potions et soins' ':(potion|splash_potion|lingering_potion|.+golden_apple|golden_apple|golden_carrot|ambrosia|.+_elixir|.+_tea)$' 'chemical_x|spawn' 16 60 4
Add-Category 'Composants de monstres' ':(.+_(blood|scale|scales|bone|skull|feather|fang|claw|horn|hide|carapace|rib)|dragonbone|witherbone|creeper_shard|endersoul_hand|chemical_x)$' 'armor|block|spawn|banner|pattern' 26 120 2
Add-Category 'Nourriture' ':(.+_(bread|stew|soup|pie|cake|meat|beef|pork|chicken|fish|apple|berry|berries)|ambrosia)$' 'block|spawn|raw_' 16 12 8
Add-Category 'Construction' ':(.+_(block|bricks|planks|stone|glass|lantern|torch|rack)|crafting_table|smithing_table|anvil)$' 'ore|spawn|portal|altar|creative|command' 24 20 16
Add-Category 'Utilitaires' ':(.+_(table|anvil|furnace|cauldron|bag|pouch|compass|lead)|shield|glove|tool_rack)$' 'boss|spawn|creative' 20 80 1
Add-Category 'Objets rares contrôlés' ':(gem|gems/.+|epic_material|mythic_material|sigil_.+|ancient_knowledge_fragment)$' 'boss|spawn|summoner' 18 500 1

$selected.Add([pscustomobject]@{
    Tab='Potions et soins'; Id='minecraft:potion'; Price=75; Count=1; Potion='strong_healing'
})
$selected.Add([pscustomobject]@{
    Tab='Potions et soins'; Id='minecraft:potion'; Price=90; Count=1; Potion='strong_strength'
})

if ($selected.Count -lt 350 -or $selected.Count -gt 450) {
    throw "Generated catalog contains $($selected.Count) entries; expected 350-450"
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
    $lines.Add(('            new SDMShopCatalog.ShopItem("{0}", "{1}", {2}, {3}, {4}){5}' -f
        $entry.Tab.Replace('"', '\"'), $entry.Id, $entry.Price, $entry.Count, $potion, $comma))
}
$lines.Add('        );')
$lines.Add('    }')
$lines.Add('}')

[System.IO.File]::WriteAllLines(
    $outputFile,
    [string[]]$lines,
    [System.Text.UTF8Encoding]::new($false)
)
Write-Output "Generated $($selected.Count) controlled shop entries in $outputFile"

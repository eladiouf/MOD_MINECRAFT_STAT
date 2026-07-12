$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot 'docs/generated'
$outputFile = Join-Path $outputDirectory 'sdm-shop-item-candidates.tsv'
$forbidden = 'spawn_egg|boss_summoner|creative|command_block|structure_block|debug_stick'
$tab = [char]9
$rows = [System.Collections.Generic.List[string]]::new()

foreach ($jar in Get-ChildItem -LiteralPath (Join-Path $projectRoot 'libs') -Filter '*.jar') {
    foreach ($entry in (& jar tf $jar.FullName)) {
        if ($entry -match '^assets/([^/]+)/models/item/(.+)\.json$') {
            $namespace = $Matches[1]
            $path = $Matches[2]
            $id = "${namespace}:${path}"
            if ($id -notmatch $forbidden) {
                $rows.Add($namespace + $tab + $id + $tab + $jar.Name)
            }
        }
    }
}

$localModels = Join-Path $projectRoot 'src/main/resources/assets/statmod/models/item'
if (Test-Path -LiteralPath $localModels) {
    foreach ($model in Get-ChildItem -LiteralPath $localModels -Filter '*.json' -Recurse) {
        $relative = $model.FullName.Substring($localModels.Length + 1).Replace('\', '/')
        $id = 'statmod:' + $relative.Substring(0, $relative.Length - 5)
        if ($id -notmatch $forbidden) {
            $rows.Add('statmod' + $tab + $id + $tab + 'project')
        }
    }
}

$vanillaIds = @(
    'raw_iron', 'raw_gold', 'raw_copper', 'coal', 'iron_ingot', 'gold_ingot',
    'copper_ingot', 'diamond', 'emerald', 'lapis_lazuli', 'redstone',
    'amethyst_shard', 'quartz', 'netherite_scrap', 'netherite_ingot'
)
foreach ($path in $vanillaIds) {
    $rows.Add('minecraft' + $tab + 'minecraft:' + $path + $tab + 'vanilla')
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$content = [System.Collections.Generic.List[string]]::new()
$content.Add('mod_id' + $tab + 'item_id' + $tab + 'source')
$content.AddRange([string[]]($rows | Sort-Object -Unique))
$content | Set-Content -LiteralPath $outputFile -Encoding utf8

Write-Output "Wrote $($rows.Count) candidate rows to $outputFile"

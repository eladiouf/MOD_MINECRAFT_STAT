param(
    [ValidateSet('Before', 'After')]
    [string]$Mode = 'Before'
)

$ErrorActionPreference = 'Stop'

$root = [IO.Path]::GetFullPath((git rev-parse --show-toplevel))
$branch = (git branch --show-current).Trim()
if ($branch -ne 'forge-1.20.1') {
    throw "Wrong branch: $branch"
}
if ((Split-Path $root -Leaf) -ne 'forge-1.20.1' -or $root -notmatch '[\\/]\.worktrees[\\/]forge-1\.20\.1$') {
    throw "Unsafe worktree: $root"
}

$external = Join-Path $root 'external-mods/irons-spells-forge-1.20.1'
$manifest = @(Import-Csv (Join-Path $external 'manifest.csv'))
$jars = @(Get-ChildItem `
    (Join-Path $external 'active'), `
    (Join-Path $external 'needs-testing'), `
    (Join-Path $external 'dependencies') `
    -File -Filter '*.jar')

if ($manifest.Count -ne 74 -or $jars.Count -ne 74) {
    throw 'Iron addon baseline must contain 74 entries and 74 JARs.'
}

foreach ($row in $manifest) {
    $jar = @($jars | Where-Object Name -CEQ $row.filename)
    if ($jar.Count -ne 1) {
        throw "Missing or duplicated JAR: $($row.filename)"
    }
    if ((Get-FileHash $jar[0].FullName -Algorithm SHA256).Hash -ne $row.sha256) {
        throw "SHA mismatch: $($row.filename)"
    }
}

if ($Mode -eq 'After') {
    $forbidden = & rg -n -i 'net\.neoforged|neoforge|tensura|tensura_iron_spells' `
        src/main build.gradle gradle.properties settings.gradle
    if ($LASTEXITCODE -eq 0 -and $forbidden) {
        throw "Forbidden active references:`n$forbidden"
    }
    if ((Get-Content -Raw gradle.properties) -notmatch 'minecraft_version=1\.20\.1') {
        throw 'Wrong Minecraft version.'
    }
    if ((Get-Content -Raw gradle.properties) -notmatch 'forge_version=47\.4\.10') {
        throw 'Wrong Forge version.'
    }

    $trackedJars = @(git ls-files 'external-mods/irons-spells-forge-1.20.1/*.jar')
    if ($trackedJars.Count -ne 0) {
        throw "External mod JARs must remain untracked:`n$($trackedJars -join "`n")"
    }

    $builtJars = @(Get-ChildItem (Join-Path $root 'build/libs') -File -Filter 'statmod-*.jar')
    if ($builtJars.Count -ne 1) {
        throw "Expected exactly one built STAT Mod JAR, found $($builtJars.Count)."
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($builtJars[0].FullName)
    try {
        $entries = @($archive.Entries | ForEach-Object FullName)
        $requiredEntries = @(
            'META-INF/mods.toml'
            'tong/statmod/StatMod.class'
            'tong/statmod/StatModRuntime.class'
            'tong/statmod/stats/PlayerStats.class'
            'tong/statmod/capability/PlayerStatsProvider.class'
            'tong/statmod/network/StatsSnapshotMessage.class'
            'tong/statmod/command/StatsCommands.class'
            'tong/statmod/progression/xp/XpRewardPolicy.class'
            'tong/statmod/progression/xp/PlayerXpState.class'
            'tong/statmod/progression/xp/XpAwardService.class'
            'tong/statmod/event/CombatXpEvents.class'
            'tong/statmod/event/ExplorationXpEvents.class'
            'tong/statmod/event/CraftingXpEvents.class'
            'data/statmod/tags/items/heavy_weapons.json'
            'data/statmod/tags/items/blade_weapons.json'
            'data/statmod/tags/items/precision_weapons.json'
            'data/statmod/tags/items/forgeable_equipment.json'
        )
        foreach ($requiredEntry in $requiredEntries) {
            if ($entries -notcontains $requiredEntry) {
                throw "Built JAR is missing $requiredEntry"
            }
        }

        $duplicateEntries = @($archive.Entries |
            Group-Object -Property FullName |
            Where-Object Count -GT 1)
        if ($duplicateEntries.Count -ne 0) {
            throw "Built JAR contains duplicate entries:`n$($duplicateEntries.Name -join "`n")"
        }

        $forbiddenPrefixes = @(
            'io/redspace/ironsspellbooks/'
            'yesman/epicfight/'
            'net/neoforged/'
        )
        foreach ($entry in $entries) {
            foreach ($prefix in $forbiddenPrefixes) {
                if ($entry.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
                    throw "Built JAR embeds forbidden external class path: $entry"
                }
            }
        }
    }
    finally {
        $archive.Dispose()
    }

    & git diff --check
    if ($LASTEXITCODE -ne 0) {
        throw 'git diff --check failed.'
    }
}

"OK mode=$Mode branch=$branch jars=$($jars.Count) manifest=$($manifest.Count)"

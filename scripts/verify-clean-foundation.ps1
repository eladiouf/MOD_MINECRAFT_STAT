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
        src build.gradle gradle.properties settings.gradle
    if ($LASTEXITCODE -eq 0 -and $forbidden) {
        throw "Forbidden active references:`n$forbidden"
    }
    if ((Get-Content -Raw gradle.properties) -notmatch 'minecraft_version=1\.20\.1') {
        throw 'Wrong Minecraft version.'
    }
    if ((Get-Content -Raw gradle.properties) -notmatch 'forge_version=47\.4\.10') {
        throw 'Wrong Forge version.'
    }
}

"OK mode=$Mode branch=$branch jars=$($jars.Count) manifest=$($manifest.Count)"

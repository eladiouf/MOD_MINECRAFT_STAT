Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
Import-Module (Join-Path $repoRoot 'scripts\irons-addons\MedievalContentBatch.psm1') -Force

function Assert-Equal {
    param($Expected, $Actual, [string]$Message)
    if ($Expected -ne $Actual) {
        throw "$Message Expected='$Expected' Actual='$Actual'"
    }
}

function Assert-Throws {
    param([scriptblock]$Action, [string]$Pattern)
    try {
        & $Action
        throw 'Expected command to throw.'
    } catch {
        if ($_.Exception.Message -notmatch $Pattern) { throw }
    }
}

$catalogPath = Join-Path $repoRoot 'scripts\irons-addons\medieval-content-batch.json'
$catalog = Read-MedievalContentCatalog -Path $catalogPath
Assert-Equal 9 @($catalog.artifacts).Count 'The batch must contain nine artifacts.'
Assert-Equal 7 @($catalog.artifacts | Where-Object source_kind -eq 'prepared').Count 'Prepared addon count mismatch.'
Assert-Equal 2 @($catalog.artifacts | Where-Object source_kind -eq 'remote').Count 'Dependency count mismatch.'
Assert-Equal 9 @($catalog.artifacts.file_name | Sort-Object -Unique).Count 'Filenames must be unique.'
Assert-Equal 9 @($catalog.artifacts.primary_mod_id | Sort-Object -Unique).Count 'Primary mod IDs must be unique.'
Assert-Equal 'M0uqO7Oe' ($catalog.artifacts | Where-Object primary_mod_id -eq 'azurelib').distribution_version_id 'AzureLib version ID mismatch.'
Assert-Equal '5906086' ($catalog.artifacts | Where-Object primary_mod_id -eq 'deeperdarker').distribution_file_id 'Deeper and Darker file ID mismatch.'
Assert-Equal '3.15.0' $catalog.required_active_minimum_versions.irons_spellbooks 'Iron''s Spells floor mismatch.'
Assert-Equal '3.16' $catalog.required_active_minimum_versions.cataclysm 'Cataclysm floor mismatch.'

Write-Output 'catalog_tests=PASS'

function New-TestForgeJar {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$ModId,
        [Parameter(Mandatory)][string]$Version,
        [string]$ImplementationVersion = $Version,
        [string[]]$Dependencies = @(),
        [string]$ModLoader = 'javafml',
        [string]$LoaderRange = '[47,)',
        [string]$MinecraftRange = '[1.20.1,1.21)',
        [bool]$IncludeMinecraftDependency = $true
    )
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $temp = Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path (Join-Path $temp 'META-INF') -Force | Out-Null
    try {
        $dependencyToml = @($Dependencies | ForEach-Object {
            "[[dependencies.$ModId]]`nmodId=`"$_`"`nmandatory=true`nversionRange=`"[0,)`"`nordering=`"NONE`"`nside=`"BOTH`""
        }) -join "`n"
        $minecraftToml = if ($IncludeMinecraftDependency) { "[[dependencies.$ModId]]`nmodId=`"minecraft`"`nmandatory=true`nversionRange=`"$MinecraftRange`"`nordering=`"NONE`"`nside=`"BOTH`"" } else { '' }
        $toml = "modLoader=`"$ModLoader`"`nloaderVersion=`"$LoaderRange`"`nlicense=`"All Rights Reserved`"`n[[mods]]`nmodId=`"$ModId`"`nversion=`"$Version`"`ndisplayName=`"$ModId`"`n$minecraftToml`n$dependencyToml"
        Set-Content -LiteralPath (Join-Path $temp 'META-INF\mods.toml') -Value $toml -Encoding UTF8
        Set-Content -LiteralPath (Join-Path $temp 'META-INF\MANIFEST.MF') -Value "Manifest-Version: 1.0`nImplementation-Version: $ImplementationVersion`n" -Encoding ASCII
        $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew)
        try {
            $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $true)
            try {
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $temp 'META-INF\mods.toml'), 'META-INF/mods.toml') | Out-Null
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $temp 'META-INF\MANIFEST.MF'), 'META-INF/MANIFEST.MF') | Out-Null
            } finally {
                $archive.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        Remove-Item -LiteralPath $temp -Recurse -Force
    }
}

$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('irons-batch-metadata-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
try {
    $azurePath = Join-Path $fixtureRoot 'azurelib.jar'
    New-TestForgeJar -Path $azurePath -ModId 'azurelib' -Version '3.0.9'
    $azureArtifact = [pscustomobject]@{file_name='azurelib.jar';primary_mod_id='azurelib';expected_version='3.0.9'}
    $valid = Test-BatchArtifact -Artifact $azureArtifact -Path $azurePath
    Assert-Equal 'azurelib' $valid.PrimaryModId 'Primary mod ID mismatch.'
    Assert-Equal '3.0.9' $valid.Version 'Version mismatch.'

    $tokenPath = Join-Path $fixtureRoot 'token.jar'
    New-TestForgeJar -Path $tokenPath -ModId 'tokenmod' -Version '${file.jarVersion}' -ImplementationVersion '1.2.9-1.20.1'
    $token = Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='token.jar';primary_mod_id='tokenmod';expected_version='1.2.9-1.20.1'}) -Path $tokenPath
    Assert-Equal '1.2.9-1.20.1' $token.Version 'Manifest version token was not resolved.'

    $implicitMinecraftPath = Join-Path $fixtureRoot 'implicit-minecraft.jar'
    New-TestForgeJar -Path $implicitMinecraftPath -ModId 'implicit_mc' -Version '1.0.0-1.20.1' -IncludeMinecraftDependency $false
    $implicitArtifact = [pscustomobject]@{file_name='implicit-minecraft.jar';primary_mod_id='implicit_mc';expected_version='1.0.0-1.20.1';allow_missing_minecraft_dependency=$true}
    $implicit = Test-BatchArtifact -Artifact $implicitArtifact -Path $implicitMinecraftPath
    Assert-Equal 'implicit_mc' $implicit.PrimaryModId 'Explicit Minecraft metadata exception failed.'
    Assert-Throws { Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='implicit-minecraft.jar';primary_mod_id='implicit_mc';expected_version='1.0.0-1.20.1'}) -Path $implicitMinecraftPath } 'Minecraft dependency range is missing'

    Assert-Throws { Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='azurelib.jar';primary_mod_id='fabric_only';expected_version='3.0.9'}) -Path $azurePath } 'expected mod ID'
    foreach ($invalid in @(
        @{Name='lowcode.jar';ModLoader='lowcodefml';LoaderRange='[47,)';MinecraftRange='[1.20.1,1.21)';Pattern='modLoader'},
        @{Name='forge48.jar';ModLoader='javafml';LoaderRange='[48,)';MinecraftRange='[1.20.1,1.21)';Pattern='Forge 47.4.10'},
        @{Name='mc121.jar';ModLoader='javafml';LoaderRange='[47,)';MinecraftRange='[1.21,1.22)';Pattern='Minecraft 1.20.1'}
    )) {
        $invalidPath = Join-Path $fixtureRoot $invalid.Name
        New-TestForgeJar -Path $invalidPath -ModId 'invalid' -Version '1.0.0' -ModLoader $invalid.ModLoader -LoaderRange $invalid.LoaderRange -MinecraftRange $invalid.MinecraftRange
        Assert-Throws { Test-BatchArtifact -Artifact ([pscustomobject]@{file_name=$invalid.Name;primary_mod_id='invalid';expected_version='1.0.0'}) -Path $invalidPath } $invalid.Pattern
    }

    $ownerPath = Join-Path $fixtureRoot 'owner.jar'
    New-TestForgeJar -Path $ownerPath -ModId 'owner' -Version '1.0.0' -Dependencies @('azurelib')
    $owner = Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='owner.jar';primary_mod_id='owner';expected_version='1.0.0'}) -Path $ownerPath
    $closure = Test-ProposedDependencyClosure -Records @($owner,$valid) -ProtectedModIds @() -IntroducedPrimaryModIds @('owner','azurelib') -MinimumVersions ([pscustomobject]@{})
    Assert-Equal 0 @($closure.MissingDependencies).Count 'Closure should be complete.'
    Assert-Equal 0 @($closure.Conflicts).Count 'Closure should not have conflicts.'

    $broken = Test-ProposedDependencyClosure -Records @($owner) -ProtectedModIds @() -IntroducedPrimaryModIds @('owner') -MinimumVersions ([pscustomobject]@{})
    Assert-Equal 'azurelib' @($broken.MissingDependencies)[0].DependencyId 'Missing dependency was not reported.'

    $oldIronPath = Join-Path $fixtureRoot 'iron-old.jar'
    New-TestForgeJar -Path $oldIronPath -ModId 'irons_spellbooks' -Version '1.20.1-3.14.9'
    $oldIron = Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='iron-old.jar';primary_mod_id='irons_spellbooks';expected_version='1.20.1-3.14.9'}) -Path $oldIronPath
    $minimum = Test-ProposedDependencyClosure -Records @($oldIron) -ProtectedModIds @('irons_spellbooks') -IntroducedPrimaryModIds @() -MinimumVersions ([pscustomobject]@{irons_spellbooks='3.15.0'})
    Assert-Equal 'irons_spellbooks' @($minimum.BelowMinimumVersions)[0].ModId 'Old dependency version was not reported.'
} finally {
    Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
}

Write-Output 'metadata_and_closure_tests=PASS'

function New-TestClient {
    param([Parameter(Mandatory)][string]$Root)
    New-Item -ItemType Directory -Path (Join-Path $Root 'mods') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $Root 'disabled-mods') -Force | Out-Null
    foreach ($base in @(
        @{Id='statmod';Version='1.0.0'},
        @{Id='epicfight';Version='20.10.0'},
        @{Id='irons_spellbooks';Version='1.20.1-3.16.2'},
        @{Id='project_babylon_weapons';Version='1.0.0'},
        @{Id='cataclysm';Version='3.31'}
    )) {
        New-TestForgeJar -Path (Join-Path $Root "mods\$($base.Id).jar") -ModId $base.Id -Version $base.Version
    }
}

$transactionRoot = Join-Path ([IO.Path]::GetTempPath()) ('irons-batch-transaction-' + [guid]::NewGuid().ToString('N'))
$prepared = Join-Path $transactionRoot 'prepared'
$cache = Join-Path $transactionRoot 'cache'
$client = Join-Path $transactionRoot 'client'
$rollbackClient = Join-Path $transactionRoot 'rollback-client'
New-Item -ItemType Directory -Path $prepared,$cache | Out-Null
New-TestClient -Root $client
New-TestClient -Root $rollbackClient
try {
    foreach ($artifact in @($catalog.artifacts)) {
        $targetRoot = if ($artifact.source_kind -eq 'prepared') { $prepared } else { $cache }
        New-TestForgeJar -Path (Join-Path $targetRoot $artifact.file_name) -ModId $artifact.primary_mod_id -Version $artifact.expected_version
    }

    $dryRun = Invoke-MedievalContentBatch -ClientRoot $client -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $catalogPath
    Assert-Equal 'planned' $dryRun.Status 'Dry run status mismatch.'
    Assert-Equal 5 @(Get-ChildItem (Join-Path $client 'mods') -Filter '*.jar').Count 'Dry run mutated mods.'
    Assert-Equal 9 @($dryRun.Files).Count 'Dry run did not plan nine files.'

    $applied = Invoke-MedievalContentBatch -ClientRoot $client -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $catalogPath -Apply
    Assert-Equal 'complete' $applied.Status 'Apply status mismatch.'
    Assert-Equal 14 @(Get-ChildItem (Join-Path $client 'mods') -Filter '*.jar').Count 'Apply did not add nine JARs.'
    $completeManifest = Get-Content -LiteralPath $applied.ManifestPath -Raw | ConvertFrom-Json
    Assert-Equal 'complete' $completeManifest.status 'Complete manifest status mismatch.'
    foreach ($file in @($completeManifest.files)) {
        $destinationHash = (Get-FileHash -LiteralPath (Join-Path $client "mods\$($file.file_name)") -Algorithm SHA256).Hash
        Assert-Equal $file.sha256 $destinationHash 'Installed hash mismatch.'
    }

    $corruptFifthCopy = { param($index,$destination) if ($index -eq 5) { [IO.File]::WriteAllText($destination,'corrupt') } }
    $rollback = Invoke-MedievalContentBatch -ClientRoot $rollbackClient -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $catalogPath -Apply -AfterCopyHook $corruptFifthCopy
    Assert-Equal 'rolled_back' $rollback.Status 'Rollback status mismatch.'
    Assert-Equal 5 @(Get-ChildItem (Join-Path $rollbackClient 'mods') -Filter '*.jar').Count 'Rollback left introduced JARs.'
    $rollbackManifest = Get-Content -LiteralPath $rollback.ManifestPath -Raw | ConvertFrom-Json
    Assert-Equal 'rolled_back' $rollbackManifest.status 'Rollback manifest status mismatch.'
} finally {
    Remove-Item -LiteralPath $transactionRoot -Recurse -Force
}

Write-Output 'transaction_tests=PASS'

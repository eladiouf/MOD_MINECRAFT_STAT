Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot '..\modpack\MedievalModpackTools.psm1') -Force

function Read-MedievalContentCatalog {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)

    $fullPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "Catalog does not exist: $fullPath"
    }
    $catalog = Get-Content -LiteralPath $fullPath -Raw | ConvertFrom-Json
    if ([int]$catalog.schema_version -ne 1) { throw 'Catalog schema_version must equal 1.' }
    if ([string]$catalog.minecraft_version -ne '1.20.1') { throw 'Catalog minecraft_version must equal 1.20.1.' }
    if ([string]$catalog.required_active_minimum_versions.irons_spellbooks -ne '3.15.0') { throw 'Iron''s Spells minimum must equal 3.15.0.' }
    if ([string]$catalog.required_active_minimum_versions.cataclysm -ne '3.16') { throw 'Cataclysm minimum must equal 3.16.' }

    $artifacts = @($catalog.artifacts)
    if ($artifacts.Count -ne 8) { throw "Catalog must contain exactly eight stable artifacts; found $($artifacts.Count)." }
    if (@($artifacts | Where-Object source_kind -eq 'prepared').Count -ne 5) { throw 'Catalog must contain five prepared artifacts.' }
    if (@($artifacts | Where-Object source_kind -eq 'remote').Count -ne 3) { throw 'Catalog must contain three remote artifacts.' }
    foreach ($field in 'file_name', 'primary_mod_id', 'expected_version') {
        if (@($artifacts.$field | Sort-Object -Unique).Count -ne $artifacts.Count) {
            throw "Catalog field '$field' must contain unique values."
        }
    }
    foreach ($remote in @($artifacts | Where-Object source_kind -eq 'remote')) {
        if ([string]$remote.primary_mod_id -notin @('azurelib', 'deeperdarker', 'familiarslib')) {
            throw "Unapproved remote dependency: $($remote.primary_mod_id)"
        }
    }
    return $catalog
}

function Get-JarTextEntry {
    param([Parameter(Mandatory)][string]$Path, [Parameter(Mandatory)][string]$EntryName)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path))
    try {
        $entry = $archive.GetEntry($EntryName)
        if ($null -eq $entry) { return $null }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally {
        $archive.Dispose()
    }
}

function Get-JarImplementationVersion {
    param([Parameter(Mandatory)][string]$Path)

    $content = Get-JarTextEntry -Path $Path -EntryName 'META-INF/MANIFEST.MF'
    if ($null -eq $content) { throw "JAR uses a version token but has no manifest: $Path" }
    $matches = [regex]::Matches($content, '(?m)^Implementation-Version:\s*(.+)\s*$')
    if ($matches.Count -ne 1) { throw "JAR must contain one Implementation-Version: $Path" }
    return $matches[0].Groups[1].Value.Trim()
}

function Test-VersionInRange {
    param([Parameter(Mandatory)][string]$Value, [Parameter(Mandatory)][string]$Range)

    $valueVersion = [version]$Value
    if ($Range -match '^\[([^,\]]+)\]$') {
        $exact = if ($Matches[1] -match '^\d+$') { "$($Matches[1]).0" } else { $Matches[1] }
        return $valueVersion -eq [version]$exact
    }
    $match = [regex]::Match($Range, '^([\[\(])([^,]*),([^\]\)]*)([\]\)])$')
    if (-not $match.Success) { throw "Unsupported Forge version range: $Range" }
    $lower = $match.Groups[2].Value.Trim()
    $upper = $match.Groups[3].Value.Trim()
    if ($lower) {
        $normalizedLower = if ($lower -match '^\d+$') { "$lower.0" } else { $lower }
        $comparison = $valueVersion.CompareTo([version]$normalizedLower)
        if ($comparison -lt 0 -or ($comparison -eq 0 -and $match.Groups[1].Value -eq '(')) { return $false }
    }
    if ($upper) {
        $normalizedUpper = if ($upper -match '^\d+$') { "$upper.0" } else { $upper }
        $comparison = $valueVersion.CompareTo([version]$normalizedUpper)
        if ($comparison -gt 0 -or ($comparison -eq 0 -and $match.Groups[4].Value -eq ')')) { return $false }
    }
    return $true
}

function Get-ForgeCompatibility {
    param([Parameter(Mandatory)][string]$Path)

    $toml = Get-JarTextEntry -Path $Path -EntryName 'META-INF/mods.toml'
    if ($null -eq $toml) { throw "Forge metadata is missing: $Path" }
    $loader = [regex]::Match($toml, '(?m)^\s*modLoader\s*=\s*["'']([^"'']+)["'']').Groups[1].Value
    $loaderRange = [regex]::Match($toml, '(?m)^\s*loaderVersion\s*=\s*["'']([^"'']+)["'']').Groups[1].Value

    $minecraftRange = $null
    $dependencyId = $null
    $dependencyRange = $null
    foreach ($rawLine in ($toml -split "`r?`n")) {
        $line = $rawLine.Trim()
        if ($line -match '^\[\[dependencies\.[^\]]+\]\]') {
            if ($dependencyId -eq 'minecraft') { $minecraftRange = $dependencyRange; break }
            $dependencyId = $null
            $dependencyRange = $null
            continue
        }
        if ($line -match '^modId\s*=\s*["'']([^"'']+)["'']') { $dependencyId = $Matches[1].ToLowerInvariant(); continue }
        if ($line -match '^versionRange\s*=\s*["'']([^"'']+)["'']') { $dependencyRange = $Matches[1] }
    }
    if ($null -eq $minecraftRange -and $dependencyId -eq 'minecraft') { $minecraftRange = $dependencyRange }
    if ([string]::IsNullOrWhiteSpace($loaderRange)) { throw "Forge loaderVersion is missing: $Path" }
    return [pscustomobject]@{ModLoader=$loader;LoaderRange=$loaderRange;MinecraftRange=$minecraftRange}
}

function Test-BatchArtifact {
    [CmdletBinding()]
    param([Parameter(Mandatory)]$Artifact, [Parameter(Mandatory)][string]$Path)

    $record = Get-ModJarRecord -Path $Path
    if ($record.MetadataKind -notmatch '^forge_toml') { throw "Artifact '$Path' is not a Forge mods.toml JAR." }
    $compatibility = Get-ForgeCompatibility -Path $Path
    if ($compatibility.ModLoader -ne 'javafml') { throw "Artifact '$Path' modLoader must be javafml." }
    if (-not (Test-VersionInRange -Value '47.4.10' -Range $compatibility.LoaderRange)) { throw "Artifact '$Path' does not support Forge 47.4.10." }
    if ([string]::IsNullOrWhiteSpace($compatibility.MinecraftRange)) {
        $exceptionProperty = $Artifact.PSObject.Properties['allow_missing_minecraft_dependency']
        $allowMissingMinecraft = $null -ne $exceptionProperty -and [bool]$exceptionProperty.Value
        if (-not $allowMissingMinecraft -or [string]$Artifact.expected_version -notmatch '1\.20\.1') {
            throw "Minecraft dependency range is missing: $Path"
        }
    } elseif (-not (Test-VersionInRange -Value '1.20.1' -Range $compatibility.MinecraftRange)) {
        throw "Artifact '$Path' does not support Minecraft 1.20.1."
    }

    $expectedId = ([string]$Artifact.primary_mod_id).ToLowerInvariant()
    if ($expectedId -notin @($record.ModIds)) { throw "Artifact '$Path' does not expose expected mod ID '$expectedId'." }
    $actualVersion = [string]$record.VersionByModId[$expectedId]
    if ($actualVersion -match '^\$\{.+\}$') { $actualVersion = Get-JarImplementationVersion -Path $Path }
    if ($actualVersion -ne [string]$Artifact.expected_version) {
        throw "Artifact '$Path' version '$actualVersion' does not equal '$($Artifact.expected_version)'."
    }
    return [pscustomobject]@{
        Path = $record.Path
        FileName = $record.FileName
        Sha256 = $record.Sha256
        Length = $record.Length
        PrimaryModId = $expectedId
        Version = $actualVersion
        ModIds = @($record.ModIds)
        VersionByModId = $record.VersionByModId
        MandatoryDependencies = $record.MandatoryDependencies
        MetadataKind = $record.MetadataKind
    }
}

function Test-ProposedDependencyClosure {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][object[]]$Records,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$ProtectedModIds,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$IntroducedPrimaryModIds,
        [Parameter(Mandatory)][object]$MinimumVersions
    )

    $builtIns = @('minecraft', 'forge', 'java')
    $providers = @{}
    foreach ($record in $Records) {
        foreach ($id in @($record.ModIds)) {
            $key = ([string]$id).ToLowerInvariant()
            if (-not $providers.ContainsKey($key)) { $providers[$key] = @() }
            $providers[$key] += $record
        }
    }

    $missing = [Collections.Generic.List[object]]::new()
    $conflicts = [Collections.Generic.List[object]]::new()
    $belowMinimum = [Collections.Generic.List[object]]::new()
    foreach ($idValue in $IntroducedPrimaryModIds) {
        $id = ([string]$idValue).ToLowerInvariant()
        if ($providers.ContainsKey($id)) {
            $hashes = @($providers[$id].Sha256 | Sort-Object -Unique)
            if ($hashes.Count -gt 1) {
                $conflicts.Add([pscustomobject]@{ModId=$id;Files=@($providers[$id].FileName)})
            }
        }
    }
    foreach ($record in $Records) {
        foreach ($ownerId in @($record.ModIds)) {
            if (-not $record.MandatoryDependencies.ContainsKey($ownerId)) { continue }
            foreach ($dependencyValue in @($record.MandatoryDependencies[$ownerId])) {
                $dependencyId = ([string]$dependencyValue).ToLowerInvariant()
                if ($dependencyId -notin $builtIns -and -not $providers.ContainsKey($dependencyId)) {
                    $missing.Add([pscustomobject]@{Owner=$record.FileName;DependencyId=$dependencyId})
                }
            }
        }
    }

    $absentProtected = @($ProtectedModIds | Where-Object { -not $providers.ContainsKey(([string]$_).ToLowerInvariant()) })
    foreach ($property in @($MinimumVersions.PSObject.Properties)) {
        $id = $property.Name.ToLowerInvariant()
        if (-not $providers.ContainsKey($id)) { continue }
        $raw = [string]$providers[$id][0].VersionByModId[$id]
        $matches = [regex]::Matches($raw, '(?<!\d)(\d+(?:\.\d+){1,3})(?!\d)')
        $required = [version][string]$property.Value
        if ($matches.Count -eq 0 -or [version]$matches[$matches.Count - 1].Groups[1].Value -lt $required) {
            $belowMinimum.Add([pscustomobject]@{ModId=$id;Required=[string]$required;Actual=$raw})
        }
    }
    return [pscustomobject]@{
        MissingDependencies = @($missing)
        Conflicts = @($conflicts)
        AbsentProtectedIds = $absentProtected
        BelowMinimumVersions = @($belowMinimum)
        Providers = $providers
    }
}

function Resolve-BatchSource {
    param($Artifact, [string]$PreparedRoot, [string]$CacheRoot)

    if ([string]$Artifact.source_kind -eq 'prepared') {
        return Join-Path $PreparedRoot ([string]$Artifact.file_name)
    }
    New-Item -ItemType Directory -Path $CacheRoot -Force | Out-Null
    $destination = Join-Path $CacheRoot ([string]$Artifact.file_name)
    if (Test-Path -LiteralPath $destination -PathType Leaf) { return $destination }

    $part = "$destination.part"
    Remove-Item -LiteralPath $part -Force -ErrorAction SilentlyContinue
    try {
        if ([string]$Artifact.distribution -eq 'modrinth') {
            $metadata = Invoke-RestMethod -Uri ([string]$Artifact.metadata_url) -Headers @{'User-Agent'='STAT-Mod-Irons-Batch/1.0'}
            if ([string]$metadata.id -ne [string]$Artifact.distribution_version_id) { throw 'Modrinth version ID mismatch.' }
            $primary = @($metadata.files | Where-Object primary -eq $true)
            if ($primary.Count -ne 1) { throw 'Modrinth metadata must expose one primary file.' }
            Invoke-WebRequest -UseBasicParsing -Uri ([string]$primary[0].url) -OutFile $part
            $actual = (Get-FileHash -LiteralPath $part -Algorithm SHA512).Hash.ToLowerInvariant()
            if ($actual -ne ([string]$primary[0].hashes.sha512).ToLowerInvariant()) { throw 'AzureLib SHA-512 mismatch.' }
        } elseif ([string]$Artifact.distribution -eq 'curseforge') {
            Invoke-WebRequest -UseBasicParsing -Uri ([string]$Artifact.download_url) -OutFile $part
        } else {
            throw "Unsupported distribution: $($Artifact.distribution)"
        }
        Move-Item -LiteralPath $part -Destination $destination
    } catch {
        Remove-Item -LiteralPath $part -Force -ErrorAction SilentlyContinue
        throw
    }
    return $destination
}

function Invoke-MedievalContentBatch {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$ClientRoot,
        [Parameter(Mandatory)][string]$PreparedRoot,
        [Parameter(Mandatory)][string]$CacheRoot,
        [Parameter(Mandatory)][string]$CatalogPath,
        [switch]$Apply,
        [scriptblock]$AfterCopyHook
    )

    $catalog = Read-MedievalContentCatalog -Path $CatalogPath
    $clientRootFull = [IO.Path]::GetFullPath($ClientRoot)
    $mods = Join-Path $clientRootFull 'mods'
    $disabled = Join-Path $clientRootFull 'disabled-mods'
    if (-not (Test-Path -LiteralPath $mods -PathType Container)) { throw "Missing mods directory: $mods" }
    New-Item -ItemType Directory -Path $disabled -Force | Out-Null

    $batchRecords = @($catalog.artifacts | ForEach-Object {
        $source = Resolve-BatchSource -Artifact $_ -PreparedRoot $PreparedRoot -CacheRoot $CacheRoot
        Test-BatchArtifact -Artifact $_ -Path $source
    })
    foreach ($record in $batchRecords) {
        $destination = Join-Path $mods $record.FileName
        if (Test-Path -LiteralPath $destination) { throw "Destination already exists: $destination" }
    }

    $activeRecords = @(Get-ChildItem -LiteralPath $mods -Filter '*.jar' -File | ForEach-Object { Get-ModJarRecord -Path $_.FullName })
    $closure = Test-ProposedDependencyClosure -Records @($activeRecords + $batchRecords) -ProtectedModIds @($catalog.protected_mod_ids) -IntroducedPrimaryModIds @($catalog.artifacts.primary_mod_id) -MinimumVersions $catalog.required_active_minimum_versions
    if (@($closure.MissingDependencies).Count) { throw "Missing mandatory dependencies: $($closure.MissingDependencies | ConvertTo-Json -Compress)" }
    if (@($closure.Conflicts).Count) { throw "Mod-ID conflicts: $($closure.Conflicts | ConvertTo-Json -Compress)" }
    if (@($closure.AbsentProtectedIds).Count) { throw "Protected mod IDs absent: $($closure.AbsentProtectedIds -join ', ')" }
    if (@($closure.BelowMinimumVersions).Count) { throw "Required active versions are too old: $($closure.BelowMinimumVersions | ConvertTo-Json -Compress)" }

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $manifestPath = Join-Path $disabled "irons-content-batch-$stamp.json"
    $manifest = [ordered]@{
        schema_version = 1
        status = 'planned'
        created_at = (Get-Date).ToString('o')
        client_root = $clientRootFull
        files = @($batchRecords | ForEach-Object {
            [ordered]@{file_name=$_.FileName;source=$_.Path;sha256=$_.Sha256;length=$_.Length;primary_mod_id=$_.PrimaryModId;version=$_.Version}
        })
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
    if (-not $Apply) {
        return [pscustomobject]@{Status='planned';ManifestPath=$manifestPath;Files=$batchRecords}
    }

    $clientPathPattern = [regex]::Escape($clientRootFull)
    $running = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue | Where-Object { [string]$_.CommandLine -match $clientPathPattern })
    if ($running.Count) { throw "The target client is running in Java process $($running.ProcessId -join ',')." }

    $introduced = [Collections.Generic.List[string]]::new()
    try {
        $index = 0
        foreach ($record in $batchRecords) {
            $index++
            $destination = Join-Path $mods $record.FileName
            Copy-Item -LiteralPath $record.Path -Destination $destination
            $introduced.Add($destination)
            if ($null -ne $AfterCopyHook) { & $AfterCopyHook $index $destination }
            $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToUpperInvariant()
            if ($hash -ne $record.Sha256) { throw "Destination SHA-256 mismatch: $destination" }
        }
        $manifest.status = 'complete'
        $manifest.completed_at = (Get-Date).ToString('o')
        $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        return [pscustomobject]@{Status='complete';ManifestPath=$manifestPath;Files=$batchRecords}
    } catch {
        foreach ($path in $introduced) { Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue }
        $manifest.status = 'rolled_back'
        $manifest.rolled_back_at = (Get-Date).ToString('o')
        $manifest.error = $_.Exception.Message
        $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        return [pscustomobject]@{Status='rolled_back';ManifestPath=$manifestPath;Files=$batchRecords;Error=$_.Exception.Message}
    }
}

Export-ModuleMember -Function Read-MedievalContentCatalog, Test-BatchArtifact, Test-ProposedDependencyClosure, Invoke-MedievalContentBatch

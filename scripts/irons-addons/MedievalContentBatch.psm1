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
    if ($artifacts.Count -ne 9) { throw "Catalog must contain exactly nine artifacts; found $($artifacts.Count)." }
    if (@($artifacts | Where-Object source_kind -eq 'prepared').Count -ne 7) { throw 'Catalog must contain seven prepared artifacts.' }
    if (@($artifacts | Where-Object source_kind -eq 'remote').Count -ne 2) { throw 'Catalog must contain two remote artifacts.' }
    foreach ($field in 'file_name', 'primary_mod_id', 'expected_version') {
        if (@($artifacts.$field | Sort-Object -Unique).Count -ne 9) {
            throw "Catalog field '$field' must have nine unique values."
        }
    }
    foreach ($remote in @($artifacts | Where-Object source_kind -eq 'remote')) {
        if ([string]$remote.primary_mod_id -notin @('azurelib', 'deeperdarker')) {
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

Export-ModuleMember -Function Read-MedievalContentCatalog, Test-BatchArtifact, Test-ProposedDependencyClosure

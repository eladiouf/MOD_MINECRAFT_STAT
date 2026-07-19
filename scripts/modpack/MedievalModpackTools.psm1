Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Assert-PathUnderRoot {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Root,
        [switch]$AllowRoot
    )

    $resolvedPath = [IO.Path]::GetFullPath($Path).TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $resolvedRoot = [IO.Path]::GetFullPath($Root).TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $comparison = [StringComparison]::OrdinalIgnoreCase

    if ($resolvedPath.Equals($resolvedRoot, $comparison)) {
        if ($AllowRoot) { return $true }
        throw "Path must be below root, not equal to it: $resolvedPath"
    }

    $rootPrefix = $resolvedRoot + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedPath.StartsWith($rootPrefix, $comparison)) {
        throw "Path escapes root. Path='$resolvedPath' Root='$resolvedRoot'"
    }

    return $true
}

function Get-ZipEntryText {
    param(
        [Parameter(Mandatory = $true)][IO.Compression.ZipArchive]$Archive,
        [Parameter(Mandatory = $true)][string]$EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) { return $null }

    $stream = $entry.Open()
    try {
        $reader = [IO.StreamReader]::new($stream, [Text.Encoding]::UTF8, $true)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

function Get-TomlStringValue {
    param([string]$Line, [string]$Name)
    $match = [regex]::Match($Line, '^\s*' + [regex]::Escape($Name) + '\s*=\s*["'']([^"'']*)["'']\s*(?:#.*)?$')
    if ($match.Success) { return $match.Groups[1].Value }
    return $null
}

function ConvertFrom-ModsToml {
    param([Parameter(Mandatory = $true)][string]$Content)

    $modIds = [Collections.Generic.List[string]]::new()
    $versions = @{}
    $dependencies = @{}
    $section = $null
    $currentModId = $null
    $dependencyOwner = $null
    $dependencyId = $null
    $dependencyMandatory = $false

    function Complete-Dependency {
        if ($null -ne $dependencyOwner -and $dependencyMandatory -and -not [string]::IsNullOrWhiteSpace($dependencyId)) {
            if (-not $dependencies.ContainsKey($dependencyOwner)) {
                $dependencies[$dependencyOwner] = [Collections.Generic.List[string]]::new()
            }
            $normalizedDependencyId = $dependencyId.ToLowerInvariant()
            if (-not $dependencies[$dependencyOwner].Contains($normalizedDependencyId)) {
                $dependencies[$dependencyOwner].Add($normalizedDependencyId)
            }
        }
    }

    foreach ($rawLine in ($Content -split "`r?`n")) {
        $line = $rawLine.Trim()
        if ($line -match '^\[\[mods\]\]$') {
            Complete-Dependency
            $section = 'mod'
            $currentModId = $null
            $dependencyOwner = $null
            $dependencyId = $null
            $dependencyMandatory = $false
            continue
        }
        if ($line -match '^\[\[dependencies\.([^\]]+)\]\]$') {
            Complete-Dependency
            $section = 'dependency'
            $dependencyOwner = $Matches[1].ToLowerInvariant()
            $dependencyId = $null
            $dependencyMandatory = $false
            continue
        }

        if ($section -eq 'mod') {
            $value = Get-TomlStringValue -Line $line -Name 'modId'
            if ($null -ne $value) {
                $currentModId = $value.ToLowerInvariant()
                if (-not $modIds.Contains($currentModId)) { $modIds.Add($currentModId) }
                if (-not $dependencies.ContainsKey($currentModId)) {
                    $dependencies[$currentModId] = [Collections.Generic.List[string]]::new()
                }
                continue
            }
            $value = Get-TomlStringValue -Line $line -Name 'version'
            if ($null -ne $value -and $null -ne $currentModId) {
                $versions[$currentModId] = $value
            }
            continue
        }

        if ($section -eq 'dependency') {
            $value = Get-TomlStringValue -Line $line -Name 'modId'
            if ($null -ne $value) {
                $dependencyId = $value
                continue
            }
            if ($line -match '^\s*mandatory\s*=\s*(true|false)\s*(?:#.*)?$') {
                $dependencyMandatory = [bool]::Parse($Matches[1])
            }
        }
    }
    Complete-Dependency

    $normalizedDependencies = @{}
    foreach ($modId in $modIds) {
        $normalizedDependencies[$modId] = @($dependencies[$modId] | Sort-Object -Unique)
    }

    return [pscustomobject]@{
        ModIds = @($modIds | Sort-Object -Unique)
        VersionByModId = $versions
        MandatoryDependencies = $normalizedDependencies
    }
}

function ConvertFrom-FabricModJson {
    param([Parameter(Mandatory = $true)][string]$Content)

    $json = $Content | ConvertFrom-Json
    $modId = ([string]$json.id).ToLowerInvariant()
    $dependencies = @()
    if ($null -ne $json.depends) {
        $dependencies = @($json.depends.PSObject.Properties.Name | ForEach-Object { $_.ToLowerInvariant() } | Sort-Object -Unique)
    }

    $versionMap = @{}
    $versionMap[$modId] = [string]$json.version
    $dependencyMap = @{}
    $dependencyMap[$modId] = $dependencies

    return [pscustomobject]@{
        ModIds = @($modId)
        VersionByModId = $versionMap
        MandatoryDependencies = $dependencyMap
    }
}

function Get-ModJarRecord {
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][string]$Path)

    $fullPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "JAR does not exist: $fullPath"
    }

    $stream = [IO.File]::Open($fullPath, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::Read)
    try {
        $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Read, $false)
        try {
            $metadataKind = $null
            $parsed = $null
            $content = Get-ZipEntryText -Archive $archive -EntryName 'META-INF/mods.toml'
            if ($null -ne $content) {
                $metadataKind = 'forge_toml'
                $parsed = ConvertFrom-ModsToml -Content $content
            } else {
                $content = Get-ZipEntryText -Archive $archive -EntryName 'META-INF/neoforge.mods.toml'
                if ($null -ne $content) {
                    $metadataKind = 'neoforge_toml'
                    $parsed = ConvertFrom-ModsToml -Content $content
                } else {
                    $content = Get-ZipEntryText -Archive $archive -EntryName 'fabric.mod.json'
                    if ($null -ne $content) {
                        $metadataKind = 'fabric_json'
                        $parsed = ConvertFrom-FabricModJson -Content $content
                    }
                }
            }

            if ($null -eq $parsed) {
                throw "No supported mod metadata found in JAR: $fullPath"
            }
        } finally {
            $archive.Dispose()
        }
    } finally {
        $stream.Dispose()
    }

    $item = Get-Item -LiteralPath $fullPath
    return [pscustomobject]@{
        Path = $item.FullName
        FileName = $item.Name
        Length = $item.Length
        Sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $fullPath).Hash.ToUpperInvariant()
        ModIds = $parsed.ModIds
        VersionByModId = $parsed.VersionByModId
        MandatoryDependencies = $parsed.MandatoryDependencies
        MetadataKind = $metadataKind
    }
}

function Get-ConfigurationValues {
    param($Configuration, [string]$Name)
    $property = $Configuration.PSObject.Properties[$Name]
    if ($null -eq $property -or $null -eq $property.Value) { return @() }
    return @($property.Value)
}

function Test-AnyValueInSet {
    param([object[]]$Values, [Collections.Generic.HashSet[string]]$Set)
    foreach ($value in $Values) {
        if ($Set.Contains(([string]$value).ToLowerInvariant())) { return $true }
    }
    return $false
}

function Get-MedievalSelection {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][object[]]$ActiveRecords,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][object[]]$ReferenceRecords,
        [Parameter(Mandatory = $true)]$Configuration
    )

    $protectedIds = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($id in (Get-ConfigurationValues $Configuration 'protected_mod_ids')) { [void]$protectedIds.Add([string]$id) }
    $excludedIds = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($id in (Get-ConfigurationValues $Configuration 'excluded_mod_ids')) { [void]$excludedIds.Add([string]$id) }
    $builtInIds = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($id in (Get-ConfigurationValues $Configuration 'built_in_dependency_ids')) { [void]$builtInIds.Add([string]$id) }
    $referenceIds = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($record in $ReferenceRecords) {
        foreach ($id in @($record.ModIds)) { [void]$referenceIds.Add([string]$id) }
    }
    $preferredNames = @(Get-ConfigurationValues $Configuration 'preferred_filenames')
    $excludedPatterns = @(Get-ConfigurationValues $Configuration 'excluded_filename_patterns')

    $entries = @($ActiveRecords | Sort-Object @{ Expression = { $_.FileName.ToLowerInvariant() } }, @{ Expression = { $_.FileName } } | ForEach-Object {
        [pscustomobject]@{
            Record = $_
            Decision = $null
            Reason = $null
            RequiredBy = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
            PreferredDuplicate = $false
        }
    })

    foreach ($hashGroup in @($entries | Group-Object { $_.Record.Sha256 })) {
        if ($hashGroup.Count -lt 2) { continue }
        $candidates = @($hashGroup.Group)
        $winner = $null
        foreach ($preferredName in $preferredNames) {
            $winner = @($candidates | Where-Object { $_.Record.FileName -ceq [string]$preferredName }) | Select-Object -First 1
            if ($null -ne $winner) { break }
        }
        if ($null -eq $winner) {
            $winner = $candidates | Sort-Object @{ Expression = { $_.Record.FileName.Length } }, @{ Expression = { $_.Record.FileName.ToLowerInvariant() } }, @{ Expression = { $_.Record.FileName } } | Select-Object -First 1
        }
        $winner.PreferredDuplicate = $true
        foreach ($candidate in $candidates) {
            if (-not [object]::ReferenceEquals($candidate, $winner)) {
                $candidate.Decision = 'quarantine'
                $candidate.Reason = 'duplicate_sha256'
            }
        }
    }

    foreach ($entry in $entries) {
        if ($entry.Decision -eq 'quarantine') { continue }
        $record = $entry.Record
        if (Test-AnyValueInSet @($record.ModIds) $protectedIds) {
            $entry.Decision = 'retain'
            $entry.Reason = 'protected_mod_id'
            continue
        }
        if (Test-AnyValueInSet @($record.ModIds) $excludedIds) {
            $entry.Decision = 'quarantine'
            $entry.Reason = 'excluded_mod_id'
            continue
        }
        $isExcludedPattern = $false
        foreach ($pattern in $excludedPatterns) {
            if ($record.FileName -match [string]$pattern) {
                $isExcludedPattern = $true
                break
            }
        }
        if ($isExcludedPattern) {
            $entry.Decision = 'quarantine'
            $entry.Reason = 'excluded_filename_pattern'
            continue
        }
        if (Test-AnyValueInSet @($record.ModIds) $referenceIds) {
            $entry.Decision = 'retain'
            $entry.Reason = 'reference_mod_id'
        }
    }

    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($owner in @($entries | Where-Object Decision -eq 'retain')) {
            foreach ($ownerModId in @($owner.Record.ModIds)) {
                if (-not $owner.Record.MandatoryDependencies.ContainsKey($ownerModId)) { continue }
                foreach ($dependencyId in @($owner.Record.MandatoryDependencies[$ownerModId])) {
                    $normalizedDependencyId = ([string]$dependencyId).ToLowerInvariant()
                    if ($builtInIds.Contains($normalizedDependencyId)) { continue }
                    $providers = @($entries | Where-Object {
                        $_.Decision -ne 'quarantine' -and @($_.Record.ModIds) -contains $normalizedDependencyId
                    })
                    if ($providers.Count -eq 0) {
                        $owner.Decision = 'blocked'
                        $owner.Reason = 'unresolved_mandatory_dependency'
                        [void]$owner.RequiredBy.Add($normalizedDependencyId)
                        $changed = $true
                        continue
                    }
                    $providerHashes = @($providers | ForEach-Object { $_.Record.Sha256 } | Sort-Object -Unique)
                    if ($providerHashes.Count -gt 1) {
                        foreach ($provider in $providers) {
                            $provider.Decision = 'manual_review'
                            $provider.Reason = 'same_mod_id_conflict'
                            [void]$provider.RequiredBy.Add($owner.Record.FileName)
                        }
                        continue
                    }
                    $provider = $providers[0]
                    [void]$provider.RequiredBy.Add($owner.Record.FileName)
                    if ($null -eq $provider.Decision) {
                        $provider.Decision = 'retain'
                        $provider.Reason = 'mandatory_dependency'
                        $changed = $true
                    }
                }
            }
        }
    }

    foreach ($entry in $entries) {
        if ($null -eq $entry.Decision) {
            $entry.Decision = 'quarantine'
            $entry.Reason = 'outside_medieval_reference'
        }
    }

    $selectedProviders = @{}
    foreach ($entry in @($entries | Where-Object { $_.Decision -in @('retain', 'manual_review') })) {
        foreach ($modId in @($entry.Record.ModIds)) {
            $normalizedModId = ([string]$modId).ToLowerInvariant()
            if (-not $selectedProviders.ContainsKey($normalizedModId)) { $selectedProviders[$normalizedModId] = @() }
            $selectedProviders[$normalizedModId] += $entry
        }
    }
    foreach ($modId in $selectedProviders.Keys) {
        $providers = @($selectedProviders[$modId])
        $hashes = @($providers | ForEach-Object { $_.Record.Sha256 } | Sort-Object -Unique)
        if ($providers.Count -gt 1 -and $hashes.Count -gt 1) {
            foreach ($provider in $providers) {
                $provider.Decision = 'manual_review'
                $provider.Reason = 'same_mod_id_conflict'
            }
        }
    }

    return @($entries | ForEach-Object {
        [pscustomobject]@{
            FileName = $_.Record.FileName
            Sha256 = $_.Record.Sha256
            Length = $_.Record.Length
            ModIds = (@($_.Record.ModIds) | Sort-Object -Unique) -join ';'
            Decision = $_.Decision
            Reason = $_.Reason
            RequiredBy = (@($_.RequiredBy) | Sort-Object -Unique) -join ';'
            PreferredDuplicate = [bool]$_.PreferredDuplicate
        }
    })
}

Export-ModuleMember -Function Assert-PathUnderRoot, Get-ModJarRecord, Get-MedievalSelection

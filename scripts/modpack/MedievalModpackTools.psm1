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

Export-ModuleMember -Function Assert-PathUnderRoot, Get-ModJarRecord

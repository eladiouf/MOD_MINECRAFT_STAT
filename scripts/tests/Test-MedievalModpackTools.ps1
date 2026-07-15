$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$modulePath = Join-Path $repositoryRoot 'scripts\modpack\MedievalModpackTools.psm1'
Import-Module $modulePath -Force

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]$Expected,
        [Parameter(Mandatory = $true)]$Actual,
        [string]$Message = 'Values differ.'
    )

    $expectedJson = ConvertTo-Json @($Expected) -Compress
    $actualJson = ConvertTo-Json @($Actual) -Compress
    if ($expectedJson -cne $actualJson) {
        throw "$Message Expected=$expectedJson Actual=$actualJson"
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Throws {
    param([scriptblock]$Action, [string]$Message)
    try {
        & $Action
    } catch {
        return
    }
    throw $Message
}

function New-TestJar {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][hashtable]$Entries
    )

    $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew)
    try {
        $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            foreach ($name in $Entries.Keys) {
                $entry = $archive.CreateEntry($name)
                $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
                try {
                    $writer.Write([string]$Entries[$name])
                } finally {
                    $writer.Dispose()
                }
            }
        } finally {
            $archive.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('statmod-medieval-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

try {
    $forgeJar = Join-Path $tempRoot 'forge-example.jar'
    New-TestJar -Path $forgeJar -Entries @{
        'META-INF/mods.toml' = @'
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="example"
version="1.2.3"
displayName="Example"

[[dependencies.example]]
modId="requiredlib"
mandatory=true
versionRange="[1,)"
ordering="NONE"
side="BOTH"

[[dependencies.example]]
modId="optionalhelper"
mandatory=false
versionRange="[1,)"
ordering="NONE"
side="BOTH"

[[dependencies.example]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.21)"
ordering="NONE"
side="BOTH"
'@
    }

    $neoJar = Join-Path $tempRoot 'neo-example.jar'
    New-TestJar -Path $neoJar -Entries @{
        'META-INF/neoforge.mods.toml' = @'
modLoader="javafml"
loaderVersion="[4,)"
license="MIT"

[[mods]]
modId="reference_mod"
version="2.0.0"

[[dependencies.reference_mod]]
modId="neoforge"
mandatory=true
versionRange="[21,)"
ordering="NONE"
side="BOTH"
'@
    }

    $fabricJar = Join-Path $tempRoot 'fabric-example.jar'
    New-TestJar -Path $fabricJar -Entries @{
        'fabric.mod.json' = @'
{
  "schemaVersion": 1,
  "id": "fabric_example",
  "version": "3.0.0",
  "depends": {
    "fabric-api": "*",
    "minecraft": "1.20.x",
    "java": ">=17"
  },
  "suggests": {
    "optional-helper": "*"
  }
}
'@
    }

    $forge = Get-ModJarRecord -Path $forgeJar
    Assert-Equal @('example') $forge.ModIds 'Forge mod ID parsing failed.'
    Assert-Equal '1.2.3' $forge.VersionByModId['example'] 'Forge version parsing failed.'
    Assert-Equal @('minecraft', 'requiredlib') $forge.MandatoryDependencies['example'] 'Forge mandatory dependency parsing failed.'
    Assert-True (-not ($forge.MandatoryDependencies['example'] -contains 'optionalhelper')) 'Optional Forge dependency was treated as mandatory.'
    Assert-Equal 'forge_toml' $forge.MetadataKind 'Forge metadata kind is wrong.'
    Assert-Equal (Get-FileHash -Algorithm SHA256 -LiteralPath $forgeJar).Hash $forge.Sha256 'SHA-256 differs.'

    $neo = Get-ModJarRecord -Path $neoJar
    Assert-Equal @('reference_mod') $neo.ModIds 'NeoForge mod ID parsing failed.'
    Assert-Equal @('neoforge') $neo.MandatoryDependencies['reference_mod'] 'NeoForge dependency parsing failed.'
    Assert-Equal 'neoforge_toml' $neo.MetadataKind 'NeoForge metadata kind is wrong.'

    $fabric = Get-ModJarRecord -Path $fabricJar
    Assert-Equal @('fabric_example') $fabric.ModIds 'Fabric mod ID parsing failed.'
    Assert-Equal @('fabric-api', 'java', 'minecraft') $fabric.MandatoryDependencies['fabric_example'] 'Fabric dependency parsing failed.'
    Assert-True (-not ($fabric.MandatoryDependencies['fabric_example'] -contains 'optional-helper')) 'Fabric suggestion was treated as mandatory.'
    Assert-Equal 'fabric_json' $fabric.MetadataKind 'Fabric metadata kind is wrong.'

    $inside = Join-Path $tempRoot 'child\file.jar'
    Assert-True (Assert-PathUnderRoot -Path $inside -Root $tempRoot) 'A child path should be accepted.'
    Assert-Throws { Assert-PathUnderRoot -Path (Split-Path -Parent $tempRoot) -Root $tempRoot } 'An outside path should be rejected.'
    Assert-Throws { Assert-PathUnderRoot -Path $tempRoot -Root $tempRoot } 'Root equality should require AllowRoot.'
    Assert-True (Assert-PathUnderRoot -Path $tempRoot -Root $tempRoot -AllowRoot) 'AllowRoot should accept equality.'

    $movedForgeJar = Join-Path $tempRoot 'forge-example-moved.jar'
    Move-Item -LiteralPath $forgeJar -Destination $movedForgeJar
    Assert-True (Test-Path -LiteralPath $movedForgeJar -PathType Leaf) 'The parser left the JAR archive locked.'

    Write-Output 'metadata_tests=PASS'
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}

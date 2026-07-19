[CmdletBinding()]
param(
    [string]$ClientRoot = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1',
    [string]$PreparedRoot,
    [string]$CacheRoot,
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if (-not $PreparedRoot) { $PreparedRoot = Join-Path $repoRoot 'external-mods\irons-spells-forge-1.20.1\active' }
if (-not $CacheRoot) { $CacheRoot = Join-Path $repoRoot 'external-mods\irons-spells-forge-1.20.1\dependency-cache' }
$catalog = Join-Path $PSScriptRoot 'medieval-content-batch.json'
Import-Module (Join-Path $PSScriptRoot 'MedievalContentBatch.psm1') -Force
$result = Invoke-MedievalContentBatch -ClientRoot $ClientRoot -PreparedRoot $PreparedRoot -CacheRoot $CacheRoot -CatalogPath $catalog -Apply:$Apply
$result | ConvertTo-Json -Depth 8
if ($result.Status -eq 'rolled_back') { exit 1 }

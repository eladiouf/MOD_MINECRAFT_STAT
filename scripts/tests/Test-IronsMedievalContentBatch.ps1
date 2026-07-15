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

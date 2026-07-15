Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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

Export-ModuleMember -Function Read-MedievalContentCatalog

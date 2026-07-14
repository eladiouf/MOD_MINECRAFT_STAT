param(
    [string]$Catalog = 'external-mods/irons-spells-forge-1.20.1/catalog.csv',
    [string]$Root = 'external-mods/irons-spells-forge-1.20.1',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$manifestColumns = @('project', 'version', 'filename', 'classification', 'source', 'sha256', 'size', 'status', 'dependencies')
$allowedClassifications = @('active', 'needs-testing', 'dependencies')

function New-ManifestRow {
    param($CatalogRow, [string]$Status, [string]$Sha256 = '', [long]$Size = 0)

    [pscustomobject][ordered]@{
        project        = [string]$CatalogRow.project
        version        = [string]$CatalogRow.version
        filename       = [string]$CatalogRow.filename
        classification = [string]$CatalogRow.classification
        source         = [string]$CatalogRow.url
        sha256         = $Sha256
        size           = $Size
        status         = $Status
        dependencies   = [string]$CatalogRow.dependencies
    }
}

function Get-ArchiveStatus {
    param([string]$Path)

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            $hasMetadata = $false
            foreach ($entry in $archive.Entries) {
                $entryName = $entry.FullName.Replace('\', '/')
                if ($entryName -ieq 'META-INF/mods.toml' -or $entryName -ieq 'META-INF/neoforge.mods.toml') {
                    $hasMetadata = $true
                    break
                }
            }
            if ($hasMetadata) { return 'valid' }
            return 'missing-metadata'
        }
        finally {
            $archive.Dispose()
        }
    }
    catch {
        return 'invalid-archive'
    }
}

try {
    $catalogRows = @(Import-Csv -LiteralPath $Catalog)

    # Validate the complete catalogue before creating directories or a manifest.
    foreach ($row in $catalogRows) {
        $serialized = ($row | ConvertTo-Csv -NoTypeInformation) -join "`n"
        if ($serialized -match '(?i)tensura') {
            throw "Tensura is excluded from the Iron's addon catalogue: $($row.project)"
        }
        if ([string]$row.loader -cne 'forge') {
            throw "Invalid loader for '$($row.project)': expected forge."
        }
        if ([string]$row.minecraft -cne '1.20.1') {
            throw "Invalid Minecraft version for '$($row.project)': expected 1.20.1."
        }
        $uri = $null
        if (-not [uri]::TryCreate([string]$row.url, [System.UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -cne 'https') {
            throw "Invalid HTTPS URL for '$($row.project)'."
        }
        if ([string]::IsNullOrWhiteSpace([string]$row.filename) -or -not ([string]$row.filename).EndsWith('.jar', [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Invalid JAR filename for '$($row.project)'."
        }
        if ([System.IO.Path]::GetFileName([string]$row.filename) -cne [string]$row.filename) {
            throw "JAR filename must not contain a path for '$($row.project)'."
        }
        if ($allowedClassifications -cnotcontains [string]$row.classification) {
            throw "Invalid classification for '$($row.project)'."
        }
    }

    New-Item -ItemType Directory -Path $Root -Force | Out-Null
    if (-not $DryRun) {
        foreach ($classification in $allowedClassifications) {
            New-Item -ItemType Directory -Path (Join-Path $Root $classification) -Force | Out-Null
        }
    }

    $manifestPath = Join-Path $Root 'manifest.csv'
    $existingByKey = @{}
    if (Test-Path -LiteralPath $manifestPath) {
        foreach ($entry in @(Import-Csv -LiteralPath $manifestPath)) {
            $key = '{0}|{1}' -f $entry.classification, $entry.filename
            $existingByKey[$key] = $entry
        }
    }

    $results = @()
    $hadDownloadFailure = $false
    foreach ($row in $catalogRows) {
        $destination = Join-Path (Join-Path $Root $row.classification) $row.filename
        Write-Output $destination

        if ($DryRun) {
            $results += New-ManifestRow -CatalogRow $row -Status 'dry-run'
            continue
        }

        $key = '{0}|{1}' -f $row.classification, $row.filename
        if ((Test-Path -LiteralPath $destination) -and $existingByKey.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace($existingByKey[$key].sha256)) {
            $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($hash -eq ([string]$existingByKey[$key].sha256).ToLowerInvariant()) {
                $size = (Get-Item -LiteralPath $destination).Length
                $results += New-ManifestRow -CatalogRow $row -Status 'skipped-existing' -Sha256 $hash -Size $size
                continue
            }
        }

        $partPath = "$destination.part"
        Remove-Item -LiteralPath $partPath -Force -ErrorAction SilentlyContinue
        $downloaded = $false
        $delays = @(2, 5, 10)
        for ($attempt = 0; $attempt -lt 3; $attempt++) {
            try {
                Invoke-WebRequest -Uri $row.url -OutFile $partPath -UseBasicParsing
                if (-not (Test-Path -LiteralPath $partPath) -or (Get-Item -LiteralPath $partPath).Length -le 0) {
                    throw 'Downloaded file is empty.'
                }
                Move-Item -LiteralPath $partPath -Destination $destination -Force
                $downloaded = $true
                break
            }
            catch {
                Remove-Item -LiteralPath $partPath -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds $delays[$attempt]
            }
        }

        if (-not $downloaded) {
            $hadDownloadFailure = $true
            $results += New-ManifestRow -CatalogRow $row -Status 'download-failed'
            continue
        }

        $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
        $size = (Get-Item -LiteralPath $destination).Length
        $status = Get-ArchiveStatus -Path $destination
        $results += New-ManifestRow -CatalogRow $row -Status $status -Sha256 $hash -Size $size
    }

    $results | Select-Object $manifestColumns | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding UTF8
    if ($hadDownloadFailure) { exit 1 }
    exit 0
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}

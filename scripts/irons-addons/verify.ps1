param([string]$Root = 'external-mods/irons-spells-forge-1.20.1')

$ErrorActionPreference = 'Stop'
$classifications = @('active', 'needs-testing', 'dependencies')
$manifestColumns = @('project', 'version', 'filename', 'classification', 'source', 'sha256', 'size', 'status', 'dependencies')

function Write-ManifestAtomically {
    param([object[]]$Rows, [string]$Path)

    $temporaryPath = "$Path.part"
    $backupPath = "$Path.backup.part"
    try {
        $selectedRows = @($Rows | Select-Object $manifestColumns)
        if ($selectedRows.Count -eq 0) {
            $header = ($manifestColumns | ForEach-Object { '"' + $_ + '"' }) -join ','
            [System.IO.File]::WriteAllText($temporaryPath, $header + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($true)))
        }
        else {
            $selectedRows | Export-Csv -LiteralPath $temporaryPath -NoTypeInformation -Encoding UTF8
        }

        if (Test-Path -LiteralPath $Path) {
            [System.IO.File]::Replace($temporaryPath, $Path, $backupPath)
        }
        else {
            [System.IO.File]::Move($temporaryPath, $Path)
        }
    }
    finally {
        Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $backupPath -Force -ErrorAction SilentlyContinue
    }
}

function Get-ArchiveStatus {
    param([string]$Path)

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        try {
            foreach ($entry in $archive.Entries) {
                $entryName = $entry.FullName.Replace('\', '/')
                if ([string]::Equals($entryName, 'META-INF/mods.toml', [System.StringComparison]::Ordinal) -or
                    [string]::Equals($entryName, 'META-INF/neoforge.mods.toml', [System.StringComparison]::Ordinal)) {
                    return 'valid'
                }
            }
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
    $manifestPath = Join-Path $Root 'manifest.csv'
    New-Item -ItemType Directory -Path $Root -Force | Out-Null
    $entries = @()
    if (Test-Path -LiteralPath $manifestPath) {
        $entries = @(Import-Csv -LiteralPath $manifestPath)
    }

    $entryByKey = @{}
    foreach ($entry in $entries) {
        $key = '{0}|{1}' -f $entry.classification, $entry.filename
        $entryByKey[$key] = $entry
    }

    $hadInvalidJar = $false
    foreach ($classification in $classifications) {
        $directory = Join-Path $Root $classification
        if (-not (Test-Path -LiteralPath $directory -PathType Container)) { continue }

        foreach ($jar in @(Get-ChildItem -LiteralPath $directory -Filter '*.jar' -File)) {
            $status = Get-ArchiveStatus -Path $jar.FullName
            $hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            $key = '{0}|{1}' -f $classification, $jar.Name

            if ($entryByKey.ContainsKey($key)) {
                $entry = $entryByKey[$key]
                $entry.sha256 = $hash
                $entry.size = $jar.Length
                $entry.status = $status
            }
            else {
                $entry = [pscustomobject][ordered]@{
                    project        = ''
                    version        = ''
                    filename       = $jar.Name
                    classification = $classification
                    source         = ''
                    sha256         = $hash
                    size           = $jar.Length
                    status         = $status
                    dependencies   = ''
                }
                $entries += $entry
                $entryByKey[$key] = $entry
            }

            Write-Output ("{0}: {1}" -f $jar.FullName, $status)
            if ($status -ne 'valid') { $hadInvalidJar = $true }
            Write-ManifestAtomically -Rows $entries -Path $manifestPath
        }
    }

    Write-ManifestAtomically -Rows $entries -Path $manifestPath
    if ($hadInvalidJar) { exit 1 }
    exit 0
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}

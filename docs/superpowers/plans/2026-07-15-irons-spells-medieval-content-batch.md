# Iron's Spells Medieval Content Batch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Install the seven approved Iron's Spells addons and their two official dependencies into the Forge 1.20.1 client as one verified, reversible transaction.

**Architecture:** A checked-in JSON catalog is the source of truth for filenames, sources, primary mod IDs, versions, and dependency floors. A focused PowerShell installer uses the existing JAR metadata reader, performs a non-installing dry run by default, downloads remote dependencies into a workspace cache, validates the complete proposed mod set, then applies all nine files transactionally with a timestamped manifest and rollback. A standalone PowerShell test suite builds synthetic JAR fixtures so validation and rollback can be proven without touching the real client.

**Tech Stack:** PowerShell 5.1, Forge `META-INF/mods.toml`, JSON transaction manifests, SHA-256, Modrinth API, CurseForge CDN, Forge 47.4.10, Minecraft 1.20.1.

## Global Constraints

- Target client: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1`.
- Prepared addon root: `external-mods\irons-spells-forge-1.20.1\active`.
- Install exactly the seven approved addon filenames and exactly two dependencies; add no other addon or optional dependency.
- AzureLib must be the official Forge 1.20.1 version `3.0.9` (`azurelib`, Modrinth version `M0uqO7Oe`), satisfying `3.0+`.
- Deeper and Darker must be the official Forge 1.20.1 version `1.3.3` (`deeperdarker`, CurseForge file `5906086`), satisfying `1.3.3+`.
- Every candidate must be a readable Forge JAR, expose its expected primary mod ID, and declare compatibility with Minecraft 1.20.1.
- The complete active client plus proposed batch must have zero missing mandatory dependencies and one provider for every newly introduced primary mod ID.
- Do not modify or remove any existing active mod; preserve providers for `statmod`, `epicfight`, `irons_spellbooks`, and `project_babylon_weapons`.
- Dry run is the default. Mutation requires `-Apply`, no Java process using the target client, nine absent destination filenames, and a complete preflight.
- A failed copy or destination hash check removes only files introduced by the current transaction and records `rolled_back` in the manifest.

---

## File Map

- Create `scripts/irons-addons/medieval-content-batch.json`: immutable batch catalog containing all nine artifacts and their validation rules.
- Create `scripts/irons-addons/MedievalContentBatch.psm1`: catalog loading, version/range checks, dependency closure, process guard, manifest creation, transactional copy, and rollback.
- Create `scripts/irons-addons/install-medieval-content-batch.ps1`: thin CLI entry point; dry-run by default and `-Apply` for mutation.
- Create `scripts/tests/Test-IronsMedievalContentBatch.ps1`: isolated fixture tests for catalog validation, closure, dry run, apply, and rollback.
- Create `docs/compatibility/irons-spells-medieval-content-batch.md`: operator commands, acceptance checks, and recovery procedure.
- Reuse `scripts/modpack/MedievalModpackTools.psm1`: `Get-ModJarRecord` supplies archive kind, SHA-256, primary IDs, versions, and mandatory dependency IDs.

### Task 1: Freeze and validate the approved batch catalog

**Files:**
- Create: `scripts/irons-addons/medieval-content-batch.json`
- Create: `scripts/irons-addons/MedievalContentBatch.psm1`
- Create: `scripts/tests/Test-IronsMedievalContentBatch.ps1`

**Interfaces:**
- Consumes: `Get-ModJarRecord -Path <jar>` from `scripts/modpack/MedievalModpackTools.psm1`.
- Produces: `Read-MedievalContentCatalog([string]$Path) -> PSCustomObject` and `Test-BatchArtifact([object]$Artifact, [string]$Path) -> PSCustomObject`.

- [ ] **Step 1: Add a failing catalog contract test**

Create the test harness with these exact assertions:

```powershell
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
Import-Module (Join-Path $repoRoot 'scripts\irons-addons\MedievalContentBatch.psm1') -Force

function Assert-Equal($Expected, $Actual, [string]$Message) {
    if ($Expected -ne $Actual) { throw "$Message Expected='$Expected' Actual='$Actual'" }
}
function Assert-Throws([scriptblock]$Action, [string]$Pattern) {
    try { & $Action; throw 'Expected command to throw.' }
    catch { if ($_.Exception.Message -notmatch $Pattern) { throw } }
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
Write-Output 'PASS catalog contract'
```

- [ ] **Step 2: Run the test and confirm the missing module failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: FAIL because `MedievalContentBatch.psm1` does not exist.

- [ ] **Step 3: Add the exact catalog**

Create `scripts/irons-addons/medieval-content-batch.json`:

```json
{
  "schema_version": 1,
  "minecraft_version": "1.20.1",
  "forge_version": "47.4.10",
  "protected_mod_ids": ["statmod", "epicfight", "irons_spellbooks", "project_babylon_weapons"],
  "required_active_minimum_versions": {"irons_spellbooks":"3.15.0","cataclysm":"3.16"},
  "artifacts": [
    {"file_name":"alshanex_familiars-1.20.1_v1.1.2_HotFix.jar","source_kind":"prepared","primary_mod_id":"alshanex_familiars","expected_version":"1.20.1_v1.1.2_HotFix"},
    {"file_name":"arcanists_equipage-1.0.2-20.1.jar","source_kind":"prepared","primary_mod_id":"arcanists_equipage","expected_version":"1.0.2-20.1"},
    {"file_name":"constructs_casting-2.2.5.jar","source_kind":"prepared","primary_mod_id":"constructs_casting","expected_version":"2.2.5"},
    {"file_name":"gtbcs_geomancy_plus-2.0.0-1.20.1.jar","source_kind":"prepared","primary_mod_id":"gtbcs_geomancy_plus","expected_version":"2.0.0-1.20.1"},
    {"file_name":"magiccircles-1.2.2-1.20.1.jar","source_kind":"prepared","primary_mod_id":"magiccircles","expected_version":"1.2.2-1.20.1"},
    {"file_name":"cataclysm_spellbooks-1.2.9-1.20.1-all.jar","source_kind":"prepared","primary_mod_id":"cataclysm_spellbooks","expected_version":"1.2.9-1.20.1"},
    {"file_name":"darkermagic-1.3.1-1.20.1-ver.b.jar","source_kind":"prepared","primary_mod_id":"darkermagic","expected_version":"1.3.1-1.20.1"},
    {"file_name":"azurelib-neo-3.0.9.jar","source_kind":"remote","primary_mod_id":"azurelib","expected_version":"3.0.9","distribution":"modrinth","distribution_version_id":"M0uqO7Oe","metadata_url":"https://api.modrinth.com/v2/version/M0uqO7Oe"},
    {"file_name":"deeperdarker-forge-1.20.1-1.3.3.jar","source_kind":"remote","primary_mod_id":"deeperdarker","expected_version":"1.3.3","distribution":"curseforge","distribution_file_id":"5906086","download_url":"https://edge.forgecdn.net/files/5906/086/deeperdarker-forge-1.20.1-1.3.3.jar"}
  ]
}
```

During implementation, obtain the actual AzureLib primary filename from Modrinth metadata and update only `file_name` if the API reports a different official primary filename. Keep version ID `M0uqO7Oe`, mod ID `azurelib`, and version `3.0.9` unchanged.

- [ ] **Step 4: Implement strict catalog loading**

Create the module with this public function and validation:

```powershell
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-MedievalContentCatalog {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)
    $fullPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { throw "Catalog does not exist: $fullPath" }
    $catalog = Get-Content -LiteralPath $fullPath -Raw | ConvertFrom-Json
    if ([int]$catalog.schema_version -ne 1) { throw 'Catalog schema_version must equal 1.' }
    if ([string]$catalog.minecraft_version -ne '1.20.1') { throw 'Catalog minecraft_version must equal 1.20.1.' }
    if ([string]$catalog.required_active_minimum_versions.irons_spellbooks -ne '3.15.0') { throw 'Iron''s Spells minimum must equal 3.15.0.' }
    if ([string]$catalog.required_active_minimum_versions.cataclysm -ne '3.16') { throw 'Cataclysm minimum must equal 3.16.' }
    $artifacts = @($catalog.artifacts)
    if ($artifacts.Count -ne 9) { throw "Catalog must contain exactly nine artifacts; found $($artifacts.Count)." }
    if (@($artifacts | Where-Object source_kind -eq 'prepared').Count -ne 7) { throw 'Catalog must contain seven prepared artifacts.' }
    if (@($artifacts | Where-Object source_kind -eq 'remote').Count -ne 2) { throw 'Catalog must contain two remote artifacts.' }
    foreach ($field in 'file_name','primary_mod_id','expected_version') {
        if (@($artifacts.$field | Sort-Object -Unique).Count -ne 9) { throw "Catalog field '$field' must have nine unique values." }
    }
    $allowed = @('azurelib','deeperdarker')
    foreach ($remote in @($artifacts | Where-Object source_kind -eq 'remote')) {
        if ([string]$remote.primary_mod_id -notin $allowed) { throw "Unapproved remote dependency: $($remote.primary_mod_id)" }
    }
    return $catalog
}

Export-ModuleMember -Function Read-MedievalContentCatalog
```

- [ ] **Step 5: Run the catalog contract test**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: `PASS catalog contract` and exit code 0.

- [ ] **Step 6: Commit the catalog contract**

```powershell
git add scripts/irons-addons/medieval-content-batch.json scripts/irons-addons/MedievalContentBatch.psm1 scripts/tests/Test-IronsMedievalContentBatch.ps1
git commit -m "feat: define Iron's Spells medieval content batch"
```

### Task 2: Validate JAR identity, version, loader, and dependency closure

**Files:**
- Modify: `scripts/irons-addons/MedievalContentBatch.psm1`
- Modify: `scripts/tests/Test-IronsMedievalContentBatch.ps1`

**Interfaces:**
- Consumes: catalog artifacts and `Get-ModJarRecord([string]$Path)`.
- Produces: `Test-BatchArtifact([object]$Artifact, [string]$Path) -> PSCustomObject` and `Test-ProposedDependencyClosure([object[]]$Records, [string[]]$ProtectedModIds, [string[]]$IntroducedPrimaryModIds, [object]$MinimumVersions) -> PSCustomObject`.

- [ ] **Step 1: Add failing fixture tests for identity and closure**

Append helpers that create tiny Forge JARs and assert these cases:

```powershell
function New-TestForgeJar {
    param(
        [string]$Path,[string]$ModId,[string]$Version,[string[]]$Dependencies = @(),
        [string]$ModLoader='javafml',[string]$LoaderRange='[47,)',[string]$MinecraftRange='[1.20.1,1.21)'
    )
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $temp = Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path (Join-Path $temp 'META-INF') -Force | Out-Null
    $dependencyToml = @($Dependencies | ForEach-Object {
        "[[dependencies.$ModId]]`nmodId=`"$_`"`nmandatory=true`nversionRange=`"[0,)`"`nordering=`"NONE`"`nside=`"BOTH`""
    }) -join "`n"
    $toml = "modLoader=`"$ModLoader`"`nloaderVersion=`"$LoaderRange`"`nlicense=`"All Rights Reserved`"`n[[mods]]`nmodId=`"$ModId`"`nversion=`"$Version`"`ndisplayName=`"$ModId`"`n[[dependencies.$ModId]]`nmodId=`"minecraft`"`nmandatory=true`nversionRange=`"$MinecraftRange`"`nordering=`"NONE`"`nside=`"BOTH`"`n$dependencyToml"
    Set-Content -LiteralPath (Join-Path $temp 'META-INF\mods.toml') -Value $toml -Encoding UTF8
    [IO.Compression.ZipFile]::CreateFromDirectory($temp, $Path)
    Remove-Item -LiteralPath $temp -Recurse -Force
}

$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('irons-batch-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
try {
    $azurePath = Join-Path $fixtureRoot 'azurelib.jar'
    New-TestForgeJar -Path $azurePath -ModId 'azurelib' -Version '3.0.9'
    $artifact = [pscustomobject]@{ file_name='azurelib.jar'; primary_mod_id='azurelib'; expected_version='3.0.9' }
    $valid = Test-BatchArtifact -Artifact $artifact -Path $azurePath
    Assert-Equal 'azurelib' $valid.PrimaryModId 'Primary mod ID mismatch.'
    Assert-Equal '3.0.9' $valid.Version 'Version mismatch.'
    Assert-Throws { Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='azurelib.jar';primary_mod_id='fabric_only';expected_version='3.0.9'}) -Path $azurePath } 'expected mod ID'
    foreach ($invalid in @(
        @{Name='lowcode.jar';ModLoader='lowcodefml';LoaderRange='[47,)';MinecraftRange='[1.20.1,1.21)';Pattern='modLoader'},
        @{Name='forge48.jar';ModLoader='javafml';LoaderRange='[48,)';MinecraftRange='[1.20.1,1.21)';Pattern='Forge 47.4.10'},
        @{Name='mc121.jar';ModLoader='javafml';LoaderRange='[47,)';MinecraftRange='[1.21,1.22)';Pattern='Minecraft 1.20.1'}
    )) {
        $invalidPath = Join-Path $fixtureRoot $invalid.Name
        New-TestForgeJar -Path $invalidPath -ModId 'invalid' -Version '1.0.0' -ModLoader $invalid.ModLoader -LoaderRange $invalid.LoaderRange -MinecraftRange $invalid.MinecraftRange
        Assert-Throws { Test-BatchArtifact -Artifact ([pscustomobject]@{file_name=$invalid.Name;primary_mod_id='invalid';expected_version='1.0.0'}) -Path $invalidPath } $invalid.Pattern
    }

    $ownerPath = Join-Path $fixtureRoot 'owner.jar'
    New-TestForgeJar -Path $ownerPath -ModId 'owner' -Version '1.0.0' -Dependencies @('azurelib')
    $owner = Test-BatchArtifact -Artifact ([pscustomobject]@{file_name='owner.jar';primary_mod_id='owner';expected_version='1.0.0'}) -Path $ownerPath
    $closure = Test-ProposedDependencyClosure -Records @($owner,$valid) -ProtectedModIds @() -IntroducedPrimaryModIds @('owner','azurelib') -MinimumVersions ([pscustomobject]@{})
    Assert-Equal 0 @($closure.MissingDependencies).Count 'Closure should be complete.'
    $broken = Test-ProposedDependencyClosure -Records @($owner) -ProtectedModIds @() -IntroducedPrimaryModIds @('owner') -MinimumVersions ([pscustomobject]@{})
    Assert-Equal 'azurelib' @($broken.MissingDependencies)[0].DependencyId 'Missing dependency was not reported.'
} finally {
    Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
}
Write-Output 'PASS artifact and closure validation'
```

- [ ] **Step 2: Run the suite and confirm undefined function failures**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: FAIL because `Test-BatchArtifact` is not exported.

- [ ] **Step 3: Implement artifact and closure validation**

Import the existing metadata module using `$PSScriptRoot`, then add:

```powershell
Import-Module (Join-Path $PSScriptRoot '..\modpack\MedievalModpackTools.psm1') -Force

function Get-JarImplementationVersion {
    param([Parameter(Mandatory)][string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path))
    try {
        $entry = $archive.GetEntry('META-INF/MANIFEST.MF')
        if ($null -eq $entry) { throw "JAR uses a version token but has no manifest: $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
    $matches = [regex]::Matches($content, '(?m)^Implementation-Version:\s*(.+)\s*$')
    if ($matches.Count -ne 1) { throw "JAR must contain one Implementation-Version: $Path" }
    return $matches[0].Groups[1].Value.Trim()
}

function Test-VersionInRange {
    param([Parameter(Mandatory)][string]$Value,[Parameter(Mandatory)][string]$Range)
    $valueVersion = [version]$Value
    if ($Range -match '^\[([^,\]]+)\]$') { return $valueVersion -eq [version]$Matches[1] }
    $match = [regex]::Match($Range, '^([\[\(])([^,]*),([^\]\)]*)([\]\)])$')
    if (-not $match.Success) { throw "Unsupported Forge version range: $Range" }
    $lower = $match.Groups[2].Value.Trim(); $upper = $match.Groups[3].Value.Trim()
    if ($lower) {
        $comparison = $valueVersion.CompareTo([version]$lower)
        if ($comparison -lt 0 -or ($comparison -eq 0 -and $match.Groups[1].Value -eq '(')) { return $false }
    }
    if ($upper) {
        $comparison = $valueVersion.CompareTo([version]$upper)
        if ($comparison -gt 0 -or ($comparison -eq 0 -and $match.Groups[4].Value -eq ')')) { return $false }
    }
    return $true
}

function Get-ForgeCompatibility {
    param([Parameter(Mandatory)][string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path))
    try {
        $entry = $archive.GetEntry('META-INF/mods.toml')
        if ($null -eq $entry) { throw "Forge metadata is missing: $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $toml = $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
    $loader = [regex]::Match($toml, '(?m)^\s*modLoader\s*=\s*["'']([^"'']+)["'']').Groups[1].Value
    $loaderRange = [regex]::Match($toml, '(?m)^\s*loaderVersion\s*=\s*["'']([^"'']+)["'']').Groups[1].Value
    $minecraftSection = [regex]::Match($toml, '(?ms)^\s*\[\[dependencies\.[^\]]+\]\]\s*.*?^\s*modId\s*=\s*["'']minecraft["'']\s*.*?(?=^\s*\[\[|\z)')
    $minecraftRange = [regex]::Match($minecraftSection.Value, '(?m)^\s*versionRange\s*=\s*["'']([^"'']+)["'']').Groups[1].Value
    return [pscustomobject]@{ModLoader=$loader;LoaderRange=$loaderRange;MinecraftRange=$minecraftRange}
}

function Test-BatchArtifact {
    [CmdletBinding()]
    param([Parameter(Mandatory)]$Artifact,[Parameter(Mandatory)][string]$Path)
    $record = Get-ModJarRecord -Path $Path
    if ($record.MetadataKind -notmatch '^forge_toml') { throw "Artifact '$Path' is not a Forge mods.toml JAR." }
    $compatibility = Get-ForgeCompatibility -Path $Path
    if ($compatibility.ModLoader -ne 'javafml') { throw "Artifact '$Path' modLoader must be javafml." }
    if (-not (Test-VersionInRange -Value '47.4.10' -Range $compatibility.LoaderRange)) { throw "Artifact '$Path' does not support Forge 47.4.10." }
    if (-not (Test-VersionInRange -Value '1.20.1' -Range $compatibility.MinecraftRange)) { throw "Artifact '$Path' does not support Minecraft 1.20.1." }
    $expectedId = ([string]$Artifact.primary_mod_id).ToLowerInvariant()
    if ($expectedId -notin @($record.ModIds)) { throw "Artifact '$Path' does not expose expected mod ID '$expectedId'." }
    $actualVersion = [string]$record.VersionByModId[$expectedId]
    if ($actualVersion -match '^\$\{.+\}$') { $actualVersion = Get-JarImplementationVersion -Path $Path }
    if ($actualVersion -ne [string]$Artifact.expected_version) { throw "Artifact '$Path' version '$actualVersion' does not equal '$($Artifact.expected_version)'." }
    return [pscustomobject]@{
        Path=$record.Path; FileName=$record.FileName; Sha256=$record.Sha256; Length=$record.Length
        PrimaryModId=$expectedId; Version=$actualVersion; ModIds=@($record.ModIds); VersionByModId=$record.VersionByModId
        MandatoryDependencies=$record.MandatoryDependencies; MetadataKind=$record.MetadataKind
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
    $builtIns = @('minecraft','forge','java')
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
    foreach ($id in $IntroducedPrimaryModIds) {
        $id = ([string]$id).ToLowerInvariant()
        if ($providers.ContainsKey($id)) {
            $hashes = @($providers[$id].Sha256 | Sort-Object -Unique)
            if ($hashes.Count -gt 1) { $conflicts.Add([pscustomobject]@{ModId=$id;Files=@($providers[$id].FileName)}) }
        }
    }
    foreach ($record in $Records) {
        foreach ($ownerId in @($record.ModIds)) {
            foreach ($dependencyId in @($record.MandatoryDependencies[$ownerId])) {
                $dependencyId = ([string]$dependencyId).ToLowerInvariant()
                if ($dependencyId -notin $builtIns -and -not $providers.ContainsKey($dependencyId)) {
                    $missing.Add([pscustomobject]@{Owner=$record.FileName;DependencyId=$dependencyId})
                }
            }
        }
    }
    $absentProtected = @($ProtectedModIds | Where-Object { -not $providers.ContainsKey(([string]$_).ToLowerInvariant()) })
    foreach ($property in @($MinimumVersions.PSObject.Properties)) {
        $id = $property.Name.ToLowerInvariant(); $required = [version]$property.Value
        if (-not $providers.ContainsKey($id)) { continue }
        $raw = [string]$providers[$id][0].VersionByModId[$id]
        $matches = [regex]::Matches($raw, '(?<!\d)(\d+(?:\.\d+){1,3})(?!\d)')
        if ($matches.Count -eq 0 -or [version]$matches[$matches.Count - 1].Groups[1].Value -lt $required) {
            $belowMinimum.Add([pscustomobject]@{ModId=$id;Required=[string]$required;Actual=$raw})
        }
    }
    return [pscustomobject]@{MissingDependencies=@($missing);Conflicts=@($conflicts);AbsentProtectedIds=$absentProtected;BelowMinimumVersions=@($belowMinimum);Providers=$providers}
}

Export-ModuleMember -Function Read-MedievalContentCatalog, Test-BatchArtifact, Test-ProposedDependencyClosure
```

- [ ] **Step 4: Run all fixture tests**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: both `PASS` lines and exit code 0.

- [ ] **Step 5: Validate the seven prepared real JARs**

Run:

```powershell
$catalog = Get-Content scripts/irons-addons/medieval-content-batch.json -Raw | ConvertFrom-Json
$root = 'external-mods\irons-spells-forge-1.20.1\active'
Import-Module .\scripts\irons-addons\MedievalContentBatch.psm1 -Force
$catalog.artifacts | Where-Object source_kind -eq prepared | ForEach-Object { Test-BatchArtifact -Artifact $_ -Path (Join-Path $root $_.file_name) } | Format-Table FileName,PrimaryModId,Version,Sha256
```

Expected: seven records, correct primary IDs and versions, no exception.

- [ ] **Step 6: Commit validation**

```powershell
git add scripts/irons-addons/MedievalContentBatch.psm1 scripts/tests/Test-IronsMedievalContentBatch.ps1
git commit -m "test: validate Iron's Spells batch metadata"
```

### Task 3: Acquire official dependencies and implement transactional installation

**Files:**
- Modify: `scripts/irons-addons/MedievalContentBatch.psm1`
- Create: `scripts/irons-addons/install-medieval-content-batch.ps1`
- Modify: `scripts/tests/Test-IronsMedievalContentBatch.ps1`

**Interfaces:**
- Consumes: the catalog, prepared root, cache root, and target client root.
- Produces: `Invoke-MedievalContentBatch([string]$ClientRoot,[string]$PreparedRoot,[string]$CacheRoot,[string]$CatalogPath,[switch]$Apply,[scriptblock]$AfterCopyHook) -> PSCustomObject`; `AfterCopyHook` is used only by isolated rollback tests.

- [ ] **Step 1: Add failing dry-run, apply, and rollback tests**

Extend the fixture to build nine catalog-matching test JARs. Create `mods` and `disabled-mods`, call `Invoke-MedievalContentBatch` without `-Apply`, and assert `mods` remains empty and status is `planned`. Call again with `-Apply` and assert nine JARs, nine matching hashes, and manifest status `complete`. For rollback, replace one planned source after preflight by using a module test hook scriptblock that corrupts the fifth destination, then assert zero introduced JARs remain and manifest status is `rolled_back`:

```powershell
$dryRun = Invoke-MedievalContentBatch -ClientRoot $client -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $fixtureCatalog
Assert-Equal 'planned' $dryRun.Status 'Dry run status mismatch.'
Assert-Equal 0 @(Get-ChildItem (Join-Path $client 'mods') -Filter '*.jar').Count 'Dry run mutated mods.'

$applied = Invoke-MedievalContentBatch -ClientRoot $client -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $fixtureCatalog -Apply
Assert-Equal 'complete' $applied.Status 'Apply status mismatch.'
Assert-Equal 9 @(Get-ChildItem (Join-Path $client 'mods') -Filter '*.jar').Count 'Apply did not install nine JARs.'

$corruptFifthCopy = { param($index,$destination) if ($index -eq 5) { [IO.File]::WriteAllText($destination,'corrupt') } }
$rollback = Invoke-MedievalContentBatch -ClientRoot $rollbackClient -PreparedRoot $prepared -CacheRoot $cache -CatalogPath $fixtureCatalog -Apply -AfterCopyHook $corruptFifthCopy
Assert-Equal 'rolled_back' $rollback.Status 'Rollback status mismatch.'
Assert-Equal 0 @(Get-ChildItem (Join-Path $rollbackClient 'mods') -Filter '*.jar').Count 'Rollback left introduced JARs.'
Write-Output 'PASS transaction dry-run apply rollback'
```

- [ ] **Step 2: Run tests and confirm the missing transaction function failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: FAIL because `Invoke-MedievalContentBatch` is undefined.

- [ ] **Step 3: Implement official dependency acquisition**

Add `Resolve-BatchSource` with these exact rules:

```powershell
function Resolve-BatchSource {
    param($Artifact,[string]$PreparedRoot,[string]$CacheRoot)
    if ([string]$Artifact.source_kind -eq 'prepared') { return Join-Path $PreparedRoot ([string]$Artifact.file_name) }
    New-Item -ItemType Directory -Path $CacheRoot -Force | Out-Null
    $destination = Join-Path $CacheRoot ([string]$Artifact.file_name)
    if (Test-Path -LiteralPath $destination -PathType Leaf) { return $destination }
    $part = "$destination.part"
    Remove-Item -LiteralPath $part -Force -ErrorAction SilentlyContinue
    if ([string]$Artifact.distribution -eq 'modrinth') {
        $metadata = Invoke-RestMethod -Uri ([string]$Artifact.metadata_url) -Headers @{ 'User-Agent'='STAT-Mod-Irons-Batch/1.0' }
        if ([string]$metadata.id -ne [string]$Artifact.distribution_version_id) { throw 'Modrinth version ID mismatch.' }
        $primary = @($metadata.files | Where-Object primary -eq $true)
        if ($primary.Count -ne 1) { throw 'Modrinth metadata must expose one primary file.' }
        Invoke-WebRequest -UseBasicParsing -Uri ([string]$primary[0].url) -OutFile $part
        $actual = (Get-FileHash -LiteralPath $part -Algorithm SHA512).Hash.ToLowerInvariant()
        if ($actual -ne ([string]$primary[0].hashes.sha512).ToLowerInvariant()) { throw 'AzureLib SHA-512 mismatch.' }
    } else {
        Invoke-WebRequest -UseBasicParsing -Uri ([string]$Artifact.download_url) -OutFile $part
    }
    Move-Item -LiteralPath $part -Destination $destination
    return $destination
}
```

After download, always pass the file through `Test-BatchArtifact`; the CurseForge artifact is accepted only if the JAR metadata reports `deeperdarker` version `1.3.3`.

- [ ] **Step 4: Implement the transaction**

Add `Invoke-MedievalContentBatch` with this state sequence:

```powershell
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
    $mods = Join-Path $ClientRoot 'mods'
    $disabled = Join-Path $ClientRoot 'disabled-mods'
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
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $manifestPath = Join-Path $disabled "irons-content-batch-$stamp.json"
    $manifest = [ordered]@{schema_version=1;status='planned';created_at=(Get-Date).ToString('o');client_root=[IO.Path]::GetFullPath($ClientRoot);files=@($batchRecords | ForEach-Object {[ordered]@{file_name=$_.FileName;source=$_.Path;sha256=$_.Sha256;length=$_.Length;primary_mod_id=$_.PrimaryModId;version=$_.Version}})}
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
    if (-not $Apply) { return [pscustomobject]@{Status='planned';ManifestPath=$manifestPath;Files=$batchRecords} }
    $clientPathPattern = [regex]::Escape([IO.Path]::GetFullPath($ClientRoot))
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
        $manifest.status = 'complete'; $manifest.completed_at = (Get-Date).ToString('o')
        $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        return [pscustomobject]@{Status='complete';ManifestPath=$manifestPath;Files=$batchRecords}
    } catch {
        foreach ($path in $introduced) { Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue }
        $manifest.status = 'rolled_back'; $manifest.rolled_back_at = (Get-Date).ToString('o'); $manifest.error = $_.Exception.Message
        $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
        return [pscustomobject]@{Status='rolled_back';ManifestPath=$manifestPath;Files=$batchRecords;Error=$_.Exception.Message}
    }
}

Export-ModuleMember -Function Read-MedievalContentCatalog, Test-BatchArtifact, Test-ProposedDependencyClosure, Invoke-MedievalContentBatch
```

- [ ] **Step 5: Add the thin command-line entry point**

Create `scripts/irons-addons/install-medieval-content-batch.ps1`:

```powershell
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
```

- [ ] **Step 6: Run the full isolated suite**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1`

Expected: three `PASS` lines, rollback leaves zero introduced files, exit code 0.

- [ ] **Step 7: Commit acquisition and transaction support**

```powershell
git add scripts/irons-addons/MedievalContentBatch.psm1 scripts/irons-addons/install-medieval-content-batch.ps1 scripts/tests/Test-IronsMedievalContentBatch.ps1
git commit -m "feat: install Iron's Spells batch transactionally"
```

### Task 4: Perform the real preflight and installation

**Files:**
- Runtime write: `external-mods/irons-spells-forge-1.20.1/dependency-cache/*.jar`
- External runtime write: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1\mods\*.jar`
- External runtime write: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1\disabled-mods\irons-content-batch-*.json`

**Interfaces:**
- Consumes: completed installer and unchanged target client.
- Produces: one complete manifest and nine verified active JARs.

- [ ] **Step 1: Snapshot the active baseline**

Run:

```powershell
$mods = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1\mods'
$baseline = @(Get-ChildItem -LiteralPath $mods -Filter '*.jar' -File)
"BASELINE_COUNT=$($baseline.Count)"
$baseline | Get-FileHash -Algorithm SHA256 | Sort-Object Path | Format-Table Path,Hash
```

Expected: every active JAR is readable; retain the printed baseline count for the `baseline + 9` assertion.

- [ ] **Step 2: Run the non-installing real dry run**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1`

Expected: JSON status `planned`, nine files in the manifest, no new file in the client's `mods` directory, zero missing dependencies, zero conflicts, and all four protected IDs present.

- [ ] **Step 3: Inspect the planned manifest**

Run:

```powershell
$disabled = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1\disabled-mods'
$manifest = Get-ChildItem -LiteralPath $disabled -Filter 'irons-content-batch-*.json' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-Content -LiteralPath $manifest.FullName -Raw
```

Expected: `status` equals `planned`; seven prepared paths and two dependency-cache paths are recorded with non-empty SHA-256 hashes.

- [ ] **Step 4: Apply the complete batch**

This external client write requires the user's filesystem approval.

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1 -Apply`

Expected: JSON status `complete`; the manifest contains `completed_at`; destination hashes equal source hashes.

- [ ] **Step 5: Re-run static closure and count acceptance**

Run the installer in dry-run mode again only after changing the script invocation to a verification-only command over the active folder, because destination collision protection must remain enabled. Use:

```powershell
Import-Module .\scripts\modpack\MedievalModpackTools.psm1 -Force
Import-Module .\scripts\irons-addons\MedievalContentBatch.psm1 -Force
$mods = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1\mods'
$records = @(Get-ChildItem -LiteralPath $mods -Filter '*.jar' -File | ForEach-Object { Get-ModJarRecord -Path $_.FullName })
$minimums = [pscustomobject]@{irons_spellbooks='3.15.0';cataclysm='3.16'}
$result = Test-ProposedDependencyClosure -Records $records -ProtectedModIds @('statmod','epicfight','irons_spellbooks','project_babylon_weapons') -IntroducedPrimaryModIds @('alshanex_familiars','arcanists_equipage','constructs_casting','gtbcs_geomancy_plus','magiccircles','cataclysm_spellbooks','darkermagic','azurelib','deeperdarker') -MinimumVersions $minimums
if ($result.MissingDependencies.Count -or $result.Conflicts.Count -or $result.AbsentProtectedIds.Count -or $result.BelowMinimumVersions.Count) { $result | ConvertTo-Json -Depth 8; exit 1 }
"ACTIVE_COUNT=$($records.Count)"
```

Expected: active count equals the baseline count plus 9; zero missing dependency, conflict, or absent protected ID.

### Task 5: Document and execute runtime acceptance

**Files:**
- Create: `docs/compatibility/irons-spells-medieval-content-batch.md`

**Interfaces:**
- Consumes: completed transaction manifest and Minecraft logs/crash reports.
- Produces: reproducible operator and recovery checklist.

- [ ] **Step 1: Write the operator document**

Create the document with the exact sections and commands below:

```markdown
# Iron's Spells Medieval Content Batch — Forge 1.20.1

## Installed transaction

The batch contains the seven filenames in `scripts/irons-addons/medieval-content-batch.json` plus AzureLib 3.0.9 and Deeper and Darker 1.3.3. The latest `disabled-mods/irons-content-batch-*.json` is the authoritative source/hash manifest.

## Commands

Dry run:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1`

Apply:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1 -Apply`

## Runtime acceptance

- Reach the main menu without a new crash report or fatal mod-loading error.
- Open or create a world without registry, datapack, recipe, or mixin failure.
- Cast one previously working Iron's Spells spell.
- Exercise one Cataclysm Spellbooks feature.
- Exercise one Darker Magic feature.
- Exercise one Geomancy Plus feature.
- Exercise one feature from Familiars, Arcanist's Equipage, Constructs' Casting, or Magic Circles.

## Recovery

Close Minecraft. Read the latest complete manifest and move only its nine listed filenames from `mods` to a new timestamped folder below `disabled-mods`. If logs identify one incompatible addon, quarantine that addon and any addon whose mandatory dependency requires it; otherwise quarantine all nine. Never remove STAT Mod, Epic Fight, Iron's Spells, or Project Babylon as part of this recovery.
```

- [ ] **Step 2: Capture the pre-launch crash-report timestamp**

Run:

```powershell
$client = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1'
$before = Get-ChildItem -LiteralPath (Join-Path $client 'crash-reports') -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$before | Format-List FullName,LastWriteTime
```

Expected: timestamp is recorded, or no existing crash report is present.

- [ ] **Step 3: Perform runtime acceptance in order**

Launch the `test-1.20.1` profile and execute the seven checklist items in the operator document in order. After main menu and after world entry, inspect `logs/latest.log` for `FATAL`, `ModLoadingException`, `Mixin apply failed`, `Registry remapping failed`, or `Datapack` errors.

Expected: no new crash report, no fatal match, existing casting works, and the four addon feature checks work.

- [ ] **Step 4: Recover only from evidence if runtime acceptance fails**

Read the newest crash report and `logs/latest.log`. If a single addon is named, close Minecraft and move that addon plus direct dependents from `mods` into `disabled-mods\irons-content-runtime-failure-<timestamp>`. If the failure cannot be isolated, move all nine filenames from the complete manifest. Re-run the static closure command from Task 4 before launching again.

Expected: recovery never touches files absent from the batch manifest and never removes protected providers.

- [ ] **Step 5: Run repository verification**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/tests/Test-IronsMedievalContentBatch.ps1
git diff --check
```

Expected: all test groups print `PASS`; `git diff --check` prints nothing and exits 0.

- [ ] **Step 6: Commit documentation**

```powershell
git add docs/compatibility/irons-spells-medieval-content-batch.md
git commit -m "docs: record Iron's Spells batch validation"
```

## Completion Gate

- [ ] Exactly nine batch artifacts appear in the latest complete manifest.
- [ ] The active JAR count is the captured baseline plus nine.
- [ ] Every introduced destination SHA-256 equals its recorded source SHA-256.
- [ ] Mandatory dependency closure and introduced primary mod-ID uniqueness both pass.
- [ ] `statmod`, `epicfight`, `irons_spellbooks`, and `project_babylon_weapons` remain active.
- [ ] Main menu, world loading, existing spell casting, and the four representative addon checks pass without a newer crash report.
- [ ] Deferred Iron's Spells integrations remain unchanged.

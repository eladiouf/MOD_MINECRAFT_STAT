param(
    [Parameter(Mandatory)]
    [string]$ProviderModsDirectory
)

$ErrorActionPreference = 'Stop'

$root = [IO.Path]::GetFullPath((git rev-parse --show-toplevel))
$logPath = [IO.Path]::GetFullPath((Join-Path $root 'run/logs/latest.log'))
$rootPrefix = $root.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar
$providerRoot = [IO.Path]::GetFullPath($ProviderModsDirectory)
$providerPrefix = $providerRoot.TrimEnd(
    [IO.Path]::DirectorySeparatorChar,
    [IO.Path]::AltDirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
$deobfRoot = [IO.Path]::GetFullPath((Join-Path $root 'build/provider-smoke-libs'))
$initScript = [IO.Path]::GetFullPath((Join-Path $root 'build/provider-smoke.init.gradle'))

if (-not $logPath.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe GameTest log path: $logPath"
}
if (-not (Test-Path -LiteralPath $providerRoot -PathType Container)) {
    throw "Provider mods directory does not exist: $providerRoot"
}
if (-not $deobfRoot.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase) -or
    -not $initScript.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Unsafe provider smoke workspace.'
}

if (Test-Path -LiteralPath $logPath) {
    Remove-Item -LiteralPath $logPath -Force
}

$gradle = if ($IsWindows -or $env:OS -eq 'Windows_NT') {
    Join-Path $root 'gradlew.bat'
}
else {
    Join-Path $root 'gradlew'
}

$providers = @(
    @{
        Source = 'epic-fight-20.14.17-mc1.20.1-forge.jar'
        Target = 'epicfight-20.14.17.jar'
        LogId = 'epicfight'
    },
    @{
        Source = 'puffish_attributes-0.8.2-1.20-forge.jar'
        Target = 'puffish_attributes-0.8.2.jar'
        LogId = 'puffish_attributes'
    },
    @{
        Source = 'irons_spellbooks-1.20.1-3.16.2.jar'
        Target = 'irons_spellbooks-3.16.2.jar'
        LogId = 'irons_spellbooks'
    },
    @{
        Source = 'irons_lib-1.20.1-2.1.0.jar'
        Target = 'irons_lib-2.1.0.jar'
        LogId = 'irons_lib'
    },
    @{
        Source = 'curios-forge-5.14.1+1.20.1.jar'
        Target = 'curios-5.14.1.jar'
        LogId = 'curios'
    },
    @{
        Source = 'geckolib-forge-1.20.1-4.8.4.jar'
        Target = 'geckolib-4.8.4.jar'
        LogId = 'geckolib'
    },
    @{
        Source = 'player-animation-lib-forge-1.0.2-rc1+1.20.jar'
        Target = 'playeranimator-1.0.2-rc1.jar'
        LogId = 'playeranimator'
    },
    @{
        Source = 'lootr-forge-1.20-0.7.35.94.jar'
        Target = 'lootr-0.7.35.94.jar'
        LogId = 'lootr'
    }
)
$createdFiles = [Collections.Generic.List[string]]::new()
$previousSmokeDirectory = $env:STATMOD_PROVIDER_SMOKE_DIR

try {
    New-Item -ItemType Directory -Path $deobfRoot -Force | Out-Null
    foreach ($provider in $providers) {
        $source = [IO.Path]::GetFullPath((Join-Path $providerRoot $provider.Source))
        $target = [IO.Path]::GetFullPath((Join-Path $deobfRoot $provider.Target))
        if (-not $source.StartsWith($providerPrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Unsafe provider source: $source"
        }
        if (-not $target.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Unsafe provider target: $target"
        }
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Missing required provider JAR: $source"
        }
        if (Test-Path -LiteralPath $target) {
            throw "Refusing to overwrite provider smoke file: $target"
        }
        Copy-Item -LiteralPath $source -Destination $target
        $createdFiles.Add($target)
    }

    $init = @'
gradle.beforeProject { project ->
    project.pluginManager.withPlugin('net.minecraftforge.gradle') {
        project.repositories.flatDir {
            dirs System.getenv('STATMOD_PROVIDER_SMOKE_DIR')
        }
        def fg = project.extensions.getByName('fg')
        project.dependencies.add('runtimeOnly', fg.deobf('local:epicfight:20.14.17'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:puffish_attributes:0.8.2'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:irons_spellbooks:3.16.2'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:irons_lib:2.1.0'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:curios:5.14.1'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:geckolib:4.8.4'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:playeranimator:1.0.2-rc1'))
        project.dependencies.add('runtimeOnly', fg.deobf('local:lootr:0.7.35.94'))
        project.minecraft.runs.configureEach {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile',
                    project.file('build/createSrgToMcp/output.srg').absolutePath
        }
    }
}
'@
    [IO.File]::WriteAllText($initScript, $init)
    $createdFiles.Add($initScript)
    $env:STATMOD_PROVIDER_SMOKE_DIR = $deobfRoot

    & $gradle -I $initScript runGameTestServer --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Forge GameTest server exited with code $LASTEXITCODE"
    }

    if (-not (Test-Path -LiteralPath $logPath -PathType Leaf)) {
        throw "Forge GameTest server did not create $logPath"
    }

    $log = Get-Content -LiteralPath $logPath -Raw
    foreach ($provider in $providers) {
        if ($log -notmatch [regex]::Escape($provider.LogId)) {
            throw "Forge GameTest log does not contain provider $($provider.LogId)."
        }
    }

    $fatalPattern = '\[.*FATAL.*\]|Exception in server tick loop|Failed to start the minecraft server|ModLoadingException|Crash report saved to'
    $fatalMatches = @([regex]::Matches($log, $fatalPattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase) |
        ForEach-Object Value |
        Select-Object -Unique)
    if ($fatalMatches.Count -ne 0) {
        throw "Forge GameTest server log contains fatal signatures:`n$($fatalMatches -join "`n")"
    }

    'OK required-provider Forge GameTest server smoke'
}
finally {
    $env:STATMOD_PROVIDER_SMOKE_DIR = $previousSmokeDirectory
    foreach ($createdFile in $createdFiles) {
        $resolved = [IO.Path]::GetFullPath($createdFile)
        if ($resolved.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase) -and
            (Test-Path -LiteralPath $resolved -PathType Leaf)) {
            Remove-Item -LiteralPath $resolved -Force
        }
    }
}

$ErrorActionPreference = 'Stop'

$root = [IO.Path]::GetFullPath((git rev-parse --show-toplevel))
$logPath = [IO.Path]::GetFullPath((Join-Path $root 'run/logs/latest.log'))
$rootPrefix = $root.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar

if (-not $logPath.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe GameTest log path: $logPath"
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

& $gradle runGameTestServer --console=plain
if ($LASTEXITCODE -ne 0) {
    throw "Forge GameTest server exited with code $LASTEXITCODE"
}

if (-not (Test-Path -LiteralPath $logPath -PathType Leaf)) {
    throw "Forge GameTest server did not create $logPath"
}

$log = Get-Content -LiteralPath $logPath -Raw
$fatalPattern = '\[.*FATAL.*\]|Exception in server tick loop|Failed to start the minecraft server|ModLoadingException|Crash report saved to'
$fatalMatches = @([regex]::Matches($log, $fatalPattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase) |
    ForEach-Object Value |
    Select-Object -Unique)
if ($fatalMatches.Count -ne 0) {
    throw "Forge GameTest server log contains fatal signatures:`n$($fatalMatches -join "`n")"
}

'OK dedicated Forge GameTest server smoke'

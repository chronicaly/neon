param([switch]$SkipInstall)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    # An existing file cannot be a Unix-domain socket directory. Java falls back to TCP.
    $socketFallback = Join-Path $projectRoot 'gradlew.bat'
    $env:JAVA_TOOL_OPTIONS = ($previousJavaOptions + ' -Djdk.net.unixdomain.tmpdir="' + $socketFallback + '"').Trim()
    Push-Location $projectRoot
    try {
        $buildArguments = @('build', '--console=plain')
        if ($SkipInstall) { $buildArguments += '-PskipInstall' }
        & .\gradlew.bat @buildArguments
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed: $LASTEXITCODE" }
    } finally { Pop-Location }
} finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions }

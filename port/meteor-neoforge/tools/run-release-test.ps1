param([string]$JavaHome = 'C:\Program Files\Zulu\zulu-21', [switch]$WithBaritone, [switch]$InteractiveUi, [switch]$UiOnly)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$instance = Join-Path $project 'build/release-instance'
$libraries = Join-Path $instance 'libraries'
$base = Get-Content -LiteralPath (Join-Path $instance 'versions/1.21.1/1.21.1.json') -Raw | ConvertFrom-Json
$neo = Get-Content -LiteralPath (Join-Path $instance 'versions/neoforge-21.1.265/neoforge-21.1.265.json') -Raw | ConvertFrom-Json

function Allowed($rules) {
    if (!$rules) { return $true }
    $allowed = $false
    foreach ($rule in $rules) {
        $matches = $true
        if ($rule.os.name -and $rule.os.name -ne 'windows') { $matches = $false }
        if ($rule.os.arch -and 'amd64' -notmatch $rule.os.arch) { $matches = $false }
        if ($rule.features) { $matches = $false }
        if ($matches) { $allowed = $rule.action -eq 'allow' }
    }
    return $allowed
}

$classpath = [System.Collections.Generic.List[string]]::new()
foreach ($lib in @($neo.libraries) + @($base.libraries)) {
    if (!(Allowed $lib.rules)) { continue }
    $artifact = $lib.downloads.artifact
    if (!$artifact) { throw "Missing artifact metadata: $($lib.name)" }
    $dest = Join-Path $libraries $artifact.path
    if (!(Test-Path -LiteralPath $dest)) {
        New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
        $parts = $lib.name.Split(':')
        $cacheRoot = Join-Path $env:USERPROFILE ".gradle/caches/modules-2/files-2.1/$($parts[0])/$($parts[1])/$($parts[2])"
        $cached = Get-ChildItem -LiteralPath $cacheRoot -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -eq (Split-Path $dest -Leaf) } | Select-Object -First 1
        $launcherCache = Join-Path 'D:/PCL2CE/.minecraft/libraries' $artifact.path
        if ($cached) { Copy-Item -LiteralPath $cached.FullName -Destination $dest }
        elseif (Test-Path -LiteralPath $launcherCache) { Copy-Item -LiteralPath $launcherCache -Destination $dest }
        elseif ($artifact.url) { Invoke-WebRequest -Uri $artifact.url -OutFile $dest }
        else { throw "Installer must generate $dest" }
    }
    if ($artifact.sha1 -and (Get-FileHash -LiteralPath $dest -Algorithm SHA1).Hash -ne $artifact.sha1) { throw "Checksum mismatch: $dest" }
    $classpath.Add($dest)
}
$vanillaJar = Join-Path $instance 'versions/1.21.1/1.21.1.jar'
if (!(Test-Path -LiteralPath $vanillaJar)) { throw 'Vanilla client jar not installed' }
$versionJar = Join-Path $instance 'versions/neoforge-21.1.265/neoforge-21.1.265.jar'
Copy-Item -LiteralPath $vanillaJar -Destination $versionJar
$classpath.Add($versionJar)
$natives = Join-Path $instance 'natives'
New-Item -ItemType Directory -Force -Path $natives,(Join-Path $instance 'mods') | Out-Null
if (!(Test-Path -LiteralPath (Join-Path $instance 'options.txt'))) {
    @('version:3955','onboardAccessibility:false','pauseOnLostFocus:false','renderDistance:8','simulationDistance:5','maxFps:60') |
        Set-Content -LiteralPath (Join-Path $instance 'options.txt') -Encoding utf8NoBOM
}
Copy-Item -LiteralPath (Join-Path $project 'build/libs/meteor-client-neoforge-0.5.8-neoforge.9.jar') -Destination (Join-Path $instance 'mods/meteor-client-neoforge.jar')
if ($WithBaritone) {
    Copy-Item -LiteralPath (Join-Path $project '../../baritone-neoforge-1.21.1.jar') -Destination (Join-Path $instance 'mods/baritone-neoforge.jar')
} elseif (Test-Path -LiteralPath (Join-Path $instance 'mods/baritone-neoforge.jar')) {
    $ownedBaritone = Join-Path $instance 'mods/baritone-neoforge.jar'
    if ((Get-FileHash -LiteralPath $ownedBaritone).Hash -ne (Get-FileHash -LiteralPath (Join-Path $project '../../baritone-neoforge-1.21.1.jar')).Hash) {
        throw 'Refusing to remove an unrecognized Baritone jar from the validation instance'
    }
    Remove-Item -LiteralPath $ownedBaritone
}

$substitutions = @{
    '${library_directory}' = $libraries
    '${classpath_separator}' = ';'
    '${version_name}' = 'neoforge-21.1.265'
    '${natives_directory}' = $natives
    '${launcher_name}' = 'MeteorPortValidation'
    '${launcher_version}' = '1'
    '${classpath}' = ($classpath | Select-Object -Unique) -join ';'
    '${auth_player_name}' = 'MeteorTest'
    '${game_directory}' = $instance
    '${assets_root}' = (Join-Path $env:USERPROFILE '.gradle/caches/unimined/assets')
    '${assets_index_name}' = '17'
    '${auth_uuid}' = '00000000-0000-0000-0000-000000000001'
    '${auth_access_token}' = '0'
    '${user_type}' = 'legacy'
    '${version_type}' = 'release'
    '${clientid}' = '0'
    '${auth_xuid}' = '0'
}
function Expand-Args($values) {
    foreach ($value in $values) {
        if ($value -is [string]) { $texts = @($value) }
        elseif (Allowed $value.rules) { $texts = @($value.value) }
        else { continue }
        foreach ($text in $texts) {
            foreach ($key in $substitutions.Keys) { $text = $text.Replace($key, $substitutions[$key]) }
            if ($text -match '\$\{') { throw "Unresolved launcher placeholder: $text" }
            $text
        }
    }
}
$jvm = @(Expand-Args $base.arguments.jvm) + @(Expand-Args $neo.arguments.jvm)
$game = @(Expand-Args $base.arguments.game) + @(Expand-Args $neo.arguments.game)
$launchArgs = @("-Dmeteor.ui.only=$($UiOnly.IsPresent.ToString().ToLowerInvariant())",'-Xmx3G','-Dmeteor.selftest=true', "-Dmeteor.ui.interactive=$($InteractiveUi.IsPresent.ToString().ToLowerInvariant())") + $jvm + @($neo.mainClass) + $game
# Use Java's argument file to avoid the Windows command-line length limit.
$argfile = Join-Path $instance 'launch-args.txt'
$launchArgs | ForEach-Object { '"' + $_.Replace('\','/').Replace('"','\"') + '"' } | Set-Content -LiteralPath $argfile -Encoding utf8NoBOM
Push-Location $instance
try {
    Set-Content -LiteralPath (Join-Path $instance 'meteor-selftest.txt') -Value '' -Encoding utf8NoBOM
    & (Join-Path $JavaHome 'bin/java.exe') "@$argfile" *> (Join-Path $project 'release-run265.log')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($InteractiveUi) { return }
    $report = Get-Content -LiteralPath (Join-Path $instance 'meteor-selftest.txt') -Raw
    if ($report -match '(?m)^FAIL' -or $report -notmatch 'PASS smoke test completed') { throw "Smoke test did not pass: $report" }
    Write-Output $report
}
finally { Pop-Location }

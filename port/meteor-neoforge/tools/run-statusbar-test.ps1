param([string]$JavaHome = 'C:/Program Files/Zulu/zulu-21', [switch]$Sodium)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$baseline = Join-Path $project 'build/release-instance'
$instance = Join-Path $project ('build/statusbar-tests/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $instance,(Join-Path $instance 'mods') -Force | Out-Null
Set-Content -LiteralPath (Join-Path $instance '.statusbar-test') -Value 'isolated visual test'
Copy-Item -LiteralPath (Join-Path $project 'build/libs/meteor-client-neoforge-0.5.8-neoforge.9.jar') -Destination (Join-Path $instance 'mods/xiaohsense.jar')
if ($Sodium) {
    Get-ChildItem -LiteralPath (Join-Path $baseline 'mods') -Filter '*.jar' |
        Where-Object { $_.Name -match '^(sodium|iris)-' } |
        ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $instance 'mods') }
}
@('version:3955','onboardAccessibility:false','tutorialStep:none','pauseOnLostFocus:false','renderDistance:6','simulationDistance:5','maxFps:60','guiScale:2') |
    Set-Content -LiteralPath (Join-Path $instance 'options.txt') -Encoding utf8NoBOM
$arguments = @('"-Dmeteor.statusbar.selftest=true"') + @(Get-Content -LiteralPath (Join-Path $baseline 'launch-args.txt') | Where-Object { $_ -notmatch '^-?"?-Dmeteor\.' })
for ($i=0; $i -lt $arguments.Count; $i++) {
    if ($arguments[$i] -eq '"--gameDir"') { $arguments[$i+1] = '"' + $instance.Replace('\','/') + '"' }
}
$argfile = Join-Path $instance 'launch-args.txt'
$arguments | Set-Content -LiteralPath $argfile -Encoding utf8NoBOM
$process = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList ('"@' + $argfile + '"') -WorkingDirectory $instance -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $instance 'stdout.log') -RedirectStandardError (Join-Path $instance 'stderr.log')
$result = [pscustomobject]@{ pid=$process.Id; instance=$instance; sodium=$Sodium.IsPresent }
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $project 'build/latest-statusbar-test.json')
$result | ConvertTo-Json

param([Parameter(Mandatory)][string]$Jar)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Jar))
try {
    $names = @($zip.Entries.FullName)
    foreach ($required in @('META-INF/neoforge.mods.toml','META-INF/accesstransformer.cfg','meteor-client.mixins.json','meteor-client-baritone.mixins.json','meteor-client-init.txt','LICENSE-Meteor','META-INF/licenses/DEPENDENCIES.txt')) {
        if ($required -notin $names) { throw "Missing $required" }
    }
    if ($names | Where-Object { $_ -match '^net/fabricmc/|refmap|fabric\.mod\.json$|^org/slf4j/|^javax/annotation/' }) { throw 'Forbidden bundled loader, refmap, or platform-provided library' }
    $classCount = 0
    foreach ($entry in $zip.Entries) {
        if (!$entry.FullName.EndsWith('.class')) { continue }
        $classCount++
        $stream = $entry.Open()
        $buffer = [System.IO.MemoryStream]::new()
        try { $stream.CopyTo($buffer); $text = [System.Text.Encoding]::Latin1.GetString($buffer.ToArray()) }
        finally { $stream.Dispose(); $buffer.Dispose() }
        if ($text -match 'net/fabricmc/|net/minecraft/class_\d+') { throw "Unmapped/Fabric reference in $($entry.FullName)" }
    }
    foreach ($name in @('meteor-client.mixins.json','meteor-client-baritone.mixins.json')) {
        $reader = [System.IO.StreamReader]::new($zip.GetEntry($name).Open())
        try { $config = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($config.injectors.defaultRequire -ne 1 -or $config.refmap) { throw "Non-strict mixin config: $name" }
    }
    Write-Output "PASS: $classCount classes; metadata, strict mixins, init index and licenses present; no Fabric/intermediary/refmap/platform library copies"
    Get-FileHash -LiteralPath $Jar -Algorithm SHA256 | Format-List
} finally { $zip.Dispose() }

$ErrorActionPreference = 'Stop'
$uiProject = Split-Path $PSScriptRoot -Parent
$uiDependencies = Join-Path $uiProject 'build/ui-shader-check'
New-Item -ItemType Directory -Force -Path $uiDependencies | Out-Null
$uiClasspath = @()
foreach ($uiLibrary in @('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl')) {
    foreach ($uiClassifier in @($(if ($uiLibrary -eq 'lwjgl') { '-unsafe' } else { '' }), '-natives-windows')) {
        $uiArtifact = "$uiLibrary-3.4.1$uiClassifier.jar"
        $uiDestination = Join-Path $uiDependencies $uiArtifact
        if (-not (Test-Path -LiteralPath $uiDestination)) {
            Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/lwjgl/$uiLibrary/3.4.1/$uiArtifact" -OutFile $uiDestination
        }
        $uiClasspath += $uiDestination
    }
}
Push-Location $uiProject
try {
    & java --enable-native-access=ALL-UNNAMED -cp ($uiClasspath -join ';') tools/UiShaderCheck.java
    if ($LASTEXITCODE -ne 0) { throw "UI shader checks failed ($LASTEXITCODE)." }
} finally {
    Pop-Location
}

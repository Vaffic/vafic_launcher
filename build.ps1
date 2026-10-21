# Builds vafic-launcher.apk with the bare Android SDK tools (no Gradle).
#
# Requirements:
#   * JDK 17+ (JAVA_HOME, or javac on PATH)
#   * Android SDK platform 34 and build-tools 34+
#     (ANDROID_SDK_ROOT / ANDROID_HOME, or the default Windows SDK path)
#
# Optional:
#   VAFIC_KEYSTORE       path to a signing keystore
#   VAFIC_KEYSTORE_PASS  keystore/key password
# Without them a local debug keystore is created in .keystore/ and ignored by git.
$ErrorActionPreference = "Stop"

$proj = $PSScriptRoot

function Find-Jdk {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
        return $env:JAVA_HOME
    }
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javac) {
        return (Split-Path (Split-Path $javac.Source -Parent) -Parent)
    }
    throw "JDK not found. Set JAVA_HOME to a JDK 17+ installation."
}

function Find-Sdk {
    foreach ($candidate in @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME,
            (Join-Path $env:LOCALAPPDATA "Android\Sdk"))) {
        if ($candidate -and (Test-Path (Join-Path $candidate "platforms"))) {
            return $candidate
        }
    }
    throw "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME."
}

$jdk = Find-Jdk
$sdk = Find-Sdk
$bt = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $bt) { throw "Android build-tools are not installed." }
$androidJar = Join-Path $sdk "platforms\android-34\android.jar"
if (-not (Test-Path $androidJar)) { throw "Android platform 34 is not installed." }

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$($bt.FullName);$env:PATH"
$build = Join-Path $proj "build"

if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$build\obj", "$build\dex", "$build\gen" | Out-Null

Write-Host "1/5 link resources"
# aapt2 on Windows cannot open an asset or resource directory whose path
# contains Cyrillic characters. Stage both under the system temp directory
# (normally ASCII), and compile resources to a zip before linking. The manifest
# and output can stay in the project directory.
$assetStage = Join-Path $env:TEMP "vafic-build-assets"
$resStage = Join-Path $env:TEMP "vafic-build-res"
$resZip = Join-Path $env:TEMP "vafic-build-res.zip"
if (Test-Path $assetStage) { Remove-Item $assetStage -Recurse -Force }
if (Test-Path $resStage) { Remove-Item $resStage -Recurse -Force }
if (Test-Path $resZip) { Remove-Item $resZip -Force }
New-Item -ItemType Directory -Force -Path $assetStage | Out-Null
New-Item -ItemType Directory -Force -Path $resStage | Out-Null
$assets = Join-Path $proj "assets"
if (Test-Path $assets) {
    Copy-Item "$assets\*" $assetStage -Recurse -Force
}
$res = Join-Path $proj "res"
if (Test-Path $res) {
    Copy-Item "$res\*" $resStage -Recurse -Force
}
& (Join-Path $bt.FullName "aapt2.exe") compile --dir $resStage -o $resZip
$compileExit = $LASTEXITCODE
if ($compileExit -ne 0) { throw "aapt2 compile failed" }
& (Join-Path $bt.FullName "aapt2.exe") link -o "$build\base.apk" -I $androidJar `
        -A $assetStage --manifest (Join-Path $proj "AndroidManifest.xml") `
        --java "$build\gen" --auto-add-overlay $resZip
$linkExit = $LASTEXITCODE
Remove-Item $assetStage -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $resStage -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $resZip -Force -ErrorAction SilentlyContinue
if ($linkExit -ne 0) { throw "aapt2 link failed" }

Write-Host "2/5 javac"
$sources = @(Get-ChildItem (Join-Path $proj "src") -Recurse -Filter *.java |
        ForEach-Object { $_.FullName })
$sources += @(Get-ChildItem "$build\gen" -Recurse -Filter *.java -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName })
& (Join-Path $jdk "bin\javac.exe") -encoding UTF-8 -source 8 -target 8 -nowarn `
        -bootclasspath $androidJar -classpath $androidJar -d "$build\obj" $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "3/5 d8"
$classesJar = "$build\classes.jar"
& (Join-Path $jdk "bin\jar.exe") cf $classesJar -C "$build\obj" .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }
& (Join-Path $bt.FullName "d8.bat") --release --min-api 23 --lib $androidJar `
        --output "$build\dex" $classesJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "4/5 package"
Copy-Item "$build\base.apk" "$build\unaligned.apk" -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$build\unaligned.apk", 'Update')
[void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $zip, "$build\dex\classes.dex", "classes.dex",
        [System.IO.Compression.CompressionLevel]::NoCompression)
$zip.Dispose()

& (Join-Path $bt.FullName "zipalign.exe") -f 4 "$build\unaligned.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "5/5 sign"
$ks = $env:VAFIC_KEYSTORE
$ksPass = $env:VAFIC_KEYSTORE_PASS
if (-not $ks) { $ks = Join-Path $proj ".keystore\vafic.keystore" }
if (-not $ksPass) { $ksPass = "vafic123" }
if (-not (Test-Path $ks)) {
    $ksDir = Split-Path $ks -Parent
    if ($ksDir) { New-Item -ItemType Directory -Force -Path $ksDir | Out-Null }
    & (Join-Path $jdk "bin\keytool.exe") -genkeypair -keystore $ks -alias vafic `
            -keyalg RSA -keysize 2048 -validity 10000 -storepass $ksPass `
            -keypass $ksPass -dname "CN=Vafic Launcher, O=Vafic, C=RU"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}
& (Join-Path $jdk "bin\java.exe") -jar (Join-Path $bt.FullName "lib\apksigner.jar") sign `
        --ks $ks --ks-pass "pass:$ksPass" --key-pass "pass:$ksPass" `
        --out (Join-Path $proj "vafic-launcher.apk") "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "sign failed" }

Get-Item (Join-Path $proj "vafic-launcher.apk") | Select-Object FullName, Length


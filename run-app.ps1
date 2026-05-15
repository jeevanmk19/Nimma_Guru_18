# Nimma Guru — Fast Build/Install/Run Script (Windows)
# This script handles: checking ADB, building, installing, and launching the app

Write-Host "🔥 Nimma Guru — Fast Build & Run" -ForegroundColor Cyan
Write-Host ""

# Add ADB to PATH temporarily for this session
$adbPath = "C:\Android\Sdk\platform-tools"
if (Test-Path $adbPath) {
    $env:Path += ";$adbPath"
    Write-Host "✅ Added ADB to PATH: $adbPath" -ForegroundColor Green
}

# Step 1: Check if ADB is available
Write-Host "📱 Checking ADB connection..." -ForegroundColor Yellow
$adbOutput = adb devices 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: ADB not found or not in PATH!" -ForegroundColor Red
    Write-Host "   Please install Android Platform Tools and add them to your system PATH." -ForegroundColor Red
    Write-Host "   Download: https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Gray
    exit 1
}

# Step 2: Check for connected devices
$deviceLines = ($adbOutput -split "`n") | Where-Object { $_ -match "device$" -and $_ -notmatch "List of devices" }
if ($deviceLines.Count -eq 0) {
    Write-Host "❌ ERROR: No Android device connected via USB!" -ForegroundColor Red
    Write-Host "   Please:" -ForegroundColor Yellow
    Write-Host "   1. Enable USB Debugging in Developer Options on your phone" -ForegroundColor White
    Write-Host "   2. Connect your phone via USB cable" -ForegroundColor White
    Write-Host "   3. Allow USB Debugging prompt on your phone" -ForegroundColor White
    exit 1
}
Write-Host "✅ Device connected!" -ForegroundColor Green
Write-Host ""

# Step 3: Build the debug APK using Gradle
Write-Host "🔨 Building debug APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build successful!" -ForegroundColor Green
Write-Host ""

# Step 4: Install the APK on the connected device
Write-Host "📦 Installing APK on device..." -ForegroundColor Yellow
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Installation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Installation successful!" -ForegroundColor Green
Write-Host ""

# Step 5: Launch the app
Write-Host "🚀 Launching Nimma Guru..." -ForegroundColor Yellow
adb shell monkey -p com.nimmaguru.app -c android.intent.category.LAUNCHER 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Failed to launch app!" -ForegroundColor Red
    exit 1
}
Write-Host ""
Write-Host "🎉 Done! Nimma Guru is now running on your device!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Tip: For live logs, run:" -ForegroundColor Cyan
Write-Host "   adb logcat -s NimmaGuru:* *:E" -ForegroundColor White

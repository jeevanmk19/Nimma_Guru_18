# Nimma Guru — Fast Rerun Script (Windows)
# Skips full build checks for speed — great for iterative development!

Write-Host "⚡ Nimma Guru — Fast Rerun" -ForegroundColor Cyan
Write-Host ""

# Add ADB to PATH temporarily for this session
$adbPath = "C:\Android\Sdk\platform-tools"
if (Test-Path $adbPath) {
    $env:Path += ";$adbPath"
    Write-Host "✅ Added ADB to PATH: $adbPath" -ForegroundColor Green
    Write-Host ""
}

# Step 1: Build and install debug APK in one step
Write-Host "🔨 Building & installing..." -ForegroundColor Yellow
.\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Build/install failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Installed!" -ForegroundColor Green
Write-Host ""

# Step 2: Launch the app
Write-Host "🚀 Launching Nimma Guru..." -ForegroundColor Yellow
adb shell monkey -p com.nimmaguru.app -c android.intent.category.LAUNCHER 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Failed to launch app!" -ForegroundColor Red
    exit 1
}
Write-Host ""
Write-Host "🎉 Done! Nimma Guru is now running!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Tip: For live logs, run:" -ForegroundColor Cyan
Write-Host "   .\view-logs.ps1" -ForegroundColor White

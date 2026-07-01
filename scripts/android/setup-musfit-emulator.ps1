param(
    [string] $AvdName = "MusFit_API36",
    [string] $SystemImage = "system-images;android-36.1;google_apis;x86_64",
    [string] $Device = "medium_phone"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $repoRoot "scripts\android\android-env.ps1")

$sdkManager = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat"
$avdManager = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\avdmanager.bat"
$emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"

0..20 | ForEach-Object { "y" } | & $sdkManager --licenses | Out-Host
& $sdkManager $SystemImage | Out-Host

$existingAvds = & $emulator -list-avds
if ($existingAvds -contains $AvdName) {
    Write-Host "AVD already exists: $AvdName"
} else {
    "no" | & $avdManager create avd --force -n $AvdName -k $SystemImage -d $Device | Out-Host
    Write-Host "Created AVD: $AvdName"
}

Write-Host "Available AVDs:"
& $emulator -list-avds

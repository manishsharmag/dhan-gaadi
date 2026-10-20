$ErrorActionPreference = "Stop"
$root = Get-Location
$staging = Join-Path $root "client-staging"
$zipPath = Join-Path $root "client.zip"

Write-Host "Creating clean staging directory: $staging"
if (Test-Path $staging) {
    Remove-Item $staging -Recurse -Force
}
New-Item -ItemType Directory -Path $staging | Out-Null

Write-Host "Copying source and config files..."
Copy-Item -Path (Join-Path $root "client\src") -Destination (Join-Path $staging "src") -Recurse -Force
Copy-Item -Path (Join-Path $root "client\public") -Destination (Join-Path $staging "public") -Recurse -Force
Copy-Item -Path (Join-Path $root "client\package.json") -Destination (Join-Path $staging "package.json") -Force
Copy-Item -Path (Join-Path $root "client\package-lock.json") -Destination (Join-Path $staging "package-lock.json") -Force
Copy-Item -Path (Join-Path $root "client\next.config.mjs") -Destination (Join-Path $staging "next.config.mjs") -Force
Copy-Item -Path (Join-Path $root "client\eslint.config.mjs") -Destination (Join-Path $staging "eslint.config.mjs") -Force
Copy-Item -Path (Join-Path $root "client\jsconfig.json") -Destination (Join-Path $staging "jsconfig.json") -Force
Copy-Item -Path (Join-Path $root "client\.env.local") -Destination (Join-Path $staging ".env.local") -Force
Copy-Item -Path (Join-Path $root "client\.gitignore") -Destination (Join-Path $staging ".gitignore") -Force

if (Test-Path (Join-Path $root "client\README.md")) {
    Copy-Item -Path (Join-Path $root "client\README.md") -Destination (Join-Path $staging "README.md") -Force
}

Write-Host "Copying production build (.next excluding cache)..."
$destNext = Join-Path $staging ".next"
New-Item -ItemType Directory -Path $destNext | Out-Null

Get-ChildItem -Path (Join-Path $root "client\.next") -Exclude "cache" | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination (Join-Path $destNext $_.Name) -Recurse -Force
}

Write-Host "Writing deployment instructions..."
$readmeLines = @(
    "# Dhan-Gaadi Web Client Deployment Guide",
    "",
    "This package contains the complete production-ready Next.js client for Dhan-Gaadi (Haryana Roadways).",
    "It includes both source code and pre-compiled .next production bundles with all fixes.",
    "",
    "## Quick Deployment Steps",
    "",
    "### 1. Extract to Hostinger / VPS Website Directory",
    "Extract all files from this zip into your website web directory.",
    "",
    "### 2. Verify API URL (.env.local)",
    "Make sure .env.local points to your backend API URL:",
    "NEXT_PUBLIC_API_URL=https://backend.haryanabusinfo.in",
    "",
    "### 3. Install Dependencies & Start (PM2)",
    "npm install --production",
    "pm2 start npm --name 'dhan-gaadi-client' -- start",
    "pm2 save",
    "",
    "### 4. Alternative: Standalone Mode",
    "node .next/standalone/server.js"
)
$readmeLines | Set-Content -Path (Join-Path $staging "DEPLOY_INSTRUCTIONS.md") -Encoding UTF8

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Write-Host "Compressing into client.zip..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $zipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false)

Write-Host "Cleaning up staging directory..."
Remove-Item $staging -Recurse -Force

Write-Host "SUCCESS: client.zip created successfully!"
Get-Item $zipPath | Select-Object Name, Length, LastWriteTime

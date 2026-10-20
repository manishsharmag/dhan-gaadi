$ErrorActionPreference = "Stop"
$root = Get-Location
$staging = Join-Path $root "server-staging"
$zipPath = Join-Path $root "server-deploy.zip"

Write-Host "Creating clean staging directory: $staging"
if (Test-Path $staging) {
    Remove-Item $staging -Recurse -Force
}
New-Item -ItemType Directory -Path $staging | Out-Null

Write-Host "Copying server files..."
$includeDirs = @("controllers", "helpers", "middleware", "models", "public", "routes", "services", "utils", "validator")
foreach ($dir in $includeDirs) {
    $srcDir = Join-Path $root "server\$dir"
    if (Test-Path $srcDir) {
        Copy-Item -Path $srcDir -Destination (Join-Path $staging $dir) -Recurse -Force
    }
}

$includeFiles = @("package.json", "package-lock.json", "server.js", "seed-atlas.js", "seed.js", ".env.example", ".gitignore")
foreach ($file in $includeFiles) {
    $srcFile = Join-Path $root "server\$file"
    if (Test-Path $srcFile) {
        Copy-Item -Path $srcFile -Destination (Join-Path $staging $file) -Force
    }
}

if (Test-Path (Join-Path $root "server\.env")) {
    Copy-Item -Path (Join-Path $root "server\.env") -Destination (Join-Path $staging ".env") -Force
}

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Write-Host "Compressing into server-deploy.zip..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $zipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false)

Write-Host "Cleaning up staging directory..."
Remove-Item $staging -Recurse -Force

Write-Host "SUCCESS: server-deploy.zip created successfully!"
Get-Item $zipPath | Select-Object Name, Length, LastWriteTime

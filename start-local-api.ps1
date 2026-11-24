param([int]$Port = 8000)
$ErrorActionPreference = 'Stop'
if ($Port -lt 1024 -or $Port -gt 65535) { throw 'Choose a port from 1024 to 65535.' }
$projectRoot = $PSScriptRoot
$localRoot = Join-Path $projectRoot '.local-api'
$pythonExe = Join-Path $localRoot 'venv\Scripts\python.exe'
New-Item -ItemType Directory -Force -Path $localRoot | Out-Null
if (!(Test-Path -LiteralPath $pythonExe)) {
    if (Get-Command uv -ErrorAction SilentlyContinue) {
        & uv venv --python 3.11 (Join-Path $localRoot 'venv')
        if ($LASTEXITCODE -ne 0) { throw 'Could not create Python 3.11 environment.' }
    } else {
        & py -3.11 -m venv (Join-Path $localRoot 'venv')
        if ($LASTEXITCODE -ne 0) { throw 'Install Python 3.11 or uv, then retry.' }
    }
}
$requirementsPath = Join-Path $projectRoot 'backend\requirements.txt'
$requirementsHash = (Get-FileHash -LiteralPath $requirementsPath).Hash
$installedMarker = Join-Path $localRoot 'requirements.sha256'
if (!(Test-Path -LiteralPath $installedMarker) -or [IO.File]::ReadAllText($installedMarker).Trim() -ne $requirementsHash) {
    if (Get-Command uv -ErrorAction SilentlyContinue) { & uv pip install --python $pythonExe -r $requirementsPath }
    else { & $pythonExe -m pip install -r $requirementsPath }
    if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed. Run this script again to retry.' }
    [IO.File]::WriteAllText($installedMarker, $requirementsHash)
}
$secretPath = Join-Path $localRoot 'signing-secret.txt'
if (!(Test-Path -LiteralPath $secretPath)) {
    $secretBytes = New-Object byte[] 48
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($secretBytes) } finally { $generator.Dispose() }
    [IO.File]::WriteAllText($secretPath, [Convert]::ToBase64String($secretBytes))
}
$env:SECRET_KEY = [IO.File]::ReadAllText($secretPath).Trim()
$databasePath = (Join-Path $localRoot 'runtrack-dev.sqlite3').Replace('\', '/')
$env:DATABASE_URL = 'sqlite:///' + $databasePath
$env:DEBUG = 'False'
Push-Location (Join-Path $projectRoot 'backend')
try {
    & $pythonExe -c 'import app.models; from app.database import Base, engine; Base.metadata.create_all(bind=engine)'
    if ($LASTEXITCODE -ne 0) { throw 'Local DB initialization failed.' }
    Write-Host "RunTrack local development API: http://127.0.0.1:$Port/docs"
    Write-Host 'Android Emulator base URL: http://10.0.2.2:8000/ (adjust if port changed)'
    & $pythonExe -m uvicorn app.main:app --host 127.0.0.1 --port $Port
    if ($LASTEXITCODE -ne 0) { throw 'Local API stopped with an error.' }
} finally { Pop-Location }

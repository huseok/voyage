# Local dev: host gradlew bootJar + Dockerfile.fast (seconds, no Gradle in Docker).
# Usage:
#   .\scripts\docker-local-up.ps1
#   .\scripts\docker-local-up.ps1 -InDocker
#   .\scripts\docker-local-up.ps1 -NoBuild
param(
    [switch]$InDocker,
    [switch]$NoBuild,
    [switch]$ForceJar
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$env:DOCKER_BUILDKIT = "1"
$env:COMPOSE_DOCKER_CLI_BUILD = "1"

$envFile = Join-Path $Root ".env.docker.local"
if (-not (Test-Path $envFile)) {
    Write-Host "Missing .env.docker.local - copy from .env.docker.local.example" -ForegroundColor Yellow
    exit 1
}

function Get-LatestJar {
    $dir = Join-Path $Root "build\libs"
    if (-not (Test-Path $dir)) { return $null }
    return Get-ChildItem -Path $dir -Filter "*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-plain\.jar$' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Test-NeedHostBootJar {
    if ($ForceJar) { return $true }
    $jar = Get-LatestJar
    if (-not $jar) { return $true }
    $markers = @(
        (Join-Path $Root "build.gradle.kts"),
        (Join-Path $Root "settings.gradle.kts"),
        (Join-Path $Root "gradle.properties")
    )
    $latest = $jar.LastWriteTime
    foreach ($m in $markers) {
        if ((Test-Path $m) -and (Get-Item $m).LastWriteTime -gt $latest) { return $true }
    }
    $src = Get-ChildItem -Path (Join-Path $Root "src") -Recurse -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($src -and $src.LastWriteTime -gt $latest) { return $true }
    return $false
}

if ($InDocker) {
    $env:DOCKERFILE = "Dockerfile"
    Write-Host "In-Docker Gradle build (Dockerfile, slow) - use only without host JDK" -ForegroundColor Yellow
} else {
    $env:DOCKERFILE = "Dockerfile.fast"
    if (Test-NeedHostBootJar) {
        Write-Host "Host bootJar (uses ~/.gradle cache; slow on first run or after code changes)..." -ForegroundColor Cyan
        & .\gradlew.bat bootJar -x test --no-daemon
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } else {
        Write-Host "Reusing existing build\libs\*.jar, skip host compile" -ForegroundColor DarkGray
    }
    if (-not (Get-LatestJar)) {
        Write-Host "No JAR in build\libs - bootJar failed?" -ForegroundColor Red
        exit 1
    }
    Write-Host "Docker image: COPY JAR only (Dockerfile.fast)" -ForegroundColor Cyan
}

$composeArgs = @("compose", "--profile", "local-db", "--env-file", $envFile, "up", "-d")
if (-not $NoBuild) { $composeArgs += "--build" }

Write-Host "Running: docker $($composeArgs -join ' ')" -ForegroundColor DarkGray
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$healthUrl = "http://127.0.0.1:8080/api/v1/auth/captcha"
Write-Host "Waiting for API (Spring Boot may take ~60s after container start)..." -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 40; $i++) {
    try {
        $null = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
        $ready = $true
        break
    } catch {
        Start-Sleep -Seconds 3
    }
}
if ($ready) {
    Write-Host "API is ready: http://localhost:8080" -ForegroundColor Green
} else {
    Write-Host "Containers started but API not responding yet. Check: docker logs voyage-api --tail 80" -ForegroundColor Yellow
}

# 本地默认：本机 gradlew bootJar（走 Windows 缓存）+ Dockerfile.fast 打镜像（数秒）
# 用法：
#   .\scripts\docker-local-up.ps1              # 推荐
#   .\scripts\docker-local-up.ps1 -InDocker  # 强制在容器里 Gradle 编译（慢）
#   .\scripts\docker-local-up.ps1 -NoBuild   # 不重建镜像，只 up
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
    Write-Host "缺少 .env.docker.local，请复制 .env.docker.local.example" -ForegroundColor Yellow
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
    Write-Host "容器内编译（Dockerfile，较慢，仅无本机 JDK 或需对齐 CI 时用）" -ForegroundColor Yellow
} else {
    $env:DOCKERFILE = "Dockerfile.fast"
    if (Test-NeedHostBootJar) {
        Write-Host "本机编译 bootJar（Gradle 缓存位于用户目录 .gradle，仅首次或改代码后较慢）..." -ForegroundColor Cyan
        & .\gradlew.bat bootJar -x test --no-daemon
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } else {
        Write-Host "复用已有 build\libs\*.jar，跳过本机编译" -ForegroundColor DarkGray
    }
    if (-not (Get-LatestJar)) {
        Write-Host "未生成 JAR，请检查 bootJar 输出" -ForegroundColor Red
        exit 1
    }
    Write-Host "Docker 仅打包 JAR（Dockerfile.fast）" -ForegroundColor Cyan
}

$composeArgs = @("compose", "--profile", "local-db", "--env-file", $envFile)
if (-not $NoBuild) { $composeArgs += "--build" }
$composeArgs += @("up", "-d")

Write-Host "执行: docker $($composeArgs -join ' ')" -ForegroundColor DarkGray
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "已启动。API: http://localhost:8080" -ForegroundColor Green

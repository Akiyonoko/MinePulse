# 一键构建（Windows）：优先用仓库自带的 gradlew，找不到再退回系统 gradle。
# 需要 JDK 21（JAVA_HOME 指向它，或 java 在 PATH 里）。
$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    if (Test-Path ".\gradlew.bat") {
        & ".\gradlew.bat" --no-daemon --console=plain build
    } elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
        & gradle --no-daemon --console=plain build
    } else {
        throw "找不到 gradlew，也没有系统 gradle，请先安装 Gradle 8.8+"
    }

    if ($LASTEXITCODE -eq 0) {
        $jar = Get-ChildItem "$PSScriptRoot\build\libs\*.jar" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($jar) {
            Write-Host "[build] OK -> $($jar.Name) ($([math]::Round($jar.Length / 1024, 1)) KB)" -ForegroundColor Green
        } else {
            Write-Host "[build] OK（没找到产物 jar）" -ForegroundColor Yellow
        }
    } else {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

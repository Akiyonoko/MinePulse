#!/usr/bin/env bash
# 一键构建：优先用仓库自带的 gradlew，找不到再退回系统 gradle。
# 需要 JDK 21（JAVA_HOME 指向它，或 java 在 PATH 里）。
set -euo pipefail
cd "$(dirname "$0")"

if [ -x ./gradlew ]; then
  ./gradlew --no-daemon --console=plain build "$@"
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon --console=plain build "$@"
else
  echo "[build] 找不到 gradlew，也没有系统 gradle，请先安装 Gradle 8.8+" >&2
  exit 1
fi

echo "[build] OK"
ls -la build/libs/*.jar 2>/dev/null || true

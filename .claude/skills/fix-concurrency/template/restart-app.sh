#!/usr/bin/env bash
# 측정 대상 애플리케이션을 conc 프로파일로 재기동한다.
#
# 실행 (레포 루트 기준 전체 경로로 부른다. cd 하지 않는다):
#   bash .claude/skills/fix-concurrency/template/restart-app.sh
#
# 이 스크립트가 있는 이유:
#   기존 인스턴스를 내리지 않고 `./gradlew bootRun`을 다시 돌리면
#   `Port 8080 was already in use`로 새 프로세스만 죽고 **옛 코드가 계속 응답한다.**
#   그 상태로 Phase 7을 재면 후보를 적용하지 않은 채 "효과 없음"으로 오판한다.
#   포트를 비우는 것과 기동을 확인하는 것을 한 번에 묶어 그 실수를 막는다.
#
# 어느 디렉토리에서 실행해도 되도록 스스로 레포 루트로 이동한다.

set -uo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../../../.." && pwd)
cd "$REPO_ROOT"

APP_PORT=${APP_PORT:-8080}
MGMT_PORT=${MGMT_PORT:-8081}
POOL_MIN=${POOL_MIN:-500}          # Hikari minimum-idle. 0이면 풀 충전 대기를 건너뛴다
BOOT_LOG=${BOOT_LOG:-build/conc-boot.log}

mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot --default-character-set=utf8mb4 --init-command="SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci" uss_db "$@"; }

echo "레포 루트: $REPO_ROOT"
echo

echo "===== 1. $APP_PORT 점유 프로세스 종료 ====="
PIDS=$(lsof -ti:"$APP_PORT" || true)
if [ -n "$PIDS" ]; then
  echo "종료 대상 PID: $PIDS"
  echo "$PIDS" | xargs kill 2>/dev/null || true
else
  echo "점유 프로세스 없음"
fi

for i in $(seq 1 20); do
  [ -z "$(lsof -ti:"$APP_PORT" || true)" ] && { echo "포트 해제 확인 (${i}초)"; break; }
  sleep 1
done
if [ -n "$(lsof -ti:"$APP_PORT" || true)" ]; then
  echo "!! 20초 후에도 $APP_PORT 가 점유 중이다. 확인: lsof -i:$APP_PORT"
  exit 1
fi

echo; echo "===== 2. conc 프로파일로 기동 ====="
mkdir -p "$(dirname "$BOOT_LOG")"
nohup ./gradlew bootRun --args='--spring.profiles.active=conc' > "$BOOT_LOG" 2>&1 &
echo "bootRun 시작 (로그: $BOOT_LOG)"

echo; echo "===== 3. 기동 대기 (최대 120초) ====="
UP=no
for i in $(seq 1 60); do
  if curl -s "localhost:$MGMT_PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "UP 확인 ($((i * 2))초)"; UP=yes; break
  fi
  if grep -q "APPLICATION FAILED TO START\|BUILD FAILED" "$BOOT_LOG" 2>/dev/null; then
    echo "!! 기동 실패. 로그 마지막 20줄:"; tail -20 "$BOOT_LOG"; exit 1
  fi
  sleep 2
done

if [ "$UP" != "yes" ]; then
  echo "!! 120초 내에 기동되지 않았다. 로그 마지막 20줄:"; tail -20 "$BOOT_LOG"; exit 1
fi

if [ "$POOL_MIN" -gt 0 ]; then
  echo; echo "===== 4. 커넥션 풀 충전 대기 (Threads_connected >= $POOL_MIN) ====="
  # Hikari가 minimum-idle을 채우는 데 기동 후 수 초가 걸린다.
  # 덜 찬 상태로 부하를 주면 커넥션 생성 지연이 락 대기에 섞인다.
  for i in $(seq 1 30); do
    TC=$(mysqlc -N -B -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null | awk '{print $2}')
    echo "  Threads_connected=$TC"
    [ -n "$TC" ] && [ "$TC" -ge "$POOL_MIN" ] && { echo "풀 충전 완료"; break; }
    sleep 2
  done
fi

echo; echo "재기동 완료."

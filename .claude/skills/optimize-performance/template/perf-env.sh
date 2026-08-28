#!/usr/bin/env bash
# 측정 셸 환경. 레포 루트에서, 새 터미널마다 한 번 source한다.
# 실행(bash perf-env.sh)이 아니라 source여야 함수와 변수가 현재 셸에 남는다. bash, zsh 모두 된다.
#
#   source .claude/skills/optimize-performance/template/perf-env.sh {이슈번호} {슬러그}
#
# 정의하는 것
#   PERF_DIR          이슈 디렉토리   .claude/resources/perf/{이슈번호}
#   TARGET_DIR        대상 디렉토리   $PERF_DIR/{슬러그}
#   SEEDS             시드 모듈 디렉토리
#   PERF_JWT_SECRET   application-perf.yml의 security.jwt.secret-key (토큰 발급에 쓴다)
#   mysqlp            uss_db 접속 함수.  mysqlp -e "SELECT 1;"  /  mysqlp < file.sql

if [ $# -lt 2 ]; then
    echo "사용법: source .claude/skills/optimize-performance/template/perf-env.sh {이슈번호} {슬러그}" >&2
    return 1 2>/dev/null || exit 1
fi

export PERF_DIR=.claude/resources/perf/$1
export TARGET_DIR=$PERF_DIR/$2
export SEEDS=.claude/skills/optimize-performance/template/seeds

# 주석 줄을 건너뛰고 키 값만 집는다. 파일은 Phase 2가 만든다. 그 전에는 비어 있어도 된다.
PERF_JWT_SECRET=$(grep -E '^[[:space:]]*secret-key:' src/main/resources/application-perf.yml 2>/dev/null | awk '{print $2}')
export PERF_JWT_SECRET
if [ -z "$PERF_JWT_SECRET" ]; then
    echo "경고: application-perf.yml의 secret-key를 읽지 못했다. Phase 2에서 파일을 만든 뒤 다시 source하라." >&2
fi

# uss_db 접속. 네 요소 모두 필요하다.
#   docker exec                     호스트에 mysql 클라이언트가 없다
#   함수                            zsh는 따옴표 없는 변수 확장에 단어 분리를 하지 않아 "$MYSQL -e" 형식이 깨진다
#   --default-character-set=utf8mb4 없으면 latin1로 붙어 쿼리 안의 한글 리터럴이 ?가 되고 결과가 조용히 틀린다
#   --init-command=SET NAMES ...    charset만 맞추면 collation이 서버 기본(utf8mb4_0900_ai_ci)으로 남아
#                                   utf8mb4_unicode_ci인 컬럼과 사용자 변수 비교가 ERROR 1267로 죽는다
mysqlp() {
    docker exec -i -e MYSQL_PWD=root uss-mysql \
        mysql -u root \
            --default-character-set=utf8mb4 \
            --init-command="SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci" \
            uss_db "$@"
}

echo "PERF_DIR=$PERF_DIR  TARGET_DIR=$TARGET_DIR  mysqlp 정의됨"

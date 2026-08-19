#!/usr/bin/env bash
# 측정용 JWT를 서명키로 직접 만든다.
#
# 왜 로그인 API를 쓰지 않는가:
#   시드(seeds/member.sql)가 넣는 비밀번호는 BCrypt 해시가 아니라 로그인 API를 탈 수 없고,
#   수백 개 계정을 가입 API로 만드는 것보다 시드 id에 맞춰 직접 서명하는 편이 측정 준비에 맞다.
#   토큰은 JwtProvider와 같은 방식으로 직접 서명해서 만든다.
#
# JwtProvider가 만드는 회원 토큰의 형태 (auth/infra/JwtProvider.java):
#   - 알고리즘 HS256, 키는 secret-key 문자열의 UTF-8 바이트 그대로
#   - subject = memberId (문자열)
#   - role 클레임 없음 (있으면 관리자 토큰으로 간주되어 JwtAuthenticationFilter가 거절한다)
#
# 사용:
#   bash mint-tokens.sh --secret {서명키} --start {회원 id 시작값} --count {개수} --out {경로}
#
# 예:
#   bash .claude/skills/fix-concurrency/template/mint-tokens.sh \
#     --secret conc-only-local-secret-key-not-for-any-real-environment \
#     --start 900001 --count 500 \
#     --out .claude/resources/concurrency/90/tokens.json
#
# 출력 형태 (k6 템플릿이 이 형태를 읽는다):
#   [{ "memberId": 900001, "accessToken": "eyJ..." }, ...]

set -euo pipefail

SECRET=""
START=""
COUNT=""
OUT=""
TTL_SECONDS=86400

while [ $# -gt 0 ]; do
    case "$1" in
        --secret) SECRET="$2"; shift 2 ;;
        --start)  START="$2";  shift 2 ;;
        --count)  COUNT="$2";  shift 2 ;;
        --out)    OUT="$2";    shift 2 ;;
        --ttl)    TTL_SECONDS="$2"; shift 2 ;;
        *) echo "알 수 없는 인자: $1" >&2; exit 1 ;;
    esac
done

for required in SECRET START COUNT OUT; do
    if [ -z "${!required}" ]; then
        echo "필수 인자 누락: --$(echo "$required" | tr '[:upper:]' '[:lower:]')" >&2
        exit 1
    fi
done

# HS256은 키가 32바이트 이상이어야 한다. 짧으면 애플리케이션이 토큰을 거절한다.
if [ "${#SECRET}" -lt 32 ]; then
    echo "서명키가 ${#SECRET}바이트다. HS256은 32바이트 이상이 필요하다." >&2
    exit 1
fi

# base64url: 표준 base64에서 +/ 를 -_ 로 바꾸고 패딩(=)을 뗀다.
b64url() {
    openssl base64 -A | tr '+/' '-_' | tr -d '='
}

NOW=$(date +%s)
EXP=$((NOW + TTL_SECONDS))

HEADER=$(printf '{"alg":"HS256"}' | b64url)

{
    printf '[\n'
    for i in $(seq 0 $((COUNT - 1))); do
        MEMBER_ID=$((START + i))

        PAYLOAD=$(printf '{"sub":"%s","iat":%s,"exp":%s}' "$MEMBER_ID" "$NOW" "$EXP" | b64url)
        SIGNING_INPUT="${HEADER}.${PAYLOAD}"
        SIGNATURE=$(printf '%s' "$SIGNING_INPUT" \
            | openssl dgst -sha256 -hmac "$SECRET" -binary \
            | b64url)

        SEPARATOR=","
        if [ "$i" -eq $((COUNT - 1)) ]; then
            SEPARATOR=""
        fi

        printf '  {"memberId": %s, "accessToken": "%s.%s"}%s\n' \
            "$MEMBER_ID" "$SIGNING_INPUT" "$SIGNATURE" "$SEPARATOR"
    done
    printf ']\n'
} > "$OUT"

echo "토큰 ${COUNT}건 생성: $OUT (회원 id ${START} ~ $((START + COUNT - 1)), 만료 $(date -r "$EXP" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -d "@$EXP" '+%Y-%m-%d %H:%M:%S'))"
echo "다음: 토큰 1건으로 인증이 통하는지 확인하라 (Phase 3의 6번)"

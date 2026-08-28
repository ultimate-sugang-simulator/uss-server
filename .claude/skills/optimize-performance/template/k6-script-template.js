// 부하 테스트 스크립트 템플릿.
// {…} 자리를 채워 .claude/resources/perf/{이슈번호}/{슬러그}/test-script.js 로 저장한다.
// 실행 명령은 template/commands.md에 있다. 실행은 호출자가 한다.
//
// ── 작성 규칙 ──────────────────────────────────────────────
// 1. 이 파일을 복사해 고친다. 고치는 자리는 상수 블록(TARGET, ENDPOINT, CONDITION, USER_COUNT 기본값),
//    measure 시나리오의 VU와 duration, default 함수의 요청 한 줄, check의 세 번째 항목뿐이다.
// 2. PHASE 분기를 유지한다. warmup과 measure를 한 프로세스에서 같이 돌리지 않는다.
// 3. 토큰은 init 컨텍스트에서 이슈 디렉토리의 tokens.json을 읽는다. 형태는 [{memberId, accessToken}] (mint-tokens.sh 출력).
//    setup()에서 로그인을 호출하지 마라. 로그인 SQL과 응답시간이 측정에 섞인다. '../tokens.json' 경로를 바꾸지 마라.
// 4. 인증 헤더는 access-token 하나다. JwtAuthenticationFilter가 읽는 유일한 헤더이며 Bearer 형식이 아니다.
// 5. 토큰 수가 USER_COUNT와 다르면 중단한다. 일부만 발급된 채로 측정하지 않는다.
// 6. 토큰은 exec.scenario.iterationInTest로 고른다. __VU로 고르면 VU 수만큼만 쓰여 user_count와 실제가 어긋난다.
// 7. thresholds에 응답시간 임계를 넣지 않는다. 판정은 스킬이 전후 비교로 한다.
//    summaryTrendStats는 지우지 않는다. 지우면 p(99)가 요약에서 사라진다.
// 8. check에 실제 데이터가 실렸는지 확인하는 항목을 하나 넣는다. r.json()은 parseBody로 감싼다.
//    비JSON 응답(502 HTML, 빈 본문)의 예외를 check 실패로 떨어뜨리기 위해서다.
//    본문 없는 쓰기 응답(201/204)이면 status만 검증하고 나머지 check를 지운 사실을 record.md에 남긴다.
// 9. 경로 변수와 쿼리 값은 iterationInTest나 __ITER로 흩는다. 고정값은 한 행만 반복 조회해 버퍼 풀에 완전히 올라간 상태를 잰다.
// 10. CONDITION에는 Phase 3-B에서 확정한 값을 적는다. 요약 파일만 보고 어떤 측정인지 알 수 있어야 한다.
//     duration은 ramp 구간과 유지 구간을 분리한다. 캐시는 항상 warm이다.
//
// measure 실행의 요청은 전부 대상 API다. 요청당 쿼리 수의 분모는 요약의 requests를 그대로 쓴다.
//
// 요청 한 줄의 형태:
//   GET               http.get(`${BASE_URL}/api/v1/courses/major`, params)
//   GET + 쿼리        http.get(`${BASE_URL}/api/v1/courses/search?keyword=${KEYWORDS[__ITER % KEYWORDS.length]}`, params)
//   POST 경로변수     http.post(`${BASE_URL}/api/v1/carts/${1 + (exec.scenario.iterationInTest % COURSE_COUNT)}`, null, params)
//   POST + 바디       http.post(`${BASE_URL}/api/v1/...`, JSON.stringify({…}),
//                       { headers: { ...params.headers, 'Content-Type': 'application/json' } })
// ──────────────────────────────────────────────────────────

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || {USER_COUNT});
const PHASE = __ENV.PHASE || 'measure';

const TARGET = '{슬러그}';
const ENDPOINT = '{HTTP} {경로}';
const CONDITION = {
    vus: {VU},
    steady_state_duration: '{duration}',
    ramp_up: '30s',
    ramp_down: '30s',
    total_duration: '{ramp_up + duration + ramp_down}',
    db_cache: 'warm',
    app_cache: '없음',
    user_count: USER_COUNT,
};

// 형태: [{ "memberId": 900001, "accessToken": "..." }, ...]
const tokens = JSON.parse(open('../tokens.json'));

if (tokens.length !== USER_COUNT) {
    throw new Error(`토큰 ${tokens.length}건 / 필요 ${USER_COUNT}건. mint-tokens.sh의 --count와 USER_COUNT를 맞춰라.`);
}

const scenarios = {
    warmup: {
        executor: 'constant-vus',
        vus: 5,
        duration: '30s',
    },
    measure: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '30s', target: {VU} },
            { duration: '{duration}', target: {VU} },
            { duration: '30s', target: 0 },
        ],
    },
};

export const options = {
    scenarios: { [PHASE]: scenarios[PHASE] },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        checks: ['rate>0.99'],
    },
};

// 비JSON 응답에서 예외를 던지지 않는다. 파싱 실패는 null로 떨어뜨려 check 실패로 드러낸다.
function parseBody(res) {
    if (res.status !== 200 || res.body === null || res.body.length <= 2) {
        return null;
    }
    try {
        return res.json();
    } catch (e) {
        return null;
    }
}

export default function () {
    const token = tokens[exec.scenario.iterationInTest % tokens.length];
    const params = { headers: { 'access-token': token.accessToken } };

    const res = http.get(`${BASE_URL}{대상 엔드포인트}`, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'body is not empty': (r) => r.body !== null && r.body.length > 2,
        '{대표 필드}가 실려 있다': (r) => {
            const body = parseBody(r);
            return body !== null && {데이터검증식};
        },
    });
}

export function handleSummary(data) {
    const m = data.metrics || {};
    const val = (name, key) => (m[name] && m[name].values[key] !== undefined ? m[name].values[key] : null);
    const trend = (name) => ({
        med: val(name, 'med'),
        p95: val(name, 'p(95)'),
        p99: val(name, 'p(99)'),
        max: val(name, 'max'),
    });

    const summary = {
        phase: PHASE,
        target: TARGET,
        endpoint: ENDPOINT,
        condition: CONDITION,
        requests: val('http_reqs', 'count'),
        rps: val('http_reqs', 'rate'),
        failed_rate: val('http_req_failed', 'rate'),
        checks_rate: val('checks', 'rate'),
        checks: ((data.root_group || {}).checks || []).map((c) => ({
            name: c.name,
            passes: c.passes,
            fails: c.fails,
        })),
        duration_ms: trend('http_req_duration'),
        waiting_ms: trend('http_req_waiting'),
        bytes_received: val('data_received', 'count'),
    };

    // 반올림은 터미널 한 줄에만 쓴다. summary의 값은 손대지 않는다.
    const num = (x, d) => (typeof x === 'number' ? x.toFixed(d) : '-');
    const line = [
        `[${PHASE}] ${TARGET}`,
        `요청 ${summary.requests}건`,
        `p95 ${num(summary.duration_ms.p95, 1)}ms`,
        `p99 ${num(summary.duration_ms.p99, 1)}ms`,
        `실패율 ${num(summary.failed_rate * 100, 2)}%`,
        `check ${num(summary.checks_rate * 100, 2)}%`,
    ].join(' / ');

    const out = { stdout: `\n${line}\n\n` };
    if (__ENV.SUMMARY_OUT) {
        out[__ENV.SUMMARY_OUT] = JSON.stringify(summary, null, 2);
    }
    return out;
}

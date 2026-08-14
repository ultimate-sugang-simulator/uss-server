// 동시성 폭발 부하 스크립트 템플릿.
// {…} 자리를 채워 .claude/resources/concurrency/{이슈번호}/{슬러그}/burst-script.js 로 저장한다.
//
// 실행:
//   k6 run -e SUMMARY_OUT=$TARGET_DIR/k6-burst-summary-{n}.json $TARGET_DIR/burst-script.js
//
// {n}은 후보 번호다. 0 = 아무 제어도 없는 원본, n = 후보 n을 단독 적용한 상태.
// 실행은 호출자가 한다. 스킬은 명령어만 제시한다.
//
// ── 작성 규칙 ──────────────────────────────────────────────
// 1. 이 파일을 복사해 고친다. 빈 파일에서 새로 쓰지 않는다.
// 2. 고치는 자리는 상단 상수 블록(TARGET, ENDPOINT, TARGET_COURSE_ID, CAPACITY, VU_COUNT),
//    default 함수의 요청 한 줄, EXPECTED_REJECT_CODE뿐이다.
// 3. **executor는 per-vu-iterations, iterations: 1을 유지한다.**
//    VU마다 요청을 정확히 한 번 보내 "N명이 동시에 버튼을 누른다"를 만든다.
//    ramping-vus로 바꾸지 마라. 램프를 두면 경합이 시간축으로 흩어져 결함이 재현되지 않는다.
//    gracefulStop을 늘리지 마라. 요청이 끝나기를 기다리는 시간일 뿐 경합과 무관하다.
// 4. **토큰은 __VU로 고른다.** VU와 회원이 1대1로 묶여야 중복 신청이 섞이지 않는다.
//    optimize-performance 템플릿의 exec.scenario.iterationInTest 방식을 가져오지 마라.
//    그 방식은 VU 수와 무관하게 토큰을 순환시키므로 여기서는 같은 회원이 두 번 신청하게 된다.
// 5. **토큰 수와 VU 수가 다르면 중단한다.** 하나라도 어긋나면 위반 건수가 왜곡된다.
// 6. 이 서버는 access-token 헤더 하나만 요구한다. Authorization: Bearer 형식이 아니고
//    refresh 토큰도 없다. 헤더 이름을 바꾸지 마라.
//    (근거: JwtAuthenticationFilter가 access-token 헤더만 읽는다)
// 7. **thresholds를 두지 마라.** 실패율이 높은 것이 정상인 측정이다.
//    정원 100에 VU 500이면 400건은 반드시 거절된다. threshold를 걸면 k6가 실패로 끝낸다.
// 8. 응답 코드와 에러 코드를 모두 센다. 정합성이 지켜져도 거절이 500으로 나가면 결함이다.
//    EXPECTED_REJECT_CODE에는 이 대상에서 정원 초과 시 기대하는 에러 코드를 적는다.
//    (수강신청이면 COURSE_MAX_CAPACITY_EXCEEDED = 4000, 상태 코드는 400)
// 9. **워밍업 시나리오를 만들지 마라.** 폭발은 한 번만 터뜨린다.
//    워밍업이 정원을 채워버리면 본 측정에서 전 요청이 거절된다.
// 10. 되돌리기는 스크립트가 하지 않는다. Phase 4, 7의 명령 블록이 부하 직전에 SQL로 처리한다.
//     setup()이나 teardown()에 DB 정리를 넣지 마라. k6는 DB에 접속하지 않는다.
// ──────────────────────────────────────────────────────────

import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 이 측정이 무엇이었는지 요약 파일만 보고 알 수 있게 한다.
const TARGET = '{슬러그}';
const ENDPOINT = '{HTTP} {경로}';
const TARGET_COURSE_ID = {대상 강의 id};
const CAPACITY = {허용 상한};
const VU_COUNT = {VU 수};

// 정원 초과 시 기대하는 에러 코드. 이 코드가 아닌 실패는 전부 unexpected로 샌다.
const EXPECTED_REJECT_CODE = {기대 에러 코드};

const tokens = JSON.parse(open('../tokens.json'));

if (tokens.length !== VU_COUNT) {
    throw new Error(
        `토큰 ${tokens.length}건 / 필요 ${VU_COUNT}건. VU와 회원이 1대1로 묶이지 않는다. ` +
        'mint-tokens.sh의 --count와 시드 회원 수를 VU 수에 맞추고 다시 만들어라.'
    );
}

const succeeded = new Counter('req_succeeded');
const rejectedAsExpected = new Counter('req_rejected_expected');
const rejectedOther4xx = new Counter('req_rejected_other_4xx');
const failed5xx = new Counter('req_failed_5xx');
const unparsable = new Counter('req_unparsable');

export const options = {
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: VU_COUNT,
            iterations: 1,
            // 전 VU가 요청 하나씩만 보내므로 길게 잡을 이유가 없다.
            // 여기에 걸리면 경합이 아니라 락 대기 타임아웃을 재고 있는 것이다.
            maxDuration: '60s',
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    // thresholds 없음. 거절이 대량으로 나는 것이 정상이다. (작성 규칙 7)
};

// 비JSON 응답에서 예외를 던지지 않는다. 파싱 실패는 null로 떨어뜨려 카운터로 드러낸다.
function errorCodeOf(res) {
    if (res.body === null || res.body.length <= 2) {
        return null;
    }
    try {
        const body = res.json();
        return body && body.code !== undefined ? body.code : null;
    } catch (e) {
        return null;
    }
}

export default function () {
    // VU 번호로 회원을 고정한다. __VU는 1부터 시작한다.
    const token = tokens[__VU - 1];

    // 이 서버는 access-token 헤더 하나만 요구한다.
    const params = {
        headers: { 'access-token': token.accessToken },
    };

    const res = http.post(`${BASE_URL}{대상 경로}`, null, params);

    if (res.status >= 200 && res.status < 300) {
        succeeded.add(1);
        return;
    }

    if (res.status >= 500) {
        failed5xx.add(1);
        return;
    }

    const code = errorCodeOf(res);

    if (code === null) {
        unparsable.add(1);
        return;
    }

    if (code === EXPECTED_REJECT_CODE) {
        rejectedAsExpected.add(1);
        return;
    }

    rejectedOther4xx.add(1);
}

export function handleSummary(data) {
    const m = data.metrics || {};
    const val = (name, key) => (m[name] && m[name].values[key] !== undefined ? m[name].values[key] : null);
    const count = (name) => val(name, 'count') || 0;
    const trend = (name) => ({
        med: val(name, 'med'),
        p95: val(name, 'p(95)'),
        p99: val(name, 'p(99)'),
        max: val(name, 'max'),
    });

    const summary = {
        target: TARGET,
        endpoint: ENDPOINT,
        condition: {
            executor: 'per-vu-iterations (iterations=1, ramp 없음)',
            vus: VU_COUNT,
            target_course_id: TARGET_COURSE_ID,
            capacity: CAPACITY,
            expected_reject_code: EXPECTED_REJECT_CODE,
            db_cache: 'warm (InnoDB 버퍼 풀은 재기동 없이 비울 수 없다)',
            app_cache: '없음',
        },
        requests: val('http_reqs', 'count'),
        rps: val('http_reqs', 'rate'),
        // 응답 분포. 정합성 판정은 DB의 불변식 검증으로 하고, 이 값은 응답 품질 판정에 쓴다.
        outcome: {
            succeeded: count('req_succeeded'),
            rejected_expected: count('req_rejected_expected'),
            rejected_other_4xx: count('req_rejected_other_4xx'),
            failed_5xx: count('req_failed_5xx'),
            unparsable: count('req_unparsable'),
        },
        // 성공 수가 허용 상한과 같아야 한다. 크면 상한을 넘긴 것이다.
        succeeded_over_capacity: count('req_succeeded') - CAPACITY,
        duration_ms: trend('http_req_duration'),
        waiting_ms: trend('http_req_waiting'),
    };

    // 아래 반올림은 터미널 한 줄 출력에만 쓴다. summary 객체의 값은 손대지 않는다.
    const num = (x, d) => (typeof x === 'number' ? x.toFixed(d) : '-');
    const o = summary.outcome;
    const line = [
        `[burst] ${TARGET}`,
        `요청 ${summary.requests}건`,
        `성공 ${o.succeeded} (정원 ${CAPACITY}, 초과 ${summary.succeeded_over_capacity})`,
        `기대거절 ${o.rejected_expected}`,
        `기타4xx ${o.rejected_other_4xx}`,
        `5xx ${o.failed_5xx}`,
        `p99 ${num(summary.duration_ms.p99, 1)}ms`,
    ].join(' / ');

    const out = { stdout: `\n${line}\n\n` };

    if (__ENV.SUMMARY_OUT) {
        out[__ENV.SUMMARY_OUT] = JSON.stringify(summary, null, 2);
    }

    return out;
}

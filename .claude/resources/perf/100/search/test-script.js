// PERF-100 / GET /api/v1/courses/search 부하 스크립트.
// 실행 명령은 .claude/skills/optimize-performance/template/commands.md에 있다.
//
// 템플릿과 다른 곳: KEYWORDS 풀. 시드의 검색 제목 설계(접두 40 x 접미 25)에서 고른 조합어 10개를 __ITER로 돌린다.
// 이 엔드포인트는 컨트롤러에 @Auth가 없고 필터가 DB를 보지 않으므로 회원 시드 없이 토큰만 있으면 된다.

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 50);
const PHASE = __ENV.PHASE || 'measure';

const TARGET = 'search';
const ENDPOINT = 'GET /api/v1/courses/search';
const CONDITION = {
    vus: 30,
    steady_state_duration: '1m',
    ramp_up: '30s',
    ramp_down: '30s',
    total_duration: '2m',
    db_cache: 'warm (InnoDB 버퍼 풀은 재기동 없이 비울 수 없다)',
    app_cache: '없음',
    user_count: USER_COUNT,
    course_rows: 26439,
    keyword_match_rows: '조합당 24건 (시드 24,000 / 1,000 조합)',
};

// 시드의 접두 40개 x 접미 25개 조합에서 고르게 흩어 뽑았다.
// ngram BOOLEAN MODE는 구(phrase) 검색이라 조합어 하나가 그 조합 행만 매칭한다.
// 한 키워드만 반복하면 같은 행만 조회해 버퍼 풀에 완전히 올라간 상태를 재게 되므로 순회한다.
const KEYWORDS = [
    '컴퓨터공학',
    '기계설계',
    '전자시스템',
    '화학실험',
    '경영분석',
    '미디어데이터',
    '바이오실험',
    '로봇제어',
    '금융최적화',
    '스포츠계측',
];

// tokens.json은 이슈 디렉토리에 있다(대상 간 공유).
// 형태: [{ "memberId": 900001, "accessToken": "..." }, ...]
const tokens = JSON.parse(open('../tokens.json'));

if (tokens.length !== USER_COUNT) {
    throw new Error(
        `토큰 ${tokens.length}건 / 필요 ${USER_COUNT}건. mint-tokens.sh의 --count와 USER_COUNT를 맞춰라.`
    );
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
            { duration: '30s', target: 30 },
            { duration: '1m', target: 30 },
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
    const iter = exec.scenario.iterationInTest;
    const token = tokens[iter % tokens.length];
    const keyword = KEYWORDS[iter % KEYWORDS.length];

    const params = {
        headers: {
            'access-token': token.accessToken,
        },
    };

    const res = http.get(
        `${BASE_URL}/api/v1/courses/search?keyword=${encodeURIComponent(keyword)}`,
        params
    );

    check(res, {
        'status is 200': (r) => r.status === 200,
        'body is not empty': (r) => r.body !== null && r.body.length > 2,
        'searchedCourseResponses가 실려 있다': (r) => {
            const body = parseBody(r);
            return (
                body !== null &&
                Array.isArray(body.searchedCourseResponses) &&
                body.searchedCourseResponses.length > 0
            );
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
        keywords: KEYWORDS,
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

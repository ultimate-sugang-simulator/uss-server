// PERF-110 general-education 부하 스크립트. 템플릿: .claude/skills/optimize-performance/template/k6-script-template.js
// 실행 명령은 template/commands.md에 있다. 실행은 호출자가 한다.

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 1000);
const PHASE = __ENV.PHASE || 'measure';

const TARGET = 'general-education';
const ENDPOINT = 'GET /api/v1/courses/general-education?course-area={영역}';
const CONDITION = {
    vus: 30,
    steady_state_duration: '1m',
    ramp_up: '30s',
    ramp_down: '30s',
    total_duration: '2m',
    db_cache: 'warm',
    app_cache: '없음',
    user_count: USER_COUNT,
    area_rotation: '교양 13개 영역 순환 (성능 시드 5개 3,015~3,249건 + 앱 시드만 8개 8~56건)',
};

// 교양 영역 13종 전부를 순환한다. 앞 5개는 성능 시드가 채운 큰 영역, 뒤 8개는 앱 시드만 있는 작은 영역.
const AREAS = [
    'ACADEMIC_FOUNDATION',
    'BASIC_SCIENCE_ENGINEERING',
    'CORE_HUMANITIES',
    'CORE_SOCIAL',
    'CORE_INU_SEMINAR',
    'HUMANITIES',
    'ARTS_SPORTS',
    'SOCIAL',
    'SCIENCE_TECHNOLOGY',
    'FOREIGN_LANGUAGE',
    'CORE_FOREIGN_LANGUAGE',
    'CORE_SCIENCE_TECHNOLOGY',
    'CORE_ARTS_SPORTS',
];

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
    const iteration = exec.scenario.iterationInTest;
    const token = tokens[iteration % tokens.length];
    const params = { headers: { 'access-token': token.accessToken } };
    const area = AREAS[iteration % AREAS.length];

    const res = http.get(`${BASE_URL}/api/v1/courses/general-education?course-area=${area}`, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'body is not empty': (r) => r.body !== null && r.body.length > 2,
        'generalEducationCourseResponses가 실려 있다': (r) => {
            const body = parseBody(r);
            return body !== null
                && Array.isArray(body.generalEducationCourseResponses)
                && body.generalEducationCourseResponses.length > 0;
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

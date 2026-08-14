# 동시성 개선 기록 (Concurrency)

`fix-concurrency` 스킬이 생성하는 **재현, 검증, 채택 기록**을 보관하는 디렉토리다.

이슈 하나당 디렉토리 하나, 대상 임계 구역 하나당 그 아래 하위 디렉토리 하나를 쓴다.
시드와 토큰은 이슈 전체가 공유하고, 나머지는 전부 대상별로 독립이다.

```
{이슈번호}/
├── seeds.sql                       # 변수 블록 + 모듈 SOURCE (이슈 공용)
├── tokens.json                     # 측정용 JWT (이슈 공용, gitignore 대상)
└── {대상-슬러그}/
    ├── record.md                   # 이 대상의 재현, 검증, 채택 기록 + 진행 상태
    ├── invariant-check.sql         # 불변식 검증 SQL (Phase 1에서 확정, 이후 고정)
    ├── burst-script.js             # k6 폭발 부하 스크립트
    ├── invariant-{n}.txt           # 불변식 검증 결과 (원본)
    ├── k6-burst-summary-{n}.json   # k6 요약
    └── lock-stats-{n}.txt          # 락 지표 BEFORE/AFTER 스냅샷 (원본)
```

후보 3개를 비교한 대상 디렉토리는 이렇게 된다.

```
invariant-0.txt  k6-burst-summary-0.json  lock-stats-0.txt   ← 원본 (결함이 재현되는 상태)
invariant-1.txt  k6-burst-summary-1.json  lock-stats-1.txt   ← 후보 1 단독 적용
invariant-2.txt  k6-burst-summary-2.json  lock-stats-2.txt   ← 후보 2 단독 적용
invariant-3.txt  k6-burst-summary-3.json  lock-stats-3.txt   ← 후보 3 단독 적용
invariant-final.txt                                          ← 채택안 되살린 뒤 최종 확인
```

## `{n}`은 후보 번호다

`.claude/resources/perf/`의 `{n}`(누적 상태 번호)과 **뜻이 다르다.**
동시성 제어 기법은 서로 배타적이라 누적되지 않는다. 후보 2는 후보 1 위에 얹은 것이 아니라,
후보 1을 되돌리고 원본 위에 단독으로 올린 것이다.

**따라서 모든 후보의 비교 대상은 항상 `-0`이다.** `-{n-1}`이 아니다.

## optimize-performance와 섞지 마라

| | `perf/` | `concurrency/` |
|---|---|---|
| 판정 기준 | 응답시간, 처리량 | 불변식 위반 건수 |
| 성공 | 빨라지면 성공 | 느려져도 정합하면 성공 |
| `{n}` | 누적 상태 | 후보 번호 |
| 프로파일 | `application-perf.yml` | `application-conc.yml` |
| 커넥션 풀 | 측정 안정성 우선(작게 고정) | **VU 이상**(락 경합을 재기 위해) |

산출물 규약(각 파일을 만드는 Phase, 측정 산출물의 형태, 상태 저장소)은
`.claude/skills/fix-concurrency/SKILL.md`의 **산출물 규약**이 단일 출처다.

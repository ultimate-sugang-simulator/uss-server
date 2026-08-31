# [PERF-116] GET /api/v1/carts

> 이슈: #116
> 브랜치: refactor/116-carts-perf
> 대상 디렉토리: `.claude/resources/perf/116/carts/`

이 파일은 대상 엔드포인트 하나만 다룬다. 같은 이슈의 다른 엔드포인트는 각자의 디렉토리에 각자의 `record.md`를 가진다.

## 진행 상태

> ⏳ 미완 / ✅ 완료 / ⏭️ 건너뜀. 재진입 시 ⏳로 표기된 가장 이른 Phase부터 재개한다.

**준비 (대상당 1회)**

| 1. 대상 | 2. 환경 | 3. 조건 | 4. 기준선 |
|---|---|---|---|
| ✅ | ✅ | ✅ | ✅ |

**사이클 (반복)**

| # | 기법 | 5. 설계 | 6. 스냅샷 | 7. 적용 | 8. 검증 |
|---|---|---|---|---|---|
| 1 | 담기 수 비정규화 카운터 (`courses.cart_count`) + 원자적 UPDATE | ✅ | ✅ | ✅ | ✅ |

**재개 메모**: 완료. 사이클 1로 종료했다. 남은 위험 신호(트랜잭션 제어문이 요청당 왕복의 83%)는 전역 설정 문제라 이 대상의 사이클로 다루지 않았다. 쓰기 경로의 카운터 UPDATE 비용은 `POST /carts/{courseId}`를 별도 대상으로 재야 드러난다. 아래는 최초 진입 시의 메모다 - 같은 엔드포인트를 #106에서 이미 한 번 측정했다(`.claude/resources/perf/106/carts/record.md`). #106은 "이 규모에서 DB 쿼리 병목 없음"으로 사이클 없이 종료했고, 판정에 반영하지 않은 위험 신호 두 개를 남겼다. (1) 요청당 왕복 7번 중 5번이 트랜잭션 제어문, (2) 집계 쿼리의 읽은 행/반환 행 205. 이번 이슈는 그 상태에서 이어진다. #106 시드(`.claude/resources/perf/106/seeds.sql`)와 데이터가 DB에 남아 있는지 Phase 3-A에서 확인한다.

## 대상

- 엔드포인트: `GET /api/v1/carts`
- 실행 경로: `CartController.getCartedCourse` → `CartService.getCartedCourse` → `CartRepository.findByMemberId`, `CartRepository.countCartedCoursesByCourseId`
- 인증: `@Auth` 있음 → 회원 시드 필요
- 예상 쿼리 목록 (요청 1회 기준)
  1. `CartRepository.findByMemberId` - `carts`를 `member_id`로 필터, `courses`를 inner join fetch, `course_schedules`를 left join fetch, `created_at` 정렬. 장바구니, 강의, 시간표를 한 쿼리로 읽는다
  2. `CartRepository.countCartedCoursesByCourseId` - `carts`를 `course_id IN (...)`으로 필터해 `course_id`별 COUNT. 장바구니가 비어 있으면 `CartService`의 조기 반환으로 실행되지 않는다 (빈 장바구니 1쿼리, 아니면 2쿼리)
  3. 지연 로딩 후보 없음 - `Course.schedules`는 `@BatchSize(size = 1000)`이 걸려 있으나 fetch join이 채워 발동하지 않고, `Course.department`는 `@Enumerated` 컬럼이라 조인이 없으며, `Cart.member`는 조회 경로에서 접근하지 않는다
  4. SQL은 아니지만 digest 통계에 함께 잡히는 것 - 트랜잭션 제어문 (`SET autocommit` 2회, `SET SESSION TRANSACTION READ ONLY`, `COMMIT`, `READ WRITE`). #106 기준 요청당 5건

## 측정 환경

> 값이 바뀌면 그 사실을 사이클 기록에 남긴다.

| 항목 | 값 |
|---|---|
| 프로파일 | perf (`application-perf.yml`) |
| DB | MySQL 8.0 / InnoDB (`uss-mysql`, `127.0.0.1:3307`, `uss_db`) |
| 커넥션 풀 크기 | 10 |
| InnoDB 버퍼 풀 크기 | 128 MiB |
| 데이터 규모 | `carts` 68,182 (전부 성능 시드), `members` 11,247 (id 900001~911247), `courses` 26,439 (앱 2,439 + #104 시드 24,000), `course_schedules` 79,819 (강의당 3) |
| 규모 근거 | #106과 같은 값을 그대로 쓴다. 이 이슈는 #106이 남긴 위험 신호를 이어받으므로 기준선이 #106과 직접 비교되어야 한다. `carts`는 실제 추정치를 재현한 것이다. 학년별 인원(4학년 3,430 / 3학년 2,733 / 2학년 2,520 / 1학년 2,564)과 평균 장바구니 수(4 / 6 / 9 / 6)가 실제 추정이고, 교양/전공 구성은 학년별 교육과정 비율(4학년 2:6, 3학년 2:8, 2학년 3:7, 1학년 5:3)을 담기 수에 적용해 반올림했다(4학년 1+3, 3학년 1+5, 2학년 3+6, 1학년 4+2). 전교생이 수강신청 직전에 담기를 채운 상태를 재는 것이 목적이다. `courses`, `course_schedules`는 #104 규모(운영 대비 약 10배)를 그대로 쓴다. 이 대상은 PK와 `course_id`로만 닿아 규모를 따로 키울 이유가 없다 |
| 카디널리티 | `carts.member_id` 11,247종, 회원당 4~9건 (학년별 고정). `carts.course_id` 554종에 68,182건이 몰린다 - 교양 250개 강의에 27~241건(평균 95.9), 전공 304개 강의에 42~370건(평균 145.4). 담기가 퍼지는 강의 수를 교양은 학년당 100개, 전공은 회원 학과와 학년당 15개로 제한한 결과이며, 전학년 강의는 네 학년 풀에 공통이라 가장 몰린다. `carts.created_at`은 회원 안에서 전부 다르다(분 단위). 회원은 8개 학과에 균등(각 약 1,406명), 학과 안에서 학년 구간 순 |
| 부하 조건 | VU 30 (풀 10의 3배, #106과 동일), ramp-up 30s + 유지 1m + ramp-down 30s = 총 2m, USER_COUNT 11247 (시드 회원 전원을 iterationInTest 순환으로 고르게 호출). #106과 같은 값을 쓴 이유는 기준선을 직접 비교하기 위해서다. VU가 풀보다 크므로 응답시간에는 커넥션 대기가 섞인다 |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고, Redis 캐시는 워밍업이 채운다. 매 측정 전 같은 워밍업으로 맞춘다 |
| 되돌리기 절차 | 불필요 (읽기 엔드포인트) |
| 시드 | `../seeds.sql` + `member.sql` + `cart-by-grade.sql` (`course.sql`은 #104 시드를 재사용해 미실행, `enrollment.sql`은 학년별 담기 수를 못 만들어 미사용). 변수: `@member_count` 11247, 학년 구간 2564 / 2520 / 2733 / 3430, 교양+전공 담기 4+2 / 3+6 / 1+5 / 1+3, `@ge_pool` 100, `@major_pool` 15. Phase 3-A 적재 후 `ANALYZE TABLE` 실행 |
| 토큰 | `../tokens.json` (`mint-tokens.sh`, 회원 id 900001~911247, 11,247개). Phase 4에서 발급 |

## 기준선 (Baseline)

| 지표 | 값 |
|---|---|
| p95 | 113.788 ms (med 51.4005, max 1227.054) |
| p99 | 233.371 ms |
| RPS | 398.42 (요청 47,812건 / 2m) |
| 에러율 | 0 |
| check 통과율 | 100% (3개 항목 모두 47,812 / 0) |
| 요청당 쿼리 수 | SQL 1.99 (집계 0.9977 + 조회 0.9956), 트랜잭션 제어 4.96 (`SET autocommit` 1.9815, `READ ONLY` 0.9945, `READ WRITE` 0.9945, `COMMIT` 0.9941) |
| heap 최대 / heap max | 266.8 MB / 4096.0 MB |
| GC 일시정지 합 / 최장 | 311.0 ms / 28.0 ms |
| GC overhead | 0.001 |
| HikariCP pending 최대 / acquire max | 20 (평균 11.2) / 1008.0 ms |
| 커넥션 보유 평균 | 22.5 ms |
| blocked 스레드 최대 | 0 |
| process CPU 최대 | 0.194 (평균 0.127), system CPU 최대 0.890 (평균 0.785) |
| 캐시 적중률 (구획 없으면 `-`) | - (측정 구간 접근 없음) |

### 쿼리 통계 (total_ms 상위)

> 전체: `query-stats-summary-0.md` / k6 요약: `k6-test-summary-0.json`. 진단 근거로 쓴 행만 옮긴다.

| 요청당 | mean_ms | total_ms | 비중 | 읽은행/반환행 | 출처 |
|---|---|---|---|---|---|
| 0.9977 | 0.945745 | 45112.076913 | 42.6% | 204.0763 (호출당 6.09행 반환, 약 1,242행 읽음) | `CartRepository.countCartedCoursesByCourseId` |
| 0.9956 | 0.712025 | 33893.110021 | 32.0% | 1.9998 (호출당 18.28행 반환) | `CartRepository.findByMemberId` |
| 4.96 (합) | 약 0.11 | 26885.30 (합) | 25.4% (합) | - | 트랜잭션 제어 (`SET autocommit` ×2, `READ ONLY`, `READ WRITE`, `COMMIT`) |

### 진단

- 병목 성격: DB 시간이 쏠린 곳과 응답시간이 걸리는 곳이 다르다. DB 시간의 최대 항목은 `countCartedCoursesByCourseId`(42.6%, 읽은행/반환행 204.08)이지만, 응답시간을 지배하는 것은 커넥션 대기다. 집계 쿼리는 개선 여지가 있는 유일한 하드웨어 독립 항목이므로 사이클 1의 대상으로 삼는다.
- 근거: 요청당 DB 실행 시간 합 2.21 ms(105,890.5 ms / 47,812건)에 대해 커넥션 보유 평균은 22.5 ms다. 커넥션 점유의 90%가 쿼리 실행이 아니다. 처리량도 이 값으로 설명된다 - 풀 10 / 보유 22.5 ms = 444 RPS, 실측 398.42 RPS. p95 113.788 ms는 HikariCP pending 20 상시, acquire max 1008 ms가 만든 대기다. VU 30이 풀 10을 기다리는 부하 조건의 결과이며 대상 API의 결함이 아니다. 집계 쿼리의 읽은행/반환행 204.08은 인덱스 부재가 아니라 구조다 - `idx_course_id`가 이미 있고 InnoDB 보조 인덱스 리프에 PK가 있어 `COUNT(id)`가 테이블에 닿지 않는다. 강의당 담기 수(전공 평균 145.4, 교양 평균 95.9)만큼 인덱스 엔트리를 세야 숫자 하나가 나오므로, 규모와 인기도에 정비례해 커진다.
- 예상 쿼리 목록과 어긋난 지점: 없음. 지연 로딩 쿼리 없음. 리포지토리 호출 표에서 두 메서드가 각각 요청당 1.000으로 digest의 `calls`와 일치한다.
- #106 대비: 하드웨어 독립 지표는 사실상 동일하다(집계 읽은행/반환행 204.08 대 205.19, 조회 2.00 대 2.00, 요청당 쿼리 수 동일). 하드웨어 의존 지표만 나빠졌다(RPS 398.42 대 705.07, p95 113.788 대 56.968, 커넥션 보유 평균 22.5 대 14.2 ms). 코드와 데이터가 같고 요청당 DB 실행 시간만 2.21 ms로 늘어난 것으로 보아 호스트 부하 차이다. 전후 비교는 하드웨어 독립 지표로 한다.

---

## 사이클 1: 담기 수 비정규화 카운터

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 카운터 위치 | `courses`에 컬럼 추가 | `findByMemberId`가 이미 `courses`를 조인하므로 담기 수가 그 조인에 얹혀 따라온다. 집계 쿼리와 왕복이 함께 사라진다. `current_enrollment`와 같은 자리라 선례가 일관된다 |
| 컬럼 | `cart_count INT NOT NULL DEFAULT 0` | `current_enrollment`와 같은 형태 |
| 필드 | `@Column(nullable = false, name = "cart_count") private int cartCount;` | `domain.md`. 필드명은 `common.md`의 "다른 엔티티에서 온 값은 출처를 밝힌다"에 해당 |
| 초기값 | 팩토리에서 `INITIAL_CART_COUNT = 0` | `INITIAL_ENROLLMENT` 선례 |
| 갱신 수단 | `CourseRepository`의 `@Modifying` 쿼리 | JPA에서 원자적 갱신을 표현할 수단이 이것뿐이다. `CartService`가 이미 `CourseRepository`를 주입받고 있어 의존이 늘지 않는다 |
| 증가 쿼리 조건절 | 없음 (`WHERE c.id = :id`) | 담기 수에는 정원 같은 상한 규칙이 없다. 걸 술어가 없다 |
| 감소 쿼리 조건절 | `AND c.cartCount > 0` | `decreaseEnrollmentAboveZero` 선례. 음수 방지 가드이며, 0행이 카운터 어긋남의 신호가 된다 |
| 어노테이션 | `@Modifying(flushAutomatically = true)`, `clearAutomatically` 끔 | #90 기록: 영속성 컨텍스트를 비우면 앞서 조회한 `member`, `course`가 준영속이 되어 `Cart.create(member, course)`가 준영속 참조를 든다. `addCart`가 정확히 같은 구조다 |
| 증가 쪽 영향 행 수 | 검사하지 않음 (반환 없음) | 조건절이 없어 0행이 나오려면 강의가 없어야 하는데 `findByIdWithSchedules`(`CartService.java:63`)가 바로 앞에서 존재를 확정한다 |
| 감소 쪽 0행 처리 | `NO_AFFECTED_ROW` 상수 + `CARTED_COURSE_DELETE_CONFLICT(CONFLICT, 3005, "장바구니 삭제를 반영할 수 없습니다. 다시 확인해주세요.")`로 예외, 삭제까지 롤백 | `RegistrationService.decreaseEnrollment`(`:109-115`)와 `REGISTRATION_CANCEL_CONFLICT(CONFLICT, 4004)`를 그대로 따랐다. 재시도나 자동 보정은 이번 범위에서 다루지 않는다 |
| 발행 순서 | 카운터 UPDATE → `cartRepository.save()` / `delete()` | #90 Phase 4에서 데드락 497건의 원인이 INSERT(S) → UPDATE(X) 업그레이드였다. `carts.course_id`가 `courses(id)`를 FK로 참조하므로 `carts` INSERT가 부모 행에 S 락을 건다. 같은 형태다. 선택지가 아니라 성립 조건 |
| 조회 경로 정리 | `CartCount`, `CartService.getCartCountByCourseId`, `CartRepository.countCartedCoursesByCourseId` 제거. `CartedCourseResponse.of(course)`가 `course.getCartCount()`를 읽는다 | 이 조회 경로 외에 참조가 없다 |
| 마이그레이션 | `V1_13__add_cart_count_to_courses.sql` (컬럼 추가 + 기존 데이터 backfill) | 최신이 V1_12. backfill을 빼면 기존 68,182건이 전부 0으로 시작해 실제와 어긋난 값이 서비스된다 |
| 동기화 경로 | 고려하지 않음 | `CourseSyncApplier` 관련 로직이 폐기 예정이라고 호출자가 확인 |

- 검토했지만 택하지 않은 안
  - **더티체킹** - 읽고 고치고 쓰는 구조라 같은 강의에 동시 담기가 오면 갱신 유실이다. #90이 `course.incrementEnrollment()`를 신청 경로에서 걷어낸 것과 같은 이유다. 막으려면 `@Version`이나 비관적 락을 얹어야 해 원자적 UPDATE보다 비싸다. `deleteCartedCourse`는 지금 `Cart`만 읽고 지우는데(`CartService.java:82-85`) `Cart.course`가 LAZY라 강의를 깨우는 SELECT가 삭제 경로에 하나 늘어난다
  - **이벤트 발행 + 리스너** - `AFTER_COMMIT`으로 받으면 별도 트랜잭션이라 갱신이 실패해도 롤백할 것이 없어 값이 영구히 어긋나고, 발행 순서를 잡을 수단도 없다. 같은 트랜잭션에서 받으면 같은 UPDATE에 같은 왕복이고 간접층만 는다. `CartService`가 이미 `CourseRepository`를 들고 있어 떼어낼 결합이 없다
  - **별도 테이블** (`course_cart_counts` 등) - 쓰기를 `courses`에서 떼어내 락을 분리하지만, 조회에 조인이나 쿼리가 하나 더 필요해 왕복이 줄지 않는다. 이번 개선의 이득 중 큰 쪽을 잃는다
  - **Redis 캐싱, 주기적 재계산** - 계산을 없애지 못하고 빈도만 줄인다. 무효화 설계가 필요하고 담기 수가 실시간이 아니게 된다
- 호출자가 예상한 효과
  - 요청당 SQL 2 → 1, 왕복 7 → 6. 집계 쿼리가 사라지므로 읽은행/반환행 204.08 항목 자체가 없어진다
  - 쓰기 경로에 UPDATE 1건이 늘고 인기 강의 행에 X 락이 몰린다. #90이 같은 기법에서 RPS 하락과 p99 상승을 겪었으므로 담기 수에도 나타날 것이다. 담기에는 정원 같은 조건절이 없어 모든 담기가 예외 없이 X 락을 잡는다. 이 비용은 `POST /carts/{courseId}`를 별도 대상으로 재야 드러난다

### 개선 전 지표

| 지표 | 값 |
|---|---|
| p95 / p99 | 113.788 ms / 233.371 ms |
| RPS | 398.42 (에러율 0, check 100%) |
| 요청당 쿼리 수 | SQL 1.99 + 트랜잭션 제어 4.96 (왕복 약 7) |
| 대상 쿼리 calls / mean_ms / total_ms | 47,700 / 0.945745 / 45112.076913 (비중 42.60%) |
| 읽은 행 / 반환 행 (`examined_per_sent`) | 204.0763 (호출당 6.0857행 반환, 약 1,242행 읽음) |
| 쿼리 전체 소요 (EXPLAIN ANALYZE) | Q1 집계 0.478 ms / Q2 조회 0.162 ms |
| GC 일시정지 합 / HikariCP pending 최대 | 311.0 ms / 20 |
| 요청당 리포지토리 호출 수 / 할당량 | 2.000 (`countCartedCoursesByCourseId` 1.000 + `findByMemberId` 1.000) / 0.322 MB |

**실행계획**

> 원본: `query-plan-0.txt` (개선 후는 `query-plan-1.txt`)

- 캡처한 쿼리: 두 개. 이번 사이클이 집계 쿼리(Q1)를 없애고 그 값을 조회 쿼리(Q2)의 조인에 얹는 변경이라, 개선 후에는 비교할 Q1이 존재하지 않는다. 같은 자리에서 "Q1 소멸 + Q2가 얼마나 무거워졌는가"를 보려면 둘 다 필요하다
- EXPLAIN 파라미터: 회원 907548 (JUNIOR), 담긴 강의 `1000033, 1000160, 1000178, 1000265, 1000323, 1002585`. 이후 모든 사이클에서 동일하게 쓴다
- 값 선정 근거: 흔한 값 기준으로 잡았다. 학년별 평균 읽는 행을 인원으로 가중하면 1,244.9행인데 부하 측정의 실측이 호출당 1,242.1행이라 시드가 의도대로 재현됐다. 학년별로는 SENIOR 802.0 / FRESHMAN 1,077.8 / JUNIOR 1,317.7 / SOPHOMORE 1,939.6이고, 전체 평균에 가장 가까운 JUNIOR(+5.8%)를 골랐다. 담기 수도 중앙값인 6이다. 그 학년 안에서 읽는 행이 1,318로 JUNIOR 평균(1,317.7)과 사실상 일치하는 회원을 뽑았다. 한쪽 끝(SOPHOMORE +55.9%, SENIOR −35.6%)으로 잡으면 계획이 뒤집힐 수 있어 피했다
- 쿼리 전체 소요: Q1 0.478 ms, Q2 0.162 ms
- 비용 상위 노드 (`소요 ms`가 자식을 포함하므로 자기 몫으로 분해)
  - Q1: 커버링 인덱스 range scan 0.296 ms(61.9%) / Filter 0.113 ms(23.6%) / Group aggregate 0.069 ms(14.4%)
  - Q2: `c1_0` 인덱스 조회 0.0705 ms(43.5%) / `s1_0` 6 loops 0.0339 ms(20.9%) / Sort 0.0264 ms(16.3%) / `c2_0` 6 loops 0.0229 ms(14.1%) / Nested loop 노드 2개 0.0083 ms(5.1%)
- 해석: **Q1의 비용은 집계가 아니라 1,318엔트리 읽기에 있다.** COUNT를 누적하는 일 자체는 14.4%이고, 85.5%는 엔트리를 읽고 술어를 거는 앞단이 쓴다. 이 구분이 기법 선택과 직결된다 - 비싼 것이 계산이 아니라 읽기이므로 해법이 "다르게 센다"가 아니라 "세지 않고 들고 있는다"가 된다. **Q2는 이 규모에서 문제 삼을 노드가 없다.**
- 접근 방식: Q1은 range / `idx_course_id`, `using_index: true`로 커버링이라 테이블 데이터에 닿지 않는다. 풀스캔 없음(`Handler_read_rnd_next` 0), filesort 없음. Q2는 `carts`를 `uk_member_course`로 ref, `courses`를 PRIMARY로 eq_ref, `course_schedules`를 `idx_course_id`로 ref
- 실측 rows 대 반환 행 수: Q1 1,318 읽어 6 반환(219.7:1). Q2 18행 반환
- 옵티마이저 추정 대 실측: Q1 스캔 추정 1318 = 실측 1318로 일치해 `ANALYZE TABLE`이 최신이다. Q1 루트의 추정 627 대 실측 6은 집계 결과 행 수 추정치라 접근 방식 판단에 쓰이지 않는다. Q2는 전 노드에서 괴리 없음
- 카운터: Q1 `Handler_read_key` 6(= `IN` 목록의 강의 수) / `Handler_read_next` 1,318 / `Handler_read_rnd_next` 0 / `Sort_rows` 0. Q2 `Handler_read_key` 13 / `Handler_read_next` 24 / `Sort_rows` 6 / `Sort_scan` 1 (`ORDER BY created_at`이 `uk_member_course(member_id, course_id)` 순서와 어긋나 filesort가 붙는다. 6행이라 비용은 작다)
- 캡처상 유의점
  - Q2가 파일에 두 번 들어 있다(`tee -a`). 첫 캡처 0.78 ms, 두 번째 0.162 ms로 4.8배 차이가 났고 계획과 카운터는 동일했다. warm 고정 원칙에 따라 두 번째 값을 기준으로 삼았다. 0.02 ms 단위의 노드별 귀속은 이 변동폭 안에 묻히므로 Q2의 분해는 근거로 쓰지 않는다. Q1의 1,318은 시간이 아니라 개수이고 `Handler_read_next`가 독립적으로 같은 값을 확인해 준다
  - Q2의 SELECT 목록은 digest 원문이 949자에서 잘려 있어 JPQL(`CartRepository.java:15-23`)에서 복원했다. 조인, WHERE, ORDER BY는 원문 그대로라 계획에 영향을 주는 부분은 동일하다
  - 절대 시간으로는 두 쿼리 모두 빠르다. Q1 0.478 ms는 인덱스(약 1.5 MB)가 버퍼 풀 128 MiB에 통째로 올라가 있고 커버링이라 테이블에 닿지 않아서다. 엔트리당 약 225 ns로 InnoDB 핸들러 호출 오버헤드를 감안하면 정상 범위다. 이번 개선이 겨누는 것은 절대 시간이 아니라 담기 수에 정비례해 커지는 읽기 축이다

- 락 대기 현황: 캡처하지 않았다. 이번 측정 대상은 읽기 전용이라 지금 뜨면 빈 결과다. 카운터 UPDATE의 락 경합은 `addCart`, `deleteCartedCourse`에 붙으므로 `POST /carts/{courseId}`를 별도 대상으로 잴 때 캡처한다

### 적용 내용

| 파일 | 변경 |
|---|---|
| `database/migration/V1_13__add_cart_count_to_courses.sql` | 신규. `courses`에 `cart_count INT NOT NULL DEFAULT 0` 추가 + 기존 `carts` 행을 세어 backfill |
| `course/domain/Course.java` | `cartCount` 필드(`@Column(nullable = false, name = "cart_count")`), `INITIAL_CART_COUNT` 상수, 팩토리에서 0으로 초기화 |
| `course/repository/CourseRepository.java` | `increaseCartCount(id)` (조건절 없음, 반환 없음), `decreaseCartCountAboveZero(id)` (`AND c.cartCount > 0`, 영향 행 수 `int` 반환). 둘 다 `@Modifying(flushAutomatically = true)`, `clearAutomatically` 끔 |
| `global/exception/domain/ExceptionCode.java` | `CARTED_COURSE_DELETE_CONFLICT(CONFLICT, 3005, "장바구니 삭제를 반영할 수 없습니다. 다시 확인해주세요.")` 추가 |
| `cart/repository/CartRepository.java` | `countCartedCoursesByCourseId` 제거 |
| `cart/dto/common/CartCount.java` | 삭제 (다른 참조 없음) |
| `cart/service/CartService.java` | `getCartedCourse`에서 집계 제거. `addCart`에 증가 UPDATE를 `save()` **앞에**, `deleteCartedCourse`에 감소 UPDATE를 `delete()` **앞에** 발행. `NO_AFFECTED_ROW` 상수 추가, `getCartCountByCourseId`, `extractCourseIds`, `DEFAULT_CART_COUNT` 제거 |
| `cart/dto/response/CartedCourseResponse.java` | `of(course, cartCount)` → `of(course)`. `course.getCartCount()`를 읽는다 |
| `cart/controller/CartControllerDocs.java` | DELETE에 409(3005) 응답 문서 추가 |
| `cart/fixture/CartFixture.java` (테스트) | `createCart`가 강의의 `cartCount`도 올린다. 담기를 리포지토리로 직접 심는 테스트가 운영 쓰기 경로와 같은 불변식을 지키게 한다 |
| `course/dto/response/CourseResponseTest.java` (테스트) | 시그니처 변경 반영. `cartCount`를 3으로 세팅 후 `of(course)` |
| `cart/service/CartServiceTest.java` (테스트) | `이미_담은_강의가_폐강돼도_삭제할_수_있다`에 `entityManager.refresh(course)` 추가 (아래 발견 사항) |

- `getCartedCourse`의 빈 장바구니 조기 반환을 없앴다. 집계 쿼리가 사라져 분기할 이유가 없어졌고, 이제 장바구니가 비어 있든 아니든 항상 1쿼리다
- 적용 확인
  - Flyway: `1.13 add cart count to courses` success=1
  - backfill: 실제 담기 행 수와 `cart_count`가 어긋난 강의 0건, `sum(cart_count)` 68,182 = `carts` 전체 행 수
  - EXPLAIN: `carts`를 `uk_member_course`로 ref(6행, `Using index`), `courses`를 PRIMARY로 eq_ref(1행). `cart_count`가 이미 읽던 `courses` 행에서 나오므로 조인이나 쿼리가 늘지 않았다
- 테스트: `./gradlew test` 293개 전량 통과 (1차 실행에서 1건 실패 → 아래 발견 사항 처리 후 통과)

**발견 사항: 더티 체킹이 원자적 증가를 덮어쓴다**

1차 테스트 실행에서 `이미_담은_강의가_폐강돼도_삭제할_수_있다`가 실패했다. 기전은 이렇다.

1. `addCart`의 `increaseCartCount`는 벌크 UPDATE라 DB의 `cart_count`만 1로 올린다. 영속성 컨텍스트의 `Course` 엔티티는 0인 채 남는다
2. 테스트가 같은 트랜잭션에서 `course.close()` 후 `courseRepository.save(course)`를 호출한다
3. `Course`에 `@DynamicUpdate`가 없어 더티 체킹 UPDATE가 **모든 컬럼**을 쓴다. 낡은 `cart_count = 0`이 방금 올린 값을 덮는다
4. `deleteCartedCourse`의 `decreaseCartCountAboveZero`가 0행 → `CARTED_COURSE_DELETE_CONFLICT`

#90 기록의 "더티 체킹 UPDATE가 방금 원자적으로 올린 값을 낡은 절대값으로 덮어쓴다"가 그대로 재현된 것이다.

운영 경로에는 이 조합이 없다. `addCart`는 `course`를 읽고 증가시키지만 이후 변경하지 않고(`Cart.create(member, course)`는 참조만 든다), `deleteCartedCourse`는 `Course`를 엔티티로 로드하지 않으며(`Cart.course`가 LAZY이고 접근하지 않는다), `CourseSyncApplier`는 별도 트랜잭션에서 새로 읽고 그 안에서 카운터를 건드리지 않는다. 실패한 것은 한 트랜잭션을 공유하는 통합 테스트(`@IntegrationTest`가 `@Transactional`)뿐이라 테스트에 `entityManager.refresh(course)`를 넣어 해소했다.

**다만 지뢰는 남는다.** 앞으로 담기나 빼기와 같은 트랜잭션에서 `Course` 엔티티를 수정하는 코드가 생기면 `cart_count`가 조용히 리셋된다. `current_enrollment`도 같은 조건에 놓여 있다.

### 개선 후 지표

| 구분 | 지표 | 개선 전 | 개선 후 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 113.788 ms | 73.391 ms | −35.5% |
| | p99 | 233.371 ms | 128.149 ms | −45.1% |
| | RPS | 398.42 | 625.85 | +57.1% |
| | 쿼리 전체 소요 | Q1 0.478 + Q2 0.162 ms | Q2 0.161 ms | Q1 소멸 |
| | heap 최대 | 266.8 MB | 306.9 MB | +15.0% |
| | GC 일시정지 합 / 최장 | 311.0 / 28.0 ms | 257.0 / 15.0 ms | −17.4% / −46.4% |
| | HikariCP pending 최대 | 20 | 20 | 동일 |
| | 커넥션 보유 평균 | 22.5 ms | 14.2 ms | −36.9% |
| | process CPU 최대 | 0.194 | 0.213 | +9.8% |
| 하드웨어 독립 | 요청당 쿼리 수 | SQL 1.9933 + 제어 4.9646 | SQL 0.9931 + 제어 4.9580 | SQL −1.0 |
| | 요청당 리포지토리 호출 수 | 2.000 | 0.999 | −1.0 |
| | 요청당 할당량 | 0.322 MB | 0.288 MB | −10.6% |
| | 대상 쿼리 total_ms | 45112.076913 (집계) | 없음 | 쿼리 소멸 |
| | 읽은 행 / 반환 행 | 집계 204.0763 / 조회 1.9998 | 조회 1.9989 | 집계 항목 소멸 |
| | 접근 방식과 인덱스 | `carts` ref `uk_member_course`, `courses` eq_ref PRIMARY, `schedules` ref `idx_course_id` | 동일 | 변화 없음 |
| | `Handler_read_rnd_next` | 0 | 0 | 동일 |
| | `Sort_rows` | 6 | 6 | 동일 |
| | 캐시 hit / miss, 적중률 | - | - | - |

**실행계획** (원본: `query-plan-1.txt`)

- 파라미터는 Phase 6과 동일 (회원 907548)
- 쿼리 전체 소요 0.161 ms (개선 전 Q2 0.162 ms)
- `cart_count`가 계획을 전혀 바꾸지 않았다. 접근 방식, 인덱스, loops, `Handler_read_key` 13, `Handler_read_next` 24, `Sort_rows` 6, `Sort_scan` 1이 개선 전과 완전히 동일하다. `courses` 행을 이미 PK로 단건 조회하고 있었으므로 컬럼 하나가 그 행에 얹혀 따라온 것뿐이고, 조인이나 조회가 늘지 않았다

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): **있음.** 쿼리는 digest에 있거나 없거나 둘 중 하나이므로 편차가 낄 수 없다. `countCartedCoursesByCourseId`가 사라져 요청당 SQL이 정확히 1.0 줄었고(1.9933 → 0.9931), 리포지토리 호출도 2.000 → 0.999다. 읽은행/반환행 204.0763 항목이 통째로 없어졌고, 남은 조회 쿼리는 1.9989로 그대로다. 요청당 DB 실행 시간은 2.2147 ms → 1.0750 ms
- 처리량 증가의 기전: 커넥션 보유 평균 22.5 → 14.2 ms. 풀 10 기준 이론 RPS 444 → 704(비율 1.585), 실측 398 → 626(비율 1.571)으로 두 측정이 같은 기전 위에 있다
- 예측과 실측의 어긋남: Phase 6에서 보유 시간이 약 3.85 ms 줄 것으로 예측했으나 실제로는 8.3 ms 줄었다. 왕복 단가를 고정으로 본 것이 틀렸다. 총 작업량이 줄면서 경합이 함께 풀려 남은 왕복이 싸졌다 - 왕복당 2.90 ms → 2.19 ms. 이 증폭분은 환경 고유값(macOS Docker Desktop 포트 포워딩, 호스트 CPU 포화 78~90%)이므로, 왕복이 싼 환경에서는 같은 변경의 상대 이득이 작게 나온다. 어디서든 같은 것은 구조 변화(쿼리 −1, 왕복 −1, 읽는 엔트리 1,318 → 0)이고 +57% RPS는 이 환경의 값이다
- Phase 6 위험 신호 해소 현황
  - `examined_per_sent` 100:1 초과 → **해소.** 204.0763 항목이 쿼리째 사라졌고 남은 쿼리는 1.9989
  - 추정 대 실측 10배 이상 괴리 → 해소. 전 노드에서 괴리 없음
  - OLTP 풀스캔 → 원래 없었고 그대로 없음
  - 요청당 호출 1회 초과(N+1) → 없음 (0.9931)
  - `Sort_rows`가 반환 행 수보다 큼 → 아님 (6 대 18)
  - 단일 쿼리 total_ms 30% 이상 → **형식상 미해소.** `findByMemberId`가 57.64%로 올랐다. 다만 집계가 빠져 분모가 줄어든 결과이며, 요청당으로 보면 0.7089 ms → 0.6196 ms로 오히려 싸졌다. 읽은행/반환행 2.0은 조인의 자연스러운 팬아웃이고 전 구간이 인덱스를 타므로 걷어낼 것이 없다
- 남은 위험 신호: **트랜잭션 제어문 비중이 커졌다.** 요청당 왕복 약 6 중 4.96이 제어문(83%)이고, DB 시간 비중은 25.4% → 42.4%로 올랐다. 절대량은 그대로인데 실제 쿼리가 하나로 줄어 상대 비중이 드러난 것이다. #106이 "판정에 반영하지 않은 위험 신호"로 남긴 두 항목 중 하나가 이것이고, 나머지 하나(집계 쿼리의 읽기 증폭)는 이번 사이클에서 해소했다
- 미측정: 쓰기 경로에 추가한 카운터 UPDATE의 비용과 인기 강의 행의 X 락 직렬화. 이번 측정은 읽기 전용이라 실행되지 않았다. `POST /carts/{courseId}`를 별도 대상으로 재야 드러난다
- 다음 사이클 진행 여부: 호출자 결정

---

---

## 최종 요약

> 사이클이 하나라 최종 상태는 `-1`이다. 최초와 최종의 차이는 사이클 1의 개선 전후와 같다.

| 구분 | 지표 | 최초 | 최종 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 113.788 ms | 73.391 ms | −35.5% |
| | p99 | 233.371 ms | 128.149 ms | −45.1% |
| | RPS | 398.42 | 625.85 | +57.1% |
| | 쿼리 전체 소요 | Q1 0.478 + Q2 0.162 ms | Q2 0.161 ms | Q1 소멸 |
| | heap 최대 | 266.8 MB | 306.9 MB | +15.0% |
| | GC 일시정지 합 / 최장 | 311.0 / 28.0 ms | 257.0 / 15.0 ms | −17.4% / −46.4% |
| | HikariCP pending 최대 | 20 | 20 | 동일 |
| | 커넥션 보유 평균 | 22.5 ms | 14.2 ms | −36.9% |
| | process CPU 최대 | 0.194 | 0.213 | +9.8% |
| 하드웨어 독립 | 요청당 쿼리 수 | SQL 1.9933 + 제어 4.9646 | SQL 0.9931 + 제어 4.9580 | SQL −1.0 |
| | 요청당 리포지토리 호출 수 | 2.000 | 0.999 | −1.0 |
| | 요청당 할당량 (MB) | 0.322 | 0.288 | −10.6% |
| | 읽은 행 / 반환 행 | 집계 204.0763 / 조회 1.9998 | 조회 1.9989 | 집계 항목 소멸 |
| | 접근 방식과 인덱스 | `carts` ref `uk_member_course`, `courses` eq_ref PRIMARY, `schedules` ref `idx_course_id` | 동일 | 변화 없음 |
| | `Handler_read_rnd_next` | 0 | 0 | 동일 |
| | 캐시 hit / miss, 적중률 | - | - | - |

적용한 기법: 사이클 1 - 담기 수 비정규화 카운터(`courses.cart_count`) + 원자적 UPDATE 갱신

핵심은 요청당 SQL이 2에서 1로, 왕복이 7에서 6으로 준 것이다. 담기 수를 얻으려고 인덱스 엔트리 1,318개를 훑던 일이 `courses` 행에 이미 실려 오는 컬럼 하나로 바뀌었다. 담기가 늘수록 읽는 양이 정비례해 커지던 축이 사라졌고, 이것이 #106이 "판정에 반영하지 않은 위험 신호"로 남긴 두 항목 중 하나였다.

RPS +57.1%라는 크기는 이 환경의 값이다. 왕복 단가가 큰 조건(macOS Docker Desktop 포트 포워딩, 호스트 CPU 포화 78~90%)에서 왕복 하나가 빠지면서 남은 왕복까지 싸졌기 때문이다(왕복당 2.90 → 2.19 ms). 왕복이 싼 환경에서는 상대 이득이 작게 나온다. 어디서든 같은 것은 구조 변화다.

운영 반영 시 유의점

- 마이그레이션 `V1_13__add_cart_count_to_courses.sql`은 `ALTER TABLE courses ADD COLUMN`과 기존 데이터 backfill `UPDATE` 두 문이다. 측정 환경(`courses` 26,439행, `carts` 68,182행)에서는 체감되지 않았다. **운영 행 수를 확인하지 않았으므로 운영에서의 소요 시간은 모른다.** MySQL 8.0의 컬럼 추가는 INSTANT 알고리즘 대상이지만 backfill `UPDATE`는 전 행을 갱신하므로 규모에 비례한다
- 테스트는 H2 + `ddl-auto: create-drop`이라 Flyway를 타지 않는다. 이 마이그레이션은 단위 테스트로 검증되지 않았고, perf 프로파일 기동에서만 확인했다(적용 성공, 어긋난 강의 0건, `sum(cart_count)` 68,182 = `carts` 행 수)
- `carts`의 FK가 `ON DELETE CASCADE`다. 회원이나 강의를 직접 지우면 담기 행은 사라지지만 `cart_count`는 따라 줄지 않는다. 현재 회원 삭제 API는 없다
- 담기와 같은 트랜잭션에서 `Course` 엔티티를 수정하는 코드가 생기면 더티 체킹이 `cart_count`를 낡은 값으로 덮어쓴다(사이클 1 **적용 내용**의 발견 사항). `current_enrollment`도 같은 조건이다
- 쓰기 경로(`addCart`, `deleteCartedCourse`)에 늘어난 UPDATE 1건과 인기 강의 행의 X 락 직렬화 비용은 **측정하지 않았다.** 이번 대상이 읽기 전용이라 실행되지 않았다
- 남은 위험 신호: 요청당 왕복 약 6 중 4.96(83%)이 트랜잭션 제어문이고 DB 시간의 42.4%다. 절대량은 그대로인데 실제 쿼리가 하나로 줄어 비중이 드러났다. `SET autocommit` 2회는 `hikari.auto-commit`과 `hibernate.connection.provider_disables_autocommit` 설정이 없어서, `SET SESSION TRANSACTION READ ONLY`/`READ WRITE` 쌍은 `@Transactional(readOnly = true)`가 만든다. 모든 엔드포인트에 공통이라 이 대상의 사이클로 다루지 않고 종료했다

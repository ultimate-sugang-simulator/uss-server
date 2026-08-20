# [CONC-98] RegistrationService.deleteRegisteredCourse

> 이슈: #98
> 브랜치: fix/98-registration-cancel-atomic-decrement
> 대상 디렉토리: `.claude/resources/concurrency/98/delete-registered-course/`

이 파일은 임계 구역 하나만 다룬다. 같은 이슈의 다른 임계 구역은 각자의 디렉토리에 각자의 `record.md`를 가진다.

## 진행 상태

> ⏳ 미완 / ✅ 완료 / ⏭️ 건너뜀
> 재진입 시 ⏳로 표기된 가장 이른 Phase부터 재개한다.

**준비 (대상당 1회)**

| 1. 불변식 | 2. 환경 | 3. 경합 조건 | 4. 재현 |
|---|---|---|---|
| ✅ | ✅ | ✅ | ✅ |

**후보 (반복)**

| # | 기법 | 5. 설계 | 6. 적용 | 7. 검증 |
|---|---|---|---|---|
| 1 | 원자적 조건부 UPDATE | ✅ | ✅ | ✅ |

**마무리**

| 8. 채택 |
|---|
| ✅ |

## 대상

- 임계 구역: `RegistrationService.deleteRegisteredCourse` (`src/main/java/uss/code/registration/service/RegistrationService.java`:72-86)
- 공유 자원: `courses` 행의 `current_enrollment` (집계 컬럼), `registrations` 행 (실제 등록)
- read-modify-write 구간: `:80` `courseRepository.findById`(값 읽기) → `:83` `course.decrementEnrollment()`(`Course`:244-246, 읽은 값 -1) → 커밋 시 flush
- 실제 발행 SQL: 더티 체킹. `Course`에 `@DynamicUpdate`가 없어 전 컬럼을 싣는다.
  `UPDATE courses SET academic_year=?, ..., current_enrollment=?, status=? WHERE id=?`
  - 절대값 대입인가 증감식인가: **절대값**. 바인딩되는 값은 트랜잭션이 읽어둔 값 - 1이며 `current_enrollment = current_enrollment - 1`이 아니다
- 트랜잭션 경계: `@Transactional`(`:72`)이 메서드 전체를 감싼다. UPDATE는 커밋 직전 flush에서 나가므로 읽은 시점과 쓰는 시점 사이의 창이 트랜잭션 거의 전 구간이다
- 반대편 경로: `registerCourse`(`:105`)는 `CourseRepository.increaseEnrollmentWithinCapacity`(`CourseRepository`:120-127)를 쓴다.
  `UPDATE Course c SET c.currentEnrollment = c.currentEnrollment + 1 WHERE c.id = :id AND c.currentEnrollment < c.maxCapacity` - **증감식**.
  #90에서 이 경로만 원자적 UPDATE로 바꿨고 취소 경로는 더티 체킹으로 남았다. 같은 컬럼에 두 형태가 섞여 있다
- 기존 방어 장치

  | 수단 | 유무 | 막아주는 범위 |
  |---|---|---|
  | `@Version` | 없음 (코드베이스 전체에 0건) | - |
  | 비관적 락 | 없음 (락 힌트 0건) | - |
  | UNIQUE 제약 | 있음 `uk_member_course (member_id, course_id)` (`V1_0__init_table.sql`) | 같은 회원의 같은 강의 중복 행. 카운터 유실은 못 막는다 |
  | CHECK 제약 | 없음 (`current_enrollment INT NOT NULL DEFAULT 0`, `V1_0__init_table.sql`:48) | NULL만. 음수와 정원 초과는 못 막는다 |
  | FK | 있음 `registrations.course_id → courses(id)` | `:80`의 `COURSE_NOT_FOUND` 분기가 도달 불가임을 보장한다 |

## 불변식

> Phase 1에서 호출자와 확정한 것. **후보마다 바꾸지 마라.**
> 검증 SQL은 `delete-registered-course/invariant-check.sql`에 저장하고 Phase 4, 7에서 그대로 재사용한다.

| # | 불변식 | 검증 방법 | 깨졌을 때의 증상 |
|---|---|---|---|
| I1 | `current_enrollment` = `COUNT(registrations)` | 두 값의 절대 차를 `violations`로 센다 | 취소 두 건이 같은 값을 읽고 같은 절대값을 써서 감소 하나가 유실된다. 카운터가 실제보다 크게 남아, 자리가 비어 있는데도 신청 게이트(`current_enrollment < max_capacity`)에 걸려 마감으로 보인다 |
| I2 | `current_enrollment >= 0` | 음수면 `violations` 1 | 유실이 누적된 상태에서 취소가 계속 들어오면 카운터가 음수로 내려간다. `decrementEnrollment()`에 하한이 없고 컬럼 CHECK 제약도 없다 |

응답 코드 기대치: 취소 성공 200. 신청하지 않은 강의 취소는 404 `REGISTERED_COURSE_NOT_FOUND`(4003).

**채택하지 않은 축** (Phase 1에서 후보로 올렸으나 호출자가 제외)

| 축 | 제외 사유 | 어디에 남겼는가 |
|---|---|---|
| `COUNT(registrations) <= max_capacity` | 취소 단독 부하로는 깨지지 않는다. 신청과 취소를 섞은 부하에서만 의미가 생긴다 | `invariant-check.sql`의 `ref. 등록 행 수 대 정원` |
| 동시 중복 취소의 응답 코드 | 데이터는 롤백되어 지켜진다. 핸들러 추가는 이번 작업 범위에서 뺀다 | 아래 관찰 기록 |

> 동시 중복 취소(같은 회원, 같은 강의) 관찰: 두 트랜잭션이 모두 `:77`에서 등록 행을 찾고, 뒤늦은 쪽의
> `DELETE`가 0행에 걸린다. Hibernate가 행 수 불일치로 `StaleStateException`을 던지는데
> `GlobalExceptionHandler`(`:25-77`)에 JPA 계열 핸들러가 없어 catch-all `Exception`으로 떨어져
> `UNEXPECTED_SERVER_ERROR`(500)가 나간다. 의도한 응답은 404다. 불변식으로 채택하지 않았으므로
> 판정 기준에 넣지 않는다. Phase 4에서 응답 분포에 500이 섞이면 이 경로를 먼저 의심한다.

## 측정 환경

> 값이 바뀌면 그 사실을 후보 기록에 남긴다.

| 항목 | 값 |
|---|---|
| 프로파일 | conc (`application-conc.yml`). #90이 만든 파일을 그대로 쓴다 |
| DB | MySQL 8.0 / InnoDB (`127.0.0.1:3307`, `uss_db`) |
| 커넥션 풀 크기 | 설정 `maximum-pool-size: 500`, `minimum-idle: 500`. 실측 `Threads_connected=501`(풀 500 + mysqlc 세션 1) |
| MySQL `max_connections` | 600. #90이 `SET PERSIST`로 올린 값이 유지되고 있다(실측 확인) |
| 격리 수준 | `REPEATABLE-READ` (global = session), `autocommit=1` |
| 락 대기 타임아웃 | `innodb_lock_wait_timeout` 50초 |
| 관측 가능한 락 지표 | `Innodb_row_lock%` 5개 / `INNODB_METRICS`의 `lock_deadlocks`, `lock_timeouts`, `lock_row_lock_waits` 모두 `enabled` / `performance_schema.data_locks` 조회 가능 |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고 애플리케이션 캐시는 쓰지 않는다 |

**측정 시작 시점의 누적 카운터.** MySQL 컨테이너를 새로 띄운 직후라 전부 0에서 출발한다.
`Innodb_row_lock%` 5개, `lock_deadlocks`, `lock_timeouts`, `lock_row_lock_waits` 모두 0.

**대상 강의 초기 상태.** id 990001, `max_capacity=100`, `current_enrollment=0`, 등록 행 0.
#90의 되돌리기가 끝난 상태 그대로다.

**셸 접속 (zsh).** 호스트에 `mysql` 클라이언트가 없고, `SKILL.md`가 제시하는 `$MYSQL_CONC` 문자열 변수는
zsh가 파라미터 확장 결과를 단어 분할하지 않아 동작하지 않는다. #90과 같이 컨테이너 경유 함수를 쓴다.

```bash
mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot uss_db "$@"; }
```

**환경 구성에서 손댄 것.** 이번 대상에서 새로 바꾼 것은 없다. #90이 잡아둔 아래 두 가지가 그대로 유효하다.

| 대상 | 상태 |
|---|---|
| `DataSourceConfig`:24 `@ConfigurationProperties("spring.datasource.hikari")` | 커밋되어 dev에 반영됨. 없으면 풀 설정이 바인딩되지 않는다 |
| MySQL `max_connections = 600` (`SET PERSIST`) | 유지 확인. 되돌리기는 `RESET PERSIST max_connections` |

> #90이 넣었던 `management.health.db.enabled: false`는 현재 `application-conc.yml`에 없다.
> 도달 불가능한 더미 Oracle DataSource에 헬스 프로브가 붙는 문제 때문이었는데,
> #92에서 Oracle 경로가 제거되며 원인이 사라졌다.

## 경합 조건

> Phase 3에서 확정. **모든 후보가 이 조건을 그대로 쓴다.**

부하 형태는 **취소 단독**이다. 신청과 섞지 않는다.
호출자가 Phase 1에서 `COUNT(registrations) <= max_capacity`를 불변식으로 채택하지 않았으므로
혼합 부하가 있어야만 드러나는 축이 판정에 들어오지 않는다. I1은 취소 단독으로도 깨진다.

| 항목 | 값 |
|---|---|
| 다투는 자원 | 강의 id 990001 (`invariant-check.sql`이 이 값을 쓴다) |
| 허용 상한 | 500. 취소에는 정원 초과 개념이 없다. 상한은 **시드가 꽂아둔 등록 행 수**이며 성공 수가 이 값과 정확히 같아야 한다 |
| 동시 요청 수 (VU) | 500 (커넥션 풀 500이 상한. 실측 `Threads_connected=501`로 확인) |
| 회원 수 | 500 (`@member_start=900001` ~ 900500). VU와 1대1 |
| 대상 강의 정원 | `@target_capacity = 500`. 500명이 등록된 상태가 정원 안에 들어와야 시드가 정합하다 |
| 출발 상태 | **만석.** `registrations` 500행, `current_enrollment` 500 |
| executor | `per-vu-iterations`, `iterations: 1` (ramp 없음) |
| 시드 SQL | `../seeds.sql` (변수 블록) + `../seed-registrations.sql` (이슈 전용 꼬리) |
| 시드 모듈과 변수 | `member.sql`, `contention-course.sql`을 `cat`으로 이어 붙인다. `SOURCE`는 컨테이너 경유 클라이언트에서 동작하지 않는다 |
| 측정용 토큰 | `../tokens.json` (서명키로 직접 생성, 로그인 API 미사용) |
| 기대 결과 | 성공 500, `current_enrollment` 0, 등록 행 0 |

> **모듈 순서를 바꾸지 마라.** `contention-course.sql`이 말미에 `registrations`를 비우고
> 카운터를 0으로 되돌리므로, 등록을 채우는 `seed-registrations.sql`은 반드시 그 뒤에 와야 한다.

**I2에 대한 단서.** 취소 단독 부하에서 `current_enrollment`는 음수가 되지 않는다.
유실은 카운터를 실제보다 **크게** 남기는 방향으로만 작용하고, 각 트랜잭션이 쓰는 값은
자기가 읽은 값 - 1이라 500에서 출발하면 하한을 뚫을 수 없다.
따라서 baseline에서 I2 위반은 0으로 나오는 것이 정상이며, 이 지표는 후보 기법이
하한 조건을 실제로 걸었는지 확인하는 회귀 방지용으로 남는다. **I2가 0이라고 결함이 없는 것이 아니다.**

**되돌리기 SQL** (매 측정 전에 실행)

```sql
-- 취소 대상의 되돌리기는 "비우기"가 아니라 "다시 채우기"다.
DELETE FROM registrations WHERE course_id = 990001;

INSERT INTO registrations (member_id, course_id, created_at)
SELECT id, 990001, NOW() FROM members WHERE id BETWEEN 900001 AND 900500;

UPDATE courses SET current_enrollment = 500 WHERE id = 990001;

-- 되돌아갔는지 확인. **둘 다 500이어야 한다** (신청 대상은 0이었다).
SELECT (SELECT COUNT(*) FROM registrations WHERE course_id = 990001) AS rows_left,
       (SELECT current_enrollment FROM courses WHERE id = 990001)    AS counter;
```

## 재현 (Baseline, `-0`)

> 원본: `invariant-0.txt` / `k6-burst-summary-0.json` / `lock-stats-0.txt`
> 여기에는 판정 근거로 쓴 값만 옮긴다. 전체를 복사하지 않는다.

**정합성**

| 불변식 | 기대 | 실측 | 위반 |
|---|---|---|---|
| I1 | 0 | 497 | **497** |
| I2 | `>= 0` | 497 | 0 |

I2가 0인 것은 결함이 없어서가 아니다. 취소 단독 부하에서는 구조적으로 하한을 뚫을 수 없다(경합 조건 참조).

**응답 분포**

| 상태 코드 | 건수 | 의미 |
|---|---|---|
| 2xx | 500 | 전 요청 성공. 애플리케이션은 아무 이상도 감지하지 못했다 |
| 404 (4003) | 0 | 기대 거절 없음. 시드가 정확히 만석이었다는 뜻 |
| 기타 4xx | 0 | - |
| 5xx | 0 | 동시 중복 취소 경로(같은 회원 2회)는 이번 부하에 없다 |

RPS 58.9, p95 8074.8ms, p99 8175.6ms, max 8214.4ms.

**경합**

| 지표 | BEFORE | AFTER | 증가분 |
|---|---|---|---|
| `Innodb_row_lock_waits` | 0 | 499 | 499 |
| `Innodb_row_lock_time` | 0 | 1104367 | 1104367 |
| `Innodb_row_lock_time_avg` | 0 | 2213 | 2213 |
| `Innodb_row_lock_time_max` | 0 | 3844 | 3844 |
| `lock_deadlocks` | 0 | 0 | 0 |
| `lock_timeouts` | 0 | 0 | 0 |

`LATEST DETECTED DEADLOCK` 섹션은 비어 있다.

**진단**

- 재현 여부: **재현됨.** I1 위반 497건
- 결함 성격: 절대값 대입 UPDATE로 인한 lost update. 500번의 감소 중 3번만 반영됐다
- 근거: 등록 행은 500개가 전부 지워졌는데(`rows_left=0`) 카운터는 497이 남았다.
  카운터 정합 오차 497이 곧 유실된 감소 횟수다. 응답은 500건 전부 2xx라
  **애플리케이션은 아무 이상도 감지하지 못했다.** 이 결함은 응답만 봐서는 발견되지 않는다
- 추정 인터리빙:

  | 단계 | 일어난 일 | 근거 |
  |---|---|---|
  | 1 | 500개 트랜잭션이 `findById`로 `current_enrollment`를 읽는다. **락을 잡지 않는다** | 락 힌트 0건. REPEATABLE READ의 평범한 SELECT는 non-locking consistent read다. `lock_deadlocks=0`이 이를 뒷받침한다 - S락을 쥔 채 X락 승격을 시도했다면 lock upgrade deadlock이 대량 발생했을 것이다 |
  | 2 | 거의 전부가 같은 값 `500`을 읽고, 각자 `499`를 쓸 준비를 한다 | 최종 카운터 497. 감소가 3번만 반영됐다는 것은 497개가 같은 값을 읽었다는 뜻이다 |
  | 3 | 커밋 시점의 UPDATE에서 처음으로 줄을 선다. 첫 트랜잭션이 X락을 쥐고 나머지 499개가 대기한다 | `Innodb_row_lock_waits` 499 = 500 - 1. 평균 대기 2213ms |
  | 4 | 순서대로 통과하지만 각자 손에 든 절대값 `499`를 그대로 쓴다. 마지막 쓰기만 남는다 | p99 8.2초는 직렬화 비용이다. 직렬화는 됐지만 값은 낡은 채였다 |
  | 5 | 늦게 시작해 스냅샷을 나중에 뜬 3개만 `499`, `498`을 읽어 실제 감소를 반영했다 | 500 - 497 = 3 |

  **락은 쓰기 순서를 정해줄 뿐, 읽은 값이 낡았다는 사실은 고쳐주지 못한다.**
  이 대상에서 직렬화(비관적 락)만으로는 부족하고, 읽기와 쓰기가 같은 원자 단위에 들어가야 한다는 것이
  baseline이 보여준 사실이다.

---

## 후보 1: 원자적 조건부 UPDATE

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 조건절 | `WHERE c.id = :id AND c.currentEnrollment > 0` | 하한을 조건절이 지킨다. 조건 없는 안(원자성만)은 I2를 못 지켜 후보가 될 수 없다 |
| 갱신 위치 | `CourseRepository.decreaseEnrollmentAboveZero` | `repository.md`가 쿼리를 Repository에 `@Query`로 두라고 못박는다. 반대편 `increaseEnrollmentWithinCapacity`와 대칭이 된다 |
| 영향 행 0일 때 | `RestApiException(REGISTRATION_CANCEL_CONFLICT)`를 던져 트랜잭션을 롤백 | 아래 참조 |
| 신설 예외 | `REGISTRATION_CANCEL_CONFLICT(CONFLICT, 4004, "수강 취소를 반영할 수 없습니다. 다시 확인해주세요.")` | 수강신청 카테고리가 4000~4003을 쓰고 있어 4004가 다음 번호다. 두 원인을 모두 덮는 이름을 골랐다 |

**영향 행 0의 두 가지 원인.** 이 분기를 예외로 처리하기로 한 근거다.

| 원인 | 상황 | 이 결정의 효과 |
|---|---|---|
| 1. 무결성 이미 깨짐 | 카운터 < 실제 등록 행 수. 발급한 티켓보다 유저가 든 티켓이 많다. I1이 이미 깨진 상태 | 사용자가 잠긴다. 손상이 동결되지만 드러난다. 수동 SQL이나 배치가 손대야 생기는 드문 경우 |
| 2. 동시 중복 취소 | 같은 회원이 취소를 두 번 눌러, 뒤늦은 트랜잭션이 조건절 `> 0`을 통과하지 못한다. **무결성이 멀쩡해도 난다** | **500이 404급 의미 있는 응답으로 바뀐다.** 예외를 안 던지면 다음 줄의 `delete`가 0행에 걸려 Hibernate가 `StaleStateException`을 던지고, 핸들러가 없어 catch-all로 떨어져 `UNEXPECTED_SERVER_ERROR`(500)가 나간다 |

원인 2가 운영에서 훨씬 흔하고, 그쪽에서 응답 품질이 개선되는 것이 이 결정의 실익이다.
Phase 1에서 범위 밖으로 뺀 "동시 중복 취소가 500으로 나간다"는 축이 부수적으로 정리된다.

- 검토했지만 택하지 않은 안
  - **조건절 없는 원자적 UPDATE** - 유실은 사라지지만 하한이 없어 I2를 지키지 못한다
  - **영향 행 0을 무시하고 등록 행 삭제만 진행** - 원인 2에서 조용히 넘어가지 못한다. `delete`가 0행에 걸려 500이 난다. 조용한 처리가 성립하지 않는다
  - **기존 `REGISTERED_COURSE_NOT_FOUND`(4003) 재사용** - 호출자가 원인 1과 2를 구분해 드러낼 수 있는 신설 코드를 택했다

- 호출자가 예상한 결과
  - 정합성: 일제히 개선
  - 경합: 조금 더 늘어날 수도 (악화 방향)

> **스킬이 덧붙인 단서.** 정합성 예상은 타당하다. 경합 예상은 "동시성 제어는 처리량을 깎는다"는
> 일반론으로는 맞지만 이 후보에는 그대로 적용되지 않을 수 있다. **이 기법은 락을 새로 걸지 않는다.**
> baseline에서 이미 X락이 499번 잡혔고 후보 1도 같은 자리에서 같은 X락을 잡는다.
> 달라지는 것은 값을 어디서 계산하느냐뿐이다. 오히려 `courseRepository.findById` SELECT 하나가
> 사라지고, `@DynamicUpdate`가 없어 27개 컬럼을 전부 싣던 UPDATE가 한 컬럼짜리로 줄어
> 락 보유 구간이 짧아질 여지가 있다. 어느 쪽이 이길지는 Phase 7에서 실측으로 가른다.

### 적용 내용

| 파일 | 변경 |
|---|---|
| `course/repository/CourseRepository.java` | `decreaseEnrollmentAboveZero` 추가. `increaseEnrollmentWithinCapacity` 바로 뒤에 대칭 배치 |
| `global/exception/domain/ExceptionCode.java` | `REGISTRATION_CANCEL_CONFLICT(CONFLICT, 4004, ...)` 추가 |
| `registration/service/RegistrationService.java` | `deleteRegisteredCourse`에서 `courseRepository.findById`와 `course.decrementEnrollment()` 제거, `decreaseEnrollment(courseId)` 호출로 교체. `increaseEnrollment`와 대칭인 private 메서드 추가 |

3파일 19줄 추가, 4줄 삭제. **기법 하나만 올렸다.** `Course.decrementEnrollment()`와 이미 죽어 있던
`incrementEnrollment()` 제거는 측정과 무관한 정리라 Phase 8로 미뤘다.

- 추가한 마이그레이션: 없음 (스키마 변경 없음)
- 테스트: `./gradlew test` BUILD SUCCESSFUL

**제어 발행 확인**

```
UPDATE `courses` `c1_0` SET `current_enrollment` = ( `c1_0` . `current_enrollment` - ? )
WHERE `c1_0` . `id` = ? AND `c1_0` . `current_enrollment` > ?
```

증감식이며 조건절이 붙어 있다. baseline의 27개 컬럼짜리 더티 체킹 UPDATE는 나타나지 않는다.

| 확인 | 결과 |
|---|---|
| 정상 취소 | 200, `rows_left=0`, `counter=0` |
| 영향 0행 (카운터 0인데 등록 행 존재) | **409 / code 4004**, `rows_left=1`로 롤백 확인 |

> **1차 확인은 무효였다.** 기존 앱을 내리지 않고 재기동해 `Port 8080 was already in use`로 실패했고,
> 옛 코드가 계속 8080을 잡고 있었다. digest에 27개 컬럼 UPDATE와 `current_enrollment = - ?`가
> 찍힌 것이 그 증거다(옛 코드가 카운터 0에서 0-1=-1을 썼다). 포트를 비우고 다시 기동해 재확인했다.
> **Phase 6의 발행 확인이 없었다면 이 상태로 Phase 7을 측정하고 "효과 없음"으로 오판했을 것이다.**

**부수 관찰 (이번 범위 밖).** `RestApiException`이 메시지를 담지 않아
(`exceptionCode` 필드만 가진다) `GlobalExceptionHandler`:29의
`log.error("예외 발생: {}", e.getMessage())`가 항상 `null`을 찍는다.
4004가 발생해도 로그에는 `예외 발생: null`만 남는다.
A안을 고른 근거가 "원인 1의 손상이 드러난다"였는데, 현재 로깅으로는 드러나지 않는다.
Phase 8의 운영 유의점으로 넘긴다.

### 검증 결과

**정합성 (1급)**

| 불변식 | 원본(`-0`) 위반 | 후보(`-1`) 위반 | 판정 |
|---|---|---|---|
| I1 | 497 | **0** | 확보 |
| I2 | 0 | 0 | 유지 |

참고 지표 `ref. 카운터가 어긋난 강의 수`도 1 → **0**. 대상 강의뿐 아니라 전체 강의에서 어긋난 행이 없다.

판정: **정합성 확보.** 두 불변식 모두 위반 0건.

**경합 (2급)**

| 지표 | 원본(`-0`) | 후보(`-1`) | 변화 |
|---|---|---|---|
| 성공 요청 수 | 500 | 500 | 동일 |
| RPS | 58.9483890348451 | 246.81619449905938 | 4.19배 증가 |
| p95 / p99 | 8074.8233 / 8175.550560000001 | 1903.4141499999998 / 1964.6279200000001 | 4.24배 / 4.16배 감소 |
| `Innodb_row_lock_waits` 증가분 | 499 | 499 | **동일** |
| `Innodb_row_lock_time` 증가분 | 1104367 | 309638 | 3.57배 감소 |
| `Innodb_row_lock_time_avg` (구간) | 2213 | 620.5 | 3.57배 감소 |
| `lock_deadlocks` 증가분 | 0 | 0 | 동일 |
| `lock_timeouts` 증가분 | 0 | 0 | 동일 |
| 재시도 횟수 | 해당 없음 | 해당 없음 | - |

> `Innodb_row_lock_time_avg`는 서버 기동 이후 전역 누적 평균이라 뺄셈으로 증가분을 낼 수 없다.
> 구간 평균은 `Innodb_row_lock_time` 증가분 / `Innodb_row_lock_waits` 증가분으로 직접 계산했다.
> baseline 1104367/499 = 2213ms, 후보 309638/499 = 620.5ms.

- 응답 코드 품질: 500건 전부 2xx. 4xx, 5xx, 파싱 불가 모두 0건. 500이 섞이지 않았다
- 예상과의 대조

  | 축 | 호출자 예상 | 실측 | 판정 |
  |---|---|---|---|
  | 정합성 | 일제히 개선 | I1 497 → 0, I2 0 유지 | **적중** |
  | 경합 | 조금 더 늘어날 수도 (악화) | RPS 4.19배 증가, p99 4.16배 감소 | **어긋남** |

  틀린 가정은 "동시성 제어는 처리량을 깎는다"를 이 후보에 그대로 적용한 것이다.
  그 일반론은 **락을 새로 걸거나 임계 구역을 넓히는 기법**에 해당한다.
  이 후보는 락을 추가하지 않았다. `Innodb_row_lock_waits` 증가분이 baseline과 **정확히 같은 499**인 것이
  그 직접 증거다. 락을 잡는 횟수도, 줄 서는 구조도 바뀌지 않았다.

  바뀐 것은 **락을 쥔 채로 하던 일의 양**이다. baseline은 인덱스에 걸린 11개 컬럼을 포함해
  27개 컬럼을 매번 다시 썼고(`@DynamicUpdate` 없음), 후보는 어느 인덱스에도 걸리지 않은
  `current_enrollment` 하나만 건드린다. `courses`에는 BTREE 4개와 FULLTEXT 1개가 붙어 있어
  이 차이가 유독 크게 나타난다.

  > **락 시간을 논할 때는 락 안에서 벌어진 일만 세야 한다.** 이번 변경으로 `courseRepository.findById`
  > SELECT도 사라졌지만 그것은 락 획득 **이전**의 일이라 응답시간에만 기여하고 락 보유 시간에는
  > 영향이 없다. 2213ms → 620.5ms를 만든 것은 UPDATE 문장 자체다.
  >
  > 다만 InnoDB는 값이 바뀌지 않은 컬럼의 세컨더리 인덱스 갱신은 건너뛴다. 인덱스 5개가 통째로
  > 갱신됐다고 단정하면 과하다. 확실한 것은 긴 VARCHAR가 다수인 27개 컬럼을 매번 전송, 비교하고
  > undo/redo에 그만큼 큰 row image를 남겨야 했다는 것이다.

### 되돌리기

- stash: **없음.** 후보가 하나뿐이었고 호출자가 Phase 7에서 비교 종료를 택해 그대로 채택했다.
  스킬 절차(코드를 stash 했다가 Phase 8에서 되살리기)는 다음 후보의 측정 오염을 막는 것이 목적인데
  다음 후보가 없어 왕복이 의미가 없다. 이 편차와 이유를 여기 남긴다
- 스키마 되돌리기: 해당 없음 (스키마 변경 없음)
- 데이터 복원 확인: `rows_left = 500`, `counter = 500` **확인**
  (최종 검증 직전 되돌리기 출력. 취소 대상은 만석이 초기 상태다)

---

## 최종 요약

### 후보 비교

| # | 기법 | 불변식 위반 | 성공 요청 수 | RPS | p99 | 락 대기 | 데드락 | 응답 코드 |
|---|---|---|---|---|---|---|---|---|
| 0 | (원본, 더티 체킹) | I1 **497** / I2 0 | 500 | 58.9483890348451 | 8175.550560000001 | 499 | 0 | 2xx 500, 5xx 0 |
| 1 | 원자적 조건부 UPDATE | I1 **0** / I2 0 | 500 | 246.81619449905938 | 1964.6279200000001 | 499 | 0 | 2xx 500, 5xx 0 |

처리량은 로컬 측정이므로 절대값을 단정하지 않고 원본 대비 상대 변화로만 읽는다.

### 채택

- **채택한 기법: 후보 1 - 원자적 조건부 UPDATE**
- 채택 근거: 후보 1 단독으로 두 불변식을 모두 지켰고(I1 497 → 0), 정합성을 얻는 대가가
  음수였다. 처리량이 4.19배 올랐고 p99가 4.16배 줄었다. 다른 후보를 재서 뒤집을 여지가 없다고
  판단해 호출자가 비교를 종료했다
- 배제한 후보와 이유

  | 후보 | 상태 | 이유 |
  |---|---|---|
  | 비관적 락 (`SELECT ... FOR UPDATE`) | 재지 않음 | baseline이 이미 답을 줬다. X락이 **499회** 잡혔는데도 497이 유실됐다. 이 대상의 문제는 직렬화 부족이 아니라 **락 밖에서 읽은 값이 낡았다는 것**이다. `FOR UPDATE`는 읽기를 락 안으로 넣어 정합성은 확보하겠지만, 락 구간을 커밋 시점의 UPDATE에서 읽기 시점까지 넓힌다. 이미 p99 8.2초인 구간을 더 늘리는 방향이라 대가만 커진다 |
  | 낙관적 락 (`@Version`) + 재시도 | Phase 5-A에서 제외 | 신청 경로의 `increaseEnrollmentWithinCapacity`가 JPQL bulk UPDATE라 `version`을 올리지 않는다(HQL `UPDATE VERSIONED`로 명시하지 않는 한). 취소의 버전 검사가 통과하면서 신청의 증가가 유실되는 구조적 결함이 있다. 더해서 500개 동시 충돌이면 재시도가 폭증한다 |
  | 분산 락, Redis 원자 카운터 | Phase 5-A에서 제외 | 단일 인스턴스에서 의미가 없고 인프라 추가가 이 이슈 범위를 넘는다 |

- 최종 검증: `invariant-final.txt` - **위반 0건** (I1 0, I2 0, `courses_with_drift` 0)
  - 최종 코드(정리와 테스트 추가 반영) 재기동 후 동일 조건 재측정:
    RPS 260.15001290344065, p99 1851.7725799999998ms, 성공 500/500, 5xx 0
  - `./gradlew test` BUILD SUCCESSFUL

### 운영 반영 시 유의점

| 확인 | 내용 |
|---|---|
| 마이그레이션 | **스키마 변경 없음.** `CHECK (current_enrollment >= 0)`는 보완재로 검토했으나 이번 범위에서 뺐다. 하한은 조건절이 지킨다 |
| 인프라 | **추가 없음** |
| 기존 데이터 | **보정이 필요할 수 있다.** 이 결함은 카운터를 실제보다 **크게** 남기므로, 어긋난 강의는 자리가 비어 있는데도 신청이 막힌 상태다. 아래 조회로 확인하고 한산한 시간대에 보정한다 |
| 인스턴스 수 | **다중 인스턴스에서도 성립한다.** 제어가 DB 단일 문장 안에 있고 애플리케이션 레벨 락이 아니다 |

```sql
-- 어긋난 강의 확인
SELECT c.id, c.current_enrollment,
       (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id) AS actual
FROM courses c
WHERE c.current_enrollment <> (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id);

-- 보정 (한산한 시간대에)
UPDATE courses c
   SET c.current_enrollment = (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id)
 WHERE c.current_enrollment <> (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id);
```

**함께 넘기는 관찰 (이번 범위 밖).** `RestApiException`이 메시지를 담지 않아
`GlobalExceptionHandler`:29의 `log.error("예외 발생: {}", e.getMessage())`가 항상 `null`을 찍는다.
4004가 발생해도 로그에는 `예외 발생: null`만 남는다. 이 설계는 "카운터가 어긋난 상태를
조용히 넘기지 않는다"를 근거로 예외를 택했는데, 현재 로깅으로는 그 신호가 운영자에게 닿지 않는다.
`RestApiException`이 `exceptionCode`를 메시지로 넘기도록 고치면 이 결함 전체의 관측성이 함께 오른다.

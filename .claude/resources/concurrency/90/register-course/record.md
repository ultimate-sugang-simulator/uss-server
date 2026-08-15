# [CONC-90] RegistrationService.registerCourse

> 이슈: #90
> 브랜치: fix/90-registration-concurrency
> 대상 디렉토리: `.claude/resources/concurrency/90/register-course/`

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

- 임계 구역: `RegistrationService.registerCourse` (`src/main/java/uss/code/registration/service/RegistrationService.java`:44-69)
- 공유 자원: `courses` 행의 `current_enrollment` (집계 컬럼), `registrations` 행 (실제 등록)
- read-modify-write 구간: `:54` `courseRepository.findById` → `:58` `validateCourseCapacity`(`:104` `currentEnrollment >= maxCapacity`) → `:66` `course.incrementEnrollment()` → 커밋 시 flush
- 실제 발행 SQL: 더티 체킹. `Course`에 `@DynamicUpdate`가 없어 전 컬럼을 싣는다.
  `UPDATE courses SET academic_year=?, ..., current_enrollment=?, status=? WHERE id=?`
  - 절대값 대입인가 증감식인가: **절대값**. 바인딩되는 값은 트랜잭션이 읽어둔 값 + 1이며 `current_enrollment = current_enrollment + 1`이 아니다
- 트랜잭션 경계: `@Transactional`(`:44`)이 메서드 전체를 감싼다. UPDATE는 커밋 직전 flush에서 나가므로 검사 시점과 쓰기 시점 사이의 창이 트랜잭션 거의 전 구간이다
- 기존 방어 장치

  | 수단 | 유무 | 막아주는 범위 |
  |---|---|---|
  | `@Version` | 없음 (코드베이스 전체에 0건) | - |
  | 비관적 락 | 없음 (락 힌트 0건) | - |
  | UNIQUE 제약 | 있음 `uk_member_course (member_id, course_id)` (`V1_0__init_table.sql`:81) | 같은 회원의 같은 강의 중복 행. 정원 초과와 카운터 유실은 못 막는다 |
  | CHECK 제약 | 없음 (`current_enrollment INT NOT NULL DEFAULT 0`, `V1_0__init_table.sql`:48) | NULL만. 음수와 정원 초과는 못 막는다 |

같은 창을 공유하는 다른 검증 (이번 대상의 불변식에는 넣지 않았다)

| 검증 | 근거 | 상한 |
|---|---|---|
| `validateCreditLimit` | `CourseValidator`:82-94 | `member.maxCredit` |
| `validateCourseTypeLimit` | `CourseValidator`:60-78 | OCU 2개, K-MOOC 1개 |
| `validateCourseScheduleConflict` | `CourseValidator`:22-58 | 시간 겹침 0 |
| `validateDuplicateCourse` | `RegistrationService`:109-119 | 동일 강의 1개 (UNIQUE가 후방 방어) |

## 불변식

> Phase 1에서 호출자와 확정한 것. **후보마다 바꾸지 마라.**
> 검증 SQL은 `register-course/invariant-check.sql`에 저장하고 Phase 4, 7에서 그대로 재사용한다.

전제: **취소를 배제한다.** 이 대상의 부하는 신청 단독이며 `decrementEnrollment()`(`RegistrationService`:82)는 발화하지 않는다.

| # | 불변식 | 검증 방법 | 깨졌을 때의 증상 |
|---|---|---|---|
| I1 | `current_enrollment <= max_capacity` | `GREATEST(0, current_enrollment - max_capacity)` | 집계 인원이 정원을 넘음 |
| I2 | `COUNT(registrations WHERE course_id) <= max_capacity` | `GREATEST(0, COUNT(*) - max_capacity)` | 정원 초과 등록 |
| I3 | `current_enrollment = COUNT(registrations WHERE course_id)` | `ABS(current_enrollment - COUNT(*))` | 갱신 유실 (lost update) |

I1과 I2는 서로 다른 결함을 잡는다. 갱신이 유실되면 정원 100에 200명이 등록되고 `current_enrollment`가 1이 되는데,
이때 I1은 `1 <= 100`으로 통과하고 I2만 깨진다. I3이 그 유실 규모를 직접 센다.

채택하지 않은 축

| 축 | 배제 근거 |
|---|---|
| 응답 코드 정합 (성공 수 = 등록 행 수, 실패는 4000) | 판정 기준에서 제외. k6 요약에 남는 관측값으로만 참고한다 |
| 중복 신청 경합 (행 1개 + 응답 4002) | 부하 형태가 달라 별도 시나리오가 필요. 이번 대상 범위 밖 |
| `current_enrollment >= 0` | 취소를 배제해 깨질 경로가 없다. 취소 대상 슬러그에서 다룬다 |

응답 코드 기대치: 정원 초과 거절은 `COURSE_MAX_CAPACITY_EXCEEDED`(4000, HTTP 400).
`GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러가 없어(`:73` catch-all `Exception`)
제약 위반이 튀면 `UNEXPECTED_SERVER_ERROR`(9999, HTTP 500)로 나간다. 불변식은 아니지만 관측한다.

## 측정 환경

> 값이 바뀌면 그 사실을 후보 기록에 남긴다.

| 항목 | 값 |
|---|---|
| 프로파일 | conc (`application-conc.yml`) |
| DB | MySQL 8.0 / InnoDB (`127.0.0.1:3307`, `uss_db`) |
| 커넥션 풀 크기 | `maximum-pool-size: 500`, `minimum-idle: 500`. 실측 `hikaricp_connections_max=500`, `Threads_connected=501` |
| MySQL `max_connections` | 600 (`SET PERSIST`. 기본 151로는 풀 500을 못 받는다. 되돌리기는 `RESET PERSIST max_connections`) |
| 격리 수준 | `REPEATABLE-READ` (global = session) |
| 락 대기 타임아웃 | `innodb_lock_wait_timeout` 50초, `autocommit=1` |
| 관측 가능한 락 지표 | `Innodb_row_lock%` 5개 / `INNODB_METRICS`의 `lock_deadlocks`, `lock_timeouts`, `lock_row_lock_waits` 모두 `enabled` / `performance_schema.data_locks` 조회 가능 |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고 애플리케이션 캐시는 쓰지 않는다 |

**환경 구성에서 손댄 것.** 동시성 기법이 아니라 측정 환경이므로 모든 후보에서 동일하게 유지한다. 되돌리지 않는다.

| 대상 | 변경 | 이유 |
|---|---|---|
| `DataSourceConfig.java`:17 | `dataSource` 빈에 `@ConfigurationProperties("spring.datasource.hikari")` 추가 | 커스텀 `@Primary` DataSource 빈이 Boot 자동설정을 물러나게 해 `spring.datasource.hikari.*`가 어디에도 바인딩되지 않았다. 실측으로 확인: 설정 200인데 `hikaricp_connections_max=10`(Hikari 기본값). 풀이 VU보다 작으면 락이 아니라 커넥션 대기를 재게 된다 |
| `application-conc.yml` | `management.health.db.enabled: false` | 도달 불가능한 더미 Oracle DataSource에 헬스 프로브가 붙어 `/actuator/health`가 ORA-12541로 DOWN이 된다. DataSource 빈 하나만 골라 끄는 표준 설정이 없어 프로브 자체를 뺐다 |
| MySQL 서버 | `SET PERSIST max_connections = 600` | 기본 151 |
| Docker 볼륨 | `CREATE DATABASE uss_db` | 볼륨이 예전에 `uss_queue_db`로 초기화돼 있어 compose의 `MYSQL_DATABASE`가 먹지 않았다(빈 데이터 디렉토리에서만 1회 작동) |

**측정 전 확인 절차**

- Hikari가 `minimum-idle: 500`을 채우는 데 기동 후 수 초가 걸린다. 실제로 기동 직후 스크랩에서 `hikaricp_connections=164`가 관측됐다.
  **매 측정 직전에 `Threads_connected`가 500 이상인지 확인한다.** 덜 찬 상태로 부하를 주면 커넥션 생성 지연이 락 대기에 섞인다.

**셸 접속 (zsh)**

스킬이 제시하는 `$MYSQL_CONC` 문자열 변수는 zsh에서 동작하지 않는다(zsh는 파라미터 확장 결과를 단어 분할하지 않는다).
호스트에 `mysql` 클라이언트도 없어 컨테이너 경유로 함수를 쓴다.

```bash
mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot uss_db "$@"; }
```

## 경합 조건

> Phase 3에서 확정. **모든 후보가 이 조건을 그대로 쓴다.**

| 항목 | 값 |
|---|---|
| 다투는 자원 | 강의 1개, `courses.id = 990001` (앱 시드 강의는 id 1~2439라 겹치지 않는다) |
| 허용 상한 | 정원 100 |
| 동시 요청 수 (VU) | 500 (상한의 5배. 이슈 완료 기준과 같다) |
| 회원 수 | 500, `members.id` 900001~900500 (VU와 1:1) |
| executor | `per-vu-iterations`, `iterations: 1` (ramp 없음), `maxDuration: 60s` |
| 시드 SQL | `../seeds.sql` (이슈 공용) |
| 시드 모듈과 변수 | `member.sql` + `contention-course.sql`. `@member_start=900001`, `@member_count=500`, `@student_id_start=900000001`, `@target_course_id=990001`, `@target_capacity=100` |
| 측정용 토큰 | `../tokens.json` (서명키로 직접 생성, 로그인 API 미사용) |
| 부하 스크립트 | `burst-script.js`. `POST /api/v1/registration/990001`, 기대 거절 코드 4000 |

**시드 적재 방식이 스킬 기본값과 다르다.** 템플릿은 `seeds.sql`에 `SOURCE` 줄을 두라고 하지만,
`SOURCE`는 mysql 클라이언트가 직접 파일을 여는 명령이라 컨테이너 안 클라이언트가 호스트 경로를 찾지 못한다.
`seeds.sql`에는 변수 블록만 두고, 모듈을 실행 시점에 이어 붙여 한 세션으로 흘려보낸다.

```bash
cat $CONC_DIR/seeds.sql \
    .claude/skills/fix-concurrency/template/seeds/member.sql \
    .claude/skills/fix-concurrency/template/seeds/contention-course.sql \
  | mysqlc -t
```

**요청 하나가 정원 검증까지 도달하는지 확인한 것**

| 검증 | 통과 근거 |
|---|---|
| `validateCourseActive` | 시드가 `status='ACTIVE'` |
| `validateDuplicateCourse`, `uk_member_course` | VU 500 : 회원 500 = 1:1 |
| `validateCreditLimit` | `last_semester_gpa=4.2` → 최대 이수 24학점, 대상 강의 3학점, 기존 등록 0건 |
| `validateCourseScheduleConflict` | 대상 강의에 `course_schedules` 행이 없어 즉시 통과 |
| `validateCourseTypeLimit` | 대상 강의 `type_code='1'`(강의(이론)). OCU·K-MOOC가 아니다 |

**되돌리기 SQL** (매 측정 전에 실행)

```sql
DELETE FROM registrations WHERE course_id = 990001;
UPDATE courses SET current_enrollment = 0, status = 'ACTIVE', max_capacity = 100 WHERE id = 990001;

SELECT (SELECT COUNT(*) FROM registrations WHERE course_id = 990001) AS rows_left,
       (SELECT current_enrollment FROM courses WHERE id = 990001) AS counter;
```

`rows_left`와 `counter`가 모두 0이어야 다음 측정이 성립한다.

## 재현 (Baseline, `-0`)

> 원본: `invariant-0.txt` / `k6-burst-summary-0.json` / `lock-stats-0.txt`
> 여기에는 판정 근거로 쓴 값만 옮긴다.

**정합성**

| 불변식 | 기대 | 실측 | 위반 |
|---|---|---|---|
| I1 `current_enrollment <= max_capacity` | 100 | 2 | 0 |
| I2 `COUNT(registrations) <= max_capacity` | 100 | 3 | 0 |
| I3 `current_enrollment = COUNT(registrations)` | 3 | 2 | 1 |

**응답 분포**

| 결과 | 건수 | 의미 |
|---|---|---|
| 2xx | 3 | 데드락 사이클에서 살아남은 트랜잭션 |
| 4000 정원 마감 | 0 | 정원에 닿기 전에 끝났다 |
| 기타 4xx | 0 | |
| 5xx | 497 | 데드락 피해자. `UNEXPECTED_SERVER_ERROR`(9999) |

요청 500건, RPS 246.52085122663843, p95 1850.62625ms, p99 1887.3802999999998ms.

**경합**

| 지표 | BEFORE | AFTER | 증가분 |
|---|---|---|---|
| `Innodb_row_lock_waits` | 0 | 829 | 829 |
| `Innodb_row_lock_time` | 0 | 142500 | 142500 |
| `Innodb_row_lock_time_avg` | 0 | 171 | 171 |
| `lock_deadlocks` | 0 | 497 | 497 |
| `lock_timeouts` | 0 | 0 | 0 |

**진단**

- 재현 여부: **재현됨** (I3 위반 1건)
- 결함 성격: lost update. 다만 이슈 본문이 예상한 "정원 초과 대량 등록"이 아니라 **FK가 유발한 데드락 폭풍**이 지배적이다
- 근거
  - 데드락 원문의 두 트랜잭션이 발행한 UPDATE가 **둘 다 `current_enrollment=2`** 다. 같은 값을 읽고 같은 값을 절대값으로 썼다는 직접 증거다
  - `lock_deadlocks` 증가분 497 = `failed_5xx` 497. 5xx는 전부 데드락 피해자다
  - I1·I2가 0인 것은 코드가 안전해서가 아니다. InnoDB가 497건을 롤백해 정원 초과가 일어날 기회가 사라진 것이다. **우연한 직렬화이지 정합성이 아니다**
- 추정 인터리빙: 아래가 데드락 원문으로 확인된다

  | 시점 | SQL | 락 |
  |---|---|---|
  | `registrationRepository.save()`(`:68`) | `INSERT INTO registrations`. id가 IDENTITY라 flush를 기다리지 않고 즉시 발행된다 | FK 검사로 `courses#990001`에 **S** (`lock mode S locks rec but not gap`) |
  | 커밋 직전 flush | 더티 체킹의 `UPDATE courses SET ... WHERE id=990001` | 같은 행에 **X** 요청 (`lock_mode X locks rec but not gap waiting`) |

  소스 순서는 `incrementEnrollment()`(`:66`)가 `save()`(`:68`)보다 앞이지만 실제 SQL 순서는 반대다.
  S는 공유 락이라 다수가 동시에 쥐고, X는 그 전원이 놓기를 기다린다. 전원이 S를 쥔 채 X를 기다려 순환 대기가 된다.

- 파생 관측: 데드락 피해자가 받는 `DeadlockLoserDataAccessException`을 처리하는 핸들러가 없어
  (`GlobalExceptionHandler`:73 catch-all) `UNEXPECTED_SERVER_ERROR`(9999, 500)로 나간다.
  불변식으로 채택하지 않았지만 후보 판정 시 응답 품질로 본다.

- 측정 명령 정정: 스킬이 제시한 `sed -n '/LATEST DETECTED DEADLOCK/,/^---/p'`는 헤더 바로 아래 구분선에서 멈춰
  본문을 못 잡는다. `sed -n '/LATEST DETECTED DEADLOCK/,/^TRANSACTIONS/p'`로 재수집해 `lock-stats-0.txt` 말미에 덧붙였다.

- 동시성 상한 보정: 앱의 동시 처리 상한은 커넥션 풀 500이 아니라 **Tomcat 워커 스레드 200**(Spring Boot 기본값,
  이 프로젝트에 설정 없음)이다. VU 500 중 동시에 도는 것은 최대 200이고 나머지는 accept 큐에서 기다린다.
  정원 100 < 동시 200이라 경합 재현에는 충분했고(데드락 497건), 모든 후보에 같은 조건이 걸리므로 비교는 성립한다.
  이슈 완료 기준의 "동시 500"을 문자대로 맞추려면 `server.tomcat.threads.max`를 올리고 `-0`을 다시 재야 하므로 200을 유지하기로 했다.

---

## 후보 1: 원자적 조건부 UPDATE

### 설계 결정

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 조건절 | `WHERE id = :courseId AND current_enrollment < max_capacity` | 술어를 하나만 두어 영향 행 수 0의 원인을 '정원 마감'으로 고정한다. `findById`(`:54`)가 앞서 행의 존재를 확정하므로 다른 원인이 없다 |
| 갱신식 | `SET current_enrollment = current_enrollment + 1` | 증감식이라 읽은 값이 개입하지 않는다. I3(갱신 유실)이 원천적으로 불가능해진다 |
| 0행일 때 예외 | `COURSE_MAX_CAPACITY_EXCEEDED`(4000, HTTP 400) | 같은 사유에 이미 코드가 있다. 새로 만들면 한 사유에 두 코드가 생긴다 |
| 갱신 위치 | `CourseRepository`의 `@Modifying` 쿼리 | JPA에서 원자적 조건부 UPDATE를 표현할 수단이 이것뿐이다 |
| 도메인 사전 검사 `validateCourseCapacity`(`:58`) | **제거** | 집행 지점을 UPDATE 하나로 모은다. 규칙을 두 곳에 적어 동기화 책임을 만드는 대신, 정원 규칙이 SQL에만 남는 비용을 감수한다 |
| `course.incrementEnrollment()`(`:66`) | 신청 경로에서 호출 제거 (메서드는 유지) | 남기면 더티 체킹 UPDATE가 방금 원자적으로 올린 값을 낡은 절대값으로 덮어쓴다. 메서드 자체는 취소 경로가 쓴다 |
| 발행 순서 | 조건부 UPDATE → `registrationRepository.save()` | Phase 4에서 데드락 497건의 원인이 INSERT(S) → UPDATE(X) 업그레이드였다. UPDATE를 먼저 내면 X를 선점해 업그레이드가 사라진다. 이 기법이 성립하기 위한 조건이지 선택지가 아니다 |

- 검토했지만 택하지 않은 안
  - 조건절에 `status = 'ACTIVE'` 포함 — 검증과 갱신 사이에 강의가 닫히는 경우까지 막지만, 0행의 원인이 둘로 갈려 실패 사유를 구분할 수 없게 된다
  - 사전 검사 유지 — 정원이 찬 뒤 들어온 요청이 UPDATE 없이 거절되어 X 락 경합이 줄지만, 정원 규칙이 도메인과 SQL 두 곳에 적혀 동기화 책임이 생긴다
- 호출자가 예상한 결과
  - 정합성: I1·I2·I3 모두 위반 0. 증감식이라 갱신 유실이 불가능하고, 조건절이 정원을 원자적으로 판정한다
  - 경합: **RPS 하락, p99 상승.** 동시성 제어는 처리량을 깎으며, 한 행에 X 락이 집중되어 직렬화 비용이 드러날 것이다

### 적용 내용

| 파일 | 변경 |
|---|---|
| `course/repository/CourseRepository.java` | `increaseEnrollmentWithinCapacity(id)` 추가. `@Modifying(flushAutomatically = true)` + JPQL `UPDATE Course c SET c.currentEnrollment = c.currentEnrollment + 1 WHERE c.id = :id AND c.currentEnrollment < c.maxCapacity`, 영향 행 수를 `int`로 반환 |
| `registration/service/RegistrationService.java` | `validateCourseCapacity` 제거 → `increaseEnrollment(courseId)` 신설. 영향 행 수 0이면 `COURSE_MAX_CAPACITY_EXCEEDED`. `course.incrementEnrollment()` 호출 제거. 조건부 UPDATE를 `registrationRepository.save()`보다 먼저 발행 |

- `clearAutomatically`는 켜지 않았다. 영속성 컨텍스트를 비우면 앞서 조회한 `member`·`course`가 준영속이 되어
  `Registration.create(member, course)`가 준영속 참조를 들게 된다.
- 취소 경로(`deleteRegisteredCourse`의 `decrementEnrollment()`)는 손대지 않았다. 이번 대상의 불변식이 취소를 배제한다.
- 추가한 마이그레이션: 없음 (스키마 변경 없음)

**제어 발행 확인** — `performance_schema.events_statements_summary_by_digest`

```
UPDATE `courses` `c1_0` SET `current_enrollment` = ( `c1_0`.`current_enrollment` + ? )
 WHERE `c1_0`.`id` = ? AND `c1_0`.`current_enrollment` < `c1_0`.`max_capacity`
```

증감식이며 조건절에 정원 술어가 있다. 전 컬럼을 나열하던 더티 체킹 절대값 UPDATE는 발행되지 않는다.

**테스트** — `./gradlew test` 282건 통과 (실패 0, 에러 0)

### 검증 결과

**정합성 (1급)**

| 불변식 | 원본(`-0`) 위반 | 후보(`-1`) 위반 | 판정 |
|---|---|---|---|
| I1 | 0 | 0 | 통과 |
| I2 | 0 | 0 | 통과 |
| I3 | 1 | 0 | 통과 |

판정: **정합성 확보.** `current_enrollment` 100 = 등록 행 100 = 정원 100. 카운터가 어긋난 강의 수 1 → 0.

`-0`의 I1·I2가 0이었던 것은 데드락이 497건을 죽여 정원 초과가 일어날 기회가 없었기 때문이다.
후보 1은 100건이 실제로 통과한 뒤 상한에서 멈췄다. 같은 0이지만 의미가 다르다.

**경합 (2급)**

| 지표 | 원본(`-0`) | 후보(`-1`) | 변화 |
|---|---|---|---|
| 성공 요청 수 | 3 | 100 | 정원과 정확히 일치 |
| 기대 거절(4000) | 0 | 400 | 나머지 전부 의도한 코드 |
| 5xx | 497 | 0 | 소멸 |
| RPS | 246.52085122663843 | 356.6593289954048 | +44.7% |
| p95 | 1850.62625 | 1319.5252 | -28.7% |
| p99 | 1887.3802999999998 | 1331.76286 | -29.4% |
| `Innodb_row_lock_waits` 증가분 | 829 | 499 | -330 |
| `Innodb_row_lock_time` 증가분 | 142500 | 136459 | -6041 |
| `lock_deadlocks` 증가분 | 497 | 0 | 소멸 |
| `lock_timeouts` 증가분 | 0 | 0 | 없음 |
| 재시도 횟수 | 해당 없음 | 해당 없음 | 낙관적 락이 아니다 |

- 응답 코드 품질: 거절 400건이 전부 `COURSE_MAX_CAPACITY_EXCEEDED`(4000, HTTP 400). 500 없음
- `lock-stats-1.txt`의 `LATEST DETECTED DEADLOCK`은 `2026-08-14 20:19:37`로 baseline 때 원문이 남은 것이다.
  이번 부하의 데드락 증가분은 0이다
- 예상과의 대조: 호출자의 예상("RPS 하락, p99 상승")은 **어긋났다.** 처리량이 44.7% 오르고 p99가 29.4% 내렸다.
  틀린 가정은 "baseline이 제어 없는 빠른 경로"라는 전제다. `-0`의 497건은 락 대기(회당 평균 142500/829 = 171ms)와
  데드락 감지, 롤백, 500 응답 경로를 모두 거친 뒤 실패했다.
  후보 1은 회당 대기가 오히려 길지만(136459/499 = 약 273ms) 대기 횟수가 830에서 499로 줄고 롤백이 0이 되어
  부하 전체 소요가 2.028초에서 1.402초로 짧아졌다.
  이 대상에서는 비교 상대가 정상 경로가 아니라 데드락 폭풍이었으므로, 정합성과 처리량을 함께 얻었다

### 되돌리기

- stash: `stash@{0}: On fix/90-registration-concurrency: candidate-1-atomic-conditional-update`
  - 후보 코드 두 파일만 경로 지정으로 stash했다. `DataSourceConfig.java`(환경 수정)와 측정 산출물은 남겼다.
    `-0`을 쟀을 때와 같은 작업 트리 상태여야 다음 후보가 같은 조건에서 측정된다
- 스키마 되돌리기: 해당 없음 (스키마 변경 없음)
- 데이터 복원 확인: `rows_left = 0`, `counter = 0`
- Phase 8에서 `git stash apply stash@{0}`으로 되살렸다. stash는 기록으로 남긴다

---

## 최종 요약

### 후보 비교

| # | 기법 | 불변식 위반 | 성공 요청 수 | RPS | p99 | 락 대기 증가분 | 데드락 증가분 | 응답 코드 |
|---|---|---|---|---|---|---|---|---|
| 0 | (원본, 제어 없음) | I3 = 1 | 3 | 246.52085122663843 | 1887.3802999999998 | 829 | 497 | 5xx 497건 |
| 1 | 원자적 조건부 UPDATE | 0 | 100 (정원과 일치) | 356.6593289954048 | 1331.76286 | 499 | 0 | 4000 400건, 5xx 0 |

후보를 하나만 쟀으므로 이 표의 변별력은 원본 대비까지다.
B(비관적 락)와 C(낙관적 락)는 측정하지 않았으므로 "다른 기법보다 낫다"는 근거는 없다.
있는 근거는 "원본의 결함을 없앴고 그 대가가 음수였다"이다.

처리량 수치는 로컬 측정이므로 절대값으로 단정하지 않는다. 원본 대비 상대 변화로만 쓴다.

### 채택

- 채택한 기법: **원자적 조건부 UPDATE** (후보 1)
- 채택 근거: 호출자가 다섯 축을 모두 근거로 삼았다

  | 축 | 후보 1의 위치 |
  |---|---|
  | 정합성 | I1·I2·I3 전부 위반 0. 등록 100 = 카운터 100 = 정원 100 |
  | 처리량 대가 | 대가가 없다. RPS +44.7%, p99 -29.4% |
  | 실패 응답 품질 | 거절 400건 전부 `COURSE_MAX_CAPACITY_EXCEEDED`(4000). 5xx 497 → 0 |
  | 운영 비용 | 스키마 변경 없음, 인프라 추가 없음 |
  | 코드 위치 | 정원 규칙이 SQL로 내려간다. **유일하게 손해인 축** |

  마지막 축(도메인 로직의 DB 침투)은 호출자가 후보 선택 단계에서 이미 최대 단점으로 지목한 항목이다.
  그럼에도 채택한 이유는 나머지 네 축에서 얻은 것이 그 비용을 넘어선다고 판단했기 때문이다.
  함께 지목했던 "상세한 실패 원인 파악 불가"는 이 흐름에서 성립하지 않는 것으로 확인됐다.
  `findById`(`RegistrationService`:54)가 앞서 행의 존재를 확정하고 조건절에 술어를 하나만 두었으므로,
  영향 행 수 0의 원인은 정원 마감 하나뿐이다.

- 배제한 후보와 이유
  - B 비관적 락 — 측정하지 않았다. 후보 1이 다섯 축 모두에서 만족스러워 비교를 종료하기로 했다.
    미측정이므로 "B보다 낫다"고 적을 근거는 없다
  - C 낙관적 락 — 측정하지 않았다. 버전 UPDATE도 커밋 시점에 나가 INSERT(S) → UPDATE(X) 순서가 바뀌지 않으므로
    Phase 4에서 관측한 데드락이 그대로 남을 것으로 예상됐고, 재시도가 시도 횟수를 늘려 악화시킬 여지가 있었다.
    이는 예상이지 측정이 아니다
  - D DB CHECK 제약 — 단독으로는 후보가 될 수 없다. 사용자에게 돌려줄 응답이 없다
  - 분산 락, Redis 원자 카운터 — 단일 인스턴스이고 `build.gradle`에 Redis 의존성이 없다. 인프라를 새로 들여야 성립한다

- 최종 검증: `invariant-final.txt` — I1·I2·I3 위반 **0건**.
  `current_enrollment` 100 = 등록 행 100 = 정원 100, 카운터가 어긋난 강의 0.
  `k6-burst-summary-final.json` — 성공 100, 기대 거절 400, 5xx 0, RPS 368.3884405601567, p99 1294.42481.
  되살린 코드는 `git diff stash@{0}` 결과가 비어 stash와 동일하다.
  `./gradlew test`는 입력이 Phase 6 실행과 동일해 Gradle이 UP-TO-DATE로 건너뛰었다(282건 통과 결과가 유효).

### 운영 반영 시 유의점

| 확인 | 내용 |
|---|---|
| 마이그레이션 | **스키마 변경 없음.** 추가한 Flyway 파일 없음 |
| 인프라 | 추가 없음 |
| 기존 데이터 | 운영에 이미 어긋난 카운터가 있으면 보정이 필요하다. `UPDATE courses c SET c.current_enrollment = (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id)` 형태로 실제 행 수에 맞춘다. 어긋난 행은 `invariant-check.sql`의 `ref. 카운터가 어긋난 강의 수`로 센다 |
| 인스턴스 수 | **다중 인스턴스에서도 성립한다.** 제어가 DB 한 문장 안에 있어 애플리케이션 인스턴스 수와 무관하다 |
| 커넥션 풀 | `application-prod.yml`에 hikari 블록이 없어 운영 풀이 기본값 10이다. Tomcat 스레드 200과 어긋나므로, 경합이 몰리면 락 대기가 커넥션 대기로 전이되어 30초 뒤 500이 난다. 이번 변경으로 임계 구역이 짧아져 완화되지만 설정 자체는 별도로 손봐야 한다 |
| 남은 결함 | 취소 경로(`deleteRegisteredCourse`의 `decrementEnrollment()`)는 여전히 절대값 UPDATE다. 중복 신청 경합의 `DataIntegrityViolationException`도 여전히 500으로 나간다. 둘 다 이 대상의 범위 밖이며 이슈 #90의 나머지 완료 기준이다 |

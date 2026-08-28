# 시드 모듈

성능 측정용 더미 데이터를 도메인 단위로 모듈화해 둔 것이다. **규모와 카디널리티는 변수로만 정한다.** 모듈 본문은 고치지 않는다.

## 쓰는 법

`.claude/resources/perf/{이슈번호}/seeds.sql`에 **변수 블록만** 쓴다.

```sql
-- PERF-{이슈번호} 시드
-- 대상: {측정할 엔드포인트들}

-- 재귀 CTE 기본 깊이 상한은 1000이다. 안 올리면 1000행에서 끊긴다.
SET SESSION cte_max_recursion_depth = 10000000;

-- 회원
SET @member_start      = 900001;
SET @member_count      = 1000;
SET @student_id_start  = 200000001;
SET @pw_hash           = 'perf-not-a-real-hash';
SET @member_dept_count = 5;

-- 강의. @course_start는 SELECT MAX(id) FROM courses; 보다 커야 한다. 겹치면 시드 정리가 실제 강의를 지운다.
SET @course_start         = 1000001;
SET @course_count         = 20000;
SET @course_dept_count    = 20;
SET @course_area_count    = 8;
SET @schedules_per_course = 2;
SET @course_term_year     = 2026;
SET @course_term          = 'FIRST';
SET @course_title_mode    = 'plain';

-- 장바구니, 수강신청
SET @cart_per_member         = 8;
SET @registration_per_member = 6;
```

실행은 호출자가 레포 루트에서 한다. 변수 블록과 모듈을 호스트에서 이어 붙여 표준입력으로 넘긴다.
`cat` 순서가 곧 FK 의존 순서다. 필요 없는 모듈은 뺀다.

```bash
cat $PERF_DIR/seeds.sql $SEEDS/member.sql $SEEDS/course.sql $SEEDS/enrollment.sql | mysqlp
```

`SOURCE`는 쓰지 마라. mysql 클라이언트가 컨테이너 안에서 돌아 호스트 경로를 못 본다.

## 모듈

| 모듈 | 채우는 테이블 | 선행 모듈 |
|---|---|---|
| `member.sql` | `members` | 없음 |
| `course.sql` | `courses`, `course_schedules` | 없음 |
| `enrollment.sql` | `carts`, `registrations` (+ `courses.current_enrollment` 동기화) | `member`, `course` |

## 변수

모듈은 MySQL 사용자 변수만 읽는다. 정의하지 않은 변수는 `NULL`이 되고 `NOT NULL` 컬럼에서 에러로 죽는다. 조용히 0건이 되지는 않는다.
`ELT` 목록으로 카디널리티를 만드는 변수는 **목록 길이가 상한**이다. 넘기면 `NULL`이 들어가 죽는다.

| 변수 | 모듈 | 뜻 | 상한 |
|---|---|---|---|
| `@member_start` | member, enrollment | 시드 회원 id 시작값 | |
| `@member_count` | member, enrollment | 시드 회원 수. k6의 `USER_COUNT`, `mint-tokens.sh`의 `--count`와 맞춘다 | |
| `@student_id_start` | member | 학번 시작값 (영숫자 1~20자) | |
| `@pw_hash` | member | 회원 전원이 공유할 `password` 값. 토큰을 `mint-tokens.sh`로 만들면 로그인을 안 타므로 아무 문자열이나 된다. **로그인 경로를 측정할 때만** Phase 3-A에서 가입 API로 뽑은 BCrypt 해시를 넣는다 | |
| `@member_dept_count` | member | 회원이 퍼질 학과 수 | 5 |
| `@course_start` | course, enrollment | 시드 강의 id 시작값 | |
| `@course_count` | course, enrollment | 시드 강의 수 | |
| `@course_dept_count` | course | 강의가 퍼질 학과 수. 앞 5개가 member의 학과와 같아 전공 조회가 0건이 되지 않는다 | 20 |
| `@course_area_count` | course | 강의가 퍼질 영역 수. 앞 3개는 전공 영역, 4번째부터 교양 영역이다. 교양 조회를 재면 4 이상 | 8 |
| `@schedules_per_course` | course | 강의당 시간표 행 수 | |
| `@course_term_year` | course | 학년도. `uk_year_term_haksu`의 첫 컬럼 | |
| `@course_term` | course | 학기. `CourseTerm` 상수 (`FIRST`, `SECOND`, `SUMMER`, `WINTER`) | |
| `@course_title_mode` | course | `plain`이면 `성능측정강의{n}`, `search`면 아래 **검색 제목 설계**의 조합 제목 | |
| `@cart_per_member` | enrollment | 회원당 장바구니 강의 수. 담기 상한이 10이라 넘기면 담기 API가 항상 실패한다 | 10 |
| `@registration_per_member` | enrollment | 회원당 수강신청 강의 수 | |

**쓰기 엔드포인트를 측정할 때.** `POST /carts/{courseId}`나 `POST /registration/{courseId}`가 대상이면 부하가 담으려는 강의를
미리 채워두면 전부 중복 실패다. `@cart_per_member` 또는 `@registration_per_member`를 0으로 둔다.

## 검색 제목 설계 (`@course_title_mode = 'search'`)

ngram 파서 + BOOLEAN MODE는 검색어를 OR 합집합이 아니라 **구(phrase) 검색**으로 다룬다.
`plain` 제목은 전 행이 같은 바이그램을 공유해 키워드 하나가 전 행에 걸리거나 0건이 된다.

`search` 모드는 제목을 `{접두 40종}{접미 25종}({n})`으로 만든다. 접두는 `n % 40`, 접미는 `FLOOR(n / 40) % 25`라 두 축이 독립이고,
연속한 1000개 n마다 1000개 조합이 한 번씩 나온다. 조합어 하나(`컴퓨터공학`)는 그 조합 행만 매칭하므로

    조합당 매칭 건수 = @course_count / 1000

k6 키워드 풀에는 조합어를 넣는다. 접두나 접미 하나만 넣으면 부분 일치가 되어 선택도가 통제되지 않는다.
매칭 건수를 바꾸려면 `@course_count`를 바꾼다.

## FULLTEXT 커버리지

`course.sql`의 검증 쿼리 `fts_indexed`가 `rows_seeded`와 같아야 한다. 적으면 인덱스에서 빠진 행이 있는 것이다.
2026-08-28 실측에서 2,000행 대량 INSERT 직후 앞쪽 19행이 색인되지 않은 채 남은 적이 있다(`OPTIMIZE TABLE`로도 안 돌아옴, 재현은 간헐적).
빠진 행은 검색에 안 잡히므로 선택도가 조용히 틀린다. 어긋나면 아래 **대량 적재**의 인덱스 재생성을 실행한다.

## 규모 상한과 대량 적재

모듈은 재귀 CTE로 행을 만든다. **수십만 행까지**가 편한 범위다. 그 이상이면 아래를 함께 한다.

- `courses`의 FULLTEXT(ngram) 인덱스는 행마다 증분 유지되어 적재를 몇 배 느리게 한다. 떼고 넣고 다시 만든다.
  인덱스가 없는 동안 `MATCH ... AGAINST`는 에러(애플리케이션은 500)이므로 재생성 전에 대상 API를 호출하지 마라.

  ```bash
  mysqlp -e "ALTER TABLE courses DROP INDEX ft_idx_course_search;"
  cat $PERF_DIR/seeds.sql $SEEDS/course.sql | mysqlp
  mysqlp -e "ALTER TABLE courses ADD FULLTEXT INDEX ft_idx_course_search (course_code, haksu_code, title_kr, title_en) WITH PARSER ngram;"
  ```

- 인덱스 재생성은 규모와 무관하게 **FULLTEXT 커버리지**가 어긋났을 때의 복구 수단이기도 하다. `ALTER ... DROP INDEX`와 `ADD FULLTEXT INDEX`만 실행하면 된다.
- 적재 세션에서 `SET SESSION unique_checks = 0; SET SESSION foreign_key_checks = 0;`를 변수 블록에 넣으면 빨라진다.
  전역 설정(`innodb_flush_log_at_trx_commit`)은 건드리지 마라. 되돌리지 않으면 측정이 운영과 다른 내구성에서 돈다.
- 데이터가 InnoDB 버퍼 풀(기본 128MiB)보다 커지면 측정이 디스크 I/O를 잰다. 규모를 키우기 전에 그 사실을 Phase 3에서 확정한다.

## 모듈을 고칠 때

- **실제 스키마와 먼저 대조하라.** 마이그레이션이 추가돼도 모듈은 따라오지 않는다. 아래로 NOT NULL·기본값 없음 컬럼을 뽑아
  모듈의 INSERT 컬럼 목록과 맞춘다.

  ```bash
  mysqlp -e "
  SELECT TABLE_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) AS required_columns
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'uss_db'
    AND TABLE_NAME IN ('members', 'courses', 'course_schedules', 'carts', 'registrations')
    AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT IS NULL AND EXTRA NOT LIKE '%auto_increment%'
  GROUP BY TABLE_NAME;"
  ```

- **enum 컬럼에는 실재하는 상수명만.** 애플리케이션이 `valueOf`로 파싱하므로 없는 값은 조회 시점에 500이다. DB는 `VARCHAR`라 들어갈 때는 통과한다.
  기준은 `src/main/java/uss/code/course/domain/`과 `member/domain/`의 enum 파일이다.
  `courses`는 enum 컬럼(`college`, `department`, `area`, `term`, `status`)과 `{*_code, *_name}` String 쌍이 섞여 있다.
- 값이 실제 강의처럼 보일 필요는 없다. FK, 개수, 카디널리티만 맞으면 된다.
- 재실행해도 중복이 쌓이지 않아야 한다. `INSERT IGNORE`와 UNIQUE 제약이 그 장치다.
- 카디널리티가 걸린 컬럼은 `ELT(1 + (n % @변수), ...)`로 서로 다른 값의 개수를 명시적으로 통제한다.
- 새 도메인은 새 모듈 파일로 만들고 위 표에 추가한다. 검증 쿼리는 모듈 말미에 두고 행 수와 카디널리티를 뽑는다.
- 고친 모듈은 커밋 전에 한 번 실행한다.

## 시드를 지울 때

id를 명시 삽입하므로 범위로 지운다. FK가 `ON DELETE CASCADE`라 `members`와 `courses`만 지우면 나머지가 따라 지워진다.

```sql
DELETE FROM members WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;
DELETE FROM courses WHERE id BETWEEN @course_start AND @course_start + @course_count - 1;
```

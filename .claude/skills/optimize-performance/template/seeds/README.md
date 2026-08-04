# 시드 모듈

성능 측정용 더미 데이터를 도메인 단위로 모듈화해 둔 것이다.
**규모만 정하면 바로 쓸 수 있어야 한다.** 매번 INSERT 문을 새로 쓰지 마라.

## 쓰는 법

`.claude/resources/perf/{이슈번호}/seeds.sql`을 아래 형태로 만든다.
변수 블록과 `SOURCE` 줄만 있으면 된다. **모듈 본문을 복사하지 마라.**

```sql
-- PERF-{이슈번호} 시드
-- 대상: {측정할 엔드포인트들}

-- MySQL의 재귀 CTE 기본 깊이 상한은 1000이다. 이걸 안 올리면 1000행에서 끊긴다.
SET SESSION cte_max_recursion_depth = 10000000;

-- 회원
SET @member_start      = 900001;
SET @member_count      = 1000;
SET @student_id_start  = 200000001;
SET @pw_hash           = '{Phase 3-A에서 뽑은 BCrypt 해시를 그대로 붙여넣는다}';
SET @member_dept_count = 5;

-- 강의
SET @course_start        = 900001;
SET @course_count        = 20000;
SET @course_dept_count   = 20;
SET @course_area_count   = 10;
SET @schedules_per_course = 2;

-- 장바구니, 수강신청
SET @cart_per_member         = 8;
SET @registration_per_member = 6;

SOURCE .claude/skills/optimize-performance/template/seeds/member.sql
SOURCE .claude/skills/optimize-performance/template/seeds/course.sql
SOURCE .claude/skills/optimize-performance/template/seeds/enrollment.sql
```

실행은 호출자가 프로젝트 루트에서 한다.

```bash
$MYSQL_PERF < $PERF_DIR/seeds.sql
```

> `SOURCE`는 mysql 클라이언트의 명령이라 **경로가 실행 위치 기준**이다.
> 반드시 프로젝트 루트에서 실행해야 한다. `-e "SOURCE ..."` 형태로는 동작하지 않으므로
> 위처럼 파일을 표준입력으로 넘긴다.

## 모듈

| 모듈 | 채우는 테이블 | 선행 모듈 |
|---|---|---|
| `member.sql` | `members` | 없음 |
| `course.sql` | `courses`, `course_schedules` | 없음 |
| `enrollment.sql` | `carts`, `registrations` | `member`, `course` |

`SOURCE` 순서가 곧 FK 의존 순서다. 표의 위에서 아래로 부른다.

## 변수

모듈은 MySQL 사용자 변수만 읽는다. 정의하지 않은 변수는 `NULL`이 되고,
`NOT NULL` 컬럼에 들어가면서 에러로 죽는다. 조용히 0건이 들어가지는 않는다.

| 변수 | 쓰는 모듈 | 뜻 |
|---|---|---|
| `@member_start` | member, enrollment | 시드 회원 id 시작값 |
| `@member_count` | member, enrollment | 시드 회원 수. k6 스크립트의 `USER_COUNT`와 일치시킨다 |
| `@student_id_start` | member | 학번 시작값. **9자리 숫자여야 로그인이 된다.** k6의 `STUDENT_ID_START`와 일치시킨다 |
| `@pw_hash` | member | 회원 전원이 공유할 BCrypt 해시. Phase 3-A에서 실제 가입 API로 뽑은 값을 쓴다 |
| `@member_dept_count` | member | 회원이 퍼질 학과 수 (전공 조회의 카디널리티) |
| `@course_start` | course, enrollment | 시드 강의 id 시작값 |
| `@course_count` | course, enrollment | 시드 강의 수 |
| `@course_dept_count` | course | 강의가 퍼질 학과 수 |
| `@course_area_count` | course | 강의가 퍼질 교양 영역 수 |
| `@schedules_per_course` | course | 강의당 시간표 행 수 |
| `@cart_per_member` | enrollment | 회원당 장바구니 강의 수. **10을 넘기지 마라** (담기 상한이 10이라 실제로 도달 불가능한 상태가 된다) |
| `@registration_per_member` | enrollment | 회원당 수강신청 강의 수 |

## 모듈을 고칠 때

- **규모는 변수로만 조절한다.** 특정 이슈의 숫자를 모듈 본문에 박지 마라.
- 값이 실제 강의처럼 보일 필요는 없다. FK 관계와 개수, 카디널리티만 맞으면 된다.
- **enum 컬럼에는 반드시 실재하는 enum 상수명을 넣어라.** 애플리케이션이 `valueOf`로 파싱하므로
  없는 값이 들어가면 조회 시점에 500이 난다. DB에는 `VARCHAR(50)`이라 들어갈 때는 통과한다.
  값 목록은 `src/main/java/uss/code/course/domain/`과 `member/domain/`의 enum 파일이 기준이다.
- 모든 모듈은 **재실행해도 중복이 쌓이지 않아야 한다.** `INSERT IGNORE`와 UNIQUE 제약이 그 장치다. 지우지 마라.
- 카디널리티가 걸린 컬럼은 `ELT(1 + (n % @변수), ...)` 형태로 서로 다른 값의 개수를 명시적으로 통제한다.
  한 값에 전 행이 몰리거나 전 행이 서로 다른 값을 갖게 두지 마라.
- 새 도메인이 필요하면 새 모듈 파일을 만들고 이 표에 추가한다. 기존 모듈에 덧붙이지 마라.
- 검증 쿼리는 모듈 말미에 둔다. 행 수와 함께 카디널리티를 반드시 뽑는다.

## 시드를 지울 때

시드 회원과 강의는 id를 명시 삽입하므로 범위로 지울 수 있다. FK가 `ON DELETE CASCADE`라
`members`와 `courses`만 지우면 `carts`, `registrations`, `course_schedules`가 함께 사라진다.

```sql
DELETE FROM members WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;
DELETE FROM courses WHERE id BETWEEN @course_start AND @course_start + @course_count - 1;
```

앱 시드(`database/seed/`의 실제 강의 데이터)와 id가 겹치지 않도록 `@course_start`를 충분히 크게 잡는다.
겹치면 위 DELETE가 실제 강의 데이터를 지운다.

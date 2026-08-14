# 시드 모듈 (동시성 측정용)

동시성 측정에 필요한 것은 **많은 데이터가 아니라 하나의 뜨거운 자원과 그것을 다툴 회원들**이다.
규모를 키우는 것이 목적인 optimize-performance의 시드와 목적이 다르다.

> **optimize-performance의 시드 모듈을 가져다 쓰지 마라.**
> 그 모듈은 `members`에 `email`, `password`, `member_college` 컬럼이 있던 시절에 작성되어
> 현재 스키마(`database/migration/V1_0__init_table.sql`)와 맞지 않는다. 적재하면 실패한다.

## 쓰는 법

`.claude/resources/concurrency/{이슈번호}/seeds.sql`을 아래 형태로 만든다.
변수 블록과 `SOURCE` 줄만 있으면 된다. **모듈 본문을 복사하지 마라.**

```sql
-- CONC-{이슈번호} 시드
-- 대상: {측정할 임계 구역}

-- MySQL의 재귀 CTE 기본 깊이 상한은 1000이다. VU가 1000을 넘으면 이걸 올려야 한다.
SET SESSION cte_max_recursion_depth = 100000;

-- 회원: VU 수와 반드시 같아야 한다.
SET @member_start     = 900001;
SET @member_count     = 500;
SET @student_id_start = 900000001;

-- 경합 대상 강의: 딱 하나만 만든다.
SET @target_course_id = 990001;
SET @target_capacity  = 100;

SOURCE .claude/skills/fix-concurrency/template/seeds/member.sql
SOURCE .claude/skills/fix-concurrency/template/seeds/contention-course.sql
```

실행은 호출자가 프로젝트 루트에서 한다.

```bash
$MYSQL_CONC < $CONC_DIR/seeds.sql
```

> `SOURCE`는 mysql 클라이언트의 명령이라 **경로가 실행 위치 기준**이다.
> 반드시 프로젝트 루트에서 실행해야 한다. `-e "SOURCE ..."` 형태로는 동작하지 않으므로
> 위처럼 파일을 표준입력으로 넘긴다.

## 모듈

| 모듈 | 채우는 테이블 | 선행 모듈 |
|---|---|---|
| `member.sql` | `members` | 없음 |
| `contention-course.sql` | `courses` | 없음 |

## 변수

모듈은 MySQL 사용자 변수만 읽는다. 정의하지 않은 변수는 `NULL`이 되고,
`NOT NULL` 컬럼에 들어가면서 에러로 죽는다. 조용히 0건이 들어가지는 않는다.

| 변수 | 쓰는 모듈 | 뜻 |
|---|---|---|
| `@member_start` | member | 시드 회원 id 시작값. `mint-tokens.sh`의 `--start`와 같아야 한다 |
| `@member_count` | member | 시드 회원 수. **VU 수와 반드시 같다** |
| `@student_id_start` | member | 학번 시작값. 9자리 숫자로 둔다 |
| `@target_course_id` | contention-course | 경합 대상 강의 id. 앱 시드와 겹치지 않게 크게 잡는다 |
| `@target_capacity` | contention-course | 대상 강의의 정원. VU의 1/3 ~ 1/5로 잡는다 |

## 설계 규칙

- **회원 수 = VU 수.** 어긋나면 같은 회원이 두 번 신청하게 되고, 그 실패가 위반 건수를 왜곡한다.
- **경합 대상은 강의 하나다.** 여러 개로 흩으면 행마다 경합이 옅어져 결함이 재현되지 않는다.
- **회원은 학점 상한에 걸리지 않게 만든다.** `last_semester_gpa`를 4.2로 고정해 최대 이수 학점을 24로 둔다.
  (`Member.getMaxCredit()`: 4.0 이상 24 / 3.5 이상 21 / 그 외 19)
- **대상 강의의 `type_code`를 OCU, K-MOOC가 아닌 값으로 둔다.** 그 둘은 별도 개수 상한이 걸려
  정원이 아니라 유형 상한에 먼저 막힌다. `'1'`(강의(이론))이 안전하다.
- **`status`는 `ACTIVE`여야 한다.** 아니면 `validateCourseActive`에서 먼저 걸린다.
- **enum 컬럼에는 실재하는 enum 상수명을 넣어라.** 애플리케이션이 `valueOf`로 파싱하므로
  없는 값이 들어가면 조회 시점에 500이 난다. DB에는 `VARCHAR(50)`이라 들어갈 때는 통과한다.
  값 목록은 `src/main/java/uss/code/course/domain/`과 `member/domain/`의 enum 파일이 기준이다.
- 모든 모듈은 **재실행해도 중복이 쌓이지 않아야 한다.** `INSERT IGNORE`와 UNIQUE 제약이 그 장치다. 지우지 마라.
- 검증 쿼리는 모듈 말미에 둔다. 행 수와 함께 실제로 들어간 값을 뽑는다.

## 시드를 지울 때

시드 회원과 강의는 id를 명시 삽입하므로 범위로 지울 수 있다. FK가 `ON DELETE CASCADE`라
`members`와 `courses`만 지우면 `registrations`, `carts`, `course_schedules`가 함께 사라진다.

```sql
DELETE FROM members WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;
DELETE FROM courses WHERE id = @target_course_id;
```

앱 시드(`database/seed/`의 실제 강의 데이터)와 id가 겹치지 않도록 `@target_course_id`를 충분히 크게 잡는다.
겹치면 위 DELETE가 실제 강의 데이터를 지운다.

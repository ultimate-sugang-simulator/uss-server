-- PERF-100 시드 (이슈 공용)
-- 대상: GET /api/v1/courses/search
--
-- 변수 블록만 둔다. 모듈 본문은 template/seeds/ 에 있고 실행 시 cat으로 이어 붙인다.
--   cat $PERF_DIR/seeds.sql $SEEDS/course.sql | mysqlp
--
-- 규모: 운영(앱 시드 courses 2,439) 대비 10x. 검색이 학년도·학기로 거르지 않아 학기가 ~10개 쌓인 운영 상태를 재현한다.
--       search 모드 조합당 매칭 = 24,000 / 1,000 = 24건. 실데이터 선택도(컴퓨터 21/2,440)와 같은 수준이다.
-- 크기: 행당 실측(courses 1.19KB, course_schedules 0.24KB) 기준 약 46MB. 버퍼 풀 128MiB 안에 든다.
-- 회원: 대상 컨트롤러에 @Auth가 없어 회원 시드가 필요 없다. 토큰은 mint-tokens.sh로 서명한다. member.sql은 붙이지 않는다.
--
-- 되돌리기 (FK CASCADE로 course_schedules가 따라 지워진다):
--   DELETE FROM courses WHERE id BETWEEN 1000001 AND 1024000;

-- 재귀 CTE 기본 깊이 상한은 1000이다. 안 올리면 1000행에서 끊긴다.
SET SESSION cte_max_recursion_depth = 10000000;

-- 강의. @course_start는 SELECT MAX(id) FROM courses; (2026-08-28 기준 2,439) 보다 커야 한다.
SET @course_start         = 1000001;
SET @course_count         = 24000;
SET @course_dept_count    = 20;
SET @course_area_count    = 8;
SET @schedules_per_course = 3;
SET @course_term_year     = 2026;
SET @course_term          = 'FIRST';
SET @course_title_mode    = 'search';

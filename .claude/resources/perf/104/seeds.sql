-- PERF-104 시드
-- 대상: GET /api/v1/courses/major
--
-- 모듈 조합: member.sql + course.sql
-- carts, registrations는 대상 쿼리가 읽지 않으므로 enrollment.sql은 쓰지 않는다.
--
-- unique_checks / foreign_key_checks는 끄지 않는다. INSERT IGNORE의 재실행 멱등성이
-- UNIQUE 제약에 걸려 있어서, 끄면 두 번째 실행에서 중복이 쌓일 수 있다.
-- 적재 속도는 README의 FULLTEXT 인덱스 재생성 절차로 확보한다.

-- 재귀 CTE 기본 깊이 상한은 1000이다. 안 올리면 1000행에서 끊긴다.
SET SESSION cte_max_recursion_depth = 10000000;

-- 회원
SET @member_start      = 900001;
SET @member_count      = 1000;
SET @student_id_start  = 200000001;
SET @pw_hash           = 'perf-not-a-real-hash';
SET @member_dept_count = 8;

-- 강의. @course_start는 현재 MAX(id)(2,500 미만)보다 충분히 크다.
SET @course_start         = 1000001;
SET @course_count         = 24000;
SET @course_dept_count    = 29;
SET @course_area_count    = 8;
SET @schedules_per_course = 3;
SET @course_term_year     = 2026;
SET @course_term          = 'FIRST';
SET @course_title_mode    = 'plain';

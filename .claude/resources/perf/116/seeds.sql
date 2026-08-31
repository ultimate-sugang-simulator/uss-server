-- PERF-116 시드
-- 대상: GET /api/v1/carts
--
-- 모듈 조합: member.sql + cart-by-grade.sql
-- courses, course_schedules는 #104 시드(id 1000001~1024000, 24,000건, 강의당 시간표 3건)가 DB에 그대로 남아 있다.
-- course.sql은 돌리지 않는다. Phase 3-A 실측: courses 26,439 / course_schedules 79,819으로 #106과 일치.
-- member.sql은 #104와 같은 시작값, 같은 공식이라 INSERT IGNORE가 기존 1,000명(900001~901000)을 건너뛰고 10,247명만 더한다.
-- enrollment.sql은 쓰지 않는다. 학년별 담기 수와 교양/전공 풀 상한이 필요해 cart-by-grade.sql로 대체한다.
--
-- 규모 근거: #106과 동일한 값을 그대로 쓴다. 이 이슈는 #106이 남긴 위험 신호를 이어받으므로 기준선이 직접 비교되어야 한다.
-- 학년별 인원과 평균 장바구니 수는 실제 추정치(4학년 3,430명 4과목, 3학년 2,733명 6과목,
-- 2학년 2,520명 9과목, 1학년 2,564명 6과목)이고, 교양/전공 구성은 학년별 교육과정 비율(1학년 5:3, 2학년 3:7,
-- 3학년 2:8, 4학년 2:6)을 담기 수에 적용해 반올림한 값이다. 총 68,182건.

-- 재귀 CTE 기본 깊이 상한은 1000이다. 안 올리면 1000행에서 끊긴다.
SET SESSION cte_max_recursion_depth = 10000000;

-- 회원
SET @member_start      = 900001;
SET @member_count      = 11247;
SET @student_id_start  = 200000001;
SET @pw_hash           = 'perf-not-a-real-hash';
SET @member_dept_count = 8;

-- 강의. cart-by-grade.sql이 풀을 세우는 범위. course.sql을 돌리지 않으므로 #104 값과 같아야 한다.
SET @course_start = 1000001;
SET @course_count = 24000;

-- 학년 구간 (id 순서대로 1학년→4학년, 합 = @member_count)
SET @grade1_count = 2564;
SET @grade2_count = 2520;
SET @grade3_count = 2733;
SET @grade4_count = 3430;

-- 학년별 담기 수 (교양 / 전공)
SET @ge_carts_g1 = 4;
SET @major_carts_g1 = 2;
SET @ge_carts_g2 = 3;
SET @major_carts_g2 = 6;
SET @ge_carts_g3 = 1;
SET @major_carts_g3 = 5;
SET @ge_carts_g4 = 1;
SET @major_carts_g4 = 3;

-- 담기가 퍼지는 강의 수. 교양은 학년당(풀 6,000), 전공은 회원 학과와 학년당(풀 약 124, 복수 소유 학과는 그 배수).
-- 강의당 담기 수 = 그 학년의 담기 총량 / 풀 크기. 값을 정한 근거는 record.md 측정 환경의 카디널리티 항목에.
SET @ge_pool    = 100;
SET @major_pool = 15;

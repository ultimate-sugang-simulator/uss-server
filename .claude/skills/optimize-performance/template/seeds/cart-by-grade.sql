-- 학년별 장바구니 시드: carts
--
-- 필요한 변수: @member_start, @member_count, @course_start, @course_count,
--              @grade1_count ~ @grade4_count (1학년→4학년 인원, 합은 @member_count),
--              @ge_carts_g1 ~ @ge_carts_g4, @major_carts_g1 ~ @major_carts_g4 (학년별 교양/전공 담기 수, 학년별 합 상한 10),
--              @ge_pool, @major_pool (담기가 퍼지는 강의 수. 교양은 학년당, 전공은 회원 학과와 학년당)
-- 선행 모듈: member.sql, course.sql
--
-- enrollment.sql은 회원 전체에 담기 수 하나를 전 강의에 균등하게 뿌린다. 이 모듈은
--   - 회원 학년을 id 구간 순(1학년→4학년)으로 다시 매겨 학년별로 다른 담기 수를 준다. member.sql의 n % 4 학년을 덮어쓴다.
--   - 교양은 교양 영역, 전공은 회원 학과가 소유한 학과(CourseDepartment.ownedBy)의 전공 영역에서 고른다.
--   - 회원 학년 강의와 전학년 강의만 고른다 (course.sql의 grade_code '01'~'04', '05' = 전학년).
--   - 풀을 id 순으로 세워 앞 @ge_pool / @major_pool개에만 담기를 얹는다. 인기 강의 집중을 이 두 수로 통제한다.
--     소유 학과가 여럿인 회원 학과도 풀은 회원 학과 기준 하나라, @major_pool개에 집중된다.
-- 한 회원의 담기 행은 created_at이 서로 다르다 (ORDER BY created_at이 동점이 되지 않도록 분 단위로 벌린다).
-- 강의 선택은 (회원 순번 * 37 + k) % 풀 크기라 재실행해도 같은 행이 나오고, INSERT IGNORE가 중복을 막는다.

SELECT '[cart-by-grade.sql] members.grade 재배정' AS '';

UPDATE members
SET grade = CASE
    WHEN id < @member_start + @grade1_count THEN 'FRESHMAN'
    WHEN id < @member_start + @grade1_count + @grade2_count THEN 'SOPHOMORE'
    WHEN id < @member_start + @grade1_count + @grade2_count + @grade3_count THEN 'JUNIOR'
    ELSE 'SENIOR'
END
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

SELECT '[cart-by-grade.sql] 강의 풀 구성' AS '';

-- 풀은 임시 테이블에 (파티션, 순번) PK로 둔다. 순번 등식 조인이 PK 탐색으로 풀리게 하려는 것이다.
-- 임시 테이블을 한 쿼리에서 두 번 참조하지 않도록 풀 크기는 별도 테이블에 둔다.
DROP TEMPORARY TABLE IF EXISTS perf_ge_pool;
CREATE TEMPORARY TABLE perf_ge_pool (PRIMARY KEY (grade_code, rn)) AS
SELECT grade_code, rn, course_id
FROM (
    SELECT g.grade_code,
           ROW_NUMBER() OVER (PARTITION BY g.grade_code ORDER BY c.id) AS rn,
           c.id AS course_id
    FROM courses c
    JOIN (SELECT '01' AS grade_code UNION ALL SELECT '02' UNION ALL SELECT '03' UNION ALL SELECT '04') g
      ON c.grade_code IN (g.grade_code, '05')
    WHERE c.id BETWEEN @course_start AND @course_start + @course_count - 1
      AND c.area NOT IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE')
) ranked
WHERE rn <= @ge_pool;

DROP TEMPORARY TABLE IF EXISTS perf_ge_size;
CREATE TEMPORARY TABLE perf_ge_size (PRIMARY KEY (grade_code)) AS
SELECT grade_code, count(*) AS size
FROM perf_ge_pool
GROUP BY grade_code;

DROP TEMPORARY TABLE IF EXISTS perf_major_pool;
CREATE TEMPORARY TABLE perf_major_pool (PRIMARY KEY (member_department, grade_code, rn)) AS
SELECT member_department, grade_code, rn, course_id
FROM (
    SELECT o.member_department,
           g.grade_code,
           ROW_NUMBER() OVER (PARTITION BY o.member_department, g.grade_code ORDER BY c.id) AS rn,
           c.id AS course_id
    FROM courses c
    JOIN (
        -- CourseDepartment.ownedBy(MemberDepartment). member.sql의 학과 8개가 소유한 CourseDepartment 14개.
        SELECT 'COMPUTER_ENGINEERING' AS member_department, 'COMPUTER_ENGINEERING' AS course_department
        UNION ALL SELECT 'MECHANICAL_ENGINEERING', 'MECHANICAL_ENGINEERING'
        UNION ALL SELECT 'MATHEMATICS', 'MATHEMATICS'
        UNION ALL SELECT 'BUSINESS_ADMINISTRATION', 'BUSINESS_ADMINISTRATION'
        UNION ALL SELECT 'ECONOMICS', 'ECONOMICS'
        UNION ALL SELECT 'ELECTRONICS_ENGINEERING_SCHOOL', 'ELECTRONICS_ENGINEERING'
        UNION ALL SELECT 'ELECTRONICS_ENGINEERING_SCHOOL', 'ELECTRONICS_ENGINEERING_SCHOOL'
        UNION ALL SELECT 'ELECTRONICS_ENGINEERING_SCHOOL', 'ELECTRONICS_ENGINEERING_MAJOR'
        UNION ALL SELECT 'ELECTRONICS_ENGINEERING_SCHOOL', 'SEMICONDUCTOR_CONVERGENCE_MAJOR'
        UNION ALL SELECT 'LIFE_SCIENCE_SCHOOL', 'LIFE_SCIENCE_SCHOOL'
        UNION ALL SELECT 'LIFE_SCIENCE_SCHOOL', 'LIFE_SCIENCE_MAJOR'
        UNION ALL SELECT 'LIFE_SCIENCE_SCHOOL', 'MOLECULAR_LIFE_SCIENCE_MAJOR'
        UNION ALL SELECT 'GLOBAL_TRADE_SERVICE', 'TRADE'
        UNION ALL SELECT 'GLOBAL_TRADE_SERVICE', 'GLOBAL_TRADE_SERVICE'
    ) o ON o.course_department = c.department
    JOIN (SELECT '01' AS grade_code UNION ALL SELECT '02' UNION ALL SELECT '03' UNION ALL SELECT '04') g
      ON c.grade_code IN (g.grade_code, '05')
    WHERE c.id BETWEEN @course_start AND @course_start + @course_count - 1
      AND c.area IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE')
) ranked
WHERE rn <= @major_pool;

DROP TEMPORARY TABLE IF EXISTS perf_major_size;
CREATE TEMPORARY TABLE perf_major_size (PRIMARY KEY (member_department, grade_code)) AS
SELECT member_department, grade_code, count(*) AS size
FROM perf_major_pool
GROUP BY member_department, grade_code;

SELECT '[cart-by-grade.sql] carts 적재' AS '';

INSERT IGNORE INTO carts (member_id, course_id, created_at)
WITH RECURSIVE k AS (
    SELECT 0 AS k
    UNION ALL
    SELECT k + 1 FROM k WHERE k < 9
),
mem AS (
    SELECT id,
           id - @member_start AS i,
           department,
           CASE grade
               WHEN 'FRESHMAN'  THEN '01'
               WHEN 'SOPHOMORE' THEN '02'
               WHEN 'JUNIOR'    THEN '03'
               ELSE                  '04'
           END AS grade_code,
           CASE grade
               WHEN 'FRESHMAN'  THEN @ge_carts_g1
               WHEN 'SOPHOMORE' THEN @ge_carts_g2
               WHEN 'JUNIOR'    THEN @ge_carts_g3
               ELSE                  @ge_carts_g4
           END AS ge_carts,
           CASE grade
               WHEN 'FRESHMAN'  THEN @major_carts_g1
               WHEN 'SOPHOMORE' THEN @major_carts_g2
               WHEN 'JUNIOR'    THEN @major_carts_g3
               ELSE                  @major_carts_g4
           END AS major_carts
    FROM members
    WHERE id BETWEEN @member_start AND @member_start + @member_count - 1
)
SELECT m.id,
       p.course_id,
       NOW() - INTERVAL (m.i % 30) DAY - INTERVAL k.k MINUTE
FROM mem m
JOIN k ON k.k < m.ge_carts
JOIN perf_ge_size s ON s.grade_code = m.grade_code
JOIN perf_ge_pool p
  ON p.grade_code = m.grade_code
 AND p.rn = 1 + (m.i * 37 + k.k) % s.size
UNION ALL
SELECT m.id,
       p.course_id,
       NOW() - INTERVAL (m.i % 30) DAY - INTERVAL (10 + k.k) MINUTE
FROM mem m
JOIN k ON k.k < m.major_carts
JOIN perf_major_size s ON s.member_department = m.department AND s.grade_code = m.grade_code
JOIN perf_major_pool p
  ON p.member_department = m.department
 AND p.grade_code = m.grade_code
 AND p.rn = 1 + (m.i * 37 + k.k) % s.size;

ANALYZE TABLE members, carts;

-- 검증

-- 학년 구간 합이 회원 수와 같아야 한다. 0이면 마지막 구간(4학년)이 남는 회원을 전부 흡수한 것이다.
SELECT @grade1_count + @grade2_count + @grade3_count + @grade4_count = @member_count AS grade_counts_match;

SELECT 'members'   AS table_name,
       grade,
       count(*)    AS members
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1
GROUP BY grade
ORDER BY FIELD(grade, 'FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR');

-- 학년별 담기 수와 교양/전공 구성. per_member가 @ge_carts + @major_carts와 같아야 한다.
SELECT 'carts'                                                                AS table_name,
       m.grade,
       count(*)                                                               AS rows_seeded,
       count(DISTINCT c.member_id)                                            AS members,
       count(*) / NULLIF(count(DISTINCT c.member_id), 0)                      AS per_member,
       SUM(co.area NOT IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE'))    AS ge_carts,
       SUM(co.area IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE'))        AS major_carts
FROM carts c
JOIN members m ON m.id = c.member_id
JOIN courses co ON co.id = c.course_id
WHERE c.member_id BETWEEN @member_start AND @member_start + @member_count - 1
GROUP BY m.grade
ORDER BY FIELD(m.grade, 'FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR');

-- 강의당 담기 수. course_id IN (...) GROUP BY course_id 집계가 요청 1회에 읽는 행 수의 근거다.
SELECT IF(co.area IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE'), 'major', 'general') AS kind,
       count(*)              AS carted_courses,
       MIN(x.n)              AS min_per_course,
       ROUND(AVG(x.n), 1)    AS avg_per_course,
       MAX(x.n)              AS max_per_course
FROM (
    SELECT course_id, count(*) AS n
    FROM carts
    WHERE member_id BETWEEN @member_start AND @member_start + @member_count - 1
    GROUP BY course_id
) x
JOIN courses co ON co.id = x.course_id
GROUP BY kind;

-- 목표보다 적게 담긴 회원. 풀이 담기 수보다 작으면 생긴다. 0이어야 한다.
SELECT count(*) AS members_short_of_target
FROM (
    SELECT m.id
    FROM members m
    LEFT JOIN carts c ON c.member_id = m.id
    WHERE m.id BETWEEN @member_start AND @member_start + @member_count - 1
    GROUP BY m.id, m.grade
    HAVING count(c.id) < CASE m.grade
        WHEN 'FRESHMAN'  THEN @ge_carts_g1 + @major_carts_g1
        WHEN 'SOPHOMORE' THEN @ge_carts_g2 + @major_carts_g2
        WHEN 'JUNIOR'    THEN @ge_carts_g3 + @major_carts_g3
        ELSE                  @ge_carts_g4 + @major_carts_g4
    END
) g;

-- 장바구니 상한(10)을 넘긴 회원이 있으면 담기 API가 항상 실패한다. 0이어야 한다.
SELECT count(*) AS members_over_cart_limit
FROM (
    SELECT member_id FROM carts
    WHERE member_id BETWEEN @member_start AND @member_start + @member_count - 1
    GROUP BY member_id HAVING count(*) > 10
) g;

DROP TEMPORARY TABLE IF EXISTS perf_ge_pool;
DROP TEMPORARY TABLE IF EXISTS perf_ge_size;
DROP TEMPORARY TABLE IF EXISTS perf_major_pool;
DROP TEMPORARY TABLE IF EXISTS perf_major_size;

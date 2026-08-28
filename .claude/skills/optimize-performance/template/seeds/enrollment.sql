-- 장바구니, 수강신청 시드: carts, registrations
--
-- 필요한 변수: @member_start, @member_count, @course_start, @course_count,
--              @cart_per_member (상한 10), @registration_per_member
-- 선행 모듈: member.sql, course.sql
--
-- 두 테이블 모두 UNIQUE KEY uk_member_course (member_id, course_id)를 가진다.
-- 회원마다 서로 다른 강의를 배정하도록 (m * 소수 + k) % @course_count로 흩는다.
-- 담기·신청 API 자체를 측정할 때는 해당 변수를 0으로 둔다. 미리 채워두면 부하가 전부 중복 실패다.

SELECT '[enrollment.sql] carts 적재' AS '';

INSERT IGNORE INTO carts (member_id, course_id, created_at)
WITH RECURSIVE m AS (
    SELECT 0 AS i
    UNION ALL
    SELECT i + 1 FROM m WHERE i < @member_count - 1
),
c AS (
    SELECT 0 AS k
    UNION ALL
    SELECT k + 1 FROM c WHERE k < @cart_per_member - 1
)
SELECT @member_start + m.i,
       @course_start + ((m.i * 37 + c.k) % @course_count),
       NOW() - INTERVAL (m.i % 30) DAY
FROM m CROSS JOIN c
WHERE @cart_per_member > 0;

SELECT '[enrollment.sql] registrations 적재' AS '';

INSERT IGNORE INTO registrations (member_id, course_id, created_at)
WITH RECURSIVE m AS (
    SELECT 0 AS i
    UNION ALL
    SELECT i + 1 FROM m WHERE i < @member_count - 1
),
r AS (
    SELECT 0 AS k
    UNION ALL
    SELECT k + 1 FROM r WHERE k < @registration_per_member - 1
)
SELECT @member_start + m.i,
       @course_start + ((m.i * 53 + r.k) % @course_count),
       NOW() - INTERVAL (m.i % 30) DAY
FROM m CROSS JOIN r
WHERE @registration_per_member > 0;

-- current_enrollment를 실제 신청 건수와 맞춘다. 어긋나면 정원 판정이 실제 데이터와 다르게 동작한다.
UPDATE courses c
SET c.current_enrollment = (SELECT count(*) FROM registrations r WHERE r.course_id = c.id)
WHERE c.id BETWEEN @course_start AND @course_start + @course_count - 1;

ANALYZE TABLE carts, registrations, courses;

-- 검증
SELECT 'carts'                                          AS table_name,
       count(*)                                         AS rows_seeded,
       count(DISTINCT member_id)                        AS members,
       count(*) / NULLIF(count(DISTINCT member_id), 0)  AS per_member
FROM carts
WHERE member_id BETWEEN @member_start AND @member_start + @member_count - 1;

SELECT 'registrations'                                  AS table_name,
       count(*)                                         AS rows_seeded,
       count(DISTINCT member_id)                        AS members,
       count(*) / NULLIF(count(DISTINCT member_id), 0)  AS per_member
FROM registrations
WHERE member_id BETWEEN @member_start AND @member_start + @member_count - 1;

-- 장바구니 상한(10)을 넘긴 회원이 있으면 담기 API가 항상 실패한다. 0이어야 한다.
SELECT count(*) AS members_over_cart_limit
FROM (
    SELECT member_id FROM carts
    WHERE member_id BETWEEN @member_start AND @member_start + @member_count - 1
    GROUP BY member_id HAVING count(*) > 10
) g;

-- 정원을 넘긴 강의가 있으면 신청 API가 항상 마감으로 실패한다. 0이어야 한다.
SELECT count(*) AS courses_over_capacity
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1
  AND current_enrollment >= max_capacity;

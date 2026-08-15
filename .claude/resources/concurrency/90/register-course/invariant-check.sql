-- [CONC-90] registerCourse 불변식 검증
-- Phase 1에서 확정. Phase 4와 7에서 **그대로** 재사용한다. 후보마다 바꾸지 마라.
--
-- 실행:
--   $MYSQL_CONC -t < .claude/resources/concurrency/90/register-course/invariant-check.sql \
--     | tee $TARGET_DIR/invariant-{n}.txt

-- 대상 자원. Phase 3에서 확정했다.
SET @target_course_id = 990001;

SELECT '=== 불변식 검증 ===' AS '';

-- I1. 집계 컬럼이 정원을 넘지 않는다.
--     violations = 정원을 초과한 만큼의 인원 수.
SELECT 'I1. current_enrollment <= max_capacity'                  AS invariant,
       c.max_capacity                                            AS expected,
       c.current_enrollment                                      AS actual,
       GREATEST(0, c.current_enrollment - c.max_capacity)         AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- I2. 등록 행 수가 정원을 넘지 않는다.
--     violations = 정원을 초과해 들어간 등록 건수.
--     갱신이 유실되면 I1은 통과하면서 여기서만 깨진다.
SELECT 'I2. COUNT(registrations) <= max_capacity'                AS invariant,
       c.max_capacity                                            AS expected,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                               AS actual,
       GREATEST(0, (SELECT COUNT(*) FROM registrations r
                     WHERE r.course_id = c.id) - c.max_capacity)  AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- I3. 집계 컬럼이 실제 등록 행 수와 일치한다.
--     violations != 0 이면 갱신이 유실된 것이다. lost update의 직접 증거다.
--     I1, I2가 둘 다 0이어도 이 값이 0이 아닐 수 있다. 반드시 함께 본다.
SELECT 'I3. current_enrollment = COUNT(registrations)'           AS invariant,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                               AS expected,
       c.current_enrollment                                      AS actual,
       ABS(c.current_enrollment - (SELECT COUNT(*) FROM registrations r
                                    WHERE r.course_id = c.id))    AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- 참고 자료. 불변식은 아니지만 판정에 쓴다.
SELECT '=== 참고 ===' AS '';

-- 음수 인원. 이 대상은 취소를 배제해 decrementEnrollment 발화 경로가 없으므로 불변식에서 제외했다.
-- 취소 대상 슬러그에서 불변식으로 승격한다.
SELECT 'ref. current_enrollment 부호'                             AS metric,
       c.current_enrollment                                      AS value,
       IF(c.current_enrollment < 0, 'NEGATIVE', 'OK')             AS sign
FROM courses c
WHERE c.id = @target_course_id;

-- 중복 등록이 실제로 들어갔는지. uk_member_course가 있으면 항상 1 이하여야 한다.
-- 2 이상이면 UNIQUE 제약이 없거나 깨진 것이다.
SELECT 'ref. 회원당 중복 등록'                                     AS metric,
       COALESCE(MAX(cnt), 0)                                     AS max_per_member
FROM (
    SELECT COUNT(*) AS cnt
    FROM registrations
    WHERE course_id = @target_course_id
    GROUP BY member_id
) g;

-- 전체 강의로 넓혀 카운터가 어긋난 행이 또 있는지 본다.
-- 대상 강의만 보다가 다른 곳의 오염을 놓치지 않기 위한 것이다.
SELECT 'ref. 카운터가 어긋난 강의 수'                              AS metric,
       COUNT(*)                                                  AS courses_with_drift
FROM courses c
WHERE c.current_enrollment <> (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id);

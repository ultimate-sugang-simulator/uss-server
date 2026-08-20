-- [CONC-98] deleteRegisteredCourse 불변식 검증
-- Phase 1에서 확정. Phase 4와 7에서 **그대로** 재사용한다. 후보마다 바꾸지 마라.
--
-- 실행:
--   mysqlc -t < .claude/resources/concurrency/98/delete-registered-course/invariant-check.sql \
--     | tee $TARGET_DIR/invariant-{n}.txt

-- 대상 자원. 이슈 공용 경합 대상 강의. Phase 3의 시드가 이 id를 그대로 쓴다.
SET @target_course_id = 990001;

SELECT '=== 불변식 검증 ===' AS '';

-- I1. 집계 컬럼이 실제 등록 행 수와 일치한다.
--     violations != 0 이면 갱신이 유실된 것이다. lost update의 직접 증거다.
--     취소 두 건이 같은 값을 읽고 같은 절대값을 쓰면 감소 하나가 사라지고,
--     카운터가 실제 등록 행 수보다 크게 남아 자리가 안 풀린 것처럼 보인다.
SELECT 'I1. current_enrollment = COUNT(registrations)'         AS invariant,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                             AS expected,
       c.current_enrollment                                    AS actual,
       ABS(c.current_enrollment - (SELECT COUNT(*) FROM registrations r
                                    WHERE r.course_id = c.id))  AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- I2. 집계 컬럼은 음수가 되지 않는다.
--     decrementEnrollment()에 하한이 없고 컬럼에도 CHECK 제약이 없다.
--     I1이 0이어도 이 값이 0이 아닐 수 있다. 반드시 함께 본다.
SELECT 'I2. current_enrollment >= 0'                           AS invariant,
       '>= 0'                                                  AS expected,
       c.current_enrollment                                    AS actual,
       IF(c.current_enrollment < 0, 1, 0)                      AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- 참고 자료. 불변식은 아니지만 판정에 쓴다.
SELECT '=== 참고 ===' AS '';

-- 등록 행 수와 정원. Phase 1에서 불변식으로 채택하지 않았다.
-- 취소 단독 부하로는 깨지지 않고, 신청과 취소를 섞은 부하에서만 의미가 생긴다.
-- 혼합 부하를 쓰게 되면 이 값으로 카운터 오차가 정원 초과로 번졌는지 확인한다.
SELECT 'ref. 등록 행 수 대 정원'                                AS metric,
       c.max_capacity                                          AS max_capacity,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                             AS rows_left,
       GREATEST(0, (SELECT COUNT(*) FROM registrations r
                     WHERE r.course_id = c.id) - c.max_capacity) AS over_capacity
FROM courses c
WHERE c.id = @target_course_id;

-- 중복 등록이 실제로 들어갔는지. uk_member_course가 있으면 항상 1 이하여야 한다.
-- 2 이상이면 UNIQUE 제약이 없거나 깨진 것이다.
SELECT 'ref. 회원당 중복 등록'                                  AS metric,
       COALESCE(MAX(cnt), 0)                                   AS max_per_member
FROM (
    SELECT COUNT(*) AS cnt
    FROM registrations
    WHERE course_id = @target_course_id
    GROUP BY member_id
) g;

-- 전체 강의로 넓혀 카운터가 어긋난 행이 또 있는지 본다.
-- 대상 강의만 보다가 다른 곳의 오염을 놓치지 않기 위한 것이다.
SELECT 'ref. 카운터가 어긋난 강의 수'                            AS metric,
       COUNT(*)                                                AS courses_with_drift
FROM courses c
WHERE c.current_enrollment <> (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.id);

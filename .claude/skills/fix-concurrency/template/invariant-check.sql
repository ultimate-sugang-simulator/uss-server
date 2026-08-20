-- 불변식 검증 템플릿.
-- Phase 1에서 대상에 맞게 고쳐 .claude/resources/concurrency/{이슈번호}/{슬러그}/invariant-check.sql 로 저장한다.
-- Phase 4와 7에서 **그대로** 재사용한다. 후보마다 바꾸지 마라. 바꾸면 전후 비교가 성립하지 않는다.
--
-- 실행:
--   mysqlc -t < .claude/resources/concurrency/{이슈번호}/{슬러그}/invariant-check.sql \
--     | tee $TARGET_DIR/invariant-{n}.txt
--
-- ── 작성 규칙 ──────────────────────────────────────────────
-- 1. 불변식 하나가 결과 한 행으로 나온다. 한 쿼리에 여러 불변식을 섞지 마라.
-- 2. 모든 행이 같은 칼럼 구조를 갖는다: invariant / expected / actual / violations.
--    구조가 다르면 Phase 4와 7에서 같은 방식으로 읽을 수 없다.
-- 3. **violations는 0이 통과다.** 불리언이 아니라 개수로 만든다.
--    "몇 건 어긋났는가"가 lost update의 규모를 보여준다.
-- 4. 집계 컬럼과 실제 행 수를 대조하는 불변식을 반드시 넣는다(아래 I2).
--    상한을 안 넘겨도 갱신이 유실됐을 수 있고, 그건 이 불변식으로만 잡힌다.
-- 5. 역방향 동작(취소, 감소)에 걸린 불변식을 빼먹지 마라(아래 I3).
-- 6. 값을 반올림하거나 가공하지 마라. 원본 그대로 파일에 남긴다.
-- ──────────────────────────────────────────────────────────

-- 대상 자원. Phase 3에서 확정한 값으로 바꾼다.
SET @target_course_id = {대상 강의 id};

SELECT '=== 불변식 검증 ===' AS '';

-- I1. 등록 행 수는 정원을 넘지 않는다.
--     violations = 정원을 초과해 들어간 등록 건수.
SELECT 'I1. 등록 행 수 <= 정원'                                AS invariant,
       c.max_capacity                                          AS expected,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                             AS actual,
       GREATEST(0, (SELECT COUNT(*) FROM registrations r
                     WHERE r.course_id = c.id) - c.max_capacity) AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- I2. 집계 컬럼이 실제 등록 행 수와 일치한다.
--     violations != 0 이면 갱신이 유실된 것이다. lost update의 직접 증거다.
--     I1이 0이어도 이 값이 0이 아닐 수 있다. 반드시 함께 본다.
SELECT 'I2. current_enrollment = COUNT(registrations)'         AS invariant,
       (SELECT COUNT(*) FROM registrations r
         WHERE r.course_id = c.id)                             AS expected,
       c.current_enrollment                                    AS actual,
       ABS(c.current_enrollment - (SELECT COUNT(*) FROM registrations r
                                    WHERE r.course_id = c.id))  AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- I3. 집계 컬럼은 음수가 되지 않는다.
--     취소 경로에 같은 제어를 걸지 않으면 여기서 드러난다.
SELECT 'I3. current_enrollment >= 0'                           AS invariant,
       '>= 0'                                                  AS expected,
       c.current_enrollment                                    AS actual,
       IF(c.current_enrollment < 0, 1, 0)                      AS violations
FROM courses c
WHERE c.id = @target_course_id;

-- 참고 자료. 불변식은 아니지만 판정에 쓴다.
SELECT '=== 참고 ===' AS '';

-- 중복 등록이 실제로 들어갔는지. uk_member_course가 있으면 항상 0이어야 한다.
-- 0이 아니면 UNIQUE 제약이 없거나 깨진 것이다.
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

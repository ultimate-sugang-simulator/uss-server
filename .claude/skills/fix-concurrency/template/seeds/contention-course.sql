-- 경합 대상 강의 시드: courses
--
-- 필요한 변수: @target_course_id, @target_capacity
--
-- **강의를 하나만 만든다.** 동시성 측정에서 경합 대상을 여러 개로 흩으면
-- 행마다 경합이 옅어져 결함이 재현되지 않는다.
--
-- 시간표(course_schedules)는 만들지 않는다.
-- CourseValidator.validateCourseScheduleNotConflict는 대상 강의의 시간표가 비어 있으면
-- 즉시 통과하므로, 시간 충돌 검증에 걸려 정원 경합에 도달하지 못하는 요청이 생기지 않는다.
--
-- 값이 실제 강의처럼 보일 필요는 없다. 아래 세 가지만 지키면 된다.
--   1. status = 'ACTIVE'          (validateCourseActive 통과)
--   2. type_code가 OCU, K-MOOC가 아님 (validateCourseTypeLimit에 먼저 걸리지 않게)
--   3. enum 컬럼에 실재하는 상수명   (valueOf 파싱 실패로 500이 나지 않게)

SELECT '[contention-course.sql] 경합 대상 강의 적재' AS '';

INSERT IGNORE INTO courses
    (id, academic_year, term, title_kr, title_en, course_code, haksu_code,
     college, department, classification_code, classification_name,
     area, area_code, area_name, type_code, type_name, grade_code, grade_name,
     concentration_code, concentration_name, credits,
     is_english_course, english_code, english_name, is_huss_course,
     max_capacity, current_enrollment, status)
VALUES (
    @target_course_id,
    2026,
    'SECOND',
    '동시성측정용강의',
    'Concurrency Test Course',
    '9999999',
    -- uk_year_term_haksu (academic_year, term, haksu_code)에 걸리므로
    -- 실제 강의와 겹치지 않는 값을 쓴다.
    '9999999001',
    'ENGINEERING',
    'COMPUTER_ENGINEERING',
    '31',
    '전공핵심',
    'MAJOR_CORE',
    '34',
    '전공핵심',
    -- '1' = LECTURE(강의(이론)). OCU, K-MOOC가 아니어야 유형 상한에 먼저 걸리지 않는다.
    '1',
    '강의(이론)',
    '1',
    '1학년',
    '0',
    '일반(1~15주)',
    -- 학점은 작게 둔다. 회원의 최대 이수 학점(24)에 여유가 있어야 한다.
    3,
    false,
    '0',
    '비대상',
    false,
    @target_capacity,
    0,
    -- ACTIVE가 아니면 validateCourseActive에서 먼저 걸린다.
    'ACTIVE'
);

-- 재실행 시 INSERT IGNORE로 건너뛰므로 정원과 카운터를 명시적으로 되돌린다.
-- 앞선 측정이 남긴 상태가 그대로 남아 있으면 재현이 되지 않는다.
UPDATE courses
   SET max_capacity       = @target_capacity,
       current_enrollment = 0,
       status             = 'ACTIVE'
 WHERE id = @target_course_id;

DELETE FROM registrations WHERE course_id = @target_course_id;

ANALYZE TABLE courses;

-- 검증
SELECT 'target course'    AS table_name,
       id,
       haksu_code,
       type_code,
       status,
       max_capacity,
       current_enrollment,
       (SELECT COUNT(*) FROM registrations r WHERE r.course_id = courses.id) AS registrations
FROM courses
WHERE id = @target_course_id;

-- 측정을 시작할 수 있는 상태인지 한 줄로 확인한다.
SELECT IF(status = 'ACTIVE'
              AND current_enrollment = 0
              AND max_capacity = @target_capacity
              AND (SELECT COUNT(*) FROM registrations r WHERE r.course_id = courses.id) = 0,
          'OK',
          '초기 상태가 아니다. 되돌리기 SQL을 실행하라') AS target_state_check
FROM courses
WHERE id = @target_course_id;

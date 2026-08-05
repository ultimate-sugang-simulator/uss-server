-- 강의 시드: courses, course_schedules
--
-- 필요한 변수: @course_start, @course_count, @course_dept_count, @course_area_count, @schedules_per_course
--
-- id를 명시 삽입한다. @course_start를 앱 시드(database/seed/)의 강의 id보다 충분히 크게 잡아야
-- 시드 정리 시 실제 강의 데이터를 지우지 않는다.
--
-- course_area의 값 선택이 중요하다. 교양 조회(/api/v1/courses/general-education)는
-- 교양 영역만 통과시키므로(CourseArea.isGeneralEducationArea), 교양 조회를 잴 거면
-- 아래 목록에 교양 영역이 반드시 들어 있어야 한다.

SELECT '[course.sql] courses 적재' AS '';

INSERT IGNORE INTO courses
    (id, title_kr, title_en, course_code,
     course_college, course_department, course_classification,
     course_area, course_type, course_grade,
     professor_name, classroom, credits, is_english_course,
     max_capacity, current_enrollment)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @course_count - 1
)
SELECT @course_start + n,
       CONCAT('성능측정강의', n),
       CONCAT('perf course ', n),
       CONCAT('PERF', LPAD(n, 6, '0')),
       ELT(1 + (n % 5),
           'INFORMATION_TECHNOLOGY', 'ENGINEERING', 'NATURAL_SCIENCES',
           'BUSINESS', 'COMMERCE_PUBLIC_AFFAIRS'),
       -- member.sql의 member_department 앞 5개와 겹치게 둔다. 겹치지 않으면 전공 조회가 0건이 된다.
       ELT(1 + (n % @course_dept_count),
           'COMPUTER_ENGINEERING', 'MECHANICAL_ENGINEERING', 'MATHEMATICS',
           'BUSINESS_ADMINISTRATION', 'ECONOMICS',
           'INFORMATION_COMMUNICATION_ENGINEERING', 'EMBEDDED_SYSTEM',
           'ELECTRICAL_ENGINEERING', 'ELECTRONICS_ENGINEERING', 'PHYSICS',
           'CHEMISTRY', 'DATA_SCIENCE', 'TAX_ACCOUNTING', 'TRADE',
           'CONSUMER_SCIENCE', 'SOCIAL_WELFARE', 'PUBLIC_ADMINISTRATION',
           'POLITICS_DIPLOMACY', 'URBAN_ENGINEERING', 'SAFETY_ENGINEERING'),
       ELT(1 + (n % 6),
           'MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE',
           'BASIC_LIBERAL_ARTS', 'CORE_LIBERAL_ARTS', 'ADVANCED_LIBERAL_ARTS'),
       -- 앞 3개는 전공 영역, 나머지는 교양 영역이다. 교양 조회는 뒤쪽만 통과한다.
       ELT(1 + (n % @course_area_count),
           'MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE',
           'ACADEMIC_FOUNDATION', 'BASIC_SCIENCE_ENGINEERING',
           'CORE_HUMANITIES', 'CORE_SOCIAL', 'CORE_SCIENCE_TECHNOLOGY',
           'HUMANITIES', 'FOREIGN_LANGUAGE'),
       -- OCU 2개, K-MOOC 1개 상한이 있는 유형을 소수 섞어 둔다.
       -- 전부 LECTURE로 채우면 과목 유형 제한 분기를 한 번도 타지 않는다.
       ELT(1 + (n % 10),
           'LECTURE', 'LECTURE', 'LECTURE', 'LECTURE', 'LECTURE',
           'THEORY_LAB', 'LAB', 'E_LEARNING', 'OCU', 'K_MOOC'),
       ELT(1 + (n % 5), 'FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR', 'ALL'),
       CONCAT('교수', n % 200),
       CONCAT('호관 ', 100 + (n % 300)),
       ELT(1 + (n % 3), 1, 2, 3),
       IF(n % 20 = 0, TRUE, FALSE),
       -- 정원 마감 분기를 재려면 current_enrollment를 따로 올린다. 시드는 0에서 시작한다.
       30 + (n % 70),
       0
FROM seq;

SELECT '[course.sql] course_schedules 적재' AS '';

-- 강의당 @schedules_per_course개. 요일과 시간대를 흩어 시간표 충돌 판정이 실제로 갈리게 한다.
-- 전부 같은 요일, 같은 시간에 몰아넣으면 담기와 신청이 첫 건 이후 전부 충돌로 실패한다.
INSERT IGNORE INTO course_schedules
    (course_id, schedule_text, course_day, start_time, end_time)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @course_count - 1
),
slot AS (
    SELECT 0 AS s
    UNION ALL
    SELECT s + 1 FROM slot WHERE s < @schedules_per_course - 1
)
SELECT @course_start + seq.n,
       CONCAT(
           ELT(1 + ((seq.n + slot.s) % 5), '월', '화', '수', '목', '금'),
           ' ',
           LPAD(9 + ((seq.n * 2 + slot.s) % 9), 2, '0'), ':00-',
           LPAD(10 + ((seq.n * 2 + slot.s) % 9), 2, '0'), ':00'
       ),
       ELT(1 + ((seq.n + slot.s) % 5), 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'),
       MAKETIME(9 + ((seq.n * 2 + slot.s) % 9), 0, 0),
       MAKETIME(10 + ((seq.n * 2 + slot.s) % 9), 0, 0)
FROM seq CROSS JOIN slot;

ANALYZE TABLE courses, course_schedules;

-- 검증
SELECT 'courses'                          AS table_name,
       count(*)                           AS rows_seeded,
       count(DISTINCT course_department)  AS dept_cardinality,
       count(DISTINCT course_area)        AS area_cardinality,
       count(DISTINCT course_type)        AS type_cardinality,
       min(id)                            AS min_id,
       max(id)                            AS max_id
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1;

SELECT 'course_schedules'                 AS table_name,
       count(*)                           AS rows_seeded,
       count(*) / NULLIF(count(DISTINCT course_id), 0) AS schedules_per_course,
       count(DISTINCT course_day)         AS day_cardinality
FROM course_schedules
WHERE course_id BETWEEN @course_start AND @course_start + @course_count - 1;

-- 교양 조회를 잴 거면 교양 영역 강의가 실제로 있어야 한다. 0이면 @course_area_count를 늘려라.
SELECT count(*) AS general_education_courses
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1
  AND course_area NOT IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE');

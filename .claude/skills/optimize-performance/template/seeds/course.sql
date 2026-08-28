-- 강의 시드: courses, course_schedules
--
-- 필요한 변수: @course_start, @course_count, @course_dept_count (상한 20), @course_area_count (상한 8),
--              @schedules_per_course, @course_term_year, @course_term, @course_title_mode ('plain' | 'search')
--
-- id를 명시 삽입한다. @course_start는 SELECT MAX(id) FROM courses; 보다 커야 한다.
-- haksu_code는 VARCHAR(15)다. 'P' + 10자리라 @course_count가 커져도 넘지 않는다.
-- college, department, area, term, status는 enum 컬럼이고 classification, type, grade, concentration, english는
-- {*_code, *_name} String 쌍이다. enum 컬럼에는 실재하는 상수명만 넣는다.

SELECT '[course.sql] courses 적재' AS '';

INSERT IGNORE INTO courses
    (id, academic_year, term, title_kr, title_en, course_code, haksu_code,
     college, department, classification_code, classification_name,
     area, area_code, area_name, type_code, type_name,
     grade_code, grade_name, concentration_code, concentration_name,
     credits, is_english_course, english_code, english_name,
     is_huss_course, max_capacity, current_enrollment, status)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @course_count - 1
)
SELECT @course_start + n,
       @course_term_year,
       @course_term,
       -- search 모드: {접두 40}{접미 25}(n). 조합어 하나가 그 조합 행만 매칭한다 (README 검색 제목 설계).
       IF(@course_title_mode = 'search',
          CONCAT(
              ELT(1 + (n % 40),
                  '컴퓨터','기계','전자','전기','화학','물리','수학','통계','경영','경제',
                  '무역','행정','정치','사회','심리','교육','역사','철학','문학','언어',
                  '미디어','디자인','건축','도시','환경','에너지','신소재','반도체','바이오','의료',
                  '해양','항공','로봇','자동차','금융','회계','마케팅','물류','관광','스포츠'),
              ELT(1 + (FLOOR(n / 40) % 25),
                  '공학','과학','개론','실습','설계','시스템','이론','분석','응용','실험',
                  '연습','특강','세미나','연구','방법론','프로그래밍','알고리즘','데이터','네트워크','보안',
                  '최적화','시뮬레이션','모델링','제어','계측'),
              '(', n, ')'),
          CONCAT('성능측정강의', n)),
       CONCAT('perf course ', n),
       CONCAT('PERF', LPAD(n, 7, '0')),
       CONCAT('P', LPAD(n, 10, '0')),
       ELT(1 + (n % 8),
           'HUMANITIES', 'NATURAL_SCIENCES', 'SOCIAL_SCIENCES', 'COMMERCE_PUBLIC_AFFAIRS',
           'ENGINEERING', 'INFORMATION_TECHNOLOGY', 'BUSINESS', 'ARTS_PHYSICAL_EDUCATION'),
       -- 앞 5개는 member.sql의 department와 같다 (CourseDepartment, MemberDepartment 양쪽에 있는 상수).
       ELT(1 + (n % @course_dept_count),
           'COMPUTER_ENGINEERING', 'MECHANICAL_ENGINEERING', 'MATHEMATICS',
           'BUSINESS_ADMINISTRATION', 'ECONOMICS',
           'KOREAN_LITERATURE', 'ENGLISH_LITERATURE', 'GERMAN_STUDIES', 'FRENCH_STUDIES',
           'JAPANESE_LITERATURE', 'CHINESE_STUDIES', 'PHYSICS', 'CHEMISTRY',
           'FASHION_INDUSTRY', 'MARINE_SCIENCE', 'SOCIAL_WELFARE', 'MEDIA_COMMUNICATION',
           'LIBRARY_INFO', 'CREATIVE_HRD', 'PUBLIC_ADMINISTRATION'),
       LPAD(1 + (n % 6), 2, '0'),
       ELT(1 + (n % 6), '전공심화', '전공기초', '전공핵심', '기초교양', '핵심교양', '심화교양'),
       -- 앞 3개는 전공 영역, 4번째부터 교양 영역 (CourseArea.isGeneralEducationArea).
       ELT(1 + (n % @course_area_count),
           'MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE',
           'BASIC_SCIENCE_ENGINEERING', 'ACADEMIC_FOUNDATION', 'CORE_INU_SEMINAR',
           'CORE_HUMANITIES', 'CORE_SOCIAL'),
       LPAD(1 + (n % @course_area_count), 2, '0'),
       ELT(1 + (n % @course_area_count),
           '전공심화', '전공기초', '전공핵심', '기초과학공학',
           '학문기초', 'INU세미나', '핵심인문', '핵심사회'),
       LPAD(1 + (n % 5), 2, '0'),
       ELT(1 + (n % 5), '강의', '이론실습', '실습', '이러닝', '원격'),
       LPAD(1 + (n % 5), 2, '0'),
       ELT(1 + (n % 5), '1학년', '2학년', '3학년', '4학년', '전학년'),
       LPAD(1 + (n % 3), 2, '0'),
       ELT(1 + (n % 3), '해당없음', '연계전공', '융합전공'),
       1 + (n % 3),
       IF(n % 20 = 0, 1, 0),
       LPAD(1 + (n % 2), 2, '0'),
       ELT(1 + (n % 2), '해당없음', '영어강의'),
       IF(n % 25 = 0, 1, 0),
       30 + (n % 70),
       -- 0에서 시작한다. enrollment.sql이 실제 신청 건수로 맞춘다.
       0,
       'ACTIVE'
FROM seq;

SELECT '[course.sql] course_schedules 적재' AS '';

-- 강의당 @schedules_per_course개. 요일과 시간대를 흩어 시간표 충돌 판정이 실제로 갈리게 한다.
-- classroom은 CourseScheduleFormatter가 묶음 단위로 쓰므로 강의 안에서는 같은 값이다.
INSERT IGNORE INTO course_schedules
    (course_id, day_of_week, period_code, period_name, classroom, start_time, end_time)
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
       ELT(1 + ((seq.n + slot.s) % 5), 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'),
       LPAD(1 + ((seq.n * 2 + slot.s) % 9), 2, '0'),
       CONCAT(1 + ((seq.n * 2 + slot.s) % 9), '교시'),
       CONCAT(ELT(1 + (seq.n % 5), '1', '2', '3', '4', '5'), '호관 ', 100 + (seq.n % 300)),
       MAKETIME(9 + ((seq.n * 2 + slot.s) % 9), 0, 0),
       MAKETIME(10 + ((seq.n * 2 + slot.s) % 9), 0, 0)
FROM seq CROSS JOIN slot;

ANALYZE TABLE courses, course_schedules;

-- 검증
SELECT 'courses'                    AS table_name,
       count(*)                     AS rows_seeded,
       count(DISTINCT department)   AS dept_cardinality,
       count(DISTINCT area)         AS area_cardinality,
       min(id)                      AS min_id,
       max(id)                      AS max_id
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1;

SELECT 'course_schedules'           AS table_name,
       count(*)                     AS rows_seeded,
       count(*) / NULLIF(count(DISTINCT course_id), 0) AS schedules_per_course,
       count(DISTINCT day_of_week)  AS day_cardinality
FROM course_schedules
WHERE course_id BETWEEN @course_start AND @course_start + @course_count - 1;

-- 교양 조회를 재면 0이어서는 안 된다. 0이면 @course_area_count를 4 이상으로.
SELECT count(*) AS general_education_courses
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1
  AND area NOT IN ('MAJOR_ADVANCED', 'MAJOR_BASIC', 'MAJOR_CORE');

-- FULLTEXT 커버리지. 시드 전 행의 course_code가 'PERF'로 시작하므로 fts_indexed = rows_seeded여야 한다.
-- 적으면 인덱스에서 빠진 행이 있는 것이다(대량 INSERT에서 앞쪽 행이 빠지는 현상이 관측된 적 있다).
-- README의 대량 적재 절차대로 FULLTEXT 인덱스를 떼고 다시 만들면 전 행이 다시 색인된다.
SELECT count(*) AS fts_indexed
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1
  AND MATCH(course_code, haksu_code, title_kr, title_en) AGAINST('PERF' IN BOOLEAN MODE);

-- search 모드의 선택도. 목표는 @course_count / 1000. plain 모드면 0이 정상이다.
SELECT '컴퓨터공학' AS keyword, count(*) AS matched
FROM courses
WHERE MATCH(course_code, haksu_code, title_kr, title_en) AGAINST('컴퓨터공학' IN BOOLEAN MODE)
  AND status = 'ACTIVE';

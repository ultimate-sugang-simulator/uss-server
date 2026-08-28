-- PERF-100 시드 (이슈 공용)
-- 대상: GET /api/v1/courses/search
--
-- 주의: template/seeds/course.sql은 현재 스키마와 맞지 않아 쓰지 못한다.
--   - 없는 컬럼에 INSERT: course_college, course_department, course_classification,
--     course_area, course_type, course_grade, professor_name, classroom,
--     schedule_text, course_day
--   - NOT NULL인데 빠진 컬럼: academic_year, term, haksu_code, classification_code,
--     classification_name, area_code, area_name, type_code, type_name,
--     grade_code, grade_name, concentration_code, concentration_name,
--     english_code, english_name, is_huss_course, status,
--     course_schedules.period_code, period_name, classroom
-- 그래서 이 파일에 직접 쓴다.
--
-- 실행(프로젝트 루트에서):
--   docker exec -i -e MYSQL_PWD=root uss-mysql mysql -u root --default-character-set=utf8mb4 uss_db < .claude/resources/perf/100/seeds.sql
--
-- 되돌리기:
--   DELETE FROM courses WHERE id BETWEEN 1000001 AND 3000000;   -- course_schedules는 FK CASCADE로 함께 지워진다
--   DROP TABLE IF EXISTS perf_seq, perf_digits, perf_two;

-- ── 변수 ────────────────────────────────────────────────
-- 기존 강의 id 최대값이 990001이라 그 위에서 시작한다. 겹치면 되돌리기가 실제 강의를 지운다.
SET @course_start        = 1000001;
SET @course_count        = 2000000;
SET @schedules_per_course = 3;
-- 검색어 조합 수. 접두 40 x 접미 25 = 1000.
-- 조합당 매칭 = @course_count / 1000 = 2000건.
SET @combo_count = 1000;

-- ── 적재 가속 ───────────────────────────────────────────
-- 측정 전에 8번 블록에서 전부 원복한다.
SET SESSION unique_checks = 0;
SET SESSION foreign_key_checks = 0;
SET SESSION sql_log_bin = 0;
SET GLOBAL innodb_flush_log_at_trx_commit = 2;

-- ── 1. 번호 테이블 (0 ~ 1,999,999) ──────────────────────
SELECT '[1/8] 번호 테이블 생성' AS '';

DROP TABLE IF EXISTS perf_seq;
DROP TABLE IF EXISTS perf_digits;
DROP TABLE IF EXISTS perf_two;

CREATE TABLE perf_digits (d INT NOT NULL);
INSERT INTO perf_digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

CREATE TABLE perf_two (t INT NOT NULL);
INSERT INTO perf_two VALUES (0),(1);

CREATE TABLE perf_seq (n BIGINT NOT NULL PRIMARY KEY);
INSERT INTO perf_seq (n)
SELECT a.d + b.d*10 + c.d*100 + e.d*1000 + f.d*10000 + g.d*100000 + t.t*1000000
FROM perf_digits a, perf_digits b, perf_digits c, perf_digits e,
     perf_digits f, perf_digits g, perf_two t;

-- ── 2. FULLTEXT 인덱스 제거 ─────────────────────────────
-- 2,000,000행을 넣는 동안 ngram 인덱스를 증분 유지하면 적재가 몇 배로 느려진다.
-- 적재 후 한 번에 다시 만든다(5번). 최종 인덱스 정의는 V1_0__init_table.sql과 동일하다.
SELECT '[2/8] FULLTEXT 인덱스 제거' AS '';
ALTER TABLE courses DROP INDEX ft_idx_course_search;

-- ── 3. courses 적재 ─────────────────────────────────────
-- title_kr = {접두}{접미}({n})
--   접두는 n % 40, 접미는 FLOOR(n/40) % 25로 골라 두 축이 서로 독립이다.
--   연속한 1000개 n마다 1000개 조합이 정확히 한 번씩 나온다.
--   ngram BOOLEAN MODE는 구(phrase) 검색이라 '컴퓨터공학'은 그 조합 행만 매칭된다.
SELECT '[3/8] courses 2,000,000행 적재 (수 분 소요)' AS '';

INSERT INTO courses
    (id, academic_year, term, title_kr, title_en, course_code, haksu_code,
     college, department, classification_code, classification_name,
     area, area_code, area_name, type_code, type_name,
     grade_code, grade_name, concentration_code, concentration_name,
     credits, is_english_course, english_code, english_name,
     is_huss_course, max_capacity, current_enrollment, status)
SELECT
    @course_start + n,
    2026,
    'FIRST',
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
        '(', n, ')'
    ),
    CONCAT('perf course ', n),
    CONCAT('PERF', LPAD(n, 7, '0')),
    CONCAT('P', LPAD(n, 10, '0')),
    ELT(1 + (n % 8),
        'HUMANITIES','NATURAL_SCIENCES','SOCIAL_SCIENCES','COMMERCE_PUBLIC_AFFAIRS',
        'ENGINEERING','INFORMATION_TECHNOLOGY','BUSINESS','ARTS_PHYSICAL_EDUCATION'),
    ELT(1 + (n % 20),
        'KOREAN_LITERATURE','ENGLISH_LITERATURE','GERMAN_STUDIES','FRENCH_STUDIES',
        'JAPANESE_LITERATURE','CHINESE_STUDIES','MATHEMATICS','PHYSICS','CHEMISTRY',
        'FASHION_INDUSTRY','MARINE_SCIENCE','SOCIAL_WELFARE','MEDIA_COMMUNICATION',
        'LIBRARY_INFO','CREATIVE_HRD','PUBLIC_ADMINISTRATION','POLITICS_DIPLOMACY',
        'ECONOMICS','ECONOMICS_NIGHT','TRADE'),
    LPAD(1 + (n % 6), 2, '0'),
    ELT(1 + (n % 6), '전공심화','전공기초','전공핵심','기초교양','핵심교양','심화교양'),
    ELT(1 + (n % 8),
        'MAJOR_ADVANCED','MAJOR_BASIC','MAJOR_CORE','BASIC_SCIENCE_ENGINEERING',
        'ACADEMIC_FOUNDATION','CORE_INU_SEMINAR','CORE_HUMANITIES','CORE_SOCIAL'),
    LPAD(1 + (n % 8), 2, '0'),
    ELT(1 + (n % 8),
        '전공심화','전공기초','전공핵심','기초과학공학','학문기초','INU세미나','핵심인문','핵심사회'),
    LPAD(1 + (n % 5), 2, '0'),
    ELT(1 + (n % 5), '강의','이론실습','실습','이러닝','원격'),
    LPAD(1 + (n % 5), 2, '0'),
    ELT(1 + (n % 5), '1학년','2학년','3학년','4학년','전학년'),
    LPAD(1 + (n % 3), 2, '0'),
    ELT(1 + (n % 3), '해당없음','연계전공','융합전공'),
    1 + (n % 3),
    IF(n % 20 = 0, 1, 0),
    LPAD(1 + (n % 2), 2, '0'),
    ELT(1 + (n % 2), '해당없음','영어강의'),
    IF(n % 25 = 0, 1, 0),
    30 + (n % 70),
    0,
    'ACTIVE'
FROM perf_seq;

-- ── 4. course_schedules 적재 ────────────────────────────
SELECT '[4/8] course_schedules 6,000,000행 적재 (수 분 소요)' AS '';

INSERT INTO course_schedules
    (course_id, day_of_week, period_code, period_name, classroom, start_time, end_time)
SELECT
    @course_start + q.n,
    ELT(1 + ((q.n + s.t) % 5), 'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'),
    LPAD(1 + ((q.n * 2 + s.t) % 9), 2, '0'),
    CONCAT(1 + ((q.n * 2 + s.t) % 9), '교시'),
    CONCAT(ELT(1 + (q.n % 5), '1','2','3','4','5'), '호관 ', 100 + (q.n % 300)),
    MAKETIME(9 + ((q.n * 2 + s.t) % 9), 0, 0),
    MAKETIME(10 + ((q.n * 2 + s.t) % 9), 0, 0)
FROM perf_seq q
CROSS JOIN (SELECT 0 AS t UNION ALL SELECT 1 UNION ALL SELECT 2) s;

-- ── 5. FULLTEXT 인덱스 재생성 ───────────────────────────
SELECT '[5/8] FULLTEXT 인덱스 재생성 (수 분 소요)' AS '';
ALTER TABLE courses ADD FULLTEXT INDEX ft_idx_course_search
    (course_code, haksu_code, title_kr, title_en) WITH PARSER ngram;

-- ── 6. 옵티마이저 통계 갱신 ─────────────────────────────
SELECT '[6/8] 통계 갱신' AS '';
ANALYZE TABLE courses, course_schedules;

-- ── 7. 검증 ─────────────────────────────────────────────
SELECT '[7/8] 검증' AS '';

SELECT 'courses' AS table_name,
       count(*) AS rows_seeded,
       count(DISTINCT department) AS dept_cardinality,
       count(DISTINCT area) AS area_cardinality,
       count(DISTINCT status) AS status_cardinality,
       min(id) AS min_id, max(id) AS max_id
FROM courses
WHERE id BETWEEN @course_start AND @course_start + @course_count - 1;

SELECT 'course_schedules' AS table_name,
       count(*) AS rows_seeded,
       count(*) / NULLIF(count(DISTINCT course_id), 0) AS schedules_per_course,
       count(DISTINCT day_of_week) AS day_cardinality
FROM course_schedules
WHERE course_id BETWEEN @course_start AND @course_start + @course_count - 1;

SELECT 'courses 전체' AS scope, count(*) AS total FROM courses;

-- 키워드 선택도. 목표는 조합당 2,000건이다.
SELECT '컴퓨터공학' AS kw, count(*) AS matched FROM courses
 WHERE MATCH(course_code,haksu_code,title_kr,title_en) AGAINST('컴퓨터공학' IN BOOLEAN MODE) AND status='ACTIVE'
UNION ALL SELECT '기계설계', count(*) FROM courses
 WHERE MATCH(course_code,haksu_code,title_kr,title_en) AGAINST('기계설계' IN BOOLEAN MODE) AND status='ACTIVE'
UNION ALL SELECT '전자시스템', count(*) FROM courses
 WHERE MATCH(course_code,haksu_code,title_kr,title_en) AGAINST('전자시스템' IN BOOLEAN MODE) AND status='ACTIVE'
UNION ALL SELECT '경영분석', count(*) FROM courses
 WHERE MATCH(course_code,haksu_code,title_kr,title_en) AGAINST('경영분석' IN BOOLEAN MODE) AND status='ACTIVE'
UNION ALL SELECT '바이오실험', count(*) FROM courses
 WHERE MATCH(course_code,haksu_code,title_kr,title_en) AGAINST('바이오실험' IN BOOLEAN MODE) AND status='ACTIVE';

-- ── 8. 적재 가속 원복 ───────────────────────────────────
-- 이 블록을 빼면 측정이 실제 운영 설정과 다른 내구성 설정에서 돌아간다.
SELECT '[8/8] 설정 원복' AS '';
SET GLOBAL innodb_flush_log_at_trx_commit = 1;
SET SESSION unique_checks = 1;
SET SESSION foreign_key_checks = 1;

SELECT @@innodb_flush_log_at_trx_commit AS flush_setting_restored;

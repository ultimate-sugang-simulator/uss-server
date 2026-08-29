-- 회원 시드: members
--
-- 필요한 변수: @member_start, @member_count, @student_id_start, @pw_hash, @member_dept_count (상한 8)
--
-- id를 명시 삽입한다. mint-tokens.sh의 --start / --count가 이 범위와 맞아야 한다.
-- 이메일은 perf{id}@inu.ac.kr, 학번은 @student_id_start부터 1씩 증가한다.

SELECT '[member.sql] members 적재' AS '';

INSERT IGNORE INTO members
    (id, email, student_id, password, name,
     college, department, grade, academic_status,
     last_semester_gpa, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @member_count - 1
)
SELECT @member_start + n,
       CONCAT('perf', @member_start + n, '@inu.ac.kr'),
       CAST(@student_id_start + n AS CHAR),
       @pw_hash,
       CONCAT('perf', n),
       -- college와 department는 같은 인덱스로 골라 단과대학-학과 쌍이 맞는다.
       ELT(1 + (n % @member_dept_count),
           'INFORMATION_TECHNOLOGY', 'ENGINEERING', 'NATURAL_SCIENCES',
           'BUSINESS', 'COMMERCE_PUBLIC_AFFAIRS',
           'ENGINEERING', 'LIFE_SCIENCES_BIOENGINEERING', 'COMMERCE_PUBLIC_AFFAIRS'),
       -- 전공 조회(/api/v1/courses/major)가 이 값을 CourseDepartment.ownedBy로 변환한다.
       -- 앞 5개는 소유한 CourseDepartment가 1개씩이라 department IN 목록이 1개고,
       -- 뒤 3개는 각각 4개, 3개, 2개를 소유해 IN 목록이 여러 개가 된다.
       -- 소유분은 전부 course.sql의 department 목록 앞 14개에 들어 있다.
       ELT(1 + (n % @member_dept_count),
           'COMPUTER_ENGINEERING', 'MECHANICAL_ENGINEERING', 'MATHEMATICS',
           'BUSINESS_ADMINISTRATION', 'ECONOMICS',
           'ELECTRONICS_ENGINEERING_SCHOOL', 'LIFE_SCIENCE_SCHOOL', 'GLOBAL_TRADE_SERVICE'),
       ELT(1 + (n % 4), 'FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR'),
       -- 휴학생을 섞어 학적 상태 분기가 생겼을 때 그대로 쓸 수 있게 한다.
       IF(n % 10 = 0, 'LEAVE_OF_ABSENCE', 'ENROLLED'),
       -- 최대 이수 학점이 성적으로 갈린다(4.0↑ 24 / 3.5↑ 21 / 그 외 19). 세 구간을 모두 만든다.
       ELT(1 + (n % 3), 4.2, 3.7, 3.0),
       NOW() - INTERVAL 200 DAY,
       NOW()
FROM seq;

ANALYZE TABLE members;

-- 검증
SELECT 'members'                          AS table_name,
       count(*)                           AS rows_seeded,
       count(DISTINCT department)         AS dept_cardinality,
       count(DISTINCT last_semester_gpa)  AS gpa_cardinality,
       min(id)                            AS min_id,
       max(id)                            AS max_id,
       min(student_id)                    AS min_student_id,
       max(student_id)                    AS max_student_id
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

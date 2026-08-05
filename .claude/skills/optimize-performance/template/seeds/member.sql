-- 회원 시드: members
--
-- 필요한 변수: @member_start, @member_count, @student_id_start, @pw_hash, @member_dept_count
--
-- id와 학번을 명시 삽입하므로 k6 스크립트의 STUDENT_ID_START / USER_COUNT와 그대로 맞아떨어진다.
-- 학번은 9자리여야 로그인 요청이 검증을 통과한다(LoginRequest의 ^\d{9}$).
-- @pw_hash는 Phase 3-A에서 실제 가입 API로 만든 BCrypt 해시다. 직접 만들어 넣지 마라.

SELECT '[member.sql] members 적재' AS '';

INSERT IGNORE INTO members
    (id, email, student_id, password, name,
     member_college, member_department, member_grade, academic_status,
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
       -- member_college는 아래 member_department가 속한 단과대학과 맞춰야 의미가 통한다.
       ELT(1 + (n % @member_dept_count),
           'INFORMATION_TECHNOLOGY', 'ENGINEERING', 'NATURAL_SCIENCES',
           'BUSINESS', 'COMMERCE_PUBLIC_AFFAIRS'),
       -- 전공 조회(/api/v1/courses/major)가 이 값을 CourseDepartment로 그대로 변환한다.
       -- course.sql이 채우는 course_department 목록과 앞쪽이 겹쳐야 조회 결과가 0건이 아니게 된다.
       ELT(1 + (n % @member_dept_count),
           'COMPUTER_ENGINEERING', 'MECHANICAL_ENGINEERING', 'MATHEMATICS',
           'BUSINESS_ADMINISTRATION', 'ECONOMICS'),
       ELT(1 + (n % 4), 'FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR'),
       -- 휴학생을 섞으면 학적 상태 분기가 생겼을 때 그대로 쓸 수 있다.
       IF(n % 10 = 0, 'LEAVE_OF_ABSENCE', 'ENROLLED'),
       -- 최대 이수 학점이 성적으로 갈린다(4.0↑ 24 / 3.5↑ 21 / 그 외 19).
       -- 수강신청 측정에서 학점 상한에 걸리는 회원과 아닌 회원이 섞이게 3구간을 모두 만든다.
       ELT(1 + (n % 3), 4.2, 3.7, 3.0),
       NOW() - INTERVAL 200 DAY,
       NOW()
FROM seq;

ANALYZE TABLE members;

-- 검증
SELECT 'members'                        AS table_name,
       count(*)                         AS rows_seeded,
       count(DISTINCT member_department) AS dept_cardinality,
       count(DISTINCT last_semester_gpa) AS gpa_cardinality,
       min(id)                          AS min_id,
       max(id)                          AS max_id,
       min(student_id)                  AS min_student_id,
       max(student_id)                  AS max_student_id
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

-- 해시가 제대로 들어갔는지 확인한다. 여기서 어긋나면 Phase 4의 토큰 발급이 통째로 실패한다.
SELECT IF(count(*) = @member_count, 'OK', CONCAT('불일치: ', count(*), '/', @member_count)) AS pw_hash_check
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1
  AND password = @pw_hash;

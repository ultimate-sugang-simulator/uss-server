-- 회원 시드: members
--
-- 필요한 변수: @member_start, @member_count, @student_id_start
--
-- id를 명시 삽입하므로 mint-tokens.sh의 --start / --count와 그대로 맞아떨어진다.
-- password는 BCrypt 해시가 아닌 더미 문자열이다. 측정 토큰은 mint-tokens.sh로 직접 서명해
-- 만들므로 로그인 경로를 타지 않고, BCrypt 형식이 아니면 로그인 시도도 실패로 끝난다.
--
-- 회원 수는 VU 수와 반드시 같아야 한다.
-- 적으면 같은 회원이 두 번 신청해 uk_member_course에 걸리고, 그 실패가 위반 건수를 왜곡한다.

SELECT '[member.sql] members 적재' AS '';

INSERT IGNORE INTO members
    (id, email, password, student_id, name, college, department, grade,
     academic_status, last_semester_gpa, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @member_count - 1
)
SELECT @member_start + n,
       -- 이메일은 uk_email 제약 때문에 행마다 달라야 한다.
       CONCAT('conc', n, '@uss.local'),
       'not-a-bcrypt-hash',
       CAST(@student_id_start + n AS CHAR),
       CONCAT('conc', n),
       -- 수강신청은 소속을 보지 않는다. 분기를 만들 이유가 없으므로 한 값으로 고정한다.
       'INFORMATION_TECHNOLOGY',
       'COMPUTER_ENGINEERING',
       'FRESHMAN',
       'ENROLLED',
       -- 최대 이수 학점을 24로 만든다(4.0 이상). 학점 상한에 먼저 걸리면
       -- 정원 경합에 도달하지 못한 요청을 재게 된다.
       4.2,
       NOW() - INTERVAL 200 DAY,
       NOW()
FROM seq;

ANALYZE TABLE members;

-- 검증
SELECT 'members'          AS table_name,
       count(*)           AS rows_seeded,
       min(id)            AS min_id,
       max(id)            AS max_id,
       min(student_id)    AS min_student_id,
       max(student_id)    AS max_student_id,
       min(last_semester_gpa) AS min_gpa
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

-- 회원 수가 요청한 만큼 들어갔는지 확인한다.
-- 여기서 어긋나면 k6 스크립트가 토큰 수 불일치로 중단한다.
SELECT IF(count(*) = @member_count,
          'OK',
          CONCAT('불일치: ', count(*), '/', @member_count, ' - id 범위가 기존 행과 겹치는지 확인하라')) AS member_count_check
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

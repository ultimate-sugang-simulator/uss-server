-- CONC-98 이슈 전용 시드 꼬리: registrations 채우기
--
-- 필요한 변수: @member_start, @member_count, @target_course_id
-- 선행 모듈: member.sql, contention-course.sql (이 둘 뒤에 붙여야 한다)
--
-- 취소 경합의 출발점은 "만석"이다. 시드 회원 전원이 대상 강의를 신청해둔 상태를 만든다.
-- 애플리케이션 경로(POST /registration)를 타지 않고 직접 넣는 이유는,
-- 신청 경로의 원자적 UPDATE가 이미 #90에서 검증된 구간이라 측정 준비에 섞을 이유가 없어서다.

SELECT '[seed-registrations.sql] 취소 대상 등록 적재' AS '';

INSERT IGNORE INTO registrations (member_id, course_id, created_at)
SELECT id, @target_course_id, NOW()
FROM members
WHERE id BETWEEN @member_start AND @member_start + @member_count - 1;

-- 카운터를 실제 등록 행 수에 맞춘다. 이 값이 어긋난 채로 시작하면
-- 부하 뒤의 I1 위반이 부하 때문인지 시드 때문인지 구분되지 않는다.
UPDATE courses
   SET current_enrollment = (SELECT COUNT(*) FROM registrations WHERE course_id = @target_course_id)
 WHERE id = @target_course_id;

ANALYZE TABLE registrations;

-- 검증
SELECT 'registrations'                                        AS table_name,
       (SELECT COUNT(*) FROM registrations
         WHERE course_id = @target_course_id)                 AS rows_seeded,
       (SELECT current_enrollment FROM courses
         WHERE id = @target_course_id)                        AS counter,
       (SELECT max_capacity FROM courses
         WHERE id = @target_course_id)                        AS max_capacity;

-- 측정을 시작할 수 있는 상태인지 한 줄로 확인한다.
-- 등록 행 수 = 카운터 = 회원 수여야 한다. 셋 중 하나라도 어긋나면 부하를 주지 마라.
SELECT IF((SELECT COUNT(*) FROM registrations WHERE course_id = @target_course_id) = @member_count
              AND (SELECT current_enrollment FROM courses WHERE id = @target_course_id) = @member_count,
          'OK',
          '만석 상태가 아니다. member.sql이 회원을 다 넣었는지, 모듈 순서가 맞는지 확인하라'
       ) AS seed_state_check;

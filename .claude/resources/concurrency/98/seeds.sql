-- CONC-98 시드
-- 대상: RegistrationService.deleteRegisteredCourse (취소 경합)
--
-- 이 파일은 **변수 블록만** 담는다. 모듈 본문을 복사하지 않는다.
--
-- 취소 대상은 신청 대상과 출발점이 다르다. 신청은 등록 0건에서 시작하지만
-- 취소는 대상 강의가 이미 차 있어야 한다. 그래서 공용 모듈 뒤에
-- 이슈 전용 꼬리(seed-registrations.sql)를 하나 더 붙인다. 순서를 바꾸지 마라.
-- contention-course.sql이 말미에 registrations를 비우고 카운터를 0으로 되돌리므로,
-- 등록을 채우는 것은 반드시 그 뒤여야 한다.
--
-- 실행 (프로젝트 루트에서):
--   mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot uss_db "$@"; }
--   cat .claude/resources/concurrency/98/seeds.sql \
--       .claude/skills/fix-concurrency/template/seeds/member.sql \
--       .claude/skills/fix-concurrency/template/seeds/contention-course.sql \
--       .claude/resources/concurrency/98/seed-registrations.sql \
--     | mysqlc -t

-- MySQL의 재귀 CTE 기본 깊이 상한은 1000이다. VU가 1000을 넘으면 이걸 올려야 한다.
SET SESSION cte_max_recursion_depth = 100000;

-- 회원: VU 수와 반드시 같아야 한다.
SET @member_start     = 900001;
SET @member_count     = 500;
SET @student_id_start = 900000001;

-- 경합 대상 강의: 딱 하나만 만든다.
-- 앱 시드의 강의 id는 1~2439이므로 990001은 겹치지 않는다.
-- invariant-check.sql이 이 id를 하드코딩하고 있다. 바꾸려면 그쪽도 함께 본다.
SET @target_course_id = 990001;

-- 정원을 회원 수와 같게 둔다. 취소 단독 측정에서는 정원이 판정에 쓰이지 않지만,
-- 500명이 등록된 상태가 정원 안에 들어와야 시드가 정합하다.
-- #90은 같은 강의를 정원 100으로 썼다. contention-course.sql이 매번 이 값으로 덮어쓴다.
SET @target_capacity  = 500;

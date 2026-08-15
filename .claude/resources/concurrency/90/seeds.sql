-- CONC-90 시드
-- 대상: RegistrationService.registerCourse (정원 경합)
--
-- 이 파일은 **변수 블록만** 담는다. 모듈 본문을 복사하지 않는다.
--
-- 스킬 템플릿은 여기에 SOURCE 줄을 두라고 하지만, 이 환경에서는 동작하지 않는다.
-- 호스트에 mysql 클라이언트가 없어 컨테이너 안의 클라이언트를 쓰는데,
-- SOURCE는 클라이언트가 직접 파일을 여는 명령이라 컨테이너 파일시스템에서 경로를 찾는다.
-- 그래서 모듈을 실행 시점에 이어 붙여 한 세션으로 흘려보낸다. 변수는 세션이 하나라 그대로 이어진다.
--
-- 실행 (프로젝트 루트에서):
--   mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot uss_db "$@"; }
--   cat $CONC_DIR/seeds.sql \
--       .claude/skills/fix-concurrency/template/seeds/member.sql \
--       .claude/skills/fix-concurrency/template/seeds/contention-course.sql \
--     | mysqlc -t

-- MySQL의 재귀 CTE 기본 깊이 상한은 1000이다. VU가 1000을 넘으면 이걸 올려야 한다.
SET SESSION cte_max_recursion_depth = 100000;

-- 회원: VU 수와 반드시 같아야 한다.
SET @member_start     = 900001;
SET @member_count     = 500;
SET @student_id_start = 900000001;

-- 경합 대상 강의: 딱 하나만 만든다.
-- 앱 시드의 강의 id는 1~2439이므로 990001은 겹치지 않는다.
SET @target_course_id = 990001;
SET @target_capacity  = 100;

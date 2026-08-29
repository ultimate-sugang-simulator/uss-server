-- 측정 근거 없이 만들어진 보조 인덱스를 걷어낸다.
-- 이슈 #104의 성능 개선은 인덱스 없는 상태를 기준선으로 잡고, 관측된 실행계획을 근거로 인덱스를 다시 설계한다.
-- PK, UNIQUE 제약, FK를 받치는 인덱스, FULLTEXT 인덱스는 대상이 아니다.

-- courses: 조회 조건 + ORDER BY를 그대로 옮겨 담은 5컬럼 복합 인덱스 3개.
-- /major, /other-department, /general-education, /huss 의 정렬 조회를 받치던 인덱스다.
ALTER TABLE courses DROP INDEX idx_department_sort;
ALTER TABLE courses DROP INDEX idx_area_sort;
ALTER TABLE courses DROP INDEX idx_huss_sort;

-- members: V1_9에서 포털 로그인을 회원 계정으로 교체하면서 student_id 조회가 사라졌다.
-- 현재 MemberRepository는 email로만 조회하므로 이 인덱스를 타는 쿼리가 없다.
ALTER TABLE members DROP INDEX idx_student_id;

-- course_sync_jobs: 관리자 동기화 이력 조회용.
-- findFirstByOrderByStartedAtDesc, findFirstByStatus 가 쓰던 인덱스다.
ALTER TABLE course_sync_jobs DROP INDEX idx_started_at;
ALTER TABLE course_sync_jobs DROP INDEX idx_status;

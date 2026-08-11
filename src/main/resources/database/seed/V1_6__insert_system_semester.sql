-- 표시 학기 초기 1행. 조회 API가 행이 없으면 실패하므로 시드가 존재를 보장한다.
-- 값은 백오피스에서 언제든 바꿀 수 있다. 적재된 강의 학기와 일치할 필요는 없다.
INSERT INTO system_semesters (academic_year, term, updated_at)
VALUES (2026, 'SECOND', NOW());

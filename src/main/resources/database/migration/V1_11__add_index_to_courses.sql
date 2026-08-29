-- 전공 강의 조회(GET /api/v1/courses/major)가 courses를 풀스캔하는 것을 없앤다.
-- V1_10에서 걷어낸 idx_department_sort와 이름은 같지만 컬럼 구성이 다르다.
-- 선택도가 0인 status(전 행 ACTIVE)를 선두에서 빼고, department를 앞에 두어 조회 범위를 좁힌다.
-- 뒤따르는 세 컬럼은 ORDER BY grade_code, classification_code, haksu_code 와 순서가 같아
-- IN 목록이 하나일 때 정렬을 인덱스로 대신한다.
-- 측정 근거는 .claude/resources/perf/104/major/record.md 의 사이클 1이다.
ALTER TABLE courses ADD INDEX idx_department_sort (department, grade_code, classification_code, haksu_code);

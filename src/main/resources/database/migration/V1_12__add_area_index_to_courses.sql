-- 교양 강의 조회(GET /api/v1/courses/general-education)가 courses를 풀스캔하는 것을 없앤다.
-- V1_10에서 걷어낸 idx_area_sort와 이름은 같지만, V1_5가 선두에 뒀던 status(전 행 ACTIVE, 선택도 0)를 빼고
-- 등호 필터 area를 선두에 둔다. 뒤따르는 세 컬럼은 ORDER BY grade_code, classification_code, haksu_code 와
-- 순서가 같아 정렬을 인덱스로 대신한다. haksu_code는 선택도가 아니라 정렬 대체를 완성하기 위해 들어간다.
-- 측정 근거는 .claude/resources/perf/110/general-education/record.md 의 사이클 1이다.
ALTER TABLE courses ADD INDEX idx_area_sort (area, grade_code, classification_code, haksu_code);

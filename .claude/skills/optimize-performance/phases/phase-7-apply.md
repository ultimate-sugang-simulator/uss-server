## Phase 7. 개선 적용

### 목적
호출자가 고른 기법 하나를 코드에 반영하고, 동작이 그대로인지 확인한다.

### 선행 조건
- Phase 6 완료

### 참조 파일
- `.claude/rules/code-convention/`
- `.claude/rules/migration.md`
- `.claude/spec/service-policy/`

### 절차

1. 호출자가 고른 기법을 적용한다.
   - Phase 5-B에서 확정한 설계 결정 그대로 구현한다.
   - **이번 대상의 개선만 적용한다.** 같은 이슈의 다른 대상에도 통할 것 같은 변경이라도 여기서 함께 넣지 마라.
     아직 기준선을 잡지 않은 대상에 코드가 먼저 들어가면 그 대상의 `-0`이 원본이 아니게 된다.
     그런 변경이 보이면 다음 대상의 Phase 5에서 후보로 꺼낸다. (`SKILL.md`의 **대상 진행 규칙**)

2. 수정해야 할 레이어에 맞는 코드 컨벤션 파일을 Read로 읽고 그에 맞춰 작성한다.

3. 인덱스 추가를 비롯한 스키마 변경은 `.claude/rules/migration.md`를 따라 Flyway 마이그레이션 파일로 작성한다.
   - 이미 적용된 마이그레이션 파일을 고치지 마라. 새 버전 파일로 추가한다.
   - 인덱스는 기존 테이블 정의 안이 아니라 `ALTER TABLE`로 추가한다.

     ```sql
     -- V0_7__add_index_to_courses.sql
     ALTER TABLE courses ADD INDEX idx_department_grade (course_department, course_grade);
     ```

   - 마이그레이션 적용 후 대상 테이블의 통계를 갱신하도록 호출자에게 제시한다.
     인덱스를 만든 직후의 통계는 실제 분포와 어긋나 있을 수 있다.

     ```bash
     $MYSQL_PERF -e "ANALYZE TABLE {테이블};"
     ```

4. 개선이 실제로 그 쿼리에 붙었는지 **적용 직후에 한 번 확인한다.** 재측정 전에 확인해야
   Phase 8에서 "효과 없음"과 "적용 안 됨"을 구분할 수 있다.

   ```bash
   $MYSQL_PERF -e "EXPLAIN {대상 쿼리}\G" | grep -E 'key|type|rows|Extra'
   ```

   - 새로 만든 인덱스가 `key`에 잡히지 않으면 그 사실을 먼저 보고하고, Phase 8로 넘어가지 마라.
     컬럼 순서, 함수 감싸기, 타입 불일치를 순서대로 확인한다.

5. 테스트를 실행하도록 호출자에게 제시하고 결과를 받는다. **실행은 호출자가 한다.**

   ```bash
   ./gradlew test
   ```

   - 실패한 테스트가 있으면 원인을 짚어 보고하고, 해소 전까지 Phase 8로 넘어가지 마라.
   - 실패 원인이 이번 변경과 무관하다는 판단이 서면 근거를 밝히고 호출자의 확답을 받는다.

6. 무엇을 어떻게 바꿨는지 `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 사이클 {n} **적용 내용**에
   수정한 파일 경로 + 변경 요지를 작성한다. 4번의 확인 결과와 테스트 결과도 같은 항목에 적는다.

### 출력
- 코드 변경
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 사이클 {n} **적용 내용**이 채워짐
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 진행 상태의 사이클 {n} Phase 7이 ✅으로 기록

### 실패 처리
- 없음

> 다음 Phase 조건: 설계대로 적용되었고 테스트가 통과했을 때 → Phase 8.
> 이번 변경과 무관한 실패를 호출자가 승인한 경우, 그 근거를 **적용 내용**에 적은 뒤 → Phase 8

> Skip 조건: 없음 (필수 Phase)

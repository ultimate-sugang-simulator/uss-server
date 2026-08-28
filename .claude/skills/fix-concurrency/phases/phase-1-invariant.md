## Phase 1. 임계 구역과 불변식 확정

### 목적
이슈와 작업 브랜치를 확보하고, 어떤 공유 자원의 어느 구간이 경합하는지 짚은 뒤,
**깨지면 안 되는 불변식을 호출자와 함께 문장으로 못 박아** `record.md`를 생성한다.

불변식이 정의되지 않으면 무엇이 깨졌는지 셀 수 없고, 셀 수 없으면 고쳤는지도 확인할 수 없다.
이 Phase의 산출물이 이후 모든 판정의 기준이 된다.

### 선행 조건
- SKILL.md에서 전달받은 대상이 있다. 비어있다면 중단하고 호출자에게 질의한다.
- 진행 중인 다른 대상이 없다. 있다면 그 대상이 Phase 8까지 끝난 뒤에 이 Phase를 시작한다.

### 참조 파일
- `.claude/skills/fix-concurrency/template/CONCURRENCY-template.md`
- `.claude/spec/git-convention.md`
- `.claude/spec/service-policy/` (대상 도메인 파일이 있으면)

### 절차

1. 현재 브랜치에서 이슈 번호를 확보한다.

   ```bash
   git branch --show-current
   ```

   - 브랜치명이 `{종류}/{이슈번호}-{slug}` 형식이면 이슈 번호를 뽑아 이슈를 조회한다.

     ```bash
     gh issue view {이슈번호} --json number,title,state,url
     ```

   - 조회 결과(번호, 제목, 상태)를 호출자에게 보고하고, 이 이슈로 진행할지 확답을 받는다.
   - 이슈가 `CLOSED`면 그 사실을 함께 알리고, 확답 전까지 다음 단계로 넘어가지 마라.
   - 브랜치에서 번호를 못 뽑거나, 호출자가 다른 이슈를 원하면 `open-issue` 스킬을 호출해 이슈와 브랜치를 확보한다.

2. 대상이 여러 개면 **하나로 좁힌다.** 나머지는 "이 대상이 Phase 8까지 끝난 뒤에 진행한다"고 알린다.
   대기 목록을 파일로 만들지 마라.

3. 대상 임계 구역의 **슬러그**를 정한다. 이 슬러그가 대상 디렉토리 이름이 된다.
   - 경합하는 동작을 케밥 케이스로 쓴다. (`registerCourse` → `register-course`)
   - 같은 이슈의 기존 대상과 겹치면 도메인을 접두로 붙인다. (`registration-capacity`)
   - 정한 슬러그를 호출자에게 알린다.

4. 실행 경로를 Controller → Service → Repository 순으로 읽고, **임계 구역을 짚어 제시한다.**
   여기까지는 사실만 펼친다. 해석을 붙이지 마라.

   확인해 제시할 것:

   | 항목 | 무엇을 찾는가 |
   |---|---|
   | 공유 자원 | 여러 요청이 동시에 읽고 쓰는 행, 컬럼, 집계 값 |
   | read-modify-write 구간 | 읽은 값을 근거로 판단한 뒤 그 값을 갱신하는 코드 범위 |
   | 실제 발행 SQL | dirty checking이 만드는 `UPDATE`가 절대값 대입인지 증감식인지 |
   | 트랜잭션 경계 | `@Transactional`이 임계 구역 전체를 감싸는지, 어디서 열리고 닫히는지 |
   | 기존 방어 장치 | `@Version`, 비관적 락, UNIQUE 제약, CHECK 제약의 유무 |
   | DB 제약이 잡아주는 범위 | 애플리케이션 검증이 뚫려도 제약이 막는 것과 못 막는 것 |

   - JPA 더티 체킹이 만드는 SQL 형태를 반드시 확인한다. 엔티티 필드를 `++`로 올리면
     `SET col = {읽은 값 + 1}`이 나간다. `SET col = col + 1`이 아니다. 이 차이가 결함의 원인인 경우가 많다.
   - UNIQUE 제약이 막아주는 경합은 **데이터는 지켜지지만 응답 코드가 틀어질 수 있다.**
     제약 위반 예외를 받는 핸들러가 있는지 `GlobalExceptionHandler`에서 확인한다.

5. **호출자에게 불변식을 먼저 묻는다.** `SKILL.md`의 **분석 주도 규칙**을 따른다.

   - 묻는다: "이 동작이 동시에 일어나도 항상 참이어야 하는 것은 무엇입니까?"
   - 답을 받으면 각 항목이 **SQL로 셀 수 있는 형태**인지 판정한다. 셀 수 없는 불변식은 불변식이 아니다.

     | 쓸 수 있음 | 쓸 수 없음 |
     |---|---|
     | `등록 행 수 <= 정원` | "정원이 잘 지켜진다" |
     | `집계 컬럼 = COUNT(실제 행)` | "카운터가 정확하다" |
     | `집계 컬럼 >= 0` | "음수가 안 나온다" (검증 쿼리가 없으면 못 센다) |

   - 호출자가 놓치기 쉬운 축을 짚는다. 답에 없으면 후보로 제시하고 채택 여부를 묻는다.
     - **역방향 동작.** 증가만 보고 감소를 빼먹는 경우가 많다
     - **집계 값과 실제 행 수의 일치.** 상한을 안 넘겨도 갱신이 유실됐을 수 있다
     - **응답 코드.** 데이터가 지켜져도 사용자가 500을 받으면 결함이다
   - 호출자가 답하기 어려워하면 그때 목록을 제시하고 근거가 되는 코드 위치를 함께 보여준다.

6. 확정한 불변식마다 **검증 SQL**을 함께 확정한다.
   - `template/invariant-check.sql`을 Read해 구조를 따르고, 대상에 맞게 조정한다.
   - 불변식 하나가 결과 한 행으로 나오고, `violations` 칼럼이 0이면 통과인 형태로 만든다.
   - 이 SQL은 Phase 4와 7에서 **그대로** 재사용된다. 후보마다 바꾸지 마라.

7. 작업 디렉토리 `.claude/resources/concurrency/{이슈번호}/{슬러그}/`를 만들고 그 안에 `record.md`를 생성한다.
   - `template/CONCURRENCY-template.md`를 Read해 그 구조 그대로 만든다.
   - **대상**, **불변식**, **진행 상태**의 Phase 1을 채운다.
   - 셸 변수를 잡도록 호출자에게 제시한다. 이후 모든 셸 명령이 이 변수들을 쓴다.

     ```bash
     export CONC_DIR=.claude/resources/concurrency/{이슈번호}
     export TARGET_DIR=$CONC_DIR/{슬러그}

     mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot --default-character-set=utf8mb4 --init-command="SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci" uss_db "$@"; }
     ```

   - `mysqlc`는 문자열 변수가 아니라 함수다. 이유는 `SKILL.md`의 **측정 스택**을 보라.
     `Read`와 `Write`의 대상 경로에는 셸 변수가 통하지 않으므로 전체 경로를 쓴다.

### 출력
- `.claude/resources/concurrency/{이슈번호}/{슬러그}/record.md` 생성
- `record.md`의 **대상**에 임계 구역과 실제 발행 SQL 형태가 기록
- `record.md`의 **불변식**에 불변식 문장과 검증 SQL이 기록
- `record.md`의 진행 상태의 Phase 1이 ✅로 기록

### 실패 처리
- 임계 구역이 여러 곳으로 흩어져 있으면 그 목록을 보고하고, 이번 대상에서 다룰 범위를 호출자에게 확정받는다.

> 다음 Phase 조건: 불변식이 검증 SQL과 함께 `record.md`에 적혔을 때 → Phase 2

> Skip 조건: 없음 (필수 Phase)

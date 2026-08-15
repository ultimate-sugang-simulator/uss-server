## Phase 3. 경합 조건 설계

### 목적
결함이 드러날 만큼 센 경합을 만든다. 시드, 토큰, 폭발 부하 스크립트, 되돌리기 SQL을 준비한다.

경합이 약하면 결함은 재현되지 않는다. **재현되지 않은 결함은 존재하지 않는 것과 구분되지 않는다.**

### 선행 조건
- Phase 2 완료

### 참조 파일
- `.claude/skills/fix-concurrency/template/seeds/README.md`
- `.claude/skills/fix-concurrency/template/mint-tokens.sh`
- `.claude/skills/fix-concurrency/template/k6-burst-template.js`

### 절차

#### 3-A. 경합 조건 확정

1. **경합 조건을 호출자와 확정한다.** 아래 세 값이 이후 모든 측정에서 고정된다.

   | 값 | 정하는 기준 |
   |---|---|
   | 다투는 자원의 수 | **1개로 좁힌다.** 여러 행에 흩으면 행마다 경합이 옅어져 결함이 안 드러난다 |
   | 허용 상한 | 그 자원이 받아줄 수 있는 최대치. 수강신청이면 대상 강의의 정원 |
   | 동시 요청 수 (VU) | **허용 상한보다 충분히 크게.** 초과분이 곧 위반 후보다 |

   - 권장 출발점은 **허용 상한의 3~5배**다. 정원 100이면 VU 300~500.
   - VU는 Phase 2에서 확인한 커넥션 풀 크기를 넘기지 마라. 넘기면 락 대기가 아니라 커넥션 대기를 재게 된다.
     풀을 못 올리면 상한을 낮춰 비율을 맞춘다. (풀 100이면 정원 20에 VU 100)
   - 정한 값을 `record.md`의 **경합 조건**에 적는다.

2. **요청 하나가 대상 검증만 통과하도록 조건을 정리한다.**
   다른 검증에 먼저 걸리면 경합 지점까지 도달하지 못한 요청을 재게 된다.

   수강신청 경로에서 확인할 것:

   | 검증 | 통과 조건 |
   |---|---|
   | `validateCourseActive` | 대상 강의 `status = 'ACTIVE'` |
   | `validateDuplicateCourse`, `uk_member_course` | **VU마다 서로 다른 회원.** 회원 수 = VU 수 |
   | `validateCreditLimit` | 회원의 최대 이수 학점 이상으로 미리 등록해두지 않는다. 시드는 등록 0건에서 시작 |
   | `validateCourseScheduleConflict` | 기존 등록이 없으면 자동 통과 |
   | `validateCourseTypeLimit` | 대상 강의의 `type_code`를 OCU, K-MOOC가 아닌 값으로 둔다 |

   - 회원 수가 VU보다 적으면 중복 신청으로 실패한 요청이 섞여 위반 건수가 왜곡된다. **반드시 일치시킨다.**

#### 3-B. 시드

3. `.claude/resources/concurrency/{이슈번호}/seeds.sql`을 만든다.
   `template/seeds/README.md`의 작성 규칙을 따른다. 모듈 본문을 복사하지 마라.

4. 시드를 적재하고 결과를 받는다. 실행은 호출자가 프로젝트 루트에서 한다.

   ```bash
   $MYSQL_CONC < $CONC_DIR/seeds.sql
   ```

   - 각 모듈 말미의 검증 쿼리 결과로 행 수와 대상 강의의 정원을 확인한다.
   - 회원 수가 3-A에서 정한 VU와 다르면 여기서 멈춘다.

#### 3-C. 토큰

5. **측정용 토큰을 서명키로 직접 만든다.** 로그인 API를 쓰지 마라.
   로그인은 학교 포털 Oracle 함수를 호출하므로 로컬에서 성공하지 않는다.

   ```bash
   bash .claude/skills/fix-concurrency/template/mint-tokens.sh \
     --secret "$(grep -A2 'jwt:' src/main/resources/application-conc.yml | grep 'secret-key' | sed 's/.*secret-key: *//')" \
     --start {시드 회원 id 시작값} \
     --count {VU 수} \
     --out $CONC_DIR/tokens.json

   jq 'length' $CONC_DIR/tokens.json
   ```

6. **토큰이 실제로 통하는지 한 번 확인한다.** 여기서 401이 나면 부하 전체가 401로 끝난다.

   ```bash
   TOKEN=$(jq -r '.[0].accessToken' $CONC_DIR/tokens.json)
   curl -s -o /dev/null -w '%{http_code}\n' \
     -H "access-token: $TOKEN" \
     localhost:8080/api/v1/members/profile
   ```

   - 200이 아니면 서명키가 `application-conc.yml`의 값과 같은지, 회원 id가 실재하는지 확인한다.
   - 이 서버는 `access-token` 헤더 하나만 요구한다. `Authorization: Bearer` 형식이 아니고 refresh 토큰도 없다.

#### 3-D. 부하 스크립트와 되돌리기

7. `template/k6-burst-template.js`를 Read해 `$TARGET_DIR/burst-script.js`로 만든다.
   템플릿 상단의 작성 규칙을 따른다.

   - **executor는 `per-vu-iterations`, `iterations: 1`이다.** VU마다 요청을 정확히 한 번 보내
     "N명이 동시에 버튼을 누른다"를 만든다. ramp 구간을 두지 마라. 램프를 두면 경합이 시간축으로 흩어진다.
   - 토큰은 `__VU`로 고른다. VU와 회원이 1대1로 묶여야 중복 신청이 섞이지 않는다.
     optimize-performance 템플릿의 `iterationInTest` 방식을 가져오지 마라.

8. **되돌리기 SQL을 확정해 `record.md`에 적는다.** 매 측정 전에 이걸 돌린다.

   ```sql
   -- 대상 강의의 등록을 모두 지우고 카운터를 0으로 되돌린다
   DELETE FROM registrations WHERE course_id = {대상 강의 id};
   UPDATE courses SET current_enrollment = 0 WHERE id = {대상 강의 id};

   -- 되돌아갔는지 확인
   SELECT (SELECT COUNT(*) FROM registrations WHERE course_id = {대상 강의 id}) AS rows_left,
          (SELECT current_enrollment FROM courses WHERE id = {대상 강의 id}) AS counter;
   ```

   - 두 값이 모두 0이어야 다음 측정이 성립한다.
   - **되돌리기를 건너뛰면 정원이 이미 찬 상태에서 재게 되어 전 요청이 정원 초과로 실패한다.**
     그 결과는 "정합성이 지켜졌다"로 보이지만 아무것도 검증하지 못한 것이다.

9. 확정한 조건과 산출물 경로를 `record.md`의 **경합 조건**에 채운다.

### 출력
- `.claude/resources/concurrency/{이슈번호}/seeds.sql` 생성, 적재 완료
- `.claude/resources/concurrency/{이슈번호}/tokens.json` 생성, 인증 확인 완료
- `.claude/resources/concurrency/{이슈번호}/{슬러그}/burst-script.js` 생성
- `record.md`의 **경합 조건**에 자원 수, 허용 상한, VU, 되돌리기 SQL이 기록
- `record.md`의 진행 상태의 Phase 3이 ✅로 기록

### 실패 처리

| 증상 | 처리 |
|---|---|
| 시드 회원 수 ≠ VU | 시드를 다시 적재한다. 그대로 진행하지 마라 |
| 토큰 확인이 401 | 서명키 불일치 또는 회원 id 부재. 부하로 넘어가지 마라 |
| VU를 풀 크기 이상으로 잡아야 함 | 허용 상한을 낮춰 비율을 맞춘다 |

> 다음 Phase 조건: 시드, 토큰, 스크립트, 되돌리기 SQL이 모두 준비되고 토큰 인증이 확인되었을 때 → Phase 4

> Skip 조건: 2회차 이상이고 시드, 토큰, 스크립트가 이미 준비되어 있으면 → Phase 4
> (단 되돌리기 SQL은 매번 실행한다. 준비물이 있다는 것과 데이터가 초기 상태라는 것은 다르다)

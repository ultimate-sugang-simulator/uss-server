## Phase 4. 결함 재현

### 목적
아무 제어도 없는 원본 상태(`-0`)에서 불변식이 실제로 깨지는 것을 관측한다.
이 측정이 모든 후보의 비교 기준이 된다.

**게이트.** 위반이 관측되지 않으면 Phase 5로 넘어가지 마라.
재현하지 못한 결함은 고쳤는지도 확인할 수 없다.

### 선행 조건
- Phase 3 완료
- 애플리케이션이 **아무 동시성 제어도 적용하지 않은 원본 코드**로 기동되어 있다
- `git status`가 깨끗하다 (후보 코드가 섞여 있으면 `-0`이 원본이 아니게 된다)

### 참조 파일
- `.claude/skills/fix-concurrency/template/invariant-check.sql`

### 절차

1. 아래 블록을 호출자에게 제시하고 결과를 받는다. **실행은 호출자가 한다.**

   **레포 루트에서 이 블록 전체를 한 번에 붙여넣는다.** 변수와 함수 정의를 매번 포함하므로
   새 터미널이든 아니든 그대로 돈다(`SKILL.md`의 **명령 전달**).

   ```bash
   CONC_DIR=.claude/resources/concurrency/{이슈번호}
   TARGET_DIR=$CONC_DIR/{슬러그}
   mysqlc() { docker exec -i -e MYSQL_PWD=root uss-mysql mysql -uroot uss_db "$@"; }

   N=0   # 후보 번호. 원본은 0이다

   # 1) 되돌리기 - record.md에 적어둔 SQL을 그대로 실행한다
   mysqlc -e "
   DELETE FROM registrations WHERE course_id = {대상 강의 id};
   UPDATE courses SET current_enrollment = 0 WHERE id = {대상 강의 id};
   SELECT (SELECT COUNT(*) FROM registrations WHERE course_id = {대상 강의 id}) AS rows_left,
          (SELECT current_enrollment FROM courses WHERE id = {대상 강의 id}) AS counter;"

   # 2) 락 카운터 - 부하 직전 스냅샷
   {
     echo "=== BEFORE ==="
     mysqlc -e "SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';"
     mysqlc -e "
     SELECT NAME, COUNT FROM information_schema.INNODB_METRICS
     WHERE NAME IN ('lock_deadlocks','lock_timeouts','lock_row_lock_waits');"
   } > $TARGET_DIR/lock-stats-$N.txt

   # 3) 폭발 부하
   k6 run -e SUMMARY_OUT=$TARGET_DIR/k6-burst-summary-$N.json $TARGET_DIR/burst-script.js

   # 4) 락 카운터 - 부하 직후 스냅샷
   {
     echo "=== AFTER ==="
     mysqlc -e "SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';"
     mysqlc -e "
     SELECT NAME, COUNT FROM information_schema.INNODB_METRICS
     WHERE NAME IN ('lock_deadlocks','lock_timeouts','lock_row_lock_waits');"

     echo "=== LATEST DEADLOCK ==="
     mysqlc -e "SHOW ENGINE INNODB STATUS\G" \
       | sed -n '/LATEST DETECTED DEADLOCK/,/^---/p'
   } >> $TARGET_DIR/lock-stats-$N.txt

   # 5) 불변식 검증 - Phase 1에서 확정한 SQL을 그대로 쓴다
   mysqlc -t < $TARGET_DIR/invariant-check.sql \
     | tee $TARGET_DIR/invariant-$N.txt
   ```

   - **워밍업을 돌리지 마라.** 폭발 부하는 한 번만 터뜨린다. 워밍업이 정원을 채워버린다.
   - 락 카운터는 전역 누적값이다. 측정 구간의 증가분은 BEFORE와 AFTER의 차이로 낸다.
   - `LATEST DETECTED DEADLOCK` 섹션이 비어 있으면 이번 부하에서 데드락이 없었거나
     인스턴스 기동 후 한 번도 없었던 것이다. 둘을 구분하려면 `lock_deadlocks` 증가분을 본다.

2. 실행이 끝나면 아래 파일을 Read로 읽는다. 터미널 출력을 붙여넣게 하지 마라.

   | 산출물 | 파일 |
   |---|---|
   | 불변식 검증 | `invariant-0.txt` |
   | k6 요약 | `k6-burst-summary-0.json` |
   | 락 지표 | `lock-stats-0.txt` |

3. **재현 여부를 먼저 판정한다.** 이 판정이 게이트다.

   | 조건 | 판정 |
   |---|---|
   | `violations`가 하나라도 0이 아님 | **재현됨.** Phase 5로 간다 |
   | 전부 0인데 요청이 대부분 실패 | **재현 실패.** 요청이 경합 지점에 도달하지 못했다 |
   | 전부 0이고 요청도 정상 | **재현 실패.** 경합이 약하다 |

   재현에 실패했으면 원인을 아래 순서로 짚고 Phase 3으로 돌아간다.

   - 응답 코드 분포를 먼저 본다. 성공 수가 허용 상한보다 한참 적으면 다른 검증에 걸린 것이다
     (중복 신청, 학점 상한, 시간 충돌). Phase 3-A의 2번 표를 다시 확인한다
   - 되돌리기가 실제로 됐는지 본다. 1번의 `rows_left`, `counter`가 0이 아니었으면 그것이 원인이다
   - 위 둘이 아니면 VU를 올리거나 허용 상한을 낮춰 경합을 세게 만든다
   - **위반이 안 나온다고 "문제가 없다"고 결론짓지 마라.** 재현 조건을 못 만든 것과 결함이 없는 것은 다르다.
     세 번 이상 조건을 바꿔도 재현되지 않으면 그 사실을 호출자에게 보고하고 판단을 받는다

4. 관측 자료를 표로 정리해 제시하고, **해석은 호출자에게 묻는다.** `SKILL.md`의 **분석 주도 규칙**을 따른다.

   **정합성 (1급)**

   | 불변식 | 기대 | 실측 | 위반 |
   |---|---|---|---|

   **응답 분포**

   | 상태 코드 | 건수 | 의미 |
   |---|---|---|

   **경합 (2급)**

   | 지표 | BEFORE | AFTER | 증가분 |
   |---|---|---|---|

   - 물을 것: "이 수치가 나오려면 요청들이 어떤 순서로 겹쳐야 했을까요?"
   - 특히 **카운터 정합 오차**를 짚어 묻는다. 이 값이 lost update의 직접 증거다.
     등록 행은 N개인데 카운터가 1이면, N개의 트랜잭션이 같은 값을 읽고 같은 값을 썼다는 뜻이다.
   - 호출자가 답하기 어려워하면 그때 인터리빙을 시간순으로 그려 보여준다.

5. 결과를 `record.md`의 **재현 (Baseline)**에 채운다. 원본 수치를 옮겨 적지 말고 판정과 근거만 적는다.

### 출력
- `$TARGET_DIR/invariant-0.txt`, `k6-burst-summary-0.json`, `lock-stats-0.txt` 생성
- `record.md`의 **재현 (Baseline)**에 불변식별 위반 건수, 응답 분포, 락 지표, 진단이 기록
- `record.md`의 진행 상태의 Phase 4가 ✅로 기록

### 실패 처리
- 요청 실패율이 높아 경합 지점에 도달하지 못했으면 **수치를 해석하지 말고** 원인부터 보고한다.
- 애플리케이션이 부하 중 죽었으면 그 사실과 로그를 먼저 보고한다. 부분 결과로 판정하지 마라.

> 다음 Phase 조건: 불변식 위반이 관측되어 `record.md`에 기록되었을 때 → Phase 5

> 재현 실패 시 → Phase 3 (경합 조건을 다시 잡는다)

> Skip 조건: 없음 (필수 Phase)

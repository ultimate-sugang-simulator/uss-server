## Phase 3. 측정 조건 구성

### 목적
데이터 규모와 카디널리티를 확정해 시드를 채우고(3-A, 이슈 공용),
부하 조건을 확정해 k6 스크립트를 만든다(3-B, 대상별).

### 선행 조건
- Phase 2 완료

### 참조 파일
- `.claude/skills/optimize-performance/template/seeds/README.md`
- `.claude/skills/optimize-performance/template/k6-script-template.js`

---

### 3-A. 데이터셋 (이슈 공용)

> 같은 이슈의 다른 대상에서 이미 채웠으면 건너뛰고 3-B로 간다.
> 단, 이번 대상의 쿼리가 앞선 대상이 다루지 않은 테이블을 읽으면 그 테이블만 추가로 채운다.

1. 현재 행 수를 확인하도록 제시하고 결과를 받는다.

   ```bash
   $MYSQL_PERF -e "
   SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = 'uss_db' ORDER BY TABLE_ROWS DESC;"
   ```

   **`TABLE_ROWS`를 행 수로 믿지 마라.** InnoDB에서 이 값은 옵티마이저가 샘플링한 **추정치**이고,
   실제와 배 단위로 어긋난다. 목표 규모를 판정할 때는 반드시 실제 카운트로 확인한다.

   ```bash
   $MYSQL_PERF -e "
   SELECT 'courses' AS t, count(*) AS n FROM courses
   UNION ALL SELECT 'course_schedules', count(*) FROM course_schedules
   UNION ALL SELECT 'members', count(*) FROM members
   UNION ALL SELECT 'carts', count(*) FROM carts
   UNION ALL SELECT 'registrations', count(*) FROM registrations;"
   ```

2. 목표 규모를 호출자와 확정한다.
   - Phase 1에서 확정한 쿼리가 읽는 테이블만을 대상으로 삼는다. 그 외 테이블은 다루지 않는다.
   - 정렬, 집계가 걸린 쿼리가 있다면 해당 테이블의 목표 규모를 개별 수치로 확정한다.
   - 목표 규모는 스킬이 임의로 확정하지 않는다. 호출자와의 인터렉션을 통해 확정한다.

3. 목표 카디널리티를 호출자와 확정한다.
   - 대상 쿼리의 `WHERE`, `ORDER BY`, `GROUP BY`에 쓰이는 컬럼마다 서로 다른 값의 개수를 정한다.
   - `course_department`, `course_area`처럼 enum이 들어가는 컬럼은 **분포의 치우침**까지 정한다.
     한 학과에 강의가 몰린 분포와 균등 분포는 인덱스 선택도가 달라진다.
   - Phase 5-B의 인덱스 설계가 이 값을 근거로 쓴다. 확정하지 않은 채 넘어가지 마라.

4. **인증이 필요한 대상이면 로그인용 비밀번호 해시를 먼저 확보한다.**
   시드 회원 전원이 같은 해시를 쓰고, Phase 4가 그 비밀번호로 로그인해 토큰을 받는다.
   해시를 지어내지 마라. 애플리케이션이 실제로 만든 값을 그대로 재사용한다.

   ```bash
   # 1) 인증 완료 상태의 이메일 인증 기록을 직접 넣는다 (메일 발송을 우회한다)
   $MYSQL_PERF -e "
   INSERT INTO email_verification_codes (email, code, verified, failed_count, resend_count, expires_at)
   VALUES ('perfseed@inu.ac.kr', '000000', TRUE, 0, 0, NOW() + INTERVAL 1 DAY)
   ON DUPLICATE KEY UPDATE verified = TRUE, expires_at = NOW() + INTERVAL 1 DAY;"

   # 2) 실제 회원가입 API로 회원을 만든다 (비밀번호가 애플리케이션 인코더로 해시된다)
   curl -s -X POST localhost:8080/api/v1/auth/sign-up \
     -H 'Content-Type: application/json' \
     -d '{"studentId":"999999999","password":"perfPassw0rd","name":"perf",
          "email":"perfseed@inu.ac.kr","memberCollege":"INFORMATION_TECHNOLOGY",
          "memberDepartment":"COMPUTER_ENGINEERING","memberGrade":"SENIOR",
          "academicStatus":"ENROLLED","lastSemesterGPA":4.0}'

   # 3) 저장된 해시를 꺼낸다. 이 값이 seeds.sql의 @pw_hash가 된다
   $MYSQL_PERF -N -e "SELECT password FROM members WHERE email = 'perfseed@inu.ac.kr';"
   ```

   - 학번은 9자리 숫자, 이메일은 `@inu.ac.kr`만 통과한다. 이 제약을 어기면 400이 떨어진다.
   - 받은 해시를 그대로 `seeds.sql`의 `@pw_hash`에 넣는다. 해시 문자열에 `$`가 들어가므로
     셸에서 다시 가공하지 말고 SQL 파일에 작은따옴표로 감싸 붙여넣게 한다.
   - 이 회원은 시드 범위 밖의 학번을 쓰므로 측정 대상에 섞이지 않는다.

5. 현재 행 수가 목표 규모에 미달하는 테이블이 있다면 `template/seeds/README.md`를 Read하고,
   필요한 모듈을 골라 `.claude/resources/perf/{이슈번호}/seeds.sql`을 만든다.
   - **모듈 본문을 복사하지 마라.** 변수 블록을 쓰고 `SOURCE`로 모듈을 불러오는 형태로만 작성한다.
   - 필요한 테이블이 기존 모듈에 없을 때만 그 테이블 블록을 `seeds.sql`에 직접 쓴다.
     같은 이슈에서 두 번 이상 쓸 것 같으면 새 모듈로 만들자고 호출자에게 제안한다.
   - 실행 명령을 호출자에게 제시하고, 모듈 말미의 검증 쿼리 결과를 받는다.

     ```bash
     $MYSQL_PERF < $PERF_DIR/seeds.sql
     ```

   - 검증 결과가 2번, 3번에서 확정한 값과 어긋나면 변수를 고쳐 다시 실행하게 한다.
     어긋난 채로 5번을 통과시키지 마라.

6. 시드 적재 후 옵티마이저 통계를 갱신하게 한다. 이 단계를 빼면 첫 측정의 실행계획이 적재 전 통계로 잡힌다.

   ```bash
   $MYSQL_PERF -e "ANALYZE TABLE members, courses, course_schedules, carts, registrations;"
   ```

7. 확정한 목표 규모, 카디널리티, 실제 검증값을 `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 **측정 환경**에 적는다.

---

### 3-B. 부하 스크립트 (대상별)

1. `template/k6-script-template.js`를 Read하고, 작성 규칙에 따라 스크립트를 작성한다.
   - `.claude/resources/perf/{이슈번호}/{슬러그}/test-script.js`로 파일을 생성한다.
   - `TARGET`에는 Phase 1에서 정한 슬러그를, `ENDPOINT`에는 경로를 그대로 넣는다.
   - `tokens.json`은 이슈 디렉토리에 있다. `open('../tokens.json')` 경로를 바꾸지 마라.
   - 실행은 Phase 4에서 호출자가 직접 한다.

2. 부하 조건(VU, duration)을 호출자와 확정해
   스크립트의 `CONDITION` 블록과 `record.md`의 **측정 환경**에 적는다.
   - 같은 이슈의 다른 대상과 조건을 맞출지 호출자에게 확인한다. 조건이 다르면 대상 간 비교가 불가능해진다.
   - VU 상한은 커넥션 풀 크기와 함께 본다. 풀보다 훨씬 큰 VU는 쿼리가 아니라 커넥션 대기를 재게 된다.
   - 이후 사이클에서 이 값이 바뀌면 변경된 값과 변경 시점을 같은 항목에 덧붙인다. 기존 값을 덮어쓰지 마라.

3. **쓰기 엔드포인트면 되돌리기 절차를 여기서 확정한다.**
   수강신청과 장바구니 담기는 행을 남기고 `courses.current_enrollment`를 바꾼다.
   측정마다 시작 상태가 같아야 하므로, 되돌리는 SQL을 확정해 `record.md`에 적어둔다.
   Phase 4와 8이 같은 자리에서 이 SQL을 실행한다.

   ```sql
   -- 예: 수강신청 측정의 되돌리기
   DELETE FROM registrations WHERE member_id BETWEEN @member_start AND @member_end;
   UPDATE courses SET current_enrollment = 0 WHERE id BETWEEN @course_start AND @course_end;
   ```

   - `UNIQUE KEY uk_member_course` 때문에 되돌리지 않으면 2회차부터 전부 중복 실패가 된다.
     되돌리기를 빼면 측정이 아니라 에러율을 재게 된다.

### 출력
- `.claude/resources/perf/{이슈번호}/seeds.sql` 생성 (시드가 필요한 경우)
- `.claude/resources/perf/{이슈번호}/{슬러그}/test-script.js` 생성
- `record.md`의 측정 환경에 목표 데이터 규모, 카디널리티, 부하 조건, 되돌리기 절차가 기록
- `record.md`의 진행 상태의 Phase 3이 ✅로 기록

### 실패 처리
- 없음

> 다음 Phase 조건: k6 스크립트가 작성되었고 목표 규모에 도달했을 때 → Phase 4

> Skip 조건: 3-A는 **이번 대상의 쿼리가 읽는 모든 테이블**이 목표 규모와 카디널리티를 충족했을 때만 건너뛴다.
> 다른 대상이 시드를 돌렸다는 사실만으로는 건너뛰지 마라. 누락된 테이블이나 목표에 미달하는 테이블이 있으면 그것만 3-A에서 추가로 채운다.
> 3-B는 2회차 이상이고 스크립트가 이미 있으면 건너뛴다. 둘 다 건너뛰면 진행 상태에 ⏭️로 표기한다.

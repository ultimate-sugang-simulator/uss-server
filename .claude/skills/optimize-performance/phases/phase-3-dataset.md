## Phase 3. 측정 조건 구성

### 목적
데이터 규모와 카디널리티를 확정해 시드를 채우고(3-A, 이슈 공용), 부하 조건을 확정해 k6 스크립트를 만든다(3-B, 대상별).

### 선행 조건
- Phase 2 완료

### 참조 파일
- `.claude/skills/optimize-performance/template/seeds/README.md`
- `.claude/skills/optimize-performance/template/k6-script-template.js`

---

### 3-A. 데이터셋 (이슈 공용)

> 같은 이슈의 다른 대상이 채웠으면 3-B로 간다. 단, 이번 대상의 쿼리가 앞선 대상이 다루지 않은 테이블을 읽으면 그 테이블만 추가로 채운다.

1. 현재 행 수를 확인하게 한다. `information_schema.TABLES.TABLE_ROWS`는 샘플링 추정치라 배 단위로 어긋난다. 실제 카운트를 쓴다.

   ```bash
   mysqlp -e "
   SELECT 'courses' AS t, count(*) AS n FROM courses
   UNION ALL SELECT 'course_schedules', count(*) FROM course_schedules
   UNION ALL SELECT 'members', count(*) FROM members
   UNION ALL SELECT 'carts', count(*) FROM carts
   UNION ALL SELECT 'registrations', count(*) FROM registrations;"
   ```

2. 목표 규모를 호출자와 확정한다. Phase 1에서 확정한 쿼리가 읽는 테이블만 다룬다.
   - 앱 시드(`database/seed/`)가 곧 운영 규모의 근사치다. 목표는 **운영 대비 배수**로 정하고,
     그 배수를 고른 이유(무엇을 관측하려는지)를 함께 확정한다. 배수가 클수록 적재, 워밍업, 버퍼 풀 비용이 커지고
     운영에서 볼 일 없는 구간을 재게 된다. 근거 없는 큰 수를 받아들이지 마라.
   - 정렬, 집계가 걸린 쿼리가 있으면 그 테이블의 목표를 개별 수치로 확정한다.
   - 규모는 스킬이 정하지 않는다.

3. 목표 카디널리티를 호출자와 확정한다.
   - 대상 쿼리의 `WHERE`, `ORDER BY`, `GROUP BY` 컬럼마다 서로 다른 값의 개수를 정한다.
   - enum 컬럼(`department`, `area`)은 **분포의 치우침**까지 정한다. 한 값에 몰린 분포와 균등 분포는 인덱스 선택도가 다르다.
   - FULLTEXT 검색이 대상이면 키워드당 매칭 건수를 정한다. `seeds/README.md`의 **검색 제목 설계** 참고.
   - Phase 5-B가 이 값을 근거로 쓴다. 확정하지 않고 넘어가지 마라.

4. 인증이 필요한 대상이면 토큰 방식을 정한다.
   - 기본은 **서명키로 직접 발급**(`mint-tokens.sh`, Phase 4에서 실행)이다. 로그인 API를 태우지 마라.
     로그인 SQL이 측정에 섞이고 수백 계정을 가입시키는 것보다 느리다.
   - **대상 컨트롤러에 `@Auth` 파라미터가 없으면 회원 시드가 필요 없다.** 필터가 DB를 보지 않으므로
     토큰의 `memberId`가 `members`에 없어도 통과한다. 컨트롤러 시그니처를 먼저 확인하라.
   - **로그인 경로 자체를 측정할 때만** 실제 해시가 필요하다. 지어내지 말고 가입 API가 만든 값을 쓴다.

     ```bash
     curl -s -X POST localhost:8080/api/v1/auth/sign-up -H 'Content-Type: application/json' \
       -d '{"email":"perfseed@inu.ac.kr","password":"perfPassw0rd","studentId":"999999999","name":"perf",
            "college":"INFORMATION_TECHNOLOGY","department":"COMPUTER_ENGINEERING","grade":"SENIOR",
            "academicStatus":"ENROLLED","lastSemesterGpa":4.0}'

     mysqlp -N -e "SELECT password FROM members WHERE email = 'perfseed@inu.ac.kr';"
     ```

     로그인 식별자는 이메일이다 (`LoginRequest{email, password}`). 비밀번호는 8~20자, 학번은 영숫자 1~20자.
     받은 해시는 `$`를 포함하므로 셸에서 가공하지 말고 `seeds.sql`의 `@pw_hash`에 작은따옴표로 감싸 넣게 한다.

5. 목표에 미달하는 테이블이 있으면 `seeds/README.md`를 Read하고 `.claude/resources/perf/{이슈번호}/seeds.sql`을 만든다.
   - `seeds.sql`에는 **변수 블록만** 쓴다. 모듈 본문을 복사하지 마라. 실행할 때 `cat`으로 이어 붙인다.
   - 필요한 테이블이 모듈에 없을 때만 그 블록을 `seeds.sql`에 직접 쓴다. 두 번 이상 쓸 것 같으면 새 모듈을 제안한다.
   - 모듈의 규모 상한(README)을 넘는 규모면 README의 **대량 적재** 절차를 함께 제시한다.

     ```bash
     cat $PERF_DIR/seeds.sql $SEEDS/member.sql $SEEDS/course.sql $SEEDS/enrollment.sql | mysqlp
     ```

   - 모듈 말미의 검증 결과가 2, 3에서 확정한 값과 어긋나면 변수를 고쳐 다시 실행하게 한다. 어긋난 채로 넘어가지 마라.

6. 옵티마이저 통계를 갱신하게 한다. 빼면 첫 측정의 실행계획이 적재 전 통계로 잡힌다.

   ```bash
   mysqlp -e "ANALYZE TABLE members, courses, course_schedules, carts, registrations;"
   ```

7. 확정한 규모(운영 대비 배수와 근거 포함), 카디널리티, 검증값을 `record.md`의 **측정 환경**에 적는다.

---

### 3-B. 부하 스크립트 (대상별)

1. `template/k6-script-template.js`를 Read하고 작성 규칙대로 `.claude/resources/perf/{이슈번호}/{슬러그}/test-script.js`를 만든다.
   `TARGET`은 슬러그, `ENDPOINT`는 경로. 실행은 Phase 4에서 호출자가 한다.

2. 부하 조건(VU, duration, USER_COUNT)을 호출자와 확정해 스크립트의 `CONDITION`과 `record.md`의 **측정 환경**에 적는다.
   - 같은 이슈의 다른 대상과 조건을 맞출지 확인한다. 다르면 대상 간 비교가 불가능하다.
   - VU 상한은 커넥션 풀 크기와 함께 본다. 풀보다 훨씬 큰 VU는 쿼리가 아니라 커넥션 대기를 잰다.
   - 이후 값이 바뀌면 변경값과 시점을 덧붙인다. 기존 값을 덮어쓰지 마라.

3. **쓰기 엔드포인트면 되돌리기 SQL을 여기서 확정해 `record.md`에 적는다.** Phase 4와 8이 같은 자리에서 실행한다.
   수강신청과 장바구니는 행을 남기고 `courses.current_enrollment`를 바꾼다. `uk_member_course` 때문에 되돌리지 않으면 2회차부터 전부 중복 실패다.

   ```sql
   DELETE FROM registrations WHERE member_id BETWEEN {회원 id 시작} AND {회원 id 끝};
   UPDATE courses SET current_enrollment = 0 WHERE id BETWEEN {강의 id 시작} AND {강의 id 끝};
   ```

### 출력
- `seeds.sql` (필요한 경우), `test-script.js`
- `record.md`의 **측정 환경**에 규모와 근거, 카디널리티, 부하 조건, 되돌리기 절차, 진행 상태 Phase 3 ✅

> 다음 Phase 조건: k6 스크립트가 있고 목표 규모에 도달했을 때 → Phase 4
>
> Skip 조건: 3-A는 이번 대상의 쿼리가 읽는 모든 테이블이 목표를 충족할 때만. 3-B는 2회차 이상이고 스크립트가 있을 때.
> 둘 다 건너뛰면 ⏭️로 표기한다.

# [PLAN-101] 강의 검색 결과의 학년 정렬 누락

> 이슈: #101
> 브랜치: fix/101-search-sort

## 목표
강의 검색 결과도 다른 조회 5종과 같은 학년 정렬(전학년 → 1 → 2 → 3 → 4)을 따르게 하고,
방향이 빠져 관련도가 **낮은 순**으로 나가던 정렬을 높은 순으로 정정한다. 검색 경로에 정렬 회귀를 잡는 테스트를 둔다.

## 사전 확인 (2026-08-28 조사)
- `CourseRepository.findByKeyword`는 MySQL FULLTEXT 네이티브 쿼리다. `ORDER BY MATCH ... AGAINST` 뒤에 방향이 없어 ASC, 즉 관련도 낮은 순이다.
  나머지 조회 5종(`findByDepartment`, `findByDepartmentIn`, `findByArea`, `findHussCourses`)은 `ORDER BY c.gradeCode, c.classificationCode, c.haksuCode`다.
- `grade_code`는 `0`이 전학년이라(`CourseGrade.ALL`) `ORDER BY grade_code` 오름차순만으로 전학년 → 1 → 2 → 3 → 4가 된다. 별도 CASE 식이 필요 없다.
- **검색 경로에는 테스트가 없다.** `CourseServiceTest:946-1173`의 `키워드_검색_테스트`는 #40(FULLTEXT 도입) 때부터 통째로 주석이다.
  테스트 DB인 H2(`MODE=MySQL`)가 `MATCH ... AGAINST`를 실행하지 못하기 때문이다. 이 버그가 그대로 나간 원인이기도 하다.
- 이슈 항목 3(정렬 검증 통합 테스트)은 H2 `@IntegrationTest`로는 만들 수 없다. 실제 MySQL이 필요하며 Testcontainers로 띄운다. 여건:
  - 로컬: Docker 28.4.0 기동 중, `mysql:8.0` 이미지 보유
  - CI: `ubuntu-latest`(Docker 내장). `ci.yml`에 MySQL 서비스는 없지만 Testcontainers가 직접 띄우므로 워크플로 수정은 불필요
  - Boot 4.0.1 BOM이 Testcontainers **2.0.3**을 관리한다. 2.x는 1.x와 좌표와 패키지가 다르다 (Maven Central에서 확인):
    `org.testcontainers:testcontainers-mysql`, `org.testcontainers:testcontainers-junit-jupiter`, 클래스 `org.testcontainers.mysql.MySQLContainer`(제네릭 아님).
    `org.testcontainers.containers.MySQLContainer`(구 패키지)와 `org.testcontainers:mysql`(구 좌표)을 쓰지 않는다
  - `org.springframework.boot:spring-boot-testcontainers`의 `@ServiceConnection`(`org.springframework.boot.testcontainers.service.connection`)이 datasource url, 계정을 주입한다
- **InnoDB FULLTEXT 인덱스는 커밋된 행만 검색한다** (인덱스 갱신이 커밋 시점에 일어난다). `@Transactional` 롤백 격리 안에서 `saveAll`한 행은
  같은 트랜잭션의 `MATCH`에 잡히지 않는다. 그래서 MySQL 테스트는 트랜잭션 없이 돌리고 정리는 테스트가 직접 한다. 주석 처리된 테스트를 그대로 옮기면 안 되는 이유다.
- Flyway: 테스트 yml에 flyway 설정이 없고 기본 위치(`db/migration`)에 파일이 없어 H2 컨텍스트에서는 사실상 아무것도 안 한다.
  MySQL 컨텍스트는 `spring.flyway.locations=classpath:database/migration`으로 V1_0~V1_9를 적용해 FULLTEXT ngram 인덱스까지 만든다. `database/seed`는 제외한다.
  `V1_0`의 `courses`는 문자셋을 지정하지 않아 서버 기본을 따르며, MySQL 8.0 기본값(utf8mb4, `ngram_token_size=2`)으로 충분하다
- 관련도 산식: InnoDB는 `tf × idf²`, `idf = log10(전체 행 수 / 검색어 포함 행 수)`다. **모든 행이 검색어를 포함하면 idf = 0이라 관련도가 전부 0으로 같아진다.**
  테스트 데이터에 검색어와 무관한 행을 반드시 섞어야 관련도 차이가 생긴다
- 정책 `service-policy/course.md` "강의 검색" 절은 "관련도가 높은 순으로 정렬한다"뿐이라 학년 우선으로 갱신해야 한다

## 영향 범위
### 신규 파일
- `src/test/java/uss/code/global/infra/MySqlContainerConfig.java` — `MySQLContainer` 빈 + `@ServiceConnection`
- `src/test/java/uss/code/global/infra/MySqlIntegrationTest.java` — MySQL 컨테이너 기반 통합 테스트 메타 어노테이션 (트랜잭션 없음)
- `src/test/java/uss/code/course/service/CourseServiceSearchTest.java` — 검색 정렬 검증

### 수정 파일
- `src/main/java/uss/code/course/repository/CourseRepository.java` — `findByKeyword`의 ORDER BY를 학년 → 관련도 DESC → 학수번호로 교체
- `build.gradle` — Testcontainers 테스트 의존성 3개 추가
- `.claude/spec/service-policy/course.md` — "강의 검색" 절의 정렬 규칙을 학년 우선으로 갱신
- `.claude/spec/test-convention.md` — MySQL 전용 쿼리 테스트 규칙 추가

> `CourseService`, DTO, Controller, API 문서는 손대지 않는다(응답 형식 불변). `CourseServiceTest`의 주석 블록도 이번에 건드리지 않는다.

## 구현 계획

### 1. Repository: `CourseRepository.findByKeyword(final String keyword)`
ORDER BY만 교체한다. SELECT, WHERE, `MATCH` 컬럼 목록은 그대로 둔다 (목록이 인덱스 `ft_idx_course_search`와 어긋나면 인덱스를 못 탄다 — PLAN-62).
```sql
SELECT DISTINCT c.*
FROM courses c
WHERE MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
  AND c.status = 'ACTIVE'
ORDER BY c.grade_code,
         MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE) DESC,
         c.haksu_code
```
- `DISTINCT`와 ORDER BY 조합: ORDER BY 식이 참조하는 컬럼이 전부 `c.*`에 들어 있어 MySQL 8의 3065(`ORDER BY clause is not in SELECT list ... incompatible with DISTINCT`) 대상이 아니다. 현행 쿼리와 같은 조건이다.
- 옵티마이저는 WHERE와 ORDER BY의 동일한 `MATCH` 식을 한 번만 계산한다.

### 2. `build.gradle` — Test 블록에 추가
```groovy
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
testImplementation 'org.testcontainers:testcontainers-mysql'
```
버전은 Boot BOM(testcontainers-bom 2.0.3)이 관리한다. 명시하지 않는다.

### 3. `MySqlContainerConfig` (신규, `uss.code.global.infra`)
```java
@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfig {

    private static final String MYSQL_IMAGE = "mysql:8.0";

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(MYSQL_IMAGE);
    }
}
```
- import는 `org.testcontainers.mysql.MySQLContainer`.
- 컨테이너 수명은 스프링 컨텍스트가 관리한다. 컨텍스트 캐시로 JVM당 한 번 뜨고 종료 시 내려간다.
- `@ServiceConnection`이 `JdbcConnectionDetails`를 등록해 테스트 yml의 H2 `url`, `driver-class-name`, 계정을 덮어쓴다. 드라이버는 url에서 유도된다.
- `docker-compose-local.yml`의 `--character-set-server`, `--collation-server`, `--ngram_token_size=2`는 8.0 기본값과 검색 결과가 같으므로 넣지 않는다.
  기동이나 접속이 실패하면(인증 플러그인, SSL) `withUrlParam("allowPublicKeyRetrieval", "true")`, `withUrlParam("useSSL", "false")`를 붙인다.

### 4. `MySqlIntegrationTest` (신규, `uss.code.global.infra`)
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(MySqlContainerConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:database/migration"
})
public @interface MySqlIntegrationTest {
}
```
- **`@Transactional`을 붙이지 않는다.** InnoDB FULLTEXT 커밋 가시성 때문이다. 데이터 정리는 테스트 클래스 책임이다.
- `disabledWithoutDocker = true`: Docker가 없는 로컬에서는 이 어노테이션이 붙은 테스트만 skip된다. CI는 Docker가 있어 항상 실행된다.
- `ddl-auto=none` + Flyway 마이그레이션으로 운영과 같은 스키마를 만든다. `validate`는 쓰지 않는다 (타입 매핑 차이로 이번 범위 밖의 실패가 날 수 있다).
- dialect를 명시하는 이유: 테스트 yml이 `H2Dialect`를 고정하고 있어 빈 값으로는 못 지운다.

### 5. `CourseServiceSearchTest` (신규, `uss.code.course.service`)
```java
@MySqlIntegrationTest
class CourseServiceSearchTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @AfterEach
    void tearDown() {
        courseRepository.deleteAllInBatch();
    }

    @Nested
    class 키워드_검색_정렬_테스트 {
        private static final String KEYWORD = "정렬";

        @BeforeEach
        void setUp() { ... }

        @Test void 학년이_관련도보다_먼저_정렬된다()
        @Test void 같은_학년_안에서는_관련도가_높은_강의가_먼저_온다()
        @Test void 관련도가_같으면_학수번호_순으로_정렬된다()
        @Test void 검색어와_무관한_강의는_결과에_포함되지_않는다()
    }
}
```
- 트랜잭션이 없으므로 `courseRepository.saveAll`(자체 트랜잭션)이 바로 커밋되고 FULLTEXT 인덱스에 반영된다. `courseService.searchCourses`는 별도 읽기 트랜잭션에서 커밋된 행을 본다.
- 바깥 클래스의 `@AfterEach`는 `@Nested` 테스트에도 적용된다. 시간표를 만들지 않으므로 `deleteAllInBatch()`가 FK에 걸리지 않는다.
- 검색어 `정렬`은 2글자라 ngram 토큰 하나다. 3글자 이상이면 BOOLEAN MODE에서 구문 검색으로 바뀌어 관련도 예측이 어려워진다.
- `setUp` 데이터 — 전부 `CourseFixture.createCourseWithDetails(titleKr, titleEn, courseCode, haksuCode, grade)`. **저장 순서는 기대 순서와 다르게 뒤섞는다.**

| haksuCode | titleKr | grade | 검색어 출현 | 의도 |
|---|---|---|---|---|
| `SRCH005001` | `정렬과 정렬 응용과 정렬` | SENIOR | 3회 (최고 관련도) | 관련도가 가장 높아도 학년이 뒤면 마지막 |
| `SRCH001001` | `정렬 기초` | ALL | 1회 | 전학년 안에서 관련도 2위 |
| `SRCH002001` | `정렬과 정렬 응용` | ALL | 2회 | 전학년 안에서 관련도 1위 → `SRCH001001`보다 앞 |
| `SRCH004001` | `정렬 입문` | FRESHMAN | 1회 | 1학년, `SRCH003001`과 관련도 동률 |
| `SRCH003001` | `정렬 입문` | FRESHMAN | 1회 | 동률이면 학수번호 순 → `SRCH004001`보다 앞 |
| `NONE001001` | `자료구조` | ALL | 0회 | idf > 0을 만들기 위한 비매칭 행 |
| `NONE002001` | `운영체제` | FRESHMAN | 0회 | 위와 같음 |

- 기대 전체 순서: `SRCH002001, SRCH001001, SRCH003001, SRCH004001, SRCH005001`
  - `학년이_관련도보다_먼저_정렬된다`: `SearchedCourseResponse::grade` → `containsExactly("전학년", "전학년", "1학년", "1학년", "4학년")`
  - `같은_학년_안에서는_관련도가_높은_강의가_먼저_온다`: 앞 두 건의 haksuCode → `containsExactly("SRCH002001", "SRCH001001")`
  - `관련도가_같으면_학수번호_순으로_정렬된다`: 3, 4번째 haksuCode → `containsExactly("SRCH003001", "SRCH004001")`
  - `검색어와_무관한_강의는_결과에_포함되지_않는다`: haksuCode에 `NONE001001`, `NONE002001` 없음, 총 5건
- courseCode는 haksuCode 앞 7자리(`SRCH005`), titleEn은 `Sorting` 계열 영문. 영문 제목에 검색어 토큰이 들어가지 않게 한다.

### 6. `service-policy/course.md` — "강의 검색" 절 교체
```
## 강의 검색

- 학수번호, 과목코드, 국문 강의명, 영문 강의명을 대상으로 검색한다
- 검색 결과는 **학년, 관련도, 학수번호 순**으로 정렬한다. 학년은 다른 조회와 같은 기준(코드 오름차순, 전학년이 앞)이고,
  같은 학년 안에서는 검색어와의 관련도가 높은 순, 관련도가 같으면 학수번호 오름차순이다
```

### 7. `test-convention.md` — 절 추가
- 상단 문장을 "모든 서비스 테스트는 통합 테스트로 작성한다. 기본은 `@IntegrationTest`(H2)다"로 손질하고, 아래 절을 Fixture 앞에 넣는다.
```
## MySQL 전용 쿼리 테스트

FULLTEXT 등 H2가 실행하지 못하는 네이티브 쿼리는 `@MySqlIntegrationTest`(Testcontainers MySQL)로 검증한다.

- 별도 클래스로 분리하고 이름은 `{Class}{기능}Test`로 짓는다 (예: `CourseServiceSearchTest`)
- 트랜잭션 롤백 격리가 없다. InnoDB FULLTEXT는 커밋된 행만 검색하므로 저장이 그대로 커밋된다.
  `@AfterEach`에서 `deleteAllInBatch()`로 직접 지워라
- Docker가 없는 환경에서는 skip된다. CI에서는 항상 실행된다
- 관련도를 검증할 때는 검색어와 무관한 행을 함께 넣어라. 모든 행이 검색어를 포함하면 idf가 0이라 관련도가 전부 같아진다
```

## 결정 필요 (Decisions needed)
- [x] **1. 검색 정렬 기준** — A) 학년 → 관련도 높은 순 → 학수번호 (이슈 제안) / B) 학년 → 이수구분 → 관련도 높은 순 → 학수번호 (목록 조회와 앞 두 키까지 동일)
  → **A 확정 (사용자 선택, 2026-08-28).** 검색 결과는 학과와 이수구분이 섞여 있어 이수구분 묶음이 주는 의미가 약하고, 사용자가 입력한 검색어와의 관련도가 두 번째 키로 더 쓸모 있다. 계획 1번과 6번은 A 기준으로 확정.
- [x] **2. 정렬 검증 방식** — A) Testcontainers MySQL 통합 테스트 도입 (이슈 항목 3 그대로. 신규 3파일 + `build.gradle` + `test-convention.md`) / B) 로컬 MySQL(`docker-compose-local` + `conc` 프로파일)에서 수동 확인하고 결과를 PR에 기록. 테스트 인프라는 별도 이슈로 분리
  → **A 확정 (사용자 선택, 2026-08-28).** 검색 경로는 #40 이후 테스트가 0건이라 이 버그가 그대로 나갔고, Docker와 CI 여건이 이미 갖춰져 있다.
  비용은 테스트 시간 +10~20초(컨테이너 1회 기동)와 로컬 Docker 필요(없으면 해당 테스트만 skip). 계획 2~5, 7번과 신규 파일 3개를 전부 구현한다.

## 검증
- `./gradlew test` (백그라운드, 로그 파일로 리다이렉트)
  - 기존 `CourseServiceTest` 전부 통과 — 추가 의존성이 H2 컨텍스트를 바꾸지 않는지. `@ServiceConnection` 빈은 `@Import`한 MySQL 컨텍스트에만 있다
  - `CourseServiceSearchTest` 4건 통과. 이 테스트가 `MATCH`를 실제로 실행하므로 Flyway 적용과 `ft_idx_course_search` 생성 실패는 여기서 바로 드러난다
  - 테스트 로그에서 MySQL 컨테이너가 한 번만 뜨는지 (`Container mysql:8.0 started`가 1회)
- 정렬 방향 회귀 확인: 1번 수정을 잠시 되돌리고(`DESC` 제거) `같은_학년_안에서는_관련도가_높은_강의가_먼저_온다`가 실패하는지 본 뒤 복원한다
- 로컬 확인 (선택): `docker-compose -f docker/docker-compose-local.yml up -d mysql` → `./gradlew bootRun --args='--spring.profiles.active=conc'` →
  `GET /api/v1/courses/search?keyword=데이터`(`access-token` 헤더, 토큰은 `mint-tokens.sh`) 응답이 전학년 → 1 → 2 → 3 → 4 순이고 같은 학년 안에서 제목 일치도가 높은 강의가 앞인지
- API 문서, 응답 스키마 변경 없음

## Deviation Log
> implement 스킬이 구현 중 계획을 벗어난 지점을 여기에 기록한다. (작성 시점엔 비워둔다)

- `MySqlContainerConfig`: `@ServiceConnection`을 걷어내고 `DynamicPropertyRegistrar`로 `spring.datasource.url/username/password/driver-class-name`을 컨테이너 값으로 주입 — 이유: 계획 3번은 `@ServiceConnection`이 테스트 yml의 H2 접속 정보를 덮어쓴다고 봤지만 실제로는 덮이지 않는다. 이 프로젝트는 `global/config/DataSourceConfig`에서 `@Primary` DataSource를 `DataSourceProperties`(`spring.datasource.*`)로 직접 만들기 때문에, `JdbcConnectionDetails`를 참조하는 부트 자동설정 경로를 타지 않는다. 진단으로 확인한 상태는 "컨테이너 기동됨(`isRunning=true`) + `jdbcContainerConnectionDetailsForMysqlContainer` 빈 등록됨 + 그런데 DataSource URL은 `jdbc:h2:mem:testdb`"였다. `DataSourceConfig`가 읽는 프로퍼티에 직접 주입하는 방식으로 바꿔 해결했다 (사용자 선택, 2026-08-28). 운영 코드는 손대지 않았다.
- `CourseServiceSearchTest.검색어와_무관한_강의는_결과에_포함되지_않는다`: 계획의 "haksuCode에 `NONE001001`, `NONE002001` 없음, 총 5건" 그대로 `hasSize(5)` + `doesNotContain(...)`으로 작성 — 이유: 전체 순서 검증은 `학년이_관련도보다_먼저_정렬된다`가 이미 맡고 있어 중복을 피했다.

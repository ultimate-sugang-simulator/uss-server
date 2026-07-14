# [PLAN-54] 모니터링 스택 구축(메트릭·로그 수집 파이프라인)

> 이슈: #54
> 브랜치: feat/54-monitoring-stack

## 목표

uss-server가 자기 상태를 밖으로 내보내게 만든다. 애플리케이션은 `/actuator/prometheus`로 메트릭을 노출하고, 로그는 파일로 떨어져 Alloy가 Loki로 밀어넣는다. 앱센터 중앙 Prometheus/Grafana가 이 둘을 수집·시각화하면, 이후 동시성·쿼리 개선의 before/after를 숫자로 비교할 수 있다.

## 전제 (중앙 스택 현황)

- 중앙 스택(`inu-appcenter/appcenter-server-metric-monitoring`)에는 **Prometheus + Grafana만** 있다. Loki는 없다.
- 중앙 Prometheus는 각 서비스의 도커 네트워크에 `external`로 조인해 **컨테이너 이름:포트**로 스크랩한다 (예: `gravit-server-prod:8080`).
- 따라서 uss는 ① 네트워크 이름을 고정하고 ② Loki를 자기 compose에서 직접 운영하며, 중앙 레포에는 scrape target + Loki datasource 등록 PR을 따로 올린다.

## 영향 범위

### 신규 파일

**애플리케이션**
- `src/main/resources/logback-spring.xml` — 레벨별(info/warn/error) 롤링 파일 appender + `API_PERF` 전용 appender. prod/release 프로파일에서만 파일 기록, 그 외엔 콘솔만.
- `src/main/java/uss/code/global/filter/HttpLoggingFilter.java` — traceId(MDC) 부여 + 요청/응답 로깅 + 민감 쿼리 파라미터 마스킹.
- `src/main/java/uss/code/global/interceptor/ApiPerformanceInterceptor.java` — 핸들러 처리 시간 측정, logfmt 형식으로 `API_PERF` 로거에 기록.
- `src/main/java/uss/code/global/config/InterceptorConfig.java` — `WebMvcConfigurer.addInterceptors`로 위 인터셉터 등록.

**인프라**
- `docker/infra/loki-config.yml` — Loki 단일 노드(filesystem, tsdb v13) 설정. **retention 7일**(`limits_config.retention_period: 168h` + `compactor.retention_enabled: true`) — 공용 서버라 로그가 무한히 쌓이면 디스크를 잠식한다. 로그 파일 쪽 `maxHistory 7`과 보존 기간을 맞춘다.
- `docker/infra/config.alloy` — `/var/log/spring/{info,warn,error,api-perf}/*.log` tail → Loki push. 라벨: `service=uss-server`, `env`, `level`, `log_type`.
> 로컬 모니터링 스택(Prometheus/Grafana 로컬 compose)은 만들지 않는다. 배포 환경 + 중앙 스택만 대상으로 한다.
> 대시보드 JSON도 이 레포에 두지 않는다. 중앙 Grafana가 단일 소유자이며, 확정 후 중앙 레포의 provisioning으로 관리한다(원본이 두 곳에 갈라지는 것을 막는다).

### 수정 파일

- `build.gradle` — `spring-boot-starter-actuator`, `io.micrometer:micrometer-registry-prometheus` 추가 (Monitoring 주석 그룹 신설).
- `src/main/resources/application-prod.yml` / `application-release.yml` — `management` 섹션 추가(관리 포트 분리, prometheus 노출, 히스토그램, 공통 태그).
- `src/main/java/uss/code/global/http/WhitelistEndpoint.java` — `/actuator/**` 항목 추가(JWT 필터 우회).
- `src/main/java/uss/code/global/config/FilterChainConfig.java` — `HttpLoggingFilter`를 order 0으로 등록(기존 JwtException=1, JwtAuthentication=2보다 앞).
- `docker/docker-compose-prod.yml` — Loki·Alloy 서비스 추가, `uss-prod` 네트워크를 **external**로 선언, 앱에 `ALLOY_ENV` 전달.
- `.github/workflows/cd-prod.yml` — scp 전송 대상에 `docker/infra/*` 포함, `.env`에 `COMPOSE_PROJECT_NAME=uss-prod` 기록, 네트워크 사전 생성, `down` 제거.

## 구현 계획

> 레이어가 아니라 파이프라인 단계 순서로 간다: 앱 계측 → 로그 → 컨테이너 → 대시보드.

### 1. 메트릭 노출 (build.gradle + application-*.yml)

`build.gradle` dependencies에 추가:
```gradle
// Monitoring
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

`application-prod.yml`(release도 동일, `application` 태그 값만 `uss-server-release`)에 추가:
```yaml
management:
  server:
    port: 8081                      # 호스트에 미공개 → 외부에서 액추에이터 접근 불가
  endpoints:
    web:
      exposure:
        include: health,prometheus
  metrics:
    tags:
      application: uss-server-prod  # 서비스 구분 라벨 (모든 메트릭에 공통 부착)
    distribution:
      percentiles-histogram:
        http.server.requests: true  # p95/p99를 Grafana에서 계산하려면 버킷 필요
      slo:
        http.server.requests: 100ms,200ms,500ms,1s,2s,5s
```

- **관리 포트를 8081로 분리**하는 이유: prod compose는 `8181:8080`만 공개하므로 8081은 컨테이너 밖(공인망)에서 닿지 않는다. 같은 도커 네트워크에 조인한 중앙 Prometheus만 `uss-server-prod:8081`로 스크랩한다.
- 관리 포트를 분리하면 액추에이터가 별도 서블릿 컨텍스트에서 뜨므로 메인 컨텍스트의 `FilterRegistrationBean`(JWT 필터)이 적용되지 않는 것이 정상이지만, 안전망으로 화이트리스트에도 넣는다(아래 2).
- `slo` 버킷 경계는 앱센터 기존 서비스가 쓰는 `100ms,200ms,500ms,1s`를 따르되, **`2s,5s`를 덧붙인다.** uss의 대상 API는 대용량 강의 조회라 개선 전 baseline이 1s 위에 몰릴 가능성이 크고, 그러면 상단 버킷이 없어 p99가 `+Inf` 구간에 갇혀 "1초보다 느리다"는 것 외엔 아무것도 못 읽는다. 개선 후 값이 아래로 내려오는 걸 보려면 위쪽 해상도가 필요하다.
- `application` 태그는 gravit과 동일한 방식으로 서비스를 구분한다. 다만 대시보드 쿼리는 중앙 스크랩 설정의 `job` 라벨(`job="uss-prod"`)을 쓴다 — 중앙 Prometheus가 job으로 서비스를 나누고 있어 그쪽이 단일 기준이다.

**같은 파일의 `logging.level`에 p6spy 가드를 추가한다:**
```yaml
logging:
  level:
    p6spy: WARN   # 기존 root/hibernate 설정 아래에 추가
```
`spy.properties`가 `executionThreshold=0` + `Slf4JLogger`라 **모든 SQL을 INFO로** 찍는다. 지금은 콘솔로만 나가 티가 안 나지만, 파일 로깅이 켜지는 순간 `info.log`를 거쳐 그대로 Loki로 흘러든다(부하 구간에선 폭증). prod 데이터소스 URL이 `jdbc:p6spy:` 프리픽스가 아니면 애초에 p6spy가 붙지 않지만, 시크릿이라 확인이 불가하므로 로거 레벨로 막아둔다.

### 2. 인증 우회 (WhitelistEndpoint)

`WHITELIST` 리스트에 추가 — 기존 `EndPoint` 레코드는 `/**` 접미사 패턴을 이미 지원한다.
```java
new EndPoint("/actuator/**", null)
```

### 3. 요청 추적 로그 (HttpLoggingFilter + FilterChainConfig)

`uss.code.global.filter.HttpLoggingFilter extends OncePerRequestFilter` — `@Component`를 붙이지 않고 `FilterChainConfig`에서 `FilterRegistrationBean`으로 등록한다(기존 JWT 필터와 동일한 방식).

- `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)`
  - `MDC.put(TRACE_ID_KEY, generateTraceId())` — UUID에서 하이픈 제거 후 앞 16자.
  - 제외 경로(`/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`)면 로깅 없이 통과시키되 traceId는 유지, `finally`에서 `MDC.clear()`.
  - 그 외에는 `[REQUEST] {method} {uri}` → 체인 실행 → `[RESPONSE] {uri} memberId={} ({status})`.
  - memberId는 `request.getAttribute("member-id")` — `JwtAuthenticationFilter`가 넣는 키와 동일하게 맞춘다.
- `maskSensitiveParams(String)` — 쿼리스트링에서 `access-token`, `refresh-token`, `password` 키의 값을 `****`로 치환.
- 상수: `TRACE_ID_KEY`, `MEMBER_ID_ATTRIBUTE`, `MASK_VALUE`, `EXCLUDE_PATTERNS`, `SENSITIVE_KEYS`를 클래스 상단에 `private static final`로 선언(common.md 상수 규칙).

`FilterChainConfig` 수정:
```java
private static final int HTTP_LOGGING_FILTER_ORDER = 0;   // 신규
private static final int JWT_EXCEPTION_FILTER_ORDER = 1;
private static final int JWT_AUTHENTICATION_FILTER_ORDER = 2;

@Bean
public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilter() { ... }
```
→ 인증 실패(401) 응답도 traceId를 달고 로그에 남는다.

### 4. API 성능 로그 (ApiPerformanceInterceptor + InterceptorConfig)

`uss.code.global.interceptor.ApiPerformanceInterceptor implements HandlerInterceptor`, `@Component`.

- `preHandle` — `request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis())` 후 `true` 반환.
- `afterCompletion` — 경과 시간 계산 후 전용 로거로 기록:
  ```java
  private static final Logger API_PERF = LoggerFactory.getLogger("API_PERF");
  private static final long RESPONSE_TIME_THRESHOLD_MS = 3_000L;

  API_PERF.info("type=API_PERFORMANCE method={} uri={} response_time={} status={}", ...);
  // 임계치 초과 시 warn 레벨로
  ```
- logfmt(`key=value`) 형식을 지키는 이유: Loki에서 `| logfmt | response_time > 1000` 필터가 그대로 먹는다.

`InterceptorConfig implements WebMvcConfigurer` — `addInterceptors(registry)`에서 `registry.addInterceptor(apiPerformanceInterceptor).excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")`. (`ArgumentResolverConfig`는 리졸버 전용이므로 건드리지 않고 설정 클래스를 분리한다.)

### 5. 로그 파일 출력 (logback-spring.xml)

- `LOG_PATTERN`: `timestamp=%d{yyyy-MM-dd'T'HH:mm:ss,Asia/Seoul} level=%-5level traceId=%X{traceId:-null} %msg%n` — traceId를 모든 라인에 박아 로그를 요청 단위로 묶는다.
- `<springProfile name="prod | release">` 안에서 `LOG_PATH=/var/log/spring`, 아래 4개 `RollingFileAppender`(TimeBased, `maxHistory` 7):
  - `INFO_FILE` → `info/info.log` (`LevelFilter` INFO만 ACCEPT)
  - `WARN_FILE` → `warn/warn.log` (WARN만)
  - `ERROR_FILE` → `error/error.log` (ERROR만)
  - `API_PERF` → `api-perf/api-perf.log`, `additivity=false`인 `API_PERF` 로거 전용
- `<springProfile name="!prod &amp; !release">`는 콘솔만 → 로컬·테스트에서 파일이 생기지 않는다.

### 6. 컨테이너 구성 (docker-compose-prod.yml + infra/)

`docker/docker-compose-prod.yml`:
```yaml
services:
  uss-server-prod:
    # (기존) ports 8181:8080 — 8081(관리 포트)은 공개하지 않는다
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    volumes:
      - ./logs:/var/log/spring          # (기존) 이제 실제로 파일이 쌓인다

  uss-loki:
    image: grafana/loki:2.9.8
    container_name: uss-loki
    command: -config.file=/etc/loki/local-config.yml
    volumes:
      - ./infra/loki-config.yml:/etc/loki/local-config.yml:ro
      - loki-data:/loki
    networks: [uss-prod]               # 호스트 포트 미공개(중앙 Grafana가 네트워크로 접근)

  uss-alloy:
    image: grafana/alloy:latest
    container_name: uss-alloy
    command: ["run", "/etc/alloy/config.alloy"]
    environment:
      - ALLOY_ENV=prod
    volumes:
      - ./logs:/var/log/spring:ro
      - ./infra/config.alloy:/etc/alloy/config.alloy:ro
    networks: [uss-prod]
    depends_on: [uss-loki]

networks:
  uss-prod:
    external: true                     # ★ CD가 미리 만든 네트워크를 참조 (아래 6-1)
    name: uss-prod

volumes:
  loki-data:
```
- Loki 포트를 호스트에 공개하지 않는다 — 인증이 없어 공개 시 로그가 그대로 열린다.

`docker/infra/config.alloy`: `local.file_match` 4개(info/warn/error/api-perf) → `loki.source.file` → `loki.process`(static_labels) → `loki.write`.
- 라벨: `service="uss-server"`, `env=sys.env("ALLOY_ENV")`, `level`(info/warn/error), `log_type`(application/performance).
- `loki.write` endpoint: `http://uss-loki:3100/loki/api/v1/push` — **같은 compose 안의 Loki 컨테이너 이름을 쓴다.** 앱센터의 다른 서비스는 Alloy가 존재하지 않는 호스트명으로 push하고 있어 배포 환경에서 로그가 Loki에 적재되지 않는다. 같은 함정을 피하려면 Alloy와 Loki가 반드시 같은 네트워크에 있어야 한다.
- `livedebugging`은 끈다(운영 환경에서 불필요한 오버헤드).
- Alloy UI 포트(12345)는 호스트에 노출하지 않는다.

### 6-1. 배포 스크립트 정리 (cd-prod.yml)

**문제: 현재 CD는 `docker-compose down`을 친다.**
`down`은 compose가 만든 네트워크를 지우려 한다. 중앙 Prometheus가 붙어 있으면 "active endpoints"로 삭제가 실패해 살아남지만, 삭제에 성공하는 순간 새 네트워크에 Prometheus는 붙어 있지 않아 스크랩이 조용히 죽는다(중앙 스택 재기동 전까지 복구 안 됨). Loki 데이터도 배포마다 날아간다.

**해결** — deploy 스텝의 script를 아래로 교체:
```bash
cd /home/serverking/uss/prod

echo "DOCKER_HUB_USERNAME=${{ secrets.DOCKER_HUB_USERNAME }}" > .env

docker network create uss-prod 2>/dev/null || true  # external 네트워크 사전 생성 (멱등)

docker-compose -f docker-compose-prod.yml up -d --no-deps uss-loki uss-alloy   # 인프라: 변경 시에만 재생성
docker-compose -f docker-compose-prod.yml pull uss-server-prod
docker-compose -f docker-compose-prod.yml up -d --no-deps --force-recreate uss-server-prod
docker image prune -f
```
- `down`을 없애 네트워크·Loki 데이터가 배포마다 날아가지 않게 한다.
- 네트워크를 CD가 만들고 compose는 `external`로 참조하므로, 어떤 compose 명령도 이 네트워크를 지우지 못한다 → 중앙 Prometheus의 endpoint가 안정적으로 유지된다.
- 첫 배포 때 앱 컨테이너가 기존 네트워크(`prod_uss-prod`)에서 새 네트워크(`uss-prod`)로 옮겨 붙는다. `--force-recreate`가 처리하므로 수동 작업은 없다. 빈 껍데기로 남는 옛 네트워크는 무해하다.

**compose 프로젝트명은 건드리지 않는다 (의도적 결정).**
배포 디렉토리명이 프로젝트명이 되므로 gravit(`/home/serverking/gravit/prod`)과 uss(`/home/serverking/uss/prod`)는 둘 다 프로젝트 `prod`다. 배포 시 서로를 orphan으로 인식해 경고를 찍지만(지금도 그렇다) 컨테이너를 건드리진 않는다. `COMPOSE_PROJECT_NAME`으로 분리하면 기존 컨테이너가 옛 프로젝트 소속이라 **이름 충돌로 첫 배포가 실패**하고, 서버에서 수동 정리가 필요하다. 얻는 것(`--remove-orphans`를 누가 붙였을 때의 사고 방지)보다 지금 치를 비용이 크다.
> ⚠️ **금지**: uss·gravit 배포 스크립트에 `--remove-orphans`를 붙이지 마라. 프로젝트명이 겹쳐 있어 상대 서비스의 컨테이너를 지운다.

scp 스텝:
```yaml
source: "docker/docker-compose-prod.yml,docker/infra/*"
target: "/home/serverking/uss/prod"
strip_components: 1
```
→ 서버에 `docker-compose-prod.yml`과 `infra/`가 나란히 놓여 compose의 `./infra/...` 마운트 경로와 맞는다.

### 7. 중앙 스택 등록 (별도 PR — 이 레포 밖)

`inu-appcenter/appcenter-server-metric-monitoring`에 올릴 변경 3가지.

**(1) `prometheus/prometheus.yml`** — job 추가 + `global` 신설
현재 파일에 `global`이 없어 scrape_interval이 기본값 **1분**이다. 평시엔 충분하지만 짧은 부하 구간의 p99를 보려면 데이터 포인트가 턱없이 모자라므로 15s로 낮춘다.
- `global.scrape_interval: 15s`
- `job_name: 'uss-prod'` / `targets: ['uss-server-prod:8081']` / `metrics_path: '/actuator/prometheus'`
  (8081은 관리 포트. 컨테이너 내부 포트라 호스트 publish 없이도 같은 네트워크에서 닿는다 — gravit이 8080으로 스크랩되는 것과 동일한 원리다.)

**(2) `grafana/provisioning/datasources/datasources.yml`** — Loki datasource 추가
`name: Loki (uss)`, `type: loki`, `access: proxy`, `url: http://uss-loki:3100`

**(3) `docker-compose.yml`** — `uss-prod` 네트워크를 external로 선언하고 **prometheus·grafana 양쪽** `networks`에 추가
Grafana도 조인해야 `uss-loki:3100`에 닿는다. external 네트워크는 없으면 스택이 뜨지 않으므로 **uss 배포(네트워크 생성)가 이 PR 머지보다 먼저**여야 한다.

### 8. 대시보드 (중앙 Grafana — 이 레포에 파일을 두지 않는다)

배포 후 중앙 Grafana에서 직접 패널을 구성한다. 확정되면 중앙 레포에 dashboard provisioning(`grafana/provisioning/dashboards/`)을 추가해 파일로 관리한다. 쿼리는 중앙 관행에 맞춰 `job="uss-prod"` 라벨로 필터한다(공통 태그 `application`도 붙지만, 중앙에선 job이 서비스 구분자다).
- 처리량: `sum(rate(http_server_requests_seconds_count{job="uss-prod"}[1m]))`
- 응답시간 p95/p99: `histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket{job="uss-prod"}[1m])))`
- 에러율: `sum(rate(http_server_requests_seconds_count{job="uss-prod", status=~"5.."}[1m])) / sum(rate(http_server_requests_seconds_count{job="uss-prod"}[1m]))`
- JVM: heap used/max, GC pause, live threads
- DB: `hikaricp_connections_active`, `hikaricp_connections_pending` — 커넥션 풀 고갈은 동시성 작업에서 바로 볼 지표다.
- 로그: Loki 패널 `{service="uss-server", log_type="performance"} | logfmt | response_time > 1000`

## 결정 필요 (Decisions needed)

- [x] 액추에이터 포트 — **관리 포트 8081 분리**(호스트 미공개). 대안이던 "8080 그대로 노출 + 화이트리스트"는 8181 포트를 통해 액추에이터가 외부에 열리므로 채택하지 않는다.
- [x] Loki 위치 — **uss compose에서 자체 운영**, 중앙 Grafana에는 datasource만 등록(사용자 확정).
- [x] release 환경 — 앱 계측(메트릭·로그 파일)은 prod/release 공통으로 넣되, **Alloy/Loki/scrape 등록은 prod만** 한다. release 로그는 파일로만 남는다.
- [x] k6 부하 테스트·baseline 측정 — 이번 범위 제외(사용자 확정), 별도 이슈.
- [x] 로컬 모니터링 스택 — **만들지 않는다**(사용자 확정). 배포 환경 + 중앙 스택만 대상. 대시보드는 중앙 Grafana에서 만들고 JSON만 레포에 보관한다.
- [x] compose 프로젝트명 — **그대로 둔다**(6-1 참조). 분리하면 첫 배포가 컨테이너 이름 충돌로 실패해 서버 수동 정리가 필요하다. 대신 `--remove-orphans` 금지를 명시한다.
- [x] Loki retention — **7일**. 공용 서버 디스크 보호.
- [x] p6spy — prod/release에서 `logging.level.p6spy: WARN`으로 막는다. SQL 로그가 Loki로 흘러드는 것을 방지.

## 검증

DB·비즈니스 로직 변경이 없어 기존 테스트는 영향을 받지 않는다.

**빌드·테스트**
- `./gradlew test` — 액추에이터·필터·인터셉터 빈이 추가된 상태로 기존 통합 테스트 컨텍스트가 그대로 뜨는지. (테스트 프로파일에는 `management` 설정이 없으므로 관리 포트 분리는 적용되지 않는다.)

**로컬 수동 확인** (모니터링 컨테이너 없이 앱만)
1. `SPRING_PROFILES_ACTIVE=prod`로 기동 → `curl localhost:8081/actuator/prometheus`에 `http_server_requests_seconds_bucket`이 나오고, `curl localhost:8080/actuator/prometheus`는 404인지.
2. 강의 조회 API 몇 번 호출 → `logs/info/info.log`에 `traceId=`가 붙은 REQUEST/RESPONSE 라인, `logs/api-perf/api-perf.log`에 `response_time=` 라인이 쌓이는지.

**배포 순서 및 확인** (순서가 중요하다 — 중앙 스택의 external 네트워크는 먼저 존재해야 한다)
1. uss PR 머지 → prod 배포 → 서버에서 `docker network ls | grep uss-prod`, `docker ps`에 `uss-loki`·`uss-alloy` 확인.
2. 중앙 레포 PR 머지 → 중앙 스택 재기동 → Prometheus `/targets`에서 `uss-prod` job UP 확인.
3. 중앙 Grafana에서 대시보드 패널에 값이 그려지고, Loki 데이터소스에서 `{service="uss-server"}` 로그가 조회되는지 확인 → 대시보드 JSON export해 레포에 커밋.

## Deviation Log

- `WhitelistEndpoint`: 계획의 `/actuator/**` 화이트리스트 항목을 **넣지 않았다** — 이유: 관리 포트(8081)를 분리하면 액추에이터가 별도 서블릿 컨텍스트에서 뜨고 메인 컨텍스트의 `FilterRegistrationBean`(JWT 필터)이 적용되지 않는다는 것을 실측으로 확인했다(토큰 없이 8081 → 200). 반대로 화이트리스트에 넣어두면 메인 포트의 `/actuator/*` 요청이 JWT 필터를 통과해 핸들러 매핑까지 가서 `GlobalExceptionHandler`가 500 + ERROR 로그를 남긴다(우리가 구축하는 에러 로그 스트림이 오염된다). 제거 후 메인 포트는 401로 막히고 error.log는 비어 있다.
- `logback-spring.xml`: 로그 경로 변수명을 `LOG_PATH` → `LOG_ROOT`로 바꾸고 `${LOG_ROOT:-/var/log/spring}`로 오버라이드 가능하게 했다 — 이유: `LOG_PATH`는 Spring Boot가 예약한 이름이라 외부 값이 먹지 않는다. prod 기본값은 `/var/log/spring` 그대로이며, 로컬에서 파일 로깅을 검증할 때만 `LOG_ROOT`로 경로를 돌린다.
- `HttpLoggingFilter`: 제외 경로 접두사에서 끝 슬래시를 뺐다(`/v3/api-docs/` → `/v3/api-docs`) — 이유: 슬래시가 붙으면 `/v3/api-docs`(정확히 그 경로) 요청이 제외되지 않아 로그에 남았다.
- 대시보드 JSON을 이 레포에 두지 않기로 했다(작성했다가 삭제) — 이유: 이 레포의 JSON은 아무것도 읽지 않는 죽은 파일이다. Grafana가 자동으로 올리려면 중앙 레포의 dashboard provisioning에 있어야 하고, 그러면 원본이 중앙 레포로 간다. 두 곳에 같은 JSON을 두면 어느 쪽이 최신인지 갈린다. 대시보드는 배포 후 중앙 Grafana에서 만들고, 확정되면 중앙 레포 provisioning으로 관리한다.

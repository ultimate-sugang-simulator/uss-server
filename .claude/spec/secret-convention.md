---
description: GitHub Secrets 네이밍 규칙과 발급, 교체 절차
---

# Secret Convention

배포에 쓰이는 값은 GitHub Secrets에 두고, CD 워크플로의 `microsoft/variable-substitution` 단계에서
`application-{profile}.yml`에 주입한다. 이 문서는 그 시크릿의 이름을 어떻게 짓고 어떻게 교체하는지를 정한다.

배포 환경은 둘이다. `release` 브랜치 push가 release 배포를, `main` 브랜치 push가 prod 배포를 트리거한다.

## 네이밍 규칙

- 환경에 따라 **값이 달라야 하는** 시크릿은 `RELEASE_`, `PROD_` 접두사를 붙인다
- 환경과 무관하게 같은 값을 쓰는 시크릿만 접두사 없이 쓴다
- 접두사 없는 이름을 두 워크플로가 함께 참조하고 있다면, 그것은 "환경 간 공유"라는 뜻이다.
  공유해도 되는 값인지 확인하고, 아니면 접두사를 붙여 분리하라

## 현재 분리 상태

### 환경별로 분리된 값

| 시크릿 | release | prod |
|---|---|---|
| DB 접속 URL | `RELEASE_DATABASE_URL` | `PROD_DATABASE_URL` |

### 공유해도 되는 값

배포 대상 서버와 이미지 저장소가 하나뿐이라 환경을 나눌 대상이 아니다.

- 배포 서버 접속: `APPCENTER_SERVER_IP`, `APPCENTER_SERVER_USERNAME`, `APPCENTER_SERVER_PASSWORD`, `APPCENTER_SERVER_PORT`
- Docker Hub 계정: `DOCKER_HUB_USERNAME`, `DOCKER_HUB_PASSWORD`

release와 prod는 같은 서버에서 서로 다른 디렉토리(`/home/serverking/uss/release`, `/home/serverking/uss/prod`)와
서로 다른 이미지 태그(`uss-server:release`, `uss-server:prod`)로 분리된다.

### 아직 분리되지 않은 값

아래는 release와 prod가 같은 시크릿을 참조한다. 환경별로 값이 달라야 하는데 공유하고 있는 것들이다.

| 시크릿 | 공유의 위험 |
|---|---|
| `JWT_SECRET_KEY` | release에서 발급한 토큰이 prod에서 그대로 통한다. 교체 시 두 환경이 동시에 전부 로그아웃된다 |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | 한쪽 계정이 유출되면 두 DB가 모두 열린다 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | release 테스트 메일이 운영 계정에서 나간다 |

분리한다면 `RELEASE_JWT_SECRET_KEY` / `PROD_JWT_SECRET_KEY`처럼 위 네이밍 규칙을 따르고,
두 워크플로의 참조를 각각 바꾼 뒤 배포한다.

## JWT 시크릿 교체 절차

1. 새 값을 생성한다. HS256 대칭키이므로 최소 32바이트 이상의 무작위 문자열을 쓴다
   ```bash
   openssl rand -base64 48
   ```
2. GitHub 저장소 Settings > Secrets and variables > Actions 에서 새 이름으로 등록한다
3. CD 워크플로의 `security.jwt.secret-key` 참조를 새 이름으로 바꾼다
4. 배포한다. release는 `release` 브랜치 push, prod는 `main` 브랜치 push가 트리거다
5. 배포된 서버가 정상 동작하는 것을 확인한 뒤 구 시크릿을 삭제한다

> **경고**: JWT 서명 키를 교체하면 이미 발급된 액세스 토큰과 리프레시 토큰이 전부 무효가 된다.
> 리프레시 토큰을 서버에 저장하지 않고 재발급 엔드포인트도 없으므로, 사용자는 **다시 로그인하는 것 외에 복구 수단이 없다.**
> 게다가 현재 `JWT_SECRET_KEY`는 release와 prod가 공유하므로 **두 환경의 전체 사용자가 함께 로그아웃**된다.
> 교체가 꼭 필요한 상황인지 먼저 판단하고, 가능하면 환경 분리를 먼저 하라.

> 워크플로 머지와 시크릿 등록의 순서가 어긋나면 `security.jwt.secret-key`가 빈 값으로 주입된다.
> 등록을 먼저 하고 워크플로를 머지하라.

## 주의

- 시크릿 값을 코드, 설정 파일, 계획서, PR 본문, 커밋 메시지에 적지 마라. **이름만 적는다**
- `application-prod.yml`과 `application-release.yml`은 워크플로가 값을 덮어쓰는 자리다.
  실제 값을 커밋해 두지 마라
- 워크플로 로그에 시크릿을 `echo` 하지 마라. GitHub가 마스킹하지만 가공된 형태는 새어 나간다

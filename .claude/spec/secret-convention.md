---
description: GitHub Secrets 네이밍 규칙과 발급, 교체 절차
---

# Secret Convention

배포에 쓰이는 값은 GitHub Secrets에 두고, CD 워크플로의 `microsoft/variable-substitution` 단계에서
`application-{profile}.yml`에 주입한다. 이 문서는 그 시크릿의 이름을 어떻게 짓고 어떻게 교체하는지를 정한다.

배포 환경은 **prod 하나뿐**이다. `main` 브랜치 push가 prod 배포를 트리거한다.

## 네이밍 규칙

- 환경에 따라 **값이 달라야 하는** 시크릿만 환경 접두사(`PROD_`)를 붙인다
- 환경이 하나뿐인 지금은 접두사가 사실상 불필요하다. 기존 `PROD_DATABASE_URL`은 그대로 두되,
  **새 시크릿에 접두사를 붙이지 마라.** 환경이 늘어날 때 그때 일괄로 붙인다
- 환경이 다시 둘 이상이 되면, 값이 갈려야 하는 시크릿부터 접두사를 붙여 분리한다.
  접두사 없는 이름을 두 워크플로가 함께 참조하고 있다면 그것은 "환경 간 공유"라는 뜻이므로,
  공유해도 되는 값인지 먼저 확인하라

## 현재 사용 중인 시크릿

| 용도 | 시크릿 | 주입 대상 |
|---|---|---|
| DB 접속 URL | `PROD_DATABASE_URL` | `spring.datasource.url` |
| DB 계정 | `DATABASE_USERNAME`, `DATABASE_PASSWORD` | `spring.datasource.username` / `.password` |
| 메일 계정 | `MAIL_USERNAME`, `MAIL_PASSWORD` | `spring.mail.username` / `.password` |
| JWT 서명 키 | `JWT_SECRET_KEY` | `security.jwt.secret-key` |
| 학교 연계 API 주소 | `INU_API_BASE_URL` | `inu.course-api.base-url` |
| 학교 연계 API 인증 키 | `INU_API_AUTH_KEY` | `inu.course-api.auth-key` |
| Docker Hub | `DOCKER_HUB_USERNAME`, `DOCKER_HUB_PASSWORD` | 이미지 push, 배포 `.env` |
| 배포 서버 접속 | `APPCENTER_SERVER_IP`, `APPCENTER_SERVER_USERNAME`, `APPCENTER_SERVER_PASSWORD`, `APPCENTER_SERVER_PORT` | scp, ssh 액션 |

## 정리 대상

`release` 환경을 없애면서 아래 시크릿은 참조하는 워크플로가 사라졌다.
GitHub 저장소 Settings > Secrets and variables > Actions 에서 삭제한다.

- `RELEASE_DATABASE_URL`

쓰이지 않는 시크릿을 남겨두지 마라. 유출 시 피해 범위만 넓히고, 어떤 값이 살아있는지 판단을 흐린다.

## JWT 시크릿 교체 절차

1. 새 값을 생성한다. HS256 대칭키이므로 최소 32바이트 이상의 무작위 문자열을 쓴다
   ```bash
   openssl rand -base64 48
   ```
2. GitHub 저장소 Settings > Secrets and variables > Actions 에서 새 이름으로 등록한다
3. CD 워크플로(`.github/workflows/cd-prod.yml`)의 `security.jwt.secret-key` 참조를 새 이름으로 바꾼다
4. 배포한다. `main` 브랜치 push가 트리거다
5. 배포된 서버가 정상 동작하는 것을 확인한 뒤 구 시크릿을 삭제한다

> **경고**: JWT 서명 키를 교체하면 이미 발급된 액세스 토큰과 리프레시 토큰이 전부 무효가 된다.
> 리프레시 토큰을 서버에 저장하지 않고 재발급 엔드포인트도 없으므로, 사용자는 **다시 로그인하는 것 외에 복구 수단이 없다.**
> 환경이 prod 하나뿐이라 교체는 곧 **전체 사용자 로그아웃**을 뜻한다. 교체가 꼭 필요한 상황인지 먼저 판단하라.

> 워크플로 머지와 시크릿 등록의 순서가 어긋나면 `security.jwt.secret-key`가 빈 값으로 주입된다.
> 등록을 먼저 하고 워크플로를 머지하라.

## 주의

- 시크릿 값을 코드, 설정 파일, 계획서, PR 본문, 커밋 메시지에 적지 마라. **이름만 적는다**
- `application-prod.yml`은 워크플로가 값을 덮어쓰는 자리다. 실제 값을 커밋해 두지 마라
- 워크플로 로그에 시크릿을 `echo` 하지 마라. GitHub가 마스킹하지만 가공된 형태는 새어 나간다
- 로컬 측정용 `src/main/resources/application-perf.yml`에는 실제 시크릿을 넣지 마라.
  측정 전용 더미 값을 쓴다(파일 안 주석 참조)

---
description: 프로젝트 디렉토리 구조. 새 파일을 생성하거나 패키지 위치를 결정할 때 참조
paths:
  - "src/main/java/**/*.java"
---

# 프로젝트 구조

```
src/main/java/uss/code/
├── global/              # 공통 설정(config), 예외(exception), 어노테이션, HTTP, 공용 infra
├── auth/                # JWT 발급/검증(filter, resolver), 로그인, 비밀번호 인코딩
│
├── member/              # 회원(학생) 정보
├── course/              # 강의 + 강의 시간표(CourseSchedule), 강의 조회/검색
├── cart/                # 장바구니
└── registration/       # 수강신청 (신청/취소/조회)
```

## 도메인 패키지 내부 구조

```
{domain}/
├── controller/
│   ├── {Domain}Controller.java
│   └── {Domain}ControllerDocs.java   # Swagger 문서 인터페이스 (같은 controller/ 패키지에 둔다)
├── service/
│   └── {Domain}Service.java
├── repository/
│   └── {Domain}Repository.java
├── domain/
│   └── {Domain}.java                 # Entity (+ 관련 enum)
├── dto/
│   ├── request/
│   ├── response/
│   └── common/                       # 공용·projection record (선택, 예: cart/dto/common/CartCount)
└── infra/                            # 도메인 전용 검증/헬퍼 (선택, 예: CourseValidator)
```

레이어 흐름은 **Controller → Service → Repository**다 (Facade 레이어 없음). 필요할 때만 `infra/`에 도메인 보조 로직을 둔다.

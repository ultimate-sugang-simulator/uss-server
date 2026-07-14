---
description: Repository 레이어 작성 패턴
paths:
  - "src/main/java/**/repository/**/*.java"
---

# Repository Convention

- JPA Repository 인터페이스는 `{domain}/repository/` 패키지에 위치시켜라
- 쿼리는 Repository 인터페이스에 `@Query`로 직접 작성하라. JPQL을 기본으로 하고, FULLTEXT 등 DB 종속 쿼리만 `nativeQuery = true`로 작성하라 (예: `CourseRepository.findByKeyword`)
  - 파라미터는 `@Param("...")`으로 바인딩하고 `final`로 선언하라
- 기본 조회는 Entity를 반환하고 DTO 조립은 Service에서 한다. 집계·부분 데이터는 record projection으로 직접 반환하라 (예: `CartRepository.countCartedCoursesByCourseId` → `cart/dto/common/CartCount`)

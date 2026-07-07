---
description: Entity(domain) 클래스 작성 패턴
paths:
  - "src/main/java/**/domain/**/*.java"
---

# Domain (Entity) Convention

- `@Entity` + `@Getter` + `@NoArgsConstructor`를 사용하라 (JPA 프록시/리플렉션용)
- `createdAt` 등 시각 필드는 각 Entity에서 직접 관리하라 (공통 `BaseEntity` 없음)
- 검증 로직은 Entity 내부 private 메서드로 구현하라

객체 생성(정적 팩토리 + private `@Builder`)과 예외 처리 규칙은 `common.md`를 따른다.

## DB 매핑

- Entity 클래스명과 테이블명이 다를 경우 `@Table(name = "...")`을 명시하라
- 컬럼명은 `@Column(name = "snake_case", ...)`으로 명시하라 (`@Column(name = "current_enrollment", nullable = false)`)
- enum 매핑은 `@Enumerated(EnumType.STRING)`을 사용하라. ORDINAL을 사용하지 마라
- ID 생성은 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 사용하라 (MySQL AUTO_INCREMENT)
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)` 기본, 컬렉션은 필요 시 `@OneToMany` + `FetchType.LAZY`

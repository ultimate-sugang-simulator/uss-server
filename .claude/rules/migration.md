---
description: Flyway 마이그레이션 및 SQL 작성 규칙
paths:
  - "src/main/resources/database/**"
---

# 마이그레이션 & SQL

## Flyway 파일

- 스키마 마이그레이션: `src/main/resources/database/migration/` (예: `V0_0__init_table.sql`)
- 시드 데이터: `src/main/resources/database/seed/` (예: `V0_1__insert_course.sql`)
- 파일명: `V{major}_{minor}__{description}.sql` (버전 숫자 사이 `_` 1개, 이름 앞 `__` 2개, description은 snake_case)
- 적용된 파일은 절대 수정·삭제하지 마라 (Flyway checksum 실패). 변경은 항상 새 버전 파일로 추가하라
- 각 SQL 문 앞에 목적을 주석으로 설명하라

## SQL 스타일 (MySQL / InnoDB)

- 테이블·컬럼명은 snake_case (`course_schedules`, `current_enrollment`)
- 스토리지 엔진: `ENGINE=InnoDB`
- PK: `id BIGINT AUTO_INCREMENT PRIMARY KEY`
- FK 컬럼: `{참조테이블}_id` (`course_id`, `member_id`) + `FOREIGN KEY ... REFERENCES ...`
- timestamp: `DATETIME`, 시간: `TIME`
- enum: `VARCHAR(50)` (`@Enumerated(EnumType.STRING)`과 매핑, CHECK 제약은 쓰지 않음)
- 인덱스: 테이블 정의 안에 `INDEX idx_{용도} (컬럼)` 인라인 선언
- 전문 검색: `FULLTEXT INDEX ft_idx_{용도} (컬럼들) WITH PARSER ngram`

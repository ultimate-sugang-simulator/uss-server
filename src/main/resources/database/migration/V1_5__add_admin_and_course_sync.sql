-- 백오피스 관리자 계정. API로 생성하지 않고 시드 또는 DB 직접 등록으로만 만든다.
-- 비밀번호는 BCrypt 해시이며 jbcrypt가 $2a$ 형식만 받는다.
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_login_id (login_id)
) ENGINE=InnoDB;

-- 표시 학기. 프론트엔드 노출용 라벨이며 courses 데이터에 영향을 주지 않는다.
-- 항상 1행이며 시드가 초기 1행을 보장한다.
CREATE TABLE IF NOT EXISTS system_semesters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year INT NOT NULL,
    term VARCHAR(50) NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB;

-- 강의 동기화 작업 이력. 진행 중(RUNNING) 작업은 전체에서 하나만 존재한다.
-- 수집, 반영 건수는 SUCCESS가 아니면 NULL이다. 화면이 '-'로 표시한다.
CREATE TABLE IF NOT EXISTS course_sync_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    academic_year INT NOT NULL,
    term VARCHAR(50) NOT NULL,
    strategy VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    phase VARCHAR(50) NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    fetched_course_count INT NULL,
    fetched_schedule_count INT NULL,
    created_count INT NULL,
    updated_count INT NULL,
    closed_count INT NULL,
    warning_count INT NULL,
    partially_applied BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason TEXT NULL,
    FOREIGN KEY (admin_id) REFERENCES admins(id),
    INDEX idx_started_at (started_at),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- 작업별 변경 항목. 작업이 끝나면 불변이라 목록 페이지 오프셋이 밀리지 않는다.
-- course_name은 경고 항목에서 강의 정보 확보에 실패할 수 있어 NULL을 허용한다.
CREATE TABLE IF NOT EXISTS course_sync_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    haksu_code VARCHAR(15) NOT NULL,
    course_name VARCHAR(255) NULL,
    reason VARCHAR(255) NULL,
    FOREIGN KEY (job_id) REFERENCES course_sync_jobs(id) ON DELETE CASCADE,
    INDEX idx_job_change_haksu (job_id, change_type, haksu_code)
) ENGINE=InnoDB;

-- 수정 항목의 필드별 변경 전후 값. before, after는 MySQL 키워드라 접미사를 붙인다.
CREATE TABLE IF NOT EXISTS course_sync_changed_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_id BIGINT NOT NULL,
    field VARCHAR(50) NOT NULL,
    before_value VARCHAR(500) NOT NULL,
    after_value VARCHAR(500) NOT NULL,
    FOREIGN KEY (detail_id) REFERENCES course_sync_details(id) ON DELETE CASCADE,
    INDEX idx_detail_id (detail_id)
) ENGINE=InnoDB;

-- 폐강은 물리 삭제하지 않고 상태로 표시한다. carts, registrations가 course_id를 참조한다.
ALTER TABLE courses ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- 학생 조회 4종이 상태로 먼저 걸러지므로 기존 정렬 인덱스 선두에 status를 넣는다.
ALTER TABLE courses DROP INDEX idx_department_sort;
ALTER TABLE courses DROP INDEX idx_area_sort;
ALTER TABLE courses DROP INDEX idx_huss_sort;
ALTER TABLE courses ADD INDEX idx_department_sort (status, department, grade_code, classification_code, haksu_code);
ALTER TABLE courses ADD INDEX idx_area_sort (status, area, grade_code, classification_code, haksu_code);
ALTER TABLE courses ADD INDEX idx_huss_sort (status, is_huss_course, grade_code, classification_code, haksu_code);

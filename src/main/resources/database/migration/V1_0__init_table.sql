-- 회원. 식별 기준은 학번이다.
-- 인증은 학교 포털(F_LOGIN_CHECK)에 위임하므로 로컬 비밀번호 컬럼을 두지 않는다.
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    college VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    academic_status VARCHAR(50) NOT NULL,
    last_semester_gpa DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB;

-- 강의. 한 행이 분반 하나이며 학년도, 학기, 학수번호로 유일하다.
-- course_code(과목코드)는 같은 과목의 여러 분반이 공유하므로 유일하지 않다.
-- 이수구분, 이수영역, 수업유형, 학년, 집중이수제, 원어강의는 학교 연계 API 원문 코드와 명칭을 그대로 담는다.
-- area만 교양 영역 판별에 쓰이므로 enum 컬럼을 함께 둔다.
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year INT NOT NULL,
    term VARCHAR(50) NOT NULL,
    title_kr VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    haksu_code VARCHAR(15) NOT NULL,
    college VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    classification_code VARCHAR(10) NOT NULL,
    classification_name VARCHAR(50) NOT NULL,
    area VARCHAR(50) NOT NULL,
    area_code VARCHAR(10) NOT NULL,
    area_name VARCHAR(50) NOT NULL,
    type_code VARCHAR(10) NOT NULL,
    type_name VARCHAR(50) NOT NULL,
    grade_code VARCHAR(10) NOT NULL,
    grade_name VARCHAR(50) NOT NULL,
    concentration_code VARCHAR(10) NOT NULL,
    concentration_name VARCHAR(50) NOT NULL,
    credits INT NOT NULL,
    is_english_course BOOLEAN NOT NULL DEFAULT FALSE,
    english_code VARCHAR(10) NOT NULL,
    english_name VARCHAR(50) NOT NULL,
    is_huss_course BOOLEAN NOT NULL DEFAULT FALSE,
    max_capacity INT NOT NULL,
    current_enrollment INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_year_term_haksu (academic_year, term, haksu_code),
    INDEX idx_department_sort (department, grade_code, classification_code, haksu_code),
    INDEX idx_area_sort (area, grade_code, classification_code, haksu_code),
    INDEX idx_huss_sort (is_huss_course, grade_code, classification_code, haksu_code),
    FULLTEXT INDEX ft_idx_course_search (course_code, haksu_code, title_kr, title_en) WITH PARSER ngram
) ENGINE=InnoDB;

-- 강의 시간표. 강의실은 강의가 아니라 시간표에 속한다.
-- 같은 강의도 요일마다 강의실이 다를 수 있어 8.7%가 두 개 이상을 쓴다.
-- day는 H2 예약어라 테이블 생성이 실패하므로 day_of_week로 둔다.
-- period_code(교시 코드)는 B계열만 75분 수업이라 수업 길이 판별의 원천이 된다.
CREATE TABLE IF NOT EXISTS course_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    day_of_week VARCHAR(50) NOT NULL,
    period_code VARCHAR(8) NOT NULL,
    period_name VARCHAR(255) NOT NULL,
    classroom VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB;

-- 수강신청
CREATE TABLE IF NOT EXISTS registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_member_course (member_id, course_id),
    INDEX idx_member_id (member_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB;

-- 장바구니
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_member_course (member_id, course_id),
    INDEX idx_member_id (member_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB;

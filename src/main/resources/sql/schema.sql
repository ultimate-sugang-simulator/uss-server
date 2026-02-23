ALTER DATABASE uss_db_prod CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
ALTER DATABASE uss_db_release CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    student_id VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    member_college VARCHAR(50) NOT NULL,
    member_department VARCHAR(50) NOT NULL,
    member_grade VARCHAR(50) NOT NULL,
    academic_status VARCHAR(50) NOT NULL,
    last_semester_gpa DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_email (email),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title_kr VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    course_code VARCHAR(50) NOT NULL UNIQUE,
    course_college VARCHAR(50) NOT NULL,
    course_department VARCHAR(50) NOT NULL,
    course_classification VARCHAR(50) NOT NULL,
    course_area VARCHAR(50) NOT NULL,
    course_type VARCHAR(50) NOT NULL,
    course_grade VARCHAR(50) NOT NULL,
    professor_name VARCHAR(255),
    classroom VARCHAR(255),
    credits INT NOT NULL,
    is_english_course BOOLEAN NOT NULL DEFAULT FALSE,
    max_capacity INT NOT NULL,
    current_enrollment INT NOT NULL DEFAULT 0,
    INDEX idx_course_code (course_code),
    INDEX idx_course_type (course_type),
    INDEX idx_course_college (course_college),
    INDEX idx_course_department (course_department),
    INDEX idx_course_area (course_area),
    FULLTEXT INDEX ft_idx_course_search (course_code, title_kr, title_en) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    schedule_text VARCHAR(255) NOT NULL,
    course_day VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    INDEX idx_course_id (course_id),
    INDEX idx_course_day (course_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_count INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS challenge_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    code VARCHAR(255) NOT NULL,
    failure_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
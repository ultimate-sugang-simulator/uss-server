-- Dummy Data for Testing
-- 더미 사용자 1명 (컴퓨터공학부)
-- 더미 Cart 3개 (시간표 안겹침)
-- 더미 Registration 3개 (시간표 안겹침)

-- 1. 더미 사용자 (Member)
-- 비밀번호: password123! (BCrypt 암호화)
INSERT INTO members (email, student_id, password, name, member_college, member_department, member_grade,
                     academic_status, last_semester_gpa, created_at, updated_at)
VALUES
('ahh0520@inu.ac.kr', '202012345', '$2a$10$I6nJ6NigpTL80q42scLkKeyTfO6uVP0rhzJC4wq5lmPv79dbFVO1K',
 '테스트', 'INFORMATION_TECHNOLOGY', 'COMPUTER_ENGINEERING', 'SOPHOMORE',
 'ENROLLED', 3.5, NOW(), NOW());

-- 2. 더미 Registration (수강신청 완료된 과목 3개)
-- 시간표가 겹치지 않는 과목들로 구성
INSERT INTO registrations (member_id, course_id, created_at)
VALUES
-- 이산수학 (금요일 13:00-13:50)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = 'IA02009001'),
 NOW()),

-- 확률및통계 (목요일 09:00-09:50)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = 'IAA6068001'),
 NOW()),

-- 데이터프로그래밍 (화요일 야간 18:00-18:50)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = '0011445001'),
 NOW());

-- 3. 더미 Cart (장바구니 과목 3개)
-- 시간표가 겹치지 않는 과목들로 구성
INSERT INTO carts (member_id, course_id, created_at)
VALUES
-- 컴퓨터공학개론 (월요일 10:00-10:50)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = '0001762001'),
 NOW()),

-- 프로그래밍입문 (화요일 13:00-13:50, 수요일 14:00-14:50)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = '0003426001'),
 NOW()),

-- 자료구조 (수요일 09:00-10:15, 금요일 09:00-10:15)
((SELECT id FROM members WHERE email = 'ahh0520@inu.ac.kr'),
 (SELECT id FROM courses WHERE course_code = 'IAA6002001'),
 NOW());

-- 시간표 요약:
-- [수강신청 완료]
-- 월: -
-- 화: 18:00-18:50 (데이터프로그래밍)
-- 수: -
-- 목: 09:00-09:50 (확률및통계)
-- 금: 13:00-13:50 (이산수학)
--
-- [장바구니]
-- 월: 10:00-10:50 (컴퓨터공학개론)
-- 화: 13:00-13:50 (프로그래밍입문)
-- 수: 09:00-10:15 (자료구조), 14:00-14:50 (프로그래밍입문)
-- 목: -
-- 금: 09:00-10:15 (자료구조)
-- 이메일 인증 절차 제거에 따른 인증코드 테이블 삭제
DROP TABLE IF EXISTS email_verification_codes;

-- 회원 식별 기준이 이메일에서 학번으로 옮겨감에 따른 이메일 컬럼 삭제
ALTER TABLE members DROP COLUMN email;

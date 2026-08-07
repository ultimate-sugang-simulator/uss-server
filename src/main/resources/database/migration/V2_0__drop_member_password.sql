-- 회원 도메인에서 password 제거. 인증은 학교 포털(F_LOGIN_CHECK)로 위임하므로 로컬 비밀번호 컬럼을 삭제한다.
ALTER TABLE members DROP COLUMN password;

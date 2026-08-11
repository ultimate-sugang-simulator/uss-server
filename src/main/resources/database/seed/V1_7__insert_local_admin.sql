-- 로컬 개발용 관리자 계정. 아이디 local-admin, 비밀번호 uss-local-admin.
-- 레포가 public이므로 운영 계정을 여기에 넣지 마라. 운영 계정은 DB에 직접 등록한다.
-- 해시는 jbcrypt가 받는 $2a$ 형식이어야 한다. htpasswd가 만드는 $2y$는 거부된다.
INSERT INTO admins (login_id, password, name, role, created_at)
VALUES ('local-admin', '$2a$10$ufu9Et/j47o1mUGO9z2LF.Hiha5XT/O1uOlinc3IWG9IUFyWJYi8u', '로컬관리자', 'ADMIN', NOW());

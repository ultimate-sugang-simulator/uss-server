-- 포털 로그인으로 만들어진 회원은 이메일과 비밀번호가 없어 새 인증 체계에서 로그인할 수 없다.
-- registrations, carts는 member_id에 ON DELETE CASCADE가 걸려 있어 함께 지워진다.
DELETE FROM members;

-- 위 CASCADE는 registrations만 지우고 courses의 수강인원은 건드리지 않아, 신청이 없는데 인원만 남는다.
UPDATE courses SET current_enrollment = 0;

-- 이메일과 비밀번호를 회원 인증 수단으로 추가한다. 남은 행이 없으므로 바로 NOT NULL로 만든다.
ALTER TABLE members
    ADD COLUMN email VARCHAR(255) NOT NULL,
    ADD COLUMN password VARCHAR(255) NOT NULL;

-- 이메일은 로그인 식별자이므로 회원 간 중복될 수 없다. 중복 가입 경합의 최종 방어선이다.
ALTER TABLE members
    ADD CONSTRAINT uk_email UNIQUE (email);

-- 장바구니 조회가 매 요청 carts를 세는 대신 강의가 담긴 수를 직접 들고 있게 한다.
ALTER TABLE courses ADD COLUMN cart_count INT NOT NULL DEFAULT 0;

-- 이미 쌓인 장바구니 행을 반영한다. 빼면 기존 강의가 전부 0으로 시작해 실제와 어긋난 값이 서비스된다.
UPDATE courses c
SET c.cart_count = (
    SELECT COUNT(*) FROM carts WHERE course_id = c.id
);

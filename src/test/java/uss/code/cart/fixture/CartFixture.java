package uss.code.cart.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.cart.domain.Cart;
import uss.code.course.domain.Course;
import uss.code.member.domain.Member;

import java.time.LocalDateTime;

public class CartFixture {

    public static Cart createCart(final Member member, final Course course) {
        return createCart(member, course, LocalDateTime.now());
    }

    public static Cart createCart(
            final Member member,
            final Course course,
            final LocalDateTime createdAt
    ) {
        Cart cart = new Cart();
        ReflectionTestUtils.setField(cart, "member", member);
        ReflectionTestUtils.setField(cart, "course", course);
        ReflectionTestUtils.setField(cart, "createdAt", createdAt);

        increaseCartCount(course);

        return cart;
    }

    private static void increaseCartCount(final Course course) {
        ReflectionTestUtils.setField(course, "cartCount", course.getCartCount() + 1);
    }
}

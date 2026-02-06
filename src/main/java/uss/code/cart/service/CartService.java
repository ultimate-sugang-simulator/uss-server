package uss.code.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.cart.domain.Cart;
import uss.code.cart.dto.common.CartCount;
import uss.code.cart.dto.response.CartedCourseResponse;
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.cart.repository.CartRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final long DEFAULT_CART_COUNT = 0L;

    private final CartRepository cartRepository;

    @Transactional(readOnly = true)
    public CartedCoursesResponse getCart(final long memberId) {
        final List<Cart> carts = cartRepository.findCartedCoursesByMemberId(memberId);

        if (carts.isEmpty()) {
            return CartedCoursesResponse.of(List.of());
        }

        final Map<Long, Long> cartCountMap = getCartCountByCourseId(carts);
        final List<CartedCourseResponse> cartedCourseResponses = carts.stream()
                .map(cart -> {
                    final Long count = cartCountMap.getOrDefault(cart.getCourse().getId(), DEFAULT_CART_COUNT);
                    return CartedCourseResponse.of(cart.getCourse(), count);
                })
                .toList();

        return CartedCoursesResponse.of(cartedCourseResponses);
    }

    private Map<Long, Long> getCartCountByCourseId(final List<Cart> carts) {
        final List<Long> courseIds = extractCourseIds(carts);

        return cartRepository.countCartedCoursesByCourseId(courseIds)
                .stream()
                .collect(Collectors.toMap(
                        CartCount::courseId,
                        CartCount::cartCount
                ));
    }

    private List<Long> extractCourseIds(final List<Cart> carts) {
        return carts.stream()
                .map(cart -> cart.getCourse().getId())
                .distinct()
                .toList();
    }
}

package uss.code.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.cart.domain.Cart;
import uss.code.cart.dto.common.CartCount;
import uss.code.cart.dto.response.CartedCourseResponse;
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.Course;
import uss.code.course.infra.CourseValidator;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uss.code.global.exception.domain.ExceptionCode.*;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final long DEFAULT_CART_COUNT = 0L;

    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public CartedCoursesResponse getCartedCourse(final long memberId) {
        final List<Cart> carts = cartRepository.findByMemberId(memberId);

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

    @Transactional
    public void addCart(
            final long memberId,
            final long courseId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        final List<Cart> carts = cartRepository.findByMemberId(memberId);

        Course course = courseRepository.findByIdWithSchedules(courseId)
                .orElseThrow(() -> new RestApiException(COURSE_NOT_FOUND));

        validateCartLimit(carts);
        validateDuplicateCourse(carts, courseId);
        validateCourseScheduleConflict(carts, course);
        validateCourseTypeLimit(carts, course);

        final Cart cart = Cart.create(member, course);

        cartRepository.save(cart);
    }

    @Transactional
    public void deleteCartedCourse(
            final long memberId,
            final long courseId
    ) {
        final Cart cart = cartRepository.findByMemberIdAndCourseId(memberId, courseId)
                .orElseThrow(() -> new RestApiException(CARTED_COURSE_NOT_FOUND));

        cartRepository.deleteById(cart.getId());
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

    private void validateCartLimit(List<Cart> carts) {
        if (carts.size() >= 10) {
            throw new RestApiException(CARTED_COURSE_LIMIT_EXCEEDED);
        }
    }

    private void validateCourseScheduleConflict(
            final List<Cart> carts,
            final Course course
    ) {
        if (!CourseValidator.validateCourseScheduleNotConflict(carts, course)) {
            throw new RestApiException(COURSE_SCHEDULE_CONFLICT);
        }
    }

    private void validateCourseTypeLimit(
            final List<Cart> carts,
            final Course course
    ){
        if(!CourseValidator.validateCourseTypeLimit(carts, course)) {
            throw new RestApiException(COURSE_TYPE_LIMIT_EXCEEDED);
        }
    }

    private void validateDuplicateCourse(
            final List<Cart> carts,
            final long courseId
    ) {
        boolean exists = carts.stream()
                .anyMatch(cart -> cart.getCourse().getId().equals(courseId));

        if (exists) {
            throw new RestApiException(COURSE_ALREADY_IN_CART);
        }
    }
}

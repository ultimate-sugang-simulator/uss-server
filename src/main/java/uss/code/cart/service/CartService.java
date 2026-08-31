package uss.code.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.cart.domain.Cart;
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

import static uss.code.global.exception.domain.ExceptionCode.*;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int NO_AFFECTED_ROW = 0;

    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public CartedCoursesResponse getCartedCourse(final long memberId) {
        final List<Cart> carts = cartRepository.findByMemberId(memberId);

        final List<CartedCourseResponse> cartedCourseResponses = carts.stream()
                .map(cart -> CartedCourseResponse.of(cart.getCourse()))
                .toList();

        return CartedCoursesResponse.of(cartedCourseResponses);
    }

    @Transactional
    public void addCart(
            final long memberId,
            final long courseId
    ) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        final List<Cart> carts = cartRepository.findByMemberId(memberId);

        final Course course = courseRepository.findByIdWithSchedules(courseId)
                .orElseThrow(() -> new RestApiException(COURSE_NOT_FOUND));

        validateCourseActive(course);
        validateCartLimit(carts);
        validateDuplicateCourse(carts, courseId);
        validateCourseScheduleConflict(carts, course);
        validateCourseTypeLimit(carts, course);

        courseRepository.increaseCartCount(courseId);

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

        decreaseCartCount(courseId);

        cartRepository.delete(cart);
    }

    private void decreaseCartCount(final long courseId) {
        final int affectedRows = courseRepository.decreaseCartCountAboveZero(courseId);

        if (affectedRows == NO_AFFECTED_ROW) {
            throw new RestApiException(CARTED_COURSE_DELETE_CONFLICT);
        }
    }

    private void validateCourseActive(final Course course) {
        if (!course.isActive()) {
            throw new RestApiException(COURSE_CLOSED);
        }
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
        final List<Course> cartedCourses = carts.stream()
                .map(Cart::getCourse)
                .toList();

        if (!CourseValidator.validateCourseScheduleNotConflict(cartedCourses, course)) {
            throw new RestApiException(COURSE_SCHEDULE_CONFLICT);
        }
    }

    private void validateCourseTypeLimit(
            final List<Cart> carts,
            final Course course
    ) {
        final List<Course> cartedCourses = carts.stream()
                .map(Cart::getCourse)
                .toList();

        if (!CourseValidator.validateCourseTypeLimit(cartedCourses, course)) {
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

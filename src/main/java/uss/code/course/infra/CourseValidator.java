package uss.code.course.infra;

import lombok.experimental.UtilityClass;
import uss.code.cart.domain.Cart;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseSchedule;

import java.time.LocalTime;
import java.util.List;

import static uss.code.course.domain.CourseType.K_MOOC;
import static uss.code.course.domain.CourseType.OCU;

@UtilityClass
public class CourseValidator {

    private static final int MAX_OCU_COURSE_COUNT = 2;
    private static final int MAX_K_MOOC_COURSE_COUNT = 1;

    public static boolean validateCourseScheduleNotConflict(
            final List<Cart> carts,
            final Course course
    ) {
        if(carts.isEmpty()){
            return true;
        }

        // 장바구니에 담으려고 하는 과목의 시간표 가져오기
        List<CourseSchedule> courseSchedules = course.getCourseSchedules();

        if(courseSchedules.isEmpty()){
            return true;
        }

        // 현재 장바구니에 있는 과목들의 시간표와 겹치는지 확인
        for (final Cart cart : carts) {
            List<CourseSchedule> cartedCourseSchedules = cart.getCourse().getCourseSchedules();

            for (final CourseSchedule courseSchedule : courseSchedules) {
                final CourseDay courseDay = courseSchedule.getCourseDay();
                final LocalTime courseStartTime = courseSchedule.getStartTime();
                final LocalTime courseEndTime = courseSchedule.getEndTime();

                for (final CourseSchedule cartedSchedule : cartedCourseSchedules) {
                    final CourseDay cartedCourseDay = cartedSchedule.getCourseDay();
                    final LocalTime cartedStartTime = cartedSchedule.getStartTime();
                    final LocalTime cartedEndTime = cartedSchedule.getEndTime();

                    // 같은 요일이고 시간이 겹치는지 확인
                    if (courseDay == cartedCourseDay && isTimeOverlap(courseStartTime, courseEndTime, cartedStartTime, cartedEndTime)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean validateCourseTypeLimit(
            final List<Cart> carts,
            final Course course
    ) {
        final var courseType = course.getCourseType();

        long sameTypeCount = carts.stream()
                .filter(cart -> cart.getCourse().getCourseType() == courseType)
                .count();

        if (courseType == OCU) {
            return sameTypeCount < MAX_OCU_COURSE_COUNT;
        }

        if (courseType == K_MOOC) {
            return sameTypeCount < MAX_K_MOOC_COURSE_COUNT;
        }

        return true;
    }

    private static boolean isTimeOverlap(
            final LocalTime newStart,
            final LocalTime newEnd,
            final LocalTime existingStart,
            final LocalTime existingEnd
    ) {
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }
}

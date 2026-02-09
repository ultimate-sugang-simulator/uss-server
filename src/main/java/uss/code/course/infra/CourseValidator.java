package uss.code.course.infra;

import lombok.experimental.UtilityClass;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseSchedule;
import uss.code.member.domain.Member;
import uss.code.registration.domain.Registration;

import java.time.LocalTime;
import java.util.List;

import static uss.code.course.domain.CourseType.K_MOOC;
import static uss.code.course.domain.CourseType.OCU;

@UtilityClass
public class CourseValidator {

    private static final int MAX_OCU_COURSE_COUNT = 2;
    private static final int MAX_K_MOOC_COURSE_COUNT = 1;

    public static boolean validateCourseScheduleNotConflict(
            final List<Course> existingCourses,
            final Course newCourse
    ) {
        if (existingCourses.isEmpty()) {
            return true;
        }

        // 추가하려는 과목의 시간표 가져오기
        List<CourseSchedule> newCourseSchedules = newCourse.getCourseSchedules();

        if (newCourseSchedules.isEmpty()) {
            return true;
        }

        // 현재 등록된 과목들의 시간표와 겹치는지 확인
        for (final Course existingCourse : existingCourses) {
            List<CourseSchedule> existingCourseSchedules = existingCourse.getCourseSchedules();

            for (final CourseSchedule newSchedule : newCourseSchedules) {
                final CourseDay newCourseDay = newSchedule.getCourseDay();
                final LocalTime newStartTime = newSchedule.getStartTime();
                final LocalTime newEndTime = newSchedule.getEndTime();

                for (final CourseSchedule existingSchedule : existingCourseSchedules) {
                    final CourseDay existingCourseDay = existingSchedule.getCourseDay();
                    final LocalTime existingStartTime = existingSchedule.getStartTime();
                    final LocalTime existingEndTime = existingSchedule.getEndTime();

                    // 같은 요일이고 시간이 겹치는지 확인
                    if (newCourseDay == existingCourseDay && isTimeOverlap(newStartTime, newEndTime, existingStartTime, existingEndTime)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean validateCourseTypeLimit(
            final List<Course> existingCourses,
            final Course newCourse
    ) {
        final var courseType = newCourse.getCourseType();

        long sameTypeCount = existingCourses.stream()
                .filter(course -> course.getCourseType() == courseType)
                .count();

        if (courseType == OCU) {
            return sameTypeCount < MAX_OCU_COURSE_COUNT;
        }

        if (courseType == K_MOOC) {
            return sameTypeCount < MAX_K_MOOC_COURSE_COUNT;
        }

        return true;
    }

    public static boolean validateCreditLimit(
            final List<Registration> registrations,
            final Member member,
            final Course newCourse
    ) {
        int maxCredit = member.getMaxCredit();

        int currentCredit = registrations.stream()
                .mapToInt(registration -> registration.getCourse().getCredits())
                .sum();

        return currentCredit + newCourse.getCredits() <= maxCredit;
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

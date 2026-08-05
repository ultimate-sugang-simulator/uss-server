package uss.code.course.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseSchedule;

import java.time.LocalTime;

public class CourseScheduleFixture {

    private static final String DEFAULT_PERIOD_NAME = "1-2A";
    private static final String DEFAULT_CLASSROOM = "07-401";

    public static CourseSchedule createCourseSchedule(final Course course) {
        return createCourseSchedule(
                course,
                CourseDay.MONDAY,
                DEFAULT_PERIOD_NAME,
                DEFAULT_CLASSROOM,
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );
    }

    public static CourseSchedule createCourseSchedule(
            final Course course,
            final CourseDay dayOfWeek,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        return createCourseSchedule(
                course,
                dayOfWeek,
                DEFAULT_PERIOD_NAME,
                DEFAULT_CLASSROOM,
                startTime,
                endTime
        );
    }

    public static CourseSchedule createCourseSchedule(
            final Course course,
            final CourseDay dayOfWeek,
            final String periodName,
            final String classroom,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        CourseSchedule courseSchedule = new CourseSchedule();
        ReflectionTestUtils.setField(courseSchedule, "course", course);
        ReflectionTestUtils.setField(courseSchedule, "dayOfWeek", dayOfWeek);
        ReflectionTestUtils.setField(courseSchedule, "periodName", periodName);
        ReflectionTestUtils.setField(courseSchedule, "classroom", classroom);
        ReflectionTestUtils.setField(courseSchedule, "startTime", startTime);
        ReflectionTestUtils.setField(courseSchedule, "endTime", endTime);
        return courseSchedule;
    }

}

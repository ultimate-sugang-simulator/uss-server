package uss.code.course.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseSchedule;

import java.time.LocalTime;
import java.util.UUID;

public class CourseScheduleFixture {

    public static CourseSchedule createCourseSchedule(final Course course) {
        return createCourseSchedule(
                course,
                "월 09:00-10:30",
                CourseDay.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );
    }

    public static CourseSchedule createCourseSchedule(
            final Course course,
            final String scheduleText,
            final CourseDay courseDay,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        CourseSchedule courseSchedule = new CourseSchedule();
        ReflectionTestUtils.setField(courseSchedule, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(courseSchedule, "course", course);
        ReflectionTestUtils.setField(courseSchedule, "scheduleText", scheduleText);
        ReflectionTestUtils.setField(courseSchedule, "courseDay", courseDay);
        ReflectionTestUtils.setField(courseSchedule, "startTime", startTime);
        ReflectionTestUtils.setField(courseSchedule, "endTime", endTime);
        return courseSchedule;
    }
}

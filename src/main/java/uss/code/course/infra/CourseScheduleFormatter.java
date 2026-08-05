package uss.code.course.infra;

import lombok.experimental.UtilityClass;
import uss.code.course.domain.CourseSchedule;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class CourseScheduleFormatter {

    private static final String NO_SCHEDULE = "-";
    private static final String GROUP_PREFIX = "[";
    private static final String GROUP_SUFFIX = "]";
    private static final String CLASSROOM_DELIMITER = ":";
    private static final String PERIOD_DELIMITER = ",";
    private static final String PERIOD_PREFIX = "(";
    private static final String PERIOD_SUFFIX = ")";

    public static String format(final List<CourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return NO_SCHEDULE;
        }

        return groupByClassroom(schedules).entrySet().stream()
                .map(entry -> formatClassroomSchedules(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining());
    }

    private static Map<String, List<CourseSchedule>> groupByClassroom(final List<CourseSchedule> schedules) {
        return schedules.stream()
                .sorted(Comparator.comparing(CourseSchedule::getDayOfWeek)
                        .thenComparing(CourseSchedule::getStartTime))
                .collect(Collectors.groupingBy(
                        CourseSchedule::getClassroom,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private static String formatClassroomSchedules(
            final String classroom,
            final List<CourseSchedule> classroomSchedules
    ) {
        final String periods = classroomSchedules.stream()
                .map(CourseScheduleFormatter::formatPeriod)
                .collect(Collectors.joining(PERIOD_DELIMITER));

        return GROUP_PREFIX + classroom + CLASSROOM_DELIMITER + periods + GROUP_SUFFIX;
    }

    private static String formatPeriod(final CourseSchedule schedule) {
        return schedule.getDayOfWeek().getName() + PERIOD_PREFIX + schedule.getPeriodName() + PERIOD_SUFFIX;
    }
}

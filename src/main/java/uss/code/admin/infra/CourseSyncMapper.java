package uss.code.admin.infra;

import org.springframework.stereotype.Component;
import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseCollege;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseSnapshot;
import uss.code.course.domain.CourseTerm;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CourseSyncMapper {

    private static final String ENGLISH_COURSE_FLAG = "Y";
    private static final String HUSS_COURSE_FLAG = "Y";

    private static final String ALL_GRADE_NAME = "전학년";
    private static final String GRADE_NAME_SUFFIX = "학년";
    private static final String DIGITS_PATTERN = "\\d+";

    private static final Pattern BUILDING_NUMBER_PATTERN = Pattern.compile("^제(\\d+)호관");
    private static final String CLASSROOM_DELIMITER = "-";
    private static final String WHITESPACE_PATTERN = "\\s+";
    private static final String SINGLE_SPACE = " ";

    private static final int NO_CREDIT = 0;

    public CourseSnapshot toSnapshot(
            final InuCourseResponse course,
            final int academicYear,
            final CourseTerm term
    ) {
        return CourseSnapshot.builder()
                .academicYear(academicYear)
                .term(term)
                .titleKr(normalize(course.titleKr()))
                .titleEn(normalize(course.titleEn()))
                .courseCode(normalize(course.courseCode()))
                .haksuCode(normalize(course.haksuCode()))
                .college(toCollege(course))
                .department(toDepartment(course))
                .classificationCode(normalize(course.classificationCode()))
                .classificationName(normalize(course.classificationName()))
                .area(toArea(course))
                .areaCode(normalize(course.areaCode()))
                .areaName(normalize(course.areaName()))
                .typeCode(normalize(course.typeCode()))
                .typeName(normalize(course.typeName()))
                .gradeCode(normalize(course.gradeCode()))
                .gradeName(toGradeName(course.gradeName()))
                .concentrationCode(normalize(course.concentrationCode()))
                .concentrationName(normalize(course.concentrationName()))
                .credits(course.credits() == null ? NO_CREDIT : course.credits())
                .isEnglishCourse(ENGLISH_COURSE_FLAG.equalsIgnoreCase(normalize(course.englishYn())))
                .englishCode(normalize(course.englishCode()))
                .englishName(normalize(course.englishName()))
                .isHussCourse(HUSS_COURSE_FLAG.equalsIgnoreCase(normalize(course.hussCourseYn())))
                .build();
    }

    public CourseSchedule toSchedule(final InuTimetableResponse timetable) {
        final CourseDay dayOfWeek = CourseDay.tryFromCode(timetable.dayCode())
                .orElseThrow(() -> new CourseMappingException("미등록 요일 코드: " + timetable.dayCode()));

        return CourseSchedule.create(
                dayOfWeek,
                normalize(timetable.periodCode()),
                normalize(timetable.periodName()),
                toClassroom(timetable.roomName()),
                toTime(timetable.startTime()),
                toTime(timetable.endTime())
        );
    }

    private CourseCollege toCollege(final InuCourseResponse course) {
        return CourseCollege.tryFromCode(course.collegeCode())
                .orElseThrow(() -> new CourseMappingException("미등록 단과대학 코드: " + course.collegeCode()));
    }

    private CourseDepartment toDepartment(final InuCourseResponse course) {
        return CourseDepartment.tryFromCode(course.departmentCode())
                .orElseThrow(() -> new CourseMappingException("미등록 학과 코드: " + course.departmentCode()));
    }

    private CourseArea toArea(final InuCourseResponse course) {
        return CourseArea.tryFromCode(course.areaCode())
                .orElseThrow(() -> new CourseMappingException("미등록 이수영역 코드: " + course.areaCode()));
    }

    private String toGradeName(final String gradeName) {
        final String normalized = normalize(gradeName);

        if (normalized.isBlank()) {
            return ALL_GRADE_NAME;
        }

        if (normalized.matches(DIGITS_PATTERN)) {
            return normalized + GRADE_NAME_SUFFIX;
        }

        return normalized;
    }

    private String toClassroom(final String roomName) {
        final String normalized = normalize(roomName);

        final int delimiterIndex = normalized.indexOf(CLASSROOM_DELIMITER);
        if (delimiterIndex < 0) {
            return normalized;
        }

        final String building = normalized.substring(0, delimiterIndex);
        final String room = normalized.substring(delimiterIndex + 1).split(SINGLE_SPACE)[0];

        final Matcher matcher = BUILDING_NUMBER_PATTERN.matcher(building);
        if (matcher.find()) {
            return "%02d%s%s".formatted(Integer.parseInt(matcher.group(1)), CLASSROOM_DELIMITER, room);
        }

        return building + CLASSROOM_DELIMITER + room;
    }

    private LocalTime toTime(final String value) {
        try {
            return LocalTime.parse(normalize(value));
        } catch (final DateTimeParseException e) {
            throw new CourseMappingException("해석할 수 없는 교시 시각: " + value);
        }
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll(WHITESPACE_PATTERN, SINGLE_SPACE);
    }
}

package uss.code.admin.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.admin.domain.SystemSemester;
import uss.code.course.domain.CourseTerm;

import java.time.LocalDateTime;

public class SystemSemesterFixture {

    private static final int DEFAULT_ACADEMIC_YEAR = 2026;
    private static final CourseTerm DEFAULT_TERM = CourseTerm.SECOND;

    public static SystemSemester createSystemSemester() {
        return createSystemSemester(DEFAULT_ACADEMIC_YEAR, DEFAULT_TERM);
    }

    public static SystemSemester createSystemSemester(
            final int academicYear,
            final CourseTerm term
    ) {
        SystemSemester systemSemester = new SystemSemester();

        ReflectionTestUtils.setField(systemSemester, "academicYear", academicYear);
        ReflectionTestUtils.setField(systemSemester, "term", term);
        ReflectionTestUtils.setField(systemSemester, "updatedAt", LocalDateTime.now());

        return systemSemester;
    }
}

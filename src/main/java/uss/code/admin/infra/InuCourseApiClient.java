package uss.code.admin.infra;

import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.course.domain.CourseTerm;

import java.util.List;

public interface InuCourseApiClient {
    List<InuCourseResponse> fetchCourses(
            final int academicYear,
            final CourseTerm term
    );

    List<InuTimetableResponse> fetchTimetables(
            final int academicYear,
            final CourseTerm term
    );
}

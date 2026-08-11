package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.dto.common.CourseTermInfo;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CourseTermResponse(
        int academicYear,
        String termCode,
        String termName
) {
    public static CourseTermResponse from(final CourseTermInfo courseTermInfo) {
        return CourseTermResponse.builder()
                .academicYear(courseTermInfo.academicYear())
                .termCode(courseTermInfo.term().getCode())
                .termName(courseTermInfo.term().getName())
                .build();
    }
}

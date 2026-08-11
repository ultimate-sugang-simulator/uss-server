package uss.code.course.domain;

public record CourseFieldChange(
        String field,

        String before,

        String after
) {
    public static CourseFieldChange of(
            final String field,
            final String before,
            final String after
    ) {
        return new CourseFieldChange(field, before, after);
    }
}

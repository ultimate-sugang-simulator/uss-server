package uss.code.course.dto.common;

public record CourseCapacity(
        long id,

        int currentEnrollment,

        int maxCapacity
) {
    public boolean isRegisterable() {
        return currentEnrollment < maxCapacity;
    }
}

package uss.code.course.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.member.domain.MemberDepartment;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CourseCacheWarmer {

    private final CourseCacheLoader courseCacheLoader;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${cache.major-courses.refresh-cron}", zone = "${cache.major-courses.refresh-zone}")
    public void warmMajorCourses() {
        Arrays.stream(MemberDepartment.values())
                .filter(department -> !CourseDepartment.ownedBy(department).isEmpty())
                .forEach(courseCacheLoader::refreshMajorCourses);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${cache.general-education-courses.refresh-cron}", zone = "${cache.general-education-courses.refresh-zone}")
    public void warmGeneralEducationCourses() {
        Arrays.stream(CourseArea.values())
                .filter(CourseArea::isGeneralEducationArea)
                .forEach(courseCacheLoader::refreshGeneralEducationCourses);
    }
}

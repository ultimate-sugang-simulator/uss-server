package uss.code.admin.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inu.course-api")
public record InuCourseApiProperties(
        String baseUrl,

        String authKey,

        String modDate
) {}

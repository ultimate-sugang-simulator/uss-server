package uss.code.global.http;

import org.springframework.http.HttpMethod;

import java.util.List;

public final class AdminEndpoint {

    private static final String ADMIN_BASE_PATH = "/api/v1/admin";
    private static final String PATH_DELIMITER = "/";

    private static final List<EndPoint> ADMIN_WHITELIST = List.of(
            new EndPoint("/api/v1/admin/auth/login", HttpMethod.POST),
            new EndPoint("/api/v1/admin/auth/refresh", HttpMethod.POST)
    );

    public static boolean isAdminPath(final String uri) {
        return uri.equals(ADMIN_BASE_PATH) || uri.startsWith(ADMIN_BASE_PATH + PATH_DELIMITER);
    }

    public static boolean isWhitelisted(
            final String path,
            final String method
    ) {
        return ADMIN_WHITELIST.stream()
                .anyMatch(endpoint -> endpoint.matches(path, method));
    }

    private record EndPoint(
            String path,
            HttpMethod httpMethod
    ) {
        boolean matches(
                final String uri,
                final String method
        ) {
            return path.equals(uri) && httpMethod.name().equalsIgnoreCase(method);
        }
    }
}

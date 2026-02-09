package uss.code.global.http;

import org.springframework.http.HttpMethod;

import java.util.List;

public final class WhitelistEndpoint {

    private static final List<EndPoint> WHITELIST = List.of(
            new EndPoint("/api/v1/auth/login", HttpMethod.POST),
            new EndPoint("/api/v1/auth/sign-up", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes/re", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes", HttpMethod.PATCH),
            new EndPoint("/swagger-ui/**", null),
            new EndPoint("/v3/api-docs/**", null),
            new EndPoint("/swagger-resources/**", null)
    );

    public static boolean isWhitelisted(
            final String path,
            final String method
    ) {
        return WHITELIST.stream()
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
            final boolean pathMatches = isPathMatch(uri);
            final boolean methodMatches = httpMethod == null || httpMethod.name().equalsIgnoreCase(method);
            return pathMatches && methodMatches;
        }

        private boolean isPathMatch(final String uri) {
            if (path.endsWith("/**")) {
                final String basePattern = path.substring(0, path.length() - 3);
                return uri.startsWith(basePattern);
            }
            return path.equals(uri);
        }
    }
}
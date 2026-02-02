package uss.code.global.http;

import org.springframework.http.HttpMethod;

import java.util.List;

public final class WhitelistEndpoint {

    private static final List<EndPoint> WHITELIST = List.of(
            new EndPoint("/api/v1/auth/login", HttpMethod.POST),
            new EndPoint("/api/v1/auth/sign-up", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes/re", HttpMethod.POST),
            new EndPoint("/api/v1/email-verification-codes", HttpMethod.PATCH)
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
            return path.equals(uri) && httpMethod.name().equalsIgnoreCase(method);
        }
    }
}
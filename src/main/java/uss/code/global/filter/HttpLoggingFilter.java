package uss.code.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final int TRACE_ID_LENGTH = 16;

    private static final String MEMBER_ID_ATTRIBUTE = "member-id";

    private static final List<String> EXCLUDE_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources"
    );

    private static final List<String> SENSITIVE_KEYS = List.of(
            "access-token", "password"
    );
    private static final String MASK_VALUE = "****";

    private static final String QUERY_DELIMITER = "&";
    private static final String KEY_VALUE_DELIMITER = "=";

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        MDC.put(TRACE_ID_KEY, generateTraceId());

        try {
            if (isExcluded(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            logRequest(request);
            filterChain.doFilter(request, response);
            logResponse(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, TRACE_ID_LENGTH);
    }

    private boolean isExcluded(final HttpServletRequest request) {
        final String uri = request.getRequestURI();

        return EXCLUDE_PREFIXES.stream()
                .anyMatch(uri::startsWith);
    }

    private void logRequest(final HttpServletRequest request) {
        log.info("[REQUEST] {} {}", request.getMethod(), buildUri(request));
    }

    private void logResponse(
            final HttpServletRequest request,
            final HttpServletResponse response
    ) {
        final Object memberId = request.getAttribute(MEMBER_ID_ATTRIBUTE);

        log.info("[RESPONSE] {} {} memberId={} status={}",
                request.getMethod(), buildUri(request), memberId, response.getStatus());
    }

    private String buildUri(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        final String query = request.getQueryString();

        if (query == null || query.isBlank()) {
            return path;
        }

        return path + "?" + maskSensitiveParams(query);
    }

    // 인코딩된 원본 쿼리를 먼저 쪼갠 뒤 키·값을 개별 디코딩한다.
    // 통째로 디코딩하면 값에 담긴 %26(&)·%3D(=)가 구분자로 둔갑해 파라미터가 잘못 쪼개진다.
    private String maskSensitiveParams(final String query) {
        final StringBuilder masked = new StringBuilder();
        final String[] params = query.split(QUERY_DELIMITER);

        for (int i = 0; i < params.length; i++) {
            masked.append(maskIfSensitive(params[i]));

            if (i < params.length - 1) {
                masked.append(QUERY_DELIMITER);
            }
        }

        return masked.toString();
    }

    private String maskIfSensitive(final String param) {
        final int delimiterIndex = param.indexOf(KEY_VALUE_DELIMITER);

        if (delimiterIndex < 0) {
            return decode(param);
        }

        final String key = decode(param.substring(0, delimiterIndex));

        if (isSensitiveKey(key)) {
            return key + KEY_VALUE_DELIMITER + MASK_VALUE;
        }

        final String value = decode(param.substring(delimiterIndex + 1));

        return key + KEY_VALUE_DELIMITER + value;
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private boolean isSensitiveKey(final String key) {
        return SENSITIVE_KEYS.stream()
                .anyMatch(key::equalsIgnoreCase);
    }
}

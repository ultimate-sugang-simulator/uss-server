package uss.code.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiPerformanceInterceptor implements HandlerInterceptor {

    private static final Logger API_PERF = LoggerFactory.getLogger("API_PERF");
    private static final String LOG_FORMAT = "type=API_PERFORMANCE method={} uri={} response_time={} status={}";

    private static final String START_TIME_ATTRIBUTE = "start-time";
    private static final long RESPONSE_TIME_THRESHOLD_MS = 3_000L;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler
    ) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());

        return true;
    }

    @Override
    public void afterCompletion(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler,
            final Exception exception
    ) {
        final Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);

        if (startTime == null) {
            return;
        }

        final long responseTime = System.currentTimeMillis() - startTime;
        final String method = request.getMethod();
        final String uri = request.getRequestURI();
        final int status = response.getStatus();

        if (responseTime > RESPONSE_TIME_THRESHOLD_MS) {
            API_PERF.warn(LOG_FORMAT, method, uri, responseTime, status);
            return;
        }

        API_PERF.info(LOG_FORMAT, method, uri, responseTime, status);
    }
}

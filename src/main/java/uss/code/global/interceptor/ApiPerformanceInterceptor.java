package uss.code.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Log4j2(topic = "API_PERF")
@Component
public class ApiPerformanceInterceptor implements HandlerInterceptor {

    private static final String LOG_FORMAT = "type=API_PERFORMANCE method={} uri={} response_time={} status={}";

    private static final String START_TIME_ATTRIBUTE = "start-time";
    private static final long RESPONSE_TIME_THRESHOLD_MS = 3_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler
    ) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());

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

        final long responseTime = (System.nanoTime() - startTime) / NANOS_PER_MILLI;
        final String method = request.getMethod();
        final String uri = request.getRequestURI();
        final int status = response.getStatus();

        if (responseTime > RESPONSE_TIME_THRESHOLD_MS) {
            log.warn(LOG_FORMAT, method, uri, responseTime, status);
            return;
        }

        log.info(LOG_FORMAT, method, uri, responseTime, status);
    }
}

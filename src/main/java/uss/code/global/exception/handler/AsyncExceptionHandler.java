package uss.code.global.exception.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Log4j2
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(
            final Throwable ex,
            final Method method,
            final Object... params
    ) {
        log.error("Uncaught async exception. method={}, message={}", method.getName(), ex.getMessage(), ex);
    }
}

package uss.code.global.exception.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Log4j2
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... parmas) {
        log.error("비동기 처리중 예외 발생. 예외: {} | 메서드: {}", ex.getMessage(), method.getName());
    }
}

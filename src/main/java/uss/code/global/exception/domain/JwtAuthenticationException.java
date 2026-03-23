package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtAuthenticationException extends RuntimeException {
    private final int code;
    private final String message;

    public  JwtAuthenticationException(final ExceptionCode exceptionCode) {
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
    }
}

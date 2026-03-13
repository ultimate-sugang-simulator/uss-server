package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class AtAuthenticationException extends RuntimeException {
    private final int code;
    private final String message;

    public AtAuthenticationException(final AuthenticationExceptionCode exceptionCode) {
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
    }
}

package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtTokenInvalidException extends JwtAuthenticationException {
    public JwtTokenInvalidException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

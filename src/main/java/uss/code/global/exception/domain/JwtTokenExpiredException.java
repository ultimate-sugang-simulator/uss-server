package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtTokenExpiredException extends JwtAuthenticationException {

    public JwtTokenExpiredException(final ExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

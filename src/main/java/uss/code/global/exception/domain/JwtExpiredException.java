package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtExpiredException extends JwtAuthenticationException {
    public JwtExpiredException(final AuthenticationExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

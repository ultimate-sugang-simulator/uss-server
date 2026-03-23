package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtInvalidException extends JwtAuthenticationException {
    public JwtInvalidException(final AuthenticationExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

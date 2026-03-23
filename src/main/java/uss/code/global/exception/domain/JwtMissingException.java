package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtMissingException extends JwtAuthenticationException {
    public JwtMissingException(final AuthenticationExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

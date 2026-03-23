package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class AtInvalidException extends AtAuthenticationException {
    public AtInvalidException(final AuthenticationExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class AtMissingException extends AtAuthenticationException {
    public AtMissingException(final AuthenticationExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

package uss.code.global.exception.domain;

import lombok.Getter;

@Getter
public class JwtTokenMissingException extends JwtAuthenticationException {
    public JwtTokenMissingException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}

package uss.code.global.exception.dto.response;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
        HttpStatus status,
        int code,
        String message
) {
    public static ErrorResponse of(
            HttpStatus status,
            int code,
            String message
    ){
        return new ErrorResponse(status, code, message);
    }
}

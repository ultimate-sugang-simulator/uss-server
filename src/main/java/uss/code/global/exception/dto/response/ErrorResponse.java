package uss.code.global.exception.dto.response;

public record ErrorResponse(
        int code,
        String message
) {
    public static ErrorResponse of(
            final int code,
            final String message
    ){
        return new ErrorResponse(code, message);
    }
}

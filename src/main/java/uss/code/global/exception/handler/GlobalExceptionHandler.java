package uss.code.global.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;
import uss.code.global.exception.domain.ExceptionCode;
import uss.code.global.exception.domain.JwtAuthenticationException;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.exception.dto.response.ErrorResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_REQUEST_PARAMETER;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(final RestApiException e) {
        log.warn("Business exception. code={}, message={}", e.getExceptionCode().getCode(), e.getMessage());
        return makeExceptionResponse(e.getExceptionCode());
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(final JwtAuthenticationException e) {
        log.warn("Authentication failed. code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(UNAUTHORIZED).body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidException(final MethodArgumentNotValidException e) {
        final BindingResult result = e.getBindingResult();

        final StringBuilder errorMessages = new StringBuilder();
        for (FieldError error : result.getFieldErrors()) {
            errorMessages
                    .append("[ ")
                    .append(error.getField()).append(" ] [ ")
                    .append(error.getDefaultMessage()).append(" ] [ ")
                    .append(error.getRejectedValue())
                    .append(" ]\n");
        }

        final String message = errorMessages.toString().trim();
        log.warn("Request validation failed. errors={}", message);

        return makeExceptionResponse(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(final ConstraintViolationException e) {
        log.warn("Request parameter invalid. message={}", e.getMessage());
        return makeExceptionResponse(INVALID_REQUEST_PARAMETER);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        log.warn("Request body unreadable. message={}", e.getMessage());

        if (isEnumConversionFailure(e)) {
            return makeExceptionResponse(INVALID_ENUM_TYPE);
        }

        return makeExceptionResponse(INVALID_REQUEST_PARAMETER);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e){
        log.error("Unexpected exception. message={}", e.getMessage(), e);
        return makeExceptionResponse(ExceptionCode.UNEXPECTED_SERVER_ERROR);
    }

    private boolean isEnumConversionFailure(final HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();

        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormat) {
                final Class<?> targetType = invalidFormat.getTargetType();
                return targetType != null && targetType.isEnum();
            }
            cause = cause.getCause();
        }

        return false;
    }

    private ResponseEntity<ErrorResponse> makeExceptionResponse(final ExceptionCode exceptionCode) {
        return ResponseEntity.status(exceptionCode.getStatus()).body(makeErrorResponse(exceptionCode));
    }

    private ResponseEntity<ErrorResponse> makeExceptionResponse(final String message){
        return ResponseEntity.status(BAD_REQUEST).body(makeErrorResponse(message));
    }

    private ErrorResponse makeErrorResponse(final ExceptionCode exceptionCode) {
        return ErrorResponse.of(
                exceptionCode.getCode(),
                exceptionCode.getMessage()
        );
    }

    private ErrorResponse makeErrorResponse(final String message) {
        return ErrorResponse.of(
                400,
                message
        );
    }
}

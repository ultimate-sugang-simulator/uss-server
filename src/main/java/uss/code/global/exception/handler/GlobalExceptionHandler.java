package uss.code.global.exception.handler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uss.code.global.exception.domain.ExceptionCode;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.exception.dto.response.ErrorResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_REQUEST_PARAMETER;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(final RestApiException e) {
        return makeExceptionResponse(e.getExceptionCode());
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

        return makeExceptionResponse(errorMessages.toString().trim());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(final ConstraintViolationException e) {
        return makeExceptionResponse(INVALID_REQUEST_PARAMETER);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e){
        return makeExceptionResponse(ExceptionCode.UNEXPECTED_SERVER_ERROR);
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

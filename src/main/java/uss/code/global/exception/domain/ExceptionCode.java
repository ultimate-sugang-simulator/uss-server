package uss.code.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {
    // 전역
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR, 9999, "서버 내부 오류가 발생했습니다."),
    INVALID_ENUM_TYPE(BAD_REQUEST, 8888, "유효하지 않은 열거타입입니다."),
    INVALID_REQUEST_PARAMETER(BAD_REQUEST, 7777, "유효하지 않은 입력 파라미터입니다."),
    // 액세스 토큰
    MISSING_ACCESS_TOKEN(UNAUTHORIZED, 1000, "액세스 토큰이 누락되었습니다."),
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, 1001, "액세스 토큰이 유효하지 않습니다."),
    INVALID_FORM_ACCESS_TOKEN(UNAUTHORIZED, 1002, "액세스 토큰 형식이 올바르지 않습니다."),
    INVALID_SIGNATURE_ACCESS_TOKEN(UNAUTHORIZED, 1003, "액세스 토큰 서명이 유효하지 않습니다."),
    EXPIRED_ACCESS_TOKEN(UNAUTHORIZED, 1004, "액세스 토큰이 만료되었습니다."),

    // 리프레시 토큰
    MISSING_REFRESH_TOKEN(UNAUTHORIZED, 1005, "리프레시 토큰이 누락되었습니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, 1006, "리프레시 토큰이 유효하지 않습니다."),
    INVALID_FORM_REFRESH_TOKEN(UNAUTHORIZED, 1007, "리프레시 토큰 형식이 올바르지 않습니다."),
    INVALID_SIGNATURE_REFRESH_TOKEN(UNAUTHORIZED, 1008, "리프레시 토큰 서명이 유효하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(UNAUTHORIZED, 1009, "리프레시 토큰이 만료되었습니다."),

    // 회원
    MEMBER_NOT_FOUND(NOT_FOUND, 1010, "사용자를 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(BAD_REQUEST, 1011, "이미 존재하는 사용자입니다."),
    PASSWORD_NOT_MATCH(UNAUTHORIZED, 1012, "비밀번호가 일치하지 않습니다."),

    // 이메일 인증
    EMAIL_SENDING_FAILED(SERVICE_UNAVAILABLE, 1013, "이메일 전송에 실패했습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(NOT_FOUND, 1014, "이메일 인증 정보를 찾을 수 없습니다."),
    EMAIL_VERIFICATION_IN_PROGRESS(BAD_REQUEST, 1015, "이미 인증이 진행 중입니다."),
    EMAIL_VERIFICATION_NOT_COMPLETED(BAD_REQUEST, 1016, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFICATION_ALREADY_COMPLETED(BAD_REQUEST, 1017, "이미 인증이 완료되었습니다."),
    VERIFICATION_CODE_NOT_FOUND(NOT_FOUND, 1018, "인증코드를 찾을 수 없습니다."),
    VERIFICATION_CODE_EXPIRED(BAD_REQUEST, 1019, "인증코드가 만료되었습니다."),
    VERIFICATION_CODE_NOT_MATCH(BAD_REQUEST, 1020, "인증코드가 일치하지 않습니다."),
    VERIFICATION_RESEND_LIMIT_EXCEEDED(BAD_REQUEST, 1021, "인증코드 재전송 횟수를 초과했습니다."),
    VERIFICATION_FAILED_LIMIT_EXCEEDED(BAD_REQUEST, 1022, "인증 실패 횟수를 초과했습니다."),

    // 과목
    INVALID_GENERAL_EDUCATION_AREA(BAD_REQUEST, 2000, "유효하지 않은 교양 영역입니다."),
    INVALID_INTERDISCIPLINARY_DEPARTMENT(BAD_REQUEST, 2001, "유효하지 않은 연계전공과목입니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
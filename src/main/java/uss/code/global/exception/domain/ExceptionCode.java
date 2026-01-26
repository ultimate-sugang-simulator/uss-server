package uss.code.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {

    // 전역
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "예기치 못한 예외 발생"),

    // 엑세스 토큰
    MISSING_ACCESS_TOKEN(UNAUTHORIZED, 1000, "엑세스 토큰이 존재하지 않습니다."),
    EXPIRED_PERIOD_ACCESS_TOKEN(UNAUTHORIZED, 1001, "만료된 엑세스 토큰입니다."),
    INVALID_FORM_ACCESS_TOKEN(UNAUTHORIZED, 1002, "형식이 올바르지 않은 엑세스 토큰입니다."),
    INVALID_SIGNATURE_ACCESS_TOKEN(UNAUTHORIZED, 1003, "서명이 유효하지 않은 엑세스 토큰입니다."),
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, 1004, "유효하지 않은 엑세스 토큰입니다."),

    // 리프레쉬 토큰
    MISSING_REFRESH_TOKEN(UNAUTHORIZED, 1005, "리프레쉬 토큰이 존재하지 않습니다."),
    EXPIRED_PERIOD_REFRESH_TOKEN(UNAUTHORIZED, 1006, "만료된 리프레쉬 토큰입니다."),
    INVALID_FORM_REFRESH_TOKEN(UNAUTHORIZED, 1007, "형식이 올바르지 않은 리프레쉬 토큰입니다."),
    INVALID_SIGNATURE_REFRESH_TOKEN(UNAUTHORIZED, 1008, "서명이 유효하지 않은 리프레쉬 토큰입니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, 1009, "유효하지 않은 리프레쉬 토큰입니다."),

    // 인증
    MEMBER_ACCOUNT_NOT_FOUND(NOT_FOUND, 1010, "존재하지 않는 사용자입니다."),
    PASSWORD_NOT_CORRECT(UNAUTHORIZED, 1011, "비밀번호가 일치하지 않습니다."),

    // 맴버
    MEMBER_NOT_FOUND(NOT_FOUND, 1012, "사용자 조회에 실패하였습니다."),
    MEMBER_ALREADY_EXISTS(BAD_REQUEST, 1013, "이미 존재하는 사용자입니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}

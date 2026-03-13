package uss.code.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
@RequiredArgsConstructor
public enum AuthenticationExceptionCode {

    // 공통
    MEMBER_NOT_FOUND(UNAUTHORIZED, 9999, "인증에 필요한 유저 정보 조회에 실패하였습니다."),

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

    // 입장 토큰(대기열)
    MISSING_ADMISSION_TOKEN(UNAUTHORIZED, 1010, "입장 토큰(대기열)이 누락되었습니다."),
    INVALID_ADMISSION_TOKEN(UNAUTHORIZED, 1011, "입장 토큰(대기열)이 유효하지 않습니다."),
    ADMISSION_TOKEN_NOT_FOUND(UNAUTHORIZED, 1012, "입장 토큰(대기열) 조회에 실패하였습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
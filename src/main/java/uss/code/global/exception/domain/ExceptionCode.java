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
    PASSWORD_NOT_MATCH(UNAUTHORIZED, 1012, "비밀번호가 일치하지 않습니다."),

    // 과목
    INVALID_GENERAL_EDUCATION_AREA(BAD_REQUEST, 2000, "유효하지 않은 교양 영역입니다."),
    INVALID_INTERDISCIPLINARY_DEPARTMENT(BAD_REQUEST, 2001, "유효하지 않은 연계전공과목입니다."),
    COURSE_NOT_FOUND(NOT_FOUND, 2002, "과목을 찾을 수 없습니다."),

    // 장바구니
    CARTED_COURSE_NOT_FOUND(NOT_FOUND, 3000, "장바구니에 담은 과목을 찾을 수 없습니다."),
    CARTED_COURSE_LIMIT_EXCEEDED(BAD_REQUEST, 3001, "장바구니는 최대 10개의 과목을 담을 수 있습니다."),
    COURSE_SCHEDULE_CONFLICT(CONFLICT, 3002, "과목 시간표가 겹칩니다."),
    COURSE_TYPE_LIMIT_EXCEEDED(BAD_REQUEST, 3003, "해당 과목 유형의 등록 제한을 초과했습니다."),
    COURSE_ALREADY_IN_CART(BAD_REQUEST, 3004, "이미 장바구니에 담긴 과목입니다."),

    // 수강신청
    COURSE_MAX_CAPACITY_EXCEEDED(BAD_REQUEST, 4000, "수강 정원이 마감되었습니다."),
    CREDIT_LIMIT_EXCEEDED(BAD_REQUEST, 4001, "최대 이수 가능 학점을 초과하였습니다."),
    COURSE_ALREADY_REGISTERED(BAD_REQUEST, 4002, "이미 신청된 과목입니다."),
    REGISTERED_COURSE_NOT_FOUND(NOT_FOUND, 4003, "수강신청한 과목을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
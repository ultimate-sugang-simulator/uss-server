package uss.code.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode {

    // 전역
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR, 9999, "서버 내부 오류가 발생했습니다."),
    INVALID_ENUM_TYPE(BAD_REQUEST, 8888, "유효하지 않은 열거타입입니다."),
    INVALID_REQUEST_PARAMETER(BAD_REQUEST, 7777, "유효하지 않은 입력 파라미터입니다."),

    // 회원
    MEMBER_NOT_FOUND(NOT_FOUND, 2000, "사용자를 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(BAD_REQUEST, 2001, "이미 존재하는 사용자입니다."),
    PASSWORD_NOT_MATCH(UNAUTHORIZED, 2002, "비밀번호가 일치하지 않습니다."),

    // 이메일 인증
    EMAIL_SENDING_FAILED(SERVICE_UNAVAILABLE, 2100, "이메일 전송에 실패했습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(NOT_FOUND, 2101, "이메일 인증 정보를 찾을 수 없습니다."),
    EMAIL_VERIFICATION_IN_PROGRESS(BAD_REQUEST, 2102, "이미 인증이 진행 중입니다."),
    EMAIL_VERIFICATION_NOT_COMPLETED(BAD_REQUEST, 2103, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFICATION_ALREADY_COMPLETED(BAD_REQUEST, 2104, "이미 인증이 완료되었습니다."),
    VERIFICATION_CODE_NOT_FOUND(NOT_FOUND, 2105, "인증코드를 찾을 수 없습니다."),
    VERIFICATION_CODE_EXPIRED(BAD_REQUEST, 2106, "인증코드가 만료되었습니다."),
    VERIFICATION_CODE_NOT_MATCH(BAD_REQUEST, 2107, "인증코드가 일치하지 않습니다."),
    VERIFICATION_RESEND_LIMIT_EXCEEDED(BAD_REQUEST, 2108, "인증코드 재전송 횟수를 초과했습니다."),
    VERIFICATION_FAILED_LIMIT_EXCEEDED(BAD_REQUEST, 2109, "인증 실패 횟수를 초과했습니다."),

    // 과목
    INVALID_GENERAL_EDUCATION_AREA(BAD_REQUEST, 3000, "유효하지 않은 교양 영역입니다."),
    INVALID_INTERDISCIPLINARY_DEPARTMENT(BAD_REQUEST, 3001, "유효하지 않은 연계전공과목입니다."),
    COURSE_NOT_FOUND(NOT_FOUND, 3002, "과목을 찾을 수 없습니다."),

    // 장바구니
    CARTED_COURSE_NOT_FOUND(NOT_FOUND, 4000, "장바구니에 담은 과목을 찾을 수 없습니다."),
    CARTED_COURSE_LIMIT_EXCEEDED(BAD_REQUEST, 4001, "장바구니는 최대 10개의 과목을 담을 수 있습니다."),
    COURSE_SCHEDULE_CONFLICT(CONFLICT, 4002, "과목 시간표가 겹칩니다."),
    COURSE_TYPE_LIMIT_EXCEEDED(BAD_REQUEST, 4003, "해당 과목 유형의 등록 제한을 초과했습니다."),
    COURSE_ALREADY_IN_CART(BAD_REQUEST, 4004, "이미 장바구니에 담긴 과목입니다."),

    // 수강신청
    COURSE_MAX_CAPACITY_EXCEEDED(BAD_REQUEST, 5000, "수강 정원이 마감되었습니다."),
    CREDIT_LIMIT_EXCEEDED(BAD_REQUEST, 5001, "최대 이수 가능 학점을 초과하였습니다."),
    COURSE_ALREADY_REGISTERED(BAD_REQUEST, 5002, "이미 신청된 과목입니다."),
    REGISTERED_COURSE_NOT_FOUND(NOT_FOUND, 5003, "수강신청한 과목을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
package uss.code.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {
    // 전역 (GLB)
    UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR, "GLB-001", "서버 내부 오류가 발생했어요."),
    INVALID_ENUM_TYPE(BAD_REQUEST, "GLB-002", "유효하지 않은 열거타입이에요."),
    INVALID_REQUEST_PARAMETER(BAD_REQUEST, "GLB-003", "유효하지 않은 입력 파라미터예요."),

    // 액세스 토큰 (AUTH)
    MISSING_ACCESS_TOKEN(UNAUTHORIZED, "AUTH-001", "액세스 토큰이 누락됐어요."),
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, "AUTH-002", "액세스 토큰이 유효하지 않아요."),
    INVALID_FORM_ACCESS_TOKEN(UNAUTHORIZED, "AUTH-003", "액세스 토큰 형식이 올바르지 않아요."),
    INVALID_SIGNATURE_ACCESS_TOKEN(UNAUTHORIZED, "AUTH-004", "액세스 토큰 서명이 유효하지 않아요."),
    EXPIRED_ACCESS_TOKEN(UNAUTHORIZED, "AUTH-005", "액세스 토큰이 만료됐어요."),

    // 회원 (MEM)
    MEMBER_NOT_FOUND(NOT_FOUND, "MEM-001", "사용자를 찾을 수 없어요."),
    PASSWORD_NOT_MATCH(UNAUTHORIZED, "MEM-002", "비밀번호가 일치하지 않아요."),
    EMAIL_ALREADY_EXISTS(CONFLICT, "MEM-003", "이미 사용 중인 이메일이에요."),
    COLLEGE_DEPARTMENT_MISMATCH(BAD_REQUEST, "MEM-004", "학과의 소속 단과대학과 일치하지 않아요."),

    // 과목 (CRS)
    INVALID_GENERAL_EDUCATION_AREA(BAD_REQUEST, "CRS-001", "유효하지 않은 교양 영역이에요."),
    INVALID_INTERDISCIPLINARY_DEPARTMENT(BAD_REQUEST, "CRS-002", "유효하지 않은 연계전공과목이에요."),
    COURSE_NOT_FOUND(NOT_FOUND, "CRS-003", "과목을 찾을 수 없어요."),
    COURSE_CLOSED(BAD_REQUEST, "CRS-004", "폐강된 과목이에요."),
    COURSE_SCHEDULE_CONFLICT(CONFLICT, "CRS-005", "과목 시간표가 겹쳐요."),
    COURSE_TYPE_LIMIT_EXCEEDED(BAD_REQUEST, "CRS-006", "해당 과목 유형의 등록 제한을 초과했어요."),

    // 장바구니 (CART)
    CARTED_COURSE_NOT_FOUND(NOT_FOUND, "CART-001", "장바구니에 담은 과목을 찾을 수 없어요."),
    CARTED_COURSE_LIMIT_EXCEEDED(BAD_REQUEST, "CART-002", "장바구니는 최대 10개의 과목을 담을 수 있어요."),
    COURSE_ALREADY_IN_CART(BAD_REQUEST, "CART-003", "이미 장바구니에 담긴 과목이에요."),
    CARTED_COURSE_DELETE_CONFLICT(CONFLICT, "CART-004", "장바구니 삭제를 반영할 수 없어요. 다시 확인해주세요."),

    // 수강신청 (REG)
    COURSE_MAX_CAPACITY_EXCEEDED(BAD_REQUEST, "REG-001", "수강 정원이 마감됐어요."),
    CREDIT_LIMIT_EXCEEDED(BAD_REQUEST, "REG-002", "최대 이수 가능 학점을 초과했어요."),
    COURSE_ALREADY_REGISTERED(BAD_REQUEST, "REG-003", "이미 신청된 과목이에요."),
    REGISTERED_COURSE_NOT_FOUND(NOT_FOUND, "REG-004", "수강신청한 과목을 찾을 수 없어요."),
    REGISTRATION_CANCEL_CONFLICT(CONFLICT, "REG-005", "수강 취소를 반영할 수 없어요. 다시 확인해주세요."),

    // 관리자 (ADM)
    ADMIN_LOGIN_FAILED(UNAUTHORIZED, "ADM-001", "아이디나 비밀번호가 맞지 않아요."),
    ADMIN_NOT_FOUND(NOT_FOUND, "ADM-002", "관리자를 찾을 수 없어요."),
    ADMIN_ACCESS_DENIED(FORBIDDEN, "ADM-003", "관리자 권한이 없어요."),
    SYSTEM_SEMESTER_NOT_FOUND(NOT_FOUND, "ADM-004", "표시 학기 설정을 찾을 수 없어요."),
    SYNC_JOB_ALREADY_RUNNING(CONFLICT, "ADM-005", "이미 업데이트가 진행 중이에요."),
    SYNC_STRATEGY_MISMATCH(CONFLICT, "ADM-006", "데이터가 변경됐어요. 다시 확인해주세요."),
    SYNC_JOB_NOT_FOUND(NOT_FOUND, "ADM-007", "업데이트 작업을 찾을 수 없어요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

package uss.code.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.exception.domain.JwtTokenInvalidException;
import uss.code.global.exception.domain.JwtTokenMissingException;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.InuMemberRepository;
import uss.code.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static uss.code.global.exception.domain.ExceptionCode.*;

@IntegrationTest
class AuthServiceTest {

    private static final String TEST_STUDENT_ID = "202012345";
    private static final String TEST_RAW_PASSWORD = "password1234";
    private static final String TEST_NAME = "홍길동";
    private static final MemberCollege TEST_COLLEGE = MemberCollege.INFORMATION_TECHNOLOGY;
    private static final MemberDepartment TEST_DEPARTMENT = MemberDepartment.COMPUTER_ENGINEERING;
    private static final MemberGrade TEST_GRADE = MemberGrade.JUNIOR;
    private static final AcademicStatus TEST_ACADEMIC_STATUS = AcademicStatus.ENROLLED;
    private static final double TEST_GPA = 3.5;

    private static final String UNKNOWN_STUDENT_ID = "209999999";
    private static final String WRONG_PASSWORD = "wrongPassword1234";
    private static final long UNKNOWN_MEMBER_ID = 999_999L;

    private static final String OTHER_SECRET_KEY = "another-secret-key-for-signature-mismatch-test";
    private static final String MALFORMED_TOKEN = "this-is-not-a-jwt";
    private static final long EXPIRED_VALIDITY_TIME = -60_000L;
    private static final long VALID_VALIDITY_TIME = 600_000L;

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private InuMemberRepository inuMemberRepository;

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private long savedMemberId;

    @BeforeEach
    void setUp() {
        reset(inuMemberRepository);

        final Member member = MemberFixture.createMember(
                TEST_STUDENT_ID,
                TEST_NAME,
                TEST_COLLEGE,
                TEST_DEPARTMENT,
                TEST_GRADE,
                TEST_ACADEMIC_STATUS,
                TEST_GPA
        );

        memberRepository.save(member);

        savedMemberId = member.getId();
    }

    private String generateExpiredToken(final long memberId) {
        return new JwtProvider(secretKey, EXPIRED_VALIDITY_TIME)
                .generateAuthToken(memberId)
                .accessToken();
    }

    private String generateTokenSignedWithOtherKey(final long memberId) {
        return new JwtProvider(OTHER_SECRET_KEY, VALID_VALIDITY_TIME)
                .generateAuthToken(memberId)
                .accessToken();
    }

    @Nested
    class 로그인_테스트 {

        @Test
        void 포털_인증에_성공하고_가입된_회원이면_액세스_토큰을_발급한다() {
            //given
            given(inuMemberRepository.verifyInuMember(TEST_STUDENT_ID, TEST_RAW_PASSWORD)).willReturn(true);

            final LoginRequest request = new LoginRequest(TEST_STUDENT_ID, TEST_RAW_PASSWORD);

            //when
            final AuthTokenResponse response = authService.login(request);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(jwtProvider.getMemberId(response.accessToken())).isEqualTo(savedMemberId);
        }

        @Test
        void 포털_인증에_성공하고_미가입_회원이면_회원을_생성하고_토큰을_발급한다() {
            //given
            given(inuMemberRepository.verifyInuMember(UNKNOWN_STUDENT_ID, TEST_RAW_PASSWORD)).willReturn(true);
            assertThat(memberRepository.findByStudentId(UNKNOWN_STUDENT_ID)).isEmpty();

            final LoginRequest request = new LoginRequest(UNKNOWN_STUDENT_ID, TEST_RAW_PASSWORD);

            //when
            final AuthTokenResponse response = authService.login(request);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(memberRepository.findByStudentId(UNKNOWN_STUDENT_ID)).isPresent();
        }

        @Test
        void 포털_인증에_실패하면_예외가_발생한다() {
            //given
            given(inuMemberRepository.verifyInuMember(TEST_STUDENT_ID, WRONG_PASSWORD)).willReturn(false);

            final LoginRequest request = new LoginRequest(TEST_STUDENT_ID, WRONG_PASSWORD);

            //when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", PORTAL_LOGIN_FAILED);
        }
    }

    @Nested
    class 액세스_토큰_재발급_테스트 {

        @Test
        void 만료된_토큰이어도_서명이_유효하면_재발급에_성공한다() {
            //given
            final String expiredToken = generateExpiredToken(savedMemberId);

            assertThatThrownBy(() -> jwtProvider.validateToken(expiredToken))
                    .hasFieldOrPropertyWithValue("code", EXPIRED_ACCESS_TOKEN.getCode());

            //when
            final AuthTokenResponse response = authService.reIssue(expiredToken);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.accessToken()).isNotEqualTo(expiredToken);
            assertThat(jwtProvider.getMemberId(response.accessToken())).isEqualTo(savedMemberId);
            assertThatCode(() -> jwtProvider.validateToken(response.accessToken()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 아직_만료되지_않은_토큰으로도_재발급에_성공한다() {
            //given
            given(inuMemberRepository.verifyInuMember(TEST_STUDENT_ID, TEST_RAW_PASSWORD)).willReturn(true);

            final String validToken = authService.login(new LoginRequest(TEST_STUDENT_ID, TEST_RAW_PASSWORD))
                    .accessToken();

            //when
            final AuthTokenResponse response = authService.reIssue(validToken);

            //then
            assertThat(jwtProvider.getMemberId(response.accessToken())).isEqualTo(savedMemberId);
            assertThatCode(() -> jwtProvider.validateToken(response.accessToken()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 토큰이_없으면_예외가_발생한다() {
            //given

            //when & then
            assertThatThrownBy(() -> authService.reIssue(null))
                    .isInstanceOf(JwtTokenMissingException.class)
                    .hasFieldOrPropertyWithValue("code", MISSING_ACCESS_TOKEN.getCode());
        }

        @Test
        void 서명이_다른_토큰이면_예외가_발생한다() {
            //given
            final String otherKeyToken = generateTokenSignedWithOtherKey(savedMemberId);

            //when & then
            assertThatThrownBy(() -> authService.reIssue(otherKeyToken))
                    .isInstanceOf(JwtTokenInvalidException.class)
                    .hasFieldOrPropertyWithValue("code", INVALID_SIGNATURE_ACCESS_TOKEN.getCode());
        }

        @Test
        void 형식이_올바르지_않은_토큰이면_예외가_발생한다() {
            //given

            //when & then
            assertThatThrownBy(() -> authService.reIssue(MALFORMED_TOKEN))
                    .isInstanceOf(JwtTokenInvalidException.class)
                    .hasFieldOrPropertyWithValue("code", INVALID_FORM_ACCESS_TOKEN.getCode());
        }

        @Test
        void 토큰의_회원이_존재하지_않으면_예외가_발생한다() {
            //given
            final String unknownMemberToken = generateExpiredToken(UNKNOWN_MEMBER_ID);

            //when & then
            assertThatThrownBy(() -> authService.reIssue(unknownMemberToken))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }
    }
}

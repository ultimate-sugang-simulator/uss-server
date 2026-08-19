package uss.code.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.dto.response.EmailAvailabilityResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.auth.infra.MemberPasswordEncoder;
import uss.code.global.exception.domain.JwtTokenInvalidException;
import uss.code.global.exception.domain.JwtTokenMissingException;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.*;

@IntegrationTest
class AuthServiceTest {

    private static final String TEST_EMAIL = "student@inu.ac.kr";
    private static final String TEST_RAW_PASSWORD = "password1234";
    private static final String TEST_STUDENT_ID = "202012345";
    private static final String TEST_NAME = "홍길동";
    private static final String TEST_COLLEGE = "INFORMATION_TECHNOLOGY";
    private static final String TEST_DEPARTMENT = "COMPUTER_ENGINEERING";
    private static final String TEST_GRADE = "JUNIOR";
    private static final String TEST_ACADEMIC_STATUS = "ENROLLED";
    private static final double TEST_GPA = 3.5;

    private static final String UNKNOWN_EMAIL = "unknown@inu.ac.kr";
    private static final String WRONG_PASSWORD = "wrongPassword1234";
    private static final String MISMATCHED_COLLEGE = "ENGINEERING";
    private static final String UNKNOWN_DEPARTMENT = "존재하지_않는_학과";
    private static final long UNKNOWN_MEMBER_ID = 999_999L;

    private static final String OTHER_SECRET_KEY = "another-secret-key-for-signature-mismatch-test";
    private static final String MALFORMED_TOKEN = "this-is-not-a-jwt";
    private static final long EXPIRED_VALIDITY_TIME = -60_000L;
    private static final long VALID_VALIDITY_TIME = 600_000L;
    private static final long ADMIN_VALIDITY_TIME = 7_200_000L;

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private MemberPasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private SignUpRequest createSignUpRequest(
            final String college,
            final String department
    ) {
        return new SignUpRequest(
                TEST_EMAIL,
                TEST_RAW_PASSWORD,
                TEST_STUDENT_ID,
                TEST_NAME,
                college,
                department,
                TEST_GRADE,
                TEST_ACADEMIC_STATUS,
                TEST_GPA
        );
    }

    private SignUpRequest createSignUpRequest() {
        return createSignUpRequest(TEST_COLLEGE, TEST_DEPARTMENT);
    }

    private String generateExpiredToken(final long memberId) {
        return new JwtProvider(secretKey, EXPIRED_VALIDITY_TIME, ADMIN_VALIDITY_TIME)
                .generateAuthToken(memberId)
                .accessToken();
    }

    private String generateTokenSignedWithOtherKey(final long memberId) {
        return new JwtProvider(OTHER_SECRET_KEY, VALID_VALIDITY_TIME, ADMIN_VALIDITY_TIME)
                .generateAuthToken(memberId)
                .accessToken();
    }

    @Nested
    class 회원가입할_때 {

        @Test
        void 유효한_요청이면_가입에_성공하고_토큰을_반환한다() {
            //given
            final SignUpRequest request = createSignUpRequest();

            //when
            final AuthTokenResponse response = authService.signUp(request);

            //then
            assertThat(response.accessToken()).isNotBlank();

            final Member member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(jwtProvider.getMemberId(response.accessToken())).isEqualTo(member.getId());
            assertThat(member.getCollege()).isEqualTo(MemberCollege.INFORMATION_TECHNOLOGY);
        }

        @Test
        void 비밀번호는_평문으로_저장되지_않는다() {
            //given
            final SignUpRequest request = createSignUpRequest();

            //when
            authService.signUp(request);

            //then
            final Member member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(member.getPassword()).isNotEqualTo(TEST_RAW_PASSWORD);
            assertThat(passwordEncoder.matches(TEST_RAW_PASSWORD, member.getPassword())).isTrue();
        }

        @Test
        void 이미_사용_중인_이메일이면_예외를_반환한다() {
            //given
            authService.signUp(createSignUpRequest());

            //when & then
            assertThatThrownBy(() -> authService.signUp(createSignUpRequest()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", EMAIL_ALREADY_EXISTS);
        }

        @Test
        void 학과와_단과대학이_어긋나면_예외를_반환한다() {
            //given
            final SignUpRequest request = createSignUpRequest(MISMATCHED_COLLEGE, TEST_DEPARTMENT);

            //when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COLLEGE_DEPARTMENT_MISMATCH);
        }

        @Test
        void 유효하지_않은_학과면_예외를_반환한다() {
            //given
            final SignUpRequest request = createSignUpRequest(TEST_COLLEGE, UNKNOWN_DEPARTMENT);

            //when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }

    @Nested
    class 이메일_중복을_검사할_때 {

        @Test
        void 쓰이지_않은_이메일이면_사용_가능으로_응답한다() {
            //given

            //when
            final EmailAvailabilityResponse response = authService.checkEmailAvailability(UNKNOWN_EMAIL);

            //then
            assertThat(response.available()).isTrue();
        }

        @Test
        void 이미_쓰이는_이메일이면_사용_불가로_응답한다() {
            //given
            authService.signUp(createSignUpRequest());

            //when
            final EmailAvailabilityResponse response = authService.checkEmailAvailability(TEST_EMAIL);

            //then
            assertThat(response.available()).isFalse();
        }
    }

    @Nested
    class 로그인할_때 {

        @BeforeEach
        void setUp() {
            authService.signUp(createSignUpRequest());
        }

        @Test
        void 이메일과_비밀번호가_맞으면_토큰을_반환한다() {
            //given
            final LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_RAW_PASSWORD);

            //when
            final AuthTokenResponse response = authService.login(request);

            //then
            assertThat(response.accessToken()).isNotBlank();

            final Member member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(jwtProvider.getMemberId(response.accessToken())).isEqualTo(member.getId());
        }

        @Test
        void 없는_이메일이면_예외를_반환한다() {
            //given
            final LoginRequest request = new LoginRequest(UNKNOWN_EMAIL, TEST_RAW_PASSWORD);

            //when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }

        @Test
        void 비밀번호가_틀리면_예외를_반환한다() {
            //given
            final LoginRequest request = new LoginRequest(TEST_EMAIL, WRONG_PASSWORD);

            //when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", PASSWORD_NOT_MATCH);
        }
    }

    @Nested
    class 액세스_토큰_재발급_테스트 {

        private long savedMemberId;

        @BeforeEach
        void setUp() {
            final Member member = memberRepository.save(MemberFixture.createMember());
            savedMemberId = member.getId();
        }

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
            final String validToken = jwtProvider.generateAuthToken(savedMemberId).accessToken();

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

package uss.code.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uss.code.admin.domain.Admin;
import uss.code.admin.dto.request.AdminLoginRequest;
import uss.code.admin.dto.response.AdminTokenResponse;
import uss.code.admin.fixture.AdminFixture;
import uss.code.admin.repository.AdminRepository;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.exception.domain.JwtTokenInvalidException;
import uss.code.global.exception.domain.JwtTokenMissingException;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_ACCESS_DENIED;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_LOGIN_FAILED;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_FORM_ACCESS_TOKEN;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_SIGNATURE_ACCESS_TOKEN;
import static uss.code.global.exception.domain.ExceptionCode.MISSING_ACCESS_TOKEN;

@IntegrationTest
class AdminAuthServiceTest {

    private static final String OTHER_SECRET_KEY = "other-secret-key-for-unit-testing-purposes-only";
    private static final long EXPIRED_VALIDITY_TIME = -60_000L;
    private static final long VALID_VALIDITY_TIME = 600_000L;
    private static final String MALFORMED_TOKEN = "malformed.token";

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private AdminRepository adminRepository;

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private Admin savedAdmin;

    @BeforeEach
    void setUp() {
        savedAdmin = adminRepository.save(AdminFixture.createAdmin());
    }

    private String generateExpiredAdminToken(final long adminId) {
        return new JwtProvider(secretKey, VALID_VALIDITY_TIME, EXPIRED_VALIDITY_TIME)
                .generateAdminToken(adminId);
    }

    private String generateAdminTokenSignedWithOtherKey(final long adminId) {
        return new JwtProvider(OTHER_SECRET_KEY, VALID_VALIDITY_TIME, VALID_VALIDITY_TIME)
                .generateAdminToken(adminId);
    }

    @Nested
    class 관리자_로그인_테스트 {

        @Test
        void 아이디와_비밀번호가_맞으면_토큰과_이름을_받는다() {
            //given
            final AdminLoginRequest request = new AdminLoginRequest(
                    AdminFixture.DEFAULT_LOGIN_ID,
                    AdminFixture.DEFAULT_PASSWORD
            );

            //when
            final AdminTokenResponse response = adminAuthService.login(request);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.name()).isEqualTo(AdminFixture.DEFAULT_NAME);
        }

        @Test
        void 발급된_토큰은_관리자_토큰이다() {
            //given
            final AdminLoginRequest request = new AdminLoginRequest(
                    AdminFixture.DEFAULT_LOGIN_ID,
                    AdminFixture.DEFAULT_PASSWORD
            );

            //when
            final AdminTokenResponse response = adminAuthService.login(request);

            //then
            assertThat(jwtProvider.isAdminToken(response.accessToken())).isTrue();
            assertThat(jwtProvider.getAdminId(response.accessToken())).isEqualTo(savedAdmin.getId());
        }

        @Test
        void 존재하지_않는_아이디면_예외가_발생한다() {
            //given
            final AdminLoginRequest request = new AdminLoginRequest(
                    "unknown-admin",
                    AdminFixture.DEFAULT_PASSWORD
            );

            //when & then
            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", ADMIN_LOGIN_FAILED);
        }

        @Test
        void 비밀번호가_틀리면_아이디_없음과_같은_코드로_실패한다() {
            //given
            final AdminLoginRequest request = new AdminLoginRequest(
                    AdminFixture.DEFAULT_LOGIN_ID,
                    "wrong-password"
            );

            //when & then
            assertThatThrownBy(() -> adminAuthService.login(request))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", ADMIN_LOGIN_FAILED);
        }
    }

    @Nested
    class 관리자_토큰_재발급_테스트 {

        @Test
        void 유효한_토큰이면_재발급에_성공한다() {
            //given
            final String accessToken = jwtProvider.generateAdminToken(savedAdmin.getId());

            //when
            final AdminTokenResponse response = adminAuthService.reIssue(accessToken);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.name()).isEqualTo(AdminFixture.DEFAULT_NAME);
        }

        @Test
        void 만료된_토큰이어도_서명이_유효하면_재발급에_성공한다() {
            //given
            final String expiredToken = generateExpiredAdminToken(savedAdmin.getId());

            //when
            final AdminTokenResponse response = adminAuthService.reIssue(expiredToken);

            //then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(jwtProvider.isAdminToken(response.accessToken())).isTrue();
        }

        @Test
        void 토큰이_없으면_예외가_발생한다() {
            //when & then
            assertThatThrownBy(() -> adminAuthService.reIssue(null))
                    .isInstanceOf(JwtTokenMissingException.class)
                    .hasFieldOrPropertyWithValue("code", MISSING_ACCESS_TOKEN.getCode());
        }

        @Test
        void 형식이_올바르지_않은_토큰이면_예외가_발생한다() {
            //when & then
            assertThatThrownBy(() -> adminAuthService.reIssue(MALFORMED_TOKEN))
                    .isInstanceOf(JwtTokenInvalidException.class)
                    .hasFieldOrPropertyWithValue("code", INVALID_FORM_ACCESS_TOKEN.getCode());
        }

        @Test
        void 서명이_다른_토큰이면_예외가_발생한다() {
            //given
            final String otherKeyToken = generateAdminTokenSignedWithOtherKey(savedAdmin.getId());

            //when & then
            assertThatThrownBy(() -> adminAuthService.reIssue(otherKeyToken))
                    .isInstanceOf(JwtTokenInvalidException.class)
                    .hasFieldOrPropertyWithValue("code", INVALID_SIGNATURE_ACCESS_TOKEN.getCode());
        }

        @Test
        void 관리자_토큰이_아니면_예외가_발생한다() {
            //given
            final String memberToken = jwtProvider.generateAuthToken(savedAdmin.getId()).accessToken();

            //when & then
            assertThatThrownBy(() -> adminAuthService.reIssue(memberToken))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", ADMIN_ACCESS_DENIED);
        }

        @Test
        void 토큰의_관리자가_존재하지_않으면_예외가_발생한다() {
            //given
            final String accessToken = jwtProvider.generateAdminToken(savedAdmin.getId() + 999L);

            //when & then
            assertThatThrownBy(() -> adminAuthService.reIssue(accessToken))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", ADMIN_NOT_FOUND);
        }
    }
}

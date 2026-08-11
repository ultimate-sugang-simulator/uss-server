package uss.code.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admin.domain.Admin;
import uss.code.admin.dto.request.AdminLoginRequest;
import uss.code.admin.dto.response.AdminTokenResponse;
import uss.code.admin.infra.AdminPasswordEncoder;
import uss.code.admin.repository.AdminRepository;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.exception.domain.RestApiException;

import static uss.code.global.exception.domain.ExceptionCode.ADMIN_ACCESS_DENIED;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_LOGIN_FAILED;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JwtProvider jwtProvider;
    private final AdminPasswordEncoder passwordEncoder;

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public AdminTokenResponse login(final AdminLoginRequest request) {
        final Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new RestApiException(ADMIN_LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), admin.getPassword()))
            throw new RestApiException(ADMIN_LOGIN_FAILED);

        return AdminTokenResponse.of(jwtProvider.generateAdminToken(admin.getId()), admin.getName());
    }

    @Transactional(readOnly = true)
    public AdminTokenResponse reIssue(final String accessToken) {
        final Long adminId = jwtProvider.getAdminIdAllowingExpiration(accessToken);

        if (!jwtProvider.isAdminToken(accessToken))
            throw new RestApiException(ADMIN_ACCESS_DENIED);

        final Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RestApiException(ADMIN_NOT_FOUND));

        return AdminTokenResponse.of(jwtProvider.generateAdminToken(admin.getId()), admin.getName());
    }
}

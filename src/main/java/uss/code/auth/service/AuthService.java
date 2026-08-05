package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.auth.infra.PasswordEncoder;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.ExceptionCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AuthTokenResponse login(final LoginRequest request) {
        final Member member = memberRepository.findByStudentId(request.studentId())
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword()))
            throw new RestApiException(PASSWORD_NOT_MATCH);

        return jwtProvider.generateAuthToken(member.getId());
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse reIssue(final String accessToken) {
        final Long memberId = jwtProvider.getMemberIdAllowingExpiration(accessToken);

        if (!memberRepository.existsById(memberId))
            throw new RestApiException(MEMBER_NOT_FOUND);

        return jwtProvider.generateAuthToken(memberId);
    }
}

package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.auth.infra.PasswordEncoder;
import uss.code.auth.repository.EmailVerificationCodeRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.BusinessExceptionCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Transactional(readOnly = true)
    public AuthTokenResponse login(final LoginRequest request) {
        final Member member = memberRepository.findByStudentId(request.studentId())
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword()))
            throw new RestApiException(PASSWORD_NOT_MATCH);

        return jwtProvider.generateAuthTokens(member.getId());
    }

    @Transactional
    public void signUp(final SignUpRequest request) {
        if(!emailVerificationCodeRepository.existsByEmailAndVerified(request.email(), true))
            throw new RestApiException(EMAIL_VERIFICATION_NOT_COMPLETED);

        if (validateUserExists(request.email()))
            throw new RestApiException(MEMBER_ALREADY_EXISTS);

        final Member member = Member.signUp(
                request,
                passwordEncoder.encode(request.password())
        );

        emailVerificationCodeRepository.deleteByEmail(request.email());

        memberRepository.save(member);
    }

    private boolean validateUserExists(final String email){
        return memberRepository.existsByEmail(email);
    }
}

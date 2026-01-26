package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
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

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public AuthTokenResponse login(final LoginRequest request) {
        final Member member = memberRepository.findByStudentId(request.studentId())
                .orElseThrow(() -> new RestApiException(MEMBER_ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword()))
            throw new RestApiException(PASSWORD_NOT_CORRECT);

        return jwtProvider.generateAuthToken(member.getId());
    }

    @Transactional
    public void signUp(final SignUpRequest request) {
        if (validateUserExists(request.studentId()))
            throw new RestApiException(MEMBER_ALREADY_EXISTS);

        final Member member = Member.signUp(
                request,
                passwordEncoder.encode(request.password())
        );

        memberRepository.save(member);
    }

    private boolean validateUserExists(final String studentId){
        return memberRepository.existsByStudentId(studentId);
    }
}

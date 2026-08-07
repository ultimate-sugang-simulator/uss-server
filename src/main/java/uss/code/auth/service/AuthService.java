package uss.code.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.InuMemberRepository;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.PORTAL_LOGIN_FAILED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;

    private final MemberRepository memberRepository;
    private final InuMemberRepository inuMemberRepository;

    @Transactional
    public AuthTokenResponse login(final LoginRequest request) {
        if (!inuMemberRepository.verifyInuMember(request.studentId(), request.password()))
            throw new RestApiException(PORTAL_LOGIN_FAILED);

        final Member member = memberRepository.findByStudentId(request.studentId())
                .orElseGet(() -> memberRepository.save(Member.createDefault(request.studentId())));

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

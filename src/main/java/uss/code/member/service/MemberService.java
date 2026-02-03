package uss.code.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.dto.res.MemberProfileResponse;
import uss.code.member.repository.MemberRepository;

import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(final Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        return MemberProfileResponse.of(member);
    }
}
